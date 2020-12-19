package io.github.tonybro233.littlewheels.common;

import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class RandomSubListUtilsTest {

    @Test
    void testRandomSubList() {
        // ensure element is unique
        List<Integer> dataList = Arrays.asList(
                3, 2, 1, 5, 4 ,0, 9, 7, 8
        );

        Set<Integer> setView = new HashSet<>(dataList);

        assertThrows(IndexOutOfBoundsException.class, () -> {
            RandomSubListUtils.randomSubList(dataList, dataList.size() + 1);
        }, "Should throw IndexOutOfBoundsException when length param is greater than list's size");

        for (int i = 1; i <= dataList.size(); i++) {
            for (int j = 0; j < 10; j++) {
                List<Integer> subList = RandomSubListUtils.randomSubList(dataList, i);
                assertEquals(i, subList.size(), "Sublist's size is not right");

                Set<Integer> subSet = new HashSet<>(subList);
                assertEquals(subList.size(), subSet.size(), "Find repeat element(s) when origin list's element is unique");

                assertTrue(setView.containsAll(subSet));
            }
        }
    }

    @Test
    void testRandomSubList2() {
        // 1->1  2->2  3->3  4->4  5->5
        List<Integer> dataList = Arrays.asList(
                1, 2, 2, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 5, 5
        );

        for (int i = 2; i < dataList.size(); i++) {
            for (int j = 0; j < 10; j++) {
                List<Integer> subList = RandomSubListUtils.randomSubList(dataList, i);
                assertEquals(i, subList.size(), "Sublist's size is not right");

                // group count
                Map<Integer, Long> collectMap = subList.stream()
                        .collect(Collectors.groupingBy(integer -> integer, Collectors.counting()));

                for (Map.Entry<Integer, Long> entry : collectMap.entrySet()) {
                    assertTrue(entry.getKey() < 6);
                    assertTrue(entry.getKey() > 0);
                    assertTrue(entry.getKey() >= entry.getValue());
                }
            }
        }
    }

    @Test
    void testSortedRandomSubList() {
        List<Integer> dataList = Arrays.asList(
                1, 2, 3, 4, 5, 6, 7, 8, 9, 10
        );

        for (int i = 2; i < dataList.size(); i++) {
            for (int j = 0; j < 10; j++) {
                List<Integer> subList = RandomSubListUtils.sortedRandomSubList(dataList, i);
                assertEquals(i, subList.size(), "Sublist's size is not right");
                assertTrue(inOrder(subList), "Sublist should in order");
            }
        }
    }

    boolean inOrder(List<Integer> list) {
        for (int i = 1; i < list.size(); i++) {
            if (list.get(i) < list.get(i - 1)) {
                return false;
            }
        }
        return true;
    }

}