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
import java.util.function.IntToLongFunction;
import java.util.function.LongBinaryOperator;
import java.util.function.LongToIntFunction;
import java.util.function.Predicate;
import java.util.function.ToLongFunction;

import net.pfeifdom.java.util.function.IntLongConsumer;
import net.pfeifdom.java.util.function.LongBiPredicate;

/**
 * A mutable sequence of bits.
 * 
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
 *      |   |           0 ||    clear    | copyFrom  |  norNot   |    and    |
 *      |   |-------------||-------------|-----------|-----------|-----------|
 *      |   |           1 || copyNotFrom |    set    |   nand    |   orNot   |
 *      | 0 |-------------||-------------|-----------|-----------|-----------|
 *      |   |        FLIP ||     nor     |  nandNot  |   flip    |   xnor    |
 *      |   |-------------||-------------|-----------|-----------|-----------|
 *      |   |   UNCHANGED ||    andNot   |     or    |    xor    |           |
 *      |===|================================================================|
 * 
 * </pre> 
 * 
 * @author james
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
    private static final boolean ONE_DFLT = ONE;
    private static final boolean ZERO_DFLT = ZERO;
    
    public static final Field ALL = new Field.All();
    
    public static Field field(int offset, int length) {
        return new Field(offset, length);
    }
    
    public static Field indexField(int fromIndex, int toIndex) {
        return Field.indexRange(fromIndex, toIndex);
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
    
    public Field all() {
        return all(0);
    }
    
    public Field all(int offset) {
        checkThisPosition(offset);
        if (offset == this.length()) return field(offset == 0 ? 0 : offset - 1, 0);
        return field(offset, this.length() - offset);
    }
    
    public void setLength(int newLength) {
        if (newLength == this.stringLength) return;
        if (newLength < 0) throw new IllegalArgumentException("specified length is negative: " + newLength);
        ensureCapacity(newLength);
        final int oldLength = this.stringLength;
        this.stringLength = newLength;
        if (newLength > oldLength) iClear(oldLength, newLength - oldLength);
        if (newLength < oldLength) incrementModCount(); // do not invalidate Ranges if appending
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
    
    abstract void setWord(int wordIndex, long word);
    
    int bitIndex(int offset) {
        return offset;
    }
    
    private int firstBitIndex(int offset) {
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
    
//    private long getNextIteratorWord(int[] iterator) {
//        return getNextIteratorWord(iterator, ZERO_FILL);
//    }
    
//    private long getNextIteratorWord(int[] iterator, boolean fill) {
//        int wordIndex = iterator[0];
//        final int firstWordIndex = iterator[1];
//        final int lastWordIndex = iterator[2];
//        final int leftMarginSize = iterator[3];
//        final int rightMarginSize = iterator[4];
//        final int remainingLength = iterator[5];
//        if (wordIndex == lastWordIndex || remainingLength <= 0) throw new IllegalStateException();
//        if (wordIndex == -1) wordIndex = firstWordIndex;
//        else wordIndex++;
//        iterator[0] = wordIndex;
//        if (wordIndex == firstWordIndex && wordIndex == lastWordIndex) {
//            iterator[6] = (remainingLength > BITS_PER_WORD) ? BITS_PER_WORD-(rightMarginSize+leftMarginSize) : remainingLength;
//        } else if (wordIndex == firstWordIndex) {
//            iterator[6] = (remainingLength > BITS_PER_WORD) ? BITS_PER_WORD-leftMarginSize : remainingLength;
//        } else if (wordIndex == lastWordIndex) {
//            iterator[6] = (remainingLength > BITS_PER_WORD) ? BITS_PER_WORD-rightMarginSize : remainingLength;
//        } else {
//            iterator[6] = (remainingLength > BITS_PER_WORD) ? BITS_PER_WORD : remainingLength;
//        }
//        iterator[5] -= iterator[6];
//        return fillMargins(fill, getWord(wordIndex), wordIndex,
//                firstWordIndex, lastWordIndex, leftMarginSize, rightMarginSize);
//    }
    
//    private long getPreviousIteratorWord(int[] iterator) {
//        return getPreviousIteratorWord(iterator, ZERO_FILL);
//    }
    
//    private long getPreviousIteratorWord(int[] iterator, boolean fill) {
//        int wordIndex = iterator[0];
//        final int firstWordIndex = iterator[1];
//        final int lastWordIndex = iterator[2];
//        final int leftMarginSize = iterator[3];
//        final int rightMarginSize = iterator[4];
//        final int remainingLength = iterator[5];
//        if (wordIndex == firstWordIndex || remainingLength <= 0) throw new IllegalStateException();
//        if (wordIndex == -1) wordIndex = lastWordIndex;
//        else wordIndex--;
//        iterator[0] = wordIndex;
//        if (wordIndex == firstWordIndex && wordIndex == lastWordIndex) {
//            iterator[6] = (remainingLength > BITS_PER_WORD) ? BITS_PER_WORD-(rightMarginSize+leftMarginSize) : remainingLength;
//        } else if (wordIndex == firstWordIndex) {
//            iterator[6] = (remainingLength > BITS_PER_WORD) ? BITS_PER_WORD-leftMarginSize : remainingLength;
//        } else if (wordIndex == lastWordIndex) {
//            iterator[6] = (remainingLength > BITS_PER_WORD) ? BITS_PER_WORD-rightMarginSize : remainingLength;
//        } else {
//            iterator[6] = (remainingLength > BITS_PER_WORD) ? BITS_PER_WORD : remainingLength;
//        }
//        iterator[5] -= iterator[6];
//        return fillMargins(fill, getWord(wordIndex), wordIndex,
//                firstWordIndex, lastWordIndex, leftMarginSize, rightMarginSize);
//    }
    
    private long getNextIteratorFullWord(int[] iterator) {
        return getNextIteratorFullWord(iterator, ZERO_FILL);
    }
    
    private long getNextIteratorFullWord(int[] iterator, boolean fill) {
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
    
    private long getPreviousIteratorFullWord(int[] iterator) {
        return getPreviousIteratorFullWord(iterator, ZERO_FILL);
    }
    
    private long getPreviousIteratorFullWord(int[] iterator, boolean fill) {
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
     * Return rArg shifted right the specified number of bits. The far right bits in
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
     * Return lArg shifted left the specified number of bits. The far left bits in
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
    
    private void restoreMargin(int wordIndex,
            long originalFirstWord, long originalLastWord,
            int firstWordIndex, int lastWordIndex,
            int leftMarginSize, int rightMarginSize) {
        if (wordIndex == firstWordIndex) restoreLeftMargin(originalFirstWord, firstWordIndex, leftMarginSize);
        if (wordIndex == lastWordIndex) restoreRightMargin(originalLastWord, lastWordIndex, rightMarginSize);
    }
    
    private void restoreLeftMargin(long originalFirstWord,
            int firstWordIndex, int leftMarginSize) {
        if (leftMarginSize > 0) {
            setWord(firstWordIndex,
                    (originalFirstWord & (WORD_MASK << (BITS_PER_WORD - leftMarginSize)))
                  | (getWord(firstWordIndex) & (WORD_MASK >>> leftMarginSize)));
        }
    }
    
    private void restoreRightMargin(long originalLastWord,
            int lastWordIndex, int rightMarginSize) {
        if (rightMarginSize > 0) {
            setWord(lastWordIndex,
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
    
    static long[] packInts(int[] ints) {
        return pack(ints.length, Integer.SIZE,
                (index) -> { return Integer.toUnsignedLong(ints[index]); });
    }
    
    static long[] packShorts(short[] shorts) {
        return pack(shorts.length, Short.SIZE,
                (index) -> { return Short.toUnsignedLong(shorts[index]); });
    }
    
    private static long[] pack(int primitiveCount, int primitiveSize,
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
    
    private void checkAvailableSpace(int offset, int requiredSpace) {
        final int availableSpace = length() - offset;
        if (availableSpace < requiredSpace) {
            throw new IllegalStateException("not enough space to perform the operation"
                    + "; required space=" + requiredSpace + ", available space=" + availableSpace
                    + ", stringLength=" + length() + ", offset=" + offset);
        }
    }
    
    void checkBaseLengthIncrease(long length) {
        if (baseLength() + length > Integer.MAX_VALUE) {
            throw new UnsupportedOperationException(
                    "the operation would result in the length of the base BitString exceeding the maximum of "
                            + Integer.MAX_VALUE + ": " + length);
        }
    }
    
    void iAppend(BitString that, int thatOffset, int thatLength) {
        assert that.isValidOffset(thatOffset);
        assert that.isValidLength(thatOffset, thatLength);
        ensureCapacity(this.length() + thatLength);
        setLength(this.length() + thatLength);
        iCopy(this.length(), thatLength, that, thatOffset);
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
        if (position == this.length()) { iAppend(that, thatOffset, thatLength); return; }
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
        iBitwiseOp(thisOffset, length, that, thatOffset,
                (lArg, rArg) -> { return lArg & rArg; });
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
        iBitwiseOp(thisOffset, length, that, thatOffset,
                (lArg, rArg) -> { return lArg & ~rArg; });
    }
    
    void iClear(int offset, int length) {
        iBitwiseOp(offset, length, ZEROS, offset,
                (lArg, rArg) -> { return rArg; });
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
        iBitwiseOp(thisOffset, length, that, thatOffset,
                (lArg, rArg) -> { return ~(lArg & rArg); });
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
        iBitwiseOp(thisOffset, length, that, thatOffset,
                (lArg, rArg) -> { return ~lArg | rArg; });
             // (lArg, rArg) -> { return ~(lArg & ~rArg); });
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
        iBitwiseOp(thisOffset, length, that, thatOffset,
                (lArg, rArg) -> { return ~(lArg | rArg); });
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
        iBitwiseOp(thisOffset, length, that, thatOffset,
                (lArg, rArg) -> { return ~lArg & rArg; });
             // (lArg, rArg) -> { return ~(lArg | !rArg); });
    }
    
    private void iNot(int offset, int length) {
        iBitwiseOp(offset, length, ZEROS, offset,
                (lArg, rArg) -> { return ~lArg; });
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
        iBitwiseOp(thisOffset, length, that, thatOffset,
                (lArg, rArg) -> { return lArg | rArg; });
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
        iBitwiseOp(thisOffset, length, that, thatOffset,
                (lArg, rArg) -> { return lArg | ~rArg; });
    }
    
    private void iSet(int offset, int length) {
        iBitwiseOp(offset, length, ONES, offset,
                (lArg, rArg) -> { return rArg; });
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
        iBitwiseOp(thisOffset, length, that, thatOffset,
                (lArg, rArg) -> { return ~(lArg ^ rArg); });
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
        iBitwiseOp(thisOffset, length, that, thatOffset,
                (lArg, rArg) -> { return lArg ^ rArg; });
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
    private void iBitwiseOp(int thisOffset, int length, BitString that, int thatOffset,
            LongBinaryOperator op) {
        
        assert (((long)thisOffset + length) <= this.length());
        assert (((long)thatOffset + length) <= that.length());
        if (length <= 0) return;
        
        final int thisFirstWordIndex = this.firstWordIndex(thisOffset);
        final int thisLastWordIndex = this.lastWordIndex(thisOffset, length);
        final int thatFirstWordIndex = that.firstWordIndex(thatOffset);
        final int thatLastWordIndex = that.lastWordIndex(thatOffset, length);
        final int thisLeftMarginSize = this.leftMarginSize(thisOffset);
        final int thisRightMarginSize = this.rightMarginSize(thisOffset, length);
        final int thatLeftMarginSize = that.leftMarginSize(thatOffset);
        final int thatRightMarginSize = that.rightMarginSize(thatOffset, length);
        
        final long thisOriginalFirstWord = that.getWord(thisFirstWordIndex);
        final long thisOriginalLastWord = that.getWord(thisLastWordIndex);
        
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
            
            setWord(thisWordCursor, op.applyAsLong(getWord(thisWordCursor), arg));
            
            restoreMargin(thisWordCursor,
                    thisOriginalFirstWord, thisOriginalLastWord,
                    thisFirstWordIndex, thisLastWordIndex,
                    thisLeftMarginSize, thisRightMarginSize);
            
        }
        
    }
    
    private void iBitwiseOpRL(int thisOffset, int length, BitString that, int thatOffset,
            LongBinaryOperator op) {
        
        assert (((long)thisOffset + length) <= this.length());
        assert (((long)thatOffset + length) <= that.length());
        if (length <= 0) return;
        
        final int thisFirstWordIndex = this.firstWordIndex(thisOffset);
        final int thisLastWordIndex = this.lastWordIndex(thisOffset, length);
        final int thatFirstWordIndex = that.firstWordIndex(thatOffset);
        final int thatLastWordIndex = that.lastWordIndex(thatOffset, length);
        final int thisLeftMarginSize = this.leftMarginSize(thisOffset);
        final int thisRightMarginSize = this.rightMarginSize(thisOffset, length);
        final int thatLeftMarginSize = that.leftMarginSize(thatOffset);
        final int thatRightMarginSize = that.rightMarginSize(thatOffset, length);
        
        final long thisOriginalFirstWord = that.getWord(thisFirstWordIndex);
        final long thisOriginalLastWord = that.getWord(thisLastWordIndex);
        
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
            
            setWord(thisWordCursor, op.applyAsLong(getWord(thisWordCursor), arg));
            
            restoreMargin(thisWordCursor,
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
        
        assert (((long)thisOffset + length) <= this.length());
        assert (((long)thatOffset + length) <= that.length());
        if (length <= 0) return false;
        
        final int[] thisIterator = this.getIterator(thisOffset, length);
        final int[] thatIterator = that.getIterator(thatOffset, length);
        while (this.hasNextIteratorWord(thisIterator)) {
            final long thisWord = this.getNextIteratorFullWord(thisIterator, fill);
            final long thatWord = that.getNextIteratorFullWord(thatIterator, fill);
            if (op.test(thisWord, thatWord)^dflt) return !dflt;
        }        
        
        return dflt;
        
    }
    
    private void iCopy(int thisOffset, int length, BitString that, int thatOffset) {
        iBitwiseOp(thisOffset, length, that, thatOffset,
                (lArg, rArg) -> { return rArg; });
    }
    
    private void iCopyRL(int thisOffset, int length, BitString that, int thatOffset) {
        iBitwiseOpRL(thisOffset, length, that, thatOffset,
                (lArg, rArg) -> { return rArg; });
    }
    
    private void iCopyNot(int thisOffset, int length, BitString that, int thatOffset) {
        iBitwiseOp(thisOffset, length, that, thatOffset,
                (lArg, rArg) -> { return ~rArg; });
    }
    
    private void iCopyNotRL(int thisOffset, int length, BitString that, int thatOffset) {
        iBitwiseOpRL(thisOffset, length, that, thatOffset,
                (lArg, rArg) -> { return ~rArg; });
    }
    
    private void iCopyFromFrontOf(int thisOffset, int thisLength, BitString that, int thatOffset, int thatLength) {
        assert this.isValidOffset(thisOffset);
        assert this.isValidLength(thisOffset, thisLength);
        assert that.isValidOffset(thatOffset);
        assert that.isValidLength(thatOffset, thatLength);
        if (thisLength == 0) return;
        
        final int copyLength = Math.min(thisLength, thatLength);
        final int clearLength = thisLength - copyLength;
        
        iCopy(thisOffset, copyLength, that, thatOffset);
        iClear(thisOffset + copyLength, clearLength);
    }
    
    private void iCopyFromBackOf(int thisOffset, int thisLength, BitString that, int thatOffset, int thatLength) {
        assert this.isValidOffset(thisOffset);
        assert this.isValidLength(thisOffset, thisLength);
        assert that.isValidOffset(thatOffset);
        assert that.isValidLength(thatOffset, thatLength);
        if (thisLength == 0) return;
        
        final int copyLength = Math.min(thisLength, thatLength);
        final int clearLength = thisLength - copyLength;
        
        iCopyRL(thisOffset + clearLength, copyLength, that, thatOffset);
        iClear(thisOffset, clearLength);
    }
    
    private void iCopyNotFromFrontOf(int thisOffset, int thisLength, BitString that, int thatOffset, int thatLength) {
        assert this.isValidOffset(thisOffset);
        assert this.isValidLength(thisOffset, thisLength);
        assert that.isValidOffset(thatOffset);
        assert that.isValidLength(thatOffset, thatLength);
        if (thisLength == 0) return;
        
        final int copyLength = Math.min(thisLength, thatLength);
        final int setLength = thisLength - copyLength;
        
        iCopyNot(thisOffset, copyLength, that, thatOffset);
        iSet(thisOffset + copyLength, setLength);
    }
    
    private void iCopyNotFromBackOf(int thisOffset, int thisLength, BitString that, int thatOffset, int thatLength) {
        assert this.isValidOffset(thisOffset);
        assert this.isValidLength(thisOffset, thisLength);
        assert that.isValidOffset(thatOffset);
        assert that.isValidLength(thatOffset, thatLength);
        if (thisLength == 0) return;
        
        final int copyLength = Math.min(thisLength, thatLength);
        final int setLength = thisLength - copyLength;
        
        iCopyNotRL(thisOffset + setLength, copyLength, that, thatOffset);
        iSet(thisOffset, setLength);
    }
    
    private void iReverse(int offset, int length) {
        assert isValidOffset(offset);
        assert isValidLength(offset, length);
        if (length == 0) return;
        
        final int firstWordIndex = firstWordIndex(offset);
        final int lastWordIndex = lastWordIndex(offset, length);
        final int leftMarginSize = leftMarginSize(offset);
        final int rightMarginSize = rightMarginSize(offset, length);
        
        // basically swap the first reversed word and the last reversed word,
        // the second reversed word and the next to last reversed word, etc.
        
        int frontLastBitIndex = firstBitIndex(offset) - 1; //offset-1;
        int backFirstBitIndex = lastBitIndex(offset, length) + 1; //length;
        int frontWordBitCount = 0;
        int backWordBitCount = 0;
        int frontWordIndex = -1;
        int backWordIndex = -1;
        long frontWordReversed = 0L;
        long backWordReversed = 0L;
        
        final int[] frontIterator = getIterator(offset, length);
        final int[] backIterator = getIterator(offset, length);
        int numberOfBitsRemainingToReverse = length;
        while (numberOfBitsRemainingToReverse > 0) {
            
            if (hasNextIteratorWord(frontIterator)) {
                frontWordReversed = Long.reverse(getNextIteratorFullWord(frontIterator));
                frontWordIndex = getIteratorWordIndex(frontIterator);
                frontWordBitCount = getIteratorWordBitCount(frontIterator);
                frontLastBitIndex += frontWordBitCount;
            } else {
                frontWordBitCount = 0;
            }
            if (hasPreviousIteratorWord(backIterator)) {
                backWordReversed = Long.reverse(getPreviousIteratorFullWord(backIterator));
                backWordIndex = getIteratorWordIndex(backIterator);
                backWordBitCount = getIteratorWordBitCount(backIterator);
                backFirstBitIndex -= backWordBitCount;
            } else {
                backWordBitCount = 0;
            }
            
            // do the front word and back word overlap?
            if (frontLastBitIndex >= backFirstBitIndex) {
                final int delta = frontLastBitIndex - backFirstBitIndex;
                if (frontWordBitCount > backWordBitCount) {
                    frontWordBitCount -= (delta + 1);
                } else {
                    backWordBitCount -= (delta + 1);
                }
            }
            numberOfBitsRemainingToReverse -= (frontWordBitCount + backWordBitCount);
            
            // front
            if (backWordBitCount > 0) {
                long word = backWordReversed;
                final int thisWordSize = (leftMarginSize + backWordBitCount > BITS_PER_WORD) ?
                        BITS_PER_WORD - leftMarginSize : backWordBitCount;
                final int nextWordSize = backWordBitCount - thisWordSize;
                final int thisRightMarginSize = BITS_PER_WORD - leftMarginSize - thisWordSize;
                final long originalWord = getWord(frontWordIndex);
                if (leftMarginSize > 0) {
                    word = (originalWord & (WORD_MASK << BITS_PER_WORD - leftMarginSize)) // preserve left margin
                         | (backWordReversed >>> leftMarginSize);
                    if (nextWordSize > 0 && frontWordIndex+1 <= lastWordIndex) {
                        setWord(frontWordIndex+1,
                                (backWordReversed << BITS_PER_WORD - nextWordSize)
                              | (getWord(frontWordIndex+1) & (WORD_MASK >>> nextWordSize))); 
                    }
                }
                // restore right margin
                if (thisRightMarginSize > 0) {
                    word = (word & (WORD_MASK << thisRightMarginSize))
                         | (originalWord & (WORD_MASK >>> BITS_PER_WORD - thisRightMarginSize));
                }
                setWord(frontWordIndex, word);
            }

            // back
            if (frontWordBitCount > 0) {
                long word = frontWordReversed;
                final int thisWordSize = (rightMarginSize + frontWordBitCount > BITS_PER_WORD) ?
                        BITS_PER_WORD - rightMarginSize : frontWordBitCount;
                final int prevWordSize = frontWordBitCount - thisWordSize;
                final int thisLeftMarginSize = BITS_PER_WORD - rightMarginSize - thisWordSize;
                final long originalWord = getWord(backWordIndex);
                if (rightMarginSize > 0) {
                    word = (frontWordReversed << rightMarginSize)
                         | (originalWord & (WORD_MASK >>> BITS_PER_WORD - rightMarginSize)); // preserve right margin
                    if (prevWordSize > 0 && backWordIndex-1 >= firstWordIndex) {
                        setWord(backWordIndex-1,
                                (getWord(backWordIndex-1) & (WORD_MASK << prevWordSize))
                              | (frontWordReversed >>> BITS_PER_WORD - prevWordSize)); 
                    } 
                }
                // restore left margin
                if (thisLeftMarginSize > 0) {
                    word = (originalWord & (WORD_MASK << BITS_PER_WORD - thisLeftMarginSize))
                         | (word & (WORD_MASK >>> thisLeftMarginSize));
                }
                setWord(backWordIndex, word); 
            }
        }
    }
    
    private void iRotateLeft(int nBits, int offset, int length) {
        assert isValidOffset(offset);
        assert isValidLength(offset, length);
        if (nBits == 0 || length == 0) return;
        if (nBits < 0) {
            if (nBits == Integer.MIN_VALUE) {
                iRotateRight(-(nBits + length), offset, length);
                //iRotateRight(1, offset, length);
                //iRotateRight(Integer.MAX_VALUE, offset, length);
            } else {
                iRotateRight(-nBits, offset, length); 
            }
            return;
        }
        
        final int shift = nBits % length;
        if (shift == 0) return;
        final BitString spill = new LongBitString(shift);
        this.iShiftLeft(shift, ZERO_FILL, offset, length, spill, 0, shift);
        spill.iShiftLeft(shift, ZERO_FILL, 0, shift, this, offset+length-shift, shift);
        //iCopyFromFrontOf(offset+length-shift, shift, spill, 0, shift);
    }
    
    private void iRotateLeft(int nBits, int thisOffset, int thisLength, BitString that, int thatOffset, int thatLength) {
        assert this.isValidOffset(thisOffset);
        assert this.isValidLength(thisOffset, thisLength);
        assert that.isValidOffset(thatOffset);
        assert that.isValidLength(thatOffset, thatLength);
        if (nBits == 0) return;
        if (thatLength == 0) {
            iRotateLeft(nBits, thisOffset, thisLength);
            return;
        }
        if (nBits < 0) {
            if (nBits == Integer.MIN_VALUE) {
                if (thisLength+thatLength <= -((long)nBits)) {
                    iRotateRight(-(nBits+thisLength+thatLength), thisOffset, thisLength, that, thatOffset, thatLength);
                } else {
                    iRotateRight(1, thisOffset, thisLength, that, thatOffset, thatLength);
                    iRotateRight(Integer.MAX_VALUE, thisOffset, thisLength, that, thatOffset, thatLength);
                }
            } else {
                iRotateRight(-nBits, thisOffset, thisLength, that, thatOffset, thatLength);
            }
            return;
        }
        
        final int shift = (int)(nBits % ((long)thisLength+thatLength));
        if (shift == 0) return;
        final BitString spill = new LongBitString(shift);
        
        int thatShift = Math.min(shift, thatLength);
        //int thatShift = shift > thatLength ? thatLength : shift;
        int thisShift = shift - thatShift;
        that.iShiftLeft(thatShift, ZERO_FILL, thatOffset, thatShift, spill, 0, shift);
        if (thisShift > 0) {
            this.iShiftLeft(thisShift, ZERO_FILL, thisOffset, thisShift, spill, 0, shift);
        }
        
        this.iShiftLeft(shift, ZERO_FILL, thisOffset, thisLength, that, thatOffset, thatLength);
        
        thisShift = Math.min(shift, thisLength);
        //thisShift = shift > thisLength ? thisLength : shift;
        thatShift = shift - thisShift;
        if (thatShift > 0) {
            spill.iShiftLeft(thatShift, ZERO_FILL, 0, shift, that, thatOffset+thatLength-thatShift, thatShift);
        }
        spill.iShiftLeft(thisShift, ZERO_FILL, 0, shift, this, thisOffset+thisLength-thisShift, thisShift);
    }
    
    private void iRotateRight(int nBits, int offset, int length) {
        assert isValidOffset(offset);
        assert isValidLength(offset, length);
        if (nBits == 0 || length == 0) return;
        if (nBits < 0) {
            if (nBits == Integer.MIN_VALUE) {
                iRotateRight(-(nBits + length), offset, length);
                //iRotateLeft(1, offset, length);
                //iRotateLeft(Integer.MAX_VALUE, offset, length);
            } else {
                iRotateLeft(-nBits, offset, length); 
            }
            return;
        }
        
        final int shift = nBits % length;
        if (shift == 0) return;
        final BitString spill = new LongBitString(shift);
        this.iShiftRight(shift, ZERO_FILL, offset, length, spill, 0, shift);
        spill.iShiftRight(shift, ZERO_FILL, 0, shift, this, offset, shift);
        //iCopyFromFrontOf(offset, shift, spill, 0, shift);
    }
    
    private void iRotateRight(int nBits, int thisOffset, int thisLength, BitString that, int thatOffset, int thatLength) {
        assert this.isValidOffset(thisOffset);
        assert this.isValidLength(thisOffset, thisLength);
        assert that.isValidOffset(thatOffset);
        assert that.isValidLength(thatOffset, thatLength);
        if (nBits == 0) return;
        if (thatLength == 0) {
            iRotateRight(nBits, thisOffset, thisLength);
            return;
        }
        if (nBits < 0) {
            if (nBits == Integer.MIN_VALUE) {
                if (thisLength+thatLength <= -((long)nBits)) {
                    iRotateLeft(-(nBits+thisLength+thatLength), thisOffset, thisLength, that, thatOffset, thatLength);
                } else {
                    iRotateLeft(1, thisOffset, thisLength, that, thatOffset, thatLength);
                    iRotateLeft(Integer.MAX_VALUE, thisOffset, thisLength, that, thatOffset, thatLength);
                }
            } else {
                iRotateLeft(-nBits, thisOffset, thisLength, that, thatOffset, thatLength);
            }
            return;
        }
        
        final int shift = (int)(nBits % ((long)thisLength+thatLength));
        if (shift == 0) return;
        final BitString spill = new LongBitString(shift);
        
        int thatShift = Math.min(shift, thatLength);
        //int thatShift = shift > thatLength ? thatLength : shift;
        int thisShift = shift - thatShift;
        that.iShiftRight(thatShift, ZERO_FILL, thatOffset+thatLength-thatShift, thatShift, spill, 0, shift);
        if (thisShift > 0) {
            this.iShiftRight(thisShift, ZERO_FILL, thisOffset+thisLength-thisShift, thisShift, spill, 0, shift);
        }
        
        this.iShiftRight(shift, ZERO_FILL, thisOffset, thisLength, that, thatOffset, thatLength);
        
        thisShift = Math.min(shift, thisLength);
        //thisShift = shift > thisLength ? thisLength : shift;
        thatShift = shift - thisShift;
        if (thatShift > 0) {
            spill.iShiftRight(thatShift, ZERO_FILL, 0, shift, that, thatOffset, thatShift);
        }
        spill.iShiftRight(thisShift, ZERO_FILL, 0, shift, this, thisOffset, thisShift);
    }
    
    private void iShiftLeft(int nBits, boolean fill, int offset, int length) {
        assert isValidOffset(offset);
        assert isValidLength(offset, length);
        if (nBits == 0 || length == 0) return;
        if (nBits < 0) {
            if (nBits == Integer.MIN_VALUE) {
                iShiftRight(1, fill, offset, length);
                iShiftRight(Integer.MAX_VALUE, fill, offset, length);
            } else {
                iShiftRight(-nBits, fill, offset, length); 
            }
            return;
        }
        
        if (nBits < length) iCopyFromFrontOf(offset, length, this, offset + nBits, length - nBits);
        else iClear(offset, length);
    }
    
    private void iShiftLeft(int nBits, boolean fill,
            int thisOffset, int thisLength,
            BitString that, int thatOffset, int thatLength) {
        assert this.isValidOffset(thisOffset);
        assert this.isValidLength(thisOffset, thisLength);
        assert that.isValidOffset(thatOffset);
        assert that.isValidLength(thatOffset, thatLength);
        if (nBits == 0) return;
        if (thatLength == 0) {
            this.iShiftLeft(nBits, fill, thisOffset, thisLength);
            return;
        }
        if (thisLength == 0) {
            that.iShiftLeft(nBits, fill, thatOffset, thatLength);
            return;
        }
        if (nBits < 0) {
            if (nBits == Integer.MIN_VALUE) {
                iShiftRight(1, fill, thisOffset, thisLength, that, thatOffset, thatLength);
                iShiftRight(Integer.MAX_VALUE, fill, thisOffset, thisLength, that, thatOffset, thatLength);
            } else {
                iShiftRight(-nBits, fill, thisOffset, thisLength, that, thatOffset, thatLength); 
            }
            return;
        }
        
        that.iShiftLeft(nBits, fill, thatOffset, thatLength);
        
        // Copy bits from the front of this BitString into that BitString
        // Insert the copied bits after the original last bit, of that BitString,
        // which has been shifted left thatNBits
        final int thisNBits = Math.min(nBits, thisLength);
        final int thatNBits = Math.min(nBits, thatLength);
        final int copyLength = Math.min(thisNBits, thatNBits);
        if (nBits < thisLength + thatLength) {
            that.iCopy(thatOffset - thatNBits + thatLength, copyLength, this, thisOffset - copyLength + thisNBits);
        }
        
        this.iShiftLeft(nBits, fill, thisOffset, thisLength);
    }
    
    private void iShiftRight(int nBits, boolean fill, int offset, int length) {
        assert nBits != Integer.MIN_VALUE;
        assert isValidOffset(offset);
        assert isValidLength(offset, length);
        if (nBits == 0 || length == 0) return;
        if (nBits < 0) {
            if (nBits == Integer.MIN_VALUE) {
                iShiftLeft(1, fill, offset, length);
                iShiftLeft(Integer.MAX_VALUE, fill, offset, length);
            } else {
                iShiftLeft(-nBits, fill, offset, length); 
            }
            return;
        }
        
        if (nBits < length) iCopyFromBackOf(offset, length, this, offset, length - nBits);
        else iClear(offset, length);
    }
    
    private void iShiftRight(int nBits, boolean fill,
            int thisOffset, int thisLength,
            BitString that, int thatOffset, int thatLength) {
        assert nBits != Integer.MIN_VALUE;
        assert this.isValidOffset(thisOffset);
        assert this.isValidLength(thisOffset, thisLength);
        assert that.isValidOffset(thatOffset);
        assert that.isValidLength(thatOffset, thatLength);
        if (nBits == 0) return;
        if (thatLength == 0) {
            this.iShiftRight(nBits, fill, thisOffset, thisLength);
            return;
        }
        if (thisLength == 0) {
            that.iShiftRight(nBits, fill, thatOffset, thatLength);
            return;
        }
        if (nBits < 0) {
            if (nBits == Integer.MIN_VALUE) {
                iShiftLeft(1, fill, thisOffset, thisLength, that, thatOffset, thatLength);
                iShiftLeft(Integer.MAX_VALUE, fill, thisOffset, thisLength, that, thatOffset, thatLength);
            } else {
                iShiftLeft(-nBits, fill, thisOffset, thisLength, that, thatOffset, thatLength); 
            }
            return;
        }
        
        that.iShiftRight(nBits, fill, thatOffset, thatLength);
        
        // Copy bits from the end of this BitString into that BitString.
        // Insert the copied bits before the original first bit, of that BitString,
        // which has been shifted right thatNBits.
        final int thisNBits = Math.min(nBits, thisLength);
        final int thatNBits = Math.min(nBits, thatLength);
        final int copyLength = Math.min(thisNBits, thatNBits);
        if (nBits < thisLength + thatLength) {
            that.iCopyRL(thatOffset - copyLength + thatNBits, copyLength, this, thisOffset - thisNBits + thisLength);
        }
        
        this.iShiftRight(nBits, fill, thisOffset, thisLength);
    }
    
    private long iGetPrimitive(int offset, int primitiveSize) {
        assert isValidOffset(offset);
        assert primitiveSize <= BITS_PER_WORD;
        final int bitIndex = bitIndex(offset);
        final long word = shiftWordLeft(wordBitIndex(bitIndex), ZERO_FILL, wordIndex(bitIndex),
                lastWordIndex(), rightMarginSize());
        return word >> (BITS_PER_WORD - primitiveSize);
    }
    
    private void iPutPrimitive(int offset, int primitiveSize, long primitiveBits) {
        assert isValidOffset(offset);
        assert primitiveSize <= BITS_PER_WORD;
        
        final int firstWordIndex = firstWordIndex(offset);
        final int firstWordBitIndex = firstWordBitIndex(offset);
          
        final int primitiveFirstWordBitIndex = BITS_PER_WORD - primitiveSize;
        
        long mask = WORD_MASK >>> primitiveFirstWordBitIndex;
        int shift = primitiveFirstWordBitIndex - firstWordBitIndex;
        
        if (shift >= 0) {
            setWord(firstWordIndex,
                    (getWord(firstWordIndex) & ~(mask << shift)) | (primitiveBits << shift));
        } else {
            shift = -shift;
            setWord(firstWordIndex,
                    (getWord(firstWordIndex) & ~(mask >>> shift)) | (primitiveBits >>> shift));
            setWord(firstWordIndex + 1,
                    (getWord(firstWordIndex + 1) & (WORD_MASK >>> shift)) | (primitiveBits << (BITS_PER_WORD - shift)));
        }
    }
    
    private boolean[] iToBooleanArray(int offset, int length) {
        assert isValidOffset(offset);
        assert isValidLength(offset, length);
        if (length == 0) return new boolean[0];
        final boolean[] booleans = new boolean[length];
        iToPrimitiveArray(offset, length, 1,
                (index, word) -> { booleans[index] = (word == 0) ? false : true; });
        return booleans;
    }
    
    private byte[] iToByteArray(int offset, int length) {
        assert isValidOffset(offset);
        assert isValidLength(offset, length);
        if (length == 0) return new byte[0];
        final byte[] bytes = new byte[(length - 1) / Byte.SIZE + 1];
        iToPrimitiveArray(offset, length, Byte.SIZE,
                (index, word) -> { bytes[index] = (byte)word; });
        return bytes;
    }
    
    private char[] iToCharArray(int offset, int length) {
        assert isValidOffset(offset);
        assert isValidLength(offset, length);
        if (length == 0) return new char[0];
        final char[] chars = new char[(length - 1) / Character.SIZE + 1];
        iToPrimitiveArray(offset, length, Character.SIZE,
                (index, word) -> { chars[index] = (char)word; });
        return chars;
    }
    
    private double[] iToDoubleArray(int offset, int length) {
        assert isValidOffset(offset);
        assert isValidLength(offset, length);
        if (length == 0) return new double[0];
        final double[] doubles = new double[(length - 1) / Long.SIZE + 1];
        iToPrimitiveArray(offset, length, Long.SIZE,
                (index, word) -> { doubles[index] = Double.longBitsToDouble(word); });
        return doubles;
    }
    
    private float[] iToFloatArray(int offset, int length) {
        assert isValidOffset(offset);
        assert isValidLength(offset, length);
        if (length == 0) return new float[0];
        final float[] floats = new float[(length - 1) / Integer.SIZE + 1];
        iToPrimitiveArray(offset, length, Integer.SIZE,
                (index, word) -> { floats[index] = Float.intBitsToFloat((int)word); });
        return floats;
    }
    
    private int[] iToIntArray(int offset, int length) {
        assert isValidOffset(offset);
        assert isValidLength(offset, length);
        if (length == 0) return new int[0];
        final int[] ints = new int[(length - 1) / Integer.SIZE + 1];
        iToPrimitiveArray(offset, length, Integer.SIZE,
                (index, word) -> { ints[index] = (int)word; });
        return ints;
    }
    
    private long[] iToLongArray(int offset, int length) {
        assert isValidOffset(offset);
        assert isValidLength(offset, length);
        if (length == 0) return new long[0];
        final long[] longs = new long[(length - 1) / Long.SIZE + 1];
        iToPrimitiveArray(offset, length, Long.SIZE,
                (index, word) -> { longs[index] = word; });
        return longs;
    }
    
    private short[] iToShortArray(int offset, int length) {
        assert isValidOffset(offset);
        assert isValidLength(offset, length);
        if (length == 0) return new short[0];
        final short[] shorts = new short[(length - 1) / Short.SIZE + 1];
        iToPrimitiveArray(offset, length, Short.SIZE,
                (index, word) -> { shorts[index] = (short)word; });
        return shorts;
    }
    
    private void iToPrimitiveArray(int offset, int length,
            int primitiveSize,
            IntLongConsumer setPrimitiveArrayElementFromUnsignedLong) {
        assert isValidOffset(offset);
        assert isValidLength(offset, length);
        final int primitivesPerWord = BITS_PER_WORD / primitiveSize;
        final long primitiveMask = WORD_MASK >>> (BITS_PER_WORD - primitiveSize);
        int index = 0;
        final int[] iterator = getIterator(offset, length);
        while (length > 0) {
            final long word = getNextIteratorFullWord(iterator);
            for (int p = primitivesPerWord - 1; p >= 0 && length > 0; p--, length -= primitiveSize) {
                setPrimitiveArrayElementFromUnsignedLong.accept(index++, (word >>> (p * primitiveSize)) & primitiveMask);
            }
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
        return iNumberOfLeadingOrTrailingOnesOrZeros(offset, length,
                iterator -> { return hasNextIteratorWord(iterator); },
                iterator -> { return ~getNextIteratorFullWord(iterator, ZERO_FILL); },
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
        return iNumberOfLeadingOrTrailingOnesOrZeros(offset, length,
                iterator -> { return hasNextIteratorWord(iterator); },
                iterator -> { return getNextIteratorFullWord(iterator, ONE_FILL); },
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
        return iNumberOfLeadingOrTrailingOnesOrZeros(offset, length,
                iterator -> { return hasPreviousIteratorWord(iterator); },
                iterator -> { return ~getPreviousIteratorFullWord(iterator, ZERO_FILL); },
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
        return iNumberOfLeadingOrTrailingOnesOrZeros(offset, length,
                iterator -> { return hasPreviousIteratorWord(iterator); },
                iterator -> { return getPreviousIteratorFullWord(iterator, ONE_FILL); },
                word -> { return Long.numberOfTrailingZeros(word); });
    }

    /**
     * Returns the number of leading/trailing {@code ONES}/{@code ZEROS} of the specified substring of
     * this {@code BitString}.
     * 
     * @param offset the start of the substring
     * @param length the length of the substring
     * @param hasIteratorWord a predicate that indicates if there exists a next or previous word
     * @param getIteratorFullWord a function that returns the next or previous full word
     * @param numberOfLeadingOrTrailingZeros a function that counts the number of leading or trailing Zeros of a word
     * @return the number of leading/trailing {@code ONES}/{@code ZEROS} of the substring
     */
    private int iNumberOfLeadingOrTrailingOnesOrZeros(int offset, int length,
            Predicate<int[]> hasIteratorWord,
            ToLongFunction<int[]> getIteratorFullWord,
            LongToIntFunction numberOfLeadingOrTrailingZeros) {
        assert isValidOffset(offset);
        assert isValidLength(offset, length);
        int count = 0;
        final int[] iterator = getIterator(offset, length);
        while (hasIteratorWord.test(iterator)) {
            final long word = getIteratorFullWord.applyAsLong(iterator);
            if (word != 0L) {
                count += numberOfLeadingOrTrailingZeros.applyAsInt(word);
                break;
            }
            count += BITS_PER_WORD;
        }
        return count;
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
    
    public BitString append(BitString that) {
        final int thatLength = that.length();
        checkBaseLengthIncrease(thatLength);
        iAppend(that, 0, thatLength);
        return this;
    }
    
    public BitString append(BitString that, int thatOffset, int thatLength) {
        that.checkArgOffset(thatOffset);
        that.checkArgLength(thatOffset, thatLength);
        checkBaseLengthIncrease(thatLength);
        iAppend(that, thatOffset, thatLength);
        return this;
    }
    
    public BitString append(BitString that, Field thatField) {
        return append(that, thatField.offset(), thatField.length(that));
    }
    
    public BitString delete() {
        final int length = length();
        iDelete(firstBitIndex(0), length);
        return this;
    }
    
    public BitString delete(int offset, int length) {
        checkThisOffset(offset);
        checkThisLength(offset, length);
        iDelete(firstBitIndex(offset), length);
        return this;
    }
    
    public BitString delete(Field field) {
        return delete(field.offset(), field.length(this));
    }
    
    public BitString insert(int position, BitString that) {
        checkThisPosition(position);
        final int thatLength = that.length();
        checkBaseLengthIncrease(thatLength);
        iInsert(bitIndex(position), that, 0, thatLength);
        return this;
    }
    
    public BitString insert(int position, BitString that, int thatOffset, int thatLength) {
        checkThisPosition(position);
        that.checkArgOffset(thatOffset);
        that.checkArgLength(thatOffset, thatLength);
        checkBaseLengthIncrease(thatLength);
        iInsert(bitIndex(position), that, thatOffset, thatLength);
        return this;
    }
    
    public BitString insert(int position, BitString that, Field thatField) {
        return insert(position, that, thatField.offset(), thatField.length(that));
    }
    
    public BitString replace(BitString that) {
        final int thisLength = this.length();
        final int thatLength = that.length();
        checkBaseLengthIncrease(thatLength - thisLength);
        iReplace(firstBitIndex(0), thisLength, that, 0, thatLength);
        return this;
    }
    
    public BitString replace(int thisOffset, int thisLength, BitString that, int thatOffset, int thatLength) {
        checkThisOffset(thisOffset);
        checkThisLength(thisOffset, thisLength);
        that.checkArgOffset(thatOffset);
        that.checkArgLength(thatOffset, thatLength);
        checkBaseLengthIncrease(thatLength - thisLength);
        iReplace(firstBitIndex(thisOffset), thisLength, that, thatOffset, thatLength);
        return this;
    }
    
    public BitString replace(Field thisField, BitString that, Field thatField) {
        return replace(thisField.offset(), thisField.length(this), that, thatField.offset(), thatField.length(that));
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
     * Sets all of the bits in a substring of this {@code BitString} to {@code ZERO}.
     * 
     * This substring starts at offset 'offset' of this {@code BitString} and
     * extends to the end of this {@code BitString}.
     *
     * @param offset the start of this substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     */
    public BitString clear(int offset) {
        checkThisOffset(offset);
        iClear(offset, length() - offset);
        return this;
    }
    
    /**
     * Sets all of the bits in a substring of this {@code BitString} to {@code ZERO}.
     *
     * The substring starts at offset 'offset' of this {@code BitString} and has a
     * length of 'length'.
     * 
     * @param offset the start of this substring
     * @param length the length of this substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     * @throws IllegalArgumentException        if
     *                                         {@code length < 0 || length > length() - offset}
     */
    public BitString clear(int offset, int length) {
        checkThisOffset(offset);
        checkThisLength(offset, length);
        iClear(offset, length);
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
        return clear(field.offset(), field.length(this));
    }
    
    /**
     * Sets the bit at the specified offset to {@code ZERO}.
     * 
     * @param bitOffset the offset of the bit to set
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code bitOffset < 0 || bitOffset > 0 && bitOffset >= length()}
     */
    public BitString clearBit(int bitOffset) {
        checkThisOffset(bitOffset);
        final int bitIndex = bitIndex(bitOffset);
        int wordIndex = wordIndex(bitIndex);
        setWord(wordIndex, getWord(wordIndex) & ~(BIT_MASK >>> wordBitIndex(bitIndex)));
        return this;
    }
    
    public BitString clearBit(int bitOffset, int offset, int length) {
        checkThisOffset(offset);
        checkThisLength(offset, length);
        checkRelativeOffset(bitOffset, length);
        return clearBit(offset + bitOffset);
    }
    
    public BitString clearBit(int bitOffset, Field field) {
        return clearBit(bitOffset, field.offset(), field.length(this));
    }
    
    /**
     * Sets all of the bits in this {@code BitString} to the complement of its
     * current value.
     * 
     * @return this {@code BitString}
     */
    public BitString flip() {
        iNot(0, length());
        return this;
    }
    
    /**
     * Sets all of the bits in a substring of this {@code BitString} to the
     * complement of its current value.
     *
     * The substring starts at offset 'offset' of this {@code BitString} and has a
     * length of 'length'.
     * 
     * @param offset the start of this substring
     * @param length the length of this substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     * @throws IllegalArgumentException        if
     *                                         {@code length < 0 || length > length() - offset}
     */  
    public BitString flip(int offset, int length) {
        checkThisOffset(offset);
        checkThisLength(offset, length);
        iNot(offset, length);
        return this;
    }
    
    /**
     * Sets all of the bits in a Field of this {@code BitString} to the complement
     * of its current value.
     * 
     * @param field a Field of this {@code BitString}
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code field.offset() > 0 && field.offset() >= length()}
     * @throws IllegalArgumentException        if
     *                                         {@code field.length() > length() - field.offset()}
     */
    public BitString flip(Field field) {
        return flip(field.offset(), field.length(this));
    }
    
    /**
     * Sets the bit at the specified offset to the complement of its current value.
     * 
     * @param bitOffset the offset of the bit to flip
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     */
    public BitString flipBit(int bitOffset) {
        checkThisOffset(bitOffset);
        int wordIndex = wordIndex(bitIndex(bitOffset));
        setWord(wordIndex, getWord(wordIndex) ^ (BIT_MASK >>> wordBitIndex(bitIndex(bitOffset))));
        return this;
    }
    
    public BitString flipBit(int bitOffset, int offset, int length) {
        checkThisOffset(offset);
        checkThisLength(offset, length);
        checkRelativeOffset(bitOffset, length);
        return setBit(offset + bitOffset);
    }
    
    public BitString flipBit(int bitOffset, Field field) {
        return flipBit(bitOffset, field.offset(), field.length(this));
    }
    
    /**
     * Returns all of the bits in this {@code BitString}.
     * 
     * @return a copy of this {@code BitString}
     */
    public BitString get() {
        return get(0, length());
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
    public BitString get(int offset) {
        checkThisOffset(offset);
        return get(offset, this.length() - offset);
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
    public BitString get(int offset, int length) {
        checkThisOffset(offset);
        checkThisLength(offset, length);
        final BitString substring = newBitString(length);
        substring.iCopy(0, length, this, offset);
        return substring;
    }
    
    /**
     * Returns a Field of this {@code BitString}.
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
     * Returns the bit at the specified offset.
     * 
     * @param bitOffset the offset of the bit to get
     * @return the bit at the specified offset
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     */
    public boolean getBit(int bitOffset) {
        checkThisOffset(bitOffset);
        final int bitIndex = bitIndex(bitOffset);
        return (getWord(wordIndex(bitIndex)) & (BIT_MASK >>> wordBitIndex(bitIndex))) != 0L;
    }
    
    public boolean getBit(int bitOffset, int offset, int length) {
        checkThisOffset(offset);
        checkThisLength(offset, length);
        checkRelativeOffset(bitOffset, length);
        return getBit(offset + bitOffset);
    }
    
    public boolean getBit(int bitOffset, Field field) {
        return getBit(bitOffset, field.offset(), field.length(this));
    }
    
    public boolean getBoolean(int offset) {
        return getBit(offset);
    }
    
    public byte getByte(int offset) {
        checkThisOffset(offset);
        checkAvailableSpace(offset, Byte.SIZE);
        return (byte)(iGetPrimitive(offset, Byte.SIZE));
    }
    
    public char getChar(int offset) {
        assert Character.SIZE <= BITS_PER_WORD;
        checkThisOffset(offset);
        checkAvailableSpace(offset, Character.SIZE);
        return (char)(iGetPrimitive(offset, Character.SIZE));
    }
    
    public double getDouble(int offset) {
        checkThisOffset(offset);
        checkAvailableSpace(offset, Long.SIZE);
        return Double.longBitsToDouble((long)(iGetPrimitive(offset, Long.SIZE)));
    }
    
    public float getFloat(int offset) {
        checkThisOffset(offset);
        checkAvailableSpace(offset, Integer.SIZE);
        return Float.intBitsToFloat((int)(iGetPrimitive(offset, Integer.SIZE)));
    }
    
    public int getInt(int offset) {
        checkThisOffset(offset);
        checkAvailableSpace(offset, Integer.SIZE);
        return (int)(iGetPrimitive(offset, Integer.SIZE));
    }
    
    public long getLong(int offset) {
        checkThisOffset(offset);
        checkAvailableSpace(offset, Long.SIZE);
        return (long)(iGetPrimitive(offset, Long.SIZE));
    }
    
    public short getShort(int offset) {
        checkThisOffset(offset);
        checkAvailableSpace(offset, Short.SIZE);
        return (short)(iGetPrimitive(offset, Short.SIZE));
    }
    
    public BitString put(BitString that) {
        final int length = that.length();
        checkAvailableSpace(0, length);
        iCopy(0, length, that, 0);
        return this;
    }
    
    public BitString put(int offset, BitString that) {
        checkThisOffset(offset);
        final int length = that.length();
        checkAvailableSpace(offset, length);
        iCopy(offset, length, that, 0);
        return this;
    }
    
    public BitString put(int offset, BitString that, int thatOffset, int thatLength) {
        checkThisOffset(offset);
        checkArgOffset(thatOffset);
        checkArgLength(thatOffset, thatLength);
        checkAvailableSpace(offset, thatLength);
        iCopy(offset, thatLength, that, thatOffset);
        return this;
    }
    
    public BitString put(int offset, BitString that, Field thatField) {
        return put(offset, that, thatField.offset(), thatField.length(that));
    }
    
    public BitString putBoolean(int offset, boolean primitive) {
        return setBit(primitive, offset);
    }
    
    public BitString putByte(int offset, byte primitive) {
        checkThisOffset(offset);
        checkAvailableSpace(offset, Byte.SIZE);
        iPutPrimitive(offset, Byte.SIZE, Byte.toUnsignedLong(primitive));
        return this;
    }
    
    public BitString putChar(int offset, char primitive) {
        checkThisOffset(offset);
        checkAvailableSpace(offset, Character.SIZE);
        iPutPrimitive(offset, Character.SIZE, (long)primitive);
        return this;
    }
    
    public BitString putDouble(int offset, double primitive) {
        checkThisOffset(offset);
        checkAvailableSpace(offset, Long.SIZE);
        iPutPrimitive(offset, Long.SIZE, Double.doubleToRawLongBits(primitive));
        return this;
    }
    
    public BitString putFloat(int offset, float primitive) {
        checkThisOffset(offset);
        checkAvailableSpace(offset, Integer.SIZE);
        iPutPrimitive(offset, Integer.SIZE, Integer.toUnsignedLong(Float.floatToRawIntBits(primitive)));
        return this;
    }
    
    public BitString putInt(int offset, int primitive) {
        checkThisOffset(offset);
        checkAvailableSpace(offset, Integer.SIZE);
        iPutPrimitive(offset, Integer.SIZE, Integer.toUnsignedLong(primitive));
        return this;
    }
    
    public BitString putLong(int offset, long primitive) {
        checkThisOffset(offset);
        checkAvailableSpace(offset, Long.SIZE);
        iPutPrimitive(offset, Long.SIZE, primitive);
        return this;
    }
    
    public BitString putShort(int offset, short primitive) {
        checkThisOffset(offset);
        checkAvailableSpace(offset, Short.SIZE);
        iPutPrimitive(offset, Short.SIZE, Short.toUnsignedLong(primitive));
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
     * Sets all of the bits in a substring of this {@code BitString} to {@code ONE}.
     * 
     * This substring starts at offset 'offset' of this {@code BitString} and
     * extends to the end of this {@code BitString}.
     *
     * @param offset the start of this substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     */
    public BitString set(int offset) {
        checkThisOffset(offset);
        iSet(offset, length() - offset);
        return this;
    }
    
    /**
     * Sets all of the bits in a substring of this {@code BitString} to {@code ONE}.
     *
     * The substring starts at offset 'offset' of this {@code BitString} and has a
     * length of 'length'.
     * 
     * @param offset the start of this substring
     * @param length the length of this substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     * @throws IllegalArgumentException        if
     *                                         {@code length < 0 || length > length() - offset}
     */    
    public BitString set(int offset, int length) {
        checkThisOffset(offset);
        checkThisLength(offset, length);
        iSet(offset, length);
        return this;
    }
    
    /**
     * Sets all of the bits in a Field of this {@code BitString} to {@code ONE}.
     * 
     * @param field a Field of this {@code BitString}
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code field.offset() > 0 && field.offset() >= length()}
     * @throws IllegalArgumentException        if
     *                                         {@code field.length() > length() - field.offset()}
     */
    public BitString set(Field field) {
        return set(field.offset(), field.length(this));
    }
    
    /**
     * Sets all of the bits in this {@code BitString} to the specified bit.
     * 
     * @param bit the bit all the bits in this {@code BitString} are set to
     * @return this {@code BitString}
     */
    public BitString set(boolean bit) {
        return bit ? set() : clear();
    }
    
    /**
     * Sets all of the bits in a substring of this {@code BitString} to the
     * specified Bit.
     * 
     * This substring starts at offset 'offset' of this {@code BitString} and
     * extends to the end of this {@code BitString}.
     * 
     * @param bit    the Bit all the bits in this substring are set to
     * @param offset the start of this substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code field.offset() > 0 && field.offset() >= length()}
     */
    public BitString set(boolean bit, int offset) {
        return bit ? set(offset) : clear(offset);
    }
    
    /**
     * Sets all of the bits in a substring of this {@code BitString} to the
     * specified bit.
     * 
     * The substring starts at offset 'offset' of this {@code BitString} and has a
     * length of 'length'.
     * 
     * @param bit    the bit all the bits in this substring are set to
     * @param offset the start of this substring
     * @param length the length of this substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code field.offset() > 0 && field.offset() >= length()}
     * @throws IllegalArgumentException        if
     *                                         {@code field.length() > length() - field.offset()}
     */
    public BitString set(boolean bit, int offset, int length) {
        return bit ? set(offset, length) : clear(offset, length);
    }
    
    /**
     * Sets all of the bits in a Field of this {@code BitString} to the specified
     * bit.
     * 
     * @param bit   the bit all the bits in the Field are set to
     * @param field a Field of this {@code BitString}
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code field.offset() > 0 && field.offset() >= length()}
     * @throws IllegalArgumentException        if
     *                                         {@code field.length() > length() - field.offset()}
     */
    public BitString set(boolean bit, Field field) {
        return set(bit, field.offset(), field.length(this));
    }
    
    /**
     * Sets the bit at the specified offset to {@code ONE}.
     * 
     * @param bitOffset the offset of the bit to set
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code bitOffset < 0 || bitOffset > 0 && bitOffset >= length()}
     */
    public BitString setBit(int bitOffset) {
        checkThisOffset(bitOffset);
        final int bitIndex = bitIndex(bitOffset);
        final int wordIndex = wordIndex(bitIndex);
        setWord(wordIndex, getWord(wordIndex) | (BIT_MASK >>> wordBitIndex(bitIndex)));
        return this;
    }
    
    public BitString setBit(int bitOffset, int offset, int length) {
        checkThisOffset(offset);
        checkThisLength(offset, length);
        checkRelativeOffset(bitOffset, length);
        return setBit(offset + bitOffset);
    }
    
    public BitString setBit(int bitOffset, Field field) {
        return setBit(bitOffset, field.offset(), field.length(this));
    }
    
    /**
     * Sets the bit at the specified offset to the specified bit.
     * 
     * @param bit       the bit at the specified offset is set to this bit
     * @param bitOffset the offset of the bit to set
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code bitOffset < 0 || bitOffset > 0 && bitOffset >= length()}
     */
    public BitString setBit(boolean bit, int bitOffset) {
        return bit ? setBit(bitOffset) : clearBit(bitOffset);
    }
    
    public BitString setBit(boolean bit, int bitOffset, int offset, int length) {
        return bit ? setBit(bitOffset, offset, length) : clearBit(bitOffset, offset, length);
    }
    
    public BitString setBit(boolean bit, int bitOffset, Field field) {
        return bit ? setBit(bitOffset, field) : clearBit(bitOffset, field);
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
     * Performs a logical <b>AND</b> of a substring of this {@code BitString} with a
     * substring of the specified bit string (arg).
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * has a length of 'thisLength'.
     * 
     * The argument substring starts at offset 'argOffset' of the specified bit
     * string and has a length of 'argLength'.
     * 
     * The length of the operation is equal to the smaller of the length of this
     * substring or the length of the argument substring.
     * <p>
     * The bits this substring are modified according to the following
     * logic table. For each {@code ONE} bit in the argument substring, the
     * corresponding bit in this substring is left unchanged, otherwise,
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
    public BitString and(int thisOffset, int thisLength, BitString arg, int argOffset, int argLength) {
        checkThisOffset(thisOffset);
        checkThisLength(thisOffset, thisLength);
        arg.checkArgOffset(argOffset);
        arg.checkArgLength(argOffset, argLength);
        iAnd(thisOffset, Math.min(thisLength, argLength), arg, argOffset);
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
        return and(thisField.offset(), thisField.length(this), arg, argField.offset(), argField.length(arg));
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
     * Clears all of the bits in a substring of this {@code BitString} whose corresponding bit is
     * set in a substring of the specified bit string (arg).
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
     * The bits in this substring are modified according to the following
     * logic table. For each {@code ONE} bit in the argument substring, the
     * corresponding bit in this substring is set to {@code ZERO}, otherwise,
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
    public BitString andNot(int thisOffset, int thisLength, BitString arg, int argOffset, int argLength) {
        checkThisOffset(thisOffset);
        checkThisLength(thisOffset, thisLength);
        arg.checkArgOffset(argOffset);
        arg.checkArgLength(argOffset, argLength);
        iAndNot(thisOffset, Math.min(thisLength, argLength), arg, argOffset);
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
        return andNot(thisField.offset(), thisField.length(this), arg, argField.offset(), argField.length(arg));
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
     * Performs a logical <b>NAND</b> of a substring of this {@code BitString} with a
     * substring of the specified bit string (arg).
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
     * The bits in this substring are modified according to the following
     * logic table. For each {@code ONE} bit in the argument substring, the
     * corresponding bit in this substring is flipped, otherwise,
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
    public BitString nand(int thisOffset, int thisLength, BitString arg, int argOffset, int argLength) {
        checkThisOffset(thisOffset);
        checkThisLength(thisOffset, thisLength);
        arg.checkArgOffset(argOffset);
        arg.checkArgLength(argOffset, argLength);
        iNand(thisOffset, Math.min(thisLength, argLength), arg, argOffset);
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
        return nand(thisField.offset(), thisField.length(this), arg, argField.offset(), argField.length(arg));
    }
    
    /**
     * Sets all of the bits in this {@code BitString} whose corresponding bit is
     * set in the specified bit string, otherwise, the bits are flipped.
     * 
     * The length of the operation is equal to the smaller of the length of this
     * {@code BitString} or the length of the specified bit string.
     * <p>
     * The bits in this {@code BitString} are modified according to the following
     * logic table. For each {@code ONE} bit in the argument, the
     * corresponding bit in this {@code BitString} is set to {@code ONE}, otherwise,
     * the bit is flipped. This operation is the same as an <b>NAND</b> operation
     * where the argument bit value is flipped, or the compliment of the <b>ANDNOT<b> operation.
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
     * Sets all of the bits in a substring of this {@code BitString} whose corresponding bit is
     * set in a substring of the specified bit string, otherwise, the bits are flipped.
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
     * The bits in this substring are modified according to the following
     * logic table. For each {@code ONE} bit in the argument substring, the
     * corresponding bit in this substring is set to {@code ONE}, otherwise,
     * the bit is flipped. This operation is the same as an <b>NAND</b> operation
     * where the argument bit value is flipped, or the compliment of the <b>ANDNOT<b> operation.
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
    public BitString nandNot(int thisOffset, int thisLength, BitString arg, int argOffset, int argLength) {
        checkThisOffset(thisOffset);
        checkThisLength(thisOffset, thisLength);
        arg.checkArgOffset(argOffset);
        arg.checkArgLength(argOffset, argLength);
        iNandNot(thisOffset, Math.min(thisLength, argLength), arg, argOffset);
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
     * where the argument bit value is flipped, or the compliment of the <b>ANDNOT<b> operation.
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
        return nandNot(thisField.offset(), thisField.length(this), arg, argField.offset(), argField.length(arg));
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
     * Performs a logical <b>NOR</b> of a substring of this {@code BitString} with a
     * substring of the specified bit string (arg).
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
     * The bits in this substring are modified according to the following
     * logic table. For each {@code ONE} bit in the argument substring, the
     * corresponding bit in this substring is set to {@code ZERO}, otherwise,
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
    public BitString nor(int thisOffset, int thisLength, BitString arg, int argOffset, int argLength) {
        checkThisOffset(thisOffset);
        checkThisLength(thisOffset, thisLength);
        arg.checkArgOffset(argOffset);
        arg.checkArgLength(argOffset, argLength);
        iNor(thisOffset, Math.min(thisLength, argLength), arg, argOffset);
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
        return nor(thisField.offset(), thisField.length(this), arg, argField.offset(), argField.length(arg));
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
     * where the argument bit value is flipped, or the compliment of the <b>ORNOT<b> operation.
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
     * Flips all of the bits in a substring of this {@code BitString} whose corresponding bit is
     * {@code ONE} in a substring of the specified bit string, otherwise, the bits are set to {@code ZERO}.
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
     * The bits in this substring are modified according to the following
     * logic table. For each {@code ONE} bit in the argument substring, the
     * corresponding bit in this substring is flipped, otherwise,
     * the bit is set to {@code ZERO}. This operation is the same as a <b>NOR</b> operation
     * where the argument bit value is flipped, or the compliment of the <b>ORNOT<b> operation.
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
    public BitString norNot(int thisOffset, int thisLength, BitString arg, int argOffset, int argLength) {
        checkThisOffset(thisOffset);
        checkThisLength(thisOffset, thisLength);
        arg.checkArgOffset(argOffset);
        arg.checkArgLength(argOffset, argLength);
        iNorNot(thisOffset, Math.min(thisLength, argLength), arg, argOffset);
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
     * where the argument bit value is flipped, or the compliment of the <b>ORNOT<b> operation.
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
        return norNot(thisField.offset(), thisField.length(this), arg, argField.offset(), argField.length(arg));
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
     * Performs a logical <b>OR</b> of a substring of this {@code BitString} with a
     * substring of the specified bit string (arg).
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
     * The bits in this substring are modified according to the following
     * logic table. For each {@code ONE} bit in the argument substring, the
     * corresponding bit in this substring is set to {@code ONE}, otherwise,
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
    public BitString or(int thisOffset, int thisLength, BitString arg, int argOffset, int argLength) {
        checkThisOffset(thisOffset);
        checkThisLength(thisOffset, thisLength);
        arg.checkArgOffset(argOffset);
        arg.checkArgLength(argOffset, argLength);
        iOr(thisOffset, Math.min(thisLength, argLength), arg, argOffset);
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
        return or(thisField.offset(), thisField.length(this), arg, argField.offset(), argField.length(arg));
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
     * Sets all of the bits in a substring of this {@code BitString} whose corresponding bit is
     * {@code ZERO} in a substring of the specified bit string (arg).
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
     * The bits in this substring are modified according to the following
     * logic table. For each {@code ONE} bit in the argument substring, the
     * corresponding bit in this substring is left unchanged, otherwise,
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
    public BitString orNot(int thisOffset, int thisLength, BitString arg, int argOffset, int argLength) {
        checkThisOffset(thisOffset);
        checkThisLength(thisOffset, thisLength);
        arg.checkArgOffset(argOffset);
        arg.checkArgLength(argOffset, argLength);
        iOrNot(thisOffset, Math.min(thisLength, argLength), arg, argOffset);
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
        return orNot(thisField.offset(), thisField.length(this), arg, argField.offset(), argField.length(arg));
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
     * Performs a logical <b>XOR</b> of a substring of this {@code BitString} with a
     * substring of the specified bit string (arg).
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
     * The bits in this substring are modified according to the following
     * logic table. For each {@code ONE} bit in the argument substring, the
     * corresponding bit in this substring is flipped, otherwise,
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
    public BitString xor(int thisOffset, int thisLength, BitString arg, int argOffset, int argLength) {
        checkThisOffset(thisOffset);
        checkThisLength(thisOffset, thisLength);
        arg.checkArgOffset(argOffset);
        arg.checkArgLength(argOffset, argLength);
        iXor(thisOffset, Math.min(thisLength, argLength), arg, argOffset);
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
        return xor(thisField.offset(), thisField.length(this), arg, argField.offset(), argField.length(arg));
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
     * Performs a logical <b>XNOR</b> of a substring of this {@code BitString} with a
     * substring of the specified bit string (arg).
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
     * The bits in this substring are modified according to the following
     * logic table. For each {@code ONE} bit in the argument substring, the
     * corresponding bit in this substring is left unchanged, otherwise,
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
    public BitString xnor(int thisOffset, int thisLength, BitString arg, int argOffset, int argLength) {
        checkThisOffset(thisOffset);
        checkThisLength(thisOffset, thisLength);
        arg.checkArgOffset(argOffset);
        arg.checkArgLength(argOffset, argLength);
        iXnor(thisOffset, Math.min(thisLength, argLength), arg, argOffset);
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
        return xnor(thisField.offset(), thisField.length(this), arg, argField.offset(), argField.length(arg));
    }
    
    /**
     * Copy bits from the specified bit string (that) into this {@code BitString}.
     * 
     * The bits are copied from front to back. if the specified bit string is
     * shorter in length than this {@code BitString}, this {@code BitString} is
     * padded (on the right) with {@code ZEROS}. If the specified bit string is
     * longer in length than this {@code BitString}, the copy is truncated to fit
     * this {@code BitString}.
     * 
     * @param that the bit string to copy
     * @return this {@code BitString}
     */
    public BitString copyFrom(BitString that) {
        iCopyFromFrontOf(0, this.length(), that, 0, that.length());
        return this;
    }
    
    /**
     * Copy bits from a substring of the specified bit string (that) into a
     * substring of this {@code BitString}.
     * 
     * The bits are copied from front to back. if that substring is shorter in
     * length than this substring, this substring is padded (on the right) with
     * {@code ZEROS}. If that substring is longer in length than this substring, the
     * copy is truncated to fit this substring.
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * has a length of 'thisLength'.
     * 
     * That substring starts at offset 'thatOffset' of the specified bit string and
     * has a length of 'thatLength'.
     * 
     * @param thisOffset the start of this substring
     * @param thisLength the length of this substring
     * @param that       the bit string to copy
     * @param thatOffset the start of the that substring
     * @param thatLength the length of the that substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code thatOffset < 0 || thatOffset > 0 && thatOffset >= that.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisLength < 0 || thisLength > this.length() - thisOffset}
     *                                         or
     *                                         {@code thatLength < 0 || thatLength > that.length() - thatOffset}
     */
    public BitString copyFrom(int thisOffset, int thisLength, BitString that, int thatOffset, int thatLength) {
        checkThisOffset(thisOffset);
        checkThisLength(thisOffset, thisLength);
        that.checkArgOffset(thatOffset);
        that.checkArgLength(thatOffset, thatLength);
        iCopyFromFrontOf(thisOffset, thisLength, that, thatOffset, thatLength);
        return this;
    }
    
    /**
     * Copy bits from a Field of the specified bit string (that) into a Field of
     * this {@code BitString}.
     * 
     * The bits are copied from front to back. if that Field is shorter in length
     * than this Field, this Field is padded (on the right) with {@code ZEROS}.
     * If that Field is longer in length than this Field, the copy is truncated to
     * fit this Field.
     * 
     * @param thisField a Field of this {@code BitString}
     * @param that      the bit string to copy
     * @param thatField a Field of that bit string
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisField.offset() > 0 && thisField.offset() >= this.length()}
     *                                         or
     *                                         {@code thatField.offset() > 0 && thatField.offset() >= that.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisField.length() > this.length() - thisField.offset()}
     *                                         or
     *                                         {@code thatField.length() > that.length() - thatField.offset()}
     */
    public BitString copyFrom(Field thisField, BitString that, Field thatField) {
        return copyFrom(thisField.offset(), thisField.length(this), that, thatField.offset(), thatField.length(that));
    }
    
    /**
     * Copy bits from the back of the specified bit string (that) into this
     * {@code BitString}.
     * 
     * The bits are copied from back to front. if the specified
     * bit string is shorter in length than this {@code BitString}, this
     * {@code BitString} is padded (on the left) with {@code ZEROS}. If the
     * specified bit string is longer in length than this {@code BitString}, the
     * copy is truncated to fit this {@code BitString}.
     * 
     * @param that the bit string to copy
     * @return this {@code BitString}
     */
    public BitString copyFromBackOf(BitString that) {
        iCopyFromBackOf(0, this.length(), that, 0, that.length());
        return this;
    }
    
    /**
     * Copy bits from the back of a substring of the specified bit string (that)
     * into a substring of this {@code BitString}.
     * 
     * The bits are copied from back to front. if that substring is shorter in
     * length than this substring, this substring is padded (on the left) with
     * {@code ZEROS}. If that substring is longer in length than this substring, the
     * copy is truncated to fit this substring.
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * has a length of 'thisLength'.
     * 
     * That substring starts at offset 'thatOffset' of the specified bit string and
     * has a length of 'thatLength'.
     * 
     * @param thisOffset the start of this substring
     * @param thisLength the length of this substring
     * @param that       the bit string to copy
     * @param thatOffset the start of the that substring
     * @param thatLength the length of the that substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code thatOffset < 0 || thatOffset > 0 && thatOffset >= that.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisLength < 0 || thisLength > this.length() - thisOffset}
     *                                         or
     *                                         {@code thatLength < 0 || thatLength > that.length() - thatOffset}
     */
    public BitString copyFromBackOf(int thisOffset, int thisLength, BitString that, int thatOffset, int thatLength) {
        checkThisOffset(thisOffset);
        checkThisLength(thisOffset, thisLength);
        that.checkArgOffset(thatOffset);
        that.checkArgLength(thatOffset, thatLength);
        iCopyFromBackOf(thisOffset, thisLength, that, thatOffset, thatLength);
        return this;
    }
    
    /**
     * Copy bits from the back of a Field of the specified bit string (that) into a
     * Field of this {@code BitString}.
     * 
     * The bits are copied from back to front. if that Field is shorter in length
     * than this Field, this Field is padded (on the left) with {@code ZEROS}. If
     * that Field is longer in length than this Field, the copy is truncated to fit
     * this Field.
     * 
     * @param thisField a Field of this {@code BitString}
     * @param that      the bit string to copy
     * @param thatField a Field of that bit string
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisField.offset() > 0 && thisField.offset() >= this.length()}
     *                                         or
     *                                         {@code thatField.offset() > 0 && thatField.offset() >= that.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisField.length() > this.length() - thisField.offset()}
     *                                         or
     *                                         {@code thatField.length() > that.length() - thatField.offset()}
     */
    public BitString copyFromBackOf(Field thisField, BitString that, Field thatField) {
        return copyFromBackOf(thisField.offset(), thisField.length(this), that, thatField.offset(), thatField.length(that));
    }
    
    /**
     * Copy the complement of the bits from the specified bit string (that) into
     * this {@code BitString}.
     * 
     * The bits are copied from front to back. if the specified bit string is
     * shorter in length than this {@code BitString}, this {@code BitString} is
     * padded (on the right) with {@code ONES}. If the specified bit string is
     * longer in length than this {@code BitString}, the copy is truncated to fit
     * this {@code BitString}.
     * 
     * @param that the bit string to copy
     * @return this {@code BitString}
     */
    public BitString copyNotFrom(BitString that) {
        iCopyNotFromFrontOf(0, this.length(), that, 0, that.length());
        return this;
    }
    
    /**
     * Copy the complement of the bits from a substring of the specified bit string
     * (that) into a substring of this {@code BitString}.
     * 
     * The bits are copied from front to back. if that substring is shorter in
     * length than this substring, this substring is padded (on the right) with
     * {@code ONES}. If that substring is longer in length than this substring, the
     * copy is truncated to fit this substring.
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * has a length of 'thisLength'.
     * 
     * That substring starts at offset 'thatOffset' of the specified bit string and
     * has a length of 'thatLength'.
     * 
     * @param thisOffset the start of this substring
     * @param thisLength the length of this substring
     * @param that       the bit string to copy
     * @param thatOffset the start of the that substring
     * @param thatLength the length of the that substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code thatOffset < 0 || thatOffset > 0 && thatOffset >= that.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisLength < 0 || thisLength > this.length() - thisOffset}
     *                                         or
     *                                         {@code thatLength < 0 || thatLength > that.length() - thatOffset}
     */
    public BitString copyNotFrom(int thisOffset, int thisLength, BitString that, int thatOffset, int thatLength) {
        checkThisOffset(thisOffset);
        checkThisLength(thisOffset, thisLength);
        that.checkArgOffset(thatOffset);
        that.checkArgLength(thatOffset, thatLength);
        iCopyNotFromFrontOf(thisOffset, thisLength, that, thatOffset, thatLength);
        return this;
    }
    
    /**
     * Copy the complement of the bits from a Field of the specified bit string
     * (that) into a Field of this {@code BitString}.
     * 
     * The bits are copied from front to back. if that Field is shorter in length
     * than this Field, this Field is padded (on the right) with {@code ONES}. If
     * that Field is longer in length than this Field, the copy is truncated to fit
     * this Field.
     * 
     * @param thisField a Field of this {@code BitString}
     * @param that      the bit string to copy
     * @param thatField a Field of that bit string
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisField.offset() > 0 && thisField.offset() >= this.length()}
     *                                         or
     *                                         {@code thatField.offset() > 0 && thatField.offset() >= that.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisField.length() > this.length() - thisField.offset()}
     *                                         or
     *                                         {@code thatField.length() > that.length() - thatField.offset()}
     */
    public BitString copyNotFrom(Field thisField, BitString that, Field thatField) {
        return copyNotFrom(thisField.offset(), thisField.length(this), that, thatField.offset(), thatField.length(that));
    }
    
    /**
     * Copy the complement of the bits from the back of the specified bit string
     * (that) into this {@code BitString}.
     * 
     * The bits are copied from back to front. if the specified bit string is
     * shorter in length than this {@code BitString}, this {@code BitString} is
     * padded (on the left) with {@code ONES}. If the specified bit string is longer
     * in length than this {@code BitString}, the copy is truncated to fit this
     * {@code BitString}.
     * 
     * @param that the bit string to copy
     * @return this {@code BitString}
     */
    public BitString copyNotFromBackOf(BitString that) {
        iCopyNotFromBackOf(0, this.length(), that, 0, that.length());
        return this;
    }
    
    /**
     * Copy the complement of the bits from the back of a substring of the specified
     * bit string (that) into a substring of this {@code BitString}.
     * 
     * The bits are copied from back to front. if that substring is shorter in
     * length than this substring, this substring is padded (on the left) with
     * {@code ONES}. If that substring is longer in length than this substring, the
     * copy is truncated to fit this substring.
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * has a length of 'thisLength'.
     * 
     * That substring starts at offset 'thatOffset' of the specified bit string and
     * has a length of 'thatLength'.
     * 
     * @param thisOffset the start of this substring
     * @param thisLength the length of this substring
     * @param that       the bit string to copy
     * @param thatOffset the start of the that substring
     * @param thatLength the length of the that substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code thatOffset < 0 || thatOffset > 0 && thatOffset >= that.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisLength < 0 || thisLength > this.length() - thisOffset}
     *                                         or
     *                                         {@code thatLength < 0 || thatLength > that.length() - thatOffset}
     */
    public BitString copyNotFromBackOf(int thisOffset, int thisLength, BitString that, int thatOffset, int thatLength) {
        checkThisOffset(thisOffset);
        checkThisLength(thisOffset, thisLength);
        that.checkArgOffset(thatOffset);
        that.checkArgLength(thatOffset, thatLength);
        iCopyNotFromBackOf(thisOffset, thisLength, that, thatOffset, thatLength);
        return this;
    }
    
    /**
     * Copy the complement of the bits from the back of a Field of the specified bit
     * string (that) into a Field of this {@code BitString}.
     * 
     * The bits are copied from back to front. if that Field is shorter in length
     * than this Field, this Field is padded (on the left) with {@code ONES}. If
     * that Field is longer in length than this Field, the copy is truncated to fit
     * this Field.
     * 
     * @param thisField a Field of this {@code BitString}
     * @param that      the bit string to copy
     * @param thatField a Field of that bit string
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisField.offset() > 0 && thisField.offset() >= this.length()}
     *                                         or
     *                                         {@code thatField.offset() > 0 && thatField.offset() >= that.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisField.length() > this.length() - thisField.offset()}
     *                                         or
     *                                         {@code thatField.length() > that.length() - thatField.offset()}
     */
    public BitString copyNotFromBackOf(Field thisField, BitString that, Field thatField) {
        return copyNotFromBackOf(thisField.offset(), thisField.length(this), that, thatField.offset(), thatField.length(that));
    }

    /**
     * Returns {@code true} if this {@code BitString} and the specified object (obj)
     * are equal. This {@code BitString} and the object are equal if and only if the
     * object is a {@code BitString} that has the the same length as this
     * {@code BitString} and has the same set of bits set to {@code ONE} as this
     * {@code BitString}. That is, for every nonnegative {@code int} index {@code k}
     * less than the length of the comparison,
     * 
     * <pre>
     * this.getBit(k) == ((BitString) obj).getBit(k)
     * </pre>
     * 
     * must be true.
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
     * Returns the number of leading {@code ONES} of this {@code BitString}.
     * 
     * @return the number of leading {@code ONES} of this {@code BitString}
     */
    public int numberOfLeadingOnes() {
        return iNumberOfLeadingOnes(0, length());
    }
    
    /**
     * Returns the number of leading {@code ONES} of the specified substring of this
     * {@code BitString}.
     * 
     * The specified substring starts at offset 'offset' of this {@code BitString}
     * and has a length of 'length'.
     * 
     * @param offset the start of the substring
     * @param length the length of the substring
     * @return the number of leading {@code ONES} of the substring
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     * @throws IllegalArgumentException        if
     *                                         {@code length < 0 || length > length() - offset}
     */
    public int numberOfLeadingOnes(int offset, int length) {
        checkThisOffset(offset);
        checkThisLength(offset, length);
        return iNumberOfLeadingOnes(offset, length);
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
        return numberOfLeadingOnes(field.offset(), field.length(this));
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
     * Returns the number of leading {@code ZEROS} of the specified substring of
     * this {@code BitString}.
     * 
     * The specified substring starts at offset 'offset' of this {@code BitString}
     * and has a length of 'length'.
     * 
     * @param offset the start of the substring
     * @param length the length of the substring
     * @return the number of leading {@code ZEROS} of the substring
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     * @throws IllegalArgumentException        if
     *                                         {@code length < 0 || length > length() - offset}
     */
    public int numberOfLeadingZeros(int offset, int length) {
        checkThisOffset(offset);
        checkThisLength(offset, length);
        return iNumberOfLeadingZeros(offset, length);
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
        return numberOfLeadingZeros(field.offset(), field.length(this));
    }
    
    /**
     * Returns the number of {@code ONES} in this {@code BitString}.
     *
     * @return the number of {@code ONES} in this {@code BitString}
     */
    public int numberOfOnes() {
        return numberOfOnes(0, length());
    }
    
    /**
     * Returns the number of {@code ONES} in the specified substring of this
     * {@code BitString}.
     * 
     * The specified substring starts at offset 'offset' of this {@code BitString}
     * and has a length of 'length'.
     * 
     * @param offset the start of the substring
     * @param length the length of the substring
     * @return the number of {@code ONES} in the specified substring
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     * @throws IllegalArgumentException        if
     *                                         {@code length < 0 || length > length() - offset}
     */
    public int numberOfOnes(int offset, int length) {
        checkThisOffset(offset);
        checkThisLength(offset, length);
        int sum = 0;
        final int[] iterator = getIterator(offset, length);
        while (hasNextIteratorWord(iterator)) {
            sum += Long.bitCount(getNextIteratorFullWord(iterator));
        }
        return sum;
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
        return numberOfOnes(field.offset(), field.length(this));
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
     * Returns the number of trailing {@code ONES} of the specified substring of
     * this {@code BitString}.
     * 
     * The specified substring starts at offset 'offset' of this {@code BitString}
     * and has a length of 'length'.
     * 
     * @param offset the start of the substring
     * @param length the length of the substring
     * @return the number of trailing {@code ONES} of the substring
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     * @throws IllegalArgumentException        if
     *                                         {@code length < 0 || length > length() - offset}
     */
    public int numberOfTrailingOnes(int offset, int length) {
        checkThisOffset(offset);
        checkThisLength(offset, length);
        return iNumberOfTrailingOnes(offset, length);
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
        return numberOfTrailingOnes(field.offset(), field.length(this));
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
     * Returns the number of trailing {@code ZEROS} of the specified substring of
     * this {@code BitString}.
     * 
     * The specified substring starts at offset 'offset' of this {@code BitString}
     * and has a length of 'length'.
     * 
     * @param offset the start of the substring
     * @param length the length of the substring
     * @return the number of trailing {@code ZEROS} of the substring
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     * @throws IllegalArgumentException        if
     *                                         {@code length < 0 || length > length() - offset}
     */
    public int numberOfTrailingZeros(int offset, int length) {
        checkThisOffset(offset);
        checkThisLength(offset, length);
        return iNumberOfTrailingZeros(offset, length);
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
        return numberOfTrailingZeros(field.offset(), field.length(this));
    }
    
    /**
     * Returns the number of {@code ZEROS} in this {@code BitString}.
     *
     * @return the number of {@code ZEROS} in this {@code BitString}
     */
    public int numberOfZeros() {
        return numberOfZeros(0, length());
    }
    
    /**
     * Returns the number of {@code ZEROS} in the specified substring of this
     * {@code BitString}.
     * 
     * The specified substring starts at offset 'offset' of this {@code BitString}
     * and has a length of 'length'.
     * 
     * @param offset the start of the substring
     * @param length the length of the substring
     * @return the number of {@code ZEROS} in the specified substring
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     * @throws IllegalArgumentException        if
     *                                         {@code length < 0 || length > length() - offset}
     */
    public int numberOfZeros(int offset, int length) {
        checkThisOffset(offset);
        checkThisLength(offset, length);
        int sum = 0;
        final int[] iterator = getIterator(offset, length);
        while (hasNextIteratorWord(iterator)) {
            sum += Long.SIZE - Long.bitCount(getNextIteratorFullWord(iterator));
        }
        return sum;
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
        return numberOfZeros(field.offset(), field.length(this));
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
     * within the specified substring of this {@code BitString}. If no such bit
     * exists, -1 is returned. The returned offset is relative to the start of the
     * substring.
     * 
     * The specified substring starts at offset 'offset' of this {@code BitString}
     * and has a length of 'length'.
     * 
     * @param offset the start of the substring
     * @param length the length of the substring
     * @return the offset of the first {@code ONE} bit or -1 if no bits are set to
     *         {@code ONE}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     * @throws IllegalArgumentException        if
     *                                         {@code length < 0 || length > length() - offset}
     */
    public int offsetOfFirstOne(int offset, int length) {
        checkThisOffset(offset);
        checkThisLength(offset, length);
        return iOffsetOfFirstOne(offset, length);
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
        return offsetOfFirstOne(field.offset(), field.length(this));
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
      * within the specified substring of this {@code BitString}. If no such bit
      * exists, -1 is returned. The returned offset is relative to the start of the
      * substring.
      * 
      * The specified substring starts at offset 'offset' of this {@code BitString}
      * and has a length of 'length'.
      * 
      * @param offset the start of the substring
      * @param length the length of the substring
      * @return the offset of the first {@code ZERO} bit or -1 if no bits are set to
      *         {@code ZERO}
      * @throws StringIndexOutOfBoundsException if
      *                                         {@code offset < 0 || offset > 0 && offset >= length()}
      * @throws IllegalArgumentException        if
      *                                         {@code length < 0 || length > length() - offset}
      */
    public int offsetOfFirstZero(int offset, int length) {
        checkThisOffset(offset);
        checkThisLength(offset, length);
        return iOffsetOfFirstZero(offset, length);
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
        return offsetOfFirstZero(field.offset(), field.length(this));
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
     * within the specified substring of this {@code BitString}. If no such bit
     * exists, -1 is returned. The returned offset is relative to the start of the
     * substring.
     * 
     * The specified substring starts at offset 'offset' of this {@code BitString}
     * and has a length of 'length'.
     * 
     * @param offset the start of the substring
     * @param length the length of the substring
     * @return the offset of the last {@code ONE} bit or -1 if no bits are set to
     *         {@code ONE}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     * @throws IllegalArgumentException        if
     *                                         {@code length < 0 || length > length() - offset}
     */
    public int offsetOfLastOne(int offset, int length) {
        checkThisOffset(offset);
        checkThisLength(offset, length);
        return iOffsetOfLastOne(offset, length);
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
        return offsetOfLastOne(field.offset(), field.length(this));
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
     * within the specified substring of this {@code BitString}. If no such bit
     * exists, -1 is returned. The returned offset is relative to the start of the
     * substring.
     * 
     * The specified substring starts at offset 'offset' of this {@code BitString}
     * and has a length of 'length'.
     * 
     * @param offset the start of the substring
     * @param length the length of the substring
     * @return the offset of the last {@code ZERO} bit or -1 if no bits are set to
     *         {@code ZERO}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     * @throws IllegalArgumentException        if
     *                                         {@code length < 0 || length > length() - offset}
     */
    public int offsetOfLastZero(int offset, int length) {
        checkThisOffset(offset);
        checkThisLength(offset, length);
        return iOffsetOfLastZero(offset, length);
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
        return offsetOfLastZero(field.offset(), field.length(this));
    }
    
    /**
     * Returns the offset of the next bit that is set to {@code ONE} that occurs on
     * or after the specified start offset within this {@code BitString}. If no such
     * bit exists, or {@code startOffset == length()}, -1 is returned. The start
     * offset and the returned offset are relative to the start of this
     * {@code BitString}.
     * 
     * @param startOffset the offset to start checking from (inclusive)
     * @return the offset of the next {@code ONE} bit or -1 if no bits are set to
     *         {@code ONE}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code startOffset < 0 || startOffset > length()}
     */
    public int offsetOfNextOne(int startOffset) {
        if (startOffset == length()) return -1;
        checkRelativeOffset(startOffset, length());
        final int bitOffset = iOffsetOfFirstOne(startOffset, length() - startOffset);
        return (bitOffset == -1) ? -1 : startOffset + bitOffset;
    }
    
    /**
     * Returns the offset of the next bit that is set to {@code ONE} that occurs on
     * or after the specified start offset within the specified substring of this
     * {@code BitString}. If no such bit exists, or
     * {@code startOffset == length() - offset}, -1 is returned. The start offset
     * and the returned offset are relative to the start of the substring.
     * 
     * The specified substring starts at offset 'offset' of this {@code BitString}
     * and extends to the end of this {@code BitString}.
     * 
     * @param startOffset the offset to start checking from (inclusive)
     * @param offset      the start of the substring
     * @return the offset of the next {@code ONE} bit or -1 if no bits are set to
     *         {@code ONE}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     *                                         or
     *                                         {@code startOffset < 0 || startOffset > length() - offset}
     */
    public int offsetOfNextOne(int startOffset, int offset) {
        checkThisOffset(offset);
        final int length = length() - offset;
        if (startOffset == length) return -1;
        checkRelativeOffset(startOffset, length);
        final int bitOffset = iOffsetOfFirstOne(offset + startOffset, length - startOffset);
        return (bitOffset == -1) ? -1 : startOffset + bitOffset;
    }
    
    /**
     * Returns the offset of the next bit that is set to {@code ONE} that occurs on
     * or after the specified start offset within the specified substring of this
     * {@code BitString}. If no such bit exists, or {@code startOffset == length},
     * -1 is returned. The start offset and the returned offset are relative to the
     * start of the substring.
     * 
     * The specified substring starts at offset 'offset' of this {@code BitString}
     * and has a length of 'length'.
     * 
     * @param startOffset the offset to start checking from (inclusive)
     * @param offset      the start of the substring
     * @param length      the length of the substring
     * @return the offset of the next {@code ONE} bit or -1 if no bits are set to
     *         {@code ONE}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     *                                         or
     *                                         {@code startOffset < 0 || startOffset > length}
     * @throws IllegalArgumentException        if
     *                                         {@code length < 0 || length > length() - offset}
     */
    public int offsetOfNextOne(int startOffset, int offset, int length) {
        checkThisOffset(offset);
        checkThisLength(offset, length);
        if (startOffset == length) return -1;
        checkRelativeOffset(startOffset, length);
        final int bitOffset = iOffsetOfFirstOne(offset + startOffset, length - startOffset);
        return (bitOffset == -1) ? -1 : startOffset + bitOffset;
    }
    
    /**
     * Returns the offset of the next bit that is set to {@code ONE} that occurs on
     * or after the specified start offset within the specified Field of this
     * {@code BitString}. If no such bit exists, or
     * {@code startOffset == field.length()}, -1 is returned. The start offset and
     * the returned offset are relative to the start of the field.
     * 
     * @param startOffset the offset to start checking from (inclusive)
     * @param field       a Field of this {@code BitString}
     * @return the offset of the next {@code ONE} bit or -1 if no bits are set to
     *         {@code ONE}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code field.offset() > 0 && field.offset() >= length()}
     *                                         or
     *                                         {@code startOffset < 0 || startOffset > field.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code field.length() > length() - field.offset()}
     */
    public int offsetOfNextOne(int startOffset, Field field) {
        checkThisOffset(field.offset());
        checkThisLength(field.offset(), field.length(this));
        if (startOffset == field.length(this)) return -1;
        checkRelativeOffset(startOffset, field.length(this));
        final int bitOffset = iOffsetOfFirstOne(field.offset() + startOffset, field.length(this) - startOffset);
        return (bitOffset == -1) ? -1 : startOffset + bitOffset;
    }
    
    /**
     * Returns the offset of the next bit that is set to {@code ZERO} that occurs on
     * or after the specified start offset within this {@code BitString}. If no such
     * bit exists, or {@code startOffset == length()}, -1 is returned. The start
     * offset and the returned offset are relative to the start of this
     * {@code BitString}.
     * 
     * @param startOffset the offset to start checking from (inclusive)
     * @return the offset of the next {@code ZERO} bit or -1 if no bits are set to
     *         {@code ZERO}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code startOffset < 0 || startOffset > length()}
     */
    public int offsetOfNextZero(int startOffset) {
        if (startOffset == length()) return -1;
        checkRelativeOffset(startOffset, length());
        final int bitOffset = iOffsetOfFirstZero(startOffset, length() - startOffset);
        return (bitOffset == -1) ? -1 : startOffset + bitOffset;
    }
    
    /**
     * Returns the offset of the next bit that is set to {@code ZERO} that occurs on
     * or after the specified start offset within the specified substring of this
     * {@code BitString}. If no such bit exists, or
     * {@code startOffset == length() - offset}, -1 is returned. The start offset
     * and the returned offset are relative to the start of the substring.
     * 
     * The specified substring starts at offset 'offset' of this {@code BitString}
     * and extends to the end of this {@code BitString}.
     * 
     * @param startOffset the offset to start checking from (inclusive)
     * @param offset      the start of the substring
     * @return the offset of the next {@code ZERO} bit or -1 if no bits are set to
     *         {@code ZERO}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     *                                         or
     *                                         {@code startOffset < 0 || startOffset > length() - offset}
     */
    public int offsetOfNextZero(int startOffset, int offset) {
        checkThisOffset(offset);
        final int length = length() - offset;
        if (startOffset == length) return -1;
        checkRelativeOffset(startOffset, length);
        final int bitOffset = iOffsetOfFirstZero(offset + startOffset, length - startOffset);
        return (bitOffset == -1) ? -1 : startOffset + bitOffset;
    }
    
    /**
     * Returns the offset of the next bit that is set to {@code ZERO} that occurs on
     * or after the specified start offset within the specified substring of this
     * {@code BitString}. If no such bit exists, or {@code startOffset == length},
     * -1 is returned. The start offset and the returned offset are relative to the
     * start of the substring.
     * 
     * The specified substring starts at offset 'offset' of this {@code BitString}
     * and has a length of 'length'.
     * 
     * @param startOffset the offset to start checking from (inclusive)
     * @param offset      the start of the substring
     * @param length      the length of the substring
     * @return the offset of the next {@code ZERO} bit or -1 if no bits are set to
     *         {@code ZERO}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     *                                         or
     *                                         {@code startOffset < 0 || startOffset > length}
     * @throws IllegalArgumentException        if
     *                                         {@code length < 0 || length > length() - offset}
     */
    public int offsetOfNextZero(int startOffset, int offset, int length) {
        checkThisOffset(offset);
        checkThisLength(offset, length);
        if (startOffset == length) return -1;
        checkRelativeOffset(startOffset, length);
        final int bitOffset = iOffsetOfFirstZero(offset + startOffset, length - startOffset);
        return (bitOffset == -1) ? -1 : startOffset + bitOffset;
    }
    
    /**
     * Returns the offset of the next bit that is set to {@code ZERO} that occurs on
     * or after the specified start offset within the specified Field of this
     * {@code BitString}. If no such bit exists, or
     * {@code startOffset == field.length()}, -1 is returned. The start offset and
     * the returned offset are relative to the start of the field.
     * 
     * @param startOffset the offset to start checking from (inclusive)
     * @param field       a Field of this {@code BitString}
     * @return the offset of the next {@code ZERO} bit or -1 if no bits are set to
     *         {@code ZERO}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code field.offset() > 0 && field.offset() >= length()}
     *                                         or
     *                                         {@code startOffset < 0 || startOffset > field.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code field.length() > length() - field.offset()}
     */
    public int offsetOfNextZero(int startOffset, Field field) {
        checkThisOffset(field.offset());
        checkThisLength(field.offset(), field.length(this));
        if (startOffset == field.length(this)) return -1;
        checkRelativeOffset(startOffset, field.length(this));
        final int bitOffset = iOffsetOfFirstZero(field.offset() + startOffset, field.length(this) - startOffset);
        return (bitOffset == -1) ? -1 : startOffset + bitOffset;
    }
    
    /**
     * Returns the offset of the previous bit that is set to {@code ONE} that occurs
     * on or before the specified start offset within this {@code BitString}. If no
     * such bit exists, or -1 if given as the start offset, -1 is returned. The
     * start offset and the returned offset are relative to the start of this
     * {@code BitString}.
     * 
     * @param startOffset the offset to start checking from (inclusive)
     * @return the offset of the previous {@code ONE} bit or -1 if no bits are set
     *         to {@code ONE}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code startOffset < -1 || startOffset > 0 && offset >= length()}
     */
    public int offsetOfPreviousOne(int startOffset) {
        if (startOffset == -1) return -1;
        checkRelativeOffset(startOffset, length());
        return iOffsetOfLastOne(0, startOffset+1);
    }
    
    /**
     * Returns the offset of the previous bit that is set to {@code ONE} that occurs
     * on or before the specified start offset within the specified substring of
     * this {@code BitString}. If no such bit exists, or -1 if given as the start
     * offset, -1 is returned. The start offset and the returned offset are relative
     * to the start of the substring.
     * 
     * The specified substring starts at offset 'offset' of this {@code BitString}
     * and extends to the end of this {@code BitString}.
     * 
     * @param startOffset the offset to start checking from (inclusive)
     * @param offset      the start of the substring
     * @return the offset of the previous {@code ONE} bit or -1 if no bits are set
     *         to {@code ONE}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     *                                         or
     *                                         {@code startOffset < -1 || startOffset > 0 && startOffset >= length() - offset}
     */
    public int offsetOfPreviousOne(int startOffset, int offset) {
        checkThisOffset(offset);
        if (startOffset == -1) return -1;
        checkRelativeOffset(startOffset, length() - offset);
        return iOffsetOfLastOne(offset, startOffset+1);
    }
    
    /**
     * Returns the offset of the previous bit that is set to {@code ONE} that occurs
     * on or before the specified start offset within the specified substring of
     * this {@code BitString}. If no such bit exists, or -1 if given as the start
     * offset, -1 is returned. The start offset and the returned offset are relative
     * to the start of the substring.
     * 
     * The specified substring starts at offset 'offset' of this {@code BitString}
     * and has a length of 'length'.
     * 
     * @param startOffset the offset to start checking from (inclusive)
     * @param offset      the start of the substring
     * @param length      the length of the substring
     * @return the offset of the previous {@code ONE} bit or -1 if no bits are set
     *         to {@code ONE}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     *                                         or
     *                                         {@code startOffset < -1 || startOffset > 0 && startOffset >= length}
     * @throws IllegalArgumentException        if
     *                                         {@code length < 0 || length > length() - offset}
     */
    public int offsetOfPreviousOne(int startOffset, int offset, int length) {
        checkThisOffset(offset);
        checkThisLength(offset, length);
        if (startOffset == -1) return -1;
        checkRelativeOffset(startOffset, length);
        return iOffsetOfLastOne(offset, startOffset+1);
    }
    
    /**
     * Returns the offset of the previous bit that is set to {@code ONE} that occurs
     * on or before the specified start offset within the specified Field of this
     * {@code BitString}. If no such bit exists, or -1 if given as the start offset,
     * -1 is returned. The start offset and the returned offset are relative to the
     * start of the field.
     * 
     * @param startOffset the offset to start checking from (inclusive)
     * @param field       a Field of this {@code BitString}
     * @return the offset of the previous {@code ONE} bit or -1 if no bits are set
     *         to {@code ONE}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code field.offset() > 0 && field.offset() >= length()}
     *                                         or
     *                                         {@code startOffset < -1 || startOffset > 0 && startOffset >= field.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code field.length() > length() - field.offset()}
     */
    public int offsetOfPreviousOne(int startOffset, Field field) {
        checkThisOffset(field.offset());
        checkThisLength(field.offset(), field.length(this));
        if (startOffset == -1) return -1;
        checkRelativeOffset(startOffset, field.length(this));
        return iOffsetOfLastOne(field.offset(), startOffset+1);
    }
    
    /**
     * Returns the offset of the previous bit that is set to {@code ZERO} that occurs
     * on or before the specified start offset within this {@code BitString}. If no
     * such bit exists, or -1 if given as the start offset, -1 is returned. The
     * start offset and the returned offset are relative to the start of this
     * {@code BitString}.
     * 
     * @param startOffset the offset to start checking from (inclusive)
     * @return the offset of the previous {@code ZERO} bit or -1 if no bits are set
     *         to {@code ZERO}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code startOffset < -1 || startOffset > 0 && offset >= length()}
     */
    public int offsetOfPreviousZero(int startOffset) {
        if (startOffset == -1) return -1;
        checkRelativeOffset(startOffset, length());
        return iOffsetOfLastZero(0, startOffset+1);
    }
    
    /**
     * Returns the offset of the previous bit that is set to {@code ZERO} that occurs
     * on or before the specified start offset within the specified substring of
     * this {@code BitString}. If no such bit exists, or -1 if given as the start
     * offset, -1 is returned. The start offset and the returned offset are relative
     * to the start of the substring.
     * 
     * The specified substring starts at offset 'offset' of this {@code BitString}
     * and extends to the end of this {@code BitString}.
     * 
     * @param startOffset the offset to start checking from (inclusive)
     * @param offset      the start of the substring
     * @return the offset of the previous {@code ZERO} bit or -1 if no bits are set
     *         to {@code ZERO}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     *                                         or
     *                                         {@code startOffset < -1 || startOffset > 0 && startOffset >= length() - offset}
     */
    public int offsetOfPreviousZero(int startOffset, int offset) {
        checkThisOffset(offset);
        if (startOffset == -1) return -1;
        checkRelativeOffset(startOffset, length() - offset);
        return iOffsetOfLastZero(offset, startOffset+1);
    }
    
    /**
     * Returns the offset of the previous bit that is set to {@code ZERO} that occurs
     * on or before the specified start offset within the specified substring of
     * this {@code BitString}. If no such bit exists, or -1 if given as the start
     * offset, -1 is returned. The start offset and the returned offset are relative
     * to the start of the substring.
     * 
     * The specified substring starts at offset 'offset' of this {@code BitString}
     * and has a length of 'length'.
     * 
     * @param startOffset the offset to start checking from (inclusive)
     * @param offset      the start of the substring
     * @param length      the length of the substring
     * @return the offset of the previous {@code ZERO} bit or -1 if no bits are set
     *         to {@code ZERO}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     *                                         or
     *                                         {@code startOffset < -1 || startOffset > 0 && startOffset >= length}
     * @throws IllegalArgumentException        if
     *                                         {@code length < 0 || length > length() - offset}
     */
    public int offsetOfPreviousZero(int startOffset, int offset, int length) {
        checkThisOffset(offset);
        checkThisLength(offset, length);
        if (startOffset == -1) return -1;
        checkRelativeOffset(startOffset, length);
        return iOffsetOfLastZero(offset, startOffset+1);
    }
    
    /**
     * Returns the offset of the previous bit that is set to {@code ZERO} that occurs
     * on or before the specified start offset within the specified Field of this
     * {@code BitString}. If no such bit exists, or -1 if given as the start offset,
     * -1 is returned. The start offset and the returned offset are relative to the
     * start of the field.
     * 
     * @param startOffset the offset to start checking from (inclusive)
     * @param field       a Field of this {@code BitString}
     * @return the offset of the previous {@code ZERO} bit or -1 if no bits are set
     *         to {@code ZERO}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code field.offset() > 0 && field.offset() >= length()}
     *                                         or
     *                                         {@code startOffset < -1 || startOffset > 0 && startOffset >= field.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code field.length() > length() - field.offset()}
     */
    public int offsetOfPreviousZero(int startOffset, Field field) {
        checkThisOffset(field.offset());
        checkThisLength(field.offset(), field.length(this));
        if (startOffset == -1) return -1;
        checkRelativeOffset(startOffset, field.length(this));
        return iOffsetOfLastZero(field.offset(), startOffset+1);
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
        iReverse(0, length());
        return this;
    }
    
    public BitString reverse(int offset, int length) {
        checkThisOffset(offset);
        checkThisLength(offset, length);
        iReverse(offset, length);
        return this;
    }
    
    public BitString reverse(Field field) {
        return reverse(field.offset(), field.length(this));
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
     * @param nBits the number of bits to rotate
     * @return this {@code BitString}
     * 
     */
    public BitString rotateLeft(int nBits) {
        iRotateLeft(nBits, 0, length());
        return this;
    }
    
    /**
     * Rotates a substring of this {@code BitString} left the specified number of
     * bits (nBits).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a rotateRight(|nBits|...) is
     * performed instead of a rotateLeft.
     * 
     * The substring starts at offset 'offset' of this {@code BitString} and has a
     * length of 'length'.
     * 
     * Any bits rotated out on the left are rotated back into this {@code BitString}
     * on the right.
     * 
     * @param nBits  the number of bits to rotate
     * @param offset the start of this substring
     * @param length the length of this substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= this.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code length < 0 || length > this.length() - offset}
     */
    public BitString rotateLeft(int nBits, int offset, int length) {
        checkThisOffset(offset);
        checkThisLength(offset, length);
        iRotateLeft(nBits, offset, length);
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
     * @param nBits the number of bits to rotate
     * @param field a Field of this {@code BitString}
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code field.offset() > 0 && field.offset() >= this.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code field.length() > this.length() - field.offset()}
     */
    public BitString rotateLeft(int nBits, Field field) {
        return rotateLeft(nBits, field.offset(), field.length(this));
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
     * @param nBits number of bits to rotate
     * @param other the other bit string
     * @return this {@code BitString}
     */
    public BitString rotateLeft(int nBits, BitString other) {
        iRotateLeft(nBits, 0, this.length(), other, 0, other.length());
        return this;
    }
    
    /**
     * Rotates left a substring of this {@code BitString} and a substring of the
     * specified bit string (other) by the specified number of bits (nBits).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a rotateRight(|nBits|...) is
     * performed instead of a rotateLeft.
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
     * @param nBits       number of bits to rotate
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
    public BitString rotateLeft(int nBits,
            int thisOffset, int thisLength,
            BitString other, int otherOffset, int otherLength) {
        checkThisOffset(thisOffset);
        checkThisLength(thisOffset, thisLength);
        other.checkArgOffset(otherOffset);
        other.checkArgLength(otherOffset, otherLength);
        iRotateLeft(nBits, thisOffset, thisLength, other, otherOffset, otherLength);
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
     * @param nBits      number of bits to rotate
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
    public BitString rotateLeft(int nBits,
           Field thisField,
           BitString other, Field otherField) {
        return rotateLeft(nBits, thisField.offset(), thisField.length(this), other, otherField.offset(), otherField.length(other));
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
     * @param nBits the number of bits to rotate
     * @return this {@code BitString}
     * 
     */
    public BitString rotateRight(int nBits) {
        iRotateRight(nBits, 0, length());
        return this;
    }
    
    /**
     * Rotates a substring of this {@code BitString} right the specified number of
     * bits (nBits).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a rotateLeft(|nBits|...) is
     * performed instead of a rotateRight.
     * 
     * The substring starts at offset 'offset' of this {@code BitString} and has a
     * length of 'length'.
     * 
     * Any bits rotated out on the right are rotated back into this
     * {@code BitString} on the left.
     * 
     * @param nBits  the number of bits to rotate
     * @param offset the start of this substring
     * @param length the length of this substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= this.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code length < 0 || length > this.length() - offset}
     */
    public BitString rotateRight(int nBits, int offset, int length) {
         checkThisOffset(offset);
        checkThisLength(offset, length);
        iRotateRight(nBits, offset, length);
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
     * @param nBits the number of bits to rotate
     * @param field a Field of this {@code BitString}
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code field.offset() > 0 && field.offset() >= this.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code field.length() > this.length() - field.offset()}
     */
    public BitString rotateRight(int nBits, Field field) {
        return rotateRight(nBits, field.offset(), field.length(this));
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
     * @param nBits number of bits to rotate
     * @param other the other bit string
     * @return this {@code BitString}
     */
    public BitString rotateRight(int nBits, BitString other) {
        iRotateRight(nBits, 0, this.length(), other, 0, other.length());
        return this;
    }
    
    /**
     * Rotates right a substring of this {@code BitString} and a substring of the
     * specified bit string (other) by the specified number of bits (nBits).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a rotateLeft(|nBits|...) is
     * performed instead of a rotateRight.
     * 
     * This {@code BitString} and the other bit string are modified by this
     * operation (see {@link #rotateRight(int, BitString)} for details).
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * has a length of 'thisLength'.
     * 
     * The other substring starts at offset 'otherOffset' of the specified bit
     * string and has a length of 'otherLength'.
     * 
     * @param nBits       number of bits to rotate
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
    public BitString rotateRight(int nBits,
            int thisOffset, int thisLength,
            BitString other, int otherOffset, int otherLength) {
        checkThisOffset(thisOffset);
        checkThisLength(thisOffset, thisLength);
        other.checkArgOffset(otherOffset);
        other.checkArgLength(otherOffset, otherLength);
        iRotateRight(nBits, thisOffset, thisLength, other, otherOffset, otherLength);
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
     * @param nBits      number of bits to rotate
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
    public BitString rotateRight(int nBits,
            Field thisField,
            BitString other, Field otherField) {
        return rotateRight(nBits, thisField.offset(), thisField.length(this), other, otherField.offset(), otherField.length(other));
    }

    /**
     * Shifts this {@code BitString} left the specified number of bits (nBits).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a shiftRight(|nbits|) is
     * performed instead of a shiftLeft.
     * <p>
     * Any bits shifted out on the left are lost. Any positions vacated on the right
     * are filled by a {@code ZERO} value. If {@code nBits >= length}, this
     * bit string will be completely filled by zeros.
     * 
     * @param nBits number of bits to shift
     * @return this {@code BitString}
     */
    public BitString shiftLeft(int nBits) {
        return shiftLeft(nBits, ZERO_FILL);
    }
    
    /**
     * Shifts a substring of this {@code BitString} left the specified number of
     * bits (nBits).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a shiftRight(|nbits|...) is
     * performed instead of a shiftLeft.
     * 
     * This {@code BitString} is modified by this operation (see
     * {@link #shiftLeft(int)}.
     * 
     * The substring starts at offset 'offset' of this {@code BitString} and has a
     * length of 'length'.
     * 
     * @param nBits  number of bits to shift
     * @param offset the start of this substring
     * @param length the length of this substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= this.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code length < 0 || length > this.length() - offset}
     */
    public BitString shiftLeft(int nBits, int offset, int length) {
        return shiftLeft(nBits, ZERO_FILL, offset, length);
    }
    
    /**
     * Shifts a Field of this {@code BitString} left the specified number of bits
     * (nBits).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a shiftRight(|nbits|...) is
     * performed instead of a shiftLeft.
     * 
     * This {@code BitString} is modified by this operation (see
     * {@link #shiftLeft(int)}.
     * 
     * @param nBits number of bits to shift
     * @param field a Field of this {@code BitString}
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code field.offset() > 0 && field.offset() >= this.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code field.length() > this.length() - field.offset()}
     */
    public BitString shiftLeft(int nBits, Field field) {
        return shiftLeft(nBits, field.offset(), field.length(this));
    }
    
    /**
     * Shifts this {@code BitString} left the specified number of bits (nBits),
     * filling any vacated positions by the specified fill value (fill).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a shiftRight(|nbits|...) is
     * performed instead of a shiftLeft.
     * <p>
     * Any bits shifted out on the left are lost. Any positions vacated on the right
     * are filled by the specified fill value. If {@code nBits >= length()}, this
     * {@code BitString} will be completely filled by the specified fill value.
     * 
     * @param nBits number of bits to shift
     * @param fill  the fill value
     * @return this {@code BitString}
     */
    public BitString shiftLeft(int nBits, boolean fill) {
        iShiftLeft(nBits, fill, 0, length());
        return this;
    }
    
    /**
     * Shifts a substring of this {@code BitString} left the specified number of
     * bits (nBits), filling any vacated positions by the specified fill value
     * (fill).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a shiftRight(|nbits|...) is
     * performed instead of a shiftLeft.
     * 
     * This {@code BitString} is modified by this operation (see
     * {@link #shiftLeft(int, boolean)}.
     * 
     * The substring starts at offset 'offset' of this {@code BitString} and has a
     * length of 'length'.
     * 
     * @param nBits  number of bits to shift
     * @param fill   the fill value
     * @param offset the start of this substring
     * @param length the length of this substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= this.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code length < 0 || length > this.length() - offset}
     */
    public BitString shiftLeft(int nBits, boolean fill, int offset, int length) {
        checkThisOffset(offset);
        checkThisLength(offset, length);
        iShiftLeft(nBits, fill, offset, length);
        return this;
    }
    
    /**
     * Shifts a Field of this {@code BitString} left the specified number of bits
     * (nBits), filling any vacated positions by the specified fill value (fill).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a shiftRight(|nbits|...) is
     * performed instead of a shiftLeft.
     * 
     * This {@code BitString} is modified by this operation (see
     * {@link #shiftLeft(int, boolean)}.
     * 
     * @param nBits number of bits to shift
     * @param fill  the fill value
     * @param field a Field of this {@code BitString}
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code field.offset() > 0 && field.offset() >= this.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code field.length() > this.length() - field.offset()}
     */
    public BitString shiftLeft(int nBits, boolean fill, Field field) {
        return shiftLeft(nBits, fill, field.offset(), field.length(this));
    }
    
    /**
     * Shifts left this {@code BitString} and the specified bit string (other) by
     * the specified number of bits (nBits).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a shiftRight(|nbits|...)
     * is performed instead of a shiftLeft.
     * <p>
     * This bit string and the specified other bit string are treated as one bit
     * string. Any bits shifted out on the left of this bit string are shifted into
     * the other bit string on the right. Any positions vacated on the right of this
     * bit string are filled by a {@code ZERO} value. If
     * {@code nBits >= this.length}, this bit string will be completely filled by
     * zeros. if {@code nBits >= this.length + other.length}, both this bit string
     * and the other bit string will be completely filled by zeros.
     * 
     * @param nBits number of bits to shift
     * @param other the other bit string
     * @return this {@code BitString}
     */
    public BitString shiftLeft(int nBits, BitString other) {
        return shiftLeft(nBits, ZERO_FILL, other);
    }
    
    /**
     * Shifts left a substring of this {@code BitString} and a substring of the
     * specified bit string (other) by the specified number of bits (nBits).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a shiftRight(|nbits|...) is
     * performed instead of a shiftLeft.
     * 
     * This {@code BitString} and the other bit string are modified by this
     * operation (see {@link #shiftLeft(int, BitString)} for details).
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * has a length of 'thisLength'.
     * 
     * The other substring starts at offset 'otherOffset' of the specified bit
     * string and has a length of 'otherLength'.
     * 
     * @param nBits       number of bits to shift
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
    public BitString shiftLeft(int nBits,
            int thisOffset, int thisLength,
            BitString other, int otherOffset, int otherLength) {
        return shiftLeft(nBits, ZERO_FILL, thisOffset, thisLength, other, otherOffset, otherLength);
    }
    
    /**
     * Shifts left a Field of this {@code BitString} and a Field of the specified
     * bit string (other) by the specified number of bits (nBits).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a shiftRight(|nbits|...) is
     * performed instead of a shiftLeft.
     * 
     * This {@code BitString} and the other bit string are modified by this
     * operation (see {@link #shiftLeft(int, BitString)} for details).
     * 
     * @param nBits      number of bits to shift
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
    public BitString shiftLeft(int nBits,
            Field thisField,
            BitString other, Field otherField) {
        return shiftLeft(nBits, thisField.offset(), thisField.length(this), other, otherField.offset(), otherField.length(other));
    }
    
    /**
     * Shifts left this {@code BitString} and the specified bit string (other) by
     * the specified number of bits (nBits), filling any vacated positions by the
     * specified fill value (fill).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a shiftRight(|nbits|...)
     * is performed instead of a shiftLeft.
     * <p>
     * This {@code BitString} and the specified other bit string are treated as one
     * bit string. Any bits shifted out on the left of this {@code BitString} are
     * shifted into the other bit string on the right. Any positions vacated on the
     * right of this {@code BitString} are filled by the fill value. If
     * {@code nBits >= this.length()}, this {@code BitString} will be completely
     * filled by the fill value. if {@code nBits >= this.length() + other.length()},
     * both this {@code BitString} and the other bit string will be completely
     * filled by the fill value.
     * 
     * @param nBits number of bits to shift
     * @param fill  the fill value
     * @param other the other bit string
     * @return this {@code BitString}
     */
    public BitString shiftLeft(int nBits, boolean fill, BitString other) {
        iShiftLeft(nBits, fill, 0, this.length(), other, 0, other.length());
        return this;
    }
    
    /**
     * Shifts left a substring of this {@code BitString} and a substring of the
     * specified bit string (other) by the specified number of bits (nBits), filling
     * any vacated positions by the specified fill value (fill).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a shiftRight(|nbits|...) is
     * performed instead of a shiftLeft.
     * 
     * This {@code BitString} and the other bit string are modified by this
     * operation (see {@link #shiftLeft(int, boolean, BitString)} for details).
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * has a length of 'thisLength'.
     * 
     * The other substring starts at offset 'otherOffset' of the specified bit
     * string and has a length of 'otherLength'.
     * 
     * @param nBits       number of bits to shift
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
    public BitString shiftLeft(int nBits, boolean fill,
            int thisOffset, int thisLength,
            BitString other, int otherOffset, int otherLength) {
        checkThisOffset(thisOffset);
        checkThisLength(thisOffset, thisLength);
        other.checkArgOffset(otherOffset);
        other.checkArgLength(otherOffset, otherLength);
        iShiftLeft(nBits, fill, thisOffset, thisLength, other, otherOffset, otherLength);
        return this;
    }
    
    /**
     * Shifts left a Field of this {@code BitString} and a Field of the specified
     * bit string (other) by the specified number of bits (nBits), filling any
     * vacated positions by the specified fill value (fill).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a shiftRight(|nbits|...) is
     * performed instead of a shiftLeft.
     * 
     * This {@code BitString} and the other bit string are modified by this
     * operation (see {@link #shiftLeft(int, boolean, BitString)} for details).
     * 
     * @param nBits      number of bits to shift
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
    public BitString shiftLeft(int nBits, boolean fill,
            Field thisField,
            BitString other, Field otherField) {
        return shiftLeft(nBits, fill, thisField.offset(), thisField.length(this), other, otherField.offset(), otherField.length(other));
    }
    
    /**
     * Shifts this {@code BitString} right the specified number of bits (nBits).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a shiftLeft(|nbits|) is
     * performed instead of a shiftRight.
     * <p>
     * Any bits shifted out on the right are lost. Any positions vacated on the left
     * are filled by a {@code ZERO} value. If {@code nBits >= length}, this
     * bit string will be completely filled by zeros.
     * 
     * @param nBits number of bits to shift
     * @return this {@code BitString}
     */
    public BitString shiftRight(int nBits) {
        return shiftRight(nBits, ZERO_FILL);
    }
    
    /**
     * Shifts a substring of this {@code BitString} right the specified number of
     * bits (nBits).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a shiftLeft(|nbits|...) is
     * performed instead of a shiftRight.
     * 
     * This {@code BitString} is modified by this operation (see
     * {@link #shiftRight(int)}.
     * 
     * The substring starts at offset 'offset' of this {@code BitString} and has a
     * length of 'length'.
     * 
     * @param nBits  number of bits to shift
     * @param offset the start of this substring
     * @param length the length of this substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= this.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code length < 0 || length > this.length() - offset}
     */
    public BitString shiftRight(int nBits, int offset, int length) {
        return shiftRight(nBits, ZERO_FILL, offset, length);
    }
    
    /**
     * Shifts a Field of this {@code BitString} right the specified number of bits
     * (nBits).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a shiftLeft(|nbits|...) is
     * performed instead of a shiftRight.
     * 
     * This {@code BitString} is modified by this operation (see
     * {@link #shiftRight(int)}.
     * 
     * @param nBits number of bits to shift
     * @param field a Field of this {@code BitString}
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code field.offset() > 0 && field.offset() >= this.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code field.length() > this.length() - field.offset()}
     */
    public BitString shiftRight(int nBits, Field field) {
        return shiftRight(nBits, field.offset(), field.length(this));
    }
    
    /**
     * Shifts this {@code BitString} right the specified number of bits (nBits),
     * filling any vacated positions by the specified fill value (fill).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a shiftLeft(|nbits|...) is
     * performed instead of a shiftRight.
     * <p>
     * Any bits shifted out on the right are lost. Any positions vacated on the left
     * are filled by the specified fill value. If {@code nBits >= length()}, this
     * {@code BitString} will be completely filled by the specified fill value.
     * 
     * @param nBits number of bits to shift
     * @param fill  the fill value
     * @return this {@code BitString}
     */
    public BitString shiftRight(int nBits, boolean fill) {
        iShiftRight(nBits, fill, 0, length());
        return this;
    }
    
    /**
     * Shifts a substring of this {@code BitString} right the specified number of
     * bits (nBits), filling any vacated positions by the specified fill value
     * (fill).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a shiftLeft(|nbits|...) is
     * performed instead of a shiftRight.
     * 
     * This {@code BitString} is modified by this operation (see
     * {@link #shiftRight(int, boolean)}.
     * 
     * The substring starts at offset 'offset' of this {@code BitString} and has a
     * length of 'length'.
     * 
     * @param nBits  number of bits to shift
     * @param fill   the fill value
     * @param offset the start of this substring
     * @param length the length of this substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= this.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code length < 0 || length > this.length() - offset}
     */
    public BitString shiftRight(int nBits, boolean fill, int offset, int length) {
        checkThisOffset(offset);
        checkThisLength(offset, length);
        iShiftRight(nBits, fill, offset, length);
        return this;
    }
    
    /**
     * Shifts a Field of this {@code BitString} right the specified number of bits
     * (nBits), filling any vacated positions by the specified fill value (fill).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a shiftLeft(|nbits|...) is
     * performed instead of a shiftRight.
     * 
     * This {@code BitString} is modified by this operation (see
     * {@link #shiftRight(int, boolean)}.
     * 
     * @param nBits number of bits to shift
     * @param fill  the fill value
     * @param field a Field of this {@code BitString}
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code field.offset() > 0 && field.offset() >= this.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code field.length() > this.length() - field.offset()}
     */
    public BitString shiftRight(int nBits, boolean fill, Field field) {
        return shiftRight(nBits, fill, field.offset(), field.length(this));
    }
    
    /**
     * Shifts right this {@code BitString} and the specified bit string (other) by
     * the specified number of bits (nBits).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a shiftLeft(|nbits|...)
     * is performed instead of a shiftRight.
     * <p>
     * This bit string and the specified other bit string are treated as one bit
     * string. Any bits shifted out on the right of this bit string are shifted into
     * the other bit string on the left. Any positions vacated on the left of this
     * bit string are filled by a {@code ZERO} value. If
     * {@code nBits >= this.length}, this bit string will be completely filled by
     * zeros. if {@code nBits >= this.length + other.length}, both this bit string
     * and the other bit string will be completely filled by zeros.
     * 
     * @param nBits number of bits to shift
     * @param other the other bit string
     * @return this {@code BitString}
     */
    public BitString shiftRight(int nBits, BitString other) {
        return shiftRight(nBits, ZERO_FILL, other);
    }
    
    /**
     * Shifts right a substring of this {@code BitString} and a substring of the
     * specified bit string (other) by the specified number of bits (nBits).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a shiftLeft(|nbits|...) is
     * performed instead of a shiftRight.
     * 
     * This {@code BitString} and the other bit string are modified by this
     * operation (see {@link #shiftRight(int, BitString)} for details).
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * has a length of 'thisLength'.
     * 
     * The other substring starts at offset 'otherOffset' of the specified bit
     * string and has a length of 'otherLength'.
     * 
     * @param nBits       number of bits to shift
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
    public BitString shiftRight(int nBits,
            int thisOffset, int thisLength,
            BitString other, int otherOffset, int otherLength) {
        return shiftRight(nBits, ZERO_FILL, thisOffset, thisLength, other, otherOffset, otherLength);
    }
    
    /**
     * Shifts right a Field of this {@code BitString} and a Field of the specified
     * bit string (other) by the specified number of bits (nBits).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a shiftLeft(|nbits|...) is
     * performed instead of a shiftRight.
     * 
     * This {@code BitString} and the other bit string are modified by this
     * operation (see {@link #shiftRight(int, BitString)} for details).
     * 
     * @param nBits      number of bits to shift
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
    public BitString shiftRight(int nBits,
            Field thisField,
            BitString other, Field otherField) {
        return shiftRight(nBits, thisField.offset(), thisField.length(this), other, otherField.offset(), otherField.length(other));
    }
    
    /**
     * Shifts right this {@code BitString} and the specified bit string (other) by
     * the specified number of bits (nBits), filling any vacated positions by the
     * specified fill value (fill).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a shiftLeft(|nbits|...)
     * is performed instead of a shiftRight.
     * <p>
     * This {@code BitString} and the specified other bit string are treated as one
     * bit string. Any bits shifted out on the right of this {@code BitString} are
     * shifted into the other bit string on the left. Any positions vacated on the
     * left of this {@code BitString} are filled by the fill value. If
     * {@code nBits >= this.length()}, this {@code BitString} will be completely
     * filled by the fill value. if {@code nBits >= this.length() + other.length()},
     * both this {@code BitString} and the other bit string will be completely
     * filled by the fill value.
     * 
     * @param nBits number of bits to shift
     * @param fill  the fill value
     * @param other the other bit string
     * @return this {@code BitString}
     */
    public BitString shiftRight(int nBits, boolean fill, BitString other) {
        iShiftRight(nBits, fill, 0, this.length(), other, 0, other.length());
        return this;
    }
    
    /**
     * Shifts right a substring of this {@code BitString} and a substring of the
     * specified bit string (other) by the specified number of bits (nBits), filling
     * any vacated positions by the specified fill value (fill).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a shiftLeft(|nbits|...) is
     * performed instead of a shiftRight.
     * 
     * This {@code BitString} and the other bit string are modified by this
     * operation (see {@link #shiftRight(int, boolean, BitString)} for details).
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * has a length of 'thisLength'.
     * 
     * The other substring starts at offset 'otherOffset' of the specified bit
     * string and has a length of 'otherLength'.
     * 
     * @param nBits       number of bits to shift
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
    public BitString shiftRight(int nBits, boolean fill,
            int thisOffset, int thisLength,
            BitString other, int otherOffset, int otherLength) {
        checkThisOffset(thisOffset);
        checkThisLength(thisOffset, thisLength);
        other.checkArgOffset(otherOffset);
        other.checkArgLength(otherOffset, otherLength);
        iShiftRight(nBits, fill, thisOffset, thisLength, other, otherOffset, otherLength);
        return this;
    }
    
    /**
     * Shifts right a Field of this {@code BitString} and a Field of the specified
     * bit string (other) by the specified number of bits (nBits), filling any
     * vacated positions by the specified fill value (fill).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a shiftLeft(|nbits|...) is
     * performed instead of a shiftRight.
     * 
     * This {@code BitString} and the other bit string are modified by this
     * operation (see {@link #shiftRight(int, boolean, BitString)} for details).
     * 
     * @param nBits      number of bits to shift
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
    public BitString shiftRight(int nBits, boolean fill,
            Field thisField,
            BitString other, Field otherField) {
        return shiftRight(nBits, fill, thisField.offset(), thisField.length(this), other, otherField.offset(), otherField.length(other));
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
    
    public boolean[] toBooleanArray() {
        return iToBooleanArray(0, length());
    }
    
    public boolean[] toBooleanArray(int offset, int length) {
        checkThisOffset(offset);
        checkThisLength(offset, length);
        return iToBooleanArray(offset, length);
    }
    
    public boolean[] toBooleanArray(Field field) {
        return toBooleanArray(field.offset(), field.length(this));
    }

    /**
     * Returns a new byte array containing all the bits in this bit string.
     *
     * <p>More precisely, if
     * <br>{@code byte[] bytes = s.toByteArray();}
     * <br>then {@code bytes.length == (s.length()+7)/8} and
     * <br>{@code s.get(n) == ((bytes[n/8] & (1<<(n%8))) != 0)}
     * <br>for all {@code n < 8 * bytes.length}.
     *
     * @return a byte array containing a little-endian representation
     *         of all the bits in this bit set
     */
    public byte[] toByteArray() {
        return iToByteArray(0, length());
    }
    
    public byte[] toByteArray(int offset, int length) {
        checkThisOffset(offset);
        checkThisLength(offset, length);
        return iToByteArray(offset, length);
    }
    
    public byte[] toByteArray(Field field) {
        return toByteArray(field.offset(), field.length(this));
    }
    
    public char[] toCharArray() {
        return iToCharArray(0, length());
    }
    
    public char[] toCharArray(int offset, int length) {
        checkThisOffset(offset);
        checkThisLength(offset, length);
        return iToCharArray(offset, length);
    }
    
    public char[] toCharArray(Field field) {
        return toCharArray(field.offset(), field.length(this));
    }
    
    public double[] toDoubleArray() {
        return iToDoubleArray(0, length());
    }
    
    public double[] toDoubleArray(int offset, int length) {
        checkThisOffset(offset);
        checkThisLength(offset, length);
        return iToDoubleArray(offset, length);
    }
    
    public double[] toDoubleArray(Field field) {
        return toDoubleArray(field.offset(), field.length(this));
    }
    
    public float[] toFloatArray() {
        return iToFloatArray(0, length());
    }
    
    public float[] toFloatArray(int offset, int length) {
        checkThisOffset(offset);
        checkThisLength(offset, length);
        return iToFloatArray(offset, length);
    }
    
    public float[] toFloatArray(Field field) {
        return toFloatArray(field.offset(), field.length(this));
    }
    
    public int[] toIntArray() {
        return iToIntArray(0, length());
    }
    
    public int[] toIntArray(int offset, int length) {
        checkThisOffset(offset);
        checkThisLength(offset, length);
        return iToIntArray(offset, length);
    }
    
    public int[] toIntArray(Field field) {
        return toIntArray(field.offset(), field.length(this));
    }

    /**
     * Returns a new long array containing all the bits in this bit string.
     *
     * <p>More precisely, if
     * <br>{@code long[] longs = s.toLongArray();}
     * <br>then {@code longs.length == (s.length()+63)/64} and
     * <br>{@code s.get(n) == ((longs[n/64] & (1L<<(n%64))) != 0)}
     * <br>for all {@code n < 64 * longs.length}.
     *
     * @return a long array containing a big-endian representation
     *         of all the bits in this bit set
     */
    public long[] toLongArray() {
        return iToLongArray(0, length());
    }
    
    public long[] toLongArray(int offset, int length) {
        checkThisOffset(offset);
        checkThisLength(offset, length);
        return iToLongArray(offset, length);
    }
    
    public long[] toLongArray(Field field) {
        return toLongArray(field.offset(), field.length(this));
    }
    
    public short[] toShortArray() {
        return iToShortArray(0, length());
    }
    
    public short[] toShortArray(int offset, int length) {
        checkThisOffset(offset);
        checkThisLength(offset, length);
        return iToShortArray(offset, length);
    }
    
    public short[] toShortArray(Field field) {
        return toShortArray(field.offset(), field.length(this));
    }
    
    @Override
    public String toString() {
        return toString(0, this.length());
    }
    
    /**
     * 
     * @param offset
     * @param length
     * @return
     * @throws java.lang.OutOfMemoryError Requested array size exceeds VM limit
     */
    public String toString(int offset, int length) {
        checkThisOffset(offset);
        checkThisLength(offset, length);
        if (length == 0) return "";
        final StringBuilder string = new StringBuilder(length);
        final int[] iterator = getIterator(offset, length);
        while (hasNextIteratorWord(iterator)) {
            final long word = getNextIteratorFullWord(iterator);
            String wordString = String.format("%64s", Long.toBinaryString(word)).replace(' ', '0');
            final int wordBitCount = getIteratorWordBitCount(iterator);
            if (wordBitCount < BITS_PER_WORD) wordString = wordString.substring(0, wordBitCount);
            string.append(wordString);
        }
        return string.toString();
    }
    
    public String toString(Field field) {
        return toString(field.offset(), field.length(this));
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
            final long word = getNextIteratorFullWord(iterator);
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
        
        public static Field indexRange(int fromIndex, int toIndex) {
            if (fromIndex < 0) throw new IndexOutOfBoundsException("fromIndex is negative; index="+fromIndex);
            if (toIndex < 0) throw new IndexOutOfBoundsException("toIndex is negative; index="+toIndex);
            if (fromIndex > toIndex) throw new IndexOutOfBoundsException("fromIndex > toIndex; from="+fromIndex+", to="+toIndex);
            return new Field(fromIndex, toIndex-fromIndex);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(length, offset);
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof Field)) return false;
            if (obj instanceof Field.All) return false;
            final Field that = (Field)obj;
            return this.offset() == that.length() && this.length() == that.length();
        }
        
        @Override
        public String toString() {
            return offset() + "," + length();
        }

        private static class All extends Field {
            
            private All() {
                super(0, 0);
            }
            
            @Override
            int length(BitString bitString) {
                return bitString.length();
            }
            
            @Override
            public boolean equals(Object obj) {
                if (!(obj instanceof Field.All)) return false;
                return super.equals(obj);
            }
            
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
        void setWord(int wordIndex, long word) {
            throwCanNotBeModifiedException();
        }
        
        private String noCapacityMsg() {
            return "BitString Constants have no actual capacity";
        }
        
        @Override
        void resizeBackingArray(int capacity) {
            throw new UnsupportedOperationException(noCapacityMsg());
        }
        
        @Override
        public int capacity() {
            throw new UnsupportedOperationException(noCapacityMsg());
        }
        
        @Override
        public int ensureCapacity(int bitsRequired) {
            throw new UnsupportedOperationException(noCapacityMsg());
        }
        
        @Override
        public int trimToLength() {
            throw new UnsupportedOperationException(noCapacityMsg());
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
        void setWord(int wordIndex, long word) {
            checkForModificationException();
            base.setWord(wordIndex, word);
        }
        
        @Override
        void resizeBackingArray(int capacity) {
            base.resizeBackingArray(capacity);
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
            return base.ensureCapacity((int)baseCapacity);
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
            int bitIndex = super.lastBitIndex() + 1;
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
        public BitString append(BitString that, int thatOffset, int thatLength) {
            that.checkArgOffset(thatOffset);
            that.checkArgLength(thatOffset, thatLength);
            base.checkBaseLengthIncrease(thatLength);
            iInsert(super.lastBitIndex()+1, that, thatOffset, thatLength);
            return this;
        }
        
        @Override
        public BitString range(int offset, int length) {
            checkForModificationException();
            super.checkThisOffset(offset);
            super.checkThisLength(offset, length);
            return new Range(base, this, super.firstBitIndex(offset), length);
        }
        
    }

}
