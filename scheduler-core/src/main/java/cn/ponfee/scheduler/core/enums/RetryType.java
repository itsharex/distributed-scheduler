package cn.ponfee.scheduler.core.enums;

import cn.ponfee.scheduler.common.util.Enums;

import java.util.Map;

/**
 * The retry type enum definition.
 * <p>mapped by sched_job.retry_type
 *
 * @author Ponfee
 */
public enum RetryType {

    /**
     * 不重试
     */
    NONE(0),
    
    /**
     * 重试所有的Task(re-split job param to task param)
     */
    ALL(1),

    /**
     * 只重试失败的Task(copy previous failed task param)
     */
    FAILED(2),

    ;

    private static final Map<Integer, RetryType> MAPPING = Enums.toMap(RetryType.class, RetryType::value);

    private final int value;

    RetryType(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    public boolean equals(Integer value) {
        return value != null && this.value == value;
    }

    public static RetryType of(Integer value) {
        if (value == null) {
            throw new IllegalArgumentException("Retry type cannot be null.");
        }
        RetryType runType = MAPPING.get(value);
        if (runType == null) {
            throw new IllegalArgumentException("Invalid retry type: " + value);
        }
        return runType;
    }

}
