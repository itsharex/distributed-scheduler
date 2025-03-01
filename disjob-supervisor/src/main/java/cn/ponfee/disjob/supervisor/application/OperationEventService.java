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

package cn.ponfee.disjob.supervisor.application;

import cn.ponfee.disjob.common.base.SingletonClassConstraint;
import cn.ponfee.disjob.supervisor.base.OperationEventType;
import org.apache.commons.lang3.mutable.MutableObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static cn.ponfee.disjob.common.concurrent.ThreadPoolExecutors.commonScheduledPool;

/**
 * Operation event service
 *
 * @author Ponfee
 */
@Service
public class OperationEventService extends SingletonClassConstraint {

    private static final Logger LOG = LoggerFactory.getLogger(OperationEventService.class);
    private static final long PERIOD_MS = 5000L;
    private static final Map<OperationEventType, MutableObject<String>> MAP = new ConcurrentHashMap<>();

    private final SchedGroupService groupService;

    public OperationEventService(SchedGroupService groupService) {
        this.groupService = groupService;

        long initialDelay = PERIOD_MS + ThreadLocalRandom.current().nextLong(PERIOD_MS);
        commonScheduledPool().scheduleWithFixedDelay(this::process, initialDelay, PERIOD_MS, TimeUnit.MILLISECONDS);
    }

    public static void subscribe(OperationEventType eventType, String data) {
        if (eventType != null) {
            // add or update value
            MAP.put(eventType, new MutableObject<>(data));
        }
    }

    // ---------------------------------------------------------private methods

    private void process() {
        List<OperationEventType> list = new ArrayList<>(MAP.keySet());
        for (OperationEventType eventType : list) {
            MutableObject<String> dataWrapper = MAP.remove(eventType);
            if (dataWrapper != null) {
                String data = dataWrapper.getValue();
                try {
                    process(eventType, data);
                    LOG.info("Process operation event success: {}, {}", eventType, data);
                } catch (Throwable t) {
                    LOG.error("Process operation event error: " + eventType + ", " + data, t);
                }
            }
        }
    }

    private void process(OperationEventType eventType, String data) {
        if (eventType == OperationEventType.REFRESH_GROUP) {
            groupService.refresh();
        } else {
            LOG.error("Unsupported subscribe operation event type: {}", eventType);
        }
    }

}
