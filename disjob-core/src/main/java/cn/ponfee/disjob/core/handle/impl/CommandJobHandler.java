/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.core.handle.impl;

import cn.ponfee.disjob.common.model.Result;
import cn.ponfee.disjob.common.util.Files;
import cn.ponfee.disjob.common.util.Jsons;
import cn.ponfee.disjob.core.handle.Checkpoint;
import cn.ponfee.disjob.core.handle.JobHandler;
import cn.ponfee.disjob.core.model.SchedTask;
import cn.ponfee.disjob.core.util.ProcessUtils;
import lombok.Data;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.nio.charset.Charset;

/**
 * <pre>
 *  The job handler for executes system operation command.
 *
 *  /bin/bash -c "echo $(date +%Y/%m/%d)"
 *  Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", "echo $(date +%Y/%m/%d)"});
 *
 *  bin/bash -c 后面接 命令
 *  /bin/bash 后面接 执行的脚本
 * </pre>
 *
 * @author Ponfee
 */
public class CommandJobHandler extends JobHandler<String> {

    @Override
    public Result<String> execute(Checkpoint checkpoint) throws Exception {
        final SchedTask task = task();
        Assert.hasText(task.getTaskParam(), "Command param cannot be empty.");
        CommandParam commandParam = Jsons.fromJson(task.getTaskParam(), CommandParam.class);
        Assert.notEmpty(commandParam.cmdarray, "Command array cannot be empty.");
        Charset charset = Files.charset(commandParam.charset);

        Process process = Runtime.getRuntime().exec(commandParam.cmdarray, commandParam.envp);
        return ProcessUtils.complete(process, charset, task, log);
    }

    @Data
    public static class CommandParam implements Serializable {
        private static final long serialVersionUID = 2079640617453920047L;

        private String[] cmdarray;
        private String[] envp;
        private String charset;
    }

}
