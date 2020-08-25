package io.github.tonybro233.littlewheels.common;

import org.springframework.data.domain.*;
import org.springframework.lang.Nullable;

import java.beans.Introspector;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 数据集合分页包装器
 *
 * @author tony
 */
@SuppressWarnings({"unchecked"})
public final class PageWrapper {

    /**
     * 根据分页信息将{@link List}包装为{@link Page}，集合将根据分页信息
     * 重新排序(如果有排序内容)、切割。 <br/>
     * 分页信息自动合理化，不会超过最大页数 <br/>
     * 若分页信息为空，返回单页page <br/>
     * NULL默认为最小值
     *
     * @param content  内容
     * @param pageable 分页
     * @param <T>      泛型
     * @return 分页结果
     */
    public static <T> Page<T> wrap(List<T> content, @Nullable Pageable pageable) {
        Optional<Pageable> optionalPageable = Optional.ofNullable(pageable);
        if (optionalPageable.map(Pageable::isUnpaged).orElse(false)) {
            throw new UnsupportedOperationException("Not support unpaged!");
        }
        final List<T> list = new ArrayList<>(content);
        sortContent(list, optionalPageable.map(Pageable::getSort).orElse(null));
        Pageable reasonablePage = rationalization(list, pageable);
        List<T> subList = sliceContent(list, reasonablePage);

        return new PageImpl<>(subList, reasonablePage, list.size());
    }

    private PageWrapper() {
    }

    private static final Map<Class<?>, Map<String, Method>> CLAZZ_GETTERS = new ConcurrentHashMap<>(256);

    /**
     * 根据分页排序信息将内容排序
     *
     * @param content 内容
     * @param sort    分页信息
     */
    private static <T> void sortContent(List<T> content, Sort sort) {
        if (null == content || content.size() == 0) {
            return;
        }
        if (null == sort || !sort.iterator().hasNext()) {
            return;
        }

        // 获取、筛选排序信息
        Map<String, Method> getters = CLAZZ_GETTERS.computeIfAbsent(
                content.get(0).getClass(), PageWrapper::findAllGetters);

        List<GetterComparator> comparators = sort.stream()
                .filter(o -> getters.containsKey(o.getProperty()))
                .map(order -> new GetterComparator(order, getters.get(order.getProperty())))
                .collect(Collectors.toList());

        if (comparators.isEmpty()) {
            return;
        }

        // 排序
        content.sort(new ComparatorChain(comparators));
    }

    /**
     * 合理化分页信息
     *
     * @param pageable 输入的分页信息
     * @return 调整后的合理化信息
     */
    private static Pageable rationalization(List content, Pageable pageable) {
        Optional<List> optList = Optional.ofNullable(content);
        Optional<Pageable> optPageable = Optional.ofNullable(pageable);

        int pageNumber = optPageable.map(Pageable::getPageNumber).orElse(0);
        int pageSize = optPageable.map(Pageable::getPageSize)
                .orElse(optList.map(List::size).orElse(10));
        int size = optList.map(List::size).orElse(0);

        // 每页大小至少为1
        pageSize = pageSize < 1 ? 1 : pageSize;
        int totalPages = (int) Math.ceil((double) size / (double) pageSize);

        // 限制不能超过最大页数
        if (pageNumber > totalPages) {
            pageNumber = totalPages - 1;
        }
        pageNumber = pageNumber < 0 ? 0 : pageNumber;

        return PageRequest.of(pageNumber, pageSize,
                optPageable.map(Pageable::getSort).orElse(null));
    }

    /**
     * 截取指定的内容
     *
     * @param content  内容列表
     * @param pageable 分页信息
     * @return 截取的列表
     */
    private static <T> List<T> sliceContent(List<T> content, final Pageable pageable) {
        if (null == content || content.size() == 0) {
            return Collections.emptyList();
        }
        if (null == pageable || pageable.getPageSize() < 1) {
            return content;
        }

        int pageSize = pageable.getPageSize();
        if (pageable.getOffset() > Integer.MAX_VALUE) {
            throw new UnsupportedOperationException("Offset too large");
        }
        // pageNumber * pageSize
        int offset = (int) pageable.getOffset();
        int size = content.size();

        if (offset == 0 && pageSize >= size) {
            return content;
        }

        // 子列表的起始、结尾序号（不包含结尾）
        int start = offset;
        int end = offset + pageSize;

        // 最后一页的开始
        int lastStart = size - pageSize;
        lastStart = lastStart < 0 ? 0 : lastStart;

        if (start >= size) {
            // 超过最后一页取最后一页，合理化后不会进入这个语句块
            start = lastStart;
            end = size;
        } else {
            end = end > size ? size : end;
        }

        return content.subList(start, end);
    }

    /**
     * 获取class所有的Getter
     *
     * @param clazz 类
     * @return name-method map
     */
    private static Map<String, Method> findAllGetters(Class<?> clazz) {
        Map<String, Method> result = new HashMap<>();
        for (Method method : clazz.getMethods()) {
            // if the method has parameters, skip it
            if (method.getParameterCount() != 0) {
                continue;
            }

            if (Modifier.isStatic(method.getModifiers())) {
                continue;
            }

            if (Void.TYPE == method.getReturnType() || Void.class == method.getReturnType()) {
                continue;
            }

            final String methodName = method.getName();

            // try "get"
            if (methodName.startsWith("get")) {
                final String stemName = methodName.substring(3);
                result.put(Introspector.decapitalize(stemName), method);
            }
            // if not "get", then try "is"
            else if (methodName.startsWith("is")) {
                final String stemName = methodName.substring(2);
                result.put(Introspector.decapitalize(stemName), method);
            }

        }
        return result;
    }

    private static class GetterComparator implements Comparator {

        private final Sort.Order order;

        private final Method getter;

        private final boolean comparable;

        GetterComparator(Sort.Order order, Method getter) {
            this.order = order;
            this.getter = getter;
            this.comparable = Comparable.class.isAssignableFrom(getter.getReturnType());
            // 防止无法读取内部类
            this.getter.setAccessible(true);
        }

        @Override
        public int compare(Object o1, Object o2) {
            Object val1 = null, val2 = null;
            try {
                val1 = getter.invoke(o1);
            } catch (Exception ignored) {
            }
            try {
                val2 = getter.invoke(o2);
            } catch (Exception ignored) {
            }

            if (!comparable) {
                if (null != val1) {
                    val1 = val1.toString();
                }
                if (null != val2) {
                    val2 = val2.toString();
                }
            }
            Comparable cp1 = (Comparable) val1, cp2 = (Comparable) val2;

            // 排序属性处理
            Sort.NullHandling nullHandling = order.getNullHandling();
            Sort.Direction direction = order.getDirection();
            boolean ignoreCase = order.isIgnoreCase();

            // 比较
            int result = 0;
            boolean adjust = false;
            if (null == cp1 && null == cp2) {
                // 0
            } else if (null == cp1) {
                switch (nullHandling) {
                    case NATIVE:
                        // 默认NULL最小，升序NULL最前，降序NULL最后
                        result = -1;
                        adjust = true;
                        break;
                    case NULLS_FIRST:
                        result = -1;
                        break;
                    case NULLS_LAST:
                        // 集合排序默认升序，因此NULL排在最后说明NULL最大
                        result = 1;
                        break;
                    default:
                }
            } else if (null == cp2) {
                switch (nullHandling) {
                    case NATIVE:
                        result = 1;
                        adjust = true;
                        break;
                    case NULLS_FIRST:
                        result = 1;
                        break;
                    case NULLS_LAST:
                        result = -1;
                        break;
                    default:
                }
            } else {
                if (cp1 instanceof String) {
                    if (ignoreCase) {
                        result = ((String) cp1).compareToIgnoreCase((String) cp2);
                    } else {
                        result = ((String) cp1).compareTo((String) cp2);
                    }
                } else {
                    result = cp1.compareTo(cp2);
                }
                adjust = true;
            }

            // 集合的排序默认是升序，如果排序属性是降序，返回值取反
            if (adjust && direction == Sort.Direction.DESC) {
                result = -result;
            }

            return result;
        }
    }

    private static class ComparatorChain<T> implements Comparator<T> {

        private final List<Comparator<T>> comparators;

        ComparatorChain(List<Comparator<T>> comparators) {
            this.comparators = comparators;
        }

        @Override
        public int compare(T o1, T o2) {
            for (Comparator<T> comparator : comparators) {
                int retval = comparator.compare(o1, o2);
                if (retval != 0) {
                    return retval;
                }
            }
            return 0;
        }
    }

}
