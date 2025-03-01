/*
 * Copyright 2022-2024 Ponfee (http://www.ponfee.cn/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.ponfee.disjob.supervisor;

import cn.ponfee.disjob.common.base.SingletonClassConstraint;
import cn.ponfee.disjob.common.base.Startable;
import cn.ponfee.disjob.common.concurrent.TripState;
import cn.ponfee.disjob.common.exception.Throwables.ThrowingRunnable;
import cn.ponfee.disjob.common.lock.LockTemplate;
import cn.ponfee.disjob.core.base.JobConstants;
import cn.ponfee.disjob.core.base.Supervisor;
import cn.ponfee.disjob.dispatch.TaskDispatcher;
import cn.ponfee.disjob.registry.SupervisorRegistry;
import cn.ponfee.disjob.supervisor.component.JobManager;
import cn.ponfee.disjob.supervisor.component.JobQuerier;
import cn.ponfee.disjob.supervisor.component.WorkerClient;
import cn.ponfee.disjob.supervisor.configuration.SupervisorProperties;
import cn.ponfee.disjob.supervisor.scanner.RunningInstanceScanner;
import cn.ponfee.disjob.supervisor.scanner.TriggeringJobScanner;
import cn.ponfee.disjob.supervisor.scanner.WaitingInstanceScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Startup supervisor.
 *
 * @author Ponfee
 */
public class SupervisorStartup extends SingletonClassConstraint implements Startable {

    private static final Logger LOG = LoggerFactory.getLogger(SupervisorStartup.class);

    private final Supervisor.Local localSupervisor;
    private final SupervisorRegistry supervisorRegistry;
    private final TaskDispatcher taskDispatcher;
    private final WaitingInstanceScanner waitingInstanceScanner;
    private final RunningInstanceScanner runningInstanceScanner;
    private final TriggeringJobScanner triggeringJobScanner;
    private final TripState state = TripState.create();

    public SupervisorStartup(Supervisor.Local localSupervisor,
                             SupervisorProperties supervisorConf,
                             SupervisorRegistry supervisorRegistry,
                             WorkerClient workerClient,
                             JobManager jobManager,
                             JobQuerier jobQuerier,
                             TaskDispatcher taskDispatcher,
                             LockTemplate scanWaitingInstanceLocker,
                             LockTemplate scanRunningInstanceLocker,
                             LockTemplate scanTriggeringJobLocker) {
        Objects.requireNonNull(localSupervisor, "Local supervisor cannot null.");
        Objects.requireNonNull(supervisorConf, "Supervisor config cannot null.").check();
        Objects.requireNonNull(supervisorRegistry, "Supervisor registry cannot null.");
        Objects.requireNonNull(workerClient, "Worker client cannot null.");
        Objects.requireNonNull(jobManager, "Job manager cannot null.");
        Objects.requireNonNull(jobQuerier, "Job querier cannot null.");
        Objects.requireNonNull(taskDispatcher, "Task dispatcher cannot null.");
        Objects.requireNonNull(scanWaitingInstanceLocker, "Scan waiting instance locker cannot null.");
        Objects.requireNonNull(scanRunningInstanceLocker, "Scan running instance locker cannot null.");
        Objects.requireNonNull(scanTriggeringJobLocker, "Scan triggering job locker cannot null.");

        this.localSupervisor = localSupervisor;
        this.supervisorRegistry = supervisorRegistry;
        this.taskDispatcher = taskDispatcher;
        this.waitingInstanceScanner = new WaitingInstanceScanner(supervisorConf, jobManager, jobQuerier, workerClient, scanWaitingInstanceLocker);
        this.runningInstanceScanner = new RunningInstanceScanner(supervisorConf, jobManager, jobQuerier, workerClient, scanRunningInstanceLocker);
        this.triggeringJobScanner   = new TriggeringJobScanner  (supervisorConf, jobManager, jobQuerier, workerClient, scanTriggeringJobLocker);
    }

    @Override
    public void start() {
        if (!state.start()) {
            LOG.warn("Supervisor already started.");
            return;
        }

        LOG.info("Supervisor start begin: {}", localSupervisor);
        waitingInstanceScanner.start();
        runningInstanceScanner.start();
        triggeringJobScanner.start();
        ThrowingRunnable.doCaught(supervisorRegistry::discoverServers);
        supervisorRegistry.register(localSupervisor);
        printBanner();
        LOG.info("Supervisor start end: {}", localSupervisor);
    }

    @Override
    public void stop() {
        if (!state.stop()) {
            LOG.warn("Supervisor already Stopped.");
            return;
        }

        LOG.info("Supervisor stop begin: {}", localSupervisor);
        ThrowingRunnable.doCaught(supervisorRegistry::close);
        ThrowingRunnable.doCaught(triggeringJobScanner::toStop);
        ThrowingRunnable.doCaught(runningInstanceScanner::toStop);
        ThrowingRunnable.doCaught(waitingInstanceScanner::toStop);
        ThrowingRunnable.doCaught(taskDispatcher::close);
        ThrowingRunnable.doCaught(triggeringJobScanner::close);
        ThrowingRunnable.doCaught(runningInstanceScanner::close);
        ThrowingRunnable.doCaught(waitingInstanceScanner::close);
        LOG.info("Supervisor stop end: {}", localSupervisor);
    }

    public boolean isRunning() {
        return state.isRunning();
    }

    @SuppressWarnings("all")
    private static void printBanner() {
        String bannerSupervisor = "Disjob supervisor banner\n\n" +
            "==================================================================================\n" +
            "      ___ _      _       _        __                             _\n" +
            "     /   (_)___ (_) ___ | |__    / _\\_   _ _ __   ___ _ ____   _(_)___  ___  _ __\n" +
            "    / /\\ / / __|| |/ _ \\| '_ \\   \\ \\| | | | '_ \\ / _ \\ '__\\ \\ / / / __|/ _ \\| '__|\n" +
            "   / /_//| \\__ \\| | (_) | |_) |  _\\ \\ |_| | |_) |  __/ |   \\ V /| \\__ \\ (_) | |\n" +
            "  /___,' |_|___// |\\___/|_.__/   \\__/\\__,_| .__/ \\___|_|    \\_/ |_|___/\\___/|_|\n" +
            "              |__/                        |_|\n\n" +
            "  Supervisor: " + Supervisor.local() + "\n" +
            "  Version   : " + JobConstants.VERSION + "\n" +
            "==================================================================================\n";
        LOG.info(bannerSupervisor);
    }

}
