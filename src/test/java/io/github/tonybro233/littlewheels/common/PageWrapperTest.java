package io.github.tonybro233.littlewheels.common;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PageWrapperTest {

    @Test
    void test0() {
        List<TestObj> contents = new ArrayList<>();
        for (int i = 1; i <= 25; i++) {
            contents.add(new TestObj("name-" + i, i, i));
        }

        Page<TestObj> page = PageWrapper.wrap(contents, PageRequest.of(0, 10));
        contents.clear();

        assertNotEquals(0, page.getContent().size(), "Should not modify the origin list");
    }

    @Test
    void test1() {
        List<TestObj> contents = new ArrayList<>();
        for (int i = 1; i <= 25; i++) {
           contents.add(new TestObj("name-" + i, i, i));
        }

        PageRequest pageRequest = PageRequest.of(2, 10, Sort.Direction.DESC, "val1");
        Page<TestObj> page = PageWrapper.wrap(contents, pageRequest);

        assertEquals(5, page.getContent().size());
        assertEquals(5, page.getContent().get(0).getVal1());
    }

    @Test
    void testNull() {
        List<TestObj> contents = new ArrayList<>();
        for (int i = 1; i <= 14; i++) {
            contents.add(new TestObj("name-" + i, i, i));
        }
        contents.add(null);
        contents.add(new TestObj("null", null, 15));
        for (int i = 16; i <= 25;i++) {
            contents.add(new TestObj("name-" + i, i, i));
        }

        PageRequest pageRequest = PageRequest.of(0, 10, Sort.Direction.ASC, "val1");
        Page<TestObj> page = PageWrapper.wrap(contents, pageRequest);

        assertNull(page.getContent().get(0));
        assertEquals("null", page.getContent().get(1).getName());
    }

    @Test
    void testNull2() {
        List<TestObj> contents = new ArrayList<>();
        contents.add(new TestObj("null", null, 0));
        for (int i = 1; i <= 14; i++) {
            contents.add(new TestObj("name-" + i, i, i));
        }

        Sort.Order order = new Sort.Order(Sort.Direction.ASC, "val1", Sort.NullHandling.NULLS_LAST);
        PageRequest pageRequest = PageRequest.of(1, 10, Sort.by(order));
        Page<TestObj> page = PageWrapper.wrap(contents, pageRequest);
        assertEquals("null", page.getContent().get(page.getContent().size() - 1).getName());

        Sort.Order order2 = new Sort.Order(Sort.Direction.DESC, "val1", Sort.NullHandling.NULLS_FIRST);
        PageRequest pageRequest2 = PageRequest.of(0, 10, Sort.by(order2));
        Page<TestObj> page2 = PageWrapper.wrap(contents, pageRequest2);
        assertEquals("null", page2.getContent().get(0).getName());
    }

    @Test
    void testMulti() {
        List<TestObj> contents = new ArrayList<>();
        contents.add(new TestObj("name-1", 1, 0));
        contents.add(new TestObj("name-2", 5, 2));
        contents.add(new TestObj("name-3", 4, 10));
        contents.add(new TestObj("name-4", 4, 9));
        contents.add(new TestObj("name-5", null, 0));
        contents.add(new TestObj("name-6", 2, 1));
        contents.add(new TestObj("name-7", 3, 2));
        contents.add(new TestObj("name-8", 3, 3));
        contents.add(new TestObj("name-9", 1, 1));
        contents.add(new TestObj("name-10", 0, 0));
        contents.add(new TestObj("name-11", 5, 10));
        contents.add(new TestObj("name-12", 3, null));

        Sort.Order order1 = new Sort.Order(Sort.Direction.ASC, "val1", Sort.NullHandling.NULLS_LAST);
        Sort.Order order2 = new Sort.Order(Sort.Direction.DESC, "val2", Sort.NullHandling.NULLS_FIRST);

        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by(order1, order2));
        Page<TestObj> page = PageWrapper.wrap(contents, pageRequest);
        List<TestObj> pageContent = page.getContent();

        assertEquals(10, pageContent.size());
        assertEquals("name-10", pageContent.get(0).getName());
        assertEquals("name-9", pageContent.get(1).getName());
        assertEquals("name-1", pageContent.get(2).getName());
        assertEquals("name-6", pageContent.get(3).getName());
        assertEquals("name-12", pageContent.get(4).getName());
        assertEquals("name-8", pageContent.get(5).getName());
        assertEquals("name-7", pageContent.get(6).getName());
        assertEquals("name-3", pageContent.get(7).getName());
        assertEquals("name-4", pageContent.get(8).getName());
        assertEquals("name-11", pageContent.get(9).getName());

        Page<TestObj> page2 = PageWrapper.wrap(contents, pageRequest.next());
        List<TestObj> pageContent2 = page2.getContent();

        assertEquals(2, pageContent2.size());
        assertEquals("name-2", pageContent2.get(0).getName());
        assertEquals("name-5", pageContent2.get(1).getName());
    }


    static class TestObj {

        private String name;

        private Integer val1;

        private Integer val2;

        public TestObj(String name, Integer val1, Integer val2) {
            this.name = name;
            this.val1 = val1;
            this.val2 = val2;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getVal1() {
            return val1;
        }

        public void setVal1(Integer val1) {
            this.val1 = val1;
        }

        public Integer getVal2() {
            return val2;
        }

        public void setVal2(Integer val2) {
            this.val2 = val2;
        }
    }
}