/*
 * This work is licensed under a Creative Commons Attribution-NonCommercial 3.0 Unported License:
 * http://creativecommons.org/licenses/by-nc/3.0/
 * For alternative conditions contact the author.
 *
 * Copyright (c) 2010 "Robin Wenglewski <robin@wenglewski.de>"
 */
package com.freshbourne.multimap.btree;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;

import com.freshbourne.multimap.MultiMap;
import com.freshbourne.multimap.MultiMapProvider;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;

public class BTreeProvider implements MultiMapProvider<Integer, String> {

	private final Injector injector;
	private static SecureRandom srand;
	private final String path;
	
	public BTreeProvider(String path) {
		this.path = path;
		injector = Guice.createInjector(new BTreeModule(path)); 
	}
	
	private static SecureRandom srand(){
		if(srand == null)
			srand = new SecureRandom(); 
		
		return srand;
	}
	
	public BTree<Integer, String> getInstance(){
		return injector.getInstance(Key.get(new TypeLiteral<BTree<Integer,String>>(){}));
	}
	
	
	
	@Override
	public MultiMap<Integer, String> createNewMultiMap() throws IOException {
		File f = new File(path);
		if(f.exists())
			f.delete();
		
		BTree<Integer, String> tree = getInstance();
		tree.initialize();
		return tree;
	}

	@Override
	public Integer createRandomKey() {
		return srand().nextInt();
	}
	
	

	@Override
	public String createRandomValue() {
		return (new BigInteger(130, srand())).toString(32);
	}


	@Override
	public Integer createMaxKey() {
		return Integer.MAX_VALUE;
	}


	@Override
	public Integer createMinKey() {
		return Integer.MIN_VALUE;
	}

}