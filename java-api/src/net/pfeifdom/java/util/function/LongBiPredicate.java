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

import java.util.Objects;

/**
 * Represents a predicate (boolean-valued function) of two {@code long}-valued
 * operands. This is the two-arity specialization of {@link java.util.function.Predicate}.
 *
 * <p>
 * This is a functional interface whose functional method is
 * {@link #test(long, long)}.
 *
 * @see java.util.function.Predicate
 * @since 1.1
 * @since JDK 1.8
 */
@FunctionalInterface
public interface LongBiPredicate {
    
    /**
     * Evaluates this predicate on the given operands.
     *
     * @param left  the left operand
     * @param right the right operand
     * @return the result of the predicate function on the given operands
     */
    boolean test(long left, long right);

    /**
     * Returns a composed predicate that represents a short-circuiting logical AND
     * of this predicate and another. When evaluating the composed predicate, if
     * this predicate is {@code false}, then the {@code other} predicate is not
     * evaluated.
     *
     * <p>
     * Any exceptions thrown during evaluation of either predicate are relayed to
     * the caller; if evaluation of this predicate throws an exception, the
     * {@code other} predicate will not be evaluated.
     *
     * @param other a predicate that will be logically-ANDed with this predicate
     * @return a composed predicate that represents the short-circuiting logical AND
     *         of this predicate and the {@code other} predicate
     * @throws NullPointerException if other is null
     */
    default LongBiPredicate and(LongBiPredicate other) {
        Objects.requireNonNull(other);
        return (left, right) -> test(left, right) && other.test(left, right);
    }

    /**
     * Returns a predicate that represents the logical negation of this predicate.
     *
     * @return a predicate that represents the logical negation of this predicate
     */
    default LongBiPredicate negate() {
        return (left, right) -> !test(left, right);
    }

    /**
     * Returns a composed predicate that represents a short-circuiting logical OR of
     * this predicate and another. When evaluating the composed predicate, if this
     * predicate is {@code true}, then the {@code other} predicate is not evaluated.
     *
     * <p>
     * Any exceptions thrown during evaluation of either predicate are relayed to
     * the caller; if evaluation of this predicate throws an exception, the
     * {@code other} predicate will not be evaluated.
     *
     * @param other a predicate that will be logically-ORed with this predicate
     * @return a composed predicate that represents the short-circuiting logical OR
     *         of this predicate and the {@code other} predicate
     * @throws NullPointerException if other is null
     */
    default LongBiPredicate or(LongBiPredicate other) {
        Objects.requireNonNull(other);
        return (left, right) -> test(left, right) || other.test(left, right);
    }

}
