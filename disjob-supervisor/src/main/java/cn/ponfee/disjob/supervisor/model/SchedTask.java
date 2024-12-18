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

package cn.ponfee.disjob.supervisor.model;

import cn.ponfee.disjob.common.model.BaseEntity;
import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.core.enums.ExecuteState;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.beans.Transient;
import java.util.Comparator;
import java.util.Date;

/**
 * The schedule task entity, mapped database table sched_task
 *
 * @author Ponfee
 */
@Getter
@Setter
public class SchedTask extends BaseEntity {
    private static final long serialVersionUID = 4882055618593707631L;

    public static final Comparator<SchedTask> TASK_NO_COMPARATOR = Comparator.comparing(SchedTask::getTaskNo);

    /**
     * 全局唯一ID
     */
    private Long taskId;

    /**
     * sched_instance.instance_id
     *
     * @see SchedInstance#getInstanceId()
     */
    private Long instanceId;

    /**
     * 当前任务序号(从1开始)
     */
    private Integer taskNo;

    /**
     * 任务总数量
     */
    private Integer taskCount;

    /**
     * job_executor执行task的参数(参考sched_job.job_param)
     */
    private String taskParam;

    /**
     * 执行开始时间
     */
    private Date executeStartTime;

    /**
     * 执行结束时间
     */
    private Date executeEndTime;

    /**
     * 执行状态：10-等待执行；20-正在执行；30-暂停执行；40-执行完成；50-派发失败；51-初始化异常；52-执行失败；53-执行异常；54-执行超时；55-执行冲突；56-广播终止；57-执行终止；58-关机取消；59-手动取消；
     *
     * @see ExecuteState
     */
    private Integer executeState;

    /**
     * 保存的执行快照数据
     */
    private String executeSnapshot;

    /**
     * 工作进程(JVM进程，GROUP:WORKER-ID:HOST:PORT)
     */
    private String worker;

    /**
     * 任务派发失败的次数(失败次数达到阈值后需要终止)
     */
    private Integer dispatchFailedCount;

    /**
     * 执行错误信息
     */
    private String errorMsg;

    /**
     * Creates sched tasks
     *
     * @param taskParam  the task param
     * @param taskId     the task id
     * @param instanceId the instance id
     * @param taskNo     the task no
     * @param taskCount  the task count
     * @param worker     the worker
     * @return SchedTask
     */
    public static SchedTask of(String taskParam, long taskId, long instanceId,
                               int taskNo, int taskCount, String worker) {
        SchedTask task = new SchedTask();
        task.setTaskParam(taskParam == null ? "" : taskParam);
        task.setTaskId(taskId);
        task.setInstanceId(instanceId);
        task.setTaskNo(taskNo);
        task.setTaskCount(taskCount);
        task.setWorker(worker);
        task.setExecuteState(ExecuteState.WAITING.value());
        return task;
    }

    public Worker worker() {
        return StringUtils.isBlank(worker) ? null : Worker.deserialize(worker);
    }

    @Transient
    public boolean isWaiting() {
        return ExecuteState.WAITING.equalsValue(executeState);
    }

    @Transient
    public boolean isExecuting() {
        return ExecuteState.EXECUTING.equalsValue(executeState);
    }

    @Transient
    public boolean isPausable() {
        return ExecuteState.of(executeState).isPausable();
    }

    @Transient
    public boolean isTerminal() {
        return ExecuteState.of(executeState).isTerminal();
    }

    @Transient
    public boolean isFailure() {
        return ExecuteState.of(executeState).isFailure();
    }

}
