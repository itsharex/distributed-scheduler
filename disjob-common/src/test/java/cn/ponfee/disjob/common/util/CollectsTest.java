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

package cn.ponfee.disjob.common.util;

import cn.ponfee.disjob.common.collect.Collects;
import cn.ponfee.disjob.common.date.Dates;
import cn.ponfee.disjob.common.tree.print.BinaryTreePrinter;
import cn.ponfee.disjob.common.tuple.Tuple2;
import com.google.common.collect.ImmutableList;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;
import org.mockito.internal.util.collections.Sets;

import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Collects test
 *
 * @author Ponfee
 */
public class CollectsTest {

    @Test
    public void testTruncate() {
        assertThat(Collects.truncate(null, 2)).isNull();
        assertThat(Collects.truncate(Collections.emptySet(), 2)).isEmpty();
        assertThat(Collects.truncate(Sets.newSet(1), 0)).size().isEqualTo(1);
        assertThat(Collects.truncate(Sets.newSet(1, 2, 3, 4, 5), 3)).size().isEqualTo(3);
        assertThat(Stream.of().anyMatch(Objects::isNull)).isFalse();
        assertThat(Stream.of(1).anyMatch(Objects::isNull)).isFalse();
        assertThat(Stream.of(1, null).anyMatch(Objects::isNull)).isTrue();
    }

    @Test
    public void testNewArray() {
        List<List<Integer>> list = Arrays.asList(Arrays.asList(1, 2, 3), Arrays.asList(2, 3, 4), Arrays.asList(6, 7, 8));
        Integer[] array1 = list.stream().flatMap(List::stream).toArray(length -> Collects.newArray(Integer[].class, length));
        System.out.println(Arrays.toString(array1));
        assertThat(array1).hasSize(9);

        Object[] array2 = list.stream().flatMap(List::stream).toArray(length -> Collects.newArray(Object[].class, length));
        System.out.println(Arrays.toString(array2));
        assertThat(array2).hasSize(9);
    }

    @Test
    public void testSort() {
        List<Tuple2<String, Long>> list = new ArrayList<>();
        list.add(Tuple2.of("A", 3L));
        list.add(Tuple2.of("B", 2L));
        list.add(Tuple2.of("C", 1L));
        list.add(Tuple2.of("D", 4L));
        Comparator<Tuple2<String, Long>> comparator = Comparator.comparingLong(e -> e.b);

        list.sort(comparator);
        System.out.println(list);
        Tuple2<String, Long> first = list.get(0);
        assertThat(first.a).isEqualTo("C");
        first.b += 1L;

        list.sort(comparator);
        System.out.println(list);
        first = list.get(0);
        assertThat(first.a).isEqualTo("C");
        first.b += 1L;

        list.sort(comparator);
        System.out.println(list);
        first = list.get(0);
        assertThat(first.a).isEqualTo("B");
        first.b += 1L;

        list.sort(comparator);
        System.out.println(list);
        first = list.get(0);
        assertThat(first.a).isEqualTo("B");
        first.b += 1L;

        list.sort(comparator);
        System.out.println(list);
        first = list.get(0);
        assertThat(first.a).isEqualTo("C");
        first.b += 1L;

        list.sort(comparator);
        System.out.println(list);
        first = list.get(0);
        assertThat(first.a).isEqualTo("A");
        first.b += 1L;
    }

    @Test
    public void testConcat() {
        String[] b = {"a", "b", "c"};

        Integer[] a1 = {1, 2, 3};
        assertThatThrownBy(() -> Collects.concat(a1, b))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Cannot store java.lang.String into java.lang.Integer[]");

        Object[] a2 = a1;
        assertThatThrownBy(() -> Collects.concat(a2, b))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Cannot store java.lang.String into java.lang.Integer[]");

        Object[] a3 = {1, 2, 3};
        assertThat(Collects.concat(a3, b)).hasSize(6);

        assertThatThrownBy(() -> Collects.concat(b, a3))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Cannot store java.lang.Object into java.lang.String[]");
    }

    @Test
    public void testTreePrint() throws IOException {
        BinaryTreePrinter<Integer> printer = BinaryTreePrinter.<Integer>builder(
                Object::toString,
                i -> i > 5 ? null : i + 1,
                i -> i > 7 ? null : i + 2
            )
            .directed(true)
            .branch(BinaryTreePrinter.Branch.RECTANGLE)
            .build();
        printer.print(1);
    }

    @Test
    public void testImmutableList() {
        List<String> list = new ArrayList<>();
        list.add("A");
        list.add("B");
        List<String> unmodifiableList = Collections.unmodifiableList(list);
        assertThat(list.size()).isEqualTo(2);
        assertThat(unmodifiableList.size()).isEqualTo(2);
        list.add("C");
        assertThat(list.size()).isEqualTo(3);
        assertThat(unmodifiableList.size()).isEqualTo(3);

        List<String> immutableList = ImmutableList.copyOf(list);
        assertThat(immutableList.size()).isEqualTo(3);
        list.add("D");
        assertThat(list.size()).isEqualTo(4);
        assertThat(immutableList.size()).isEqualTo(3);

        assertThatThrownBy(() -> immutableList.add("E"))
            .isInstanceOf(UnsupportedOperationException.class);

        List<String> immutableList2 = ImmutableList.of();
        assertThatThrownBy(() -> immutableList2.add("E"))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    public void testComparator() {
        List<Integer> ascList = Lists.newArrayList(3, 1, null, 9, 10, 8);
        ascList.sort(Comparators.asc());
        assertThat(ascList.toString()).isEqualTo("[null, 1, 3, 8, 9, 10]");

        List<Integer> descList = Lists.newArrayList(3, 1, null, 9, 10, 8);
        descList.sort(Comparators.desc());
        assertThat(descList.toString()).isEqualTo("[10, 9, 8, 3, 1, null]");

        System.out.println(Lists.newArrayList(3, 1, null, 9, 10, 8).stream().max(Comparators.asc()).orElse(null));
        System.out.println(Lists.<Integer>newArrayList().stream().max(Comparators.asc()).orElse(null));
        System.out.println(Lists.<Integer>newArrayList(null, null).stream().filter(Objects::nonNull).max(Comparators.asc()).orElse(null));
        System.out.println(Lists.newArrayList(3, 1, null, 9, 10, 8).stream().reduce(-1, (a, b) -> a == null ? b : (b == null ? a : Integer.max(a, b))));

        List<Date> dates = Lists.newArrayList(Dates.toDate("2000-01-01", Dates.DATE_PATTERN), null, Dates.toDate("2001-01-01", Dates.DATE_PATTERN), null);
        Date maxDate = dates.stream().filter(Objects::nonNull).max(Comparator.naturalOrder()).orElseGet(Date::new);
        assertThat(Dates.format(maxDate)).isEqualTo("2001-01-01 00:00:00");
    }

}
