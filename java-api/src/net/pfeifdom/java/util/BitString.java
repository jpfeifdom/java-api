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

package net.pfeifdom.java.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ConcurrentModificationException;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.IntToLongFunction;
import java.util.function.LongBinaryOperator;
import java.util.function.LongToIntFunction;
import java.util.function.LongUnaryOperator;
import java.util.function.Predicate;
import java.util.function.ToLongFunction;

import net.pfeifdom.java.util.function.IntLongConsumer;
import net.pfeifdom.java.util.function.LongBiPredicate;

/**
 * A mutable sequence of bits.
 * 
 * <p>
 * A BitString is not a binary number.
 * <p>
 * Every bit string has a capacity. As long as the length of the bit sequence
 * contained in the bit string does not exceed the capacity, it is not necessary
 * to allocate a new internal buffer. If the internal buffer overflows, it is
 * automatically made larger.
 * <p>
 * By default, all bits in the string initially have the value
 * {@code ZERO}/{@code false}.
 * <p>
 * Operations with multiple bit string operands can operate on substrings from
 * the same bit string, but if the substrings overlap, the results are
 * unpredictable.
 * <p>
 * Unless otherwise noted, passing a null parameter to any of the methods in a
 * {@code BitString} will result in a {@code NullPointerException}.
 * <p>
 * A {@code BitString} is not safe for multithreaded use without external
 * synchronization.
 * <p>
 * Methods in this class that do not otherwise have a value to return are
 * specified to return the bit string upon which they are invoked. This allows
 * method invocations to be chained.
 * 
 * <pre>
 *              q
 *          | 0 | 1 |
 *       ===|=======|
 *        0 | r | r |
 *      p --|-------| r = p op q
 *        1 | r | r |
 *       ============
 * 
 * op:             and       andnot              nornot              xor       or
 *      |=======| |=======| |=======| |=======| |=======| |=======| |=======| |=======|
 *      | 0 | 0 | | 0 | 0 | | 0 | 0 | | 0 | 0 | | 0 | 1 | | 0 | 1 | | 0 | 1 | | 0 | 1 |
 *      |-------| |-------| |-------| |-------| |-------| |-------| |-------| |-------|
 *      | 0 | 0 | | 0 | 1 | | 1 | 0 | | 1 | 1 | | 0 | 0 | | 0 | 1 | | 1 | 0 | | 1 | 1 |
 *      ========= ========= ========= ========= ========= ========= ========= =========
 *       set(0)                        set(p)              set(q)
 * 
 * op:   nor       xnor                ornot               nandnot   nand
 *      |=======| |=======| |=======| |=======| |=======| |=======| |=======| |=======|
 *      | 1 | 0 | | 1 | 0 | | 1 | 0 | | 1 | 0 | | 1 | 1 | | 1 | 1 | | 1 | 1 | | 1 | 1 |
 *      |-------| |-------| |-------| |-------| |-------| |-------| |-------| |-------|
 *      | 0 | 0 | | 0 | 1 | | 1 | 0 | | 1 | 1 | | 0 | 0 | | 0 | 1 | | 1 | 0 | | 1 | 1 |
 *      ========= ========= ========= ========= ========= ========= ========= =========
 *                           set(~q)             set(~p)                       set(1)
 *                           
 *      bit-wise method lookup table:
 *          p = p op q where p is the data bit being acted on, and
 *                           q is the control bit (argument).
 *          First select the column with the action (0, 1, FLIP, or UNCHANGED)
 *          you want performed on the data (p) bit when the control (q)
 *          bit is 1. Then select the row with the action you want performed
 *          on the data bit when the control bit is 0. Where the selected
 *          column and row intersect is the name of  method (op) you want to use
 *          to perform your selected actions.
 *          For example, If you want the data bit to flip when the control bit is 1 and
 *          left unchanged when the control bit is 0, use the xor op.                      
 *      |====================================================================|                  
 *      | q |                                        1                       |
 *      |---|================================================================|                       
 *      |   | action on p ||      0      |     1     |   FLIP    | UNCHANGED |
 *      |   |=============||=============|===========|===========|===========| 
 *      |   |           0 ||    clear    |   copy    |  norNot   |    and    |
 *      |   |-------------||-------------|-----------|-----------|-----------|
 *      |   |           1 ||   copyNot   |    set    |   nand    |   orNot   |
 *      | 0 |-------------||-------------|-----------|-----------|-----------|
 *      |   |        FLIP ||     nor     |  nandNot  |   flip    |   xnor    |
 *      |   |-------------||-------------|-----------|-----------|-----------|
 *      |   |   UNCHANGED ||    andNot   |     or    |    xor    |           |
 *      |===|================================================================|
 * 
 * </pre> 
 * 
 * @author James Pfeifer
 * @since 1.1
 * @since JDK 1.8
 */
public abstract class BitString implements Cloneable, Serializable  {
    
    /**
     * 
     */
    private static final long serialVersionUID = 3661279563936726028L;
    
    /*
     * BitStrings are packed into arrays of "words."  Currently a word is
     * a long, which consists of 64 bits, requiring 6 address bits.
     * The choice of word size is determined purely by performance concerns.
     */
    private static final int ADDRESS_BITS_PER_WORD = 6;
    static final int BITS_PER_WORD = 1 << ADDRESS_BITS_PER_WORD;
    private static final int BIT_INDEX_MASK = BITS_PER_WORD - 1;

    private static final long WORD_MASK = 0xffffffffffffffffL;
    private static final long BIT_MASK = 0x8000000000000000L;
    
    static final int MAX_BYTES = Integer.MAX_VALUE / Byte.SIZE + 1;
    static final int MAX_CHARS = Integer.MAX_VALUE / Character.SIZE + 1;
    static final int MAX_DOUBLES = Integer.MAX_VALUE / Double.SIZE + 1;
    static final int MAX_FLOATS = Integer.MAX_VALUE / Float.SIZE + 1;
    static final int MAX_INTS = Integer.MAX_VALUE / Integer.SIZE + 1;
    static final int MAX_LONGS = Integer.MAX_VALUE / Long.SIZE + 1;
    static final int MAX_SHORTS = Integer.MAX_VALUE / Short.SIZE + 1;
    
    public static final BitString ONES = new Constant(WORD_MASK);
    public static final BitString ZEROS = new Constant(0L);
    
    public static final boolean ONE = true;
    public static final boolean ZERO = false;
    public static final boolean ONE_FILL = ONE;
    public static final boolean ZERO_FILL = ZERO;
    public static final boolean ONE_PAD = ONE;
    public static final boolean ZERO_PAD = ZERO;
    private static final boolean ONE_DFLT = ONE;
    private static final boolean ZERO_DFLT = ZERO;
    
    /**
     * Bitwise unary operators.
     * 
     * @author james
     *
     */
    public enum UnaryOp {
        /**
         * 
         */
        CLEAR   ((arg) -> { return 0L; },   (arg, bitMask) -> { return arg & ~bitMask; }),
        FLIP    ((arg) -> { return ~arg; }, (arg, bitMask) -> { return arg ^  bitMask; }),
        SET     ((arg) -> { return -1L; },  (arg, bitMask) -> { return arg |  bitMask; });
        private LongUnaryOperator op;
        private LongBinaryOperator bitOp;
        private UnaryOp(LongUnaryOperator op, LongBinaryOperator bitOp) { this.op = op; this.bitOp = bitOp;}
        private LongUnaryOperator op() { return op; }
        private LongBinaryOperator bitOp() { return bitOp; }

        /**
         * Returns unary operator <b>SET</b> if the specified bit is true, otherwise,
         * unary operator <b>CLEAR</b> is returned.
         * 
         * @param bit used to select either SET or CLEAR
         * 
         * @return bit ? SET : CLEAR
         */
        public static UnaryOp set(boolean bit) { return bit ? SET : CLEAR; }
    }
    
    /**
     * Bitwise binary operators.
     * 
     * @author james
     *
     */
    public enum BinaryOp {
        /**
         * 
         */
        AND     ((lArg, rArg) -> { return lArg & rArg; }),
        ANDNOT  ((lArg, rArg) -> { return lArg & ~rArg; }),
        COPY    ((lArg, rArg) -> { return rArg; }),
        COPYNOT ((lArg, rArg) -> { return ~rArg; }),
        NAND    ((lArg, rArg) -> { return ~(lArg & rArg); }),
        NANDNOT ((lArg, rArg) -> { return ~lArg | rArg; }),
        NOR     ((lArg, rArg) -> { return ~(lArg | rArg); }),
        NORNOT  ((lArg, rArg) -> { return ~lArg & rArg; }),
        OR      ((lArg, rArg) -> { return lArg | rArg; }),
        ORNOT   ((lArg, rArg) -> { return lArg | ~rArg; }),
        XNOR    ((lArg, rArg) -> { return ~(lArg ^ rArg); }),
        /**
         * 
         */
        XOR     ((lArg, rArg) -> { return lArg ^ rArg; });
        private LongBinaryOperator op;
        private BinaryOp(LongBinaryOperator op) { this.op = op; }
        private LongBinaryOperator op() { return op; }
    }
    
    /**
     * Operation directions.
     * 
     * @author james
     *
     */
    public enum Direction {
        LEFT        (false),
        RIGHT       (true),
        LTR         (true),
        RTL         (false),
        NEXT        (true),
        PREVIOUS    (false);
        private boolean direction;
        private Direction(boolean direction) { this.direction = direction; }
        boolean isLeft()        { return !direction; }
        boolean isRight()       { return direction; }
        boolean isLTR()         { return direction; }
        boolean isRTL()         { return !direction; }
        boolean isNext()        { return direction; }
        boolean isPrevious()    { return !direction; }
    }
    
    /**
     * Operation positions.
     * 
     * @author james
     *
     */
    public enum Position {
        FIRST       (false),
        LAST        (true),
        LEADING     (false),
        TRAILING    (true);
        private boolean position;
        private Position(boolean position) { this.position = position; }
        boolean isFirst()       { return !position; }
        boolean isLast()        { return position; }
        boolean isLeading()     { return !position; }
        boolean isTrailing()    { return position; }
    }
    
    /**
     * Returns a Field with the specified offset and length.
     * 
     * @param offset the offset of the Field
     * @param length the length of the Field.
     * @return a Field with the specified offset and length.
     * @throws IllegalArgumentException if {@code offset < 0 || length < 0}
     */
    public static Field field(int offset, int length) {
        return new Field(offset, length);
    }
    
//    /**
//     * 
//     * @param fromIndex
//     * @param toIndex
//     * @return
//     */
    public static Field indexField(int fromIndex, int toIndex) {
        return Field.indexRange(fromIndex, toIndex);
    }
    
    /**
     * A Field which represents all the bits of a BitString. The offset of this
     * Field is 0 and the length is equal to the target BitString's length.
     */
    public static final Field ALL = new Field.All();
    
    /**
     * Returns a Field which represents all the bits of this {@code BitString}. The
     * offset of the returned Field is 0 and its length is this.length().
     * 
     * @return a Field which represents all the bits of this {@code BitString}
     */
    public Field all() {
        return all(0);
    }
    
    /**
     * Returns a Field which represents a substring of this {@code BitString}.
     * 
     * If position equals this.length(), an empty Field (length == 0) positioned at
     * the end of this {@code BitString} is returned, otherwise, a Field
     * representing a substring which starts at offset 'position' of this
     * {@code BitString} and extends to the end of this {@code BitString} is
     * returned.
     * 
     * @param position the start of the substring
     * @return a Field which represents a substring of this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code position < 0 || position > length()}
     */
    public Field all(int position) {
        checkThisPosition(position);
        if (position == this.length()) return field(position == 0 ? 0 : position - 1, 0);
        return field(position, this.length() - position);
    }
    
    /**
     * The number of bits in this bit string
     */
    int stringLength;
    
    /**
     * Incremented any time the length of the bit string is modified
     * in a way that could invalidate Ranges
     */
    private long modCount = 0L;
    
    BitString() {
        this(0);
    }

     /**
      * Creates a new {@code BitString} with the specified length. The capacity of
      * the new {@code BitString} will equal the nearest multiple of the word size (64
      * bits) greater than or equal to the length. All bits are initially set to
      * {@code ZERO}.
      *
      * @param length the initial length of the new {@code BitString}
      * @throws IllegalArgumentException if the specified length is negative
      */
    BitString(int length) {
        checknBits(length);
        this.stringLength = length;
    }
    
    abstract BitString newBitString(int length);
    
    abstract void resizeBackingArray(int capacity);
    
    /**
     * Returns the capacity of this {@code BitString}.
     * 
     * @return the capacity of this {@code BitString}
     */
    public abstract int capacity();
    
    public int ensureCapacity(int capacity) {
        if (capacity > capacity()) resizeBackingArray(capacity);
        return capacity();
    }
    
    public int trimToLength() {
        resizeBackingArray(length());
        return capacity();
    }
    
    public int length() {
        return this.stringLength;
    }
    
    int baseLength() {
        return this.stringLength;
    }
    
    public boolean isEmpty() {
        return length() == 0;
    }
    
    public void setLength(int newLength) {
        if (newLength == this.stringLength) return;
        if (newLength < 0) throw new IllegalArgumentException("specified length is negative: " + newLength);
        final int oldLength = this.stringLength;
        ensureCapacity(newLength);
        this.stringLength = newLength;
        if (newLength > oldLength) iClear(oldLength, newLength - oldLength);
        if (newLength < oldLength) incrementModCount(); // do not invalidate Ranges if appending
    }
    
    void iSetLength(int newLength) {
        assert newLength >= 0;
        if (newLength == this.stringLength) return;
        ensureCapacity(newLength);
        if (newLength < this.stringLength) incrementModCount(); // do not invalidate Ranges if appending
        this.stringLength = newLength;
    }
    
    // a Range of this BitString has changed its length by the specified delta at the specified bitIndex
    void setRangeLength(int delta, int bitIndex) {
        if (delta == 0) return;
        final long newLength = length() + delta;
        assert newLength >= 0;
        if (newLength > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "the specified new length will cause the base BitString length to exceed the maximum size of "
                            + Integer.MAX_VALUE + ": " + "newLength=" + newLength + ", delta=" + delta);
        }
        if (bitIndex == bitIndex(length())) {
            setLength((int) newLength);
        } else {
            if (delta > 0) {
                setLength((int) newLength);
                iShiftRight(delta, ZERO_FILL, bitIndex, length() - bitIndex);
            } else {
                iShiftLeft(delta, ZERO_FILL, bitIndex, length() - bitIndex);
                setLength((int) newLength);
            }
            incrementModCount();
        }
    }
    
    long modCount() {
        return this.modCount;
    }
    
    void incrementModCount() {
        this.modCount++;
    }
    
    /**
     * Cloning this {@code BitString} produces a new {@code BitString}
     * that is equal to it.
     * The clone of the bit set is another bit string that has exactly the
     * same bits set to {@code true} as this bit string.
     *
     * @return a clone of this bit string
     */
    @Override
    public BitString clone() {
        try {
            return (BitString) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }

    abstract long getWord(int wordIndex);
    
    abstract void putWord(int wordIndex, long word);
    
    private void putWordBits(int bitIndex, int nBits, long word) {
        assert nBits <= BITS_PER_WORD;
        if (nBits <= 0) return;
        
        final int wordIndex = wordIndex(bitIndex);
        final int shift = wordBitIndex(bitIndex);
        final long mask = WORD_MASK << (BITS_PER_WORD - nBits);
        word = word & mask;
        
        putWord(wordIndex,
                (getWord(wordIndex) & ~(mask >>> shift)) | (word >>> shift));
        if (nBits + shift > BITS_PER_WORD) {
            putWord(wordIndex + 1,
                    (getWord(wordIndex + 1) & (WORD_MASK >>> shift)) | (word << (BITS_PER_WORD - shift)));
        }
    }
    
    int bitIndex(int offset) {
        return offset;
    }
    
    int firstBitIndex() {
        return firstBitIndex(0);
    }
    
    int firstBitIndex(int offset) {
        return bitIndex(offset);
    }
    
    int lastBitIndex() {
        return lastBitIndex(0, length());
    }
    
    private int lastBitIndex(int offset, int length) {
        return firstBitIndex(offset) + (length - 1);
    }
    
    private static int wordBitIndex(int bitIndex) {
        return bitIndex & BIT_INDEX_MASK; 
    }
    
    private int firstWordBitIndex(int offset) {
        return wordBitIndex(firstBitIndex(offset));
     }
    
    private int lastWordBitIndex(int offset, int length) {
        return wordBitIndex(lastBitIndex(offset, length));
    }
    
    private int leftMarginSize(int offset) {
        return leftMarginSizeOfWordBitIndex(firstWordBitIndex(offset));
    }
    
    private int leftMarginSizeOfWordBitIndex(int wordBitIndex) {
        return wordBitIndex;
    }
    
    private int rightMarginSize() {
        return rightMarginSize(0, length());
    }
    
    private int rightMarginSize(int offset, int length) {
        return rightMarginSizeOfWordBitIndex(lastWordBitIndex(offset, length));
    }
    
    private int rightMarginSizeOfWordBitIndex(int wordBitIndex) {
        return BITS_PER_WORD - wordBitIndex - 1;
    }
    
    /**
     * Given a bit index, return index of word containing the bit.
     */
    static int wordIndex(int bitIndex) {
        return bitIndex >> ADDRESS_BITS_PER_WORD;
    }
    
    private int firstWordIndex(int offset) {
        return wordIndex(firstBitIndex(offset));
    }
    
    private int lastWordIndex() {
        return wordIndex(lastBitIndex());
    }
    
    private int lastWordIndex(int offset, int length) {
        return wordIndex(lastBitIndex(offset, length));
    }
    
    private int[] getIterator() {
        return getIterator(0, length());
    }
    
    private int[] getIterator(int offset, int length) {
        final int[] iterator = new int[7];
        iterator[0] = -1; // word index
        iterator[1] = firstWordIndex(offset);
        iterator[2] = lastWordIndex(offset, length);
        iterator[3] = leftMarginSize(offset);
        iterator[4] = rightMarginSize(offset, length);
        iterator[5] = length; // remaining bit count
        iterator[6] = 0; // word bit count
        return iterator;
    }
    
    private int getIteratorWordIndex(int[] iterator) {
        return iterator[0];
    }
    
    private int getIteratorWordBitCount(int[] iterator) {
        return iterator[6];
    }
    
    private boolean hasNextIteratorWord(int[] iterator) {
        // return currentWordIndex != lastWordIndex && remainingLength > 0
        return iterator[0] != iterator[2] && iterator[5] > 0;
    }
    
    private boolean hasPreviousIteratorWord(int[] iterator) {
        // return currentWordIndex != firstWordIndex && remainingLength > 0
        return iterator[0] != iterator[1] && iterator[5] > 0;
    }
    
    private long getNextIteratorWord(int[] iterator) {
        return getNextIteratorWord(iterator, ZERO_FILL);
    }
    
    private long getNextIteratorWord(int[] iterator, boolean fill) {
        int wordIndex = iterator[0];
        final int firstWordIndex = iterator[1];
        final int lastWordIndex = iterator[2];
        final int leftMarginSize = iterator[3];
        final int rightMarginSize = iterator[4];
        final int remainingLength = iterator[5];
        if (wordIndex == lastWordIndex || remainingLength <= 0) throw new IllegalStateException();
        if (wordIndex == -1) wordIndex = firstWordIndex;
        else wordIndex++;
        iterator[0] = wordIndex;
        iterator[6] = (remainingLength > BITS_PER_WORD) ? BITS_PER_WORD : remainingLength;
        iterator[5] -= iterator[6];
        
        return shiftWordLeft(leftMarginSize, fill, wordIndex, lastWordIndex, rightMarginSize);
    }
    
    private long getPreviousIteratorWord(int[] iterator) {
        return getPreviousIteratorWord(iterator, ZERO_FILL);
    }
    
    private long getPreviousIteratorWord(int[] iterator, boolean fill) {
        int wordIndex = iterator[0];
        final int firstWordIndex = iterator[1];
        final int lastWordIndex = iterator[2];
        final int leftMarginSize = iterator[3];
        final int rightMarginSize = iterator[4];
        int remainingLength = iterator[5];
        if (wordIndex == firstWordIndex || remainingLength <= 0) throw new IllegalStateException();
        if (wordIndex == -1) wordIndex = lastWordIndex;
        else wordIndex--;
        iterator[0] = wordIndex;
        iterator[6] = (remainingLength > BITS_PER_WORD) ? BITS_PER_WORD : remainingLength;
        iterator[5] -= iterator[6];
        
        return shiftWordRight(rightMarginSize, fill, wordIndex, firstWordIndex, leftMarginSize);
    }
    
    /**
     * Returns rArg shifted right the specified number of bits. The far right bits in
     * rArg that are shifted out, are lost. The far left bits in rArg are replaced
     * by the far right bits that are shifted in from lArg.
     * 
     * @param shift the number of bits to shift right
     * @param lArg left argument
     * @param rArg right argument 
     * @return rArg shifted right the specified number of bits
     */
    private static long shiftArgsRight(int shift, long lArg, long rArg) {
        assert (shift > 0 && shift < BITS_PER_WORD);
        return (lArg << (BITS_PER_WORD - shift)) | (rArg >>> shift);
    }
    
    /**
     * Returns lArg shifted left the specified number of bits. The far left bits in
     * lArg that are shifted out, are lost. The far right bits in lArg are replaced
     * by the far left bits that are shifted in from rArg.
     * 
     * @param shift the number of bits to shift left
     * @param lArg left argument
     * @param rArg right argument
     * @return lArg shifted left the specified number of bits
     */
    private static long shiftArgsLeft(int shift, long lArg, long rArg) {
        assert (shift > 0 && shift < BITS_PER_WORD);
        return (lArg << shift) | (rArg >>> (BITS_PER_WORD - shift));
    }
    
    private long shiftWord(int shift, boolean fill, int wordIndex,
            int firstWordIndex, int lastWordIndex,
            int leftMarginSize, int rightMarginSize) {
        assert shift > -BITS_PER_WORD && shift < BITS_PER_WORD;
        long word;
        if (shift == 0) word = getWord(wordIndex);
        else if (shift > 0) word = shiftWordRight(shift, fill, wordIndex, firstWordIndex, leftMarginSize);
        else                word = shiftWordLeft(-shift, fill, wordIndex, lastWordIndex, rightMarginSize);
        return word;
    }
    
    private long shiftWordLeft(int shift, boolean fill, int wordIndex,
            int lastWordIndex, int rightMarginSize) {
        assert shift >= 0 && shift < BITS_PER_WORD;
        long word = getWord(wordIndex);
        if (shift == 0) return word;
        if (wordIndex == lastWordIndex) {
            word = fillRightMargin(fill, word, wordIndex, lastWordIndex, rightMarginSize);
            word = shiftArgsLeft(shift, word, fill ? WORD_MASK : 0L);
        } else {
            final long nextWord = fillRightMargin(fill, getWord(wordIndex+1), wordIndex+1, lastWordIndex, rightMarginSize);
            word = shiftArgsLeft(shift, word, nextWord);
        }
        return word;
    }
    
    private long shiftWordRight(int shift, boolean fill, int wordIndex,
            int firstWordIndex, int leftMarginSize) {
        assert shift >= 0 && shift < BITS_PER_WORD;
        long word = getWord(wordIndex);
        if (shift == 0) return word;
        if (wordIndex == firstWordIndex) {
            word = fillLeftMargin(fill, word, wordIndex, firstWordIndex, leftMarginSize);
            word = shiftArgsRight(shift, fill ? WORD_MASK : 0L, word);
        } else {
            final long prevWord = fillLeftMargin(fill, getWord(wordIndex-1), wordIndex-1, firstWordIndex, leftMarginSize);
            word = shiftArgsRight(shift, prevWord, word);
        }
        return word;
    }
    
    /**
     * Fill the margins of the specified word with the specified fill value.
     * 
     * @param fill            value
     * @param word            word whose margins are to be filled
     * @param wordIndex       index of the specified word of the BitString
     * @param firstWordIndex  index of the first word of the BitString
     * @param lastWordIndex   index of the last word of the BitString
     * @param leftMarginSize  size, in bits, of the left margin
     * @param rightMarginSize size, in bits, of the right margin
     * @return word with its margins filled with the fill value
     */
    private static long fillRightMargin(boolean fill, long word, int wordIndex,
            int lastWordIndex, int rightMarginSize) {
        if (wordIndex == lastWordIndex && rightMarginSize > 0) {
            if (fill) {
                word |= WORD_MASK >>> (BITS_PER_WORD - rightMarginSize);
            } else {
                word &= WORD_MASK << rightMarginSize;
            }
        }
        return word;
    }
    
    private static long fillLeftMargin(boolean fill, long word, int wordIndex,
            int firstWordIndex, int leftMarginSize) {
        if (wordIndex == firstWordIndex && leftMarginSize > 0) {
            if (fill) {
                word |= WORD_MASK << (BITS_PER_WORD - leftMarginSize);
            } else {
                word &= WORD_MASK >>> leftMarginSize;
            }
        }
        return word;
    }
    
    private void restoreMarginsIf(int wordIndex,
            long originalFirstWord, long originalLastWord,
            int firstWordIndex, int lastWordIndex,
            int leftMarginSize, int rightMarginSize) {
        if (wordIndex == firstWordIndex) restoreLeftMargin(originalFirstWord, firstWordIndex, leftMarginSize);
        if (wordIndex == lastWordIndex) restoreRightMargin(originalLastWord, lastWordIndex, rightMarginSize);
    }
    
    private void restoreMargins(
            long originalFirstWord, long originalLastWord,
            int firstWordIndex, int lastWordIndex,
            int leftMarginSize, int rightMarginSize) {
        restoreLeftMargin(originalFirstWord, firstWordIndex, leftMarginSize);
        restoreRightMargin(originalLastWord, lastWordIndex, rightMarginSize);
    }
    
    private void restoreLeftMargin(long originalFirstWord,
            int firstWordIndex, int leftMarginSize) {
        if (leftMarginSize > 0) {
            putWord(firstWordIndex,
                    (originalFirstWord & (WORD_MASK << (BITS_PER_WORD - leftMarginSize)))
                  | (getWord(firstWordIndex) & (WORD_MASK >>> leftMarginSize)));
        }
    }
    
    private void restoreRightMargin(long originalLastWord,
            int lastWordIndex, int rightMarginSize) {
        if (rightMarginSize > 0) {
            putWord(lastWordIndex,
                    (originalLastWord & (WORD_MASK >>> (BITS_PER_WORD - rightMarginSize)))
                  | (getWord(lastWordIndex) & (WORD_MASK << rightMarginSize)));
        }
    }
    
    static long[] packBooleans(boolean[] booleans) {
        return pack(booleans.length, 1,
                (index) -> { return booleans[index] ? 1L : 0L; });
    }
    
    static long[] packBytes(byte[] bytes) {
        return pack(bytes.length, Byte.SIZE,
                (index) -> { return Byte.toUnsignedLong(bytes[index]); });
    }
    
    static long[] packChars(char[] chars) {
        return pack(chars.length, Character.SIZE,
                (index) -> { return (long)(chars[index]); });
    }
    
    static long[] packDoubles(double[] doubles) {
        return pack(doubles.length, Long.SIZE,
                (index) -> { return Double.doubleToRawLongBits(doubles[index]); });
    }
    
    static long[] packFloats(float[] floats) {
        return pack(floats.length, Integer.SIZE,
                (index) -> { return Integer.toUnsignedLong(Float.floatToRawIntBits(floats[index])); });
    }
    
    static long[] packInt(int primitiveInt) {
        return pack(1, Integer.SIZE,
                (index) -> { return Integer.toUnsignedLong(primitiveInt); });
    }
    
    static long[] packInts(int[] ints) {
        return pack(ints.length, Integer.SIZE,
                (index) -> { return Integer.toUnsignedLong(ints[index]); });
    }
    
    static long[] packShorts(short[] shorts) {
        return pack(shorts.length, Short.SIZE,
                (index) -> { return Short.toUnsignedLong(shorts[index]); });
    }
    
    static long[] pack(int primitiveCount, int primitiveSize,
            IntToLongFunction getPrimitiveAsUnsignedLong) {
        final int primitivesPerLong = Long.SIZE / primitiveSize;
        final long[] longs = new long[(primitiveCount + primitivesPerLong - 1) / primitivesPerLong];
        for (int i = 0, l = 0, s = 0;
                i < primitiveCount;
                l = ++i / primitivesPerLong, s = (s+1) % primitivesPerLong) {
            longs[l] |= getPrimitiveAsUnsignedLong.applyAsLong(i) << (Long.SIZE - primitiveSize * (s + 1));
        }
        return longs;
    }
    
    private static boolean isValidRelativeOffset(int offset, int length) {
        return offset == 0 || offset > 0 && offset < length;
    }
    
    private static void checkRelativeOffset(int offset, int length) {
        if (!isValidRelativeOffset(offset, length)) {
            throw new StringIndexOutOfBoundsException(
                    "specified offset is negative, or greater than or equal to the length; offset=" + offset + ", length=" + length);
        }
    }
    
    private boolean isValidOffset(int offset) {
        return isValidRelativeOffset(offset, this.length());
    }
    
    void checkThisOffset(int offset) {
        if (!isValidOffset(offset)) {
            throw new StringIndexOutOfBoundsException(
                    "specified offset is invalid for this BitString; offset=" + offset + ", length=" + length());
        }
    }
    
    void checkArgOffset(int offset) {
        if (!isValidOffset(offset)) {
            throw new StringIndexOutOfBoundsException(
                    "The specified BitString's offset is invalid; offset=" + offset + ", length=" + length());
        }
    }
    
    private boolean isValidPosition(int position) {
        return position >= 0 && position <= this.length();
    }
    
    private void checkThisPosition(int position) {
        if (!isValidPosition(position)) {
            throw new StringIndexOutOfBoundsException(
                    "specified position is invalid for this BitString; position=" + position + ", length=" + length());
        }
    }
    
    private boolean isValidLength(int offset, int length) {
        return length >= 0 && length <= (this.length() - offset);
    }
    
    void checkThisLength(int offset, int length) {
        if (!isValidLength(offset, length))
            throw new IllegalArgumentException("specified length is negative or exceeds offset+length of this BitString; length="
                    + length + " specified BitString's offset=" + offset + " length=" + this.length());
    }
    
    void checkArgLength(int offset, int length) {
        if (!isValidLength(offset, length))
            throw new IllegalArgumentException(
                    "specified length exceeds offset+length of the specified BitString; length=" + length
                            + " specified BitString's offset=" + offset + " length=" + this.length());
    }
    
    static void checknBits(int nBits) {
        if (nBits < 0) {
            throw new IllegalArgumentException("argument is negative: " + nBits);
        }
    }
    
    private boolean spaceIsAvailable(int availableSpace, long requiredSpace) {
        return availableSpace >= requiredSpace;
    }
    
    private void checkAvailableSpace(int offset, int availableSpace, int requiredSpace) {
        if (!(spaceIsAvailable(availableSpace, requiredSpace))) {
            throw new UnsupportedOperationException("not enough space starting at offset " + offset
                    + " to perform the operation"
                    + "; required space=" + requiredSpace
                    + ", available space=" + availableSpace);
        }
    }
    
    private void checkAvailableSpace(int offset, int availableSpace, long requiredSpace, int count) {
        if (!(spaceIsAvailable(availableSpace, requiredSpace))) {
            throw new UnsupportedOperationException("not enough space starting at offset " + offset
                    + " to perform the operation"
                    + "; required space=" + requiredSpace
                    + ", available space=" + availableSpace
                    + ", Array Count=" + count);
        }
    }
    
    void checkBaseLengthIncrease(long increase) {
        if (baseLength() + increase > Integer.MAX_VALUE) {
            throw new UnsupportedOperationException(
                    "the operation would result in the length of the base BitString exceeding the maximum of "
                            + Integer.MAX_VALUE + ": base BitString length=" + baseLength() + ": increase=" + increase);
        }
    }
    
    void iAppend(BitString that, int thatOffset, int thatLength) {
        assert that.isValidOffset(thatOffset);
        assert that.isValidLength(thatOffset, thatLength);
        final int thisLength = this.length();
        final int newLength = thisLength + thatLength;
        ensureCapacity(newLength);
        setLength(newLength);
        iCopy(thisLength, thatLength, that, thatOffset);
    }
    
    void iDelete(int bitIndex, int length) {
        assert isValidOffset(bitIndex);
        assert isValidLength(bitIndex, length);
        if (length > 0) {
            iShiftLeft(length, ZERO_FILL, bitIndex, length() - bitIndex);
            setLength(length() - length);
        }
    }
    
    void iInsert(int position, BitString that, int thatOffset, int thatLength) {
        assert this.isValidPosition(position);
        assert that.isValidOffset(thatOffset);
        assert that.isValidLength(thatOffset, thatLength);
        if (thatLength == 0) return;
        if (position == this.length()) {
            iAppend(that, thatOffset, thatLength);
            return;
        }
        ensureCapacity(this.length() + thatLength);
        setLength(this.length() + thatLength);
        iShiftRight(thatLength, ZERO_FILL, position, this.length() - position);
        iCopy(position, thatLength, that, thatOffset);
    }
    
    void iReplace(int thisBitIndex, int thisLength, BitString that, int thatOffset, int thatLength) {
        assert this.isValidOffset(thisBitIndex);
        assert this.isValidLength(thisBitIndex, thisLength);
        assert that.isValidOffset(thatOffset);
        assert that.isValidLength(thatOffset, thatLength);
        final int newLength = this.length() - thisLength + thatLength;
        ensureCapacity(newLength);
        if (newLength > length()) setLength(newLength);
        iShiftRight(thatLength - thisLength, ZERO_FILL, thisBitIndex, this.length() - thisBitIndex);
        if (newLength < length()) setLength(newLength);
        iCopy(thisBitIndex, thatLength, that, thatOffset);
    }
    
    void iClear(int offset, int length) {
        iUnaryOp(UnaryOp.CLEAR.op(), offset, length);
    }
    
    private void iClearBit(int offset) {
        iBitOp(UnaryOp.CLEAR, offset);
    }
    
    private void iFlip(int offset, int length) {
        iUnaryOp(UnaryOp.FLIP.op(), offset, length);
    }
    
    private void iFlipBit(int offset) {
        iBitOp(UnaryOp.FLIP, offset);
    }
    
    private void iSet(int offset, int length) {
        iUnaryOp(UnaryOp.SET.op(), offset, length);
    }
    
    private void iSetBit(int offset) {
        iBitOp(UnaryOp.SET, offset);
    }
    
    private void iOp(UnaryOp unaryOp, int offset, int length) {
        iUnaryOp(unaryOp.op(), offset, length);
    }
    
    private void iBitOp(UnaryOp unaryOp, int offset) {
        iBitOp(unaryOp.bitOp(), offset);
    }
    
    private void iBitOp(LongBinaryOperator op, int offset) {
        final int bitIndex = bitIndex(offset);
        final int wordIndex = wordIndex(bitIndex);
        putWord(wordIndex, op.applyAsLong(getWord(wordIndex), BIT_MASK >>> wordBitIndex(bitIndex)));
    }
    
    private void iUnaryOp(LongUnaryOperator op, int offset, int length) {
        
        assert isValidOffset(offset);
        assert isValidLength(offset, length);
        if (length == 0) return;
        
        final int firstWordIndex = firstWordIndex(offset);
        final int lastWordIndex = lastWordIndex(offset, length);
        final int leftMarginSize = leftMarginSize(offset);
        final int rightMarginSize = rightMarginSize(offset, length);
        
        final long originalFirstWord = getWord(firstWordIndex);
        final long originalLastWord = getWord(lastWordIndex);
        
        for (int wordIndex = firstWordIndex; wordIndex <= lastWordIndex; wordIndex++) {
            putWord(wordIndex, op.applyAsLong(getWord(wordIndex)));
        }
        
        restoreMargins(originalFirstWord, originalLastWord,
                firstWordIndex, lastWordIndex,
                leftMarginSize, rightMarginSize);
        
    }
    
    /**
     * Perform an <b>AND</b> operation on a substring of this BitString and a
     * substring the specified BitString (that).
     * 
     * <pre>
     *                 that bit
     *                  value
     *                | 0 | 1 |
     *             ===|=======|
     *        this  0 | 0 | 0 |
     *         bit ---|-------|
     *       value  1 | 0 | 1 |
     *             ============
     * </pre>
     * 
     * @param thisOffset the offset of this substring
     * @param length     the length of the substrings
     * @param that       the argument bit string
     * @param thatOffset the offset of that substring
     */
    private void iAnd(int thisOffset, int length, BitString that, int thatOffset) {
        iBinaryOpLTR(BinaryOp.AND.op(), thisOffset, length, that, thatOffset);
    }
    
    /**
     * Perform an <b>ANDNOT</b> operation on a substring of this BitString and a
     * substring the specified BitString (that).
     * 
     * <pre>
     *                 that bit
     *                  value
     *                | 0 | 1 |
     *             ===|=======|
     *        this  0 | 0 | 0 |
     *         bit ---|-------|
     *       value  1 | 1 | 0 |
     *             ============
     * </pre>
     * 
     * @param thisOffset the offset of this substring
     * @param length     the length of the substrings
     * @param that       the argument bit string
     * @param thatOffset the offset of that substring
     */
    private void iAndNot(int thisOffset, int length, BitString that, int thatOffset) {
        iBinaryOpLTR(BinaryOp.ANDNOT.op(), thisOffset, length, that, thatOffset);
    }
    
    private void iCopy(int thisOffset, int length, BitString that, int thatOffset) {
        iBinaryOpLTR(BinaryOp.COPY.op(), thisOffset, length, that, thatOffset);
    }
    
    private void iCopyRTL(int thisOffset, int length, BitString that, int thatOffset) {
        iBinaryOpRTL(BinaryOp.COPY.op(), thisOffset, length, that, thatOffset);
    }

    private void iCopyNot(int thisOffset, int length, BitString that, int thatOffset) {
        iBinaryOpLTR(BinaryOp.COPYNOT.op(), thisOffset, length, that, thatOffset);
    }
    
    /**
     * Perform an <b>NAND</b> operation on a substring of this BitString and a
     * substring the specified BitString (that).
     * 
     * <pre>
     *                 that bit
     *                  value
     *                | 0 | 1 |
     *             ===|=======|
     *        this  0 | 1 | 1 |
     *         bit ---|-------|
     *       value  1 | 1 | 0 |
     *             ============
     * </pre>
     * 
     * @param thisOffset the offset of this substring
     * @param length     the length of the substrings
     * @param that       the argument bit string
     * @param thatOffset the offset of that substring
     */
    private void iNand(int thisOffset, int length, BitString that, int thatOffset) {
        iBinaryOpLTR(BinaryOp.NAND.op(), thisOffset, length, that, thatOffset);
    }
    
    /**
     * Perform an <b>NANDNOT</b> operation on a substring of this BitString and a
     * substring the specified BitString (that).
     * 
     * <pre>
     *                 that bit
     *                  value
     *                | 0 | 1 |
     *             ===|=======|
     *        this  0 | 1 | 1 |
     *         bit ---|-------|
     *       value  1 | 0 | 1 |
     *             ============
     * </pre>
     * 
     * @param thisOffset the offset of this substring
     * @param length     the length of the substrings
     * @param that       the argument bit string
     * @param thatOffset the offset of that substring
     */
    private void iNandNot(int thisOffset, int length, BitString that, int thatOffset) {
        iBinaryOpLTR(BinaryOp.NANDNOT.op(), thisOffset, length, that, thatOffset);
    }
    
    /**
     * Perform an <b>NOR</b> operation on a substring of this BitString and a
     * substring the specified BitString (that).
     * 
     * <pre>
     *                 that bit
     *                  value
     *                | 0 | 1 |
     *             ===|=======|
     *        this  0 | 1 | 0 |
     *         bit ---|-------|
     *       value  1 | 0 | 0 |
     *             ============
     * </pre>
     * 
     * @param thisOffset the offset of this substring
     * @param length     the length of the substrings
     * @param that       the argument bit string
     * @param thatOffset the offset of that substring
     */
    private void iNor(int thisOffset, int length, BitString that, int thatOffset) {
        iBinaryOpLTR(BinaryOp.NOR.op(), thisOffset, length, that, thatOffset);
    }
    
    /**
     * Perform an <b>NORNOT</b> operation on a substring of this BitString and a
     * substring the specified BitString (that).
     * 
     * <pre>
     *                 that bit
     *                  value
     *                | 0 | 1 |
     *             ===|=======|
     *        this  0 | 0 | 1 |
     *         bit ---|-------|
     *       value  1 | 0 | 0 |
     *             ============
     * </pre>
     * 
     * @param thisOffset the offset of this substring
     * @param length     the length of the substrings
     * @param that       the argument bit string
     * @param thatOffset the offset of that substring
     */
    private void iNorNot(int thisOffset, int length, BitString that, int thatOffset) {
        iBinaryOpLTR(BinaryOp.NORNOT.op(), thisOffset, length, that, thatOffset);
    }
    
    /**
     * Perform an <b>OR</b> operation on a substring of this BitString and a
     * substring the specified BitString (that).
     * 
     * <pre>
     *                 that bit
     *                  value
     *                | 0 | 1 |
     *             ===|=======|
     *        this  0 | 0 | 1 |
     *         bit ---|-------|
     *       value  1 | 1 | 1 |
     *             ============
     * </pre>
     * 
     * @param thisOffset the offset of this substring
     * @param length     the length of the substrings
     * @param that       the argument bit string
     * @param thatOffset the offset of that substring
     */
    private void iOr(int thisOffset, int length, BitString that, int thatOffset) {
        iBinaryOpLTR(BinaryOp.OR.op(), thisOffset, length, that, thatOffset);
    }
    
    /**
     * Perform an <b>ORNOT</b> operation on a substring of this BitString and a
     * substring the specified BitString (that).
     * 
     * <pre>
     *                 that bit
     *                  value
     *                | 0 | 1 |
     *             ===|=======|
     *        this  0 | 1 | 0 |
     *         bit ---|-------|
     *       value  1 | 1 | 1 |
     *             ============
     * </pre>
     * 
     * @param thisOffset the offset of this substring
     * @param length     the length of the substrings
     * @param that       the argument bit string
     * @param thatOffset the offset of that substring
     */
    private void iOrNot(int thisOffset, int length, BitString that, int thatOffset) {
        iBinaryOpLTR(BinaryOp.ORNOT.op(), thisOffset, length, that, thatOffset);
    }
    
    /**
     * Perform an <b>XNOR</b> operation on a substring of this BitString and a
     * substring the specified BitString (that).
     * 
     * <pre>
     *                 that bit
     *                  value
     *                | 0 | 1 |
     *             ===|=======|
     *        this  0 | 1 | 0 |
     *         bit ---|-------|
     *       value  1 | 0 | 1 |
     *             ============
     * </pre>
     * 
     * @param thisOffset the offset of this substring
     * @param length     the length of the substrings
     * @param that       the argument bit string
     * @param thatOffset the offset of that substring
     */
    private void iXnor(int thisOffset, int length, BitString that, int thatOffset) {
        iBinaryOpLTR(BinaryOp.XNOR.op(), thisOffset, length, that, thatOffset);
    }
    
    /**
     * Perform an <b>XOR</b> operation on a substring of this BitString and a
     * substring the specified BitString (that).
     * 
     * <pre>
     *                 that bit
     *                  value
     *                | 0 | 1 |
     *             ===|=======|
     *        this  0 | 0 | 1 |
     *         bit ---|-------|
     *       value  1 | 1 | 0 |
     *             ============
     * </pre>
     * 
     * @param thisOffset the offset of this substring
     * @param length     the length of the substrings
     * @param that       the argument bit string
     * @param thatOffset the offset of that substring
     */
    private void iXor(int thisOffset, int length, BitString that, int thatOffset) {
        iBinaryOpLTR(BinaryOp.XOR.op(), thisOffset, length, that, thatOffset);
    }
    
    private void iOp(BinaryOp binaryOp, Direction direction,
            int thisOffset, int thisLength,
            BitString that, int thatOffset, int thatLength, boolean pad) {
        iOp(binaryOp, direction, thisOffset, thisLength, that, thatOffset, thatLength);
        if (thatLength < thisLength) {
            if (direction.isLTR()) thisOffset += thatLength;
            iBinaryOpLTR(binaryOp.op(), thisOffset, thisLength - thatLength, pad ? ONES : ZEROS, 0);
        }
    }
    
    private void iOp(BinaryOp binaryOp, Direction direction,
            int thisOffset, int thisLength,
            BitString that, int thatOffset, int thatLength) {
        final int length = Math.min(thisLength, thatLength);
        if (direction.isLTR()) {
            iBinaryOpLTR(binaryOp.op(), thisOffset, length, that, thatOffset);
        } else {
            iBinaryOpRTL(binaryOp.op(), thisOffset - length + thisLength, length, that, thatOffset - length + thatLength);
        }
    }
    
    /**
     * Perform the specified bitwise operation (op) on a substring of this {@code BitString}
     * and a substring of the specified bit string (that).
     * 
     * @param thisOffset the offset of this substring
     * @param length the length of the substrings
     * @param that the argument bit string
     * @param thatOffset the offset of that substring
     * @param op the bitwise operation to perform
     */
    private void iBinaryOpLTR(LongBinaryOperator op,
            int thisOffset, int length, BitString that, int thatOffset) {
        
        assert this.isValidOffset(thisOffset);
        assert this.isValidLength(thisOffset, length);
        assert that.isValidOffset(thatOffset);
        assert that.isValidLength(thatOffset, length);
        if (length == 0) return;
        
        final int thisFirstWordIndex = this.firstWordIndex(thisOffset);
        final int thisLastWordIndex = this.lastWordIndex(thisOffset, length);
        final int thatFirstWordIndex = that.firstWordIndex(thatOffset);
        final int thatLastWordIndex = that.lastWordIndex(thatOffset, length);
        final int thisLeftMarginSize = this.leftMarginSize(thisOffset);
        final int thisRightMarginSize = this.rightMarginSize(thisOffset, length);
        final int thatLeftMarginSize = that.leftMarginSize(thatOffset);
        final int thatRightMarginSize = that.rightMarginSize(thatOffset, length);
        
        final long thisOriginalFirstWord = this.getWord(thisFirstWordIndex);
        final long thisOriginalLastWord = this.getWord(thisLastWordIndex);
        
        long arg;
        int thatShift = this.firstWordBitIndex(thisOffset) - that.firstWordBitIndex(thatOffset);
        for (int thisWordCursor = thisFirstWordIndex, thatWordCursor = thatFirstWordIndex;
                thisWordCursor <= thisLastWordIndex;
                thisWordCursor++, thatWordCursor++) {
            
            if (thatWordCursor > thatLastWordIndex) {
                // This situation occurs if thisWord spans across two words,
                // but thatWord, before it is shifted into place,
                // is contained wholly within a single word.
                // The portion of the last thatWord that is shifted into
                // the next word, needs to be shifted into place.
                assert thisWordCursor == thisLastWordIndex;
                thatShift = this.lastWordBitIndex(thisOffset, length) - that.lastWordBitIndex(thatOffset, length);
                thatWordCursor = thatLastWordIndex;
            }
            
            // Shift that substring (the argument) so that it aligns up with this substring.
            arg = that.shiftWord(thatShift, ZERO_FILL, thatWordCursor,
                    thatFirstWordIndex, thatLastWordIndex,
                    thatLeftMarginSize, thatRightMarginSize);
            
            putWord(thisWordCursor, op.applyAsLong(getWord(thisWordCursor), arg));
            
            restoreMarginsIf(thisWordCursor,
                    thisOriginalFirstWord, thisOriginalLastWord,
                    thisFirstWordIndex, thisLastWordIndex,
                    thisLeftMarginSize, thisRightMarginSize);
            
        }
        
    }
    
    private void iBinaryOpRTL(LongBinaryOperator op,
            int thisOffset, int length, BitString that, int thatOffset) {
        
        assert this.isValidOffset(thisOffset);
        assert this.isValidLength(thisOffset, length);
        assert that.isValidOffset(thatOffset);
        assert that.isValidLength(thatOffset, length);
        if (length == 0) return;
        
        final int thisFirstWordIndex = this.firstWordIndex(thisOffset);
        final int thisLastWordIndex = this.lastWordIndex(thisOffset, length);
        final int thatFirstWordIndex = that.firstWordIndex(thatOffset);
        final int thatLastWordIndex = that.lastWordIndex(thatOffset, length);
        final int thisLeftMarginSize = this.leftMarginSize(thisOffset);
        final int thisRightMarginSize = this.rightMarginSize(thisOffset, length);
        final int thatLeftMarginSize = that.leftMarginSize(thatOffset);
        final int thatRightMarginSize = that.rightMarginSize(thatOffset, length);
        
        final long thisOriginalFirstWord = this.getWord(thisFirstWordIndex);
        final long thisOriginalLastWord = this.getWord(thisLastWordIndex);
        
        long arg;
        int thatShift = this.lastWordBitIndex(thisOffset, length) - that.lastWordBitIndex(thatOffset, length);
        for (int thisWordCursor = thisLastWordIndex, thatWordCursor = thatLastWordIndex;
                thisWordCursor >= thisFirstWordIndex;
                thisWordCursor--, thatWordCursor--) {
            
            if (thatWordCursor < thatFirstWordIndex) {
                // This situation occurs if thisWord spans across two words,
                // but thatWord, before it is shifted into place,
                // is contained wholly within a single word.
                // The portion of the first thatWord that is shifted into
                // the next word, needs to be shifted into place.
                assert thisWordCursor == thisFirstWordIndex;
                thatShift = this.firstWordBitIndex(thisOffset) - that.firstWordBitIndex(thatOffset);
                thatWordCursor = thatFirstWordIndex;
            }
            
            // Shift that substring (the argument) so that it aligns up with this substring.
            arg = that.shiftWord(thatShift, ZERO_FILL, thatWordCursor,
                    thatFirstWordIndex, thatLastWordIndex,
                    thatLeftMarginSize, thatRightMarginSize);
            
            putWord(thisWordCursor, op.applyAsLong(getWord(thisWordCursor), arg));
            
            restoreMarginsIf(thisWordCursor,
                    thisOriginalFirstWord, thisOriginalLastWord,
                    thisFirstWordIndex, thisLastWordIndex,
                    thisLeftMarginSize, thisRightMarginSize);
            
        }
        
    }
    
    private boolean iEquals(int thisOffset, int length, BitString that, int thatOffset) {
        return iPredicate( (lArg, rArg) -> { return lArg == rArg; }, ONE_DFLT, ONE_FILL,
                thisOffset, length, that, thatOffset);
    }
    
    private boolean iIntersects(int thisOffset, int length, BitString that, int thatOffset) {
        return iPredicate( (lArg, rArg) -> { return (lArg & rArg) != 0; }, ZERO_DFLT, ZERO_FILL,
                thisOffset, length, that, thatOffset);
    }
    
    private boolean iPredicate(LongBiPredicate op, boolean dflt, boolean fill,
            int thisOffset, int length, BitString that, int thatOffset) {
        
        assert this.isValidOffset(thisOffset);
        assert this.isValidLength(thisOffset, length);
        assert that.isValidOffset(thatOffset);
        assert that.isValidLength(thatOffset, length);
        if (length == 0) return dflt;
        
        final int[] thisIterator = this.getIterator(thisOffset, length);
        final int[] thatIterator = that.getIterator(thatOffset, length);
        while (this.hasNextIteratorWord(thisIterator)) {
            final long thisWord = this.getNextIteratorWord(thisIterator, fill);
            final long thatWord = that.getNextIteratorWord(thatIterator, fill);
            if (op.test(thisWord, thatWord)^dflt) return !dflt;
        }        
        
        return dflt;
        
    }
    
    private BitString iReverse(int offset, int length) {
        assert isValidOffset(offset);
        assert isValidLength(offset, length);
        
        final BitString reversedBitString = newBitString(length);
        
        int bitIndex = firstBitIndex(offset);
        final int[] iterator = getIterator(offset, length);
        while (hasPreviousIteratorWord(iterator)) {
            
            final long backWordReversed = Long.reverse(getPreviousIteratorWord(iterator));
            final int nBits = getIteratorWordBitCount(iterator);
            
            reversedBitString.putWordBits(bitIndex, nBits, backWordReversed);
            
            bitIndex += nBits;
        }
        
        return reversedBitString;
    }
    
    private void iRotate(Direction direction, int distance, int offset, int length) {
        if (direction.isLeft()) {
            iRotateLeft(distance, offset, length);
        } else {
            iRotateRight(distance, offset, length);
        }
    }
    
    private void iRotate(Direction direction, int distance, int thisOffset, int thisLength, BitString that, int thatOffset, int thatLength) {
        if (direction.isLeft()) {
            iRotateLeft(distance, thisOffset, thisLength, that, thatOffset, thatLength);
        } else {
            iRotateRight(distance, thisOffset, thisLength, that, thatOffset, thatLength);
        }
    }
    
    private void iRotateLeft(int distance, int offset, int length) {
        assert isValidOffset(offset);
        assert isValidLength(offset, length);
        // assumption: -Integer.MAX_VALUE == Integer.Min_VALUE + 1
        if (distance == 0 || length == 0) return;
        if (distance < 0) {
            if (distance == Integer.MIN_VALUE) {
                iRotateRight(-(distance + length), offset, length);
            } else {
                iRotateRight(-distance, offset, length); 
            }
            return;
        }
        
        final int shift = distance % length;
        if (shift == 0) return;
        final BitString spill = new LongBitString(shift);
        this.iShiftLeft(shift, ZERO_FILL, offset, length, spill, 0, shift);
        spill.iShiftLeft(shift, ZERO_FILL, 0, shift, this, offset+length-shift, shift);
    }
    
    private void iRotateLeft(int distance, int thisOffset, int thisLength, BitString that, int thatOffset, int thatLength) {
        assert this.isValidOffset(thisOffset);
        assert this.isValidLength(thisOffset, thisLength);
        assert that.isValidOffset(thatOffset);
        assert that.isValidLength(thatOffset, thatLength);
        // assumption: -Integer.MAX_VALUE == Integer.Min_VALUE + 1 and Integer.MIN_VALUE is even
        if (distance == 0) return;
        if (thatLength == 0) {
            this.iRotateLeft(distance, thisOffset, thisLength);
            return;
        }
        if (thisLength == 0) {
            that.iRotateLeft(distance, thatOffset, thatLength);
            return;
        }
        if (distance < 0) {
            if (distance == Integer.MIN_VALUE) {
                if (distance + thisLength + thatLength <= 0) {
                    iRotateRight(-(distance + thisLength + thatLength), thisOffset, thisLength, that, thatOffset, thatLength);
                } else {
                    iRotateRight(-(distance / 2), thisOffset, thisLength, that, thatOffset, thatLength);
                    iRotateRight(-(distance / 2), thisOffset, thisLength, that, thatOffset, thatLength);
                }
            } else {
                iRotateRight(-distance, thisOffset, thisLength, that, thatOffset, thatLength);
            }
            return;
        }
        
        // determine how many total bits from this and that need to be shifted into the spill
        final int shift = (int)(distance % ((long)thisLength + thatLength));
        if (shift == 0) return;
        final BitString spill = new LongBitString(shift);
        
        // shift this and that into the spill
        int thatShift = Math.min(shift, thatLength);
        int thisShift = shift - thatShift;
        that.iShiftLeft(thatShift, ZERO_FILL, thatOffset, thatShift, spill, 0, shift);
        if (thisShift > 0) {
            this.iShiftLeft(thisShift, ZERO_FILL, thisOffset, thisShift, spill, 0, shift);
        }
        
        // shift this into that
        this.iShiftLeft(shift, ZERO_FILL, thisOffset, thisLength, that, thatOffset, thatLength);
        
        // shift spill back into this and that
        thisShift = Math.min(shift, thisLength);
        thatShift = shift - thisShift;
        if (thatShift > 0) {
            spill.iShiftLeft(thatShift, ZERO_FILL, 0, shift, that, thatOffset + thatLength - thatShift, thatShift);
        }
        spill.iShiftLeft(thisShift, ZERO_FILL, 0, shift, this, thisOffset + thisLength - thisShift, thisShift);
    }
    
    private void iRotateRight(int distance, int offset, int length) {
        assert isValidOffset(offset);
        assert isValidLength(offset, length);
        // assumption: -Integer.MAX_VALUE == Integer.Min_VALUE + 1
        if (distance == 0 || length == 0) return;
        if (distance < 0) {
            if (distance == Integer.MIN_VALUE) {
                iRotateRight(-(distance + length), offset, length);
            } else {
                iRotateLeft(-distance, offset, length); 
            }
            return;
        }
        
        final int shift = distance % length;
        if (shift == 0) return;
        final BitString spill = new LongBitString(shift);
        this.iShiftRight(shift, ZERO_FILL, offset, length, spill, 0, shift);
        spill.iShiftRight(shift, ZERO_FILL, 0, shift, this, offset, shift);
    }
    
    private void iRotateRight(int distance, int thisOffset, int thisLength, BitString that, int thatOffset, int thatLength) {
        assert this.isValidOffset(thisOffset);
        assert this.isValidLength(thisOffset, thisLength);
        assert that.isValidOffset(thatOffset);
        assert that.isValidLength(thatOffset, thatLength);
        // assumption: -Integer.MAX_VALUE == Integer.Min_VALUE + 1 and Integer.MIN_VALUE is even
        if (distance == 0) return;
        if (thatLength == 0) {
            this.iRotateRight(distance, thisOffset, thisLength);
            return;
        }
        if (thisLength == 0) {
            that.iRotateRight(distance, thatOffset, thatLength);
            return;
        }
        if (distance < 0) {
            if (distance == Integer.MIN_VALUE) {
                if (distance + thisLength + thatLength <= 0) {
                    iRotateLeft(-(distance + thisLength + thatLength), thisOffset, thisLength, that, thatOffset, thatLength);
                } else {
                    iRotateLeft(-(distance / 2), thisOffset, thisLength, that, thatOffset, thatLength);
                    iRotateLeft(-(distance / 2), thisOffset, thisLength, that, thatOffset, thatLength);
                }
            } else {
                iRotateLeft(-distance, thisOffset, thisLength, that, thatOffset, thatLength);
            }
            return;
        }
        
        // determine how many total bits from this and that need to be shifted into the spill
        final int shift = (int)(distance % ((long)thisLength + thatLength));
        if (shift == 0) return;
        final BitString spill = new LongBitString(shift);
        
        // shift this and that into the spill
        int thatShift = Math.min(shift, thatLength);
        int thisShift = shift - thatShift;
        that.iShiftRight(thatShift, ZERO_FILL, thatOffset + thatLength - thatShift, thatShift, spill, 0, shift);
        if (thisShift > 0) {
            this.iShiftRight(thisShift, ZERO_FILL, thisOffset + thisLength - thisShift, thisShift, spill, 0, shift);
        }
        
        // shift this into that
        this.iShiftRight(shift, ZERO_FILL, thisOffset, thisLength, that, thatOffset, thatLength);
        
        // shift spill back into this and that
        thisShift = Math.min(shift, thisLength);
        thatShift = shift - thisShift;
        if (thatShift > 0) {
            spill.iShiftRight(thatShift, ZERO_FILL, 0, shift, that, thatOffset, thatShift);
        }
        spill.iShiftRight(thisShift, ZERO_FILL, 0, shift, this, thisOffset, thisShift);
    }
    
    private void iShift(Direction direction, int distance, boolean fill, int offset, int length) {
        if (direction.isLeft()) {
            iShiftLeft(distance, fill, offset, length);
        } else {
            iShiftRight(distance, fill, offset, length);
        }
    }
    
    private void iShift(Direction direction, int distance, boolean fill,
            int thisOffset, int thisLength,
            BitString that, int thatOffset, int thatLength) {
        if (direction.isLeft()) {
            iShiftLeft(distance, fill, thisOffset, thisLength, that, thatOffset, thatLength);
        } else {
            iShiftRight(distance, fill, thisOffset, thisLength, that, thatOffset, thatLength);
        }
    }
    
    private void iShiftLeft(int distance, boolean fill, int offset, int length) {
        assert isValidOffset(offset);
        assert isValidLength(offset, length);
        // assumption: -Integer.MAX_VALUE == Integer.Min_VALUE + 1
        if (distance == 0 || length == 0) return;
        if (distance < 0) {
            if (distance == Integer.MIN_VALUE) {
                iShiftRight(Integer.MAX_VALUE, fill, offset, length);
            } else {
                iShiftRight(-distance, fill, offset, length); 
            }
            return;
        }
        
        if (distance < length) iCopy(offset, length - distance, this, offset + distance);
        else distance = length;
        iOp(UnaryOp.set(fill), offset - distance + length, distance);
    }
    
    private void iShiftLeft(int distance, boolean fill,
            int thisOffset, int thisLength,
            BitString that, int thatOffset, int thatLength) {
        assert this.isValidOffset(thisOffset);
        assert this.isValidLength(thisOffset, thisLength);
        assert that.isValidOffset(thatOffset);
        assert that.isValidLength(thatOffset, thatLength);
        // assumption: -Integer.MAX_VALUE == Integer.Min_VALUE + 1 and Integer.MIN_VALUE is even
        if (distance == 0) return;
        if (thatLength == 0) {
            this.iShiftLeft(distance, fill, thisOffset, thisLength);
            return;
        }
        if (thisLength == 0) {
            that.iShiftLeft(distance, fill, thatOffset, thatLength);
            return;
        }
        if (distance < 0) {
            if (distance == Integer.MIN_VALUE) {
                if (distance + thisLength + thatLength < 0) {
                    iShiftRight(Integer.MAX_VALUE, fill, thisOffset, thisLength, that, thatOffset, thatLength);
                } else {
                    iShiftRight(-(distance / 2), fill, thisOffset, thisLength, that, thatOffset, thatLength);
                    iShiftRight(-(distance / 2), fill, thisOffset, thisLength, that, thatOffset, thatLength);
                }
            } else {
                iShiftRight(-distance, fill, thisOffset, thisLength, that, thatOffset, thatLength); 
            }
            return;
        }
        
        that.iShiftLeft(distance, fill, thatOffset, thatLength);
        
        // Copy bits from the front of this BitString into that BitString
        // Insert the copied bits after the original last bit, of that BitString,
        // which has been shifted left thatNBits
        final int thisNBits = Math.min(distance, thisLength);
        final int thatNBits = Math.min(distance, thatLength);
        final int copyLength = Math.min(thisNBits, thatNBits);
        if (distance < thisLength + thatLength) {
            that.iCopy(thatOffset - thatNBits + thatLength, copyLength, this, thisOffset - copyLength + thisNBits);
        }
        
        this.iShiftLeft(distance, fill, thisOffset, thisLength);
    }
    
    private void iShiftRight(int distance, boolean fill, int offset, int length) {
        assert isValidOffset(offset);
        assert isValidLength(offset, length);
        // assumption: -Integer.MAX_VALUE == Integer.Min_VALUE + 1
        if (distance == 0 || length == 0) return;
        if (distance < 0) {
            if (distance == Integer.MIN_VALUE) {
                iShiftLeft(Integer.MAX_VALUE, fill, offset, length);
            } else {
                iShiftLeft(-distance, fill, offset, length); 
            }
            return;
        }
        
        if (distance < length) iCopyRTL(offset + distance, length - distance, this, offset);
        else distance = length;
        iOp(UnaryOp.set(fill), offset, distance);
    }
    
    private void iShiftRight(int distance, boolean fill,
            int thisOffset, int thisLength,
            BitString that, int thatOffset, int thatLength) {
        assert this.isValidOffset(thisOffset);
        assert this.isValidLength(thisOffset, thisLength);
        assert that.isValidOffset(thatOffset);
        assert that.isValidLength(thatOffset, thatLength);
        // assumption: -Integer.MAX_VALUE == Integer.Min_VALUE + 1 and Integer.MIN_VALUE is even
        if (distance == 0) return;
        if (thatLength == 0) {
            this.iShiftRight(distance, fill, thisOffset, thisLength);
            return;
        }
        if (thisLength == 0) {
            that.iShiftRight(distance, fill, thatOffset, thatLength);
            return;
        }
        if (distance < 0) {
            if (distance == Integer.MIN_VALUE) {
                if (distance + thisLength + thatLength < 0) {
                    iShiftLeft(Integer.MAX_VALUE, fill, thisOffset, thisLength, that, thatOffset, thatLength);
                } else {
                    iShiftLeft(-(distance / 2), fill, thisOffset, thisLength, that, thatOffset, thatLength);
                    iShiftLeft(-(distance / 2), fill, thisOffset, thisLength, that, thatOffset, thatLength);
                }
            } else {
                iShiftLeft(-distance, fill, thisOffset, thisLength, that, thatOffset, thatLength); 
            }
            return;
        }
        
        that.iShiftRight(distance, fill, thatOffset, thatLength);
        
        // Copy bits from the end of this BitString into that BitString.
        // Insert the copied bits before the original first bit, of that BitString,
        // which has been shifted right thatNBits.
        final int thisNBits = Math.min(distance, thisLength);
        final int thatNBits = Math.min(distance, thatLength);
        final int copyLength = Math.min(thisNBits, thatNBits);
        if (distance < thisLength + thatLength) {
            that.iCopyRTL(thatOffset - copyLength + thatNBits, copyLength, this, thisOffset - thisNBits + thisLength);
        }
        
        this.iShiftRight(distance, fill, thisOffset, thisLength);
    }
    
    private long iGetPrimitiveWord(int offset, int primitiveSize) {
        assert isValidOffset(offset);
        assert primitiveSize <= BITS_PER_WORD;
        final int bitIndex = bitIndex(offset);
        final long word = shiftWordLeft(wordBitIndex(bitIndex), ZERO_FILL, wordIndex(bitIndex),
                lastWordIndex(), rightMarginSize());
        return word >>> (BITS_PER_WORD - primitiveSize);
    }
    
    void iPutPrimitiveWord(int offset, int primitiveSize, long primitiveWord) {
        assert isValidOffset(offset);
        assert primitiveSize <= BITS_PER_WORD;
        assert spaceIsAvailable(length() - offset, primitiveSize);
        putWordBits(bitIndex(offset), primitiveSize, primitiveWord << (BITS_PER_WORD - primitiveSize));
    }
    
    void iPutPrimitiveArray(int offset, Primitive[] primitives) {
        assert isValidOffset(offset);
        assert spaceIsAvailable(length() - offset, Primitive.arrayCumulativeSize(primitives));
        for (int index = 0; index < primitives.length; index++) {
            final int primitiveSize = primitives[index].type().size();
            iPutPrimitiveWord(offset, primitiveSize, primitives[index].longValue());
            offset += primitiveSize;
        }
    }
    
    private boolean[] iToBooleanArray(int offset, int length) {
        assert isValidOffset(offset);
        assert isValidLength(offset, length);
        if (length == 0) return new boolean[0];
        final boolean[] booleans = new boolean[length];
        iLoadPrimitiveArray(offset, length, 1,
                (index, word) -> { booleans[index] = (word == 0) ? false : true; });
        return booleans;
    }
    
    private byte[] iToByteArray(int offset, int length) {
        assert isValidOffset(offset);
        assert isValidLength(offset, length);
        if (length == 0) return new byte[0];
        final byte[] bytes = new byte[(length - 1) / Byte.SIZE + 1];
        iLoadPrimitiveArray(offset, length, Byte.SIZE,
                (index, word) -> { bytes[index] = (byte)word; });
        return bytes;
    }
    
    private char[] iToCharArray(int offset, int length) {
        assert isValidOffset(offset);
        assert isValidLength(offset, length);
        if (length == 0) return new char[0];
        final char[] chars = new char[(length - 1) / Character.SIZE + 1];
        iLoadPrimitiveArray(offset, length, Character.SIZE,
                (index, word) -> { chars[index] = (char)word; });
        return chars;
    }
    
    private double[] iToDoubleArray(int offset, int length) {
        assert isValidOffset(offset);
        assert isValidLength(offset, length);
        if (length == 0) return new double[0];
        final double[] doubles = new double[(length - 1) / Long.SIZE + 1];
        iLoadPrimitiveArray(offset, length, Long.SIZE,
                (index, word) -> { doubles[index] = Double.longBitsToDouble(word); });
        return doubles;
    }
    
    private float[] iToFloatArray(int offset, int length) {
        assert isValidOffset(offset);
        assert isValidLength(offset, length);
        if (length == 0) return new float[0];
        final float[] floats = new float[(length - 1) / Integer.SIZE + 1];
        iLoadPrimitiveArray(offset, length, Integer.SIZE,
                (index, word) -> { floats[index] = Float.intBitsToFloat((int)word); });
        return floats;
    }
    
    private int[] iToIntArray(int offset, int length) {
        assert isValidOffset(offset);
        assert isValidLength(offset, length);
        if (length == 0) return new int[0];
        final int[] ints = new int[(length - 1) / Integer.SIZE + 1];
        iLoadPrimitiveArray(offset, length, Integer.SIZE,
                (index, word) -> { ints[index] = (int)word; });
        return ints;
    }
    
    private long[] iToLongArray(int offset, int length) {
        assert isValidOffset(offset);
        assert isValidLength(offset, length);
        if (length == 0) return new long[0];
        final long[] longs = new long[(length - 1) / Long.SIZE + 1];
        iLoadPrimitiveArray(offset, length, Long.SIZE,
                (index, word) -> { longs[index] = word; });
        return longs;
    }
    
    private short[] iToShortArray(int offset, int length) {
        assert isValidOffset(offset);
        assert isValidLength(offset, length);
        if (length == 0) return new short[0];
        final short[] shorts = new short[(length - 1) / Short.SIZE + 1];
        iLoadPrimitiveArray(offset, length, Short.SIZE,
                (index, word) -> { shorts[index] = (short)word; });
        return shorts;
    }
    
    private Primitive[] iToPrimitiveArray(Primitive.Type type, int offset, int length) {
        assert isValidOffset(offset);
        assert isValidLength(offset, length);
        if (length == 0) return new Primitive[0];
        final Primitive[] primitives = new Primitive[(length - 1) / type.size() + 1];
        iLoadPrimitiveArray(offset, length, type.size(),
                (index, word) -> { primitives[index] = new Primitive(type, word); });
        return primitives;
    }
    
    private Primitive[] iToPrimitiveArray(Primitive.Type[] types, int offset, int length) {
        assert isValidOffset(offset);
        assert isValidLength(offset, length);
        if (length == 0) return new Primitive[0];
        final Primitive[] primitives = new Primitive[types.length];
        int index = 0;
        for (Primitive.Type type: types) {
            primitives[index++] = new Primitive(type, iGetPrimitiveWord(offset, type.size()));
            offset += type.size();
        }
        return primitives;
    }
    
    private Primitive[] iToPrimitiveArray(int offset, int length) {
        assert isValidOffset(offset);
        assert isValidLength(offset, length);
        if (length == 0) return new Primitive[0];
        int remainingLength = length;
        int primitiveCount = 0;
        final Primitive.Type[] primitiveTypes = {
                Primitive.Type.LONG,
                Primitive.Type.INT,
                Primitive.Type.SHORT,
                Primitive.Type.BYTE,
                Primitive.Type.BOOLEAN
                };
        for (Primitive.Type primitiveType: primitiveTypes) {
            if (remainingLength <= 0) break;
            final int primitiveSize = primitiveType.size();
            final int count = remainingLength / primitiveSize;
            primitiveCount += count;
            remainingLength -= count * primitiveSize;
        }
        remainingLength = length;
        final Primitive[] primitives = new Primitive[primitiveCount];
        for (Primitive.Type primitiveType: primitiveTypes) {
            if (remainingLength <= 0) break;
            final int primitiveSize = primitiveType.size();
            final int count = remainingLength / primitiveSize;
            if (count > 0) {
                final int substringLength = count * primitiveSize;
                iLoadPrimitiveArray(offset, substringLength, primitiveSize,
                        (index, word) -> { primitives[index] = new Primitive(primitiveType, word); });
                remainingLength -= substringLength;
                offset += substringLength;
            }
        }
        return primitives;
    }
    
    private void iLoadPrimitiveArray(int offset, int length,
            int primitiveSize,
            IntLongConsumer setPrimitiveArrayElementFromUnsignedLong) {
        assert isValidOffset(offset);
        assert isValidLength(offset, length);
        final int primitivesPerWord = BITS_PER_WORD / primitiveSize;
        final long primitiveMask = WORD_MASK >>> (BITS_PER_WORD - primitiveSize);
        int index = 0;
        final int[] iterator = getIterator(offset, length);
        while (length > 0) {
            final long word = getNextIteratorWord(iterator);
            for (int p = primitivesPerWord - 1; p >= 0 && length > 0; p--, length -= primitiveSize) {
                setPrimitiveArrayElementFromUnsignedLong.accept(index++, (word >>> (p * primitiveSize)) & primitiveMask);
            }
        }
    }
    
    private int iNumberOf(boolean bit, int offset, int length) {
        assert isValidOffset(offset);
        assert isValidLength(offset, length);
        int bitCount = 0;
        final int[] iterator = getIterator(offset, length);
        while (hasNextIteratorWord(iterator)) {
            bitCount += Long.bitCount(getNextIteratorWord(iterator));
        }
        return bit ? bitCount : length - bitCount;
    }
    
    private int iNumberOf(Position position, boolean bit, int offset, int length) {
        if (position.isLeading()) {
            return bit ? iNumberOfLeadingOnes(offset, length)
                       : iNumberOfLeadingZeros(offset, length);
        } else {
            return bit ? iNumberOfTrailingOnes(offset, length)
                       : iNumberOfTrailingZeros(offset, length);
        }
    }
    
    /**
     * Returns the number of leading {@code ONES} of the specified substring of this
     * {@code BitString}.
     * 
     * @param offset the start of the substring
     * @param length the length of the substring
     * @return the number of leading {@code ONES} of the substring
     */
    private int iNumberOfLeadingOnes(int offset, int length) {
        return iNumberOf(offset, length,
                iterator -> { return hasNextIteratorWord(iterator); },
                iterator -> { return ~getNextIteratorWord(iterator, ZERO_FILL); },
                word -> { return Long.numberOfLeadingZeros(word); });
    }
    
    /**
     * Returns the number of leading {@code ZEROS} of the specified substring of
     * this {@code BitString}.
     * 
     * @param offset the start of the substring
     * @param length the length of the substring
     * @return the number of leading {@code ZEROS} of the substring
     */
    private int iNumberOfLeadingZeros(int offset, int length) {
        return iNumberOf(offset, length,
                iterator -> { return hasNextIteratorWord(iterator); },
                iterator -> { return getNextIteratorWord(iterator, ONE_FILL); },
                word -> { return Long.numberOfLeadingZeros(word); });
    }
    
    /**
     * Returns the number of trailing {@code ONES} of the specified substring of
     * this {@code BitString}.
     * 
     * @param offset the start of the substring
     * @param length the length of the substring
     * @return the number of trailing {@code ONES} of the substring
     */
    private int iNumberOfTrailingOnes(int offset, int length) {
        return iNumberOf(offset, length,
                iterator -> { return hasPreviousIteratorWord(iterator); },
                iterator -> { return ~getPreviousIteratorWord(iterator, ZERO_FILL); },
                word -> { return Long.numberOfTrailingZeros(word); });
    }
    
    /**
     * Returns the number of trailing {@code ZEROS} of the specified substring of
     * this {@code BitString}.
     * 
     * @param offset the start of the substring
     * @param length the length of the substring
     * @return the number of trailing {@code ZEROS} of the substring
     */
    private int iNumberOfTrailingZeros(int offset, int length) {
        return iNumberOf(offset, length,
                iterator -> { return hasPreviousIteratorWord(iterator); },
                iterator -> { return getPreviousIteratorWord(iterator, ONE_FILL); },
                word -> { return Long.numberOfTrailingZeros(word); });
    }

    /**
     * Returns the number of leading/trailing {@code ONES}/{@code ZEROS} of the specified substring of
     * this {@code BitString}.
     * 
     * @param offset the start of the substring
     * @param length the length of the substring
     * @param hasIteratorWord a predicate that indicates if there exists a next or previous word
     * @param getIteratorWord a function that returns the next or previous full word
     * @param numberOfLeadingOrTrailingZeros a function that counts the number of leading or trailing Zeros of a word
     * @return the number of leading/trailing {@code ONES}/{@code ZEROS} of the substring
     */
    private int iNumberOf(int offset, int length,
            Predicate<int[]> hasIteratorWord,
            ToLongFunction<int[]> getIteratorWord,
            LongToIntFunction numberOfLeadingOrTrailingZeros) {
        assert isValidOffset(offset);
        assert isValidLength(offset, length);
        int count = 0;
        final int[] iterator = getIterator(offset, length);
        while (hasIteratorWord.test(iterator)) {
            final long word = getIteratorWord.applyAsLong(iterator);
            if (word != 0L) {
                count += numberOfLeadingOrTrailingZeros.applyAsInt(word);
                break;
            }
            count += BITS_PER_WORD;
        }
        return count;
    }
    
    private int iOffsetOf(Position position, boolean bit, int offset, int length) {
        if (position.isFirst()) {
            return bit ? iOffsetOfFirstOne(offset, length)
                       : iOffsetOfFirstZero(offset, length);
        } else {
            return bit ? iOffsetOfLastOne(offset, length)
                       : iOffsetOfLastZero(offset, length);
        }
    }
    
    /**
     * Returns the offset of the first bit that is set to {@code ONE} that occurs
     * within the specified substring of this {@code BitString}. If no bits in the
     * substring are set to {@code ONE}, -1 is returned. The returned offset is
     * relative to the start of the substring.
     * 
     * @param offset the start of the substring
     * @param length the length of the substring
     * @return the offset of the first {@code ONE} bit or -1 if no bits are set to
     *         {@code ONE}
     */
    private int iOffsetOfFirstOne(int offset, int length) {
        assert isValidOffset(offset);
        assert isValidLength(offset, length);
        if (length == 0) return -1;
        final int count = iNumberOfLeadingZeros(offset, length);
        return (count == length) ? -1 : count;
    }
    
    /**
     * Returns the offset of the first bit that is set to {@code ZERO} that occurs
     * within the specified substring of this {@code BitString}. If no bits in the
     * substring are set to {@code ZERO}, -1 is returned. The returned offset is
     * relative to the start of the substring.
     * 
     * @param offset the start of the substring
     * @param length the length of the substring
     * @return the offset of the first {@code ZERO} bit or -1 if no bits are set to
     *         {@code ZERO}
     */
    private int iOffsetOfFirstZero(int offset, int length) {
        assert isValidOffset(offset);
        assert isValidLength(offset, length);
        if (length == 0) return -1;
        final int count = iNumberOfLeadingOnes(offset, length);
        return (count == length) ? -1 : count;
    }
    
    /**
     * Returns the offset of the last bit that is set to {@code ONE} that occurs
     * within the specified substring of this {@code BitString}. If no bits in the
     * substring are set to {@code ONE}, -1 is returned. The returned offset is
     * relative to the start of the substring.
     * 
     * @param offset the start of the substring
     * @param length the length of the substring
     * @return the offset of the last {@code ONE} bit or -1 if no bits are set to
     *         {@code ONE}
     */
    private int iOffsetOfLastOne(int offset, int length) {
        assert isValidOffset(offset);
        assert isValidLength(offset, length);
        return length - iNumberOfTrailingZeros(offset, length) - 1;
    }
    
    /**
     * Returns the offset of the last bit that is set to {@code ZERO} that occurs
     * within the specified substring of this {@code BitString}. If no bits in the
     * substring are set to {@code ZERO}, -1 is returned. The returned offset is
     * relative to the start of the substring.
     * 
     * @param offset the start of the substring
     * @param length the length of the substring
     * @return the offset of the last {@code ZERO} bit or -1 if no bits are set to
     *         {@code ZERO}
     */
    private int iOffsetOfLastZero(int offset, int length) {
        assert isValidOffset(offset);
        assert isValidLength(offset, length);
        return length - iNumberOfTrailingOnes(offset, length) - 1;
    }
    
    private int iOffsetOf(Direction direction, boolean bit, int startOffset, int offset, int length) {
        if (direction.isNext()) {
            return bit ? iOffsetOfNextOne(startOffset, offset, length)
                       : iOffsetOfNextZero(startOffset, offset, length);
        } else {
            return bit ? iOffsetOfPreviousOne(startOffset, offset, length)
                       : iOffsetOfPreviousZero(startOffset, offset, length);
        }
    }
    
    private int iOffsetOfNextOne(int startOffset, int offset, int length) {
        assert isValidOffset(offset);
        assert isValidLength(offset, length);
        assert isValidRelativeOffset(startOffset, length);
        if (length == 0 || startOffset == length - 1) return -1;
        final int bitOffset = iOffsetOfFirstOne(offset + startOffset + 1, length - startOffset - 1);
        return (bitOffset == -1) ? -1 : startOffset + 1 + bitOffset;
    }
    
    private int iOffsetOfNextZero(int startOffset, int offset, int length) {
        assert isValidOffset(offset);
        assert isValidLength(offset, length);
        assert isValidRelativeOffset(startOffset, length);
        if (length == 0 || startOffset == length - 1) return -1;
        final int bitOffset = iOffsetOfFirstZero(offset + startOffset + 1, length - startOffset - 1);
        return (bitOffset == -1) ? -1 : startOffset + 1 + bitOffset ;
    }
    
    private int iOffsetOfPreviousOne(int startOffset, int offset, int length) {
        assert isValidOffset(offset);
        assert isValidLength(offset, length);
        assert isValidRelativeOffset(startOffset, length);
        if (length == 0 || startOffset == 0) return -1;
        return iOffsetOfLastOne(offset, startOffset);
    }
    
    private int iOffsetOfPreviousZero(int startOffset, int offset, int length) {
        assert isValidOffset(offset);
        assert isValidLength(offset, length);
        assert isValidRelativeOffset(startOffset, length);
        if (length == 0 || startOffset == 0) return -1;
        return iOffsetOfLastZero(offset, startOffset);
    }
    
    /**
     * Append the specified BitString to the end of this BitString.
     * 
     * @param that the BitString to be appended
     * @return this BitString
     * @throws UnsupportedOperationException if the length of this base BitString,
     *                                       after the append operation, would
     *                                       exceed the maximum allowed length of
     *                                       Integer.MAXSIZE
     */
    public BitString append(BitString that) {
        final int thatLength = that.length();
        checkBaseLengthIncrease(thatLength);
        iAppend(that, 0, thatLength);
        return this;
    }
    
    /**
     * Append a substring of the specified BitString to the end of this BitString.
     * 
     * The substring starts at offset 'thatOffset' of this BitString and has a
     * length of 'thatLength'.
     * 
     * @param that the BitString to be appended
     * @param thatOffset the start of the substring to be appended
     * @param thatLength the length of the substring to be appended
     * @return this BitString
     * @throws UnsupportedOperationException if the length of this base BitString,
     *                                       after the append operation, would
     *                                       exceed the maximum allowed length of
     *                                       Integer.MAXSIZE
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thatOffset < 0 || thatOffset > 0 && thatOffset >= that.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thatLength < 0 || thatLength > that.length() - thatOffset}
     */
    public BitString append(BitString that, int thatOffset, int thatLength) {
        that.checkArgOffset(thatOffset);
        that.checkArgLength(thatOffset, thatLength);
        checkBaseLengthIncrease(thatLength);
        iAppend(that, thatOffset, thatLength);
        return this;
    }
    
    /**
     * Append a Field of the specified BitString to the end of this BitString.
     * 
     * @param that      the BitString to be appended
     * @param thatField the Field of that BitString to be appended
     * @return this BitString
     * @throws UnsupportedOperationException   if the length of this base BitString,
     *                                         after the append operation, would
     *                                         exceed the maximum allowed length of
     *                                         Integer.MAXSIZE
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thatField.offset() > 0 && thatField.offset() >= that.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thatField.length() > that.length() - thatField.offset()}
     */
    public BitString append(BitString that, Field thatField) {
        return append(that, thatField.offset(), thatField.length(that));
    }
    
    /**
     * Delete this BitString.
     * 
     * @return this BitString
     */
    public BitString delete() {
        final int length = length();
        iDelete(firstBitIndex(), length);
        return this;
    }
    
    /**
     * Delete a substring of this BitString.
     * 
     * The substring starts at offset 'offset' of this BitString and has a
     * length of 'length'.
     * 
     * @param offset the start of the substring to be deleted
     * @param length the length of the substring to be deleted
     * @return this BitString
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     * @throws IllegalArgumentException        if
     *                                         {@code length < 0 || length > length() - offset}
     */
    public BitString delete(int offset, int length) {
        checkThisOffset(offset);
        checkThisLength(offset, length);
        iDelete(firstBitIndex(offset), length);
        return this;
    }
    
    /**
     * Delete a Field of this BitString.
     * 
     * @param field the Field of this BitString to be deleted
     * @return this BitString
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code field.offset() > 0 && field.offset() >= length()}
     * @throws IllegalArgumentException        if
     *                                         {@code field.length() > length() - field.offset()}
     */
    public BitString delete(Field field) {
        return delete(field.offset(), field.length(this));
    }
    
    /**
     * Insert the specified BitString at the specified position in this BitString.
     * 
     * @param position the position in this BitString where the BitString is
     *                 inserted
     * @param that     the BitString to be inserted
     * @return this BitString
     * @throws UnsupportedOperationException   if the length of this base BitString,
     *                                         after the insert operation, would
     *                                         exceed the maximum allowed length of
     *                                         Integer.MAXSIZE
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code position < 0 || position > length()}
     */
    public BitString insert(int position, BitString that) {
        checkThisPosition(position);
        final int thatLength = that.length();
        checkBaseLengthIncrease(thatLength);
        iInsert(bitIndex(position), that, 0, thatLength);
        return this;
    }
    
    /**
     * Insert a substring of the specified BitString, at the specified position, in this BitString.
     * 
     * The substring starts at offset 'thatOffset' of this BitString and has a
     * length of 'thatLength'.
     * 
     * @param position the insert position
     * @param that the BitString to be inserted
     * @param thatOffset the start of the substring to be inserted
     * @param thatLength the length of the substring to be inserted
     * @return this BitString
     * @throws UnsupportedOperationException if the length of this base BitString,
     *                                       after the insert operation, would
     *                                       exceed the maximum allowed length of
     *                                       Integer.MAXSIZE
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code position < 0 || position > length()}
     *                                         or
     *                                         {@code thatOffset < 0 || thatOffset > 0 && thatOffset >= that.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thatLength < 0 || thatLength > that.length() - thatOffset}
     */
    public BitString insert(int position, BitString that, int thatOffset, int thatLength) {
        checkThisPosition(position);
        that.checkArgOffset(thatOffset);
        that.checkArgLength(thatOffset, thatLength);
        checkBaseLengthIncrease(thatLength);
        iInsert(bitIndex(position), that, thatOffset, thatLength);
        return this;
    }
    
    /**
     * Insert a Field of the specified BitString, at the specified position, in this BitString.
     * 
     * @param position the insert position
     * @param that      the BitString to be inserted
     * @param thatField the Field of that BitString to be inserted
     * @return this BitString
     * @throws UnsupportedOperationException   if the length of this base BitString,
     *                                         after the insert operation, would
     *                                         exceed the maximum allowed length of
     *                                         Integer.MAXSIZE
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code position < 0 || position > length()}
     *                                         or
     *                                         {@code thatField.offset() > 0 && thatField.offset() >= that.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thatField.length() > that.length() - thatField.offset()}
     */
    public BitString insert(int position, BitString that, Field thatField) {
        return insert(position, that, thatField.offset(), thatField.length(that));
    }
    
    /**
     * Replace this BitString with the specified BitString.
     * 
     * @param that the BitString to replace this BitString
     * @return this BitString
     * @throws UnsupportedOperationException if the length of this base BitString,
     *                                       after the replace operation, would
     *                                       exceed the maximum allowed length of
     *                                       Integer.MAXSIZE
     */
    public BitString replace(BitString that) {
        final int thisLength = this.length();
        final int thatLength = that.length();
        checkBaseLengthIncrease(thatLength - thisLength);
        iReplace(firstBitIndex(), thisLength, that, 0, thatLength);
        return this;
    }
    
    /**
     * Replace this BitString with a substring of the specified BitString.
     * 
     * The substring starts at offset 'thatOffset' of this BitString and has a
     * length of 'thatLength'.
     * 
     * @param thisOffset the start of this substring
     * @param thisLength the length of this substring
     * @param that the BitString to replace this BitString
     * @param thatOffset the start of the substring that replaces this BitString
     * @param thatLength the length of the substring that replaces this BitString
     * @return this BitString
     * @throws UnsupportedOperationException if the length of this base BitString,
     *                                       after the replace operation, would
     *                                       exceed the maximum allowed length of
     *                                       Integer.MAXSIZE
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thatOffset < 0 || thatOffset > 0 && thatOffset >= that.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thatLength < 0 || thatLength > that.length() - thatOffset}
     */
    public BitString replace(int thisOffset, int thisLength, BitString that, int thatOffset, int thatLength) {
        checkThisOffset(thisOffset);
        checkThisLength(thisOffset, thisLength);
        that.checkArgOffset(thatOffset);
        that.checkArgLength(thatOffset, thatLength);
        checkBaseLengthIncrease(thatLength - thisLength);
        iReplace(firstBitIndex(thisOffset), thisLength, that, thatOffset, thatLength);
        return this;
    }
    
    /**
     * Replace this BitString with a Field of the specified BitString.
     * 
     * @param thisField a Field of this Bitstring
     * @param that      the BitString to replace this BitString
     * @param thatField the Field of that BitString that replaces this BitString
     * @return this BitString
     * @throws UnsupportedOperationException   if the length of this base BitString,
     *                                         after the replace operation, would
     *                                         exceed the maximum allowed length of
     *                                         Integer.MAXSIZE
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thatField.offset() > 0 && thatField.offset() >= that.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thatField.length() > that.length() - thatField.offset()}
     */
    public BitString replace(Field thisField, BitString that, Field thatField) {
        return replace(thisField.offset(), thisField.length(this), that, thatField.offset(), thatField.length(that));
    }
    
    /**
     * Returns all of the bits in this {@code BitString} as a new BitString.
     * 
     * @return a copy of this {@code BitString}
     */
    public BitString get() {
        return get(0, length());
    }
    
    /**
     * Returns a substring of this {@code BitString} as a new BitString.
     * 
     * This substring starts at offset 'offset' of this {@code BitString} and
     * extends to the end of this {@code BitString}.
     *
     * @param offset the start of this substring
     * @return a substring of this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     */    
    public BitString get(int offset) {
        checkThisOffset(offset);
        return get(offset, this.length() - offset);
    }
    
    /**
     * Returns a substring of this {@code BitString} as a new BitString.
     *
     * The substring starts at offset 'offset' of this {@code BitString} and has a
     * length of 'length'.
     * 
     * @param offset the start of this substring
     * @param length the length of this substring
     * @return a substring of this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     * @throws IllegalArgumentException        if
     *                                         {@code length < 0 || length > length() - offset}
     */    
    public BitString get(int offset, int length) {
        checkThisOffset(offset);
        checkThisLength(offset, length);
        final BitString substring = newBitString(length);
        substring.iCopy(0, length, this, offset);
        return substring;
    }
    
    /**
     * Returns a Field of this {@code BitString} as a new BitString.
     * 
     * @param field a Field of this {@code BitString}
     * @return a Field as a substring of this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code field.offset() > 0 && field.offset() >= length()}
     * @throws IllegalArgumentException        if
     *                                         {@code field.length() > length() - field.offset()}
     */
    public BitString get(Field field) {
        return get(field.offset(), field.length(this));
    }
    
    /**
     * Returns the bit at the specified offset in this {@code BitString}.
     * 
     * @param bitOffset the offset of the bit to be returned
     * @return the bit at the specified offset
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     */
    public boolean getBit(int bitOffset) {
        checkThisOffset(bitOffset);
        final int bitIndex = bitIndex(bitOffset);
        return (getWord(wordIndex(bitIndex)) & (BIT_MASK >>> wordBitIndex(bitIndex))) != 0L;
    }
    
    /**
     * Return the bit at the specified offset in a substring of this
     * {@code BitString}. The bit offset is relative to the start of the substring.
     * 
     * The substring starts at offset 'offset' of this {@code BitString} and has a
     * length of 'length'.
     * 
     * @param bitOffset the offset of the bit to be returned
     * @param offset    the start of this substring
     * @param length    the length of this substring
     * @return the bit at the specified offset
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     * @throws IllegalArgumentException        if
     *                                         {@code length < 0 || length > length() - offset}
     */
    public boolean getBit(int bitOffset, int offset, int length) {
        checkThisOffset(offset);
        checkThisLength(offset, length);
        checkRelativeOffset(bitOffset, length);
        return getBit(offset + bitOffset);
    }
    
    /**
     * Returns the bit at the specified offset in a Field of this
     * {@code BitString}. The bit offset is relative to the start of the field.
     * 
     * @param bitOffset the offset of the bit to be returned
     * @param field a Field of this {@code BitString}
     * @return the bit at the specified offset
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code field.offset() > 0 && field.offset() >= length()}
     * @throws IllegalArgumentException        if
     *                                         {@code field.length() > length() - field.offset()}
     */
    public boolean getBit(int bitOffset, Field field) {
        return getBit(bitOffset, field.offset(), field.length(this));
    }
    
    /**
     * Returns the boolean primitive at the specified offset.
     * 
     * @param offset the offset of the boolean primitive
     * @return a boolean primitive
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     */
    public boolean getBoolean(int offset) {
        return getBit(offset);
    }
    
    /**
     * Returns an array of boolean primitives at the specified offset.
     * 
     * @param offset the offset of the array of booleans
     * @param count  the length of the array of booleans
     * @return an array of boolean primitives
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     * @throws UnsupportedOperationException   if
     *                                         {@code offset + count > length()}
     * @throws java.lang.OutOfMemoryError      Requested array size exceeds VM limit
     */
    public boolean[] getBooleanArray(int offset, int count) {
        checkThisOffset(offset);
        checkAvailableSpace(offset, length() - offset, (long)count, count);
        return iToBooleanArray(offset, count);
    }
    
    /**
     * Returns the byte primitive at the specified offset.
     * 
     * @param offset the offset of the boolean primitive
     * @return a byte primitive
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     * @throws UnsupportedOperationException   if
     *                                         {@code offset + Byte.SIZE > length()}
     */
    public byte getByte(int offset) {
        checkThisOffset(offset);
        checkAvailableSpace(offset, length() - offset, Byte.SIZE);
        return (byte)(iGetPrimitiveWord(offset, Byte.SIZE));
    }
    
    /**
     * Returns an array of byte primitives at the specified offset.
     * 
     * @param offset the offset of the array of bytes
     * @param count  the length of the array of bytes
     * @return an array of byte primitives
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     * @throws UnsupportedOperationException   if
     *                                         {@code offset + count * Byte.SIZE > length()}
     */
    public byte[] getByteArray(int offset, int count) {
        checkThisOffset(offset);
        checkAvailableSpace(offset, length() - offset, count * (long)Byte.SIZE, count);
        return iToByteArray(offset, count * Byte.SIZE);
    }
    
    /**
     * Returns the character primitive at the specified offset.
     * 
     * @param offset the offset of the character primitive
     * @return a character primitive
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     * @throws UnsupportedOperationException   if
     *                                         {@code offset + CHaracter.SIZE > length()}
     */
    public char getChar(int offset) {
        checkThisOffset(offset);
        checkAvailableSpace(offset, length() - offset, Character.SIZE);
        return (char)(iGetPrimitiveWord(offset, Character.SIZE));
    }
    
    /**
     * Returns an array of character primitives at the specified offset.
     * 
     * @param offset the offset of the array of characters
     * @param count  the length of the array of characters
     * @return an array of character primitives
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     * @throws UnsupportedOperationException   if
     *                                         {@code offset + count * Character.SIZE > length()}
     */
    public char[] getCharArray(int offset, int count) {
        checkThisOffset(offset);
        checkAvailableSpace(offset, length() - offset, count * (long)Character.SIZE, count);
        return iToCharArray(offset, count * Character.SIZE);
    }
    
    /**
     * Returns the double primitive at the specified offset.
     * 
     * @param offset the offset of the double primitive
     * @return a double primitive
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     * @throws UnsupportedOperationException   if
     *                                         {@code offset + Double.SIZE > length()}
     */
    public double getDouble(int offset) {
        checkThisOffset(offset);
        checkAvailableSpace(offset, length() - offset, Long.SIZE);
        return Double.longBitsToDouble((long)(iGetPrimitiveWord(offset, Long.SIZE)));
    }
    
    /**
     * Returns an array of double primitives at the specified offset.
     * 
     * @param offset the offset of the array of doubles
     * @param count  the length of the array of doubles
     * @return an array of double primitives
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     * @throws UnsupportedOperationException   if
     *                                         {@code offset + count * Double.SIZE > length()}
     */
    public double[] getDoubleArray(int offset, int count) {
        checkThisOffset(offset);
        checkAvailableSpace(offset, length() - offset, count * (long)Long.SIZE, count);
        return iToDoubleArray(offset, count * Long.SIZE);
    }
    
    /**
     * Returns the float primitive at the specified offset.
     * 
     * @param offset the offset of the float primitive
     * @return a float primitive
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     * @throws UnsupportedOperationException   if
     *                                         {@code offset + Float.SIZE > length()}
     */
    public float getFloat(int offset) {
        checkThisOffset(offset);
        checkAvailableSpace(offset, length() - offset, Integer.SIZE);
        return Float.intBitsToFloat((int)(iGetPrimitiveWord(offset, Integer.SIZE)));
    }
    
    /**
     * Returns an array of float primitives at the specified offset.
     * 
     * @param offset the offset of the array of floats
     * @param count  the length of the array of floats
     * @return an array of float primitives
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     * @throws UnsupportedOperationException   if
     *                                         {@code offset + count * Float.SIZE > length()}
     */
    public float[] getFloatArray(int offset, int count) {
        checkThisOffset(offset);
        checkAvailableSpace(offset, length() - offset, count * (long)Integer.SIZE, count);
        return iToFloatArray(offset, count * Integer.SIZE);
    }
    
    /**
     * Returns the integer primitive at the specified offset.
     * 
     * @param offset the offset of the integer primitive
     * @return an integer primitive
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     * @throws UnsupportedOperationException   if
     *                                         {@code offset + Integer.SIZE > length()}
     */
    public int getInt(int offset) {
        checkThisOffset(offset);
        checkAvailableSpace(offset, length() - offset, Integer.SIZE);
        return (int)(iGetPrimitiveWord(offset, Integer.SIZE));
    }
    
    /**
     * Returns an array of integer primitives at the specified offset.
     * 
     * @param offset the offset of the array of integers
     * @param count  the length of the array of integers
     * @return an array of integer primitives
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     * @throws UnsupportedOperationException   if
     *                                         {@code offset + count * Integer.SIZE > length()}
     */
    public int[] getIntArray(int offset, int count) {
        checkThisOffset(offset);
        checkAvailableSpace(offset, length() - offset, count * (long)Integer.SIZE, count);
        return iToIntArray(offset, count * Integer.SIZE);
    }
    
    /**
     * Returns the long primitive at the specified offset.
     * 
     * @param offset the offset of the long primitive
     * @return a long primitive
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     * @throws UnsupportedOperationException   if
     *                                         {@code offset + Long.SIZE > length()}
     */
    public long getLong(int offset) {
        checkThisOffset(offset);
        checkAvailableSpace(offset, length() - offset, Long.SIZE);
        return (long)(iGetPrimitiveWord(offset, Long.SIZE));
    }
    
    /**
     * Returns an array of long primitives at the specified offset.
     * 
     * @param offset the offset of the array of longs
     * @param count  the length of the array of longs
     * @return an array of long primitives
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     * @throws UnsupportedOperationException   if
     *                                         {@code offset + count * Long.SIZE > length()}
     */
    public long[] getLongArray(int offset, int count) {
        checkThisOffset(offset);
        checkAvailableSpace(offset, length() - offset, count * (long)Long.SIZE, count);
        return iToLongArray(offset, count * Long.SIZE);
    }
    
    /**
     * Returns the short primitive at the specified offset.
     * 
     * @param offset the offset of the short primitive
     * @return a short primitive
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     * @throws UnsupportedOperationException   if
     *                                         {@code offset + Short.SIZE > length()}
     */
    public short getShort(int offset) {
        checkThisOffset(offset);
        checkAvailableSpace(offset, length() - offset, Short.SIZE);
        return (short)(iGetPrimitiveWord(offset, Short.SIZE));
    }
    
    /**
     * Returns an array of short primitives at the specified offset.
     * 
     * @param offset the offset of the array of shorts
     * @param count  the length of the array of shorts
     * @return an array of short primitives
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     * @throws UnsupportedOperationException   if
     *                                         {@code offset + count * Short.SIZE > length()}
     */
    public short[] getShortArray(int offset, int count) {
        checkThisOffset(offset);
        checkAvailableSpace(offset, length() - offset, count * (long)Short.SIZE, count);
        return iToShortArray(offset, count * Short.SIZE);
    }
    
    /**
     * Returns a Primitive of the specified type at the specified offset.
     * 
     * @param type   the type of primitive to return
     * @param offset the offset of the Primitive
     * @return a Primitive of the specified type
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     * @throws UnsupportedOperationException   if
     *                                         {@code offset + type.size() > length()}
     */
    public Primitive getPrimitive(Primitive.Type type, int offset) {
        checkThisOffset(offset);
        checkAvailableSpace(offset, length() - offset, type.size());
        return new Primitive(type, iGetPrimitiveWord(offset, type.size()));
    }
    
    /**
     * Returns a Primitive of the specified type from the specified Field.
     * 
     * @param type  the type of primitive to return
     * @param field a Field of this {@code BitString}
     * @return a Primitive of the specified type
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code field.offset() > 0 && field.offset() >= length()}
     * @throws IllegalArgumentException        if
     *                                         {@code field.length() > length() - field.offset()}
     * @throws UnsupportedOperationException   if
     *                                         {@code field.offset() + type.size() > field.length()}
     */
    public Primitive getPrimitive(Primitive.Type type, Field field) {
        final int offset = field.offset();
        final int length = field.length(this);
        checkThisOffset(offset);
        checkThisLength(offset, length);
        checkAvailableSpace(offset, length, type.size());
        return new Primitive(type, iGetPrimitiveWord(offset, type.size()));
    }
    
    /**
     * Returns a Primitive array of the specified type and specified count at
     * the specified offset.
     * 
     * @param type   the type of Primitive array to return
     * @param offset the offset of the Primitive array
     * @param count  the number of array elements to return
     * @return a Primitive array of the specified type
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset > 0 && offset >= length()}
     * @throws UnsupportedOperationException   if
     *                                         {@code offset + count * type.size() > length()}
     */
    public Primitive[] getPrimitiveArray(Primitive.Type type, int offset, int count) {
        checkThisOffset(offset);
        checkAvailableSpace(offset, length() - offset, count * (long)type.size(), count);
        return iToPrimitiveArray(type, offset, count);
    }
    
    /**
     * Returns a Primitive array of the specified type and specified count from the
     * specified Field.
     * 
     * @param type  the type of Primitive array to return
     * @param field a Field of this {@code BitString}
     * @param count the number of array elements to return
     * @return a Primitive array of the specified type
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code field.offset() > 0 && field.offset() >= length()}
     * @throws IllegalArgumentException        if
     *                                         {@code field.length() > length() - field.offset()}
     * @throws UnsupportedOperationException   if
     *                                         {@code field.offset() + count * type.size() > field.length()}
     */
    public Primitive[] getPrimitiveArray(Primitive.Type type, Field field, int count) {
        final int offset = field.offset();
        final int length = field.length(this);
        checkThisOffset(offset);
        checkThisLength(offset, length);
        checkAvailableSpace(offset, length, count * (long)type.size(), count);
        return iToPrimitiveArray(type, offset, count);
    }
    
    /**
     * Returns a Primitive array, at the specified offset, whose primitive elements
     * are of the types specified in the types argument. The length of the returned
     * Primitive array is equal to the length of the specified array of Primitive
     * types.
     * 
     * @param types  an array of Primitive Types
     * @param offset the offset of the Primitive array
     * @return a Primitive array of the specified types
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset > 0 && offset >= length()}
     * @throws UnsupportedOperationException   if
     *                                         {@code offset + Primitive.arrayCumulativeSize(types) > length()}
     */
    public Primitive[] getPrimitiveArray(Primitive.Type[] types, int offset) {
        checkThisOffset(offset);
        checkAvailableSpace(offset, length() - offset, Primitive.arrayCumulativeSize(types), types.length);
        return iToPrimitiveArray(types, offset, types.length);
    }
    
    /**
     * Returns a Primitive array, from the specified Field, whose primitive elements
     * are of the types specified in the types argument. The length of the returned
     * Primitive array is equal to the length of the specified array of Primitive
     * types.
     * 
     * @param types an array of Primitive Types
     * @param field a Field of this {@code BitString}
     * @return a Primitive array of the specified types
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code field.offset() > 0 && field.offset() >= length()}
     * @throws IllegalArgumentException        if
     *                                         {@code field.length() > length() - field.offset()}
     * @throws UnsupportedOperationException   if
     *                                         {@code field.offset() + Primitive.arrayCumulativeSize(types) > field.length()}
     */
    public Primitive[] getPrimitiveArray(Primitive.Type[] types, Field field) {
        final int offset = field.offset();
        final int length = field.length(this);
        checkThisOffset(offset);
        checkThisLength(offset, length);
        checkAvailableSpace(offset, length - offset, Primitive.arrayCumulativeSize(types), types.length);
        return iToPrimitiveArray(types, offset, types.length);
    }
    
    /**
     * puts the specified BitString at the start of this {@code BitString}.
     * 
     * @param that the argument BitString to be placed at the start this
     *             {@code BitString}
     * @return this BitString
     * @throws UnsupportedOperationException if
     *                                       {@code offset + that.length() > length()}
     */
    public BitString put(BitString that) {
        final int length = that.length();
        checkAvailableSpace(0, length(), length);
        iCopy(0, length, that, 0);
        return this;
    }
    
    /**
     * puts the specified BitString, at the specified offset, in this
     * {@code BitString}.
     * 
     * @param that   the argument BitString to be placed in this {@code BitString}
     * @param offset the offset of where to put the argument BitString
     * @return this BitString
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     * @throws UnsupportedOperationException   if
     *                                         {@code offset + that.length() > length()}
     */
    public BitString put(BitString that, int offset) {
        checkThisOffset(offset);
        final int length = that.length();
        checkAvailableSpace(offset, this.length() - offset, length);
        iCopy(offset, length, that, 0);
        return this;
    }
    
    
    public BitString put(BitString that, int thatOffset, int thatLength, int offset) {
        checkThisOffset(offset);
        that.checkArgOffset(thatOffset);
        that.checkArgLength(thatOffset, thatLength);
        checkAvailableSpace(offset, length() - offset, thatLength);
        iCopy(offset, thatLength, that, thatOffset);
        return this;
    }
    
    public BitString put(BitString that, Field thatField, Field thisField) {
        final int thisOffset = thisField.offset();
        final int thisLength = thisField.length(this);
        final int thatOffset = thatField.offset();
        final int thatLength = thatField.length(that);
        checkThisOffset(thisOffset);
        checkThisLength(thisOffset, thisLength);
        that.checkArgOffset(thatOffset);
        that.checkArgLength(thatOffset, thatLength);
        checkAvailableSpace(thisOffset, thisLength, thatLength);
        iCopy(thisOffset, thatLength, that, thatOffset);
        return this;
    }
    
    /**
     * Puts the specified bit, at the specified offset, in this {@code BitString}.
     * 
     * @param bit       the bit
     * @param bitOffset the offset of where to put the bit
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code bitOffset < 0 || bitOffset > 0 && bitOffset >= length()}
     */
    public BitString putBit(boolean bit, int bitOffset) {
        checkThisOffset(bitOffset);
        iBitOp(UnaryOp.set(bit), bitOffset);
        return this;
    }
    
    /**
     * Puts the specified bit at the specified offset in a substring of this
     * {@code BitString}.
     * 
     * The substring starts at offset 'offset' of this {@code BitString} and has a
     * length of 'length'.
     * 
     * Note, the bitOffset is relative to the start of the substring.
     * 
     * @param bit       the bit
     * @param bitOffset the offset of where to put the bit
     * @param offset    the start of this substring
     * @param length    the length of this substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code bitOffset < 0 || bitOffset > 0 && bitOffset >= length}
     *                                         or
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     * @throws IllegalArgumentException        if
     *                                         {@code length < 0 || length > length() - offset}
     */
    public BitString putBit(boolean bit, int bitOffset, int offset, int length) {
        checkThisOffset(offset);
        checkThisLength(offset, length);
        checkRelativeOffset(bitOffset, length);
        iBitOp(UnaryOp.set(bit), offset + bitOffset);
        return this;
    }
    
    /**
     * Puts the specified bit at the specified offset in a Field of this
     * {@code BitString}. The bit offset is relative to the start of the field.
     * 
     * @param bit the bit
     * @param bitOffset the offset of where to put the bit
     * @param field a Field of this {@code BitString}
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code bitOffset < 0 || bitOffset > 0 && bitOffset >= field.length()}
     *                                         or
     *                                         {@code field.offset() > 0 && field.offset() >= length()}
     * @throws IllegalArgumentException        if
     *                                         {@code field.length() > length() - field.offset()}
     */
    public BitString putBit(boolean bit, int bitOffset, Field field) {
        return putBit(bit, bitOffset, field.offset(), field.length(this));
    }
    
    /**
     * puts the specified boolean value at the specified offset.
     * 
     * @param booleanValue the boolean value
     * @param offset       the offset of where to put the boolean value
     * @return this BitString
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     */
    public BitString putBoolean(boolean booleanValue, int offset) {
        checkThisOffset(offset);
        iBitOp(UnaryOp.set(booleanValue), offset);
        return this;
    }
    
    /**
     * puts an array of boolean values at the specified offset.
     * 
     * @param booleans the array of booleans
     * @param offset   the offset of where to put the array of booleans
     * @return this BitString
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     * @throws UnsupportedOperationException   if
     *                                         {@code offset + booleans.length > length()}
     */
    public BitString putBooleanArray(boolean[] booleans, int offset) {
        checkThisOffset(offset);
        checkAvailableSpace(offset, length() - offset, (long)booleans.length, booleans.length);
        for (int index = 0; index < booleans.length; index++) {
            iBitOp(UnaryOp.set(booleans[index]), offset);
            offset++;
        }
        return this;
    }
    
    /**
     * puts the specified byte value at the specified offset.
     * 
     * @param byteValue the byte value
     * @param offset    the offset of where to put the byte value
     * @return this BitString
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     * @throws UnsupportedOperationException   if
     *                                         {@code offset + Byte.SIZE > length()}
     */
    public BitString putByte(byte byteValue, int offset) {
        checkThisOffset(offset);
        checkAvailableSpace(offset, length() - offset, Byte.SIZE);
        iPutPrimitiveWord(offset, Byte.SIZE, Byte.toUnsignedLong(byteValue));
        return this;
    }
    
    /**
     * puts an array of byte values at the specified offset.
     * 
     * @param bytes  the array of bytes
     * @param offset the offset of where to put the array of bytes
     * @return this BitString
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     * @throws UnsupportedOperationException   if
     *                                         {@code offset + count * Byte.SIZE > length()}
     */
    public BitString putByteArray(byte[] bytes, int offset) {
        checkThisOffset(offset);
        checkAvailableSpace(offset, length() - offset, bytes.length * (long)Byte.SIZE, bytes.length);
        for (int index = 0; index < bytes.length; index++) {
            iPutPrimitiveWord(offset, Byte.SIZE, Byte.toUnsignedLong(bytes[index]));
            offset += Byte.SIZE;
        }
        return this;
    }
    
    /**
     * puts the specified character value at the specified offset.
     * 
     * @param charValue the character value
     * @param offset    the offset of where to put the character value
     * @return this BitString
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     * @throws UnsupportedOperationException   if
     *                                         {@code offset + Character.SIZE > length()}
     */
    public BitString putChar(char charValue, int offset) {
        checkThisOffset(offset);
        checkAvailableSpace(offset, length() - offset, Character.SIZE);
        iPutPrimitiveWord(offset, Character.SIZE, (long)charValue);
        return this;
    }
    
    /**
     * puts an array of character values at the specified offset.
     * 
     * @param chars  the array of characters
     * @param offset the offset of where to put the array of characters
     * @return this BitString
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     * @throws UnsupportedOperationException   if
     *                                         {@code offset + count * Character.SIZE > length()}
     */
    public BitString putCharArray(char[] chars, int offset) {
        checkThisOffset(offset);
        checkAvailableSpace(offset, length() - offset, chars.length * (long)Character.SIZE, chars.length);
        for (int index = 0; index < chars.length; index++) {
            iPutPrimitiveWord(offset, Character.SIZE, (long)chars[index]);
            offset += Character.SIZE;
        }
        return this;
    }
    
    /**
     * puts the specified double value at the specified offset.
     * 
     * @param doubleValue the double value
     * @param offset      the offset of where to put the double value
     * @return this BitString
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     * @throws UnsupportedOperationException   if
     *                                         {@code offset + Double.SIZE > length()}
     */
    public BitString putDouble(double doubleValue, int offset) {
        checkThisOffset(offset);
        checkAvailableSpace(offset, length() - offset, Long.SIZE);
        iPutPrimitiveWord(offset, Long.SIZE, Double.doubleToRawLongBits(doubleValue));
        return this;
    }
    
    /**
     * puts an array of double values at the specified offset.
     * 
     * @param doubles the array of doubles
     * @param offset  the offset of where to put the array of doubles
     * @return this BitString
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     * @throws UnsupportedOperationException   if
     *                                         {@code offset + count * Double.SIZE > length()}
     */
    public BitString putDoubleArray(double[] doubles, int offset) {
        checkThisOffset(offset);
        checkAvailableSpace(offset, length() - offset, doubles.length * (long)Long.SIZE, doubles.length);
        for (int index = 0; index < doubles.length; index++) {
            iPutPrimitiveWord(offset, Long.SIZE, Double.doubleToRawLongBits(doubles[index]));
            offset += Long.SIZE;
        }
        return this;
    }
    
    /**
     * puts the specified float value at the specified offset.
     * 
     * @param floatValue the float value
     * @param offset     the offset of where to put the float value
     * @return this BitString
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     * @throws UnsupportedOperationException   if
     *                                         {@code offset + Float.SIZE > length()}
     */
    public BitString putFloat(float floatValue, int offset) {
        checkThisOffset(offset);
        checkAvailableSpace(offset, length() - offset, Integer.SIZE);
        iPutPrimitiveWord(offset, Integer.SIZE, Integer.toUnsignedLong(Float.floatToRawIntBits(floatValue)));
        return this;
    }
    
    /**
     * puts an array of float values at the specified offset.
     * 
     * @param floats the array of floats
     * @param offset the offset of where to put the array of floats
     * @return this BitString
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     * @throws UnsupportedOperationException   if
     *                                         {@code offset + count * Float.SIZE > length()}
     */
    public BitString putFloatArray(float[] floats, int offset) {
        checkThisOffset(offset);
        checkAvailableSpace(offset, length() - offset, floats.length * (long)Integer.SIZE, floats.length);
        for (int index = 0; index < floats.length; index++) {
            iPutPrimitiveWord(offset, Integer.SIZE, Integer.toUnsignedLong(Float.floatToRawIntBits(floats[index])));
            offset += Integer.SIZE;
        }
        return this;
    }
    
    /**
     * puts the specified integer value at the specified offset.
     * 
     * @param intValue the integer value
     * @param offset   the offset of where to put the integer value
     * @return this BitString
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     * @throws UnsupportedOperationException   if
     *                                         {@code offset + Integer.SIZE > length()}
     */
    public BitString putInt(int intValue, int offset) {
        checkThisOffset(offset);
        checkAvailableSpace(offset, length() - offset, Integer.SIZE);
        iPutPrimitiveWord(offset, Integer.SIZE, Integer.toUnsignedLong(intValue));
        return this;
    }
    
    /**
     * puts an array of integer values at the specified offset.
     * 
     * @param ints   the array of integers
     * @param offset the offset of where to put the array of integers
     * @return this BitString
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     * @throws UnsupportedOperationException   if
     *                                         {@code offset + count * Integer.SIZE > length()}
     */
    public BitString putIntArray(int[] ints, int offset) {
        checkThisOffset(offset);
        checkAvailableSpace(offset, length() - offset, ints.length * (long)Integer.SIZE, ints.length);
        for (int index = 0; index < ints.length; index++) {
            iPutPrimitiveWord(offset, Integer.SIZE, Integer.toUnsignedLong(ints[index]));
            offset += Integer.SIZE;
        }
        return this;
    }
    
    /**
     * puts the specified long value at the specified offset.
     * 
     * @param longValue the long value
     * @param offset    the offset of where to put the long value
     * @return this BitString
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     * @throws UnsupportedOperationException   if
     *                                         {@code offset + Long.SIZE > length()}
     */
    public BitString putLong(long longValue, int offset) {
        checkThisOffset(offset);
        checkAvailableSpace(offset, length() - offset, Long.SIZE);
        iPutPrimitiveWord(offset, Long.SIZE, longValue);
        return this;
    }
    
    /**
     * puts an array of long values at the specified offset.
     * 
     * @param longs  the array of longs
     * @param offset the offset of where to put the array of longs
     * @return this BitString
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     * @throws UnsupportedOperationException   if
     *                                         {@code offset + count * Long.SIZE > length()}
     */
    public BitString putLongArray(long[] longs, int offset) {
        checkThisOffset(offset);
        checkAvailableSpace(offset, length() - offset, longs.length * (long)Long.SIZE, longs.length);
        for (int index = 0; index < longs.length; index++) {
            iPutPrimitiveWord(offset, Long.SIZE, longs[index]);
            offset += Long.SIZE;
        }
        return this;
    }
    
    /**
     * puts the specified short value at the specified offset.
     * 
     * @param shortValue the short value
     * @param offset     the offset of where to put the short value
     * @return this BitString
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     * @throws UnsupportedOperationException   if
     *                                         {@code offset + Short.SIZE > length()}
     */
    public BitString putShort(short shortValue, int offset) {
        checkThisOffset(offset);
        checkAvailableSpace(offset, length() - offset, Short.SIZE);
        iPutPrimitiveWord(offset, Short.SIZE, Short.toUnsignedLong(shortValue));
        return this;
    }
    
    /**
     * puts an array of short values at the specified offset.
     * 
     * @param shorts  the array of shorts
     * @param offset the offset of where to put the array of shorts
     * @return this BitString
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     * @throws UnsupportedOperationException   if
     *                                         {@code offset + count * Short.SIZE > length()}
     */
    public BitString putShortArray(short[] shorts, int offset) {
        checkThisOffset(offset);
        checkAvailableSpace(offset, length() - offset, shorts.length * (long)Short.SIZE, shorts.length);
        for (int index = 0; index < shorts.length; index++) {
            iPutPrimitiveWord(offset, Short.SIZE, Short.toUnsignedLong(shorts[index]));
            offset += Short.SIZE;
        }
        return this;
    }
    
    /**
     * Puts the value of the specified Primitive at the specified offset.
     * 
     * @param primitive the Primitive
     * @param offset the offset of where to put the value of the Primitive
     * @return this BitString
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     * @throws UnsupportedOperationException   if
     *                                         {@code offset + primitive.type().size() > length()}
     */
    public BitString putPrimitive(Primitive primitive, int offset) {
        final int primitiveSize = primitive.type().size();
        checkThisOffset(offset);
        checkAvailableSpace(offset, length() - offset, primitiveSize);
        iPutPrimitiveWord(offset, primitiveSize, primitive.longValue());
        return this;
    }
    
    /**
     * Puts the value of the specified Primitive in the specified Field.
     * 
     * @param primitive the Primitive
     * @param field the Field where to put the value of the Primitive
     * @return this BitString
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code field.offset() > 0 && field.offset() >= length()}
     * @throws IllegalArgumentException        if
     *                                         {@code field.length() > length() - field.offset()}
     * @throws UnsupportedOperationException   if
     *                                         {@code primitive.type().size() > field.length()}
     */
    public BitString putPrimitive(Primitive primitive, Field field) {
        final int offset = field.offset();
        final int length = field.length(this);
        final int primitiveSize = primitive.type().size();
        checkThisOffset(offset);
        checkThisLength(offset, length);
        checkAvailableSpace(offset, length, primitiveSize);
        iPutPrimitiveWord(offset, primitiveSize, primitive.longValue());
        return this;
    }
    
    /**
     * Puts the values of the specified array of Primitives at the specified offset.
     * 
     * @param primitives the array of Primitives
     * @param offset     the offset of where to put the array of Primitive values
     * @return this BitString
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     * @throws UnsupportedOperationException   if
     *                                         {@code offset + Primitive.arrayCumulativeSize(primitives) > length()}
     */
    public BitString putPrimitiveArray(Primitive[] primitives, int offset) {
        checkThisOffset(offset);
        checkAvailableSpace(offset, length() - offset, Primitive.arrayCumulativeSize(primitives), primitives.length);
        iPutPrimitiveArray(offset, primitives);
        return this;
    }
    
    /**
     * Puts the values of the specified array of Primitives in the specified Field.
     * 
     * @param primitives the array of Primitives
     * @param field      the Field where to put the values of the array of
     *                   Primitives
     * @return this BitString
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code field.offset() > 0 && field.offset() >= length()}
     * @throws IllegalArgumentException        if
     *                                         {@code field.length() > length() - field.offset()}
     * @throws UnsupportedOperationException   if
     *                                         {@code Primitive.arrayCumulativeSize(primitives) > field.length()}
     */
    public BitString putPrimitiveArray(Primitive[] primitives, Field field) {
        final int offset = field.offset();
        final int length = field.length(this);
        checkThisOffset(offset);
        checkThisLength(offset, length);
        checkAvailableSpace(offset, length, Primitive.arrayCumulativeSize(primitives), primitives.length);
        iPutPrimitiveArray(offset, primitives);
        return this;
    }
    
    /**
     * Sets all of the bits in this {@code BitString} to {@code ZERO}.
     * 
     * @return this {@code BitString}
     */
    public BitString clear() {
        iClear(0, length());
        return this;
    }
    
    /**
     * Sets all of the bits in a Field of this {@code BitString} to {@code ZERO}.
     * 
     * @param field a Field of this {@code BitString}
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code field.offset() > 0 && field.offset() >= length()}
     * @throws IllegalArgumentException        if
     *                                         {@code field.length() > length() - field.offset()}
     */
    public BitString clear(Field field) {
        final int offset = field.offset();
        final int length = field.length(this);
        checkThisOffset(offset);
        checkThisLength(offset, length);
        iClear(offset, length);
        return this;
    }
    
    /**
     * Sets the single bit at the specified offset to {@code ZERO}.
     * 
     * @param bitOffset the offset of the bit to clear
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code bitOffset < 0 || bitOffset > 0 && bitOffset >= length()}
     */
    public BitString clearBit(int bitOffset) {
        checkThisOffset(bitOffset);
        iClearBit(bitOffset);
        return this;
    }
    
    /**
     * Sets the single bit at the specified offset in a Field of this BitString to
     * {@code ZERO}.
     * 
     * Note, the bitOffset is relative to the start of the Field.
     * 
     * @param bitOffset the offset of the bit to clear
     * @param field     a Field of this {@code BitString}
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code bitOffset < 0 || bitOffset > 0 && bitOffset >= length()}
     *                                         or
     *                                         {@code field.offset() > 0 && field.offset() >= length()}
     * @throws IllegalArgumentException        if
     *                                         {@code field.length() > length() - field.offset()}
     */
    public BitString clearBit(int bitOffset, Field field) {
        final int offset = field.offset();
        final int length = field.length(this);
        checkThisOffset(offset);
        checkThisLength(offset, length);
        checkRelativeOffset(bitOffset, length);
        iClearBit(offset + bitOffset);
        return this;
    }
    
    /**
     * Sets all of the bits in this {@code BitString} to the complement of its
     * current value.
     * 
     * @return this {@code BitString}
     */
    public BitString flip() {
        iFlip(0, length());
        return this;
    }
    
    /**
     * Sets all of the bits in a Field of this {@code BitString} to the complement
     * of its current value.
     * Note, the bitOffset is relative to the start of the Field.
     * 
     * @param field a Field of this {@code BitString}
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code field.offset() > 0 && field.offset() >= length()}
     * @throws IllegalArgumentException        if
     *                                         {@code field.length() > length() - field.offset()}
     */
    public BitString flip(Field field) {
        final int offset = field.offset();
        final int length = field.length(this);
        checkThisOffset(offset);
        checkThisLength(offset, length);
        iFlip(offset, length);
        return this;
    }
    
    /**
     * Sets the single bit at the specified offset to the complement of its current
     * value.
     * 
     * @param bitOffset the offset of the bit to flip
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     */
    public BitString flipBit(int bitOffset) {
        checkThisOffset(bitOffset);
        iFlipBit(bitOffset);
        return this;
    }
    
    /**
     * Sets the single bit at the specified offset in a Field of this BitString the
     * complement of its current value.
     * 
     * Note, the bitOffset is relative to the start of the Field.
     * 
     * @param bitOffset the offset of the bit to flip
     * @param field     a Field of this {@code BitString}
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code bitOffset < 0 || bitOffset > 0 && bitOffset >= length()}
     *                                         or
     *                                         {@code field.offset() > 0 && field.offset() >= length()}
     * @throws IllegalArgumentException        if
     *                                         {@code field.length() > length() - field.offset()}
     */
    public BitString flipBit(int bitOffset, Field field) {
        final int offset = field.offset();
        final int length = field.length(this);
        checkThisOffset(offset);
        checkThisLength(offset, length);
        checkRelativeOffset(bitOffset, length);
        iSetBit(offset + bitOffset);
        return this;
    }
    
    /**
     * Sets all of the bits in this {@code BitString} to {@code ONE}.
     * 
     * @return this {@code BitString}
     */
    public BitString set() {
        iSet(0, length());
        return this;
    }
    
    /**
     * Sets all of the bits in a Field of this {@code BitString} to {@code ONE}.
     * Note, the bitOffset is relative to the start of the Field.
     * 
     * @param field a Field of this {@code BitString}
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code field.offset() > 0 && field.offset() >= length()}
     * @throws IllegalArgumentException        if
     *                                         {@code field.length() > length() - field.offset()}
     */
    public BitString set(Field field) {
        final int offset = field.offset();
        final int length = field.length(this);
        checkThisOffset(offset);
        checkThisLength(offset, length);
        iSet(offset, length);
        return this;
    }
    
    /**
     * Sets the single bit at the specified offset to {@code ONE}.
     * 
     * @param bitOffset the offset of the bit to set
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code bitOffset < 0 || bitOffset > 0 && bitOffset >= length()}
     */
    public BitString setBit(int bitOffset) {
        checkThisOffset(bitOffset);
        iSetBit(bitOffset);
        return this;
    }
    
    /**
     * Set the single bit at the specified offset in a Field of this BitString to
     * {@code ONE}.
     * 
     * Note, the bitOffset is relative to the start of the Field.
     * 
     * @param bitOffset the offset of the bit to set
     * @param field     a Field of this {@code BitString}
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code bitOffset < 0 || bitOffset > 0 && bitOffset >= length()}
     *                                         or
     *                                         {@code field.offset() > 0 && field.offset() >= length()}
     * @throws IllegalArgumentException        if
     *                                         {@code field.length() > length() - field.offset()}
     */
    public BitString setBit(int bitOffset, Field field) {
        final int offset = field.offset();
        final int length = field.length(this);
        checkThisOffset(offset);
        checkThisLength(offset, length);
        checkRelativeOffset(bitOffset, length);
        iSetBit(offset + bitOffset);
        return this;
    }
    
    /**
     * Performs the specified unary bitwise operation (op) on all of the bits in
     * this {@code BitString}.
     * 
     * @param op the unary bitwise operation to be performed
     * @return this {@code BitString}
     */
    public BitString op(UnaryOp op) {
        iUnaryOp(op.op(), 0, length());
        return this;
    }
    
    /**
     * Performs the specified unary bitwise operation (op) on all of the bits in a
     * substring of this {@code BitString}.
     * 
     * This substring starts at offset 'offset' of this {@code BitString} and
     * extends to the end of this {@code BitString}.
     * 
     * @param op     the unary bitwise operation to be performed
     * @param offset the start of this substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     */
    public BitString op(UnaryOp op, int offset) {
        checkThisOffset(offset);
        iUnaryOp(op.op(), offset, length() - offset);
        return this;
    }
    
    /**
     * Performs the specified unary bitwise operation (op) on all of the bits in a
     * Field of this {@code BitString}.
     *
     * The substring starts at offset 'offset' of this {@code BitString} and has a
     * length of 'length'.
     * 
     * @param op     the unary bitwise operation to be performed
     * @param offset the start of this substring
     * @param length the length of this substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     * @throws IllegalArgumentException        if
     *                                         {@code length < 0 || length > length() - offset}
     */
    public BitString op(UnaryOp op, int offset, int length) {
        checkThisOffset(offset);
        checkThisLength(offset, length);
        iUnaryOp(op.op(), offset, length);
        return this;
    }
    
    /**
     * Performs the specified unary bitwise operation (op) on all of the bits in a
     * Field of this {@code BitString}.
     * 
     * @param op    the unary bitwise operation to be performed
     * @param field a Field of this {@code BitString}
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code field.offset() > 0 && field.offset() >= length()}
     * @throws IllegalArgumentException        if
     *                                         {@code field.length() > length() - field.offset()}
     */
    public BitString op(UnaryOp op, Field field) {
        return op(op, field.offset(), field.length(this));
    }
    
    /**
     * Performs the specified unary bitwise operation (op) on the single bit at the
     * specified offset.
     * 
     * @param op        the unary bitwise operation to be performed
     * @param bitOffset the offset of the bit that is operated on
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code bitOffset < 0 || bitOffset > 0 && bitOffset >= length()}
     */
    public BitString bitOp(UnaryOp op, int bitOffset) {
        checkRelativeOffset(bitOffset, length());
        iBitOp(op, bitOffset);
        return this;
    }
    
    /**
     * Performs the specified unary bitwise operation (op) on the single bit at the
     * specified offset in a substring of this BitString.
     * 
     * The substring starts at offset 'offset' of this {@code BitString} and has a
     * length of 'length'.
     * 
     * Note, the bitOffset is relative to the start of the substring.
     * 
     * @param op        the unary bitwise operation to be performed
     * @param bitOffset the offset of the bit that is operated on
     * @param offset    the start of this substring
     * @param length    the length of this substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code bitOffset < 0 || bitOffset > 0 && bitOffset >= length()}
     *                                         or
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     * @throws IllegalArgumentException        if
     *                                         {@code length < 0 || length > length() - offset}
     */
    public BitString bitOp(UnaryOp op, int bitOffset, int offset, int length) {
        checkThisOffset(offset);
        checkThisLength(offset, length);
        checkRelativeOffset(bitOffset, length);
        iBitOp(op, offset + bitOffset);
        return this;
    }
    
    /**
     * Performs the specified unary bitwise operation (op) on the single bit at the
     * specified offset in a Field of this BitString.
     * 
     * Note, the bitOffset is relative to the start of the Field.
     * 
     * @param op        the unary bitwise operation to be performed
     * @param bitOffset the offset of the bit that is operated on
     * @param field     a Field of this {@code BitString}
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code bitOffset < 0 || bitOffset > 0 && bitOffset >= length()}
     *                                         or
     *                                         {@code field.offset() > 0 && field.offset() >= length()}
     * @throws IllegalArgumentException        if
     *                                         {@code field.length() > length() - field.offset()}
     */
    public BitString bitOp(UnaryOp op, int bitOffset, Field field) {
        return bitOp(op, bitOffset, field.offset(), field.length(this));
    }
    
    /**
     * Performs a logical <b>AND</b> of this {@code BitString} with the specified
     * bit string (arg).
     *
     * The length of the operation is equal to the smaller of the length of this
     * {@code BitString} or the length of the specified bit string.
     * <p>
     * The bits in this {@code BitString} are modified according to the following
     * logic table. For each {@code ONE} bit in the argument, the
     * corresponding bit in this {@code BitString} is left unchanged, otherwise,
     * the bit is set to {@code ZERO}.
     * 
     * <pre>
     *            arg bit
     *             value
     *           | 0 | 1 |
     *        ===|=======|
     *   this  0 | 0 | 0 |
     *    bit ---|-------|
     *  value  1 | 0 | 1 |
     *        ============
     * </pre>
     *
     * @param arg bit string argument
     * @return this {@code BitString} with the results of the operation
     */
    public BitString and(BitString arg) {
        iAnd(0, Math.min(this.length(), arg.length()), arg, 0);
        return this;
    }
    
    /**
     * Performs a logical <b>AND</b> of a Field of this {@code BitString} with a
     * Field of the specified bit string (arg).
     * 
     * The length of the operation is equal to the smaller of the length of this
     * Field or the length of the argument Field.
     * <p>
     * The bits this field are modified according to the following
     * logic table. For each {@code ONE} bit in the argument field, the
     * corresponding bit in this field is left unchanged, otherwise,
     * the bit is set to {@code ZERO}.
     * 
     * <pre>
     *            arg bit
     *             value
     *           | 0 | 1 |
     *        ===|=======|
     *   this  0 | 0 | 0 |
     *    bit ---|-------|
     *  value  1 | 0 | 1 |
     *        ============
     * </pre>
     * 
     * @param thisField Field of this {@code BitString}
     * @param arg       bit string argument
     * @param argField  Field of the bit string argument
     * @return this {@code BitString} with the results of the operation
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisField.offset() > 0 && thisField.offset() >= this.length()}
     *                                         or
     *                                         {@code argField.offset() > 0 && argField.offset() >= arg.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisField.length() > this.length() - thisField.offset()}
     *                                         or
     *                                         {@code argField.length() > arg.length() - argField.offset()}
     */
    public BitString and(Field thisField, BitString arg, Field argField) {
        final int thisOffset = thisField.offset();
        final int thisLength = thisField.length(this);
        final int argOffset = argField.offset();
        final int argLength = argField.length(arg);
        checkThisOffset(thisOffset);
        checkThisLength(thisOffset, thisLength);
        arg.checkArgOffset(argOffset);
        arg.checkArgLength(argOffset, argLength);
        iAnd(thisOffset, Math.min(thisLength, argLength), arg, argOffset);
        return this;
    }
    
    /**
     * Clears all of the bits in this {@code BitString} whose corresponding bit is
     * set in the specified bit string.
     * 
     * The length of the operation is equal to the smaller of the length of this
     * {@code BitString} or the length of the specified bit string.
     * <p>
     * The bits in this {@code BitString} are modified according to the following
     * logic table. For each {@code ONE} bit in the argument, the
     * corresponding bit in this {@code BitString} is set to {@code ZERO}, otherwise,
     * the bit is left unchanged. This operation is the same as an <b>AND</b> operation
     * where the argument bit value is flipped.
     * 
     * <pre>
     *            arg bit
     *             value
     *           | 0 | 1 |
     *        ===|=======|
     *   this  0 | 0 | 0 |
     *    bit ---|-------|
     *  value  1 | 1 | 0 |
     *        ============
     * </pre>
     *
     * @param arg bit string argument used to mask this {@code BitString} 
     * @return this {@code BitString} with the results of the operation
     */
    public BitString andNot(BitString arg) {
        iAndNot(0, Math.min(this.length(), arg.length()), arg, 0);
        return this;
    }
    
    /**
     * Clears all of the bits in a Field of this {@code BitString} whose corresponding bit is
     * set in a Field of the specified bit string (arg).
     * 
     * The length of the operation is equal to the smaller of the length of this
     * Field or the length of the specified Field.
     * <p>
     * The bits in this field are modified according to the following
     * logic table. For each {@code ONE} bit in the argument field, the
     * corresponding bit in this field is set to {@code ZERO}, otherwise,
     * the bit is left unchanged. This operation is the same as an <b>AND</b> operation
     * where the argument bit value is flipped.
     * 
     * <pre>
     *            arg bit
     *             value
     *           | 0 | 1 |
     *        ===|=======|
     *   this  0 | 0 | 0 |
     *    bit ---|-------|
     *  value  1 | 1 | 0 |
     *        ============
     * </pre>
     * 
     * @param thisField Field of this {@code BitString}
     * @param arg       bit string argument
     * @param argField  Field of the bit string argument
     * @return this {@code BitString} with the results of the operation
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisField.offset() > 0 && thisField.offset() >= this.length()}
     *                                         or
     *                                         {@code argField.offset() > 0 && argField.offset() >= arg.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisField.length() > this.length() - thisField.offset()}
     *                                         or
     *                                         {@code argField.length() > arg.length() - argField.offset()}
     */
    public BitString andNot(Field thisField, BitString arg, Field argField) {
        final int thisOffset = thisField.offset();
        final int thisLength = thisField.length(this);
        final int argOffset = argField.offset();
        final int argLength = argField.length(arg);
        checkThisOffset(thisOffset);
        checkThisLength(thisOffset, thisLength);
        arg.checkArgOffset(argOffset);
        arg.checkArgLength(argOffset, argLength);
        iAndNot(thisOffset, Math.min(thisLength, argLength), arg, argOffset);
        return this;
    }
    
    /**
     * The bits in this {@code BitString} are set to the corresponding bit in the
     * specified bit string. In other words, the argument bit string is copied into
     * this {@code BitString}.
     * 
     * The length of the operation is equal to the smaller of the length of this
     * {@code BitString} or the length of the specified bit string.
     * <p>
     * The bits in this {@code BitString} are modified according to the following
     * logic table. Each bit in this {@code BitString} is set to the corresponding
     * bit in the argument bit string..
     * 
     * <pre>
     *            arg bit
     *             value
     *           | 0 | 1 |
     *        ===|=======|
     *   this  0 | 0 | 1 |
     *    bit ---|-------|
     *  value  1 | 0 | 1 |
     *        ============
     * </pre>
     * 
     * @param arg bit string argument used to mask this {@code BitString}
     * @return this {@code BitString} with the results of the operation
     */
    public BitString copy(BitString arg) {
        iCopy(0, Math.min(this.length(), arg.length()), arg, 0);
        return this;
    }
    
    /**
     * The bits in a Field of this {@code BitString} are set to the corresponding
     * bit in a Field of the specified bit string (arg). In other words, the
     * argument Field is copied into this Field.
     * 
     * The length of the operation is equal to the smaller of the length of this
     * Field or the length of the specified Field.
     * <p>
     * The bits in this field are modified according to the following logic table.
     * Each bit in this Field is set to the corresponding bit in the argument Field.
     * 
     * <pre>
     *            arg bit
     *             value
     *           | 0 | 1 |
     *        ===|=======|
     *   this  0 | 0 | 1 |
     *    bit ---|-------|
     *  value  1 | 0 | 1 |
     *        ============
     * </pre>
     * 
     * @param thisField Field of this {@code BitString}
     * @param arg       bit string argument
     * @param argField  Field of the bit string argument
     * @return this {@code BitString} with the results of the operation
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisField.offset() > 0 && thisField.offset() >= this.length()}
     *                                         or
     *                                         {@code argField.offset() > 0 && argField.offset() >= arg.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisField.length() > this.length() - thisField.offset()}
     *                                         or
     *                                         {@code argField.length() > arg.length() - argField.offset()}
     */
    public BitString copy(Field thisField, BitString arg, Field argField) {
        final int thisOffset = thisField.offset();
        final int thisLength = thisField.length(this);
        final int argOffset = argField.offset();
        final int argLength = argField.length(arg);
        checkThisOffset(thisOffset);
        checkThisLength(thisOffset, thisLength);
        arg.checkArgOffset(argOffset);
        arg.checkArgLength(argOffset, argLength);
        iCopy(thisOffset, Math.min(thisLength, argLength), arg, argOffset);
        return this;
    }
    
    /**
     * The bits in this {@code BitString} are set to the complement of the
     * corresponding bit in the specified bit string. In other words, a complement
     * of the argument bit string is copied into this {@code BitString}.
     * 
     * The length of the operation is equal to the smaller of the length of this
     * {@code BitString} or the length of the specified bit string.
     * <p>
     * The bits in this {@code BitString} are modified according to the following
     * logic table. Each bit in this {@code BitString} is set to the complement of
     * the corresponding bit in the argument bit string.
     * 
     * <pre>
     *            arg bit
     *             value
     *           | 0 | 1 |
     *        ===|=======|
     *   this  0 | 1 | 0 |
     *    bit ---|-------|
     *  value  1 | 1 | 0 |
     *        ============
     * </pre>
     * 
     * @param arg bit string argument used to mask this {@code BitString}
     * @return this {@code BitString} with the results of the operation
     */
    public BitString copyNot(BitString arg) {
        iCopyNot(0, Math.min(this.length(), arg.length()), arg, 0);
        return this;
    }
    
    /**
     * The bits in a Field of this {@code BitString} are set to the complement of the corresponding bit
     * in a Field of the specified bit string (arg). In other words, a complement of the argument Field is
     * copied into this Field.
     * 
     * The length of the operation is equal to the smaller of the length of this
     * Field or the length of the specified Field.
     * <p>
     * The bits in this field are modified according to the following
     * logic table. Each bit in this Field is set to the complement of the
     * corresponding bit in the argument Field.
     * 
     * <pre>
     *            arg bit
     *             value
     *           | 0 | 1 |
     *        ===|=======|
     *   this  0 | 1 | 0 |
     *    bit ---|-------|
     *  value  1 | 1 | 0 |
     *        ============
     * </pre>
     * 
     * @param thisField Field of this {@code BitString}
     * @param arg       bit string argument
     * @param argField  Field of the bit string argument
     * @return this {@code BitString} with the results of the operation
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisField.offset() > 0 && thisField.offset() >= this.length()}
     *                                         or
     *                                         {@code argField.offset() > 0 && argField.offset() >= arg.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisField.length() > this.length() - thisField.offset()}
     *                                         or
     *                                         {@code argField.length() > arg.length() - argField.offset()}
     */
    public BitString copyNot(Field thisField, BitString arg, Field argField) {
        final int thisOffset = thisField.offset();
        final int thisLength = thisField.length(this);
        final int argOffset = argField.offset();
        final int argLength = argField.length(arg);
        checkThisOffset(thisOffset);
        checkThisLength(thisOffset, thisLength);
        arg.checkArgOffset(argOffset);
        arg.checkArgLength(argOffset, argLength);
        iCopyNot(thisOffset, Math.min(thisLength, argLength), arg, argOffset);
        return this;
    }
    
    /**
     * Performs a logical <b>NAND</b> of this {@code BitString} with the specified bit
     * string (arg).
     * 
     * The length of the operation is equal to the smaller of the length of this
     * {@code BitString} or the length of the specified bit string.
     * <p>
     * The bits in this {@code BitString} are modified according to the following
     * logic table. For each {@code ONE} bit in the argument, the
     * corresponding bit in this {@code BitString} is flipped, otherwise,
     * the bit is set to {@code ONE}. This operation is the complement of the <b>AND</b> operation.
     * 
     * <pre>
     *            arg bit
     *             value
     *           | 0 | 1 |
     *        ===|=======|
     *   this  0 | 1 | 1 |
     *    bit ---|-------|
     *  value  1 | 1 | 0 |
     *        ============
     * </pre>
     *
     * @param arg bit string argument
     * @return this {@code BitString} with the results of the operation
     */
    public BitString nand(BitString arg) {
        iNand(0, Math.min(this.length(), arg.length()), arg, 0);
        return this;
    }
    
    /**
     * Performs a logical <b>NAND</b> of a Field of this {@code BitString} with a
     * Field of the specified bit string (arg).
     * 
     * The length of the operation is equal to the smaller of the length of this
     * Field or the length of the specified Field.
     * <p>
     * The bits in this field are modified according to the following
     * logic table. For each {@code ONE} bit in the argument field, the
     * corresponding bit in this field is flipped, otherwise,
     * the bit is set to {@code ONE}. This operation is the complement of the <b>AND</b> operation.
     * 
     * <pre>
     *            arg bit
     *             value
     *           | 0 | 1 |
     *        ===|=======|
     *   this  0 | 1 | 1 |
     *    bit ---|-------|
     *  value  1 | 1 | 0 |
     *        ============
     * </pre>
     * 
     * @param thisField Field of this {@code BitString}
     * @param arg       bit string argument
     * @param argField  Field of the bit string argument
     * @return this {@code BitString} with the results of the operation
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisField.offset() > 0 && thisField.offset() >= this.length()}
     *                                         or
     *                                         {@code argField.offset() > 0 && argField.offset() >= arg.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisField.length() > this.length() - thisField.offset()}
     *                                         or
     *                                         {@code argField.length() > arg.length() - argField.offset()}
     */
    public BitString nand(Field thisField, BitString arg, Field argField) {
        final int thisOffset = thisField.offset();
        final int thisLength = thisField.length(this);
        final int argOffset = argField.offset();
        final int argLength = argField.length(arg);
        checkThisOffset(thisOffset);
        checkThisLength(thisOffset, thisLength);
        arg.checkArgOffset(argOffset);
        arg.checkArgLength(argOffset, argLength);
        iNand(thisOffset, Math.min(thisLength, argLength), arg, argOffset);
        return this;
    }
    
    /**
     * Sets all of the bits in this {@code BitString} whose corresponding bit is
     * set in the specified bit string, otherwise, the bits are fliped.
     * 
     * The length of the operation is equal to the smaller of the length of this
     * {@code BitString} or the length of the specified bit string.
     * <p>
     * The bits in this {@code BitString} are modified according to the following
     * logic table. For each {@code ONE} bit in the argument, the
     * corresponding bit in this {@code BitString} is set to {@code ONE}, otherwise,
     * the bit is flipped. This operation is the same as an <b>NAND</b> operation
     * where the argument bit value is fliped, or the compliment of the <b>ANDNOT</b> operation.
     * 
     * <pre>
     *            arg bit
     *             value
     *           | 0 | 1 |
     *        ===|=======|
     *   this  0 | 1 | 1 |
     *    bit ---|-------|
     *  value  1 | 0 | 1 |
     *        ============
     * </pre>
     *
     * @param arg bit string argument used to mask this {@code BitString} 
     * @return this {@code BitString} with the results of the operation
     */
    public BitString nandNot(BitString arg) {
        iNandNot(0, Math.min(this.length(), arg.length()), arg, 0);
        return this;
    }
    
    /**
     * Sets all of the bits in a field of this {@code BitString} whose corresponding bit is
     * set in a field of the specified bit string, otherwise, the bits are flipped.
     * 
     * The length of the operation is equal to the smaller of the length of this
     * Field or the length of the specified Field.
     * <p>
     * The bits in this field are modified according to the following
     * logic table. For each {@code ONE} bit in the argument field, the
     * corresponding bit in this field is set to {@code ONE}, otherwise,
     * the bit is flipped. This operation is the same as an <b>NAND</b> operation
     * where the argument bit value is flipped, or the compliment of the <b>ANDNOT</b> operation.
     * 
     * <pre>
     *            arg bit
     *             value
     *           | 0 | 1 |
     *        ===|=======|
     *   this  0 | 1 | 1 |
     *    bit ---|-------|
     *  value  1 | 0 | 1 |
     *        ============
     * </pre>
     * 
     * @param thisField Field of this {@code BitString}
     * @param arg       bit string argument
     * @param argField  Field of the bit string argument
     * @return this {@code BitString} with the results of the operation
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisField.offset() > 0 && thisField.offset() >= this.length()}
     *                                         or
     *                                         {@code argField.offset() > 0 && argField.offset() >= arg.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisField.length() > this.length() - thisField.offset()}
     *                                         or
     *                                         {@code argField.length() > arg.length() - argField.offset()}
     */
    public BitString nandNot(Field thisField, BitString arg, Field argField) {
        final int thisOffset = thisField.offset();
        final int thisLength = thisField.length(this);
        final int argOffset = argField.offset();
        final int argLength = argField.length(arg);
        checkThisOffset(thisOffset);
        checkThisLength(thisOffset, thisLength);
        arg.checkArgOffset(argOffset);
        arg.checkArgLength(argOffset, argLength);
        iNandNot(thisOffset, Math.min(thisLength, argLength), arg, argOffset);
        return this;
    }
    
    /**
     * Performs a logical <b>NOR</b> of this {@code BitString} with the specified bit
     * string (arg).
     * 
     * The length of the operation is equal to the smaller of the length of this
     * {@code BitString} or the length of the specified bit string.
     * <p>
     * The bits in this {@code BitString} are modified according to the following
     * logic table. For each {@code ONE} bit in the argument, the
     * corresponding bit in this {@code BitString} is set to {@code ZERO}, otherwise,
     * the bit is flipped. This operation is the complement of the <b>OR</b> operation.
     * 
     * <pre>
     *            arg bit
     *             value
     *           | 0 | 1 |
     *        ===|=======|
     *   this  0 | 1 | 0 |
     *    bit ---|-------|
     *  value  1 | 0 | 0 |
     *        ============
     * </pre>
     *
     * @param arg bit string argument
     * @return this {@code BitString} with the results of the operation
     */
    public BitString nor(BitString arg) {
        iNor(0, Math.min(this.length(), arg.length()), arg, 0);
        return this;
    }
    
    /**
     * Performs a logical <b>NOR</b> of a Field of this {@code BitString} with a
     * Field of the specified bit string (arg).
     * 
     * The length of the operation is equal to the smaller of the length of this
     * Field or the length of the specified Field.
     * <p>
     * The bits in this field are modified according to the following
     * logic table. For each {@code ONE} bit in the argument field, the
     * corresponding bit in this field is set to {@code ZERO}, otherwise,
     * the bit is flipped. This operation is the complement of the <b>OR</b> operation.
     * 
     * <pre>
     *            arg bit
     *             value
     *           | 0 | 1 |
     *        ===|=======|
     *   this  0 | 1 | 0 |
     *    bit ---|-------|
     *  value  1 | 0 | 0 |
     *        ============
     * </pre>
     * 
     * @param thisField Field of this {@code BitString}
     * @param arg       bit string argument
     * @param argField  Field of the bit string argument
     * @return this {@code BitString} with the results of the operation
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisField.offset() > 0 && thisField.offset() >= this.length()}
     *                                         or
     *                                         {@code argField.offset() > 0 && argField.offset() >= arg.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisField.length() > this.length() - thisField.offset()}
     *                                         or
     *                                         {@code argField.length() > arg.length() - argField.offset()}
     */
    public BitString nor(Field thisField, BitString arg, Field argField) {
        final int thisOffset = thisField.offset();
        final int thisLength = thisField.length(this);
        final int argOffset = argField.offset();
        final int argLength = argField.length(arg);
        checkThisOffset(thisOffset);
        checkThisLength(thisOffset, thisLength);
        arg.checkArgOffset(argOffset);
        arg.checkArgLength(argOffset, argLength);
        iNor(thisOffset, Math.min(thisLength, argLength), arg, argOffset);
        return this;
    }
    
    /**
     * Flips all of the bits in this {@code BitString} whose corresponding bit is
     * {@code ONE} in the specified bit string, otherwise, the bits are set to {@code ZERO}.
     * 
     * The length of the operation is equal to the smaller of the length of this
     * {@code BitString} or the length of the specified bit string.
     * <p>
     * The bits in this {@code BitString} are modified according to the following
     * logic table. For each {@code ONE} bit in the argument, the
     * corresponding bit in this {@code BitString} is flipped, otherwise,
     * the bit is set to {@code ZERO}. This operation is the same as a <b>NOR</b> operation
     * where the argument bit value is flipped, or the compliment of the <b>ORNOT</b> operation.
     * 
     * <pre>
     *            arg bit
     *             value
     *           | 0 | 1 |
     *        ===|=======|
     *   this  0 | 0 | 1 |
     *    bit ---|-------|
     *  value  1 | 0 | 0 |
     *        ============
     * </pre>
     *
     * @param arg bit string argument used to mask this {@code BitString} 
     * @return this {@code BitString} with the results of the operation
     */
    public BitString norNot(BitString arg) {
        iNorNot(0, Math.min(this.length(), arg.length()), arg, 0);
        return this;
    }
    
    /**
     * Flips all of the bits in a field of this {@code BitString} whose corresponding bit is
     * {@code ONE} in a field of the specified bit string, otherwise, the bits are set to {@code ZERO}.
     * 
     * The length of the operation is equal to the smaller of the length of this
     * Field or the length of the specified Field.
     * <p>
     * The bits in this field are modified according to the following
     * logic table. For each {@code ONE} bit in the argument field, the
     * corresponding bit in this field is flipped, otherwise,
     * the bit is set to {@code ZERO}. This operation is the same as a <b>NOR</b> operation
     * where the argument bit value is flipped, or the compliment of the <b>ORNOT</b> operation.
     * 
     * <pre>
     *            arg bit
     *             value
     *           | 0 | 1 |
     *        ===|=======|
     *   this  0 | 0 | 1 |
     *    bit ---|-------|
     *  value  1 | 0 | 0 |
     *        ============
     * </pre>
     * 
     * @param thisField Field of this {@code BitString}
     * @param arg       bit string argument
     * @param argField  Field of the bit string argument
     * @return this {@code BitString} with the results of the operation
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisField.offset() > 0 && thisField.offset() >= this.length()}
     *                                         or
     *                                         {@code argField.offset() > 0 && argField.offset() >= arg.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisField.length() > this.length() - thisField.offset()}
     *                                         or
     *                                         {@code argField.length() > arg.length() - argField.offset()}
     */
    public BitString norNot(Field thisField, BitString arg, Field argField) {
        final int thisOffset = thisField.offset();
        final int thisLength = thisField.length(this);
        final int argOffset = argField.offset();
        final int argLength = argField.length(arg);
        checkThisOffset(thisOffset);
        checkThisLength(thisOffset, thisLength);
        arg.checkArgOffset(argOffset);
        arg.checkArgLength(argOffset, argLength);
        iNorNot(thisOffset, Math.min(thisLength, argLength), arg, argOffset);
        return this;
    }
    
    /**
     * Performs a logical <b>OR</b> of this {@code BitString} with the specified bit
     * string (arg).
     * 
     * The length of the operation is equal to the smaller of the length of this
     * {@code BitString} or the length of the specified bit string.
     * <p>
     * The bits in this {@code BitString} are modified according to the following
     * logic table. For each {@code ONE} bit in the argument, the
     * corresponding bit in this {@code BitString} is set to {@code ONE}, otherwise,
     * the bit is left unchanged.
     * 
     * <pre>
     *            arg bit
     *             value
     *           | 0 | 1 |
     *        ===|=======|
     *   this  0 | 0 | 1 |
     *    bit ---|-------|
     *  value  1 | 1 | 1 |
     *        ============
     * </pre>
     *
     * @param arg bit string argument
     * @return this {@code BitString} with the results of the operation
     */
    public BitString or(BitString arg) {
        iOr(0, Math.min(this.length(), arg.length()), arg, 0);
        return this;
    }
    
    /**
     * Performs a logical <b>OR</b> of a Field of this {@code BitString} with a
     * Field of the specified bit string (arg).
     * 
     * This {@code BitString} is modified by this operation (see
     * {@link #or(BitString)}).
     * 
     * The length of the operation is equal to the smaller of the length of this
     * Field or the length of the specified Field.
     * <p>
     * The bits in this field are modified according to the following
     * logic table. For each {@code ONE} bit in the argument field, the
     * corresponding bit in this field is set to {@code ONE}, otherwise,
     * the bit is left unchanged.
     * 
     * <pre>
     *            arg bit
     *             value
     *           | 0 | 1 |
     *        ===|=======|
     *   this  0 | 0 | 1 |
     *    bit ---|-------|
     *  value  1 | 1 | 1 |
     *        ============
     * </pre>
     * 
     * @param thisField Field of this {@code BitString}
     * @param arg       bit string argument
     * @param argField  Field of the bit string argument
     * @return this {@code BitString} with the results of the operation
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisField.offset() > 0 && thisField.offset() >= this.length()}
     *                                         or
     *                                         {@code argField.offset() > 0 && argField.offset() >= arg.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisField.length() > this.length() - thisField.offset()}
     *                                         or
     *                                         {@code argField.length() > arg.length() - argField.offset()}
     */
    public BitString or(Field thisField, BitString arg, Field argField) {
        final int thisOffset = thisField.offset();
        final int thisLength = thisField.length(this);
        final int argOffset = argField.offset();
        final int argLength = argField.length(arg);
        checkThisOffset(thisOffset);
        checkThisLength(thisOffset, thisLength);
        arg.checkArgOffset(argOffset);
        arg.checkArgLength(argOffset, argLength);
        iOr(thisOffset, Math.min(thisLength, argLength), arg, argOffset);
        return this;
    }
    
    /**
     * Sets all of the bits in this {@code BitString} whose corresponding bit is
     * {@code ZERO} in the specified bit string.
     * 
     * The length of the operation is equal to the smaller of the length of this
     * {@code BitString} or the length of the specified bit string.
     * <p>
     * The bits in this {@code BitString} are modified according to the following
     * logic table. For each {@code ONE} bit in the argument, the
     * corresponding bit in this {@code BitString} is left unchanged, otherwise,
     * the bit is set to {@code ONE}. This operation is the same as an <b>OR</b> operation
     * where the argument bit value is flipped.
     * 
     * <pre>
     *            arg bit
     *             value
     *           | 0 | 1 |
     *        ===|=======|
     *   this  0 | 1 | 0 |
     *    bit ---|-------|
     *  value  1 | 1 | 1 |
     *        ============
     * </pre>
     *
     * @param arg bit string argument used to mask this {@code BitString} 
     * @return this {@code BitString} with the results of the operation
     */
    public BitString orNot(BitString arg) {
        iOrNot(0, Math.min(this.length(), arg.length()), arg, 0);
        return this;
    }
    
    /**
     * Sets all of the bits in a field of this {@code BitString} whose corresponding bit is
     * {@code ZERO} in a field of the specified bit string (arg).
     * 
     * The length of the operation is equal to the smaller of the length of this
     * Field or the length of the specified Field.
     * <p>
     * The bits in this field are modified according to the following
     * logic table. For each {@code ONE} bit in the argument field, the
     * corresponding bit in this field is set left unchanged, otherwise,
     * the bit is set to {@code ONE}. This operation is the same as an <b>OR</b> operation
     * where the argument bit value is flipped.
     * 
     * <pre>
     *            arg bit
     *             value
     *           | 0 | 1 |
     *        ===|=======|
     *   this  0 | 1 | 0 |
     *    bit ---|-------|
     *  value  1 | 1 | 1 |
     *        ============
     * </pre>
     * 
     * @param thisField Field of this {@code BitString}
     * @param arg       bit string argument
     * @param argField  Field of the bit string argument
     * @return this {@code BitString} with the results of the operation
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisField.offset() > 0 && thisField.offset() >= this.length()}
     *                                         or
     *                                         {@code argField.offset() > 0 && argField.offset() >= arg.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisField.length() > this.length() - thisField.offset()}
     *                                         or
     *                                         {@code argField.length() > arg.length() - argField.offset()}
     */
    public BitString orNot(Field thisField, BitString arg, Field argField) {
        final int thisOffset = thisField.offset();
        final int thisLength = thisField.length(this);
        final int argOffset = argField.offset();
        final int argLength = argField.length(arg);
        checkThisOffset(thisOffset);
        checkThisLength(thisOffset, thisLength);
        arg.checkArgOffset(argOffset);
        arg.checkArgLength(argOffset, argLength);
        iOrNot(thisOffset, Math.min(thisLength, argLength), arg, argOffset);
        return this;
    }
    
    /**
     * Performs a logical <b>XOR</b> of this {@code BitString} with the specified
     * bit string (arg).
     * 
     * The length of the operation is equal to the smaller of the length of this
     * {@code BitString} or the length of the specified bit string.
     * <p>
     * The bits in this {@code BitString} are modified according to the following
     * logic table. For each {@code ONE} bit in the argument, the
     * corresponding bit in this {@code BitString} is flipped, otherwise,
     * the bit is left unchanged.
     * 
     * <pre>
     *            arg bit
     *             value
     *           | 0 | 1 |
     *        ===|=======|
     *   this  0 | 0 | 1 |
     *    bit ---|-------|
     *  value  1 | 1 | 0 |
     *        ============
     * </pre>
     *
     * @param arg bit string argument
     * @return this {@code BitString} with the results of the operation
     */
    public BitString xor(BitString arg) {
        iXor(0, Math.min(this.length(), arg.length()), arg, 0);
        return this;
    }
    
    /**
     * Performs a logical <b>XOR</b> of a Field of this {@code BitString} with a
     * Field of the specified bit string (arg).
     * 
     * The length of the operation is equal to the smaller of the length of this
     * Field or the length of the specified Field.
     * <p>
     * The bits in this field are modified according to the following
     * logic table. For each {@code ONE} bit in the argument field, the
     * corresponding bit in this field is flipped, otherwise,
     * the bit is left unchanged.
     * 
     * <pre>
     *            arg bit
     *             value
     *           | 0 | 1 |
     *        ===|=======|
     *   this  0 | 0 | 1 |
     *    bit ---|-------|
     *  value  1 | 1 | 0 |
     *        ============
     * </pre>
     * 
     * @param thisField Field of this {@code BitString}
     * @param arg       bit string argument
     * @param argField  Field of the bit string argument
     * @return this {@code BitString} with the results of the operation
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisField.offset() > 0 && thisField.offset() >= this.length()}
     *                                         or
     *                                         {@code argField.offset() > 0 && argField.offset() >= arg.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisField.length() > this.length() - thisField.offset()}
     *                                         or
     *                                         {@code argField.length() > arg.length() - argField.offset()}
     */
    public BitString xor(Field thisField, BitString arg, Field argField) {
        final int thisOffset = thisField.offset();
        final int thisLength = thisField.length(this);
        final int argOffset = argField.offset();
        final int argLength = argField.length(arg);
        checkThisOffset(thisOffset);
        checkThisLength(thisOffset, thisLength);
        arg.checkArgOffset(argOffset);
        arg.checkArgLength(argOffset, argLength);
        iXor(thisOffset, Math.min(thisLength, argLength), arg, argOffset);
        return this;
    }
    
    /**
     * Performs a logical <b>XNOR</b> of this {@code BitString} with the specified
     * bit string (arg).
     * 
     * The length of the operation is equal to the smaller of the length of this
     * {@code BitString} or the length of the specified bit string.
     * <p>
     * The bits in this {@code BitString} are modified according to the following
     * logic table. For each {@code ONE} bit in the argument, the
     * corresponding bit in this {@code BitString} is left unchanged, otherwise,
     * the bit is flipped. This operation is the complement of the <b>XOR</b> operation.
     * 
     * <pre>
     *            arg bit
     *             value
     *           | 0 | 1 |
     *        ===|=======|
     *   this  0 | 1 | 0 |
     *    bit ---|-------|
     *  value  1 | 0 | 1 |
     *        ============
     * </pre>
     *
     * @param arg bit string argument
     * @return this {@code BitString} with the results of the operation
     */
    public BitString xnor(BitString arg) {
        iXnor(0, Math.min(this.length(), arg.length()), arg, 0);
        return this;
    }
    
    /**
     * Performs a logical <b>XNOR</b> of a Field of this {@code BitString} with a
     * Field of the specified bit string (arg).
     * 
     * The length of the operation is equal to the smaller of the length of this
     * Field or the length of the specified Field.
     * <p>
     * The bits in this field are modified according to the following
     * logic table. For each {@code ONE} bit in the argument field, the
     * corresponding bit in this field is left unchanged, otherwise,
     * the bit is flipped. This operation is the complement of the <b>XOR</b> operation.
     * 
     * <pre>
     *            arg bit
     *             value
     *           | 0 | 1 |
     *        ===|=======|
     *   this  0 | 1 | 0 |
     *    bit ---|-------|
     *  value  1 | 0 | 1 |
     *        ============
     * </pre>
     * 
     * @param thisField Field of this {@code BitString}
     * @param arg       bit string argument
     * @param argField  Field of the bit string argument
     * @return this {@code BitString} with the results of the operation
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisField.offset() > 0 && thisField.offset() >= this.length()}
     *                                         or
     *                                         {@code argField.offset() > 0 && argField.offset() >= arg.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisField.length() > this.length() - thisField.offset()}
     *                                         or
     *                                         {@code argField.length() > arg.length() - argField.offset()}
     */
    public BitString xnor(Field thisField, BitString arg, Field argField) {
        final int thisOffset = thisField.offset();
        final int thisLength = thisField.length(this);
        final int argOffset = argField.offset();
        final int argLength = argField.length(arg);
        checkThisOffset(thisOffset);
        checkThisLength(thisOffset, thisLength);
        arg.checkArgOffset(argOffset);
        arg.checkArgLength(argOffset, argLength);
        iXnor(thisOffset, Math.min(thisLength, argLength), arg, argOffset);
        return this;
    }
    
    /**
     * Performs the specified binary bitwise operation (op) of this
     * {@code BitString} with the specified bit string (arg).
     * 
     * The length of the operation is equal to the smaller of the length of this
     * {@code BitString} or the length of the specified bit string.
     * <p>
     * The direction of the operation is either Left-to-right (LTR/RIGHT) or
     * right-to-left (RTL/LEFT) as specified by the parameter 'direction'. if
     * right-to-left, both this {@code BitString} and the argument bit string are
     * processed from the end of the bit string towards the front of the bit string.
     * 
     * @param op        the binary bitwise operation to be performed
     * @param direction the direction of the operation (LTR or RTL)
     * @param arg       bit string argument
     * @return this {@code BitString} with the results of the operation
     */
    public BitString op(BinaryOp op, Direction direction, BitString arg) {
        iOp(op, direction, 0, this.length(), arg, 0, arg.length());
        return this;
    }
    
    /**
     * Performs the specified binary bitwise operation (op) of this
     * {@code BitString} with the specified bit string (arg).
     * 
     * The length of the operation is equal to the the length of this
     * {@code BitString}. If the length of the argument bit string is shorter, it is
     * padded, with the 'pad' parameter, to make it equal in length.
     * <p>
     * The direction of the operation is either Left-to-right (LTR/RIGHT) or
     * right-to-left (RTL/LEFT) as specified by the parameter 'direction'. if
     * right-to-left, both this {@code BitString} and the argument bit string are
     * processed from the end of the bit string towards the front of the bit string.
     * The direction also determines which end of the argument bit string is padded
     * if necessary. If the direction is left-to-right, the argument string is
     * padded on the right, otherwise, it is padded on the left.
     * 
     * @param op        the binary bitwise operation to be performed
     * @param direction the direction of the operation (LTR or RTL)
     * @param arg       bit string argument
     * @param pad       the bit padded onto the argument bit string if necessary
     * @return this {@code BitString} with the results of the operation
     */
    public BitString op(BinaryOp op, Direction direction, BitString arg, boolean pad) {
        iOp(op, direction, 0, this.length(), arg, 0, arg.length(), pad);
        return this;
    }

    /**
     * Performs the specified binary bitwise operation (op) of a substring of this
     * {@code BitString} with a substring of the specified bit string (arg).
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * has a length of 'thisLength'.
     * 
     * The substring argument starts at offset 'argOffset' of the specified bit
     * string and has a length of 'argLength'.
     * 
     * The length of the operation is equal to the smaller of the length of this
     * substring or the length of the substring argument.
     * <p>
     * The direction of the operation is either Left-to-right (LTR/RIGHT) or
     * right-to-left (RTL/LEFT) as specified by the parameter 'direction'. if
     * right-to-left, both this sinstring and the argument substring are processed
     * from the end of the substring towards the front of the substring.
     * 
     * @param op         the binary bitwise operation to be performed
     * @param direction  the direction of the operation (LTR or RTL)
     * @param thisOffset the start of this substring
     * @param thisLength the length of this substring
     * @param arg        bit string argument
     * @param argOffset  the start of the argument substring
     * @param argLength  the length of the argument substring
     * @return this {@code BitString} with the results of the operation
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code argOffset < 0 || argOffset > 0 && argOffset >= arg.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisLength < 0 || thisLength > this.length() - thisOffset}
     *                                         or
     *                                         {@code argLength < 0 || argLength > arg.length() - argOffset}
     */
    public BitString op(BinaryOp op, Direction direction,
            int thisOffset, int thisLength,
            BitString arg, int argOffset, int argLength) {
        checkThisOffset(thisOffset);
        checkThisLength(thisOffset, thisLength);
        arg.checkArgOffset(argOffset);
        arg.checkArgLength(argOffset, argLength);
        iOp(op, direction, thisOffset, thisLength, arg, argOffset, argLength);
        return this;
    }
    
    /**
     * Performs the specified binary bitwise operation (op) of a substring of this
     * {@code BitString} with a substring of the specified bit string (arg).
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * has a length of 'thisLength'.
     * 
     * The substring argument starts at offset 'argOffset' of the specified bit
     * string and has a length of 'argLength'.
     * 
     * The length of the operation is equal to the the length of this substring. If
     * the length of the argument substring is shorter, it is padded, with the 'pad'
     * parameter, to make it equal in length.
     * <p>
     * The direction of the operation is either Left-to-right (LTR/RIGHT) or
     * right-to-left (RTL/LEFT) as specified by the parameter 'direction'. if
     * right-to-left, both this sunstring and the argument substring are processed
     * from the end of the substring towards the front of the substring.
     * 
     * @param op         the binary bitwise operation to be performed
     * @param direction  the direction of the operation (LTR or RTL)
     * @param thisOffset the start of this substring
     * @param thisLength the length of this substring
     * @param arg        bit string argument
     * @param argOffset  the start of the argument substring
     * @param argLength  the length of the argument substring
     * @param pad        the bit padded onto the argument substring if necessary
     * @return this {@code BitString} with the results of the operation
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code argOffset < 0 || argOffset > 0 && argOffset >= arg.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisLength < 0 || thisLength > this.length() - thisOffset}
     *                                         or
     *                                         {@code argLength < 0 || argLength > arg.length() - argOffset}
     */
    public BitString op(BinaryOp op, Direction direction,
            int thisOffset, int thisLength,
            BitString arg, int argOffset, int argLength, boolean pad) {
        checkThisOffset(thisOffset);
        checkThisLength(thisOffset, thisLength);
        arg.checkArgOffset(argOffset);
        arg.checkArgLength(argOffset, argLength);
        iOp(op, direction, thisOffset, thisLength, arg, argOffset, argLength, pad);
        return this;
    }
    
    /**
     * Performs the specified binary bitwise operation (op) of a Field of this
     * {@code BitString} with a Field of the specified bit string (arg).
     * 
     * The length of the operation is equal to the smaller of the length of this
     * Field or the length of the specified Field.
     * <p>
     * The direction of the operation is either Left-to-right (LTR/RIGHT) or
     * right-to-left (RTL/LEFT) as specified by the parameter 'direction'. if
     * right-to-left, both this Field and the argument Field are processed from the
     * end of the Field towards the front of the Field.
     * 
     * @param op        the binary bitwise operation to be performed
     * @param direction the direction of the operation (LTR or RTL)
     * @param thisField Field of this {@code BitString}
     * @param arg       bit string argument
     * @param argField  Field of the bit string argument
     * @return this {@code BitString} with the results of the operation
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisField.offset() > 0 && thisField.offset() >= this.length()}
     *                                         or
     *                                         {@code argField.offset() > 0 && argField.offset() >= arg.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisField.length() > this.length() - thisField.offset()}
     *                                         or
     *                                         {@code argField.length() > arg.length() - argField.offset()}
     */
    public BitString op(BinaryOp op, Direction direction,
            Field thisField, BitString arg, Field argField) {
        return op(op, direction,
                thisField.offset(), thisField.length(this),
                arg, argField.offset(), argField.length(arg));
    }
    
    /**
     * Performs the specified binary bitwise operation (op) of a Field of this
     * {@code BitString} with a Field of the specified bit string (arg).
     * 
     * The length of the operation is equal to the the length of this Field. If the
     * length of the argument Field is shorter, it is padded, with the 'pad'
     * parameter, to make it equal in length.
     * <p>
     * The direction of the operation is either Left-to-right (LTR/RIGHT) or
     * right-to-left (RTL/LEFT) as specified by the parameter 'direction'. if
     * right-to-left, both this Field and the argument Field are processed from the
     * end of the Field towards the front of the Field.
     * 
     * @param op        the binary bitwise operation to be performed
     * @param direction the direction of the operation (LTR or RTL)
     * @param thisField Field of this {@code BitString}
     * @param arg       bit string argument
     * @param argField  Field of the bit string argument
     * @param pad       the bit padded onto the argument Field if necessary
     * @return this {@code BitString} with the results of the operation
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisField.offset() > 0 && thisField.offset() >= this.length()}
     *                                         or
     *                                         {@code argField.offset() > 0 && argField.offset() >= arg.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisField.length() > this.length() - thisField.offset()}
     *                                         or
     *                                         {@code argField.length() > arg.length() - argField.offset()}
     */
    public BitString op(BinaryOp op, Direction direction,
            Field thisField, BitString arg, Field argField, boolean pad) {
        return op(op, direction,
                thisField.offset(), thisField.length(this),
                arg, argField.offset(), argField.length(arg), pad);
    }

    /**
     * Returns {@code true} if this {@code BitString} and the specified object (obj)
     * are equal. This {@code BitString} and the object are equal if and only if the
     * object is a {@code BitString} that has the the same length as this
     * {@code BitString} and has the same set of bits set to {@code ONE} as this
     * {@code BitString}. That is the following comparison is true
     * for every nonnegative {@code int} index {@code k}
     * less than the length of the comparison,
     * <pre>
     * this.getBit(k) == ((BitString) obj).getBit(k)
     * </pre>
     *
     * @param obj the object to compare against
     * @return {@code true} if this {@code BitString} and the object are equal
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof BitString)) return false;
        return equals((BitString)obj);
    }
    
    /**
     * Returns {@code true} if this {@code BitString} and the specified bit string
     * (that) are equal. The two bit strings are equal if and only if both bit
     * strings have the the same length, and both bit strings have the same set of
     * bits set to {@code ONE}. That is, for every nonnegative {@code int} offset
     * {@code k} less than the length of the comparison,
     * 
     * <pre>
     * this.getBit(k) == that.getBit(k)
     * </pre>
     * 
     * must be true.
     *
     * @param that the bit string to compare against
     * @return {@code true} if this {@code BitString} and that bit string are equal
     */
    public boolean equals(BitString that) {
        if (that == null) return false;
        if (this == that) return true;
        if (this.length() != that.length()) return false;
        return iEquals(0, this.length(), that, 0);
    }
    
    /**
     * Returns {@code true} if a substring of this {@code BitString} and a substring
     * of the specified bit string (that) are equal. The two substrings are equal if
     * and only if both substrings have the the same length, and both substrings have
     * the same set of bits set to {@code ONE}. That is, for every nonnegative
     * {@code int} offset {@code k} less than the length of the comparison,
     * 
     * <pre>
     * this.getBit(k, thisOffset, thisLength) == that.getBit(k, thatOffset, thatLength)
     * </pre>
     * 
     * must be true.
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * has a length of 'thisLength'.
     * 
     * That substring starts at offset 'thatOffset' of the specified bit string and
     * has a length of 'thatLength'.
     * 
     * @param thisOffset the start of this substring
     * @param thisLength the length of this substring
     * @param that       the bit string to compare against
     * @param thatOffset the start of the that substring
     * @param thatLength the length of the that substring
     * @return {@code true} if this substring and that substring are equal
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code thatOffset < 0 || thatOffset > 0 && thatOffset >= that.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisLength < 0 || thisLength > this.length() - thisOffset}
     *                                         or
     *                                         {@code thatLength < 0 || thatLength > that.length() - thatOffset}
     */
    public boolean equals(int thisOffset, int thisLength, BitString that, int thatOffset, int thatLength) {
        checkThisOffset(thisOffset);
        checkThisLength(thisOffset, thisLength);
        if (that == null) return false;
        that.checkArgOffset(thatOffset);
        that.checkArgLength(thatOffset, thatLength);
        if (thisLength != thatLength) return false;
        return iEquals(thisOffset, thisLength, that, thatOffset);
    }
    
    /**
     * Returns {@code true} if a Field of this {@code BitString} and a Field
     * of the specified bit string (that) are equal. The two fields are equal if
     * and only if both fields have the the same length, and both fields have
     * the same set of bits set to {@code ONE}. That is, for every nonnegative
     * {@code int} offset {@code k} less than the length of the comparison,
     * 
     * <pre>
     * this.getBit(k, thisField) == that.getBit(k, thatField)
     * </pre>
     * 
     * must be true.
     * 
     * @param thisField a Field of this {@code BitString}
     * @param that       the bit string to compare against
     * @param thatField a Field of that bit string
     * @return {@code true} if this field and that field are equal
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisField.offset() > 0 && thisField.offset() >= this.length()}
     *                                         or
     *                                         {@code thatField.offset() > 0 && thatField.offset() >= that.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisField.length() > this.length() - thisField.offset()}
     *                                         or
     *                                         {@code thatField.length() > that.length() - thatField.offset()}
     */
    public boolean equals(Field thisField, BitString that, Field thatField) {
        return equals(thisField.offset(), thisField.length(this), that, thatField.offset(), thatField.length(that));
    }
    
    /**
     * Returns {@code true} if this {@code BitString} intersects with the specified
     * bit string (that). The two bit strings intersect if they have any bits, at
     * the same offset, that are set to {@code ONE}.
     *
     * @param that bit string
     * @return {@code true} if this {@code BitString} intersects with the specified
     *         bit string
     */
    public boolean intersects(BitString that) {
        return iIntersects(0, Math.min(this.length(), that.length()), that, 0);
    }
    
    /**
     * Returns {@code true} if a substring of this {@code BitString} intersects with
     * a substring of the specified bit string (that). The two bit substrings
     * intersect if they have any bits, at the same offset, that are set to
     * {@code ONE}.
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * has a length of 'thisLength'.
     * 
     * That substring starts at offset 'thatOffset' of the specified bit string and
     * has a length of 'thatLength'.
     * 
     * @param thisOffset the start of this substring
     * @param thisLength the length of this substring
     * @param that       that bit string
     * @param thatOffset the start of that substring
     * @param thatLength the length of that substring
     * @return {@code true} if this substring intersects with that substring
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code thatOffset < 0 || thatOffset > 0 && thatOffset >= that.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisLength < 0 || thisLength > this.length() - thisOffset}
     *                                         or
     *                                         {@code thatLength < 0 || thatLength > that.length() - thatOffset}
     */
    public boolean intersects(int thisOffset, int thisLength, BitString that, int thatOffset, int thatLength) {
        checkThisOffset(thisOffset);
        checkThisLength(thisOffset, thisLength);
        that.checkArgOffset(thatOffset);
        that.checkArgLength(thatOffset, thatLength);
        return iIntersects(thisOffset, Math.min(thisLength, thatLength), that, thatOffset);
    }
    
    /**
     * Returns {@code true} if a Field of this {@code BitString} intersects with a
     * Field of the specified bit string (that). The fields intersect if they have
     * any bits, at the same offset, that are set to {@code ONE}.
     * 
     * @param thisField a Field of this {@code BitString}
     * @param that      that bit string
     * @param thatField a Field of that bit string
     * @return {@code true} if this field intersects with that substring
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisField.offset() > 0 && thisField.offset() >= this.length()}
     *                                         or
     *                                         {@code thatField.offset() > 0 && thatField.offset() >= that.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisField.length() > this.length() - thisField.offset()}
     *                                         or
     *                                         {@code thatField.length() > that.length() - thatField.offset()}
     */
    public boolean intersects(Field thisField, BitString that, Field thatField) {
        return intersects(thisField.offset(), thisField.length(this), that, thatField.offset(), thatField.length(that));
    }
    
    /**
     * Returns the number of {@code ONES} in this {@code BitString}.
     *
     * @return the number of {@code ONES} in this {@code BitString}
     */
    public int numberOfOnes() {
        return iNumberOf(ONE, 0, length());
    }
    
    /**
     * Returns the number of {@code ONES} in the specified Field of this
     * {@code BitString}.
     * 
     * @param field a Field of this {@code BitString}
     * @return the number of {@code ONES} in the specified field
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code field.offset() > 0 && field.offset() >= length()}
     * @throws IllegalArgumentException        if
     *                                         {@code field.length() > length() - field.offset()}
     */
    public int numberOfOnes(Field field) {
        final int offset = field.offset();
        final int length = field.length(this);
        checkThisOffset(offset);
        checkThisLength(offset, length);
        return iNumberOf(ONE, offset, length);
    }
    
    /**
     * Returns the number of {@code ZEROS} in this {@code BitString}.
     *
     * @return the number of {@code ZEROS} in this {@code BitString}
     */
    public int numberOfZeros() {
        return iNumberOf(ZERO, 0, length());
    }
    
    /**
     * Returns the number of {@code ZEROS} in the specified Field of this
     * {@code BitString}.
     * 
     * @param field a Field of this {@code BitString}
     * @return the number of {@code ZEROS} in the specified field
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code field.offset() > 0 && field.offset() >= length()}
     * @throws IllegalArgumentException        if
     *                                         {@code field.length() > length() - field.offset()}
     */
    public int numberOfZeros(Field field) {
        final int offset = field.offset();
        final int length = field.length(this);
        checkThisOffset(offset);
        checkThisLength(offset, length);
        return iNumberOf(ZERO, offset, length);        
    }
    
    /**
     * Returns the number of leading {@code ONES} of this {@code BitString}.
     * 
     * @return the number of leading {@code ONES} of this {@code BitString}
     */
    public int numberOfLeadingOnes() {
        return iNumberOfLeadingOnes(0, length());
    }
    
    /**
     * Returns the number of leading {@code ONES} of the specified Field of this
     * {@code BitString}.
     * 
     * @param field a Field of this {@code BitString}
     * @return the number of leading {@code ONES} of the field
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code field.offset() > 0 && field.offset() >= length()}
     * @throws IllegalArgumentException        if
     *                                         {@code field.length() > length() - field.offset()}
     */
    public int numberOfLeadingOnes(Field field) {
        final int offset = field.offset();
        final int length = field.length(this);
        checkThisOffset(offset);
        checkThisLength(offset, length);
        return iNumberOfLeadingOnes(offset, length);
    }
    
    /**
     * Returns the number of leading {@code ZEROS} of this {@code BitString}.
     * 
     * @return the number of leading {@code ZEROS} of this {@code BitString}
     */
    public int numberOfLeadingZeros() {
        return iNumberOfLeadingZeros(0, length());
    }
    
    /**
     * Returns the number of leading {@code ZEROS} of the specified Field of this
     * {@code BitString}.
     * 
     * @param field a Field of this {@code BitString}
     * @return the number of leading {@code ZEROS} of the field
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code field.offset() > 0 && field.offset() >= length()}
     * @throws IllegalArgumentException        if
     *                                         {@code field.length() > length() - field.offset()}
     */
    public int numberOfLeadingZeros(Field field) {
        final int offset = field.offset();
        final int length = field.length(this);
        checkThisOffset(offset);
        checkThisLength(offset, length);
        return iNumberOfLeadingZeros(offset, length);
    }
    
    /**
     * Returns the number of trailing {@code ONES} of this {@code BitString}.
     * 
     * @return the number of trailing {@code ONES} of this {@code BitString}
     */
    public int numberOfTrailingOnes() {
         return iNumberOfTrailingOnes(0, length());
    }
    
    /**
     * Returns the number of trailing {@code ONES} of the specified Field of this
     * {@code BitString}.
     * 
     * @param field a Field of this {@code BitString}
     * @return the number of trailing {@code ONES} of the field
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code field.offset() > 0 && field.offset() >= length()}
     * @throws IllegalArgumentException        if
     *                                         {@code field.length() > length() - field.offset()}
     */
    public int numberOfTrailingOnes(Field field) {
        final int offset = field.offset();
        final int length = field.length(this);
        checkThisOffset(offset);
        checkThisLength(offset, length);
        return iNumberOfTrailingOnes(offset, length);
    }
    
    /**
     * Returns the number of trailing {@code ZEROS} of this {@code BitString}.
     * 
     * @return the number of trailing {@code ZEROS} of this {@code BitString}
     */
    public int numberOfTrailingZeros() {
        return iNumberOfTrailingZeros(0, length());
    }
    
    /**
     * Returns the number of trailing {@code ZEROS} of the specified Field of this
     * {@code BitString}.
     * 
     * @param field a Field of this {@code BitString}
     * @return the number of trailing {@code ZEROS} of the field
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code field.offset() > 0 && field.offset() >= length()}
     * @throws IllegalArgumentException        if
     *                                         {@code field.length() > length() - field.offset()}
     */
    public int numberOfTrailingZeros(Field field) {
        final int offset = field.offset();
        final int length = field.length(this);
        checkThisOffset(offset);
        checkThisLength(offset, length);
        return iNumberOfTrailingZeros(offset, length);
    }
    
    /**
     * Returns the number of bits in this {@code BitString} that have the same value
     * as the specified bit.
     *
     * @param bit the bit being counted
     * @return the number of bits in this {@code BitString} that have the same value
     *         of the specified bit
     */
    public int numberOf(boolean bit) {
        return iNumberOf(bit, 0, length());
    }
    
    /**
     * Returns the number of bits in the specified substring of this
     * {@code BitString} that have the same value as the specified bit.
     * 
     * The specified substring starts at offset 'offset' of this {@code BitString}
     * and has a length of 'length'.
     * 
     * @param bit    the bit being counted
     * @param offset the start of the substring
     * @param length the length of the substring
     * @return the number of bits in this substring that have the same value of the
     *         specified bit
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     * @throws IllegalArgumentException        if
     *                                         {@code length < 0 || length > length() - offset}
     */
    public int numberOf(boolean bit, int offset, int length) {
        checkThisOffset(offset);
        checkThisLength(offset, length);
        return iNumberOf(bit, offset, length);
    }
    
    /**
     * Returns the number of {bits in the specified Field of this {@code BitString}
     * that have the same value as the specified bit.
     * 
     * @param bit   the bit being counted
     * @param field a Field of this {@code BitString}
     * @return the number of bits in this Field that have the same value of the
     *         specified bit
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code field.offset() > 0 && field.offset() >= length()}
     * @throws IllegalArgumentException        if
     *                                         {@code field.length() > length() - field.offset()}
     */
    public int numberOf(boolean bit, Field field) {
        return numberOf(bit, field.offset(), field.length());
    }
    
    /**
     * Returns the number of leading or trailing bits, as specified by position, in
     * this {@code BitString}, that have the same value of the specified bit.
     *
     * @param position the position (LEADING/TRAILING) of the bits being coounted
     * @param bit      the bit being counted
     * @return the number of leading or trailing bits in this {@code BitString} that
     *         have the same value of the specified bit
     */
    public int numberOf(Position position, boolean bit) {
        return iNumberOf(position, bit, 0, length());
    }
    
    /**
     * Returns the number of leading or trailing bits, as specified by position, in
     * the specified substring of this {@code BitString}, that have the same value
     * as the specified bit.
     * 
     * The specified substring starts at offset 'offset' of this {@code BitString}
     * and has a length of 'length'.
     * 
     * @param position the position (LEADING/TRAILING) of the bits being coounted
     * @param bit      the bit being counted
     * @param offset   the start of the substring
     * @param length   the length of the substring
     * @return the number of leading or trailing bits, in this substring, that have
     *         the same value of the specified bit
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     * @throws IllegalArgumentException        if
     *                                         {@code length < 0 || length > length() - offset}
     */
    public int numberOf(Position position, boolean bit, int offset, int length) {
        checkThisOffset(offset);
        checkThisLength(offset, length);
        return iNumberOf(position, bit, offset, length);
    }
    
    /**
     * Returns the number of leading or trailing bits, as specified by position, in
     * the specified Field of this {@code BitString}, that have the same value as
     * the specified bit.
     * 
     * @param position the position (LEADING/TRAILING) of the bits being coounted
     * @param bit      the bit being counted
     * @param field    a Field of this {@code BitString}
     * @return the number of leading or trailing bits, in this Field, that have the
     *         same value of the specified bit
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code field.offset() > 0 && field.offset() >= length()}
     * @throws IllegalArgumentException        if
     *                                         {@code field.length() > length() - field.offset()}
     */
    public int numberOf(Position position, boolean bit, Field field) {
        return numberOf(position, bit, field.offset(), field.length());
    }
    
    /**
     * Returns the offset of the first bit that is set to {@code ONE} that occurs
     * within this {@code BitString}. If no such bit exists, -1 is returned. The
     * returned offset is relative to the start of this {@code BitString}.
     * 
     * @return the offset of the first {@code ONE} bit or -1 if no bits are set to
     *         {@code ONE}
     */
    public int offsetOfFirstOne() {
        return iOffsetOfFirstOne(0, length());
    }
    
    /**
     * Returns the offset of the first bit that is set to {@code ONE} that occurs
     * within the specified Field of this {@code BitString}. If no such bit exists,
     * -1 is returned. The returned offset is relative to the start of the field.
     * 
     * @param field a Field of this {@code BitString}
     * @return the offset of the first {@code ONE} bit or -1 if no bits are set to
     *         {@code ONE}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code field.offset() > 0 && field.offset() >= length()}
     * @throws IllegalArgumentException        if
     *                                         {@code field.length() > length() - field.offset()}
     */
    public int offsetOfFirstOne(Field field) {
        final int offset = field.offset();
        final int length = field.length(this);
        checkThisOffset(offset);
        checkThisLength(offset, length);
        return iOffsetOfFirstOne(offset, length);
    }
    
    /**
     * Returns the offset of the first bit that is set to {@code ZERO} that occurs
     * within this {@code BitString}. If no such bit exists, -1 is returned. The
     * returned offset is relative to the start of this {@code BitString}.
     * 
     * @return the offset of the first {@code ZERO} bit or -1 if no bits are set to
     *         {@code ZERO}
     */
    public int offsetOfFirstZero() {
        return iOffsetOfFirstZero(0, length());
    }
    
    /**
     * Returns the offset of the first bit that is set to {@code ZERO} that occurs
     * within the specified Field of this {@code BitString}. If no such bit exists,
     * -1 is returned. The returned offset is relative to the start of the field.
     * 
     * @param field a Field of this {@code BitString}
     * @return the offset of the first {@code ZERO} bit or -1 if no bits are set to
     *         {@code ZERO}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code field.offset() > 0 && field.offset() >= length()}
     * @throws IllegalArgumentException        if
     *                                         {@code field.length() > length() - field.offset()}
     */
    public int offsetOfFirstZero(Field field) {
        final int offset = field.offset();
        final int length = field.length(this);
        checkThisOffset(offset);
        checkThisLength(offset, length);
        return iOffsetOfFirstZero(offset, length);
    }
    
    /**
     * Returns the offset of the last bit that is set to {@code ONE} that occurs
     * within this {@code BitString}. If no such bit exists, -1 is returned. The
     * returned offset is relative to the start of this {@code BitString}.
     * 
     * @return the offset of the last {@code ONE} bit or -1 if no bits are set to
     *         {@code ONE}
     */
    public int offsetOfLastOne() {
        return iOffsetOfLastOne(0, length());
    }
    
    /**
     * Returns the offset of the last bit that is set to {@code ONE} that occurs
     * within the specified Field of this {@code BitString}. If no such bit exists,
     * -1 is returned. The returned offset is relative to the start of the field.
     * 
     * @param field a Field of this {@code BitString}
     * @return the offset of the last {@code ONE} bit or -1 if no bits are set to
     *         {@code ONE}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code field.offset() > 0 && field.offset() >= length()}
     * @throws IllegalArgumentException        if
     *                                         {@code field.length() > length() - field.offset()}
     */
    public int offsetOfLastOne(Field field) {
        final int offset = field.offset();
        final int length = field.length(this);
        checkThisOffset(offset);
        checkThisLength(offset, length);
        return iOffsetOfLastOne(offset, length);
    }
    
    /**
     * Returns the offset of the last bit that is set to {@code ZERO} that occurs
     * within this {@code BitString}. If no such bit exists, -1 is returned. The
     * returned offset is relative to the start of this {@code BitString}.
     * 
     * @return the offset of the last {@code ZERO} bit or -1 if no bits are set to
     *         {@code ZERO}
     */
    public int offsetOfLastZero() {
        return iOffsetOfLastZero(0, length());
    }
    
    /**
     * Returns the offset of the last bit that is set to {@code ZERO} that occurs
     * within the specified Field of this {@code BitString}. If no such bit exists,
     * -1 is returned. The returned offset is relative to the start of the field.
     * 
     * @param field a Field of this {@code BitString}
     * @return the offset of the last {@code ZERO} bit or -1 if no bits are set to
     *         {@code ZERO}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code field.offset() > 0 && field.offset() >= length()}
     * @throws IllegalArgumentException        if
     *                                         {@code field.length() > length() - field.offset()}
     */
    public int offsetOfLastZero(Field field) {
        final int offset = field.offset();
        final int length = field.length(this);
        checkThisOffset(offset);
        checkThisLength(offset, length);
        return iOffsetOfLastZero(offset, length);
    }
    
    /**
     * Returns the offset of the first or last bit, as specified by position, in
     * this {@code BitString}, that has the same value as the specified bit. If no
     * such bit exists, -1 is returned. The returned offset is relative to the start
     * of this {@code BitString}.
     * 
     * @param position the position (FIRST/LAST) of the bit being searched for
     * @param bit      the bit being searched for
     * @return the offset of the first or last bit that has the same value as the
     *         specified bit or -1 if no such bit exits
     */
    public int offsetOf(Position position, boolean bit) {
        return iOffsetOf(position, bit, 0, length());
    }
    
    /**
     * Returns the offset of the first or last bit, as specified by position, in the
     * specified substring of this {@code BitString}, that has the same value as the
     * specified bit. If no such bit exists, -1 is returned. The returned offset is
     * relative to the start of this substring.
     * 
     * The specified substring starts at offset 'offset' of this {@code BitString}
     * and has a length of 'length'.
     * 
     * @param position the position (FIRST/LAST) of the bit being searched for
     * @param bit      the bit being searched for
     * @param offset   the start of the substring
     * @param length   the length of the substring
     * @return the offset of the first or last bit that has the same value as the
     *         specified bit or -1 if no such bit exits
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     * @throws IllegalArgumentException        if
     *                                         {@code length < 0 || length > length() - offset}
     */
    public int offsetOf(Position position, boolean bit, int offset, int length) {
        checkThisOffset(offset);
        checkThisLength(offset, length);
        return iOffsetOf(position, bit, offset, length);
    }
    
    /**
     * Returns the offset of the first or last bit, as specified by position, in the
     * specified Field of this {@code BitString}, that has the same value as the
     * specified bit. If no such bit exists, -1 is returned. The returned offset is
     * relative to the start of the field.
     * 
     * @param position the position (FIRST/LAST) of the bit being searched for
     * @param bit      the bit being searched for
     * @param field    a Field of this {@code BitString}
     * @return the offset of the first or last bit that has the same value as the
     *         specified bit or -1 if no such bit exits
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code field.offset() > 0 && field.offset() >= length()}
     * @throws IllegalArgumentException        if
     *                                         {@code field.length() > length() - field.offset()}
     */
    public int offsetOf(Position position, boolean bit, Field field) {
        return offsetOf(position, bit, field.offset(), field.length());
    }
    
    /**
     * Returns the offset of the next bit that is set to {@code ONE} that occurs on
     * or after the specified start offset within this {@code BitString}. If no such
     * bit exists, or {@code startOffset == length()}, -1 is returned. The start
     * offset and the returned offset are relative to the start of this
     * {@code BitString}.
     * 
     * @param startOffset the offset to start searching from (exclusive)
     * @return the offset of the next {@code ONE} bit or -1 if no bits are set to
     *         {@code ONE}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code startOffset < 0 || startOffset >= length()}
     */
    public int offsetOfNextOne(int startOffset) {
        checkRelativeOffset(startOffset, length());
        return iOffsetOfNextOne(startOffset, 0, length());
    }
    
    /**
     * Returns the offset of the next bit that is set to {@code ONE} that occurs on
     * or after the specified start offset within the specified Field of this
     * {@code BitString}. If no such bit exists, or
     * {@code startOffset == field.length()}, -1 is returned. The start offset and
     * the returned offset are relative to the start of the field.
     * 
     * @param startOffset the offset to start searching from (exclusive)
     * @param field       a Field of this {@code BitString}
     * @return the offset of the next {@code ONE} bit or -1 if no bits are set to
     *         {@code ONE}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code field.offset() > 0 && field.offset() >= length()}
     *                                         or
     *                                         {@code startOffset < 0 || startOffset >= field.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code field.length() > length() - field.offset()}
     */
    public int offsetOfNextOne(int startOffset, Field field) {
        final int offset = field.offset();
        final int length = field.length(this);
        checkThisOffset(offset);
        checkThisLength(offset, length);
        checkRelativeOffset(startOffset, length);
        return iOffsetOfNextOne(startOffset, offset, length);
    }
    
    /**
     * Returns the offset of the next bit that is set to {@code ZERO} that occurs on
     * or after the specified start offset within this {@code BitString}. If no such
     * bit exists, or {@code startOffset == length()}, -1 is returned. The start
     * offset and the returned offset are relative to the start of this
     * {@code BitString}.
     * 
     * @param startOffset the offset to start searching from (exclusive)
     * @return the offset of the next {@code ZERO} bit or -1 if no bits are set to
     *         {@code ZERO}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code startOffset < 0 || startOffset >= length()}
     */
    public int offsetOfNextZero(int startOffset) {
        if (startOffset == length()) return -1;
        checkRelativeOffset(startOffset, length());
        return iOffsetOfNextZero(startOffset, 0, length());
    }
    
    /**
     * Returns the offset of the next bit that is set to {@code ZERO} that occurs on
     * or after the specified start offset within the specified Field of this
     * {@code BitString}. If no such bit exists, or
     * {@code startOffset == field.length()}, -1 is returned. The start offset and
     * the returned offset are relative to the start of the field.
     * 
     * @param startOffset the offset to start searching from (exclusive)
     * @param field       a Field of this {@code BitString}
     * @return the offset of the next {@code ZERO} bit or -1 if no bits are set to
     *         {@code ZERO}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code field.offset() > 0 && field.offset() >= length()}
     *                                         or
     *                                         {@code startOffset < 0 || startOffset >= field.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code field.length() > length() - field.offset()}
     */
    public int offsetOfNextZero(int startOffset, Field field) {
        final int offset = field.offset();
        final int length = field.length(this);
        checkThisOffset(offset);
        checkThisLength(offset, length);
        checkRelativeOffset(startOffset, length);
        return iOffsetOfNextZero(startOffset, offset, length);
    }
    
    /**
     * Returns the offset of the previous bit that is set to {@code ONE} that occurs
     * on or before the specified start offset within this {@code BitString}. If no
     * such bit exists, or -1 is given as the start offset, -1 is returned. The
     * start offset and the returned offset are relative to the start of this
     * {@code BitString}.
     * 
     * @param startOffset the offset to start searching from (exclusive)
     * @return the offset of the previous {@code ONE} bit or -1 if no bits are set
     *         to {@code ONE}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code startOffset < 0 || startOffset > 0 && offset >= length()}
     */
    public int offsetOfPreviousOne(int startOffset) {
        checkRelativeOffset(startOffset, length());
        return iOffsetOfPreviousOne(startOffset, 0, length());
    }
    
    /**
     * Returns the offset of the previous bit that is set to {@code ONE} that occurs
     * on or before the specified start offset within the specified Field of this
     * {@code BitString}. If no such bit exists, or -1 is given as the start offset,
     * -1 is returned. The start offset and the returned offset are relative to the
     * start of the field.
     * 
     * @param startOffset the offset to start searching from (exclusive)
     * @param field       a Field of this {@code BitString}
     * @return the offset of the previous {@code ONE} bit or -1 if no bits are set
     *         to {@code ONE}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code field.offset() > 0 && field.offset() >= length()}
     *                                         or
     *                                         {@code startOffset < 0 || startOffset > 0 && startOffset >= field.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code field.length() > length() - field.offset()}
     */
    public int offsetOfPreviousOne(int startOffset, Field field) {
        final int offset = field.offset();
        final int length = field.length(this);
        checkThisOffset(offset);
        checkThisLength(offset, length);
        checkRelativeOffset(startOffset, length);
        return iOffsetOfPreviousOne(startOffset, offset, length);
    }
    
    /**
     * Returns the offset of the previous bit that is set to {@code ZERO} that occurs
     * on or before the specified start offset within this {@code BitString}. If no
     * such bit exists, or -1 is given as the start offset, -1 is returned. The
     * start offset and the returned offset are relative to the start of this
     * {@code BitString}.
     * 
     * @param startOffset the offset to start searching from (exclusive)
     * @return the offset of the previous {@code ZERO} bit or -1 if no bits are set
     *         to {@code ZERO}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code startOffset < 0 || startOffset > 0 && offset >= length()}
     */
    public int offsetOfPreviousZero(int startOffset) {
        checkRelativeOffset(startOffset, length());
        return iOffsetOfPreviousZero(startOffset, 0, length());
    }
    
    /**
     * Returns the offset of the previous bit that is set to {@code ZERO} that occurs
     * on or before the specified start offset within the specified Field of this
     * {@code BitString}. If no such bit exists, or -1 is given as the start offset,
     * -1 is returned. The start offset and the returned offset are relative to the
     * start of the field.
     * 
     * @param startOffset the offset to start searching from (exclusive)
     * @param field       a Field of this {@code BitString}
     * @return the offset of the previous {@code ZERO} bit or -1 if no bits are set
     *         to {@code ZERO}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code field.offset() > 0 && field.offset() >= length()}
     *                                         or
     *                                         {@code startOffset < 0 || startOffset > 0 && startOffset >= field.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code field.length() > length() - field.offset()}
     */
    public int offsetOfPreviousZero(int startOffset, Field field) {
        final int offset = field.offset();
        final int length = field.length(this);
        checkThisOffset(offset);
        checkThisLength(offset, length);
        checkRelativeOffset(startOffset, length);
        return iOffsetOfPreviousZero(startOffset, offset, length);
    }
    
    /**
     * Returns the offset of the next or previous bit, as specified by direction,
     * that has the same value as the specified bit and occurs after or before the
     * specified start offset within this {@code BitString}. If no such bit exists,
     * -1 is returned. The start offset and the returned offset are relative to the
     * start of this {@code BitString}.
     * 
     * @param direction   the direction (NEXT/PREVIOUS) of the bit being searched
     *                    for
     * @param bit         the bit being searched for
     * @param startOffset the offset to start searching from (exclusive)
     * @return the offset of the next or previous bit that has the same value as the
     *         specified bit or -1 if no such bit exits
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code startOffset < 0 || startOffset > 0 && offset >= length()}
     */
    public int offsetOf(Direction direction, boolean bit, int startOffset) {
        checkRelativeOffset(startOffset, length());
        return iOffsetOf(direction, bit, startOffset, 0, length());
    }
    
    /**
     * Returns the offset of the next or previous bit, as specified by direction,
     * that has the same value as the specified bit and occurs after or before the
     * specified start offset within the specified substring of this
     * {@code BitString}. If no such bit exists, -1 is returned. The start offset
     * and the returned offset are relative to the start of the substring.
     * 
     * The specified substring starts at offset 'offset' of this {@code BitString}
     * and has a length of 'length'.
     * 
     * @param direction   the direction (NEXT/PREVIOUS) of the bit being searched
     *                    for
     * @param bit         the bit being searched for
     * @param startOffset the offset to start searching from (exclusive)
     * @param offset      the start of the substring
     * @param length      the length of the substring
     * @return the offset of the next or previous bit that has the same value as the
     *         specified bit or -1 if no such bit exits
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     *                                         or
     *                                         {@code startOffset < 0 || startOffset > 0 && startOffset >= length}
     * @throws IllegalArgumentException        if
     *                                         {@code length < 0 || length > length() - offset}
     */
    public int offsetOf(Direction direction, boolean bit, int startOffset, int offset, int length) {
        checkThisOffset(offset);
        checkThisLength(offset, length);
        checkRelativeOffset(startOffset, length);
        return iOffsetOf(direction, bit, startOffset, offset, length);
    }
    
    /**
     * Returns the offset of the next or previous bit, as specified by direction,
     * that has the same value as the specified bit and occurs after or before the
     * specified start offset within the specified Field of this
     * {@code BitString}. If no such bit exists, -1 is returned. The start offset
     * and the returned offset are relative to the start of the field.
     * 
     * @param direction the direction (NEXT/PREVIOUS) of the bit being searched for
     * @param bit      the bit being searched for
     * @param startOffset the offset to start searching from (exclusive)
     * @param field       a Field of this {@code BitString}
     * @return the offset of the previous {@code ZERO} bit or -1 if no bits are set
     *         to {@code ZERO}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code field.offset() > 0 && field.offset() >= length()}
     *                                         or
     *                                         {@code startOffset < 0 || startOffset > 0 && startOffset >= field.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code field.length() > length() - field.offset()}
     */
    public int offsetOf(Direction direction, boolean bit, int startOffset, Field field) {
        return offsetOf(direction, bit, startOffset, field.offset(), field.length());
    }
    
    /**
     * Rotates this {@code BitString} either left or right, as specified by direction, by the specified number of bits (distance).
     * 
     * If distance is negative (including Integer.MIN_VALUE), a rotate in the
     * opposite direction than the specified direction is performed.
     * 
     * Any bits rotated out are rotated back into this bit string on the
     * opposite end.
     * 
     * @param direction the direction of the rotate (LEFT or RIGHT)
     * @param distance the number of bits to rotate
     * @return this {@code BitString}
     * 
     */
    public BitString rotate(Direction direction, int distance) {
        iRotate(direction, distance, 0, length());
        return this;
    }
    
    /**
     * Rotates a substring of this {@code BitString} either left or right, as specified by direction, by the specified number of
     * bits (distance).
     * 
     * If distance is negative (including Integer.MIN_VALUE), a rotate in the
     * opposite direction than the specified direction is performed.
     * 
     * The substring starts at offset 'offset' of this {@code BitString} and has a
     * length of 'length'.
     * 
     * Any bits rotated out on the left are rotated back into this {@code BitString}
     * on the right.
     * 
     * @param direction the direction of the rotate (LEFT or RIGHT)
     * @param distance  the number of bits to rotate
     * @param offset the start of this substring
     * @param length the length of this substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= this.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code length < 0 || length > this.length() - offset}
     */
    public BitString rotate(Direction direction, int distance, int offset, int length) {
        checkThisOffset(offset);
        checkThisLength(offset, length);
        iRotate(direction, distance, offset, length);
        return this;
    }
    
    /**
     * Rotates a Field of this {@code BitString} either left or right, as specified by direction, by the specified number of bits
     * (distance).
     * 
     * If distance is negative (including Integer.MIN_VALUE), a rotate in the
     * opposite direction than the specified direction is performed.
     * 
     * Any bits rotated out on the left are rotated back into this {@code BitString}
     * on the right.
     * 
     * @param direction the direction of the rotate (LEFT or RIGHT)
     * @param distance the number of bits to rotate
     * @param field a Field of this {@code BitString}
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code field.offset() > 0 && field.offset() >= this.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code field.length() > this.length() - field.offset()}
     */
    public BitString rotate(Direction direction, int distance, Field field) {
        return rotate(direction, distance, field.offset(), field.length(this));
    }
    
    /**
     * Rotates this {@code BitString} and the specified bit string (other) either left or right, as specified by direction, by
     * the specified number of bits (distance).
     * 
     * If distance is negative (including Integer.MIN_VALUE), a rotate in the
     * opposite direction than the specified direction is performed.
     * <p>
     * This bit string and the specified other bit string are treated as one bit
     * string. Any bits rotated out on the left of this bit string are rotated into
     * the other bit string on the right. Any bits rotated out of the other bit
     * string on the left are rotated back into this bit string on the right.
     * Conceptually, you can think of the other bit string positioned before this
     * bit string and being fed bits rotating out of this bit string. Or you can
     * think of the other bit string positioned after this bit string and feeding
     * this bit string bits as it's being rotated.
     * 
     * @param direction the direction of the rotate (LEFT or RIGHT)
     * @param distance number of bits to rotate
     * @param other the other bit string
     * @return this {@code BitString}
     */
    public BitString rotate(Direction direction, int distance, BitString other) {
        iRotate(direction, distance, 0, this.length(), other, 0, other.length());
        return this;
    }
    
    /**
     * Rotates a substring of this {@code BitString} and a substring of the
     * specified bit string (other) either left or right, as specified by direction, by the specified number of bits (nBits).
     * 
     * If distance is negative (including Integer.MIN_VALUE), a rotate in the
     * opposite direction than the specified direction is performed.
     * 
     * This {@code BitString} and the other bit string are modified by this
     * operation (see {@link #rotateLeft(int, BitString)} for details).
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * has a length of 'thisLength'.
     * 
     * The other substring starts at offset 'otherOffset' of the specified bit
     * string and has a length of 'otherLength'.
     * 
     * @param direction the direction of the rotate (LEFT or RIGHT)
     * @param distance       number of bits to rotate
     * @param thisOffset  the start of this substring
     * @param thisLength  the length of this substring
     * @param other       the other bit string
     * @param otherOffset the start of the other substring
     * @param otherLength the length of the other substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code otherOffset < 0 || otherOffset > 0 && otherOffset >= other.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisLength < 0 || thisLength > this.length() - thisOffset}
     *                                         or
     *                                         {@code otherLength < 0 || otherLength > other.length() - otherOffset}
     */   
    public BitString rotate(Direction direction, int distance,
            int thisOffset, int thisLength,
            BitString other, int otherOffset, int otherLength) {
        checkThisOffset(thisOffset);
        checkThisLength(thisOffset, thisLength);
        other.checkArgOffset(otherOffset);
        other.checkArgLength(otherOffset, otherLength);
        iRotate(direction, distance, thisOffset, thisLength, other, otherOffset, otherLength);
        return this;
    }
    
    /**
     * Rotates a Field of this {@code BitString} and a Field of the specified
     * bit string (other) either left or right, as specified by direction, by the specified number of bits (nBits).
     * 
     * If distance is negative (including Integer.MIN_VALUE), a rotate in the
     * opposite direction than the specified direction is performed.
     * 
     * This {@code BitString} and the other bit string are modified by this
     * operation (see {@link #rotateLeft(int, BitString)} for details).
     * 
     * @param direction the direction of the rotate (LEFT or RIGHT)
     * @param distance      number of bits to rotate
     * @param thisField  a Field of this {@code BitString}
     * @param other      the other bit string
     * @param otherField a Field of the other bit string
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisField.offset() > 0 && thisField.offset() >= this.length()}
     *                                         or
     *                                         {@code otherField.offset() > 0 && otherField.Offset() >= other.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisField.length() > this.length() - thisField.offset()}
     *                                         or
     *                                         {@code otherField.length() > other.length() - otherField.offset()}
     */
    public BitString rotate(Direction direction, int distance,
           Field thisField,
           BitString other, Field otherField) {
        return rotate(direction, distance, thisField.offset(), thisField.length(this), other, otherField.offset(), otherField.length(other));
    }
    
    /**
     * Rotates this {@code BitString} left the specified number of bits (nBits).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a rotateRight(|nBits|) is
     * performed instead of a rotateLeft.
     * 
     * Any bits rotated out on the left are rotated back into this bit string on the
     * right.
     * 
     * @param distance the number of bits to rotate
     * @return this {@code BitString}
     * 
     */
    public BitString rotateLeft(int distance) {
        iRotateLeft(distance, 0, length());
        return this;
    }
    
    /**
     * Rotates a Field of this {@code BitString} left the specified number of bits
     * (nBits).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a rotateRight(|nBits|...) is
     * performed instead of a rotateLeft.
     * 
     * Any bits rotated out on the left are rotated back into this {@code BitString}
     * on the right.
     * 
     * @param distance the number of bits to rotate
     * @param field a Field of this {@code BitString}
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code field.offset() > 0 && field.offset() >= this.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code field.length() > this.length() - field.offset()}
     */
    public BitString rotateLeft(int distance, Field field) {
        //return rotateLeft(distance, field.offset(), field.length(this));
        checkThisOffset(field.offset());
        checkThisLength(field.offset(), field.length(this));
        iRotateLeft(distance, field.offset(), field.length(this));
        return this;
    }
    
    /**
     * Rotates left this {@code BitString} and the specified bit string (other) by
     * the specified number of bits (nBits).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a rotateRight(|nBits|...)
     * is performed instead of a rotateLeft.
     * <p>
     * This bit string and the specified other bit string are treated as one bit
     * string. Any bits rotated out on the left of this bit string are rotated into
     * the other bit string on the right. Any bits rotated out of the other bit
     * string on the left are rotated back into this bit string on the right.
     * Conceptually, you can think of the other bit string positioned before this
     * bit string and being fed bits rotating out of this bit string. Or you can
     * think of the other bit string positioned after this bit string and feeding
     * this bit string bits as it's being rotated.
     * 
     * @param distance number of bits to rotate
     * @param other the other bit string
     * @return this {@code BitString}
     */
    public BitString rotateLeft(int distance, BitString other) {
        iRotateLeft(distance, 0, this.length(), other, 0, other.length());
        return this;
    }
    
    /**
     * Rotates left a Field of this {@code BitString} and a Field of the specified
     * bit string (other) by the specified number of bits (nBits).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a rotateRight(|nBits|...) is
     * performed instead of a rotateLeft.
     * 
     * This {@code BitString} and the other bit string are modified by this
     * operation (see {@link #rotateLeft(int, BitString)} for details).
     * 
     * @param distance      number of bits to rotate
     * @param thisField  a Field of this {@code BitString}
     * @param other      the other bit string
     * @param otherField a Field of the other bit string
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisField.offset() > 0 && thisField.offset() >= this.length()}
     *                                         or
     *                                         {@code otherField.offset() > 0 && otherField.Offset() >= other.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisField.length() > this.length() - thisField.offset()}
     *                                         or
     *                                         {@code otherField.length() > other.length() - otherField.offset()}
     */
    public BitString rotateLeft(int distance,
           Field thisField,
           BitString other, Field otherField) {
        //return rotateLeft(distance, thisField.offset(), thisField.length(this), other, otherField.offset(), otherField.length(other));
        checkThisOffset(thisField.offset());
        checkThisLength(thisField.offset(), thisField.length(this));
        other.checkArgOffset(otherField.offset());
        other.checkArgLength(otherField.offset(), otherField.length(other));
        iRotateLeft(distance, thisField.offset(), thisField.length(this), other, otherField.offset(), otherField.length(other));
        return this;
    }
    
    /**
     * Rotates this {@code BitString} right the specified number of bits (nBits).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a rotateLeft(|nBits|) is
     * performed instead of a rotateRight.
     * 
     * Any bits rotated out on the right are rotated back into this bit string on
     * the left.
     * 
     * @param distance the number of bits to rotate
     * @return this {@code BitString}
     * 
     */
    public BitString rotateRight(int distance) {
        iRotateRight(distance, 0, length());
        return this;
    }
    
    /**
     * Rotates a Field of this {@code BitString} right the specified number of bits
     * (nBits).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a rotateLeft(|nBits|...) is
     * performed instead of a rotateRight.
     * 
     * Any bits rotated out on the right are rotated back into this
     * {@code BitString} on the left.
     * 
     * @param distance the number of bits to rotate
     * @param field a Field of this {@code BitString}
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code field.offset() > 0 && field.offset() >= this.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code field.length() > this.length() - field.offset()}
     */
    public BitString rotateRight(int distance, Field field) {
        //return rotateRight(distance, field.offset(), field.length(this));
        checkThisOffset(field.offset());
        checkThisLength(field.offset(), field.length(this));
        iRotateRight(distance, field.offset(), field.length(this));
        return this;
    }
    
    /**
     * Rotates right this {@code BitString} and the specified bit string (other) by
     * the specified number of bits (nBits).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a rotateLeft(|nBits|...)
     * is performed instead of a rotateRight.
     * <p>
     * This bit string and the specified other bit string are treated as one bit
     * string. Any bits rotated out on the right of this bit string are rotated into
     * the other bit string on the left. Any bits rotated out of the other bit
     * string on the right are rotated back into this bit string on the left.
     * Conceptually, you can think of the other bit string positioned after this bit
     * string and being fed bits rotating out of this bit string. Or you can think
     * of the other bit string positioned before this bit string and feeding this
     * {@code BitString} bits as it's being rotated.
     * 
     * @param distance number of bits to rotate
     * @param other the other bit string
     * @return this {@code BitString}
     */
    public BitString rotateRight(int distance, BitString other) {
        iRotateRight(distance, 0, this.length(), other, 0, other.length());
        return this;
    }
    
    /**
     * Rotates right a Field of this {@code BitString} and a Field of the specified
     * bit string (other) by the specified number of bits (nBits).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a rotateLeft(|nBits|...) is
     * performed instead of a rotateRight.
     * 
     * This {@code BitString} and the other bit string are modified by this
     * operation (see {@link #rotateRight(int, BitString)} for details).
     * 
     * @param distance      number of bits to rotate
     * @param thisField  a Field of this {@code BitString}
     * @param other      the other bit string
     * @param otherField a Field of the other bit string
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisField.offset() > 0 && thisField.offset() >= this.length()}
     *                                         or
     *                                         {@code otherField.offset() > 0 && otherField.Offset() >= other.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisField.length() > this.length() - thisField.offset()}
     *                                         or
     *                                         {@code otherField.length() > other.length() - otherField.offset()}
     */
    public BitString rotateRight(int distance,
            Field thisField,
            BitString other, Field otherField) {
        //return rotateRight(distance, thisField.offset(), thisField.length(this), other, otherField.offset(), otherField.length(other));
        checkThisOffset(thisField.offset());
        checkThisLength(thisField.offset(), thisField.length(this));
        other.checkArgOffset(otherField.offset());
        other.checkArgLength(otherField.offset(), otherField.length(other));
        iRotateRight(distance, thisField.offset(), thisField.length(this), other, otherField.offset(), otherField.length(other));
        return this;
    }
    
    /**
     * Shifts this {@code BitString} either left or right, as specified by
     * direction, by the specified number of bits (distance), filling any vacated
     * positions by the specified fill value (fill).
     * 
     * If distance is negative (including Integer.MIN_VALUE), a shift in the
     * opposite direction than the specified direction is performed.
     * <p>
     * Any bits shifted out of this {@code BitString} are lost; any positions
     * vacated are filled by the specified fill value. If
     * {@code distance >= length()}, this {@code BitString} will be completely
     * filled by the specified fill value.
     * 
     * @param direction the direction of the shift (LEFT or RIGHT)
     * @param distance  the number of bits to shift
     * @param fill      the fill value
     * @return this {@code BitString}
     */
    public BitString shift(Direction direction, int distance, boolean fill) {
        iShift(direction, distance, fill, 0, this.length());
        return this;
    }
    
    /**
     * Shifts a substring of this {@code BitString} either left or right, as
     * specified by direction, by the specified number of bits (distance), filling
     * any vacated positions by the specified fill value (fill).
     * 
     * If distance is negative (including Integer.MIN_VALUE), a shift in the
     * opposite direction than the specified direction is performed.
     * 
     * The substring starts at offset 'offset' of this {@code BitString} and has a
     * length of 'length'.
     * <p>
     * Any bits shifted out of this substring are lost; any positions vacated are
     * filled by the specified fill value. If {@code distance >= length}, this
     * {@code BitString} will be completely filled by the specified fill value.
     * 
     * @param direction the direction of the shift (LEFT or RIGHT)
     * @param distance  the number of bits to shift
     * @param fill      the fill value
     * @param offset    the start of this substring
     * @param length    the length of this substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= this.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code length < 0 || length > this.length() - offset}
     */
    public BitString shift(Direction direction, int distance, boolean fill, int offset, int length) {
        checkThisOffset(offset);
        checkThisLength(offset, length);
        iShift(direction, distance, fill, offset, length);
        return this;
    }
    
    /**
     * Shifts a Field of this {@code BitString} either left or right, as specified
     * by direction, by the specified number of bits (distance), filling any vacated
     * positions by the specified fill value (fill).
     * 
     * If distance is negative (including Integer.MIN_VALUE), a shift in the
     * opposite direction than the specified direction is performed.
     * <p>
     * Any bits shifted out of this field are lost; any positions vacated are filled
     * by the specified fill value. If {@code distance >= field.length()}, this
     * {@code BitString} will be completely filled by the specified fill value.
     * 
     * @param direction the direction of the shift (LEFT or RIGHT)
     * @param distance  the number of bits to shift
     * @param fill      the fill value
     * @param field     a Field of this {@code BitString}
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code field.offset() > 0 && field.offset() >= this.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code field.length() > this.length() - field.offset()}
     */
    public BitString shift(Direction direction, int distance, boolean fill, Field field) {
        return shift(direction, distance, fill, field.offset(), field.length(this));
    }
    
    /**
     * Shifts this {@code BitString} and the specified bit string (other) either
     * left or right, as specified by direction, by the specified number of bits
     * (distance), filling any vacated positions by the specified fill value (fill).
     * 
     * If distance is negative (including Integer.MIN_VALUE), a shift in the
     * opposite direction than the specified direction is performed.
     * <p>
     * This {@code BitString} and the other bit string are treated as one bit string
     * where this {@code BitString} precedes the other bit string if shifting right,
     * or this {@code BitString} follows the other bit string if shifting left. Any
     * bits shifted out of the other bit string are lost. Any bits shifted out of
     * this {@code BitString} are shifted into the other bit string; any vacated
     * positions are filled by the fill value. If {@code distance >= this.length()},
     * this {@code BitString} will be completely filled by the fill value. if
     * {@code distance >= this.length() + other.length()}, both this
     * {@code BitString} and the other bit string will be completely filled by the
     * fill value.
     * 
     * @param direction the direction of the shift (LEFT or RIGHT)
     * @param distance  the number of bits to shift
     * @param fill      the fill value
     * @param other     the other bit string
     * @return this {@code BitString}
     */
    public BitString shift(Direction direction, int distance, boolean fill, BitString other) {
        iShift(direction, distance, fill, 0, this.length(), other, 0, other.length());
        return this;
    }
    
    /**
     * Shifts a substring of this {@code BitString} and a substring of the specified
     * bit string (other) either left or right, as specified by direction, by the
     * specified number of bits (distance), filling any vacated positions by the
     * specified fill value (fill).
     * 
     * If distance is negative (including Integer.MIN_VALUE), a shift in the
     * opposite direction than the specified direction is performed.
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * has a length of 'thisLength'.
     * 
     * The other substring starts at offset 'otherOffset' of the specified bit
     * string and has a length of 'otherLength'.
     * <p>
     * This substring and the other substring are treated as one bit string where
     * this substring precedes the other substring if shifting right, or this
     * substring follows the other substring if shifting left. Any bits shifted out
     * of the other substring are lost. Any bits shifted out of this substring are
     * shifted into the other substring; any vacated positions are filled by the
     * fill value. If {@code distance >= thisLength}, this substring will be
     * completely filled by the fill value. if
     * {@code distance >= thisLength + otherLength}, both this substring and the
     * other substring will be completely filled by the fill value.
     * 
     * @param direction   the direction of the shift (LEFT or RIGHT)
     * @param distance    the number of bits to shift
     * @param fill        the fill value
     * @param thisOffset  the start of this substring
     * @param thisLength  the length of this substring
     * @param other       the other bit string
     * @param otherOffset the start of the other substring
     * @param otherLength the length of the other substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code otherOffset < 0 || otherOffset > 0 && otherOffset >= other.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisLength < 0 || thisLength > this.length() - thisOffset}
     *                                         or
     *                                         {@code otherLength < 0 || otherLength > other.length() - otherOffset}
     */
    public BitString shift(Direction direction, int distance, boolean fill,
            int thisOffset, int thisLength,
            BitString other, int otherOffset, int otherLength) {
        checkThisOffset(thisOffset);
        checkThisLength(thisOffset, thisLength);
        other.checkArgOffset(otherOffset);
        other.checkArgLength(otherOffset, otherLength);
        iShift(direction, distance, fill, thisOffset, thisLength, other, otherOffset, otherLength);
        return this;
    }
    
    /**
     * Shifts a Field of this {@code BitString} and a Field of the specified bit
     * string (other) either left or right, as specified by direction, by the
     * specified number of bits (distance), filling any vacated positions by the
     * specified fill value (fill).
     * 
     * If distance is negative (including Integer.MIN_VALUE), a shift in the
     * opposite direction than the specified direction is performed.
     * <p>
     * This field and the other field are treated as one bit string where this field
     * precedes the other field if shifting right, or this field follows the other
     * field if shifting left. Any bits shifted out of the other field are lost. Any
     * bits shifted out of this field are shifted into the other field; any vacated
     * positions are filled by the fill value. If
     * {@code distance >= thisField.length()}, this field will be completely filled
     * by the fill value. if
     * {@code distance >= thisField.length() + otherField.length()}, both this field
     * and the other field will be completely filled by the fill value.
     * 
     * @param direction  the direction of the shift (LEFT or RIGHT)
     * @param distance   the number of bits to shift
     * @param fill       the fill value
     * @param thisField  a Field of this {@code BitString}
     * @param other      the other bit string
     * @param otherField a Field of the other substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisField.offset() > 0 && thisField.offset() >= this.length()}
     *                                         or
     *                                         {@code otherField.offset() > 0 && otherField.offset() >= other.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisField.length() > this.length() - thisField.offset()}
     *                                         or
     *                                         {@code otherField.length() > other.length() - otherField.offset()}
     */
    public BitString shift(Direction direction, int distance, boolean fill,
            Field thisField,
            BitString other, Field otherField) {
        return shift(direction, distance, fill,
                thisField.offset(), thisField.length(this),
                other, otherField.offset(), otherField.length(other));
    }

    /**
     * Shifts this {@code BitString} left the specified number of bits (distance).
     * 
     * If distance is negative (including Integer.MIN_VALUE), a
     * shiftRight(|distance|) is performed instead of a shiftLeft.
     * <p>
     * Any bits shifted out on the left of this {@code BitString} are lost; any
     * positions vacated on the right are filled by a {@code ZERO} value. If
     * {@code distance >= length()}, this bit string will be completely filled by
     * zeros.
     * 
     * @param distance the number of bits to shift
     * @return this {@code BitString}
     */
    public BitString shiftLeft(int distance) {
        iShiftLeft(distance, ZERO_FILL, 0, length());
        return this;
    }
    
    /**
     * Shifts a Field of this {@code BitString} left the specified number of bits
     * (distance).
     * 
     * If distance is negative (including Integer.MIN_VALUE), a
     * shiftRight(|distance|...) is performed instead of a shiftLeft.
     * <p>
     * Any bits shifted out on the left of this field are lost; any positions
     * vacated on the right are filled by a {@code ZERO} value. If
     * {@code distance >= field.length()}, this field will be completely filled by
     * zeros.
     * 
     * @param distance the number of bits to shift
     * @param field    a Field of this {@code BitString}
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code field.offset() > 0 && field.offset() >= this.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code field.length() > this.length() - field.offset()}
     */
    public BitString shiftLeft(int distance, Field field) {
        checkThisOffset(field.offset());
        checkThisLength(field.offset(), field.length(this));
        iShiftLeft(distance, ZERO_FILL, field.offset(), field.length(this));
        return this;
    }
    
    /**
     * Shifts left this {@code BitString} and the specified bit string (other) by
     * the specified number of bits (distance).
     * 
     * If distance is negative (including Integer.MIN_VALUE), a
     * shiftRight(|distance|...) is performed instead of a shiftLeft.
     * <p>
     * This {@code BitString} and the other bit string are treated as one bit string
     * where this {@code BitString} follows the other bit string. Any bits shifted
     * out on the left of the other bit string are lost. Any bits shifted out on the
     * left of this {@code BitString} are shifted into the other bit string on the
     * right; any positions vacated on the right are filled by a {@code ZERO} value.
     * If {@code distance >= this.length()}, this {@code BitString} will be
     * completely filled by zeros. if
     * {@code distance >= this.length() + other.length()}, both this
     * {@code BitString} and the other bit string will be completely filled by
     * zeros.
     * 
     * @param distance the number of bits to shift
     * @param other    the other bit string
     * @return this {@code BitString}
     */
    public BitString shiftLeft(int distance, BitString other) {
        iShiftLeft(distance, ZERO_FILL, 0, this.length(), other, 0, other.length());
        return this;
    }
    
    /**
     * Shifts left a Field of this {@code BitString} and a Field of the specified
     * bit string (other) by the specified number of bits (distance).
     * 
     * If distance is negative (including Integer.MIN_VALUE), a
     * shiftRight(|distance|...) is performed instead of a shiftLeft.
     * <p>
     * This field and the other field are treated as one bit string where this field
     * follows the other field. Any bits shifted out on the left of the other field
     * are lost. Any bits shifted out on the left of this field are shifted into the
     * other field on the right; any positions vacated on the right are filled by a
     * {@code ZERO} value. If {@code distance >= thisField.length()}, this field
     * will be completely filled by zeros. if
     * {@code distance >= thisField.length() + otherField.length()}, both this field
     * and the other field will be completely filled by zeros.
     * 
     * @param distance   the number of bits to shift
     * @param thisField  a Field of this {@code BitString}
     * @param other      the other bit string
     * @param otherField a Field of the other substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisField.offset() > 0 && thisField.offset() >= this.length()}
     *                                         or
     *                                         {@code otherField.offset() > 0 && otherField.offset() >= other.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisField.length() > this.length() - thisField.offset()}
     *                                         or
     *                                         {@code otherField.length() > other.length() - otherField.offset()}
     */
    public BitString shiftLeft(int distance,
            Field thisField,
            BitString other, Field otherField) {
        checkThisOffset(thisField.offset());
        checkThisLength(thisField.offset(), thisField.length(this));
        other.checkArgOffset(otherField.offset());
        other.checkArgLength(otherField.offset(), otherField.length(other));
        iShiftLeft(distance, ZERO_FILL,
                thisField.offset(), thisField.length(this),
                other, otherField.offset(), otherField.length(other));
        return this;
    }
    
    /**
     * Shifts this {@code BitString} right the specified number of bits (distance).
     * 
     * If distance is negative (including Integer.MIN_VALUE), a
     * shiftLeft(|distance|) is performed instead of a shiftRight.
     * <p>
     * Any bits shifted out on the right of this {@code BitString} are lost; any
     * positions vacated on the left are filled by a {@code ZERO} value. If
     * {@code distance >= length()}, this bit string will be completely filled by
     * zeros.
     * 
     * @param distance the number of bits to shift
     * @return this {@code BitString}
     */
    public BitString shiftRight(int distance) {
        iShiftRight(distance, ZERO_FILL, 0, length());
        return this;
    }
    
    /**
     * Shifts a Field of this {@code BitString} right the specified number of bits
     * (distance).
     * 
     * If distance is negative (including Integer.MIN_VALUE), a
     * shiftLeft(|distance|...) is performed instead of a shiftRight.
     * <p>
     * Any bits shifted out on the right of this field are lost; any positions
     * vacated on the left are filled by a {@code ZERO} value. If
     * {@code distance >= field.length()}, this field will be completely filled by
     * zeros.
     * 
     * @param distance the number of bits to shift
     * @param field    a Field of this {@code BitString}
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code field.offset() > 0 && field.offset() >= this.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code field.length() > this.length() - field.offset()}
     */
    public BitString shiftRight(int distance, Field field) {
        checkThisOffset(field.offset());
        checkThisLength(field.offset(), field.length(this));
        iShiftRight(distance, ZERO_FILL, field.offset(), field.length(this));
        return this;
    }
    
    /**
     * Shifts right this {@code BitString} and the specified bit string (other) by
     * the specified number of bits (distance).
     * 
     * If distance is negative (including Integer.MIN_VALUE), a
     * shiftLeft(|distance|...) is performed instead of a shiftRight.
     * <p>
     * This {@code BitString} and the other bit string are treated as one bit string
     * where this {@code BitString} precedes the other bit string. Any bits shifted
     * out on the right of the other bit string are lost. Any bits shifted out on
     * the right of this {@code BitString} are shifted into the other bit string on
     * the left; any positions vacated on the left are filled by a {@code ZERO}
     * value. If {@code distance >= this.length()}, this {@code BitString} will be
     * completely filled by zeros. if
     * {@code distance >= this.length() + other.length()}, both this
     * {@code BitString} and the other bit string will be completely filled by
     * zeros.
     * 
     * @param distance the number of bits to shift
     * @param other    the other bit string
     * @return this {@code BitString}
     */
    public BitString shiftRight(int distance, BitString other) {
        iShiftRight(distance, ZERO_FILL, 0, this.length(), other, 0, other.length());
        return this;
    }
    
    /**
     * Shifts right a Field of this {@code BitString} and a Field of the specified
     * bit string (other) by the specified number of bits (distance).
     * 
     * If distance is negative (including Integer.MIN_VALUE), a
     * shiftLeft(|distance|...) is performed instead of a shiftRight.
     * <p>
     * This field and the other field are treated as one bit string where this field
     * precedes the other field. Any bits shifted out on the right of the other
     * field are lost. Any bits shifted out on the right of this field are shifted
     * into the other field on the left; any positions vacated on the left are
     * filled by a {@code ZERO} value. If {@code distance >= thisField.length()},
     * this field will be completely filled by zeros. if
     * {@code distance >= thisField.length() + otherField.length()}, both this field
     * and the other field will be completely filled by zeros.
     * 
     * @param distance   the number of bits to shift
     * @param thisField  a Field of this {@code BitString}
     * @param other      the other bit string
     * @param otherField a Field of the other substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisField.offset() > 0 && thisField.offset() >= this.length()}
     *                                         or
     *                                         {@code otherField.offset() > 0 && otherField.offset() >= other.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisField.length() > this.length() - thisField.offset()}
     *                                         or
     *                                         {@code otherField.length() > other.length() - otherField.offset()}
     */
    public BitString shiftRight(int distance,
            Field thisField,
            BitString other, Field otherField) {
        checkThisOffset(thisField.offset());
        checkThisLength(thisField.offset(), thisField.length(this));
        other.checkArgOffset(otherField.offset());
        other.checkArgLength(otherField.offset(), otherField.length(other));
        iShiftRight(distance, ZERO_FILL,
                thisField.offset(), thisField.length(this),
                other, otherField.offset(), otherField.length(other));
        return this;
    }
    
    public BitString range(int offset) {
        checkThisOffset(offset);
        return range(offset, length() - offset);
    }
    
    public BitString range(int offset, int length) {
        checkThisOffset(offset);
        checkThisLength(offset, length);
        return new Range(this, this, firstBitIndex(offset), length);
    }
    
    public BitString range(Field field) {
        return range(field.offset(), field.length(this));
    }
    
    public BitString reverse() {
        return iReverse(0, length());
    }
    
    public BitString reverse(int offset, int length) {
        checkThisOffset(offset);
        checkThisLength(offset, length);
        return iReverse(offset, length);
    }
    
    public BitString reverse(Field field) {
        return reverse(field.offset(), field.length(this));
    }
    
    /**
     * Returns all of the bits in this {@code BitString}.
     * 
     * @return a copy of this {@code BitString}
     */
    public BitString substring() {
        return substring(0, length());
    }
    
    /**
     * Returns a substring of this {@code BitString}.
     * 
     * This substring starts at offset 'offset' of this {@code BitString} and
     * extends to the end of this {@code BitString}.
     *
     * @param offset the start of this substring
     * @return a substring of this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     */ 
    public BitString substring(int offset) {
        checkThisOffset(offset);
        return substring(offset, length() - offset);
    }
    
    /**
     * Returns a substring of this {@code BitString}.
     *
     * The substring starts at offset 'offset' of this {@code BitString} and has a
     * length of 'length'.
     * 
     * @param offset the start of this substring
     * @param length the length of this substring
     * @return a substring of this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     * @throws IllegalArgumentException        if
     *                                         {@code length < 0 || length > length() - offset}
     */
    public BitString substring(int offset, int length) {
        checkThisOffset(offset);
        checkThisLength(offset, length);
        final BitString substring = newBitString(length);
        substring.iCopy(0, length, this, offset);
        return substring;
    }
    
    /**
     * Returns a Field as a substring of this {@code BitString}.
     * 
     * @param field a Field of this {@code BitString}
     * @return a Field as a substring of this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code field.offset() > 0 && field.offset() >= length()}
     * @throws IllegalArgumentException        if
     *                                         {@code field.length() > length() - field.offset()}
     */
    public BitString subString(Field field) {
        return substring(field.offset(), field.length(this));
    }
    
    /**
     * Returns a new boolean array representing all the bits in this BitString.
     * <p>
     * More precisely, if <br>
     * {@code boolean[] booleans = s.toBooleanArray();} <br>
     * then {@code booleans.length == s.length()} and <br>
     * {@code s.getBit(n) == booleans[n]} <br>
     * for all {@code n < s.length()}.
     * 
     * @return a boolean array representing all the bits in this BitString
     * @throws java.lang.OutOfMemoryError Requested array size exceeds VM limit
     */
    public boolean[] toBooleanArray() {
        return iToBooleanArray(0, length());
    }

    /**
     * Returns a new byte array containing all the bits in this BitString.
     * <p>
     * More precisely, if <br>
     * {@code byte[] bytes = s.toByteArray();} <br>
     * then {@code bytes.length == (s.length()+7)/8} and <br>
     * {@code s.getBit(n) == ((bytes[n/8] & (1<<(7-n%8))) != 0)} <br>
     * for all {@code n < s.length()}. <br>
     * If {@code 8*bytes.length > s.length()} <br>
     * then the last byte of the array is padded on the right by
     * {@code 8*bytes.length - s.length()} ZEROS.
     *
     * @return a byte array containing all the bits in this BitString
     */
    public byte[] toByteArray() {
        return iToByteArray(0, length());
    }
    
    /**
     * Returns a new int array containing all the bits in this BitString.
     * <p>
     * More precisely, if <br>
     * {@code int[] ints = s.toIntArray();} <br>
     * then {@code ints.length == (s.length()+31)/32} and <br>
     * {@code s.getBit(n) == ((ints[n/32] & (1<<(31-n%32))) != 0)} <br>
     * for all {@code n < s.length()}. <br>
     * If {@code 32*ints.length > s.length()} <br>
     * then the last int of the array is padded on the right by
     * {@code 32*ints.length - s.length()} ZEROS.
     *
     * @return a int array containing all the bits in this BitString
     */
    public int[] toIntArray() {
        return iToIntArray(0, length());
    }

    /**
     * Returns a new long array containing all the bits in this BitString.
     * <p>
     * More precisely, if <br>
     * {@code long[] longs = s.toLongArray();} <br>
     * then {@code longs.length == (s.length()+63)/64} and <br>
     * {@code s.getBit(n) == ((longs[n/64] & (1L<<(63-n%64))) != 0)} <br>
     * for all {@code n < 64*s.length()}. <br>
     * If {@code 64*longs.length > s.length()} <br>
     * then the last long of the array is padded on the right by
     * {@code 64*longs.length - s.length()} ZEROS.
     *
     * @return a long array containing all the bits in this BitString
     */
    public long[] toLongArray() {
        return iToLongArray(0, length());
    }
    
    /**
     * Returns a new short array containing all the bits in this BitString.
     * <p>
     * More precisely, if <br>
     * {@code short[] shorts = s.toShortArray();} <br>
     * then {@code shorts.length == (s.length()+15)/16} and <br>
     * {@code s.getBit(n) == ((shorts[n/16] & (1<<(15-n%16))) != 0)} <br>
     * for all {@code n < s.length()}. <br>
     * If {@code 16*shorts.length > s.length()} <br>
     * then the last short of the array is padded on the right by
     * {@code 16*shorts.length - s.length()} ZEROS.
     *
     * @return a short array containing all the bits in this BitString
     */
    public short[] toShortArray() {
        return iToShortArray(0, length());
    }
    
    /**
     * Returns a new Primitive array containing all the bits in this BitString.
     * The Primitive array consists of LONG values, INT values, SHORT values,
     * BYTE values, and BOOLEAN values, in that order, such that the cumulative
     * sizes of all the primitive sizes exactly equal the length of this {@code BitString}.
     *
     * @return a Primitive array containing all the bits in this BitString
     */
    public Primitive[] toPrimitiveArray() {
        return iToPrimitiveArray(0, length());
    }
    
    /**
     * Returns a String of binary digits, representing all the bits in this
     * BitString.
     * <p>
     * The characters {@code '0'} ({@code '\u005Cu0030'}) and {@code
     * '1'} ({@code '\u005Cu0031'}) are used as binary digits.
     * <p>
     * This method is equivalent to {@link #toBinaryString}.
     * 
     * @return a String of binary digits, representing all the bits in this
     *         BitString
     * @throws java.lang.OutOfMemoryError Requested array size exceeds VM limit
     */
    @Override
    public String toString() {
        return toBinaryString();
    }
    
    /**
     * Returns a String of binary digits, representing all the bits in this
     * BitString.
     * <p>
     * The characters {@code '0'} ({@code '\u005Cu0030'}) and {@code
     * '1'} ({@code '\u005Cu0031'}) are used as binary digits.
     * 
     * @return a String of binary digits, representing all the bits in this
     *         BitString
     * @throws java.lang.OutOfMemoryError Requested array size exceeds VM limit
     */
    public String toBinaryString() {
        final int offset = 0;
        final int length = this.length();
        if (length == 0) return "";
        final StringBuilder binaryString = new StringBuilder(length);
        final int[] iterator = getIterator(offset, length);
        while (hasNextIteratorWord(iterator)) {
            final long word = getNextIteratorWord(iterator);
            String binarySubString = String.format("%64s", Long.toBinaryString(word)).replace(' ', '0');
            final int wordBitCount = getIteratorWordBitCount(iterator);
            if (wordBitCount < BITS_PER_WORD) binarySubString = binarySubString.substring(0, wordBitCount);
            binaryString.append(binarySubString);
        }
        return binaryString.toString();
    }
    
    /**
     * Returns a String, of hexadecimal digits, representing all the bits in this
     * BitString.
     * <p>
     * The following characters are used as hexadecimal digits:
     * 
     * <blockquote> {@code 0123456789abcdef} </blockquote>
     *
     * These are the characters {@code '\u005Cu0030'} through {@code '\u005Cu0039'}
     * and {@code '\u005Cu0061'} through {@code '\u005Cu0066'}.
     * <p>
     * BitStrings are processed from left to right; from the lowest offset to the
     * highest offset. Any remaining bits on the far right that do not form a full
     * hexadecimal digit, will be padded on the right with character {@code '0'}
     * ({@code '\u005Cu0030'}).
     * 
     * @return a String, of hexadecimal digits, representing all the bits in this
     *         BitString
     */
    public String toHexString() {
        final int offset = 0;
        final int length = this.length();
        if (length == 0) return "";
        final int bitsPerHexDigit = 4;
        final int HexDigitsPerWord = 16;
        int hexCount = (length - 1) / bitsPerHexDigit + 1;
        final StringBuilder hexString = new StringBuilder(hexCount);
        final int[] iterator = getIterator(offset, length);
        while (hasNextIteratorWord(iterator)) {
            final long word = getNextIteratorWord(iterator);
            String hexSubString = String.format("%16s", Long.toHexString(word)).replace(' ', '0');
            if (hexCount < HexDigitsPerWord) hexSubString = hexSubString.substring(0, hexCount);
            hexString.append(hexSubString);
            hexCount -= HexDigitsPerWord;
        }
        return hexString.toString();
    }
    
    /**
     * Returns a String, of octal digits, representing all the bits in this
     * BitString.
     * <p>
     * The following characters are used as octal digits:
     * 
     * <blockquote> {@code 01234567} </blockquote>
     *
     * These are the characters {@code '\u005Cu0030'} through {@code '\u005Cu0037'}.
     * <p>
     * BitStrings are processed from left to right; from the lowest offset to the
     * highest offset. Any remaining bits on the far right that do not form a full
     * octal digit, will be padded on the right with character {@code '0'}
     * ({@code '\u005Cu0030'}).
     * 
     * @return a String, of octal digits, representing all the bits in this
     *         BitString
     */
    public String toOctalString() {
        final int offset = 0;
        final int length = this.length();
        if (length == 0) return "";
        final int bitsPerOctalDigit = 3;
        final int octalDigitsPerWord = 21;
        int octalCount = (length - 1) / bitsPerOctalDigit + 1;
        final StringBuilder octalString = new StringBuilder(octalCount);
        final int[] iterator = getIterator(offset, length);
        while (octalCount > 0) {
            long prevWord = 0L;
            // a word contains 21 full octal digits (3 bits each for 63 bits) with one
            // extra bit at the end which goes with the next two bits in the next word.
            // Therefore, each word is shifted one bit to the right before it is
            // converted to octal digits. In addition, any bits that were shifted
            // out of the previous word, need to be shifted into the current word.
            // After every third word that has been shifted as described above,
            // the 3 bits that were shifted out of the current word constitute one
            // octal digit.
            for (int n = 0; n < bitsPerOctalDigit && octalCount > 0; n++) {
                final long word = (hasNextIteratorWord(iterator)) ? getNextIteratorWord(iterator) : 0L;
                final long octalWord = ((n == 0) ? word : shiftArgsRight(n, prevWord, word)) >>> 1;
                String octalSubString = String.format("%21s", Long.toOctalString(octalWord)).replace(' ', '0');
                if (octalCount < octalDigitsPerWord) octalSubString = octalSubString.substring(0, octalCount);
                octalString.append(octalSubString);
                octalCount -= octalDigitsPerWord;
                prevWord = word;
            }
            if (octalCount > 0) {
                octalString.append(Long.toOctalString(prevWord & 0x00000007L));
                octalCount--;
            }
        }
        return octalString.toString();
    }

    /**
     * return the hash code value for this bit String}
     *
     * The hash code depends only on which bits are set within this
     * {@code BitString}.
     *
     * <p>The hash code is defined to be the result of the following
     * calculation:
     *  <pre> {@code
     * public int hashCode() {
     *     long h = 1234;
     *     long[] words = toLongArray();
     *     for (int i = words.length; --i >= 0; )
     *         h ^= words[i] * (i + 1);
     *     return (int)((h >> 32) ^ h);
     * }}</pre>
     * Note that the hash code changes if the set of bits is altered.
     */
    @Override
    public int hashCode() {
        long hashcode = 1234;
        final int[] iterator = getIterator();
        while (hasNextIteratorWord(iterator)) {
            final long word = getNextIteratorWord(iterator);
            hashcode ^= word * (getIteratorWordIndex(iterator) + 1);
        }
        return (int)((hashcode >> 32) ^ hashcode);
    }

    public static class Field implements Cloneable {
        
        private final int offset;
        private final int length;
        
        public Field(int offset, int length) {
            if (offset < 0) throw new IllegalArgumentException("offset is negative; offset="+offset);
            if (length < 0) throw new IllegalArgumentException("length is negative; length="+length);
            this.offset = offset;
            this.length = length;
        }
        
        @Override
        public Field clone() {
            try {
                return (Field) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new InternalError(e);
            }
        }
        
        public int offset() {
            return this.offset;
        }
        
        public int length() {
            return this.length;
        }
        
        int length(BitString bitString) {
            return this.length;
        }
        
        public boolean isAll() {
            return false;
        }
        
        public Field at(int offset) {
            return new Field(offset, this.length); 
        }
        
        public static Field indexRange(int fromIndex, int toIndex) {
            if (fromIndex < 0) throw new IndexOutOfBoundsException("fromIndex is negative; index="+fromIndex);
            if (toIndex < 0) throw new IndexOutOfBoundsException("toIndex is negative; index="+toIndex);
            if (fromIndex > toIndex) throw new IndexOutOfBoundsException("fromIndex > toIndex; from="+fromIndex+", to="+toIndex);
            return new Field(fromIndex, toIndex-fromIndex);
        }
        
        @Override
        public String toString() {
            return offset() + "," + length();
        }

        @Override
        public int hashCode() {
            return Objects.hash(length, offset);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            Field other = (Field) obj;
            //return length == other.length && offset == other.offset;
            return this.offset() == other.offset() && this.length() == other.length();
        }

        private static class All extends Field {
            
            private All() {
                super(0, 0);
            }
            
            private All(int offset) {
                super(offset, 0);
            }
            
            @Override
            int length(BitString bitString) {
                return bitString.length() - offset();
            }
            
            @Override
            public boolean isAll() {
                return true;
            }
            
            @Override
            public Field at(int offset) {
                return new All(offset);
            }

            @Override
            public int hashCode() {
                return super.hashCode();
            }

            @Override
            public boolean equals(Object obj) {
                if (this == obj) return true;
                if (!super.equals(obj)) return false;
                //if (getClass() != obj.getClass()) return false;
                return true;
            }
            
//            @Override
//            public boolean equals(Object obj) {
//                if (!(obj instanceof Field.All)) return false;
//                return super.equals(obj);
//            }
            
            
        }
        
    }
    
    /**
     * The Primitive class wraps a value of a primitive type (boolean, byte, char,
     * double, float, int, long, and short) in an object. All primitive types are
     * encapsulated in a single field of type long.
     * 
     * @author James Pfeifer
     *
     */
    public static class Primitive {
        
        /**
         * Primitive types.
         * 
         * @author James Pfeifer
         *
         */
        public enum Type {
            /**
             * Boolean primitive type.
             */
            BOOLEAN (1,
                    primitive -> { return Boolean.valueOf(primitive.booleanValue()); },
                    primitive -> { return Byte.valueOf(primitive.booleanValue() ? (byte)1 : 0); },
                    primitive -> { return primitive.booleanValue() ? "1" : "0"; }
                    ),
            /**
             * Byte primitive type.
             */
            BYTE    (Byte.SIZE,
                    primitive -> { return Byte.valueOf(primitive.byteValue()); },
                    primitive -> { return Byte.valueOf(primitive.byteValue()); },
                    primitive -> { return Byte.toString(primitive.byteValue()); }
                    ),
            /**
             * Character (char) primitive type.
             */
            CHAR    (Character.SIZE,
                    primitive -> { return Character.valueOf(primitive.charValue()); },
                    primitive -> { return Short.valueOf(primitive.shortValue()); },
                    primitive -> { return Character.toString(primitive.charValue()); }
                    ),
            /**
             * Double primitive type.
             */
            DOUBLE  (Double.SIZE,
                    primitive -> { return Double.valueOf(primitive.doubleValue()); },
                    primitive -> { return Double.valueOf(primitive.doubleValue()); },
                    primitive -> { return Double.toString(primitive.doubleValue()); }
                    ),
            /**
             * Float primitive type.
             */
            FLOAT   (Float.SIZE,
                    primitive -> { return Float.valueOf(primitive.floatValue()); },
                    primitive -> { return Float.valueOf(primitive.floatValue()); },
                    primitive -> { return Float.toString(primitive.floatValue()); }
                    ),
            /**
             * Integer (int) primitive type.
             */
            INT     (Integer.SIZE,
                    primitive -> { return Integer.valueOf(primitive.intValue()); },
                    primitive -> { return Integer.valueOf(primitive.intValue()); },
                    primitive -> { return Integer.toString(primitive.intValue()); }
                    ),
            /**
             * Long primitive type.
             */
            LONG    (Long.SIZE,
                    primitive -> { return Long.valueOf(primitive.longValue()); },
                    primitive -> { return Long.valueOf(primitive.longValue()); },
                    primitive -> { return Long.toString(primitive.longValue()); }
                    ),
            /**
             * Short primitive type.
             */
            SHORT   (Short.SIZE,
                    primitive -> { return Short.valueOf(primitive.shortValue()); },
                    primitive -> { return Short.valueOf(primitive.shortValue()); },
                    primitive -> { return Short.toString(primitive.shortValue()); }
                    );
            private final int size;
            private final Function<Primitive, Object> toObjectFunction;
            private final Function<Primitive, Number> toNumberFunction;
            private final Function<Primitive, String> toStringFunction;
            private Type(int size,
                    Function<Primitive, Object> toObjectFunction,
                    Function<Primitive, Number> toNumberFunction,
                    Function<Primitive, String> toStringFunction) {
                this.size = size;
                this.toObjectFunction = toObjectFunction;
                this.toNumberFunction = toNumberFunction;
                this.toStringFunction = toStringFunction;
            }
            int size() { return this.size; }
            private Function<Primitive, Object> toObjectFunction() { return this.toObjectFunction; }
            private Function<Primitive, Number> toNumberFunction() { return this.toNumberFunction; }
            private Function<Primitive, String> toStringFunction() { return this.toStringFunction; }
        }
        
        private final Type type;
        private final long longValue;
        
        private Primitive(Type type, long longValue) {
            this.type = type;
            this.longValue = longValue;
        }
        
        /**
         * Returns the sum of all the sizes of the primitives in the specified array of
         * primitives.
         * 
         * @param primitives an array of primitives
         * @return the sum of all the sizes in an array of primitives
         */
        public static long arrayCumulativeSize(Primitive[] primitives) {
            long size = 0L;
            for (Primitive primitive: primitives) size += primitive.type().size();
            return size;
        }
        
        /**
         * Returns the sum of all the sizes of the primitive types in the specified
         * array of primitive types.
         * 
         * @param types an array of primitive typess
         * @return the sum of all the sizes in an array of primitive typess
         */
        public static long arrayCumulativeSize(Primitive.Type[] types) {
            long size = 0L;
            for (Primitive.Type type: types) size += type.size();
            return size;
        }
        
        /**
         * returns the type of primitive that was initially used to construct the
         * primitive object.
         * 
         * @return the type of primitive contained in this primitive
         */
        public Type type() {
            return this.type;
        }
        
        /**
         * Returns a new Primitive constructed from the specified boolean value.
         * 
         * @param booleanValue the boolean value used to construct this Primitive
         * @return a new Primitive
         */
        public static Primitive valueOf(boolean booleanValue) {
            return new Primitive(Type.BOOLEAN, booleanValue ? 1L : 0L);
        }
        
        /**
         * Returns a new Primitive constructed from the specified byte value.
         * 
         * @param byteValue the byte value used to construct this Primitive
         * @return a new Primitive
         */
        public static Primitive valueOf(byte byteValue) {
            return new Primitive(Type.BYTE, Byte.toUnsignedLong(byteValue));
        }
        
        /**
         * Returns a new Primitive constructed from the specified char value.
         * 
         * @param charValue the character value used to construct this Primitive
         * @return a new Primitive
         */
        public static Primitive valueOf(char charValue) {
            return new Primitive(Type.CHAR, (long)charValue);
        }
        
        /**
         * Returns a new Primitive constructed from the specified double value.
         * 
         * @param doubleValue the double value used to construct this Primitive
         * @return a new Primitive
         */
        public static Primitive valueOf(double doubleValue) {
            return new Primitive(Type.DOUBLE, Double.doubleToRawLongBits(doubleValue));
        }
        
        /**
         * Returns a new Primitive constructed from the specified float value.
         * 
         * @param floatValue the float value used to construct this Primitive
         * @return a new Primitive
         */
        public static Primitive valueOf(float floatValue) {
            return new Primitive(Type.FLOAT, Integer.toUnsignedLong(Float.floatToRawIntBits(floatValue)));
        }
        
        /**
         * Returns a new Primitive constructed from the specified int value.
         * 
         * @param intValue the integer value used to construct this Primitive
         * @return a new Primitive
         */
        public static Primitive valueOf(int intValue) {
            return new Primitive(Type.INT, Integer.toUnsignedLong(intValue));
        }
        
        /**
         * Returns a new Primitive constructed from the specified long value.
         * 
         * @param longValue the long value used to construct this Primitive
         * @return a new Primitive
         */
        public static Primitive valueOf(long longValue) {
            return new Primitive(Type.LONG, longValue);
        }
        
        /**
         * Returns a new Primitive constructed from the specified short value.
         * 
         * @param shortValue the short value used to construct this Primitive
         * @return a new Primitive
         */
        public static Primitive valueOf(short shortValue) {
            return new Primitive(Type.SHORT, Short.toUnsignedLong(shortValue));
        }

        /**
         * Returns {@code true} if this Primitive was constructed from a boolean value.
         * 
         * @return {@code true} if this Primitive was constructed from a boolean value
         */
        public boolean isBoolean() {
            return type == Type.BOOLEAN ? true : false;
        }
        
        /**
         * Returns {@code true} if this Primitive was constructed from a byte value.
         * 
         * @return {@code true} if this Primitive was constructed from a byte value
         */
        public boolean isByte() {
            return type == Type.BYTE ? true : false;
        }

        /**
         * Returns {@code true} if this Primitive was constructed from a char value.
         * 
         * @return {@code true} if this Primitive was constructed from a char value
         */
        public boolean isCharacter() {
            return type == Type.CHAR ? true : false;
        }
        
        /**
         * Returns {@code true} if this Primitive was constructed from a double value.
         * 
         * @return {@code true} if this Primitive was constructed from a double value
         */
        public boolean isDouble() {
            return type == Type.DOUBLE ? true : false;
        }

        /**
         * Returns {@code true} if this Primitive was constructed from a float value.
         * 
         * @return {@code true} if this Primitive was constructed from a float value
         */
        public boolean isFloat() {
            return type == Type.FLOAT ? true : false;
        }
        
        /**
         * Returns {@code true} if this Primitive was constructed from an int value.
         * 
         * @return {@code true} if this Primitive was constructed from an int value
         */
        public boolean isInteger() {
            return type == Type.INT ? true : false;
        }

        /**
         * Returns {@code true} if this Primitive was constructed from a long value.
         * 
         * @return {@code true} if this Primitive was constructed from a long value
         */
        public boolean isLong() {
            return type == Type.LONG ? true : false;
        }
        
        /**
         * Returns {@code true} if this Primitive was constructed from a short value.
         * 
         * @return {@code true} if this Primitive was constructed from a short value
         */
        public boolean isShort() {
            return type == Type.SHORT ? true : false;
        }
        
        /**
         * Returns the boolean value of this Primitive. Only the last (right most) bit
         * of the internal long field is returned as a boolean.
         * 
         * @return the boolean value of this Primitive
         */
        public boolean booleanValue() {
            return (longValue & 1L) == 0 ? false : true;
        }
        
        /**
         * Returns the byte value of this Primitive. Only the last (right most) 8 bits
         * of the internal long field is returned as a byte.
         * 
         * @return the byte value of this Primitive
         */
        public byte byteValue() {
            return (byte) longValue; 
        }
        
        /**
         * Returns the char value of this Primitive. Only the last (right most) 16 bits
         * of the internal long field is returned as a char.
         * 
         * @return the char value of this Primitive
         */
        public char charValue() {
            return (char) longValue; 
        }
        
        /**
         * Returns the double value of this Primitive. The internal long field is
         * considered to be a representation of a floating-point value according to the
         * IEEE 754 floating-point "double format" bit layout.
         * 
         * @return the double value of this Primitive
         */
        public double doubleValue() {
            return Double.longBitsToDouble(longValue);
        }
        
        /**
         * Returns the float value of this Primitive. The last (right most) 32 bits of
         * the internal long field is considered to be a representation of a
         * floating-point value according to the IEEE 754 floating-point "single format"
         * bit layout.
         * 
         * @return the float value of this Primitive
         */
        public float floatValue() {
            return Float.intBitsToFloat((int) longValue);
        }
        
        /**
         * Returns the int value of this Primitive. Only the last (right most) 32 bits
         * of the internal long field is returned as an int.
         * 
         * @return the int value of this Primitive
         */
        public int intValue() {
            return (int) longValue;
        }
        
        /**
         * Returns the long value of this Primitive. The internal long field is returned
         * as is.
         * 
         * @return the char value of this Primitive
         */
        public long longValue() {
            return longValue;
        }
        
        /**
         * Returns the short value of this Primitive. Only the last (right most) 16 bits
         * of the internal long field is returned as a short.
         * 
         * @return the short value of this Primitive
         */
        public short shortValue() {
            return (short) longValue;
        }
        
        /**
         * Returns a Number object (Byte, Double, Float, Integer, Long, or Short)
         * corresponding to this Primitive's type. A Primitive of type BOOLEAN is
         * returned as a Byte and a Primitive of type CHAR is returned as a Short.
         * 
         * @return a Number object corresponding to this Primitive's type
         */
        public Number toNumber() {
            return type.toNumberFunction().apply(this);
        }
        
        /**
         * Returns a primitive wrapper object (Boolean, Byte, Character, Double, Float,
         * Integer, Long, or Short) corresponding to this Primitive's type.
         * 
         * @return a primitive wrapper object corresponding to this Primitive's type
         */
        public Object toObject() {
            return type.toObjectFunction().apply(this);
        }
        
        @Override
        public String toString() {
            return type.toStringFunction().apply(this);
        }

        @Override
        public int hashCode() {
            return Objects.hash(longValue, type);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (this.getClass() != obj.getClass()) return false;
            Primitive other = (Primitive) obj;
            return this.longValue == other.longValue && this.type == other.type;
        }
        
    }
    
    private static class Constant extends BitString {
        
        private static final long serialVersionUID = -6897302428744057266L;
        
        private long constant;
        
        private Constant(long constant) {
            super(Integer.MAX_VALUE);
            this.constant = constant;
        }
        
        @Override
        LongBitString newBitString(int length) {
            return new LongBitString(length);
        }
        
        @Override
        public Constant clone() {
            return (Constant) super.clone();
        }
        
        private void writeObject(ObjectOutputStream stream)
            throws IOException {
            throw new java.io.InvalidObjectException("not serializable");
        }

        private void readObject(ObjectInputStream stream)
            throws IOException, ClassNotFoundException {
            throw new java.io.InvalidObjectException("not serializable");
        }
        
        @Override
        long getWord(int wordIndex) {
            assert wordIndex >= 0;
            return constant;
        }
        
        private void throwCanNotBeModifiedException() {
            throw new UnsupportedOperationException("Constant BitStrings cannot be modified");
        }
        
        @Override
        void putWord(int wordIndex, long word) {
            throwCanNotBeModifiedException();
        }
        
//        private String noCapacityMsg() {
//            return "BitString Constants have no actual capacity";
//        }
        
        @Override
        void resizeBackingArray(int capacity) {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public int capacity() {
            return Integer.MAX_VALUE;
            //throw new UnsupportedOperationException(noCapacityMsg());
        }
        
        @Override
        public int ensureCapacity(int bitsRequired) {
            return capacity();
            //throw new UnsupportedOperationException(noCapacityMsg());
        }
        
        @Override
        public int trimToLength() {
            return capacity();
            //throw new UnsupportedOperationException(noCapacityMsg());
        }
        
        @Override
        public void setLength(int newLength) {
            throwCanNotBeModifiedException();
        }
        
        @Override
        void setRangeLength(int lengthDelta, int bitIndex) {
            throwCanNotBeModifiedException();
        }
        
    }
    
    private static class Range extends BitString {
        
        private static final long serialVersionUID = -1012410172593108057L;
        
        private final BitString base;
        private final BitString parent;
        private final int firstBitIndex;
        private long expectantModCount;
        
        private Range(BitString base, BitString parent, int firstBitIndex, int length) {
            super(length);
            this.base = base;
            this.parent = parent;
            this.firstBitIndex = firstBitIndex;
            this.expectantModCount = modCount();
        }
        
        @Override
        BitString newBitString(int length) {
            return base.newBitString(length);
        }
        
        @Override
        public Range clone() {
            checkForModificationException();
            return (Range)super.clone();
        }
        
        private void writeObject(ObjectOutputStream stream)
            throws IOException {
            throw new java.io.InvalidObjectException("not serializable");
        }

        private void readObject(ObjectInputStream stream)
            throws IOException, ClassNotFoundException {
            throw new java.io.InvalidObjectException("not serializable");
        }
        
        @Override
        long getWord(int wordIndex) {
            assert wordIndex >= 0;
            checkForModificationException();
            return base.getWord(wordIndex);
        }
        
        @Override
        void putWord(int wordIndex, long word) {
            checkForModificationException();
            base.putWord(wordIndex, word);
        }
        
        @Override
        void resizeBackingArray(int capacity) {
            throw new UnsupportedOperationException();
            //base.resizeBackingArray(capacity);
        }
        
        @Override
        public int capacity() {
            checkForModificationException();
            return base.capacity() - base.length() + this.stringLength;
        }
        
        @Override
        public int ensureCapacity(int capacity) {
            checkForModificationException();
            long baseCapacity = capacity - this.stringLength + base.length();
            if (baseCapacity > Integer.MAX_VALUE) baseCapacity = Integer.MAX_VALUE;
            base.ensureCapacity((int)baseCapacity);
            return capacity();
        }
        
        @Override
        public int trimToLength() {
            checkForModificationException();
            return base.trimToLength();
        }
        
        @Override
        public int length() {
            checkForModificationException();
            return this.stringLength;
        }
        
        @Override
        int baseLength() {
            return base.baseLength();
        }
        
        @Override
        public void setLength(int newLength) {
            checkForModificationException();
            if (newLength < 0) throw new IllegalArgumentException("specified length is negative: "+newLength);
            final int lengthDelta = newLength - length();
            int bitIndex = lastBitIndex() + 1;
            if (lengthDelta < 0) bitIndex += lengthDelta;
            setRangeLength(lengthDelta, bitIndex);
        }
        
        @Override
        void setRangeLength(int lengthDelta, int bitIndex) {
            parent.setRangeLength(lengthDelta, bitIndex);
            updateExpectantModCountAndLength(lengthDelta);
        }
        
        @Override
        long modCount() {
            return base.modCount();
        }
        
        private void updateExpectantModCountAndLength(int lengthDelta) {
            this.stringLength += lengthDelta;
            this.expectantModCount = modCount();
        }
        
        private void checkForModificationException() {
            if (this.expectantModCount != modCount()) {
                throw new ConcurrentModificationException();
            }
        }
        
        @Override
        int bitIndex(int offset) {
            return this.firstBitIndex + offset;
        }
        
        @Override
        void iAppend(BitString that, int thatOffset, int thatLength) {
            checkForModificationException();
            assert that.isValidOffset(thatOffset);
            assert that.isValidLength(thatOffset, thatLength);
            parent.iAppend(that, thatOffset, thatLength);
            updateExpectantModCountAndLength(thatLength);
        }
        
        @Override
        void iDelete(int bitIndex, int length) {
            checkForModificationException();
            if (length > 0) {
                parent.iDelete(bitIndex, length);
                updateExpectantModCountAndLength(-length);
            }
        }
        
        @Override
        void iInsert(int position, BitString that, int thatOffset, int thatLength) {
            checkForModificationException();
            assert that.isValidOffset(thatOffset);
            assert that.isValidLength(thatOffset, thatLength);
            if (thatLength == 0) return;
            parent.iInsert(position, that, thatOffset, thatLength);
            updateExpectantModCountAndLength(thatLength);
        }
        
        @Override
        void iReplace(int thisBitIndex, int thisLength, BitString that, int thatOffset, int thatLength) {
            checkForModificationException();
            assert that.isValidOffset(thatOffset);
            assert that.isValidLength(thatOffset, thatLength);
            parent.iReplace(thisBitIndex, thisLength, that, thatOffset, thatLength);
            updateExpectantModCountAndLength(thatLength-thisLength);
        }
        
        @Override
        public BitString append(BitString that) {
            checkBaseLengthIncrease(that.length());
            iInsert(lastBitIndex()+1, that, 0, that.length());
            return this;
        }
        
        @Override
        public BitString append(BitString that, int thatOffset, int thatLength) {
            that.checkArgOffset(thatOffset);
            that.checkArgLength(thatOffset, thatLength);
            checkBaseLengthIncrease(thatLength);
            iInsert(lastBitIndex()+1, that, thatOffset, thatLength);
            return this;
        }
        
        @Override
        public BitString range(int offset, int length) {
            checkForModificationException();
            checkThisOffset(offset);
            checkThisLength(offset, length);
            return new Range(base, this, firstBitIndex(offset), length);
        }
        
    }

}
