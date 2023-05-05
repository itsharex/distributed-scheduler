/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.samples.supervisor;

import cn.ponfee.disjob.samples.common.AbstractSamplesApplication;
import cn.ponfee.disjob.samples.common.util.SampleConstants;
import cn.ponfee.disjob.supervisor.configuration.EnableSupervisor;
import org.springframework.boot.SpringApplication;

/**
 * Supervisor application based spring boot
 *
 * @author Ponfee
 */
@EnableSupervisor
public class SupervisorApplication extends AbstractSamplesApplication {

    static {
        // for log4j log file dir
        System.setProperty(SampleConstants.APP_NAME, "separately-supervisor");
    }

    public static void main(String[] args) {
        SpringApplication.run(SupervisorApplication.class, args);
    }

}