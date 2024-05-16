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
import java.lang.reflect.Field;
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
 * 
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
 * 
 * <p>
 * This implementation also has the capability to create reversed lists and
 * sublists. In addition, the method {@code mergeSort} has been implemented,
 * which can sort any size list.
 * 
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
 * 
 * <p>
 * A large {@code NodableLinkedList} can be as much as 33% - 34% larger than a
 * {@code java.util.LinkedList}. An empty {@code NodableLinkedList} is almost 5
 * times larger than an empty {@code java.util.LinkedList}.
 * 
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
 * 
 * <p>
 * There are two ways to visualize a {@code NodableLinkedList}: one as a list of
 * elements (the standard view), and the other as a list of nodes which contain
 * the elements and references to the previous and next nodes in a list. The
 * latter view is implemented by the inner class {@code LinkedNodes}. A
 * {@code NodableLinkedList} is backed by one and only one {@code LinkedNodes}
 * object. Method {@link #linkedNodes()} returns the {@code LinkedNodes} object
 * backing a {@code NodableLinkedList} instance.
 * 
 * <p>
 * All of the operations perform as could be expected for a doubly-linked list.
 * Operations that index into the list will traverse the list from the beginning
 * or the end, whichever is closer to the specified index. Indexes can only be
 * used to reference the first Integer.MAX_VALUE+1 elements.
 * 
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
 *     int insize, nmerges, psize, qsize;
 *     
 *     if (list == null) return; // no list to sort
 *      
 *     insize = 1; // initial size of the p and q lists;
 *                 // after each pass through the list, insize is doubled
 *      
 *     // make as many passes through the list as necessary until the list is sorted
 *     while (true) {
 *
 *         tail = null; // no nodes have been merged yet
 *                      // the merged nodes are placed at the front of the list, and
 *                      // tail is the last node of the merged (sorted) nodes
 *          
 *         nmerges = 0; // initialize the number of merges performed in this pass
 *             
 *         // merge successive sublists (p and q lists) of the list, of size 'insize',
 *         // until the entire list has been processed
 *         p = list.getFirstNode(); // start the p list at the beginning of the list
 *         while (p != null) {
 *             // p list is not empty; there's more nodes to merge
 *           
 *             nmerges++; // count the number of merges performed in this pass
 *              
 *             // find the start of the q list
 *             // which is 'insize' nodes from the start of the p list
 *             q = p;
 *             psize = 0;
 *             for (int i = 0; i < insize; i++) {
 *                 psize++; // determine the size of the p list
 *                 q = q.next(); // traverse the list
 *                 if (q == null) break; // quit if the end of the list has been reached
 *             }
 *             qsize = insize; // assume the q list size is the maximum possible size
 *              
 *             // we have two lists where p is the first node of the first list (p list),
 *             // and q is the first node (or null if the list is empty) of the second
 *             // list (q list); we also know the exact size of the p list
 *              
 *             // merge the p and q lists
 *             while (psize > 0 || (qsize > 0 && q != null)) {
 *              
 *                 // decide where the next element (e) to merge comes from
 *                 // (the p list or the q list)
 *                 if (psize == 0) {
 *                     // p list is empty; e must come from the q list
 *                     e = q; q = q.next(); qsize--;
 *                 } else if (qsize == 0 || q == null) {
 *                     // q list is empty; e must come from the p list
 *                     e = p; p = p.next(); psize--;
 *                 } else if (p.compareTo(q) <= 0) {
 *                     // p's element is lower (or same); e must come from the p list
 *                     e = p; p = p.next(); psize--;
 *                 } else {
 *                     // q's element is lower; e must come from the q list
 *                     e = q; q = q.next(); qsize--;
 *                 }
 *                  
 *                 // remove e then add it back to the front of the list
 *                 e.remove(); // remove e from the list
 *                 if (tail == null) {
 *                     // e is the first node to be added to the merged list
 *                     list.addNodeFirst(e);
 *                 } else {
 *                     // add e after the last node in the merged list
 *                     e.addAfter(tail);
 *                 }
 *                 tail = e; // tail is the last node of the merged (sorted) list
 *             }
 *              
 *             p = q; // the next p list starts where the q list ended
 *         }
 *          
 *         if (nmerges <= 1) break; // list is completely sorted if only one merge
 *                                  // was performed on this pass
 *          
 *         insize *= 2; // make another pass with the p and q lists twice the size
 *     }
 * }
 * }
 * </pre>
 * 
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
 *     NodableLinkedList.Node<String> node = list.getFirstNode();
 *     for (NodableLinkedList.Node<String> sortedNode: sortedNodes) {
 *         node.swapWith(sortedNode);
 *         node = sortedNode.next(); // node = node.next() effectively
 *                                   // sortedNode has replaced node's position in the list
 *     }
 * }
 * }
 * </pre>
 * 
 * <p>
 * <strong>Note that this implementation is not synchronized.</strong> If
 * multiple threads access a linked list concurrently, and at least one of the
 * threads modifies the list structurally, it <i>must</i> be synchronized
 * externally. (A structural modification is any operation that adds or deletes
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
 * 
 * <p>
 * The iterators returned by this class's {@code iterator} and
 * {@code listIterator} methods are <i>fail-fast</i>: if the
 * {@code NodableLinkedList} is structurally modified at any time after the
 * iterator is created, in any way except through the Iterator's own
 * {@code remove} or {@code add} methods, the iterator will throw a {@code
 * ConcurrentModificationException}. Thus, in the face of concurrent
 * modification, the iterator fails quickly and cleanly, rather than risking
 * arbitrary, non-deterministic behavior at an undetermined time in the future.
 * 
 * <p>
 * Note that the fail-fast behavior of an iterator cannot be guaranteed as it
 * is, generally speaking, impossible to make any hard guarantees in the
 * presence of unsynchronized concurrent modification. Fail-fast iterators throw
 * {@code ConcurrentModificationException} on a best-effort basis. Therefore, it
 * would be wrong to write a program that depended on this exception for its
 * correctness: <i>the fail-fast behavior of iterators should be used only to
 * detect bugs.</i>
 * 
 * <p>
 * Instead of using a {@code ListIterator}, consider iterating over the
 * list via {@code Nodes}. For example:
 * <pre>
 * {@code
 *     // list is a NodableLinkedList<Integer>
 *     NodableLinkedList.Node<Integer> linkNode = list.getFirstNode();
 *     while (linkNode != null) {
 *         System.out.println(linkNode.element());
 *         linkNode = linkNode.next();
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

    private transient final LinkedNodes linkedNodes;
    private static final Field linkedNodesField;
    static {
        try {
            linkedNodesField = NodableLinkedList.class.getDeclaredField("linkedNodes");
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("linkedNodes field missing? "+e.getMessage(), e);
        }
    }

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
            linkedNodesField.setAccessible(true);
            linkedNodesField.set(clone, clone.new LinkedNodes()); // clone.linkedNodes = clone.new LinkedNodes()
            linkedNodesField.setAccessible(false);
            clone.addAll(this);
            return clone;               
        } catch (CloneNotSupportedException | ReflectiveOperationException e) {
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
                node = linkedNodes.getNodeAfter(node)) {
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
        try {
            linkedNodesField.setAccessible(true);
            linkedNodesField.set(this, new LinkedNodes()); // this.linkedNodes = new LinkedNodes()
            linkedNodesField.setAccessible(false);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("linkedNodesField not initialized? "+e.getMessage(), e);
        }
        long nodeCount = stream.readLong();
        while (nodeCount-- > 0) {
            @SuppressWarnings("unchecked")
            final E element = (E)stream.readObject();
            addLast(element);			
        }
    }

    //protected int modCount inherited from class java.util.AbstractList
    int modCount() {
        return this.modCount;
    }

    /**
     * Returns the list of nodes which back this {@code NodableLinkedList}.
     * 
     * @return the list of nodes which back this {@code NodableLinkedList}
     */
    public LinkedNodes linkedNodes() {
        return linkedNodes;
    }

    /**
     * Returns a new {@code Node} containing a {@code null} element. The node will
     * not be linked to any list.
     * 
     * @param <T> type of element to be contained within the returned {@code Node}
     * @return an unlinked {@code LinkNode} containing a {@code null} element
     */
    public static <T> LinkNode<T> node() {
        return new LinkNode<>();
    }

    /**
     * Returns a new {@code Node} containing the specified element which can be
     * {@code null}. The node will not be linked to any list.
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
     * Returns the number of elements in this list. If this list contains more than
     * {@code Integer.MAX_VALUE} elements, {@code Integer.MAX_VALUE} is returned.
     *
     * @return the number of elements in this list
     */
    @Override
    public int size() {
        return linkedNodes.size();
    }

    /**
     * Returns the number of elements in this list.
     *
     * @return the number of elements in this list
     */
    public long longSize() {
        return linkedNodes.longSize();
    }
    
    /**
     * Returns {@code true} if this list contains no elements.
     *
     * @return {@code true} if this list contains no elements
     */        
    @Override
    public boolean isEmpty() {
        return linkedNodes.isEmpty();
    }    

    /**
     * Appends the specified node to the end of this list.
     *
     * <p>
     * This method is equivalent to {@link #addNodeLast}.
     *
     * @param node {@code Node} to be appended to the end of this list
     * @throws IllegalArgumentException if node is {@code null} or already a node of
     *                                  a list
     */
    public void addNode(Node<E> node) {
        addNodeLast(node);
    }

    /**
     * Inserts the specified node at the beginning of this list.
     *
     * @param node the {@code Node} to be inserted at the beginning of this list
     * @throws IllegalArgumentException if node is {@code null} or already a node of
     *                                  a list
     */
    public void addNodeFirst(Node<E> node) {
        linkedNodes.addFirst(node);
    }

    /**
     * Appends the specified node to the end of this list.
     *
     * <p>
     * This method is equivalent to {@link #addNode}.
     *
     * @param node {@code Node} to be appended to the end of this list
     * @throws IllegalArgumentException if node is {@code null} or already a node of
     *                                  a list
     */
    public void addNodeLast(Node<E> node) {
        linkedNodes.addLast(node);
    }
    
    /**
     * Returns the first node of this list, or {@code null} if this list is empty.
     * 
     * A reversed {@code LinkNode} is returned for reversed lists.
     *
     * @return the first node of this list, or {@code null} if this list is empty
     */
    public LinkNode<E> getFirstNode() {
        return linkedNodes.getFirstNode();
    }

    /**
     * Returns the last node of this list, or {@code null} if this list is empty.
     * 
     * A reversed {@code LinkNode} is returned for reversed lists.
     *
     * @return the last node of this list, or {@code null} if this list is empty
     */
    public LinkNode<E> getLastNode() {
        return linkedNodes.getLastNode();
    }

    /**
     * Removes and returns the first node of this list.
     * 
     * Returns the removed node or {@code null} if this list is empty
     *
     * @return the first node of this list that was removed, or {@code null} if this
     *         list is empty
     */
    public Node<E> removeFirstNode() {
        return linkedNodes.pollFirst();
    }

    /**
     * Removes and returns the last node of this list.
     * 
     * Returns the removed node or {@code null} if this list is empty
     *
     * @return the last node of this list that was removed, or {@code null} if this
     *         list is empty
     */
    public LinkNode<E> removeLastNode() {
        return linkedNodes.pollLast();
    }

    /**
     * Removes all of the elements from this list.
     */
    @Override
    public void clear() {
        linkedNodes.clear();
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
            for (; node != null; node = linkedNodes.getNodeAfter(node)) {
                if (node.element() == null) return true;          
            }
        } else {
            for (; node != null; node = linkedNodes.getNodeAfter(node)) {
                if (object.equals(node.element())) return true;           
            }
        }
        return false;
    }    

    /**
     * Returns the index of the first occurrence of the specified object (element)
     * in this list, or -1 if there is no such index (this list does not contain the
     * object (element) or the {@code index > Integer.MAX_VALUE}). More formally,
     * returns the lowest index {@code i} such that
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
            for (; node != null; index++, node = linkedNodes.getNodeAfter(node)) {
                if (node.element() == null) {
                    return (index > Integer.MAX_VALUE) ? -1 : (int)index;			
                }
            }
        } else {
            for (; node != null; index++, node = linkedNodes.getNodeAfter(node)) {
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
     * object (element) or the {@code index > Integer.MAX_VALUE}). More formally,
     * returns the highest index {@code i} such that
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
            for (; node != null; index--, node = linkedNodes.getNodeBefore(node)) {
                if (node.element() == null) {
                    return (index > Integer.MAX_VALUE) ? -1 : (int)index;			
                }
            }
        } else {
            for (; node != null; index--, node = linkedNodes.getNodeBefore(node)) {
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
     * 
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
     * 
     * <p>
     * The behavior of this operation is undefined if the specified collection is
     * modified while the operation is in progress. (Note that this will occur if
     * the specified collection is this list, and it's nonempty.)
     *
     * @param index      position where to insert the first element from the
     *                   specified collection
     * @param collection collection containing elements to be added to this list
     * @return {@code true} if this list changed as a result of the call
     * @throws IndexOutOfBoundsException if the index is out of range
     *                                   {@code (index < 0 || index > longSize())}
     * @throws NullPointerException      if the specified collection is {@code null}
     */
    @Override
    public boolean addAll(int index, Collection<? extends E> collection) {
        if (index < 0 || index > longSize()) {
            throw new IndexOutOfBoundsException("index=" + index + ", size=" + longSize());
        }
        if (index == longSize()) return addAll(collection);
        final long initialSize = longSize();
        final LinkNode<E> targetNode = linkedNodes.getNode(index);
        for (E element: collection) {
            linkedNodes.addNodeBefore(node(element), targetNode);
        }
        return longSize() != initialSize;
    }

    /**
     * Inserts all of the elements in the specified collection into this list,
     * before the specified node. Shifts the element currently at that position and
     * any subsequent elements to the right (increases their indices). The new
     * elements will appear in the list in the order that they are returned by the
     * specified collection's iterator. if the specified node is {@code null}, the
     * elements will be appended to the end of this list.
     * 
     * Note that {@code addAll(null, Collection)} is identical in function to
     * {@code addAll(Collection)}.
     * 
     * <p>
     * The behavior of this operation is undefined if the specified collection is
     * modified while the operation is in progress. (Note that this will occur if
     * the specified collection is this list, and it's nonempty.)
     *
     * @param node       {@code Node} the specified collection is to be inserted
     *                   before
     * @param collection collection containing elements to be added to this list
     * @return {@code true} if this list changed as a result of the call
     * @throws IllegalArgumentException if node is not linked to this list
     * @throws NullPointerException     if the specified collection is {@code null}
     */
    public boolean addAll(Node<E> node, Collection<? extends E> collection) {
        if (node == null) return addAll(collection);
        if (!linkedNodes.contains(node)) {
            throw new IllegalArgumentException("Specified node is not linked to this list");
        }
        final LinkNode<E> linkNode = node.linkNode();
        final long initialSize = longSize();
        for (E element: collection) {
            linkedNodes.addNodeBefore(node(element), linkNode);
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
     * to the right (adds one to their indices).
     *
     * @param index   position where the specified element is to be inserted
     * @param element element to be inserted
     * @throws IndexOutOfBoundsException if the index is out of range
     *                                   {@code (index < 0 || index > longSize())}
     */
    @Override
    public void add(int index, E element) {
        linkedNodes.add(index, node(element));
    }	

    /**
     * Inserts the specified element at the beginning of this list.
     *
     * @param element the element to add
     */
    @Override
    public void addFirst(E element) {
        linkedNodes.addFirst(node(element));
    }

    /**
     * Appends the specified element to the end of this list.
     *
     * @param element the element to add
     */
    @Override
    public void addLast(E element) {
        linkedNodes.addLast(node(element));
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
     * @throws IndexOutOfBoundsException if the index is out of range
     *                                   {@code (index < 0 || index >= longSize())}
     */
    @Override
    public E get(int index) {
        return linkedNodes.get(index).element();
    }	

    /**
     * Returns the first element in this list.
     *
     * @return the first element in this list
     * @throws NoSuchElementException if this list is empty
     */
    @Override
    public E getFirst() {
        return linkedNodes.getFirst().element();
    }

    /**
     * Returns the last element in this list.
     *
     * @return the last element in this list
     * @throws NoSuchElementException if this list is empty
     */
    @Override
    public E getLast() {
        return linkedNodes.getLast().element();
    }
    
    /**
     * Returns {@code true} if this list is a reverse-ordered view of the base
     * {@code NodableLinkedList}.
     * 
     * @return {@code true} if this list is a reverse-ordered view of the base
     *         {@code NodableLinkedList}
     */
    public boolean isReversed() {
        return linkedNodes.isReversed();
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
     *
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
     *
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
     * @throws IndexOutOfBoundsException if the index is out of range
     *                                   {@code (index < 0 || index >= longSize())}
     */
    @Override
    public E remove(int index) {
        return linkedNodes.remove(index).element();
    }	

    /**
     * Removes and returns the first element from this list.
     *
     * @return the first element from this list
     * @throws NoSuchElementException if this list is empty
     */
    @Override
    public E removeFirst() {
        return linkedNodes.removeFirst().element();
    }

    /**
     * Removes and returns the last element from this list.
     *
     * @return the last element from this list
     * @throws NoSuchElementException if this list is empty
     */
    @Override
    public E removeLast() {
        return linkedNodes.removeLast().element();
    }

    /**
     * Removes, if present, the first occurrence of the specified object (element)
     * from this list. If this list does not contain the specified object (element),
     * it is unchanged. More formally, removes the element with the lowest index
     * {@code i} such that
     * {@code (object==null ? get(i)==null : object.equals(get(i)))} (if such an
     * element exists). Returns {@code true} if this list contained the specified
     * object (element) (or equivalently, if this list changed as a result of the
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
     * the specified object (element), it is unchanged.
     *
     * @param object {@code Object} (element) to be removed from this list, if
     *               present
     * @return {@code true} if the list contained the specified object (element)
     */
    @Override
    public boolean removeFirstOccurrence(Object object) {
        if (object == null) {
            for (LinkNode<E> node = getFirstNode(); node != null;
                    node = linkedNodes.getNodeAfter(node)) {
                if (node.element() == null) {
                    linkedNodes.removeNode(node);
                    return true;
                }
            }
        } else {
            for (LinkNode<E> node = getFirstNode(); node != null;
                    node = linkedNodes.getNodeAfter(node)) {
                if (object.equals(node.element())) {
                    linkedNodes.removeNode(node);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Removes the last occurrence of the specified object (element) in this list
     * (when traversing the list from head to tail). If the list does not contain
     * the specified object (element), it is unchanged.
     *
     * @param object {@code Object} (element) to be removed from this list, if
     *               present
     * @return {@code true} if the list contained the specified object (element)
     */
    @Override
    public boolean removeLastOccurrence(Object object) {
        if (object == null) {
            for (LinkNode<E> node = getLastNode(); node != null;
                    node = linkedNodes.getNodeBefore(node)) {
                if (node.element() == null) {
                    linkedNodes.removeNode(node);
                    return true;
                }
            }
        } else {
            for (LinkNode<E> node = getLastNode(); node != null;
                    node = linkedNodes.getNodeBefore(node)) {
                if (object.equals(node.element())) {
                    linkedNodes.removeNode(node);
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Returns reverse-order view of this list.
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
        return new Reversed<E>(this);
    }

    /**
     * Replaces the element at the specified position in this list with the
     * specified element.
     *
     * @param index   index of the element to replace
     * @param element element to be stored at the specified position
     * @return the element previously at the specified position
     * @throws IndexOutOfBoundsException if the index is out of range
     *                                   {@code (index < 0 || index >= longSize())}
     */
    @Override
    public E set(int index, E element) {
        if (index < 0 || index >= longSize()) {
            throw new IndexOutOfBoundsException("index=" + index + ", size=" + longSize());
        }
        final Node<E> node = linkedNodes.getNode(index);
        final E originalElement = node.element();
        node.set(element);
        return originalElement;
    }    

    /**
     * Sorts this list according to the order induced by the specified comparator.
     *
     * <p>
     * All elements in this list must be <i>mutually comparable</i> using the
     * specified comparator (that is, {@code c.compare(e1, e2)} must not throw a
     * {@code ClassCastException} for any elements {@code e1} and {@code e2} in the
     * list).
     *
     * If the specified comparator is {@code null} then all elements in this list
     * must implement the {@code Comparable} interface and the elements' natural
     * ordering should be used.
     * 
     * <p>
     * <strong>Implementation Specification:</strong> This implementation obtains an
     * array containing all nodes in this list, sorts the array using
     * {@code Arrays.sort(T[] a, Comparator<? super T> c)}, and then effectively
     * clears the list and puts the sorted nodes from the array back into this list
     * in order. If this list's {@code size > Integer.MAX_VALUE-8}, a
     * {@link #mergeSort} is performed.
     * 
     * <p>
     * <strong>Implementation Note:</strong> This implementation is a stable,
     * adaptive, iterative mergesort that requires far fewer than n lg(n)
     * comparisons when the input array is partially sorted, while offering the
     * performance of a traditional mergesort when the input array is randomly
     * ordered. If the input array is nearly sorted, the implementation requires
     * approximately n comparisons. Temporary storage requirements vary from a small
     * constant for nearly sorted input arrays to n/2 object references for randomly
     * ordered input arrays.
     *
     * <p>
     * The implementation takes equal advantage of ascending and descending order in
     * its input array, and can take advantage of ascending and descending order in
     * different parts of the same input array. It is well-suited to merging two or
     * more sorted arrays: simply concatenate the arrays and sort the resulting
     * array.
     *
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
     * @throws ClassCastException if the list contains elements that are not
     *                            {@code mutually comparable} using the specified
     *                            comparator
     */
    @Override
    public void sort(Comparator<? super E> comparator) {
        if (comparator == null) {
            linkedNodes.sort(null);
        } else {
            linkedNodes.sort((node1, node2) -> {
                return comparator.compare(node1.element(), node2.element());
            });
        }
    }
    
    /**
     * Sorts this list according to the order induced by the specified comparator.
     *
     * <p>
     * All elements in this list must be <i>mutually comparable</i> using the
     * specified comparator (that is, {@code c.compare(e1, e2)} must not throw a
     * {@code ClassCastException} for any elements {@code e1} and {@code e2} in the
     * list).
     *
     * If the specified comparator is {@code null} then all elements in this list
     * must implement the {@code Comparable} interface and the elements' natural
     * ordering should be used.
     * 
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
     * @throws ClassCastException if the list contains elements that are not
     *                            <i>mutually comparable</i> using the specified
     *                            comparator
     */
    public void mergeSort(Comparator<? super E> comparator) {
        if (comparator == null) {
            linkedNodes.mergeSort(null);
        } else {
            linkedNodes.mergeSort((node1, node2) -> {
                return comparator.compare(node1.element(), node2.element());
            });
        }
    }

    /**
     * Merge Sort
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
        int inSize, nMerges, pSize, qSize;
        inSize = 1;
        while (true) {
            sortedLastNode = null;
            nMerges = 0;
            pNode = list.getFirstNode();
            while (pNode != null) {
                nMerges++;
                qNode = pNode;
                pSize = 0;
                for (int i = 0; i < inSize; i++) {
                    pSize++;
                    qNode = list.getNodeAfter(qNode);
                    if (qNode == null) break;
                }
                qSize = inSize;
                while (pSize > 0 || (qSize > 0 && qNode != null)) {
                    if (pSize == 0) {
                        sortedNode = qNode;
                        qNode = list.getNodeAfter(qNode);
                        qSize--;
                    } else if (qSize == 0 || qNode == null) {
                        sortedNode = pNode;
                        pNode = list.getNodeAfter(pNode);
                        pSize--;
                    } else if (((comparator == null)
                                ? pNode.compareTo(qNode)
                                : comparator.compare(pNode, qNode)) <= 0)
                    {
                        sortedNode = pNode;
                        pNode = list.getNodeAfter(pNode);
                        pSize--;
                    } else {
                        sortedNode = qNode;
                        qNode = list.getNodeAfter(qNode);
                        qSize--;
                    }
                    list.removeNode(sortedNode);
                    if (sortedLastNode == null) {
                        list.addNodeFirst(sortedNode);
                    } else {
                        list.addNodeAfter(sortedNode, sortedLastNode);  
                    }
                    sortedLastNode = sortedNode;
                }
                pNode = qNode;
            }
            if (nMerges <= 1) break;
            inSize *= 2;
        }
    } // mergeSort
    
    /**
     * Returns a view of the portion of this list between the specified fromIndex,
     * inclusive, and toIndex, exclusive. (If the specified {@code fromIndex} and
     * {@code toIndex} are equal, the returned {@code SubList} is empty.) The
     * returned {@code SubList} is backed by this list, so structural changes in the
     * returned {@code SubList} are reflected in this list. The returned
     * {@code SubList} supports all of the optional {@code List} operations.
     *
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
     *
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
    public SubList subList(int fromIndex, int toIndex) {
        return linkedNodes.newSubList(fromIndex, toIndex);
    }
    
    /**
     * Returns a view of the portion of this list between the specified firstNode,
     * and lastNode (both inclusive). The returned {@code SubList} is backed by this
     * list, so structural changes in the returned {@code SubList} are reflected in
     * this list. The returned {@code SubList} supports all of the optional
     * {@code List} operations.
     * 
     * <p>
     * If the specified firstNode is {@code null}, an empty {@code SubList},
     * positioned right before the specified lastNode, is returned. If the specified
     * lastNode is {@code null}, an empty {@code SubList}, positioned right after
     * the specified firstNode, is returned. if both the specified firstNode and
     * lastNode are {@code null}, an empty {@code SubList}, positioned at the end of
     * this list, is returned.
     * 
     * <p>
     * <strong>Implementation Note:</strong> For performance reasons, this
     * implementation does not verify that the specified lastNode comes after (or
     * on) the specified firstNode in this list. Therefore, if the specified
     * lastNode does come before the specified firstNode in the list, an
     * {@code IllegalStateException} can be thrown for any operation on the returned
     * {@code SubList}, indicating that the end of the list was reached
     * unexpectedly.
     *
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
     *
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
     *                                  list or if the lastNode comes right before
     *                                  the firstNode in this list.
     */
    public SubList subList(Node<E> firstNode, Node<E> lastNode) {
        return linkedNodes.newSubList(firstNode, lastNode);
    }    

    /**
     * Returns an array containing all of the elements in this list in proper
     * sequence (from first to last element).
     *
     * <p>
     * The returned array will be "safe" in that no references to it are maintained
     * by this list. (In other words, this method must allocate a new array). The
     * caller is thus free to modify the returned array.
     *
     * <p>
     * This method acts as bridge between array-based and collection-based APIs.
     *
     * @return an array containing all of the elements in this list in proper
     *         sequence
     * @throws IllegalStateException if the list is too large to fit in an array
     */
    @Override
    public Object[] toArray() {
        if (longSize() > Integer.MAX_VALUE) {
            throw new IllegalStateException("list size (" + longSize() + ") is too large to fit in an array");
        }
        final Object[] elements = new Object[size()];
        int index = 0;
        for (LinkNode<E> node = getFirstNode(); node != null;
                node = linkedNodes.getNodeAfter(node)) {
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
     *
     * <p>
     * If the list fits in the specified array with room to spare (i.e., the array
     * has more elements than the list), the element in the array immediately
     * following the end of the list is set to {@code null}. (This is useful in
     * determining the length of the list <i>only</i> if the caller knows that the
     * list does not contain any null elements.)
     *
     * <p>
     * Like the {@link #toArray()} method, this method acts as bridge between
     * array-based and collection-based APIs. Further, this method allows precise
     * control over the runtime type of the output array, and may, under certain
     * circumstances, be used to save allocation costs.
     *
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
     * @throws IllegalStateException if the list is too large to fit in an array
     * @throws NullPointerException  if the specified array is {@code null}
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] array) {
        if (longSize() > Integer.MAX_VALUE) {
            throw new IllegalStateException("list size (" + longSize() + ") is too large to fit in an array");
        }
        if (array.length < size()) {
            array = (T[]) java.lang.reflect.Array.newInstance(
                                    array.getClass().getComponentType(),size());
        }
        int index = 0;
        Object[] elements = array;
        for (LinkNode<E> node = getFirstNode(); node != null;
                node = linkedNodes.getNodeAfter(node)) {
            elements[index++] = node.element();			
        }
        if (array.length > size()) array[size()] = null;
        return array;
    }	
    
    /**
     * Common linked list view of LinkedNodes and SubLinkedNodes.
     * 
     * INTERNAL USE ONLY
     */
    abstract class InternalLinkedList extends AbstractSequentialList<Node<E>> {
        
        abstract public long longSize();
        
        abstract void addNodeFirst(LinkNode<E> linkNode);
        
        abstract void addNodeLast(LinkNode<E> linkNode);
        
        abstract void addNodeAfter(LinkNode<E> linkNode, LinkNode<E> afterThisLinkNode);
        
        abstract void addNodeBefore(LinkNode<E> linkNode, LinkNode<E> beforeThisLinkNode);
        
        abstract void removeNode(LinkNode<E> linkNode);
        
        abstract void replaceNode(LinkNode<E> linkNode, LinkNode<E> withThisLinkNode);
        
        abstract boolean hasNodeAfter(LinkNode<E> linkNode);
        
        abstract boolean hasNodeBefore(LinkNode<E> linkNode);
        
        /**
         * Returns this list's headSentinel.
         * 
         * @return this list's headSentinel
         */
        abstract LinkNode<E> getHeadSentinel();
        
        /**
         * Returns this list's tailSentinel. For sublists, a null is returned if the
         * tailSentinel is unknown.
         * 
         * @return this list's tailSentinel
         */      
        abstract LinkNode<E> getTailSentinel();
        
        /**
         * Returns the LinkedNodes' headSentinel from the perspective of this list. If
         * this list is reversed in relation to the LinkedNodes' traversal direction,
         * than the LinkeNodes' tailSentinel is returned.
         * 
         * @return the LinkedNodes' headSentinel from the perspective of this list
         */
        abstract LinkNode<E> getMyLinkedNodesHeadSentinel();
        
        /**
         * Returns the LinkedNodes' tailSentinel from the perspective of this list. If
         * this list is reversed in relation to the LinkedNodes' traversal direction,
         * than the LinkeNodes' headSentinel is returned.
         * 
         * @return the LinkedNodes' tailSentinel from the perspective of this list
         */
        abstract LinkNode<E> getMyLinkedNodesTailSentinel();
        
        abstract LinkNode<E> getFirstNode();
        
        abstract LinkNode<E> getLastNode();
        
        abstract LinkNode<E> getNodeAfter(LinkNode<E> linkNode);
        
        /**
         * Returns the LinkNode that comes after the specified LinkNode. This method is
         * the same as getNodeAfter method, except this method returns the tailSentinel
         * instead of null.
         * 
         * @param node the LinkNode whose next LinkNode is returned
         * @return the LinkNode that comes after the specified LinkNode
         */
        abstract LinkNode<E> getNodeAfterOrTailSentinel(LinkNode<E> linkNode);

        /**
         * Returns the LinkNode that comes after the specified LinkNode. This method is
         * the same as getNodeAfterOrTailSentinel method, except this method returns the
         * LinkNode from this list or a parent (sub)list whose tailSentinel is known.
         * This avoids having to traverse to the end of the list to find the
         * tailSentinel if the tailSentinel is unknown.
         * 
         * @param linkNode the LinkNode whose next LinkNode is returned
         * @return the LinkNode that comes after the specified LinkNode
         */
        abstract LinkNode<E> getNodeAfterFromListWithKnownTailSentinel(LinkNode<E> linknode);
        
        abstract LinkNode<E> getNodeBefore(LinkNode<E> linkNode);
        
        /**
         * Returns the LinkNode that comes before the specified LinkNode. This method is
         * the same as getNodeBefore method, except this method returns the headSentinel
         * instead of null.
         * 
         * @param linkNode the LinkNode whose previous LInkNode is returned
         * @return the LinkNode that comes before the specified LinkNode
         */
        abstract LinkNode<E> getNodeBeforeOrHeadSentinel(LinkNode<E> linkNode);
        
    } // InternalLinkedList

    /**
     * Doubly-linked list of nodes which back a {@code NodableLinkedList}.
     * Implements the {@code List} and {@code Deque} interfaces. The elements are of
     * type {@code NodableLinkedList.LinkNode}, and are never {@code null}.
     * Implements all optional {@code List} operations
     *
     * <p>
     * All of the operations perform as could be expected for a doubly-linked list.
     * Operations that index into the list will traverse the list from the beginning
     * or the end, whichever is closer to the specified index.
     *
     * <p>
     * The iterators returned by this class's {@code iterator} and
     * {@code listIterator} methods are <i>fail-fast</i>: if the
     * {@code NodableLinkedList} is structurally modified at any time after the
     * iterator is created, in any way except through the Iterator's own
     * {@code remove} or {@code add} methods, the iterator will throw a {@code
     * ConcurrentModificationException}. Thus, in the face of concurrent
     * modification, the iterator fails quickly and cleanly, rather than risking
     * arbitrary, non-deterministic behavior at an undetermined time in the future.
     *
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

        private long size;
        private final LinkNode<E> headSentinel = new LinkNode<>();
        private final LinkNode<E> tailSentinel = new LinkNode<>();

        private LinkedNodes() {
            NodableLinkedList.this.modCount = modCount = 0;
            size = 0L;
            headSentinel.linkedNodes = this;
            tailSentinel.linkedNodes = this;
            headSentinel.next = tailSentinel;		
            tailSentinel.previous = headSentinel;			
        }

        //protected int modCount inherited from class java.util.AbstractList
        int modCount() {
            return this.modCount;
        }
        private void incrementModCount() {
            NodableLinkedList.this.modCount = ++modCount; // keep both modCounts in sync
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
        @Override
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
        void addNodeFirst(LinkNode<E> node) {
            //assert node != null && !node.isLinked() : "Node is null or already an element of a list";
            addNodeAfter(node, getHeadSentinel());
        }
        
        @Override
        void addNodeLast(LinkNode<E> node) {
            //assert node != null && !node.isLinked() : "Node is null or already an element of a list";
            addNodeBefore(node, getTailSentinel());
        }

        @Override
        void addNodeAfter(LinkNode<E> node, LinkNode<E> afterThisNode) {
            //assert node != null && !node.isLinked() : "Node is null or already an element of a list";
            //assert this.contains(afterThisNode) : "After Node is not an element of this list";			
            incrementModCount();			
            node.setLinkedNodes(this);
            node.setNext(afterThisNode.getNext());
            node.setPrevious(afterThisNode);
            node.getPrevious().setNext(node);
            node.getNext().setPrevious(node);
            size++;
        }

        @Override
        void addNodeBefore(LinkNode<E> node, LinkNode<E> beforeThisNode) {
            //assert node != null && !node.isLinked() : "Node is null or already an element of a list";
            //assert this.contains(beforeThisNode) : "Before Node is not an element of this list";			
            incrementModCount();			
            node.setLinkedNodes(this);
            node.setNext(beforeThisNode);
            node.setPrevious(beforeThisNode.getPrevious());
            node.getPrevious().setNext(node);
            node.getNext().setPrevious(node);
            size++;
        }

        @Override
        void removeNode(LinkNode<E> node) {
            //assert this.contains(node) : "Node is not an element of this list";			
            incrementModCount();			
            node.getNext().setPrevious(node.getPrevious());
            node.getPrevious().setNext(node.getNext());
            node.setNext(null);		
            node.setPrevious(null);
            node.setLinkedNodes(null);
            size--;
        }

        @Override
        void replaceNode(LinkNode<E> node, LinkNode<E> replacementNode) {
            //assert this.contains(node) : "Node is not an element of this list";
            //assert replacementNode != null && !replacementNode.isLinked() :
            //    "Replacement Node is null or already an element of a list";
            incrementModCount();
            replacementNode.setLinkedNodes(this);
            replacementNode.setNext(node.getNext());
            replacementNode.setPrevious(node.getPrevious());
            replacementNode.getPrevious().setNext(replacementNode);
            replacementNode.getNext().setPrevious(replacementNode);
            node.setNext(null);		
            node.setPrevious(null);
            node.setLinkedNodes(null);
        }

        @Override
        boolean hasNodeAfter(LinkNode<E> node) {
            //assert this.contains(node) : "Node is not an element of this list";			
            return (node.getNext() == getTailSentinel()) ? false : true; 
        }

        @Override
        boolean hasNodeBefore(LinkNode<E> node) {
            //assert this.contains(node) : "Node is not an element of this list";			
            return (node.getPrevious() == getHeadSentinel()) ? false : true; 
        }
        
        @Override
        LinkNode<E> getHeadSentinel() {
            return headSentinel;
        }
        
        @Override
        LinkNode<E> getTailSentinel() {
            return tailSentinel;
        }
        
        @Override
        LinkNode<E> getMyLinkedNodesHeadSentinel() {
            return headSentinel;
        }
        
        @Override
        LinkNode<E> getMyLinkedNodesTailSentinel() {
            return headSentinel;
        }
        
        @Override
        LinkNode<E> getFirstNode() {
            return (isEmpty()) ? null : getNodeAfter(getHeadSentinel());
        }
        
        @Override
        LinkNode<E> getLastNode() {
            return (isEmpty()) ? null : getNodeBefore(getTailSentinel());
        }        

        @Override
        LinkNode<E> getNodeAfter(LinkNode<E> node) {
            //assert this.contains(node) : "Node is not an element of this list";			
            return (node.getNext() == getTailSentinel()) ? null : node.getNext();
        }

        @Override
        LinkNode<E> getNodeAfterOrTailSentinel(LinkNode<E> node) {
            //assert this.contains(node) : "Node is not an element of this list";           
            return node.getNext();
        }
        
        @Override
        LinkNode<E> getNodeAfterFromListWithKnownTailSentinel(LinkNode<E> node) {
            //assert this.contains(node) : "Node is not an element of this list";           
            return node.getNext();
        }

        @Override
        LinkNode<E> getNodeBefore(LinkNode<E> node) {
            //assert this.contains(node) : "Node is not an element of this list";			
            return (node.getPrevious() == getHeadSentinel()) ? null : node.getPrevious();
        }
        
        @Override
        LinkNode<E> getNodeBeforeOrHeadSentinel(LinkNode<E> node) {
            //assert this.contains(node) : "Node is not an element of this list";           
            return node.getPrevious();
        }        

        private long getIndex(LinkNode<?> node) {
            long index = 0L;
            for (LinkNode<E> cursorNode = getFirstNode(); cursorNode != null;
                    cursorNode = getNodeAfter(cursorNode), index++) {
                if (cursorNode == node) return index;				
            }
            return -1L;
        }
        
        private LinkNode<E> getNode(long index) {
            //assert index >= 0L && index < longSize() : "index=" + index + ", size=" + longSize();
            LinkNode<E> node;
            long nodeIndex;
            final long lastIndex = longSize() - 1L;
            if (index < (lastIndex >> 1)) {
                for (node = getFirstNode(), nodeIndex = 0L;
                     nodeIndex < index && node != null;
                     nodeIndex++, node = getNodeAfter(node));
            } else {
                for (node = getLastNode(), nodeIndex = lastIndex;
                     nodeIndex > index && node != null;
                     nodeIndex--, node = getNodeBefore(node));
            }       
            return node;        
        }        

        /**
         * Removes all of the nodes from this list.
         */
        @Override
        public void clear() {
            final LinkNode<E> headSentinel = getHeadSentinel();
            while (!isEmpty()) removeNode(getNodeAfter(headSentinel));
        }        

        /**
         * Returns {@code true} if this list contains the specified object
         * ({@code Node}). The specified object must be a {@code Node}, otherwise,
         * {@code false} is returned.
         * 
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
             return (node != null && node.linkedNodes() == this) ? true : false;
        }
        
        /**
         * Returns and possibly reverses the specified {@code Node's} backing
         * {@code LinkNode} to match the forward direction of this list. In other words,
         * returns a {@code LinkNode} which can be used to traverse this list from the
         * specified node to the list's last node when making successive calls to the
         * {@code LinkNode.next()} method.
         * 
         * @param node the {@code Node} whose backing {@code LinkNode} is returned and
         *             possibly reversed to match the forward direction of this list
         * @return a {@code LinkNode} which can be used to traverse this list in a
         *         forward direction
         * @throws IllegalArgumentException if node is not linked to this list
         */
        public LinkNode<E> forwardLinkNode(Node<E> node) {
            if (!this.contains(node)) {
                throw new IllegalArgumentException("Specified node is not linked to this list");
            }
            return node.linkNode();
        }
        
        /**
         * Returns and possibly reverses the specified {@code Node's} backing
         * {@code LinkNode} to match the reverse direction of this list. In other words,
         * returns a {@code LinkNode} which can be used to traverse this list from the
         * specified node to the list's first node when making successive calls to the
         * {@code LinkNode.next()} method.
         * 
         * @param node the {@code Node} whose backing {@code LinkNode} is returned and
         *             possibly reversed to match the reverse direction of this list
         * @return a {@code LinkNode} which can be used to traverse this list in a
         *         reverse direction
         * @throws IllegalArgumentException if node is not linked to this list
         */
        public LinkNode<E> reverseLinkNode(Node<E> node) {
            if (!this.contains(node)) {
                throw new IllegalArgumentException("Specified node is not linked to this list");
            }
            return node.linkNode().reversed();
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
         * if there is no such index (this list does not contain the specified object
         * ({@code Node}) or the {@code index > Integer.MAX_VALUE}). The specified
         * object must be a {@code Node}, otherwise, -1 is returned.
         *
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
            final long index = getIndex(node.linkNode());
            return (index > Integer.MAX_VALUE) ? -1 : (int)index;
        }

        /**
         * Returns the index of the specified object ({@code Node}) in this list, or -1
         * if there is no such index (this list does not contain the object
         * ({@code Node}) or the {@code index > Integer.MAX_VALUE}). The specified
         * object must be a {@code Node}, otherwise, -1 is returned.
         * 
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
            for (LinkNode<E> node = getLastNode(); node != null;
                    node = getNodeBefore(node), index--) {
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
         * 
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
            for (Node<E> node: collection) addLast(node);
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
         * 
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
         * @throws IndexOutOfBoundsException if the index is out of range
         *                                   {@code (index < 0 || index > longSize())}
         * @throws NullPointerException      if the specified collection is {@code null}
         */
        @Override
        public boolean addAll(int index, Collection<? extends Node<E>> collection) {
            if (index < 0 || index > longSize()) {
                throw new IndexOutOfBoundsException("index=" + index + ", size=" + longSize());
            }
            if (index == longSize()) addAll(collection);
            final long initialSize = longSize();
            final LinkNode<E> targetNode = getNode(index);
            for (Node<E> node: collection) {
                if (node == null || node.isLinked()) {
                    throw new IllegalArgumentException("Node in collection is null or already an element of a list");
                }
                addNodeBefore(node.linkNode(), targetNode);
            }
            return longSize() != initialSize;
        }

        /**
         * Inserts all of the {@code Nodes} in the specified collection into this list,
         * before the specified node. Shifts the {@code Node} currently at that position
         * and any subsequent {@code Nodes} to the right (increases their indices). The
         * new {@code Nodes} will appear in the list in the order that they are returned
         * by the specified collection's iterator. if the specified node is
         * {@code null}, the {@code Nodes} will be appended to the end of this list.
         * 
         * Note that {@code addAll(null, Collection)} is identical in function to
         * {@code addAll(Collection)}.
         * 
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
         * @throws IllegalArgumentException if node is not linked to this list, or any
         *                                  {@code Node} in the collection is
         *                                  {@code null} or already a node of a list
         * @throws NullPointerException     if the specified collection is {@code null}
         */
        public boolean addAll(Node<E> node, Collection<? extends Node<E>> collection) {
            if (node == null) return addAll(collection);
            if (!this.contains(node)) {
                throw new IllegalArgumentException("Specified node is not linked to this list");
            }
            final LinkNode<E> linkNode = node.linkNode();
            final long initialSize = longSize();
            for (Node<E> collectionNode: collection) {
                if (collectionNode == null || collectionNode.isLinked()) {
                    throw new IllegalArgumentException("Node in collection is null or already an element of a list");
                }
                addNodeBefore(collectionNode.linkNode(), linkNode);
            }
            return longSize() != initialSize;
        }

        /**
         * Appends the specified node to the end of this list.
         *
         * @param node {@code Node} to be appended to this list
         * @return {@code true} (as specified by {@link Collection#add})
         * @throws IllegalArgumentException if node is {@code null} or already a node of
         *                                  a list
         */
        @Override
        public boolean add(Node<E> node) {
            addLast(node);
            return true;
        }

        /**
         * Inserts the specified node at the specified position in this list. Shifts the
         * {@code Node} currently at that position (if any) and any subsequent
         * {@code Nodes} to the right (adds one to their indices).
         *
         * @param index position where the specified node is to be inserted
         * @param node  {@code Node} to be inserted
         * @throws IndexOutOfBoundsException if the index is out of range
         *                                   {@code (index < 0 || index > longSize())}
         * @throws IllegalArgumentException  if node is {@code null} or already a node
         *                                   of a list
         */
        @Override
        public void add(int index, Node<E> node) {
            if (index < 0 || index > longSize()) {
                throw new IndexOutOfBoundsException("index=" + index + ", size=" + longSize());
            }
            if (index == longSize()) {
                addLast(node);
            } else {
                if (node == null || node.isLinked()) {
                    throw new IllegalArgumentException("Node is null or already an element of a list");
                }
                addNodeBefore(node.linkNode(), getNode(index));
            }       
        }        

        /**
         * Inserts the specified node at the beginning of this list.
         *
         * @param node the {@code Node} to add
         * @throws IllegalArgumentException if node is {@code null} or already a node of
         *                                  a list
         */
        @Override
        public void addFirst(Node<E> node) {
            if (node == null || node.isLinked()) {
                throw new IllegalArgumentException("Node is null or already an element of a list");
            }
            addNodeFirst(node.linkNode());
        }		

        /**
         * Appends the specified node to the end of this list.
         *
         * @param node the {@code Node} to add
         * @throws IllegalArgumentException if node is {@code null} or already a node of
         *                                  a list
         */
        @Override
        public void addLast(Node<E> node) {
            if (node == null || node.isLinked()) {
                throw new IllegalArgumentException("Node is null or already an element of a list");
            }
            addNodeLast(node.linkNode());
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
         * @throws IndexOutOfBoundsException if the index is out of range
         *                                   {@code (index < 0 || index >= longSize())}
         */
        @Override
        public LinkNode<E> get(int index) {
            if (index < 0 || index >= longSize()) {
                throw new IndexOutOfBoundsException("index=" + index + ", size=" + longSize());
            }
            return getNode(index);
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
            if (isEmpty()) throw new NoSuchElementException("List is empty");
            return getFirstNode();
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
            if (isEmpty()) throw new NoSuchElementException("List is empty");
            return getLastNode();
        }

        /**
         * Adds the specified node as the tail (last {@code Node}) of this list.
         *
         * @param node the {@code Node} to add
         * @return {@code true} (as specified by {@link java.util.Queue#offer})
         * @throws IllegalArgumentException if node is {@code null} or already a node of
         *                                  a list
         */
        @Override
        public boolean offer(Node<E> node) {
            return add(node);
        }

        /**
         * Inserts the specified node at the front of this list.
         *
         * @param node the {@code Node} to insert
         * @return {@code true} (as specified by {@link Deque#offerFirst})
         * @throws IllegalArgumentException if node is {@code null} or already a node of
         *                                  a list
         */
        @Override
        public boolean offerFirst(Node<E> node) {
            addFirst(node);
            return true;
        }

        /**
         * Inserts the specified node at the end of this list.
         *
         * @param node the {@code Node} to insert
         * @return {@code true} (as specified by {@link Deque#offerLast})
         * @throws IllegalArgumentException if node is {@code null} or already a node of
         *                                  a list
         */
        @Override
        public boolean offerLast(Node<E> node) {
            addLast(node);
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
            return getFirstNode();
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
            return getLastNode();
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
            if (isEmpty()) return null;
            final LinkNode<E> node = getFirstNode();
            removeNode(node);
            return node;
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
            if (isEmpty()) return null;
            final LinkNode<E> node = getLastNode();
            removeNode(node);
            return node;
        }        

        /**
         * Pops a {@code Node} from the stack represented by this list. In other words,
         * removes and returns the first {@code Node} of this list.
         *
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
         * words, inserts the specified node at the front of this list.
         *
         * <p>
         * This method is equivalent to {@link #addFirst}.
         *
         * @param node the {@code Node} to push
         * @throws IllegalArgumentException if node is {@code null} or already a node of
         *                                  a list
         */
        @Override
        public void push(Node<E> node) {
            addFirst(node);
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
         * @throws IndexOutOfBoundsException if the index is out of range
         *                                   {@code (index < 0 || index >= longSize())}
         */
        @Override
        public LinkNode<E> remove(int index) {
            if (index < 0 || index >= longSize()) {
                throw new IndexOutOfBoundsException("index=" + index + ", size=" + longSize());
            }
            final LinkNode<E> node = getNode(index);
            removeNode(node);
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
            if (isEmpty()) throw new NoSuchElementException("List is empty");
            final LinkNode<E> node = getFirstNode();
            removeNode(node);
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
            if (isEmpty()) throw new NoSuchElementException("List is empty");
            final LinkNode<E> node = getLastNode();
            removeNode(node);
            return node;
        }

        /**
         * Removes, if present, the specified object ({@code Node}) from this list. If
         * this list does not contain the specified object ({@code Node}), it is
         * unchanged.
         * 
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
            removeNode(node.linkNode());
            return true;		
        }

        /**
         * Removes, if present, the specified object ({@code Node}) from this list. If
         * this list does not contain the specified object ({@code Node}), it is
         * unchanged.
         * 
         * <p>
         * This operation performs in constant time.
         * 
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
         * this list does not contain the specified object ({@code Node}), it is
         * unchanged.
         * 
         * <p>
         * This operation performs in constant time.
         * 
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
         * Returns reverse-order view of this list.
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
            return (new Reversed<E>(nodableLinkedList())).linkedNodes();
        }        

        /**
         * Replaces the {@code Node} at the specified position in this list with the
         * specified node}.
         *
         * @param index index of the {@code Node} to replace
         * @param node  {@code Node} to be stored at the specified position
         * @return the {@code LinkNode} previously at the specified position
         * @throws IndexOutOfBoundsException if the index is out of range
         *                                   {@code (index < 0 || index >= longSize())}
         * @throws IllegalArgumentException  if node is {@code null} or already a node
         *                                   of a list
         */
        @Override
        public LinkNode<E> set(int index, Node<E> node) {
            if (index < 0 || index >= longSize()) {
                throw new IndexOutOfBoundsException("index=" + index + ", size=" + longSize());
            }
            if (node == null || node.isLinked()) {
                throw new IllegalArgumentException("Specified node is null or already an element of a list");            
            }
            final LinkNode<E> originalNode = getNode(index);
            replaceNode(originalNode, node.linkNode());
            return originalNode;
        }        

        /**
         * Sorts this list according to the order induced by the specified comparator.
         *
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
         *
         * <p>
         * <strong>Implementation Specification:</strong> This implementation obtains an
         * array containing all {@code Nodes} in this list, sorts the array using
         * {@code Arrays.sort(T[] a, Comparator<? super T> c)}, and then effectively
         * clears the list and puts the sorted {@code Nodes} from the array back into
         * this list in order. If this list's {@code size > Integer.MAX_VALUE-8}, a
         * {@link #mergeSort} is performed.
         * 
         * <p>
         * <strong>Implementation Note:</strong> This implementation is a stable,
         * adaptive, iterative mergesort that requires far fewer than n lg(n)
         * comparisons when the input array is partially sorted, while offering the
         * performance of a traditional mergesort when the input array is randomly
         * ordered. If the input array is nearly sorted, the implementation requires
         * approximately n comparisons. Temporary storage requirements vary from a small
         * constant for nearly sorted input arrays to n/2 object references for randomly
         * ordered input arrays.
         *
         * <p>
         * The implementation takes equal advantage of ascending and descending order in
         * its input array, and can take advantage of ascending and descending order in
         * different parts of the same input array. It is well-suited to merging two or
         * more sorted arrays: simply concatenate the arrays and sort the resulting
         * array.
         *
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
         * @throws ClassCastException if the list contains elements that are not
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
            for (LinkNode<E> node = getFirstNode(); node != null; node = getNodeAfter(node)) {
                sortedNodes[index++] = node;
            }
            Arrays.sort(sortedNodes, comparator);
            LinkNode<E> node;
            LinkNode<E> sortedNode;
            for (index = 0, node = getFirstNode(); index < sortedNodes.length - 1; index++) {
                sortedNode = sortedNodes[index];
                LinkNode.swapNodes(node, sortedNode);
                node = getNodeAfter(sortedNode); // node = node.next (effectively)
                                                 // sortedNode has replaced node's position in the list
            }
        }
        
        /**
         * Sorts this list according to the order induced by the specified comparator.
         *
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
         * 
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
         * @throws ClassCastException if the list contains elements that are not
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
         * {@code SubList} are reflected in this lsit. The returned {@code SubList}
         * supports all of the optional {@code List} operations.
         *
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
         *
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
        public SubList.LinkedSubNodes subList(int fromIndex, int toIndex) {
            return newSubList(fromIndex, toIndex).linkedSubNodes;
        }
        
        private SubList newSubList(int fromIndex, int toIndex) {
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
                return new SubList(getNodeBeforeOrHeadSentinel(getTailSentinel()), getTailSentinel(), null, size);
            }
            final LinkNode<E> headSentinel = (fromIndex == 0) ? this.getHeadSentinel() : getNode(fromIndex-1);
            final LinkNode<E> tailSentinel = (size == 0L)
                                             ? getNodeAfterOrTailSentinel(headSentinel)
                                             : null; // tailSentinel is currently unknown
            return new SubList(headSentinel, tailSentinel, null, size);
        }
        
        /**
         * Returns a view of the portion of this list between the specified firstNode,
         * and lastNode (both inclusive). The returned {@code SubList} is backed by this
         * list, so structural changes in the returned {@code SubList} are reflected in
         * this list. The returned {@code SubList} supports all of the optional
         * {@code List} operations.
         * 
         * <p>
         * If the specified firstNode is {@code null}, an empty {@code SubList},
         * positioned right before the specified lastNode, is returned. If the specified
         * lastNode is {@code null}, an empty {@code SubList}, positioned right after
         * the specified firstNode, is returned. if both the specified firstNode and
         * lastNode are {@code null}, an empty {@code SubList}, positioned at the end of
         * this list, is returned.
         * 
         * <p>
         * <strong>Implementation Note:</strong> For performance reasons, this
         * implementation does not verify that the specified lastNode comes after (or
         * on) the specified firstNode in this list. Therefore, if the specified
         * lastNode does come before the specified firstNode in the list, an
         * {@code IllegalStateException} can be thrown for any operation on the returned
         * {@code SubList}, indicating that the end of the list was reached
         * unexpectedly.
         *
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
         *
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
         *                                  list or if the lastNode comes right before
         *                                  the firstNode in this list.
         */
        public SubList.LinkedSubNodes subList(Node<E> firstNode, Node<E> lastNode) {
            return newSubList(firstNode, lastNode).linkedSubNodes;
        }
        
        private SubList newSubList(Node<E> firstNode, Node<E> lastNode) {
            if (firstNode == null && lastNode == null) {
                // both firstNode and lastNode are null
                return new SubList(getNodeBeforeOrHeadSentinel(getTailSentinel()), getTailSentinel(), null, 0L);
            } else if (firstNode == null) {
                // only the lastNode is specified
                if (!contains(lastNode)) {
                    throw new IllegalArgumentException("Specified last node is not linked to this list");
                }
                return new SubList(getNodeBeforeOrHeadSentinel(lastNode.linkNode()), lastNode.linkNode(), null, 0L);
            } else if (lastNode == null) {
                // only the firstNode is specified
                if (!contains(firstNode)) {
                    throw new IllegalArgumentException("Specified first node is not linked to this list");
                }
                return new SubList(firstNode.linkNode(), getNodeAfterOrTailSentinel(firstNode.linkNode()), null, 0L);
            }
            // both firstNode and LastNode are specified 
            if (!this.contains(firstNode)) {
                throw new IllegalArgumentException("Specified first node is not linked to this list");
            }
            if (!this.contains(lastNode)) {
                throw new IllegalArgumentException("Specified last node is not linked to this list");
            }
            if (getNodeAfter(lastNode.linkNode()) == firstNode.linkNode()) {
                throw new IllegalArgumentException("Specified last Node comes before the specified first node in this list");
            }
            return new SubList(getNodeBeforeOrHeadSentinel(firstNode.linkNode()),
                    getNodeAfterOrTailSentinel(lastNode.linkNode()), null, -1L);
        }

        /**
         * Returns an array containing all of the {@code Nodes} in this list in proper
         * sequence (from first to last node). The {@code Nodes} in the array are still
         * linked to this list.
         *
         * <p>
         * The returned array will be "safe" in that no references to it are maintained
         * by this list. (In other words, this method allocates a new array). The caller
         * is thus free to modify the returned array.
         *
         * <p>
         * This method acts as bridge between array-based and collection-based APIs.
         *
         * @return an array containing all of the {@code LinkNodes} in this list in proper
         *         sequence
         * @throws IllegalStateException if the list is too large to fit in an array
         */
        @Override
        public Object[] toArray() {
            if (longSize() > Integer.MAX_VALUE) {
                throw new IllegalStateException("list size (" + longSize() + ") is too large to fit in an array");
            }
            final Object[] nodes = new Object[(int) longSize()];
            int index = 0;
            for (LinkNode<E> node = getFirstNode(); node != null; node = getNodeAfter(node)) {
                nodes[index++] = node;
            }
            return nodes;
        }

        /**
         * Returns an array containing all of the {@code Nodes} in this list in proper
         * sequence (from first to last node); the runtime type of the returned array is
         * that of the specified array. If the list fits in the specified array, it is
         * returned therein. Otherwise, a new array is allocated with the runtime type
         * of the specified array and the size of this list.
         *
         * <p>
         * If the list fits in the specified array with room to spare (i.e., the array
         * has more elements than the list), the element in the array immediately
         * following the end of the list is set to {@code null}. (This is useful in
         * determining the length of the list <i>only</i> if the caller knows that the
         * list does not contain any null elements.)
         *
         * <p>
         * Like the {@link #toArray()} method, this method acts as bridge between
         * array-based and collection-based APIs. Further, this method allows precise
         * control over the runtime type of the output array, and may, under certain
         * circumstances, be used to save allocation costs.
         *
         * Note that {@code toArray(new Object[0])} is identical in function to
         * {@code toArray()}.
         *
         * @param array the array into which the {@code LinkNodes} of the list are to be
         *              stored, if it is big enough; otherwise, a new array of the same
         *              runtime type is allocated for this purpose.
         * @return an array containing the {@code LinkNodes} of the list
         * @throws ArrayStoreException   if the runtime type of the specified array is
         *                               not a supertype of the runtime type of every
         *                               node in this list
         * @throws IllegalStateException if the list is too large to fit in an array
         * @throws NullPointerException  if the specified array is {@code null}
         */
        @Override
        @SuppressWarnings("unchecked")
        public <T> T[] toArray(T[] array) {
            if (longSize() > Integer.MAX_VALUE) {
                throw new IllegalStateException("list size (" + longSize() + ") is too large to fit in an array");
            }
            if (array.length < longSize()) {
                array = (T[]) java.lang.reflect.Array.newInstance(array.getClass().getComponentType(), (int) longSize());
            }
            int index = 0;
            Object[] nodes = array;
            for (LinkNode<E> node = getFirstNode(); node != null; node = getNodeAfter(node)) {
                nodes[index++] = node;
            }
            if (array.length > longSize()) array[(int) longSize()] = null;
            return array;
        }

        /**
         * Returns a {@code ListIterator} of the {@code Nodes} in this list (in proper
         * sequence), starting at the specified position in this list. Obeys the general
         * contract of {@code List.listIterator(int)}.
         * 
         * <p>
         * <strong>Implementation Note:</strong> The {@code ListIterator} returned by
         * this method behaves differently when the list's
         * {@code size > Integer.MAX_VALUE}. Methods {@code nextIndex} and
         * {@code previousIndex} return -1 if the {@code index > Integer_MAX_VALUE}.
         *
         * <p>
         * The {@code ListIterator} is <i>fail-fast</i>: if the
         * {@code NodableLinkedList} is structurally modified at any time after the
         * Iterator is created, in any way except through the {@code ListIterator's} own
         * {@code remove} or {@code add} methods, the {@code ListIterator} will throw a
         * {@code ConcurrentModificationException}. Thus, in the face of concurrent
         * modification, the iterator fails quickly and cleanly, rather than risking
         * arbitrary, non-deterministic behavior at an undetermined time in the future.
         * 
         * <p>
         * Instead of using a {@code ListIterator}, consider iterating over the
         * list via {@code Nodes}. For example:
         * <pre>
         * {@code
         *     // list is a NodableLinkedList<Integer>.LinkedNodes
         *     Node<Integer> linkNode = list.get(index); // or list.getFirst();
         *     while (linkNode != null) {
         *         System.out.println(linkNode.element());
         *         linkNode = linkNode.next();
         *     }
         * }
         * </pre>
         *
         * @param index index of the first {@code Node} to be returned from the
         *              {@code ListIterator} (by a call to {@code next})
         * @return a ListIterator of the {@code LinkNodes} in this list (in proper
         *         sequence), starting at the specified position in this list
         * @throws IndexOutOfBoundsException if the index is out of range
         *                                   {@code (index < 0 || index > longSize())}
         * @see List#listIterator(int)
         */
        @Override
        public ListIterator<Node<E>> listIterator(int index) {
            if (index < 0 || index > longSize()) {
                throw new IndexOutOfBoundsException("index=" + index + ", size=" + longSize());
            }
            return linkedNodesListIterator(index);
        }

        private LinkedNodesListIterator linkedNodesListIterator(long index) {
            //assert index >= 0 && index <= size : "index out of range; index=" + index + ", size=" + size;
            LinkNode<E> node;
            if (index == longSize()) {
                index = longSize();
                node = getTailSentinel();
            } else {
                node = getNode(index);
            }
            return new LinkedNodesListIterator(this, longSize(), index, IndexType.ABSOLUTE,
                    node, this.getHeadSentinel(), this.getTailSentinel());
        }

        /**
         * Returns a {@code ListIterator} of the {@code Nodes} in this list (in proper
         * sequence), starting at the specified node in this list. if the specified node
         * is {@code null}, the {@code ListIterator} will be positioned right after the
         * last {@code Node} in this list.
         * 
         * <p>
         * <strong>Implementation Note:</strong> The index returned by the returned
         * {@code ListIterator's} methods {@code nextIndex} and {@code previousIndex} is
         * relative to the specified node which has an index of zero. Nodes which come
         * before the specified node in this list, will have a negative index; nodes
         * that come after will have a positive index. Method {@code nextIndex} returns
         * {@code longSize()} if at the end of the list, and method
         * {@code previousIndex} returns {@code -longSize()} if at the beginning of the
         * list. if {@code index < Integer.MIN_VALUE or index > Integer.MAX_VALUE},
         * {@code Integer.MIN_VALUE} or {@code Integer.MAX_VALUE} is returned
         * respectively.
         *
         * <p>
         * The {@code ListIterator} is <i>fail-fast</i>: if the
         * {@code NodableLinkedList} is structurally modified at any time after the
         * Iterator is created, in any way except through the {@code ListIterator's} own
         * {@code remove} or {@code add} methods, the {@code ListIterator} will throw a
         * {@code ConcurrentModificationException}. Thus, in the face of concurrent
         * modification, the iterator fails quickly and cleanly, rather than risking
         * arbitrary, non-deterministic behavior at an undetermined time in the future.
         * 
         * <p>
         * Instead of using a {@code ListIterator}, consider iterating over the
         * list via {@code Nodes}. For example:
         * <pre>
         * {@code
         *     // list is a NodableLinkedList<Integer>.LinkedNodes
         *     // make sure node is a forward traversing LinkNode
         *     Node<Integer> linkNode = list.forwardLinkNode(node);
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
         * @throws IllegalArgumentException if node is not linked to this list
         */
        public ListIterator<Node<E>> listIterator(Node<E> node) {
            if (node != null && !this.contains(node)) {
                throw new IllegalArgumentException("Specified node is not linked to this list");
            }
            return linkedNodesListIterator(node);
        }
        
        private LinkedNodesListIterator linkedNodesListIterator(Node<E> node) {
            return new LinkedNodesListIterator(this, longSize(), 0L, IndexType.RELATIVE,
                    (node == null) ? this.getTailSentinel() : node.linkNode(),
                    this.getHeadSentinel(), this.getTailSentinel());
        }

        /**
         * Returns an iterator over the {@code Nodes} in this list in reverse sequential
         * order. The {@code Nodes} will be returned in order from last (tail) to first
         * (head).
         * 
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
            return linkedNodesReverseListIterator(0); 
        }
        
        private LinkedNodesReverseListIterator linkedNodesReverseListIterator(long index) {
            return new LinkedNodesReverseListIterator(this, longSize(), index, getNode(longSize() - index - 1),
                    this.getTailSentinel(), this.getHeadSentinel());
        }

        /**
         * Creates a <i>late-binding</i> and <i>fail-fast</i> {@link Spliterator} over
         * the {@code Nodes} in this list.
         *
         * <p>
         * The {@code Spliterator} reports {@link Spliterator#SIZED} and
         * {@link Spliterator#ORDERED}. Overriding implementations should document the
         * reporting of additional characteristic values.
         *
         * <p>
         * <strong>Implementation Note:</strong> The {@code Spliterator} additionally
         * reports {@link Spliterator#SUBSIZED} and {@link Spliterator#NONNULL}, and
         * implements {@code trySplit} to permit limited parallelism..
         *
         * @return a {@code Spliterator} over the {@code LinkNodes} in this list
         */
        @Override
        public Spliterator<Node<E>> spliterator() {
            return linkedNodesSpliterator();
        }
        
        private LinkedNodesSpliterator linkedNodesSpliterator() {
            return new LinkedNodesSpliterator(this);
        }

    } // LinkedNodes

    /**
     * LinkedNodes ListIterator that all other ListIterators utilize.
     */
    enum IndexType { ABSOLUTE, RELATIVE}
    private class LinkedNodesListIterator implements ListIterator<Node<E>> {
        
        private final InternalLinkedList list; // LinkedNodes or SubListNodes
        private final LinkNode<E> headSentinel;
        private final LinkNode<E> tailSentinel;
        private long size; // can be < 0 which indicates size is unknown

        private long cursorIndex;
        private IndexType indexType;
        private LinkNode<E> cursorNode;
        private LinkNode<E> targetNode;
        private int expectedModCount = modCount();
        
        /**
         * Constructs a ListIterator of the LinkedNodes.
         * 
         * The headSentinel and tailSentinel are used to define a sublist of the
         * LinkedNodes to be iterated over. The specified size (if known) is the size of
         * this defined sublist.
         * 
         * For sublists, there is no guarantee that the tailSentinel comes after the
         * headSentinel in the list. If it doesn't, this ListIterator will iterate all
         * the way through the list to the end, at which time, it will throw an
         * IllegalStateException indicating the end of the list was reached
         * unexpectedly. The specified node is guaranteed to come after the headSentinel
         * and before the tailSentinel unless the tailSentinel comes before the
         * headSentinel in the list.
         * 
         * An index can be ABSOLUTE or RELATIVE. An ABSOLUTE index is indexed from the
         * the beginning of the list. A RELATIVE index is relative to the starting node
         * where the starting node has an index of zero, all nodes that come before the
         * starting node have a negative index, and all nodes that come after the
         * starting code have a positive index.
         * 
         * @param list         LinkedNodes or SubListNodes to iterate over
         * @param size         list size which may be < 0 indicating size is unknown
         * @param index        the index of the starting node
         * @param indexType    ABSOLUTE or RELATIVE.
         * @param node         the starting node; the first node to be returned by a
         *                     call to next()
         * @param headSentinel the head sentinel of the (sub)list to be iterated over
         * @param tailSentinel the tail sentinel of the (sub)list to be iterated over
         */
        private LinkedNodesListIterator(InternalLinkedList list, long size, long index, IndexType indexType,
                LinkNode<E> node, LinkNode<E> headSentinel, LinkNode<E> tailSentinel) {
            // assert index >= 0 && index <= size :
            //     "index out of range; index=" + index + ", size=" + size;
            // assert node != null && node.linkedNodes == LinkedNodes.this :
            //     "Specified node is null or is not linked to this list";
            // assert headSentinel != null && headSentinel.linkedNodes == LinkedNodes.this :
            //     "head sentinel is null or is not linked to this list";
            // assert tailSentinel != null && tailSentinel.linkedNodes == LinkedNodes.this :
            //     "tail sentinel is null or is not linked to this list";
            this.list = list;
            this.size = size;
            this.headSentinel = headSentinel;
            this.tailSentinel = tailSentinel;
            this.indexType = indexType;
            cursorIndex = index - 1L;
            this.cursorNode = list.getNodeBeforeOrHeadSentinel(node);
            targetNode = null;
        }
        
        InternalLinkedList list() {
            return this.list;
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
        
        void addNodeAfter(LinkNode<E> node, LinkNode<E> afterThisNode) {
            list.addNodeAfter(node, afterThisNode);
        }
        
        LinkNode<E> getNodeAfter(LinkNode<E> node) {
            final LinkNode<E> nextNode = list.getNodeAfterFromListWithKnownTailSentinel(node);
            if (nextNode == tailSentinel) return null;
            if (nextNode == list.getMyLinkedNodesTailSentinel()) {
                throw new IllegalStateException("End of list reached unexpectedly; the sublist's last node most likely comes before the sublists's first node in the list");
            }
            return nextNode;
        }
        
        LinkNode<E> getNodeBeforeOrHeadSentinel(LinkNode<E> node) {
            return list.getNodeBeforeOrHeadSentinel(node);
        }
        
        void removeNode(LinkNode<E> node) {
            list.removeNode(node);
        }
        
        void replaceNode(LinkNode<E> node, LinkNode<E> replacementNode) {
            list.replaceNode(node, replacementNode);
        }

        @Override
        public boolean hasNext() {
            checkForModificationException();
            if (size >= 0 && cursorIndex >= (size - 1L)) {
                return false;
            }
            final LinkNode<E> cursorNodeNext = getNodeAfter(cursorNode);
            return (cursorNodeNext != null && cursorNodeNext != tailSentinel);
        }

        @Override
        public boolean hasPrevious() {
            checkForModificationException();
            return (cursorNode != headSentinel);
        }

        @Override
        public LinkNode<E> next() {
            checkForModificationException();
            targetNode = null;
            if (!hasNext()) throw new NoSuchElementException();
            cursorNode = getNodeAfter(cursorNode);
            cursorIndex++;
            targetNode = cursorNode;
            return targetNode;
        }

        @Override
        public LinkNode<E> previous() {
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
            if (node == null || node.isLinked()) {
                throw new IllegalArgumentException("Specified node is null or already an element of a list");
            }
            addNodeAfter(node.linkNode(), cursorNode);
            cursorIndex++;
            cursorNode = node.linkNode();
            targetNode = null;
            expectedModCount = modCount();
            if (size >= 0) size++;
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
            if (size >= 0) size--;
        }

        @Override
        public void set(Node<E> node) {
            checkForModificationException();
            if (targetNode == null) {
                throw new IllegalStateException("Neither next nor previous have been called, or remove or add have been called after the last call to next or previous");
            }
            if (node == null || node.isLinked()) {
                throw new IllegalArgumentException("Specified Node is null or already an element of a list");
            }
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

    } // LinkedNodesListIterator
    
    private class LinkedNodesReverseListIterator extends LinkedNodesListIterator {
         
        private LinkedNodesReverseListIterator(InternalLinkedList list, long size, long index,
                LinkNode<E> node, LinkNode<E> headSentinel, LinkNode<E> tailSentinel) {
            super(list, size, index, IndexType.ABSOLUTE, node, headSentinel, tailSentinel);
        }
        
        private LinkedNodesReverseListIterator(InternalLinkedList list, long size, LinkNode<E> node,
                LinkNode<E> headSentinel, LinkNode<E> tailSentinel) {
            super(list, size, 0L, IndexType.RELATIVE, node, headSentinel, tailSentinel);
        }
        
        @Override
        void addNodeAfter(LinkNode<E> node, LinkNode<E> afterThisNode) {
            list().addNodeBefore(node, afterThisNode);
        }            
        
        @Override
        LinkNode<E> getNodeAfter(LinkNode<E> node) {
            return list().getNodeBefore(node);
        }
        
        @Override
        LinkNode<E> getNodeBeforeOrHeadSentinel(LinkNode<E> node) {
            return list().getNodeAfterOrTailSentinel(node);
        }
        
    } // LinkedNodesReverseListIterator

    private class LinkedNodesSpliterator implements Spliterator<Node<E>> {

        private static final int BATCH_INCREMENT = 1 << 10;
        private static final int MAX_BATCH_SIZE  = 1 << 25;
        
        private InternalLinkedList list;

        private LinkNode<E> cursor;
        private long remainingSize = -1L;
        private int batchSize = 0;
        private int expectedModCount;

        private LinkedNodesSpliterator(InternalLinkedList list) {
            this.list = list;
            this.cursor = list.getHeadSentinel();
        }

        private void bind() {
            this.remainingSize = list.longSize();
            this.expectedModCount = modCount();
        }
        
        private int batchSize() {
            return batchSize;
        }
        
        private void checkForModificationException() {
            if (modCount() != expectedModCount) {
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
            if (list.getNodeAfter(cursor) == null || remainingSize < 1L) return false;
            remainingSize--;
            cursor = list.getNodeAfter(cursor);
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
            while (list.getNodeAfter(node) != null && remainingSize-- > 0L) {
                node = list.getNodeAfter(node);
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
            while (index < arraySize && list.getNodeAfter(node) != null) {
                node = list.getNodeAfter(node);
                array[index++] = action.apply(node);                    
            }               
            cursor = node;
            batchSize = index;
            remainingSize -= batchSize;
            return array;
        }            

    } // LinkedNodesSpliterator

    /**
     * Returns a {@code ListIterator} of the elements in this list (in proper
     * sequence), starting at the specified position in this list. Obeys the general
     * contract of {@code List.listIterator(int)}.
     * 
     * <p>
     * <strong>Implementation Note:</strong> The {@code ListIterator} returned by
     * this method behaves differently when the list's
     * {@code size > Integer.MAX_VALUE}. Methods {@code nextIndex} and
     * {@code previousIndex} return -1 if the {@code index > Integer_MAX_VALUE}.
     *
     * <p>
     * The {@code ListIterator} is <i>fail-fast</i>: if the
     * {@code NodableLinkedList} is structurally modified at any time after the
     * Iterator is created, in any way except through the {@code ListIterator's} own
     * {@code remove} or {@code add} methods, the {@code ListIterator} will throw a
     * {@code ConcurrentModificationException}. Thus, in the face of concurrent
     * modification, the iterator fails quickly and cleanly, rather than risking
     * arbitrary, non-deterministic behavior at an undetermined time in the future.
     * 
     * <p>
     * Instead of using a {@code ListIterator}, consider iterating over the
     * list via {@code Nodes}. For example:
     * <pre>
     * {@code
     *     // list is a NodableLinkedList<Integer>
     *     Node<Integer> linkNode = list.linkedNodes().get(index); // or list.getFirstNode();
     *     while (linkNode != null) {
     *         System.out.println(linkNode.element());
     *         linkNode = linkNode.next();
     *     }
     * }
     * </pre>
     *
     * @param index index of the first element to be returned from the
     *              {@code ListIterator} (by a call to {@code next})
     * @return a ListIterator of the elements in this list (in proper sequence),
     *         starting at the specified position in this list
     * @throws IndexOutOfBoundsException if the index is out of range
     *                                   {@code (index < 0 || index > longSize())}
     * @see List#listIterator(int)
     */
    @Override
    public ListIterator<E> listIterator(int index) {
        if (index < 0 || index > longSize()) {
            throw new IndexOutOfBoundsException("index=" + index + ", size=" + longSize());
        }
        return new NodableLinkedListListIterator(index);
    }

    /**
     * Returns a {@code ListIterator} of the elements in this list (in proper
     * sequence), starting at the specified node in this list. if the specified node
     * is {@code null}, the {@code ListIterator} will be positioned right after
     * the last {@code Node} in this list.
     * 
     * <p>
     * <strong>Implementation Note:</strong> The index returned by the returned
     * {@code ListIterator's} methods {@code nextIndex} and {@code previousIndex} is
     * relative to the specified node which has an index of zero. Nodes which come
     * before the specified node in this list, will have a negative index; nodes
     * that come after will have a positive index. Method {@code nextIndex} returns
     * {@code longSize()} if at the end of the list, and method
     * {@code previousIndex} returns {@code -longSize()} if at the beginning of the
     * list. if {@code index < Integer.MIN_VALUE or index > Integer.MAX_VALUE},
     * {@code Integer.MIN_VALUE} or {@code Integer.MAX_VALUE} is returned
     * respectively.
     *
     * <p>
     * The {@code ListIterator} is <i>fail-fast</i>: if the
     * {@code NodableLinkedList} is structurally modified at any time after the
     * Iterator is created, in any way except through the {@code ListIterator's} own
     * {@code remove} or {@code add} methods, the {@code ListIterator} will throw a
     * {@code ConcurrentModificationException}. Thus, in the face of concurrent
     * modification, the iterator fails quickly and cleanly, rather than risking
     * arbitrary, non-deterministic behavior at an undetermined time in the future.
     * 
     * <p>
     * Instead of using a {@code ListIterator}, consider iterating over the
     * list via {@code Nodes}. For example:
     * <pre>
     * {@code
     *     // list is a NodableLinkedList<Integer>
     *     // make sure node is a forward traversing LinkNode
     *     Node<Integer> linkNode = list.linkedNodes().forwardLinkNode(node);
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
     * @throws IllegalArgumentException if node is not linked to this list
     */
    public ListIterator<E> listIterator(Node<E> node) {
        if (node != null && !linkedNodes.contains(node)) {
            throw new IllegalArgumentException("Specified node is not linked to this list");
        }
        return new NodableLinkedListListIterator(node);
    }

    private class NodableLinkedListListIterator implements ListIterator<E> {

        private final LinkedNodesListIterator listIterator;

        private NodableLinkedListListIterator(int index) {
            listIterator = linkedNodes.linkedNodesListIterator(index);
        }

        private NodableLinkedListListIterator(Node<E> node) {
            listIterator = linkedNodes.linkedNodesListIterator(node);
        }
        
        private NodableLinkedListListIterator(LinkedNodesListIterator listIterator) {
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

    } // NodableLinkedListListIterator

    /**
     * Returns an iterator over the elements in this list in reverse sequential
     * order. The elements will be returned in order from last (tail) to first
     * (head).
     * 
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
        return new NodableLinkedListReverseListIterator(0);
    }

    private class NodableLinkedListReverseListIterator extends NodableLinkedListListIterator {

        private NodableLinkedListReverseListIterator(int index) {
            super(linkedNodes.linkedNodesReverseListIterator(index));
        }

    } // NodableLinkedListReverseListIterator

    /**
     * Creates a <i>late-binding</i> and <i>fail-fast</i> {@code Spliterator} over
     * the elements in this list.
     *
     * <p>
     * The {@code Spliterator} reports {@link Spliterator#SIZED} and
     * {@link Spliterator#ORDERED}. Overriding implementations should document the
     * reporting of additional characteristic values.
     *
     * <p>
     * <strong>Implementation Note:</strong> The {@code Spliterator} additionally
     * reports {@link Spliterator#SUBSIZED} and implements {@code trySplit} to
     * permit limited parallelism..
     *
     * @return a {@code Spliterator} over the elements in this list
     */
    @Override
    public Spliterator<E> spliterator() {
        return new NodableLinkedListSpliterator();
    }

    private class NodableLinkedListSpliterator implements Spliterator<E> {

        private final LinkedNodesSpliterator spliterator = linkedNodes.linkedNodesSpliterator();

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

    } // NodableLinkedListSpliterator

    /**
     * Sublist of a {@code NodableLinkedList}. Implements all optional {@code List}
     * interface operations, and permits all elements (including {@code null}). A
     * {@code SubList} represents a range of elements of a
     * {@code NodableLinkedList}. If an element is added to or removed from a
     * {@code SubList}, the element is respectively added to or removed from the
     * backing {@code NodableLinkedList}.
     * 
     * <p>
     * Just like a {@code NodableLinkedList}, there are two ways to visualize a
     * {@code NodableLinkedList.SubList}: one as a list of elements (the standard
     * view), and the other as a list of nodes. The latter view is implemented by
     * the inner class {@code LinkedSubNodes}. A {@code SubList} is backed by one
     * and only one {@code LinkedSubNodes} instance. Method
     * {@link #linkedSubNodes()} returns the {@code LinkedSubNodes} object backing a
     * {@code NodableLinkedList.SubList}.
     * 
     * <p>
     * <strong>Implementation Note:</strong> When a {@code SubList} is created,
     * normally either the last node is unknown (if created via subList(fromIndex,
     * toIndex)), or the size is unknown (if created via subList(firstNode,
     * lastNode)). To determine the unknown size or unknown last node, it would be
     * necessary to traverse the entire sublist from beginning to end. Therefore,
     * this implementation delays making that determination until it's necessary (if
     * ever). This can have other performance implications. for instance, normally
     * for a doubly-linked list, you would expect operations that index into the
     * list will traverse the list from the beginning or the end, whichever is
     * closer to the specified index. However, if the last node is unknown (or it's
     * uncertain if the last node actually comes after the first node in the
     * sublist), then this implementation has no choice but to traverse the sublist
     * from the beginning, even if the index is closer to the end of the sublist.
     * Performing an operation like {@code getLastNode()}, will either complete in
     * constant time if both the size and the last node are already known, or it
     * will traverse the entire sublist from the beginning to the end. But, when
     * completed, both the size and the last node of the sublist will be known.
     * 
     * <p>
     * If the {@code NodableLinkedList} that a {@code SubList} is part of is
     * structurally modified in anyway except via operations on the {@code SubList}
     * itself, the {@code SubList} is invalidated and any future operations on the
     * {@code SubList} will throw a {@code ConcurrentModificationException}.
     *
     * <p>
     * The iterators returned by this class's {@code iterator} and
     * {@code listIterator} methods are <i>fail-fast</i>: if the
     * {@code NodableLinkedList} is structurally modified at any time after the
     * iterator is created, in any way except through the Iterator's own
     * {@code remove} or {@code add} methods, the iterator will throw a {@code
     * ConcurrentModificationException}. Thus, in the face of concurrent
     * modification, the iterator fails quickly and cleanly, rather than risking
     * arbitrary, non-deterministic behavior at an undetermined time in the future.
     *
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
    public class SubList extends AbstractSequentialList<E> implements List<E> {
        
        private final LinkedSubNodes linkedSubNodes;
        
        private SubList(LinkNode<E> headSentinel, LinkNode<E> tailSentinel,
                SubList parent, long size) {
            this.linkedSubNodes = new LinkedSubNodes(headSentinel, tailSentinel, parent, size);
        }
        
        // used by ReversedSubList
        private SubList(LinkedSubNodes linkedSubNodes) {
            this.linkedSubNodes = new ReversedLinkedSubNodes(linkedSubNodes);
        }
        
        //protected int modCount inherited from class java.util.AbstractList
        private void checkForModificationException() {
            if (this.modCount != NodableLinkedList.this.modCount()) {
                throw new ConcurrentModificationException();
            }
        }
        
        /**
         * Returns the {@code NodableLinkedList} which contains this {@code SubList}.
         * 
         * @return the {@code NodableLinkedList} which contains this {@code SubList}
         */
        public NodableLinkedList<E> nodableLinkedList() {
            return NodableLinkedList.this;
        }
        
        /**
         * Returns the sublist of nodes which back this {@code SubList}
         * 
         * @return the sublist of nodes which back this {@code SubList}
         */
        public LinkedSubNodes linkedSubNodes() {
            return this.linkedSubNodes;
        }

        /**
         * Inserts the specified node at the beginning of this {@code SubList}.
         * 
         * <p>
         * If the specified node is a {@code SubListNode}, after this operation is
         * completed, it will be marked that it is contained by this {@code SubList}.
         *
         * @param node the {@code Node} to be inserted at the beginning of this
         *             {@code SubList}
         * @throws IllegalArgumentException if node is {@code null} or already a node of
         *                                  a list
         */
        public void addNodeFirst(Node<E> node) {
            checkForModificationException();
            if (node == null || node.isLinked()) {
                throw new IllegalArgumentException("Node is null or already an element of a list");
            }
            linkedSubNodes.addNodeFirst(node.linkNode());
            if (node.isSubListNode()) {
                final SubListNode<E> subListNode = (SubListNode<E>)node;
                subListNode.setSubList(this);
                subListNode.updateExpectedModCount();
            }
        }

        /**
         * Appends the specified node to the end of this {@code SubList}.
         * 
         * <p>
         * If the specified node is a {@code SubListNode}, after this operation is
         * completed, it will be marked that it is contained by this {@code SubList}.
         *
         * @param node {@code Node} to be appended to the end of this {@code SubList}
         * @throws IllegalArgumentException if node is {@code null} or already a node of
         *                                  a list
         */
        public void addNodeLast(Node<E> node) {
            checkForModificationException();
            if (node == null || node.isLinked()) {
                throw new IllegalArgumentException("Node is null or already an element of a list");
            }
            if (this.isEmpty()) addNodeFirst(node);
            else linkedSubNodes.addNodeAfter(node.linkNode(), getLastNode());
            if (node.isSubListNode()) {
                final SubListNode<E> subListNode = (SubListNode<E>)node;
                subListNode.setSubList(this);
                subListNode.updateExpectedModCount();
            }
        }
        
        /**
         * Returns the first {@code Node} of this {@code SubList}, or returns
         * {@code null} if this {@code SubList} is empty.
         * 
         * A reversed {@code LinkNode} may be returned that traverses this
         * {@code SubList} in the same direction as this {@code SubList}. In other
         * words, the {@code LinkNode} that is returned can be used to traverse this
         * {@code SubList} from the sublist's first node to the last node when making
         * successive calls to the {@code LinkNode.next()} method.
         *
         * @return the first {@code LinkNode} of this {@code SubList}, or {@code null}
         *         if this {@code SubList} is empty
         */
        public LinkNode<E> getFirstNode() {
            checkForModificationException();
            if (this.isEmpty()) return null;
            final LinkNode<E> linkNode = linkedSubNodes.getFirstNode();
            return (isReversed()) ? linkNode.reversed() : linkNode;
        }        
        
        /**
         * Returns the first {@code SubListNode} of this {@code SubList}, or returns
         * {@code null} if this {@code SubList} is empty.
         *
         * @return the first {@code SubListNode} of this {@code SubList}, or
         *         {@code null} if this {@code SubList} is empty
         */
        public SubListNode<E> getFirstSubListNode() {
            return (this.isEmpty()) ? null : new SubListNode<E>(getFirstNode(), this);
        }
        
        /**
         * Returns the last {@code Node} of this {@code SubList}, or returns
         * {@code null} if this {@code SubList} is empty.
         * 
         * A reversed {@code LinkNode} may be returned that traverses this
         * {@code SubList} in the same direction as this {@code SubList}. In other
         * words, the {@code LinkNode} that is returned can be used to traverse this
         * {@code SubList} from the sublist's first node to the last node when making
         * successive calls to the {@code LinkNode.next()} method.
         *
         * @return the last {@code LinkNode} of this {@code SubList}, or {@code null} if
         *         this {@code SubList} is empty
         */
        public LinkNode<E> getLastNode() {
            checkForModificationException();
            if (this.isEmpty()) return null;
            final LinkNode<E> linkNode = linkedSubNodes.getLastNode();
            return (isReversed()) ? linkNode.reversed() : linkNode;
        }        
        
        /**
         * Returns the last {@code SubListNode} of this {@code SubList}, or returns
         * {@code null} if this {@code SubList} is empty.
         *
         * @return the last {@code SubListNode} of this {@code SubList}, or {@code null}
         *         if this {@code SubList} is empty
         */
        public SubListNode<E> getLastSubListNode() {
            return (this.isEmpty()) ? null : new SubListNode<E>(getLastNode(), this);
        }

        /**
         * Removes and returns the first {@code Node} of this {@code SubList}.
         * 
         * Returns the removed {@code Node} or {@code null} if this {@code SubList} is
         * empty
         *
         * @return the first {@code LinkNode} of this {@code SubList} that was removed,
         *         or {@code null} if this {@code SubList} is empty
         */
        public LinkNode<E> removeFirstNode() {
            final LinkNode<E> firstNode = getFirstNode();
            if (firstNode != null) linkedSubNodes.removeNode(firstNode);
            return firstNode;
        }
        
        /**
         * Removes and returns the first {@code SubListNode} of this {@code SubList}.
         * 
         * Returns the removed {@code SubListNode} or {@code null} if this
         * {@code SubList} is empty
         *
         * @return the first {@code SubListNode} of this {@code SubList} that was
         *         removed, or {@code null} if this {@code SubList} is empty
         */       
        public SubListNode<E> removeFirstSubListNode() {
            return (this.isEmpty()) ? null : new SubListNode<E>(removeFirstNode(), this);
        }

        /**
         * Removes and returns the last {@code Node} of this {@code SubList}.
         * 
         * Returns the removed {@code Node} or {@code null} if this {@code SubList} is
         * empty
         *
         * @return the last {@code LinkNode} of this {@code SubList} that was removed, or
         *         {@code null} if this {@code SubList} is empty
         */
        public LinkNode<E> removeLastNode() {
            final LinkNode<E> lastNode = getLastNode();
            if (lastNode != null) linkedSubNodes.removeNode(lastNode);
            return lastNode;
        }
        
        /**
         * Removes and returns the last {@code SubListNode} of this {@code SubList}.
         * 
         * Returns the removed {@code SubListNode} or {@code null} if this
         * {@code SubList} is empty
         *
         * @return the last {@code SubListNode} of this {@code SubList} that was
         *         removed, or {@code null} if this {@code SubList} is empty
         */        
        public SubListNode<E> removeLastSubListNode() {
            return (this.isEmpty()) ? null : new SubListNode<E>(removeLastNode(), this);
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
         * @throws IndexOutOfBoundsException if the index is out of range
         *                                   {@code (index < 0 || index >= longSize())}
         */
        @Override
        public E get(int index) {
            return linkedSubNodes.get(index).element();
        }
        
        /**
         * Inserts the specified element at the specified position in this
         * {@code SubList}. Shifts the element currently at that position (if any) and
         * any subsequent elements to the right (adds one to their indices).
         *
         * @param index   position where the specified element is to be inserted
         * @param element element to be inserted
         * @throws IndexOutOfBoundsException if the index is out of range
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
         * @throws IndexOutOfBoundsException if the index is out of range
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
         * Returns reverse-order view of this {@code SubList}.
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
        public SubList reversed() {
            linkedSubNodes.getConfirmedTailSentinel(); // make sure new reversed sublist's headSentinel is valid
            return new ReversedSubList(this);
        }
        
        /**
         * Replaces the element at the specified position in this {@code SubList} with
         * the specified element.
         *
         * @param index   index of the element to replace
         * @param element element to be stored at the specified position
         * @return the element previously at the specified position
         * @throws IndexOutOfBoundsException if the index is out of range
         *                                   {@code (index < 0 || index >= longSize())}
         */
        @Override
        public E set(int index, E element) {
            final Node<E> node = linkedSubNodes.get(index);
            final E originalElement = node.element();
            node.set(element);
            return originalElement;
        }
        
        /**
         * Appends all of the elements in the specified collection to the end of this
         * {@code SubList}, in the order that they are returned by the specified
         * collection's iterator.
         * 
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
                linkedSubNodes.addNodeBefore(node(element), tailSentinel);
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
         * 
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
         * @throws IndexOutOfBoundsException if the index is out of range
         *                                   {@code (index < 0 || index > longSize())}
         * @throws NullPointerException      if the specified collection is {@code null}
         */
        @Override
        public boolean addAll(int index, Collection<? extends E> collection) {
            checkForModificationException();
            if (linkedSubNodes.sizeIsKnown() && (index < 0 || index > longSize())) {
                throw new IndexOutOfBoundsException("index=" + index + ", size=" + longSize());
            }
            boolean changed = false;
            final LinkNode<E> targetNode = linkedSubNodes.getNode(index);
            for (E element : collection) {
                linkedSubNodes.addNodeBefore(node(element), targetNode);
                changed = true;
            }
            return changed;
        }
        
        /**
         * Inserts all of the elements in the specified collection into this
         * {@code SubList}, before the specified node. Shifts the element currently at
         * that position and any subsequent elements to the right (increases their
         * indices). The new elements will appear in this {@code SubList} in the order
         * that they are returned by the specified collection's iterator. if the
         * specified node is {@code null}, the elements will be appended to the end of
         * this {@code SubList}.
         * 
         * Note that {@code addAll(null, Collection)} is identical in function to
         * {@code addAll(Collection)}.
         * 
         * <p>
         * The behavior of this operation is undefined if the specified collection is
         * modified while the operation is in progress. (Note that this will occur if
         * the specified collection is this {@code SubList}, and it's nonempty.)
         *
         * @param node       {@code Node} the specified collection is to be inserted
         *                   before
         * @param collection collection containing elements to be added to this list
         * @return {@code true} if this {@code SubList} changed as a result of the call
         * @throws IllegalArgumentException if node is not linked to this list
         * @throws NullPointerException     if the specified collection is {@code null}
         */
        public boolean addAll(Node<E> node, Collection<? extends E> collection) {
            checkForModificationException();
            if (node == null) return addAll(collection);
            if (!linkedSubNodes.contains(node)) {
                throw new IllegalArgumentException("specified node is not part of this sublist");
            }
            final LinkNode<E> linkNode = node.linkNode();
            boolean changed = false;
            for (E element: collection) {
                linkedSubNodes.addNodeBefore(node(element), linkNode);
                changed = true;
            }
            return changed;
        }

        /**
         * Sorts this {@code SubList} according to the order induced by the specified
         * comparator.
         *
         * <p>
         * All elements in this {@code SubList} must be <i>mutually comparable</i> using
         * the specified comparator (that is, {@code c.compare(e1, e2)} must not throw a
         * {@code ClassCastException} for any elements {@code e1} and {@code e2} in the
         * {@code SubList}).
         *
         * If the specified comparator is {@code null} then all elements in this
         * {@code SubList} must implement the {@code Comparable} interface and the
         * elements' natural ordering should be used.
         * 
         * <p>
         * <strong>Implementation Specification:</strong> This implementation obtains an
         * array containing all nodes in this {@code SubList}, sorts the array using
         * {@code Arrays.sort(T[] a, Comparator<? super T> c)}, and then effectively
         * clears the {@code SubList} and puts the sorted nodes from the array back into
         * this {@code SubList} in order. If this {@code SubList's}
         * {@code size > Integer.MAX_VALUE-8}, a {@link #mergeSort} is performed.
         * 
         * <p>
         * <strong>Implementation Note:</strong> This implementation is a stable,
         * adaptive, iterative mergesort that requires far fewer than n lg(n)
         * comparisons when the input array is partially sorted, while offering the
         * performance of a traditional mergesort when the input array is randomly
         * ordered. If the input array is nearly sorted, the implementation requires
         * approximately n comparisons. Temporary storage requirements vary from a small
         * constant for nearly sorted input arrays to n/2 object references for randomly
         * ordered input arrays.
         *
         * <p>
         * The implementation takes equal advantage of ascending and descending order in
         * its input array, and can take advantage of ascending and descending order in
         * different parts of the same input array. It is well-suited to merging two or
         * more sorted arrays: simply concatenate the arrays and sort the resulting
         * array.
         *
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
         * @throws ClassCastException if the sublist contains elements that are not
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
         *
         * <p>
         * All elements in this {@code SubList} must be <i>mutually comparable</i> using
         * the specified comparator (that is, {@code c.compare(e1, e2)} must not throw a
         * {@code ClassCastException} for any elements {@code e1} and {@code e2} in the
         * {@code SubList}).
         *
         * If the specified comparator is {@code null} then all elements in this
         * {@code SubList} must implement the {@code Comparable} interface and the
         * elements' natural ordering should be used.
         * 
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
         * @throws ClassCastException if the list contains elements that are not
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
         *
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
         *
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
        public SubList subList(int fromIndex, int toIndex) {
            return linkedSubNodes.newSubList(fromIndex, toIndex);
        }
        
        /**
         * Returns a view of the portion of this {@code SubList} between the specified
         * fisrtNode, and lastNode (both inclusive). The returned {@code SubList} is
         * backed by this {@code SubList}, so structural changes in the returned
         * {@code SubList} are reflected in this {@code SubList}. The returned
         * {@code SubList} supports all of the optional {@code List} operations.
         * 
         * <p>
         * If the specified firstNode is {@code null}, an empty {@code SubList},
         * positioned right before the specified lastNode, is returned. If the specified
         * lastNode is {@code null}, an empty {@code SubList}, positioned right after
         * the specified firstNode, is returned. if both the specified firstNode and
         * lastNode are {@code null}, an empty {@code SubList}, positioned at the end of
         * this {@code SubList}, is returned.
         * 
         * <p>
         * This method verifies the specified lastNode comes after the specified
         * FirstNode in this {@code SubList}. Also, the size of the returned
         * {@code SubList} is known.
         *
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
         *
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
         *                                  {@code SubList} or if the lastNode comes
         *                                  before the firstNode in this {@code SubList}
         */
        public SubList subList(Node<E> firstNode, Node<E> lastNode) {
            checkForModificationException();
            return linkedSubNodes.newSubList(firstNode, lastNode);
        }
        
        /**
         * Returns a {@code ListIterator} of the elements in this {@code SubList} (in
         * proper sequence), starting at the specified position in this {@code SubList}.
         * Obeys the general contract of {@code List.listIterator(int)}.
         * 
         * <p>
         * <strong>Implementation Note:</strong> The {@code ListIterator} returned by
         * this method behaves differently when the sublist's
         * {@code size > Integer.MAX_VALUE}. Methods {@code nextIndex} and
         * {@code previousIndex} return -1 if the {@code index > Integer_MAX_VALUE}.
         *
         * <p>
         * The {@code ListIterator} is <i>fail-fast</i>: if the
         * {@code NodableLinkedList} is structurally modified at any time after the
         * Iterator is created, in any way except through the {@code ListIterator's} own
         * {@code remove} or {@code add} methods, the {@code ListIterator} will throw a
         * {@code ConcurrentModificationException}. Thus, in the face of concurrent
         * modification, the iterator fails quickly and cleanly, rather than risking
         * arbitrary, non-deterministic behavior at an undetermined time in the future.
         * 
         * <p>
         * Instead of using a {@code ListIterator}, consider iterating over the
         * {@code SubList} via {@code SubListNodes}. For example:
         * <pre>
         * {@code
         *     // sublist is a NodableLinkedList<Integer>.SubList
         *     Node<Integer> subListNode = sublist.linkedSubNodes().getSubListNode(index);
         *     //                       or sublist.getFirstSubListNode();
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
         * @throws IndexOutOfBoundsException if the index is out of range
         *                                   {@code (index < 0 || index > longSize())}
         * @see List#listIterator(int)
         */
        @Override
        public ListIterator<E> listIterator(int index) {
            if (index < 0) throw new IndexOutOfBoundsException("index=" + index);
            return new SubListIterator(index);
        }
        
        /**
         * Returns a {@code ListIterator} of the elements in this {@code SubList} (in
         * proper sequence), starting at the specified node in this {@code SubList}. if
         * the specified node is {@code null}, the {@code ListIterator} will be
         * positioned right after the last {@code Node} in this {@code SubList}.
         * 
         * <p>
         * <strong>Implementation Note:</strong> The index returned by the returned
         * {@code ListIterator's} methods {@code nextIndex} and {@code previousIndex} is
         * relative to the specified node which has an index of zero. Nodes which come
         * before the specified node in this {@code SubList}, will have a negative
         * index; nodes that come after will have a positive index. Method
         * {@code nextIndex} returns {@code longSize()} if at the end of the
         * {@code SubList}, and method {@code previousIndex} returns {@code -longSize()}
         * if at the beginning of the {@code SubList}. if
         * {@code index < Integer.MIN_VALUE or index > Integer.MAX_VALUE},
         * {@code Integer.MIN_VALUE} or {@code Integer.MAX_VALUE} is returned
         * respectively.
         *
         * <p>
         * The {@code ListIterator} is <i>fail-fast</i>: if the
         * {@code NodableLinkedList} is structurally modified at any time after the
         * Iterator is created, in any way except through the {@code ListIterator's} own
         * {@code remove} or {@code add} methods, the {@code ListIterator} will throw a
         * {@code ConcurrentModificationException}. Thus, in the face of concurrent
         * modification, the iterator fails quickly and cleanly, rather than risking
         * arbitrary, non-deterministic behavior at an undetermined time in the future.
         * 
         * <p>
         * Instead of using a {@code ListIterator}, consider iterating over the
         * {@code SubList} via {@code SubListNodes}. For example:
         * <pre>
         * {@code
         *     // sublist is a NodableLinkedList<Integer>.SubList
         *     Node<Integer> subListNode = node.subListNode(sublist);
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
         * @throws IllegalArgumentException if node is not linked to this
         *                                  {@code SubList}
         */
        public ListIterator<E> listIterator(Node<E> node) {
            if (node != null && !linkedNodes.contains(node)) {
                throw new IllegalArgumentException("Specified node is not linked to this list");
            }
            return new SubListIterator(node);
        }
        
        /**
         * Doubly-linked sublist of nodes which back a
         * {@code NodableLinkedList.SubList}. Implements all optional {@code List}
         * interface operations. The elements are of type
         * {@code NodableLinkedList.LinkNode}, and are never {@code null}.
         * 
         * A {@code LinkedSubNodes} sublist represents a range of {@code LinkNodes} of a
         * {@code NodableLinkedList.LinkedNodes} list. If a {@code Node} is added to or
         * removed from a {@code LinkedSubNodes} sublist, the {@code Node's}
         * {@code LinkNode} is respectively added to or removed from the backing
         * {@code LinkedNodes} list.
         * 
         * <p>
         * The iterators returned by this class's {@code iterator} and
         * {@code listIterator} methods are <i>fail-fast</i>: if the
         * {@code NodableLinkedList} is structurally modified at any time after the
         * iterator is created, in any way except through the Iterator's own
         * {@code remove} or {@code add} methods, the iterator will throw a {@code
         * ConcurrentModificationException}. Thus, in the face of concurrent
         * modification, the iterator fails quickly and cleanly, rather than risking
         * arbitrary, non-deterministic behavior at an undetermined time in the future.
         *
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
        public class LinkedSubNodes
            extends InternalLinkedList
            implements List<Node<E>> {
            
            private LinkNode<E> headSentinel;
            private LinkNode<E> tailSentinel;
            private final LinkedSubNodes parent;
            private long size;
            
            private LinkedSubNodes() {
                this.headSentinel = null;
                this.tailSentinel = null;
                this.parent = null;
                this.size = 0L;
            }
            
            private LinkedSubNodes(LinkNode<E> headSentinel, LinkNode<E> tailSentinel,
                    SubList sublist, long size) {
                // assert headSentinel != null : "headSentinel is null";
                // assert tailSentinel != null : "tailSentinel is null";
                // assert headSentinel.linkedNodes == linkedNodes :
                //     "headSentinel not linked to this linkedList";
                // assert tailSentinel.linkedNodes == linkedNodes :
                //     "tailSentinel not linked to this linkedList";
                // considerations:
                // . the tailSentinel(null) and size(-1) may be unknown,
                //   but never both at the same time
                // . no guarantee that headSentinel comes before tailSentinel
                //   in the parent list
                this.headSentinel = headSentinel;
                this.tailSentinel = tailSentinel;
                this.parent = (sublist == null) ? null : sublist.linkedSubNodes();
                this.size = size;
                updateModCount();
            }
            
            //protected int modCount inherited from class java.util.AbstractList
            int modCount() {
                return this.modCount;
            }
            
            void updateModCount() {
                SubList.this.modCount = this.modCount = NodableLinkedList.this.modCount(); //keep all modCounts in sync
            }
            
            private void updateModCounts() {
                if (parent() != null) parent().updateModCounts();
                updateModCount();
            }            
            
            private void updateSizeAndModCount(long sizeChange) {
                if (sizeIsKnown()) setSize(getSize() + sizeChange);
                updateModCount();
            }
            
            private void checkForModificationException() {
                if (this.modCount != NodableLinkedList.this.modCount()) {
                    throw new ConcurrentModificationException();
                }
            }
            
            /**
             * Returns the {@code NodableLinkedList.SubList} this {@code LinkedSubNodes}
             * object is backing.
             * 
             * @return the {@code NodableLinkedList.SubList} this {@code LinkedSubNodes}
             *         object is backing
             */
            public SubList nodableLinkedListSubList() {
                return SubList.this;
            }
            
            @Override
            LinkNode<E> getHeadSentinel() {
                return this.headSentinel;
            }
            
            @Override
            LinkNode<E> getTailSentinel() {
                return this.tailSentinel;
            }
            
            void setHeadSentinel(LinkNode<E> headSentinel) {
                this.headSentinel = headSentinel;
            }
            
            void setTailSentinel(LinkNode<E> tailSentinel) {
                this.tailSentinel = tailSentinel;
            }
            
            LinkedSubNodes parent () {
                return this.parent;
            }
            
            long getSize() {
                return this.size;
            }
            
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
            LinkNode<E> getMyLinkedNodesHeadSentinel() {
                if (parent() == null) return linkedNodes.getHeadSentinel();
                return parent().getMyLinkedNodesHeadSentinel();
            }
            
            /**
             * Returns the LinkedNodes' tailSentinel from the perspective of this sublist. If
             * this sublist is reversed in relation to the LinkedNodes' traversal direction,
             * than the LinkeNodes' headSentinel is returned.
             * 
             * @return the LinkedNodes' tailSentinel from the perspective of this list
             */
            @Override
            LinkNode<E> getMyLinkedNodesTailSentinel() {
                if (parent() == null) return linkedNodes.getTailSentinel();
                return parent().getMyLinkedNodesTailSentinel();
            }
            
            private boolean tailSentinelIsKnown() {
                return this.getTailSentinel() != null;
            }
            
            LinkNode<E> tailSentinel() {
                if (!tailSentinelIsKnown()) { // tailSentinel unknown?
                    //assert size >= 0 : "sublist size is unknown";
                    long remaining = longSize();
                    final LinkNode<E> linkedNodesTailSentinel = getMyLinkedNodesTailSentinel();
                    LinkNode<E> tailSentinel = getNodeAfterFromListWithKnownTailSentinel(getHeadSentinel());
                    while (remaining > 0 && tailSentinel != linkedNodesTailSentinel) {
                        remaining--;
                        tailSentinel = getNodeAfterFromListWithKnownTailSentinel(tailSentinel);
                    }
                    if (remaining > 0) {
                        throw new IllegalStateException("End of list reached unexpectedly");
                    }
                    setTailSentinel(tailSentinel);
                }
                return getTailSentinel();
            }
            
            LinkNode<E> getConfirmedTailSentinel() {
                if (sizeIsKnown() & tailSentinelIsKnown()) return getTailSentinel();
                return getNode(longSize());
            }
            
            @Override
            void addNodeFirst(LinkNode<E> node) {
                //assert node != null && !node.isLinked() :
                //    "Node is null or already an element of a list";
                addNodeAfter(node, getHeadSentinel());
            }
            
            @Override
            void addNodeLast(LinkNode<E> node) {
                //assert node != null && !node.isLinked() :
                //    "Node is null or already an element of a list";
                addNodeBefore(node, getConfirmedTailSentinel());
            }

            @Override
            void addNodeAfter(LinkNode<E> node, LinkNode<E> afterThisNode) {
                //assert node != null && !node.isLinked() :
                //    "Node is null or already an element of a list";
                //assert this.contains(afterThisNode) :
                //    "Before Node is not an element of this sublist";
                if (parent() == null) linkedNodes.addNodeAfter(node, afterThisNode);
                else parent().addNodeAfter(node, afterThisNode);
                updateSizeAndModCount(1L);
            }

            @Override
            void addNodeBefore(LinkNode<E> node, LinkNode<E> beforeThisNode) {
                //assert node != null && !node.isLinked() :
                //    "Node is null or already an element of a list";
                //assert this.contains(beforeThisNode) :
                //    "Before Node is not an element of this sublist";
                if (parent() == null) linkedNodes.addNodeBefore(node, beforeThisNode);
                else parent.addNodeBefore(node, beforeThisNode);
                updateSizeAndModCount(1L);
            }
            
            @Override
            void removeNode(LinkNode<E> node) {
                //assert this.contains(node) : "Node is not an element of this sublist";
                if (parent() == null) linkedNodes.removeNode(node);
                else parent.removeNode(node);
                updateSizeAndModCount(-1L);                
            }
            
            @Override
            void replaceNode(LinkNode<E> node, LinkNode<E> replacementNode) {
                //assert this.contains(node) : "Node is not an element of this sublist";
                //assert replacementNode != null && !replacementNode.isLinked() :
                //    "Replacement Node is null or is already an element of a list";
                if (parent() == null) linkedNodes.replaceNode(node, replacementNode);
                else parent().replaceNode(node, replacementNode);
                updateSizeAndModCount(0L);
            }

            @Override
            boolean hasNodeAfter(LinkNode<E> node) {
                //assert this.contains(node) || node == headSentinel : "Node is not an element of this sublist";
                LinkNode<?> nextNode;
                if (parent() == null) nextNode = linkedNodes.getNodeAfterOrTailSentinel(node);
                else nextNode = parent().getNodeAfterOrTailSentinel(node);
                if (nextNode == tailSentinel()) return false;
                if (nextNode == getMyLinkedNodesTailSentinel()) {
                    throw new IllegalStateException(
                            "End of list reached unexpectedly; the sublists's last node most likely comes before the sublist's first node in the list");
                }
                return true;
            }

            @Override
            boolean hasNodeBefore(LinkNode<E> node) {
                //assert this.contains(node) || node == tailSentinel() : "Node is not an element of this sublist";
                if (parent() == null) return (linkedNodes.getNodeBeforeOrHeadSentinel(node) == getHeadSentinel()) ? false : true;
                return (parent().getNodeBeforeOrHeadSentinel(node) == getHeadSentinel()) ? false : true; 
            }
            
            @Override
            LinkNode<E> getFirstNode() {
                return getNodeAfter(getHeadSentinel());
            }
            
            @Override
            LinkNode<E> getLastNode() {
                return getNodeBefore(getConfirmedTailSentinel());
            }            
            
            @Override
            LinkNode<E> getNodeAfter(LinkNode<E> node) {
                //assert this.contains(node) || node == headSentinel : "Node is not an element of this sublist";
                if (!hasNodeAfter(node)) return null;
                if (parent() == null) return linkedNodes.getNodeAfter(node);
                return parent().getNodeAfter(node);
            }
            
            @Override
            LinkNode<E> getNodeAfterOrTailSentinel(LinkNode<E> node) {
                //assert this.contains(node) || node == headSentinel : "Node is not an element of this sublist";
                if (!hasNodeAfter(node)) return tailSentinel();
                if (parent() == null) return linkedNodes.getNodeAfter(node);
                return parent().getNodeAfter(node);
            }
            
            @Override
            LinkNode<E> getNodeAfterFromListWithKnownTailSentinel(LinkNode<E> node) {
                LinkedSubNodes linkedSubNodes = this;
                do {
                    if (linkedSubNodes.tailSentinelIsKnown()) {
                        return linkedSubNodes.getNodeAfterOrTailSentinel(node);
                    }
                linkedSubNodes = linkedSubNodes.parent();
                } while (linkedSubNodes != null);
                return linkedNodes.getNodeAfterOrTailSentinel(node);
            }
            
            @Override
            LinkNode<E> getNodeBefore(LinkNode<E> node) {
                //assert this.contains(node) || node == tailSentinel() : "Node is not an element of this sublist";
                if (!hasNodeBefore(node)) return null;
                if (parent() == null) return linkedNodes.getNodeBefore(node);
                return parent().getNodeBefore(node);
            }
            
            @Override
            LinkNode<E> getNodeBeforeOrHeadSentinel(LinkNode<E> node) {
                //assert this.contains(node) || node == tailSentinel() : "Node is not an element of this sublist";
                if (!hasNodeBefore(node)) return getHeadSentinel();
                if (parent() == null) return linkedNodes.getNodeBefore(node);
                return parent().getNodeBefore(node);
            }
            
            private void swappedNodes(LinkNode<E> subSetNode, LinkNode<E> swappedNode) {
                if (subSetNode.linkedNodes() == swappedNode.linkedNodes()) {
                    // both nodes are nodes of the same list, therefore,
                    // the head or tail sentinels may have been swapped
                    if (swappedNode == getHeadSentinel()) setHeadSentinel(subSetNode);
                    if (swappedNode == getTailSentinel()) setTailSentinel(subSetNode);
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
             * @throws IndexOutOfBoundsException if the index is out of range
             *                                   {@code (index < 0 || index >= longSize())}
             * @throws IllegalStateException     if end of list is reached unexpectedly; the
             *                                   sublist's last node most likely comes
             *                                   before the sublists's first node in the
             *                                   list
             */
            private LinkNode<E> getNode(long index) {
                // Note, this routine returns the tailSentinel if index = longSize()
                if (index < 0L) throw new IndexOutOfBoundsException("index=" + index);
                LinkNode<E> node;
                long cursorIndex;
                if (sizeIsKnown() && tailSentinelIsKnown()) {
                    // both size and tailSentinel is known, therefore, we also know that
                    // the tailSentinel comes after the headSentinel in the list
                    if (index > longSize()) {
                        throw new IndexOutOfBoundsException("index=" + index + " > size=" + longSize());
                    }
                    if (index <= ((longSize()) >> 1)) {
                        cursorIndex = 0L;
                        node = getNodeAfterOrTailSentinel(getHeadSentinel());
                        while (cursorIndex < index) { node = getNodeAfter(node); cursorIndex++; }
                    } else {
                        cursorIndex = longSize();
                        node = tailSentinel();
                        while (cursorIndex > index) { node = getNodeBefore(node); cursorIndex--; }
                    }
                } else if (sizeIsKnown()) {
                    // size is known, tailSentinel is unknown
                    if (index > longSize()) {
                        throw new IndexOutOfBoundsException("index=" + index + " > size=" + longSize());
                    }
                    cursorIndex = 0L;
                    node = getNodeAfterFromListWithKnownTailSentinel(getHeadSentinel());
                    while (cursorIndex < index) {
                        node = getNodeAfterFromListWithKnownTailSentinel(node);
                        cursorIndex++;
                    }
                    if (index == longSize()) setTailSentinel(node);
                    if (index == longSize()-1) setTailSentinel(getNodeAfterFromListWithKnownTailSentinel(node));
                } else {
                    // size is unknown, tailSentinel is known
                    cursorIndex = 0L;
                    node = getNodeAfterOrTailSentinel(getHeadSentinel());
                    while ( cursorIndex < index && node != getTailSentinel()) {
                        node = getNodeAfterOrTailSentinel(node);
                        cursorIndex++;
                    }
                    if (cursorIndex < index) {
                        throw new IndexOutOfBoundsException("index=" + index + " > size=" + cursorIndex);
                    }
                    if (node == getTailSentinel()) this.setSize(cursorIndex);
                    else if (getNodeAfterOrTailSentinel(node) == getTailSentinel()) this.setSize(cursorIndex + 1L);
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
            private long getIndex(LinkNode<?> node) {
                if (!linkedNodes.contains(node)) return -1;
                long cursorIndex = 0L;
                LinkNode<E> cursorNode;
                if (sizeIsKnown() && tailSentinelIsKnown()) {
                    // both size and tailSentinel is known, therefore, we also know that
                    // the tailSentinel comes after the headSentinel in the list
                    cursorNode = getFirstNode();
                    while ( cursorNode != null && !cursorNode.isEquivalentTo(node)) {
                        cursorIndex++;
                    cursorNode = getNodeAfter(cursorNode);
                    }
                    if (cursorNode == null) cursorIndex = -1L; // node not found
                } else if (sizeIsKnown()) {
                    // size is known, tailSentinel is unknown
                    cursorNode = getNodeAfterFromListWithKnownTailSentinel(getHeadSentinel());
                    while (cursorIndex < longSize() && !cursorNode.isEquivalentTo(node)) {
                        cursorIndex++;
                    cursorNode = getNodeAfterFromListWithKnownTailSentinel(cursorNode);
                    }
                    if (cursorIndex == longSize()-1) {
                        this.setTailSentinel(getNodeAfterFromListWithKnownTailSentinel(cursorNode));
                    }
                    if (cursorIndex == longSize()) {
                        this.setTailSentinel(cursorNode);
                        cursorIndex = -1L; // node not found
                    }
                } else {
                    // size is unknown, tailSentinel is known
                    cursorNode = getFirstNode();
                    while ( cursorNode != null && !cursorNode.isEquivalentTo(node)) {
                        cursorIndex++;
                        cursorNode = getNodeAfter(cursorNode);
                    }
                    if (cursorNode == null) {
                        this.setSize(cursorIndex);
                        cursorIndex = -1L; // node not found
                    } else if (getNodeAfter(cursorNode) == null) this.setSize(cursorIndex + 1L);
                }
                return cursorIndex;
            }
            
            private boolean sizeIsKnown() {
                return getSize() >= 0L;
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
                    for (node = getFirstNode(); node != null; node = getNodeAfter(node)) {
                        size++;
                    }
                    this.setSize(size);
                }
                return getSize();
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
                return !hasNodeAfter(getHeadSentinel());
            }
            
            /**
             * Removes all of the {@code Nodes} from this sublist.
             */
            @Override
            public void clear() {
                checkForModificationException();
                LinkNode<E> node = getFirstNode();
                if (sizeIsKnown()) {
                     while (longSize() > 0) {
                        final LinkNode<E> nodeToRemove = node;
                        node = getNodeAfterFromListWithKnownTailSentinel(node);
                        removeNode(nodeToRemove);
                    }
                } else {
                    while (node != null) {
                        final LinkNode<E> nodeToRemove = node;
                        node = getNodeAfter(node);
                        removeNode(nodeToRemove);
                    }
                }
            }
            
            /**
             * Returns and possibly reverses the specified {@code Node's} backing
             * {@code LinkNode} to match the forward direction of this sublist. In other
             * words, returns a {@code LinkNode} which can be used to traverse this sublist
             * from the specified node to the sublist's last node when making successive
             * calls to the {@code LinkNode.next()} method.
             * 
             * @param node the {@code Node} whose backing {@code LinkNode} is returned and
             *             possibly reversed to match the forward direction of this sublist
             * @return a {@code LinkNode} which can be used to traverse this sublist in a
             *         forward direction
             * @throws IllegalArgumentException if node is not linked to this sublist
             */
            public LinkNode<E> forwardLinkNode(Node<E> node) {
                if (!this.contains(node)) {
                    throw new IllegalArgumentException("Specified node is not linked to this sublist");
                }
                return (isReversed()) ? node.linkNode().reversed() : node.linkNode();
            }
            
            /**
             * Returns and possibly reverses the specified {@code Node's} backing
             * {@code LinkNode} to match the reverse direction of this sublist. In other
             * words, returns a {@code LinkNode} which can be used to traverse this sublist
             * from the specified node to the sublist's first node when making successive
             * calls to the {@code LinkNode.next()} method.
             * 
             * @param node the {@code Node} whose backing {@code LinkNode} is returned and
             *             possibly reversed to match the reverse direction of this sublist
             * @return a {@code LinkNode} which can be used to traverse this sublist in a
             *         reverse direction
             * @throws IllegalArgumentException if node is not linked to this sublist
             */
            public LinkNode<E> reverseLinkNode(Node<E> node) {
                if (!this.contains(node)) {
                    throw new IllegalArgumentException("Specified node is not linked to this sublist");
                }
                return (isReversed()) ? node.linkNode() : node.linkNode().reversed();
            }

            /**
             * Returns the index of the specified object ({@code Node}) in this sublist, or
             * -1 if there is no such index (this sublist does not contain the specified
             * object ({@code Node)} or the {@code index > Integer.MAX_VALUE}). Note, a -1 is
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
                    if (sublistnode.subList() != this.nodableLinkedListSubList()) {
                        return -1;
                    }
                    node = ((SubListNode<?>)object).linkNode();
                } else {
                    return -1;
                }
                final long index = getIndex(node);
                return (index > Integer.MAX_VALUE) ? -1 : (int)index;
            }
            
            /**
             * Returns the index of the specified object ({@code Node}) in this sublist, or
             * -1 if there is no such index (this sublist does not contain the specified
             * object ({@code Node}) or the {@code index > Integer.MAX_VALUE}). Note, a -1
             * is returned if the specified object is a {@code SubListNode} and it is not
             * associated with this sublist.
             * 
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
                    if (sublistnode.subList() != this.nodableLinkedListSubList()) {
                        return -1;
                    }
                    searchNode = ((SubListNode<?>) object).linkNode();
                } else {
                    return -1;
                }

                long index = longSize() - 1L;
                for (LinkNode<E> node = getLastNode(); node != null; node = getNodeBefore(node), index--) {
                    if (node == searchNode) {
                        return (index > Integer.MAX_VALUE) ? -1 : (int) index;
                    }
                }
                return -1;
            }
            
            /**
             * Returns the {@code Node} at the specified position in this sublist.
             * 
             * A reversed {@code LinkNode} may be returned that traverses this
             * {@code SubList} in the same direction as this {@code SubList}. In other
             * words, the {@code LinkNode} that is returned can be used to traverse this
             * {@code SubList} from the sublist's first node to the last node when making
             * successive calls to the {@code LinkNode.next()} method.
             *
             * @param index index of the {@code Node} to return
             * @return the {@code LinkNode} at the specified position in this sublist
             * @throws IndexOutOfBoundsException if the index is out of range
             *                                   {@code (index < 0 || index >= longSize())}
             */
            @Override
            public LinkNode<E> get(int index) {
                checkForModificationException();
                final long getIndex = index;
                if (sizeIsKnown() && (getIndex < 0L || getIndex >= longSize())) {
                    throw new IndexOutOfBoundsException("index=" + index + ", size=" + longSize());        
                }
                LinkNode<E> node = getNode(index);
                if (node == getTailSentinel()) {
                    throw new IndexOutOfBoundsException("index=" + index + " = size=" + longSize());
                }
                return (isReversed()) ? node.reversed() : node;
            }
            
            /**
             * Returns the {@code SublistNode} at the specified position in this sublist.
             *
             * @param index index of the {@code SubListNode} to return
             * @return the {@code SubListNode} at the specified position in this sublist
             * @throws IndexOutOfBoundsException if the index is out of range
             *                                   {@code (index < 0 || index >= longSize())}
             */
            public SubListNode<E> getSubListNode(int index) {
                return new SubListNode<E>(get(index), this.nodableLinkedListSubList());
            }            
            
            /**
             * Inserts the specified node at the specified position in this sublist. Shifts
             * the {@code Node} currently at that position (if any) and any subsequent
             * {@code Nodes} to the right (adds one to their indices).
             * 
             * <p>
             * If the specified node is a {@code SubListNode}, after this operation is completed,
             * it will be marked that it is contained by this sublist.
             *
             * @param index position where the specified node is to be inserted
             * @param node  {@code Node} to be inserted
             * @throws IndexOutOfBoundsException if the index is out of range
             *                                   {@code (index < 0 || index > longSize())}
             * @throws IllegalArgumentException  if node is {@code null} or already a node
             *                                   of a list
             */
            @Override
            public void add(int index, Node<E> node) {
                checkForModificationException();
                if (sizeIsKnown() && (index < 0L || index > longSize())) {
                    throw new IndexOutOfBoundsException("index=" + index + ", size=" + longSize());
                }
                if (node == null || node.isLinked()) {
                    throw new IllegalArgumentException("Node is null or already an element of a list");
                }
                addNodeBefore(node.linkNode(), getNode(index));
                if (node.isSubListNode()) {
                    final SubListNode<E> subListNode = (SubListNode<E>)node;
                    subListNode.setSubList(this.nodableLinkedListSubList());
                    subListNode.updateExpectedModCount();
                }
            }
            
            /**
             * Removes and returns the {@code Node} at the specified position in this
             * sublist. Shifts any subsequent {@code Nodes} to the left (subtracts one from
             * their indices).
             *
             * @param index the index of the {@code Node} to be removed
             * @return the {@code LinkNode} previously at the specified position
             * @throws IndexOutOfBoundsException if the index is out of range
             *                                   {@code (index < 0 || index >= longSize())}
             */
            @Override
            public LinkNode<E> remove(int index) {
                checkForModificationException();
                if (sizeIsKnown() && (index < 0L || index >= longSize())) {
                    throw new IndexOutOfBoundsException("index=" + index + ", size=" + longSize());
                }
                final LinkNode<E> node = get(index);
                removeNode(node);
                return node;
            }
            
            /**
             * Removes and returns the {@code SubListNode} at the specified position in this
             * sublist. Shifts any subsequent {@code Nodes} to the left (subtracts one from
             * their indices).
             *
             * @param index the index of the {@code SubListNode} to be removed
             * @return the {@code SubListNode} previously at the specified position
             * @throws IndexOutOfBoundsException if the index is out of range
             *                                   {@code (index < 0 || index >= longSize())}
             */
            public SubListNode<E> removeSubListNode(int index) {
                checkForModificationException();
                if (sizeIsKnown() && (index < 0L || index >= longSize())) {
                    throw new IndexOutOfBoundsException("index=" + index + ", size=" + longSize());
                }
                final LinkNode<E> node = get(index);
                removeNode(node);
                return new SubListNode<E>(node, this.nodableLinkedListSubList());
            }            

            /**
             * Removes, if present, the specified object ({@code Node}) from this sublist.
             * If this sublist does not contain the specified object ({@code Node}), it is
             * unchanged. Note, if the specified object is a {@code SubListNode}, it will
             * not be removed if the {@code SubListNode} is not associated with this
             * sublist.
             * 
             * @param object {@code Object} ({@code Node}) to be removed from this sublist
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
                } else {
                    return false;
                }
                removeNode(node);
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
                if (parent() == null) return linkedNodes.isReversed();
                return parent().isReversed();
            }            
            
            /**
             * Returns reverse-order view of this sublist.
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
                return (new ReversedSubList(nodableLinkedListSubList())).linkedSubNodes();
            }
            
            /**
             * Replaces the {@code Node} at the specified position in this sublist with the
             * specified node.
             *
             * @param index index of the node to replace
             * @param node  {@code Node} to be stored at the specified position
             * @return the {@code LinkNode} previously at the specified position
             * @throws IndexOutOfBoundsException if the index is out of range
             *                                   {@code (index < 0 || index >= longSize())}
             * @throws IllegalArgumentException  if node is {@code null} or already a node
             *                                   of a list
             */
            @Override
            public LinkNode<E> set(int index, Node<E> node) {
                checkForModificationException();
                if (sizeIsKnown() && (index < 0L || index >= longSize())) {
                    throw new IndexOutOfBoundsException("index=" + index + ", size=" + longSize());
                }
                if (node == null || node.isLinked()) {
                    throw new IllegalArgumentException("Replacement Node is null or already an element of a list");           
                }
                final LinkNode<E> originalNode = get(index);
                replaceNode(originalNode, node.linkNode());
                if (node.isSubListNode()) {
                    final SubListNode<E> subListNode = (SubListNode<E>)node;
                    subListNode.setSubList(this.nodableLinkedListSubList());
                    subListNode.updateExpectedModCount();
                }
                return originalNode;
            }
            
            /**
             * Replaces the {@code SubListNode} at the specified position in this sublist
             * with the specified subListNode.
             *
             * @param index       index of the {@code SubListNode} to replace
             * @param subListNode {@code SubListNode} to be stored at the specified position
             * @return the {@code SubListNode} previously at the specified position
             * @throws IndexOutOfBoundsException if the index is out of range
             *                                   {@code (index < 0 || index >= longSize())}
             * @throws IllegalArgumentException  if subListNode is {@code null} or already a
             *                                   node of a sublist
             */
            public SubListNode<E> setSubListNode(int index, SubListNode<E> subListNode) {
                checkForModificationException();
                if (sizeIsKnown() && (index < 0L || index >= longSize())) {
                    throw new IndexOutOfBoundsException("index=" + index + ", size=" + longSize());
                }
                if (subListNode == null || subListNode.isLinked()) {
                    throw new IllegalArgumentException("Replacement subListNode is null or already an element of a sublist");           
                }
                final LinkNode<E> originalNode = get(index);
                replaceNode(originalNode, subListNode.linkNode());
                subListNode.setSubList(this.nodableLinkedListSubList());
                subListNode.updateExpectedModCount();
                return new SubListNode<E>(originalNode, this.nodableLinkedListSubList());
            }            
            
            /**
             * Appends all of the {@code Nodes} in the specified collection to the end of
             * this sublist, in the order that they are returned by the specified
             * collection's iterator.
             * 
             * <p>
             * After this operation is completed, all {@code SubListNodes} in the specified
             * collection will be marked that they are contained by this sublist.
             * 
             * <p>
             * The behavior of this operation is undefined if the specified collection is
             * modified while the operation is in progress. (Note that this will occur if
             * the specified collection is this sublist, and it's nonempty.)
             *
             * @param collection collection containing {@code Nodes} to be added to this
             *                   sublist
             * @return {@code true} if this sublist changed as a result of the call
             * @throws NullPointerException     if the specified collection is {@code null}
             * @throws IllegalArgumentException if any {@code Node} in the collection is
             *                                  {@code null} or already a node of a list
             */
            @Override
            public boolean addAll(Collection<? extends Node<E>> collection) {
                checkForModificationException();
                final long initialSize = longSize();
                final LinkNode<E> tailSentinel = getConfirmedTailSentinel();
                for (Node<E> node: collection) {
                    if (node == null || node.isLinked()) {
                        throw new IllegalArgumentException("Node in collection is null or already a node of a list");
                    }
                    addNodeBefore(node.linkNode(), tailSentinel);
                    if (node.isSubListNode()) {
                        final SubListNode<E> subListNode = (SubListNode<E>)node;
                        subListNode.setSubList(this.nodableLinkedListSubList());
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
             * 
             * <p>
             * After this operation is completed, all {@code SubListNodes} in the specified
             * collection will be marked that they are contained by this sublist.
             * 
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
             * @throws IllegalArgumentException  if any {@code Node} in the collection is
             *                                   {@code null} or already a node of a list
             * @throws IndexOutOfBoundsException if the index is out of range
             *                                   {@code (index < 0 || index > longSize())}
             * @throws NullPointerException      if the specified collection is {@code null}
             */
            @Override
            public boolean addAll(int index, Collection<? extends Node<E>> collection) {
                checkForModificationException();
                if (sizeIsKnown() && (index < 0 || index > longSize())) {
                    throw new IndexOutOfBoundsException("index=" + index + ", size=" + longSize());
                }
                boolean changed = false;            
                final LinkNode<E> targetNode = getNode(index);
                for (Node<E> node: collection) {
                    if (node == null || node.isLinked()) {
                        throw new IllegalArgumentException("Node in collection is null or already a node of a list");
                    }
                    addNodeBefore(node.linkNode(), targetNode);
                    if (node.isSubListNode()) {
                        final SubListNode<E> subListNode = (SubListNode<E>)node;
                        subListNode.setSubList(this.nodableLinkedListSubList());
                    }
                    changed = true;
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
             * sublist, before the specified targetNode. Shifts the {@code Node} currently
             * at that position and any subsequent {@code Nodes} to the right (increases
             * their indices). The new {@code Nodes} will appear in this sublist in the
             * order that they are returned by the specified collection's iterator. if the
             * specified targetNode is {@code null}, the {@code Nodes} will be appended to
             * the end of this sublist.
             * 
             * Note that {@code addAll(null, Collection)} is identical in function to
             * {@code addAll(Collection)}.
             * 
             * <p>
             * After this operation is completed, all {@code SubListNodes} in the specified
             * collection will be marked that they are contained by this sublist.
             * 
             * <p>
             * The behavior of this operation is undefined if the specified collection is
             * modified while the operation is in progress. (Note that this will occur if
             * the specified collection is this sublist, and it's nonempty.)
             *
             * @param targetNode {@code Node} the specified collection is to be inserted
             *                   before
             * @param collection collection containing {@code Nodes} to be added to this
             *                   sublist
             * @return {@code true} if this sublist changed as a result of the call
             * @throws IllegalArgumentException if targetNode is not linked to this sublist,
             *                                  or any {@code Node} in the collection is
             *                                  {@code null} or already a node of a list
             * @throws NullPointerException     if the specified collection is {@code null}
             */
            public boolean addAll(Node<E> targetNode, Collection<? extends Node<E>> collection) {
                checkForModificationException();
                if (targetNode == null) return addAll(collection);
                if (!this.contains(targetNode)) {
                    throw new IllegalArgumentException("specified targetNode is not part of this sublist");
                }
                final LinkNode<E> targetListNode = targetNode.linkNode();
                boolean changed = false;
                for (Node<E> node : collection) {
                    if (node == null || node.isLinked()) {
                        throw new IllegalArgumentException("Node in collection is null or already a node of a list");
                    }
                    addNodeBefore(node.linkNode(), targetListNode);
                    if (node.isSubListNode()) {
                        final SubListNode<E> subListNode = (SubListNode<E>) node;
                        subListNode.setSubList(this.nodableLinkedListSubList());
                    }
                    changed = true;
                }
                for (Node<E> node : collection) {
                    if (node.isSubListNode()) {
                        final SubListNode<E> subListNode = (SubListNode<E>) node;
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
             * @return {@code true} if this sublist contains the specified object
             *         ({@code Node})
             */
            @Override
            public boolean contains(Object object) {
                checkForModificationException();
                LinkNode<?> node;
                if (object instanceof LinkNode) {
                    node = (LinkNode<?>)object;
                } else if (object instanceof SubListNode) {
                    final SubListNode<?> sublistnode = (SubListNode<?>)object;
                    if (sublistnode.subList() != this.nodableLinkedListSubList()) {
                        return false;
                    }
                    node = sublistnode.linkNode();
                } else {
                    return false;
                }
                return contains(node);
            }

            private boolean contains(LinkNode<?> node) {
                if (sizeIsKnown() && tailSentinelIsKnown() &&
                        (linkedNodes.longSize() - this.longSize() < this.longSize() / 2)) {
                    // The portion of the list not occupied by the sublist is less than
                    // 1/2 the size of the sublist, therefore, it might be faster
                    // to search the portion of the list not occupied by the sublist
                    // instead of searching the sublist itself
                    if (!linkedNodes.contains(node)) return false;
                    LinkNode<E> linkNode = linkedNodes.getHeadSentinel();
                    while (linkNode != null) {
                        if (node == linkNode) return false;
                        if (linkNode == this.getHeadSentinel() || linkNode == this.getTailSentinel()) {
                            if (linkNode == this.getHeadSentinel())
                                 linkNode = this.getTailSentinel();
                            else linkNode = this.getHeadSentinel();
                            if (linkNode == linkedNodes.getTailSentinel()) break;
                            if (node == linkNode) return false;
                        }
                    linkNode = linkedNodes.getNodeAfter(linkNode);
                    }
                    return true;
                }
                return (getIndex(node) < 0L) ? false : true;
            }
            
            /**
             * Returns a view of the portion of this sublist between the specified
             * fromIndex, inclusive, and toIndex, exclusive. (If the specified fromIndex and
             * toIndex are equal, the returned {@code SubList} is empty.) The returned
             * {@code SubList} is backed by this sublist, so structural changes in the
             * returned {@code SubList} are reflected in this sublist. The returned
             * {@code SubList} supports all of the optional {@code List} operations.
             *
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
             *
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
            public SubList.LinkedSubNodes subList(int fromIndex, int toIndex) {
                return newSubList(fromIndex, toIndex).linkedSubNodes;
            }
            
            private SubList newSubList(int fromIndex, int toIndex) {
                checkForModificationException();
                if (fromIndex < 0L) throw new IndexOutOfBoundsException("fromIndex = " + fromIndex);
                if (toIndex > longSize()) throw new IndexOutOfBoundsException("toIndex(" + toIndex +") > size(" + longSize() + ")");
                if (fromIndex > toIndex) throw new IndexOutOfBoundsException("fromIndex(" + fromIndex +") > toIndex(" + toIndex + ")");
                final long size = toIndex - fromIndex;
                final LinkNode<E> headSentinel = (fromIndex == 0) ? this.getHeadSentinel() : getNode(fromIndex-1);
                final LinkNode<E> tailSentinel = (size == 0L)
                                                 ? getNodeAfterOrTailSentinel(headSentinel)
                                                 : null; // tailSentinel is unknown
                return new SubList(headSentinel, tailSentinel, SubList.this, size);
            }
            
            /**
             * Returns a view of the portion of this sublist between the specified
             * fisrtNode, and lastNode (both inclusive). The returned {@code SubList} is
             * backed by this sublist, so structural changes in the returned {@code SubList}
             * are reflected in this sublist. The returned {@code SubList} supports all of
             * the optional {@code List} operations.
             * 
             * <p>
             * If the specified firstNode is {@code null}, an empty {@code SubList},
             * positioned right before the specified lastNode, is returned. If the specified
             * lastNode is {@code null}, an empty {@code SubList}, positioned right after
             * the specified firstNode, is returned. if both the specified firstNode and
             * lastNode are {@code null}, an empty {@code SubList}, positioned at the end of
             * this sublist, is returned.
             * 
             * <p>
             * This method verifies the specified lastNode comes after the specified
             * FirstNode in this {@code SubList}. Also, the size of the returned
             * {@code SubList} is known.
             * 
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
             *
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
             *                                  sublist or if the lastNode comes before the
             *                                  firstNode in this sublist.
             */
            public SubList.LinkedSubNodes subList(Node<E> firstNode, Node<E> lastNode) {
                return newSubList(firstNode, lastNode).linkedSubNodes;
            }
            
            private SubList newSubList(Node<E> firstNode, Node<E> lastNode) {
                checkForModificationException();
                if (firstNode == null && lastNode == null) {
                    // both firstNode and lastNode are null
                    final LinkNode<E> tailSentinel = getConfirmedTailSentinel();
                    return new SubList(getNodeBeforeOrHeadSentinel(tailSentinel), tailSentinel, SubList.this, 0L);
                } else if (firstNode == null) {
                    // only the lastNode is specified
                    if (!contains(lastNode)) {
                        throw new IllegalArgumentException("Specified last node is not linked to this sublist");
                    }
                    return new SubList(getNodeBeforeOrHeadSentinel(lastNode.linkNode()), lastNode.linkNode(), SubList.this, 0L);
                } else if (lastNode == null) {
                    // only the firstNode is specified
                    if (!contains(firstNode)) {
                        throw new IllegalArgumentException("Specified first node is not linked to this sublist");
                    }
                    return new SubList(firstNode.linkNode(), getNodeAfterOrTailSentinel(firstNode.linkNode()), SubList.this, 0L);
                }
                // both firstNode and LastNode are specified
                if (!linkedNodes.contains(firstNode)) {
                    throw new IllegalArgumentException("Specified first node is not linked to this list");
                }
                if (!linkedNodes.contains(lastNode)) {
                    throw new IllegalArgumentException("Specified last node is not linked to this list");
                }
                LinkNode<E> node;
                long subListSize = 0;
                boolean foundFirstNode = false;
                boolean foundLastNode = false;
                final LinkNode<E> firstLinkNode = firstNode.linkNode();
                final LinkNode<E> lastLinkNode = lastNode.linkNode();
                if (sizeIsKnown()) {
                    long remaining = longSize();
                    for (node = getNodeAfterFromListWithKnownTailSentinel(getHeadSentinel()); remaining > 0;
                            node = getNodeAfterFromListWithKnownTailSentinel(node), remaining--) {    
                        if (node == firstLinkNode) foundFirstNode = true;
                        if (foundFirstNode) subListSize++;
                        if (node == lastLinkNode) {
                            foundLastNode = true;
                            break;
                        }
                    }
                    if (remaining == 0) this.setTailSentinel(node);
                    if (remaining == 1) this.setTailSentinel(getNodeAfterFromListWithKnownTailSentinel(node));
                } else {
                    long listSize = 0;
                    for (node = getNodeAfterOrTailSentinel(getHeadSentinel()); node != getTailSentinel();
                            node = getNodeAfterOrTailSentinel(node)) {
                        listSize++;
                        if (node == firstLinkNode) foundFirstNode = true;
                        if (foundFirstNode) subListSize++;
                        if (node == lastLinkNode) {
                            foundLastNode = true;
                            break;
                        }
                    }
                    if (node == getTailSentinel()) this.setSize(listSize);
                    else if (getNodeAfterOrTailSentinel(node) == getTailSentinel()) this.setSize(listSize + 1L);
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
                return new SubList(getNodeBeforeOrHeadSentinel(firstLinkNode),
                        getNodeAfterOrTailSentinel(lastLinkNode), SubList.this, subListSize);
            }

            /**
             * Sorts this sublist according to the order induced by the specified
             * comparator.
             *
             * <p>
             * The specified comparator compares the {@code Nodes} not the elements of the
             * {@code Nodes}. for example:
             * {@code sort((node1, node2) -> { return node1.compareTo(node2); });}, or
             * {@code sort((node1, node2) -> { return
             * node1.element().compareTo(node2.element()); });}.
             *
             * If the specified comparator is {@code null} then all elements in this sublist
             * must implement the {@code Comparable} interface and the elements' natural
             * ordering should be used.
             *
             * <p>
             * <strong>Implementation Specification:</strong> This implementation obtains an
             * array containing all nodes in this sublist, sorts the array using
             * {@code Arrays.sort(T[] a, Comparator<? super T> c)}, and then effectively
             * clears the sublist and puts the sorted nodes from the array back into this
             * sublist in order. If this sublist's {@code size > Integer.MAX_VALUE-8}, a
             * {@link #mergeSort} is performed.
             * 
             * <p>
             * <strong>Implementation Note:</strong> This implementation is a stable,
             * adaptive, iterative mergesort that requires far fewer than n lg(n)
             * comparisons when the input array is partially sorted, while offering the
             * performance of a traditional mergesort when the input array is randomly
             * ordered. If the input array is nearly sorted, the implementation requires
             * approximately n comparisons. Temporary storage requirements vary from a small
             * constant for nearly sorted input arrays to n/2 object references for randomly
             * ordered input arrays.
             *
             * <p>
             * The implementation takes equal advantage of ascending and descending order in
             * its input array, and can take advantage of ascending and descending order in
             * different parts of the same input array. It is well-suited to merging two or
             * more sorted arrays: simply concatenate the arrays and sort the resulting
             * array.
             *
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
             * @throws ClassCastException if the sublist contains elements that are not
             *                            <i>mutually comparable</i> using the specified
             *                            comparator
             */
            @Override
            public void sort(Comparator<? super Node<E>> comparator) {
                checkForModificationException();
                if (longSize() < 2L) return;
                if (longSize() > Integer.MAX_VALUE-8) { mergeSort(comparator); return; }
                @SuppressWarnings("unchecked")
                final LinkNode<E>[] sortedNodes = this.toArray(new LinkNode[0]);
                Arrays.sort(sortedNodes, comparator);
                LinkNode<E> node = getFirstNode();
                for (LinkNode<E> sortedNode: sortedNodes) {
                    LinkNode.swapNodes(node, sortedNode);
                    node = getNodeAfter(sortedNode); // node = node.next() (effectively)
                                                     // sortedNode has replaced node's position in the list
                }
                updateModCounts();
            }
            
            /**
             * Sorts this sublist according to the order induced by the specified
             * comparator.
             *
             * <p>
             * The specified comparator compares the {@code Nodes} not the elements of the
             * {@code Nodes}. for example:
             * {@code sort((node1, node2) -> { return node1.compareTo(node2); });}, or
             * {@code sort((node1, node2) -> { return
             * node1.element().compareTo(node2.element()); });}.
             *
             * If the specified comparator is {@code null} then all elements in this sublist
             * must implement the {@code Comparable} interface and the elements' natural
             * ordering should be used.
             * 
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
             * @throws ClassCastException if the sublist contains elements that are not
             *                            <i>mutually comparable</i> using the specified
             *                            comparator
             */
            public void mergeSort(Comparator<? super Node<E>> comparator) {
                checkForModificationException();
                NodableLinkedList.mergeSort(this, comparator);
                updateModCounts();
            }
            
            /**
             * Returns a {@code ListIterator} of the {@code Nodes} in this sublist (in
             * proper sequence), starting at the specified position in this sublist. Obeys
             * the general contract of {@code List.listIterator(int)}.
             * 
             * <p>
             * <strong>Implementation Note:</strong> The {@code ListIterator} returned by
             * this method behaves differently when the sublist's
             * {@code size > Integer.MAX_VALUE}. Methods {@code nextIndex} and
             * {@code previousIndex} return -1 if the {@code index > Integer_MAX_VALUE}.
             *
             * <p>
             * The {@code ListIterator} is <i>fail-fast</i>: if the
             * {@code NodableLinkedList} is structurally modified at any time after the
             * Iterator is created, in any way except through the {@code ListIterator's} own
             * {@code remove} or {@code add} methods, the {@code ListIterator} will throw a
             * {@code ConcurrentModificationException}. Thus, in the face of concurrent
             * modification, the iterator fails quickly and cleanly, rather than risking
             * arbitrary, non-deterministic behavior at an undetermined time in the future.
             * 
             * <p>
             * Instead of using a {@code ListIterator}, consider iterating over the
             * sublist via {@code SubListNodes}. For example:
             * <pre>
             * {@code
             *     // sublist is a NodableLinkedList<Integer>.SubList.LinkedSubNodes
             *     Node<Integer> subListNode = sublist.getSubListNode(index);
             *     //                       or sublist.nodableLinkedListSubList().getFirstSubListNode();
             *     while (subListNode != null) {
             *         System.out.println(subListNode.element());
             *         subListNode = subListNode.next();
             *     }
             * }
             * </pre>
             *
             * @param index index of the first {@code Node} to be returned from the
             *              {@code ListIterator} (by a call to {@code next})
             * @return a ListIterator of the {@code Nodes} in this sublist (in proper
             *         sequence), starting at the specified position in this sublist
             * @throws IndexOutOfBoundsException if the index is out of range
             *                                   {@code (index < 0 || index > longSize())}
             * @see List#listIterator(int)
             */
            @Override
            public ListIterator<Node<E>> listIterator(int index) {
                if (index < 0) throw new IndexOutOfBoundsException("index=" + index);
                return linkedSubNodesListIterator(index);
            }
            
            private LinkedNodesListIterator linkedSubNodesListIterator(long index) {
                checkForModificationException();
                LinkNode<E> node;
                if (index < 0L) {
                    index = longSize();
                    node = tailSentinel();
                } else {
                    node = getNode(index);
                }
                return new LinkedNodesListIterator(this, this.getSize(), index, IndexType.ABSOLUTE,
                        node, this.getHeadSentinel(), knownTailSentinel());
            }
            
            /**
             * Returns a {@code ListIterator} of the {@code Nodes} in this sublist (in
             * proper sequence), starting at the specified node in this sublist. if the
             * specified node is {@code null}, the {@code ListIterator} will be positioned
             * right after the last {@code Node} in this sublist.
             * 
             * <p>
             * <strong>Implementation Note:</strong> The index returned by the returned
             * {@code ListIterator's} methods {@code nextIndex} and {@code previousIndex} is
             * relative to the specified node which has an index of zero. Nodes which come
             * before the specified node in this sublist, will have a negative index; nodes
             * that come after will have a positive index. Method {@code nextIndex} returns
             * {@code longSize()} if at the end of the sublist, and method
             * {@code previousIndex} returns {@code -longSize()} if at the beginning of the
             * sublist. if {@code index < Integer.MIN_VALUE or index > Integer.MAX_VALUE},
             * {@code Integer.MIN_VALUE} or {@code Integer.MAX_VALUE} is returned
             * respectively.
             *
             * <p>
             * The {@code ListIterator} is <i>fail-fast</i>: if the
             * {@code NodableLinkedList} is structurally modified at any time after the
             * Iterator is created, in any way except through the {@code ListIterator's} own
             * {@code remove} or {@code add} methods, the {@code ListIterator} will throw a
             * {@code ConcurrentModificationException}. Thus, in the face of concurrent
             * modification, the iterator fails quickly and cleanly, rather than risking
             * arbitrary, non-deterministic behavior at an undetermined time in the future.
             * 
             * <p>
             * Instead of using a {@code ListIterator}, consider iterating over the
             * sublist via {@code SubListNodes}. For example:
             * <pre>
             * {@code
             *     // sublist is a NodableLinkedList<Integer>.SubList.LinkedSubNodes
             *     Node<Integer> subListNode = node.subListNode(sublist.nodableLinkedListSubList());
             *     while (subListNode != null) {
             *         System.out.println(subListNode.element());
             *         subListNode = subListNode.next();
             *     }
             * }
             * </pre>
             *
             * @param node first {@code Node} to be returned from the {@code ListIterator}
             *             (by a call to {@code next})
             * @return a ListIterator of the {@code Nodes} in this sublist (in proper
             *         sequence), starting at the specified node in the sublist
             * @throws IllegalArgumentException if node is not linked to this sublist
             */
            public ListIterator<Node<E>> listIterator(Node<E> node) {
                if (node != null && !linkedNodes.contains(node)) {
                    throw new IllegalArgumentException("Specified node is not linked to this list");
                }
                return linkedSubNodesListIterator(node);
            }            
            
            private LinkedNodesListIterator linkedSubNodesListIterator(Node<E> node) {
                checkForModificationException();
                if (node == null) {
                    return new LinkedNodesListIterator(this, this.getSize(), 0L, IndexType.RELATIVE,
                            getConfirmedTailSentinel(), this.getHeadSentinel(), getConfirmedTailSentinel());
                }
                if (!this.contains(node)) {
                    throw new IllegalArgumentException("specified node is not part of this sublist");
                }
                return new LinkedNodesListIterator(this, this.getSize(), 0L, IndexType.RELATIVE,
                        node.linkNode(), this.getHeadSentinel(), knownTailSentinel());
            }
            
            private LinkNode<E> knownTailSentinel() {
                LinkedSubNodes sublist = this;
                do {
                    if (sublist.getTailSentinel() != null) return sublist.getTailSentinel();
                    sublist = sublist.parent();
                } while (sublist != null);
                return getMyLinkedNodesTailSentinel();
            }
            
        } // LinkedSubNodes

        private class SubListIterator implements ListIterator<E> {
            
            private final LinkedNodesListIterator listIterator;
            
            private SubListIterator(long index) {
                listIterator = linkedSubNodes.linkedSubNodesListIterator(index);
            }
            
            private SubListIterator(Node<E> node) {
                listIterator = linkedSubNodes.linkedSubNodesListIterator(node);
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
                checkForModificationException();
                if (action == null) throw new NullPointerException();
                while (hasNext()) {
                    action.accept(next());
                }
                checkForModificationException();
            }
            
        } // SubListIterator
        
        private class ReversedLinkedSubNodes extends LinkedSubNodes {
            
            private SubList.LinkedSubNodes linkedSubNodes;
            
            private ReversedLinkedSubNodes(LinkedSubNodes linkedSubNodes) {
                this.linkedSubNodes = linkedSubNodes;
                super.updateModCount();
            }
            
            //protected int modCount inherited from class java.util.AbstractList
            @Override
            int modCount() {
                return linkedSubNodes.modCount();
            }
            
            @Override
            void updateModCount() {
                linkedSubNodes.updateModCount();
                super.updateModCount();
            }
            
            private void syncModCountWithBase() {
                SubList.this.modCount = this.modCount = modCount(); // try to keep all modCounts in sync
            }
            
            @Override
            LinkNode<E> getHeadSentinel() {
                syncModCountWithBase();
                return linkedSubNodes.getTailSentinel();
            }
            
            @Override
            LinkNode<E> getTailSentinel() {
                syncModCountWithBase();
                return linkedSubNodes.getHeadSentinel();
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
            long getSize() {
                syncModCountWithBase();
                return linkedSubNodes.getSize();
            }
            
            @Override
            void setSize(long size) {
                syncModCountWithBase();
                linkedSubNodes.setSize(size);
            }
            
            @Override
            LinkNode<E> getMyLinkedNodesHeadSentinel() {
                syncModCountWithBase();
                return linkedSubNodes.getMyLinkedNodesTailSentinel();
            }
            
            @Override
            LinkNode<E> getMyLinkedNodesTailSentinel() {
                syncModCountWithBase();
                return linkedSubNodes.getMyLinkedNodesHeadSentinel();
            }
            
            @Override
            LinkNode<E> tailSentinel() {
                syncModCountWithBase();
                return linkedSubNodes.getHeadSentinel();
            }
            
            @Override
            LinkNode<E> getConfirmedTailSentinel() {
                syncModCountWithBase();
                return linkedSubNodes.getHeadSentinel();
            }
            
            @Override
            void addNodeAfter(LinkNode<E> node, LinkNode<E> afterThisNode) {
                linkedSubNodes.addNodeBefore(node, afterThisNode);
                syncModCountWithBase();
            }

            @Override
            void addNodeBefore(LinkNode<E> node, LinkNode<E> beforeThisNode) {
                linkedSubNodes.addNodeAfter(node, beforeThisNode);
                syncModCountWithBase();
            }

            @Override
            void removeNode(LinkNode<E> node) {
                linkedSubNodes.removeNode(node);
                syncModCountWithBase();
            }

            @Override
            void replaceNode(LinkNode<E> node, LinkNode<E> replacementNode) {
                linkedSubNodes.replaceNode(node, replacementNode);
                syncModCountWithBase();
            }

            @Override
            boolean hasNodeAfter(LinkNode<E> node) {
                syncModCountWithBase();
                return linkedSubNodes.hasNodeBefore(node);           
            }

            @Override
            boolean hasNodeBefore(LinkNode<E> node) {
                syncModCountWithBase();
                return linkedSubNodes.hasNodeAfter(node);           
            }
            
            @Override
            LinkNode<E> getFirstNode() {
                syncModCountWithBase();
                return linkedSubNodes.getLastNode();
            }
            
            @Override
            LinkNode<E> getLastNode() {
                syncModCountWithBase();
                return linkedSubNodes.getFirstNode();
            }
            
            @Override
            LinkNode<E> getNodeAfter(LinkNode<E> node) {
                syncModCountWithBase();
                return linkedSubNodes.getNodeBefore(node);
            }
            
            @Override
            LinkNode<E> getNodeAfterOrTailSentinel(LinkNode<E> node) {
                syncModCountWithBase();
                return linkedSubNodes.getNodeBeforeOrHeadSentinel(node);
            }
            
            @Override
            LinkNode<E> getNodeAfterFromListWithKnownTailSentinel(LinkNode<E> node) {
                syncModCountWithBase();
                return linkedSubNodes.getNodeBeforeOrHeadSentinel(node);
            }
            
            @Override
            LinkNode<E> getNodeBefore(LinkNode<E> node) {
                syncModCountWithBase();
                return linkedSubNodes.getNodeAfter(node);
            }
            
            @Override
            LinkNode<E> getNodeBeforeOrHeadSentinel(LinkNode<E> node) {
                syncModCountWithBase();
                return linkedSubNodes.getNodeAfterOrTailSentinel(node);
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
        
    } // SubList
    
    private class ReversedSubList extends SubList {
        
        private SubList sublist;
        
        private ReversedSubList(SubList sublist) {
            super(sublist.linkedSubNodes());
            this.sublist = sublist;
        }
        
        @Override
        public NodableLinkedList<E>.SubList reversed() {
            return this.sublist;
        }
        
    } // ReversedSubList
    
    private static class Reversed<E> extends NodableLinkedList<E> implements java.io.Externalizable {
        
        private NodableLinkedList<E> nodableLinkedList;
        
        private Reversed(NodableLinkedList<E> nodableLinkedList) {
            super(nodableLinkedList.linkedNodes());
            this.nodableLinkedList = nodableLinkedList;
        }
        
        @Override
        int modCount() {
            return nodableLinkedList.modCount();
        }
        
        @Override
        public LinkNode<E> getFirstNode() {
            final LinkNode<E> linkNode = linkedNodes().getFirstNode();
            return (linkNode == null) ? null : linkNode.reversed();
        }
        
        @Override
        public LinkNode<E> getLastNode() {
            final LinkNode<E> linkNode = linkedNodes().getLastNode();
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
        
    } // Reversed
        
    private class ReversedLinkedNodes extends LinkedNodes {
        
        private LinkedNodes linkedNodes;
        
        private ReversedLinkedNodes(LinkedNodes linkedNodes) {
            this.linkedNodes = linkedNodes;
        }
        
        //protected int modCount inherited from class java.util.AbstractList
        @Override
        int modCount() {
            return linkedNodes.modCount();
        }
        
        private void updateModCount() {
            NodableLinkedList.this.modCount = this.modCount = modCount(); // try to keep all modCounts in sync
        }
        
        @Override
        public long longSize() {
            updateModCount();
            return linkedNodes.longSize();
        }            
        
        @Override
        void addNodeAfter(LinkNode<E> node, LinkNode<E> afterThisNode) {
            linkedNodes.addNodeBefore(node, afterThisNode);
            updateModCount();
        }
    
        @Override
        void addNodeBefore(LinkNode<E> node, LinkNode<E> beforeThisNode) {
            linkedNodes.addNodeAfter(node, beforeThisNode);
            updateModCount();
        }
    
        @Override
        void removeNode(LinkNode<E> node) {
            linkedNodes.removeNode(node);
            updateModCount();
        }
    
        @Override
        void replaceNode(LinkNode<E> node, LinkNode<E> replacementNode) {
            linkedNodes.replaceNode(node, replacementNode);
            updateModCount();
        }
    
        @Override
        boolean hasNodeAfter(LinkNode<E> node) {
            updateModCount();
            return linkedNodes.hasNodeBefore(node);           
        }
    
        @Override
        boolean hasNodeBefore(LinkNode<E> node) {
            updateModCount();
            return linkedNodes.hasNodeAfter(node);           
        }
        
        @Override
        LinkNode<E> getHeadSentinel() {
            updateModCount();
            return linkedNodes.getTailSentinel();
        }
        
        @Override
        LinkNode<E> getTailSentinel() {
            updateModCount();
            return linkedNodes.getHeadSentinel();
        }
        
        @Override
        LinkNode<E> getMyLinkedNodesHeadSentinel() {
            return linkedNodes.getMyLinkedNodesTailSentinel();
        }
        
        @Override
        LinkNode<E> getMyLinkedNodesTailSentinel() {
            return linkedNodes.getMyLinkedNodesHeadSentinel();
        }
        
        @Override
        LinkNode<E> getFirstNode() {
            updateModCount();
            return linkedNodes.getLastNode();
        }
        
        @Override
        LinkNode<E> getLastNode() {
            updateModCount();
            return linkedNodes.getFirstNode();
        }        
    
        @Override
        LinkNode<E> getNodeAfter(LinkNode<E> node) {
            updateModCount();
            return linkedNodes.getNodeBefore(node);
        }
    
        @Override
        LinkNode<E> getNodeAfterOrTailSentinel(LinkNode<E> node) {
            updateModCount();
            return linkedNodes.getNodeBeforeOrHeadSentinel(node);
        }
        
        @Override
        LinkNode<E> getNodeAfterFromListWithKnownTailSentinel(LinkNode<E> node) {
            updateModCount();
            return linkedNodes.getNodeBeforeOrHeadSentinel(node);
        }
    
        @Override
        LinkNode<E> getNodeBefore(LinkNode<E> node) {
            updateModCount();
            return linkedNodes.getNodeAfter(node);
        }
        
        @Override
        LinkNode<E> getNodeBeforeOrHeadSentinel(LinkNode<E> node) {
            updateModCount();
            return linkedNodes.getNodeAfterOrTailSentinel(node);
        }
        
        @Override
        boolean contains(Node<?> node) {
             return (node != null && node.linkedNodes() == linkedNodes) ? true : false;
        }
        
        @Override
        public LinkNode<E> forwardLinkNode(Node<E> node) {
            return linkedNodes.reverseLinkNode(node);
        }
        
        @Override
        public LinkNode<E> reverseLinkNode(Node<E> node) {
            return linkedNodes.forwardLinkNode(node);
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
     * Sublist node of a {@code NodableLinkedList.SubList}. A {@code SubListNode}
     * represents a {@code NodableLinkedList.LinkNode} that is associated with a
     * specific {@code SubList}. Any operation performed on a {@code SubListNode} is
     * performed on its associated {@code SubList}. A {@code SubListNode} can be
     * removed from its current {@code SubList} and added to a different
     * {@code SubList}.
     * 
     * <p>
     * <b>Performance Consideration:</b> Unlike operations on a {@code LinkNode},
     * operations on a {@code SubListNode} are not necessarily performed in constant
     * time because it may be necessary to verify, in linear time, that the
     * {@code SubListNode} is still a node of its associated {@code SubList}. If the
     * {@code NodableLinkedList} which contains the {@code SubListNode}, is
     * structurally modified in anyway except via the {@code SubListNode}, the
     * {@code SubListNode} is invalidated and it will be necessary to verify that
     * the {@code SubListNode} is still contained by its associated {@code SubList}
     * the next time the {@code SubListNode} is used.
     * 
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
        
        private final LinkNode<E> linkNode; // never a ReverseLinkNode
        private NodableLinkedList<E>.SubList subList;
        
        private int expectedModCount;
        
        private SubListNode() {
            this.linkNode = null;
            this.subList = null;
        }
        
        private SubListNode(LinkNode<E> linkNode, NodableLinkedList<E>.SubList subList) {
            this.linkNode = (linkNode.isReversed()) ? linkNode.reversed() : linkNode;
            this.subList = subList;
            updateExpectedModCount();
        }
        
        void setSubList(NodableLinkedList<E>.SubList subList) {
            this.subList = subList;
        }
        
        void updateExpectedModCount() {
            this.expectedModCount = subList().nodableLinkedList().modCount();
        }
        
        boolean isStillNodeOfSubList() {
            if (expectedModCount == subList().nodableLinkedList().modCount() && isLinked()) {
                return true;
            }
            if (!subList().linkedSubNodes().contains(linkNode())) return false;
            updateExpectedModCount();
            return true;
        }        
                    
        private void checkIfStillNodeOfSubList() {
            if (!isStillNodeOfSubList()) {
                throw new IllegalStateException("This SubListNode is no longer a node of its associated sublist");
            }
        }
        
        private void checkIfArgStillNodeOfSubList() {
            if (!isStillNodeOfSubList()) {
                throw new IllegalArgumentException("The specified subListNode is no longer a node of its associated sublist");
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
         * @return the {@code LinkedNodes} list this {@code SubListrNode} belongs to, or
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
         * Returns the {@code NodableLinkedList.SubList} this {@code SubListNode}
         * belongs to.
         * 
         * <p>
         * Note, even if this {@code SubListNode} is unlinked, this {@code SubListNode}
         * is still associated with a {@code NodableLinkedList.SubList}. In other words,
         * a {@code null} value is never returned.
         * 
         * @return the {@code NodableLinkedList.SubList} this {@code SubListNode}
         *         belongs to
         */
        @Override
        public NodableLinkedList<E>.SubList subList() {
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
         * 
         * <p>
         * If the specified node is a {@code SubListNode}, it is verified that it is
         * still contained by its associated {@code SubList}. Also, this
         * {@code SubListNode} is effectively inserted into the specified
         * {@code SubListNode's} associated {@code SubList}.
         * 
         * <p>
         * If this {@code SubListNode} is reversed, this operation behaves like the
         * {@code addBefore(Node)} method.
         * 
         * @param node the {@code Node} this {@code SubListNode} is to be inserted after
         * 
         * @throws IllegalStateException    if this {@code SubListNode} is already a
         *                                  node of a list
         * @throws IllegalArgumentException if node is {@code null} or not a node of a
         *                                  list
         */
        @Override
        public void addAfter(Node<E> node) {
            if (node == null || !node.isLinked()) {
                throw new IllegalArgumentException("Specified node is null or not a node of a list");
            }
            if (this.isLinked()) {
                throw new IllegalStateException("This sublist node is already a node of a list");
            }
            if (node.isSubListNode()) {
                final SubListNode<E> subListNode = (SubListNode<E>)node;
                subListNode.subList().checkForModificationException();
                subListNode.checkIfArgStillNodeOfSubList();
                subList().linkedSubNodes().addNodeAfter(linkNode(), node.linkNode());
                subListNode.updateExpectedModCount();
                this.setSubList(subListNode.subList());
                updateExpectedModCount();
            } else {
                subList().linkedSubNodes().addNodeAfter(linkNode(), node.linkNode()); 
            }
        }
        
        /**
         * Inserts this {@code SubListNode} before the specified node. This
         * {@code SubListNode} must not already belong to a list, and the specified node
         * must belong to a list.
         * 
         * <p>
         * If the specified node is a {@code SubListNode}, it is verified that it is
         * still contained by its associated {@code SubList}. Also, this
         * {@code SubListNode} is effectively inserted into the specified
         * {@code SubListNode's} associated {@code SubList}.
         * 
         * <p>
         * If this {@code SubListNode} is reversed, this operation behaves like the
         * {@code addAfter(Node)} method.
         * 
         * @param node the {@code Node} this {@code SubListNode} is to be inserted
         *             before
         * @throws IllegalStateException    if this {@code SubListNode} is already a
         *                                  node of a list
         * @throws IllegalArgumentException if node is {@code null} or not a node of a
         *                                  list
         */
        @Override
        public void addBefore(Node<E> node) {
            if (node == null || !node.isLinked()) {
                throw new IllegalArgumentException("Specified node is null or not a node of a list");
            }
            if (this.isLinked()) {
                throw new IllegalStateException("This sublist node is already a node of a list");
            }
            if (node.isSubListNode()) {
                final SubListNode<E> subListNode = (SubListNode<E>)node;
                subListNode.subList().checkForModificationException();
                subListNode.checkIfArgStillNodeOfSubList();
                subList().linkedSubNodes().addNodeBefore(linkNode(), node.linkNode());
                subListNode.updateExpectedModCount();
                this.setSubList(subListNode.subList());
                updateExpectedModCount();
            } else {
                subList().linkedSubNodes().addNodeBefore(linkNode(), node.linkNode());  
            }
        }

        /**
         * Returns {@code true} if there exists a node which comes after this
         * {@code SubListNode} in a {@code SubList}. In other words, returns
         * {@code true} if this {@code SubListNode} is not the last node of a
         * {@code SubList}.
         * 
         * <p>
         * If this {@code SubListNode} is reversed, this operation behaves like the
         * {@code hasPrevious()} method.
         * 
         * @return {@code true} if there exists a node which comes after this
         *         {@code SubListNode}
         * @throws IllegalStateException if this {@code SubListNode} is no longer a node
         *                               of its sublist
         */
        @Override
        public boolean hasNext() {
            subList().checkForModificationException();
            checkIfStillNodeOfSubList();
            return subList().linkedSubNodes().hasNodeAfter(linkNode());
        }

        /**
         * Returns {@code true} if there exists a node which comes before this
         * {@code SubListNode} in a {@code SubList}. In other words, returns
         * {@code true} if this {@code SubListNode} is not the first node of a
         * {@code SubList}.
         * 
         * <p>
         * If this {@code SubListNode} is reversed, this operation behaves like the
         * {@code hasNext()} method.
         * 
         * @return {@code true} if there exists a node which comes before this
         *         {@code SubListNode}
         * @throws IllegalStateException if this {@code SubListNode} is no longer a node
         *                               of its sublist
         */
        @Override
        public boolean hasPrevious() {
            subList().checkForModificationException();
            checkIfStillNodeOfSubList();
            return subList().linkedSubNodes().hasNodeBefore(linkNode());
        }

        /**
         * Returns the index of this {@code SubListNode} in its {@code SubList}, or -1
         * if this {@code SubListNode} does not belong to a {@code SubList} or the
         * {@code index > Integer.MAX_VALUE}.
         * 
         * <p>
         * If this {@code SubListNode} is reversed, the index returned is relative from
         * the end of the sublist (the last node has an index of zero, the second to
         * last node has an index of 1, etc.).
         * 
         * <p>
         * <b>Performance Consideration:</b> This operation is performed in linear time.
         *
         * @return the index of this {@code SubListNode} in its {@code SubList}, or -1
         *         if this {@code SubListNode} does not belong to a {@code SubList} or
         *         the {@code index > Integer.MAX_VALUE}.
         */
        @Override
        public int index() {
            subList().checkForModificationException();
            final long index = subList().linkedSubNodes().getIndex(linkNode());
            return (index > Integer.MAX_VALUE) ? -1 : (int)index;
        }

        /**
         * Returns the {@code SubListNode} which comes after this {@code SubListNode} in
         * a {@code SubList}. if this {@code SubListNode} is the last or only node,
         * {@code null} is returned.
         * 
         * <p>
         * If this is a reversed {@code SubListNode}, this operation behaves like the
         * {@code previous()} method, and the returned {@code SubListNode} will also be
         * reversed.
         * 
         * @return the {@code SubListNode} which comes after this {@code SubListNode} in
         *         a {@code SubList}, or {@code null} if this {@code SubListNode} is the
         *         last or only node
         * @throws IllegalStateException if this {@code SubListNode} is no longer a node
         *                               of its sublist
         */
        @Override
        public SubListNode<E> next() {
            subList().checkForModificationException();
            checkIfStillNodeOfSubList();
            final LinkNode<E> node = subList().linkedSubNodes().getNodeAfter(linkNode());
            if (node == null) return null;
            final SubListNode<E> subListNode = new SubListNode<E>(node, subList());
            return (this.isReversed()) ? subListNode.reversed() : subListNode;
        }

        /**
         * Returns the {@code SubListNode} which comes before this {@code SubListNode}
         * in a {@code SubList}. if this {@code SubListNode} is the first or only node,
         * {@code null} is returned.
         * 
         * <p>
         * If this is a reversed {@code SubListNode}, this operation behaves like the
         * {@code next()} method, and the returned {@code SubListNode} will also be
         * reversed.
         * 
         * @return the {@code SubListNode} which comes before this {@code SubListNode}
         *         in a {@code SubList}, or {@code null} if this {@code SubListNode} is
         *         the first or only node
         * @throws IllegalStateException if this {@code SubListNode} is no longer a node
         *                               of its sublist
         */
        @Override
        public SubListNode<E> previous() {
            subList().checkForModificationException();
            checkIfStillNodeOfSubList();
            final LinkNode<E> node = subList().linkedSubNodes().getNodeBefore(linkNode());
            if (node == null) return null;
            final SubListNode<E> subListNode = new SubListNode<E>(node, subList());
            return (this.isReversed()) ? subListNode.reversed() : subListNode;
        }

        /**
         * Removes this {@code SubListNode} from the {@code SubList} it is linked to.
         * 
         * @throws IllegalStateException if this {@code SubListNode} is no longer a node
         *                               of its sublist
         */
        @Override
        public void remove() {
            subList().checkForModificationException();
            checkIfStillNodeOfSubList();
            subList().linkedSubNodes().removeNode(linkNode());
        }

        /**
         * Replaces this {@code SubListNode} with the specified node. This
         * {@code SubListNode} must belong to a {@code SubList}, and the specified node
         * must not belong to a list.
         * 
         * <p>
         * If the specified node is a {@code SubListNode}, after this operation is
         * completed, The specified {@code SubListNode} will be marked that it is
         * contained by its new associated {@code SubList}.
         * 
         * @param node {@code Node} to replace this {@code SubListNode}
         * @throws IllegalStateException    if this {@code SubListNode} is no longer a
         *                                  node of its sublist
         * @throws IllegalArgumentException if node is {@code null} or already a node of
         *                                  a list
         */
        @Override
        public void replaceWith(Node<E> node) {
            if (node == null || node.isLinked()) {
                throw new IllegalArgumentException("Specified node is null or already a node of a list");
            }
            subList().checkForModificationException();
            checkIfStillNodeOfSubList();
            subList().linkedSubNodes().replaceNode(linkNode(), node.linkNode());
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
         * {@code hasNext}, {@code hasPrevious}, {@code next}, {@code previous}, and
         * {@code index} also operate in a reverse way.
         * 
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
         * can be different lists.
         * 
         * <p>
         * If the specified node is a {@code SubListNode}, it is verified that it is
         * still contained by its associated {@code SubList}. Also, after this operation
         * is completed, both this {@code SubListNode} and the specified
         * {@code SubListNode} will be marked that they are contained by their possibly
         * new associated {@code SubLists}. If the specified node is a {@code LinkNode},
         * it is effectively inserted into this {@code SubListNode's} associated
         * {@code SubList}.
         * 
         * <p>
         * <strong>Synchronization consideration:</strong> This operation can
         * potentially operate on two different lists. if synchronization is required,
         * both lists should be synchronized by the same object.
         * 
         * @param node the {@code Node} to swap with this {@code SubListNode}
         * @throws IllegalStateException    if this {@code SubListNode} is no longer a
         *                                  node of its sublist
         * @throws IllegalArgumentException if node is {@code null} or not linked
         */
        @Override
        public void swapWith(Node<E> node) {
            if (node == null || !node.isLinked()) {
                throw new IllegalArgumentException("The specified node is null or not a node of a list");
            }
            subList().checkForModificationException();
            checkIfStillNodeOfSubList();
            if (node.isSubListNode()) {
                final SubListNode<E> subListNode = (SubListNode<E>)node;
                final NodableLinkedList<E>.SubList thatSubList = subListNode.subList();
                thatSubList.checkForModificationException();
                subListNode.checkIfArgStillNodeOfSubList();
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
         * 
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
        public SubListNode<E> subListNode(NodableLinkedList<E>.SubList subList) {
            if (subList == null) throw new IllegalArgumentException("Specified SubList is null");
            subList.checkForModificationException();
            if (this.isLinked() && !subList.linkedSubNodes().contains(this.linkNode())) {
                throw new IllegalStateException("This SubListNode's LinkNode is not a node of the specified subList");
            }
            return new SubListNode<E>(this.linkNode(), subList());
        }

        /**
         * Returns a new unverified {@code SubListNode}, backed by this
         * {@code SubListNode's} {@code LinkNode}, for the specified subList. The
         * {@code LinkNode} which backs the returned {@code SubListNode}, must be a node
         * of the specified subList, or unlinked.
         * 
         * <p>
         * <b>Use with CAUTION:</b> This {@code SubListNode}, if linked, is not verified
         * that it is a node of the specified subList. The results of using the returned
         * {@code SubListNode} are unpredictable if this {@code SubListNode} is not a
         * node of the specified sublist. This method is provided to avoid the cost of
         * verifying that a {@code SubListNode} is a node of the specified subList when
         * it's certain that it is. For example, nodes returned by a sublist's iterator
         * don't need to be verified. Note, just like any {@code SubListNode}, if the
         * {@code NodableLinkedList} is subsequently modified, the returned
         * {@code SubListNode}, will have to be reverified.
         * 
         * @param subList {@code SubList} containing this {@code SubListNode's}
         *                {@code LinkNode}
         * @return a new unverified {@code SubListNode}, backed by this
         *         {@code SubListNode's} {@code LinkNode}, for the specified subList
         * @throws IllegalArgumentException if the specified subList is {@code null}
         */
        @Override
        public SubListNode<E> unverifiedSubListNode(NodableLinkedList<E>.SubList subList) {
            if (subList == null) {
                throw new IllegalArgumentException("Specified SubList is null");
            }
            subList.checkForModificationException();
            return new SubListNode<E>(this.linkNode(), subList());
        }        
        
        /**
         * Returns (and marks) this {@code SubListNode} as a {@code SubListNode} which
         * has just been verified that it is still a node of its associated
         * {@code SubList}.
         * 
         * <p>
         * <b>Use with CAUTION:</b> This {@code SubListNode}, if linked, is not verified
         * that it is still a node of its associated {@code SubList}. The results of
         * using this {@code SubListNode} are unpredictable if this {@code SubListNode}
         * is not a node of its associated {@code SubList}. This method is provided to
         * avoid the performance cost of verifying a {@code SubListNode} is still a node
         * of its associated {@code SubList} when it's certain that it still is. Note,
         * just like any {@code SubListNode}, if the {@code NodableLinkedList} is
         * subsequently modified, this {@code SubListNode}, will once again have to be
         * verified.
         * 
         * @return this {@code SubListNode} marked as verified that it is still a node
         *         of its associated {@code SubList}
         */
        public SubListNode<E> unverified() {
            subList().checkForModificationException();
            updateExpectedModCount();
            return this;
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
         * @return {@code true} if the specified object ({@code Node}) is equal to this
         *         {@code SubListNode}
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
       
       private final SubListNode<E> subListNode;
       
       private ReverseSubListNode(SubListNode<E> subListNode) {
           super();
           this.subListNode = subListNode;
       }
       
       @Override
       public NodableLinkedList<E>.SubList subList() {
           return subListNode.subList();
       }
       
       @Override
       void setSubList(NodableLinkedList<E>.SubList subList) {
           subListNode.setSubList(subList);
       }
       
       @Override
       void updateExpectedModCount() {
           subListNode.updateExpectedModCount();
       }
       
       boolean isStillNodeOfSubList() {
           return subListNode.isStillNodeOfSubList();
       }
       
       @Override
       public E element() {
           return subListNode.element();
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
       public int index() {
           final int index = subListNode.index();
           return (index < 0) ? index : (linkedNodes().size() - index) - 1;
       }
       
       @Override
       public SubListNode<E> next() {
           return subListNode.previous().reversed();
       }
       
       @Override
       public SubListNode<E> previous() {
           return subListNode.next().reversed();
       }
       
       @Override
       public SubListNode<E> reversed() {
           return subListNode;
       }
       
   } // ReverseSubListNode    

   /**
    * Node of a {@code NodableLinkedList.LinkedNodes} list. Contains references to
    * the previous and next {@code LinkNodes} in a doubly-linked list, and contains
    * an element which can be {@code null}. Does not belong to any particular
    * {@code NodableLinkedList} until the {@code LinkNode} is inserted/added. Once
    * inserted, the {@code LinkNode} remains linked to a {@code NodableLinkedList}
    * until removed. A {@code LinkNode} can belong to different lists, just not at
    * the same time.
    * 
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
    * 
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

        private E element;

        private transient LinkNode<E> next = null;
        private transient LinkNode<E> previous = null;		
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
        
        void setLinkedNodes(NodableLinkedList<E>.LinkedNodes linkedNodes) {
            this.linkedNodes = linkedNodes;
        }
        
        LinkNode<E> getNext() {
            return this.next;
        }
        
        LinkNode<E> getPrevious() {
            return this.previous;
        }
        
        void setNext(LinkNode<E> next) {
            this.next = next;
        }
        
        void setPrevious(LinkNode<E> previous) {
            this.previous = previous;
        }

        /**
         * Returns {@code null}; {@code LinkNodes} are never part of a {@code SubList}.
         * 
         * @return {@code null}
         */
        @Override
        public NodableLinkedList<E>.SubList subList() {
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
         * Returns this {@code LinkNode}
         * 
         * @return this {@code LinkNode}
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
         * 
         * <p>
         * If the specified node is a {@code SubListNode}, it is verified that it is
         * still contained by its associated {@code SubList}. Also, this
         * {@code LinkNode} is effectively inserted into the specified
         * {@code SubListNode's} associated {@code SubList}.
         * 
         * <p>
         * If this {@code LinkNode} is reversed, this operation behaves like the
         * {@code addBefore(Node)} method.
         * 
         * @param node the {@code Node} this {@code LinkNode} is to be inserted after
         * @throws IllegalStateException    if this {@code LinkNode} is already a node
         *                                  of a list
         * @throws IllegalArgumentException if node is {@code null} or not a node of a
         *                                  list
         */
        @Override
        public void addAfter(Node<E> node) {
            if (node == null || !node.isLinked()) {
                throw new IllegalArgumentException("Specified Node is null or not a node of a list");
            }
            if (this.isLinked()) {
                throw new IllegalStateException("This node is already a node of a list");
            }
            if (node.isSubListNode()) {
                final SubListNode<E> subListNode = (SubListNode<E>) node;
                subListNode.subList().checkForModificationException();
                subListNode.checkIfArgStillNodeOfSubList();
                subListNode.subList().linkedSubNodes().addNodeAfter(this, subListNode.linkNode());
                subListNode.updateExpectedModCount();
            } else {
                node.linkedNodes().addNodeAfter(this, node.linkNode());
            }

        }

        /**
         * Inserts this {@code LinkNode} before the specified node. This
         * {@code LinkNode} must not already belong to a list, and the specified node
         * must belong to a list.
         * 
         * <p>
         * If the specified node is a {@code SubListNode}, it is verified that it is
         * still contained by its associated {@code SubList}. Also, this
         * {@code LinkNode} is effectively inserted into the specified
         * {@code SubListNode's} associated {@code SubList}.
         * 
         * <p>
         * If this {@code LinkNode} is reversed, this operation behaves like the
         * {@code addAfter(Node)} method.
         * 
         * @param node the {@code Node} this {@code LinkNode} is to be inserted before
         * @throws IllegalStateException    if this {@code LinkNode} is already a node
         *                                  of a list
         * @throws IllegalArgumentException if node is {@code null} or not a node of a
         *                                  list
         */
        @Override
        public void addBefore(Node<E> node) {
            if (node == null || !node.isLinked()) {
                throw new IllegalArgumentException("Specified Node is null or not a node of a list");
            }
            if (this.isLinked()) {
                throw new IllegalStateException("This node is already a node of a list");
            }
            if (node.isSubListNode()) {
                final SubListNode<E> subListNode = (SubListNode<E>) node;
                subListNode.subList().checkForModificationException();
                subListNode.checkIfArgStillNodeOfSubList();
                subListNode.subList().linkedSubNodes().addNodeBefore(this, subListNode.linkNode());
                subListNode.updateExpectedModCount();
            } else {
                node.linkedNodes().addNodeBefore(this, node.linkNode());
            }
        }
        
        /**
         * Returns {@code true} if there exists a {@code Node} which comes after this
         * {@code LinkNode} in a list. In other words, returns {@code true} if this
         * {@code LinkNode} is not the last node of a list.
         * 
         * <p>
         * If this {@code LinkNode} is reversed, this operation behaves like the
         * {@code hasPrevious()} method.
         * 
         * @return {@code true} if there exists a {@code Node} which comes after this
         *         {@code LinkNode}
         * @throws IllegalStateException if this {@code LinkNode} is not linked
         */
        @Override
        public boolean hasNext() {
            if (!this.isLinked()) {
                throw new IllegalStateException("This node is not a node of a list"); 
            }
            return linkedNodes().hasNodeAfter(this);
        }

        /**
         * Returns {@code true} if there exists a {@code Node} which comes before this
         * {@code LinkNode} in a list. In other words, returns {@code true} if this
         * {@code LinkNode} is not the first node of a list.
         * 
         * <p>
         * If this {@code LinkNode} is reversed, this operation behaves like the
         * {@code hasNext()} method.
         * 
         * @return {@code true} if there exists a {@code Node} which comes before this
         *         {@code LinkNode}
         * @throws IllegalStateException if this {@code Node} is not linked
         */
        @Override
        public boolean hasPrevious() {
            if (!this.isLinked()) {
                throw new IllegalStateException("This node is not a node of a list");
            }
            return linkedNodes().hasNodeBefore(this);
        }

        /**
         * Returns the index of this {@code LinkNode} in a list, or -1 if this
         * {@code LinkNode} does not belong to a list or the
         * {@code index > Integer.MAX_VALUE}.
         * 
         * <p>
         * If this {@code LinkNode} is reversed, the index returned is relative from the
         * end of the list (the last node has an index of zero, the second to last node
         * has an index of 1, etc.).
         * 
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
            final long index = linkedNodes().getIndex(this);
            return (index > Integer.MAX_VALUE) ? -1 : (int)index;
        }

        /**
         * Returns the {@code LinkNode} which comes after this {@code LinkNode} in a
         * list. if this {@code LinkNode} is the last or only {@code LinkNode},
         * {@code null} is returned.
         * 
         * <p>
         * If this is a reversed {@code LinkNode}, this operation behaves like the
         * {@code previous()} method, and the returned {@code LinkNode} will also be
         * reversed.
         * 
         * @return the {@code LinkNode} which comes after this {@code LinkNode} in a
         *         list, or {@code null} if this {@code LinkNode} is the last or only
         *         {@code LinkNode}
         * @throws IllegalStateException if this {@code LinkNode} is not linked
         */
        @Override
        public LinkNode<E> next() {
            if (!this.isLinked()) {
                throw new IllegalStateException("This node is not a node of a list");
            }
            final LinkNode<E> linkNode = linkedNodes().getNodeAfter(this);
            return (this.isReversed() && linkNode != null) ? linkNode.reversed() : linkNode;
        }

        /**
         * Returns the {@code LinkNode} which comes before this {@code LinkNode} in a
         * list. if this {@code LinkNode} is the first or only {@code LinkNode},
         * {@code null} is returned.
         * 
         * <p>
         * If this is a reversed {@code LinkNode}, this operation behaves like the
         * {@code next()} method, and the returned {@code LinkNode} will also be
         * reversed.
         * 
         * @return the {@code LinkNode} which comes before this {@code LinkNode} in a
         *         list, or {@code null} if this {@code LinkNode} is the first or only
         *         {@code LinkNode}
         * @throws IllegalStateException if this {@code LinkNode} is not linked
         */
        @Override
        public LinkNode<E> previous() {
            if (!this.isLinked()) {
                throw new IllegalStateException("This node is not a node of a list");
            }
            final LinkNode<E> linkNode = linkedNodes().getNodeBefore(this);
            return (this.isReversed() && linkNode != null) ? linkNode.reversed() : linkNode;
        }

        /**
         * Removes this {@code LinkNode} from the list it is linked to.
         * 
         * @throws IllegalStateException if this {@code LinkNode} is not linked
         */
        @Override
        public void remove() {
            if (!this.isLinked()) {
                throw new IllegalStateException("This node is not a node of a list");
            }
            linkedNodes().removeNode(this);
        }

        /**
         * Replaces this {@code LinkNode} with the specified node. This {@code LinkNode}
         * must belong to a list, and the specified node must not already belong to a
         * list.
         * 
         * @param node {@code Node} to replace this {@code LinkNode}
         * @throws IllegalStateException    if this {@code LinkNode} is not linked
         * @throws IllegalArgumentException if node is {@code null} or already a node of
         *                                  a list
         */
        @Override
        public void replaceWith(Node<E> node) {
            if (node == null || node.isLinked()) {
                throw new IllegalArgumentException("Specified node is null or already a node of a list");
            }
            if (!this.isLinked()) {
                throw new IllegalStateException("This node is not a node of a list");
            }
            linkedNodes().replaceNode(this, node.linkNode());			
        }
        
        /**
         * Returns a {@code LinkNode} that can be used to traverse the list it is linked
         * to in the reverse direction than this {@code LinkNode}. Order-sensitive
         * operations like {@code addAfter}, {@code addBefore}, {@code hasNext},
         * {@code hasPrevious}, {@code next}, {@code previous}, and {@code index} also
         * operate in a reverse way.
         * 
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
         *         to in the reverse direction than this {@code LinkNode}
         */
        @Override
        public LinkNode<E> reversed() {
            return new ReverseLinkNode<E>(this);
        }

        /**
         * Swaps this {@code LinkNode} with the specified node. Both this {@code LinkNode} and
         * the specified node must belong to a list, but they can be different lists.
         * 
         * <p>
         * If the specified node is a {@code SubListNode}, it is verified that it is
         * still contained by its associated {@code SubList}. Also, this
         * {@code LinkNode} is effectively inserted into the specified
         * {@code SubListNode's} associated {@code SubList}.
         * 
         * <p>
         * <strong>Synchronization consideration:</strong> This operation can
         * potentially operate on two different lists. if synchronization is required,
         * both lists should be synchronized by the same object.
         * 
         * @param node the {@code Node} to swap with this {@code LinkNode}
         * @throws IllegalStateException    if this {@code LinkNode} is not linked
         * @throws IllegalArgumentException if node is {@code null} or not linked
         */
        @Override
        public void swapWith(Node<E> node) {
            if (node == null || !node.isLinked()) {
                throw new IllegalArgumentException("The specified node is null or not a node of a list");
            }
            if (!this.isLinked()) {
                throw new IllegalStateException("This node is not a node of a list");
            }
            if (node.isSubListNode()) {
                final SubListNode<E> subListNode = (SubListNode<E>)node;
                final NodableLinkedList<E>.SubList thatSubList = subListNode.subList();
                thatSubList.checkForModificationException();
                subListNode.checkIfArgStillNodeOfSubList();
                swapNodes(this, node.linkNode());
                thatSubList.linkedSubNodes().swappedNodes(subListNode.linkNode(), this);
            } else {
                swapNodes(this, node.linkNode());
            }
        }

        private static <T> void swapNodes(LinkNode<T> node1, LinkNode<T> node2) {
            //assert node1 != null && node1.isLinked() : "node1 is null or not a node of a list";
            //assert node2 != null && node2.isLinked() : "node2 is null or not a node of a list";
            if (node1 == node2) return;
            node1.linkedNodes().incrementModCount();
            if (node1.linkedNodes() != node2.linkedNodes()) {
                // nodes are linked in different lists
                node2.linkedNodes().incrementModCount();
                LinkNode<T> tempNode;
                NodableLinkedList<T>.LinkedNodes tempLinkedNodes;
                // swap lists
                tempLinkedNodes = node1.linkedNodes();
                node1.setLinkedNodes(node2.linkedNodes());
                node2.setLinkedNodes(tempLinkedNodes);
                // swap next references
                tempNode = node1.getNext();
                node1.setNext(node2.getNext());
                node2.setNext(tempNode);
                // swap previous references
                tempNode = node1.getPrevious();
                node1.setPrevious(node2.getPrevious());
                node2.setPrevious(tempNode);
            } else {
                // nodes are linked in the same list
                if (node1.getNext() == node2) {
                    // node1 comes right before node2 in the list
                    node1.setNext(node2.getNext()); node2.setNext(node1);
                    node2.setPrevious(node1.getPrevious()); node1.setPrevious(node2);
                } else if (node1.getPrevious() == node2) {
                    // node1 comes right after node2 in the list
                    node2.setNext(node1.getNext()); node1.setNext(node2);
                    node1.setPrevious(node2.getPrevious()); node2.setPrevious(node1);
                } else {
                    LinkNode<T> tempNode;
                    // swap next references
                    tempNode = node1.getNext();
                    node1.setNext(node2.getNext());
                    node2.setNext(tempNode);
                    // swap previous references
                    tempNode = node1.getPrevious();
                    node1.setPrevious(node2.getPrevious());
                    node2.setPrevious(tempNode);
                }
            }
            // update the node's neighbors
            node1.getPrevious().setNext(node1);
            node1.getNext().setPrevious(node1);
            node2.getPrevious().setNext(node2);
            node2.getNext().setPrevious(node2);
        }        

        /**
         * Returns a {@code SubListNode}, backed by this {@code LinkNode}, for the
         * specified subList. The returned {@code SubListNode} is backed by this
         * {@code LinkNode} which must be a node of the specified subList, or unlinked.
         * 
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
        public SubListNode<E> subListNode(NodableLinkedList<E>.SubList subList) {
            if (subList == null) throw new IllegalArgumentException("Specified SubList is null");
            subList.checkForModificationException();
            if (this.isLinked() && !subList.linkedSubNodes().contains(this)) {
                throw new IllegalStateException("This node is not a node of the specified subList");
            }
            return new SubListNode<E>(this, subList);
        }

        /**
         * Returns an unverified {@code SubListNode}, backed by this
         * {@code LinkNode}, for the specified subList. The returned {@code SubListNode}
         * is backed by this {@code LinkNode} which must be a node of the specified
         * subList, or unlinked.
         * 
         * <p>
         * <b>Use with CAUTION:</b> This {@code LinkNode}, if linked, is not verified
         * that it is a node of the specified subList. The results of using the returned
         * {@code SubListNode} are unpredictable if this {@code LinkNode} is not a node
         * of the specified sublist. This method is provided to avoid the cost of
         * verifying that a {@code LinkNode} is a node of the specified subList when
         * it's certain that it is. For example, nodes returned by a sublist's iterator
         * don't need to be verified. Note, just like any {@code SubListNode}, if the
         * {@code NodableLinkedList} is subsequently modified, the returned
         * {@code SubListNode}, will have to be reverified.
         * 
         * @param subList {@code SubList} believed to contain this {@code LinkNode}
         * @return an unverified {@code SubListNode}, backed by this
         *         {@code LinkNode}, for the specified subList
         * @throws IllegalArgumentException if the specified subList is {@code null}
         */
        @Override
        public SubListNode<E> unverifiedSubListNode(NodableLinkedList<E>.SubList subList) {
            if (subList == null) throw new IllegalArgumentException("Specified SubList is null");
            subList.checkForModificationException();
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
        
        private final LinkNode<E> linkNode; // never a ReverseLinkNode
        
        private ReverseLinkNode(LinkNode<E> linkNode) {
            super();
            //assert !linkNode.isReversed() : "linkNode is reversed";
            this.linkNode = linkNode;
        }
        
        @Override
        void setLinkedNodes(NodableLinkedList<E>.LinkedNodes linkedNodes) {
            linkNode.setLinkedNodes(linkedNodes);
        }
        
        @Override
        LinkNode<E> getNext() {
            return linkNode.getNext();
        }
        
        @Override
        LinkNode<E> getPrevious() {
            return linkNode.getPrevious();
        }
        
        @Override
        void setNext(LinkNode<E> next) {
            linkNode.setNext(next);
        }
        
        @Override
        void setPrevious(LinkNode<E> previous) {
            linkNode.setPrevious(previous);
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
        public int index() {
            final int index = linkNode.index();
            return (index < 0) ? index : (linkedNodes().size() - index) - 1;
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
     * Node of a {@code NodableLinkedList}.
     * 
     * <p>
     * There are two types of {@code Nodes}: One is a {@code LinkNode} which
     * contains an element and references to the previous and next nodes in a
     * doubly-linked list, and the other is a {@code SubListNode} which represents a
     * {@code LinkNode} that is associated with a {@code SubList}. {@code Nodes} can
     * also be reversed. Reversing a {@code Node} affects all order-sensitive
     * operations ({@code next()}, {@code previous()}, etc.).
     * 
     * <p>
     * A {@code Node} does not belong to any particular list until the {@code Node}
     * is inserted/added. Once inserted, the {@code Node} remains linked to the list
     * until removed. A {@code Node} can belong to different lists, just not at the
     * same time.
     * 
     * <p>
     * In general, all operations, except {@code index()} and {@code subListNode()},
     * perform in constant time. This does not necessarily apply to
     * {@code SubListNodes} (see the description for {@code SubListNode}).
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
         * 
         * <p>
         * If the specified node is a {@code SubListNode}, it is verified that it is
         * still contained by its associated {@code SubList}. Also, this {@code Node} is
         * effectively inserted into the specified {@code SubListNode's} associated
         * {@code SubList}.
         * 
         * <p>
         * If this {@code Node} is reversed, this operation behaves like the
         * {@code addBefore(Node)} method.
         * 
         * @param node the {@code Node} this {@code Node} is to be inserted after
         * @throws IllegalStateException    if this {@code Node} is already a node of a
         *                                  list
         * @throws IllegalArgumentException if node is {@code null} or not a node of a
         *                                  list
         */
        public void addAfter(Node<E> node);

        /**
         * Inserts this {@code Node} before the specified node. This {@code Node} must
         * not already belong to a list, and the specified node must belong to a list.
         * 
         * <p>
         * If the specified node is a {@code SubListNode}, it is verified that it is
         * still contained by its associated {@code SubList}. Also, this {@code Node} is
         * effectively inserted into the specified {@code SubListNode's} associated
         * {@code SubList}.
         * 
         * <p>
         * If this {@code Node} is reversed, this operation behaves like the
         * {@code addAfter(Node)} method.
         * 
         * @param node the {@code Node} this {@code Node} is to be inserted before
         * @throws IllegalStateException    if this {@code Node} is already a node of a
         *                                  list
         * @throws IllegalArgumentException if node is {@code null} or not a node of a
         *                                  list
         */
        public void addBefore(Node<E> node);

        /**
         * Returns {@code true} if there exists a {@code Node} which comes after this
         * {@code Node} in a list. In other words, returns {@code true} if this
         * {@code Node} is not the last node of a list.
         * 
         * <p>
         * If this {@code Node} is a {@code SubListNode}, it is verified that it is
         * still contained by its associated {@code SubList}.
         * 
         * <p>
         * If this {@code Node} is reversed, this operation behaves like the
         * {@code hasPrevious()} method.
         * 
         * @return {@code true} if there exists a {@code Node} which comes after this
         *         {@code Node}
         * @throws IllegalStateException if this {@code Node} does not belong to a list
         */
        public boolean hasNext();

        /**
         * Returns {@code true} if there exists a {@code Node} which comes before this
         * {@code Node} in a list. In other words, returns {@code true} if this
         * {@code Node} is not the first node of a list.
         * 
         * <p>
         * If this {@code Node} is a {@code SubListNode}, it is verified that it is
         * still contained by its associated {@code SubList}.
         * 
         * <p>
         * If this {@code Node} is reversed, this operation behaves like the
         * {@code hasNext()} method.
         * 
         * @return {@code true} if there exists a {@code Node} which comes before this
         *         {@code Node}
         * @throws IllegalStateException if this {@code Node} does not belong to a list
         */
        public boolean hasPrevious();

        /**
         * Returns the index of this {@code Node} in a list, or -1 if this {@code Node}
         * does not belong to a list or the {@code index > Integer.MAX_VALUE}.
         * 
         * <p>
         * If this {@code Node} is reversed, the index returned is relative from the end
         * of the list (the last node has an index of zero, the second to last node has
         * an index of 1, etc.).
         * 
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
         * 
         * <p>
         * If this is a reversed {@code Node}, this operation behaves like the
         * {@code previous()} method, and the returned {@code Node} will also be
         * reversed.
         * 
         * @return the {@code Node} which comes after this {@code Node} in a list, or
         *         {@code null} if this {@code Node} is the last or only {@code Node}
         * @throws IllegalStateException if this {@code Node} does not belong to a list
         */
        public Node<E> next();

        /**
         * Returns the {@code Node} which comes before this {@code Node} in a list. if
         * this {@code Node} is the first or only {@code Node}, {@code null} is
         * returned.
         * 
         * <p>
         * If this is a reversed {@code Node}, this operation behaves like the
         * {@code next()} method, and the returned {@code Node} will also be reversed.
         * 
         * @return the {@code Node} which comes before this {@code Node} in a list, or
         *         {@code null} if this {@code Node} is the first or only {@code Node}
         * @throws IllegalStateException if this {@code Node} does not belong to a list
         */
        public Node<E> previous();
        
        /**
         * Removes this {@code Node} from the list it is linked to.
         * 
         * @throws IllegalStateException if this {@code Node} does not belong to a list
         */
        public void remove();

        /**
         * Replaces this {@code Node} with the specified node. This {@code Node} must
         * belong to a list, and the specified node must not already belong to a list.
         * 
         * <p>
         * If both this {@code Node} and the specified node are {@code SubListNodes},
         * after this operation is completed, the specified {@code SubListNode} will be
         * marked that it is contained by its new associated {@code SubList}.
         * 
         * @param node {@code Node} to replace this {@code Node}
         * @throws IllegalStateException    if this {@code Node} does not belong to a
         *                                  list
         * @throws IllegalArgumentException if node is {@code null} or already a node of
         *                                  a list
         */
        public void replaceWith(Node<E> node);
        
        /**
         * Returns a {@code Node} that can be used to traverse a list in the reverse
         * direction than this {@code Node}. Order-sensitive operations like
         * {@code addAfter}, {@code addBefore}, {@code hasNext}, {@code hasPrevious},
         * {@code next}, {@code previous}, and {@code index} also operate in a reverse
         * way.
         * 
         * @return a {@code Node} that can be used to traverse a list in the reverse
         *         direction than this {@code Node}
         */
        public Node<E> reversed();

        /**
         * Swaps this {@code Node} with the specified node. Both this {@code Node} and
         * the specified node must belong to a list, but they can be different lists.
         * 
         * <p>
         * If the specified node is a {@code SubListNode}, it is verified that it is
         * still contained by its associated {@code SubList}. If either this
         * {@code Node} or the specified node are {@code SubListNodes}, the other
         * {@code Node} is effectively inserted into the {@code SubListNode's}
         * associated {@code SubList}. If both this {@code Node} and the specified node
         * are {@code SubListNodes}, after this operation is completed, both will be
         * marked that they are contained by their possibly new associated
         * {@code SubLists}.
         * 
         * <p>
         * <strong>Synchronization consideration:</strong> This operation can
         * potentially operate on two different lists. if synchronization is required,
         * both lists should be synchronized by the same object.
         * 
         * @param node the {@code Node} to swap with this {@code Node}
         * @throws IllegalStateException    if this {@code Node} does not belong to a
         *                                  list
         * @throws IllegalArgumentException if node is {@code null} or does not belong
         *                                  to a list
         */
        public void swapWith(Node<E> node);

        /**
         * Returns the {@code SubList} this {@code Node} belongs to.
         * 
         * A {@code SubListNode} never returns a {@code null}, whereas, 
         * a {@code LinkNode} always returns a  {@code null}.
         * 
         * @return the {@code NodableLinkedList.SubList} this {@code Node} belongs to
         */
        public NodableLinkedList<E>.SubList subList();       

        /**
         * Returns a {@code SubListNode}, backed by this {@code Node's}
         * {@code LinkNode}, for the specified subList. The {@code LinkNode} which backs
         * the returned {@code SubListNode}, must be a node of the specified subList, or
         * unlinked.
         * 
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
        public SubListNode<E> subListNode(NodableLinkedList<E>.SubList subList);

        /**
         * Returns an unverified {@code SubListNode}, backed by this {@code Node's}
         * {@code LinkNode}, for the specified subList. The {@code LinkNode} which backs
         * the returned {@code SubListNode}, must be a node of the specified subList, or
         * unlinked.
         * 
         * <p>
         * <b>Use with CAUTION:</b> This {@code Node}, if linked, is not verified that
         * it is a node of the specified subList. The results of using the returned
         * {@code SubListNode} are unpredictable if this {@code Node} is not a node of
         * the specified sublist. This method is provided to avoid the cost of verifying
         * that a {@code Node} is a node of the specified subList when it's certain that
         * it is. For example, nodes returned by a sublist's iterator don't need to be
         * verified. Note, just like any {@code SubListNode}, if the
         * {@code NodableLinkedList} is subsequently modified, the returned
         * {@code SubListNode}, will have to be reverified.
         * 
         * @param subList {@code SubList} believed to contain this {@code Node}
         * @return an unverified {@code SubListNode}, backed by this {@code Node's}
         *         {@code LinkNode}, for the specified subList
         * @throws IllegalArgumentException if the specified subList is {@code null}
         */
        public SubListNode<E> unverifiedSubListNode(NodableLinkedList<E>.SubList subList);        
        
    } // Node

} // NodableLinkedList
