/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.core.enums;

import cn.ponfee.disjob.common.base.IntValueEnum;
import cn.ponfee.disjob.common.util.Enums;

import java.util.Map;
import java.util.Objects;

/**
 * The collision strategy enum definition.
 * <p>mapped by sched_job.collision_strategy
 *
 * @author Ponfee
 */
public enum CollisionStrategy implements IntValueEnum<CollisionStrategy> {

    /**
     * 并行
     */
    CONCURRENT(1),

    /**
     * 串行
     */
    SERIAL(2),

    /**
     * 覆盖上一次（取消上一次任务，并执行当前任务）
     */
    OVERRIDE(3),

    /**
     * 丢弃当前任务
     */
    DISCARD(4),

    ;

    private static final Map<Integer, CollisionStrategy> MAPPING = Enums.toMap(CollisionStrategy.class, CollisionStrategy::value);

    private final int value;

    CollisionStrategy(int value) {
        this.value = value;
    }

    @Override
    public int value() {
        return value;
    }

    public static CollisionStrategy of(Integer value) {
        return Objects.requireNonNull(MAPPING.get(value), () -> "Invalid collision strategy value: " + value);
    }

}