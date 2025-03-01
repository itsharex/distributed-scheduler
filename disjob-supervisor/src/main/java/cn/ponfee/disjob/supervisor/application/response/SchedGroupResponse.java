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

package cn.ponfee.disjob.supervisor.application.response;

import cn.ponfee.disjob.common.base.ToJsonString;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.Date;

/**
 * Sched group response
 *
 * @author Ponfee
 */
@Getter
@Setter
public class SchedGroupResponse extends ToJsonString implements Serializable {
    private static final long serialVersionUID = -8381578632306318642L;

    private String group;
    private String supervisorToken;
    private String workerToken;
    private String userToken;
    private String ownUser;
    private String alertUsers;
    private String devUsers;
    private String workerContextPath;
    private String webhook;

    private Integer version;
    private Date updatedAt;
    private Date createdAt;
    private String updatedBy;
    private String createdBy;

    public void maskToken() {
        this.supervisorToken = mask(supervisorToken);
        this.workerToken = mask(workerToken);
        this.userToken = mask(userToken);
    }

    private static String mask(String str) {
        if (StringUtils.isEmpty(str)) {
            return str;
        }
        return "*****";
    }

}
