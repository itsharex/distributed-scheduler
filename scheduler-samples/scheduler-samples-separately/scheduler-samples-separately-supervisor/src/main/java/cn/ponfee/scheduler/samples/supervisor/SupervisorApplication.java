/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.samples.supervisor;

import cn.ponfee.scheduler.samples.common.AbstractSchedulerSamplesApplication;
import cn.ponfee.scheduler.samples.common.util.Constants;
import cn.ponfee.scheduler.supervisor.configuration.EnableSupervisor;
import org.springframework.boot.SpringApplication;

/**
 * Supervisor application based spring boot
 *
 * @author Ponfee
 */
@EnableSupervisor
public class SupervisorApplication extends AbstractSchedulerSamplesApplication {

    static {
        System.setProperty(Constants.APP_NAME, "scheduler-samples-separately-supervisor");
    }

    public static void main(String[] args) {
        SpringApplication.run(SupervisorApplication.class, args);
    }

}
