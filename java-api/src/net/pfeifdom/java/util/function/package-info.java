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
 * <em>Functional interfaces</em> provide target types for lambda expressions
 * and method references.  Each functional interface has a single abstract
 * method, called the <em>functional method</em> for that functional interface,
 * to which the lambda expression's parameter and return types are matched or
 * adapted.
 *
 * <p>The interfaces in this package are annotated with
 * {@link java.lang.FunctionalInterface}. This annotation is not a requirement
 * for the compiler to recognize an interface as a functional interface, but
 * merely an aid to capture design intent and enlist the help of the compiler in
 * identifying accidental violations of design intent.
 *
 * <p>Functional interfaces often represent abstract concepts like functions,
 * actions, or predicates.  In documenting functional interfaces, or referring
 * to variables typed as functional interfaces, it is common to refer directly
 * to those abstract concepts, for example using "this function" instead of
 * "the function represented by this object".  When an API method is said to
 * accept or return a functional interface in this manner, such as "applies the
 * provided function to...", this is understood to mean a <i>non-null</i>
 * reference to an object implementing the appropriate functional interface,
 * unless potential nullity is explicitly specified.
 *
 * @see java.lang.FunctionalInterface
 * @since 1.1.0
 */
package net.pfeifdom.java.util.function;