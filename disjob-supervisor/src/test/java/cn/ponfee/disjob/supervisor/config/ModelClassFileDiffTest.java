/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.config;

import cn.ponfee.disjob.common.util.ClassUtils;
import cn.ponfee.disjob.core.model.SchedInstance;
import cn.ponfee.disjob.core.model.SchedJob;
import cn.ponfee.disjob.core.model.SchedTask;
import cn.ponfee.disjob.supervisor.web.request.UpdateSchedJobRequest;
import cn.ponfee.disjob.supervisor.web.response.SchedInstanceResponse;
import cn.ponfee.disjob.supervisor.web.response.SchedJobResponse;
import cn.ponfee.disjob.supervisor.web.response.SchedTaskResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Objects;

/**
 * ModelClassFileDiffTest
 *
 * @author Ponfee
 */
public class ModelClassFileDiffTest {

    @Test
    public void test() {
        Assertions.assertTrue(Objects.equals(null, null));
        System.out.println(ClassUtils.fieldDiff(SchedJob.class, SchedJobResponse.class));
        System.out.println(ClassUtils.fieldDiff(SchedJob.class, UpdateSchedJobRequest.class));
        System.out.println(ClassUtils.fieldDiff(SchedInstance.class, SchedInstanceResponse.class));
        System.out.println(ClassUtils.fieldDiff(SchedTask.class, SchedTaskResponse.class));
    }
}
