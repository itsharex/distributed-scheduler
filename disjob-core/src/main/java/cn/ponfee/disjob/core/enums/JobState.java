/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.core.enums;

import cn.ponfee.disjob.common.base.IntValueEnum;
import org.springframework.util.Assert;

/**
 * The job state enum definition.
 * <p>mapped by sched_job.job_state
 *
 * @author Ponfee
 */
public enum JobState implements IntValueEnum<JobState> {

    /**
     * 已禁用
     */
    DISABLE(0),

    /**
     * 已启用
     */
    ENABLE(1),

    ;

    private final int value;

    JobState(int value) {
        this.value = value;
    }

    @Override
    public int value() {
        return value;
    }

    public static JobState of(Integer value) {
        Assert.notNull(value, "Job state value cannot be null.");
        for (JobState state : JobState.values()) {
            if (state.value == value) {
                return state;
            }
        }
        throw new IllegalArgumentException("Invalid job state value: " + value);
    }

}