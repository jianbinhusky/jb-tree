/*
 * This work is licensed under a Creative Commons Attribution-NonCommercial 3.0 Unported License:
 *
 * http://creativecommons.org/licenses/by-nc/3.0/
 *
 * For alternative conditions contact the author.
 *
 * Copyright (c) 2011 "Robin Wenglewski <robin@wenglewski.de>"
 */
package de.rwhq.btree.run;

import de.rwhq.btree.BTree;
import de.rwhq.comparator.IntegerComparator;
import de.rwhq.io.rm.ResourceManager;
import de.rwhq.io.rm.ResourceManagerBuilder;
import de.rwhq.serializer.FixedStringSerializer;
import de.rwhq.serializer.IntegerSerializer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

/**
 * Iterates over the values of a tree
 */
public class PrintTree {

	private static Log LOG = LogFactory.getLog(PrintTree.class);

	public static void main(final String[] args) throws IOException {
		
		
		final File f = new File("/tmp/indexha");
		if(!f.exists())
			throw new IllegalArgumentException("File does not exist");

		final ResourceManager resourceManager = new ResourceManagerBuilder().file(f).build();
		final BTree<Integer, String> tree = BTree.create(resourceManager, IntegerSerializer.INSTANCE,
				FixedStringSerializer.INSTANCE_1000,
				IntegerComparator.INSTANCE);
		
		final Iterator<String> it = tree.getIterator();
		while(it.hasNext()){
			System.out.println(it.next());
		}
	}

}
