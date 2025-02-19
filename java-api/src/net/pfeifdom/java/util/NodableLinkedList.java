/* NodableLinkedList.java -- Linked list implementation of the List interface
 * Copyright (C) 2023 James R. Pfeifer. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details (a copy is included in the
 * LICENSE file that accompanied this code).
 *
 * You should have received a copy of the GNU General Public License
 * along with this work.  If not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole
 * combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent
 * modules, and to copy and distribute the resulting executable under
 * terms of your choice, provided that you also meet, for each linked
 * independent module, the terms and conditions of the license of that
 * module.  An independent module is a module which is not derived from
 * or based on this library.  If you modify this library, you may extend
 * this exception to your version of the library, but you are not
 * obligated to do so.  If you do not wish to do so, delete this
 * exception statement from your version.
 *
 * Please contact James Pfeifer at james@pfeifdom.net if you need additional
 * information or have any questions.
 */

package net.pfeifdom.java.util;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.AbstractSequentialList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Doubly-linked list implementation of the {@code List} and {@code Deque}
 * interfaces, with the capability to perform insert and remove operations, at
 * any point in the list, in constant time. Implements all optional list
 * operations, and permits all elements (including {@code null}).
 * <p>
 * Implements all the same constructors and methods as the
 * {@code java.util.LinkedList} class, but includes additional methods:
 * {@code longSize} which returns the size of the list as a {@code long},
 * {@code node} which returns a new {@code NodableLinkedList.Node},
 * {@code linkedNodes} which returns the {@code NodableLinkedList.LinkedNodes}
 * object (list of nodes) backing the {@code NodableLinkedList}, and
 * {@code addNode}, {@code addNodeFirst}, {@code addNodeLast},
 * {@code getFirstNode}, {@code getLastNode}, {@code removeFirstNode},
 * {@code removeLastNode}, {@code addAll(Node)}, {@code listIterator(Node)} and
 * {@code subList(firstNode, lastNode)} which perform operations on a
 * {@code NodableLinkedList.Node}.
 * <p>
 * This implementation also has the capability to create reversed lists and
 * sublists. In addition, the method {@code mergeSort} has been implemented,
 * which can sort any size list.
 * <p>
 * This implementation behaves differently than the standard
 * {@code java.util.LinkedList} class when it comes to lists which contain more
 * than {@code Integer.MAX_VALUE} elements. The {@code size} method returns
 * Integer.MAX_VALUE when the list's {@code size > Integer.MAX_VALUE}. The
 * method {@code longSize} can be used to get the actual size of the list. The
 * {@code indexOf} and {@code lastIndexOf} methods return -1 if the
 * {@code index > Integer.MAX_VALUE}. The {@code toArray} methods throw an
 * IllegalStateException if the list's {@code size > Integer_MAX_VALUE}. The
 * {@code nextIndex} and {@code previousIndex} methods of the
 * {@code java.util.ListIterator} returned by {@code listIterator}, return -1 if
 * the {@code index > Integer_MAX_VALUE}.
 * <p>
 * A large {@code NodableLinkedList} can be as much as 33% - 34% larger than a
 * {@code java.util.LinkedList}. An empty {@code NodableLinkedList} is almost 5
 * times larger than an empty {@code java.util.LinkedList}.
 * <p>
 * Use {@code NodableLinkedList} in place of {@code java.util.LinkedList} when:
 * <ul>
 * <li>you require the capability to insert or remove elements, anywhere in the
 * list, in constant time
 * <li>you require the flexibility of traversing the list via the next and
 * previous pointers of the nodes which comprise the linked list
 * <li>you require the capability to create a reversed list prior to JDK 21
 * <li>the list could have more than {@code Integer.MAX_VALUE} elements
 * </ul>
 * <p>
 * There are two ways to visualize a {@code NodableLinkedList}: one as a list of
 * elements (the standard view), and the other as a list of nodes which contain
 * the elements and references to the previous and next nodes in a list. The
 * latter view is implemented by the inner class {@code LinkedNodes}. A
 * {@code NodableLinkedList} is backed by one and only one {@code LinkedNodes}
 * object. Method {@link #linkedNodes()} returns the {@code LinkedNodes} object
 * backing a {@code NodableLinkedList} instance.
 * <p>
 * All of the operations perform as could be expected for a doubly-linked list.
 * Operations that index into the list will traverse the list from the beginning
 * or the end, whichever is closer to the specified index. Indexes can only be
 * used to reference the first Integer.MAX_VALUE+1 elements.
 * <p>
 * <b>Example 1:</b> This example performs a Mergesort on a
 * {@code NodableLinkedList} of integers. The algorithm used in this example is
 * an adaptation of an algorithm by Simon Tatham (<a href=
 * "https://www.chiark.greenend.org.uk/~sgtatham/algorithms/listsort.html">
 * SimonTathamMergesort</a>). All {@code NodableLinkedList} operations in this
 * example perform in constant time.
 * 
 * <pre>
 * {@code
 * static void nodableLinkedListMergeSort(NodableLinkedList<Integer> list) {
 *       
 *     NodableLinkedList.Node<Integer> p, q, e, tail;
 *     long subListSize, nMerges, pSize, qSize;
 *     
 *     if (list == null) return; // no list to sort
 *      
 *     subListSize = 1; // initial size of the p and q lists;
 *                      // after each pass through the list, subListSize is doubled
 *      
 *     // make as many passes through the list as necessary until the list is sorted
 *     while (true) {
 *
 *         tail = null; // no nodes have been merged yet
 *                      // the merged nodes are placed at the front of the list, and
 *                      // tail is the last node of the merged (sorted) nodes
 *          
 *         nMerges = 0; // initialize the number of merges performed in this pass
 *             
 *         // merge successive sublists (p and q lists) of the list, of size 'subListSize',
 *         // until the entire list has been processed
 *         p = list.getFirstNode(); // start the p list at the beginning of the list
 *         while (p != null) {
 *             // p list is not empty; there's more nodes to merge
 *           
 *             nMerges++; // count the number of merges performed in this pass
 *              
 *             // find the start of the q list
 *             // which is 'subListSize' nodes from the start of the p list
 *             q = p; // set the start of the q list to the start of the p list
 *             pSize = 0;
 *             for (long i = 0; i < subListSize; i++) {
 *                 pSize++; // determine the exact size of the p list
 *                 q = q.next(); // move the start of the q list forward in the list
 *                 if (q == null) break; // quit if the end of the list has been reached
 *             }
 *             qSize = subListSize; // assume the q list size is the maximum possible size
 *              
 *             // we have two lists where p is the first node of the first list (p list),
 *             // and q is the first node (or null if the list is empty) of the second
 *             // list (q list); we also know the exact size of the p list
 *              
 *             // merge the p and q lists
 *             while (pSize > 0 || (qSize > 0 && q != null)) {
 *              
 *                 // decide where the next element (e) to merge comes from
 *                 // (the p list or the q list)
 *                 if (pSize == 0) {
 *                     // p list is empty; e must come from the q list
 *                     e = q; q = q.next(); qSize--;
 *                 } else if (qSize == 0 || q == null) {
 *                     // q list is empty; e must come from the p list
 *                     e = p; p = p.next(); pSize--;
 *                 } else if (p.compareTo(q) <= 0) {
 *                     // p's element is lower (or equal); e must come from the p list
 *                     e = p; p = p.next(); pSize--;
 *                 } else {
 *                     // q's element is lower; e must come from the q list
 *                     e = q; q = q.next(); qSize--;
 *                 }
 *                  
 *                 // remove e then add it back to the front of the list
 *                 e.remove(); // remove e from the list
 *                 if (tail == null) {
 *                     // e is the first node to be added to the merged list
 *                     list.addNodeFirst(e);
 *                 } else {
 *                     // add e after the last node of the merged list
 *                     e.addAfter(tail);
 *                 }
 *                 tail = e; // tail is the last node of the merged (sorted) list
 *                 
 *             } // loop until all elements in the p and q lists have been merged
 *              
 *             p = q; // the next p list starts where the q list ended
 *             
 *         } // loop to perform a merge of the next p and q sublists
 *           // until the entire list has been processed
 *          
 *         if (nMerges <= 1) break; // list is completely sorted (or empty)
 *                                  // if only one merge (or zero merges)
 *                                  // was performed on this pass
 *          
 *         subListSize *= 2; // make another pass with the p and q lists twice the size
 *     }
 * }
 * }
 * </pre>
 * <p>
 * <b>Example 2:</b> This example performs an Arrays.sort on a
 * {@code NodableLinkedList} of strings.
 * 
 * <pre>
 * {@code
 * static void nodableLinkedListArraysSort(NodableLinkedList<String> list) {
 * 
 *     if (list == null) return; // no list to sort
 *
 *     // copy the nodes to an array and sort the array
 *     NodableLinkedList.Node<String>[]
 *             sortedNodes = list.linkedNodes().toArray(new NodableLinkedList.Node[0]);
 *     Arrays.sort(sortedNodes);
 *     
 *     // effectively copy the sorted nodes in the array back into the list
 *     // by rearranging the nodes in the list
 *     NodableLinkedList.Node<String> cursor = list.getFirstNode();
 *     for (NodableLinkedList.Node<String> sortedNode: sortedNodes) {
 *         cursor.swapWith(sortedNode); // move the sortedNode to its position in the list
 *         cursor = sortedNode.next(); // basically cursor = cursor.next() since
 *                                     // sortedNode has replaced cursor's position in the list
 *     }
 * }
 * }
 * </pre>
 * <p>
 * <strong>Note that this implementation is not synchronized.</strong> If
 * multiple threads access a linked list concurrently, and at least one of the
 * threads modifies the list structurally, it <i>must</i> be synchronized
 * externally (A structural modification is any operation that adds or deletes
 * one or more elements (or nodes); merely setting the value of an element is
 * not a structural modification.) This is typically accomplished by
 * synchronizing on some object that naturally encapsulates the list.
 *
 * If no such object exists, the list should be "wrapped" using the
 * {@link java.util.Collections#synchronizedList} method. This is best done at
 * creation time, to prevent accidental unsynchronized access to the list:
 * 
 * <pre>
 *   List list = Collections.synchronizedList(new NodableLinkedList(...));
 * </pre>
 * <p>
 * The iterators returned by this class's {@code iterator} and
 * {@code listIterator} methods are <i>fail-fast</i>: if the
 * {@code NodableLinkedList} is structurally modified at any time after the
 * iterator is created, in any way except through the Iterator's own
 * {@code remove} or {@code add} methods, the iterator will throw a {@code
 * ConcurrentModificationException}. Thus, in the face of concurrent
 * modification, the iterator fails quickly and cleanly, rather than risking
 * arbitrary, non-deterministic behavior at an undetermined time in the future.
 * <p>
 * Note that the fail-fast behavior of an iterator cannot be guaranteed as it
 * is, generally speaking, impossible to make any hard guarantees in the
 * presence of unsynchronized concurrent modification. Fail-fast iterators throw
 * {@code ConcurrentModificationException} on a best-effort basis. Therefore, it
 * would be wrong to write a program that depended on this exception for its
 * correctness: <i>the fail-fast behavior of iterators should be used only to
 * detect bugs.</i>
 * <p>
 * Instead of using a {@code ListIterator}, consider iterating over the
 * list via {@code Nodes}. For example:
 * <pre>
 * {@code
 *     // list is a NodableLinkedList<Integer>
 *     NodableLinkedList.Node<Integer> node = list.getFirstNode();
 *     while (node != null) {
 *         System.out.println(node.element());
 *         node = node.next();
 *     }
 * }
 * </pre>
 *
 * @author James Pfeifer
 * @param <E> the type of elements held in this collection
 * @see java.util.LinkedList
 * @see List
 * @see Node
 * @since JDK 1.8
 */
public class NodableLinkedList<E>
    extends AbstractSequentialList<E>
    implements List<E>, Deque<E>, Cloneable, Serializable
{

    private static final long serialVersionUID = 6932924870547825064L;

    /**
     * The doubly-linked list of nodes which backs this NodableLinkedList.
     */
    private transient LinkedNodes linkedNodes;

    /**
     * Constructs an empty list.
     */
    public NodableLinkedList() {
        this.linkedNodes = new LinkedNodes();
    }

    /**
     * Constructs a list containing the elements of the specified collection, in the
     * order they are returned by the collection's iterator.
     *
     * @param collection the collection whose elements are to be placed into this
     *                   list
     * @throws NullPointerException if the specified collection is {@code null}
     */
    public NodableLinkedList(Collection<? extends E> collection) {
        this();
        addAll(collection);
    }
    
    /**
     * Constructs a reversed {@code NodableLinkedList}.
     * 
     * @param linkedNodes {@code LinkNodes} of the base {@code NodableLinkedList}
     *                    that backs this reversed {@code NodableLinkedList}
     */
    private NodableLinkedList(LinkedNodes linkedNodes) {
        this.linkedNodes = new ReversedLinkedNodes(linkedNodes);
    }

    /**
     * Returns a shallow copy of this {@code NodableLinkedList}. (The elements
     * themselves are not cloned.)
     *
     * @return a shallow copy of this {@code NodableLinkedList} instance
     */
    @Override
    public Object clone() {
        try {
            @SuppressWarnings("unchecked")
            final NodableLinkedList<E> clone = (NodableLinkedList<E>) super.clone();
            clone.linkedNodes = clone.new LinkedNodes();
            clone.addAll(this);
            return clone;               
        } catch (CloneNotSupportedException e) {
            throw new AssertionError("Not Cloneable? "+e.getMessage(), e);
        }
    }    

    // serialize
    /**
     * Save the state of this {@code NodableLinkedList} instance to a stream (that
     * is, serialize it).
     *
     * @param stream stream to save the state of this instance
     * @throws IOException I/O error occurred while writing to stream
     * @serialData The size of the list (the number of elements it contains) is
     *             emitted (long), followed by all of its elements (each an Object)
     *             in the proper order.
     */
    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject();
        stream.writeLong(longSize());
        for (LinkNode<E> node = getFirstNode(); node != null;
                node = linkedNodes().iGetNodeAfter(node)) {
            stream.writeObject(node.element());
        }
    }

    // deserialize
    /**
     * Reconstitutes this {@code NodableLinkedList} instance from a stream (that is
     * deserialize it).
     * 
     * @param stream stream to be deserialized
     * @throws ClassNotFoundException Class of a serialized object cannot be found.
     * @throws IOException            I/O error occurred while reading from stream
     */
    private void readObject(ObjectInputStream stream)
            throws IOException, ClassNotFoundException
    {
        stream.defaultReadObject();
        this.linkedNodes = new LinkedNodes();
        long nodeCount = stream.readLong();
        while (nodeCount-- > 0) {
            @SuppressWarnings("unchecked")
            final E element = (E)stream.readObject();
            addLast(element);			
        }
    }

    /**
     * Returns the protected int modCount inherited from class
     * java.util.AbstractList.
     * 
     * @return returns the modCount
     */
    int modCount() {
        return this.modCount;
    }

    /**
     * Returns the list of nodes which back this {@code NodableLinkedList}.
     * 
     * @return the list of nodes which back this {@code NodableLinkedList}
     */
    public LinkedNodes linkedNodes() {
        return this.linkedNodes;
    }

    /**
     * Returns a new {@code Node} containing a {@code null} element. The node is
     * not linked to any list.
     * 
     * @param <T> type of element to be contained within the returned {@code Node}
     * @return an unlinked {@code LinkNode} containing a {@code null} element
     */
    public static <T> LinkNode<T> node() {
        return new LinkNode<>();
    }

    /**
     * Returns a new {@code Node} containing the specified element which can be
     * {@code null}. The node is not linked to any list.
     * 
     * @param <T>     type of element to be contained within the returned
     *                {@code Node}
     * @param element element to be contained within the returned {@code Node}
     * @return an unlinked {@code LinkNode} containing the specified element
     */
    public static <T> LinkNode<T> node(T element) {
        return new LinkNode<>(element);
    }
    
    /**
     * Returns true if the specified index is the index of an existing node.
     * 
     * @param index the index to check
     * @param size  the size of the list
     * @return true if {@code index >= 0 && index < size}
     */
    private static boolean isNodeIndex(long index, long size) {
        return index >= 0L && index < size;
    }
    
    /**
     * Returns true if the specified index is the index of a valid position for an
     * iterator or an add operation.
     * 
     * @param index the index to check
     * @param size  the size of the list
     * @return true if {@code index >= 0 && index <= size}
     */
    private static boolean isPositionIndex(long index, long size) {
        return index >= 0L && index <= size;
    }
    
    /**
     * Constructs an IndexOutOfBoundsException detail message.
     * 
     * @param index the index that is out of bounds
     * @return IndexOutOfBoundsException detail message
     */
    private static String outOfBoundsMsg(long index) {
        return "index=" + index;
    }
    
    /**
     * Constructs an IndexOutOfBoundsException detail message.
     * 
     * @param index the index that is out of bounds
     * @param size  the size of the list
     * @return IndexOutOfBoundsException detail message
     */
    private static String outOfBoundsMsg(long index, long size) {
        return "index=" + index + ", size=" + size;
    }
    
    /**
     * Check if the specified index is a valid index to reference an existing node
     * in a list of size 'size'.
     * 
     * @param index the index to be checked
     * @param size  the size of the list
     * @throws IndexOutOfBoundsException if {@code index < 0 || index >= size}
     */
    private static void checkNodeIndex(long index, long size) {
        if (!isNodeIndex(index, size)) {
            throw new IndexOutOfBoundsException(outOfBoundsMsg(index, size));
        }
    }
    
    /**
     * Check if the specified index is a valid position in a list of size 'size'.
     * 
     * @param index the index to be checked
     * @param size  the size of the list
     * @throws IndexOutOfBoundsException if {@code index < 0 || index > size}
     */
    private static void checkPositionIndex(long index, long size) {
        if (!isPositionIndex(index, size)) {
            throw new IndexOutOfBoundsException(outOfBoundsMsg(index, size));
        }
    }
    
    /**
     * Returns true if the specified (not null) Node is linked to a list.
     * 
     * @param node the Node to check
     * @return true if the specified (not null) Node is linked
     */
    private static boolean isLinkedNode(Node<?> node) {
        return node != null && node.isLinked();
    }
    
    /**
     * Returns true if the specified (not null) Node is unlinked (not already linked
     * to a list).
     * 
     * @param node the Node to check
     * @return true if the specified (not null) Node is unlinked (not already
     *         linked)
     */
    private static boolean isUnLinkedNode(Node<?> node) {
        return node != null && !node.isLinked();
    }
    
    /**
     * Constructs a 'Node is not an element of a list' message.
     * 
     * @return 'Node is not an element of a list' message
     */
    private static String nodeIsNotLinkedMsg() {
        return "Specified node is null or not a element of a list";
    }
    
    /**
     * Constructs a 'Node is already an element of a list' message.
     * 
     * @return 'Node is already an element of a list' message
     */
    private static String nodeIsNotUnLinkedMsg() {
        return "Specified node is null or already an element of a list";
    }
    
    /**
     * Check if the specified Node is linked.
     * 
     * @param node the Node to check
     * @throws IllegalArgumentException if the specified Node is null or not linked
     */
    private static void checkNodeIsLinked(Node<?> node) {
        checkNodeIsLinked(node, nodeIsNotLinkedMsg());
    }
    
    /**
     * Check if the specified Node is linked.
     * 
     * @param node the Node to check
     * @param msg  the IllegalArgumentException message
     * @throws IllegalArgumentException if the specified Node is null or not linked
     */
    private static void checkNodeIsLinked(Node<?> node, String msg) {
        if (!isLinkedNode(node)) throw new IllegalArgumentException(msg);
    }
    
    /**
     * Check if the specified Node is unlinked (not already linked).
     * 
     * @param node the Node to check
     * @throws IllegalArgumentException if the specified Node is null or already
     *                                  linked
     */
    private static void checkNodeIsUnLinked(Node<?> node) {
        checkNodeIsUnLinked(node, nodeIsNotUnLinkedMsg());
    }
    
    /**
     * Check if the specified Node is unlinked (not already linked).
     * 
     * @param node the Node to check
     * @param msg  the IllegalArgumentException message
     * @throws IllegalArgumentException if the specified Node is null or already
     *                                  linked
     */
    private static void checkNodeIsUnLinked(Node<?> node, String msg) {
        if (!isUnLinkedNode(node)) throw new IllegalArgumentException(msg);
    }
    
    /**
     * Check if a List can fit in an array.
     * 
     * @param size the size of the list
     * @throws IllegalStateException if the list is too large to fit in an array
     */
    private static void checkListCanFitInAnArray(long size) {
        if (size > Integer.MAX_VALUE) {
            throw new IllegalStateException("list size (" + size + ") is too large to fit in an array");
        }
    }

    /**
     * Returns the number of elements in this list. If this list contains more than
     * {@code Integer.MAX_VALUE} elements, {@code Integer.MAX_VALUE} is returned.
     *
     * @return the number of elements in this list
     */
    @Override
    public int size() {
        return linkedNodes().size();
    }

    /**
     * Returns the number of elements in this list.
     *
     * @return the number of elements in this list
     */
    public long longSize() {
        return linkedNodes().longSize();
    }
    
    /**
     * Returns {@code true} if this list contains no elements.
     *
     * @return {@code true} if this list contains no elements
     */        
    @Override
    public boolean isEmpty() {
        return linkedNodes().isEmpty();
    }    

    /**
     * Appends the specified node to the end of this list. The specified node must
     * not already belong to a list.
     * <p>
     * This method is equivalent to {@link #addNodeLast}.
     *
     * @param node {@code Node} to be appended to the end of this list
     * @throws IllegalArgumentException if the specified node is {@code null} or
     *                                  already a node of a list
     */
    public void addNode(Node<E> node) {
        addNodeLast(node);
    }

    /**
     * Inserts the specified node at the beginning of this list. The specified node
     * must not already belong to a list.
     *
     * @param node the {@code Node} to be inserted at the beginning of this list
     * @throws IllegalArgumentException if the specified node is {@code null} or
     *                                  already a node of a list
     */
    public void addNodeFirst(Node<E> node) {
        linkedNodes().addNodeFirst(node);
    }

    /**
     * Appends the specified node to the end of this list. The specified node must
     * not already belong to a list.
     * <p>
     * This method is equivalent to {@link #addNode}.
     *
     * @param node {@code Node} to be appended to the end of this list
     * @throws IllegalArgumentException if the specified node is {@code null} or
     *                                  already a node of a list
     */
    public void addNodeLast(Node<E> node) {
        linkedNodes().addNodeLast(node);
    }
    
    /**
     * Returns the first node of this list, or {@code null} if this list is empty.
     * 
     * A reversed {@code LinkNode} is returned for reversed lists.
     *
     * @return the first node of this list, or {@code null} if this list is empty
     */
    public LinkNode<E> getFirstNode() {
        return linkedNodes().iGetFirstNode();
    }

    /**
     * Returns the last node of this list, or {@code null} if this list is empty.
     * 
     * A reversed {@code LinkNode} is returned for reversed lists.
     *
     * @return the last node of this list, or {@code null} if this list is empty
     */
    public LinkNode<E> getLastNode() {
        return linkedNodes().iGetLastNode();
    }

    /**
     * Removes and returns the first node of this list, or returns {@code null} if
     * this list is empty.
     * 
     * Returns the removed node or {@code null} if this list is empty
     *
     * @return the first node of this list that was removed, or {@code null} if this
     *         list is empty
     */
    public LinkNode<E> removeFirstNode() {
        return linkedNodes().removeFirstNode();
    }

    /**
     * Removes and returns the last node of this list, or returns {@code null} if
     * this list is empty.
     * 
     * Returns the removed node or {@code null} if this list is empty
     *
     * @return the last node of this list that was removed, or {@code null} if this
     *         list is empty
     */
    public LinkNode<E> removeLastNode() {
        return linkedNodes().removeLastNode();
    }

    /**
     * Removes all of the elements from this list.
     */
    @Override
    public void clear() {
        linkedNodes().clear();
    }

    /**
     * Returns {@code true} if this list contains the specified object (element).
     * More formally, returns {@code true} if and only if this list contains at
     * least one element such that
     * {@code (object==null ? element==null : object.equals(element))}.
     *
     * @param object {@code Object} (element) whose presence in this list is to be
     *               tested
     * @return {@code true} if this list contains the specified object (element)
     */
    @Override
    public boolean contains(Object object) {
        LinkNode<E> node = getFirstNode();
        if (object == null) {
            for (; node != null; node = linkedNodes().iGetNodeAfter(node)) {
                if (node.element() == null) return true;          
            }
        } else {
            for (; node != null; node = linkedNodes().iGetNodeAfter(node)) {
                if (object.equals(node.element())) return true;           
            }
        }
        return false;
    }    

    /**
     * Returns the index of the first occurrence of the specified object (element)
     * in this list, or -1 if there is no such index (this list does not contain the
     * element or the {@code index > Integer.MAX_VALUE}). More formally, returns the
     * lowest index {@code i} such that
     * {@code (object==null ? get(i)==null : object.equals(get(i)))}, or -1 if there
     * is no such index.
     *
     * @param object {@code Object} (element) to search for
     * @return the index of the first occurrence of the specified object (element)
     *         in this list, or -1 if there is no such index.
     */
    @Override
    public int indexOf(Object object) {
        long index = 0;
        LinkNode<E> node = getFirstNode();
        if (object == null) {
            for (; node != null; index++, node = linkedNodes().iGetNodeAfter(node)) {
                if (node.element() == null) {
                    return (index > Integer.MAX_VALUE) ? -1 : (int)index;			
                }
            }
        } else {
            for (; node != null; index++, node = linkedNodes().iGetNodeAfter(node)) {
                if (object.equals(node.element())) {
                    return (index > Integer.MAX_VALUE) ? -1 : (int)index;			
                }
            }
        }
        return -1;
    }

    /**
     * Returns the index of the last occurrence of the specified object (element) in
     * this list, or -1 if there is no such index (this list does not contain the
     * element or the {@code index > Integer.MAX_VALUE}). More formally, returns the
     * highest index {@code i} such that
     * {@code (object==null ? get(i)==null : object.equals(get(i)))}, or -1 if there
     * is no such index.
     *
     * @param object {@code Object} (element) to search for
     * @return the index of the last occurrence of the specified object (element) in
     *         this list, or -1 if there is no such index.
     */
    @Override
    public int lastIndexOf(Object object) {
        long index = longSize() - 1L;
        LinkNode<E> node = getLastNode();
        if (object == null) {
            for (; node != null; index--, node = linkedNodes().iGetNodeBefore(node)) {
                if (node.element() == null) {
                    return (index > Integer.MAX_VALUE) ? -1 : (int)index;			
                }
            }
        } else {
            for (; node != null; index--, node = linkedNodes().iGetNodeBefore(node)) {
                if (object.equals(node.element())) {
                    return (index > Integer.MAX_VALUE) ? -1 : (int)index;			
                }
            }
        }
        return -1;
    }

    /**
     * Appends all of the elements in the specified collection to the end of this
     * list, in the order that they are returned by the specified collection's
     * iterator.
     * <p>
     * The behavior of this operation is undefined if the specified collection is
     * modified while the operation is in progress. (Note that this will occur if
     * the specified collection is this list, and it's nonempty.)
     *
     * @param collection collection containing elements to be added to this list
     * @return {@code true} if this list changed as a result of the call
     * @throws NullPointerException if the specified collection is {@code null}
     */
    @Override
    public boolean addAll(Collection<? extends E> collection) {
        final long initialSize = longSize();
        for (E element: collection) addLast(element);
        return longSize() != initialSize;
    }

    /**
     * Inserts all of the elements in the specified collection into this list,
     * starting at the specified position. Shifts the element currently at that
     * position (if any) and any subsequent elements to the right (increases their
     * indices). The new elements will appear in the list in the order that they are
     * returned by the specified collection's iterator. if the specified
     * {@code index == longSize()}, the elements will be appended to the end of this
     * list.
     * 
     * Note that {@code addAll(longSize(), Collection)} is identical in function to
     * {@code addAll(Collection)}.
     * <p>
     * The behavior of this operation is undefined if the specified collection is
     * modified while the operation is in progress. (Note that this will occur if
     * the specified collection is this list, and it's nonempty.)
     *
     * @param index      position where to insert the first element from the
     *                   specified collection
     * @param collection collection containing elements to be added to this list
     * @return {@code true} if this list changed as a result of the call
     * @throws IndexOutOfBoundsException if the specified index is out of range
     *                                   {@code (index < 0 || index > longSize())}
     * @throws NullPointerException      if the specified collection is {@code null}
     */
    @Override
    public boolean addAll(int index, Collection<? extends E> collection) {
        checkPositionIndex(index, longSize());
        if (index == longSize()) return addAll(collection);
        final long initialSize = longSize();
        final LinkNode<E> beforeThisNode = linkedNodes().iGetNode(index);
        for (E element: collection) {
            linkedNodes().iAddNodeBefore(node(element), beforeThisNode);
        }
        return longSize() != initialSize;
    }

    /**
     * Inserts all of the elements in the specified collection into this list,
     * before the specified node. Shifts the element of the specified node and any
     * subsequent elements to the right (increases their indices). The new elements
     * will appear in the list in the order that they are returned by the specified
     * collection's iterator. if the specified node is {@code null}, the elements
     * will be appended to the end of this list.
     * 
     * Note that {@code addAll(null, Collection)} is identical in function to
     * {@code addAll(Collection)}.
     * <p>
     * The behavior of this operation is undefined if the specified collection is
     * modified while the operation is in progress. (Note that this will occur if
     * the specified collection is this list, and it's nonempty.)
     *
     * @param node       {@code Node} the specified collection is to be inserted
     *                   before
     * @param collection collection containing elements to be added to this list
     * @return {@code true} if this list changed as a result of the call
     * @throws IllegalArgumentException if the specified node is not linked to this
     *                                  list
     * @throws NullPointerException     if the specified collection is {@code null}
     */
    public boolean addAll(Node<E> node, Collection<? extends E> collection) {
        if (node == null) return addAll(collection);
        linkedNodes().checkListContainsNode(node);
        final LinkNode<E> beforeThisNode = node.linkNode();
        final long initialSize = longSize();
        for (E element: collection) {
            linkedNodes().iAddNodeBefore(node(element), beforeThisNode);
        }
        return longSize() != initialSize;
    }

    /**
     * Appends the specified element to the end of this list.
     *
     * @param element element to be appended to this list
     * @return {@code true} (as specified by {@code Collection#add})
     */
    @Override
    public boolean add(E element) {
        addLast(element);
        return true;
    }    

    /**
     * Inserts the specified element at the specified position in this list. Shifts
     * the element currently at that position (if any) and any subsequent elements
     * to the right (adds one to their indices). if the specified
     * {@code index == longSize()}, the specified element will be appended to the
     * end of this list.
     *
     * @param index   position where the specified element is to be inserted
     * @param element element to be inserted
     * @throws IndexOutOfBoundsException if the specified index is out of range
     *                                   {@code (index < 0 || index > longSize())}
     */
    @Override
    public void add(int index, E element) {
        linkedNodes().add(index, node(element));
    }	

    /**
     * Inserts the specified element at the beginning of this list.
     *
     * @param element the element to add
     */
    @Override
    public void addFirst(E element) {
        linkedNodes().addFirst(node(element));
    }

    /**
     * Appends the specified element to the end of this list.
     *
     * @param element the element to add
     */
    @Override
    public void addLast(E element) {
        linkedNodes().addLast(node(element));
    }

    /**
     * Retrieves, but does not remove, the head (first element) of this list.
     * 
     * @return the head of this list
     * @throws NoSuchElementException if this list is empty
     */
    @Override
    public E element() {
        return getFirst();
    }

    /**
     * Returns the element at the specified position in this list.
     *
     * @param index index of the element to return
     * @return the element at the specified position in this list
     * @throws IndexOutOfBoundsException if the specified index is out of range
     *                                   {@code (index < 0 || index >= longSize())}
     */
    @Override
    public E get(int index) {
        return linkedNodes().get(index).element();
    }	

    /**
     * Returns the first element in this list.
     *
     * @return the first element in this list
     * @throws NoSuchElementException if this list is empty
     */
    @Override
    public E getFirst() {
        return linkedNodes().getFirst().element();
    }

    /**
     * Returns the last element in this list.
     *
     * @return the last element in this list
     * @throws NoSuchElementException if this list is empty
     */
    @Override
    public E getLast() {
        return linkedNodes().getLast().element();
    }
    
    /**
     * Returns {@code true} if this list is a reverse-ordered view of the base
     * {@code NodableLinkedList}.
     * 
     * @return {@code true} if this list is a reverse-ordered view of the base
     *         {@code NodableLinkedList}
     */
    public boolean isReversed() {
        return linkedNodes().isReversed();
    }

    /**
     * Adds the specified element as the tail (last element) of this list.
     *
     * @param element the element to add
     * @return {@code true} (as specified by {@link java.util.Queue#offer})
     */
    @Override
    public boolean offer(E element) {
        return offerLast(element);
    }

    /**
     * Inserts the specified element at the front of this list.
     *
     * @param element the element to insert
     * @return {@code true} (as specified by {@link Deque#offerFirst})
     */
    @Override
    public boolean offerFirst(E element) {
        addFirst(element);
        return true;
    }

    /**
     * Inserts the specified element at the end of this list.
     *
     * @param element the element to insert
     * @return {@code true} (as specified by {@link Deque#offerLast})
     */
    @Override
    public boolean offerLast(E element) {
        addLast(element);
        return true;
    }    

    /**
     * Retrieves, but does not remove, the head (first element) of this list.
     * 
     * @return the head of this list, or {@code null} if this list is empty
     */
    @Override
    public E peek() {
        return peekFirst();
    }

    /**
     * Retrieves, but does not remove, the first element of this list, or returns
     * {@code null} if this list is empty.
     *
     * @return the first element of this list, or {@code null} if this list is empty
     */
    @Override
    public E peekFirst() {
        return (longSize() == 0L) ? null : getFirst();
    }

    /**
     * Retrieves, but does not remove, the last element of this list, or returns
     * {@code null} if this list is empty.
     *
     * @return the last element of this list, or {@code null} if this list is empty
     */
    @Override
    public E peekLast() {
        return (longSize() == 0L) ? null : getLast();
    }

    /**
     * Retrieves and removes the head (first element) of this list
     * 
     * @return the head of this list, or {@code null} if this list is empty
     */
    @Override
    public E poll() {
        return pollFirst();
    }

    /**
     * Retrieves and removes the first element of this list, or returns {@code null}
     * if this list is empty.
     *
     * @return the first element of this list, or {@code null} if this list is empty
     */
    @Override
    public E pollFirst() {
        return (longSize() == 0L) ? null : removeFirst();
    }

    /**
     * Retrieves and removes the last element of this list, or returns {@code null}
     * if this list is empty.
     *
     * @return the last element of this list, or {@code null} if this list is empty
     */
    @Override
    public E pollLast() {
        return (longSize() == 0L) ? null : removeLast();
    }    

    /**
     * Pops an element from the stack represented by this list. In other words,
     * removes and returns the first element of this list.
     * <p>
     * This method is equivalent to {@link #removeFirst()}.
     *
     * @return the element at the front of this list (which is the top of the stack
     *         represented by this list)
     * @throws NoSuchElementException if this list is empty
     */
    @Override
    public E pop() {
        return removeFirst();
    }

    /**
     * Pushes an element onto the stack represented by this list. In other words,
     * inserts the element at the front of this list.
     * <p>
     * This method is equivalent to {@link #addFirst}.
     *
     * @param element the element to push
     */
    @Override
    public void push(E element) {
        addFirst(element);
    }    

    /**
     * Removes and returns the head (first element) of this list.
     *
     * @return the head of this list
     * @throws NoSuchElementException if this list is empty
     */
    @Override
    public E remove() {
        return removeFirst();
    }	

    /**
     * Removes and returns the element at the specified position in this list.
     * Shifts any subsequent elements to the left (subtracts one from their
     * indices).
     *
     * @param index the index of the element to be removed
     * @return the element previously at the specified position
     * @throws IndexOutOfBoundsException if the specified index is out of range
     *                                   {@code (index < 0 || index >= longSize())}
     */
    @Override
    public E remove(int index) {
        return linkedNodes().remove(index).element();
    }	

    /**
     * Removes and returns the first element from this list.
     *
     * @return the first element from this list
     * @throws NoSuchElementException if this list is empty
     */
    @Override
    public E removeFirst() {
        return linkedNodes().removeFirst().element();
    }

    /**
     * Removes and returns the last element from this list.
     *
     * @return the last element from this list
     * @throws NoSuchElementException if this list is empty
     */
    @Override
    public E removeLast() {
        return linkedNodes().removeLast().element();
    }

    /**
     * Removes, if present, the first occurrence of the specified object (element)
     * from this list. If this list does not contain the specified element, it is
     * unchanged. More formally, removes the element with the lowest index {@code i}
     * such that {@code (object==null ? get(i)==null : object.equals(get(i)))} (if
     * such an element exists). Returns {@code true} if this list contained the
     * specified element (or equivalently, if this list changed as a result of the
     * call).
     *
     * @param object {@code Object} (element) to be removed from this list, if
     *               present
     * @return {@code true} if this list contained the specified object (element)
     */
    @Override
    public boolean remove(Object object) {
        return removeFirstOccurrence(object);
    }

    /**
     * Removes the first occurrence of the specified object (element) in this list
     * (when traversing the list from head to tail). If the list does not contain
     * the specified element, it is unchanged.
     *
     * @param object {@code Object} (element) to be removed from this list, if
     *               present
     * @return {@code true} if the list contained the specified object (element)
     */
    @Override
    public boolean removeFirstOccurrence(Object object) {
        if (object == null) {
            for (LinkNode<E> node = getFirstNode(); node != null;
                    node = linkedNodes().iGetNodeAfter(node)) {
                if (node.element() == null) {
                    linkedNodes().iRemoveNode(node);
                    return true;
                }
            }
        } else {
            for (LinkNode<E> node = getFirstNode(); node != null;
                    node = linkedNodes().iGetNodeAfter(node)) {
                if (object.equals(node.element())) {
                    linkedNodes().iRemoveNode(node);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Removes the last occurrence of the specified object (element) in this list
     * (when traversing the list from head to tail). If the list does not contain
     * the specified element, it is unchanged.
     *
     * @param object {@code Object} (element) to be removed from this list, if
     *               present
     * @return {@code true} if the list contained the specified object (element)
     */
    @Override
    public boolean removeLastOccurrence(Object object) {
        if (object == null) {
            for (LinkNode<E> node = getLastNode(); node != null;
                    node = linkedNodes().iGetNodeBefore(node)) {
                if (node.element() == null) {
                    linkedNodes().iRemoveNode(node);
                    return true;
                }
            }
        } else {
            for (LinkNode<E> node = getLastNode(); node != null;
                    node = linkedNodes().iGetNodeBefore(node)) {
                if (object.equals(node.element())) {
                    linkedNodes().iRemoveNode(node);
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Returns a reverse-order view of this list.
     * 
     * The encounter order of elements in the returned view is the inverse of the
     * encounter order of elements in this list. The reverse ordering affects all
     * order-sensitive operations, including those on the view collections of the
     * returned view. Modifications to the reversed view are permitted and will be
     * propagated to this list. In addition, modifications to this list will be
     * visible in the reversed view.
     * 
     * @return a reverse-order view of this list
     */
    //@Override //not until JDK 21
    public NodableLinkedList<E> reversed() {
        return new ReversedNodableLinkedList<E>(this);
    }

    /**
     * Replaces the element at the specified position in this list with the
     * specified element.
     *
     * @param index   index of the element to replace
     * @param element element to be stored at the specified position
     * @return the element previously at the specified position
     * @throws IndexOutOfBoundsException if the specified index is out of range
     *                                   {@code (index < 0 || index >= longSize())}
     */
    @Override
    public E set(int index, E element) {
        checkNodeIndex(index, longSize());
        return linkedNodes().iGetNode(index).set(element);
    }    

    /**
     * Sorts this list according to the order induced by the specified comparator.
     * <p>
     * All elements in this list must be <i>mutually comparable</i> using the
     * specified comparator (that is, {@code c.compare(e1, e2)} must not throw a
     * {@code ClassCastException} for any elements {@code e1} and {@code e2} in the
     * list).
     *
     * If the specified comparator is {@code null} then all elements in this list
     * must implement the {@code Comparable} interface and the elements' natural
     * ordering should be used.
     * <p>
     * <strong>Implementation Specification:</strong> This implementation obtains an
     * array containing all nodes in this list, sorts the array using
     * {@code Arrays.sort(T[] a, Comparator<? super T> c)}, and then effectively
     * clears the list and puts the sorted nodes from the array back into this list
     * in order. If this list's {@code size > Integer.MAX_VALUE-8}, a
     * {@link #mergeSort} is performed.
     * <p>
     * <strong>Implementation Note:</strong> This implementation is a stable,
     * adaptive, iterative mergesort that requires far fewer than n lg(n)
     * comparisons when the input array is partially sorted, while offering the
     * performance of a traditional mergesort when the input array is randomly
     * ordered. If the input array is nearly sorted, the implementation requires
     * approximately n comparisons. Temporary storage requirements vary from a small
     * constant for nearly sorted input arrays to n/2 object references for randomly
     * ordered input arrays.
     * <p>
     * The implementation takes equal advantage of ascending and descending order in
     * its input array, and can take advantage of ascending and descending order in
     * different parts of the same input array. It is well-suited to merging two or
     * more sorted arrays: simply concatenate the arrays and sort the resulting
     * array.
     * <p>
     * The implementation was adapted from Tim Peters's list sort for Python
     * (<a href="http://svn.python.org/projects/python/trunk/Objects/listsort.txt">
     * TimSort</a>). It uses techniques from Peter McIlroy's "Optimistic Sorting and
     * Information Theoretic Complexity", in Proceedings of the Fourth Annual
     * ACM-SIAM Symposium on Discrete Algorithms, pp 467-474, January 1993.
     *
     * @see Arrays#sort
     * @param comparator the {@code Comparator} used to compare list elements. A
     *                   {@code null} value indicates that the elements' natural
     *                   ordering should be used
     * @throws ClassCastException if this list contains elements that are not
     *                            {@code mutually comparable} using the specified
     *                            comparator
     */
    @Override
    public void sort(Comparator<? super E> comparator) {
        if (comparator == null) {
            linkedNodes().sort(null);
        } else {
            linkedNodes().sort((node1, node2) -> {
                return comparator.compare(node1.element(), node2.element());
            });
        }
    }
    
    /**
     * Sorts this list according to the order induced by the specified comparator.
     * <p>
     * All elements in this list must be <i>mutually comparable</i> using the
     * specified comparator (that is, {@code c.compare(e1, e2)} must not throw a
     * {@code ClassCastException} for any elements {@code e1} and {@code e2} in the
     * list).
     *
     * If the specified comparator is {@code null} then all elements in this list
     * must implement the {@code Comparable} interface and the elements' natural
     * ordering should be used.
     * <p>
     * <strong>Implementation Note:</strong> This implementation is a stable,
     * iterative mergesort that requires n lg(n) comparisons. this implementation
     * avoids the N auxiliary storage cost normally associated with a mergesort.
     *
     * The implementation was adapted from Simon Tatham's Mergesort for Linked Lists
     * (<a href=
     * "https://www.chiark.greenend.org.uk/~sgtatham/algorithms/listsort.html">
     * SimonTathamMergesort</a>).
     *
     * @param comparator the {@code Comparator} used to compare list elements. A
     *                   {@code null} value indicates that the elements' natural
     *                   ordering should be used
     * @throws ClassCastException if this list contains elements that are not
     *                            <i>mutually comparable</i> using the specified
     *                            comparator
     */
    public void mergeSort(Comparator<? super E> comparator) {
        if (comparator == null) {
            linkedNodes().mergeSort(null);
        } else {
            linkedNodes().mergeSort((node1, node2) -> {
                return comparator.compare(node1.element(), node2.element());
            });
        }
    }

    /**
     * Merge Sort.
     * 
     * @param <T>        the type of elements held in the specified list
     * @param list       the internal linked list to be sorted
     * @param comparator the Comparator used to compare list elements. A null value
     *                   indicates that the elements' natural ordering should be
     *                   used
     */
    private static <T> void mergeSort(
                    NodableLinkedList<T>.InternalLinkedList list,
                    Comparator<? super Node<T>> comparator) {
        LinkNode<T> pNode, qNode, sortedNode, sortedLastNode;
        long subListSize, nMerges, pSize, qSize;
        subListSize = 1L;
        while (true) {
            sortedLastNode = null;
            nMerges = 0L;
            pNode = list.iGetFirstNode();
            while (pNode != null) {
                nMerges++;
                qNode = pNode;
                pSize = 0L;
                for (long i = 0L; i < subListSize; i++) {
                    pSize++;
                    qNode = list.iGetNodeAfter(qNode);
                    if (qNode == null) break;
                }
                qSize = subListSize;
                while (pSize > 0L || (qSize > 0L && qNode != null)) {
                    if (pSize == 0L) {
                        sortedNode = qNode;
                        qNode = list.iGetNodeAfter(qNode);
                        qSize--;
                    } else if (qSize == 0L || qNode == null) {
                        sortedNode = pNode;
                        pNode = list.iGetNodeAfter(pNode);
                        pSize--;
                    } else if (((comparator == null)
                                ? pNode.compareTo(qNode)
                                : comparator.compare(pNode, qNode)) <= 0)
                    {
                        sortedNode = pNode;
                        pNode = list.iGetNodeAfter(pNode);
                        pSize--;
                    } else {
                        sortedNode = qNode;
                        qNode = list.iGetNodeAfter(qNode);
                        qSize--;
                    }
                    list.iRemoveNode(sortedNode);
                    if (sortedLastNode == null) {
                        list.iAddNodeFirst(sortedNode);
                    } else {
                        list.iAddNodeAfter(sortedNode, sortedLastNode);  
                    }
                    sortedLastNode = sortedNode;
                }
                pNode = qNode;
            }
            if (nMerges <= 1L) break;
            subListSize *= 2L;
        }
    } // mergeSort
    
    /**
     * Returns a view of the portion of this list between the specified fromIndex,
     * inclusive, and toIndex, exclusive. (If the specified {@code fromIndex} and
     * {@code toIndex} are equal, the returned {@code SubList} is empty.) The
     * returned {@code SubList} is backed by this list, so structural changes in the
     * returned {@code SubList} are reflected in this list. The returned
     * {@code SubList} supports all of the optional {@code List} operations.
     * <p>
     * This method eliminates the need for explicit range operations (of the sort
     * that commonly exist for arrays). Any operation that expects a list can be
     * used as a range operation by passing a {@code SubList} view instead of a
     * whole list. For example, the following idiom removes a range of elements from
     * a list:
     * <pre>
     * {@code
     *      list.subList(fromIndex, toIndex).clear();
     * }
     * </pre>
     * 
     * Similar idioms may be constructed for {@code indexOf} and
     * {@code lastIndexOf}, and all of the algorithms in the {@code Collections}
     * class can be applied to a sublist.
     * <p>
     * The semantics of the {@code SubList} returned by this method become undefined
     * if the {@code NodableLinkedList} is <i>structurally modified</i> in any way
     * other than via the returned {@code SubList} or any of its sublists.
     * (Structural modifications are those that change the size of this list, or
     * otherwise perturb it in such a fashion that iterations in progress may yield
     * incorrect results.) A {@code ConcurrentModificationException} is thrown for
     * any operation on a {@code SubList} that is structurally unsound.
     *
     * @param fromIndex low endpoint (inclusive) of the {@code SubList}
     * @param toIndex   high endpoint (exclusive) of the {@code SubList}
     * @return a view of the specified range within this list
     * @throws IndexOutOfBoundsException for an illegal endpoint index value
     *                                   ({@code fromIndex < 0 || toIndex > size ||
     *                                   fromIndex > toIndex})
     */
    @Override
    public SubList<E> subList(int fromIndex, int toIndex) {
        return linkedNodes().newSubList(fromIndex, toIndex);
    }
    
    /**
     * Returns a view of the portion of this list between the specified firstNode,
     * and lastNode (both inclusive). The returned {@code SubList} is backed by this
     * list, so structural changes in the returned {@code SubList} are reflected in
     * this list. The returned {@code SubList} supports all of the optional
     * {@code List} operations.
     * <p>
     * If the specified firstNode is {@code null}, an empty {@code SubList},
     * positioned right before the specified lastNode, is returned. If the specified
     * lastNode is {@code null}, an empty {@code SubList}, positioned right after
     * the specified firstNode, is returned. if both the specified firstNode and
     * lastNode are {@code null}, an empty {@code SubList}, positioned at the end of
     * this list, is returned.
     * <p>
     * <strong>Implementation Note:</strong> For performance reasons, this
     * implementation does not verify that the specified lastNode comes after (or
     * on) the specified firstNode in this list. Therefore, if the specified
     * lastNode does come before the specified firstNode in the list, an
     * {@code IllegalStateException} can be thrown for any subsequent operation on
     * the returned {@code SubList}, indicating that the end of the list was reached
     * unexpectedly.
     * <p>
     * This method eliminates the need for explicit range operations (of the sort
     * that commonly exist for arrays). Any operation that expects a list can be
     * used as a range operation by passing a {@code SubList} view instead of a
     * whole list. For example, the following idiom removes a range of elements from
     * a list:
     * 
     * <pre>
     * {@code
     *      list.subList(firstNode, lastNode).clear();
     * }
     * </pre>
     * 
     * Similar idioms may be constructed for {@code indexOf} and
     * {@code lastIndexOf}, and all of the algorithms in the {@code Collections}
     * class can be applied to a {@code SubList}.
     * <p>
     * The semantics of the {@code SubList} returned by this method become undefined
     * if the {@code NodableLinkedList} is <i>structurally modified</i> in any way
     * other than via the returned {@code SubList} or any of its sublists.
     * (Structural modifications are those that change the size of this list, or
     * otherwise perturb it in such a fashion that iterations in progress may yield
     * incorrect results.) A {@code ConcurrentModificationException} is thrown for
     * any operation on a {@code SubList} that is structurally unsound.
     *
     * @param firstNode low endpoint (inclusive) of the {@code SubList}
     * @param lastNode  high endpoint (inclusive) of the {@code SubList}
     * @return a view of the specified range within this list
     * @throws IllegalArgumentException if any specified node is not linked to this
     *                                  list, or the lastNode comes right before
     *                                  the firstNode in this list.
     */
    public SubList<E> subList(Node<E> firstNode, Node<E> lastNode) {
        return linkedNodes().newSubList(firstNode, lastNode);
    }    

    /**
     * Returns an array containing all of the elements in this list in proper
     * sequence (from first to last element).
     * <p>
     * The returned array will be "safe" in that no references to it are maintained
     * by this list. (In other words, this method must allocate a new array). The
     * caller is thus free to modify the returned array.
     * <p>
     * This method acts as bridge between array-based and collection-based APIs.
     *
     * @return an array containing all of the elements in this list in proper
     *         sequence
     * @throws IllegalStateException if this list is too large to fit in an array
     */
    @Override
    public Object[] toArray() {
        checkListCanFitInAnArray(longSize());
        final Object[] elements = new Object[size()];
        int index = 0;
        for (LinkNode<E> node = getFirstNode(); node != null;
                node = linkedNodes().iGetNodeAfter(node)) {
            elements[index++] = node.element();
        }
        return elements;
    }

    /**
     * Returns an array containing all of the elements in this list in proper
     * sequence (from first to last element); the runtime type of the returned array
     * is that of the specified array. If the list fits in the specified array, it
     * is returned therein. Otherwise, a new array is allocated with the runtime
     * type of the specified array and the size of this list.
     * <p>
     * If the list fits in the specified array with room to spare (i.e., the array
     * has more elements than the list), the element in the array immediately
     * following the end of the list is set to {@code null}. (This is useful in
     * determining the length of the list <i>only</i> if the caller knows that the
     * list does not contain any null elements.)
     * <p>
     * Like the {@link #toArray()} method, this method acts as bridge between
     * array-based and collection-based APIs. Further, this method allows precise
     * control over the runtime type of the output array, and may, under certain
     * circumstances, be used to save allocation costs.
     * <p>
     * Suppose <i>x</i> is a list known to contain only strings. The following code
     * can be used to dump the list into a newly allocated array of {@code String}:
     * <pre>
     * String[] y = x.toArray(new String[0]);
     * </pre>
     *
     * Note that {@code toArray(new Object[0])} is identical in function to
     * {@code toArray()}.
     *
     * @param array the array into which the elements of the list are to be stored,
     *              if it is big enough; otherwise, a new array of the same runtime
     *              type is allocated for this purpose.
     * @return an array containing the elements of the list
     * @throws ArrayStoreException   if the runtime type of the specified array is
     *                               not a supertype of the runtime type of every
     *                               element in this list
     * @throws IllegalStateException if this list is too large to fit in an array
     * @throws NullPointerException  if the specified array is {@code null}
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] array) {
        checkListCanFitInAnArray(longSize());
        if (array.length < size()) {
            array = (T[]) java.lang.reflect.Array.newInstance(
                                    array.getClass().getComponentType(),size());
        }
        int index = 0;
        Object[] elements = array;
        for (LinkNode<E> node = getFirstNode(); node != null;
                node = linkedNodes().iGetNodeAfter(node)) {
            elements[index++] = node.element();			
        }
        if (array.length > size()) array[size()] = null;
        return array;
    }

    /**
     * Returns a {@code ListIterator} of the elements in this list (in proper
     * sequence), starting at the specified position in this list. Obeys the general
     * contract of {@code List.listIterator(int)}. if the specified
     * {@code index == longSize()}, the {@code ListIterator} will be positioned at
     * the end of this list.
     * <p>
     * <strong>Implementation Note:</strong> The {@code ListIterator} returned by
     * this method behaves differently when the list's
     * {@code size > Integer.MAX_VALUE}. Methods {@code nextIndex} and
     * {@code previousIndex} return -1 if the {@code index > Integer_MAX_VALUE}.
     * <p>
     * The {@code ListIterator} is <i>fail-fast</i>: if the
     * {@code NodableLinkedList} is structurally modified at any time after the
     * Iterator is created, in any way except through the {@code ListIterator's} own
     * {@code remove} or {@code add} methods, the {@code ListIterator} will throw a
     * {@code ConcurrentModificationException}. Thus, in the face of concurrent
     * modification, the iterator fails quickly and cleanly, rather than risking
     * arbitrary, non-deterministic behavior at an undetermined time in the future.
     * <p>
     * Instead of using a {@code ListIterator}, consider iterating over the list via
     * {@code Nodes}. For example:
     * 
     * <pre>
     * {@code
     *     // list is a NodableLinkedList<Integer>
     *     Node<Integer> node = list.linkedNodes().get(index); // or list.getFirstNode();
     *     while (node != null) {
     *         System.out.println(node.element());
     *         node = node.next();
     *     }
     * }
     * </pre>
     *
     * @param index index of the first element to be returned from the
     *              {@code ListIterator} (by a call to {@code next})
     * @return a ListIterator of the elements in this list (in proper sequence),
     *         starting at the specified position in this list
     * @throws IndexOutOfBoundsException if the specified index is out of range
     *                                   {@code (index < 0 || index > longSize())}
     * @see List#listIterator(int)
     */
    @Override
    public ListIterator<E> listIterator(int index) {
        checkPositionIndex(index, longSize());
        return new ElementListIterator(linkedNodes(), index, IndexType.ABSOLUTE, linkedNodes().iGetNode(index));
    }

    /**
     * Returns a {@code ListIterator} of the elements in this list (in proper
     * sequence), starting at the specified node in this list. if the specified node
     * is {@code null}, the {@code ListIterator} will be positioned right after the
     * last {@code Node} in this list.
     * <p>
     * <strong>Implementation Note:</strong> The index returned by the returned
     * {@code ListIterator's} methods {@code nextIndex} and {@code previousIndex} is
     * relative to the specified node which has an index of zero. Nodes which come
     * before the specified node in this list, will have a negative index; nodes
     * that come after will have a positive index. Method {@code nextIndex} returns
     * {@code longSize()} if the {@code ListIterator} is positioned at the end of
     * the list, and method {@code previousIndex} returns {@code -longSize()} if the
     * {@code ListIterator} is positioned at the beginning of the list. if
     * {@code index < Integer.MIN_VALUE or index > Integer.MAX_VALUE},
     * {@code Integer.MIN_VALUE} or {@code Integer.MAX_VALUE} is returned
     * respectively.
     * <p>
     * The {@code ListIterator} is <i>fail-fast</i>: if the
     * {@code NodableLinkedList} is structurally modified at any time after the
     * Iterator is created, in any way except through the {@code ListIterator's} own
     * {@code remove} or {@code add} methods, the {@code ListIterator} will throw a
     * {@code ConcurrentModificationException}. Thus, in the face of concurrent
     * modification, the iterator fails quickly and cleanly, rather than risking
     * arbitrary, non-deterministic behavior at an undetermined time in the future.
     * <p>
     * Instead of using a {@code ListIterator}, consider iterating over the list via
     * {@code Nodes}. For example:
     * 
     * <pre>
     * {@code
     *     // list is a NodableLinkedList<Integer>
     *     // make sure node is a forward traversing LinkNode of this list
     *     Node<Integer> linkNode = (node == null) ? null : list.linkedNodes().forwardLinkNodeOf(node);
     *     while (linkNode != null) {
     *         System.out.println(linkNode.element());
     *         linkNode = linkNode.next();
     *     }
     * }
     * </pre>
     *
     * @param node node of the first element to be returned from the
     *             {@code ListIterator} (by a call to {@code next})
     * @return a ListIterator of the elements in this list (in proper sequence),
     *         starting at the specified node in this list
     * @throws IllegalArgumentException if the specified node is not linked to this
     *                                  list
     */
    public ListIterator<E> listIterator(Node<E> node) {
        if (node != null) linkedNodes().checkListContainsNode(node);
        return new ElementListIterator(linkedNodes(), 0L, IndexType.RELATIVE,
                (node == null) ? linkedNodes().iGetTailSentinel() : node.linkNode());
    }

    /**
     * Returns an iterator over the elements in this list in reverse sequential
     * order. The elements will be returned in order from last (tail) to first
     * (head).
     * <p>
     * Instead of using a descending {@code Iterator}, consider iterating over the
     * list via {@code nodes}. For example:
     * <pre>
     * {@code
     *     Node<Integer> linkNode = getLastNode();
     *     while (linkNode != null) {
     *         System.out.println(linkNode.element());
     *         linkNode = linkNode.previous();
     *     }
     *     
     *     or use a reversed {@code LinkNode}:
     *     
     *     Node<Integer> linkNode = getLastNode();
     *     if (linkNode != null) linkNode = linkNode.reversed();
     *     while (linkNode != null) {
     *         System.out.println(linkNode.element());
     *         linkNode = linkNode.next();
     *     }
     *     
     * }
     * </pre>
     *
     * @return an iterator over the elements in this list in reverse order
     */
    @Override
    public Iterator<E> descendingIterator() {
        final LinkNode<E> node = isEmpty() ? linkedNodes().iGetHeadSentinel() : getLastNode();
        return new ElementReverseListIterator(this.linkedNodes(), 0L, IndexType.ABSOLUTE, node);
    }

    /**
     * Creates a <i>late-binding</i> and <i>fail-fast</i> {@code Spliterator} over
     * the elements in this list.
     * <p>
     * The {@code Spliterator} reports {@link Spliterator#SIZED} and
     * {@link Spliterator#ORDERED}. Overriding implementations should document the
     * reporting of additional characteristic values.
     * <p>
     * <strong>Implementation Note:</strong> The {@code Spliterator} additionally
     * reports {@link Spliterator#SUBSIZED} and implements {@code trySplit} to
     * permit limited parallelism..
     *
     * @return a {@code Spliterator} over the elements in this list
     */
    @Override
    public Spliterator<E> spliterator() {
        return new ElementSpliterator<E>(linkedNodes());
    }
    
    private static class ReversedNodableLinkedList<E>
        extends NodableLinkedList<E> implements java.io.Externalizable {
        
        /**
         * The NodableLinkedList that was reversed.
         */
        private NodableLinkedList<E> nodableLinkedList;
        
        /**
         * Constructs a new Reversed NodableLinkedList.
         * 
         * @param nodableLinkedList the NodableLinkedList that was reversed
         */
        private ReversedNodableLinkedList(NodableLinkedList<E> nodableLinkedList) {
            super(nodableLinkedList.linkedNodes());
            this.nodableLinkedList = nodableLinkedList;
        }
        
        @Override
        int modCount() {
            return nodableLinkedList.modCount();
        }
        
        @Override
        public LinkNode<E> getFirstNode() {
            final LinkNode<E> linkNode = linkedNodes().iGetFirstNode();
            return (linkNode == null) ? null : linkNode.reversed();
        }
        
        @Override
        public LinkNode<E> getLastNode() {
            final LinkNode<E> linkNode = linkedNodes().iGetLastNode();
            return (linkNode == null) ? null : linkNode.reversed();
        }        
        
        @Override
        public NodableLinkedList<E> reversed() {
            return this.nodableLinkedList;
        }
        
        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            throw new java.io.InvalidObjectException("not serializable");
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            throw new java.io.InvalidObjectException("not serializable");
        }
        
    } // ReversedNodableLinkedList
    
    /**
     * Common linked list view of LinkedNodes and SubLinkedNodes.
     * <p>
     * INTERNAL USE ONLY
     */
    abstract class InternalLinkedList extends AbstractSequentialList<Node<E>> {
        
        /**
         * Returns the list's size, or -1 if the size unknown.
         * 
         * @return the list's size, or -1 if the size is unknown
         */
        abstract long iGetSize();
        
        /**
         * Inserts the specified node at the beginning of the list.
         * 
         * @param linkNode the LinkNode to be inserted at the beginning of the list
         */
        abstract void iAddNodeFirst(LinkNode<E> linkNode);
        
        /**
         * Appends the specified LinkNode to the end of the list.
         *
         * @param linkNode the LinkNode to be appended to the end of the list
         */
        abstract void iAddNodeLast(LinkNode<E> linkNode);
        
        /**
         * Inserts the specified LinkNode after the specified afterThisLinkNode.
         * 
         * @param linkNode          the LinkNode to be inserted
         * @param afterThisLinkNode the LinkNode to be inserted after
         */
        abstract void iAddNodeAfter(LinkNode<E> linkNode, LinkNode<E> afterThisLinkNode);
        
        /**
         * Inserts the specified LinkNode before the specified beforeThisLinkNode.
         * 
         * @param linkNode           the LinkNode to be inserted
         * @param beforeThisLinkNode the LinkNode to be inserted before
         */
        abstract void iAddNodeBefore(LinkNode<E> linkNode, LinkNode<E> beforeThisLinkNode);
        
        /**
         * Remove the specified LinkNode from the list.
         * 
         * @param linkNode the LinkNode to be removed
         */
        abstract void iRemoveNode(LinkNode<E> linkNode);
        
        /**
         * Replace the specified LinkNode with the withThisLinkNode.
         * 
         * @param linkNode the LinkNode to be replaced
         * @param withThisLinkNode the replacement LinkNode
         */
        abstract void iReplaceNode(LinkNode<E> linkNode, LinkNode<E> withThisLinkNode);
        
        /**
         * Return true if there exists a node after the specified LinkNode in the list.
         * In other words, return true if the specified LinkNode is not the last node in
         * the list.
         * 
         * @param linkNode the LinkNode to check if it has a next node in the list.
         * @return true if there exists a node after the specified LinkNode in the list.
         */
        abstract boolean iHasNodeAfter(LinkNode<E> linkNode);
        
        /**
         * Return true if there exists a node before the specified LinkNode in the list.
         * In other words, return true if the specified LinkNode is not the first node
         * in the list.
         * 
         * @param linkNode the LinkNode to check if it has a previous node in the list.
         * @return true if there exists a node before the specified LinkNode in the
         *         list.
         */
        abstract boolean iHasNodeBefore(LinkNode<E> linkNode);
        
        /**
         * Returns the list's headSentinel.
         * 
         * @return the list's headSentinel
         */
        abstract LinkNode<E> iGetHeadSentinel();
        
        /**
         * Returns the list's tailSentinel, or null if the tailSentinel is unknown.
         * 
         * @return the list's tailSentinel, or null if the tail sentinel is unknown
         */     
        abstract LinkNode<E> iGetTailSentinel();
        
        /**
         * Returns the LinkedNodes' headSentinel from the perspective of the list. If
         * the list is reversed in relation to the LinkedNodes' traversal direction,
         * then the LinkeNodes' tailSentinel is returned.
         * 
         * @return the LinkedNodes' headSentinel from the perspective of this list
         */
        abstract LinkNode<E> iGetMyLinkedNodesHeadSentinel();
        
        /**
         * Returns the LinkedNodes' tailSentinel from the perspective of the list. If
         * the list is reversed in relation to the LinkedNodes' traversal direction,
         * then the LinkeNodes' headSentinel is returned.
         * 
         * @return the LinkedNodes' tailSentinel from the perspective of this list
         */
        abstract LinkNode<E> iGetMyLinkedNodesTailSentinel();
        
        /**
         * Returns the first LinkNode of the list, or null if the list is empty.
         * 
         * @return the first LinkNode of the list, or null if the list is empty
         */
        abstract LinkNode<E> iGetFirstNode();
        
        /**
         * Returns the last LinkNode of the list, or null if the list is empty.
         * 
         * @return the last LinkNode of the list, or null if the list is empty
         */
        abstract LinkNode<E> iGetLastNode();
        
        /**
         * Returns the LinkNode that comes after the specified LinkNode, or null if the
         * specified LinkNode is the last Node in the list.
         * 
         * @param linkNode the LinkNode whose next LinkNode is returned
         * @return the LinkNode that comes after the specified LinkNode, or null if the
         *         specified LinkNode is the last Node in the list
         */
        abstract LinkNode<E> iGetNodeAfter(LinkNode<E> linkNode);
        
        /**
         * Returns the LinkNode that comes after the specified LinkNode, or the
         * tailSentinel if the specified LinkNode is the last Node in the list. This
         * method is the same as the iGetNodeAfter method, except this method returns
         * the tailSentinel instead of null if the specified LinkNode is the last Node
         * in the list.
         * 
         * @param linkNode the LinkNode whose next LinkNode is returned
         * @return the LinkNode that comes after the specified LinkNode, or the
         *         tailSentinel if the specified LinkNode is the last Node in the list
         */
        abstract LinkNode<E> iGetNodeAfterOrTailSentinel(LinkNode<E> linkNode);

        /**
         * Returns the LinkNode that comes after the specified LinkNode, or the
         * tailSentinel if the specified LinkNode is the last Node in the list. This
         * method is the same as the iGetNodeAfterOrTailSentinel method, except this
         * method returns the LinkNode from the list or parent sublist whose
         * tailSentinel is known. This method is used when the tailSentinel might be
         * unknown. It avoids the need to traverse the list to find the unknown
         * tailSentinel (which is needed to determine if the end of the list has been
         * reached) by getting the next LinkNode from a parent sublist whose
         * tailSentinel is already known.
         * 
         * @param linkNode the LinkNode whose next LinkNode is returned
         * @return the LinkNode that comes after the specified LinkNode, or the
         *         tailSentinel if the specified LinkNode is the last Node in the list
         */
        abstract LinkNode<E> iGetNodeAfterFromListWithKnownTailSentinel(LinkNode<E> linkNode);
        
        /**
         * Returns the LinkNode that comes before the specified LinkNode, or null if the
         * specified LinkNode is the first Node in the list.
         * 
         * @param linkNode the LinkNode whose previous LinkNode is returned
         * @return the LinkNode that comes before the specified LinkNode, or null if the
         *         specified LinkNode is the first Node in the list
         */
        abstract LinkNode<E> iGetNodeBefore(LinkNode<E> linkNode);
        
        /**
         * Returns the LinkNode that comes before the specified LinkNode, or the
         * headSentinel if the specified LinkNode is the first node in the list. This
         * method is the same as the iGetNodeBefore method, except this method returns
         * the headSentinel instead of null if the specified LinkNode is the first Node
         * in the list
         * 
         * @param linkNode the LinkNode whose previous LinkNode is returned
         * @return the LinkNode that comes before the specified LinkNode, or the
         *         headSentinel if the specified LinkNode is the first node in the list
         */
        abstract LinkNode<E> iGetNodeBeforeOrHeadSentinel(LinkNode<E> linkNode);
        
    } // InternalLinkedList

    /**
     * Doubly-linked list of nodes which back a {@code NodableLinkedList}.
     * Implements the {@code List} and {@code Deque} interfaces. The elements are of
     * type {@code NodableLinkedList.LinkNode<E>}, and are never {@code null}.
     * Implements all optional {@code List} operations.
     * <p>
     * All of the operations perform as could be expected for a doubly-linked list.
     * Operations that index into the list will traverse the list from the beginning
     * or the end, whichever is closer to the specified index.
     * <p>
     * The iterators returned by this class's {@code iterator} and
     * {@code listIterator} methods are <i>fail-fast</i>: if the
     * {@code NodableLinkedList} is structurally modified at any time after the
     * iterator is created, in any way except through the Iterator's own
     * {@code remove} or {@code add} methods, the iterator will throw a {@code
     * ConcurrentModificationException}. Thus, in the face of concurrent
     * modification, the iterator fails quickly and cleanly, rather than risking
     * arbitrary, non-deterministic behavior at an undetermined time in the future.
     * <p>
     * Note that the fail-fast behavior of an iterator cannot be guaranteed as it
     * is, generally speaking, impossible to make any hard guarantees in the
     * presence of unsynchronized concurrent modification. Fail-fast iterators throw
     * {@code ConcurrentModificationException} on a best-effort basis. Therefore, it
     * would be wrong to write a program that depended on this exception for its
     * correctness: <i>the fail-fast behavior of iterators should be used only to
     * detect bugs.</i>
     *
     * @author James Pfeifer
     * @see List
     * @see Node
     *
     */
    public class LinkedNodes
        extends InternalLinkedList
        implements List<Node<E>>, Deque<Node<E>>
    {

        /**
         * The number of nodes/elements in this list.
         */
        private long size;
        
        /**
         * The LinkNode which represents this list's head sentinel.
         */
        private final LinkNode<E> headSentinel = new LinkNode<>();
        
        /**
         * The LinkNode which represents this list's tail sentinel.
         */
        private final LinkNode<E> tailSentinel = new LinkNode<>();

        /**
         * Constructs a new empty doubly-linked list of nodes.
         */
        private LinkedNodes() {
            NodableLinkedList.this.modCount = this.modCount = 0;
            size = 0L;
            headSentinel.linkedNodes = this;
            tailSentinel.linkedNodes = this;
            headSentinel.next = tailSentinel;		
            tailSentinel.previous = headSentinel;			
        }

        /**
         * Returns the protected int modCount inherited from class
         * java.util.AbstractList.
         * 
         * @return returns the modCount
         */
        int modCount() {
            return this.modCount;
        }
        
        /**
         * Increments the modCount. The modCount for the NodableLinkedList which this
         * LinkedNodes object backs, is also kept in sync.
         */
        private void incrementModCount() {
            NodableLinkedList.this.modCount = ++this.modCount;
        }

        /**
         * Returns the {@code NodableLinkedList} this {@code LinkedNodes} instance
         * backs.
         * 
         * @return the {@code NodableLinkedList} this {@code LinkedNodes} instance
         *         backs.
         */
        public NodableLinkedList<E> nodableLinkedList() {
            return NodableLinkedList.this;
        }
        
        /**
         * Check if this list is not empty.
         * 
         * @throws NoSuchElementException if this list is empty
         */
        private void checkListIsNotEmpty() {
            if (this.isEmpty()) throw new NoSuchElementException("List is empty");
        }
        
        /**
         * Check if this list contains the specified Node.
         * 
         * @param node the Node to check
         * @throws IllegalArgumentException if the specified Node is not contained by
         *                                  this list
         */
        private void checkListContainsNode(Node<?> node) {
            checkListContainsNode(node, "Specified node is not linked to this list");
        }
        
        /**
         * Check if this list contains the specified Node.
         * 
         * @param node the Node to check
         * @param msg  the IllegalArgumentException message
         * @throws IllegalArgumentException if the specified Node is not contained by
         *                                  this list
         */
        private void checkListContainsNode(Node<?> node, String msg) {
            if (!this.contains(node)) throw new IllegalArgumentException(msg);
        }

        /**
         * Returns the number of nodes in this list. If this list contains more than
         * Integer.MAX_VALUE nodes, Integer.MAX_VALUE is returned.
         *
         * @return the number of nodes in this list.
         */
        @Override
        public int size() {
            return (longSize() > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int)longSize();
        }

        /**
         * Returns the number of nodes in this list.
         *
         * @return the number of nodes in this list.
         */
        public long longSize() {
            return size;
        }
        
        /**
         * Returns {@code true} if this list contains no {@code Nodes}.
         *
         * @return {@code true} if this list contains no {@code Nodes}
         */        
        @Override
        public boolean isEmpty() {
            return longSize() == 0L;
        }
        
        @Override
        long iGetSize() {
            return size;
        }
        
        @Override
        void iAddNodeFirst(LinkNode<E> node) {
            //assert isUnLinkedNode(node) : nodeIsNotUnLinkedMsg();
            iAddNodeAfter(node, iGetHeadSentinel());
        }
        
        @Override
        void iAddNodeLast(LinkNode<E> node) {
            //assert isUnLinkedNode(node) : nodeIsNotUnLinkedMsg();
            iAddNodeBefore(node, iGetTailSentinel());
        }

        @Override
        void iAddNodeAfter(LinkNode<E> node, LinkNode<E> afterThisNode) {
            //assert isUnLinkedNode(node) : nodeIsNotUnLinkedMsg();
            //assert this.contains(afterThisNode) : "After Node is not an element of this list";			
            incrementModCount();			
            node.iSetLinkedNodes(this);
            node.iSetNext(afterThisNode.iGetNext());
            node.iSetPrevious(afterThisNode);
            node.iGetPrevious().iSetNext(node);
            node.iGetNext().iSetPrevious(node);
            size++;
        }

        @Override
        void iAddNodeBefore(LinkNode<E> node, LinkNode<E> beforeThisNode) {
            //assert isUnLinkedNode(node) : nodeIsNotUnLinkedMsg();
            //assert this.contains(beforeThisNode) : "Before Node is not an element of this list";			
            incrementModCount();			
            node.iSetLinkedNodes(this);
            node.iSetNext(beforeThisNode);
            node.iSetPrevious(beforeThisNode.iGetPrevious());
            node.iGetPrevious().iSetNext(node);
            node.iGetNext().iSetPrevious(node);
            size++;
        }

        @Override
        void iRemoveNode(LinkNode<E> node) {
            //assert this.contains(node) : "Node is not an element of this list";			
            incrementModCount();			
            node.iGetNext().iSetPrevious(node.iGetPrevious());
            node.iGetPrevious().iSetNext(node.iGetNext());
            node.iSetNext(null);		
            node.iSetPrevious(null);
            node.iSetLinkedNodes(null);
            size--;
        }

        @Override
        void iReplaceNode(LinkNode<E> node, LinkNode<E> replacementNode) {
            //assert this.contains(node) : "Node is not an element of this list";
            //assert isUnLinkedNode(replacementNode) : "replacement"+nodeIsNotUnLinkedMsg();
            incrementModCount();
            replacementNode.iSetLinkedNodes(this);
            replacementNode.iSetNext(node.iGetNext());
            replacementNode.iSetPrevious(node.iGetPrevious());
            replacementNode.iGetPrevious().iSetNext(replacementNode);
            replacementNode.iGetNext().iSetPrevious(replacementNode);
            node.iSetNext(null);		
            node.iSetPrevious(null);
            node.iSetLinkedNodes(null);
        }

        @Override
        boolean iHasNodeAfter(LinkNode<E> node) {
            //assert this.contains(node) : "Node is not an element of this list";			
            return node.iGetNext() != iGetTailSentinel(); 
        }

        @Override
        boolean iHasNodeBefore(LinkNode<E> node) {
            //assert this.contains(node) : "Node is not an element of this list";			
            return node.iGetPrevious() != iGetHeadSentinel(); 
        }
        
        @Override
        LinkNode<E> iGetHeadSentinel() {
            return headSentinel;
        }
        
        @Override
        LinkNode<E> iGetTailSentinel() {
            return tailSentinel;
        }
        
        @Override
        LinkNode<E> iGetMyLinkedNodesHeadSentinel() {
            return headSentinel;
        }
        
        @Override
        LinkNode<E> iGetMyLinkedNodesTailSentinel() {
            return tailSentinel;
        }
        
        @Override
        LinkNode<E> iGetFirstNode() {
            return (isEmpty()) ? null : iGetNodeAfter(iGetHeadSentinel());
        }
        
        @Override
        LinkNode<E> iGetLastNode() {
            return (isEmpty()) ? null : iGetNodeBefore(iGetTailSentinel());
        }        

        @Override
        LinkNode<E> iGetNodeAfter(LinkNode<E> node) {
            //assert this.contains(node) : "Node is not an element of this list";			
            return (node.iGetNext() == iGetTailSentinel()) ? null : node.iGetNext();
        }

        @Override
        LinkNode<E> iGetNodeAfterOrTailSentinel(LinkNode<E> node) {
            //assert this.contains(node) : "Node is not an element of this list";           
            return node.iGetNext();
        }
        
        @Override
        LinkNode<E> iGetNodeAfterFromListWithKnownTailSentinel(LinkNode<E> node) {
            //assert this.contains(node) : "Node is not an element of this list";           
            return node.iGetNext();
        }

        @Override
        LinkNode<E> iGetNodeBefore(LinkNode<E> node) {
            //assert this.contains(node) : "Node is not an element of this list";			
            return (node.iGetPrevious() == iGetHeadSentinel()) ? null : node.iGetPrevious();
        }
        
        @Override
        LinkNode<E> iGetNodeBeforeOrHeadSentinel(LinkNode<E> node) {
            //assert this.contains(node) : "Node is not an element of this list";           
            return node.iGetPrevious();
        }        

        /**
         * Returns the index of the specified node in this list, or -1 if this list does
         * not contain the specified node.
         * 
         * @param node {@code Node} to search for
         * @return the index of the specified node in this list, or -1 if this list does
         *         not contain the specified node
         */
        private long iGetIndex(LinkNode<?> node) {
            if (this.contains(node)) {
                long index = 0L;
                for (LinkNode<E> cursorNode = iGetFirstNode(); cursorNode != null;
                        cursorNode = iGetNodeAfter(cursorNode), index++) {
                    if (cursorNode == node) return index;				
                }
            }
            return -1L;
        }
        
        /**
         * Returns the node at the specified position in this sublist.
         * 
         * Note, this routine returns the tailSentinel if {@code index == longSize()}.
         *
         * @param index index of the node to return
         * @return the node at the specified position in this sublist
         */
        private LinkNode<E> iGetNode(long index) {
            //assert isPositionIndex(index, longSize()) : outOfBoundsMsg(index, longSize());
            if (index == longSize()) return iGetTailSentinel();
            LinkNode<E> node;
            long nodeIndex;
            final long lastIndex = longSize() - 1L;
            if (index < (lastIndex >> 1)) {
                for (node = iGetFirstNode(), nodeIndex = 0L;
                     nodeIndex < index && node != null;
                     nodeIndex++, node = iGetNodeAfter(node));
            } else {
                for (node = iGetLastNode(), nodeIndex = lastIndex;
                     nodeIndex > index && node != null;
                     nodeIndex--, node = iGetNodeBefore(node));
            }       
            return node;        
        }

        /**
         * Appends the specified node to the end of this list. The specified node must
         * not already belong to a list.
         * <p>
         * This method is equivalent to {@link #addNodeLast}.
         *
         * @param node {@code Node} to be appended to the end of this list
         * @throws IllegalArgumentException if the specified node is {@code null} or
         *                                  already a node of a list
         */
        public void addNode(Node<E> node) {
            addNodeLast(node);
        }

        /**
         * Inserts the specified node at the beginning of this list. The specified node
         * must not already belong to a list.
         * <p>
         * Note that {@code addNodeFirst} is identical in function to {@code addFirst}.
         *
         * @param node the {@code Node} to be inserted at the beginning of this list
         * @throws IllegalArgumentException if the specified node is {@code null} or
         *                                  already a node of a list
         */
        public void addNodeFirst(Node<E> node) {
            checkNodeIsUnLinked(node);
            iAddNodeFirst(node.linkNode());
        }

        /**
         * Appends the specified node to the end of this list. The specified node must
         * not already belong to a list.
         * <p>
         * Note that {@code addNodeLast} is identical in function to {@code addLast}.
         *
         * @param node {@code Node} to be appended to the end of this list
         * @throws IllegalArgumentException if the specified node is {@code null} or
         *                                  already a node of a list
         */
        public void addNodeLast(Node<E> node) {
            checkNodeIsUnLinked(node);
            iAddNodeLast(node.linkNode());
        }
        
        /**
         * Returns the first node of this list, or {@code null} if this list is empty.
         * 
         * A reversed {@code LinkNode} is returned for reversed lists.
         * 
         * @return the first node of this list, or {@code null} if this list is empty
         */
        public LinkNode<E> getFirstNode() {
            return iGetFirstNode();
        }

        /**
         * Returns the last node of this list, or {@code null} if this list is empty.
         * 
         * A reversed {@code LinkNode} is returned for reversed lists.
         *
         * @return the last node of this list, or {@code null} if this list is empty
         */
        public LinkNode<E> getLastNode() {
            return iGetLastNode();
        }

        /**
         * Removes and returns the first node of this list, or returns {@code null} if
         * this list is empty.
         *
         * @return the first node of this list that was removed, or {@code null} if this
         *         list is empty
         */
        public LinkNode<E> removeFirstNode() {
            if (isEmpty()) return null;
            final LinkNode<E> node = iGetFirstNode();
            iRemoveNode(node);
            return node;
        }

        /**
         * Removes and returns the last node of this list, or returns {@code null} if
         * this list is empty.
         *
         * @return the last node of this list that was removed, or {@code null} if this
         *         list is empty
         */
        public LinkNode<E> removeLastNode() {
            if (isEmpty()) return null;
            final LinkNode<E> node = iGetLastNode();
            iRemoveNode(node);
            return node;
        }

        /**
         * Removes all of the nodes from this list.
         */
        @Override
        public void clear() {
            final LinkNode<E> headSentinel = iGetHeadSentinel();
            while (!isEmpty()) iRemoveNode(iGetNodeAfter(headSentinel));
        }        

        /**
         * Returns {@code true} if this list contains the specified object
         * ({@code Node}). The specified object must be a {@code Node}, otherwise,
         * {@code false} is returned.
         * <p>
         * This operation is performed in constant time.
         *
         * @param object {@code Object} ({@code Node}) whose presence in this list is to
         *               be tested
         * @return {@code true} if this list contains the specified object
         *         ({@code Node})
         */
        @Override
        public boolean contains(Object object) {
            if (!(object instanceof Node)) return false;
            return contains((Node<?>)object);
        }
        
        boolean contains(Node<?> node) {
             return node != null && node.linkedNodes() == this;
        }
        
        /**
         * Returns and possibly reverses the specified {@code Node's} backing
         * {@code LinkNode} to match the forward direction of this list. In other words,
         * returns a {@code LinkNode} which can be used to traverse this list from the
         * specified node to this list's last node when making successive calls to the
         * {@code LinkNode.next()} method. The specified node must contained by this
         * list.
         * 
         * @param node the {@code Node} whose backing {@code LinkNode} is returned and
         *             possibly reversed to match the forward direction of this list
         * @return a {@code LinkNode} which can be used to traverse this list in a
         *         forward direction
         * @throws IllegalArgumentException if the specified node is not contained by
         *                                  this list
         */
        public LinkNode<E> forwardLinkNodeOf(Node<E> node) {
            checkListContainsNode(node);
            return node.linkNode();
        }
        
        /**
         * Returns {@code true} if this list is a reverse-ordered view of the base
         * {@code NodableLinkedList}.
         * 
         * @return {@code true} if this list is a reverse-ordered view of the base
         *         {@code NodableLinkedList}
         */
        public boolean isReversed() {
            return false;
        }

        /**
         * Returns the index of the specified object ({@code Node}) in this list, or -1
         * if there is no such index (this list does not contain the specified
         * {@code Node} or the {@code index > Integer.MAX_VALUE}). The specified object
         * must be a {@code Node}, otherwise, -1 is returned.
         * <p>
         * Note that {@code indexOf} is identical in function to {@code lastIndexOf},
         * except {@code indexOf} traverses the list forwards from the beginning.
         * 
         * @param object {@code Object} ({@code Node}) to search for
         * @return the index of the specified object ({@code Node}) in this list, or -1
         *         if there is no such index.
         */
        @Override
        public int indexOf(Object object) {
            if (!(object instanceof Node)) return -1;
            final Node<?> node = (Node<?>)object;
            if (!this.contains(node)) return -1;
            final long index = iGetIndex(node.linkNode());
            return (index > Integer.MAX_VALUE) ? -1 : (int)index;
        }

        /**
         * Returns the index of the specified object ({@code Node}) in this list, or -1
         * if there is no such index (this list does not contain the {@code Node} or the
         * {@code index > Integer.MAX_VALUE}). The specified object must be a
         * {@code Node}, otherwise, -1 is returned.
         * <p>
         * Note that {@code lastIndexOf} is identical in function to {@code indexOf},
         * except {@code lastIndexOf} traverses the list backwards from the end of the
         * list.
         *
         * @param object {@code Object} ({@code Node}) to search for
         * @return the index of the specified object ({@code Node}) in this list, or -1
         *         if there is no such index.
         */
        @Override
        public int lastIndexOf(Object object) {
            if (!(object instanceof Node)) return -1;
            final LinkNode<?> objectsNode = ((Node<?>)object).linkNode();
            if (!this.contains(objectsNode)) return -1;
            long index = longSize() - 1L;
            for (LinkNode<E> node = iGetLastNode(); node != null;
                    node = iGetNodeBefore(node), index--) {
                if (node == objectsNode) {
                    return (index > Integer.MAX_VALUE) ? -1 : (int)index;
                }
            }
            return -1;
        }

        /**
         * Appends all of the {@code Nodes} in the specified collection to the end of
         * this list, in the order that they are returned by the specified collection's
         * iterator.
         * <p>
         * The behavior of this operation is undefined if the specified collection is
         * modified while the operation is in progress. (Note that this will occur if
         * the specified collection is this list, and it's nonempty.)
         *
         * @param collection collection containing {@code Nodes} to be added to this
         *                   list
         * @return {@code true} if this list changed as a result of the call
         * @throws NullPointerException     if the specified collection is {@code null}
         * @throws IllegalArgumentException if any {@code Node} in the collection is
         *                                  {@code null} or already a node of a list
         */
        @Override
        public boolean addAll(Collection<? extends Node<E>> collection) {
            final long initialSize = longSize();
            for (Node<E> node: collection) {
                checkNodeIsUnLinked(node, "Node in collection (@"+(longSize()-initialSize)+") is null or already an element of a list");
                iAddNodeLast(node.linkNode());
            }
            return longSize() != initialSize;
        }

        /**
         * Inserts all of the {@code Nodes} in the specified collection into this list,
         * starting at the specified position. Shifts the {@code Node} currently at that
         * position (if any) and any subsequent {@code Nodes} to the right (increases
         * their indices). The new {@code Nodes} will appear in the list in the order
         * that they are returned by the specified collection's iterator. if the
         * specified {@code index == longSize()}, the {@code Nodes} will be appended to
         * the end of this list.
         * 
         * Note that {@code addAll(longSize(), Collection)} is identical in function to
         * {@code addAll(Collection)}.
         * <p>
         * The behavior of this operation is undefined if the specified collection is
         * modified while the operation is in progress. (Note that this will occur if
         * the specified collection is this list, and it's nonempty.)
         *
         * @param index      position where to insert the first {@code Node} from the
         *                   specified collection
         * @param collection collection containing {@code Nodes} to be added to this
         *                   list
         * @return {@code true} if this list changed as a result of the call
         * @throws IllegalArgumentException  if any {@code Node} in the collection is
         *                                   {@code null} or already a node of a list
         * @throws IndexOutOfBoundsException if the specified index is out of range
         *                                   {@code (index < 0 || index > longSize())}
         * @throws NullPointerException      if the specified collection is {@code null}
         */
        @Override
        public boolean addAll(int index, Collection<? extends Node<E>> collection) {
            checkPositionIndex(index, longSize());
            if (index == longSize()) return addAll(collection);
            final long initialSize = longSize();
            final LinkNode<E> beforeThisNode = iGetNode(index);
            for (Node<E> node: collection) {
                checkNodeIsUnLinked(node, "Node in collection (@"+(longSize()-initialSize)+") is null or already an element of a list");
                iAddNodeBefore(node.linkNode(), beforeThisNode);
            }
            return longSize() != initialSize;
        }

        /**
         * Inserts all of the {@code Nodes} in the specified collection into this list,
         * before the specified node. Shifts the specified node and any subsequent
         * {@code Nodes} to the right (increases their indices). The new {@code Nodes}
         * will appear in the list in the order that they are returned by the specified
         * collection's iterator. if the specified node is {@code null}, the
         * {@code Nodes} will be appended to the end of this list.
         * 
         * Note that {@code addAll(null, Collection)} is identical in function to
         * {@code addAll(Collection)}.
         * <p>
         * The behavior of this operation is undefined if the specified collection is
         * modified while the operation is in progress. (Note that this will occur if
         * the specified collection is this list, and it's nonempty.)
         *
         * @param node       {@code Node} the specified collection is to be inserted
         *                   before
         * @param collection collection containing {@code Nodes} to be added to this
         *                   list
         * @return {@code true} if this list changed as a result of the call
         * @throws IllegalArgumentException if the specified node is not linked to this
         *                                  list, or any {@code Node} in the collection
         *                                  is {@code null} or already a node of a list
         * @throws NullPointerException     if the specified collection is {@code null}
         */
        public boolean addAll(Node<E> node, Collection<? extends Node<E>> collection) {
            if (node == null) return addAll(collection);
            checkListContainsNode(node);
            final LinkNode<E> beforeThisNode = node.linkNode();
            final long initialSize = longSize();
            for (Node<E> collectionNode: collection) {
                checkNodeIsUnLinked(collectionNode, "Node in collection ("+(longSize()-initialSize)+") is null or already an element of a list");
                iAddNodeBefore(collectionNode.linkNode(), beforeThisNode);
            }
            return longSize() != initialSize;
        }

        /**
         * Appends the specified node to the end of this list. The specified node must
         * not already belong to a list.
         *
         * @param node {@code Node} to be appended to this list
         * @return {@code true} (as specified by {@link Collection#add})
         * @throws IllegalArgumentException if the specified node is {@code null} or
         *                                  already a node of a list
         */
        @Override
        public boolean add(Node<E> node) {
            addNodeLast(node);
            return true;
        }

        /**
         * Inserts the specified node at the specified position in this list. Shifts the
         * {@code Node} currently at that position (if any) and any subsequent
         * {@code Nodes} to the right (adds one to their indices). The specified node
         * must not already belong to a list. if the specified
         * {@code index == longSize()}, the specified node will be appended to the end
         * of this list.
         *
         * @param index position where the specified node is to be inserted
         * @param node  {@code Node} to be inserted
         * @throws IndexOutOfBoundsException if the specified index is out of range
         *                                   {@code (index < 0 || index > longSize())}
         * @throws IllegalArgumentException  if the specified node is {@code null} or
         *                                   already a node of a list
         */
        @Override
        public void add(int index, Node<E> node) {
            checkPositionIndex(index, longSize());
            if (index == longSize()) {
                addNodeLast(node);
            } else {
                checkNodeIsUnLinked(node);
                iAddNodeBefore(node.linkNode(), iGetNode(index));
            }       
        }        

        /**
         * Inserts the specified node at the beginning of this list. The specified node
         * must not already belong to a list.
         * <p>
         * Note that {@code addFirst} is identical in function to {@code addNodeFirst}.
         *
         * @param node the {@code Node} to add
         * @throws IllegalArgumentException if the specified node is {@code null} or
         *                                  already a node of a list
         */
        @Override
        public void addFirst(Node<E> node) {
            addNodeFirst(node);
        }		

        /**
         * Appends the specified node to the end of this list. The specified node must
         * not already belong to a list.
         * <p>
         * Note that {@code addLast} is identical in function to {@code addNodeLast}.
         *
         * @param node the {@code Node} to add
         * @throws IllegalArgumentException if the specified node is {@code null} or
         *                                  already a node of a list
         */
        @Override
        public void addLast(Node<E> node) {
            addNodeLast(node);
        }

        /**
         * Retrieves, but does not remove, the head (first {@code Node}) of this list.
         * 
         * A reversed {@code LinkNode} is returned for reversed lists.
         * 
         * @return the head of this list
         * @throws NoSuchElementException if this list is empty
         */
        @Override
        public LinkNode<E> element() {
            return getFirst();
        }

        /**
         * Returns the {@code Node} at the specified position in this list.
         * 
         * A reversed {@code LinkNode} is returned for reversed lists.
         *
         * @param index index of the {@code LinkNode} to return
         * @return the {@code LinkNode} at the specified position in this list
         * @throws IndexOutOfBoundsException if the specified index is out of range
         *                                   {@code (index < 0 || index >= longSize())}
         */
        @Override
        public LinkNode<E> get(int index) {
            checkNodeIndex(index, longSize());
            return iGetNode(index);
        }        

        /**
         * Returns the first {@code Node} in this list.
         * 
         * A reversed {@code LinkNode} is returned for reversed lists.
         *
         * @return the first {@code LinkNode} in this list
         * @throws NoSuchElementException if this list is empty
         */
        @Override
        public LinkNode<E> getFirst() {
            checkListIsNotEmpty();
            return iGetFirstNode();
        }

        /**
         * Returns the last {@code Node} in this list.
         * 
         * A reversed {@code LinkNode} is returned for reversed lists.
         *
         * @return the last {@code LinkNode} in this list
         * @throws NoSuchElementException if this list is empty
         */
        @Override
        public LinkNode<E> getLast() {
            checkListIsNotEmpty();
            return iGetLastNode();
        }

        /**
         * Adds the specified node as the tail (last {@code Node}) of this list. The
         * specified node must not already belong to a list.
         *
         * @param node the {@code Node} to add
         * @return {@code true} (as specified by {@link java.util.Queue#offer})
         * @throws IllegalArgumentException if the specified node is {@code null} or
         *                                  already a node of a list
         */
        @Override
        public boolean offer(Node<E> node) {
            return add(node);
        }

        /**
         * Inserts the specified node at the front of this list. The specified node must
         * not already belong to a list.
         *
         * @param node the {@code Node} to insert
         * @return {@code true} (as specified by {@link Deque#offerFirst})
         * @throws IllegalArgumentException if the specified node is {@code null} or
         *                                  already a node of a list
         */
        @Override
        public boolean offerFirst(Node<E> node) {
            addNodeFirst(node);
            return true;
        }

        /**
         * Inserts the specified node at the end of this list. The specified node must
         * not already belong to a list.
         *
         * @param node the {@code Node} to insert
         * @return {@code true} (as specified by {@link Deque#offerLast})
         * @throws IllegalArgumentException if the specified node is {@code null} or
         *                                  already a node of a list
         */
        @Override
        public boolean offerLast(Node<E> node) {
            addNodeLast(node);
            return true;
        }        

        /**
         * Retrieves, but does not remove, the head (first {@code Node}) of this list.
         * 
         * A reversed {@code LinkNode} is returned for reversed lists.
         * 
         * @return the head of this list, or {@code null} if this list is empty
         */
        @Override
        public LinkNode<E> peek() {
            return peekFirst();
        }

        /**
         * Retrieves, but does not remove, the first {@code Node} of this list, or
         * returns {@code null} if this list is empty.
         * 
         * A reversed {@code LinkNode} is returned for reversed lists.
         *
         * @return the first {@code LinkNode} of this list, or {@code null} if this list is empty
         */
        @Override
        public LinkNode<E> peekFirst() {
            return iGetFirstNode();
        }

        /**
         * Retrieves, but does not remove, the last {@code Node} of this list, or returns
         * {@code null} if this list is empty.
         * 
         * A reversed {@code LinkNode} is returned for reversed lists.
         *
         * @return the last {@code LinkNode} of this list, or {@code null} if this list is empty
         */
        @Override
        public LinkNode<E> peekLast() {
            return iGetLastNode();
        }

        /**
         * Retrieves and removes the head (first {@code Node}) of this list
         * 
         * @return the head of this list, or {@code null} if this list is empty
         */
        @Override
        public LinkNode<E> poll() {
            return pollFirst();
        }

        /**
         * Retrieves and removes the first {@code Node} of this list, or returns
         * {@code null} if this list is empty.
         *
         * @return the first {@code LinkNode} of this list, or {@code null} if this list
         *         is empty
         */
        @Override
        public LinkNode<E> pollFirst() {
            return removeFirstNode();
        }

        /**
         * Retrieves and removes the last {@code Node} of this list, or returns
         * {@code null} if this list is empty.
         *
         * @return the last {@code LinkNode} of this list, or {@code null} if this list
         *         is empty
         */
        @Override
        public LinkNode<E> pollLast() {
            return removeLastNode();
        }        

        /**
         * Pops a {@code Node} from the stack represented by this list. In other words,
         * removes and returns the first {@code Node} of this list.
         * <p>
         * This method is equivalent to {@link #removeFirst()}.
         *
         * @return the {@code LinkNode} at the front of this list (which is the top of
         *         the stack represented by this list)
         * @throws NoSuchElementException if this list is empty
         */
        @Override
        public LinkNode<E> pop() {
            return removeFirst();
        }

        /**
         * Pushes a {@code Node} onto the stack represented by this list. In other
         * words, inserts the specified node at the front of this list. The specified
         * node must not already belong to a list.
         * <p>
         * This method is equivalent to {@link #addFirst}.
         *
         * @param node the {@code Node} to push
         * @throws IllegalArgumentException if the specified node is {@code null} or
         *                                  already a node of a list
         */
        @Override
        public void push(Node<E> node) {
            addNodeFirst(node);
        }        

        /**
         * Removes and returns the head (first {@code Node}) of this list.
         *
         * @return the head of this list
         * @throws NoSuchElementException if this list is empty
         */
        @Override
        public LinkNode<E> remove() {
            return removeFirst();
        }

        /**
         * Removes and returns the {@code Node} at the specified position in this list.
         * Shifts any subsequent {@code Nodes} to the left (subtracts one from their
         * indices).
         *
         * @param index the index of the {@code Node} to be removed
         * @return the {@code LinkNode} previously at the specified position
         * @throws IndexOutOfBoundsException if the specified index is out of range
         *                                   {@code (index < 0 || index >= longSize())}
         */
        @Override
        public LinkNode<E> remove(int index) {
            checkNodeIndex(index, longSize());
            final LinkNode<E> node = iGetNode(index);
            iRemoveNode(node);
            return node;
        }        

        /**
         * Removes and returns the first {@code Node} from this list.
         *
         * @return the first {@code LinkNode} from this list
         * @throws NoSuchElementException if this list is empty
         */
        @Override
        public LinkNode<E> removeFirst() {
            checkListIsNotEmpty();
            final LinkNode<E> node = iGetFirstNode();
            iRemoveNode(node);
            return node;
        }

        /**
         * Removes and returns the last {@code Node} from this list.
         *
         * @return the last {@code LinkNode} from this list
         * @throws NoSuchElementException if this list is empty
         */
        @Override
        public LinkNode<E> removeLast() {
            checkListIsNotEmpty();
            final LinkNode<E> node = iGetLastNode();
            iRemoveNode(node);
            return node;
        }

        /**
         * Removes, if present, the specified object ({@code Node}) from this list. If
         * this list does not contain the specified {@code Node}, it is unchanged.
         * <p>
         * This operation performs in constant time.
         *
         * @param object {@code Object} ({@code Node}) to be removed from this list
         * @return {@code true} if this list contained the specified object
         *         ({@code Node})
         */
        @Override
        public boolean remove(Object object) {
            if (!(object instanceof Node)) return false;
            @SuppressWarnings("unchecked")
            final Node<E> node = (Node<E>)object;
            if (!this.contains(node)) return false;
            iRemoveNode(node.linkNode());
            return true;		
        }

        /**
         * Removes, if present, the specified object ({@code Node}) from this list. If
         * this list does not contain the specified {@code Node}, it is unchanged.
         * <p>
         * This operation performs in constant time.
         * <p>
         * This method is equivalent to {@link #remove(Object object)}.
         *
         * @param object {@code Object} ({@code Node}) to be removed from this list, if
         *               present
         * @return {@code true} if this list contained the specified object
         *         ({@code Node})
         */
        @Override
        public boolean removeFirstOccurrence(Object object) {
            return remove(object);
        }

        /**
         * Removes, if present, the specified object ({@code Node}) from this list. If
         * this list does not contain the specified {@code Node}, it is unchanged.
         * <p>
         * This operation performs in constant time.
         * <p>
         * This method is equivalent to {@link #remove(Object object)}.
         *
         * @param object {@code Object} ({@code Node}) to be removed from this list, if
         *               present
         * @return {@code true} if this list contained the specified object
         *         ({@code Node})
         */
        @Override
        public boolean removeLastOccurrence(Object object) {
            return remove(object);
        }
        
        /**
         * Returns a reverse-order view of this list.
         * 
         * The encounter order of {@code Nodes} in the returned view is the inverse of
         * the encounter order of {@code Nodes} in this list. The reverse ordering
         * affects all order-sensitive operations, including those on the view
         * collections of the returned view. Modifications to the reversed view are
         * permitted and will be propagated to this list. In addition, modifications to
         * this list will be visible in the reversed view.
         * 
         * @return a reverse-order view of this list
         */
        //@Override //not until JDK 21
        public LinkedNodes reversed() {
            return (new ReversedNodableLinkedList<E>(nodableLinkedList())).linkedNodes();
        }        

        /**
         * Replaces the {@code Node} at the specified position in this list with the
         * specified node. The specified node must not already belong to a list.
         *
         * @param index index of the {@code Node} to replace
         * @param node  {@code Node} to be stored at the specified position
         * @return the {@code LinkNode} previously at the specified position
         * @throws IndexOutOfBoundsException if the specified index is out of range
         *                                   {@code (index < 0 || index >= longSize())}
         * @throws IllegalArgumentException  if the specified node is {@code null} or
         *                                   already a node of a list
         */
        @Override
        public LinkNode<E> set(int index, Node<E> node) {
            checkNodeIndex(index, longSize());
            checkNodeIsUnLinked(node);
            final LinkNode<E> originalNode = iGetNode(index);
            iReplaceNode(originalNode, node.linkNode());
            return originalNode;
        }        

        /**
         * Sorts this list according to the order induced by the specified comparator.
         * <p>
         * The specified comparator compares the {@code Nodes} not the elements of the
         * {@code Nodes}. for example:
         * {@code sort((node1, node2) -> { return node1.compareTo(node2); });}, or
         * {@code sort((node1, node2) -> { return
         * node1.element().compareTo(node2.element()); });}.
         *
         * If the specified comparator is {@code null} then all elements in this list
         * must implement the {@code Comparable} interface and the elements' natural
         * ordering should be used.
         * <p>
         * <strong>Implementation Specification:</strong> This implementation obtains an
         * array containing all {@code Nodes} in this list, sorts the array using
         * {@code Arrays.sort(T[] a, Comparator<? super T> c)}, and then effectively
         * clears the list and puts the sorted {@code Nodes} from the array back into
         * this list in order. If this list's {@code size > Integer.MAX_VALUE-8}, a
         * {@link #mergeSort} is performed.
         * <p>
         * <strong>Implementation Note:</strong> This implementation is a stable,
         * adaptive, iterative mergesort that requires far fewer than n lg(n)
         * comparisons when the input array is partially sorted, while offering the
         * performance of a traditional mergesort when the input array is randomly
         * ordered. If the input array is nearly sorted, the implementation requires
         * approximately n comparisons. Temporary storage requirements vary from a small
         * constant for nearly sorted input arrays to n/2 object references for randomly
         * ordered input arrays.
         * <p>
         * The implementation takes equal advantage of ascending and descending order in
         * its input array, and can take advantage of ascending and descending order in
         * different parts of the same input array. It is well-suited to merging two or
         * more sorted arrays: simply concatenate the arrays and sort the resulting
         * array.
         * <p>
         * The implementation was adapted from Tim Peters's list sort for Python
         * (<a href="http://svn.python.org/projects/python/trunk/Objects/listsort.txt">
         * TimSort</a>). It uses techniques from Peter McIlroy's "Optimistic Sorting and
         * Information Theoretic Complexity", in Proceedings of the Fourth Annual
         * ACM-SIAM Symposium on Discrete Algorithms, pp 467-474, January 1993.
         *
         * @see Arrays#sort
         * @param comparator the {@code Comparator} used to compare {@code Nodes}. A
         *                   {@code null} value indicates that the elements' natural
         *                   ordering should be used
         * @throws ClassCastException if this list contains elements that are not
         *                            <i>mutually comparable</i> using the specified
         *                            comparator
         */
        @Override
        public void sort(Comparator<? super Node<E>> comparator) {
            if (longSize() < 2L) return;
            if (longSize() > Integer.MAX_VALUE - 8) {
                mergeSort(comparator);
                return;
            }
            @SuppressWarnings("unchecked")
            final LinkNode<E>[] sortedNodes = new LinkNode[(int)longSize()];
            int index = 0;
            for (LinkNode<E> node = iGetFirstNode(); node != null; node = iGetNodeAfter(node)) {
                sortedNodes[index++] = node;
            }
            Arrays.sort(sortedNodes, comparator);
            LinkNode<E> cursor;
            LinkNode<E> sortedNode;
            for (index = 0, cursor = iGetFirstNode(); index < sortedNodes.length - 1; index++) {
                sortedNode = sortedNodes[index];
                LinkNode.swapNodes(cursor, sortedNode);
                cursor = iGetNodeAfter(sortedNode); // basically cursor = cursor.next since
                                                    // sortedNode has replaced cursor's position in the list
            }
        }
        
        /**
         * Sorts this list according to the order induced by the specified comparator.
         * <p>
         * The specified comparator compares the {@code Nodes} not the elements of the
         * {@code Nodes}. for example:
         * {@code sort((node1, node2) -> { return node1.compareTo(node2); });}, or
         * {@code sort((node1, node2) -> { return
         * node1.element().compareTo(node2.element()); });}.
         *
         * If the specified comparator is {@code null} then all elements in this list
         * must implement the {@code Comparable} interface and the elements' natural
         * ordering should be used.
         * <p>
         * <strong>Implementation Note:</strong> This implementation is a stable,
         * iterative mergesort that requires n lg(n) comparisons. this implementation
         * avoids the N auxiliary storage cost normally associated with a mergesort.
         *
         * The implementation was adapted from Simon Tatham's Mergesort for Linked Lists
         * (<a href=
         * "https://www.chiark.greenend.org.uk/~sgtatham/algorithms/listsort.html">
         * SimonTathamMergesort</a>).
         *
         * @param comparator the {@code Comparator} used to compare {@code Nodes}. A
         *                   {@code null} value indicates that the elements' natural
         *                   ordering should be used
         * @throws ClassCastException if this list contains elements that are not
         *                            <i>mutually comparable</i> using the specified
         *                            comparator
         */
        public void mergeSort(Comparator<? super Node<E>> comparator) {
            NodableLinkedList.mergeSort(this, comparator);
        }
        
        /**
         * Returns a view of the portion of this list between the specified fromIndex,
         * inclusive, and toIndex, exclusive. (If the specified fromIndex and toIndex
         * are equal, the returned {@code SubList} is empty.) The returned
         * {@code SubList} is backed by this list, so structural changes in the returned
         * {@code SubList} are reflected in this list. The returned {@code SubList}
         * supports all of the optional {@code List} operations.
         * <p>
         * This method eliminates the need for explicit range operations (of the sort
         * that commonly exist for arrays). Any operation that expects a list can be
         * used as a range operation by passing a {@code SubList} view instead of a
         * whole list. For example, the following idiom removes a range of elements from
         * a list:
         * <pre>
         * {@code
         *      list.subList(fromIndex, toIndex).clear();
         * }
         * </pre>
         * 
         * Similar idioms may be constructed for {@code indexOf} and
         * {@code lastIndexOf}, and all of the algorithms in the {@code Collections}
         * class can be applied to a {@code SubList}.
         * <p>
         * The semantics of the {@code SubList} returned by this method become undefined
         * if the {@code NodableLinkedList} is <i>structurally modified</i> in any way
         * other than via the returned {@code SubList} or any of its sublists.
         * (Structural modifications are those that change the size of this list, or
         * otherwise perturb it in such a fashion that iterations in progress may yield
         * incorrect results.) A {@code ConcurrentModificationException} is thrown for
         * any operation on a {@code SubList} that is structurally unsound.
         *
         * @param fromIndex low endpoint (inclusive) of the {@code SubList}
         * @param toIndex   high endpoint (exclusive) of the {@code SubList}
         * @return a view of the specified range within this list
         * @throws IndexOutOfBoundsException for an illegal endpoint index value
         *                                   ({@code fromIndex < 0 || toIndex > size ||
         *                                   fromIndex > toIndex})
         */        
        @Override
        public LinkedSubNodes subList(int fromIndex, int toIndex) {
            return newSubList(fromIndex, toIndex).linkedSubNodes;
        }
        
        /**
         * Returns a NodableLinkedList.SubList.
         * 
         * @param fromIndex low endpoint (inclusive) of the {@code SubList}
         * @param toIndex   high endpoint (exclusive) of the {@code SubList}
         * @return returns a NodableLinkedList.SubList
         */
        private SubList<E> newSubList(int fromIndex, int toIndex) {
            if (fromIndex < 0) {
                throw new IndexOutOfBoundsException("fromIndex = " + fromIndex);
            }
            if (toIndex > longSize()) {
                throw new IndexOutOfBoundsException("toIndex(" + toIndex +") > size(" + longSize() + ")");
            }
            if (fromIndex > toIndex) {
                throw new IndexOutOfBoundsException("fromIndex(" + fromIndex +") > toIndex(" + toIndex + ")");
            }
            final long size = toIndex - fromIndex;
            if (fromIndex == longSize()) {
                return new SubList<E>(nodableLinkedList(),
                        iGetNodeBeforeOrHeadSentinel(iGetTailSentinel()), iGetTailSentinel(), null, size);
            }
            final LinkNode<E> headSentinel = (fromIndex == 0) ? this.iGetHeadSentinel() : iGetNode(fromIndex-1);
            final LinkNode<E> tailSentinel = (size == 0L)
                                             ? iGetNodeAfterOrTailSentinel(headSentinel)
                                             : null; // tailSentinel is currently unknown
            return new SubList<E>(nodableLinkedList(), headSentinel, tailSentinel, null, size);
        }
        
        /**
         * Returns a view of the portion of this list between the specified firstNode,
         * and lastNode (both inclusive). The returned {@code SubList} is backed by this
         * list, so structural changes in the returned {@code SubList} are reflected in
         * this list. The returned {@code SubList} supports all of the optional
         * {@code List} operations.
         * <p>
         * If the specified firstNode is {@code null}, an empty {@code SubList},
         * positioned right before the specified lastNode, is returned. If the specified
         * lastNode is {@code null}, an empty {@code SubList}, positioned right after
         * the specified firstNode, is returned. if both the specified firstNode and
         * lastNode are {@code null}, an empty {@code SubList}, positioned at the end of
         * this list, is returned.
         * <p>
         * <strong>Implementation Note:</strong> For performance reasons, this
         * implementation does not verify that the specified lastNode comes after (or
         * on) the specified firstNode in this list. Therefore, if the specified
         * lastNode does come before the specified firstNode in the list, an
         * {@code IllegalStateException} can be thrown for any subsequent operation on
         * the returned {@code SubList}, indicating that the end of the list was reached
         * unexpectedly.
         * <p>
         * This method eliminates the need for explicit range operations (of the sort
         * that commonly exist for arrays). Any operation that expects a list can be
         * used as a range operation by passing a {@code SubList} view instead of a
         * whole list. For example, the following idiom removes a range of elements from
         * a list:
         * 
         * <pre>
         * {@code
         *      list.subList(firstNode, lastNode).clear();
         * }
         * </pre>
         * 
         * Similar idioms may be constructed for {@code indexOf} and
         * {@code lastIndexOf}, and all of the algorithms in the {@code Collections}
         * class can be applied to a {@code SubList}.
         * <p>
         * The semantics of the {@code SubList} returned by this method become undefined
         * if the {@code NodableLinkedList} is <i>structurally modified</i> in any way
         * other than via the returned {@code SubList} or any of its sublists.
         * (Structural modifications are* those that change the size of this list, or
         * otherwise perturb it in such a fashion that iterations in progress may yield
         * incorrect results.) A {@code ConcurrentModificationException} is thrown for
         * any operation on a {@code SubList} that is structurally unsound.
         *
         * @param firstNode low endpoint (inclusive) of the {@code SubList}
         * @param lastNode  high endpoint (inclusive) of the {@code SubList}
         * @return a view of the specified range within this list
         * @throws IllegalArgumentException if any specified node is not linked to this
         *                                  list, or the lastNode comes right before
         *                                  the firstNode in this list.
         */
        public LinkedSubNodes subList(Node<E> firstNode, Node<E> lastNode) {
            return newSubList(firstNode, lastNode).linkedSubNodes;
        }
        
        /**
         * Returns a NodableLinkedList.SubList.
         * 
         * @param firstNode low endpoint (inclusive) of the {@code SubList}
         * @param lastNode  high endpoint (inclusive) of the {@code SubList}
         * @return returns a NodableLinkedList.SubList
         * @throws IllegalArgumentException if any specified node is not linked to this
         *                                  list, or the lastNode comes right before
         *                                  the firstNode in this list.
         */
        private SubList<E> newSubList(Node<E> firstNode, Node<E> lastNode) {
            if (firstNode == null && lastNode == null) {
                // both firstNode and lastNode are null
                return new SubList<E>(nodableLinkedList(),
                        iGetNodeBeforeOrHeadSentinel(iGetTailSentinel()), iGetTailSentinel(), null, 0L);
            } else if (firstNode == null) {
                // only the lastNode is specified
                checkListContainsNode(lastNode, "Specified last node is not linked to this list");
                return new SubList<E>(nodableLinkedList(),
                        iGetNodeBeforeOrHeadSentinel(lastNode.linkNode()), lastNode.linkNode(), null, 0L);
            } else if (lastNode == null) {
                // only the firstNode is specified
                checkListContainsNode(firstNode, "Specified first node is not linked to this list");
                return new SubList<E>(nodableLinkedList(),
                        firstNode.linkNode(), iGetNodeAfterOrTailSentinel(firstNode.linkNode()), null, 0L);
            }
            // both firstNode and LastNode are specified
            checkListContainsNode(firstNode, "Specified first node is not linked to this list");
            checkListContainsNode(lastNode, "Specified last node is not linked to this list");
            if (iGetNodeAfter(lastNode.linkNode()) == firstNode.linkNode()) {
                throw new IllegalArgumentException("Specified last Node comes before the specified first node in this list");
            }
            return new SubList<E>(nodableLinkedList(),
                    iGetNodeBeforeOrHeadSentinel(firstNode.linkNode()),
                    iGetNodeAfterOrTailSentinel(lastNode.linkNode()), null, -1L);
        }

        /**
         * Returns an array containing all of the {@code Nodes} (specifically LinkNodes)
         * in this list in proper sequence (from first to last node). The {@code Nodes}
         * in the array are still linked to this list.
         * <p>
         * Although the {@code Nodes} in the returned array are still linked to this
         * list, it is safe for the caller to modify the array. No operation on this
         * list, will modify the returned array, however, the state of the {@code Nodes}
         * in the array may change.
         * <p>
         * This method acts as bridge between array-based and collection-based APIs.
         *
         * @return an array containing all of the {@code Nodes} (specifically LinkNodes)
         *         in this list in proper sequence
         * @throws IllegalStateException if this list is too large to fit in an array
         */
        @Override
        public Object[] toArray() {
            checkListCanFitInAnArray(longSize());
            final Object[] nodes = new Object[(int) longSize()];
            int index = 0;
            for (LinkNode<E> node = iGetFirstNode(); node != null; node = iGetNodeAfter(node)) {
                nodes[index++] = node;
            }
            return nodes;
        }

        /**
         * Returns an array containing all of the {@code Nodes} (specifically LinkNodes)
         * in this list in proper sequence (from first to last node); the runtime type
         * of the returned array is that of the specified array. If the list fits in the
         * specified array, it is returned therein. Otherwise, a new array is allocated
         * with the runtime type of the specified array and the size of this list. The
         * {@code Nodes} in the array are still linked to this list.
         * <p>
         * If the list fits in the specified array with room to spare (i.e., the array
         * has more elements than the list), the element in the array immediately
         * following the end of the list is set to {@code null}. (This is useful in
         * determining the length of the list since {@code Nodes} are never null.)
         * <p>
         * Although the {@code Nodes} in the returned array are still linked to this
         * list, it is safe for the caller to modify the array. No operation on this
         * list, will modify the returned array, however, the state of the {@code Nodes}
         * in the array may change.
         * <p>
         * Like the {@link #toArray()} method, this method acts as bridge between
         * array-based and collection-based APIs. Further, this method allows precise
         * control over the runtime type of the output array, and may, under certain
         * circumstances, be used to save allocation costs.
         *
         * Note that {@code toArray(new Object[0])} is identical in function to
         * {@code toArray()}.
         *
         * @param array the array into which the {@code Nodes} of the list are to be
         *              stored, if it is big enough; otherwise, a new array of the same
         *              runtime type is allocated for this purpose.
         * @return an array containing the {@code Nodes} (specifically LinkNodes) of the
         *         list
         * @throws ArrayStoreException   if the runtime type of the specified array is
         *                               not a supertype of the runtime type of every
         *                               node in this list
         * @throws IllegalStateException if this list is too large to fit in an array
         * @throws NullPointerException  if the specified array is {@code null}
         */
        @Override
        @SuppressWarnings("unchecked")
        public <T> T[] toArray(T[] array) {
            checkListCanFitInAnArray(longSize());
            if (array.length < longSize()) {
                array = (T[]) java.lang.reflect.Array.newInstance(array.getClass().getComponentType(), (int) longSize());
            }
            int index = 0;
            Object[] nodes = array;
            for (LinkNode<E> node = iGetFirstNode(); node != null; node = iGetNodeAfter(node)) {
                nodes[index++] = node;
            }
            if (array.length > longSize()) array[(int) longSize()] = null;
            return array;
        }

        /**
         * Returns a {@code ListIterator} of the {@code LinkNodes} in this list (in
         * proper sequence), starting at the specified position in this list. Obeys the
         * general contract of {@code List.listIterator(int)}. if the specified
         * {@code index == longSize()}, the {@code ListIterator} will be positioned at
         * the end of this list.
         * <p>
         * <strong>Implementation Note:</strong> The {@code ListIterator} returned by
         * this method behaves differently when the list's
         * {@code size > Integer.MAX_VALUE}. Methods {@code nextIndex} and
         * {@code previousIndex} return -1 if the {@code index > Integer_MAX_VALUE}.
         * <p>
         * The {@code ListIterator} is <i>fail-fast</i>: if the
         * {@code NodableLinkedList} is structurally modified at any time after the
         * Iterator is created, in any way except through the {@code ListIterator's} own
         * {@code remove} or {@code add} methods, the {@code ListIterator} will throw a
         * {@code ConcurrentModificationException}. Thus, in the face of concurrent
         * modification, the iterator fails quickly and cleanly, rather than risking
         * arbitrary, non-deterministic behavior at an undetermined time in the future.
         * <p>
         * Instead of using a {@code ListIterator}, consider iterating over the list via
         * {@code Nodes}. For example:
         * 
         * <pre>
         * {@code
         *     // list is a NodableLinkedList<Integer>.LinkedNodes
         *     Node<Integer> node = list.get(index); // or list.getFirstNode();
         *     while (node != null) {
         *         System.out.println(node.element());
         *         node = node.next();
         *     }
         * }
         * </pre>
         *
         * @param index index of the first {@code LinkNode} to be returned from the
         *              {@code ListIterator} (by a call to {@code next})
         * @return a ListIterator of the {@code LinkNodes} in this list (in proper
         *         sequence), starting at the specified position in this list
         * @throws IndexOutOfBoundsException if the specified index is out of range
         *                                   {@code (index < 0 || index > longSize())}
         * @see List#listIterator(int)
         */
        @Override
        public ListIterator<Node<E>> listIterator(int index) {
            checkPositionIndex(index, longSize());
            return new LinkNodeListIterator(this, index, IndexType.ABSOLUTE, iGetNode(index));
        }

        /**
         * Returns a {@code ListIterator} of the {@code LinkNodes} in this list (in
         * proper sequence), starting at the specified node in this list. if the
         * specified node is {@code null}, the {@code ListIterator} will be positioned
         * right after the last {@code Node} in this list.
         * <p>
         * <strong>Implementation Note:</strong> The index returned by the returned
         * {@code ListIterator's} methods {@code nextIndex} and {@code previousIndex} is
         * relative to the specified node which has an index of zero. Nodes which come
         * before the specified node in this list, will have a negative index; nodes
         * that come after will have a positive index. Method {@code nextIndex} returns
         * {@code longSize()} if the {@code ListIterator} is positioned at the end of
         * the list, and method {@code previousIndex} returns {@code -longSize()} if the
         * {@code ListIterator} is positioned at the beginning of the list. if
         * {@code index < Integer.MIN_VALUE or index > Integer.MAX_VALUE},
         * {@code Integer.MIN_VALUE} or {@code Integer.MAX_VALUE} is returned
         * respectively.
         * <p>
         * The {@code ListIterator} is <i>fail-fast</i>: if the
         * {@code NodableLinkedList} is structurally modified at any time after the
         * Iterator is created, in any way except through the {@code ListIterator's} own
         * {@code remove} or {@code add} methods, the {@code ListIterator} will throw a
         * {@code ConcurrentModificationException}. Thus, in the face of concurrent
         * modification, the iterator fails quickly and cleanly, rather than risking
         * arbitrary, non-deterministic behavior at an undetermined time in the future.
         * <p>
         * Instead of using a {@code ListIterator}, consider iterating over the list via
         * {@code Nodes}. For example:
         * 
         * <pre>
         * {@code
         *     // list is a NodableLinkedList<Integer>.LinkedNodes
         *     // make sure node is a forward traversing LinkNode of this list
         *     Node<Integer> linkNode = (node == null) ? null : list.forwardLinkNodeOf(node);
         *     while (linkNode != null) {
         *         System.out.println(linkNode.element());
         *         linkNode = linkNode.next();
         *     }
         * }
         * </pre>
         *
         * @param node first {@code Node} to be returned from the {@code ListIterator}
         *             (by a call to {@code next})
         * @return a ListIterator of the {@code LinkNodes} in this list (in proper
         *         sequence), starting at the specified node in this list
         * @throws IllegalArgumentException if the specified node is not linked to this
         *                                  list
         */
        public ListIterator<Node<E>> listIterator(Node<E> node) {
            if (node != null) checkListContainsNode(node);
            return new LinkNodeListIterator(this, 0L, IndexType.RELATIVE,
                    (node == null) ? this.iGetTailSentinel() : node.linkNode());
        }

        /**
         * Returns an iterator over the {@code Nodes} in this list in reverse sequential
         * order. The {@code Nodes} will be returned in order from last (tail) to first
         * (head).
         * <p>
         * Instead of using a descending {@code Iterator}, consider iterating over the
         * list via {@code Nodes}. For example:
         * <pre>
         * {@code
         *     Node<Integer> linkNode = getLast();
         *     while (linkNode != null) {
         *         System.out.println(linkNode.element());
         *         linkNode = linkNode.previous();
         *     }
         *     
         *     or use a reversed {@code LinkNode}:
         *     
         *     Node<Integer> linkNode = getLast();
         *     if (linkNode != null) linkNode = linkNode.reversed();
         *     while (linkNode != null) {
         *         System.out.println(linkNode.element());
         *         linkNode = linkNode.next();
         *     }
         * }
         * </pre>
         *
         * @return an iterator over the {@code LinkNodes} in this list in reverse
         *         order
         */
        @Override
        public Iterator<Node<E>> descendingIterator() {
            final LinkNode<E> node = isEmpty() ? iGetHeadSentinel() : iGetLastNode();
            return new LinkNodeReverseListIterator(this, 0L, IndexType.ABSOLUTE, node); 
        }

        /**
         * Creates a <i>late-binding</i> and <i>fail-fast</i> {@link Spliterator} over
         * the {@code Nodes} in this list.
         * <p>
         * The {@code Spliterator} reports {@link Spliterator#SIZED} and
         * {@link Spliterator#ORDERED}. Overriding implementations should document the
         * reporting of additional characteristic values.
         * <p>
         * <strong>Implementation Note:</strong> The {@code Spliterator} additionally
         * reports {@link Spliterator#SUBSIZED} and {@link Spliterator#NONNULL}, and
         * implements {@code trySplit} to permit limited parallelism..
         *
         * @return a {@code Spliterator} over the {@code LinkNodes} in this list
         */
        @Override
        public Spliterator<Node<E>> spliterator() {
            return new NodeSpliterator<E>(this);
        }

    } // LinkedNodes
    
    private class ReversedLinkedNodes extends LinkedNodes {
        
        /**
         * The LinkedNodes that was reversed.
         */
        private final LinkedNodes linkedNodes;
        
        /**
         * Construct a new ReversedLinkedNodes.
         * 
         * @param linkedNodes The LinkedNodes that was reversed
         */
        private ReversedLinkedNodes(LinkedNodes linkedNodes) {
            this.linkedNodes = linkedNodes;
        }
        
        @Override
        int modCount() {
            return linkedNodes.modCount();
        }
        
        /**
         * Update the protected int modCount inherited from class
         * java.util.AbstractList, with the modCount of the LinkedNodes that was
         * reversed. The modCount for the ReversedNodableLinkedList which this
         * ReversedLinkedNodes object backs, is also kept in sync.
         */
        private void updateModCount() {
            NodableLinkedList.this.modCount = this.modCount = modCount();
        }
        
        @Override
        public long longSize() {
            updateModCount();
            return linkedNodes.longSize();
        }
        
        @Override
        long iGetSize() {
            updateModCount();
            return linkedNodes.iGetSize();
        }        
        
        @Override
        void iAddNodeAfter(LinkNode<E> node, LinkNode<E> afterThisNode) {
            linkedNodes.iAddNodeBefore(node, afterThisNode);
            updateModCount();
        }
    
        @Override
        void iAddNodeBefore(LinkNode<E> node, LinkNode<E> beforeThisNode) {
            linkedNodes.iAddNodeAfter(node, beforeThisNode);
            updateModCount();
        }
    
        @Override
        void iRemoveNode(LinkNode<E> node) {
            linkedNodes.iRemoveNode(node);
            updateModCount();
        }
    
        @Override
        void iReplaceNode(LinkNode<E> node, LinkNode<E> replacementNode) {
            linkedNodes.iReplaceNode(node, replacementNode);
            updateModCount();
        }
    
        @Override
        boolean iHasNodeAfter(LinkNode<E> node) {
            updateModCount();
            return linkedNodes.iHasNodeBefore(node);           
        }
    
        @Override
        boolean iHasNodeBefore(LinkNode<E> node) {
            updateModCount();
            return linkedNodes.iHasNodeAfter(node);           
        }
        
        @Override
        LinkNode<E> iGetHeadSentinel() {
            updateModCount();
            return linkedNodes.iGetTailSentinel();
        }
        
        @Override
        LinkNode<E> iGetTailSentinel() {
            updateModCount();
            return linkedNodes.iGetHeadSentinel();
        }
        
        @Override
        LinkNode<E> iGetMyLinkedNodesHeadSentinel() {
            updateModCount();
            return linkedNodes.iGetMyLinkedNodesTailSentinel();
        }
        
        @Override
        LinkNode<E> iGetMyLinkedNodesTailSentinel() {
            updateModCount();
            return linkedNodes.iGetMyLinkedNodesHeadSentinel();
        }
        
        @Override
        LinkNode<E> iGetFirstNode() {
            updateModCount();
            return linkedNodes.iGetLastNode();
        }
        
        @Override
        LinkNode<E> iGetLastNode() {
            updateModCount();
            return linkedNodes.iGetFirstNode();
        }        
    
        @Override
        LinkNode<E> iGetNodeAfter(LinkNode<E> node) {
            updateModCount();
            return linkedNodes.iGetNodeBefore(node);
        }
    
        @Override
        LinkNode<E> iGetNodeAfterOrTailSentinel(LinkNode<E> node) {
            updateModCount();
            return linkedNodes.iGetNodeBeforeOrHeadSentinel(node);
        }
        
        @Override
        LinkNode<E> iGetNodeAfterFromListWithKnownTailSentinel(LinkNode<E> node) {
            updateModCount();
            return linkedNodes.iGetNodeBeforeOrHeadSentinel(node);
        }
    
        @Override
        LinkNode<E> iGetNodeBefore(LinkNode<E> node) {
            updateModCount();
            return linkedNodes.iGetNodeAfter(node);
        }
        
        @Override
        LinkNode<E> iGetNodeBeforeOrHeadSentinel(LinkNode<E> node) {
            updateModCount();
            return linkedNodes.iGetNodeAfterOrTailSentinel(node);
        }
        
        @Override
        public LinkNode<E> getFirstNode() {
            final LinkNode<E> linkNode = super.getFirstNode();
            return (linkNode == null) ? null : linkNode.reversed();
        }

        @Override
        public LinkNode<E> getLastNode() {
            final LinkNode<E> linkNode = super.getLastNode();
            return (linkNode == null) ? null : linkNode.reversed();
        }
        
        @Override
        boolean contains(Node<?> node) {
             return node != null && node.linkedNodes() == linkedNodes;
        }
        
        @Override
        public LinkNode<E> forwardLinkNodeOf(Node<E> node) {
            return linkedNodes.forwardLinkNodeOf(node).reversed();
        }
        
        @Override
        public boolean isReversed() {
            return true;
        }        
        
        @Override
        public LinkedNodes reversed() {
            updateModCount();
            return this.linkedNodes;
        }
        
        @Override
        public LinkNode<E> element() {
            return super.element().reversed();
        }
        
        @Override
        public LinkNode<E> get(int index) {
            updateModCount();
            return super.get(index).reversed();
            
        }
        
        @Override
        public LinkNode<E> getFirst() {
            return super.getFirst().reversed();
        }
        
        @Override
        public LinkNode<E> getLast() {
            return super.getLast().reversed();
        }
        
        @Override
        public LinkNode<E> peekFirst() {
            final LinkNode<E> linkNode = super.peekFirst();
            return (linkNode == null) ? null : linkNode.reversed();
        }
        
        @Override
        public LinkNode<E> peekLast() {
            final LinkNode<E> linkNode = super.peekLast();
            return (linkNode == null) ? null : linkNode.reversed();
        }
        
    } // ReversedLinkedNodes

    /**
     * Sublist of a {@link NodableLinkedList}. Implements all optional {@code List}
     * interface operations, and permits all elements (including {@code null}). A
     * {@code SubList} represents a range of elements of a
     * {@code NodableLinkedList}. If an element is added to or removed from a
     * {@code SubList}, the element is respectively added to or removed from the
     * backing {@code NodableLinkedList}.
     * <p>
     * Just like a {@code NodableLinkedList}, there are two ways to visualize a
     * {@code NodableLinkedList.SubList}: one as a list of elements (the standard
     * view), and the other as a list of nodes. The latter view is implemented by
     * the inner class {@link NodableLinkedList.LinkedSubNodes}. A {@code SubList}
     * is backed by one and only one {@code LinkedSubNodes} instance. Method
     * {@link #linkedSubNodes()} returns the {@code LinkedSubNodes} object backing
     * a {@code NodableLinkedList.SubList}.
     * <p>
     * <strong>Implementation Note:</strong> When a {@code SubList} is created,
     * normally either the last node is unknown (if created via subList(fromIndex,
     * toIndex)), or the size is unknown (if created via subList(firstNode,
     * lastNode)). To determine the unknown size or unknown last node, the entire
     * sublist has to be traversed from the first node to the last node. Therefore,
     * this implementation delays making that determination until it's necessary (if
     * ever). This can have other performance implications. for instance, normally
     * for a doubly-linked list, you would expect operations that index into the
     * list will traverse the list from the beginning or the end, whichever is
     * closer to the specified index. However, if either the size or the last node
     * is unknown (or it's uncertain if the specified last node actually comes after
     * the first node in the sublist), then this implementation has no choice but to
     * traverse the sublist from the beginning, even if the index is closer to the
     * end of the sublist. Performing an operation like {@code getLastNode()} forces
     * this implementation to determine both the size and the last node. If the size
     * and last node are already known, the {@code getLastNode()} operation will
     * perform in constant time.
     * <p>
     * If the {@code NodableLinkedList} which backs this {@code SubList}, is
     * structurally modified after the {@code SubList} is created, except via
     * operations on the {@code SubList} itself, a
     * {@code ConcurrentModificationException} will be thrown for any future
     * operations on the {@code SubList}.
     * <p>
     * The iterators returned by this class's {@code iterator} and
     * {@code listIterator} methods are <i>fail-fast</i>: if the
     * {@code NodableLinkedList} which backs this {@code SubList}, is structurally
     * modified after the iterator is created, in any way except through the
     * Iterator's own {@code remove} or {@code add} methods, the iterator will throw
     * a {@code ConcurrentModificationException}. Thus, in the face of concurrent
     * modification, the iterator fails quickly and cleanly, rather than risking
     * arbitrary, non-deterministic behavior at an undetermined time in the future.
     * <p>
     * Note that the fail-fast behavior of an iterator cannot be guaranteed as it
     * is, generally speaking, impossible to make any hard guarantees in the
     * presence of unsynchronized concurrent modification. Fail-fast iterators throw
     * {@code ConcurrentModificationException} on a best-effort basis. Therefore, it
     * would be wrong to write a program that depended on this exception for its
     * correctness: <i>the fail-fast behavior of iterators should be used only to
     * detect bugs.</i>
     * 
     * @author James Pfeifer
     *
     */
    public static class SubList<E> extends AbstractSequentialList<E> implements List<E> {
        
        /**
         * The NodableLinkedList which contains this SubList.
         */
        private final NodableLinkedList<E> list;
        
        /**
         * The doubly-linked list of nodes which back this SubList.
         */
        private final NodableLinkedList<E>.LinkedSubNodes linkedSubNodes;
        
        /**
         * Constructs a new SubList.
         * 
         * @param list         The NodableLinkedList which contains this sublist
         * @param headSentinel The LinkNode which represents this SubList's head
         *                     sentinel
         * @param tailSentinel The LinkNode which represents this SubList's tail
         *                     sentinel, or null if the tail sentinel is unknown
         * @param parent       The SubList which contains this SubList if this SubList
         *                     is a sublist of a SubList
         * @param size         the number of nodes/elements in this SubList, or -1 if
         *                     the size is unknown
         */
        private SubList(NodableLinkedList<E> list,
                LinkNode<E> headSentinel, LinkNode<E> tailSentinel,
                SubList<E> parent, long size) {
            this.list = list;
            this.linkedSubNodes = list.new LinkedSubNodes(headSentinel, tailSentinel, this, parent, size);
        }
        
        /**
         * Constructs the super SubList of a ReversedSubList. This constructor is
         * invoked by the ReversedSubList constructor.
         * 
         * @param sublist The SubList that was reversed
         */
        private SubList(SubList<E> sublist) {
            this.list = sublist.nodableLinkedList();
            this.linkedSubNodes = list.new ReversedLinkedSubNodes(this, sublist.linkedSubNodes());
        }
        
        /**
         * Check if the expected modCount matches the list's modCount and throw a
         * ConcurrentModificationException if they don't match. The protected int
         * modCount inherited from class java.util.AbstractList represents the expected
         * modCount.
         * 
         * @throws ConcurrentModificationException if the expected modCount does not
         *                                         match the list's modCount
         */
        private void checkForModificationException() {
            if (this.modCount != list.modCount()) {
                throw new ConcurrentModificationException();
            }
        }
        
        /**
         * Set the (expected) modCount to the specified value.
         * 
         * @param modCount new (expected) modCount value
         */
        private void setModCount(int modCount) {
            this.modCount = modCount;
        }
        
        /**
         * Returns the {@code NodableLinkedList} which contains this {@code SubList}.
         * 
         * @return the {@code NodableLinkedList} which contains this {@code SubList}
         */
        public NodableLinkedList<E> nodableLinkedList() {
            return this.list;
        }
        
        /**
         * Returns the sublist of nodes which back this {@code SubList}
         * 
         * @return the sublist of nodes which back this {@code SubList}
         */
        public NodableLinkedList<E>.LinkedSubNodes linkedSubNodes() {
            return this.linkedSubNodes;
        }

        /**
         * Appends the specified node to the end of this {@code SubList}. The specified
         * node must not already belong to a list.
         * <p>
         * If the specified node is a {@code SubListNode}, after this operation is
         * completed, it will be associated with this {@code SubList}.
         * <p>
         * This method is equivalent to {@link #addNodeLast}.
         *
         * @param node {@code Node} to be appended to the end of this {@code SubList}
         * @throws IllegalArgumentException if the specified node is {@code null} or
         *                                  already a node of a list
         */
        public void addNode(Node<E> node) {
            addNodeLast(node);
        }

        /**
         * Inserts the specified node at the beginning of this {@code SubList}. The
         * specified node must not already belong to a list.
         * <p>
         * If the specified node is a {@code SubListNode}, after this operation is
         * completed, it will be associated this {@code SubList}.
         *
         * @param node the {@code Node} to be inserted at the beginning of this
         *             {@code SubList}
         * @throws IllegalArgumentException if the specified node is {@code null} or
         *                                  already a node of a list
         */
        public void addNodeFirst(Node<E> node) {
            checkForModificationException();
            checkNodeIsUnLinked(node);
            linkedSubNodes.iAddNodeFirst(node.linkNode());
            if (node.isSubListNode()) {
                final SubListNode<E> subListNode = (SubListNode<E>)node;
                subListNode.setSubList(this);
                subListNode.updateExpectedModCount();
            }
        }

        /**
         * Appends the specified node to the end of this {@code SubList}. The specified
         * node must not already belong to a list.
         * <p>
         * If the specified node is a {@code SubListNode}, after this operation is
         * completed, it will be associated with this {@code SubList}.
         *
         * @param node {@code Node} to be appended to the end of this {@code SubList}
         * @throws IllegalArgumentException if the specified node is {@code null} or
         *                                  already a node of a list
         */
        public void addNodeLast(Node<E> node) {
            checkForModificationException();
            checkNodeIsUnLinked(node);
            linkedSubNodes.iAddNodeLast(node.linkNode());
            if (node.isSubListNode()) {
                final SubListNode<E> subListNode = (SubListNode<E>)node;
                subListNode.setSubList(this);
                subListNode.updateExpectedModCount();
            }
        }
        
        /**
         * Returns the first {@code SubListNode} of this {@code SubList}, or
         * {@code null} if this {@code SubList} is empty.
         *
         * @return the first {@code SubListNode} of this {@code SubList}, or
         *         {@code null} if this {@code SubList} is empty
         */
        public SubListNode<E> getFirstNode() {
            checkForModificationException();
            return (this.isEmpty()) ? null : new SubListNode<E>(linkedSubNodes.iGetFirstNode(), this);
        }
        
        /**
         * Returns the last {@code SubListNode} of this {@code SubList}, or {@code null}
         * if this {@code SubList} is empty.
         *
         * @return the last {@code SubListNode} of this {@code SubList}, or {@code null}
         *         if this {@code SubList} is empty
         */
        public SubListNode<E> getLastNode() {
            checkForModificationException();
            return (this.isEmpty()) ? null : new SubListNode<E>(linkedSubNodes.iGetLastNode(), this);
        }
        
        /**
         * Removes and returns the first {@code SubListNode} of this {@code SubList}, or
         * returns {@code null} if this {@code SubList} is empty.
         *
         * @return the first {@code SubListNode} of this {@code SubList} that was
         *         removed, or {@code null} if this {@code SubList} is empty
         */      
        public SubListNode<E> removeFirstNode() {
            final SubListNode<E> firstNode = getFirstNode();
            if (firstNode != null) linkedSubNodes.iRemoveNode(firstNode.linkNode());
            return firstNode;
        }
        
        /**
         * Removes and returns the last {@code SubListNode} of this {@code SubList}, or
         * returns {@code null} if this {@code SubList} is empty.
         *
         * @return the last {@code SubListNode} of this {@code SubList} that was
         *         removed, or {@code null} if this {@code SubList} is empty
         */        
        public SubListNode<E> removeLastNode() {
            final SubListNode<E> lastNode = getLastNode();
            if (lastNode != null) linkedSubNodes.iRemoveNode(lastNode.linkNode());
            return lastNode;
        }        

        /**
         * Returns the number of elements in this {@code SubList}. If this
         * {@code SubList} contains more than Integer.MAX_VALUE elements,
         * Integer.MAX_VALUE is returned.
         *
         * @return the number of elements in this {@code SubList}
         */
        @Override
        public int size() {
            return linkedSubNodes.size();
        }

        /**
         * Returns the number of elements in this {@code SubList}.
         *
         * @return the number of elements in this {@code SubList}
         */
        public long longSize() {
            return linkedSubNodes.longSize();
        }
        
        /**
         * Returns {@code true} if this {@code SubList} contains no elements.
         *
         * @return {@code true} if this {@code SubList} contains no elements
         */
        @Override
        public boolean isEmpty() {
            return linkedSubNodes.isEmpty();
        }
        
        /**
         * Removes all of the elements from this {@code SubList}.
         */
        @Override
        public void clear() {
            linkedSubNodes.clear();
        }
        
        /**
         * Returns the element at the specified position in this {@code SubList}.
         *
         * @param index index of the element to return
         * @return the element at the specified position in this {@code SubList}
         * @throws IndexOutOfBoundsException if the specified index is out of range
         *                                   {@code (index < 0 || index >= longSize())}
         */
        @Override
        public E get(int index) {
            return linkedSubNodes.get(index).element();
        }
        
        /**
         * Inserts the specified element at the specified position in this
         * {@code SubList}. Shifts the element currently at that position (if any) and
         * any subsequent elements to the right (adds one to their indices). if the
         * specified {@code index == longSize()}, the specified element will be appended
         * to the end of this list.
         *
         * @param index   position where the specified element is to be inserted
         * @param element element to be inserted
         * @throws IndexOutOfBoundsException if the specified index is out of range
         *                                   {@code (index < 0 || index > longSize())}
         */
        @Override
        public void add(int index, E element) {
            linkedSubNodes.add(index, node(element));
        }
        
        /**
         * Removes and returns the element at the specified position in this
         * {@code SubList}. Shifts any subsequent elements to the left (subtracts one
         * from their indices).
         *
         * @param index the index of the element to be removed
         * @return the element previously at the specified position
         * @throws IndexOutOfBoundsException if the specified index is out of range
         *                                   {@code (index < 0 || index >= longSize())}
         */
        @Override
        public E remove(int index) {
            return linkedSubNodes.remove(index).element();
        }
        
        /**
         * Returns {@code true} if this {@code SubList} is a reverse-ordered view of the
         * base {@code NodableLinkedList}.
         * 
         * Note, {@code true} doesn't mean this is a {@code SubList} that has been
         * reversed. It means this {@code SubList's} traversal direction is in the
         * opposite direction than the traversal direction of the base
         * {@code NodableLinkedList} (the {@code NodableLinkedList} initially created).
         * 
         * @return {@code true} if this {@code SubList} is a reverse-ordered view of the
         *         base {@code NodableLinkedList}
         */
        public boolean isReversed() {
            return linkedSubNodes.isReversed();
        }        
        
        /**
         * Returns a reverse-order view of this {@code SubList}.
         * 
         * The encounter order of elements in the returned view is the inverse of the
         * encounter order of elements in this {@code SubList}. The reverse ordering
         * affects all order-sensitive operations, including those on the view
         * collections of the returned view. Modifications to the reversed view are
         * permitted and will be propagated to this {@code SubList}. In addition,
         * modifications to this {@code SubList} will be visible in the reversed view.
         * 
         * @return a reverse-order view of this {@code SubList}
         */
        //@Override //not until JDK 21
        public SubList<E> reversed() {
            linkedSubNodes.getConfirmedTailSentinel(); // make sure new reversed sublist's headSentinel is valid
            return new ReversedSubList<E>(this);
        }
        
        /**
         * Replaces the element at the specified position in this {@code SubList} with
         * the specified element.
         *
         * @param index   index of the element to replace
         * @param element element to be stored at the specified position
         * @return the element previously at the specified position
         * @throws IndexOutOfBoundsException if the specified index is out of range
         *                                   {@code (index < 0 || index >= longSize())}
         */
        @Override
        public E set(int index, E element) {
            return linkedSubNodes.get(index).set(element);
        }
        
        /**
         * Appends all of the elements in the specified collection to the end of this
         * {@code SubList}, in the order that they are returned by the specified
         * collection's iterator.
         * <p>
         * The behavior of this operation is undefined if the specified collection is
         * modified while the operation is in progress. (Note that this will occur if
         * the specified collection is this {@code SubList}, and it's nonempty.)
         *
         * @param collection collection containing elements to be added to this
         *                   {@code SubList}
         * @return {@code true} if this {@code SubList} changed as a result of the call
         * @throws NullPointerException if the specified collection is {@code null}
         */
        @Override
        public boolean addAll(Collection<? extends E> collection) {
            checkForModificationException();
            boolean changed = false;
            final LinkNode<E> tailSentinel = linkedSubNodes.getConfirmedTailSentinel();
            for (E element: collection) {
                linkedSubNodes.iAddNodeBefore(node(element), tailSentinel);
                changed = true;
            }
            return changed;
        }
        
        /**
         * Inserts all of the elements in the specified collection into this
         * {@code SubList}, starting at the specified position. Shifts the element
         * currently at that position (if any) and any subsequent elements to the right
         * (increases their indices). The new elements will appear in this
         * {@code SubList} in the order that they are returned by the specified
         * collection's iterator. if the specified {@code index == longSize()}, the
         * elements will be appended to the end of this {@code SubList}.
         * 
         * Note that {@code addAll(longSize(), Collection)} is identical in function to
         * {@code addAll(Collection)}.
         * <p>
         * The behavior of this operation is undefined if the specified collection is
         * modified while the operation is in progress. (Note that this will occur if
         * the specified collection is this {@code SubList}, and it's nonempty.)
         *
         * @param index      position where to insert the first element from the
         *                   specified collection
         * @param collection collection containing elements to be added to this
         *                   {@code SubList}
         * @return {@code true} if this {@code SubList} changed as a result of the call
         * @throws IndexOutOfBoundsException if the specified index is out of range
         *                                   {@code (index < 0 || index > longSize())}
         * @throws NullPointerException      if the specified collection is {@code null}
         */
        @Override
        public boolean addAll(int index, Collection<? extends E> collection) {
            checkForModificationException();
            if (linkedSubNodes.sizeIsKnown()) checkPositionIndex(index, longSize());
            boolean changed = false;
            final LinkNode<E> beforeThisNode = linkedSubNodes.iGetNode(index);
            for (E element : collection) {
                linkedSubNodes.iAddNodeBefore(node(element), beforeThisNode);
                changed = true;
            }
            return changed;
        }
        
        /**
         * Inserts all of the elements in the specified collection into this
         * {@code SubList}, before the specified node. Shifts the element of the
         * specified node and any subsequent elements to the right (increases their
         * indices). The new elements will appear in this {@code SubList} in the order
         * that they are returned by the specified collection's iterator. if the
         * specified node is {@code null}, the elements will be appended to the end of
         * this {@code SubList}.
         * 
         * Note that {@code addAll(null, Collection)} is identical in function to
         * {@code addAll(Collection)}.
         * <p>
         * The behavior of this operation is undefined if the specified collection is
         * modified while the operation is in progress. (Note that this will occur if
         * the specified collection is this {@code SubList}, and it's nonempty.)
         *
         * @param node       {@code Node} the specified collection is to be inserted
         *                   before
         * @param collection collection containing elements to be added to this list
         * @return {@code true} if this {@code SubList} changed as a result of the call
         * @throws IllegalArgumentException if the specified node is not linked to this
         *                                  list
         * @throws NullPointerException     if the specified collection is {@code null}
         */
        public boolean addAll(Node<E> node, Collection<? extends E> collection) {
            checkForModificationException();
            if (node == null) return addAll(collection);
            linkedSubNodes.checkSubListContainsNode(node);
            final LinkNode<E> beforeThisNode = node.linkNode();
            boolean changed = false;
            for (E element: collection) {
                linkedSubNodes.iAddNodeBefore(node(element), beforeThisNode);
                changed = true;
            }
            return changed;
        }

        /**
         * Sorts this {@code SubList} according to the order induced by the specified
         * comparator.
         * <p>
         * All elements in this {@code SubList} must be <i>mutually comparable</i> using
         * the specified comparator (that is, {@code c.compare(e1, e2)} must not throw a
         * {@code ClassCastException} for any elements {@code e1} and {@code e2} in the
         * {@code SubList}).
         *
         * If the specified comparator is {@code null} then all elements in this
         * {@code SubList} must implement the {@code Comparable} interface and the
         * elements' natural ordering should be used.
         * <p>
         * <strong>Implementation Specification:</strong> This implementation obtains an
         * array containing all nodes in this {@code SubList}, sorts the array using
         * {@code Arrays.sort(T[] a, Comparator<? super T> c)}, and then effectively
         * clears the {@code SubList} and puts the sorted nodes from the array back into
         * this {@code SubList} in order. If this {@code SubList's}
         * {@code size > Integer.MAX_VALUE-8}, a {@link #mergeSort} is performed.
         * <p>
         * <strong>Implementation Note:</strong> This implementation is a stable,
         * adaptive, iterative mergesort that requires far fewer than n lg(n)
         * comparisons when the input array is partially sorted, while offering the
         * performance of a traditional mergesort when the input array is randomly
         * ordered. If the input array is nearly sorted, the implementation requires
         * approximately n comparisons. Temporary storage requirements vary from a small
         * constant for nearly sorted input arrays to n/2 object references for randomly
         * ordered input arrays.
         * <p>
         * The implementation takes equal advantage of ascending and descending order in
         * its input array, and can take advantage of ascending and descending order in
         * different parts of the same input array. It is well-suited to merging two or
         * more sorted arrays: simply concatenate the arrays and sort the resulting
         * array.
         * <p>
         * The implementation was adapted from Tim Peters's list sort for Python
         * (<a href="http://svn.python.org/projects/python/trunk/Objects/listsort.txt">
         * TimSort</a>). It uses techniques from Peter McIlroy's "Optimistic Sorting and
         * Information Theoretic Complexity", in Proceedings of the Fourth Annual
         * ACM-SIAM Symposium on Discrete Algorithms, pp 467-474, January 1993.
         *
         * @see Arrays#sort
         * @param comparator the {@code Comparator} used to compare {@code SubList}
         *                   elements. A {@code null} value indicates that the elements'
         *                   natural ordering should be used
         * @throws ClassCastException if this sublist contains elements that are not
         *                            {@code mutually comparable} using the specified
         *                            comparator
         */        
        @Override
        public void sort(Comparator<? super E> comparator) {
            if (comparator == null) {
                linkedSubNodes.sort(null);
            } else {
                linkedSubNodes.sort((node1, node2) -> {
                    return comparator.compare(node1.element(), node2.element());
                });
            }
        }
        
        /**
         * Sorts this {@code SubList} according to the order induced by the specified
         * comparator.
         * <p>
         * All elements in this {@code SubList} must be <i>mutually comparable</i> using
         * the specified comparator (that is, {@code c.compare(e1, e2)} must not throw a
         * {@code ClassCastException} for any elements {@code e1} and {@code e2} in the
         * {@code SubList}).
         *
         * If the specified comparator is {@code null} then all elements in this
         * {@code SubList} must implement the {@code Comparable} interface and the
         * elements' natural ordering should be used.
         * <p>
         * <strong>Implementation Note:</strong> This implementation is a stable,
         * iterative mergesort that requires n lg(n) comparisons. this implementation
         * avoids the N auxiliary storage cost normally associated with a mergesort.
         *
         * The implementation was adapted from Simon Tatham's Mergesort for Linked Lists
         * (<a href=
         * "https://www.chiark.greenend.org.uk/~sgtatham/algorithms/listsort.html">
         * SimonTathamMergesort</a>).
         *
         * @param comparator the {@code Comparator} used to compare list elements. A
         *                   {@code null} value indicates that the elements' natural
         *                   ordering should be used
         * @throws ClassCastException if this sublist contains elements that are not
         *                            <i>mutually comparable</i> using the specified
         *                            comparator
         */
        public void mergeSort(Comparator<? super E> comparator) {
            if (comparator == null) {
                linkedSubNodes.mergeSort(null);
            } else {
                linkedSubNodes.mergeSort((node1, node2) -> {
                    return comparator.compare(node1.element(), node2.element());
                });
            }
        } 
        
        /**
         * Returns a view of the portion of this {@code SubList} between the specified
         * fromIndex, inclusive, and toIndex, exclusive. (If the specified fromIndex and
         * toIndex are equal, the returned {@code SubList} is empty.) The returned
         * {@code SubList} is backed by this {@code SubList}, so structural changes in
         * the returned {@code SubList} are reflected in this {@code SubList}. The
         * returned {@code SubList} supports all of the optional {@code List} operations.
         * <p>
         * This method eliminates the need for explicit range operations (of the sort
         * that commonly exist for arrays). Any operation that expects a list can be
         * used as a range operation by passing a {@code SubList} view instead of a
         * whole list. For example, the following idiom removes a range of elements from
         * a list:
         * <pre>
         * {@code
         *      list.subList(fromIndex, toIndex).clear();
         * }
         * </pre>
         * 
         * Similar idioms may be constructed for {@code indexOf} and
         * {@code lastIndexOf}, and all of the algorithms in the {@code Collections}
         * class can be applied to a {@code SubList}.
         * <p>
         * The semantics of the {@code SubList} returned by this method become undefined
         * if the {@code NodableLinkedList} is <i>structurally modified</i> in any way
         * other than via the returned {@code SubList} or any of its sublists.
         * (Structural modifications are those that change the size of this
         * {@code SubList}, or otherwise perturb it in such a fashion that iterations in
         * progress may yield incorrect results.) A
         * {@code ConcurrentModificationException} is thrown for any operation on a
         * {@code SubList} that is structurally unsound.
         *
         * @param fromIndex low endpoint (inclusive) of the {@code SubList}
         * @param toIndex   high endpoint (exclusive) of the {@code SubList}
         * @return a view of the specified range within this {@code SubList}
         * @throws IndexOutOfBoundsException for an illegal endpoint index value
         *                                   ({@code fromIndex < 0 || toIndex > size ||
         *                                   fromIndex > toIndex})
         */
        @Override
        public SubList<E> subList(int fromIndex, int toIndex) {
            return linkedSubNodes.newSubList(fromIndex, toIndex);
        }
        
        /**
         * Returns a view of the portion of this {@code SubList} between the specified
         * firstNode, and lastNode (both inclusive). The returned {@code SubList} is
         * backed by this {@code SubList}, so structural changes in the returned
         * {@code SubList} are reflected in this {@code SubList}. The returned
         * {@code SubList} supports all of the optional {@code List} operations.
         * <p>
         * If the specified firstNode is {@code null}, an empty {@code SubList},
         * positioned right before the specified lastNode, is returned. If the specified
         * lastNode is {@code null}, an empty {@code SubList}, positioned right after
         * the specified firstNode, is returned. if both the specified firstNode and
         * lastNode are {@code null}, an empty {@code SubList}, positioned at the end of
         * this {@code SubList}, is returned.
         * <p>
         * This method verifies the specified lastNode comes after the specified
         * FirstNode in this {@code SubList}. Also, the size of the returned
         * {@code SubList} is known.
         * <p>
         * This method eliminates the need for explicit range operations (of the sort
         * that commonly exist for arrays). Any operation that expects a list can be
         * used as a range operation by passing a {@code SubList} view instead of a
         * whole list. For example, the following idiom removes a range of elements from
         * a list:
         * <pre>
         * {@code
         *      list.subList(firstNode, lastNode).clear();
         * }
         * </pre>
         * 
         * Similar idioms may be constructed for {@code indexOf} and
         * {@code lastIndexOf}, and all of the algorithms in the {@code Collections}
         * class can be applied to a {@code SubList}.
         * <p>
         * The semantics of the {@code SubList} returned by this method become undefined
         * if the {@code NodableLinkedList} is <i>structurally modified</i> in any way
         * other than via the returned {@code SubList} or any of its sublists.
         * (Structural modifications are those that change the size of this
         * {@code SubList}, or otherwise perturb it in such a fashion that iterations in
         * progress may yield incorrect results.) A
         * {@code ConcurrentModificationException} is thrown for any operation on a
         * {@code SubList} that is structurally unsound.
         *
         * @param firstNode low endpoint (inclusive) of the {@code SubList}
         * @param lastNode  high endpoint (inclusive) of the {@code SubList}
         * @return a view of the specified range within this {@code SubList}
         * @throws IllegalArgumentException if any specified node is not linked to this
         *                                  {@code SubList}, or the lastNode comes
         *                                  before the firstNode in this {@code SubList}
         */
        public SubList<E> subList(Node<E> firstNode, Node<E> lastNode) {
            checkForModificationException();
            return linkedSubNodes.newSubList(firstNode, lastNode);
        }
        
        /**
         * Returns a new unlinked {@code SubListNode} associated with this
         * {@code SubList} and backed by a new {@code LinkNode} containing a null
         * element.
         * 
         * @return an unlinked {@code SubListNode} containing a null element
         */
        public SubListNode<E> subListNode() {
            return subListNode(null);
        }
        
        /**
         * Returns a new unlinked {@code SubListNode} associated with this
         * {@code SubList} and backed by a new {@code LinkNode} containing the
         * specified element which can be {@code null}.
         * 
         * @param element element to be contained within the returned
         *                {@code SubListNode}
         * @return an unlinked {@code SubListNode} containing the specified element
         */
        public SubListNode<E> subListNode(E element) {
            return new SubListNode<E>(node(element), this);
        }

        /**
         * Returns an array containing all of the elements in this {@code SubList} in
         * proper sequence (from first to last element).
         * <p>
         * The returned array will be "safe" in that no references to it are maintained
         * by this {@code SubList}. (In other words, this method must allocate a new
         * array). The caller is thus free to modify the returned array.
         * <p>
         * This method acts as bridge between array-based and collection-based APIs.
         *
         * @return an array containing all of the elements in this {@code SubList} in
         *         proper sequence
         * @throws IllegalStateException if this {@code SubList} is too large to fit in
         *                               an array
         */
        @Override
        public Object[] toArray() {
            checkListCanFitInAnArray(longSize());
            final Object[] elements = new Object[size()];
            int index = 0;
            for (E element: this) elements[index++] = element;
            return elements;
        }

        /**
         * Returns an array containing all of the elements in this {@code SubList} in
         * proper sequence (from first to last element); the runtime type of the
         * returned array is that of the specified array. If this {@code SubList} fits
         * in the specified array, it is returned therein. Otherwise, a new array is
         * allocated with the runtime type of the specified array and the size of this
         * {@code SubList}.
         * <p>
         * If this {@code SubList} fits in the specified array with room to spare (i.e.,
         * the array has more elements than this {@code SubList}), the element in the
         * array immediately following the end of this {@code SubList} is set to
         * {@code null}. (This is useful in determining the length of this
         * {@code SubList} <i>only</i> if the caller knows that this {@code SubList}
         * does not contain any null elements.)
         * <p>
         * Like the {@link #toArray()} method, this method acts as bridge between
         * array-based and collection-based APIs. Further, this method allows precise
         * control over the runtime type of the output array, and may, under certain
         * circumstances, be used to save allocation costs.
         * <p>
         * Suppose <i>x</i> is a {@code SubList} known to contain only strings. The
         * following code can be used to dump the {@code SubList} into a newly allocated
         * array of {@code String}:
         * 
         * <pre>
         * String[] y = x.toArray(new String[0]);
         * </pre>
         *
         * Note that {@code toArray(new Object[0])} is identical in function to
         * {@code toArray()}.
         *
         * @param array the array into which the elements of this {@code SubList} are to
         *              be stored, if it is big enough; otherwise, a new array of the
         *              same runtime type is allocated for this purpose.
         * @return an array containing the elements of this {@code SubList}
         * @throws ArrayStoreException   if the runtime type of the specified array is
         *                               not a supertype of the runtime type of every
         *                               element in this {@code SubList}
         * @throws IllegalStateException if this {@code SubList} is too large to fit in
         *                               an array
         * @throws NullPointerException  if the specified array is {@code null}
         */
        @Override
        @SuppressWarnings("unchecked")
        public <T> T[] toArray(T[] array) {
            checkListCanFitInAnArray(longSize());
            if (array.length < size()) {
                array = (T[]) java.lang.reflect.Array.newInstance(
                                        array.getClass().getComponentType(),size());
            }
            int index = 0;
            Object[] elements = array;
            for (E element: this) elements[index++] = element;
            if (array.length > size()) array[size()] = null;
            return array;
        }
        
        /**
         * Returns a {@code ListIterator} of the elements in this {@code SubList} (in
         * proper sequence), starting at the specified position in this {@code SubList}.
         * Obeys the general contract of {@code List.listIterator(int)}. if the
         * specified {@code index == longSize()}, the {@code ListIterator} will be
         * positioned at the end of this {@code SubList}.
         * <p>
         * <strong>Implementation Note:</strong> The {@code ListIterator} returned by
         * this method behaves differently when the sublist's
         * {@code size > Integer.MAX_VALUE}. Methods {@code nextIndex} and
         * {@code previousIndex} return -1 if the {@code index > Integer_MAX_VALUE}.
         * <p>
         * The {@code ListIterator} is <i>fail-fast</i>: if the
         * {@code NodableLinkedList} is structurally modified at any time after the
         * Iterator is created, in any way except through the {@code ListIterator's} own
         * {@code remove} or {@code add} methods, the {@code ListIterator} will throw a
         * {@code ConcurrentModificationException}. Thus, in the face of concurrent
         * modification, the iterator fails quickly and cleanly, rather than risking
         * arbitrary, non-deterministic behavior at an undetermined time in the future.
         * <p>
         * Instead of using a {@code ListIterator}, consider iterating over the
         * {@code SubList} via {@code SubListNodes}. For example:
         * 
         * <pre>
         * {@code
         *     // sublist is a NodableLinkedList<Integer>.SubList
         *     Node<Integer> subListNode = sublist.linkedSubNodes().get(index);
         *     //                       or sublist.getFirstNode();
         *     while (subListNode != null) {
         *         System.out.println(subListNode.element());
         *         subListNode = subListNode.next();
         *     }
         * }
         * </pre>
         *
         * @param index index of the first element to be returned from the
         *              {@code ListIterator} (by a call to {@code next})
         * @return a ListIterator of the elements in this {@code SubList} (in proper
         *         sequence), starting at the specified position in this {@code SubList}
         * @throws IndexOutOfBoundsException if the specified index is out of range
         *                                   {@code (index < 0 || index > longSize())}
         * @see List#listIterator(int)
         */
        @Override
        public ListIterator<E> listIterator(int index) {
            checkForModificationException();
            if (index < 0) throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
            final NodableLinkedList<E>.LinkedSubNodes linkedSubNodes = linkedSubNodes();
            return list.new ElementListIterator(linkedSubNodes, index, IndexType.ABSOLUTE, linkedSubNodes.iGetNode(index));
        }
        
        /**
         * Returns a {@code ListIterator} of the elements in this {@code SubList} (in
         * proper sequence), starting at the specified node in this {@code SubList}. if
         * the specified node is {@code null}, the {@code ListIterator} will be
         * positioned right after the last {@code Node} in this {@code SubList}.
         * <p>
         * <strong>Implementation Note:</strong> The {@code ListIterator} returned by
         * this method behaves differently when the sublist's
         * {@code size > Integer.MAX_VALUE}. Methods {@code nextIndex} and
         * {@code previousIndex} return -1 if the {@code index > Integer_MAX_VALUE}.
         * <p>
         * The {@code ListIterator} is <i>fail-fast</i>: if the
         * {@code NodableLinkedList} is structurally modified at any time after the
         * Iterator is created, in any way except through the {@code ListIterator's} own
         * {@code remove} or {@code add} methods, the {@code ListIterator} will throw a
         * {@code ConcurrentModificationException}. Thus, in the face of concurrent
         * modification, the iterator fails quickly and cleanly, rather than risking
         * arbitrary, non-deterministic behavior at an undetermined time in the future.
         * <p>
         * Instead of using a {@code ListIterator}, consider iterating over the
         * {@code SubList} via {@code SubListNodes}. For example:
         * 
         * <pre>
         * {@code
         *     // sublist is a NodableLinkedList<Integer>.SubList
         *     Node<Integer> subListNode = (node == null) ? null : node.subListNode(sublist);
         *     while (subListNode != null) {
         *         System.out.println(subListNode.element());
         *         subListNode = subListNode.next();
         *     }
         * }
         * </pre>
         *
         * @param node {@code Node} of the first element to be returned from the
         *             {@code ListIterator} (by a call to {@code next})
         * @return a ListIterator of the elements in this {@code SubList} (in proper
         *         sequence), starting at the specified node in this {@code SubList}
         * @throws IllegalArgumentException if the specified node is not contained
         *                                  by this {@code SubList}
         */
        public ListIterator<E> listIterator(Node<E> node) {
            checkForModificationException();
            final NodableLinkedList<E>.LinkedSubNodes linkedSubNodes = linkedSubNodes();
            if (node == null) {
                return list.new ElementListIterator(linkedSubNodes,
                        longSize(), IndexType.ABSOLUTE,
                        linkedSubNodes.getConfirmedTailSentinel());
            }
            final long index = linkedSubNodes.iGetIndex(node);
            if (index < 0) throw new IllegalArgumentException("specified node is not contained by this sublist");
            return list.new ElementListIterator(linkedSubNodes, index, IndexType.ABSOLUTE, node.linkNode());
        }
        
    } // SubList
    
    private static class ReversedSubList<E> extends SubList<E> {
        
        /**
         * The SubList that was reversed.
         */
        private final SubList<E> sublist;
        
        /**
         * Constructs a new ReversedSubList.
         * 
         * @param sublist The SubList that was reversed
         */
        private ReversedSubList(SubList<E> sublist) {
            super(sublist);
            this.sublist = sublist;
        }
        
        @Override
        public SubList<E> reversed() {
            return this.sublist;
        }
        
    } // ReversedSubList
        
    /**
     * Doubly-linked sublist of nodes which back a
     * {@link NodableLinkedList.SubList}. Implements all optional {@code List}
     * interface operations. The elements are of type
     * {@link NodableLinkedList.SubListNode}, and are never {@code null}.
     * <p>
     * Note, just as a {@code SubList} represents a range of elements of a
     * {@link NodableLinkedList}, a {@code LinkedSubNodes} sublist represents a
     * range of {@code Nodes} of a {@link NodableLinkedList.LinkedNodes} list. If a
     * {@code Node} is added to or removed from a {@code LinkedSubNodes} sublist,
     * the {@code Node} is respectively added to or removed from the backing
     * {@code NodableLinkedList.LinkedNodes} list.
     * <p>
     * The iterators returned by this class's {@code iterator} and
     * {@code listIterator} methods are <i>fail-fast</i>: if the
     * {@code NodableLinkedList} is structurally modified at any time after the
     * iterator is created, in any way except through the Iterator's own
     * {@code remove} or {@code add} methods, the iterator will throw a {@code
     * ConcurrentModificationException}. Thus, in the face of concurrent
     * modification, the iterator fails quickly and cleanly, rather than risking
     * arbitrary, non-deterministic behavior at an undetermined time in the future.
     * <p>
     * Note that the fail-fast behavior of an iterator cannot be guaranteed as it
     * is, generally speaking, impossible to make any hard guarantees in the
     * presence of unsynchronized concurrent modification. Fail-fast iterators throw
     * {@code ConcurrentModificationException} on a best-effort basis. Therefore, it
     * would be wrong to write a program that depended on this exception for its
     * correctness: <i>the fail-fast behavior of iterators should be used only to
     * detect bugs.</i>
     * 
     * @author James Pfeifer
     *
     */
    public class LinkedSubNodes extends InternalLinkedList implements List<Node<E>> {
        
        /**
         * The LinkNode which represents the head sentinel of this LinkedSubNodes. It is
         * never null.
         */
        private LinkNode<E> headSentinel;
        
        /**
         * The LinkNode which represents the tail sentinel of this LinkedSubNodes, or
         * null if the tail sentinel is unknown.
         */
        private LinkNode<E> tailSentinel;
        
        /**
         * The SubList this LinkedSubNodes backs.
         */
        private final SubList<E> subList;
        
        /**
         * The LinkedSubNodes which contains this LinkedSubNodes, or null if this is not
         * a sublist of a sublist.
         */
        private final LinkedSubNodes parent;
        
        /**
         * The number of nodes in this LinkedSubNodes, or -1 if the size is unknown.
         */
        private long size;
        
        /**
         * Constructs a new LinkedSubNodes which backs a SubList.
         * <p>
         * Note, the tailSentinel(null) and size(-1) may be unknown, but never both at
         * the same time. Also, there's no guarantee that the headSentinel comes before
         * tailSentinel in the parent list.
         * 
         * @param headSentinel The LinkNode which represents this LinkedSubNodes' head
         *                     sentinel
         * @param tailSentinel The LinkNode which represents this LinkedSubNodes' tail
         *                     sentinel, or null if the tail sentinel is unknown
         * @param subList      The SubList this LinkedSubNodes backs
         * @param parent       The SubList which contains this LinkedSubNodes if
         *                     this is a sublist of a sublist, otherwise, null
         * @param size         the number of nodes in this LinkedSubNodes, or -1 if the
         *                     size is unknown
         */
        private LinkedSubNodes(LinkNode<E> headSentinel, LinkNode<E> tailSentinel,
                SubList<E> subList, SubList<E> parent, long size) {
            // assert headSentinel != null : "headSentinel is null";
            // assert subList != null : "subList is null";
            // assert headSentinel.linkedNodes == linkedNodes :
            //     "headSentinel not linked to this linkedList";
            // assert tailSentinel != null && tailSentinel.linkedNodes == linkedNodes :
            //     "tailSentinel not linked to this linkedList";
            this.headSentinel = headSentinel;
            this.tailSentinel = tailSentinel;
            this.subList = subList;
            this.parent = (parent == null) ? null : parent.linkedSubNodes();
            this.size = size;
            updateModCount();
        }
        
        /**
         * Used by ReversedLinkedSubNodes to construct its super LinkedSubNodes.
         * 
         * @param subList The (Reversed)SubList this LinkedSubNodes backs
         */
        private LinkedSubNodes(SubList<E> subList) {
            // assert sublist != null : "sublist is null";
            this.headSentinel = null;
            this.tailSentinel = null;
            this.subList = subList;
            this.parent = null;
            this.size = 0L;
        }
        
        /**
         * Returns the protected int modCount inherited from class
         * java.util.AbstractList, which represents the expected modification
         * count.
         * 
         * @return returns the modCount
         */
        int modCount() {
            return this.modCount;
        }
        
        /**
         * Set this LinkedSubNodes' (expected) modCount to the list's modCount. The
         * modCount of the SubList this LinkedSubNodes backs, is also set.
         */
        void updateModCount() {
            subList.setModCount(this.modCount = NodableLinkedList.this.modCount());
        }
        
        /**
         * Update the (expected) modCount and the modCount of any parent sublists.
         */
        private void updateParentModCount() {
            if (parent() != null) parent().updateParentModCount();
            updateModCount();
        }            
        
        /**
         * Update the (expected) modCount and the size of this sublist.
         * 
         * @param sizeDelta the change in size
         */
        private void updateSizeAndModCount(long sizeDelta) {
            if (sizeIsKnown()) setSize(iGetSize() + sizeDelta);
            updateModCount();
        }
        
        /**
         * Check if the expected modCount matches the list's modCount and throw a
         * ConcurrentModificationException if they don't match. The protected int
         * modCount inherited from class java.util.AbstractList represents the expected
         * modCount.
         * 
         * @throws ConcurrentModificationException if the expected modCount does not
         *                                         match the list's modCount.
         */
        private void checkForModificationException() {
            if (this.modCount != NodableLinkedList.this.modCount()) {
                throw new ConcurrentModificationException();
            }
        }
        
        
        /**
         * Check if this sublist contains the specified Node.
         * 
         * @param node the Node to check
         * @throws IllegalArgumentException if the specified Node is not contained by
         *                                  this sublist
         */
        private void checkSubListContainsNode(Node<?> node) {
            checkSubListContainsNode(node, "specified node is not part of this sublist");
        }
        
        /**
         * Check if this sublist contains the specified Node.
         * 
         * @param node the Node to check
         * @param msg  the IllegalArgumentException message
         * @throws IllegalArgumentException if the specified Node is not contained by
         *                                  this sublist
         */
        private void checkSubListContainsNode(Node<?> node, String msg) {
            if (!this.contains(node)) throw new IllegalArgumentException(msg);
        }
        
        /**
         * Returns the {@code NodableLinkedList} which contains this
         * {@code LinkedSubNodes}.
         * 
         * @return the {@code NodableLinkedList} which contains this
         *         {@code LinkedSubNodes}
         */
        public NodableLinkedList<E> nodableLinkedList() {
            return NodableLinkedList.this;
        }
        
        /**
         * Returns the {@code NodableLinkedList.SubList} this {@code LinkedSubNodes}
         * object is backing.
         * 
         * @return the {@code NodableLinkedList.SubList} this {@code LinkedSubNodes}
         *         object is backing
         */
        public SubList<E> subList() {
            return this.subList;
        }
        
        @Override
        LinkNode<E> iGetHeadSentinel() {
            return this.headSentinel;
        }
        
        @Override
        LinkNode<E> iGetTailSentinel() {
            return this.tailSentinel;
        }
        
        /**
         * Sets the head sentinel of this sublist.
         * 
         * @param headSentinel the sublist's head sentinel
         */
        void setHeadSentinel(LinkNode<E> headSentinel) {
            this.headSentinel = headSentinel;
        }
        
        /**
         * Sets the tail sentinel of this sublist.
         * 
         * @param tailSentinel the sublist's tail sentinel
         */
        void setTailSentinel(LinkNode<E> tailSentinel) {
            this.tailSentinel = tailSentinel;
        }
        
        /**
         * Returns the parent LinkedSubNodes of this sublist, or null if this sublist is
         * not a sublist of a sublist.
         * 
         * @return the parent LinkedSubNodes of this sublist, or null if this sublist is
         *         not a sublist of a sublist.
         */
        LinkedSubNodes parent () {
            return this.parent;
        }
        
        @Override
        long iGetSize() {
            return this.size;
        }
        
        /**
         * Sets the size of this sublist.
         * 
         * @param size the sublist's size
         */
        void setSize(long size) {
            this.size = size;
        }
        
        /**
         * Returns the LinkedNodes' headSentinel from the perspective of this sublist. If
         * this sublist is reversed in relation to the LinkedNodes' traversal direction,
         * than the LinkeNodes' tailSentinel is returned.
         * 
         * @return the LinkedNodes' headSentinel from the perspective of this list
         */
        @Override
        LinkNode<E> iGetMyLinkedNodesHeadSentinel() {
            if (parent() == null) return linkedNodes().iGetHeadSentinel();
            return parent().iGetMyLinkedNodesHeadSentinel();
        }
        
        /**
         * Returns the LinkedNodes' tailSentinel from the perspective of this sublist. If
         * this sublist is reversed in relation to the LinkedNodes' traversal direction,
         * than the LinkeNodes' headSentinel is returned.
         * 
         * @return the LinkedNodes' tailSentinel from the perspective of this list
         */
        @Override
        LinkNode<E> iGetMyLinkedNodesTailSentinel() {
            if (parent() == null) return linkedNodes().iGetTailSentinel();
            return parent().iGetMyLinkedNodesTailSentinel();
        }
        
        /**
         * Returns true if the tail sentinel is known ({@code tailSentinel != null}).
         * 
         * @return true if the tail sentinel is known ({@code tailSentinel != null})
         */
        private boolean tailSentinelIsKnown() {
            return this.iGetTailSentinel() != null;
        }
        
        /**
         * Returns the tail sentinel. If the tail sentinel is unknown, the routine will
         * search the list to find the tail sentinel.
         * <p>
         * Note, an unconfirmed tail sentinel may be returned; one that hasn't been
         * verified that the known tail sentinel actually comes after the head sentinel
         * in the list. See {@link #getConfirmedTailSentinel()}.
         * 
         * @return the tail sentinel
         */
        LinkNode<E> tailSentinel() {
            if (!tailSentinelIsKnown()) { // tailSentinel unknown?
                //assert size >= 0 : "sublist size is unknown";
                long remaining = longSize();
                final LinkNode<E> linkedNodesTailSentinel = iGetMyLinkedNodesTailSentinel();
                LinkNode<E> tailSentinel = iGetNodeAfterFromListWithKnownTailSentinel(iGetHeadSentinel());
                while (remaining > 0 && tailSentinel != linkedNodesTailSentinel) {
                    remaining--;
                    tailSentinel = iGetNodeAfterFromListWithKnownTailSentinel(tailSentinel);
                }
                if (remaining > 0) {
                    throw new IllegalStateException("End of list reached unexpectedly");
                }
                setTailSentinel(tailSentinel);
            }
            return iGetTailSentinel();
        }
        
        /**
         * Returns the tail sentinel that has been confirmed it comes after the head
         * sentinel in the list.
         * 
         * @return the tail sentinel that has been confirmed it comes after the head
         *         sentinel in the list
         */
        LinkNode<E> getConfirmedTailSentinel() {
            if (sizeIsKnown() & tailSentinelIsKnown()) return iGetTailSentinel();
            return iGetNode(longSize());
        }
        
        @Override
        void iAddNodeFirst(LinkNode<E> node) {
            //assert isUnLinkedNode(node) : nodeIsNotUnLinkedMsg();
            iAddNodeAfter(node, iGetHeadSentinel());
        }
        
        @Override
        void iAddNodeLast(LinkNode<E> node) {
            //assert isUnLinkedNode(node) : nodeIsNotUnLinkedMsg();
            iAddNodeBefore(node, getConfirmedTailSentinel());
        }

        @Override
        void iAddNodeAfter(LinkNode<E> node, LinkNode<E> afterThisNode) {
            //assert isUnLinkedNode(node) : nodeIsNotUnLinkedMsg();
            //assert this.contains(afterThisNode) : "After Node is not an element of this sublist";
            if (parent() == null) linkedNodes().iAddNodeAfter(node, afterThisNode);
            else parent().iAddNodeAfter(node, afterThisNode);
            updateSizeAndModCount(1L);
        }

        @Override
        void iAddNodeBefore(LinkNode<E> node, LinkNode<E> beforeThisNode) {
            //assert isUnLinkedNode(node) : nodeIsNotUnLinkedMsg();
            //assert this.contains(beforeThisNode) : "Before Node is not an element of this sublist";
            if (parent() == null) linkedNodes().iAddNodeBefore(node, beforeThisNode);
            else parent().iAddNodeBefore(node, beforeThisNode);
            updateSizeAndModCount(1L);
        }
        
        @Override
        void iRemoveNode(LinkNode<E> node) {
            //assert this.contains(node) : "Node is not an element of this sublist";
            if (parent() == null) linkedNodes().iRemoveNode(node);
            else parent().iRemoveNode(node);
            updateSizeAndModCount(-1L);                
        }
        
        @Override
        void iReplaceNode(LinkNode<E> node, LinkNode<E> replacementNode) {
            //assert this.contains(node) : "Node is not an element of this sublist";
            //assert isUnLinkedNode(replacementNode) : "replacement"+nodeIsNotUnLinkedMsg();
            if (parent() == null) linkedNodes().iReplaceNode(node, replacementNode);
            else parent().iReplaceNode(node, replacementNode);
            updateSizeAndModCount(0L);
        }

        @Override
        boolean iHasNodeAfter(LinkNode<E> node) {
            //assert this.contains(node) || node == headSentinel : "Node is not an element of this sublist";
            LinkNode<?> nextNode;
            if (parent() == null) nextNode = linkedNodes().iGetNodeAfterOrTailSentinel(node);
            else nextNode = parent().iGetNodeAfterOrTailSentinel(node);
            if (nextNode == tailSentinel()) return false;
            if (nextNode == iGetMyLinkedNodesTailSentinel()) {
                throw new IllegalStateException(
                        "End of list reached unexpectedly; the sublists's last node most likely comes before the sublist's first node in the list");
            }
            return true;
        }

        @Override
        boolean iHasNodeBefore(LinkNode<E> node) {
            //assert this.contains(node) || node == tailSentinel() : "Node is not an element of this sublist";
            if (parent() == null) return linkedNodes().iGetNodeBeforeOrHeadSentinel(node) != iGetHeadSentinel();
            return parent().iGetNodeBeforeOrHeadSentinel(node) != iGetHeadSentinel(); 
        }
        
        @Override
        LinkNode<E> iGetFirstNode() {
            return iGetNodeAfter(iGetHeadSentinel());
        }
        
        @Override
        LinkNode<E> iGetLastNode() {
            return iGetNodeBefore(getConfirmedTailSentinel());
        }            
        
        @Override
        LinkNode<E> iGetNodeAfter(LinkNode<E> node) {
            //assert this.contains(node) || node == headSentinel : "Node is not an element of this sublist";
            if (!iHasNodeAfter(node)) return null;
            if (parent() == null) return linkedNodes().iGetNodeAfter(node);
            return parent().iGetNodeAfter(node);
        }
        
        @Override
        LinkNode<E> iGetNodeAfterOrTailSentinel(LinkNode<E> node) {
            //assert this.contains(node) || node == headSentinel : "Node is not an element of this sublist";
            if (!iHasNodeAfter(node)) return tailSentinel();
            if (parent() == null) return linkedNodes().iGetNodeAfter(node);
            return parent().iGetNodeAfter(node);
        }
        
        @Override
        LinkNode<E> iGetNodeAfterFromListWithKnownTailSentinel(LinkNode<E> node) {
            LinkedSubNodes linkedSubNodes = this;
            do {
                if (linkedSubNodes.tailSentinelIsKnown()) {
                    return linkedSubNodes.iGetNodeAfterOrTailSentinel(node);
                }
            linkedSubNodes = linkedSubNodes.parent();
            } while (linkedSubNodes != null);
            return linkedNodes().iGetNodeAfterOrTailSentinel(node);
        }
        
        @Override
        LinkNode<E> iGetNodeBefore(LinkNode<E> node) {
            //assert this.contains(node) || node == tailSentinel() : "Node is not an element of this sublist";
            if (!iHasNodeBefore(node)) return null;
            if (parent() == null) return linkedNodes().iGetNodeBefore(node);
            return parent().iGetNodeBefore(node);
        }
        
        @Override
        LinkNode<E> iGetNodeBeforeOrHeadSentinel(LinkNode<E> node) {
            //assert this.contains(node) || node == tailSentinel() : "Node is not an element of this sublist";
            if (!iHasNodeBefore(node)) return iGetHeadSentinel();
            if (parent() == null) return linkedNodes().iGetNodeBefore(node);
            return parent().iGetNodeBefore(node);
        }
        
        /**
         * Swap node of a sublist. When a node of a sublist has been swapped it's
         * possible its head or tail sentinels where swapped and need to be reset.
         * 
         * @param subSetNode  the sublist node that was swapped
         * @param swappedNode the node the subSetNode was swapped with
         */
        private void swappedNodes(LinkNode<E> subSetNode, LinkNode<E> swappedNode) {
            if (subSetNode.linkedNodes() == swappedNode.linkedNodes()) {
                // both nodes are nodes of the same list, therefore,
                // the head or tail sentinels may have been swapped
                if (swappedNode == iGetHeadSentinel()) setHeadSentinel(subSetNode);
                if (swappedNode == iGetTailSentinel()) setTailSentinel(subSetNode);
                if (parent() != null) parent().swappedNodes(subSetNode, swappedNode);
            }
            updateSizeAndModCount(0L);
        }
        
        /**
         * Returns the node at the specified position in this sublist.
         * 
         * Note, this routine returns the tailSentinel if {@code index == longSize()}.
         *
         * @param index index of the node to return
         * @return the node at the specified position in this sublist
         * @throws IndexOutOfBoundsException if the specified index is out of range
         *                                   {@code (index < 0 || index >= longSize())}
         * @throws IllegalStateException     if end of list is reached unexpectedly; the
         *                                   sublist's last node most likely comes
         *                                   before the sublists's first node in the
         *                                   list
         */
        private LinkNode<E> iGetNode(long index) {
            // Note, this routine returns the tailSentinel if index = longSize()
            if (index < 0L) throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
            LinkNode<E> node;
            long cursorIndex;
            if (sizeIsKnown() && tailSentinelIsKnown()) {
                // both size and tailSentinel is known, therefore, we also know that
                // the tailSentinel comes after the headSentinel in the list
                checkPositionIndex(index, longSize());
                if (index <= ((longSize()) >> 1)) {
                    cursorIndex = 0L;
                    node = iGetNodeAfterOrTailSentinel(iGetHeadSentinel());
                    while (cursorIndex < index) { node = iGetNodeAfter(node); cursorIndex++; }
                } else {
                    cursorIndex = longSize();
                    node = tailSentinel();
                    while (cursorIndex > index) { node = iGetNodeBefore(node); cursorIndex--; }
                }
            } else if (sizeIsKnown()) {
                // size is known, tailSentinel is unknown
                checkPositionIndex(index, longSize());
                cursorIndex = 0L;
                node = iGetNodeAfterFromListWithKnownTailSentinel(iGetHeadSentinel());
                while (cursorIndex < index) {
                    node = iGetNodeAfterFromListWithKnownTailSentinel(node);
                    cursorIndex++;
                }
                if (index == longSize()) setTailSentinel(node);
                if (index == longSize()-1) setTailSentinel(iGetNodeAfterFromListWithKnownTailSentinel(node));
            } else {
                // size is unknown, tailSentinel is known
                cursorIndex = 0L;
                node = iGetNodeAfterOrTailSentinel(iGetHeadSentinel());
                while ( cursorIndex < index && node != iGetTailSentinel()) {
                    node = iGetNodeAfterOrTailSentinel(node);
                    cursorIndex++;
                }
                if (cursorIndex < index) {
                    throw new IndexOutOfBoundsException(outOfBoundsMsg(index, cursorIndex));
                }
                if (node == iGetTailSentinel()) this.setSize(cursorIndex);
                else if (iGetNodeAfterOrTailSentinel(node) == iGetTailSentinel()) this.setSize(cursorIndex + 1L);
            }
            return node;
        }
        
        /**
         * Returns the index of the specified node in this sublist, or -1 if this
         * sublist does not contain the specified node.
         * 
         * @param node {@code Node} to search for
         * @return the index of the specified node in this sublist, or -1 if this
         *         sublist does not contain the specified node
         * @throws IllegalStateException if end of list is reached unexpectedly; the
         *                               sublist's last node most likely comes before
         *                               the sublists's first node in the list
         */
        private long iGetIndex(Node<?> node) {
            if (node.isSubListNode() && node.subList() != this.subList()) return -1;
            return iGetIndex(node.linkNode());
        }
        
        /**
         * Returns the index of the specified LinkNode in this sublist, or -1 if this
         * sublist does not contain the specified LinkNode.
         * 
         * @param linkNode {@code LinkNode} to search for
         * @return the index of the specified LinkNode in this sublist, or -1 if this
         *         sublist does not contain the specified LinkNode
         * @throws IllegalStateException if end of list is reached unexpectedly; the
         *                               sublist's last node most likely comes before
         *                               the sublists's first node in the list
         */
        private long iGetIndex(LinkNode<?> linkNode) {
            if (!linkedNodes().contains(linkNode)) return -1;
            long cursorIndex = 0L;
            LinkNode<E> cursorNode;
            if (sizeIsKnown() && tailSentinelIsKnown()) {
                // both size and tailSentinel are known, therefore, we also know that
                // the tailSentinel comes after the headSentinel in the list
                cursorNode = iGetFirstNode();
                while ( cursorNode != null && !cursorNode.isEquivalentTo(linkNode)) {
                    cursorIndex++;
                cursorNode = iGetNodeAfter(cursorNode);
                }
                if (cursorNode == null) cursorIndex = -1L; // node not found
            } else if (sizeIsKnown()) {
                // size is known, tailSentinel is unknown
                cursorNode = iGetNodeAfterFromListWithKnownTailSentinel(iGetHeadSentinel());
                while (cursorIndex < longSize() && !cursorNode.isEquivalentTo(linkNode)) {
                    cursorIndex++;
                cursorNode = iGetNodeAfterFromListWithKnownTailSentinel(cursorNode);
                }
                if (cursorIndex == longSize()-1) {
                    this.setTailSentinel(iGetNodeAfterFromListWithKnownTailSentinel(cursorNode));
                }
                if (cursorIndex == longSize()) {
                    this.setTailSentinel(cursorNode);
                    cursorIndex = -1L; // node not found
                }
            } else {
                // size is unknown, tailSentinel is known
                cursorNode = iGetFirstNode();
                while ( cursorNode != null && !cursorNode.isEquivalentTo(linkNode)) {
                    cursorIndex++;
                    cursorNode = iGetNodeAfter(cursorNode);
                }
                if (cursorNode == null) {
                    this.setSize(cursorIndex);
                    cursorIndex = -1L; // node not found
                } else if (iGetNodeAfter(cursorNode) == null) this.setSize(cursorIndex + 1L);
            }
            return cursorIndex;
        }
        
        /**
         * Return true if the size of this sublist is known ({@code size >= 0}).
         * 
         * @return true if {@code size >= 0}
         */
        private boolean sizeIsKnown() {
            return iGetSize() >= 0L;
        }

        /**
         * Returns the number of {@code Nodes} in this sublist. If this sublist contains
         * more than Integer.MAX_VALUE nodes, Integer.MAX_VALUE is returned.
         *
         * @return the number of {@code Nodes} in this sublist
         */
        @Override
        public int size() {
            final long longSize = longSize();
            return (longSize > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int)longSize;
        }

        /**
         * Returns the number of {@code Nodes} in this sublist.
         *
         * @return the number of {@code Nodes} in this sublist
         */
        public long longSize() {
            checkForModificationException();
            if (!sizeIsKnown()) { // size unknown?
                //assert tailSentinelIsKnown() : "tail sentinel shouldn't be null";
                LinkNode<E> node;
                long size = 0L;
                for (node = iGetFirstNode(); node != null; node = iGetNodeAfter(node)) {
                    size++;
                }
                this.setSize(size);
            }
            return iGetSize();
        }
        
        /**
         * Returns {@code true} if this sublist contains no {@code Nodes}.
         *
         * @return {@code true} if this sublist contains no {@code Nodes}
         */
        @Override
        public boolean isEmpty() {
            checkForModificationException();
            if (sizeIsKnown()) return longSize() == 0L;
            return !iHasNodeAfter(iGetHeadSentinel());
        }

        /**
         * Appends the specified node to the end of this sublist. The specified node
         * must not already belong to a list.
         * <p>
         * If the specified node is a {@code SubListNode}, after this operation is
         * completed, it will be associated with this sublist.
         * <p>
         * This method is equivalent to {@link #addNodeLast}.
         *
         * @param node {@code Node} to be appended to the end of this sublist
         * @throws IllegalArgumentException if the specified node is {@code null} or
         *                                  already a node of a list
         */
        public void addNode(Node<E> node) {
            addNodeLast(node);
        }

        /**
         * Inserts the specified node at the beginning of this sublist. The specified
         * node must not already belong to a list.
         * <p>
         * If the specified node is a {@code SubListNode}, after this operation is
         * completed, it will be associated with this sublist.
         *
         * @param node the {@code Node} to be inserted at the beginning of this sublist
         * @throws IllegalArgumentException if the specified node is {@code null} or
         *                                  already a node of a list
         */
        public void addNodeFirst(Node<E> node) {
            checkForModificationException();
            checkNodeIsUnLinked(node);
            iAddNodeFirst(node.linkNode());
            if (node.isSubListNode()) {
                final SubListNode<E> subListNode = (SubListNode<E>)node;
                subListNode.setSubList(this.subList());
                subListNode.updateExpectedModCount();
            }
        }

        /**
         * Appends the specified node to the end of this sublist. The specified node
         * must not already belong to a list.
         * <p>
         * If the specified node is a {@code SubListNode}, after this operation is
         * completed, it will be associated with this sublist.
         *
         * @param node {@code Node} to be appended to the end of this sublist
         * @throws IllegalArgumentException if the specified node is {@code null} or
         *                                  already a node of a list
         */
        public void addNodeLast(Node<E> node) {
            checkForModificationException();
            checkNodeIsUnLinked(node);
            iAddNodeLast(node.linkNode());
            if (node.isSubListNode()) {
                final SubListNode<E> subListNode = (SubListNode<E>)node;
                subListNode.setSubList(this.subList());
                subListNode.updateExpectedModCount();
            }
        }
        
        /**
         * Returns the first {@code SubListNode} of this sublist, or {@code null} if
         * this sublist is empty.
         *
         * @return the first {@code SubListNode} of this sublist, or {@code null} if
         *         this sublist is empty
         */
        public SubListNode<E> getFirstNode() {
            checkForModificationException();
            return (this.isEmpty()) ? null : new SubListNode<E>(iGetFirstNode(), this.subList());
        }
        
        /**
         * Returns the last {@code SubListNode} of this sublist, or {@code null} if this
         * sublist is empty.
         *
         * @return the last {@code SubListNode} of this sublist, or {@code null} if this
         *         sublist is empty
         */
        public SubListNode<E> getLastNode() {
            checkForModificationException();
            return (this.isEmpty()) ? null : new SubListNode<E>(iGetLastNode(), this.subList());
        }
        
        /**
         * Removes and returns the first {@code SubListNode} of this sublist, or returns
         * {@code null} if this sublist is empty.
         *
         * @return the first {@code SubListNode} of this sublist that was removed, or
         *         {@code null} if this sublist is empty
         */       
        public SubListNode<E> removeFirstNode() {
            checkForModificationException();
            final SubListNode<E> firstNode = getFirstNode();
            if (firstNode != null) iRemoveNode(firstNode.linkNode());
            return firstNode;
        }
        
        /**
         * Removes and returns the last {@code SubListNode} of this sublist, or returns
         * {@code null} if this sublist is empty.
         *
         * @return the last {@code SubListNode} of this sublist that was
         *         removed, or {@code null} if this sublist is empty
         */        
        public SubListNode<E> removeLastNode() {
            checkForModificationException();
            final SubListNode<E> lastNode = getLastNode();
            if (lastNode != null) iRemoveNode(lastNode.linkNode());
            return lastNode;
        }
        
        /**
         * Removes all of the {@code Nodes} from this sublist.
         */
        @Override
        public void clear() {
            checkForModificationException();
            LinkNode<E> node = iGetFirstNode();
            if (sizeIsKnown()) {
                 while (longSize() > 0) {
                    final LinkNode<E> nodeToRemove = node;
                    node = iGetNodeAfterFromListWithKnownTailSentinel(node);
                    iRemoveNode(nodeToRemove);
                }
            } else {
                while (node != null) {
                    final LinkNode<E> nodeToRemove = node;
                    node = iGetNodeAfter(node);
                    iRemoveNode(nodeToRemove);
                }
            }
        }
        
        /**
         * Returns and possibly reverses the specified {@code Node's} backing
         * {@code LinkNode} to match the forward direction of this sublist. In other
         * words, returns a {@code LinkNode} which can be used to essentially traverse
         * this sublist from the specified node to this sublist's last node when making
         * successive calls to the returned LinkedNodes's {@code next()} method. The
         * specified {@code Node} must be contained by this sublist.
         * <p>
         * As a reminder, a {@code LinkNode} traverses the base {@code LinkedNodes} and
         * not the sublist. For instance:
         * 
         * <pre>
         * {@code
         *    import net.pfeifdom.java.util.NodableLinkedList.Node;
         *    // sublist is a NodableLinkedList<Integer>.LinkedSubNodes
         *    final Node<Integer> lastNode = sublist.getLastNode();
         *    Node<Integer> linkNode = sublist.forwardLinkNodeOf(node);
         *    while (linkNode != null) {
         *       System.out.println(linkNode.element());
         *       if (linkNode.isEquivalentTo(lastNode)) break;
         *       linkNode = linkNode.next();
         *    }
         * }
         * </pre>
         * 
         * @param node the {@code Node} whose backing {@code LinkNode} is returned and
         *             possibly reversed to match the forward direction of this sublist
         * @return a {@code LinkNode} which can be used to essentially traverse this
         *         sublist in a forward direction
         * @throws IllegalArgumentException if the specified node is not contained by
         *                                  this sublist
         */
        public LinkNode<E> forwardLinkNodeOf(Node<E> node) {
            checkSubListContainsNode(node);
            if (parent() == null) return linkedNodes().forwardLinkNodeOf(node);
            return parent().forwardLinkNodeOf(node);
        }

        /**
         * Returns the index of the specified object ({@code Node}) in this sublist, or
         * -1 if there is no such index (this sublist does not contain the specified
         * {@code Node} or the {@code index > Integer.MAX_VALUE}). Note, a -1 is
         * returned if the specified object is a {@code SubListNode} and it is not
         * associated with this sublist.
         * 
         * @param object {@code Object} ({@code Node}) to search for
         * 
         * @return the index of the specified object ({@code Node}) in this sublist, or
         *         -1 if there is no such index
         */
        @Override
        public int indexOf(Object object) {
            checkForModificationException();
            LinkNode<?> node;
            if (object instanceof LinkNode) {
                node = (LinkNode<?>)object;
            } else if (object instanceof SubListNode) {
                final SubListNode<?> sublistnode = (SubListNode<?>)object;
                if (sublistnode.subList() != this.subList()) {
                    return -1;
                }
                node = ((SubListNode<?>)object).linkNode();
            } else if (object instanceof Node) {
                node = ((Node<?>)object).linkNode();
            } else {
                return -1;
            }
            final long index = iGetIndex(node);
            return (index > Integer.MAX_VALUE) ? -1 : (int)index;
        }
        
        /**
         * Returns the index of the specified object ({@code Node}) in this sublist, or
         * -1 if there is no such index (this sublist does not contain the specified
         * {@code Node} or the {@code index > Integer.MAX_VALUE}). Note, a -1 is
         * returned if the specified object is a {@code SubListNode} and it is not
         * associated with this sublist.
         * <p>
         * Note that {@code lastIndexOf} is identical in function to {@code indexOf},
         * except {@code lastIndexOf} attempts to traverse the sublist backwards from
         * the end of the sublist. However, it's not guaranteed this routine will
         * traverse the sublist backwards from the end of the sublist. for instance, if
         * the sublist's last node is unknown, the routine will traverse the sublist
         * from the start of the sublist.
         *
         * @param object {@code Object} ({@code Node}) to search for
         * @return the index of the specified object ({@code Node}) in this sublist, or
         *         -1 if there is no such index
         */
        @Override
        public int lastIndexOf(Object object) {
            if (!tailSentinelIsKnown() || !sizeIsKnown()) return indexOf(object);
            checkForModificationException();

            LinkNode<?> searchNode;
            if (object instanceof LinkNode) {
                searchNode = (LinkNode<?>) object;
            } else if (object instanceof SubListNode) {
                final SubListNode<?> sublistnode = (SubListNode<?>) object;
                if (sublistnode.subList() != this.subList()) {
                    return -1;
                }
                searchNode = ((SubListNode<?>) object).linkNode();
            } else if (object instanceof Node) {
                searchNode = ((Node<?>) object).linkNode();
            } else {
                return -1;
            }

            long index = longSize() - 1L;
            for (LinkNode<E> node = iGetLastNode(); node != null; node = iGetNodeBefore(node), index--) {
                if (node == searchNode) {
                    return (index > Integer.MAX_VALUE) ? -1 : (int) index;
                }
            }
            return -1;
        }
        
        /**
         * Returns the {@code SublistNode} at the specified position in this sublist.
         *
         * @param index index of the {@code SubListNode} to return
         * @return the {@code SubListNode} at the specified position in this sublist
         * @throws IndexOutOfBoundsException if the specified index is out of range
         *                                   {@code (index < 0 || index >= longSize())}
         */
        @Override
        public SubListNode<E> get(int index) {
            checkForModificationException();
            final long getIndex = index;
            if (sizeIsKnown()) checkNodeIndex(getIndex, longSize());
            LinkNode<E> node = iGetNode(index);
            if (node == iGetTailSentinel()) {
                throw new IndexOutOfBoundsException(outOfBoundsMsg(index, longSize()));
            }
            return new SubListNode<E>(node, this.subList());
        }
        
        /**
         * Inserts the specified node at the specified position in this sublist. Shifts
         * the {@code Node} currently at that position (if any) and any subsequent
         * {@code Nodes} to the right (adds one to their indices). The specified node
         * must not already belong to a list. if the specified
         * {@code index == longSize()}, the specified node will be appended to the end
         * of this sublist.
         * <p>
         * If the specified node is a {@code SubListNode}, after this operation is
         * completed, it will be associated with this sublist.
         *
         * @param index position where the specified node is to be inserted
         * @param node  {@code Node} to be inserted
         * @throws IndexOutOfBoundsException if the specified index is out of range
         *                                   {@code (index < 0 || index > longSize())}
         * @throws IllegalArgumentException  if the specified node is {@code null} or
         *                                   already a node of a list
         */
        @Override
        public void add(int index, Node<E> node) {
            checkForModificationException();
            if (sizeIsKnown()) checkPositionIndex(index, longSize());
            checkNodeIsUnLinked(node);
            iAddNodeBefore(node.linkNode(), iGetNode(index));
            if (node.isSubListNode()) {
                final SubListNode<E> subListNode = (SubListNode<E>)node;
                subListNode.setSubList(this.subList());
                subListNode.updateExpectedModCount();
            }
        }
        
        /**
         * Removes and returns the {@code SubListNode} at the specified position in this
         * sublist. Shifts any subsequent {@code Nodes} to the left (subtracts one from
         * their indices).
         *
         * @param index the index of the {@code SubListNode} to be removed
         * @return the {@code SubListNode} previously at the specified position
         * @throws IndexOutOfBoundsException if the specified index is out of range
         *                                   {@code (index < 0 || index >= longSize())}
         */
        @Override
        public SubListNode<E> remove(int index) {
            checkForModificationException();
            if (sizeIsKnown()) checkNodeIndex(index, longSize());
            final SubListNode<E> subListNode = get(index);
            iRemoveNode(subListNode.linkNode());
            return subListNode;
        }            

        /**
         * Removes, if present, the specified object ({@code Node}) from this sublist.
         * If this sublist does not contain the specified {@code Node}, it is unchanged.
         * Note, if the specified {@code Node} is a {@code SubListNode}, it will not be
         * removed if the {@code SubListNode} is not associated with this sublist.
         * 
         * @param object {@code Node} to be removed from this sublist
         * @return {@code true} if this sublist contained the specified object
         *         ({@code Node})
         */
        @SuppressWarnings("unchecked")
        @Override
        public boolean remove(Object object) {
            checkForModificationException();
            if (!this.contains(object)) return false;
            LinkNode<E> node;
            if (object instanceof LinkNode) {
                node = (LinkNode<E>)object;
            } else if (object instanceof SubListNode) {
                node = ((SubListNode<E>)object).linkNode();
            } else if (object instanceof Node) {
                node = ((Node<E>)object).linkNode();
            } else {
                return false;
            }
            iRemoveNode(node);
            return true;        
        }
        
        /**
         * Returns {@code true} if this sublist is a reverse-ordered view of the base
         * {@code NodableLinkedList}.
         * 
         * Note, {@code true} doesn't mean this is a sublist that has been reversed. It
         * means this sublist's traversal direction is in the opposite direction than
         * the traversal direction of the base {@code NodableLinkedList} (the
         * {@code NodableLinkedList} initially created).
         * 
         * @return {@code true} if this sublist is a reverse-ordered view of the base
         *         {@code NodableLinkedList}
         */
        public boolean isReversed() {
            if (parent() == null) return linkedNodes().isReversed();
            return parent().isReversed();
        }            
        
        /**
         * Returns a reverse-order view of this sublist.
         * 
         * The encounter order of {@code Nodes} in the returned view is the inverse of the
         * encounter order of {@code Nodes} in this sublist. The reverse ordering affects all
         * order-sensitive operations, including those on the view collections of the
         * returned view. Modifications to the reversed view are permitted and will be
         * propagated to this sublist. In addition, modifications to this sublist will
         * be visible in the reversed view.
         * 
         * @return a reverse-order view of this sublist
         */
        //@Override //not until JDK 21
        public LinkedSubNodes reversed() {
            getConfirmedTailSentinel(); // make sure the new reversed sublist's headSentinel is valid
            return (new ReversedSubList<E>(subList())).linkedSubNodes();
        }
        
        /**
         * Replaces the {@code Node} at the specified position in this sublist with the
         * specified node.
         *
         * @param index index of the node to replace
         * @param node  {@code Node} to be stored at the specified position
         * @return the {@code SubListNode} previously at the specified position
         * @throws IndexOutOfBoundsException if the specified index is out of range
         *                                   {@code (index < 0 || index >= longSize())}
         * @throws IllegalArgumentException  if the specified node is {@code null} or
         *                                   already a node of a list
         */
        @Override
        public SubListNode<E> set(int index, Node<E> node) {
            checkForModificationException();
            if (sizeIsKnown()) checkNodeIndex(index, longSize());
            checkNodeIsUnLinked(node);
            final SubListNode<E> originalNode = get(index);
            iReplaceNode(originalNode.linkNode(), node.linkNode());
            if (node.isSubListNode()) {
                final SubListNode<E> subListNode = (SubListNode<E>)node;
                subListNode.setSubList(this.subList());
                subListNode.updateExpectedModCount();
            }
            return originalNode;
        }
        
        /**
         * Appends all of the {@code Nodes} in the specified collection to the end of
         * this sublist, in the order that they are returned by the specified
         * collection's iterator.
         * <p>
         * After this operation is completed, all {@code SubListNodes} in the specified
         * collection will be associated with this sublist.
         * <p>
         * The behavior of this operation is undefined if the specified collection is
         * modified while the operation is in progress. (Note that this will occur if
         * the specified collection is this sublist, and it's nonempty.)
         *
         * @param collection collection containing {@code Nodes} to be added to this
         *                   sublist
         * @return {@code true} if this sublist changed as a result of the call
         * @throws NullPointerException     if the specified collection is {@code null}
         * @throws IllegalArgumentException if any {@code Node} in the specified
         *                                  collection is {@code null} or already a node
         *                                  of a list
         */
        @Override
        public boolean addAll(Collection<? extends Node<E>> collection) {
            checkForModificationException();
            final long initialSize = longSize();
            final LinkNode<E> tailSentinel = getConfirmedTailSentinel();
            for (Node<E> node: collection) {
                checkNodeIsUnLinked(node, "Node in collection (@"+(longSize()-initialSize)+") is null or already an element of a list");
                iAddNodeBefore(node.linkNode(), tailSentinel);
                if (node.isSubListNode()) {
                    final SubListNode<E> subListNode = (SubListNode<E>)node;
                    subListNode.setSubList(this.subList());
                }
            }
            for (Node<E> node: collection) {
                if (node.isSubListNode()) {
                    final SubListNode<E> subListNode = (SubListNode<E>)node;
                    subListNode.updateExpectedModCount();
                }
            }
            return longSize() != initialSize;
        }
        
        /**
         * Inserts all of the {@code Nodes} in the specified collection into this
         * sublist, starting at the specified position. Shifts the {@code Node}
         * currently at that position (if any) and any subsequent {@code Nodes} to the
         * right (increases their indices). The new {@code Nodes} will appear in this
         * sublist in the order that they are returned by the specified collection's
         * iterator. if the specified {@code index == longSize()}, the {@code Nodes}
         * will be appended to the end of this sublist.
         * 
         * Note that {@code addAll(longSize(), Collection)} is identical in function to
         * {@code addAll(Collection)}.
         * <p>
         * After this operation is completed, all {@code SubListNodes} in the specified
         * collection will associated with this sublist.
         * <p>
         * The behavior of this operation is undefined if the specified collection is
         * modified while the operation is in progress. (Note that this will occur if
         * the specified collection is this sublist, and it's nonempty.)
         *
         * @param index      position where to insert the first {@code Node} from the
         *                   specified collection
         * @param collection collection containing {@code Nodes} to be added to this
         *                   sublist
         * @return {@code true} if this sublist changed as a result of the call
         * @throws IllegalArgumentException  if any {@code Node} in the specified
         *                                   collection is {@code null} or already a
         *                                   node of a list
         * @throws IndexOutOfBoundsException if the specified index is out of range
         *                                   {@code (index < 0 || index > longSize())}
         * @throws NullPointerException      if the specified collection is {@code null}
         */
        @Override
        public boolean addAll(int index, Collection<? extends Node<E>> collection) {
            checkForModificationException();
            if (sizeIsKnown()) checkPositionIndex(index, longSize());
            long position = 0;
            boolean changed = false;
            final LinkNode<E> beforeThisNode = iGetNode(index);
            for (Node<E> node: collection) {
                checkNodeIsUnLinked(node, "Node in collection (@"+position+") is null or already an element of a list");
                iAddNodeBefore(node.linkNode(), beforeThisNode);
                if (node.isSubListNode()) {
                    final SubListNode<E> subListNode = (SubListNode<E>)node;
                    subListNode.setSubList(this.subList());
                }
                changed = true;
                position++;
            }
            for (Node<E> node: collection) {
                if (node.isSubListNode()) {
                    final SubListNode<E> subListNode = (SubListNode<E>)node;
                    subListNode.updateExpectedModCount();
                }
            }
            return changed;
        }
        
        /**
         * Inserts all of the {@code Nodes} in the specified collection into this
         * sublist, before the specified node. Shifts the specified node and any
         * subsequent {@code Nodes} to the right (increases their indices). The new
         * {@code Nodes} will appear in this sublist in the order that they are returned
         * by the specified collection's iterator. if the specified node is
         * {@code null}, the {@code Nodes} will be appended to the end of this sublist.
         * 
         * Note that {@code addAll(null, Collection)} is identical in function to
         * {@code addAll(Collection)}.
         * <p>
         * After this operation is completed, all {@code SubListNodes} in the specified
         * collection will be associated with this sublist.
         * <p>
         * The behavior of this operation is undefined if the specified collection is
         * modified while the operation is in progress. (Note that this will occur if
         * the specified collection is this sublist, and it's nonempty.)
         *
         * @param node       {@code Node} the specified collection is to be inserted
         *                   before
         * @param collection collection containing {@code Nodes} to be added to this
         *                   sublist
         * @return {@code true} if this sublist changed as a result of the call
         * @throws IllegalArgumentException if the specified {@code Node} is not
         *                                  contained by this sublist, or any
         *                                  {@code Node} in the specified collection is
         *                                  {@code null} or already a node of a list
         * @throws NullPointerException     if the specified collection is {@code null}
         */
        public boolean addAll(Node<E> node, Collection<? extends Node<E>> collection) {
            checkForModificationException();
            if (node == null) return addAll(collection);
            checkSubListContainsNode(node);
            final LinkNode<E> beforeThisNode = node.linkNode();
            long position = 0;
            boolean changed = false;
            for (Node<E> collectionNode : collection) {
                checkNodeIsUnLinked(collectionNode, "Node in collection (@"+position+") is null or already an element of a list");
                iAddNodeBefore(collectionNode.linkNode(), beforeThisNode);
                if (collectionNode.isSubListNode()) {
                    final SubListNode<E> subListNode = (SubListNode<E>) collectionNode;
                    subListNode.setSubList(this.subList());
                }
                changed = true;
                position++;
            }
            for (Node<E> collectionNode : collection) {
                if (collectionNode.isSubListNode()) {
                    final SubListNode<E> subListNode = (SubListNode<E>) collectionNode;
                    subListNode.updateExpectedModCount();
                }
            }
            return changed;
        }
        
        /**
         * Returns {@code true} if this sublist contains the specified object
         * ({@code Node}). Note, if the specified object is a {@code SubListNode} and it
         * is not associated with this sublist, {@code false} will be returned even if
         * the {@code SubListNode's} {@code LinkNode} is contained by this sublist.
         *
         * @param object {@code Object} ({@code Node}) whose presence in this sublist is
         *               to be tested
         * @return {@code true} if this sublist contains the specified {@code Node}
         */
        @Override
        public boolean contains(Object object) {
            checkForModificationException();
            LinkNode<?> node;
            if (object instanceof LinkNode) {
                node = (LinkNode<?>)object;
            } else if (object instanceof SubListNode) {
                final SubListNode<?> sublistnode = (SubListNode<?>)object;
                if (sublistnode.subList() != this.subList()) {
                    return false;
                }
                if (sublistnode.isExpectedModCount()) return true;
                node = sublistnode.linkNode();
            } else if (object instanceof Node) {
                node = ((Node<?>)object).linkNode();
            } else {
                return false;
            }
            return contains(node);
        }

        private boolean contains(LinkNode<?> node) {
            if (sizeIsKnown() && tailSentinelIsKnown() &&
                    (linkedNodes().longSize() - this.longSize() < this.longSize() / 2)) {
                // The portion of the list not occupied by the sublist is less than
                // 1/2 the size of the sublist, therefore, it might be faster
                // to search the portion of the list not occupied by the sublist
                // instead of searching the sublist itself
                if (!linkedNodes().contains(node)) return false;
                LinkNode<E> linkNode = linkedNodes().iGetHeadSentinel();
                while (linkNode != null) {
                    if (node == linkNode) return false;
                    if (linkNode == this.iGetHeadSentinel() || linkNode == this.iGetTailSentinel()) {
                        if (linkNode == this.iGetHeadSentinel())
                             linkNode = this.iGetTailSentinel();
                        else linkNode = this.iGetHeadSentinel();
                        if (linkNode == linkedNodes().iGetTailSentinel()) break;
                        if (node == linkNode) return false;
                    }
                linkNode = linkedNodes().iGetNodeAfter(linkNode);
                }
                return true;
            }
            return iGetIndex(node) >= 0L;
        }
        
        /**
         * Returns a view of the portion of this sublist between the specified
         * fromIndex, inclusive, and toIndex, exclusive. (If the specified fromIndex and
         * toIndex are equal, the returned {@code SubList} is empty.) The returned
         * {@code SubList} is backed by this sublist, so structural changes in the
         * returned {@code SubList} are reflected in this sublist. The returned
         * {@code SubList} supports all of the optional {@code List} operations.
         * <p>
         * This method eliminates the need for explicit range operations (of the sort
         * that commonly exist for arrays). Any operation that expects a list can be
         * used as a range operation by passing a {@code SubList} view instead of a
         * whole list. For example, the following idiom removes a range of elements from
         * a list:
         * <pre>
         * {@code
         *      list.subList(fromIndex, toIndex).clear();
         * }
         * </pre>
         * 
         * Similar idioms may be constructed for {@code indexOf} and
         * {@code lastIndexOf}, and all of the algorithms in the {@code Collections}
         * class can be applied to a {@code SubList}.
         * <p>
         * The semantics of the {@code SubList} returned by this method become undefined
         * if the {@code NodableLinkedList} is <i>structurally modified</i> in any way
         * other than via the returned {@code SubList} or any of its sublists.
         * (Structural modifications are those that change the size of this sublist, or
         * otherwise perturb it in such a fashion that iterations in progress may yield
         * incorrect results.) A {@code ConcurrentModificationException} is thrown for
         * any operation on a {@code SubList} that is structurally unsound.
         *
         * @param fromIndex low endpoint (inclusive) of the {@code SubList}
         * @param toIndex   high endpoint (exclusive) of the {@code SubList}
         * @return a view of the specified range within this sublist
         * @throws IndexOutOfBoundsException for an illegal endpoint index value
         *                                   ({@code fromIndex < 0 || toIndex > size ||
         *                                   fromIndex > toIndex})
         */
        @Override
        public LinkedSubNodes subList(int fromIndex, int toIndex) {
            return newSubList(fromIndex, toIndex).linkedSubNodes;
        }
        
        /**
         * Returns a NodableLinkedList.SubList.
         * 
         * @param fromIndex low endpoint (inclusive) of the {@code SubList}
         * @param toIndex   high endpoint (exclusive) of the {@code SubList}
         * @return returns a NodableLinkedList.SubList
         */
        private SubList<E> newSubList(int fromIndex, int toIndex) {
            checkForModificationException();
            if (fromIndex < 0L) throw new IndexOutOfBoundsException("fromIndex = " + fromIndex);
            if (toIndex > longSize()) throw new IndexOutOfBoundsException("toIndex(" + toIndex +") > size(" + longSize() + ")");
            if (fromIndex > toIndex) throw new IndexOutOfBoundsException("fromIndex(" + fromIndex +") > toIndex(" + toIndex + ")");
            final long size = toIndex - fromIndex;
            final LinkNode<E> headSentinel = (fromIndex == 0) ? this.iGetHeadSentinel() : iGetNode(fromIndex-1);
            final LinkNode<E> tailSentinel = (size == 0L)
                                             ? iGetNodeAfterOrTailSentinel(headSentinel)
                                             : null; // tailSentinel is unknown
            return new SubList<E>(nodableLinkedList(), headSentinel, tailSentinel, subList, size);
        }
        
        /**
         * Returns a view of the portion of this sublist between the specified
         * firstNode, and lastNode (both inclusive). The returned {@code SubList} is
         * backed by this sublist, so structural changes in the returned {@code SubList}
         * are reflected in this sublist. The returned {@code SubList} supports all of
         * the optional {@code List} operations.
         * <p>
         * If the specified firstNode is {@code null}, an empty {@code SubList},
         * positioned right before the specified lastNode, is returned. If the specified
         * lastNode is {@code null}, an empty {@code SubList}, positioned right after
         * the specified firstNode, is returned. if both the specified firstNode and
         * lastNode are {@code null}, an empty {@code SubList}, positioned at the end of
         * this sublist, is returned.
         * <p>
         * This method verifies the specified lastNode comes after the specified
         * FirstNode in this {@code SubList}. Also, the size of the returned
         * {@code SubList} is known.
         * <p>
         * This method eliminates the need for explicit range operations (of the sort
         * that commonly exist for arrays). Any operation that expects a list can be
         * used as a range operation by passing a {@code SubList} view instead of a
         * whole list. For example, the following idiom removes a range of elements from
         * a list:
         * <pre>
         * {@code
         *      list.subList(firstNode, lastNode).clear();
         * }
         * </pre>
         * 
         * Similar idioms may be constructed for {@code indexOf} and
         * {@code lastIndexOf}, and all of the algorithms in the {@code Collections}
         * class can be applied to a {@code SubList}.
         * <p>
         * The semantics of the {@code SubList} returned by this method become undefined
         * if the {@code NodableLinkedList} is <i>structurally modified</i> in any way
         * other than via the returned {@code SubList} or any of its sublists.
         * (Structural modifications are those that change the size of this sublist, or
         * otherwise perturb it in such a fashion that iterations in progress may yield
         * incorrect results.) A {@code ConcurrentModificationException} is thrown for
         * any operation on a {@code SubList} that is structurally unsound.
         *
         * @param firstNode low endpoint (inclusive) of the {@code SubList}
         * @param lastNode  high endpoint (inclusive) of the {@code SubList}
         * @return a view of the specified range within this sublist
         * @throws IllegalArgumentException if any specified node is not linked to this
         *                                  sublist, or the lastNode comes before the
         *                                  firstNode in this sublist.
         */
        public LinkedSubNodes subList(Node<E> firstNode, Node<E> lastNode) {
            return newSubList(firstNode, lastNode).linkedSubNodes;
        }
        
        /**
         * Returns a NodableLinkedList.SubList.
         * 
         * @param firstNode low endpoint (inclusive) of the {@code SubList}
         * @param lastNode  high endpoint (inclusive) of the {@code SubList}
         * @return returns a NodableLinkedList.SubList
         * @throws IllegalArgumentException if any specified node is not linked to this
         *                                  list, or the lastNode comes right before
         *                                  the firstNode in this list.
         */
        private SubList<E> newSubList(Node<E> firstNode, Node<E> lastNode) {
            checkForModificationException();
            if (firstNode == null && lastNode == null) {
                // both firstNode and lastNode are null
                final LinkNode<E> tailSentinel = getConfirmedTailSentinel();
                return new SubList<E>(nodableLinkedList(),
                        iGetNodeBeforeOrHeadSentinel(tailSentinel), tailSentinel, subList, 0L);
            } else if (firstNode == null) {
                // only the lastNode is specified
                checkSubListContainsNode(lastNode, "Specified last node is not linked to this sublist");
                return new SubList<E>(nodableLinkedList(),
                        iGetNodeBeforeOrHeadSentinel(lastNode.linkNode()), lastNode.linkNode(), subList, 0L);
            } else if (lastNode == null) {
                // only the firstNode is specified
                checkSubListContainsNode(firstNode, "Specified first node is not linked to this sublist");
                return new SubList<E>(nodableLinkedList(),
                        firstNode.linkNode(), iGetNodeAfterOrTailSentinel(firstNode.linkNode()), subList, 0L);
            }
            // both firstNode and LastNode are specified
            linkedNodes().checkListContainsNode(firstNode, "Specified first node is not linked to this list");
            linkedNodes().checkListContainsNode(lastNode, "Specified last node is not linked to this list");
            LinkNode<E> node;
            long subListSize = 0;
            boolean foundFirstNode = false;
            boolean foundLastNode = false;
            final LinkNode<E> firstLinkNode = firstNode.linkNode();
            final LinkNode<E> lastLinkNode = lastNode.linkNode();
            if (sizeIsKnown()) {
                long remaining = longSize();
                for (node = iGetNodeAfterFromListWithKnownTailSentinel(iGetHeadSentinel()); remaining > 0;
                        node = iGetNodeAfterFromListWithKnownTailSentinel(node), remaining--) {    
                    if (node == firstLinkNode) foundFirstNode = true;
                    if (foundFirstNode) subListSize++;
                    if (node == lastLinkNode) {
                        foundLastNode = true;
                        break;
                    }
                }
                if (remaining == 0) this.setTailSentinel(node);
                if (remaining == 1) this.setTailSentinel(iGetNodeAfterFromListWithKnownTailSentinel(node));
            } else {
                long listSize = 0;
                for (node = iGetNodeAfterOrTailSentinel(iGetHeadSentinel()); node != iGetTailSentinel();
                        node = iGetNodeAfterOrTailSentinel(node)) {
                    listSize++;
                    if (node == firstLinkNode) foundFirstNode = true;
                    if (foundFirstNode) subListSize++;
                    if (node == lastLinkNode) {
                        foundLastNode = true;
                        break;
                    }
                }
                if (node == iGetTailSentinel()) this.setSize(listSize);
                else if (iGetNodeAfterOrTailSentinel(node) == iGetTailSentinel()) this.setSize(listSize + 1L);
            }
            if (foundLastNode && !foundFirstNode) {
                throw new IllegalArgumentException(
                        "specified last node comes before the specified first node in this sublist, or the specified first node is not part of this sublist");
            }
            if (!foundFirstNode) {
                throw new IllegalArgumentException("specified first node is not part of this sublist");
            }
            if (!foundLastNode) {
                throw new IllegalArgumentException("specified last node is not part of this sublist");
            }
            return new SubList<E>(nodableLinkedList(),
                    iGetNodeBeforeOrHeadSentinel(firstLinkNode),
                    iGetNodeAfterOrTailSentinel(lastLinkNode), subList, subListSize);
        }

        /**
         * Sorts this sublist according to the order induced by the specified
         * comparator.
         * <p>
         * The specified comparator compares the {@code Nodes} not the elements of the
         * {@code Nodes}. for example:
         * {@code sort((node1, node2) -> { return node1.compareTo(node2); });}, or
         * {@code sort((node1, node2) -> { return
         * node1.element().compareTo(node2.element()); });}.
         *
         * If the specified comparator is {@code null}, all elements in this sublist
         * must implement the {@code Comparable} interface and the elements' natural
         * ordering should be used.
         * <p>
         * <strong>Implementation Specification:</strong> This implementation obtains an
         * array containing all nodes in this sublist, sorts the array using
         * {@code Arrays.sort(T[] a, Comparator<? super T> c)}, and then effectively
         * clears the sublist and puts the sorted nodes from the array back into this
         * sublist in order. If this sublist's {@code size > Integer.MAX_VALUE-8}, a
         * {@link #mergeSort} is performed.
         * <p>
         * <strong>Implementation Note:</strong> This implementation is a stable,
         * adaptive, iterative mergesort that requires far fewer than n lg(n)
         * comparisons when the input array is partially sorted, while offering the
         * performance of a traditional mergesort when the input array is randomly
         * ordered. If the input array is nearly sorted, the implementation requires
         * approximately n comparisons. Temporary storage requirements vary from a small
         * constant for nearly sorted input arrays to n/2 object references for randomly
         * ordered input arrays.
         * <p>
         * The implementation takes equal advantage of ascending and descending order in
         * its input array, and can take advantage of ascending and descending order in
         * different parts of the same input array. It is well-suited to merging two or
         * more sorted arrays: simply concatenate the arrays and sort the resulting
         * array.
         * <p>
         * The implementation was adapted from Tim Peters's list sort for Python
         * (<a href="http://svn.python.org/projects/python/trunk/Objects/listsort.txt">
         * TimSort</a>). It uses techniques from Peter McIlroy's "Optimistic Sorting and
         * Information Theoretic Complexity", in Proceedings of the Fourth Annual
         * ACM-SIAM Symposium on Discrete Algorithms, pp 467-474, January 1993.
         *
         * @see Arrays#sort
         * @param comparator the {@code Comparator} used to compare {@code Nodes}. A
         *                   {@code null} value indicates that the elements' natural
         *                   ordering should be used
         * @throws ClassCastException if this sublist contains elements that are not
         *                            <i>mutually comparable</i> using the specified
         *                            comparator
         */
        @Override
        public void sort(Comparator<? super Node<E>> comparator) {
            checkForModificationException();
            if (longSize() < 2L) return;
            if (longSize() > Integer.MAX_VALUE-8) {
                mergeSort(comparator);
                return;
            }
            @SuppressWarnings("unchecked")
            final LinkNode<E>[] sortedNodes = new LinkNode[size()]; 
            final ListIterator<Node<E>> listIterator = linkNodeListIterator(0);
            for (int index = 0; listIterator.hasNext(); sortedNodes[index++] = (LinkNode<E>)listIterator.next());
            Arrays.sort(sortedNodes, comparator);
            LinkNode<E> node = iGetFirstNode();
            for (LinkNode<E> sortedNode: sortedNodes) {
                LinkNode.swapNodes(node, sortedNode);
                node = iGetNodeAfter(sortedNode); // basically node = node.next() since
                                                  // sortedNode has replaced node's position in the list
            }
            updateParentModCount();
        }
        
        /**
         * Sorts this sublist according to the order induced by the specified
         * comparator.
         * <p>
         * The specified comparator compares the {@code Nodes} not the elements of the
         * {@code Nodes}. for example: {@code sort((node1, node2) -> { return
         * node1.compareTo(node2); });}, or {@code sort((node1, node2) -> { return
         * node1.element().compareTo(node2.element()); });}.
         *
         * If the specified comparator is {@code null}, all elements in this sublist
         * must implement the {@code Comparable} interface and the elements' natural
         * ordering should be used.
         * <p>
         * <strong>Implementation Note:</strong> This implementation is a stable,
         * iterative mergesort that requires n lg(n) comparisons. this implementation
         * avoids the N auxiliary storage cost normally associated with a mergesort.
         *
         * The implementation was adapted from Simon Tatham's Mergesort for Linked Lists
         * (<a href=
         * "https://www.chiark.greenend.org.uk/~sgtatham/algorithms/listsort.html">
         * SimonTathamMergesort</a>).
         *
         * @param comparator the {@code Comparator} used to compare {@code Nodes}. A
         *                   {@code null} value indicates that the elements' natural
         *                   ordering should be used
         * @throws ClassCastException if this sublist contains elements that are not
         *                            <i>mutually comparable</i> using the specified
         *                            comparator
         */
        public void mergeSort(Comparator<? super Node<E>> comparator) {
            checkForModificationException();
            NodableLinkedList.mergeSort(this, comparator);
            updateParentModCount();
        }

        /**
         * Returns an array containing all of the {@code SubListNodes} in this sublist
         * in proper sequence (from first to last node). The {@code Nodes} in the array
         * are still linked to this sublist.
         * <p>
         * Although the {@code Nodes} in the returned array are still linked to this
         * sublist, it is safe for the caller to modify the array. No operation on this
         * sublist, will modify the returned array, however, the state of the
         * {@code Nodes} in the array may change.
         * <p>
         * This method acts as bridge between array-based and collection-based APIs.
         *
         * @return an array containing all of the {@code SubListNodes} in this sublist
         *         in proper sequence
         * @throws IllegalStateException if this sublist is too large to fit in an array
         */
        @Override
        public Object[] toArray() {
            checkListCanFitInAnArray(longSize());
            final Object[] nodes = new Object[(int) longSize()];
            int index = 0;
            for (Node<E> node: this) nodes[index++] = node;
            return nodes;
        }

        /**
         * Returns an array containing all of the {@code SubListNodes} in this sublist
         * in proper sequence (from first to last node); the runtime type of the
         * returned array is that of the specified array. If this sublist fits in the
         * specified array, it is returned therein. Otherwise, a new array is allocated
         * with the runtime type of the specified array and the size of this sublist.
         * The {@code Nodes} in the array are still linked to this sublist.
         * <p>
         * If this sublist fits in the specified array with room to spare (i.e., the
         * array has more elements than this sublist), the element in the array
         * immediately following the end of this sublist is set to {@code null}. (This
         * is useful in determining the length of the sublist since {@code Nodes} are
         * never null.)
         * <p>
         * Although the {@code Nodes} in the returned array are still linked to this
         * sublist, it is safe for the caller to modify the array. No operation on this
         * sublist, will modify the returned array, however, the state of the
         * {@code Nodes} in the array may change.
         * <p>
         * Like the {@link #toArray()} method, this method acts as bridge between
         * array-based and collection-based APIs. Further, this method allows precise
         * control over the runtime type of the output array, and may, under certain
         * circumstances, be used to save allocation costs.
         *
         * Note that {@code toArray(new Object[0])} is identical in function to
         * {@code toArray()}.
         *
         * @param array the array into which the {@code Nodes} of this sublist are to be
         *              stored, if it is big enough; otherwise, a new array of the same
         *              runtime type is allocated for this purpose.
         * @return an array containing the {@code SubListNodes} of this sublist
         * @throws ArrayStoreException   if the runtime type of the specified array is
         *                               not a supertype of the runtime type of every
         *                               node in this sublist
         * @throws IllegalStateException if this sublist is too large to fit in an array
         * @throws NullPointerException  if the specified array is {@code null}
         */
        @Override
        @SuppressWarnings("unchecked")
        public <T> T[] toArray(T[] array) {
            checkListCanFitInAnArray(longSize());
            if (array.length < longSize()) {
                array = (T[]) java.lang.reflect.Array.newInstance(array.getClass().getComponentType(), (int) longSize());
            }
            int index = 0;
            Object[] nodes = array;
            for (Node<E> node: this) nodes[index++] = node;
            if (array.length > longSize()) array[(int) longSize()] = null;
            return array;
        }
        
        /**
         * Returns a {@code ListIterator} of the {@code SubListNodes} in this sublist
         * (in proper sequence), starting at the specified position in this sublist.
         * Obeys the general contract of {@code List.listIterator(int)}. if the
         * specified {@code index == longSize()}, the {@code ListIterator} will be
         * positioned at the end of this sublist.
         * <p>
         * <strong>Implementation Note:</strong> The {@code ListIterator} returned by
         * this method behaves differently when the sublist's
         * {@code size > Integer.MAX_VALUE}. Methods {@code nextIndex} and
         * {@code previousIndex} return -1 if the {@code index > Integer_MAX_VALUE}.
         * <p>
         * The {@code ListIterator} is <i>fail-fast</i>: if the
         * {@code NodableLinkedList} is structurally modified at any time after the
         * Iterator is created, in any way except through the {@code ListIterator's} own
         * {@code remove} or {@code add} methods, the {@code ListIterator} will throw a
         * {@code ConcurrentModificationException}. Thus, in the face of concurrent
         * modification, the iterator fails quickly and cleanly, rather than risking
         * arbitrary, non-deterministic behavior at an undetermined time in the future.
         * <p>
         * Instead of using a {@code ListIterator}, consider iterating over the sublist
         * via {@code SubListNodes}. For example:
         * 
         * <pre>
         * {@code
         *     // sublist is a NodableLinkedList<Integer>.LinkedSubNodes
         *     Node<Integer> subListNode = sublist.get(index);
         *     //                       or sublist.getFirstNode();
         *     while (subListNode != null) {
         *         System.out.println(subListNode.element());
         *         subListNode = subListNode.next();
         *     }
         * }
         * </pre>
         *
         * @param index index of the first {@code Node} to be returned from the
         *              {@code ListIterator} (by a call to {@code next})
         * @return a ListIterator of the {@code SubListNodes} in this sublist (in proper
         *         sequence), starting at the specified position in this sublist
         * @throws IndexOutOfBoundsException if the specified index is out of range
         *                                   {@code (index < 0 || index > longSize())}
         * @see List#listIterator(int)
         */
        @Override
        public ListIterator<Node<E>> listIterator(int index) {
            checkForModificationException();
            if (index < 0) throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
            return new SubListNodeListIterator(this, index, IndexType.ABSOLUTE, iGetNode(index));
        }
        
        /**
         * Returns a {@code ListIterator} of the {@code SubListNodes} in this sublist
         * (in proper sequence), starting at the specified node in this sublist. if the
         * specified node is {@code null}, the {@code ListIterator} will be positioned
         * right after the last {@code Node} in this sublist.
         * <p>
         * <strong>Implementation Note:</strong> The {@code ListIterator} returned by
         * this method behaves differently when the sublist's
         * {@code size > Integer.MAX_VALUE}. Methods {@code nextIndex} and
         * {@code previousIndex} return -1 if the {@code index > Integer_MAX_VALUE}.
         * <p>
         * The {@code ListIterator} is <i>fail-fast</i>: if the
         * {@code NodableLinkedList} is structurally modified at any time after the
         * Iterator is created, in any way except through the {@code ListIterator's} own
         * {@code remove} or {@code add} methods, the {@code ListIterator} will throw a
         * {@code ConcurrentModificationException}. Thus, in the face of concurrent
         * modification, the iterator fails quickly and cleanly, rather than risking
         * arbitrary, non-deterministic behavior at an undetermined time in the future.
         * <p>
         * Instead of using a {@code ListIterator}, consider iterating over the sublist
         * via {@code SubListNodes}. For example:
         * 
         * <pre>
         * {@code
         *     // sublist is a NodableLinkedList<Integer>.LinkedSubNodes
         *     Node<Integer> subListNode = (node == null) ? null : node.subListNode(sublist.subList());
         *     while (subListNode != null) {
         *         System.out.println(subListNode.element());
         *         subListNode = subListNode.next();
         *     }
         * }
         * </pre>
         *
         * @param node first {@code Node} to be returned from the {@code ListIterator}
         *             (by a call to {@code next})
         * @return a ListIterator of the {@code SubListNodes} in this sublist (in proper
         *         sequence), starting at the specified node in the sublist
         * @throws IllegalArgumentException if the specified node is not linked to this
         *                                  sublist
         */
        public ListIterator<Node<E>> listIterator(Node<E> node) {
            checkForModificationException();
            if (node == null) {
                return new SubListNodeListIterator(this, longSize(), IndexType.ABSOLUTE, getConfirmedTailSentinel());
            }
            final long index = iGetIndex(node);
            if (index < 0) throw new IllegalArgumentException("specified node is not part of this sublist");
            return new SubListNodeListIterator(this, index, IndexType.ABSOLUTE, node.linkNode());
        }
        
        /**
         * Returns a {@code ListIterator} of the {@code LinkNodes} in this sublist (in
         * proper sequence), starting at the specified position in this sublist. Obeys
         * the general contract of {@code List.listIterator(int)}.
         * <p>
         * The returned {@code ListIterator} should perform better and have a smaller
         * memory footprint than the standard {@code ListIterator} returned by
         * {@link #listIterator(int)}. The standard {@code ListIterator} returns newly
         * created {@code SubListNodes}, where as, the {@code ListIterator} returned by
         * this method returns the existing {@code LinkNodes} which comprise this
         * sublist. As a reminder, a {@code LinkNode} traverses the base
         * {@code LinkedNodes} and not the sublist.
         * <p>
         * This example builds an ArrayList of {@code LinkNodes} which comprise this
         * sublist:
         * 
         * <pre>
         * {@code
         *     // sublist is a NodableLinkedList<Integer>.LinkedSubNodes
         *     final int estimatedSize = 0;
         *     final ArrayList<LinkNode<Integer>> arrayList = new ArrayList<>(estimatedSize);
         *     final ListIterator<Node<Integer>> listIterator = sublist.linkNodeListIterator(0);
         *     while (listIterator.hasNext())
         *         arrayList.add((LinkNode<Integer>) listIterator.next());
         *     for (LinkNode<Integer> linkNode : arrayList)
         *         System.out.println(linkNode.element());
         * }
         * </pre>
         * <p>
         * <strong>Implementation Note:</strong> The {@code ListIterator} returned by
         * this method behaves differently when the sublist's
         * {@code size > Integer.MAX_VALUE}. Methods {@code nextIndex} and
         * {@code previousIndex} return -1 if the {@code index > Integer_MAX_VALUE}.
         * <p>
         * The {@code ListIterator} is <i>fail-fast</i>: if the
         * {@code NodableLinkedList} is structurally modified at any time after the
         * Iterator is created, in any way except through the {@code ListIterator's} own
         * {@code remove} or {@code add} methods, the {@code ListIterator} will throw a
         * {@code ConcurrentModificationException}. Thus, in the face of concurrent
         * modification, the iterator fails quickly and cleanly, rather than risking
         * arbitrary, non-deterministic behavior at an undetermined time in the future.
         *
         * @param index index of the first {@code Node} to be returned from the
         *              {@code ListIterator} (by a call to {@code next})
         * @return a ListIterator of the {@code LinkNodes} in this sublist (in proper
         *         sequence), starting at the specified position in this sublist
         * @throws IndexOutOfBoundsException if the specified index is out of range
         *                                   {@code (index < 0 || index > longSize())}
         * @see List#listIterator(int)
         */
        public ListIterator<Node<E>> linkNodeListIterator(int index) {
            checkForModificationException();
            if (index < 0) throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
            return new LinkNodeListIterator(this, index, IndexType.ABSOLUTE, iGetNode(index));
        }
        
        /**
         * Returns a {@code ListIterator} of the {@code LinkNodes} in this sublist (in
         * proper sequence), starting at the specified node in this sublist. if the
         * specified node is {@code null}, the {@code ListIterator} will be positioned
         * right after the last {@code Node} in this sublist.
         * <p>
         * The returned {@code ListIterator} should perform better and have a smaller
         * memory footprint than the standard {@code ListIterator} returned by
         * {@link #listIterator(Node)}. The standard {@code ListIterator} returns newly
         * created {@code SubListNodes}, where as, the {@code ListIterator} returned by
         * this method returns the existing {@code LinkNodes} which comprise this
         * sublist. As a reminder, a {@code LinkNode} traverses the base
         * {@code LinkedNodes} and not the sublist.
         * <p>
         * This example builds an ArrayList of {@code LinkNodes} which comprise this
         * sublist:
         * 
         * <pre>
         * {@code
         *     // sublist is a NodableLinkedList<Integer>.LinkedSubNodes
         *     final int estimatedSize = 0;
         *     final ArrayList<LinkNode<Integer>> arrayList = new ArrayList<>(estimatedSize);
         *     final ListIterator<Node<Integer>> listIterator = sublist.linkNodeListIterator(sublist.getFirstNode());
         *     while (listIterator.hasNext())
         *         arrayList.add((LinkNode<Integer>) listIterator.next());
         *     for (LinkNode<Integer> linkNode : arrayList)
         *         System.out.println(linkNode.element());
         * }
         * </pre>
         * <p>
         * <strong>Implementation Note:</strong> The {@code ListIterator} returned by
         * this method behaves differently when the sublist's
         * {@code size > Integer.MAX_VALUE}. Methods {@code nextIndex} and
         * {@code previousIndex} return -1 if the {@code index > Integer_MAX_VALUE}.
         * <p>
         * The {@code ListIterator} is <i>fail-fast</i>: if the
         * {@code NodableLinkedList} is structurally modified at any time after the
         * Iterator is created, in any way except through the {@code ListIterator's} own
         * {@code remove} or {@code add} methods, the {@code ListIterator} will throw a
         * {@code ConcurrentModificationException}. Thus, in the face of concurrent
         * modification, the iterator fails quickly and cleanly, rather than risking
         * arbitrary, non-deterministic behavior at an undetermined time in the future.
         *
         * @param node first {@code Node} to be returned from the {@code ListIterator}
         *             (by a call to {@code next})
         * @return a ListIterator of the {@code LinkNodes} in this sublist (in proper
         *         sequence), starting at the specified node in the sublist
         * @throws IllegalArgumentException if the specified node is not contained by
         *                                  this sublist
         */
        public ListIterator<Node<E>> linkNodeListIterator(Node<E> node) {
            checkForModificationException();
            if (node == null) {
                return new LinkNodeListIterator(this, longSize(), IndexType.ABSOLUTE, getConfirmedTailSentinel());
            }
            final long index = iGetIndex(node);
            if (index < 0) throw new IllegalArgumentException("specified node is not contained by this sublist");
            return new LinkNodeListIterator(this, index, IndexType.ABSOLUTE, node.linkNode());
        }            
        
    } // LinkedSubNodes
    
    private class ReversedLinkedSubNodes extends LinkedSubNodes {
        
        /**
         * The LinkedSubNodes that was reversed.
         */
        private LinkedSubNodes linkedSubNodes;
        
        /**
         * Constructs a ReversedLinkedSubNodes.
         * 
         * @param sublist        The (Reversed.)SubList this ReversedLinkedSubNodes
         *                       backs
         * @param linkedSubNodes The LinkedSubNodes that was reversed
         */
        private ReversedLinkedSubNodes(SubList<E> sublist, LinkedSubNodes linkedSubNodes) {
            super(sublist);
            this.linkedSubNodes = linkedSubNodes;
            super.updateModCount();
        }
        
        @Override
        int modCount() {
            return linkedSubNodes.modCount();
        }
        
        @Override
        void updateModCount() {
            linkedSubNodes.updateModCount();
            super.updateModCount();
        }
        
        /**
         * Set the protected int modCount inherited from class java.util.AbstractList to
         * the modCount of the LinkedSubNodes that was reversed. The modCount represents the
         * expected modification count. The modCount of the ReversedSubList that this
         * ReversedLinkedSubNodes backs, is also updated.
         */
        private void syncModCountWithBase() {
            subList().setModCount(this.modCount = modCount());
        }
        
        @Override
        LinkNode<E> iGetHeadSentinel() {
            syncModCountWithBase();
            return linkedSubNodes.iGetTailSentinel();
        }
        
        @Override
        LinkNode<E> iGetTailSentinel() {
            syncModCountWithBase();
            return linkedSubNodes.iGetHeadSentinel();
        }
        
        @Override            
        void setHeadSentinel(LinkNode<E> headSentinel) {
            syncModCountWithBase();
            linkedSubNodes.setTailSentinel(headSentinel);
        }
        
        @Override  
        void setTailSentinel(LinkNode<E> tailSentinel) {
            syncModCountWithBase();
            linkedSubNodes.setHeadSentinel(tailSentinel);
        }
        
        @Override
        LinkedSubNodes parent () {
            syncModCountWithBase();
            return linkedSubNodes.parent();
        }
        
        @Override
        long iGetSize() {
            syncModCountWithBase();
            return linkedSubNodes.iGetSize();
        }
        
        @Override
        void setSize(long size) {
            syncModCountWithBase();
            linkedSubNodes.setSize(size);
        }
        
        @Override
        LinkNode<E> iGetMyLinkedNodesHeadSentinel() {
            syncModCountWithBase();
            return linkedSubNodes.iGetMyLinkedNodesTailSentinel();
        }
        
        @Override
        LinkNode<E> iGetMyLinkedNodesTailSentinel() {
            syncModCountWithBase();
            return linkedSubNodes.iGetMyLinkedNodesHeadSentinel();
        }
        
        @Override
        LinkNode<E> tailSentinel() {
            syncModCountWithBase();
            return linkedSubNodes.iGetHeadSentinel();
        }
        
        @Override
        LinkNode<E> getConfirmedTailSentinel() {
            syncModCountWithBase();
            return linkedSubNodes.iGetHeadSentinel();
        }
        
        @Override
        void iAddNodeAfter(LinkNode<E> node, LinkNode<E> afterThisNode) {
            linkedSubNodes.iAddNodeBefore(node, afterThisNode);
            syncModCountWithBase();
        }

        @Override
        void iAddNodeBefore(LinkNode<E> node, LinkNode<E> beforeThisNode) {
            linkedSubNodes.iAddNodeAfter(node, beforeThisNode);
            syncModCountWithBase();
        }

        @Override
        void iRemoveNode(LinkNode<E> node) {
            linkedSubNodes.iRemoveNode(node);
            syncModCountWithBase();
        }

        @Override
        void iReplaceNode(LinkNode<E> node, LinkNode<E> replacementNode) {
            linkedSubNodes.iReplaceNode(node, replacementNode);
            syncModCountWithBase();
        }

        @Override
        boolean iHasNodeAfter(LinkNode<E> node) {
            syncModCountWithBase();
            return linkedSubNodes.iHasNodeBefore(node);           
        }

        @Override
        boolean iHasNodeBefore(LinkNode<E> node) {
            syncModCountWithBase();
            return linkedSubNodes.iHasNodeAfter(node);           
        }
        
        @Override
        LinkNode<E> iGetFirstNode() {
            syncModCountWithBase();
            return linkedSubNodes.iGetLastNode();
        }
        
        @Override
        LinkNode<E> iGetLastNode() {
            syncModCountWithBase();
            return linkedSubNodes.iGetFirstNode();
        }
        
        @Override
        LinkNode<E> iGetNodeAfter(LinkNode<E> node) {
            syncModCountWithBase();
            return linkedSubNodes.iGetNodeBefore(node);
        }
        
        @Override
        LinkNode<E> iGetNodeAfterOrTailSentinel(LinkNode<E> node) {
            syncModCountWithBase();
            return linkedSubNodes.iGetNodeBeforeOrHeadSentinel(node);
        }
        
        @Override
        LinkNode<E> iGetNodeAfterFromListWithKnownTailSentinel(LinkNode<E> node) {
            syncModCountWithBase();
            return linkedSubNodes.iGetNodeBeforeOrHeadSentinel(node);
        }
        
        @Override
        LinkNode<E> iGetNodeBefore(LinkNode<E> node) {
            syncModCountWithBase();
            return linkedSubNodes.iGetNodeAfter(node);
        }
        
        @Override
        LinkNode<E> iGetNodeBeforeOrHeadSentinel(LinkNode<E> node) {
            syncModCountWithBase();
            return linkedSubNodes.iGetNodeAfterOrTailSentinel(node);
        }
        
        @Override
        public LinkNode<E> forwardLinkNodeOf(Node<E> node) {
            return linkedSubNodes.forwardLinkNodeOf(node).reversed();
        }
        
        @Override
        public boolean isReversed() {
            return !linkedSubNodes.isReversed();
        }
        
        @Override
        public LinkedSubNodes reversed() {
            syncModCountWithBase();
            return this.linkedSubNodes;
        }
        
    } // ReversedLinkedSubNodes

    /**
     * Index type (ABSOLUTE or RELATIVE) used by the ListIterators.
     * 
     * An index can be ABSOLUTE or RELATIVE. An ABSOLUTE index specifies the exact
     * position in a list, where the first node in the list has an index of zero. A
     * RELATIVE index is relative to a specified node, where the specified node has
     * an index of zero, all nodes that come before the specified node have a
     * negative index, and all nodes that come after the specified node have a
     * positive index.
     */
    enum IndexType {
        
        /**
         * The index specifies the exact position in a list, where the first node in a
         * list has an index of zero. The ListIterator methods nextIndex() and
         * previousIndex() return -1 if the {@code index > Integer_MAX_VALUE}. Also,
         * ListIterator method nextIndex() returns longSize() if the ListIterator is
         * positioned at the end of the list.
         */
        ABSOLUTE,
        
        /**
         * The index is relative to a specified node in a list. The specified node has
         * an index of zero. All nodes that come before the specified node in the list
         * have a negative index, and all nodes that come after the specified node in
         * the list have a positive index.
         * 
         * The ListIterator methods nextIndex() and previousIndex() return
         * Integer.MIN_VALUE if the {@code index < Integer.MIN_VALUE} or
         * Integer.MAX_VALUE if the {@code index > Integer.MAX_VALUE}. Also, the method
         * nextIndex() returns longSize() if the ListIterator is positioned at the end
         * of the list, and method previousIndex() returns -longSize() if the
         * ListIterator is positioned at the beginning of the list.
         */
        RELATIVE
    }

    /**
     * The ListIterator that all other ListIterators utilize.
     */
    private class NodeListIterator implements ListIterator<Node<E>> {
        
        /**
         * The internal linked list (LinkedNodes or LinkedSubNodes) that is being
         * iterated over.
         */
        private final InternalLinkedList list;
        
        /**
         * Represents the cursor position in the list. Technically the cursor position
         * is between the cursorNode and the next node in the list.
         */
        private LinkNode<E> cursorNode;

        /**
         * The index of the cursorNode in the list. The index can be Absolute or
         * relative, therefore, the cursorIndex can be negative.
         */
        private long cursorIndex;
        
        /**
         * The index type: ABSOLUTE or RELATIVE.
         * <p>
         * An index can be ABSOLUTE or RELATIVE. An ABSOLUTE index specifies a position
         * in the list, where the first node in the list has an index of zero. A
         * RELATIVE index is relative to the starting node, where the starting node has
         * an index of zero, all nodes that come before the starting node have a
         * negative index, and all nodes that come after the starting node have a
         * positive index.
         */
        private final IndexType indexType;
        
        /**
         * The target node for the remove() and set() methods. Can be null, which means
         * there is no valid target.
         */
        private LinkNode<E> targetNode;
        
        /**
         * The expected modification count. If the NodableLinkedList's modCount does not
         * match the expected modCount, a ConcurrentModificationException is thrown.
         */
        private int expectedModCount = modCount();
        
        /**
         * Constructs a ListIterator of the LinkedNodes.
         * 
         * For sublists, there is no guarantee that the tailSentinel comes after the
         * headSentinel in the list. If it doesn't, this ListIterator will iterate all
         * the way to the list, at which time, it will throw an IllegalStateException
         * indicating the end of the list was reached unexpectedly. The specified node
         * is guaranteed to come after the headSentinel and before the tailSentinel,
         * assuming the tailSentinel comes before the headSentinel.
         * 
         * An index can be ABSOLUTE or RELATIVE. An ABSOLUTE index specifies a position
         * in the list, where the first node in the list has an index of zero. A
         * RELATIVE index is relative to the starting node, where the starting node has
         * an index of zero, all nodes that come before the starting node have a
         * negative index, and all nodes that come after the starting node have a
         * positive index.
         * 
         * @param list      LinkedNodes or LinkedSubNodes to iterate over
         * @param index     the index of the starting node
         * @param indexType ABSOLUTE or RELATIVE.
         * @param node      the starting node; the first node to be returned by a call
         *                  to next()
         */
        private NodeListIterator(InternalLinkedList list,
                long index, IndexType indexType,
                LinkNode<E> node) {
            this.list = list;
            this.indexType = indexType;
            this.cursorIndex = index - 1L;
            this.cursorNode = list.iGetNodeBeforeOrHeadSentinel(node);
            this.targetNode = null;
        }
        
        InternalLinkedList list() {
            return this.list;
        }
        
        private int modCount() {
            return NodableLinkedList.this.modCount();
        }
        
        private void checkForModificationException() {
            if (modCount() != expectedModCount) {
                throw new ConcurrentModificationException();
            }
        }
        
        private LinkNode<E> targetNode() {
            checkForModificationException();
            if (targetNode == null) {
                throw new IllegalStateException(
                        "Neither next nor previous have been called, or remove or add have been called after the last call to next or previous");
            }
            return targetNode;
        }
        
        LinkNode<E> headSentinel() {
            return list.iGetHeadSentinel();
        }
        
        LinkNode<E> tailSentinel() {
            return list.iGetTailSentinel();
        }
        
        void addNodeAfter(LinkNode<E> node, LinkNode<E> afterThisNode) {
            list.iAddNodeAfter(node, afterThisNode);
        }
        
        LinkNode<E> getNodeAfter(LinkNode<E> node) {
            final LinkNode<E> nextNode = list.iGetNodeAfterFromListWithKnownTailSentinel(node);
            if (nextNode == tailSentinel()) return null;
            if (nextNode == list.iGetMyLinkedNodesTailSentinel()) {
                throw new IllegalStateException("End of list reached unexpectedly; the sublist's last node most likely comes before the sublists's first node in the list");
            }
            return nextNode;
        }
        
        LinkNode<E> getNodeBeforeOrHeadSentinel(LinkNode<E> node) {
            return list.iGetNodeBeforeOrHeadSentinel(node);
        }
        
        void removeNode(LinkNode<E> node) {
            list.iRemoveNode(node);
        }
        
        void replaceNode(LinkNode<E> node, LinkNode<E> replacementNode) {
            list.iReplaceNode(node, replacementNode);
        }

        @Override
        public boolean hasNext() {
            checkForModificationException();
            final long size = list.iGetSize();
            if (size >= 0 && cursorIndex >= (size - 1L)) {
                return false;
            }
            final LinkNode<E> cursorNodeNext = getNodeAfter(cursorNode);
            return (cursorNodeNext != null && cursorNodeNext != tailSentinel());
        }

        @Override
        public boolean hasPrevious() {
            checkForModificationException();
            return (cursorNode != headSentinel());
        }

        @Override
        public Node<E> next() {
            checkForModificationException();
            targetNode = null;
            if (!hasNext()) throw new NoSuchElementException();
            cursorNode = getNodeAfter(cursorNode);
            cursorIndex++;
            targetNode = cursorNode;
            return targetNode;
        }

        @Override
        public Node<E> previous() {
            checkForModificationException();
            targetNode = null;
            if (!hasPrevious()) throw new NoSuchElementException();
            targetNode = cursorNode;
            cursorNode = getNodeBeforeOrHeadSentinel(cursorNode);
            cursorIndex--;
            return targetNode;
        }       

        @Override
        public int nextIndex() {
            checkForModificationException();
            if (indexType == IndexType.RELATIVE) {
                if (!hasNext()) return (longSize() >= Integer.MAX_VALUE)
                                       ? Integer.MAX_VALUE
                                       : (int)longSize();
                if (cursorIndex+1 > Integer.MAX_VALUE) return Integer.MAX_VALUE;
                if (cursorIndex+1 < Integer.MIN_VALUE) return Integer.MIN_VALUE;
            } else {
                // absolute index
                if (!hasNext()) return (longSize() > Integer.MAX_VALUE)
                                       ? -1
                                       : (int)longSize();
                if (cursorIndex+1 > Integer.MAX_VALUE) return -1;
            }
            return (int)(cursorIndex+1);
        }

        @Override
        public int previousIndex() {
            checkForModificationException();
            if (indexType == IndexType.RELATIVE) {
                if (!hasPrevious()) return (longSize() <= Integer.MIN_VALUE)
                                           ? Integer.MIN_VALUE
                                           : -(int)longSize();
                if (cursorIndex > Integer.MAX_VALUE) return Integer.MAX_VALUE;
                if (cursorIndex < Integer.MIN_VALUE) return Integer.MIN_VALUE;
            } else {
                // absolute index
                if (!hasPrevious()) return -1;
                if (cursorIndex > Integer.MAX_VALUE) return -1;
            }
            return (int)cursorIndex;
        }

        @Override
        public void add(Node<E> node) {
            checkForModificationException();
            checkNodeIsUnLinked(node);
            addNodeAfter(node.linkNode(), cursorNode);
            cursorIndex++;
            cursorNode = node.linkNode();
            targetNode = null;
            expectedModCount = modCount();
        }

        @Override
        public void remove() {
            checkForModificationException();
            if (targetNode == null) {
                throw new IllegalStateException("Neither next nor previous have been called, or remove or add have been called after the last call to next or previous");
            }
            if (cursorNode == targetNode) {
                cursorIndex--;
                cursorNode = getNodeBeforeOrHeadSentinel(cursorNode);
            }
            removeNode(targetNode);
            targetNode = null;
            expectedModCount = modCount();
        }

        @Override
        public void set(Node<E> node) {
            checkForModificationException();
            if (targetNode == null) {
                throw new IllegalStateException("Neither next nor previous have been called, or remove or add have been called after the last call to next or previous");
            }
            checkNodeIsUnLinked(node);
            if (cursorNode == targetNode) cursorNode = node.linkNode();
            replaceNode(targetNode, node.linkNode());
            targetNode = node.linkNode();
            expectedModCount = modCount();
        }

        @Override
        public void forEachRemaining(Consumer<? super Node<E>> action) {
            checkForModificationException();
            if (action == null) throw new NullPointerException();
            while (hasNext()) {
                action.accept(getNodeAfter(cursorNode));
                cursorNode = getNodeAfter(cursorNode);
                cursorIndex++;
                targetNode = cursorNode;
            }
            checkForModificationException();
        }

    } // NodeListIterator
    
    private class NodeReverseListIterator extends NodeListIterator {
        
        private NodeReverseListIterator(InternalLinkedList list,
                long index, IndexType indexType,
                LinkNode<E> node) {
            super(list, index, indexType, node);
        }
        
        @Override
        LinkNode<E> headSentinel() {
            return super.tailSentinel();
        }
        
        @Override
        LinkNode<E> tailSentinel() {
            return super.headSentinel();
        }        
        
        @Override
        void addNodeAfter(LinkNode<E> node, LinkNode<E> afterThisNode) {
            list().iAddNodeBefore(node, afterThisNode);
        }            
        
        @Override
        LinkNode<E> getNodeAfter(LinkNode<E> node) {
            return list().iGetNodeBefore(node);
        }
        
        @Override
        LinkNode<E> getNodeBeforeOrHeadSentinel(LinkNode<E> node) {
            return list().iGetNodeAfterOrTailSentinel(node);
        }
        
    } // NodeReverseListIterator

    private class ElementListIterator implements ListIterator<E> {

        private final NodeListIterator listIterator;
        
        private ElementListIterator(InternalLinkedList list,
                long index, IndexType indexType,
                LinkNode<E> node) {
            listIterator = new NodeListIterator(list, index, indexType, node);
        }
        
        // used by ElementReverseListIterator
        private ElementListIterator(NodeListIterator listIterator) {
            this.listIterator = listIterator;
        }

        @Override
        public boolean hasNext() {
            return listIterator.hasNext();
        }

        @Override
        public boolean hasPrevious() {
            return listIterator.hasPrevious();
        }

        @Override
        public E next() {
            return listIterator.next().element();
        }

        @Override
        public E previous() {
            return listIterator.previous().element();
        }       

        @Override
        public int nextIndex() {
            return listIterator.nextIndex();
        }

        @Override
        public int previousIndex() {
            return listIterator.previousIndex();
        }

        @Override
        public void add(E element) {
            listIterator.add(node(element));
        }

        @Override
        public void remove() {
            listIterator.remove();
        }

        @Override
        public void set(E element) {
            listIterator.targetNode().set(element);
        }

        @Override
        public void forEachRemaining(Consumer<? super E> action) {
            listIterator.forEachRemaining((node) -> {
                action.accept(node.element());
            });
        }       

    } // ElementListIterator
    
    private class ElementReverseListIterator extends ElementListIterator {

        private ElementReverseListIterator(InternalLinkedList list, long index, IndexType indexType, LinkNode<E> node) {
            super(new NodeReverseListIterator(list, index, indexType, node));
        }

    } // ElementReverseListIterator   
    
    private class LinkNodeListIterator extends NodeListIterator {
        
        private LinkNodeListIterator(InternalLinkedList list,
                long index, IndexType indexType, LinkNode<E> node) {
            super(list, index, indexType, node);
        }
        
        @Override
        public LinkNode<E> next() {
            return (LinkNode<E>)super.next();
        }
        
        @Override
        public LinkNode<E> previous() {
            return (LinkNode<E>)super.previous();
        }        
        
    } // LinkNodeListIterator
    
    private class LinkNodeReverseListIterator extends NodeReverseListIterator {
        
        private LinkNodeReverseListIterator(InternalLinkedList list,
                long index, IndexType indexType, LinkNode<E> node) {
            super(list, index, indexType, node);
        }
        
        @Override
        public LinkNode<E> next() {
            return (LinkNode<E>)super.next();
        }
        
        @Override
        public LinkNode<E> previous() {
            return (LinkNode<E>)super.previous();
        }        
        
    } // LinkNodeReverseListIterator
    
    private class SubListNodeListIterator extends NodeListIterator {
        
        private final SubList<E> subList;
        
        private SubListNodeListIterator(NodableLinkedList<E>.LinkedSubNodes list,
                long index, IndexType indexType, LinkNode<E> node) {
            super(list, index, indexType, node);
            this.subList = list.subList();
        }
        
        @Override
        public SubListNode<E> next() {
            return new SubListNode<E>((LinkNode<E>)super.next(), subList);
        }
        
        @Override
        public SubListNode<E> previous() {
            return new SubListNode<E>((LinkNode<E>)super.previous(), subList);
        }        
        
    } // SubListNodeListIterator    

    private static final class NodeSpliterator<E> implements Spliterator<Node<E>> {

        private static final int BATCH_INCREMENT = 1 << 10;
        private static final int MAX_BATCH_SIZE  = 1 << 25;
        
        private final NodableLinkedList<E>.LinkedNodes list;

        private LinkNode<E> cursor;
        private long remainingSize = -1L;
        private int batchSize = 0;
        private int expectedModCount;

        private NodeSpliterator(NodableLinkedList<E>.LinkedNodes list) {
            this.list = list;
            this.cursor = list.iGetHeadSentinel();
        }

        private void bind() {
            this.remainingSize = list.iGetSize();
            this.expectedModCount = list.modCount();
        }
        
        private int batchSize() {
            return batchSize;
        }
        
        private void checkForModificationException() {
            if (this.expectedModCount != list.modCount()) {
                throw new ConcurrentModificationException();
            }
        }

        @Override
        public int characteristics() {
            return  Spliterator.NONNULL |
                    Spliterator.ORDERED |
                    Spliterator.SIZED   |
                    Spliterator.SUBSIZED;
        }

        @Override
        public long estimateSize() {
            if (remainingSize < 0L) bind();
            return remainingSize;
        }

        @Override
        public boolean tryAdvance(Consumer<? super Node<E>> action) {
            if (remainingSize < 0L) bind();
            checkForModificationException();
            if (action == null) throw new NullPointerException();
            if (list.iGetNodeAfter(cursor) == null || remainingSize < 1L) return false;
            remainingSize--;
            cursor = list.iGetNodeAfter(cursor);
            action.accept(cursor);
            checkForModificationException();
            return true;
        }

        @Override
        public void forEachRemaining(Consumer<? super Node<E>> action) {
            if (remainingSize < 0L) bind();
            checkForModificationException();
            if (action == null) throw new NullPointerException();
            LinkNode<E> node = cursor;
            while (list.iGetNodeAfter(node) != null && remainingSize-- > 0L) {
                node = list.iGetNodeAfter(node);
                action.accept(node);                    
            }
            cursor = node;
            checkForModificationException();
        }

        @Override
        public Spliterator<Node<E>> trySplit() {
            final Object[] array = trySplit( (node) -> { return node; } );
            return (array == null)
                   ? null
                   : Spliterators.spliterator(array, 0, batchSize,
                         Spliterator.ORDERED | Spliterator.NONNULL);
        }

        private Object[] trySplit(Function<Node<E>, Object> action) {
            if (remainingSize < 0L) bind();
            checkForModificationException();
            if (remainingSize <= 1L) return null;
            int arraySize = batchSize + BATCH_INCREMENT;
            if (arraySize > remainingSize) arraySize = (int)remainingSize;
            if (arraySize > MAX_BATCH_SIZE) arraySize = MAX_BATCH_SIZE;
            Object[] array = new Object[arraySize];
            int index = 0;
            LinkNode<E> node = cursor;
            while (index < arraySize && list.iGetNodeAfter(node) != null) {
                node = list.iGetNodeAfter(node);
                array[index++] = action.apply(node);                    
            }               
            cursor = node;
            batchSize = index;
            remainingSize -= batchSize;
            return array;
        }            

    } // NodeSpliterator

    private static final class ElementSpliterator<E> implements Spliterator<E> {

        private final NodeSpliterator<E> spliterator;
        
        private ElementSpliterator(NodableLinkedList<E>.LinkedNodes list) {
            this.spliterator = new NodeSpliterator<E>(list);
        }

        @Override
        public int characteristics() {
            return  Spliterator.ORDERED |
                    Spliterator.SIZED   | 
                    Spliterator.SUBSIZED;
        }

        @Override
        public long estimateSize() {
            return spliterator.estimateSize();
        }

        @Override
        public boolean tryAdvance(Consumer<? super E> action) {
            return spliterator.tryAdvance((node) -> {
                action.accept(node.element());
            });
        }

        @Override
        public void forEachRemaining(Consumer<? super E> action) {
            spliterator.forEachRemaining((node) -> {
                action.accept(node.element());
            });
        }

        @Override
        public Spliterator<E> trySplit() {
            final Object[] array = spliterator.trySplit((node) -> {
                return node.element();
            });
            return (array == null)
                   ? null
                   : Spliterators.spliterator(array, 0, spliterator.batchSize(),
                           Spliterator.ORDERED);
        }

    } // ElementSpliterator    

    /**
     * Node of a {@link NodableLinkedList.SubList}. A {@code SubListNode} represents
     * a {@link NodableLinkedList.LinkNode} that is associated with a specific
     * {@code SubList}. Any operation performed on a {@code SubListNode} is
     * performed on its associated {@code SubList}. A {@code SubListNode} can be
     * removed from its current {@code SubList} and added to a different
     * {@code SubList}. Note, a {@code SubListNode} is always associated with a
     * {@code SubList}, even when it is unlinked.
     * <p>
     * <b>Performance Consideration:</b> Unlike operations on a {@code LinkNode},
     * operations on a {@code SubListNode} are not necessarily performed in constant
     * time because it may be necessary to verify, in linear time, that the
     * {@code SubListNode} is still a node of its associated {@code SubList}. If the
     * {@code NodableLinkedList} which contains the {@code SubListNode}, is
     * structurally modified in any way except via the {@code SubListNode} itself,
     * the {@code SubListNode} is invalidated and it will be necessary to verify
     * that the {@code SubListNode} is still contained by its associated
     * {@code SubList} the next time the {@code SubListNode} is used.
     * <P>
     * Note, a {@code SubListNode} can be used to naturally traverse its associated
     * {@code SubList} in a forward direction (from the sublist's first {@code Node}
     * to the sublist's last {@code Node} when making successive calls to the
     * {@code next()} method). A reversed {@code SubListNode} can be created via the
     * {@code reversed()} method, which can be used to traverse the {@code SubList}
     * in the opposite direction. For instance, a reversed {@code SubListNode} can
     * be used to traverse a {@code SubList} from the sublist's last node to the
     * sublist's first node when making successive calls to the {@code next()}
     * method. The reverse ordering affects all order-sensitive operations
     * ({@code addAfter(Node)}, {@code addBefore(Node)}, etc.).
     * 
     * @author James Pfeifer
     */
    public static class SubListNode<E> implements Node<E>, Comparable<Node<E>> {
        
        /**
         * The LinkNode backing this SubListNode. The LinkNode is never a
         * ReverseLinkNode.
         */
        private final LinkNode<E> linkNode;
        
        /**
         * The SubList this SubListNode is associated with. A SubListNode is always
         * associated with a SubList even if the SubListNode is unlinked.
         */
        private SubList<E> subList;
        
        /**
         * The modification count when this SubListNode was last known to be associated
         * with its SubList.
         */
        private int expectedModCount;
        
        /**
         * Construct a SubListNode associated with the specified SubList and backed by
         * the specified LinkNode.
         * 
         * @param linkNode the LinkNode which backs this SubListNode
         * @param subList  the SubList associated with this SubListNode
         */
        private SubListNode(LinkNode<E> linkNode, SubList<E> subList) {
            this.linkNode = (linkNode.isReversed()) ? linkNode.reversed() : linkNode;
            this.subList = subList;
            updateExpectedModCount();
        }
        
        /**
         * Construct the super SubListNode for a ReverseSubListNode.
         */
        private SubListNode() {
            this.linkNode = null;
            this.subList = null;
        }
        
        /**
         * Set The SubList this SubListNode is associated with.
         * 
         * @param subList the SubList to associated with this SubListNode
         */
        void setSubList(SubList<E> subList) {
            this.subList = subList;
        }
        
        /**
         * Set the modification count when this SubListNode was last known to be
         * associated with its SubList.
         */
        void updateExpectedModCount() {
            this.expectedModCount = subList().nodableLinkedList().modCount();
        }
        
        /**
         * Return true if the expected modCount matches the NodableLinkedList's
         * modCount.
         * 
         * @return true if the expected modCount matches the NodableLinkedList's
         *         modCount
         */
        boolean isExpectedModCount() {
            return expectedModCount == subList().nodableLinkedList().modCount();
        }
        
        /**
         * Returns true if this SubListNode is still contained by its associated
         * SubList.
         * 
         * @return true if this SubListNode is still contained by its associated SubList
         */
        private boolean isStillNodeOfSubList() {
            if (!isLinked()) return false;
            if (isExpectedModCount()) return true;
            if (!subList().linkedSubNodes().contains(linkNode())) return false;
            updateExpectedModCount();
            return true;
        }        
        
        /**
         * Check if this SubListNode is still contained by its associated sublist and
         * throw an IllegalStateException if not.
         * 
         * @throws IllegalStateException if this SubListNode is no longer contained by
         *                               its associated sublist
         */
        private void checkThisIsStillNodeOfSubList() {
            if (!isStillNodeOfSubList()) {
                throw new IllegalStateException("This SubListNode is no longer a node of its associated sublist");
            }
        }
        
        /**
         * Check if this SubListNode is still contained by its associated sublist
         * and throw an IllegalArgumentException if not.
         * 
         * @throws IllegalStateException if this SubListNode is no longer contained by
         *                               its associated sublist
         */
        private void checkNodeIsStillNodeOfSubList() {
            if (!isStillNodeOfSubList()) {
                throw new IllegalArgumentException("The specified subListNode is no longer a node of its associated sublist");
            }
        }
        
        /**
         * Check if this Node is unlinked (not already linked).
         * 
         * @throws IllegalStateException if this Node is already linked
         */
        private void checkThisIsUnLinked() {
            if (this.isLinked()) {
                throw new IllegalStateException("This node is already a node of a list");
            }
        }
        
        /**
         * Returns the element contained within this {@code SubListNode}.
         * 
         * @return the element contained within this {@code SubListNode}
         */
        @Override
        public E element() {
            return linkNode().element();
        }
        
        /**
         * Returns {@code true} if this {@code SubListNode} and the specified node are
         * equivalent. The nodes are considered equivalent if both represent the same
         * {@code LinkNode} ({@code this.linkNode() == node.linkNode()}).
         * 
         * @param node {@code Node} to be compared for equivalency with this
         *             {@code SubListNode}
         * @return {@code true} if this {@code SubListNode} and the specified node are
         *         equivalent
         */
        @Override
        public boolean isEquivalentTo(Node<?> node) {
            if (node == null) return false;
            if (this.linkNode() == node.linkNode()) return true;
            return false;
        }

        /**
         * Returns {@code true} if this {@code SubListNode} belongs to a list.
         * 
         * @return {@code true} if this {@code SubListNode} belongs to a list
         */
        @Override
        public boolean isLinked() {
            return linkNode().isLinked();
        }

        /**
         * Returns {@code false}.
         * 
         * @return {@code false}
         */
        public boolean isLinkNode() {
            return false;
        }

        /**
         * Returns {@code true}.
         * 
         * @return {@code true}
         */
        public boolean isSubListNode() {
            return true;
        }
        
        /**
         * Returns {@code true} if this {@code SubListNode} is reversed.
         * 
         * @return {@code true} if this {@code SubListNode} is reversed
         */
        @Override
        public boolean isReversed() {
            return false;
        }
        
        /**
         * Returns the {@code NodableLinkedList.LinkNode} witch backs this
         * {@code SubListNode}.
         * <p>
         * To acquire a {@code LinkNode} which matches the forward direction of this
         * {@code SubListNode's} associated sublist, use:
         * 
         * <pre>
         * {@code
         *     NodableLinkedList.LinkNode linkNode = subList().linkedSubNodes().forwardLinkNodeOf(this);
         * }
         * </pre>
         * 
         * @return the {@code NodableLinkedList.LinkNode} which backs this
         *         {@code SubListNode}
         */
        @Override
        public LinkNode<E> linkNode() {
            return this.linkNode;
        }

        /**
         * Returns the {@code LinkedNodes} list this {@code SubListNode} belongs to, or
         * {@code null} if not linked.
         * 
         * @return the {@code LinkedNodes} list this {@code SubListNode} belongs to, or
         *         {@code null} if not linked
         */
        @Override
        public NodableLinkedList<E>.LinkedNodes linkedNodes() {
            return linkNode().linkedNodes();
        }

        /**
         * Returns the {@code NodableLinkedList} this {@code SubListNode} belongs to, or
         * {@code null} if not linked.
         * 
         * @return the {@code NodableLinkedList} this {@code SubListNode} belongs to, or
         *         {@code null} if not linked
         */
        @Override
        public NodableLinkedList<E> nodableLinkedList() {
            return linkNode().nodableLinkedList();
        }        

        /**
         * Returns the {@code NodableLinkedList.SubList} that is associated with this
         * {@code SubListNode}.
         * <p>
         * Note, even if this {@code SubListNode} is unlinked, this {@code SubListNode}
         * is still associated with a {@code NodableLinkedList.SubList}. In other words,
         * a {@code null} value is never returned.
         * 
         * @return the {@code NodableLinkedList.SubList} that is associated with this
         *         {@code SubListNode}
         */
        @Override
        public SubList<E> subList() {
            return this.subList;
        }

        /**
         * Replaces the element of this {@code SubListNode} with the specified element.
         * 
         * @param element element to be stored in this {@code SubListNode}
         * @return the element previously held in this {@code SubListNode}
         */
        @Override
        public E set(E element) {
            return linkNode().set(element);
        }
        
        /**
         * Inserts this {@code SubListNode} after the specified node. This
         * {@code SubListNode} must not already belong to a list, and the specified node
         * must belong to a list.
         * <p>
         * If the specified node is a {@code SubListNode}, it is verified that it is
         * still contained by its associated {@code SubList}. Also, this
         * {@code SubListNode} is effectively inserted into the specified
         * {@code SubListNode's} associated {@code SubList}.
         * <p>
         * If this {@code SubListNode} is reversed, this operation effectively behaves
         * like the {@code addBefore(Node)} method.
         * 
         * @param node the {@code Node} this {@code SubListNode} is to be inserted after
         * 
         * @throws IllegalStateException    if this {@code SubListNode} is already a
         *                                  node of a list
         * @throws IllegalArgumentException if the specified node is {@code null}, not a
         *                                  node of a list, or no longer contained by
         *                                  its associated {@code SubList}
         */
        @Override
        public void addAfter(Node<E> node) {
            checkThisIsUnLinked();
            checkNodeIsLinked(node);
            if (node.isSubListNode()) {
                final SubListNode<E> subListNode = (SubListNode<E>)node;
                subListNode.subList().checkForModificationException();
                subListNode.checkNodeIsStillNodeOfSubList();
                subListNode.subList().linkedSubNodes().iAddNodeAfter(linkNode(), node.linkNode());
                subListNode.updateExpectedModCount();
                this.setSubList(subListNode.subList());
                updateExpectedModCount();
            } else {
                node.linkedNodes().iAddNodeAfter(linkNode(), node.linkNode());
            }
        }
        
        /**
         * Inserts this {@code SubListNode} before the specified node. This
         * {@code SubListNode} must not already belong to a list, and the specified node
         * must belong to a list.
         * <p>
         * If the specified node is a {@code SubListNode}, it is verified that it is
         * still contained by its associated {@code SubList}. Also, this
         * {@code SubListNode} is effectively inserted into the specified
         * {@code SubListNode's} associated {@code SubList}.
         * <p>
         * If this {@code SubListNode} is reversed, this operation effectively behaves
         * like the {@code addAfter(Node)} method.
         * 
         * @param node the {@code Node} this {@code SubListNode} is to be inserted
         *             before
         * @throws IllegalStateException    if this {@code SubListNode} is already a
         *                                  node of a list
         * @throws IllegalArgumentException if the specified node is {@code null}, not a
         *                                  node of a list, or no longer contained by
         *                                  its associated {@code SubList}
         */
        @Override
        public void addBefore(Node<E> node) {
            checkThisIsUnLinked();
            checkNodeIsLinked(node);
            if (node.isSubListNode()) {
                final SubListNode<E> subListNode = (SubListNode<E>)node;
                subListNode.subList().checkForModificationException();
                subListNode.checkNodeIsStillNodeOfSubList();
                subListNode.subList().linkedSubNodes().iAddNodeBefore(linkNode(), node.linkNode());
                subListNode.updateExpectedModCount();
                this.setSubList(subListNode.subList());
                updateExpectedModCount();
            } else {
                node.linkedNodes().iAddNodeBefore(linkNode(), node.linkNode());
            }
        }

        /**
         * Returns {@code true} if there exists a node which comes after this
         * {@code SubListNode} in a {@code SubList}. In other words, returns
         * {@code true} if this {@code SubListNode} is not the last node of a
         * {@code SubList}.
         * <p>
         * This {@code SubListNode} is verified that it is still contained by its
         * associated {@code SubList}.
         * <p>
         * If this {@code SubListNode} is reversed, this operation effectively behaves
         * like the {@code hasPrevious()} method.
         * 
         * @return {@code true} if there exists a node which comes after this
         *         {@code SubListNode}
         * @throws IllegalStateException if this {@code SubListNode} is no longer
         *                               contained of its sublist
         */
        @Override
        public boolean hasNext() {
            subList().checkForModificationException();
            checkThisIsStillNodeOfSubList();
            return subList().linkedSubNodes().iHasNodeAfter(linkNode());
        }

        /**
         * Returns {@code true} if there exists a node which comes before this
         * {@code SubListNode} in a {@code SubList}. In other words, returns
         * {@code true} if this {@code SubListNode} is not the first node of a
         * {@code SubList}.
         * <p>
         * This {@code SubListNode} is verified that it is still contained by its
         * associated {@code SubList}.
         * <p>
         * If this {@code SubListNode} is reversed, this operation effectively behaves
         * like the {@code hasNext()} method.
         * 
         * @return {@code true} if there exists a node which comes before this
         *         {@code SubListNode}
         * @throws IllegalStateException if this {@code SubListNode} is no longer
         *                               contained of its sublist
         */
        @Override
        public boolean hasPrevious() {
            subList().checkForModificationException();
            checkThisIsStillNodeOfSubList();
            return subList().linkedSubNodes().iHasNodeBefore(linkNode());
        }

        /**
         * Returns the index of this {@code SubListNode} in its {@code SubList}, or -1
         * if this {@code SubListNode} does not belong to a {@code SubList} or the
         * {@code index > Integer.MAX_VALUE}.
         * <p>
         * <b>Performance Consideration:</b> This operation is performed in linear time.
         *
         * @return the index of this {@code SubListNode} in its {@code SubList}, or -1
         *         if this {@code SubListNode} does not belong to a {@code SubList}, or
         *         the {@code index > Integer.MAX_VALUE}.
         */
        @Override
        public int index() {
            subList().checkForModificationException();
            final long index = subList().linkedSubNodes().iGetIndex(linkNode());
            return (index > Integer.MAX_VALUE) ? -1 : (int)index;
        }

        /**
         * Returns the {@code SubListNode} which comes after this {@code SubListNode} in
         * a {@code SubList}. if this {@code SubListNode} is the last or only node,
         * {@code null} is returned.
         * <p>
         * This {@code SubListNode} is verified that it is still contained by its
         * associated {@code SubList}.
         * <p>
         * If this is a reversed {@code SubListNode}, this operation effectively behaves
         * like the {@code previous()} method, and the returned {@code SubListNode} will
         * also be reversed.
         * 
         * @return the {@code SubListNode} which comes after this {@code SubListNode} in
         *         a {@code SubList}, or {@code null} if this {@code SubListNode} is the
         *         last or only node
         * @throws IllegalStateException if this {@code SubListNode} is no longer
         *                               contained by its associated sublist
         */
        @Override
        public SubListNode<E> next() {
            subList().checkForModificationException();
            checkThisIsStillNodeOfSubList();
            final LinkNode<E> node = subList().linkedSubNodes().iGetNodeAfter(linkNode());
            if (node == null) return null;
            final SubListNode<E> subListNode = new SubListNode<E>(node, subList());
            return subListNode;
        }

        /**
         * Returns the {@code SubListNode} which comes before this {@code SubListNode}
         * in a {@code SubList}. if this {@code SubListNode} is the first or only node,
         * {@code null} is returned.
         * <p>
         * This {@code SubListNode} is verified that it is still contained by its
         * associated {@code SubList}.
         * <p>
         * If this is a reversed {@code SubListNode}, this operation effectively behaves
         * like the {@code next()} method, and the returned {@code SubListNode} will
         * also be reversed.
         * 
         * @return the {@code SubListNode} which comes before this {@code SubListNode}
         *         in a {@code SubList}, or {@code null} if this {@code SubListNode} is
         *         the first or only node
         * @throws IllegalStateException if this {@code SubListNode} is no longer
         *                               contained by its associated sublist
         */
        @Override
        public SubListNode<E> previous() {
            subList().checkForModificationException();
            checkThisIsStillNodeOfSubList();
            final LinkNode<E> node = subList().linkedSubNodes().iGetNodeBefore(linkNode());
            if (node == null) return null;
            final SubListNode<E> subListNode = new SubListNode<E>(node, subList());
            return subListNode;
        }

        /**
         * Removes this {@code SubListNode} from the {@code SubList} which contains this
         * {@code SubListNode}.
         * <p>
         * This {@code SubListNode} is verified that it is still contained by its
         * associated {@code SubList}.
         * 
         * @throws IllegalStateException if this {@code SubListNode} is no longer
         *                               contained by its sublist
         */
        @Override
        public void remove() {
            subList().checkForModificationException();
            checkThisIsStillNodeOfSubList();
            subList().linkedSubNodes().iRemoveNode(linkNode());
        }

        /**
         * Replaces this {@code SubListNode} with the specified node. This
         * {@code SubListNode} must belong to a {@code SubList}, and the specified node
         * must not belong to a list.
         * <p>
         * If the specified node is a {@code SubListNode}, after this operation is
         * completed, The specified {@code SubListNode} will be associated with this
         * {@code SubListNode's} {@code SubList}.
         * 
         * @param node {@code Node} to replace this {@code SubListNode}
         * @throws IllegalStateException    if this {@code SubListNode} is no longer
         *                                  contained by its associated sublist
         * @throws IllegalArgumentException if the specified node is {@code null} or
         *                                  already a node of a list
         */
        @Override
        public void replaceWith(Node<E> node) {
            subList().checkForModificationException();
            checkThisIsStillNodeOfSubList();
            checkNodeIsUnLinked(node);
            subList().linkedSubNodes().iReplaceNode(linkNode(), node.linkNode());
            if (node.isSubListNode()) {
                final SubListNode<E> subListNode = (SubListNode<E>)node;
                subListNode.setSubList(this.subList());
                subListNode.updateExpectedModCount();
            }
        }
        
        /**
         * Returns a {@code SubListNode} that can be used to traverse its associated
         * {@code SubList} in the reverse direction than this {@code SubListNode}.
         * Order-sensitive operations like {@code addAfter}, {@code addBefore},
         * {@code hasNext}, {@code hasPrevious}, {@code next}, and {@code previous} also
         * operate in a reverse way.
         * <P>
         * Note, a {@code SubListNode} can be used to naturally traverse its associated
         * {@code SubList} in a forward direction (from the sublist's first {@code Node}
         * to the sublist's last {@code Node} when making successive calls to the
         * {@code next()} method), therefore, a {@code SubListNode} typically doesn't
         * need to be reversed like a {@code LinkNode} which always traverses any list
         * in the same direction as the base {@code NodableLinkedList}.
         * 
         * @return a {@code SubListNode} that can be used to traverse its associated
         *         {@code SubList} in the reverse direction than this
         *         {@code SubListNode}
         */
        @Override
        public SubListNode<E> reversed() {
            return new ReverseSubListNode<E>(this);
        }        

        /**
         * Swaps this {@code SubListNode} with the specified node. Both this
         * {@code SubListNode} and the specified node must belong to a list, but they
         * can be different lists. This {@code SubListNode} is verified that it is still
         * contained by its associated {@code SubList}.
         * <p>
         * If the specified node is a {@code SubListNode}, it is verified that it is
         * still contained by its associated {@code SubList}. Also, after this operation
         * is completed, both this {@code SubListNode} and the specified
         * {@code SubListNode} will be associated with the other's {@code SubList}. If
         * the specified node is a {@code LinkNode}, it is effectively inserted into
         * this {@code SubListNode's} associated {@code SubList}.
         * <p>
         * <strong>Synchronization consideration:</strong> This operation can
         * potentially operate on two different lists. if synchronization is required,
         * both lists should be synchronized by the same object.
         * 
         * @param node the {@code Node} to swap with this {@code SubListNode}
         * @throws IllegalStateException    if this {@code SubListNode} is no longer
         *                                  contained by its associated sublist
         * @throws IllegalArgumentException if the specified node is {@code null}, not
         *                                  linked, or no longer contained by its
         *                                  associated sublist
         */
        @Override
        public void swapWith(Node<E> node) {
            subList().checkForModificationException();
            checkThisIsStillNodeOfSubList();
            checkNodeIsLinked(node);
            if (node.isSubListNode()) {
                final SubListNode<E> subListNode = (SubListNode<E>)node;
                final SubList<E> thatSubList = subListNode.subList();
                thatSubList.checkForModificationException();
                subListNode.checkNodeIsStillNodeOfSubList();
                LinkNode.swapNodes(this.linkNode(), subListNode.linkNode());
                if (this.subList() == thatSubList) {
                    // sublist nodes are in the same sublist
                    this.subList().linkedSubNodes().updateSizeAndModCount(0L);
                } else {
                    // sublist nodes are in different sublists
                    this.subList().linkedSubNodes().swappedNodes(this.linkNode(), subListNode.linkNode());
                    thatSubList.linkedSubNodes().swappedNodes(subListNode.linkNode(), this.linkNode());
                    subListNode.setSubList(this.subList());
                    this.setSubList(thatSubList);
                }
                this.updateExpectedModCount();
                subListNode.updateExpectedModCount();
            } else {
                LinkNode.swapNodes(this.linkNode(), node.linkNode());
                subList().linkedSubNodes().swappedNodes(linkNode(), node.linkNode());
            }
        }
        
        /**
         * Returns a new {@code SubListNode}, backed by this {@code SubListNode's}
         * {@code LinkNode}, for the specified subList. The {@code LinkNode} which backs
         * the returned {@code SubListNode}, must be a node of the specified subList, or
         * unlinked.
         * <p>
         * <b>Performance Consideration:</b> This {@code SubListNode's}
         * {@code LinkNode}, if linked, is verified, in linear time, that it is a node
         * of the specified subList.
         * 
         * @param subList {@code SubList} containing this {@code SubListNode's}
         *                {@code LinkNode}
         * @return a new {@code SubListNode}, backed by this {@code SubListNode's}
         *         {@code LinkNode}, for the specified subList
         * @throws IllegalArgumentException if the specified subList is {@code null}
         * @throws IllegalStateException    if this {@code SubListNode's}
         *                                  {@code LinkNode} is not a node of the
         *                                  specified subList
         */
        @Override
        public SubListNode<E> subListNode(SubList<E> subList) {
            if (subList == null) throw new IllegalArgumentException("Specified SubList is null");
            subList.checkForModificationException();
            if (this.isLinked() && !subList.linkedSubNodes().contains(this.linkNode())) {
                throw new IllegalStateException("This SubListNode's LinkNode is not a node of the specified subList");
            }
            return new SubListNode<E>(this.linkNode(), subList);
        }

        /**
         * Compares this {@code SubListNode} with the specified object ({@code Node})
         * for equality. Returns {@code true} if and only if the specified object is a
         * {@code Node}, and both pairs of elements in the two nodes are <i>equal</i>.
         * (Two elements {@code e1} and {@code e2} are <i>equal</i> if
         * {@code (e1==null ? e2==null : e1.equals(e2))}.)
         *
         * @param object {@code Object} ({@code Node}) to be compared for equality with
         *               this {@code SubListNode}
         * @return {@code true} if this {@code SubListNode} is equal to the specified
         *         object ({@code Node})
         */
        @Override
        public boolean equals(Object object) {
             return linkNode().equals(object);
        }
        
        /**
         * Compares this {@code SubListNode} to the specified node for order. Returns a
         * negative integer, zero, or a positive integer as this {@code SubListNode's}
         * element is less than, equal to, or greater than the specified node's element.
         *
         * @param node {@code Node} to be compared to this {@code SubListNode}
         * @return a negative integer, zero, or a positive integer as this
         *         {@code SubListNode's} element is less than, equal to, or greater than
         *         the specified node's element
         * @throws NullPointerException if the specified node is {@code null}
         * @throws ClassCastException   if the nodes' element types prevent them from
         *                              being compared
         */
        @Override
        public int compareTo(Node<E> node) {
            return linkNode().compareTo(node);
        }

        /**
         * Returns the hash code value of this {@code SubListNode}. Has the same hash
         * code as its backing {@code LinkNode}.
         *
         * @return the hash code value of this {@code SubListNode}
         */
        @Override
        public int hashCode() {
            return linkNode().hashCode();
        }
        
    } // SubListNode
    
    private static class ReverseSubListNode<E> extends SubListNode<E> {
       
       /**
        * The SubListNode that was reversed.
        */
       private final SubListNode<E> subListNode;
       
       /**
        * Construct a ReverseSubListNode.
        * 
        * @param subListNode the SubListNode that was reversed
        */
       private ReverseSubListNode(SubListNode<E> subListNode) {
           super();
           this.subListNode = subListNode;
       }
       
       @Override
       public SubList<E> subList() {
           return subListNode.subList();
       }
       
       @Override
       void setSubList(SubList<E> subList) {
           subListNode.setSubList(subList);
       }
       
       @Override
       void updateExpectedModCount() {
           subListNode.updateExpectedModCount();
       }
       
       @Override
       boolean isExpectedModCount() {
           return subListNode.isExpectedModCount();
       }

       @Override
       public boolean isReversed() {
           return true;
       }       
       
       @Override
       public LinkNode<E> linkNode() {
           return subListNode.linkNode();
       }
       
       @Override
       public void addAfter(Node<E> node) {
           subListNode.addBefore(node);
       }
       
       @Override
       public void addBefore(Node<E> node) {
           subListNode.addAfter(node);
       }
       
       @Override
       public boolean hasNext() {
           return subListNode.hasPrevious();
       }
       
       @Override
       public boolean hasPrevious() {
           return subListNode.hasNext();
       }
       
       @Override
       public SubListNode<E> next() {
           final SubListNode<E> nextNode = subListNode.previous();
           return (nextNode == null) ? null : nextNode.reversed();
       }
       
       @Override
       public SubListNode<E> previous() {
           final SubListNode<E> previousNode = subListNode.next();
           return (previousNode == null) ? null : previousNode.reversed();
       }
       
       @Override
       public SubListNode<E> reversed() {
           return subListNode;
       }
       
   } // ReverseSubListNode    

   /**
    * Node of a {@link NodableLinkedList}. Contains references to the previous and
    * next {@code LinkNodes} in a doubly-linked list, and contains an element which
    * can be {@code null}. Does not belong to any particular
    * {@link NodableLinkedList} until the {@code LinkNode} is inserted/added. Once
    * inserted, the {@code LinkNode} remains linked to a {@code NodableLinkedList}
    * until removed. A {@code LinkNode} can belong to different lists, just not at
    * the same time.
    * <P>
    * A {@code LinkNode} can only be used to traverse the base (initially created)
    * {@code NodableLinkedList} in a forward direction (from the list's first node
    * to the list's last node when making successive calls to the {@code next()}
    * method). A reversed {@code LinkNode} can be created via the
    * {@code reversed()} method, which can be used to traverse a list in the
    * opposite direction. For instance, a reversed {@code LinkNode} can be used to
    * traverse a non-reversed list from the list's last node to the list's first
    * node when making successive calls to the {@code next()} method or it can be
    * used to traverse a reversed list from the list's first node to the list's
    * last node when making successive calls to the {@code next()} method. The
    * reverse ordering affects all order-sensitive operations
    * ({@code addAfter(Node)}, {@code addBefore(Node)}, etc.). List operations like
    * {@code NodableLinkedList.getFirstNode()},
    * {@code NodableLinkedList.getLastNode()}, {@code LinkedNodes.element()},
    * {@code LinkedNodes.get(index)}, {@code LinkedNodes.getFirst()},
    * {@code LinkedNodes.getLast()}, {@code LinkedNodes.peek()},
    * {@code LinkedNodes.peekFirst()}, {@code LinkedNodes.peekLast()}, and
    * {@code SubLinkedNodes.get(index)} will return a reversed {@code LinkNode} if
    * the target list is reversed. Also methods {@code next()} and
    * {@code previous()} always return a {@code LinkNode} which has the same
    * traversal direction as the target {@code Node}.
    * <p>
    * All operations, except {@code index} and {@code subListNode}, are performed
    * in constant time.
    * 
    * @serialData Only the element is serialized. the references to the next and
    *             previous nodes in a list, are not serialized. When deserialized,
    *             the node will not be linked to any list.
    * 
    * @author James Pfeifer
    * @param <E> the type of element held in this {@code LinkNode}
    */
    public static class LinkNode<E>
        implements Node<E>, Serializable, Cloneable, Comparable<Node<E>> {

        private static final long serialVersionUID = 8645137994022899638L;

        /**
         * The element contained within this LinkNode.
         */
        private E element;

        /**
         * The LinkNode which comes after this LinkNode in a list.
         */
        private transient LinkNode<E> next = null;
        
        /**
         * The LinkNode which comes before this LinkNode in a list.
         */
        private transient LinkNode<E> previous = null;

        /**
         * The LinkedNodes which contains this LinkNode, or null if this LinkNode is not
         * linked.
         */
        private transient NodableLinkedList<E>.LinkedNodes linkedNodes = null;

        /**
         * Constructs a {@code LinkNode} containing a null element.
         */
        public LinkNode() {
            this.element = null;
        }

        /**
         * Constructs a {@code LinkNode} containing the specified element.
         * 
         * @param element element to be stored in this {@code LinkNode}
         */
        public LinkNode(E element) {
            this.element = element;
        }

        /**
         * Returns a shallow copy of this {@code LinkNode}. The clone will have the same
         * element as this {@code LinkNode}, but will not be linked to any list.
         *
         * @return a shallow copy of this {@code LinkNode}
         */
        @Override
        public LinkNode<E> clone() {
            try {
                @SuppressWarnings("unchecked")
                final LinkNode<E> clone = (LinkNode<E>)super.clone();
                clone.next = null;
                clone.previous = null;
                clone.linkedNodes = null;
                return clone;               
            } catch (CloneNotSupportedException e) {
                throw new AssertionError("Not Cloneable? "+e.getMessage(), e);
            }
        }
        /**
         * Check if this Node is linked.
         * 
         * @throws IllegalStateException if this Node is not linked
         */
        private void checkThisNodeIsLinked() {
            if (!this.isLinked()) {
                throw new IllegalStateException("This node is not a node of a list");
            }
        }
        
        /**
         * Check if this Node is unlinked (not already linked).
         * 
         * @throws IllegalStateException if this Node is already linked
         */
        private void checkThisNodeIsUnLinked() {
            if (this.isLinked()) {
                throw new IllegalStateException("This node is already a node of a list");
            }
        }
        
        /**
         * Set the LinkedNodes this LinkNode belongs to.
         * 
         * @param linkedNodes the LinkedNodes this LinkNode belongs to
         */
        void iSetLinkedNodes(NodableLinkedList<E>.LinkedNodes linkedNodes) {
            this.linkedNodes = linkedNodes;
        }
        
        /**
         * Returns the 'next' field value.
         * 
         * @return the 'next' field value
         */
        LinkNode<E> iGetNext() {
            return this.next;
        }
        
        /**
         * Returns the 'previous' field value.
         * 
         * @return the 'previous' field value.
         */
        LinkNode<E> iGetPrevious() {
            return this.previous;
        }
        
        /**
         * Sets the 'next' field to the specified value.
         * 
         * @param next the new value of the 'next' field
         */
        void iSetNext(LinkNode<E> next) {
            this.next = next;
        }
        
        /**
         * Sets the 'previous' field to the specified value.
         * 
         * @param previous the new value of the 'previous' field
         */
        void iSetPrevious(LinkNode<E> previous) {
            this.previous = previous;
        }

        /**
         * Returns {@code null}; {@code LinkNodes} are never part of a {@code SubList}.
         * 
         * @return {@code null}
         */
        @Override
        public SubList<E> subList() {
            return null;
        }        

        /**
         * Returns the element contained within this {@code LinkNode}.
         * 
         * @return the element contained within this {@code LinkNode}
         */
        @Override
        public E element() {
            return this.element;
        }
        
        /**
         * Returns {@code true} if this {@code LinkNode} and the specified node are
         * equivalent. The nodes are considered equivalent if both represent the same
         * {@code LinkNode} ({@code this.linkNode() == node.linkNode()}).
         * 
         * @param node {@code Node} to be compared for equivalency with this
         *             {@code LInkNode}
         * @return {@code true} if this {@code LinkNode} and the specified node are
         *         equivalent
         */
        @Override
        public boolean isEquivalentTo(Node<?> node) {
            if (node == null) return false;
            if (this.linkNode() == node.linkNode()) return true;
            return false;
        }

        /**
         * Returns {@code true} if this {@code LinkNode} belongs to a list.
         * 
         * @return {@code true} if this {@code LinkNode} belongs to a list
         */
        @Override
        public boolean isLinked() {
            return linkedNodes() != null;
        }

        /**
         * Returns {@code true}.
         * 
         * @return {@code true}
         */
        @Override
        public boolean isLinkNode() {
            return true;
        }

        /**
         * Returns {@code false}.
         * 
         * @return {@code false}
         */
        @Override
        public boolean isSubListNode() {
            return false;
        }
        
        /**
         * Returns {@code true} if this {@code LinkNode} is reversed.
         * 
         * @return {@code true} if this {@code LinkNode} is reversed
         */
        @Override
        public boolean isReversed() {
            return false;
        }        

        /**
         * Returns this {@code LinkNode} or the backing {@code LinkNode} if this is a
         * reversed {@code LinkNode}.
         * 
         * @return this {@code LinkNode} or the backing {@code LinkNode} if this is a
         *         reversed {@code LinkNode}
         */
        @Override
        public LinkNode<E> linkNode() {
            return this;
        }

        /**
         * Returns the {@code LinkedNodes} list this {@code LinkNode} belongs to, or
         * {@code null} if not linked.
         * 
         * @return the {@code LinkedNodes} list this {@code LinkNode} belongs to, or
         *         {@code null} if not linked
         */
        @Override
        public NodableLinkedList<E>.LinkedNodes linkedNodes() {
            return this.linkedNodes;
        }

        /**
         * Returns the {@code NodableLinkedList} this {@code LinkNode} belongs to, or
         * {@code null} if not linked.
         * 
         * @return the {@code NodableLinkedList} this {@code LinkNode} belongs to, or
         *         {@code null} if not linked
         */
        @Override
        public NodableLinkedList<E> nodableLinkedList() {
            return (this.isLinked())
                   ? linkedNodes().nodableLinkedList()
                   : null;
        }	

        /**
         * Replaces the element of this {@code LinkNode} with the specified element.
         * 
         * @param element element to be stored in this {@code LinkNode}
         * @return the element previously held in this {@code LinkNode}
         */
        @Override
        public E set(E element) {
            final E originalElement = this.element;
            this.element = element;
            return originalElement;
        }

        /**
         * Inserts this {@code LinkNode} after the specified node. This {@code LinkNode}
         * must not already belong to a list, and the specified node must belong to a
         * list.
         * <p>
         * If the specified node is a {@code SubListNode}, it is verified that it is
         * still contained by its associated {@code SubList}. Also, this
         * {@code LinkNode} is effectively inserted into the specified
         * {@code SubListNode's} associated {@code SubList}.
         * <p>
         * If this {@code LinkNode} is reversed, this operation effectively behaves like
         * the {@code addBefore(Node)} method.
         * 
         * @param node the {@code Node} this {@code LinkNode} is to be inserted after
         * @throws IllegalStateException    if this {@code LinkNode} is already a node
         *                                  of a list
         * @throws IllegalArgumentException if the specified node is {@code null}, not a
         *                                  node of a list, or no longer contained by
         *                                  its associated sublist
         */
        @Override
        public void addAfter(Node<E> node) {
            checkThisNodeIsUnLinked();
            checkNodeIsLinked(node);
            if (node.isSubListNode()) {
                final SubListNode<E> subListNode = (SubListNode<E>) node;
                subListNode.subList().checkForModificationException();
                subListNode.checkNodeIsStillNodeOfSubList();
                subListNode.subList().linkedSubNodes().iAddNodeAfter(this, subListNode.linkNode());
                subListNode.updateExpectedModCount();
            } else {
                node.linkedNodes().iAddNodeAfter(this, node.linkNode());
            }

        }

        /**
         * Inserts this {@code LinkNode} before the specified node. This
         * {@code LinkNode} must not already belong to a list, and the specified node
         * must belong to a list.
         * <p>
         * If the specified node is a {@code SubListNode}, it is verified that it is
         * still contained by its associated {@code SubList}. Also, this
         * {@code LinkNode} is effectively inserted into the specified
         * {@code SubListNode's} associated {@code SubList}.
         * <p>
         * If this {@code LinkNode} is reversed, this operation effectively behaves like
         * the {@code addAfter(Node)} method.
         * 
         * @param node the {@code Node} this {@code LinkNode} is to be inserted before
         * @throws IllegalStateException    if this {@code LinkNode} is already a node
         *                                  of a list
         * @throws IllegalArgumentException if the specified node is {@code null}, not a
         *                                  node of a list, or no longer contained by
         *                                  its associated sublist
         */
        @Override
        public void addBefore(Node<E> node) {
            checkThisNodeIsUnLinked();
            checkNodeIsLinked(node);
            if (node.isSubListNode()) {
                final SubListNode<E> subListNode = (SubListNode<E>) node;
                subListNode.subList().checkForModificationException();
                subListNode.checkNodeIsStillNodeOfSubList();
                subListNode.subList().linkedSubNodes().iAddNodeBefore(this, subListNode.linkNode());
                subListNode.updateExpectedModCount();
            } else {
                node.linkedNodes().iAddNodeBefore(this, node.linkNode());
            }
        }
        
        /**
         * Returns {@code true} if there exists a {@code Node} which comes after this
         * {@code LinkNode} in a list. In other words, returns {@code true} if this
         * {@code LinkNode} is not the last node of a list.
         * <p>
         * If this {@code LinkNode} is reversed, this operation effectively behaves like
         * the {@code hasPrevious()} method.
         * 
         * @return {@code true} if there exists a {@code Node} which comes after this
         *         {@code LinkNode}
         * @throws IllegalStateException if this {@code LinkNode} is not linked
         */
        @Override
        public boolean hasNext() {
            checkThisNodeIsLinked();
            return linkedNodes().iHasNodeAfter(this);
        }

        /**
         * Returns {@code true} if there exists a {@code Node} which comes before this
         * {@code LinkNode} in a list. In other words, returns {@code true} if this
         * {@code LinkNode} is not the first node of a list.
         * <p>
         * If this {@code LinkNode} is reversed, this operation effectively behaves like
         * the {@code hasNext()} method.
         * 
         * @return {@code true} if there exists a {@code Node} which comes before this
         *         {@code LinkNode}
         * @throws IllegalStateException if this {@code Node} is not linked
         */
        @Override
        public boolean hasPrevious() {
            checkThisNodeIsLinked();
            return linkedNodes().iHasNodeBefore(this);
        }

        /**
         * Returns the index of this {@code LinkNode} in a list, or -1 if this
         * {@code LinkNode} does not belong to a list or the
         * {@code index > Integer.MAX_VALUE}.
         * <p>
         * <b>Performance Consideration:</b> This operation is performed in linear time.
         *
         * @return the index of this {@code LinkNode} in a list, or -1 if this
         *         {@code LinkNode} does not belong to a list or the
         *         {@code index > Integer.MAX_VALUE}
         */
        @Override
        public int index() {
            if (!this.isLinked()) return -1;
            final long index = linkedNodes().iGetIndex(this);
            return (index > Integer.MAX_VALUE) ? -1 : (int)index;
        }

        /**
         * Returns the {@code LinkNode} which comes after this {@code LinkNode} in a
         * list. if this {@code LinkNode} is the last or only {@code LinkNode},
         * {@code null} is returned.
         * <p>
         * If this is a reversed {@code LinkNode}, this operation effectively behaves
         * like the {@code previous()} method, and the returned {@code LinkNode} will
         * also be reversed.
         * 
         * @return the {@code LinkNode} which comes after this {@code LinkNode} in a
         *         list, or {@code null} if this {@code LinkNode} is the last or only
         *         {@code LinkNode}
         * @throws IllegalStateException if this {@code LinkNode} is not linked
         */
        @Override
        public LinkNode<E> next() {
            checkThisNodeIsLinked();
            final LinkNode<E> linkNode = linkedNodes().iGetNodeAfter(this);
            return linkNode;
        }

        /**
         * Returns the {@code LinkNode} which comes before this {@code LinkNode} in a
         * list. if this {@code LinkNode} is the first or only {@code LinkNode},
         * {@code null} is returned.
         * <p>
         * If this is a reversed {@code LinkNode}, this operation effectively behaves
         * like the {@code next()} method, and the returned {@code LinkNode} will also
         * be reversed.
         * 
         * @return the {@code LinkNode} which comes before this {@code LinkNode} in a
         *         list, or {@code null} if this {@code LinkNode} is the first or only
         *         {@code LinkNode}
         * @throws IllegalStateException if this {@code LinkNode} is not linked
         */
        @Override
        public LinkNode<E> previous() {
            checkThisNodeIsLinked();
            final LinkNode<E> linkNode = linkedNodes().iGetNodeBefore(this);
            return linkNode;
        }

        /**
         * Removes this {@code LinkNode} from the list which contains this {@code LinkNode}.
         * 
         * @throws IllegalStateException if this {@code LinkNode} is not linked
         */
        @Override
        public void remove() {
            checkThisNodeIsLinked();
            linkedNodes().iRemoveNode(this);
        }

        /**
         * Replaces this {@code LinkNode} with the specified node. This {@code LinkNode}
         * must belong to a list, and the specified node must not already belong to a
         * list.
         * 
         * @param node {@code Node} to replace this {@code LinkNode}
         * @throws IllegalStateException    if this {@code LinkNode} is not linked
         * @throws IllegalArgumentException if the specified node is {@code null} or
         *                                  already a node of a list
         */
        @Override
        public void replaceWith(Node<E> node) {
            checkThisNodeIsLinked();
            checkNodeIsUnLinked(node);
            linkedNodes().iReplaceNode(this, node.linkNode());			
        }
        
        /**
         * Returns a {@code LinkNode} that can be used to traverse the list it is linked
         * to, in the reverse direction than this {@code LinkNode}. Order-sensitive
         * operations like {@code addAfter}, {@code addBefore}, {@code hasNext},
         * {@code hasPrevious}, {@code next}, and {@code previous} also operate in a
         * reverse way.
         * <p>
         * A non-reversed {@code LinkNode} always traverses the base
         * {@code NodableLinkedList} in a forward direction (from the list's first node
         * to the list's last node when making successive calls to the {@code next()}
         * method). A reversed {@code LinkNode} traverses the list in a reverse
         * direction (from the list's last node to the list's first node when making
         * successive calls to the {@code next()} method). Method {@code isReversed()}
         * can be called to determine if a {@code LinkNode} is reversed or non-reversed.
         * 
         * @return a {@code LinkNode} that can be used to traverse the list it is linked
         *         to, in the reverse direction than this {@code LinkNode}
         */
        @Override
        public LinkNode<E> reversed() {
            return new ReverseLinkNode<E>(this);
        }

        /**
         * Swaps this {@code LinkNode} with the specified node. Both this
         * {@code LinkNode} and the specified node must belong to a list, but they can
         * be different lists.
         * <p>
         * If the specified node is a {@code SubListNode}, it is verified that it is
         * still contained by its associated {@code SubList}. Also, this
         * {@code LinkNode} is effectively inserted into the specified
         * {@code SubListNode's} associated {@code SubList}.
         * <p>
         * <strong>Synchronization consideration:</strong> This operation can
         * potentially operate on two different lists. if synchronization is required,
         * both lists should be synchronized by the same object.
         * 
         * @param node the {@code Node} to swap with this {@code LinkNode}
         * @throws IllegalStateException    if this {@code LinkNode} is not linked
         * @throws IllegalArgumentException if the specified node is {@code null}, not
         *                                  linked, or no longer contained by its
         *                                  associated sublist
         */
        @Override
        public void swapWith(Node<E> node) {
            checkThisNodeIsLinked();
            checkNodeIsLinked(node);
            if (node.isSubListNode()) {
                final SubListNode<E> subListNode = (SubListNode<E>)node;
                final SubList<E> thatSubList = subListNode.subList();
                thatSubList.checkForModificationException();
                subListNode.checkNodeIsStillNodeOfSubList();
                swapNodes(this, node.linkNode());
                thatSubList.linkedSubNodes().swappedNodes(subListNode.linkNode(), this);
            } else {
                swapNodes(this, node.linkNode());
            }
        }

        /**
         * Swap the specified LinkNodes. The specified LinkNodes swap positions in their
         * respective lists.
         * 
         * @param <T>   the specified LinkNodes' element type
         * @param node1 the LinkNode to swap with node2
         * @param node2 the LinkNode to swap with node1
         */
        private static <T> void swapNodes(LinkNode<T> node1, LinkNode<T> node2) {
            //assert isLinkedNode(node1) : "node1 is null or not a node of a list";
            //assert isLinkedNode(node2) : "node2 is null or not a node of a list";
            if (node1 == node2) return;
            node1.linkedNodes().incrementModCount();
            if (node1.linkedNodes() != node2.linkedNodes()) {
                // nodes are linked in different lists
                node2.linkedNodes().incrementModCount();
                LinkNode<T> tempNode;
                NodableLinkedList<T>.LinkedNodes tempLinkedNodes;
                // swap lists
                tempLinkedNodes = node1.linkedNodes();
                node1.iSetLinkedNodes(node2.linkedNodes());
                node2.iSetLinkedNodes(tempLinkedNodes);
                // swap next references
                tempNode = node1.iGetNext();
                node1.iSetNext(node2.iGetNext());
                node2.iSetNext(tempNode);
                // swap previous references
                tempNode = node1.iGetPrevious();
                node1.iSetPrevious(node2.iGetPrevious());
                node2.iSetPrevious(tempNode);
            } else {
                // nodes are linked in the same list
                if (node1.iGetNext() == node2) {
                    // node1 comes right before node2 in the list
                    node1.iSetNext(node2.iGetNext()); node2.iSetNext(node1);
                    node2.iSetPrevious(node1.iGetPrevious()); node1.iSetPrevious(node2);
                } else if (node1.iGetPrevious() == node2) {
                    // node1 comes right after node2 in the list
                    node2.iSetNext(node1.iGetNext()); node1.iSetNext(node2);
                    node1.iSetPrevious(node2.iGetPrevious()); node2.iSetPrevious(node1);
                } else {
                    LinkNode<T> tempNode;
                    // swap next references
                    tempNode = node1.iGetNext();
                    node1.iSetNext(node2.iGetNext());
                    node2.iSetNext(tempNode);
                    // swap previous references
                    tempNode = node1.iGetPrevious();
                    node1.iSetPrevious(node2.iGetPrevious());
                    node2.iSetPrevious(tempNode);
                }
            }
            // update the node's neighbors
            node1.iGetPrevious().iSetNext(node1);
            node1.iGetNext().iSetPrevious(node1);
            node2.iGetPrevious().iSetNext(node2);
            node2.iGetNext().iSetPrevious(node2);
        }        

        /**
         * Returns a {@code SubListNode}, backed by this {@code LinkNode}, for the
         * specified subList. The returned {@code SubListNode} is backed by this
         * {@code LinkNode} which must be a node of the specified subList, or unlinked.
         * <p>
         * <b>Performance Consideration:</b> This {@code LinkNode}, if linked, is
         * verified, in linear time, that it is a node of the specified subList.
         * 
         * @param subList {@code SubList} containing this {@code LinkNode}
         * @return a {@code SubListNode}, backed by this {@code LinkNode}, for the
         *         specified subList
         * @throws IllegalArgumentException if the specified subList is {@code null}
         * @throws IllegalStateException    if this {@code LinkNode} is not a node of
         *                                  the specified subList
         */
        @Override
        public SubListNode<E> subListNode(SubList<E> subList) {
            if (subList == null) throw new IllegalArgumentException("Specified SubList is null");
            subList.checkForModificationException();
            if (this.isLinked() && !subList.linkedSubNodes().contains(this)) {
                throw new IllegalStateException("This node is not a node of the specified subList");
            }
            return new SubListNode<E>(this, subList);
        }
        
        /**
         * Compares this {@code LinkNode} with the specified object ({@code Node}) for
         * equality. Returns {@code true} if and only if the specified object is also a
         * {@code Node}, and both pairs of elements in the two nodes are <i>equal</i>.
         * (Two elements {@code e1} and {@code e2} are <i>equal</i> if
         * {@code (e1==null ? e2==null : e1.equals(e2))}.)
         *
         * @param object {@code Object} ({@code Node}) to be compared for equality with
         *               this {@code LinkNode}
         * @return {@code true} if the specified object ({@code Node}) is equal to this
         *         {@code LinkNode}
         */
        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (!(object instanceof Node)) return false;
            final Object thatElement = ((Node<?>)object).element();
            final Object thisElement = this.element();
            return (thisElement == null)
                   ? thatElement == null
                   : thisElement.equals(thatElement);
        }

        /**
         * Compares this {@code LinkNode} to the specified node for order. Returns a
         * negative integer, zero, or a positive integer as this {@code LinkNode's}
         * element is less than, equal to, or greater than the specified node's element.
         *
         * @param node {@code Node} to be compared to this {@code LinkNode}
         * @return a negative integer, zero, or a positive integer as this
         *         {@code LinkNode's} element is less than, equal to, or greater than
         *         the specified node's element
         * @throws NullPointerException if the specified node is {@code null}
         * @throws ClassCastException   if the nodes' element types prevent them from
         *                              being compared
         */
        @Override
        public int compareTo(Node<E> node) {
            if (node == null) throw new NullPointerException("The specified node is null");
            if (this == node) return 0;
            final Object thisElement = this.element();
            if (!(thisElement instanceof Comparable)) {
                throw new ClassCastException("This node's element is not Comparable");
            }
            @SuppressWarnings("unchecked")
            final Comparable<E> comparableThisElement = (Comparable<E>)thisElement;
            try { return comparableThisElement.compareTo(node.element()); }
            catch (IllegalArgumentException e) {
                throw new ClassCastException("The specified node's element type is not compatible for comparison: "+e.getMessage());
            }
        }

        /**
         * Returns the hash code value of this {@code LinkNode}.
         *
         * @return the hash code value of this {@code LinkNode}
         */
        @Override
        public int hashCode() {
            return 31 + ((element() == null) ? 0 : element().hashCode());
        }

    } // LinkNode
    
    private static class ReverseLinkNode<E> extends LinkNode<E> implements java.io.Externalizable {
        
        /**
         * The LinkNode that was reversed.
         */
        private final LinkNode<E> linkNode;
        
        /**
         * Construct a ReverseLinkNode.
         * 
         * @param linkNode the LinkNode that was reversed
         */
        private ReverseLinkNode(LinkNode<E> linkNode) {
            super();
            //assert !linkNode.isReversed() : "linkNode is reversed";
            this.linkNode = linkNode;
        }
        
        @Override
        void iSetLinkedNodes(NodableLinkedList<E>.LinkedNodes linkedNodes) {
            linkNode.iSetLinkedNodes(linkedNodes);
        }
        
        @Override
        LinkNode<E> iGetNext() {
            return linkNode.iGetNext();
        }
        
        @Override
        LinkNode<E> iGetPrevious() {
            return linkNode.iGetPrevious();
        }
        
        @Override
        void iSetNext(LinkNode<E> next) {
            linkNode.iSetNext(next);
        }
        
        @Override
        void iSetPrevious(LinkNode<E> previous) {
            linkNode.iSetPrevious(previous);
        }
        
        @Override
        public E element() {
            return linkNode.element();
        }
    
        @Override
        public boolean isReversed() {
            return true;
        }        
        
        @Override
        public LinkNode<E> linkNode() {
            return linkNode;
        }
        
        @Override
        public NodableLinkedList<E>.LinkedNodes linkedNodes() {
            return linkNode.linkedNodes();
        }
        
        @Override
        public E set(E element) {
            return linkNode.set(element);
        }
        
        @Override
        public void addAfter(Node<E> node) {
            linkNode.addBefore(node);
        }
        
        @Override
        public void addBefore(Node<E> node) {
            linkNode.addAfter(node);
        }
        
        @Override
        public boolean hasNext() {
            return linkNode.hasPrevious();
        }
        
        @Override
        public boolean hasPrevious() {
            return linkNode.hasNext();
        }
        
        @Override
        public LinkNode<E> next() {
            final LinkNode<E> node = linkNode.previous();
            return (node == null) ? null : node.reversed();
        }
        
        @Override
        public LinkNode<E> previous() {
            final LinkNode<E> node = linkNode.next();
            return (node == null) ? null : node.reversed();
        }
        
        @Override
        public LinkNode<E> reversed() {
            return linkNode;
        }
    
        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            throw new java.io.InvalidObjectException("not serializable");
        }
    
        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            throw new java.io.InvalidObjectException("not serializable");
        }
        
    } // ReverseLinkNode

    /**
     * Node of a {@link NodableLinkedList} or a {@link NodableLinkedList.SubList}.
     * <p>
     * There are two types of {@code Nodes}: One is a {@link LinkNode} which
     * contains an element and references to the previous and next nodes in a
     * doubly-linked list, and the other is a {@link SubListNode} which represents a
     * {@code LinkNode} that is associated with a {@link NodableLinkedList.SubList}.
     * {@code Nodes} can also be reversed. Reversing a {@code Node} affects all
     * order-sensitive operations ({@code next()}, {@code previous()}, etc.).
     * <p>
     * A {@code Node} does not belong to any particular list until the {@code Node}
     * is inserted/added. Once inserted, the {@code Node} remains linked to the list
     * until removed. A {@code Node} can belong to different lists, just not at the
     * same time.
     * <p>
     * In general, all operations, except {@code index()} and {@code subListNode()},
     * perform in constant time. This does not necessarily apply to
     * {@code SubListNodes} (see the description for {@link SubListNode}).
     * 
     * @author James Pfeifer
     * @param <E> the type of element held in this {@code Node}
     */
    public interface Node<E> extends Comparable<Node<E>> {

        /**
         * Returns the element contained within this {@code Node}.
         * 
         * @return the element contained within this {@code Node}
         */
        public E element();
        
        /**
         * Returns {@code true} if this {@code Node} and the specified node are
         * equivalent. The nodes are considered equivalent if both represent the same
         * {@code LinkNode} ({@code this.linkNode() == node.linkNode()}).
         * 
         * @param node {@code Node} to be compared for equivalency with this
         *             {@code Node}
         * @return {@code true} if this {@code Node} and the specified node are
         *         equivalent
         */
        public boolean isEquivalentTo(Node<?> node);        

        /**
         * Returns {@code true} if this {@code Node} belongs to a list.
         * 
         * @return {@code true} if this {@code Node} belongs to a list
         */
        public boolean isLinked();

        /**
         * Returns {@code true} if this {@code Node} is a {@code LinkNode}.
         * 
         * @return {@code true} if this {@code Node} is a {@code LinkNode}
         */
        public boolean isLinkNode();

        /**
         * Returns {@code true} if this {@code Node} is a {@code SubListNode}.
         * 
         * @return {@code true} if this {@code Node} is a {@code SubListNode}
         */
        public boolean isSubListNode();

        /**
         * Returns {@code true} if this {@code Node} is reversed.
         * 
         * @return {@code true} if this {@code Node} is reversed
         */
        public boolean isReversed();        
        
        /**
         * Returns the {@code LinkNode} backing this {@code Node}. Only non-reversed
         * {@code LinkNodes} are returned.
         * 
         * If this {@code Node} is a {@code LinkNode}, this method returns itself.
         * 
         * @return the {@code LinkNode} backing this {@code Node}, or itself if this
         *         {@code Node} is a {@code LinkNode}
         */     
        public LinkNode<E> linkNode();

        /**
         * Returns the {@code LinkedNodes} list this {@code Node} belongs to, or
         * {@code null} if not linked.
         * 
         * @return the {@code LinkedNodes} list this {@code Node} belongs to, or
         *         {@code null} if not linked
         */
        public NodableLinkedList<E>.LinkedNodes linkedNodes();        

        /**
         * Returns the {@code NodableLinkedList} this {@code Node} belongs to, or
         * {@code null} if not linked.
         * 
         * @return the {@code NodableLinkedList} this {@code Node} belongs to, or
         *         {@code null} if not linked
         */
        public NodableLinkedList<E> nodableLinkedList();

        /**
         * Replaces the element of this {@code Node} with the specified element.
         * 
         * @param element element to be stored in this {@code Node}
         * @return the element previously held in this {@code Node}
         */
        public E set(E element);

        /**
         * Inserts this {@code Node} after the specified node. This {@code Node} must
         * not already belong to a list, and the specified node must belong to a list.
         * <p>
         * If the specified node is a {@code SubListNode}, it is verified that it is
         * still contained by its associated {@code SubList}. Also, this {@code Node} is
         * effectively inserted into the specified {@code SubListNode's} associated
         * {@code SubList}.
         * <p>
         * If this {@code Node} is reversed, this operation effectively behaves like the
         * {@code addBefore(Node)} method.
         * 
         * @param node the {@code Node} this {@code Node} is to be inserted after
         * @throws IllegalStateException    if this {@code Node} is already a node of a
         *                                  list
         * @throws IllegalArgumentException if the specified node is {@code null} or not
         *                                  a node of a list
         */
        public void addAfter(Node<E> node);

        /**
         * Inserts this {@code Node} before the specified node. This {@code Node} must
         * not already belong to a list, and the specified node must belong to a list.
         * <p>
         * If the specified node is a {@code SubListNode}, it is verified that it is
         * still contained by its associated {@code SubList}. Also, this {@code Node} is
         * effectively inserted into the specified {@code SubListNode's} associated
         * {@code SubList}.
         * <p>
         * If this {@code Node} is reversed, this operation effectively behaves like the
         * {@code addAfter(Node)} method.
         * 
         * @param node the {@code Node} this {@code Node} is to be inserted before
         * @throws IllegalStateException    if this {@code Node} is already a node of a
         *                                  list
         * @throws IllegalArgumentException if the specified node is {@code null} or not
         *                                  a node of a list
         */
        public void addBefore(Node<E> node);

        /**
         * Returns {@code true} if there exists a {@code Node} which comes after this
         * {@code Node} in a list. In other words, returns {@code true} if this
         * {@code Node} is not the last node of a list.
         * <p>
         * If this {@code Node} is a {@code SubListNode}, it is verified that it is
         * still contained by its associated {@code SubList}.
         * <p>
         * If this {@code Node} is reversed, this operation effectively behaves like the
         * {@code hasPrevious()} method.
         * 
         * @return {@code true} if there exists a {@code Node} which comes after this
         *         {@code Node}
         * @throws IllegalStateException if this {@code Node} does not belong to a list,
         *                               or it is no longer contained by its associated
         *                               sublist
         */
        public boolean hasNext();

        /**
         * Returns {@code true} if there exists a {@code Node} which comes before this
         * {@code Node} in a list. In other words, returns {@code true} if this
         * {@code Node} is not the first node of a list.
         * <p>
         * If this {@code Node} is a {@code SubListNode}, it is verified that it is
         * still contained by its associated {@code SubList}.
         * <p>
         * If this {@code Node} is reversed, this operation effectively behaves like the
         * {@code hasNext()} method.
         * 
         * @return {@code true} if there exists a {@code Node} which comes before this
         *         {@code Node}
         * @throws IllegalStateException if this {@code Node} does not belong to a list,
         *                               or it is no longer contained by its associated
         *                               sublist
         */
        public boolean hasPrevious();

        /**
         * Returns the index of this {@code Node} in a list, or -1 if this {@code Node}
         * does not belong to a list or the {@code index > Integer.MAX_VALUE}.
         * <p>
         * <b>Performance Consideration:</b> This operation is performed in linear time.
         *
         * @return the index of this {@code Node} in a list, or -1 if this {@code Node}
         *         does not belong to a list or the {@code index > Integer.MAX_VALUE}
         */
        public int index();

        /**
         * Returns the {@code Node} which comes after this {@code Node} in a list. if
         * this {@code Node} is the last or only {@code Node}, {@code null} is returned.
         * <p>
         * If this {@code Node} is a {@code SubListNode}, it is verified that it is
         * still contained by its associated {@code SubList}.
         * <p>
         * If this is a reversed {@code Node}, this operation effectively behaves like
         * the {@code previous()} method, and the returned {@code Node} will also be
         * reversed.
         * 
         * @return the {@code Node} which comes after this {@code Node} in a list, or
         *         {@code null} if this {@code Node} is the last or only {@code Node}
         * @throws IllegalStateException if this {@code Node} does not belong to a list,
         *                               or it is no longer contained by its associated
         *                               sublist
         */
        public Node<E> next();

        /**
         * Returns the {@code Node} which comes before this {@code Node} in a list. if
         * this {@code Node} is the first or only {@code Node}, {@code null} is
         * returned.
         * <p>
         * If this {@code Node} is a {@code SubListNode}, it is verified that
         * it is still contained by its associated {@code SubList}.
         * <p>
         * If this is a reversed {@code Node}, this operation effectively behaves like
         * the {@code next()} method, and the returned {@code Node} will also be
         * reversed.
         * 
         * @return the {@code Node} which comes before this {@code Node} in a list, or
         *         {@code null} if this {@code Node} is the first or only {@code Node}
         * @throws IllegalStateException if this {@code Node} does not belong to a list,
         *                               or it is no longer contained by its associated
         *                               sublist
         */
        public Node<E> previous();
        
        /**
         * Removes this {@code Node} from the list it is linked to.
         * <p>
         * If this {@code Node} is a {@code SubListNode}, it is verified that it is
         * still contained by its associated {@code SubList}.
         * 
         * @throws IllegalStateException if this {@code Node} does not belong to a list,
         *                               or it is no longer contained by its associated
         *                               sublist
         */
        public void remove();

        /**
         * Replaces this {@code Node} with the specified node. This {@code Node} must
         * belong to a list, and the specified node must not already belong to a list.
         * <p>
         * If this {@code Node} is a {@code SubListNode}, it is verified that it is
         * still contained by its associated {@code SubList}. If both this {@code Node}
         * and the specified node are {@code SubListNodes}, after this operation is
         * completed, the specified {@code SubListNode} will be associated with this
         * {@code Node's} {@code SubList}.
         * 
         * @param node {@code Node} to replace this {@code Node}
         * @throws IllegalStateException    if this {@code Node} does not belong to a
         *                                  list, or is no longer contained by its
         *                                  associated sublist
         * @throws IllegalArgumentException if the specified node is {@code null} or
         *                                  already a node of a list
         */
        public void replaceWith(Node<E> node);
        
        /**
         * Returns a {@code Node} that can be used to traverse a list in the reverse
         * direction than this {@code Node}. Order-sensitive operations like
         * {@code addAfter}, {@code addBefore}, {@code hasNext}, {@code hasPrevious},
         * {@code next}, and {@code previous} also operate in a reverse
         * way.
         * 
         * @return a {@code Node} that can be used to traverse a list in the reverse
         *         direction than this {@code Node}
         */
        public Node<E> reversed();

        /**
         * Swaps this {@code Node} with the specified node. Both this {@code Node} and
         * the specified node must belong to a list, but they can be different lists.
         * <p>
         * If this {@code Node} or the specified node are {@code SubListNodes}, they are
         * verified that they are still contained by their associated {@code SubLists},
         * and the nodes are effectively inserted into the other's associated
         * {@code SubList}.
         * <p>
         * <strong>Synchronization consideration:</strong> This operation can
         * potentially operate on two different lists. if synchronization is required,
         * both lists should be synchronized by the same object.
         * 
         * @param node the {@code Node} to swap with this {@code Node}
         * @throws IllegalStateException    if this {@code Node} does not belong to a
         *                                  list, or is no longer contained by its
         *                                  associated sublist
         * @throws IllegalArgumentException if the specified node is {@code null}, does
         *                                  not belong to a list, or is no longer
         *                                  contained by its associated sublist
         */
        public void swapWith(Node<E> node);

        /**
         * Returns the {@code SubList} that is associated with this {@code Node}.
         * 
         * Note, a {@code SubListNode} is always associated with a {@code SubList},
         * whereas, a {@code LinkNode} is never associated with a {@code SubList}.
         * Therefore, a {@code SubListNode} never returns a {@code null}, and a
         * {@code LinkNode} always returns a {@code null}.
         * 
         * @return the {@code NodableLinkedList.SubList} that is associated with this
         *         {@code Node}
         */
        public SubList<E> subList();       

        /**
         * Returns a {@code SubListNode}, backed by this {@code Node's}
         * {@code LinkNode}, for the specified subList. The {@code LinkNode} which backs
         * the returned {@code SubListNode}, must be a node of the specified subList, or
         * unlinked.
         * <p>
         * <b>Performance Consideration:</b> This {@code Node}, if linked, is verified,
         * in linear time, that it is actually a node of the specified subList.
         * 
         * @param subList {@code SubList} containing this {@code Node}
         * @return a {@code SubListNode}, backed by this {@code Node's}
         *         {@code LinkNode}, for the specified subList
         * @throws IllegalArgumentException if the specified subList is {@code null}
         * @throws IllegalStateException    if this {@code Node} is not a node of the
         *                                  specified subList
         */
        public SubListNode<E> subListNode(SubList<E> subList);
        
        /**
         * Compares this {@code Node} with the specified object ({@code Node}) for
         * equality. Returns {@code true} if and only if the specified object is a
         * {@code Node}, and both pairs of elements in the two nodes are <i>equal</i>.
         * (Two elements {@code e1} and {@code e2} are <i>equal</i> if
         * {@code (e1==null ? e2==null : e1.equals(e2))}.)
         *
         * @param object {@code Object} ({@code Node}) to be compared for equality with
         *               this {@code Node}
         * @return {@code true} if the specified object ({@code Node}) is equal to this
         *         {@code Node}
         */
        @Override
        public boolean equals(Object object);
        
        /**
         * Compares this {@code Node} to the specified node for order. Returns a
         * negative integer, zero, or a positive integer as this {@code Node's} element
         * is less than, equal to, or greater than the specified node's element.
         *
         * @param node {@code Node} to be compared to this {@code Node}
         * @return a negative integer, zero, or a positive integer as this
         *         {@code Node's} element is less than, equal to, or greater than the
         *         specified node's element
         * @throws NullPointerException if the specified node is {@code null}
         * @throws ClassCastException   if the nodes' element types prevent them from
         *                              being compared
         */
        @Override
        public int compareTo(Node<E> node);

        /**
         * Returns the hash code value of this {@code Node}. Has the same hash code as
         * its backing {@code LinkNode}.
         *
         * @return the hash code value of this {@code Node}
         */
        @Override
        public int hashCode();
        
    } // Node

} // NodableLinkedList
