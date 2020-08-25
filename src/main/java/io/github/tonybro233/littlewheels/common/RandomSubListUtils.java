package io.github.tonybro233.littlewheels.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * 随机子集工具
 *
 * @author tony
 */
public abstract class RandomSubListUtils {

    /**
     * 获取随机子集
     */
    public static <T> List<T> randomSubList(List<T> list, int n) {
        if (list.size() < n) {
            throw new IndexOutOfBoundsException("want " + n + " but max is " + list.size());
        }
        int[] indexes = getRandomIndexes(0, list.size() - 1, n);
        List<T> tmp = new ArrayList<>(n);
        for (int idx : indexes) {
            tmp.add(list.get(idx));
        }
        return tmp;
    }

    /**
     * 获取有序随机子集
     */
    public static <T> List<T> sortedRandomSubList(List<T> list, int n) {
        if (list.size() < n) {
            throw new IndexOutOfBoundsException("want " + n + " but max is " + list.size());
        }
        int[] indexes = getSortedRandomIndexes(0, list.size() - 1, n);
        List<T> tmp = new ArrayList<>(n);
        for (int idx : indexes) {
            tmp.add(list.get(idx));
        }
        return tmp;
    }

    public static int[] getRandomIndexes(int min, int max, int n) {
        int len = max - min + 1;

        // 初始化给定范围的待选数组
        int[] source = new int[len];
        for (int i = min; i < min + len; i++) {
            source[i - min] = i;
        }

        int[] result = new int[n];
        Random rd = new Random();
        int index = 0;
        for (int i = 0; i < result.length; i++) {
            // 待选数组0到(len-2)随机一个下标
            index = Math.abs(rd.nextInt() % len--);
            // 将随机到的数放入结果集
            result[i] = source[index];
            // 将待选数组中被随机到的数，用待选数组(len-1)下标对应的数替换
            source[index] = source[len];
        }
        return result;
    }

    public static int[] getSortedRandomIndexes(int min, int max, int n) {
        int[] re = getRandomIndexes(min, max, n);
        Arrays.sort(re);
        return re;
    }

}
