/* Copyright (C) 2023 James R. Pfeifer. All rights reserved.
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

/**
 * Contains collection classes.
 * 
 * <p>
 * Currently only contains one class: {@code NodableLinkedList} which is
 * basically a drop in replacement for the standard {@code java.util.LinkedList}
 * class. Use {@code NodableLinkedList} in place of {@code java.util.LinkedList}
 * when:
 * <ul>
 * <li>you require the capability to insert or remove elements, anywhere in the
 * list, in constant time
 * <li>you require the flexibility of traversing the list via the next and
 * previous pointers of the nodes which comprise the linked list
 * <li>you require the capability to create a reversed list prior to JDK 21
 * <li>the list could have more than {@code Integer.MAX_VALUE} elements
 * </ul>
 * 
 * @since 1.0.0
 */
package net.pfeifdom.java.util;