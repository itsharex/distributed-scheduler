/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.supervisor.thread;

import cn.ponfee.scheduler.common.lock.DoInLocked;
import cn.ponfee.scheduler.common.util.Collects;
import cn.ponfee.scheduler.core.base.AbstractHeartbeatThread;
import cn.ponfee.scheduler.core.enums.ExecuteState;
import cn.ponfee.scheduler.core.enums.RunState;
import cn.ponfee.scheduler.core.model.SchedInstance;
import cn.ponfee.scheduler.core.model.SchedJob;
import cn.ponfee.scheduler.core.model.SchedTask;
import cn.ponfee.scheduler.supervisor.manager.SchedulerJobManager;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Date;
import java.util.List;

import static cn.ponfee.scheduler.core.base.JobConstants.PROCESS_BATCH_SIZE;

/**
 * Scan running a long time, but still is running state sched_instance record.
 *
 * @author Ponfee
 */
public class RunningInstanceScanner extends AbstractHeartbeatThread {

    private final DoInLocked doInLocked;
    private final SchedulerJobManager schedulerJobManager;
    private final long beforeMilliseconds;

    public RunningInstanceScanner(long heartbeatPeriodMilliseconds,
                                  DoInLocked doInLocked,
                                  SchedulerJobManager schedulerJobManager) {
        super(heartbeatPeriodMilliseconds);
        this.doInLocked = doInLocked;
        this.schedulerJobManager = schedulerJobManager;
        this.beforeMilliseconds = (heartbeatPeriodMs << 3); // 30s * 8 = 240s
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

    private boolean process() {
        Date now = new Date(), expireTime = new Date(now.getTime() - beforeMilliseconds);
        List<SchedInstance> instances = schedulerJobManager.findExpireRunning(expireTime, PROCESS_BATCH_SIZE);
        if (CollectionUtils.isEmpty(instances)) {
            return true;
        }

        for (SchedInstance instance : instances) {
            processEach(instance, now);
        }

        return instances.size() < PROCESS_BATCH_SIZE;
    }

    private void processEach(SchedInstance instance, Date now) {
        if (!schedulerJobManager.renewUpdateTime(instance, now)) {
            return;
        }

        List<SchedTask> tasks = schedulerJobManager.findMediumTaskByInstanceId(instance.getInstanceId());
        List<SchedTask> waitingTasks = Collects.filter(tasks, e -> ExecuteState.WAITING.equals(e.getExecuteState()));

        if (CollectionUtils.isNotEmpty(waitingTasks)) {
            // 1、has waiting task

            // sieve the (un-dispatch) or (assigned worker death) waiting tasks to do re-dispatch
            List<SchedTask> redispatchingTasks = Collects.filter(waitingTasks, e -> schedulerJobManager.isDeathWorker(e.getWorker()));
            if (CollectionUtils.isEmpty(redispatchingTasks)) {
                return;
            }
            SchedJob schedJob = schedulerJobManager.getJob(instance.getJobId());
            if (schedJob == null) {
                log.error("Scanned running state instance not found job: {}", instance.getJobId());
                return;
            }
            // check is whether not discovered worker
            if (schedulerJobManager.hasNotDiscoveredWorkers(schedJob.getJobGroup())) {
                log.error("Scanned running state instance not available worker: {} | {}", instance.getInstanceId(), schedJob.getJobGroup());
                return;
            }
            log.info("Scanned running state instance re-dispatch task: {}", instance.getInstanceId());
            schedulerJobManager.dispatch(schedJob, instance, redispatchingTasks);

        } else if (tasks.stream().allMatch(e -> ExecuteState.of(e.getExecuteState()).isTerminal())) {
            // 2、all task was terminated

            // double check instance run state
            SchedInstance reloadInstance = schedulerJobManager.getInstance(instance.getInstanceId());
            if (reloadInstance == null) {
                log.error("Scanned running state instance not exists: {}", instance.getInstanceId());
                return;
            }
            if (RunState.of(reloadInstance.getRunState()).isTerminal()) {
                return;
            }
            log.info("Scanned running state instance task all terminated: {}", instance.getInstanceId());
            schedulerJobManager.deathInstance(instance.getInstanceId());

        } else {
            // 3、has executing state task

            // check has alive executing state task
            if (schedulerJobManager.hasAliveExecuting(tasks)) {
                return;
            }
            log.info("Scanned running state instance was death: {}", instance.getInstanceId());
            schedulerJobManager.deathInstance(instance.getInstanceId());

        }
    }

}
