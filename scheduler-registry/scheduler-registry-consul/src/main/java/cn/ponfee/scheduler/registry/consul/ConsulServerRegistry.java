package cn.ponfee.scheduler.registry.consul;

import cn.ponfee.scheduler.common.base.exception.Throwables;
import cn.ponfee.scheduler.common.concurrent.MultithreadExecutors;
import cn.ponfee.scheduler.common.concurrent.NamedThreadFactory;
import cn.ponfee.scheduler.common.util.Files;
import cn.ponfee.scheduler.common.util.ObjectUtils;
import cn.ponfee.scheduler.core.base.JobConstants;
import cn.ponfee.scheduler.core.base.Server;
import cn.ponfee.scheduler.registry.ServerRegistry;
import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.agent.model.NewService;
import com.ecwid.consul.v1.health.HealthServicesRequest;
import com.ecwid.consul.v1.health.model.HealthService;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Registry server based <a href="https://developer.hashicorp.com/consul/api-docs">consul</a>.
 *
 * @author Ponfee
 */
public abstract class ConsulServerRegistry<R extends Server, D extends Server> extends ServerRegistry<R, D> {

    private static final int WAIT_TIME_SECONDS = 60;
    private static final int CHECK_PASS_INTERVAL_SECONDS = 2;
    private static final String CHECK_TTL_SECONDS = (CHECK_PASS_INTERVAL_SECONDS * 8) + "s";
    private static final String DEREGISTER_TIME_SECONDS = "20s";

    // --------------------------------------------------------------registry
    /**
     * Consul client
     */
    private final ConsulClient client;

    /**
     * Consul ACL token
     */
    private final String token;

    private final ScheduledExecutorService consulTtlCheckExecutor;

    private final ConsulSubscriberThread   consulSubscriberThread;

    protected ConsulServerRegistry(String host, int port, String token) {
        this.client = new ConsulClient(host, port);
        // Assert.notNull(client.getAgentSelf(), "Consul server is not available.");
        this.token = token;

        this.consulTtlCheckExecutor = new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("consul_server_registry", true));
        consulTtlCheckExecutor.scheduleAtFixedRate(this::checkPass, CHECK_PASS_INTERVAL_SECONDS, CHECK_PASS_INTERVAL_SECONDS, TimeUnit.SECONDS);

        this.consulSubscriberThread = new ConsulSubscriberThread(-1);
        consulSubscriberThread.setDaemon(true);
        consulSubscriberThread.setName("consul_subscriber_thread");
        consulSubscriberThread.start();
    }

    // ------------------------------------------------------------------Registry

    @Override
    public final void register(R server) {
        if (closed) {
            return;
        }

        if (token == null) {
            client.agentServiceRegister(buildRegistryServer(server));
        } else {
            client.agentServiceRegister(buildRegistryServer(server), token);
        }
        registered.add(server);
        log.info("Server registered: {} - {}", registryRole.name(), server);
    }

    @Override
    public final void deregister(R server) {
        try {
            registered.remove(server);
            if (token == null) {
                client.agentServiceDeregister(buildId(server));
            } else {
                client.agentServiceDeregister(buildId(server), token);
            }
            log.info("Server deregister: {} - {}", registryRole.name(), server);
        } catch (Exception e) {
            log.error("Agent service deregister error.", e);
        }
    }

    // ------------------------------------------------------------------Close

    @Override
    public void close() {
        closed = true;
        if (!close.compareAndSet(false, true)) {
            log.warn("Repeat call close method\n{}", ObjectUtils.getStackTrace());
            return;
        }

        try {
            consulTtlCheckExecutor.shutdownNow();
            consulTtlCheckExecutor.awaitTermination(1, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Await ttl check pass scheduled executor termination occur error.", e);
        }

        registered.forEach(this::deregister);
        registered.clear();

        Throwables.caught(() -> MultithreadExecutors.stopThread(consulSubscriberThread, 0, 0, 200));

        super.close();
    }

    // ------------------------------------------------------------------private method & class

    private NewService buildRegistryServer(R server) {
        NewService service = new NewService();
        service.setName(registryRole.key());
        service.setId(buildId(server));
        service.setAddress(server.getHost());
        service.setPort(server.getPort());
        service.setCheck(buildCheck());
        service.setTags(null);
        service.setMeta(null);
        return service;
    }

    private String buildId(R server) {
        return JobConstants.SCHEDULER_KEY_PREFIX + Files.UNIX_FOLDER_SEPARATOR + server.serialize();
    }

    private static NewService.Check buildCheck() {
        NewService.Check check = new NewService.Check();
        check.setTtl(CHECK_TTL_SECONDS);
        check.setDeregisterCriticalServiceAfter(DEREGISTER_TIME_SECONDS);
        return check;
    }

    private void checkPass() {
        if (closed) {
            return;
        }
        for (R server : registered) {
            String checkId = buildId(server);
            try {
                // Prepend "service:" for service level checks.
                if (token == null) {
                    client.agentCheckPass("service:" + checkId);
                } else {
                    client.agentCheckPass("service:" + checkId, null, token);
                }
                if (log.isDebugEnabled()) {
                    log.debug("check pass for server: " + server + " with check id: " + checkId);
                }
            } catch (Throwable t) {
                log.warn("fail to check pass for server: " + server + ", check id is: " + checkId, t);
            }
        }
    }

    private synchronized void doRefreshDiscoveryServers(List<HealthService> healthServices) {
        List<D> servers;
        if (CollectionUtils.isEmpty(healthServices)) {
            log.error("Not discovered available {} from consul.", discoveryRole.name());
            servers = Collections.emptyList();
        } else {
            servers = healthServices.stream()
                .map(HealthService::getService)
                .filter(Objects::nonNull)
                .map(s -> s.getId().substring(JobConstants.SCHEDULER_KEY_PREFIX.length() + 1))
                .map(s -> (D) discoveryRole.deserialize(s))
                .collect(Collectors.toList());
        }
        refreshDiscoveredServers(servers);
    }

    private class ConsulSubscriberThread extends Thread {
        private long lastConsulIndex;

        private ConsulSubscriberThread(long initConsulIndex) {
            this.lastConsulIndex = initConsulIndex;
        }

        @Override
        public void run() {
            while (!closed) {
                Response<List<HealthService>> response = getDiscoveryServers(lastConsulIndex, WAIT_TIME_SECONDS);
                Long currentIndex = response.getConsulIndex();
                if (currentIndex != null && currentIndex > lastConsulIndex) {
                    lastConsulIndex = currentIndex;
                    doRefreshDiscoveryServers(response.getValue());
                }
            }
        }

        private Response<List<HealthService>> getDiscoveryServers(long index, long waitTime) {
            HealthServicesRequest request = HealthServicesRequest.newBuilder()
                .setQueryParams(new QueryParams(waitTime, index))
                .setPassing(true)
                .setToken(token)
                .build();
            // Health api: /v1/health/service/{serviceName}
            // doc page: https://www.consul.io/api-docs/health
            // Blocking Queries doc page: https://www.consul.io/api-docs/features/blocking
            return client.getHealthServices(discoveryRole.key(), request);
        }
    }

}
