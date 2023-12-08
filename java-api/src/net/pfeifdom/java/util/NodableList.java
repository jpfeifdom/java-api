/* NodableList.java -- NodableLinkedList extension of the List interface
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

import java.util.Collection;
import java.util.List;
import java.util.ListIterator;

import net.pfeifdom.java.util.NodableLinkedList.Node;

/**
 * An extension of the {@code List} interface that adds methods which operate
 * on a {@code NodableLinkedList.Node}.
 *
 * @param <E> the type of elements in this list
 *
 * @author  James Pfeifer
 * @see List
 * @see NodableLinkedList
 */
public interface NodableList<E> extends List<E> {
    
    /**
     * Returns the number of elements in this list.
     *
     * @return the number of elements in this list.
     */
    public long longSize();
    
    /**
     * Inserts all of the elements in the specified collection into this
     * list, before the specified node.  Shifts the element
     * currently at that position and any subsequent elements to
     * the right (increases their indices).  The new elements will appear
     * in the list in the order that they are returned by the
     * specified collection's iterator. if the specified node is null,
     * the elements will be appended to the end of this list.
     *
     * @param T type of element held in the specified node
     * @param node node the specified collection is to be inserted before
     * @param c collection containing elements to be added to this list
     * @return {@code true} if this list changed as a result of the call
     * @throws ClassCastException if the class of an element of the specified collection prevents
     *                            it from being added to this list
     * @throws IllegalArgumentException if the specified node is not linked to this list, or
     *                                  if some property of an element of the specified collection
     *                                  prevents it from being added to this list
     * @throws NullPointerException if the specified collection is null, or
     *                              if the specified collection contains one or more null elements
     *                              and this list does not permit null elements
     */
    public <T> boolean addAll(Node<T> node, Collection<? extends E> c);
    
    /**
     * Returns a list-iterator of the elements in this list (in proper
     * sequence), starting at the specified node in the list.
     * if the specified node is {@code null}, the list-iterator will
     * start with the first node in this list.
     *
     * @param T type of element held in the specified node
     * @param node first node whose element is to be returned from the list-iterator (by a call to {@code next})
     * @return a ListIterator of the elements in this list (in proper
     *         sequence), starting at the specified node in the list
     * @throws IllegalArgumentException if the specified is node not linked to this list
     */
    public <T> ListIterator<E> listIterator(Node<T> node);
    
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
     * <p>The semantics of the list returned by this method become undefined if
     * the backing list (i.e., this list) is <i>structurally modified</i> in
     * any way other than via the returned list.  (Structural modifications are
     * those that change the size of this list, or otherwise perturb it in such
     * a fashion that iterations in progress may yield incorrect results.)
     * A {@code ConcurrentModificationException} is thrown for any operation on
     * the returned sublist if this list is structurally modified.
     *
     * @param T type of element held in the specified first and last nodes
     * @param firstNode low endpoint (inclusive) of the subList
     * @param lastNode high endpoint (inclusive) of the subList
     * @return a view of the specified range within this list
     * @throws IllegalArgumentException if any specified node is not linked to this list or
     *                                  if the lastNode comes before the firstNode in this list.
     */
    public <T> NodableList<E> subList(Node<T> firstNode, Node<T> lastNode);

}
