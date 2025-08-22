/* Copyright (C) 2025 James R. Pfeifer. All rights reserved.
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

package net.pfeifdom.java.util.function;

/**
 * Represents an operation that accepts a {@code int}-valued argument and a
 * {@code long}-valued argument, and returns no result. This is the
 * {@code (int, long)} specialization of {@link java.util.functon.BiConsumer}.
 * Unlike most other functional interfaces, {@code IntLongConsumer} is expected
 * to operate via side-effects.
 *
 * <p>
 * This is a functional interface whose functional method is
 * {@link #accept(int, long)}.
 *
 * @see java.util.function.BiConsumer
 * @since 1.1
 * @since JDK 1.8
 */
@FunctionalInterface
public interface IntLongConsumer {
    
    /**
     * Performs this operation on the given arguments.
     *
     * @param intArg  the {@code int}-valued argument
     * @param longArg the {@code long}-valued argument
     */
    void accept(int intArg, long longArg);

}
