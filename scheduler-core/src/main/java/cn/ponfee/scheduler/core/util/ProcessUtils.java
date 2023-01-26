/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.core.util;

import cn.ponfee.scheduler.common.base.exception.Throwables;
import cn.ponfee.scheduler.common.base.model.Result;
import cn.ponfee.scheduler.core.base.JobCodeMsg;
import cn.ponfee.scheduler.core.model.SchedTask;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;

/**
 * Process execute utility.
 *
 * @author Ponfee
 */
public class ProcessUtils {

    public static Result<String> complete(Process process, Charset charset, SchedTask task, Logger log) {
        try (InputStream is = process.getInputStream(); InputStream es = process.getErrorStream()) {
            // 一欠性获取全部执行结果信息：不是在控制台实时展示执行信息，所以此处不用通过异步线程去获取命令的实时执行信息
            String verbose = IOUtils.toString(is, charset);
            String error = IOUtils.toString(es, charset);
            int code = process.waitFor();
            if (code == 0) {
                log.info("Execute success: {} | {}", task.getTaskId(), verbose);
                return Result.success(verbose);
            } else {
                log.error("Execute failed: {} | {} | {} | {}", task, code, verbose, error);
                return Result.failure(JobCodeMsg.JOB_EXECUTE_FAILED.getCode(), "Execute failed: code=" + code + ", error=" + error);
            }
        } catch (Exception e) {
            log.error("Execute error: " + task, e);
            return Result.failure(JobCodeMsg.JOB_EXECUTE_FAILED.getCode(), "Execute error: " + Throwables.getRootCauseMessage(e));
        }
    }

    public static int progress(Process process, Charset charset, Logger log) {
        return progress(process, charset, log::info, log::error);
    }

    public static int progress(Process process, Charset charset, Consumer<String> verbose, Consumer<String> error) {
        // 控制台实时展示
        ForkJoinPool.commonPool().execute(() -> read(process.getInputStream(), charset, verbose));
        ForkJoinPool.commonPool().execute(() -> read(process.getErrorStream(), charset, error));
        try {
            return process.waitFor();
        } catch (InterruptedException e) {
            error.accept("Execute error: " + ExceptionUtils.getStackTrace(e));
            Thread.currentThread().interrupt();
            return -1;
        }
    }

    private static void read(InputStream input, Charset charset, Consumer<String> consumer) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, charset))) {
            while (!Thread.currentThread().isInterrupted()) {
                String line = reader.readLine();
                if (line == null) {
                    return;
                } else {
                    consumer.accept(line);
                }
            }
        } catch (IOException e) {
            consumer.accept("Read output error: " + ExceptionUtils.getStackTrace(e));
        }
    }

}