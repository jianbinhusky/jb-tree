/*
 * This work is licensed under a Creative Commons Attribution-NonCommercial 3.0 Unported License:
 * http://creativecommons.org/licenses/by-nc/3.0/
 * For alternative conditions contact the author.
 *
 * Copyright (c) 2010 "Robin Wenglewski <robin@wenglewski.de>"
 */

package com.freshbourne.btree;

import com.freshbourne.comparator.StringComparator;
import com.freshbourne.serializer.FixedStringSerializer;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.testng.annotations.BeforeMethod;

import java.io.File;
import java.io.IOException;

import static org.testng.Assert.*;

public class BTreeFactoryTest {

    private static Injector     injector;
    private        BTreeFactory factory;
    private File f1 = new File("/tmp/factorytestFile");
    private File f2 = new File("/tmp/factorytestFile2");

    static {
        injector = Guice.createInjector(new BTreeModule());
    }

    @BeforeMethod
    public void setUp() {
        f1.delete();
        f2.delete();
        factory = injector.getInstance(BTreeFactory.class);
    }

    @org.testng.annotations.Test
    public void creation() {
        assertNotNull(factory);
    }

    @org.testng.annotations.Test
    public void factoryIsSingleton() {
        BTreeFactory factory2 = injector.getInstance(BTreeFactory.class);
        assertEquals(factory, factory2);
    }


    @org.testng.annotations.Test
    public void get() throws IOException {
        BTree rm = factory.get(f1,
                FixedStringSerializer.INSTANCE, FixedStringSerializer.INSTANCE,
                StringComparator.INSTANCE);

        BTree rm2 = factory.get(f1,
                FixedStringSerializer.INSTANCE, FixedStringSerializer.INSTANCE,
                StringComparator.INSTANCE);
        
        BTree rm3 = factory.get(f2,
                FixedStringSerializer.INSTANCE, FixedStringSerializer.INSTANCE,
                StringComparator.INSTANCE);


        assertNotNull(rm);
        assertEquals(rm, rm2);
        assertNotSame(rm, rm3);
    }

}
