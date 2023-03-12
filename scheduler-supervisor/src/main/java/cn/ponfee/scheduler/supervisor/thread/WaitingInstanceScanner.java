/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.supervisor.thread;

import cn.ponfee.scheduler.common.lock.DoInLocked;
import cn.ponfee.scheduler.core.base.AbstractHeartbeatThread;
import cn.ponfee.scheduler.core.enums.ExecuteState;
import cn.ponfee.scheduler.core.model.SchedInstance;
import cn.ponfee.scheduler.core.model.SchedJob;
import cn.ponfee.scheduler.core.model.SchedTask;
import cn.ponfee.scheduler.supervisor.manager.SchedulerJobManager;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static cn.ponfee.scheduler.core.base.JobConstants.PROCESS_BATCH_SIZE;

/**
 * Scan exceed the trigger time, but still is waiting state sched_instance record.
 *
 * @author Ponfee
 */
public class WaitingInstanceScanner extends AbstractHeartbeatThread {

    private final DoInLocked doInLocked;
    private final SchedulerJobManager schedulerJobManager;
    private final long beforeMilliseconds;

    public WaitingInstanceScanner(long heartbeatPeriodMilliseconds,
                                  DoInLocked doInLocked,
                                  SchedulerJobManager schedulerJobManager) {
        super(heartbeatPeriodMilliseconds);
        this.doInLocked = doInLocked;
        this.schedulerJobManager = schedulerJobManager;
        this.beforeMilliseconds = (heartbeatPeriodMs << 3); // 15s * 8 = 120s
    }

    @Override
    protected boolean heartbeat() {
        if (schedulerJobManager.hasNotDiscoveredWorkers()) {
            log.warn("Not found available worker.");
            return true;
        }

        Boolean result = doInLocked.apply(this::process);
        return result == null || result;
    }

    // -------------------------------------------------------------process expire waiting sched instance

    private boolean process() {
        Date now = new Date(), expireTime = new Date(now.getTime() - beforeMilliseconds);
        List<SchedInstance> instances = schedulerJobManager.findExpireWaiting(expireTime, PROCESS_BATCH_SIZE);
        if (CollectionUtils.isEmpty(instances)) {
            return true;
        }

        for (SchedInstance instance : instances) {
            processEach(instance, now);
        }
        return instances.size() < PROCESS_BATCH_SIZE;
    }

    private void processEach(SchedInstance instance, Date now) {
        List<SchedTask> tasks = schedulerJobManager.findMediumTaskByInstanceId(instance.getInstanceId());

        if (tasks.stream().allMatch(t -> ExecuteState.of(t.getExecuteState()).isTerminal())) {
            // if all the tasks are terminal, then terminate sched instance record
            if (schedulerJobManager.renewUpdateTime(instance, now)) {
                log.info("All task terminal, terminate the sched instance: {}", instance.getInstanceId());
                schedulerJobManager.terminateDeadInstance(instance.getInstanceId());
            }
            return;
        }

        // sieve the waiting tasks
        List<SchedTask> waitingTasks = tasks.stream()
            .filter(e -> ExecuteState.WAITING.equals(e.getExecuteState()))
            .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(waitingTasks)) {
            return;
        }

        // filter dispatching task
        List<SchedTask> dispatchingTasks = schedulerJobManager.filterDispatchingTask(waitingTasks);
        if (CollectionUtils.isEmpty(dispatchingTasks)) {
            // none un-dispatched task
            schedulerJobManager.renewUpdateTime(instance, now);
            return;
        }

        SchedJob schedJob = schedulerJobManager.getJob(instance.getJobId());
        if (schedJob == null) {
            log.error("Waiting instance scanner sched job not found: {}", instance.getJobId());
            schedulerJobManager.updateInstanceState(ExecuteState.DATA_INVALID, tasks, instance);
            return;
        }

        // check not found worker
        if (schedulerJobManager.hasNotDiscoveredWorkers(schedJob.getJobGroup())) {
            schedulerJobManager.renewUpdateTime(instance, now);
            log.warn("Scan instance not found available group '{}' workers.", schedJob.getJobGroup());
            return;
        }

        if (schedulerJobManager.renewUpdateTime(instance, now)) {
            log.info("Waiting scanner redispatch sched instance: {}", instance);
            schedulerJobManager.dispatch(schedJob, instance, dispatchingTasks);
        }
    }

}
