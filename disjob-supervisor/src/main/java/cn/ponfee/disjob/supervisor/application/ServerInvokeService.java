/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.supervisor.application;

import cn.ponfee.disjob.common.base.RetryTemplate;
import cn.ponfee.disjob.common.base.SingletonClassConstraint;
import cn.ponfee.disjob.common.collect.Collects;
import cn.ponfee.disjob.common.concurrent.MultithreadExecutors;
import cn.ponfee.disjob.common.concurrent.ThreadPoolExecutors;
import cn.ponfee.disjob.common.spring.RestTemplateUtils;
import cn.ponfee.disjob.common.util.Numbers;
import cn.ponfee.disjob.core.base.*;
import cn.ponfee.disjob.core.exception.AuthenticationException;
import cn.ponfee.disjob.core.exception.KeyExistsException;
import cn.ponfee.disjob.core.exception.KeyNotExistsException;
import cn.ponfee.disjob.core.param.supervisor.EventParam;
import cn.ponfee.disjob.core.param.worker.ConfigureWorkerParam;
import cn.ponfee.disjob.core.param.worker.ConfigureWorkerParam.Action;
import cn.ponfee.disjob.core.param.worker.GetMetricsParam;
import cn.ponfee.disjob.registry.SupervisorRegistry;
import cn.ponfee.disjob.registry.rpc.DestinationServerRestProxy;
import cn.ponfee.disjob.registry.rpc.DestinationServerRestProxy.DestinationServerInvoker;
import cn.ponfee.disjob.supervisor.application.converter.ServerMetricsConverter;
import cn.ponfee.disjob.supervisor.application.request.ConfigureAllWorkerRequest;
import cn.ponfee.disjob.supervisor.application.request.ConfigureOneWorkerRequest;
import cn.ponfee.disjob.supervisor.application.response.SupervisorMetricsResponse;
import cn.ponfee.disjob.supervisor.application.response.WorkerMetricsResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Server info service
 *
 * @author Ponfee
 */
@Service
public class ServerInvokeService extends SingletonClassConstraint {
    private static final Logger LOG = LoggerFactory.getLogger(ServerInvokeService.class);

    private final SupervisorRegistry supervisorRegistry;
    private final Supervisor.Current currentSupervisor;
    private final DestinationServerInvoker<SupervisorRpcService, Supervisor> supervisorRpcServiceClient;
    private final DestinationServerInvoker<WorkerRpcService, Worker> workerRpcServiceClient;

    public ServerInvokeService(@Value("${server.servlet.context-path:/}") String contextPath,
                               SupervisorRegistry supervisorRegistry,
                               Supervisor.Current currentSupervisor,
                               SupervisorRpcService supervisorProvider,
                               HttpProperties http,
                               @Nullable WorkerRpcService workerProvider,
                               @Nullable ObjectMapper objectMapper) {
        http.check();
        Function<Supervisor, String> supervisorContextPath = supervisor -> contextPath;
        Function<Worker, String> workerContextPath = worker -> Supervisor.current().getWorkerContextPath(worker.getGroup());
        RestTemplate restTemplate = RestTemplateUtils.create(http.getConnectTimeout(), http.getReadTimeout(), objectMapper);
        RetryProperties retry = RetryProperties.of(0, 0);

        this.supervisorRegistry = supervisorRegistry;
        this.currentSupervisor = currentSupervisor;
        this.supervisorRpcServiceClient = DestinationServerRestProxy.create(
            SupervisorRpcService.class, supervisorProvider, currentSupervisor, supervisorContextPath, restTemplate, retry
        );
        this.workerRpcServiceClient = DestinationServerRestProxy.create(
            WorkerRpcService.class, workerProvider, Worker.current(), workerContextPath, restTemplate, retry
        );
    }

    // ------------------------------------------------------------public methods

    public List<SupervisorMetricsResponse> supervisors() throws Exception {
        List<Supervisor> list = supervisorRegistry.getRegisteredServers();
        list = Collects.sorted(list, Comparator.comparing(e -> e.equals(currentSupervisor) ? 0 : 1));
        return MultithreadExecutors.call(list, this::getSupervisorMetrics, ThreadPoolExecutors.commonThreadPool());
    }

    public List<WorkerMetricsResponse> workers(String group, String worker) {
        if (StringUtils.isNotBlank(worker)) {
            String[] array = worker.trim().split(":");
            String host = array[0].trim();
            int port = Numbers.toInt(array[1].trim(), -1);
            WorkerMetricsResponse metrics = getWorkerMetrics(new Worker(group, "", host, port));
            return StringUtils.isBlank(metrics.getWorkerId()) ? Collections.emptyList() : Collections.singletonList(metrics);
        } else {
            List<Worker> list = supervisorRegistry.getDiscoveredServers(group);
            list = Collects.sorted(list, Comparator.comparing(e -> e.equals(Worker.current()) ? 0 : 1));
            return MultithreadExecutors.call(list, this::getWorkerMetrics, ThreadPoolExecutors.commonThreadPool());
        }
    }

    public void configureOneWorker(ConfigureOneWorkerRequest req) {
        Worker worker = req.toWorker();
        if (req.getAction() == Action.ADD_WORKER) {
            List<Worker> workers = supervisorRegistry.getDiscoveredServers(req.getGroup());
            if (workers != null && workers.stream().anyMatch(worker::sameWorker)) {
                throw new KeyExistsException("Worker already registered: " + worker);
            }
            verifyWorkerSignature(worker);
            // add worker to this group
            req.setData(req.getGroup());
        } else {
            List<Worker> workers = getDiscoveredWorkers(req.getGroup());
            if (!workers.contains(worker)) {
                throw new KeyNotExistsException("Not found worker: " + worker);
            }
        }

        configureWorker(worker, req.getAction(), req.getData());
    }

    public void configureAllWorker(ConfigureAllWorkerRequest req) {
        List<Worker> workers = getDiscoveredWorkers(req.getGroup());
        MultithreadExecutors.run(
            workers,
            worker -> configureWorker(worker, req.getAction(), req.getData()),
            ThreadPoolExecutors.commonThreadPool()
        );
    }

    public void publishOtherSupervisors(EventParam eventParam) {
        try {
            List<Supervisor> supervisors = supervisorRegistry.getRegisteredServers()
                .stream()
                .filter(e -> !currentSupervisor.sameSupervisor(e))
                .collect(Collectors.toList());
            MultithreadExecutors.run(
                supervisors,
                supervisor -> publishSupervisor(supervisor, eventParam),
                ThreadPoolExecutors.commonThreadPool()
            );
        } catch (Exception e) {
            LOG.error("Publish all supervisor error.", e);
        }
    }

    // ------------------------------------------------------------private methods

    private SupervisorMetricsResponse getSupervisorMetrics(Supervisor supervisor) {
        SupervisorMetrics metrics = null;
        Long pingTime = null;
        try {
            long start = System.currentTimeMillis();
            metrics = supervisorRpcServiceClient.invoke(supervisor, SupervisorRpcService::metrics);
            pingTime = System.currentTimeMillis() - start;
        } catch (Throwable e) {
            LOG.warn("Ping supervisor occur error: {} {}", supervisor, e.getMessage());
        }

        SupervisorMetricsResponse response;
        if (metrics == null) {
            response = new SupervisorMetricsResponse();
        } else {
            response = ServerMetricsConverter.INSTANCE.convert(metrics);
        }

        response.setHost(supervisor.getHost());
        response.setPort(supervisor.getPort());
        response.setPingTime(pingTime);
        return response;
    }

    private WorkerMetricsResponse getWorkerMetrics(Worker worker) {
        WorkerMetrics metrics = null;
        Long pingTime = null;
        String group = worker.getGroup();
        GetMetricsParam param = buildGetMetricsParam(group);
        try {
            long start = System.currentTimeMillis();
            metrics = workerRpcServiceClient.invoke(worker, client -> client.metrics(param));
            pingTime = System.currentTimeMillis() - start;
        } catch (Throwable e) {
            LOG.warn("Ping worker occur error: {} {}", worker, e.getMessage());
        }

        WorkerMetricsResponse response;
        if (metrics == null || !SchedGroupService.verifyWorkerSignatureToken(metrics.getSignature(), group)) {
            response = new WorkerMetricsResponse(worker.getWorkerId());
        } else {
            response = ServerMetricsConverter.INSTANCE.convert(metrics);
        }

        response.setHost(worker.getHost());
        response.setPort(worker.getPort());
        response.setPingTime(pingTime);
        return response;
    }

    private List<Worker> getDiscoveredWorkers(String group) {
        List<Worker> list = supervisorRegistry.getDiscoveredServers(group);
        if (CollectionUtils.isEmpty(list)) {
            throw new KeyNotExistsException("Group '" + group + "' not exists workers.");
        }
        return list;
    }

    private void verifyWorkerSignature(Worker worker) {
        String group = worker.getGroup();
        GetMetricsParam param = buildGetMetricsParam(group);
        WorkerMetrics metrics = workerRpcServiceClient.invoke(worker, client -> client.metrics(param));
        if (!SchedGroupService.verifyWorkerSignatureToken(metrics.getSignature(), group)) {
            throw new AuthenticationException("Worker authenticated failed: " + worker);
        }
    }

    private void configureWorker(Worker worker, Action action, String data) {
        ConfigureWorkerParam param = new ConfigureWorkerParam(SchedGroupService.createSupervisorAuthenticationToken(worker.getGroup()));
        param.setAction(action);
        param.setData(data);
        workerRpcServiceClient.invokeWithoutResult(worker, client -> client.configureWorker(param));
    }

    private void publishSupervisor(Supervisor supervisor, EventParam param) {
        RetryTemplate.executeQuietly(() -> supervisorRpcServiceClient.invokeWithoutResult(supervisor, client -> client.publish(param)), 1, 2000);
    }

    private GetMetricsParam buildGetMetricsParam(String group) {
        return new GetMetricsParam(SchedGroupService.createSupervisorAuthenticationToken(group), group);
    }

}