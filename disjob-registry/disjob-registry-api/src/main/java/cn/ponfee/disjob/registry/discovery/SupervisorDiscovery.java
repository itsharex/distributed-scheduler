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

package cn.ponfee.disjob.registry.discovery;

import cn.ponfee.disjob.core.base.RetryProperties;
import cn.ponfee.disjob.core.base.Supervisor;
import cn.ponfee.disjob.core.base.SupervisorRpcService;
import cn.ponfee.disjob.core.base.Worker;
import cn.ponfee.disjob.core.enums.RegistryEventType;
import cn.ponfee.disjob.registry.rpc.DestinationServerRestProxy;
import cn.ponfee.disjob.registry.rpc.DestinationServerRestProxy.DestinationServerClient;
import com.google.common.collect.ImmutableList;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

/**
 * Supervisor discovery.
 *
 * @author Ponfee
 */
public final class SupervisorDiscovery extends ServerDiscovery<Supervisor, Worker> {

    private final DestinationServerClient<SupervisorRpcService, Supervisor> supervisorRpcClient;

    private volatile ImmutableList<Supervisor> supervisors = ImmutableList.of();

    SupervisorDiscovery(RestTemplate restTemplate) {
        this.supervisorRpcClient = DestinationServerRestProxy.create(
            SupervisorRpcService.class,
            null,
            null,
            supervisor -> Worker.local().getSupervisorContextPath(),
            restTemplate,
            RetryProperties.none()
        );
    }

    /**
     * <pre>
     * synchronized的执行过程：
     *   1）获得同步锁
     *   2）清空工作内存
     *   3）从主内存中复制数据副本到工作内存
     *   4）执行代码
     *   5）刷新数据到主内存
     *   6）释放锁
     * </pre>
     *
     * @param discoveredSupervisors the discovered supervisors
     */
    @Override
    public synchronized void refreshServers(List<Supervisor> discoveredSupervisors) {
        this.supervisors = toSortedImmutableList(discoveredSupervisors);
    }

    @Override
    public synchronized void updateServers(RegistryEventType eventType, Supervisor supervisor) {
        if (eventType.isRegister() && isAlive(supervisor)) {
            return;
        }
        if (eventType.isDeregister() && !isAlive(supervisor)) {
            return;
        }
        this.supervisors = mergeServers(supervisors, eventType, supervisor);
    }

    @Override
    public List<Supervisor> getServers(String group) {
        Assert.isNull(group, "Get discovery supervisor group must be null.");
        return supervisors;
    }

    @Override
    public boolean hasServers() {
        return !supervisors.isEmpty();
    }

    @Override
    public boolean isAlive(Supervisor supervisor) {
        return Collections.binarySearch(supervisors, supervisor) > -1;
    }

    // ----------------------------------------------------------------default package methods

    @Override
    List<Supervisor> getServers() {
        return supervisors;
    }

    @Override
    void notifyServer(Supervisor supervisor, RegistryEventType eventType, Worker worker) {
        try {
            supervisorRpcClient.invoke(supervisor, client -> client.subscribeWorkerEvent(eventType, worker));
        } catch (Throwable t) {
            log.error("Notify server error: {}, {}", supervisor, t.getMessage());
        }
    }

}
