package org.wsitm.rdbms.test;

import org.junit.Test;
import org.wsitm.rdbms.ehcache.CacheKit;

public class TestTableMeta {

    @Test
    public void test1() {
        CacheKit.removeAll("data");
    }

}
