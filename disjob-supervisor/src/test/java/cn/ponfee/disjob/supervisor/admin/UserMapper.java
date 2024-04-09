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

package cn.ponfee.disjob.supervisor.admin;

import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * Mybatis mapper of sched_depend database table.
 *
 * @author Ponfee
 */
public interface UserMapper {

    Map<String, Object> getByLoginName(String loginName);

    List<Map<String, Object>> findByLoginName(List<String> loginNames);

    int updateRemark(@Param("loginName") String loginName, @Param("remark") String remark);

}
