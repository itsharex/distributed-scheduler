/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.dispatch;

import cn.ponfee.disjob.common.base.Startable;
import cn.ponfee.disjob.common.base.TimingWheel;
import cn.ponfee.disjob.core.param.ExecuteTaskParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Worker receive dispatched task from supervisor.
 *
 * @author Ponfee
 */
public abstract class TaskReceiver implements Startable {
    private final static Logger LOG = LoggerFactory.getLogger(TaskReceiver.class);

    private final TimingWheel<ExecuteTaskParam> timingWheel;

    public TaskReceiver(TimingWheel<ExecuteTaskParam> timingWheel) {
        this.timingWheel = Objects.requireNonNull(timingWheel, "Timing wheel cannot be null.");
    }

    /**
     * Receives the supervisor dispatched tasks.
     *
     * @param param the execution task param
     */
    public boolean receive(ExecuteTaskParam param) {
        if (param == null) {
            LOG.error("Received task cannot be null.");
            return false;
        }

        boolean res = timingWheel.offer(param);
        if (res) {
            LOG.info("Received task success {} | {} | {}", param.getTaskId(), param.getOperation(), param.getWorker());
        } else {
            LOG.error("Received task failed " + param);
        }
        return res;
    }

    /**
     * Start do receive
     */
    @Override
    public void start() {
        // No-op
    }

    /**
     * Close resources if necessary.
     */
    @Override
    public void stop() {
        // No-op
    }

    public final TimingWheel<ExecuteTaskParam> getTimingWheel() {
        return timingWheel;
    }

}
