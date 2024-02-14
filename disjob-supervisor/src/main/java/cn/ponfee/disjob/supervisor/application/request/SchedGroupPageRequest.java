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

package cn.ponfee.disjob.supervisor.application.request;

import cn.ponfee.disjob.common.model.PageRequest;
import cn.ponfee.disjob.supervisor.application.AuthorizeGroupService;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

/**
 * Sched group page request
 *
 * @author Ponfee
 */
@Getter
@Setter
public class SchedGroupPageRequest extends PageRequest {
    private static final long serialVersionUID = -213388921649759103L;
    private Set<String> groups;

    public void authorizeAndTruncateGroup(String user) {
        this.groups = AuthorizeGroupService.authorizeAndTruncateGroup(user, this.groups);
    }

    public void truncateGroup() {
        this.groups = AuthorizeGroupService.truncateGroup(this.groups);
    }

}
