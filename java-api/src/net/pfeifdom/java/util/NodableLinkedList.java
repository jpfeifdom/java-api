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
 */

package net.pfeifdom.java.util;

import java.io.IOException;
import java.io.ObjectInputStream;
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
 * Doubly-linked list implementation of the {@code List} and {@code Deque} interfaces,
 * with the capability to perform insert and remove operations at any point in the list
 * in constant time. Implements all optional list operations, and permits all elements
 * (including {@code null}).
 * 
 * <p>Implements all the same constructors and methods as the {@code java.util.LinkedList} class,
 * but includes additional methods: {@code longSize} which returns the size of the list as a {@code long},
 * {@code node} which returns a new {@code NodableLinkedList.Node}, {@code linkedNodes} which returns the
 * {@code NodableLinkedList.LinkedNodes} object (list of nodes) backing a {@code NodableLinkedList},
 * and {@code addNode}, {@code addNodeFirst}, {@code addNodeLast}, {@code getFirstNode}, {@code getLastNode},
 * {@code removeFirstNode}, {@code removeLastNode}, {@code addAll(Node)} and {@code listIterator(Node)}
 * which perform operations on a {@code NodableLinkedList.Node}.
 * 
 * This implementation behaves differently than the standard {@code java.util.LinkedList} class
 * when it comes to lists which contain more than {@code Integer.MAX_VALUE} elements. The {@code size}
 * method returns Integer.MAX_VALUE when the list's {@code size > Integer.MAX_VALUE}. The method
 * {@code longSize} can be used to get the actual size of the list. The {@code indexOf} and
 * {@code lastIndexOf} methods return -1 if the {@code index > Integer.MAX_VALUE}. The {@code sort} and
 * {@code toArray} methods throw an IllegalStateException if the list's {@code size > Integer_MAX_VALUE}.
 * The {@code nextIndex} and {@code previousIndex} methods of the {@code java.util.ListIterator}
 * returned by {@code listIterator}, return -1 if the {@code index > Integer_MAX_VALUE}.
 * 
 * <p>There are two ways to visualize a {@code NodableLinkedList}: one as a list of elements
 * (the standard view), and the other as a list of nodes which contain the elements and references
 * to the previous and next nodes in this list. The latter view is implemented by the inner
 * class {@code LinkedNodes}. Each instance of {@code NodableLinkedList} has one and only one
 * instance of {@code LinkedNodes}.
 * 
 * <p>A large {@code NodableLinkedList} can be as much as 33% - 34% larger than a
 * {@code java.util.LinkedList}. An empty {@code NodableLinkedList} is almost 5 times larger
 * than an empty {@code java.util.LinkedList}.
 * 
 * <p>All of the operations perform as could be expected for a doubly-linked
 * list.  Operations that index into the list will traverse the list from
 * the beginning or the end, whichever is closer to the specified index.
 * Indexes can only be used to reference the first Integer.MAX_VALUE+1 elements.
 *
 * <p><strong>Note that this implementation is not synchronized.</strong>
 * If multiple threads access a linked list concurrently, and at least
 * one of the threads modifies the list structurally, it <i>must</i> be
 * synchronized externally.  (A structural modification is any operation
 * that adds or deletes one or more elements (or nodes); merely setting the value
 * of an element is not a structural modification.)  This is typically
 * accomplished by synchronizing on some object that naturally
 * encapsulates the list.
 *
 * If no such object exists, the list should be "wrapped" using the
 * {@link java.util.Collections#synchronizedList} method.  This is best done at creation time,
 * to prevent accidental unsynchronized access to the list:<pre>
 *   List list = Collections.synchronizedList(new NodableLinkedList(...));</pre>
 *
 * <p>The iterators returned by this class's {@code iterator} and
 * {@code listIterator} methods are <i>fail-fast</i>: if the list is
 * structurally modified at any time after the iterator is created, in
 * any way except through the Iterator's own {@code remove} or
 * {@code add} methods, the iterator will throw a {@link
 * ConcurrentModificationException}.  Thus, in the face of concurrent
 * modification, the iterator fails quickly and cleanly, rather than
 * risking arbitrary, non-deterministic behavior at an undetermined
 * time in the future.
 *
 * <p>Note that the fail-fast behavior of an iterator cannot be guaranteed
 * as it is, generally speaking, impossible to make any hard guarantees in the
 * presence of unsynchronized concurrent modification.  Fail-fast iterators
 * throw {@code ConcurrentModificationException} on a best-effort basis.
 * Therefore, it would be wrong to write a program that depended on this
 * exception for its correctness:   <i>the fail-fast behavior of iterators
 * should be used only to detect bugs.</i>
 *
 * @author  James Pfeifer
 * @see     List
 * @param <E> the type of elements held in this collection
 */
public class NodableLinkedList<E>
    extends AbstractSequentialList<E>
    implements List<E>, Deque<E>, Cloneable, Serializable
{

    //protected int modCount inherited from class java.util.AbstractList (see incrementModCount)	

    private static final long serialVersionUID = 5774068172585494452L;

    private transient final LinkedNodes linkedNodes = new LinkedNodes();
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
    }

    /**
     * Constructs a list containing the elements of the specified
     * collection, in the order they are returned by the collection's
     * iterator.
     *
     * @param  collection the collection whose elements are to be placed into this list
     * @throws NullPointerException if the specified collection is null
     */
    public NodableLinkedList(Collection<? extends E> collection) {
        this();
        addAll(collection);
    }

    // serialize
    /**
     * Save the state of this {@code NodableLinkedList} instance to a stream
     * (that is, serialize it).
     *
     * @param stream stream to save the state of this instance
     * @throws IOException I/O error occurred while writing to stream
     * @serialData The size of the list (the number of elements it
     *             contains) is emitted (long), followed by all of its
     *             elements (each an Object) in the proper order.
     */
    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject();
        stream.writeLong(longSize());
        for (Node<E> node = getFirstNode(); node != null; node = linkedNodes.getNodeAfter(node)) {
            stream.writeObject(node.element);
        }
    }

    // deserialize
    /**
     * Reconstitute this {@code NodableLinkedList} instance from a stream (that is
     * deserialize it).
     * 
     * @param stream stream to be deserialized
     * @throws ClassNotFoundException Class of a serialized object cannot be found.
     * @throws IOException I/O error occurred while reading from stream
     */
    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
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

    /**
     * Returns a shallow copy of this {@code NodableLinkedList}. (The elements
     * themselves are not cloned.)
     * 
     * The clone will have different nodes than the original.
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

    /**
     * Returns the list of nodes which back this {@code NodableLinkedList}
     * 
     * @return the list of nodes which back this {@code NodableLinkedList}
     */
    public LinkedNodes linkedNodes() {
        return linkedNodes;
    }

    /**
     * Returns the number of elements in this list.  If this list contains more than
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
     * Returns a {@code Node} containing the specified element which can be {@code null}.
     * The node will not be linked to any list.
     * 
     * @param <T> type of element to be contained within the returned {@code Node}
     * @param element element to be contained within the returned {@code Node}
     * @return an unlinked {@code Node} containing the specified element
     */
    public static <T> Node<T> node(T element) {
        return new Node<>(element);
    }

    /**
     * Appends the specified node to the end of this list.
     *
     * <p>This method is equivalent to {@link #addNodeLast}.
     *
     * @param node node to be appended to the end of this list
     * @throws IllegalArgumentException if node is null or already an element of a list
     */
    public void addNode(Node<E> node) {
        addNodeLast(node);
    }

    /**
     * Inserts the specified node at the beginning of this list.
     *
     * @param node the node to be inserted at the beginning of this list
     * @throws IllegalArgumentException if node is null or already an element of a list
     */
    public void addNodeFirst(Node<E> node) {
        linkedNodes.addFirst(node);
    }

    /**
     * Appends the specified node to the end of this list.
     *
     * <p>This method is equivalent to {@link #addNode}.
     *
     * @param node node to be appended to the end of this list
     * @throws IllegalArgumentException if node is null or already an element of a list
     */
    public void addNodeLast(Node<E> node) {
        linkedNodes.addLast(node);
    }

    /**
     * Retrieves, but does not remove, the first node of this list,
     * or returns {@code null} if this list is empty.
     *
     * @return the first node of this list, or {@code null}
     *         if this list is empty
     */
    public Node<E> getFirstNode() {
        return linkedNodes.peekFirst();
    }

    /**
     * Retrieves, but does not remove, the last node of this list,
     * or returns {@code null} if this list is empty.
     *
     * @return the last node of this list, or {@code null}
     *         if this list is empty
     */
    public Node<E> getLastNode() {
        return linkedNodes.peekLast();
    }

    /**
     * Removes and returns the first node of this list.
     * 
     * Returns the removed node or {@code null} if the list is empty
     *
     * @return the first node of this list that was removed,
     *         or {@code null} if this list is empty
     */
    public Node<E> removeFirstNode() {
        return linkedNodes.pollFirst();
    }

    /**
     * Removes and returns the last node of this list.
     * 
     * Returns the removed node or {@code null} if the list is empty
     *
     * @return the last node of this list that was removed,
     *         or {@code null} if this list is empty
     */
    public Node<E> removeLastNode() {
        return linkedNodes.pollLast();
    }

    private static <T> void swapNodes(Node<T> node1, Node<T> node2) {
        // assert node1 != null && node1.isLinked() : "node1 is null or not an element of a list";
        // assert node2 != null && node2.isLinked() : "node2 is null or not an element of a list";
        if (node1 == node2) return;
        node1.linkedNodes.incrementModCount();
        if (node1.linkedNodes != node2.linkedNodes) {
            // nodes are linked in different lists
            node2.linkedNodes.incrementModCount();
            NodableLinkedList<T>.LinkedNodes tempLinkedNodes;
            tempLinkedNodes = node1.linkedNodes; node1.linkedNodes = node2.linkedNodes; node2.linkedNodes = tempLinkedNodes;
            Node<T> tempNode;
            tempNode = node1.next; node1.next = node2.next; node2.next = tempNode;
            tempNode = node1.previous; node1.previous = node2.previous; node2.previous = tempNode;
        } else {
            // nodes are linked in the same list
            if (node1.next == node2) {
                node1.next = node2.next; node2.next = node1;
                node2.previous = node1.previous; node1.previous = node2;
            } else if (node1.previous == node2) {
                node2.next = node1.next; node1.next = node2;
                node1.previous = node2.previous; node2.previous = node1;
            } else {
                Node<T> tempNode;
                tempNode = node1.next; node1.next = node2.next; node2.next = tempNode;
                tempNode = node1.previous; node1.previous = node2.previous; node2.previous = tempNode;
            }
        }
        node1.previous.next = node1;
        node1.next.previous = node1;
        node2.previous.next = node2;
        node2.next.previous = node2;
    }

    /**
     * Returns {@code true} if this list contains the specified element.
     * More formally, returns {@code true} if and only if this list contains
     * at least one element {@code e} such that
     * {@code (o==null ? e==null : o.equals(e))}.
     *
     * @param o element whose presence in this list is to be tested
     * @return {@code true} if this list contains the specified element
     */
    @Override
    public boolean contains(Object o) {
        Node<E> node = getFirstNode();
        if (o == null) {
            for (; node != null; node = linkedNodes.getNodeAfter(node)) {
                if (node.element == null) return true;			
            }
        } else {
            for (; node != null; node = linkedNodes.getNodeAfter(node)) {
                if (o.equals(node.element)) return true;			
            }
        }
        return false;
    }	

    /**
     * Returns the index of the first occurrence of the specified element
     * in this list, or -1 if there is no such index (this list does not
     * contain the element or the {@code index > Integer.MAX_VALUE}).
     * More formally, returns the lowest index {@code i} such that
     * {@code (o==null ? get(i)==null : o.equals(get(i)))},
     * or -1 if there is no such index.
     *
     * @param o element to search for
     * @return the index of the first occurrence of the specified element in
     *         this list, or -1 if there is no such index.
     */
    @Override
    public int indexOf(Object o) {
        long index = 0;
        Node<E> node = getFirstNode();
        if (o == null) {
            for (; node != null; index++, node = linkedNodes.getNodeAfter(node)) {
                if (node.element == null) return (index > Integer.MAX_VALUE) ? -1 : (int)index;			
            }
        } else {
            for (; node != null; index++, node = linkedNodes.getNodeAfter(node)) {
                if (o.equals(node.element)) return (index > Integer.MAX_VALUE) ? -1 : (int)index;			
            }
        }
        return -1;
    }

    /**
     * Returns the index of the last occurrence of the specified element
     * in this list, or -1 if there is no such index (this list does not
     * contain the element or the {@code index > Integer.MAX_VALUE}).
     * More formally, returns the highest index {@code i} such that
     * {@code (o==null ? get(i)==null : o.equals(get(i)))},
     * or -1 if there is no such index.
     *
     * @param o element to search for
     * @return the index of the last occurrence of the specified element in
     *         this list, or -1 if there is no such index.
     */
    @Override
    public int lastIndexOf(Object o) {
        long index = longSize() - 1L;
        Node<E> node = getLastNode();
        if (o == null) {
            for (; node != null; index--, node = linkedNodes.getNodeBefore(node)) {
                if (node.element == null) return (index > Integer.MAX_VALUE) ? -1 : (int)index;			
            }
        } else {
            for (; node != null; index--, node = linkedNodes.getNodeBefore(node)) {
                if (o.equals(node.element)) return (index > Integer.MAX_VALUE) ? -1 : (int)index;			
            }
        }
        return -1;
    }

    /**
     * Replaces the element at the specified position in this list with the
     * specified element.
     *
     * @param index index of the element to replace
     * @param element element to be stored at the specified position
     * @return the element previously at the specified position
     * @throws IndexOutOfBoundsException if the index is out of range {@code (index < 0 || index >= longSize())}
     */
    @Override
    public E set(int index, E element) {
        final Node<E> node = linkedNodes.get(index);
        final E originalElement = node.element;
        node.set(element);
        return originalElement;
    }

    /**
     * Removes all of the elements from this list.
     */
    @Override
    public void clear() {
        linkedNodes.clear();
    }

    /**
     * Appends all of the elements in the specified collection to the end of
     * this list, in the order that they are returned by the specified
     * collection's iterator.  The behavior of this operation is undefined if
     * the specified collection is modified while the operation is in
     * progress.  (Note that this will occur if the specified collection is
     * this list, and it's nonempty.)
     *
     * @param elements collection containing elements to be added to this list
     * @return {@code true} if this list changed as a result of the call
     * @throws NullPointerException if the specified collection is null
     */
    @Override
    public boolean addAll(Collection<? extends E> elements) {
        final long initialSize = longSize();
        for (E element: elements) addLast(element);
        return longSize() != initialSize;
    }

    /**
     * Inserts all of the elements in the specified collection into this
     * list, starting at the specified position.  Shifts the element
     * currently at that position (if any) and any subsequent elements to
     * the right (increases their indices).  The new elements will appear
     * in the list in the order that they are returned by the
     * specified collection's iterator.
     *
     * @param index index at which to insert the first element
     *              from the specified collection
     * @param elements collection containing elements to be added to this list
     * @return {@code true} if this list changed as a result of the call
     * @throws IndexOutOfBoundsException if the index is out of range {@code (index < 0 || index > longSize())}
     * @throws NullPointerException if the specified collection is null
     */
    @Override
    public boolean addAll(int index, Collection<? extends E> elements) {
        if (index < 0 || index > longSize()) throw new IndexOutOfBoundsException("index=" + index + ", size=" + longSize());
        final long initialSize = longSize();
        if (index == longSize()) {
            for (E element: elements) addLast(element);
        } else {
            final Node<E> targetNode = linkedNodes.get(index);
            for (E element: elements) linkedNodes.addNodeBefore(node(element), targetNode);;
        }
        return longSize() != initialSize;
    }

    /**
     * Inserts all of the elements in the specified collection into this
     * list, before the specified node. Shifts the element
     * currently at that position and any subsequent elements to
     * the right (increases their indices).  The new elements will appear
     * in the list in the order that they are returned by the
     * specified collection's iterator. if the specified node is null,
     * the elements will be appended to the end of this list.
     * 
     * Note that {@code addAll(null, Collection)} is identical in function to
     * {@code addAll(Collection)}.
     *
     * @param node node the specified collection is to be inserted before
     * @param elements collection containing elements to be added to this list
     * @return {@code true} if this list changed as a result of the call
     * @throws IllegalArgumentException if the specified node is not linked to this list
     * @throws NullPointerException if the specified collection is null
     */
    public boolean addAll(Node<E> node, Collection<? extends E> elements) {
        if (node == null) return addAll(elements);
        if (!linkedNodes.contains(node)) throw new IllegalArgumentException("Specified node is not linked to this list");
        final long initialSize = longSize();
        for (E element: elements) linkedNodes.addNodeBefore(node(element), node);
        return longSize() != initialSize;
    }

    /**
     * Inserts the specified element at the specified position in this list.
     * Shifts the element currently at that position (if any) and any
     * subsequent elements to the right (adds one to their indices).
     *
     * @param index index at which the specified element is to be inserted
     * @param element element to be inserted
     * @throws IndexOutOfBoundsException if the index is out of range {@code (index < 0 || index > longSize())}
     */
    @Override
    public void add(int index, E element) {
        linkedNodes.add(index, node(element));
    }	

    /**
     * Appends the specified element to the end of this list.
     *
     * @param element element to be appended to this list
     * @return {@code true} (as specified by {@link Collection#add})
     */
    @Override
    public boolean add(E element) {
        addLast(element);
        return true;
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
     * Pushes an element onto the stack represented by this list.  In other
     * words, inserts the element at the front of this list.
     *
     * <p>This method is equivalent to {@link #addFirst}.
     *
     * @param element the element to push
     */
    @Override
    public void push(E element) {
        addFirst(element);
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
     * @throws IndexOutOfBoundsException if the index is out of range {@code (index < 0 || index >= longSize())}
     */
    @Override
    public E get(int index) {
        return linkedNodes.get(index).element;
    }	

    /**
     * Returns the first element in this list.
     *
     * @return the first element in this list
     * @throws NoSuchElementException if this list is empty
     */
    @Override
    public E getFirst() {
        return linkedNodes.getFirst().element;
    }

    /**
     * Returns the last element in this list.
     *
     * @return the last element in this list
     * @throws NoSuchElementException if this list is empty
     */
    @Override
    public E getLast() {
        return linkedNodes.getLast().element;
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
     * Retrieves, but does not remove, the first element of this list,
     * or returns {@code null} if this list is empty.
     *
     * @return the first element of this list, or {@code null}
     *         if this list is empty
     */
    @Override
    public E peekFirst() {
        return (longSize() == 0L) ? null : getFirst();
    }

    /**
     * Retrieves, but does not remove, the last element of this list,
     * or returns {@code null} if this list is empty.
     *
     * @return the last element of this list, or {@code null}
     *         if this list is empty
     */
    @Override
    public E peekLast() {
        return (longSize() == 0L) ? null : getLast();
    }

    /**
     * Pops an element from the stack represented by this list.  In other
     * words, removes and returns the first element of this list.
     *
     * <p>This method is equivalent to {@link #removeFirst()}.
     *
     * @return the element at the front of this list (which is the top
     *         of the stack represented by this list)
     * @throws NoSuchElementException if this list is empty
     */
    @Override
    public E pop() {
        return removeFirst();
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
     * Shifts any subsequent elements to the left (subtracts one from their indices).
     * Returns the element that was removed from the list.
     *
     * @param index the index of the element to be removed
     * @return the element previously at the specified position
     * @throws IndexOutOfBoundsException if the index is out of range {@code (index < 0 || index >= longSize())}
     */
    @Override
    public E remove(int index) {
        return linkedNodes.remove(index).element;
    }	

    /**
     * Removes and returns the first element from this list.
     *
     * @return the first element from this list
     * @throws NoSuchElementException if this list is empty
     */
    @Override
    public E removeFirst() {
        return linkedNodes.removeFirst().element;
    }

    /**
     * Removes and returns the last element from this list.
     *
     * @return the last element from this list
     * @throws NoSuchElementException if this list is empty
     */
    @Override
    public E removeLast() {
        return linkedNodes.removeLast().element;
    }

    /**
     * Removes the first occurrence of the specified element from this list,
     * if it is present.  If this list does not contain the element, it is
     * unchanged.  More formally, removes the element with the lowest index
     * {@code i} such that
     * {@code (o==null ? get(i)==null : o.equals(get(i)))}
     * (if such an element exists).  Returns {@code true} if this list
     * contained the specified element (or equivalently, if this list
     * changed as a result of the call).
     *
     * @param o element to be removed from this list, if present
     * @return {@code true} if this list contained the specified element
     */
    @Override
    public boolean remove(Object o) {
        return removeFirstOccurrence(o);
    }

    /**
     * Removes the first occurrence of the specified element in this
     * list (when traversing the list from head to tail).  If the list
     * does not contain the element, it is unchanged.
     *
     * @param o element to be removed from this list, if present
     * @return {@code true} if the list contained the specified element
     */
    @Override
    public boolean removeFirstOccurrence(Object o) {
        if (o == null) {
            for (Node<E> node = getFirstNode(); node != null; linkedNodes.getNodeAfter(node)) {
                if (node.element == null) {
                    linkedNodes.removeNode(node);
                    return true;
                }			
            }			
        } else {
            for (Node<E> node = getFirstNode(); node != null; linkedNodes.getNodeAfter(node)) {
                if (o.equals(node.element)) {
                    linkedNodes.removeNode(node);
                    return true;
                }			
            }			
        }
        return false;
    }

    /**
     * Removes the last occurrence of the specified element in this
     * list (when traversing the list from head to tail).  If the list
     * does not contain the element, it is unchanged.
     *
     * @param o element to be removed from this list, if present
     * @return {@code true} if the list contained the specified element
     */
    @Override
    public boolean removeLastOccurrence(Object o) {
        if (o == null) {
            for (Node<E> node = getLastNode(); node != null; node = linkedNodes.getNodeBefore(node)) {
                if (node.element == null) {
                    linkedNodes.removeNode(node);
                    return true;
                }
            }			
        } else {
            for (Node<E> node = getLastNode(); node != null; node = linkedNodes.getNodeBefore(node)) {
                if (o.equals(node.element)) {
                    linkedNodes.removeNode(node);
                    return true;
                }
            }			
        }
        return false;
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
     * Retrieves and removes the first element of this list,
     * or returns {@code null} if this list is empty.
     *
     * @return the first element of this list, or {@code null} if
     *     this list is empty
     */
    @Override
    public E pollFirst() {
        return (longSize() == 0L) ? null : removeFirst();
    }

    /**
     * Retrieves and removes the last element of this list,
     * or returns {@code null} if this list is empty.
     *
     * @return the last element of this list, or {@code null} if
     *     this list is empty
     */
    @Override
    public E pollLast() {
        return (longSize() == 0L) ? null : removeLast();
    }

    /**
     * Sorts this list according to the order induced by the specified
     * {@code Comparator}.
     *
     * <p>All elements in this list must be <i>mutually comparable</i> using the
     * specified comparator (that is, {@code c.compare(e1, e2)} must not throw
     * a {@code ClassCastException} for any elements {@code e1} and {@code e2}
     * in the list).
     *
     * <p>If the specified comparator is {@code null} then all elements in this
     * list must implement the {@code Comparable} interface and the elements'
     * natural ordering should be used.
     * 
     * <p><strong>Implementation Specification:</strong>
     * This implementation obtains an array containing all nodes in this list,
     * sorts the array using {@code Arrays.sort(T[] a, Comparator<? super T> c)},
     * and then effectively clears the list and puts the sorted nodes from the array
     * back into this list in order. In other words, the nodes of this list are
     * effectively reordered.
     * 
     * <p><strong>Implementation Note:</strong>
     * This implementation is a stable, adaptive, iterative mergesort that
     * requires far fewer than n lg(n) comparisons when the input array is
     * partially sorted, while offering the performance of a traditional
     * mergesort when the input array is randomly ordered.  If the input array
     * is nearly sorted, the implementation requires approximately n
     * comparisons.  Temporary storage requirements vary from a small constant
     * for nearly sorted input arrays to n/2 object references for randomly
     * ordered input arrays.
     *
     * <p>The implementation takes equal advantage of ascending and
     * descending order in its input array, and can take advantage of
     * ascending and descending order in different parts of the same
     * input array.  It is well-suited to merging two or more sorted arrays:
     * simply concatenate the arrays and sort the resulting array.
     *
     * <p>The implementation was adapted from Tim Peters's list sort for Python
     * (<a href="http://svn.python.org/projects/python/trunk/Objects/listsort.txt">
     * TimSort</a>).  It uses techniques from Peter McIlroy's "Optimistic
     * Sorting and Information Theoretic Complexity", in Proceedings of the
     * Fourth Annual ACM-SIAM Symposium on Discrete Algorithms, pp 467-474,
     * January 1993.
     *
     * @see Arrays#sort
     * @param comparator the {@code Comparator} used to compare list elements.
     *                   A {@code null} value indicates that the elements'
     *                   natural ordering should be used
     * @throws ClassCastException if the list contains elements that are not
     *         {@code mutually comparable} using the specified comparator
     * @throws IllegalStateException if the list is too large to fit in an array
     */
    @Override
    public void sort(Comparator<? super E> comparator) {
        if (comparator == null) {
            linkedNodes.sort(null);			
        } else {
            linkedNodes.sort((node1, node2) -> { return comparator.compare(node1.element, node2.element); });			
        }
    }
    
    /**
     * Returns a view of the portion of this list between the specified
     * {@code fromIndex}, inclusive, and {@code toIndex}, exclusive.  (If
     * {@code fromIndex} and {@code toIndex} are equal, the returned list is
     * empty.)  The returned list is backed by this list, so changes in the
     * returned sublist (structural or non-structural) are reflected in this
     * list. The returned sublist supports all of the optional list operations
     * supported by this list.
     *
     * <p>This method eliminates the need for explicit range operations (of
     * the sort that commonly exist for arrays).  Any operation that expects
     * a list can be used as a range operation by passing a subList view
     * instead of a whole list.  For example, the following idiom
     * removes a range of elements from a list:
     * <pre>{@code
     *      list.subList(from, to).clear();
     * }</pre>
     * Similar idioms may be constructed for {@code indexOf} and
     * {@code lastIndexOf}, and all of the algorithms in the
     * {@code Collections} class can be applied to a subList.
     *
     * <p>The semantics of the sublist returned by this method become undefined if
     * the backing list (i.e., this list) is <i>structurally modified</i> in
     * any way other than via the returned sublist.  (Structural modifications are
     * those that change the size of this list, or otherwise perturb it in such
     * a fashion that iterations in progress may yield incorrect results.)
     * A {@code ConcurrentModificationException} is thrown for any operation on
     * the returned sublist if this list is structurally modified.
     *
     * @param fromIndex low endpoint (inclusive) of the subList
     * @param toIndex high endpoint (exclusive) of the subList
     * @return a view of the specified range within this list
     * @throws IndexOutOfBoundsException for an illegal endpoint index value
     *         ({@code fromIndex < 0 || toIndex > size ||
     *         fromIndex > toIndex})
     */
    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        return linkedNodes.newSubList(fromIndex, toIndex);
    }
    
    /**
     * Returns a view of the portion of this list between the specified
     * {@code fisrtNode}, and {@code lastNode} (both inclusive).
     * The returned list is backed by this list, so changes in the returned
     * sublist (structural or non-structural) are reflected in this list.
     * The returned sublist supports all of the optional list operations
     * supported by this list.
     * 
     * <p> If the specified {@code firstNode} is {@code null}, the first node of the
     * this list will be used as the first node of the sublist. If the specified
     * {@code lastNode} is {@code null}, the last node of this list will be used as the
     * last node of the sublist. Note, the only way to produce an empty sublist is if
     * this list is empty and {@code null} is specified for both the {@code firstNode}
     * and the {@code lastNode}.
     * 
     * <p><strong>Implementation Note:</strong>
     * For performance reasons, this implementation might not verify that the specified
     * {@code lastNode} comes after the specified {@code firstNode} in this list.
     * Therefore, an {@code IllegalStateException} can be thrown for any operation on the
     * returned sublist indicating that the end of the list was reached unexpectedly. it is
     * also possible that it is never detected that the {@code lastNode} comes before the
     * {@code firstNode}.
     *
     * <p>This method eliminates the need for explicit range operations (of
     * the sort that commonly exist for arrays).  Any operation that expects
     * a list can be used as a range operation by passing a subList view
     * instead of a whole list.  For example, the following idiom
     * removes a range of elements from a list:
     * <pre>{@code
     *      list.subList(first, last).clear();
     * }</pre>
     * Similar idioms may be constructed for {@code indexOf} and
     * {@code lastIndexOf}, and all of the algorithms in the
     * {@code Collections} class can be applied to a subList.
     *
     * <p>The semantics of the sublist returned by this method become undefined if
     * the backing list (i.e., this list) is <i>structurally modified</i> in
     * any way other than via the returned sublist.  (Structural modifications are
     * those that change the size of this list, or otherwise perturb it in such
     * a fashion that iterations in progress may yield incorrect results.)
     * A {@code ConcurrentModificationException} is thrown for any operation on
     * the returned sublist if this list is structurally modified.
     *
     * @param firstNode low endpoint (inclusive) of the subList
     * @param lastNode high endpoint (inclusive) of the subList
     * @return a view of the specified range within this list
     * @throws IllegalArgumentException if any specified node is not linked to this list or
     *                                  if the lastNode comes before the firstNode in this list.
     */
    public SubList subList(Node<E> firstNode, Node<E> lastNode) {
        return linkedNodes.newSubList(firstNode, lastNode);
    }    

    /**
     * Returns an array containing all of the elements in this list
     * in proper sequence (from first to last element).
     *
     * <p>The returned array will be "safe" in that no references to it are
     * maintained by this list.  (In other words, this method must allocate
     * a new array).  The caller is thus free to modify the returned array.
     *
     * <p>This method acts as bridge between array-based and collection-based
     * APIs.
     *
     * @return an array containing all of the elements in this list
     *         in proper sequence
     * @throws IllegalStateException if the list is too large to fit in an array
     */
    @Override
    public Object[] toArray() {
        if (longSize() > Integer.MAX_VALUE) throw new IllegalStateException("list size (" + longSize() + ") is too large to fit in an array");
        final Object[] elements = new Object[size()];
        int index = 0;
        for (Node<E> node = getFirstNode(); node != null; node = linkedNodes.getNodeAfter(node)) elements[index++] = node.element;
        return elements;
    }

    /**
     * Returns an array containing all of the elements in this list in
     * proper sequence (from first to last element); the runtime type of
     * the returned array is that of the specified array.  If the list fits
     * in the specified array, it is returned therein.  Otherwise, a new
     * array is allocated with the runtime type of the specified array and
     * the size of this list.
     *
     * <p>If the list fits in the specified array with room to spare (i.e.,
     * the array has more elements than the list), the element in the array
     * immediately following the end of the list is set to {@code null}.
     * (This is useful in determining the length of the list <i>only</i> if
     * the caller knows that the list does not contain any null elements.)
     *
     * <p>Like the {@link #toArray()} method, this method acts as bridge between
     * array-based and collection-based APIs.  Further, this method allows
     * precise control over the runtime type of the output array, and may,
     * under certain circumstances, be used to save allocation costs.
     *
     * <p>Suppose <i>x</i> is a list known to contain only strings.
     * The following code can be used to dump the list into a newly
     * allocated array of {@code String}:
     *
     * <pre>
     *     String[] y = x.toArray(new String[0]);</pre>
     *
     * Note that {@code toArray(new Object[0])} is identical in function to
     * {@code toArray()}.
     *
     * @param array the array into which the elements of the list are to
     *          be stored, if it is big enough; otherwise, a new array of the
     *          same runtime type is allocated for this purpose.
     * @return an array containing the elements of the list
     * @throws ArrayStoreException if the runtime type of the specified array
     *         is not a supertype of the runtime type of every element in
     *         this list
     * @throws IllegalStateException if the list is too large to fit in an array
     * @throws NullPointerException if the specified array is null
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] array) {
        if (longSize() > Integer.MAX_VALUE) throw new IllegalStateException("list size (" + longSize() + ") is too large to fit in an array");
        if (array.length < size()) array = (T[]) java.lang.reflect.Array.newInstance(array.getClass().getComponentType(),size());
        int index = 0;
        Object[] elements = array;
        for (Node<E> node = getFirstNode(); node != null; node = linkedNodes.getNodeAfter(node)) elements[index++] = node.element;			
        if (array.length > size()) array[size()] = null;
        return array;
    }	

    /**
     * Returns a list-iterator of the elements in this list (in proper
     * sequence), starting at the specified position in the list.
     * Obeys the general contract of {@code List.listIterator(int)}.
     * 
     * <p><strong>Implementation Note:</strong>
     * The ListIterator returned by this method behaves differently when the
     * list's {@code size > Integer.MAX_VALUE}. Methods {@code nextIndex} and
     * {@code previousIndex} return -1 if the {@code index > Integer_MAX_VALUE}.
     *
     * <p>The list-iterator is <i>fail-fast</i>: if the list is structurally
     * modified at any time after the Iterator is created, in any way except
     * through the list-iterator's own {@code remove} or {@code add}
     * methods, the list-iterator will throw a
     * {@code ConcurrentModificationException}.  Thus, in the face of
     * concurrent modification, the iterator fails quickly and cleanly, rather
     * than risking arbitrary, non-deterministic behavior at an undetermined
     * time in the future.
     *
     * @param index index of the first element to be returned from the
     *              list-iterator (by a call to {@code next})
     * @return a ListIterator of the elements in this list (in proper
     *         sequence), starting at the specified position in the list
     * @throws IndexOutOfBoundsException if the index is out of range {@code (index < 0 || index > longSize())}
     * @see List#listIterator(int)
     */
    @Override
    public ListIterator<E> listIterator(int index) {
        if (index < 0 || index > longSize()) throw new IndexOutOfBoundsException("index=" + index + ", size=" + longSize());
        return new NodableLinkedListListIterator(index);
    }

    /**
     * Returns a list-iterator of the elements in this list (in proper
     * sequence), starting at the specified node in the list.
     * if the specified node is {@code null}, the list-iterator will
     * start with the first node in this list.    
     * 
     * <p><strong>Implementation Note:</strong>
     * The index returned by {@code nextIndex} and {@code previousIndex}
     * is relative to the specified node which has an index of 0.
     * Nodes which come before the specified node in this list, will
     * have a negative index; nodes that come after will have a positive index. 
     * Method {@code nextIndex} returns {@code longSize()} if at the end of the list, and
     * method {@code previousIndex} returns {@code -longSize()} if at the beginning
     * of the list. if {@code index < Integer.MAX_VALUE or index > Integer.MAX_VALUE},
     * {@code Integer.MIN_VALUE} or {@code Integer.MAX_VALUE} is returned respectively.
     *
     * <p>The list-iterator is <i>fail-fast</i>: if the list is structurally
     * modified at any time after the Iterator is created, in any way except
     * through the list-iterator's own {@code remove} or {@code add}
     * methods, the list-iterator will throw a
     * {@code ConcurrentModificationException}.  Thus, in the face of
     * concurrent modification, the iterator fails quickly and cleanly, rather
     * than risking arbitrary, non-deterministic behavior at an undetermined
     * time in the future.
     *
     * @param node node of the first element to be returned from the list-iterator
     *             (by a call to {@code next})
     * @return a ListIterator of the elements in this list (in proper
     *         sequence), starting at the specified node in the list
     * @throws IllegalArgumentException if the specified node is not linked to this list
     */
    public ListIterator<E> listIterator(Node<E> node) {
        if (node != null && !linkedNodes.contains(node)) throw new IllegalArgumentException("Specified node is not linked to this list");
        return new NodableLinkedListListIterator(node);
    }

    /**
     * Returns an iterator over the elements in this list in reverse
     * sequential order.  The elements will be returned in order from
     * last (tail) to first (head).
     *
     * @return an iterator over the elements in this list in reverse
     * sequence
     */
    @Override
    public Iterator<E> descendingIterator() {
        return new NodableLinkedListDescendingIterator();
    }

    /**
     * Creates a <i>late-binding</i> and <i>fail-fast</i> {@link Spliterator}
     * over the elements in this list.
     *
     * <p>The {@code Spliterator} reports {@link Spliterator#SIZED} and
     * {@link Spliterator#ORDERED}.  Overriding implementations should document
     * the reporting of additional characteristic values.
     *
     * <p><strong>Implementation Note:</strong>
     * The {@code Spliterator} additionally reports {@link Spliterator#SUBSIZED}
     * and implements {@code trySplit} to permit limited parallelism..
     *
     * @return a {@code Spliterator} over the elements in this list
     */
    @Override
    public Spliterator<E> spliterator() {
        return new NodableLinkedListSpliterator();
    }

    /**
     * Doubly-linked list of nodes which back a {@code NodableLinkedList}.
     * Implements the {@code List} and {@code Deque} interfaces.
     * The elements are of type {@code NodableLinkedList.Node},
     * and are never {@code null}.
     * 
     * Every instance of a {@code NodableLinkedList} has one and only one
     * {@code LinkedNodes} object backing it. Method {@link NodableLinkedList#linkedNodes()}
     * returns the {@code LinkedNodes} object backing a {@code NodableLinkedList}
     * instance.
     *
     * <p>All of the operations perform as could be expected for a doubly-linked
     * list.  Operations that index into the list will traverse the list from
     * the beginning or the end, whichever is closer to the specified index.
     *
     * <p>The iterators returned by this class's {@code iterator} and
     * {@code listIterator} methods are <i>fail-fast</i>: if the list is
     * structurally modified at any time after the iterator is created, in
     * any way except through the Iterator's own {@code remove} or
     * {@code add} methods, the iterator will throw a {@link
     * ConcurrentModificationException}.  Thus, in the face of concurrent
     * modification, the iterator fails quickly and cleanly, rather than
     * risking arbitrary, non-deterministic behavior at an undetermined
     * time in the future.
     *
     * <p>Note that the fail-fast behavior of an iterator cannot be guaranteed
     * as it is, generally speaking, impossible to make any hard guarantees in the
     * presence of unsynchronized concurrent modification.  Fail-fast iterators
     * throw {@code ConcurrentModificationException} on a best-effort basis.
     * Therefore, it would be wrong to write a program that depended on this
     * exception for its correctness:   <i>the fail-fast behavior of iterators
     * should be used only to detect bugs.</i>
     *
     * @author  James Pfeifer
     * @see     List
     * @see		Node
     *
     */
    public class LinkedNodes
        extends AbstractSequentialList<Node<E>>
        implements List<Node<E>>, Deque<Node<E>>
    {

        private long size;
        private final Node<E> headSentinel = new Node<>();
        private final Node<E> tailSentinel = new Node<>();

        //protected int modCount inherited from class java.util.AbstractList		
        private void incrementModCount() {
            NodableLinkedList.this.modCount = ++modCount; // keep both modCounts in sync
        }

        private LinkedNodes() {
            NodableLinkedList.this.modCount = modCount = 0;
            size = 0L;
            headSentinel.linkedNodes = this;
            tailSentinel.linkedNodes = this;
            headSentinel.next = tailSentinel;		
            tailSentinel.previous = headSentinel;			
        }
        private LinkedNodes(Collection<? extends Node<E>> collection) {
            this();
            addAll(collection);
        }

        /**
         * Returns the {@code NodableLinkedList} this {@code LinkedNodes} instance backs.
         *         
         * @return the {@code NodableLinkedList} this {@code LinkedNodes} instance backs.
         */
        public NodableLinkedList<E> nodableLinkedList() {
            return NodableLinkedList.this;
        }		

        /**
         * Returns the number of nodes in this list.  If this list contains more than
         * Integer.MAX_VALUE nodes, returns Integer.MAX_VALUE.
         *
         * @return the number of nodes in this list.
         */
        @Override
        public int size() {
            return (size > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int)size;
        }

        /**
         * Returns the number of nodes in this list.
         *
         * @return the number of nodes in this list.
         */
        public long longSize() {
            return size;
        }		

        private void addNodeAfter(Node<E> node, Node<E> afterThisNode) {
            // assert node != null && !node.isLinked() : "Node is null or already an element of a list";
            // assert this.contains(afterThisNode) : "After Node is not an element of this list";			
            incrementModCount();			
            node.linkedNodes = this;
            node.next = afterThisNode.next;
            node.previous = afterThisNode;
            node.previous.next = node;
            node.next.previous = node;
            size++;
        }

        private void addNodeBefore(Node<E> node, Node<E> beforeThisNode) {
            // assert node != null && !node.isLinked() : "Node is null or already an element of a list";
            // assert this.contains(beforeThisNode) : "Before Node is not an element of this list";			
            incrementModCount();			
            node.linkedNodes = this;
            node.next = beforeThisNode;
            node.previous = beforeThisNode.previous;
            node.previous.next = node;
            node.next.previous = node;
            size++;
        }

        private void removeNode(Node<E> node) {
            // assert this.contains(node) : "Node is not an element of this list";			
            incrementModCount();			
            node.next.previous = node.previous;
            node.previous.next = node.next;
            node.next = null;		
            node.previous = null;
            node.linkedNodes = null;
            size--;
        }

        private void replaceNode(Node<E> node, Node<E> replacementNode) {
            // assert this.contains(node) : "Node is not an element of this list";
            // assert replacementNode != null && !replacementNode.isLinked() : "Replacement Node is null or already an element of a list";
            incrementModCount();
            replacementNode.linkedNodes = this;
            replacementNode.next = node.next;
            replacementNode.previous = node.previous;
            replacementNode.previous.next = replacementNode;
            replacementNode.next.previous = replacementNode;
            node.next = null;		
            node.previous = null;
            node.linkedNodes = null;
        }

        private boolean hasNodeAfter(Node<E> node) {
            // assert this.contains(node) : "Node is not an element of this list";			
            return (node.next == tailSentinel) ? false : true; 
        }

        private boolean hasNodeBefore(Node<E> node) {
            // assert this.contains(node) : "Node is not an element of this list";			
            return (node.previous == headSentinel) ? false : true; 
        }	

        private Node<E> getNodeAfter(Node<E> node) {
            // assert this.contains(node) : "Node is not an element of this list";			
            return (node.next == tailSentinel) ? null : node.next;
        }

        private Node<E> getNodeBefore(Node<E> node) {
            // assert this.contains(node) : "Node is not an element of this list";			
            return (node.previous == headSentinel) ? null : node.previous;
        }

        private long indexOfNode(Node<E> node) {
            // assert this.contains(node) : "Node is not an element of this list";
            long index = 0L;
            Node<E> cursorNode = headSentinel.next;
            for (; cursorNode != tailSentinel; cursorNode = cursorNode.next, index++) {
                if (cursorNode == node) return index;				
            }
            return -1L;
        }

        /**
         * Inserts the specified node at the specified position in this list.
         * Shifts the node currently at that position (if any) and any
         * subsequent nodes to the right (adds one to their indices).
         *
         * @param index index at which the specified node is to be inserted
         * @param node node to be inserted
         * @throws IndexOutOfBoundsException if the index is out of range {@code (index < 0 || index > longSize())}
         * @throws IllegalArgumentException if node is null or already an element of a list
         */
        @Override
        public void add(int index, Node<E> node) {
            if (index == size) {
                addLast(node);
            } else {
                if (node == null || node.isLinked()) throw new IllegalArgumentException("Node is null or already an element of a list");
                addNodeBefore(node, get(index));
            }		
        }	

        /**
         * Returns the node at the specified position in this list.
         *
         * @param index index of the element to return
         * @return the node at the specified position in this list
         * @throws IndexOutOfBoundsException if the index is out of range {@code (index < 0 || index >= longSize())}
         */
        @Override
        public Node<E> get(int index) {
            final long getIndex = index;
            if (getIndex < 0L || getIndex >= size) throw new IndexOutOfBoundsException("index=" + index + ", size=" + size);		
            Node<E> node;
            long nodeIndex;
            final long lastIndex = size - 1L;
            if (getIndex < (lastIndex >> 1)) {
                for (node = getFirst(), nodeIndex = 0L;		    nodeIndex < getIndex && node != tailSentinel;	nodeIndex++, node = node.next);
            } else {
                for (node = getLast(),  nodeIndex = lastIndex;	nodeIndex > getIndex && node != headSentinel;	nodeIndex--, node = node.previous);
            }		
            return node;		
        }

        /**
         * Returns {@code true} if this list contains the specified node.
         * 
         * <p>This operation is performed in constant time.
         *
         * @param o node whose presence in this list is to be tested
         * @return {@code true} if this list contains the specified node
         */
        @Override
        public boolean contains(Object o) {
            if (!Node.class.isInstance(o)) return false;
            return contains((Node<?>) o);
        }
        
        private boolean contains(Node<?> node) {
             return (node != null && node.linkedNodes == this) ? true : false;
        }

        /**
         * Returns the index of the specified node in this list,
         * or -1 if there is no such index (this list does not
         * contain the node or the {@code index > Integer.MAX_VALUE}).
         *
         * <p>Note that {@code indexOf} is identical in function to
         * {@code lastIndexOf}, except {@code indexOf} traverses
         * the list forwards from the beginning.
         * 
         * @param o node to search for
         * @return the index of the specified node in this list,
         *         or or -1 if there is no such index.
         */
        @Override
        public int indexOf(Object o) {
            if (!Node.class.isInstance(o)) return -1;
            @SuppressWarnings("unchecked")
            final Node<E> node = (Node<E>)o;
            if (!this.contains(node)) return -1;
            final long index = indexOfNode(node);
            return (index > Integer.MAX_VALUE) ? -1 : (int)index;
        }

        /**
         * Returns the index of the specified node in this list,
         * or -1 if there is no such index (this list does not
         * contain the node or the {@code index > Integer.MAX_VALUE}.
         * 
         * <p>Note that {@code lastIndexOf} is identical in function to
         * {@code indexOf}, except {@code lastIndexOf} traverses
         * the list backwards from the end.
         *
         * @param o node to search for
         * @return the index of the specified node in this list,
         *         or -1 if there is no such index.
         */
        @Override
        public int lastIndexOf(Object o) {
            if (!Node.class.isInstance(o)) return -1;
            if (!this.contains((Node<?>) o)) return -1;
            long index = size - 1L;
            for (Node<E> node = tailSentinel.previous; node != headSentinel; node = node.previous, index--) {
                if (node == o) return (index > Integer.MAX_VALUE) ? -1 : (int)index;
            }
            return -1;
        }

        /**
         * Removes the node at the specified position in this list.  Shifts any
         * subsequent nodes to the left (subtracts one from their indices).
         * Returns the node that was removed from the list.
         *
         * @param index the index of the node to be removed
         * @return the node previously at the specified position
         * @throws IndexOutOfBoundsException if the index is out of range {@code (index < 0 || index >= longSize())}
         */
        @Override
        public Node<E> remove(int index) {
            final Node<E> node = get(index);
            removeNode(node);
            return node;
        }

        /**
         * Replaces the node at the specified position in this list with the
         * specified node.
         *
         * @param index index of the node to replace
         * @param node node to be stored at the specified position
         * @return the node previously at the specified position
         * @throws IndexOutOfBoundsException if the index is out of range {@code (index < 0 || index >= longSize())}
         * @throws IllegalArgumentException if replacement node is null or already an element of a list
         */
        @Override
        public Node<E> set(int index, Node<E> node) {
            if (node == null || node.isLinked()) throw new IllegalArgumentException("Replacement Node is null or already an element of a list");			
            final Node<E> originalNode = get(index);
            replaceNode(originalNode, node);
            return originalNode;
        }	

        /**
         * Appends the specified node to the end of this list.
         *
         * @param node node to be appended to this list
         * @return {@code true} (as specified by {@link Collection#add})
         * @throws IllegalArgumentException if node is null or already an element of a list
         */
        @Override
        public boolean add(Node<E> node) {
            addLast(node);
            return true;
        }			

        /**
         * Appends all of the nodes in the specified collection to the end of
         * this list, in the order that they are returned by the specified
         * collection's iterator.  The behavior of this operation is undefined if
         * the specified collection is modified while the operation is in
         * progress.  (Note that this will occur if the specified collection is
         * this list, and it's nonempty.)
         *
         * @param nodes collection containing nodes to be added to this list
         * @return {@code true} if this list changed as a result of the call
         * @throws NullPointerException if the specified collection is null
         * @throws IllegalArgumentException if any node in the collection is null
         *                                  or already an element of a list
         */
        @Override
        public boolean addAll(Collection<? extends Node<E>> nodes) {
            final long initialSize = size;
            for (Node<E> node: nodes) addLast(node);
            return size != initialSize;
        }

        /**
         * Inserts all of the nodes in the specified collection into this
         * list, starting at the specified position.  Shifts the node
         * currently at that position (if any) and any subsequent nodes to
         * the right (increases their indices).  The new nodes will appear
         * in the list in the order that they are returned by the
         * specified collection's iterator.
         *
         * @param index index at which to insert the first node
         *              from the specified collection
         * @param nodes collection containing nodes to be added to this list
         * @return {@code true} if this list changed as a result of the call
         * @throws IllegalArgumentException if any node in the collection is null
         *                                  or already an element of a list
         * @throws IndexOutOfBoundsException if the index is out of range {@code (index < 0 || index > longSize())}
         * @throws NullPointerException if the specified collection is null
         */
        @Override
        public boolean addAll(int index, Collection<? extends Node<E>> nodes) {
            if (index < 0 || index > size) throw new IndexOutOfBoundsException("index=" + index + ", size=" + size);
            final long initialSize = size;
            if (index == size) {
                for (Node<E> node: nodes) addLast(node);
            } else {
                final Node<E> targetNode = get(index);
                for (Node<E> node: nodes) {
                    if (node == null || node.isLinked()) throw new IllegalArgumentException("Node in collection is null or already an element of a list");
                    addNodeBefore(node, targetNode);
                }
            }
            return size != initialSize;
        }

        /**
         * Inserts all of the nodes in the specified collection into this
         * list, before the specified node.  Shifts the node
         * currently at that position and any subsequent nodes to
         * the right (increases their indices).  The new nodes will appear
         * in the list in the order that they are returned by the
         * specified collection's iterator. if the specified node is null,
         * the nodes will be appended to the end of this list.
         * 
         * Note that {@code addAll(null, Collection)} is identical in function to
         * {@code addAll(Collection)}.
         *
         * @param node node the specified collection is to be inserted before
         * @param nodes collection containing nodes to be added to this list
         * @return {@code true} if this list changed as a result of the call
         * @throws IllegalArgumentException if the specified node is not linked to this list, or
         *                                  any node in the collection is null or already an element of a list
         * @throws NullPointerException if the specified collection is null
         */
        public boolean addAll(Node<E> node, Collection<? extends Node<E>> nodes) {
            if (node == null) return addAll(nodes);
            if (!this.contains(node)) throw new IllegalArgumentException("Specified node is not linked to this list");
            final long initialSize = longSize();
            for (Node<E> collectionNode: nodes) {
                if (collectionNode == null || collectionNode.isLinked()) throw new IllegalArgumentException("Node in collection is null or already an element of a list");
                linkedNodes.addNodeBefore(collectionNode, node);
            }
            return longSize() != initialSize;
        }		

        /**
         * Removes all of the nodes from this list.
         */
        @Override
        public void clear() {
            for (Node<E> node = headSentinel.next; node != tailSentinel;) {
                final Node<E> nodeToRemove = node;
                node = node.next;
                removeNode(nodeToRemove);
            }				
        }

        /**
         * Inserts the specified node at the beginning of this list.
         *
         * @param node the node to add
         * @throws IllegalArgumentException if node is null or already an element of a list
         */
        @Override
        public void addFirst(Node<E> node) {
            if (node == null || node.isLinked()) throw new IllegalArgumentException("Node is null or already an element of a list");
            addNodeAfter(node, headSentinel);
        }		

        /**
         * Appends the specified node to the end of this list.
         *
         * @param node the node to add
         * @throws IllegalArgumentException if node is null or already an element of a list
         */
        @Override
        public void addLast(Node<E> node) {
            if (node == null || node.isLinked()) throw new IllegalArgumentException("Node is null or already an element of a list");
            addNodeBefore(node, tailSentinel);
        }

        /**
         * Pushes a node onto the stack represented by this list.  In other
         * words, inserts the node at the front of this list.
         *
         * <p>This method is equivalent to {@link #addFirst}.
         *
         * @param node the node to push
         * @throws IllegalArgumentException if node is null or already an element of a list
         */
        @Override
        public void push(Node<E> node) {
            addFirst(node);
        }

        /**
         * Adds the specified node as the tail (last node) of this list.
         *
         * @param node the node to add
         * @return {@code true} (as specified by {@link java.util.Queue#offer})
         * @throws IllegalArgumentException if node is null or already an element of a list
         */
        @Override
        public boolean offer(Node<E> node) {
            return add(node);
        }

        /**
         * Inserts the specified node at the front of this list.
         *
         * @param node the node to insert
         * @return {@code true} (as specified by {@link Deque#offerFirst})
         * @throws IllegalArgumentException if node is null or already an element of a list
         */
        @Override
        public boolean offerFirst(Node<E> node) {
            addFirst(node);
            return true;
        }

        /**
         * Inserts the specified node at the end of this list.
         *
         * @param node the node to insert
         * @return {@code true} (as specified by {@link Deque#offerLast})
         * @throws IllegalArgumentException if node is null or already an element of a list
         */
        @Override
        public boolean offerLast(Node<E> node) {
            addLast(node);
            return true;
        }

        /**
         * Retrieves, but does not remove, the head (first node) of this list.
         * 
         * @return the head of this list
         * @throws NoSuchElementException if this list is empty
         */
        @Override
        public Node<E> element() {
            return getFirst();
        }

        /**
         * Returns the first node in this list.
         *
         * @return the first node in this list
         * @throws NoSuchElementException if this list is empty
         */
        @Override
        public Node<E> getFirst() {
            if (size == 0L) throw new NoSuchElementException("List is empty");
            return headSentinel.next;
        }

        /**
         * Returns the last node in this list.
         *
         * @return the last node in this list
         * @throws NoSuchElementException if this list is empty
         */
        @Override
        public Node<E> getLast() {
            if (size == 0L) throw new NoSuchElementException("List is empty");
            return tailSentinel.previous;
        }

        /**
         * Retrieves, but does not remove, the head (first node) of this list.
         * 
         * @return the head of this list, or {@code null} if this list is empty
         */
        @Override
        public Node<E> peek() {
            return peekFirst();
        }

        /**
         * Retrieves, but does not remove, the first node of this list,
         * or returns {@code null} if this list is empty.
         *
         * @return the first node of this list, or {@code null}
         *         if this list is empty
         */
        @Override
        public Node<E> peekFirst() {
            return (size == 0L) ? null : headSentinel.next;
        }

        /**
         * Retrieves, but does not remove, the last node of this list,
         * or returns {@code null} if this list is empty.
         *
         * @return the last node of this list, or {@code null}
         *         if this list is empty
         */
        @Override
        public Node<E> peekLast() {
            return (size == 0L) ? null : tailSentinel.previous;
        }

        /**
         * Pops a node from the stack represented by this list.  In other
         * words, removes and returns the first node of this list.
         *
         * <p>This method is equivalent to {@link #removeFirst()}.
         *
         * @return the node at the front of this list (which is the top
         *         of the stack represented by this list)
         * @throws NoSuchElementException if this list is empty
         */
        @Override
        public Node<E> pop() {
            return removeFirst();
        }

        /**
         * Removes and returns the head (first node) of this list.
         *
         * @return the head of this list
         * @throws NoSuchElementException if this list is empty
         */
        @Override
        public Node<E> remove() {
            return removeFirst();
        }

        /**
         * Removes and returns the first node from this list.
         *
         * @return the first node from this list
         * @throws NoSuchElementException if this list is empty
         */
        @Override
        public Node<E> removeFirst() {
            if (size == 0L) throw new NoSuchElementException("List is empty");
            final Node<E> node = headSentinel.next;
            removeNode(node);
            return node;
        }

        /**
         * Removes and returns the last node from this list.
         *
         * @return the last node from this list
         * @throws NoSuchElementException if this list is empty
         */
        @Override
        public Node<E> removeLast() {
            if (size == 0L) throw new NoSuchElementException("List is empty");
            final Node<E> node = tailSentinel.previous;
            removeNode(node);
            return node;
        }

        /**
         * Removes the specified node from this list, if it is present.
         * If this list does not contain the element, it is unchanged.
         * 
         * <p>This operation performs in constant time.
         *
         * @param o node to be removed from this list, if present
         * @return {@code true} if this list contained the specified element
         */
        @Override
        public boolean remove(Object o) {
            if (!Node.class.isInstance(o)) return false;
            @SuppressWarnings("unchecked")
            final Node<E> node = (Node<E>)o;
            if (!this.contains(node)) return false;
            removeNode(node);
            return true;		
        }

        /**
         * Removes the specified node from this list, if it is present.
         * If this list does not contain the element, it is unchanged.
         * 
         * <p>This operation performs in constant time.
         * 
         * <p>This method is equivalent to {@link #remove(Object o)}.
         *
         * @param o node to be removed from this list, if present
         * @return {@code true} if this list contained the specified element
         */
        @Override
        public boolean removeFirstOccurrence(Object o) {
            return remove(o);
        }

        /**
         * Removes the specified node from this list, if it is present.
         * If this list does not contain the element, it is unchanged.
         * 
         * <p>This operation performs in constant time.
         * 
         * <p>This method is equivalent to {@link #remove(Object o)}.
         *
         * @param o node to be removed from this list, if present
         * @return {@code true} if this list contained the specified element
         */
        @Override
        public boolean removeLastOccurrence(Object o) {
            return remove(o);
        }		

        /**
         * Retrieves and removes the head (first node) of this list
         * 
         * @return the head of this list, or {@code null} if this list is empty
         */
        @Override
        public Node<E> poll() {
            return pollFirst();
        }

        /**
         * Retrieves and removes the first node of this list,
         * or returns {@code null} if this list is empty.
         *
         * @return the first node of this list, or {@code null} if
         *     this list is empty
         */
        @Override
        public Node<E> pollFirst() {
            if (size == 0L) return null;
            final Node<E> node = headSentinel.next;
            removeNode(node);
            return node;
        }

        /**
         * Retrieves and removes the last node of this list,
         * or returns {@code null} if this list is empty.
         *
         * @return the last node of this list, or {@code null} if
         *     this list is empty
         */
        @Override
        public Node<E> pollLast() {
            if (size == 0L) return null;
            final Node<E> node = tailSentinel.previous;
            removeNode(node);
            return node;
        }

        /**
         * Sorts this list according to the order induced by the specified
         * {@code Comparator}.
         *
         * <p>All elements in this list must be <i>mutually comparable</i> using the
         * specified comparator (that is, {@code c.compare(e1, e2)} must not throw
         * a {@code ClassCastException} for any elements {@code e1} and {@code e2}
         * in the list).
         *
         * <p>If the specified comparator is {@code null} then all elements in this
         * list must implement the {@code Comparable} interface and the elements'
         * natural ordering should be used.
         *
         * <p><strong>Implementation Specification:</strong>
         * This implementation obtains an array containing all nodes in this list,
         * sorts the array using {@code Arrays.sort(T[] a, Comparator<? super T> c)},
         * and then effectively clears the list and puts the sorted nodes from the array
         * back into this list in order. In other words, the nodes of this list are
         * effectively reordered.
         * 
         * <p><strong>Implementation Note:</strong>
         * This implementation is a stable, adaptive, iterative mergesort that
         * requires far fewer than n lg(n) comparisons when the input array is
         * partially sorted, while offering the performance of a traditional
         * mergesort when the input array is randomly ordered.  If the input array
         * is nearly sorted, the implementation requires approximately n
         * comparisons.  Temporary storage requirements vary from a small constant
         * for nearly sorted input arrays to n/2 object references for randomly
         * ordered input arrays.
         *
         * <p>The implementation takes equal advantage of ascending and
         * descending order in its input array, and can take advantage of
         * ascending and descending order in different parts of the same
         * input array.  It is well-suited to merging two or more sorted arrays:
         * simply concatenate the arrays and sort the resulting array.
         *
         * <p>The implementation was adapted from Tim Peters's list sort for Python
         * (<a href="http://svn.python.org/projects/python/trunk/Objects/listsort.txt">
         * TimSort</a>).  It uses techniques from Peter McIlroy's "Optimistic
         * Sorting and Information Theoretic Complexity", in Proceedings of the
         * Fourth Annual ACM-SIAM Symposium on Discrete Algorithms, pp 467-474,
         * January 1993.
         *
         * @see Arrays#sort
         * @param comparator the {@code Comparator} used to compare list nodes.
         *                   A {@code null} value indicates that the elements'
         *                   natural ordering should be used
         * @throws ClassCastException if the list contains elements that are not
         *         <i>mutually comparable</i> using the specified comparator
         * @throws IllegalStateException if the list is too large to fit in an array
         */
        @Override
        public void sort(Comparator<? super Node<E>> comparator) {
            if (size < 2L) return;
            if (size > Integer.MAX_VALUE) throw new IllegalStateException("too large to sort; size="+size);
            @SuppressWarnings("unchecked")
            final Node<E>[] sortedNodes = new Node[(int)size];
            int index = 0;
            for (Node<E> node = headSentinel.next; node != tailSentinel; node = node.next) sortedNodes[index++] = node;
            Arrays.sort(sortedNodes, comparator);
            Node<E> node;
            Node<E> sortedNode;
            for (index=0, node=getFirst(); index < sortedNodes.length-1; index++) {
                sortedNode = sortedNodes[index];
                swapNodes(node, sortedNode);
                // sortedNode has now replaced node in the list
                node = sortedNode.next; // node = node.next
            }
        }
        
        /**
         * Returns a view of the portion of this list between the specified
         * {@code fromIndex}, inclusive, and {@code toIndex}, exclusive.  (If
         * {@code fromIndex} and {@code toIndex} are equal, the returned list is
         * empty.)  The returned list is backed by this list, so changes in the
         * returned sublist (structural or non-structural) are reflected in this
         * list. The returned sublist supports all of the optional list operations
         * supported by this list.
         *
         * <p>This method eliminates the need for explicit range operations (of
         * the sort that commonly exist for arrays).  Any operation that expects
         * a list can be used as a range operation by passing a subList view
         * instead of a whole list.  For example, the following idiom
         * removes a range of elements from a list:
         * <pre>{@code
         *      list.subList(from, to).clear();
         * }</pre>
         * Similar idioms may be constructed for {@code indexOf} and
         * {@code lastIndexOf}, and all of the algorithms in the
         * {@code Collections} class can be applied to a subList.
         *
         * <p>The semantics of the sublist returned by this method become undefined if
         * the backing list (i.e., this list) is <i>structurally modified</i> in
         * any way other than via the returned sublist.  (Structural modifications are
         * those that change the size of this list, or otherwise perturb it in such
         * a fashion that iterations in progress may yield incorrect results.)
         * A {@code ConcurrentModificationException} is thrown for any operation on
         * the returned sublist if this list is structurally modified.
         *
         * @param fromIndex low endpoint (inclusive) of the subList
         * @param toIndex high endpoint (exclusive) of the subList
         * @return a view of the specified range within this list
         * @throws IndexOutOfBoundsException for an illegal endpoint index value
         *         ({@code fromIndex < 0 || toIndex > size ||
         *         fromIndex > toIndex})
         */        
        @Override
        public List<Node<E>> subList(int fromIndex, int toIndex) {
            return newSubList(fromIndex, toIndex).linkedSubNodes;
        }
        
        private SubList newSubList(int fromIndex, int toIndex) {
            if (fromIndex < 0) throw new IndexOutOfBoundsException("fromIndex = " + fromIndex);
            if (toIndex > longSize()) throw new IndexOutOfBoundsException("toIndex(" + toIndex +") > size(" + longSize() + ")");
            if (fromIndex > toIndex) throw new IllegalArgumentException("fromIndex(" + fromIndex +") > toIndex(" + toIndex + ")");
            final long size = toIndex - fromIndex;
            if (fromIndex == longSize()) return new SubList(tailSentinel.previous, tailSentinel, null, size);
            final Node<E> headSentinel = get(fromIndex).previous;
            final Node<E> tailSentinel = (size == 0L) ? headSentinel.next : null; // tailSentinel is currently unknown
            return new SubList(headSentinel, tailSentinel, null, size);
        }
        
        /**
         * Returns a view of the portion of this list between the specified
         * {@code fisrtNode}, and {@code lastNode} (both inclusive).
         * The returned list is backed by this list, so changes in the returned
         * sublist (structural or non-structural) are reflected in this list.
         * The returned sublist supports all of the optional list operations
         * supported by this list.
         * 
         * <p> If the specified {@code firstNode} is {@code null}, the first node of the
         * this list will be used as the first node of the sublist. If the specified
         * {@code lastNode} is {@code null}, the last node of this list will be used as the
         * last node of the sublist. Note, the only way to produce an empty sublist is if
         * this list is empty and {@code null} is specified for both the {@code firstNode}
         * and the {@code lastNode}.
         * 
         * <p><strong>Implementation Note:</strong>
         * For performance reasons, this implementation might not verify that the specified
         * {@code lastNode} comes after the specified {@code firstNode} in this list.
         * Therefore, an {@code IllegalStateException} can be thrown for any operation on the
         * returned sublist indicating that the end of the list was reached unexpectedly. it is
         * also possible that it is never detected that the {@code lastNode} comes before the
         * {@code firstNode}.
         *
         * <p>This method eliminates the need for explicit range operations (of
         * the sort that commonly exist for arrays).  Any operation that expects
         * a list can be used as a range operation by passing a subList view
         * instead of a whole list.  For example, the following idiom
         * removes a range of elements from a list:
         * <pre>{@code
         *      list.subList(first, last).clear();
         * }</pre>
         * Similar idioms may be constructed for {@code indexOf} and
         * {@code lastIndexOf}, and all of the algorithms in the
         * {@code Collections} class can be applied to a subList.
         *
         * <p>The semantics of the sublist returned by this method become undefined if
         * the backing list (i.e., this list) is <i>structurally modified</i> in
         * any way other than via the returned sublist.  (Structural modifications are
         * those that change the size of this list, or otherwise perturb it in such
         * a fashion that iterations in progress may yield incorrect results.)
         * A {@code ConcurrentModificationException} is thrown for any operation on
         * the returned sublist if this list is structurally modified.
         *
         * @param firstNode low endpoint (inclusive) of the subList
         * @param lastNode high endpoint (inclusive) of the subList
         * @return a view of the specified range within this list
         * @throws IllegalArgumentException if any specified node is not linked to this list or
         *                                  if the lastNode comes before the firstNode in this list.
         */
        public SubList.LinkedSubNodes subList(Node<E> firstNode, Node<E> lastNode) {
            return newSubList(firstNode, lastNode).linkedSubNodes;
        }
        
        private SubList newSubList(Node<E> firstNode, Node<E> lastNode) {
            if (firstNode == null) firstNode = this.headSentinel.next;
            if (lastNode == null) lastNode = this.tailSentinel.previous;
            if (!this.contains(firstNode)) throw new IllegalArgumentException("Specified first node is not linked to this list");
            if (!this.contains(lastNode)) throw new IllegalArgumentException("Specified last node is not linked to this list");
            if (lastNode.next == firstNode && firstNode != this.headSentinel.next) throw new IllegalArgumentException("Specified last Node comes before the specified first node in this list");
            final Node<E> headSentinel = firstNode.previous;
            final Node<E> tailSentinel = lastNode.next;
            final long size = (headSentinel.next == tailSentinel) ? 0L : -1L; // size is unknown
            return new SubList(headSentinel, tailSentinel, null, size);
        }

        /**
         * Returns an array containing all of the nodes in this list
         * in proper sequence (from first to last node). The nodes in the
         * array are still linked to this list.
         *
         * <p>The returned array will be "safe" in that no references to it are
         * maintained by this list.  (In other words, this method allocates
         * a new array).  The caller is thus free to modify the returned array.
         *
         * <p>This method acts as bridge between array-based and collection-based
         * APIs.
         *
         * @return an array containing all of the nodes in this list
         *         in proper sequence
         * @throws IllegalStateException if the list is too large to fit in an array
         */
        @Override
        public Object[] toArray() {
            if (size > Integer.MAX_VALUE) throw new IllegalStateException("list size (" + size + ") is too large to fit in an array");
            final Object[] nodes = new Object[(int)size];
            int index = 0;
            for (Node<E> node = headSentinel.next; node != tailSentinel; node = node.next) nodes[index++] = node;				
            return nodes;
        }

        /**
         * Returns an array containing all of the nodes in this list in
         * proper sequence (from first to last node); the runtime type of
         * the returned array is that of the specified array.  If the list fits
         * in the specified array, it is returned therein.  Otherwise, a new
         * array is allocated with the runtime type of the specified array and
         * the size of this list.
         *
         * <p>If the list fits in the specified array with room to spare (i.e.,
         * the array has more elements than the list), the element in the array
         * immediately following the end of the list is set to {@code null}.
         * (This is useful in determining the length of the list <i>only</i> if
         * the caller knows that the list does not contain any null elements.)
         *
         * <p>Like the {@link #toArray()} method, this method acts as bridge between
         * array-based and collection-based APIs.  Further, this method allows
         * precise control over the runtime type of the output array, and may,
         * under certain circumstances, be used to save allocation costs.
         *
         * Note that {@code toArray(new Object[0])} is identical in function to
         * {@code toArray()}.
         *
         * @param array the array into which the nodes of the list are to
         *              be stored, if it is big enough; otherwise, a new array of the
         *              same runtime type is allocated for this purpose.
         * @return an array containing the nodes of the list
         * @throws ArrayStoreException if the runtime type of the specified array
         *         is not a supertype of the runtime type of every node in
         *         this list
         * @throws IllegalStateException if the list is too large to fit in an array
         * @throws NullPointerException if the specified array is null
         */
        @Override
        @SuppressWarnings("unchecked")
        public <T> T[] toArray(T[] array) {
            if (size > Integer.MAX_VALUE) throw new IllegalStateException("list size (" + size + ") is too large to fit in an array");
            if (array.length < size) array = (T[]) java.lang.reflect.Array.newInstance(array.getClass().getComponentType(), (int)size);
            int index = 0;
            Object[] nodes = array;
            for (Node<E> node = headSentinel.next; node != tailSentinel; node = node.next) nodes[index++] = node;			
            if (array.length > size) array[(int)size] = null;				
            return array;
        }

        /**
         * Returns a list-iterator of the nodes in this list (in proper
         * sequence), starting at the specified position in the list.
         * Obeys the general contract of {@code List.listIterator(int)}.
         * 
         * <p><strong>Implementation Note:</strong>
         * The ListIterator returned by this method behaves differently when the
         * list's {@code size > Integer.MAX_VALUE}. Methods {@code nextIndex} and
         * {@code previousIndex} return -1 if the {@code index > Integer_MAX_VALUE}.
         *
         * <p>The list-iterator is <i>fail-fast</i>: if the list is structurally
         * modified at any time after the Iterator is created, in any way except
         * through the list-iterator's own {@code remove} or {@code add}
         * methods, the list-iterator will throw a
         * {@code ConcurrentModificationException}.  Thus, in the face of
         * concurrent modification, the iterator fails quickly and cleanly, rather
         * than risking arbitrary, non-deterministic behavior at an undetermined
         * time in the future.
         *
         * @param index index of the first node to be returned from the
         *              list-iterator (by a call to {@code next})
         * @return a ListIterator of the nodes in this list (in proper
         *         sequence), starting at the specified position in the list
         * @throws IndexOutOfBoundsException if the index is out of range {@code (index < 0 || index > longSize())}
         * @see List#listIterator(int)
         */
        @Override
        public ListIterator<Node<E>> listIterator(int index) {
            if (index < 0 || index > size) throw new IndexOutOfBoundsException("index=" + index + ", size=" + size);
            return linkedNodesListIterator(index);
        }

        /**
         * Returns a list-iterator of the nodes in this list (in proper
         * sequence), starting at the specified node in the list.
         * if the specified node is {@code null}, the list-iterator will
         * start with the first node in this list.
         * 
         * <p><strong>Implementation Note:</strong>
         * The index returned by {@code nextIndex} and {@code previousIndex}
         * is relative to the specified node which has an index of 0.
         * Nodes which come before the specified node in this list, will
         * have a negative index; nodes that come after will have a positive index. 
         * Method {@code nextIndex} returns {@code longSize()} if at the end of the list, and
         * method {@code previousIndex} returns {@code -longSize()} if at the beginning
         * of the list. if {@code index < Integer.MAX_VALUE or index > Integer.MAX_VALUE},
         * {@code Integer.MIN_VALUE} or {@code Integer.MAX_VALUE} is returned respectively.
         *
         * <p>The list-iterator is <i>fail-fast</i>: if the list is structurally
         * modified at any time after the Iterator is created, in any way except
         * through the list-iterator's own {@code remove} or {@code add}
         * methods, the list-iterator will throw a
         * {@code ConcurrentModificationException}.  Thus, in the face of
         * concurrent modification, the iterator fails quickly and cleanly, rather
         * than risking arbitrary, non-deterministic behavior at an undetermined
         * time in the future.
         *
         * @param node first node to be returned from the list-iterator (by a call to {@code next})
         * @return a ListIterator of the nodes in this list (in proper
         *         sequence), starting at the specified node in the list
         * @throws IllegalArgumentException if the specified is node not linked to this list
         */
        public ListIterator<Node<E>> listIterator(Node<E> node) {
            if (node != null && !this.contains(node)) throw new IllegalArgumentException("Specified is node not linked to this list");
            return linkedNodesListIterator(node);
        }

        /**
         * Returns an iterator over the nodes in this list in reverse
         * sequential order.  The elements will be returned in order from
         * last (tail) to first (head).
         *
         * @return an iterator over the nodes in this list in reverse
         *         sequence
         */
        @Override
        public Iterator<Node<E>> descendingIterator() {
            return new LinkedNodesDescendingIterator();
        }

        /**
         * Creates a <i>late-binding</i> and <i>fail-fast</i> {@link Spliterator}
         * over the nodes in this list.
         *
         * <p>The {@code Spliterator} reports {@link Spliterator#SIZED} and
         * {@link Spliterator#ORDERED}.  Overriding implementations should document
         * the reporting of additional characteristic values.
         *
         * <p><strong>Implementation Note:</strong>
         * The {@code Spliterator} additionally reports {@link Spliterator#SUBSIZED} and
         * {@link Spliterator#NONNULL}, and implements {@code trySplit} to permit
         * limited parallelism..
         *
         * @return a {@code Spliterator} over the nodes in this list
         */
        @Override
        public Spliterator<Node<E>> spliterator() {
            return linkedNodesSpliterator();
        }
        
        private LinkedNodesListIterator linkedNodesListIterator(long index) {
            //assert index >= -1 && index <= size : "index out of range; index=" + index + ", size=" + size;
            Node<E> node;
            long cursorIndex;
            if (index < 0 || index == size) {
                index = size;
                node = tailSentinel;
            } else {
                if (index <= (size >> 1)) {
                    cursorIndex = 0L;
                    node = headSentinel.next;
                    while (cursorIndex < index) { node = node.next; cursorIndex++; }
                } else {
                    cursorIndex = size - 1L;
                    node = tailSentinel.previous;
                    while (cursorIndex > index) { node = node.previous; cursorIndex--; }
                }
            }
            return linkedNodesListIterator(index, node, this.headSentinel, this.tailSentinel);
        }
        
        private LinkedNodesListIterator linkedNodesListIterator(long index, Node<E> node, Node<E> headSentinel, Node<E> tailSentinel) {
            return new LinkedNodesListIterator(
                    index, node,
                    (headSentinel == null) ? this.headSentinel : headSentinel,
                    (tailSentinel == null) ? this.tailSentinel : tailSentinel
                    );
        }
        
        private LinkedNodesListIterator linkedNodesListIterator(Node<E> node) {
            return linkedNodesListIterator(node, this.headSentinel, this.tailSentinel);
        }
        
        private LinkedNodesListIterator linkedNodesListIterator(Node<E> node, Node<E> headSentinel, Node<E> tailSentinel) {
            return new LinkedNodesListIterator(
                    (node == null) ? headSentinel.next : node,
                    (headSentinel == null) ? this.headSentinel : headSentinel,
                    (tailSentinel == null) ? this.tailSentinel : tailSentinel
                    );
        }

        private class LinkedNodesListIterator implements ListIterator<Node<E>> {
            
            private final Node<E> headSentinel;
            private final Node<E> tailSentinel;

            private long cursorIndex;
            private Node<E> cursorNode;
            private Node<E> targetNode;
            private int expectedModCount = modCount;
            private boolean relativeIndex = false;
            
            private LinkedNodesListIterator(long index, Node<E> node, Node<E> headSentinel, Node<E> tailSentinel) {
                // assert index >= 0 && index <= size : "index out of range; index=" + index + ", size=" + size;
                // assert node != null && node.linkedNodes == LinkedNodes.this : "Specified node is null or is not linked to this list";
                // assert headSentinel != null && headSentinel.linkedNodes == LinkedNodes.this : "head sentinel is null or is not linked to this list";
                // assert tailSentinel != null && tailSentinel.linkedNodes == LinkedNodes.this : "tail sentinel is null or is not linked to this list";
                // sublists:
                //   . not guaranteed that the tailSentinel comes after the headSentinel in the list
                //   . guaranteed that the node comes after the headSentinel in the list and
                //                             not after the tailSentinel unless the tailSentinel comes before the headSentinel
                this.headSentinel = headSentinel;
                this.tailSentinel = tailSentinel;
                cursorIndex = index - 1;
                cursorNode = node.previous;
                targetNode = null;
            }
            
            private LinkedNodesListIterator(Node<E> node, Node<E> headSentinel, Node<E> tailSentinel) {
                // assert node != null && node.linkedNodes == LinkedNodes.this : "Specified node is null or is not linked to this list";
                // assert headSentinel != null && headSentinel.linkedNodes == LinkedNodes.this : "head sentinel is null or is not linked to this list";
                // assert tailSentinel != null && tailSentinel.linkedNodes == LinkedNodes.this : "tail sentinel is null or is not linked to this list";                
                // sublists:
                //   . not guaranteed that the tailSentinel comes after the headSentinel in the list
                //   . guaranteed that the node comes after the headSentinel in the list and 
                //                             not after the tailSentinel unless the tailSentinel comes before the headSentinel
                this.headSentinel = headSentinel;
                this.tailSentinel = tailSentinel;
                cursorIndex = -1L;
                cursorNode = node.previous;
                targetNode = null;
                relativeIndex = true;
            }
            
            private long cursorIndex() {
                return cursorIndex;
            }
            
            private Node<E> cursorNode() {
                return cursorNode;
            }
            
            private Node<E> targetNode() {
                if (modCount != expectedModCount) throw new ConcurrentModificationException();
                if (targetNode == null) throw new IllegalStateException("Neither next nor previous have been called, or remove or add have been called after the last call to next or previous");
                return targetNode;
            }

            @Override
            public boolean hasNext() {
                if (modCount != expectedModCount) throw new ConcurrentModificationException();
                return (cursorNode.next != tailSentinel && cursorNode.next != LinkedNodes.this.tailSentinel);
            }

            @Override
            public boolean hasPrevious() {
                if (modCount != expectedModCount) throw new ConcurrentModificationException();
                return (cursorNode != headSentinel);
            }

            @Override
            public Node<E> next() {
                if (modCount != expectedModCount) throw new ConcurrentModificationException();
                targetNode = null;
                if (!hasNext()) throw new NoSuchElementException();
                cursorNode = cursorNode.next;
                cursorIndex++;
                targetNode = cursorNode;
                return targetNode;
            }

            @Override
            public Node<E> previous() {
                if (modCount != expectedModCount) throw new ConcurrentModificationException();
                targetNode = null;
                if (!hasPrevious()) throw new NoSuchElementException();
                targetNode = cursorNode;
                cursorNode = cursorNode.previous;
                cursorIndex--;
                return targetNode;
            }		

            @Override
            public int nextIndex() {
                if (modCount != expectedModCount) throw new ConcurrentModificationException();
                if (relativeIndex) {
                    if (!hasNext()) return (size >= Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int)size;
                    if (cursorIndex+1 > Integer.MAX_VALUE) return Integer.MAX_VALUE;
                    if (cursorIndex+1 < Integer.MIN_VALUE) return Integer.MIN_VALUE;
                } else {
                    // absolute index
                    if (!hasNext()) return (size > Integer.MAX_VALUE) ? -1 : (int)size;
                    if (cursorIndex+1 > Integer.MAX_VALUE) return -1;
                }
                return (int)(cursorIndex+1);
            }

            @Override
            public int previousIndex() {
                if (modCount != expectedModCount) throw new ConcurrentModificationException();
                if (relativeIndex) {
                    if (!hasPrevious()) return (size <= Integer.MIN_VALUE) ? Integer.MIN_VALUE : -(int)size;
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
                if (modCount != expectedModCount) throw new ConcurrentModificationException();
                if (node == null || node.isLinked()) throw new IllegalArgumentException("Node is null or already an element of a list");
                addNodeAfter(node, cursorNode);
                cursorIndex++;
                cursorNode = node;
                targetNode = null;
                expectedModCount = modCount;
            }

            @Override
            public void remove() {
                if (modCount != expectedModCount) throw new ConcurrentModificationException();
                if (targetNode == null) throw new IllegalStateException("Neither next nor previous have been called, or remove or add have been called after the last call to next or previous");
                if (cursorNode == targetNode) {
                    cursorIndex--;
                    cursorNode = cursorNode.previous;
                }
                removeNode(targetNode);
                targetNode = null;
                expectedModCount = modCount;
            }

            @Override
            public void set(Node<E> node) {
                if (modCount != expectedModCount) throw new ConcurrentModificationException();
                if (targetNode == null) throw new IllegalStateException("Neither next nor previous have been called, or remove or add have been called after the last call to next or previous");
                if (node == null || node.isLinked()) throw new IllegalArgumentException("Replacement Node is null or already an element of a list");
                if (cursorNode == targetNode) cursorNode = node;
                replaceNode(targetNode, node);
                targetNode = node;
                expectedModCount = modCount;
            }

            @Override
            public void forEachRemaining(Consumer<? super Node<E>> action) {
                if (modCount != expectedModCount) throw new ConcurrentModificationException();
                if (action == null) throw new NullPointerException();
                while (modCount == expectedModCount && (cursorNode.next != tailSentinel && cursorNode.next != LinkedNodes.this.tailSentinel)) {
                    action.accept(cursorNode.next);
                    cursorNode = cursorNode.next;
                    cursorIndex++;
                    targetNode = cursorNode;                    
                }
                if (modCount != expectedModCount) throw new ConcurrentModificationException();
            }

        } // LinkedNodesListIterator

        private class LinkedNodesDescendingIterator implements Iterator<Node<E>> {

            private final LinkedNodesListIterator listIterator = linkedNodesListIterator(-1); // start at end of list

            @Override
            public boolean hasNext() {
                return listIterator.hasPrevious();
            }

            @Override
            public Node<E> next() {
                return listIterator.previous();
            }

            @Override
            public void remove() {
                listIterator.remove();
            }

        } // LinkedNodesDescendingIterator
        
        private LinkedNodesSpliterator linkedNodesSpliterator() {
            return new LinkedNodesSpliterator();
        }

        private class LinkedNodesSpliterator implements Spliterator<Node<E>> {

            private static final int BATCH_INCREMENT = 1 << 10;
            private static final int MAX_BATCH_SIZE  = 1 << 25;

            private Node<E> cursor = headSentinel;
            private long remainingSize = -1L;
            private int batchSize = 0;
            private int expectedModCount;

            private LinkedNodesSpliterator() {
            }

            private void bind() {
                this.remainingSize = size;
                this.expectedModCount = modCount;
            }
            
            private int batchSize() {
                return batchSize;
            }

            @Override
            public int characteristics() {
                return Spliterator.NONNULL | Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED;
            }

            @Override
            public long estimateSize() {
                if (remainingSize < 0L) bind();
                return remainingSize;
            }

            @Override
            public boolean tryAdvance(Consumer<? super Node<E>> action) {
                if (remainingSize < 0L) bind();
                if (modCount != expectedModCount) throw new ConcurrentModificationException();
                if (action == null) throw new NullPointerException();
                if (cursor.next == tailSentinel || remainingSize < 1L) return false;
                remainingSize--;
                cursor = cursor.next;
                action.accept(cursor);
                return true;
            }

            @Override
            public void forEachRemaining(Consumer<? super Node<E>> action) {
                if (remainingSize < 0L) bind();
                if (modCount != expectedModCount) throw new ConcurrentModificationException();
                if (action == null) throw new NullPointerException();
                Node<E> node = cursor;
                while (node.next != tailSentinel && remainingSize-- > 0L) {
                    node = node.next;
                    action.accept(node);                    
                }
                cursor = node;
                if (modCount != expectedModCount) throw new ConcurrentModificationException();
            }

            @Override
            public Spliterator<Node<E>> trySplit() {
                final Object[] array = trySplit( (node) -> { return node; } );
                return (array == null) ? null : Spliterators.spliterator(array, 0, batchSize, Spliterator.ORDERED | Spliterator.NONNULL);
            }

            private Object[] trySplit(Function<Node<E>, Object> action) {
                if (remainingSize < 0L) bind();
                if (remainingSize <= 1L) return null;
                int arraySize = batchSize + BATCH_INCREMENT;
                if (arraySize > remainingSize) arraySize = (int)remainingSize;
                if (arraySize > MAX_BATCH_SIZE) arraySize = MAX_BATCH_SIZE;
                Object[] array = new Object[arraySize];
                int index = 0;
                Node<E> node = cursor;
                while (index < arraySize && node.next != tailSentinel) {
                    node = node.next;
                    array[index++] = action.apply(node);                    
                }               
                cursor = node;
                batchSize = index;
                remainingSize =- batchSize;
                return array;
            }            

        } // LinkedNodesSpliterator		

    } // LinkedNodes

    private class NodableLinkedListListIterator implements ListIterator<E> {

        private final LinkedNodes.LinkedNodesListIterator listIterator;

        private NodableLinkedListListIterator(int index) {
            listIterator = linkedNodes.linkedNodesListIterator(index);
        }

        private NodableLinkedListListIterator(Node<E> node) {
            listIterator = linkedNodes.linkedNodesListIterator(node);
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
            listIterator.forEachRemaining( (node) -> { action.accept(node.element); } );
        }		

    } // NodableLinkedListListIterator	

    private class NodableLinkedListDescendingIterator implements Iterator<E> {

        private final LinkedNodes.LinkedNodesListIterator listIterator = linkedNodes.linkedNodesListIterator(-1); // start at end of list

        @Override
        public boolean hasNext() {
            return listIterator.hasPrevious();
        }

        @Override
        public E next() {
            return listIterator.previous().element;
        }

        @Override
        public void remove() {
            listIterator.remove();
        }

    } // NodableLinkedListDescendingIterator

    private class NodableLinkedListSpliterator implements Spliterator<E> {

        private final LinkedNodes.LinkedNodesSpliterator spliterator = linkedNodes.linkedNodesSpliterator();

        @Override
        public int characteristics() {
            return Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED;
        }

        @Override
        public long estimateSize() {
            return spliterator.estimateSize();
        }

        @Override
        public boolean tryAdvance(Consumer<? super E> action) {
            return spliterator.tryAdvance( (node) -> { action.accept(node.element); } );
        }

        @Override
        public void forEachRemaining(Consumer<? super E> action) {
            spliterator.forEachRemaining( (node) -> { action.accept(node.element); } );
        }

        @Override
        public Spliterator<E> trySplit() {
            final Object[] array = spliterator.trySplit( (node) -> { return node.element; } );
            return (array == null) ? null : Spliterators.spliterator(array, 0, spliterator.batchSize(), Spliterator.ORDERED);
        }

    } // NodableLinkedListSpliterator
    
    /**
     * Sublist of a {@code NodableLinkedList}.
     * Implements all optional {@code List} operations, and permits all elements
     * (including {@code null}).
     * 
     * <p>Just like a {@code NodableLinkedList}, there are two ways to visualize a
     * {@code NodableLinkedList.SubList}: one as a list of elements (the standard view),
     * and the other as a list of nodes. The latter view is implemented by the inner
     * class {@code LinkedSubNodes}. Each instance of {@code SubList} has one and only one
     * instance of {@code LinkedSubNodes}.
     *
     * <p><strong>Implementation Note:</strong>
     * For performance reasons, this implementation might not verify that the specified
     * lastNode comes after the specified firstNode in the backing list.
     * Therefore, an {@code IllegalStateException} can be thrown for any operation on this
     * sublist indicating that the end of the list was reached unexpectedly. it is
     * also possible that it is never detected that the lastNode comes before the firstNode.
     *
     * <p>The iterators returned by this class's {@code iterator} and
     * {@code listIterator} methods are <i>fail-fast</i>: if the sublist is
     * structurally modified at any time after the iterator is created, in
     * any way except through the Iterator's own {@code remove} or
     * {@code add} methods, the iterator will throw a {@link
     * ConcurrentModificationException}.  Thus, in the face of concurrent
     * modification, the iterator fails quickly and cleanly, rather than
     * risking arbitrary, non-deterministic behavior at an undetermined
     * time in the future.
     *
     * <p>Note that the fail-fast behavior of an iterator cannot be guaranteed
     * as it is, generally speaking, impossible to make any hard guarantees in the
     * presence of unsynchronized concurrent modification.  Fail-fast iterators
     * throw {@code ConcurrentModificationException} on a best-effort basis.
     * Therefore, it would be wrong to write a program that depended on this
     * exception for its correctness:   <i>the fail-fast behavior of iterators
     * should be used only to detect bugs.</i> 
     * 
     * @author James Pfeifer
     *
     */
    public class SubList extends AbstractSequentialList<E> implements List<E> {
        
        //protected int modCount inherited from class java.util.AbstractList (see updateModCount)
        
        private final LinkedSubNodes linkedSubNodes;
        
        private SubList(Node<E> headSentinel, Node<E> tailSentinel, SubList parent, long size) {
            linkedSubNodes = new LinkedSubNodes(headSentinel, tailSentinel, parent, size);
        }
        
        private void checkForConcurrentModificationException() {
            if (this.modCount != NodableLinkedList.this.modCount) throw new ConcurrentModificationException();
        }
        
        /**
         * Returns the {@code NodableLinkedList} this {@code SubList} is backing.
         *         
         * @return the {@code NodableLinkedList} this {@code SubList} is backing.
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
            return linkedSubNodes;
        }

        /**
         * Returns the number of elements in this sublist.  If this sublist contains more than
         * Integer.MAX_VALUE elements, returns Integer.MAX_VALUE.
         *
         * @return the number of elements in this sublist.
         */
        @Override
        public int size() {
            return linkedSubNodes.size();
        }

        /**
         * Returns the number of elements in this sublist.
         *
         * @return the number of elements in this sublist.
         */
        public long longSize() {
            return linkedSubNodes.longSize();
        }
        
        /**
         * Returns {@code true} if this sublist contains no elements.
         *
         * @return {@code true} if this sublist contains no elements
         */
        @Override
        public boolean isEmpty() {
            return linkedSubNodes.isEmpty();
        }
        
        /**
         * Removes all of the elements from this sublist.
         */
        @Override
        public void clear() {
            linkedSubNodes.clear();
        }
        
        /**
         * Returns the element at the specified position in this sublist.
         *
         * @param index index of the element to return
         * @return the element at the specified position in this sublist
         * @throws IndexOutOfBoundsException if the index is out of range {@code (index < 0 || index >= longSize())}
         */
        @Override
        public E get(int index) {
            return linkedSubNodes.get(index).element;
        }
        
        /**
         * Inserts the specified element at the specified position in this sublist.
         * Shifts the element currently at that position (if any) and any
         * subsequent elements to the right (adds one to their indices).
         *
         * @param index index at which the specified element is to be inserted
         * @param element element to be inserted
         * @throws IndexOutOfBoundsException if the index is out of range {@code (index < 0 || index > longSize())}
         */
        @Override
        public void add(int index, E element) {
            linkedSubNodes.add(index, node(element));
        }
        
        /**
         * Removes and returns the element at the specified position in this sublist.
         * Shifts any subsequent elements to the left (subtracts one from their indices).
         * Returns the element that was removed from the list.
         *
         * @param index the index of the element to be removed
         * @return the element previously at the specified position
         * @throws IndexOutOfBoundsException if the index is out of range {@code (index < 0 || index >= longSize())}
         */
        @Override
        public E remove(int index) {
            return linkedSubNodes.remove(index).element;
        }
        
        /**
         * Replaces the element at the specified position in this sublist with the
         * specified element.
         *
         * @param index index of the element to replace
         * @param element element to be stored at the specified position
         * @return the element previously at the specified position
         * @throws IndexOutOfBoundsException if the index is out of range {@code (index < 0 || index >= longSize())}
         */
        @Override
        public E set(int index, E element) {
            final Node<E> node = linkedNodes.get(index);
            final E originalElement = node.element;
            node.set(element);
            return originalElement;
        }
        
        /**
         * Appends all of the elements in the specified collection to the end of
         * this sublist, in the order that they are returned by the specified
         * collection's iterator.  The behavior of this operation is undefined if
         * the specified collection is modified while the operation is in
         * progress.  (Note that this will occur if the specified collection is
         * this sublist, and it's nonempty.)
         *
         * @param elements collection containing elements to be added to this sublist
         * @return {@code true} if this list changed as a result of the call
         * @throws NullPointerException if the specified collection is null
         */
        @Override
        public boolean addAll(Collection<? extends E> elements) {
            checkForConcurrentModificationException();
            boolean changed = false;
            final Node<E> targetNode = linkedSubNodes.getNode(longSize());
            for (E element: elements) {
                linkedSubNodes.addNodeBefore(node(element), targetNode);
                changed = true;
            }
            return changed;
        }
        
        /**
         * Inserts all of the elements in the specified collection into this
         * sublist, starting at the specified position.  Shifts the element
         * currently at that position (if any) and any subsequent elements to
         * the right (increases their indices).  The new elements will appear
         * in the sublist in the order that they are returned by the
         * specified collection's iterator.
         *
         * @param index index at which to insert the first element
         *              from the specified collection
         * @param elements collection containing elements to be added to this sublist
         * @return {@code true} if this list changed as a result of the call
         * @throws IndexOutOfBoundsException if the index is out of range {@code (index < 0 || index > longSize())}
         * @throws NullPointerException if the specified collection is null
         */
        @Override
        public boolean addAll(int index, Collection<? extends E> elements) {
            checkForConcurrentModificationException();
            if (linkedSubNodes.sizeIsKnown() && (index < 0 || index > longSize())) throw new IndexOutOfBoundsException("index=" + index + ", size=" + longSize());
            boolean changed = false;            
            final Node<E> targetNode = linkedSubNodes.getNode(index);
            for (E element: elements) {
                linkedSubNodes.addNodeBefore(node(element), targetNode);
                changed = true;
            }
            return changed;
        }
        
        /**
         * Inserts all of the elements in the specified collection into this
         * sublist, before the specified node. Shifts the element
         * currently at that position and any subsequent elements to
         * the right (increases their indices).  The new elements will appear
         * in the sublist in the order that they are returned by the
         * specified collection's iterator. if the specified node is null,
         * the elements will be appended to the end of this sublist.
         * 
         * Note that {@code addAll(null, Collection)} is identical in function to
         * {@code addAll(Collection)}.
         *
         * @param targetNode node the specified collection is to be inserted before
         * @param elements collection containing elements to be added to this list
         * @return {@code true} if this list changed as a result of the call
         * @throws IllegalArgumentException if the specified node is not linked to this list
         * @throws NullPointerException if the specified collection is null
         */
        public boolean addAll(Node<E> targetNode, Collection<? extends E> elements) {
            checkForConcurrentModificationException();
            if (targetNode == null) return addAll(elements);
            if (!linkedSubNodes.contains(targetNode)) throw new IllegalArgumentException("specified node is not part of this sublist");
            boolean changed = false;
            for (E element: elements) {
                linkedSubNodes.addNodeBefore(node(element), targetNode);
                changed = true;
            }
            return changed;
        }
        
        /**
         * Returns a view of the portion of this sublist between the specified
         * {@code fromIndex}, inclusive, and {@code toIndex}, exclusive.  (If
         * {@code fromIndex} and {@code toIndex} are equal, the returned sublist is
         * empty.)  The returned sublist is backed by this sublist, so changes in the
         * returned sublist (structural or non-structural) are reflected in this
         * sublist. The returned sublist supports all of the optional list operations
         * supported by this sublist.
         *
         * <p>This method eliminates the need for explicit range operations (of
         * the sort that commonly exist for arrays).  Any operation that expects
         * a list can be used as a range operation by passing a subList view
         * instead of a whole list.  For example, the following idiom
         * removes a range of elements from a list:
         * <pre>{@code
         *      list.subList(from, to).clear();
         * }</pre>
         * Similar idioms may be constructed for {@code indexOf} and
         * {@code lastIndexOf}, and all of the algorithms in the
         * {@code Collections} class can be applied to a subList.
         *
         * <p>The semantics of the sublist returned by this method become undefined if
         * the backing sublist (i.e., this sublist) is <i>structurally modified</i> in
         * any way other than via the returned sublist.  (Structural modifications are
         * those that change the size of this sublist, or otherwise perturb it in such
         * a fashion that iterations in progress may yield incorrect results.)
         * A {@code ConcurrentModificationException} is thrown for any operation on
         * the returned sublist if this sublist is structurally modified.
         *
         * @param fromIndex low endpoint (inclusive) of the subList
         * @param toIndex high endpoint (exclusive) of the subList
         * @return a view of the specified range within this sublist
         * @throws IndexOutOfBoundsException for an illegal endpoint index value
         *         ({@code fromIndex < 0 || toIndex > size ||
         *         fromIndex > toIndex})
         */
        @Override
        public List<E> subList(int fromIndex, int toIndex) {
            return linkedSubNodes.newSubList(fromIndex, toIndex);
        }
        
        /**
         * Returns a view of the portion of this sublist between the specified
         * {@code fisrtNode}, and {@code lastNode} (both inclusive).
         * The returned sublist is backed by this sublist, so changes in the returned
         * sublist (structural or non-structural) are reflected in this sublist.
         * The returned sublist supports all of the optional list operations
         * supported by this sublist.
         * 
         * <p> If the specified {@code firstNode} is {@code null}, the first node of the
         * this sublist will be used as the first node of the returned sublist. If the specified
         * {@code lastNode} is {@code null}, the last node of this sublist will be used as the
         * last node of the returned sublist. Note, the only way to produce an empty sublist is if
         * this sublist is empty and {@code null} is specified for both the {@code firstNode}
         * and the {@code lastNode}.
         *
         * <p>This method eliminates the need for explicit range operations (of
         * the sort that commonly exist for arrays).  Any operation that expects
         * a list can be used as a range operation by passing a subList view
         * instead of a whole list.  For example, the following idiom
         * removes a range of elements from a list:
         * <pre>{@code
         *      list.subList(first, last).clear();
         * }</pre>
         * Similar idioms may be constructed for {@code indexOf} and
         * {@code lastIndexOf}, and all of the algorithms in the
         * {@code Collections} class can be applied to a subList.
         *
         * <p>The semantics of the sublist returned by this method become undefined if
         * the backing sublist (i.e., this sublist) is <i>structurally modified</i> in
         * any way other than via the returned sublist.  (Structural modifications are
         * those that change the size of this sublist, or otherwise perturb it in such
         * a fashion that iterations in progress may yield incorrect results.)
         * A {@code ConcurrentModificationException} is thrown for any operation on
         * the returned sublist if this sublist is structurally modified.
         *
         * @param firstNode low endpoint (inclusive) of the subList
         * @param lastNode high endpoint (inclusive) of the subList
         * @return a view of the specified range within this sublist
         * @throws IllegalArgumentException if any specified node is not linked to this sublist or
         *                                  if the lastNode comes before the firstNode in this sublist.
         */
        public SubList subList(Node<E> firstNode, Node<E> lastNode) {
            checkForConcurrentModificationException();
            return linkedSubNodes.newSubList(firstNode, lastNode);
        }
        
        /**
         * Returns a list-iterator of the elements in this sublist (in proper
         * sequence), starting at the specified position in the sublist.
         * Obeys the general contract of {@code List.listIterator(int)}.
         * 
         * <p><strong>Implementation Note:</strong>
         * The ListIterator returned by this method behaves differently when the
         * sublist's {@code size > Integer.MAX_VALUE}. Methods {@code nextIndex} and
         * {@code previousIndex} return -1 if the {@code index > Integer_MAX_VALUE}.
         *
         * <p>The list-iterator is <i>fail-fast</i>: if the sublist is structurally
         * modified at any time after the Iterator is created, in any way except
         * through the list-iterator's own {@code remove} or {@code add}
         * methods, the list-iterator will throw a
         * {@code ConcurrentModificationException}.  Thus, in the face of
         * concurrent modification, the iterator fails quickly and cleanly, rather
         * than risking arbitrary, non-deterministic behavior at an undetermined
         * time in the future.
         *
         * @param index index of the first element to be returned from the
         *              list-iterator (by a call to {@code next})
         * @return a ListIterator of the elements in this list (in proper
         *         sequence), starting at the specified position in the list
         * @throws IndexOutOfBoundsException if the index is out of range {@code (index < 0 || index > longSize())}
         * @see List#listIterator(int)
         */
        @Override
        public ListIterator<E> listIterator(int index) {
            if (index < 0) throw new IndexOutOfBoundsException("index=" + index);
            return new SubListIterator(index);
        }
        
        /**
         * Returns a list-iterator of the elements in this sublist (in proper
         * sequence), starting at the specified node in the sublist.
         * if the specified node is {@code null}, the list-iterator will
         * start with the first node in this sublist.    
         * 
         * <p><strong>Implementation Note:</strong>
         * The index returned by {@code nextIndex} and {@code previousIndex}
         * is relative to the specified node which has an index of 0.
         * Nodes which come before the specified node in this sublist, will
         * have a negative index; nodes that come after will have a positive index. 
         * Method {@code nextIndex} returns {@code longSize()} if at the end of the sublist, and
         * method {@code previousIndex} returns {@code -longSize()} if at the beginning
         * of the sublist. if {@code index < Integer.MAX_VALUE or index > Integer.MAX_VALUE},
         * {@code Integer.MIN_VALUE} or {@code Integer.MAX_VALUE} is returned respectively.
         *
         * <p>The list-iterator is <i>fail-fast</i>: if the list is structurally
         * modified at any time after the Iterator is created, in any way except
         * through the list-iterator's own {@code remove} or {@code add}
         * methods, the list-iterator will throw a
         * {@code ConcurrentModificationException}.  Thus, in the face of
         * concurrent modification, the iterator fails quickly and cleanly, rather
         * than risking arbitrary, non-deterministic behavior at an undetermined
         * time in the future.
         *
         * @param node node of the first element to be returned from the list-iterator
         *             (by a call to {@code next})
         * @return a ListIterator of the elements in this list (in proper
         *         sequence), starting at the specified node in the list
         * @throws IllegalArgumentException if the specified node is not linked to this list
         */
        public ListIterator<E> listIterator(Node<E> node) {
            if (node != null && !linkedNodes.contains(node)) throw new IllegalArgumentException("Specified node is not linked to this list");
            return new SubListIterator(node);
        }
        
        /**
         * Sublist of {@code NodableLinkedList.LinkedNodes}.
         * Implements all optional {@code List} operations.
         * The elements are of type {@code NodableLinkedList.Node},
         * and are never {@code null}.
         * 
         * Every instance of a {@code NodableLinkedList.SubList} has one and only one
         * {@code LinkedSubNodes} object backing it.
         * Method {@link NodableLinkedList.SubList#linkedSubNodes()} returns the
         * {@code LinkedSubNodes} object backing a {@code NodableLinkedList.SubList} instance.
         *
         * <p>The iterators returned by this class's {@code iterator} and
         * {@code listIterator} methods are <i>fail-fast</i>: if the sublist is
         * structurally modified at any time after the iterator is created, in
         * any way except through the Iterator's own {@code remove} or
         * {@code add} methods, the iterator will throw a {@link
         * ConcurrentModificationException}.  Thus, in the face of concurrent
         * modification, the iterator fails quickly and cleanly, rather than
         * risking arbitrary, non-deterministic behavior at an undetermined
         * time in the future.
         *
         * <p>Note that the fail-fast behavior of an iterator cannot be guaranteed
         * as it is, generally speaking, impossible to make any hard guarantees in the
         * presence of unsynchronized concurrent modification.  Fail-fast iterators
         * throw {@code ConcurrentModificationException} on a best-effort basis.
         * Therefore, it would be wrong to write a program that depended on this
         * exception for its correctness:   <i>the fail-fast behavior of iterators
         * should be used only to detect bugs.</i>
         * 
         * @author James Pfeifer
         *
         */
        public class LinkedSubNodes extends AbstractSequentialList<Node<E>> implements List<Node<E>> {
            
            private Node<E> headSentinel;
            private Node<E> tailSentinel;
            private final LinkedSubNodes parent;
            private long size;
            
            private LinkedSubNodes(Node<E> headSentinel, Node<E> tailSentinel, SubList parent, long size) {
                // assert headSentinel != null : "headSentinel is null";
                // assert tailSentinel != null : "tailSentinel is null";
                // assert headSentinel.linkedNodes == linkedNodes : "headSentinel not linked to this linkedList";
                // assert tailSentinel.linkedNodes == linkedNodes : "tailSentinel not linked to this linkedList";
                // the tailSentinel(null) and size(-1) may be unknown, but never both at the same time
                // no guarantee that headSentinel comes before tailSentinel in the parent list
                this.headSentinel = headSentinel;
                this.tailSentinel = tailSentinel; 
                this.parent = (parent == null) ? null : parent.linkedSubNodes();
                this.size = size;
                updateModCount();
            }
            
            /**
             * Returns the {@code SubList} this {@code LinkedSubNodes} is backing.
             *         
             * @return the {@code SubList} this {@code LinkedSubNodes} is backing.
             */
            public SubList subList() {
                return SubList.this;
            }
            
            //protected int modCount inherited from class java.util.AbstractList        
            private void updateModCount() {
                SubList.this.modCount = (this.modCount = NodableLinkedList.this.modCount); // keep all modCounts in sync
            }

            private void updateSizeAndModCount(long sizeChange) {
                LinkedSubNodes linkedSubNodes = this;
                do {
                    if (linkedSubNodes.sizeIsKnown()) linkedSubNodes.size += sizeChange;
                    linkedSubNodes.updateModCount();
                linkedSubNodes = linkedSubNodes.parent;
                } while (linkedSubNodes != null);
            }
            
            private void checkForConcurrentModificationException() {
                if (this.modCount != NodableLinkedList.this.modCount) throw new ConcurrentModificationException();
            }
            
            private boolean tailSentinelIsKnown() {
                return this.tailSentinel != null;
            }
            
            private Node<E> tailSentinel() {
                if (!tailSentinelIsKnown()) { // tailSentinel unknown?
                    // assert size >= 0 : "sublist size is unknown";
                    long remaining = size;
                    tailSentinel = headSentinel.next;
                    while (remaining > 0 && tailSentinel != linkedNodes.tailSentinel) {
                        remaining--;
                        tailSentinel = tailSentinel.next;
                    }
                    if (remaining > 0) throw new IllegalStateException("End of list reached unexpectedly");
                }
                return tailSentinel;
            }

            private void addNodeAfter(Node<E> node, Node<E> afterThisNode) {
                // assert node != null && !node.isLinked() : "Node is null or already an element of a list";
                // assert this.contains(afterThisNode) : "Before Node is not an element of this sublist";
                linkedNodes.addNodeAfter(node, afterThisNode);
                updateSizeAndModCount(1L);
            }

            private void addNodeBefore(Node<E> node, Node<E> beforeThisNode) {
                // assert node != null && !node.isLinked() : "Node is null or already an element of a list";
                // assert this.contains(beforeThisNode) : "Before Node is not an element of this sublist";
                linkedNodes.addNodeBefore(node, beforeThisNode);
                updateSizeAndModCount(1L);
            }
            
            private void removeNode(Node<E> node) {
                // assert this.contains(node) : "Node is not an element of this sublist";
                linkedNodes.removeNode(node);
                updateSizeAndModCount(-1L);                
            }
            
            private void replaceNode(Node<E> node, Node<E> replacementNode) {
                // assert this.contains(node) : "Node is not an element of this sublist";
                // assert replacementNode != null && !replacementNode.isLinked() : "Replacement Node is null or is already an element of a list";
                linkedNodes.replaceNode(node, replacementNode);
                updateSizeAndModCount(0L);
            }

            private boolean hasNodeAfter(Node<E> node) {
                // assert this.contains(node) : "Node is not an element of this sublist";
                if (node.next == tailSentinel()) return false;
                if (node.next == linkedNodes.tailSentinel)  throw new IllegalStateException("End of list reached unexpectedly; the specified last node most likely comes before the first node in the list");
                return true;
            }

            private boolean hasNodeBefore(Node<E> node) {
                // assert this.contains(node) : "Node is not an element of this sublist";
                return (node.previous == headSentinel) ? false : true; 
            }
            
            private Node<E> getNodeAfter(Node<E> node) {
                // assert this.contains(node) : "Node is not an element of this sublist";
                return (hasNodeAfter(node)) ? node.next : null;
            }
            
            private Node<E> getNodeBefore(Node<E> node) {
                // assert this.contains(node) : "Node is not an element of this sublist";
                return (hasNodeBefore(node)) ? node.previous : null;
            }
            
            private void swappedNodes(Node<E> myNode, Node<E> swappedNode) {
                LinkedSubNodes linkedSubNodes = this;
                do {
                    if (swappedNode == linkedSubNodes.headSentinel) linkedSubNodes.headSentinel = myNode;
                    if (swappedNode == linkedSubNodes.tailSentinel) linkedSubNodes.tailSentinel = myNode;
                  linkedSubNodes = linkedSubNodes.parent;
                } while (linkedSubNodes != null);
            }
            
            /**
             * Returns the node at the specified position in this sublist.
             * 
             * Note, this routine returns the tailSentinel
             * if {@code index == longSize()} (the size of the list) or if the list is empty 
             *
             * @param index index of the node to return
             * @return the node at the specified position in this sublist
             * @throws IndexOutOfBoundsException if the index is out of range {@code (index < 0 || index >= longSize())}
             * @throws IllegalStateException if end of list is reached unexpectedly; the specified last node most likely comes before the first node in the list
             */
            private Node<E> getNode(long index) {
                // assert index >= 0L && index <= longSize();
                // Note, this routine returns the tailSentinel if index = longSize() (the size of the list) or if the list is empty
                if (index < 0L) throw new IndexOutOfBoundsException("index=" + index);
                Node<E> node;
                long cursorIndex;
                if (sizeIsKnown() && tailSentinelIsKnown()) {
                    // both size and tailSentinel is known, therefore, we also know that the tailSentinel comes after the headSentinel in the list
                    if (index > longSize()) throw new IndexOutOfBoundsException("index=" + index + " > size=" + longSize());
                    if (index <= ((longSize()) >> 1)) {
                        cursorIndex = 0L;
                        node = headSentinel.next;
                        while (cursorIndex < index) { node = node.next; cursorIndex++; }
                    } else {
                        cursorIndex = longSize();
                        node = tailSentinel();
                        while (cursorIndex > index) { node = node.previous; cursorIndex--; }
                    }
                } else if (sizeIsKnown()) {
                    // size is known, tailSentinel is unknown
                    if (index > longSize()) throw new IndexOutOfBoundsException("index=" + index + " > size=" + longSize());
                    cursorIndex = 0L;
                    node = headSentinel.next;
                    while (cursorIndex < index) { node = node.next; cursorIndex++; }
                    if (index == longSize()) this.tailSentinel = node;
                    if (index == longSize()-1) this.tailSentinel = node.next;
                } else {
                    // size is unknown, tailSentinel is known
                    cursorIndex = 0L;
                    node = headSentinel.next;
                    while (cursorIndex < index && node != tailSentinel && node != linkedNodes.tailSentinel) { node = node.next; cursorIndex++; }
                    if (cursorIndex < index) {
                        if (node == tailSentinel) {
                            throw new IndexOutOfBoundsException("index=" + index + " > size=" + cursorIndex);
                        } else {
                            throw new IllegalStateException("End of list reached unexpectedly; the specified last node most likely comes before the first node in the list");
                        }
                    }
                    if (node == tailSentinel) this.size = cursorIndex;
                    if (node.next == tailSentinel) this.size = cursorIndex + 1;
                }
                return node;
            }
            
            /**
             * Returns the index of the specified node in this sublist,
             * or -1 if this sublist does not contain the node.
             * 
             * @param node node to search for
             * @return the index of the specified node in this sublist,
             *         or -1 if this sublist does not contain the node.             
             */
            private long getIndex(Node<E> node) {
                // assert node != null : "Node is null";
                if (!linkedNodes.contains(node)) return -1;
                long cursorIndex = 0L;
                Node<E> cursorNode = this.headSentinel.next;
                if (sizeIsKnown() && tailSentinelIsKnown()) {
                    // both size and tailSentinel is known, therefore, we also know that the tailSentinel comes after the headSentinel in the list
                    while (cursorNode != node && cursorNode != this.tailSentinel) { cursorIndex++; cursorNode = cursorNode.next; }
                    if (cursorNode == this.tailSentinel) cursorIndex = -1L; // node not found
                } else if (sizeIsKnown()) {
                    // size is known, tailSentinel is unknown
                    while (cursorNode != node && cursorIndex < longSize()) { cursorIndex++; cursorNode = cursorNode.next; }
                    if (cursorIndex == longSize()-1) this.tailSentinel = cursorNode.next;
                    if (cursorIndex == longSize()) {
                        this.tailSentinel = cursorNode;
                        cursorIndex = -1L; // node not found
                    }
                } else {
                    // size is unknown, tailSentinel is known
                    while (cursorNode != node && cursorNode != this.tailSentinel && cursorNode != linkedNodes.tailSentinel) {
                        cursorIndex++;
                        cursorNode = cursorNode.next;
                    }
                    if (cursorNode != this.tailSentinel) throw new IllegalStateException("End of list reached unexpectedly; the specified last node most likely comes before the first node in the list");
                    if (cursorNode.next == this.tailSentinel) this.size = cursorIndex + 1;
                    if (cursorNode == this.tailSentinel) {
                        this.size = cursorIndex;
                        cursorIndex = -1L; // node not found
                    }
                }
                return cursorIndex;
            }
            
            private boolean sizeIsKnown() {
                return size >= 0L;
            }

            /**
             * Returns the number of nodes in this sublist.  If this sublist contains more than
             * Integer.MAX_VALUE nodes, returns Integer.MAX_VALUE.
             *
             * @return the number of nodes in this sublist.
             */
            @Override
            public int size() {
                return (longSize() > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int)longSize();
            }

            /**
             * Returns the number of nodes in this sublist.
             *
             * @return the number of nodes in this sublist.
             */
            public long longSize() {
                checkForConcurrentModificationException();
                if (!sizeIsKnown()) { // size unknown?
                    // assert tailSentinel != null : "tail sentinel shouldn't be null";
                    Node<E> node;
                    size = 0L;
                    for (node = headSentinel.next; node != tailSentinel && node != linkedNodes.tailSentinel; node = node.next) size++;
                    if (node != tailSentinel) throw new IllegalStateException("End of list reached unexpectedly; the specified last node most likely comes before the first node in the list");
                }
                return size;
            }
            
            /**
             * Returns {@code true} if this sublist contains no nodes.
             *
             * @return {@code true} if this sublist contains no nodes
             */
            @Override
            public boolean isEmpty() {
                checkForConcurrentModificationException();
                if (sizeIsKnown()) return size == 0L;
                return headSentinel.next == tailSentinel;
            }
            
            /**
             * Removes all of the nodes from this sublist.
             */
            @Override
            public void clear() {
                checkForConcurrentModificationException();
                Node<E> node = headSentinel.next;
                if (sizeIsKnown()) {
                    long remaining = size;
                    while (remaining > 0) {
                        final Node<E> nodeToRemove = node;
                        node = node.next;
                        removeNode(nodeToRemove);
                        remaining--;
                    }
                } else {
                    while (node != tailSentinel && node != linkedNodes.tailSentinel) {
                        final Node<E> nodeToRemove = node;
                        node = node.next;
                        removeNode(nodeToRemove);
                    }
                    if (node != tailSentinel) throw new IllegalStateException("End of list reached unexpectedly; the specified last node most likely comes before the first node in the list");
                }
            }

            /**
             * Returns the index of the specified node in this sublist,
             * or -1 if there is no such index (this sublist does not
             * contain the node or the {@code index > Integer.MAX_VALUE}).
             * 
             * @param o node to search for
             * @return the index of the specified node in this sublist,
             *         or -1 if there is no such index.
             */
            @Override
            public int indexOf(Object o) {
                checkForConcurrentModificationException();
                if (!Node.class.isInstance(o)) return -1;
                @SuppressWarnings("unchecked")
                final Node<E> node = (Node<E>)o;
                final long index = getIndex(node);
                return (index > Integer.MAX_VALUE) ? -1 : (int)index;
            }
            
            /**
             * Returns the index of the specified node in this sublist,
             * or -1 if there is no such index (this sublist does not
             * contain the node or the {@code index > Integer.MAX_VALUE}.
             * 
             * <p>Note that {@code lastIndexOf} is identical in function to
             * {@code indexOf}.
             *
             * @param o node to search for
             * @return the index of the specified node in this sublist,
             *         or -1 if there is no such index.
             */
            @Override
            public int lastIndexOf(Object o) {
                return indexOf(o);
            }
            
            /**
             * Returns the node at the specified position in this sublist.
             *
             * @param index index of the element to return
             * @return the node at the specified position in this sublist
             * @throws IndexOutOfBoundsException if the index is out of range {@code (index < 0 || index >= longSize())}
             */
            @Override
            public Node<E> get(int index) {
                checkForConcurrentModificationException();
                final long getIndex = index;
                if (sizeIsKnown() && (getIndex < 0L || getIndex >= longSize())) throw new IndexOutOfBoundsException("index=" + index + ", size=" + longSize());        
                Node<E> node = getNode(index);
                if (node == tailSentinel) throw new IndexOutOfBoundsException("index=" + index + " = size=" + longSize());
                return node;        
            }
            
            /**
             * Inserts the specified node at the specified position in this sublist.
             * Shifts the node currently at that position (if any) and any
             * subsequent nodes to the right (adds one to their indices).
             *
             * @param index index at which the specified node is to be inserted
             * @param node node to be inserted
             * @throws IndexOutOfBoundsException if the index is out of range {@code (index < 0 || index > longSize())}
             * @throws IllegalArgumentException if node is null or already an element of a list
             */
            @Override
            public void add(int index, Node<E> node) {
                checkForConcurrentModificationException();
                if (sizeIsKnown() && (index < 0L || index > longSize())) throw new IndexOutOfBoundsException("index=" + index + ", size=" + longSize());
                if (node == null || node.isLinked()) throw new IllegalArgumentException("Node is null or already an element of a list");
                addNodeBefore(node, getNode(index));
            }
            
            /**
             * Removes the node at the specified position in this sublist.  Shifts any
             * subsequent nodes to the left (subtracts one from their indices).
             * Returns the node that was removed from the list.
             *
             * @param index the index of the node to be removed
             * @return the node previously at the specified position
             * @throws IndexOutOfBoundsException if the index is out of range {@code (index < 0 || index >= longSize())}
             */
            @Override
            public Node<E> remove(int index) {
                checkForConcurrentModificationException();
                if (sizeIsKnown() && (index < 0L || index >= longSize())) throw new IndexOutOfBoundsException("index=" + index + ", size=" + longSize());
                final Node<E> node = get(index);
                removeNode(node);
                return node;
            }
            
            /**
             * Replaces the node at the specified position in this sublist with the
             * specified node.
             *
             * @param index index of the node to replace
             * @param node node to be stored at the specified position
             * @return the node previously at the specified position
             * @throws IndexOutOfBoundsException if the index is out of range {@code (index < 0 || index >= longSize())}
             * @throws IllegalArgumentException if replacement node is null or already an element of a list
             */
            @Override
            public Node<E> set(int index, Node<E> node) {
                checkForConcurrentModificationException();
                if (sizeIsKnown() && (index < 0L || index >= longSize())) throw new IndexOutOfBoundsException("index=" + index + ", size=" + longSize());
                if (node == null || node.isLinked()) throw new IllegalArgumentException("Replacement Node is null or already an element of a list");           
                final Node<E> originalNode = get(index);
                replaceNode(originalNode, node);
                return originalNode;
            }
            
            /**
             * Appends all of the nodes in the specified collection to the end of
             * this sublist, in the order that they are returned by the specified
             * collection's iterator.  The behavior of this operation is undefined if
             * the specified collection is modified while the operation is in
             * progress.  (Note that this will occur if the specified collection is
             * this list, and it's nonempty.)
             *
             * @param nodes collection containing nodes to be added to this sublist
             * @return {@code true} if this sublist changed as a result of the call
             * @throws NullPointerException if the specified collection is null
             * @throws IllegalArgumentException if any node in the collection is null
             *                                  or already an element of a list
             */
            @Override
            public boolean addAll(Collection<? extends Node<E>> nodes) {
                checkForConcurrentModificationException();
                final long initialSize = longSize();
                final Node<E> targetNode = getNode(longSize()); // get sublist's tailSentinel
                for (Node<E> node: nodes) addNodeBefore(node, targetNode);
                return longSize() != initialSize;
            }
            
            /**
             * Inserts all of the nodes in the specified collection into this
             * sublist, starting at the specified position.  Shifts the node
             * currently at that position (if any) and any subsequent nodes to
             * the right (increases their indices).  The new nodes will appear
             * in the sublist in the order that they are returned by the
             * specified collection's iterator.
             *
             * @param index index at which to insert the first node
             *              from the specified collection
             * @param nodes collection containing nodes to be added to this sublist
             * @return {@code true} if this sublist changed as a result of the call
             * @throws IllegalArgumentException if any node in the collection is null
             *                                  or already an element of a list
             * @throws IndexOutOfBoundsException if the index is out of range {@code (index < 0 || index > longSize())}
             * @throws NullPointerException if the specified collection is null
             */
            @Override
            public boolean addAll(int index, Collection<? extends Node<E>> nodes) {
                checkForConcurrentModificationException();
                if (sizeIsKnown() && (index < 0 || index > size)) throw new IndexOutOfBoundsException("index=" + index + ", size=" + size);
                boolean changed = false;            
                final Node<E> targetNode = getNode(index);
                for (Node<E> node: nodes) {
                    if (node == null || node.isLinked()) throw new IllegalArgumentException("Node in collection is null or already an element of a list");
                    addNodeBefore(node, targetNode);
                    changed = true;
                }
                return changed;
            }            
            
            /**
             * Inserts all of the nodes in the specified collection into this
             * sublist, before the specified node.  Shifts the node
             * currently at that position and any subsequent nodes to
             * the right (increases their indices).  The new nodes will appear
             * in the sublist in the order that they are returned by the
             * specified collection's iterator. if the specified node is null,
             * the nodes will be appended to the end of this sublist.
             * 
             * Note that {@code addAll(null, Collection)} is identical in function to
             * {@code addAll(Collection)}.
             *
             * @param targetNode node the specified collection is to be inserted before
             * @param nodes collection containing nodes to be added to this sublist
             * @return {@code true} if this sublist changed as a result of the call
             * @throws IllegalArgumentException if the specified node is not linked to this sublist, or
             *                                  any node in the collection is null or already an element of a list
             * @throws NullPointerException if the specified collection is null
             */
            public boolean addAll(Node<E> targetNode, Collection<? extends Node<E>> nodes) {
                checkForConcurrentModificationException();
                if (targetNode == null) return addAll(nodes);
                if (!this.contains(targetNode)) throw new IllegalArgumentException("specified node is not part of this sublist");
                boolean changed = false;
                for (Node<E> node: nodes) {
                    if (node == null || node.isLinked()) throw new IllegalArgumentException("Node in collection is null or already an element of a list");
                    addNodeBefore(node, targetNode);
                    changed = true;
                }
                return changed;
            }
            
            /**
             * Returns {@code true} if this sublist contains the specified node.
             *
             * @param o node whose presence in this sublist is to be tested
             * @return {@code true} if this sublist contains the specified node
             */
            @Override
            public boolean contains(Object o) {
                if (!Node.class.isInstance(o)) return false;
                @SuppressWarnings("unchecked")
                final Node<E> node = (Node<E>)o;
                return contains(node);
            }
            
            private boolean contains(Node<E> node) {
                return (getIndex(node) < 0L) ? false : true;
            }
            
            /**
             * Returns a view of the portion of this sublist between the specified
             * {@code fromIndex}, inclusive, and {@code toIndex}, exclusive.  (If
             * {@code fromIndex} and {@code toIndex} are equal, the returned sublist is
             * empty.)  The returned sublist is backed by this sublist, so changes in the
             * returned sublist (structural or non-structural) are reflected in this
             * sublist. The returned sublist supports all of the optional list operations
             * supported by this sublist.
             *
             * <p>This method eliminates the need for explicit range operations (of
             * the sort that commonly exist for arrays).  Any operation that expects
             * a list can be used as a range operation by passing a subList view
             * instead of a whole list.  For example, the following idiom
             * removes a range of elements from a list:
             * <pre>{@code
             *      list.subList(from, to).clear();
             * }</pre>
             * Similar idioms may be constructed for {@code indexOf} and
             * {@code lastIndexOf}, and all of the algorithms in the
             * {@code Collections} class can be applied to a subList.
             *
             * <p>The semantics of the sublist returned by this method become undefined if
             * the backing sublist (i.e., this sublist) is <i>structurally modified</i> in
             * any way other than via the returned sublist.  (Structural modifications are
             * those that change the size of this sublist, or otherwise perturb it in such
             * a fashion that iterations in progress may yield incorrect results.)
             * A {@code ConcurrentModificationException} is thrown for any operation on
             * the returned sublist if this sublist is structurally modified.
             *
             * @param fromIndex low endpoint (inclusive) of the subList
             * @param toIndex high endpoint (exclusive) of the subList
             * @return a view of the specified range within this sublist
             * @throws IndexOutOfBoundsException for an illegal endpoint index value
             *         ({@code fromIndex < 0 || toIndex > size ||
             *         fromIndex > toIndex})
             */ 
            @Override
            public List<Node<E>> subList(int fromIndex, int toIndex) {
                return newSubList(fromIndex, toIndex).linkedSubNodes;
            }
            
            private SubList newSubList(int fromIndex, int toIndex) {
                checkForConcurrentModificationException();
                if (fromIndex < 0L) throw new IndexOutOfBoundsException("fromIndex = " + fromIndex);
                if (toIndex > longSize()) throw new IndexOutOfBoundsException("toIndex(" + toIndex +") > size(" + longSize() + ")");
                if (fromIndex > toIndex) throw new IndexOutOfBoundsException("fromIndex(" + fromIndex +") > toIndex(" + toIndex + ")");
                final long size = toIndex - fromIndex;
                final Node<E> headSentinel = getNode(fromIndex).previous;
                final Node<E> tailSentinel = (size == 0L) ? headSentinel.next : null; // tailSentinel is unknown
                return new SubList(headSentinel, tailSentinel, SubList.this, size);
            }
            
            /**
             * Returns a view of the portion of this sublist between the specified
             * {@code fisrtNode}, and {@code lastNode} (both inclusive).
             * The returned sublist is backed by this sublist, so changes in the returned
             * sublist (structural or non-structural) are reflected in this sublist.
             * The returned sublist supports all of the optional list operations
             * supported by this sublist.
             * 
             * <p> If the specified {@code firstNode} is {@code null}, the first node of the
             * this sublist will be used as the first node of the returned sublist. If the specified
             * {@code lastNode} is {@code null}, the last node of this sublist will be used as the
             * last node of the returned sublist. Note, the only way to produce an empty sublist is if
             * this sublist is empty and {@code null} is specified for both the {@code firstNode}
             * and the {@code lastNode}.
             * 
             * <p>This method eliminates the need for explicit range operations (of
             * the sort that commonly exist for arrays).  Any operation that expects
             * a list can be used as a range operation by passing a subList view
             * instead of a whole list.  For example, the following idiom
             * removes a range of elements from a list:
             * <pre>{@code
             *      list.subList(first, last).clear();
             * }</pre>
             * Similar idioms may be constructed for {@code indexOf} and
             * {@code lastIndexOf}, and all of the algorithms in the
             * {@code Collections} class can be applied to a sublist.
             *
             * <p>The semantics of the sublist returned by this method become undefined if
             * the backing sublist (i.e., this sublist) is <i>structurally modified</i> in
             * any way other than via the returned sublist.  (Structural modifications are
             * those that change the size of this sublist, or otherwise perturb it in such
             * a fashion that iterations in progress may yield incorrect results.)
             * A {@code ConcurrentModificationException} is thrown for any operation on
             * the returned sublist if this list is structurally modified.
             *
             * @param firstNode low endpoint (inclusive) of the subList
             * @param lastNode high endpoint (inclusive) of the subList
             * @return a view of the specified range within this sublist
             * @throws IllegalArgumentException if any specified node is not linked to this sublist or
             *                                  if the lastNode comes before the firstNode in this sublist.
             */
            public SubList.LinkedSubNodes subList(Node<E> firstNode, Node<E> lastNode) {
                checkForConcurrentModificationException();
                return newSubList(firstNode, lastNode).linkedSubNodes;
            }
            
            private SubList newSubList(Node<E> firstNode, Node<E> lastNode) {
                checkForConcurrentModificationException();
                if (firstNode == null) firstNode = headSentinel.next;
                if (lastNode == null) lastNode = tailSentinel().previous;
                if (!linkedNodes.contains(firstNode)) throw new IllegalArgumentException("Specified first node is not linked to this list");
                if (linkedNodes.contains(lastNode)) throw new IllegalArgumentException("Specified last node is not linked to this list");
                if (lastNode.next == firstNode && firstNode != headSentinel.next) throw new IllegalArgumentException("Specified last Node comes before the specified first node in this list");
                long size = 0;
                boolean foundFirstNode = false;
                boolean foundLastNode = false;
                for (Node<E> node = headSentinel.next; node != tailSentinel(); node = node.next) {
                    if (node == firstNode) foundFirstNode = true;
                    if (node == lastNode) { foundLastNode = true; break; }
                    if (foundFirstNode) size++;
                }
                if (size > 0) {
                    if (foundLastNode && !foundFirstNode) throw new IllegalArgumentException(
                            "specified last node comes before the specified first node in this sublist, or the specified first node is not part of this sublist");
                    if (!foundFirstNode) throw new IllegalArgumentException("specified first node is not part of this sublist");
                    if (!foundLastNode) throw new IllegalArgumentException("specified last node is not part of this sublist");
                }
                return new SubList(firstNode.previous, lastNode.next, SubList.this, size);
            }
            
            /**
             * Returns a list-iterator of the nodes in this sublist (in proper
             * sequence), starting at the specified position in the sublist.
             * Obeys the general contract of {@code List.listIterator(int)}.
             * 
             * <p><strong>Implementation Note:</strong>
             * The ListIterator returned by this method behaves differently when the
             * sublist's {@code size > Integer.MAX_VALUE}. Methods {@code nextIndex} and
             * {@code previousIndex} return -1 if the {@code index > Integer_MAX_VALUE}.
             *
             * <p>The list-iterator is <i>fail-fast</i>: if the sublist is structurally
             * modified at any time after the Iterator is created, in any way except
             * through the list-iterator's own {@code remove} or {@code add}
             * methods, the list-iterator will throw a
             * {@code ConcurrentModificationException}.  Thus, in the face of
             * concurrent modification, the iterator fails quickly and cleanly, rather
             * than risking arbitrary, non-deterministic behavior at an undetermined
             * time in the future.
             *
             * @param index index of the first node to be returned from the
             *              list-iterator (by a call to {@code next})
             * @return a ListIterator of the nodes in this list (in proper
             *         sequence), starting at the specified position in the list
             * @throws IndexOutOfBoundsException if the index is out of range {@code (index < 0 || index > longSize())}
             * @see List#listIterator(int)
             */
            @Override
            public ListIterator<Node<E>> listIterator(int index) {
                if (index < 0) throw new IndexOutOfBoundsException("index=" + index);
                return linkedSubNodesListIterator(index);
            }
            
            /**
             * Returns a list-iterator of the nodes in this sublist (in proper
             * sequence), starting at the specified node in the sublist.
             * if the specified node is {@code null}, the list-iterator will
             * start with the first node in this sublist.
             * 
             * <p><strong>Implementation Note:</strong>
             * The index returned by {@code nextIndex} and {@code previousIndex}
             * is relative to the specified node which has an index of 0.
             * Nodes which come before the specified node in this sublist, will
             * have a negative index; nodes that come after will have a positive index. 
             * Method {@code nextIndex} returns {@code longSize()} if at the end of the sublist, and
             * method {@code previousIndex} returns {@code -longSize()} if at the beginning
             * of the sublist. if {@code index < Integer.MAX_VALUE or index > Integer.MAX_VALUE},
             * {@code Integer.MIN_VALUE} or {@code Integer.MAX_VALUE} is returned respectively.
             *
             * <p>The list-iterator is <i>fail-fast</i>: if the list is structurally
             * modified at any time after the Iterator is created, in any way except
             * through the list-iterator's own {@code remove} or {@code add}
             * methods, the list-iterator will throw a
             * {@code ConcurrentModificationException}.  Thus, in the face of
             * concurrent modification, the iterator fails quickly and cleanly, rather
             * than risking arbitrary, non-deterministic behavior at an undetermined
             * time in the future.
             *
             * @param node first node to be returned from the list-iterator (by a call to {@code next})
             * @return a ListIterator of the nodes in this list (in proper
             *         sequence), starting at the specified node in the list
             * @throws IllegalArgumentException if the specified is node not linked to this list
             */
            public ListIterator<Node<E>> listIterator(Node<E> node) {
                if (node != null && !linkedNodes.contains(node)) throw new IllegalArgumentException("Specified node is not linked to this list");
                return linkedSubNodesListIterator(node);
            }            
            
            private LinkedSubNodesListIterator linkedSubNodesListIterator(long index) {
                checkForConcurrentModificationException();
                Node<E> node;
                if (index < 0L) {
                    index = longSize();
                    node = tailSentinel();                
                } else {
                    node = getNode(index);
                }
                return new LinkedSubNodesListIterator(index, node, this.headSentinel, this.tailSentinel, this.size);
            }
            
            private LinkedSubNodesListIterator linkedSubNodesListIterator(Node<E> node) {
                checkForConcurrentModificationException();
                if (node == null) return new LinkedSubNodesListIterator(this.headSentinel.next, this.headSentinel, this.tailSentinel, this.size);;
                if (!this.contains(node)) throw new IllegalArgumentException("specified node is not part of this sublist");
                return new LinkedSubNodesListIterator(node, this.headSentinel, this.tailSentinel, this.size);
            }            
            
            private class LinkedSubNodesListIterator implements ListIterator<Node<E>> {
                
                private final LinkedNodes.LinkedNodesListIterator listIterator;
                
                // size(-1) or tailSentinel(null) might be unknown but not both at the same time.
                // tailSentinel might also come before headSentinel in the list.
                private long size; // unknown if < 0
                final private Node<E> tailSentinel; // unknown if null
                
                private LinkedSubNodesListIterator(long index, Node<E> node, Node<E> headSentinel, Node<E> tailSentinel, long size) {
                    // assert index >= 0 && index <= longSize() : "index out of range; index=" + index + ", size=" + longSize();
                    // assert node != null && node.linkedNodes == linkedNodes : "Specified node is null or is not linked to this list";
                    // assert headSentinel != null && headSentinel.linkedNodes == linkedNodes : "head sentinel is null or is not linked to this list";
                    // assert tailSentinel != null && tailSentinel.linkedNodes == linkedNodes : "tail sentinel is null or is not linked to this list";
                    // not guaranteed that the tailSentinel is known
                    // not guaranteed that the tailSentinel, if known, comes after the headSentinel in the list
                    // guaranteed that the node comes after the headSentinel in the list
                    //                          and before the tailSentinel unless the tailSentinel comes before the headSentinel
                    this.tailSentinel = tailSentinel;
                    this.size = size;
                    this.listIterator = linkedNodes.linkedNodesListIterator(index, node, headSentinel, knownTailSentinel(tailSentinel));
                    // note if the tailSentinel is unknown, this listIterator will iterate to the end of the parent list
                }
                
                private LinkedSubNodesListIterator(Node<E> node, Node<E> headSentinel, Node<E> tailSentinel, long size) {
                    this.tailSentinel = tailSentinel;
                    this.size = size;
                    this.listIterator = linkedNodes.linkedNodesListIterator(node, headSentinel, knownTailSentinel(tailSentinel));
                    // note if the tailSentinel is unknown, this listIterator will iterate to the end of the parent list
                }
                
                private Node<E> knownTailSentinel(Node<E> tailSentinel) {
                    if (tailSentinel != null) return tailSentinel;
                    LinkedSubNodes sublist = LinkedSubNodes.this;
                    while (sublist != null) {
                        if (sublist.tailSentinel != null) break;
                        sublist = sublist.parent;
                    }
                    if (sublist == null) return null;
                    return sublist.tailSentinel;
                }
                
                private Node<E> targetNode() {
                    return listIterator.targetNode();
                }
                
                @Override
                public boolean hasNext() {
                    checkForConcurrentModificationException();
                    if (size >= 0 && listIterator.cursorIndex() >= (size - 1L)) return false;
                    final boolean hasNext = listIterator.hasNext();
                    if (tailSentinel != null) {
                        if (hasNext) {
                            if (listIterator.cursorNode().next == tailSentinel) return false;
                        } else {
                            if (listIterator.cursorNode().next != tailSentinel) {
                                throw new IllegalStateException("End of list reached unexpectedly; the specified last node most likely comes before the first node in the list");
                            }
                        }
                    }
                    return hasNext;
                }

                @Override
                public boolean hasPrevious() {
                    return listIterator.hasPrevious();
                }

                @Override
                public Node<E> next() {
                    if (!hasNext()) throw new NoSuchElementException();
                    return listIterator.next();
                }

                @Override
                public Node<E> previous() {
                    return listIterator.previous();
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
                public void add(Node<E> node) {
                    listIterator.add(node);
                    if (size >= 0) size++;
                    updateSizeAndModCount(1L);
                }

                @Override
                public void remove() {
                    listIterator.remove();
                    if (size >= 0) size--;
                    updateSizeAndModCount(-1L);
                }

                @Override
                public void set(Node<E> node) {
                    listIterator.set(node);
                    updateSizeAndModCount(0L);
                }

                @Override
                public void forEachRemaining(Consumer<? super Node<E>> action) {
                    checkForConcurrentModificationException();
                    if (action == null) throw new NullPointerException();
                    while (hasNext()) {
                        action.accept(next());
                    }
                    checkForConcurrentModificationException();
                }
                
            } // LinkedSubNodesListIterator
            
        } // LinkedSubNodes
        
        private class SubListIterator implements ListIterator<E> {
            
            private final LinkedSubNodes.LinkedSubNodesListIterator listIterator;
            
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
                return listIterator.next().element;
            }

            @Override
            public E previous() {
                return listIterator.previous().element;
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
                checkForConcurrentModificationException();
                if (action == null) throw new NullPointerException();
                while (hasNext()) {
                    action.accept(next());
                }
                checkForConcurrentModificationException();
            }
            
        } // SubListIterator
        
    } // SubList

    /**
     * Sublist node of a {@code NodableLinkedList.SubList}.
     * Every sublist node has a parent node which backs the sublist node, and
     * is associated with one and only one {@code NodableLinkedList.SubList} instance.
     * Any operation performed on a {@code SubListNode} is performed on in its
     * associated {@code NodableLinkedList.SubList}.
     * 
     * <p>Unlike its parent node, operations on a sublist node are not necessarily
     * performed in constant time because it be necessary to confirm, in linear time,
     * that the sublist node is still an element of its associated sublist.
     * 
     * @author James Pfeifer
     */
    public static class SubListNode<E> implements Comparable<Node<E>> {
        
        private final Node<E> parentNode;
        private final NodableLinkedList<E>.SubList subList;
        
        private int expectedModCount;
        
        private SubListNode(Node<E> parentNode, NodableLinkedList<E>.SubList subList) {
            this.parentNode = parentNode;
            this.subList = subList;
            updateExpectedModCount();
        }
        
        private void updateExpectedModCount() {
            this.expectedModCount = subList.nodableLinkedList().modCount;
        }
                    
        private void stillNodeOfSubList() {
            if (expectedModCount == subList.nodableLinkedList().modCount) return;
            if (!subList.linkedSubNodes().contains(parentNode)) throw new IllegalStateException("This SubListNode is no longer an element of its sublist");
            updateExpectedModCount();
        }
        
        /**
         * Returns the parent node of this sublist node. In other words,
         * returns the node backing this sublist node.
         * 
         * @return the parent node of this sublist node.
         */
        public Node<E> parentNode() {
            return parentNode;
        }
        
        /**
         * Returns the element contained within this sublist node.
         * 
         * @return the element contained within this sublist node.
         */
        public E element() {
            return parentNode.element();
        }

        /**
         * Returns {@code true} if this sublist node belongs to a list.
         * 
         * @return {@code true} if this sublist node belongs to a list.
         */
        public boolean isLinked() {
            return parentNode.isLinked();
        }

        /**
         * Returns the {@code SubList} this sublist node belongs to
         * 
         * @return the {@code SubList} this sublist node belongs to
         */
        public NodableLinkedList<E>.SubList subList() {
            return subList;
        }

        /**
         * Replaces the element of this sublist node with the specified element.
         * 
         * @param element element to be stored in this sublist node.
         */
        public void set(E element) {
            parentNode.set(element);
        }

        /**
         * Inserts the specified node after this sublist node. This sublist node must
         * belong to a sublist and the specified node must not already belong to a list.
         * 
         * @param node the node to be inserted after this sublist node.
         * @throws IllegalStateException if this SubListNode is no longer an element of its sublist.
         * @throws IllegalArgumentException if the specified node is null or is already an element of a list.
         */
        public void addNodeAfterMe(Node<E> node) {
            subList.checkForConcurrentModificationException();
            if (node == null || node.isLinked()) throw new IllegalArgumentException("Specified node is null or is already an element of a list");
            stillNodeOfSubList();
            subList.linkedSubNodes().addNodeAfter(node, parentNode);
            updateExpectedModCount();
        }

        /**
         * Inserts the specified node before this sublist node. This sublist node must
         * belong to a sublist and the specified node must not already belong to a list.
         * 
         * @param node the node to be inserted before this sublist node.
         * @throws IllegalStateException if this SubListNode is no longer an element of its sublist.
         * @throws IllegalArgumentException if the specified node is null or is already an element of a list.
         */
        public void addNodeBeforeMe(Node<E> node) {
            subList.checkForConcurrentModificationException();
            if (node == null || node.isLinked()) throw new IllegalArgumentException("Specified node is null or is already an element of a list");
            stillNodeOfSubList();
            subList.linkedSubNodes().addNodeBefore(node, parentNode);
            updateExpectedModCount();
        }

        /**
         * Returns {@code true} if there exists a node which comes after this sublist node
         * in a sublist.  In other words, returns {@code true} if this sublist node is not
         * the last node of a sublist. 
         * 
         * @return {@code true} if this sublist node is not the last node of a sublist. 
         * @throws IllegalStateException if this SubListNode is no longer an element of its sublist.
         */
        public boolean hasNext() {
            subList.checkForConcurrentModificationException();
            stillNodeOfSubList();
            return subList.linkedSubNodes().hasNodeAfter(parentNode);
        }

        /**
         * Returns {@code true} if there exists a node which comes before this sublist node
         * in a sublist.  In other words, returns {@code true} if this sublist node is not
         * the first node of a sublist.
         * 
         * @return {@code true} if this sublist node is not the first node of a sublist.
         * @throws IllegalStateException if this SubListNode is no longer an element of its sublist.
         */
        public boolean hasPrevious() {
            subList.checkForConcurrentModificationException();
            stillNodeOfSubList();
            return subList.linkedSubNodes().hasNodeBefore(parentNode);
        }

        /**
         * Returns the index of this sublist node in its sublist,
         * or -1 if this sublist node does not belong to a sublist or
         * the {@code index > Integer.MAX_VALUE}.
         *
         * @return the index of this sublist node in its sublist,
         *         or -1 if this sublist node does not belong to a sublist or
         *         the {@code index > Integer.MAX_VALUE}.
         */
        public int index() {
            subList.checkForConcurrentModificationException();
            final long index = subList.linkedSubNodes().getIndex(parentNode);
            return (index > Integer.MAX_VALUE) ? -1 : (int)index;
        }

        /**
         * Returns the node which comes after this sublist node in a sublist.
         * if this sublist node is the last or only node, {@code null} is returned.
         * 
         * @return the node which comes after this sublist node in a sublist, or
         *         {@code null} if this sublist node is the last or only node.
         * @throws IllegalStateException if this SubListNode is no longer an element of its sublist.
         */
        public Node<E> next() {
            subList.checkForConcurrentModificationException();
            stillNodeOfSubList();
            return subList.linkedSubNodes().getNodeAfter(parentNode);
        }

        /**
         * Returns the node which comes before this sublist node in a sublist.
         * if this sublist node is the first or only node, {@code null} is returned.
         * 
         * @return the node which comes before this sublist node in a sublist, or
         *         {@code null} if this sublist node is the first or only node.
         * @throws IllegalStateException if this SubListNode is no longer an element of its sublist.
         */
        public Node<E> previous() {
            subList.checkForConcurrentModificationException();
            stillNodeOfSubList();
            return subList.linkedSubNodes().getNodeBefore(parentNode);
        }

        /**
         * Removes this sublist node from the sublist it is linked to.
         * 
         * @throws IllegalStateException if this SubListNode is no longer an element of its sublist.
         */
        public void remove() {
            subList.checkForConcurrentModificationException();
            stillNodeOfSubList();
            subList.linkedSubNodes().removeNode(parentNode);
        }

        /**
         * Replaces this sublist node, in a sublist, with the specified node.
         * This sublist node must belong to a sublist, and
         * the replacement node must not already belong to a list.
         * 
         * @param node replacement node to replace this sublist node.
         * @throws IllegalStateException if this SubListNode is no longer an element of its sublist.
         * @throws IllegalArgumentException if the replacement node is {@code null} or
         *                                  already an element of a list.
         */
        public void replaceWith(Node<E> node) {
            subList.checkForConcurrentModificationException();
            if (node == null || node.isLinked()) throw new IllegalArgumentException("Replacement node is null or already an element of a list");
            stillNodeOfSubList();
            subList.linkedSubNodes().replaceNode(parentNode, node);
        }

        /**
         * Swaps this sublist node with the specified node. Both this sublist node and
         * the specified must belong to a list, but they can be different lists.
         * 
         * <p><strong>Synchronization consideration:</strong> This operation can potentially operate on two
         * different lists. if synchronization is required, both lists should be synchronized by the same
         * object.
         * 
         * @param node the node to swap with this sublist node
         * @throws IllegalStateException if this SubListNode is no longer an element of its sublist.
         * @throws IllegalArgumentException if the swap node is {@code null} or not linked
         */
        public void swapWith(Node<E> node) {
            subList.checkForConcurrentModificationException();
            if (node == null || !node.isLinked()) throw new IllegalArgumentException("The specified node is null or not an element of a list");
            stillNodeOfSubList();
            parentNode.swapWith(node);
            subList.linkedSubNodes().swappedNodes(parentNode, node);
            subList.linkedSubNodes().updateSizeAndModCount(0L);
        }

        /**
         * Swaps this sublist node with the specified sublist node. Both this sublist node
         * and the specified sublist node must belong to a sublist, but they can be different sublists.
         * 
         * <p><strong>Synchronization consideration:</strong> This operation can potentially operate on two
         * different lists. if synchronization is required, both lists should be synchronized by the same
         * object.
         * 
         * @param subListNode the sublist node to swap with this sublist node
         * @throws IllegalStateException if this SubListNode is no longer an element of its sublist.
         * @throws IllegalArgumentException if the swap node is {@code null} or not linked
         */
        public void swapWith(SubListNode<E> subListNode) {
            subList.checkForConcurrentModificationException();
            if (subListNode == null || !subListNode.isLinked()) throw new IllegalArgumentException("The specified subListNode is null or not an element of a sublist");
            subListNode.subList().checkForConcurrentModificationException();
            subListNode.stillNodeOfSubList();
            stillNodeOfSubList();
            parentNode.swapWith(subListNode.parentNode);
            subList.linkedSubNodes().swappedNodes(parentNode, subListNode.parentNode);
            subListNode.subList().linkedSubNodes().swappedNodes(subListNode.parentNode, parentNode);
            subListNode.subList().linkedSubNodes().updateSizeAndModCount(0L);
            subList.linkedSubNodes().updateSizeAndModCount(0L);
        }

        /**
         * Compares this sublist node with the specified object for equality.
         * Returns {@code true} if and only if the specified object is also a node,
         * and both pairs of elements in the two nodes are <i>equal</i>.
         * (Two elements {@code e1} and {@code e2} are <i>equal</i> if
         * {@code (e1==null ? e2==null : e1.equals(e2))}.)
         *
         * @param object object to be compared for equality with this sublist node
         * @return {@code true} if the specified object is equal to this sublist node
         */
        @Override
        public boolean equals(Object object) {
            return parentNode.equals(object);
        }

        /**
         * Compares this sublist node to the specified node for order. Returns a negative integer,
         * zero, or a positive integer as this sublist node is less than, equal
         * to, or greater than the specified node.
         *
         * @param node node to be compared to this sublist node.
         * @return a negative integer, zero, or a positive integer as the
         *         this sublist node is less than, equal to, or greater than the
         *         specified node.
         * @throws NullPointerException if the specified sublist node is null
         * @throws ClassCastException if the nodes' element types prevent them from
         *         being compared.
         */
        @Override
        public int compareTo(Node<E> node) {
            return parentNode.compareTo(node);
        }

        /**
         * Returns the hash code value of this sublist node.
         *
         * @return the hash code value of this sublist node
         */
        @Override
        public int hashCode() {
            return parentNode.hashCode();
        }
        
    } // SubListNode

    /**
     * Node of a {@code NodableLinkedList}. Contains references to the
     * previous and next nodes in a doubly-linked list, and contains an
     * element which can be {@code null}. Does not belong to any
     * particular {@code NodableLinkedList} until the node is inserted/added.
     * Once inserted, the node remains linked to a {@code NodableLinkedList} until
     * removed. A node can belong to different lists, just not at the same time.
     * 
     * <p>All operations, except {@code index}, are performed in constant time.
     * 
     * @serialData Only the element is serialized. the references to the
     *             next and previous nodes in a list, are not serialized.
     *             When deserialized, the node will not be linked to any list.
     * 
     * @author James Pfeifer
     * @param <E> the type of element held in this node.
     */
    public static class Node<E> implements Serializable, Cloneable, Comparable<Node<E>> {

        private static final long serialVersionUID = 5774389459071333685L;

        private E element;

        private transient Node<E> next = null;
        private transient Node<E> previous = null;		
        private transient NodableLinkedList<E>.LinkedNodes linkedNodes = null;

        /**
         * Constructs a node containing a null element.
         */
        public Node() {
            this.element = null;
        }

        /**
         * Constructs a node containing the specified element.
         * 
         * @param element element to be stored in this node
         */
        public Node(E element) {
            this.element = element;
        }

        /**
         * Constructs a node as a copy of the specified node.
         * The constructed node will contain the same element
         * as the specified node, and will not be linked.
         * 
         * @param node node to be copied
         */
        public Node(Node<E> node) {
            this.element = node.element;
        }

        /**
         * Returns the element contained within this node.
         * 
         * @return the element contained within this node.
         */
        public E element() {
            return this.element;
        }

        /**
         * Returns {@code true} if this node belongs to a list.
         * 
         * @return {@code true} if this node belongs to a list.
         */
        public boolean isLinked() {
            return linkedNodes != null;
        }

        /**
         * Returns the {@code LinkedNodes} list this node
         *         belongs to, or {@code null} if not linked.
         * 
         * @return the {@code LinkedNodes} list this node
         *         belongs to, or {@code null} if not linked.
         */
        public NodableLinkedList<E>.LinkedNodes linkedNodes() {
            return linkedNodes;
        }

        /**
         * Returns the {@code NodableLinkedList} this node
         *         belongs to, or {@code null} if not linked.
         *         
         * @return the {@code NodableLinkedList} this node
         *         belongs to, or {@code null} if not linked.
         */
        public NodableLinkedList<E> nodableLinkedList() {
            return (this.isLinked()) ? linkedNodes.nodableLinkedList() : null;
        }		

        /**
         * Returns a shallow copy of this node.  The clone will have the
         * same element as this node, but will not be linked to any list.
         *
         * @return a shallow copy of this node.
         */
        @Override
        public Object clone() {
            try {
                @SuppressWarnings("unchecked")
                final Node<E> clone = (Node<E>) super.clone();
                clone.next = null;
                clone.previous = null;
                clone.linkedNodes = null;
                return clone;				
            } catch (CloneNotSupportedException e) {
                throw new AssertionError("Not Cloneable? "+e.getMessage(), e);
            }
        }

        /**
         * Replaces the element of this node with the specified element.
         * 
         * @param element element to be stored in this node.
         */
        public void set(E element) {
            this.element = element;
        }

        /**
         * Inserts this node after the specified node. This node must not already belong to a
         * list, and the specified node must belong to a list.
         * 
         * @param node the node this node is to be inserted after.
         * @throws IllegalStateException if this node is already an element of a list.
         * @throws IllegalArgumentException if the specified node is null or not an element of a list.
         */
        public void addAfter(Node<E> node) {
            if (this.isLinked()) throw new IllegalStateException("This node is already an element of a list");
            if (node == null || !node.isLinked()) throw new IllegalArgumentException("Specified Node is null or not an element of a list");
            node.linkedNodes.addNodeAfter(this, node);
        }
        
        /**
         * Inserts this node after the specified sublist node. This node must not already belong to a
         * list, and the specified sublist node must belong to a sublist.
         * 
         * @param subListNode the sublist node this node is to be inserted after.
         * @throws IllegalStateException if this node is already an element of a list.
         * @throws IllegalArgumentException if the specified sublist node is null or not an element of a sublist.
         */        
        public void addAfter(SubListNode<E> subListNode) {
            if (subListNode == null) throw new IllegalArgumentException("Specified SubListNode is null");
            subListNode.addNodeAfterMe(this);
        }

        /**
         * Inserts this node before the specified node. This node must not already belong to a
         * list, and the specified node must belong to a list.
         * 
         * @param node the node this node is to be inserted before.
         * @throws IllegalStateException if this node is already an element of a list.
         * @throws IllegalArgumentException if the specified node is null or not an element of a list.
         */
        public void addBefore(Node<E> node) {
            if (this.isLinked()) throw new IllegalStateException("This node is already an element of a list");
            if (node == null || !node.isLinked()) throw new IllegalArgumentException("Specified Node is null or not an element of a list");		
            node.linkedNodes.addNodeBefore(this, node);
        }
        

        /**
         * Inserts this node before the specified sublist node. This node must not already belong to a
         * list, and the specified sublist node must belong to a sublist.
         * 
         * @param subListNode the sublist node this node is to be inserted before.
         * @throws IllegalStateException if this node is already an element of a list.
         * @throws IllegalArgumentException if the specified sublist node is null or not an element of a sublist.
         */
        public void addBefore(SubListNode<E> subListNode) {
            if (subListNode == null) throw new IllegalArgumentException("Specified SubListNode is null");
            subListNode.addNodeBeforeMe(this);
        }

        /**
         * Returns {@code true} if there exists a node which comes after this node
         * in a list.  In other words, returns {@code true} if this node is not
         * the last node of a list. 
         * 
         * @return {@code true} if this node is not the last node of a list. 
         * @throws IllegalStateException if this node is not linked.
         */
        public boolean hasNext() {
            if (!this.isLinked()) throw new IllegalStateException("This node is not an element of a list"); 
            return linkedNodes.hasNodeAfter(this);
        }

        /**
         * Returns {@code true} if there exists a node which comes before this node
         * in a list.  In other words, returns {@code true} if this node is not
         * the first node of a list.
         * 
         * @return {@code true} if this node is not the first node of a list.
         * @throws IllegalStateException if this node is not linked.
         */
        public boolean hasPrevious() {
            if (!this.isLinked()) throw new IllegalStateException("This node is not an element of a list");
            return linkedNodes.hasNodeBefore(this);
        }

        /**
         * Returns the index of this node in a list,
         * or -1 if this node does not belong to a list or
         * the {@code index > Integer.MAX_VALUE}.
         *
         * @return the index of this node in a list,
         *         or -1 if this node does not belong to a list or
         *         the {@code index > Integer.MAX_VALUE}.
         */
        public int index() {
            if (!this.isLinked()) return -1;
            final long index = linkedNodes.indexOfNode(this);
            return (index > Integer.MAX_VALUE) ? -1 : (int)index;
        }

        /**
         * Returns the node which comes after this node in a list.  if this node is
         * the last or only node, {@code null} is returned.
         * 
         * @return the node which comes after this node in a list, or
         *         {@code null} if this node is the last or only node.
         * @throws IllegalStateException if this node is not linked.
         */
        public Node<E> next() {
            if (!this.isLinked()) throw new IllegalStateException("This node is not an element of a list");
            return linkedNodes.getNodeAfter(this);
        }

        /**
         * Returns the node which comes before this node in a list.  if this node is
         * the first or only node, {@code null} is returned.
         * 
         * @return the node which comes before this node in a list, or
         *         {@code null} if this node is the first or only node.
         * @throws IllegalStateException if this node is not linked.
         */
        public Node<E> previous() {
            if (!this.isLinked()) throw new IllegalStateException("This node is not an element of a list");
            return linkedNodes.getNodeBefore(this);
        }

        /**
         * Removes this node from the list it is linked to.
         * 
         * @throws IllegalStateException if this node is not linked.
         */
        public void remove() {
            if (!this.isLinked()) throw new IllegalStateException("This node is not an element of a list");
            linkedNodes.removeNode(this);
        }

        /**
         * Replaces this node, in a list, with the specified node.  This node must belong to a
         * list, and the replacement node must not already belong to a list.
         * 
         * @param node replacement node to replace this node.
         * @throws IllegalStateException if this node is not linked.
         * @throws IllegalArgumentException if the replacement node is {@code null} or
         *                                  already an element of a list.
         */
        public void replaceWith(Node<E> node) {
            if (!this.isLinked()) throw new IllegalStateException("This node is not an element of a list");
            if (node == null || node.isLinked()) throw new IllegalArgumentException("Replacement node is null or already an element of a list");
            linkedNodes.replaceNode(this, node);			
        }

        /**
         * Swaps this node with the specified node. Both this node and the specified must belong to a list,
         * but they can be different lists.
         * 
         * <p><strong>Synchronization consideration:</strong> This operation can potentially operate on two
         * different lists. if synchronization is required, both lists should be synchronized by the same
         * object.
         * 
         * @param node the node to swap with this node
         * @throws IllegalStateException if this node is not linked.
         * @throws IllegalArgumentException if the swap node is {@code null} or not linked
         */
        public void swapWith(Node<E> node) {
            if (!this.isLinked()) throw new IllegalStateException("This node is not an element of a list");
            if (node == null || !node.isLinked()) throw new IllegalArgumentException("The specified node is null or not an element of a list");
            swapNodes(this, node);
        }

        /**
         * Returns a {@code SubListNode} for the specified sublist. The sublist node
         * is backed by this node which must be a node of the specified sublist.
         * 
         * @param subList sublist containing this node
         * @return a {@code SubListNode}, backed by this node, for the specified sublist
         * @throws IllegalStateException if this node is not a node of the specified subList
         */
        public SubListNode<E> subListNode(NodableLinkedList<E>.SubList subList) {
            if (!subList.linkedSubNodes().contains(this)) throw new IllegalStateException("Node is not an  element of the specified subList");
            return new SubListNode<E>(this, subList);
        }
        
        /**
         * Compares this node with the specified object for equality.  Returns
         * {@code true} if and only if the specified object is also a node,
         * and both pairs of elements in the two nodes are <i>equal</i>.
         * (Two elements {@code e1} and {@code e2} are <i>equal</i> if
         * {@code (e1==null ? e2==null : e1.equals(e2))}.)
         *
         * @param object object to be compared for equality with this node
         * @return {@code true} if the specified object is equal to this node
         */
        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (object == null) return false;
            if (!(object instanceof Node)) return false;
            final Object thisElement = this.element();
            final Object thatElement = ((Node<?>)object).element();
            return (thisElement==null) ? thatElement==null : thisElement.equals(thatElement);
        }

        /**
         * Compares this node to the specified node for order. Returns a negative integer,
         * zero, or a positive integer as this node is less than, equal
         * to, or greater than the specified node.
         *
         * @param node node to be compared to this node.
         * @return a negative integer, zero, or a positive integer as the
         *         this node is less than, equal to, or greater than the
         *         specified node.
         * @throws NullPointerException if the specified node is null
         * @throws ClassCastException if the nodes' element types prevent them from
         *         being compared.
         */
        @Override
        public int compareTo(Node<E> node) {
            if (node == null) throw new NullPointerException("The specified node is null");
            if (this == node) return 0;
            final Object thisElement = this.element();
            if (!(thisElement instanceof Comparable)) throw new ClassCastException("This node's element is not Comparable");
            @SuppressWarnings("unchecked")
            final Comparable<E> comparableThisElement = (Comparable<E>)thisElement;
            try { return (comparableThisElement).compareTo(node.element()); }
            catch (IllegalArgumentException e) {
                throw new ClassCastException("The specified node's element type is not compatible for comparison: "+e.getMessage());
            }
        }

        /**
         * Returns the hash code value of this node.
         *
         * @return the hash code value of this node
         */
        @Override
        public int hashCode() {
            return 31 + ((element()==null) ? 0 : element().hashCode());
        }

    } // Node

} // NodableLinkedList
