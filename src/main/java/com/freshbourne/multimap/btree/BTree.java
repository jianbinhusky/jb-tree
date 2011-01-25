/*
 * Copyright (c) 2011 Robin Wenglewski <robin@wenglewski.de>
 *
 * This work is licensed under a Creative Commons Attribution-NonCommercial 3.0 Unported License:
 * http://creativecommons.org/licenses/by-nc/3.0/
 * For alternative conditions contact the author.
 */

/**
 * This work is licensed under a Creative Commons Attribution-NonCommercial 3.0 Unported License:
 * http://creativecommons.org/licenses/by-nc/3.0/
 * For alternative conditions contact the author.
 * 
 * (c) 2010 "Robin Wenglewski <robin@wenglewski.de>"
 */
package com.freshbourne.multimap.btree;

import com.freshbourne.io.ComplexPage;
import com.freshbourne.io.RawPage;
import com.freshbourne.multimap.MultiMap;
import com.freshbourne.multimap.btree.AdjustmentAction.ACTION;
import com.freshbourne.multimap.btree.InnerNode.PagePointerAndKey;
import com.google.inject.Inject;

import java.util.Comparator;
import java.util.List;


/**
 * An implementation of a Map that can hold more that one value for each key.
 * 
 * This class does all the rotating and balancing of the BTree so that Leafs and InnerNodes are not polluted by having
 * to create new nodes. This is done exclusively in this class.
 * 
 * @author Robin Wenglewski <robin@wenglewski.de>
 *
 * @param <K>
 * @param <V>
 */
public class BTree<K, V> implements MultiMap<K, V>, ComplexPage {

	private final LeafPageManager<K,V> leafPageManager;
	private final InnerNodeManager<K, V> innerNodeManager;
	private final Comparator<K> comparator;
	private final RawPage rawPage;
	
	private Node<K, V> root;
	
	/**
	 * If a leaf page has at least that many free slots left, we can move pointers to it
	 * from another node. This number is computed from the
	 * <tt>MAX_LEAF_ENTRY_FILL_LEVEL_TO_MOVE</tt> constant.
	 */
	private int minFreeLeafEntriesToMove;
	private boolean valid = false;
	
	
	/**
	 * @param rawPage for storing meta-information like size and depth of the tree and root page
	 * @param leafPageManager
	 * @param innerNodeManager
	 * @param comparator
	 */
	@Inject
	BTree(RawPage rawPage, LeafPageManager<K,V> leafPageManager, InnerNodeManager<K, V> innerNodeManager, Comparator<K> comparator) {
		this.leafPageManager = leafPageManager;
		this.innerNodeManager = innerNodeManager;
		this.comparator = comparator;
		this.rawPage = rawPage;
		
	}
	
	/* (non-Javadoc)
	 * @see com.freshbourne.multimap.MultiMap#size()
	 */
	@Override
	public int getNumberOfEntries() {
		return root.getNumberOfEntries();
	}

	/* (non-Javadoc)
	 * @see com.freshbourne.multimap.MultiMap#containsKey(java.lang.Object)
	 */
	@Override
	public boolean containsKey(K key) throws Exception {
		return root.containsKey(key);
	}

	/* (non-Javadoc)
	 * @see com.freshbourne.multimap.MultiMap#get(java.lang.Object)
	 */
	@Override
	public List<V> get(K key) throws Exception {
		return root.get(key);
	}

	/* (non-Javadoc)
	 * @see com.freshbourne.multimap.MultiMap#add(java.lang.Object, java.lang.Object)
	 */
	@Override
	public void add(K key, V value) {
		AdjustmentAction<K, V> result = recursivelyInsert(root, key, value, 0);
		
		// insert was successful
		if(result == null)
			return;
		
		// a new root must be created
		if(result.getAction() == ACTION.INSERT_NEW_NODE){
			
			// new root
			InnerNode<K, V> newRoot = innerNodeManager.createPage();
			newRoot.initRootState(result.getKeyPointer(), root.getId(), result.getPageId());
			root = newRoot;
		}
		
	}
	
	private AdjustmentAction<K, V> recursivelyInsert(Node<K, V> node, K key, V value, int depth){
		if(depth > MAX_BTREE_DEPTH)
			throw new RuntimeException("The depth of the B-Tree should not be greater then MAX_BTREE_DEPTH (" + MAX_BTREE_DEPTH + ")");
		
		// handle final case
		if (node instanceof LeafPage) {
			return insertInLeaf((LeafPage<K, V>) node, key, value);
		}
		
		// make sure node is right type
		if (!(node instanceof InnerNode)){
			throw new IllegalArgumentException("node must be of type Leaf or InnerNode!");
		}
		
		// handle normal InnerNodes
		InnerNode<K, V> thisInnerNode = (InnerNode<K, V>) node;
		
		// get a marker for the point where we descended
		PagePointerAndKey pos = thisInnerNode.getChildWithKeyAndPosition(key);
		
		
		return null;
	}

	private AdjustmentAction<K, V> insertInLeaf(LeafPage<K, V> thisLeaf, K key, V value) {
		
		// if leaf has enough space
		if (!thisLeaf.isFull()) {
			thisLeaf.add(key, value);
			return null;
		} 
		
		// if leaf does not have enough space but we can move some data to the next leaf
		if (thisLeaf.getNextLeafId() != null) {
			LeafPage<K, V> nextLeaf = leafPageManager.getPage(thisLeaf.getNextLeafId());
			
			if(nextLeaf.getRemainingEntries() >= minFreeLeafEntriesToMove){
				nextLeaf.prependEntriesFromOtherPage(thisLeaf, nextLeaf.getRemainingEntries() >> 1);
				
				// see on which page we will insert the value
				if(comparator.compare(key, thisLeaf.getLastKey()) > 0){
					nextLeaf.insert(key, value);
				} else {
					thisLeaf.insert(key, value);
				}
				
				return new AdjustmentAction<K, V>(ACTION.UPDATE_KEY, thisLeaf.getLastKeyPointer(), null);
			}
		}
		
		
		// if we have to allocate a new leaf
		
		// allocate new leaf
		LeafPage<K,V> newLeaf = leafPageManager.createPage();
		newLeaf.setNextLeafId(thisLeaf.getId());
		thisLeaf.setNextLeafId(newLeaf.rawPage().id());
			
		// newLeaf.setLastKeyContinuesOnNextPage(root.isLastKeyContinuingOnNextPage());
			
		// move half of the keys to new page
		newLeaf.prependEntriesFromOtherPage(thisLeaf,
				root.getNumberOfEntries() >> 1);

		// see on which page we will insert the value
		if (comparator.compare(key, thisLeaf.getLastKey()) > 0) {
			newLeaf.insert(key, value);
		} else {
			thisLeaf.insert(key, value);
		}

		return new AdjustmentAction<K, V>(ACTION.INSERT_NEW_NODE,
				thisLeaf.getLastKeyPointer(), newLeaf.rawPage().id());
	}

	/* (non-Javadoc)
	 * @see com.freshbourne.multimap.MultiMap#remove(java.lang.Object)
	 */
	@Override
	public void remove(K key) throws Exception {
		root.remove(key);
	}

	/* (non-Javadoc)
	 * @see com.freshbourne.multimap.MultiMap#remove(java.lang.Object, java.lang.Object)
	 */
	@Override
	public void remove(K key, V value) throws Exception {
		root.remove(key, value);
	}

	/* (non-Javadoc)
	 * @see com.freshbourne.multimap.MultiMap#clear()
	 */
	@Override
	public void clear() throws Exception {
		root.clear();
	}
	
	/**
	 * The maximum number of levels in the B-Tree. Used to prevent infinite loops when
	 * the structure is corrupted.
	 */
	private static final int MAX_BTREE_DEPTH = 50;
	
	/**
	 * If a leaf page is less full than this factor, it may be target of operations
	 * where entries are moved from one page to another. 
	 */
	private static final float MAX_LEAF_ENTRY_FILL_LEVEL_TO_MOVE = 0.75f;


	/* (non-Javadoc)
	 * @see com.freshbourne.io.ComplexPage#initialize()
	 */
	@Override
	public void initialize() {
		root = leafPageManager.createPage();
		
		this.minFreeLeafEntriesToMove = (int) (((LeafPage<K, V>)root).getMaximalNumberOfEntries() *
                (1 - MAX_LEAF_ENTRY_FILL_LEVEL_TO_MOVE)) + 2;
	}

	/* (non-Javadoc)
	 * @see com.freshbourne.io.ComplexPage#load()
	 */
	@Override
	public void load() {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see com.freshbourne.io.ComplexPage#isValid()
	 */
	@Override
	public boolean isValid() {
		return valid ;
	}

	/* (non-Javadoc)
	 * @see com.freshbourne.io.ComplexPage#rawPage()
	 */
	@Override
	public RawPage rawPage() {
		return rawPage;
	}

}
