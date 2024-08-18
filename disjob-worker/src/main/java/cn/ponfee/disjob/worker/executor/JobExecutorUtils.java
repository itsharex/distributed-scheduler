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

package cn.ponfee.disjob.worker.executor;

import cn.ponfee.disjob.common.concurrent.Threads;
import cn.ponfee.disjob.common.dag.DAGExpressionParser;
import cn.ponfee.disjob.common.dag.DAGNode;
import cn.ponfee.disjob.common.exception.Throwables;
import cn.ponfee.disjob.common.spring.SpringContextHolder;
import cn.ponfee.disjob.common.util.ClassUtils;
import cn.ponfee.disjob.common.util.Predicates;
import cn.ponfee.disjob.common.util.ProcessUtils;
import cn.ponfee.disjob.core.base.JobCodeMsg;
import cn.ponfee.disjob.core.dto.worker.VerifyJobParam;
import cn.ponfee.disjob.core.enums.JobType;
import cn.ponfee.disjob.core.exception.JobException;
import cn.ponfee.disjob.core.exception.JobRuntimeException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.springframework.util.Assert;

import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Job executor utility
 *
 * @author Ponfee
 */
public class JobExecutorUtils {

    public static void verify(VerifyJobParam param) throws JobException {
        Assert.hasText(param.getJobExecutor(), "Job executor cannot be blank.");
        Set<String> jobExecutors;
        if (param.getJobType() == JobType.WORKFLOW) {
            jobExecutors = DAGExpressionParser.parse(param.getJobExecutor())
                .nodes()
                .stream()
                .filter(Predicates.not(DAGNode::isStartOrEnd))
                .map(DAGNode::getName)
                .collect(Collectors.toSet());
            Assert.notEmpty(jobExecutors, () -> "Invalid workflow job executor: " + param.getJobExecutor());
        } else {
            jobExecutors = Collections.singleton(param.getJobExecutor());
        }

        try {
            for (String jobExecutor : jobExecutors) {
                if (param.getRouteStrategy().isBroadcast()) {
                    JobExecutor executor = load(jobExecutor);
                    Assert.isTrue(executor instanceof BroadcastJobExecutor, () -> "Not broadcast job executor: " + jobExecutor);
                } else {
                    param.setJobExecutor(jobExecutor);
                    split(param.getJobExecutor(), param.getJobParam());
                }
            }
        } catch (JobException | JobRuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new JobException(JobCodeMsg.INVALID_JOB_EXECUTOR, e.getMessage());
        }
    }

    /**
     * Splits job to many sched task.
     *
     * @param jobExecutor the job executor
     * @param jobParam    the job param
     * @return list of task param
     * @throws JobException if split failed
     */
    public static List<String> split(String jobExecutor, String jobParam) throws JobException {
        try {
            JobSplitter jobSplitter = load(jobExecutor);
            List<String> taskParams = jobSplitter.split(jobParam);
            if (CollectionUtils.isEmpty(taskParams)) {
                throw new JobException(JobCodeMsg.SPLIT_JOB_FAILED, "Job split none tasks.");
            }
            return taskParams;
        } catch (JobException | JobRuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw new JobException(JobCodeMsg.SPLIT_JOB_FAILED, "Split job occur error", t);
        }
    }

    /**
     * Load JobExecutor instance, String parameter can be spring bean name or qualified class name or source code
     *
     * @param text spring bean name or qualified class name or source code
     * @return JobExecutor instance object
     * @throws JobException if new instance failed
     */
    public static JobExecutor load(String text) throws JobException {
        if (SpringContextHolder.isNotNull()) {
            // must be annotated with @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
            // get by spring bean name
            JobExecutor executor = SpringContextHolder.getPrototypeBean(text, JobExecutor.class);
            if (executor != null) {
                return executor;
            }

            Class<? extends JobExecutor> jobExecutorClass = getJobExecutorClass(text);
            executor = SpringContextHolder.getPrototypeBean(jobExecutorClass);
            if (executor != null) {
                return executor;
            }

            executor = ClassUtils.newInstance(jobExecutorClass);
            SpringContextHolder.autowireBean(executor);
            return executor;
        } else {
            Class<? extends JobExecutor> jobExecutorClass = getJobExecutorClass(text);
            return ClassUtils.newInstance(jobExecutorClass);
        }
    }

    public static ExecutionResult completeProcess(Process process, Charset charset, ExecutionTask task, Logger log) {
        try (InputStream is = process.getInputStream(); InputStream es = process.getErrorStream()) {
            // 一次性获取全部执行结果信息：不是在控制台实时展示执行信息，所以此处不用通过异步线程去获取命令的实时执行信息
            String verbose = IOUtils.toString(is, charset);
            String error = IOUtils.toString(es, charset);
            int code = process.waitFor();
            if (code == ProcessUtils.SUCCESS_CODE) {
                return ExecutionResult.success(verbose);
            } else {
                return ExecutionResult.failure(JobCodeMsg.JOB_EXECUTE_FAILED.getCode(), code + ": " + error);
            }
        } catch (Throwable t) {
            log.error("Process execute error: " + task, t);
            Threads.interruptIfNecessary(t);
            return ExecutionResult.failure(JobCodeMsg.JOB_EXECUTE_ERROR.getCode(), Throwables.getRootCauseMessage(t));
        } finally {
            ProcessUtils.destroy(process);
        }
    }

    private static Class<? extends JobExecutor> getJobExecutorClass(String text) throws JobException {
        Class<? extends JobExecutor> type = ClassUtils.getClass(text);
        if (type == null) {
            throw new JobException(JobCodeMsg.LOAD_JOB_EXECUTOR_ERROR, "Illegal job executor class: " + text);
        }

        // interface type: Modifier.isAbstract(type.getModifiers()) -> true
        if (!JobExecutor.class.isAssignableFrom(type) || Modifier.isAbstract(type.getModifiers())) {
            throw new JobException(JobCodeMsg.LOAD_JOB_EXECUTOR_ERROR, "Invalid job executor '" + type + "': " + text);
        }
        return type;
    }

}