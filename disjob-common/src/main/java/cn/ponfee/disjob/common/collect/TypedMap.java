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

package cn.ponfee.disjob.common.collect;

import java.util.Map;

/**
 * Get the value with typed for {@link Map}
 * 
 * @author Ponfee
 * @param <K> the key type
 * @param <V> the value type
 */
public interface TypedMap<K, V> extends Map<K, V>, TypedKeyValue<K, V> {

    @Override
    default V getValue(K key) {
        return this.get(key);
    }

}
