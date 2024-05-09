/*
 * Copyright 2022-2024 Ponfee (http://www.ponfee.cn/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.ponfee.disjob.core.handle;

import cn.ponfee.disjob.common.dag.DAGExpressionParser;
import cn.ponfee.disjob.common.dag.DAGNode;
import cn.ponfee.disjob.common.spring.SpringContextHolder;
import cn.ponfee.disjob.common.util.ClassUtils;
import cn.ponfee.disjob.common.util.Predicates;
import cn.ponfee.disjob.core.base.JobCodeMsg;
import cn.ponfee.disjob.core.enums.JobType;
import cn.ponfee.disjob.core.enums.RouteStrategy;
import cn.ponfee.disjob.core.exception.JobException;
import cn.ponfee.disjob.core.exception.JobRuntimeException;
import cn.ponfee.disjob.core.param.worker.JobHandlerParam;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.util.Assert;

import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.ponfee.disjob.core.base.JobCodeMsg.INVALID_JOB_HANDLER;
import static cn.ponfee.disjob.core.base.JobCodeMsg.SPLIT_JOB_FAILED;

/**
 * Job handler utility
 *
 * @author Ponfee
 */
public class JobHandlerUtils {

    public static void verify(JobHandlerParam param) throws JobException {
        Assert.hasText(param.getJobHandler(), "Job handler cannot be blank.");
        Set<String> jobHandlers;
        if (param.getJobType() == JobType.WORKFLOW) {
            jobHandlers = new DAGExpressionParser(param.getJobHandler())
                .parse()
                .nodes()
                .stream()
                .filter(Predicates.not(DAGNode::isStartOrEnd))
                .map(DAGNode::getName)
                .collect(Collectors.toSet());
            Assert.notEmpty(jobHandlers, () -> "Invalid workflow job handler: " + param.getJobHandler());
        } else {
            jobHandlers = Collections.singleton(param.getJobHandler());
        }

        try {
            for (String jobHandler : jobHandlers) {
                if (param.getRouteStrategy() == RouteStrategy.BROADCAST) {
                    JobHandler handler = load(jobHandler);
                    Assert.isTrue(handler instanceof BroadcastJobHandler, () -> "Not a broadcast job handler: " + jobHandler);
                } else {
                    param.setJobHandler(jobHandler);
                    Assert.notEmpty(split(param), () -> "Not split any task: " + jobHandler);
                }
            }
        } catch (JobException | JobRuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new JobException(INVALID_JOB_HANDLER, e.getMessage());
        }
    }

    /**
     * Splits job to many sched task.
     *
     * @param param the job handler parameter
     * @return list of SplitTask
     * @throws JobException if split failed
     */
    public static List<SplitTask> split(JobHandlerParam param) throws JobException {
        try {
            JobSplitter jobSplitter = load(param.getJobHandler());
            List<SplitTask> splitTasks = jobSplitter.split(param.getJobParam());
            if (CollectionUtils.isEmpty(splitTasks)) {
                throw new JobException(SPLIT_JOB_FAILED, "Job split none tasks.");
            }
            return splitTasks;
        } catch (JobException | JobRuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw new JobException(SPLIT_JOB_FAILED, "Split job occur error", t);
        }
    }

    /**
     * Load jobHandler instance, String parameter can be spring bean name or qualified class name or source code
     *
     * @param text spring bean name or qualified class name or source code
     * @return JobHandler instance object
     * @throws JobException if new instance failed
     */
    public static JobHandler load(String text) throws JobException {
        if (SpringContextHolder.applicationContext() != null) {
            // must be annotated with @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
            // get by spring bean name
            JobHandler handler = SpringContextHolder.getPrototypeBean(text, JobHandler.class);
            if (handler != null) {
                return handler;
            }

            Class<? extends JobHandler> jobHandlerClass = getJobHandlerClass(text);
            handler = SpringContextHolder.getPrototypeBean(jobHandlerClass);
            if (handler != null) {
                return handler;
            }

            handler = ClassUtils.newInstance(jobHandlerClass);
            SpringContextHolder.autowireBean(handler);
            return handler;
        } else {
            Class<? extends JobHandler> jobHandlerClass = getJobHandlerClass(text);
            return ClassUtils.newInstance(jobHandlerClass);
        }
    }

    private static Class<? extends JobHandler> getJobHandlerClass(String text) throws JobException {
        Class<? extends JobHandler> type = ClassUtils.getClass(text);
        if (type == null) {
            throw new JobException(JobCodeMsg.LOAD_HANDLER_ERROR, "Illegal job handler class: " + text);
        }

        // interface type: Modifier.isAbstract(type.getModifiers()) -> true
        if (!JobHandler.class.isAssignableFrom(type) || Modifier.isAbstract(type.getModifiers())) {
            throw new JobException(JobCodeMsg.LOAD_HANDLER_ERROR, "Invalid job handler '" + type + "': " + text);
        }
        return type;
    }

}
