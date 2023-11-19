/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.dao.mapper;

import cn.ponfee.disjob.core.model.SchedGroup;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Mybatis mapper of sched_Group database table.
 *
 * @author Ponfee
 */
public interface SchedGroupMapper {

    int insert(SchedGroup schedGroup);

    List<SchedGroup> findAll();

    int updateWorkerToken(@Param("group") String group,
                          @Param("newWorkerToken") String newWorkerToken,
                          @Param("oldWorkerToken") String oldWorkerToken);

    int updateSupervisorToken(@Param("group") String group,
                              @Param("newSupervisorToken") String newSupervisorToken,
                              @Param("oldSupervisorToken") String oldSupervisorToken);

    int updateAlarmConfig(SchedGroup schedGroup);

    int delete(String group);

}