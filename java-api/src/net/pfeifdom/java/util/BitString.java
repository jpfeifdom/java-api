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
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.IntToLongFunction;
import java.util.function.LongBinaryOperator;
import java.util.function.LongToIntFunction;
import java.util.function.Predicate;
import java.util.function.ToLongBiFunction;
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
 * </pre> 
 * 
 * @author james
 * @since 1.1
 * @since JDK 1.8
 */
public class BitString implements Cloneable, Serializable  {
    
    /**
     * 
     */
    private static final long serialVersionUID = 3661279563936726028L;

//    public static enum Bit {
//        
//        ZERO (  "zero", '0',    false,  0),
//        ONE  (  "one",  '1',    true,   1);
//        
//        private final String label;
//        private final char chr;
//        private final boolean bool;
//        private final int num;
//        
//        private Bit(String label, char chr, boolean bool, int num) {
//            this.label = label;
//            this.chr = chr;
//            this.bool = bool;
//            this.num = num;
//        }
//        public static Bit valueOf(boolean bool) { return bool ? ONE : ZERO; }
//        public static Bit valueOf(int num) { return num == 0 ? ZERO : ONE; }
//        
//        public String label() { return label; }
//        public char chr() { return chr; }
//        public boolean bool() { return bool; }
//        public int num() { return num; }
//    }
    
    /*
     * BitStrings are packed into arrays of "words."  Currently a word is
     * a long, which consists of 64 bits, requiring 6 address bits.
     * The choice of word size is determined purely by performance concerns.
     */
    private static final int ADDRESS_BITS_PER_WORD = 6;
    private static final int BITS_PER_WORD = 1 << ADDRESS_BITS_PER_WORD;
    private static final int BIT_INDEX_MASK = BITS_PER_WORD - 1;

    /* Used to shift left or right for a partial word mask */
    private static final long WORD_MASK = 0xffffffffffffffffL;
    
    private static final long BIT_MASK = 0x8000000000000000L;
    
    public static final BitString ONES = new Constant(WORD_MASK);
    public static final BitString ZEROS = new Constant(0L);
    
    public static final boolean ONE = true;
    public static final boolean ZERO = false;
    public static final boolean ONE_FILL = ONE;
    public static final boolean ZERO_FILL = ZERO;
    private static final boolean ONE_DFLT = ONE;
    private static final boolean ZERO_DFLT = ZERO;
    
    public static Field field(int offset, int length) {
        if (offset < 0) throw new IllegalArgumentException("offset is negative; offset="+offset);
        if (length < 0) throw new IllegalArgumentException("length is negative; length="+length);
        return new Field(offset, length);
    }
    
    public static Field indexField(int fromIndex, int toIndex) {
        if (fromIndex < 0) throw new IndexOutOfBoundsException("fromIndex is negative; index="+fromIndex);
        if (toIndex < 0) throw new IndexOutOfBoundsException("toIndex is negative; index="+toIndex);
        if (fromIndex > toIndex) throw new IndexOutOfBoundsException("fromIndex > toIndex; from="+fromIndex+", to="+toIndex);
        return new Field(fromIndex, toIndex-fromIndex);
    }
    
    /**
     * The internal field corresponding to the serialField "bits".
     */
    private long[] words;
    
    /**
     * The number of bits in this bit string
     */
    private int stringLength;
    
    /**
     * Incremented any time the length of the bit string is modified
     * in a way that could invalidate Ranges
     */
    private long modCount = 0L;
    
//    private BitString temp = new BitString();

    /**
     * Creates a new bit string. All bits are initially {@code false}.
     */
    public BitString() {
        this.stringLength = 0;
        initWords(BITS_PER_WORD);
     }

    /**
     * Creates a bit string whose initial size is large enough to explicitly
     * represent bits with indices in the range {@code 0} through
     * {@code nBits-1}. All bits are initially {@code false}.
     *
     * @param  length the initial size of the bit string
     * @throws IllegalArgumentException if the specified initial size
     *         is negative
     */
    public BitString(int length) {
        checknBits(length);
        this.stringLength = length;
        initWords(length);
    }
    
    public BitString(int length, int capacity) {
        checknBits(length);
        if (capacity < length) {
            throw new IllegalArgumentException("capacity (" + capacity + ") < length (" + length + ")");
        }
        this.stringLength = length;
        initWords(capacity);
    }
    
    /**
     * Creates a bit string using words as the internal representation.
     */
//    private BitString(long[] words) {
//        this.stringLength = words.length * BITS_PER_WORD;
//        this.words = words;
//    }
    
    private BitString(long[] words, int length) {
        this.stringLength = length;
        this.words = words;
    }

    private void initWords(int capacity) {
        this.words = new long[wordIndex(capacity-1) + 1];
    }
    
    private void resizeWords(int capacity) {
        final int newLength = wordIndex(capacity-1) + 1;
        if (newLength == words.length) return;
        words = Arrays.copyOf(words, newLength);
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
    public Object clone() {
        try {
            BitString result = (BitString) super.clone();
            result.words = words.clone();
            result.stringLength = stringLength;
            return result;
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }

    /**
     * Save the state of the {@code BitString} instance to a stream (i.e.,
     * serialize it).
     * 
     * @param stream stream to save the state of this instance
     * @throws IOException I/O error occurred while writing to stream
     * @serialData The length of the string (the number of bits it contains) is
     *             emitted (int), followed by all of its words (each a long)
     *             in the proper order.
     */
    private void writeObject(ObjectOutputStream stream)
        throws IOException {
        ObjectOutputStream.PutField fields = stream.putFields();
        fields.put("length", stringLength);
        fields.put("bits", words);
        stream.writeFields();
    }

    /**
     * Reconstitute the {@code BitString} instance from a stream (i.e.,
     * deserialize it).
     * 
     * @param stream stream to be deserialized
     * @throws ClassNotFoundException Class of a serialized object cannot be found.
     * @throws IOException            I/O error occurred while reading from stream
     */
    private void readObject(ObjectInputStream stream)
        throws IOException, ClassNotFoundException {
        ObjectInputStream.GetField fields = stream.readFields();
        stringLength = fields.get("length", 0);
        words = (long[]) fields.get("bits", null);
    }
    
    public static BitString valueOf(boolean[] booleans) {
        return new BitString(wrapBooleans(booleans), booleans.length);
    }
    
    public static BitString valueOf(byte[] bytes) {
        checkNewBitStringLength(bytes.length * (long)Byte.SIZE);
        return new BitString(wrapBytes(bytes), bytes.length * Byte.SIZE);
    }
    
    public static BitString valueOf(char[] chars) {
        checkNewBitStringLength(chars.length * (long)Character.SIZE);
        return new BitString(wrapChars(chars), chars.length * Character.SIZE);
    }
    
    public static BitString valueOf(double[] doubles) {
        checkNewBitStringLength(doubles.length * (long)Double.SIZE);
        return new BitString(wrapDoubles(doubles), doubles.length * Double.SIZE);
    }
    
    public static BitString valueOf(float[] floats) {
        checkNewBitStringLength(floats.length * (long)Float.SIZE);
        return new BitString(wrapFloats(floats), floats.length * Float.SIZE);
    }
    
    public static BitString valueOf(int[] ints) {
        checkNewBitStringLength(ints.length * (long)Integer.SIZE);
        return new BitString(wrapInts(ints), ints.length * Integer.SIZE);
    }
    
    /**
     * Returns a new bit String containing all the bits in the given long array.
     *
     * <p>More precisely,
     * <br>{@code BitString.valueOf(longs).get(n) == ((longs[n/64] & (BIT_MASK>>>(n%64))) != 0)}
     * <br>for all {@code n < 64 * longs.length}.
     *
     * <p>This method is equivalent to
     * {@code BitSet.valueOf(LongBuffer.wrap(longs))}.
     *
     * @param longs a long array containing a big-endian representation
     *        of a sequence of bits to be used as the initial bits of the
     *        new bit set
     * @return a {@code BitString} containing all the bits in the long array
     */
    public static BitString valueOf(long[] longs) {
        checkNewBitStringLength(longs.length * (long)Long.SIZE);
        return new BitString(Arrays.copyOf(longs, longs.length), longs.length * Long.SIZE);
    }
    
    public static BitString valueOf(short[] shorts) {
        checkNewBitStringLength(shorts.length * (long)Short.SIZE);
        return new BitString(wrapShorts(shorts), shorts.length * Short.SIZE);
    }
    
//    public static BitString valueOf(int[] ints) {
//        //assert Integer.SIZE == Long.SIZE / 2;
//        if (ints.length * (long) Integer.SIZE > Integer.MAX_VALUE) {
//            throw new IllegalArgumentException("the number of bits in the array (" + ints.length * (long) Integer.SIZE
//                    + ") exceed the maximum of " + Integer.MAX_VALUE);
//        }
//        final long[] longs = new long[(ints.length + 1) / 2];
//        for (int i = 0, l = 0; i < ints.length; i++) {
//            if ((i % 2) == 0) {
//                longs[l] = ((long)ints[i]) << Integer.SIZE; // first half of long
//            } else {
//                longs[l] |= Integer.toUnsignedLong(ints[i]); // last half of long
//                l++;
//            }
//        }
//        return new BitString(longs);
//    }
    
    /**
     * Returns a new bit string containing all the bits in the given long
     * buffer between its position and limit.
     *
     * <p>More precisely,
     * <br>{@code BitString.valueOf(lb).get(n) == ((lb.get(lb.position()+n/64) & (BIT_MASK>>>(n%64))) != 0)}
     * <br>for all {@code n < 64 * lb.remaining()}.
     *
     * <p>The long buffer is not modified by this method, and no
     * reference to the buffer is retained by the bit set.
     *
     * @param lb a long buffer containing a big-endian representation
     *        of a sequence of bits between its position and limit, to be
     *        used as the initial bits of the new bit set
     * @return a {@code BitString} containing all the bits in the buffer in the
     *         specified range
     */
    public static BitString valueOf(LongBuffer lb) {
        lb = lb.slice();
        checkNewBitStringLength(lb.remaining() * (long)Long.SIZE);
        long[] words = new long[lb.remaining()];
        lb.get(words);
        return new BitString(words, words.length * Long.SIZE);
    }

//    /**
//     * Returns a new bit string containing all the bits in the given byte array.
//     *
//     * <p>More precisely,
//     * <br>{@code BitString.valueOf(bytes).get(n) == ((bytes[n/8] & (BIT_MASK>>>(n%8))) != 0)}
//     * <br>for all {@code n <  8 * bytes.length}.
//     *
//     * <p>This method is equivalent to
//     * {@code BitSet.valueOf(ByteBuffer.wrap(bytes))}.
//     *
//     * @param bytes a byte array containing a big-endian
//     *        representation of a sequence of bits to be used as the
//     *        initial bits of the new bit set
//     * @return a {@code BitString} containing all the bits in the byte array
//     */
//    public static BitString valueOf(byte[] bytes) {
//        return BitString.valueOf(ByteBuffer.wrap(bytes));
//    }

    /**
     * Returns a new bit string containing all the bits in the given byte
     * buffer between its position and limit.
     *
     * <p>More precisely,
     * <br>{@code BitString.valueOf(bb).get(n) == ((bb.get(bb.position()+n/8) & (BIT_MASK>>>(n%8))) != 0)}
     * <br>for all {@code n < 8 * bb.remaining()}.
     *
     * <p>The byte buffer is not modified by this method, and no
     * reference to the buffer is retained by the bit set.
     *
     * @param bb a byte buffer containing a big-endian representation
     *        of a sequence of bits between its position and limit, to be
     *        used as the initial bits of the new bit set
     * @return a {@code BitString} containing all the bits in the buffer in the
     *         specified range
     */
    public static BitString valueOf(ByteBuffer bb) {
        bb = bb.slice();
        checkNewBitStringLength(bb.remaining() * (long)Byte.SIZE);
        final int bitCount = bb.remaining() * Byte.SIZE;
        final int wordCount = (bb.remaining() + 7) / 8;
        long[] words = new long[wordCount];
        int i = 0;
        while (bb.remaining() >= 8) {
            words[i++] = bb.getLong();
        }
        for (int remaining = bb.remaining(), j = 0; j < remaining; j++) {
            words[i] |= (bb.get() & 0xffL) << (8 * (6 - j));
        }
        return new BitString(words, bitCount);
    }
    
    long getWord(int wordIndex) {
        assert wordIndex >= 0 && wordIndex < this.words.length;
        return this.words[wordIndex];
    }
    
    void setWord(int wordIndex, long word) {
        assert wordIndex >= 0 && wordIndex < this.words.length;
        words[wordIndex] = word;
    }
    
    public int capacity() {
        return words.length > (Integer.MAX_VALUE / BITS_PER_WORD)
                ? Integer.MAX_VALUE
                : words.length * BITS_PER_WORD;
    }
    
    public int ensureCapacity(int capacity) {
        if (capacity > capacity()) resizeWords(capacity);
        return capacity();
    }
    
    public int trimToLength() {
        resizeWords(length());
        return capacity();
    }
    
    public int length() {
        return this.stringLength;
    }
    
    int baseLength() {
        return this.stringLength;
    }
    
    void iSetLength(int newLength) {
        this.stringLength = newLength;
    }
    
    public void setLength(int newLength) {
        if (newLength == this.stringLength) return;
        if (newLength < 0) throw new IllegalArgumentException("specified length is negative: "+newLength);
        ensureCapacity(newLength);
        final int oldLength = this.stringLength;
        this.stringLength = newLength;
        if (newLength > oldLength) iClear(oldLength, newLength-oldLength);
        if (newLength < oldLength) incrementModCount(); // do not invalidate Ranges if appending
    }
    
    // a Range of this BitString has changed its length by the specified delta at the specified bitIndex
    void setRangeLength(int delta, int bitIndex) {
        if (delta == 0) return;
        final long newLength = length() + delta;
        assert newLength > 0;
        if (newLength > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "the specified new length will cause the base BitString length to exceed the maximum size of "
                            + Integer.MAX_VALUE + ": " + "newLength=" + newLength + ", delta=" + delta);
        }
        if (bitIndex == length()) {
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
    
    public boolean isEmpty() {
        return length() == 0;
    }
    
    long modCount() {
        return this.modCount;
    }
    
    private void incrementModCount() {
        this.modCount++;
    }
    
    int bitIndex(int offset) {
        return offset;
    }
    
    private int firstBitIndex(int offset) {
        return bitIndex(offset);
    }
    
    private int lastBitIndex() {
        return lastBitIndex(0, length());
    }
    
    private int lastBitIndex(int offset, int length) {
        return firstBitIndex(offset) + (length - 1);
    }
    
    private static int wordBitIndex(int bitIndex) {
        return bitIndex & BIT_INDEX_MASK; 
    }
    
    private static int wordBitIndex(long longBitIndex) {
        return (int)(longBitIndex & BIT_INDEX_MASK); 
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
     * Given a bit index, return word index containing it.
     */
    private static int wordIndex(int bitIndex) {
        return bitIndex >> ADDRESS_BITS_PER_WORD;
    }
    
    private static int wordIndex(long longBitIndex) {
        return (int)(longBitIndex >> ADDRESS_BITS_PER_WORD);
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
        if (wordIndex == firstWordIndex && wordIndex == lastWordIndex) {
            iterator[6] = (remainingLength > BITS_PER_WORD) ? BITS_PER_WORD-(rightMarginSize+leftMarginSize) : remainingLength;
        } else if (wordIndex == firstWordIndex) {
            iterator[6] = (remainingLength > BITS_PER_WORD) ? BITS_PER_WORD-leftMarginSize : remainingLength;
        } else if (wordIndex == lastWordIndex) {
            iterator[6] = (remainingLength > BITS_PER_WORD) ? BITS_PER_WORD-rightMarginSize : remainingLength;
        } else {
            iterator[6] = (remainingLength > BITS_PER_WORD) ? BITS_PER_WORD : remainingLength;
        }
        iterator[5] -= iterator[6];
        return fillMargins(fill, getWord(wordIndex), wordIndex,
                firstWordIndex, lastWordIndex, leftMarginSize, rightMarginSize);
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
        final int remainingLength = iterator[5];
        if (wordIndex == firstWordIndex || remainingLength <= 0) throw new IllegalStateException();
        if (wordIndex == -1) wordIndex = lastWordIndex;
        else wordIndex--;
        iterator[0] = wordIndex;
        if (wordIndex == firstWordIndex && wordIndex == lastWordIndex) {
            iterator[6] = (remainingLength > BITS_PER_WORD) ? BITS_PER_WORD-(rightMarginSize+leftMarginSize) : remainingLength;
        } else if (wordIndex == firstWordIndex) {
            iterator[6] = (remainingLength > BITS_PER_WORD) ? BITS_PER_WORD-leftMarginSize : remainingLength;
        } else if (wordIndex == lastWordIndex) {
            iterator[6] = (remainingLength > BITS_PER_WORD) ? BITS_PER_WORD-rightMarginSize : remainingLength;
        } else {
            iterator[6] = (remainingLength > BITS_PER_WORD) ? BITS_PER_WORD : remainingLength;
        }
        iterator[5] -= iterator[6];
        return fillMargins(fill, getWord(wordIndex), wordIndex,
                firstWordIndex, lastWordIndex, leftMarginSize, rightMarginSize);
    }
    
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
    
    private void updateIteratorWord(int[] iterator, long word) {
        int wordIndex = iterator[0];
        if (wordIndex < 0) throw new IllegalStateException();
        setWord(wordIndex, word);
    }
    
    private static byte[] getWordBytes(long word) {
        final byte[] bytes = new byte[8];
        for (int i = 0; i < 8; i++) {
            bytes[7-i] = (byte)((word >>> 8*i) & 0xffL);
        }
        return bytes;
    }
    
    private static int[] unwrapWordToIntArray(long word) {
        final int perWord = Long.SIZE / Integer.SIZE;
        final int[] ints = new int[Long.SIZE / Integer.SIZE];
        for (int i = 0; i < ints.length; i++) {
            ints[1-i] = (int)((word >>> 32*i) & 0xffffffffL);
        }
        return ints;
    }
    
    private void getNextWords(int wordIndex, long[] words, int lastWordIndex) {
        for (int i = 0; i < words.length; i++) {
            if (wordIndex+i <= lastWordIndex) {
                words[i] = getWord(wordIndex+i);
            } else {
                words[i] = 0L;
            }
        }
    }
    
    private void getPreviousWords(int wordIndex, long[] words, int firstWordIndex) {
        for (int i = words.length-1; i >= 0; i--) {
            if (wordIndex-i >= firstWordIndex) {
                words[i] = getWord(wordIndex-i);
            } else {
                words[i] = 0L;
            }
        }
    }
    
//    private long reverseWord(long word) {
//        word = (word & 0xffffffff00000000L) >>> 32 | (word & 0x00000000ffffffffL) << 32;
//        word = (word & 0xffff0000ffff0000L) >>> 16 | (word & 0x0000ffff0000ffffL) << 16;
//        word = (word & 0xff00ff00ff00ff00L) >>> 8 | (word & 0x00ff00ff00ff00ffL) << 8;
//        word = (word & 0xf0f0f0f0f0f0f0f0L) >>> 4 | (word & 0x0f0f0f0f0f0f0f0fL) << 4;
//        word = (word & 0xccccccccccccccccL) >>> 2 | (word & 0x3333333333333333L) << 2;
//        word = (word & 0xaaaaaaaaaaaaaaaaL) >>> 1 | (word & 0x5555555555555555L) << 1;
//        return word;
//    }
    
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
        //assert (shift > 0 && shift < BITS_PER_WORD) : "shift arg ("+shift+") is out of range";
        return (lArg << (BITS_PER_WORD - shift)) | (rArg >>> shift);
    }
    
    private static void shiftArgsRight(int shift, long[] args) {
        final int nWordsShifted = shift >> ADDRESS_BITS_PER_WORD;
        if (nWordsShifted > 0) {
            for (int i = args.length-1; i >= 0; i--) {
                if (i-nWordsShifted >= 0) {
                    args[i] = args[i-nWordsShifted];
                } else {
                    args[i] = 0L;
                }
            }
        }
        final int nBitsShifted = shift & BIT_INDEX_MASK;
        if (nBitsShifted > 0) {
            for (int i = args.length-1; i >= nWordsShifted; i--) {
                if (i-1 >= 0) {
                    args[i] = (args[i] >>> nBitsShifted) | (args[i-1] << (BITS_PER_WORD - nBitsShifted));
                } else {
                    args[i] = (args[i] >>> nBitsShifted);
                }
            }
        }
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
        //assert (shift > 0 && shift < BITS_PER_WORD) : "shift arg ("+shift+") is out of range";
        return (lArg << shift) | (rArg >>> (BITS_PER_WORD - shift));
    }
    
    
    private static void shiftArgsLeft(int shift, long[] args) {
        final int nWordsShifted = shift >> ADDRESS_BITS_PER_WORD;
        if (nWordsShifted > 0) {
            for (int i = 0; i < args.length; i++) {
                if (i+nWordsShifted < args.length) {
                    args[i] = args[i+nWordsShifted];
                } else {
                    args[i] = 0L;
                }
            }
        }
        final int nBitsShifted = shift & BIT_INDEX_MASK;
        if (nBitsShifted > 0) {
            for (int i = 0; i < args.length-nWordsShifted; i++) {
                if (i+1 < args.length) {
                    args[i] = (args[i] << nBitsShifted) | (args[i+1] >>> (BITS_PER_WORD - nBitsShifted)); 
                } else {
                    args[i] = (args[i] << nBitsShifted);
                }
            }
        }
    }
    
    private long shiftWord(int shift, boolean fill, int wordIndex,
            int firstWordIndex, int lastWordIndex,
            int leftMarginSize, int rightMarginSize) {
        long word = getWord(wordIndex);
        if (shift < 0) word = shiftWordLeft(-shift, fill, wordIndex, lastWordIndex, rightMarginSize);
        if (shift > 0) word = shiftWordRight(shift, fill, wordIndex, firstWordIndex, leftMarginSize);
        return word;
    }
    
    private long shiftWordLeft(int shift, boolean fill, int wordIndex,
            int lastWordIndex, int rightMarginSize) {
        long word = getWord(wordIndex);
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
        long word = getWord(wordIndex);
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
    private static long fillMargins(boolean fill, long word, int wordIndex,
            int firstWordIndex, int lastWordIndex,
            int leftMarginSize, int rightMarginSize) {
        word = fillLeftMargin(fill, word, wordIndex, firstWordIndex, leftMarginSize);
        word = fillRightMargin(fill, word, wordIndex, lastWordIndex, rightMarginSize);
        return word;
    }
    
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
    
    private void restoreMargins(long originalFirstWord, long originalLastWord,
            int firstWordIndex, int lastWordIndex,
            int leftMarginSize, int rightMarginSize) {
        restoreLeftMargin(originalFirstWord, firstWordIndex, leftMarginSize);
        restoreRightMargin(originalLastWord, lastWordIndex, rightMarginSize);
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
    
//    private BitString temp(byte bits) {
//        assert Byte.SIZE <= BITS_PER_WORD;
//        temp.iSetLength(Byte.SIZE);
//        temp.setWord(0, ((long)bits) << (BITS_PER_WORD - Byte.SIZE));
//        return temp;
//    }
//    
//    private BitString temp(char bits) {
//        assert Character.SIZE <= BITS_PER_WORD;
//        temp.iSetLength(Character.SIZE);
//        temp.setWord(0, ((long)bits) << (BITS_PER_WORD - Character.SIZE));
//        return temp;
//    }
//    
//    private BitString temp(int bits) {
//        assert Integer.SIZE <= BITS_PER_WORD;
//        temp.iSetLength(Integer.SIZE);
//        temp.setWord(0, ((long)bits) << (BITS_PER_WORD - Integer.SIZE));
//        return temp;
//    }
//    
//    private BitString temp(long bits) {
//        assert Long.SIZE == BITS_PER_WORD;
//        temp.iSetLength(Long.SIZE);
//        temp.setWord(0, bits);
//        return temp;
//    }
//    
//    private BitString temp(short bits) {
//        assert Short.SIZE <= BITS_PER_WORD;
//        temp.iSetLength(Short.SIZE);
//        temp.setWord(0, ((long)bits) << (BITS_PER_WORD - Short.SIZE));
//        return temp;
//    }
    
    private static long[] wrapBooleans(boolean[] booleans) {
        return wrap(booleans.length, 1,
                (index) -> { return booleans[index] ? 1L : 0L; });
    }
    
    private static long[] wrapBytes(byte[] bytes) {
        return wrap(bytes.length, Byte.SIZE,
                (index) -> { return Byte.toUnsignedLong(bytes[index]); });
    }
    
    private static long[] wrapChars(char[] chars) {
        return wrap(chars.length, Character.SIZE,
                (index) -> { return (long)(chars[index]); });
    }
    
    private static long[] wrapDoubles(double[] doubles) {
        return wrap(doubles.length, Long.SIZE,
                (index) -> { return Double.doubleToRawLongBits(doubles[index]); });
    }
    
    private static long[] wrapFloats(float[] floats) {
        return wrap(floats.length, Integer.SIZE,
                (index) -> { return Integer.toUnsignedLong(Float.floatToRawIntBits(floats[index])); });
    }
    
    private static long[] wrapInts(int[] ints) {
        return wrap(ints.length, Integer.SIZE,
                (index) -> { return Integer.toUnsignedLong(ints[index]); });
    }
    
//    private static long wrapIntsIntoLong(int[] ints, int index) {
//      return wrapPrimitivesIntoLong(ints, index, ints.length, Integer.SIZE,
//              // primitives = ints
//              (primitives, i) -> { return Integer.toUnsignedLong(((int[])primitives)[i]); });
//  }
    
    private static long[] wrapShorts(short[] shorts) {
        return wrap(shorts.length, Short.SIZE,
                (index) -> { return Short.toUnsignedLong(shorts[index]); });
                //(primitives, i) -> { return Short.toUnsignedLong(((short[])primitives)[i]); });
    }
    
//    private static long wrapShortsIntoLong(short[] shorts, int index) {
//        return wrapPrimitivesIntoLong(shorts, index, shorts.length, Short.SIZE,
//                // primitives = shorts, i = index
//                (primitives, i) -> { return Short.toUnsignedLong(((short[])primitives)[i]); });
//    }
    
    private static long[] wrap(int primitiveCount, int primitiveSize,
            IntToLongFunction getPrimitiveAsUnsignedLong) {
        final int primitivesPerLong = Long.SIZE / primitiveSize;
        final long[] longs = new long[(primitiveCount + primitivesPerLong - 1) / primitivesPerLong];
        for (int i = 0, j = 0; i < primitiveCount; j = i++ / primitivesPerLong) {
            final int position = (primitivesPerLong - 1) - (i % primitivesPerLong);
            longs[j] |= getPrimitiveAsUnsignedLong.applyAsLong(i) << (position * primitiveSize);
        }
        return longs;
    }
    
    private boolean[] unwrapBooleans(int offset, int length) {
        assert isValidOffset(offset);
        assert isValidLength(offset, length);
        if (length == 0) return new boolean[0];
        final boolean[] booleans = new boolean[length];
        unwrap(offset, length, 1,
                (index, word) -> { booleans[index] = (word == 0) ? false : true; });
        return booleans;
    }
    
    private byte[] unwrapBytes(int offset, int length) {
        assert isValidOffset(offset);
        assert isValidLength(offset, length);
        if (length == 0) return new byte[0];
        final byte[] bytes = new byte[(length - 1) / Byte.SIZE + 1];
        unwrap(offset, length, Byte.SIZE,
                (index, word) -> { bytes[index] = (byte)word; });
        return bytes;
    }
    
    private char[] unwrapChars(int offset, int length) {
        assert isValidOffset(offset);
        assert isValidLength(offset, length);
        if (length == 0) return new char[0];
        final char[] chars = new char[(length - 1) / Character.SIZE + 1];
        unwrap(offset, length, Character.SIZE,
                (index, word) -> { chars[index] = (char)word; });
        return chars;
    }
    
    private double[] unwrapDoubles(int offset, int length) {
        assert isValidOffset(offset);
        assert isValidLength(offset, length);
        if (length == 0) return new double[0];
        final double[] doubles = new double[(length - 1) / Long.SIZE + 1];
        unwrap(offset, length, Long.SIZE,
                (index, word) -> { doubles[index] = Double.longBitsToDouble(word); });
        return doubles;
    }
    
    private float[] unwrapFloats(int offset, int length) {
        assert isValidOffset(offset);
        assert isValidLength(offset, length);
        if (length == 0) return new float[0];
        final float[] floats = new float[(length - 1) / Integer.SIZE + 1];
        unwrap(offset, length, Integer.SIZE,
                (index, word) -> { floats[index] = Float.intBitsToFloat((int)word); });
        return floats;
    }
    
    private int[] unwrapInts(int offset, int length) {
        assert isValidOffset(offset);
        assert isValidLength(offset, length);
        if (length == 0) return new int[0];
        final int[] ints = new int[(length - 1) / Integer.SIZE + 1];
        unwrap(offset, length, Integer.SIZE,
                (index, word) -> { ints[index] = (int)word; });
        return ints;
    }
    
    private long[] unwrapLongs(int offset, int length) {
        assert isValidOffset(offset);
        assert isValidLength(offset, length);
        if (length == 0) return new long[0];
        final long[] longs = new long[(length - 1) / Long.SIZE + 1];
        unwrap(offset, length, Long.SIZE,
                (index, word) -> { longs[index] = word; });
        return longs;
    }
    
    private short[] unwrapShorts(int offset, int length) {
        assert isValidOffset(offset);
        assert isValidLength(offset, length);
        if (length == 0) return new short[0];
        final short[] shorts = new short[(length - 1) / Short.SIZE + 1];
        unwrap(offset, length, Short.SIZE,
                (index, word) -> { shorts[index] = (short)word; });
        return shorts;
    }
    
    private void unwrap(int offset, int length,
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
    
//    private static long[] wrap2(Object primitives, int primitiveCount, int primitiveSize,
//            BiFunction<Object, Integer, Long> getPrimitiveAsUnsignedLong) {
//        assert primitives.getClass().isArray() && primitives.getClass().getComponentType().isPrimitive();
//        final int primitivesPerLong = Long.SIZE / primitiveSize;
//        final long[] longs = new long[(primitiveCount + primitivesPerLong - 1) / primitivesPerLong];
//        for (int i = 0, j = 0; i < primitiveCount; i += primitivesPerLong, j++) {
//            longs[j] = wrapPrimitivesIntoLong(primitives, i, primitiveCount, primitiveSize, getPrimitiveAsUnsignedLong);
//        }
//        return longs;
//    }
    
//    private static long wrapPrimitivesIntoLong(Object primitives, int index, int primitiveCount, int primitiveSize,
//            BiFunction<Object, Integer, Long> getPrimitiveAsUnsignedLong) {
//        long word = 0L;
//        final int primitivesPerLong = Long.SIZE / primitiveSize;
//        for (int i = index; i < primitivesPerLong && i < primitiveCount; i++) {
//            final int position = (primitivesPerLong - 1) - (i % primitivesPerLong);
//            word |= getPrimitiveAsUnsignedLong.apply(primitives, i) << (position * primitiveSize);
//        }
//        return word;
//    }
    
//    private long[] wrapInts(int[] ints) {
//        if (ints.length * (long) Integer.SIZE > Integer.MAX_VALUE) {
//            throw new IllegalArgumentException("the number of bits in the array (" + ints.length * (long) Integer.SIZE
//                    + ") exceed the maximum of " + Integer.MAX_VALUE);
//        }
//        final long[] longs = new long[(ints.length + 1) / 2];
//        for (int i = 0, l = 0; i < ints.length; i++) {
//            if ((i % 2) == 0) {
//                longs[l] = ((long)ints[i]) << Integer.SIZE; // first half of long
//            } else {
//                longs[l] |= Integer.toUnsignedLong(ints[i]); // last half of long
//                l++;
//            }
//        }
//        return longs;
//    }
    
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
        //return offset == 0 || offset > 0 && offset < this.length();
    }
    
    private void checkThisOffset(int offset) {
        if (!isValidOffset(offset)) {
            throw new StringIndexOutOfBoundsException(
                    "specified offset is invalid for this BitString; offset=" + offset + ", length=" + length());
        }
    }
    
    private void checkArgOffset(int offset) {
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
    
    private void checkArgPosition(int position) {
        if (!isValidPosition(position)) {
            throw new StringIndexOutOfBoundsException(
                    "The specified BitString's position is invalid; position=" + position + ", length=" + length());
        }
    }
    
    private boolean isValidLength(int offset, int length) {
        return length >= 0 && length <= (this.length() - offset);
    }
    
    private void checkThisLength(int offset, int length) {
        if (!isValidLength(offset, length))
            throw new IllegalArgumentException("specified length is negative or exceeds offset+length of this BitString; length="
                    + length + " specified BitString's offset=" + offset + " length=" + this.length());
    }
    
    private void checkArgLength(int offset, int length) {
        if (!isValidLength(offset, length))
            throw new IllegalArgumentException(
                    "specified length exceeds offset+length of the specified BitString; length=" + length
                            + " specified BitString's offset=" + offset + " length=" + this.length());
    }
    
    private static void checknBits(int nBits) {
        if (nBits < 0) {
            throw new IllegalArgumentException("nBits < 0: " + nBits);
        }
    }
    
    private static void checkDistance(int distance) {
        if (distance == Integer.MIN_VALUE) {
            throw new IllegalArgumentException("distance = " + distance);
        }
    }
    
    private static void checkNewBitStringLength(long length) {
        if (length > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "the size of the new BitString (" + length + ")  would exceed the maximum of " + Integer.MAX_VALUE);
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
    
    private void checkBaseLengthIncrease(long length) {
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
        ensureCapacity(this.length() - thisLength + thatLength);
        final int newLength = this.length() - thisLength + thatLength;
        final int shift = thatLength - thisLength;
        if (newLength > length()) setLength(newLength);
        iShiftRight(shift, ZERO_FILL, thisBitIndex, this.length() - thisBitIndex);
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
    
    private void iClear(int offset, int length) {
        iBitwiseOp(offset, length, ZEROS, offset,
                (lArg, rArg) -> { return rArg; });
    }
    
    private void iCopy(int thisOffset, int length, BitString that, int thatOffset) {
        iBitwiseOp(thisOffset, length, that, thatOffset,
                (lArg, rArg) -> { return rArg; });
    }
    
    private void iCopyNot(int thisOffset, int length, BitString that, int thatOffset) {
        iBitwiseOp(thisOffset, length, that, thatOffset,
                (lArg, rArg) -> { return ~rArg; });
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
        //iBitwiseOp(offset, length, ONES, offset,
        //        (lArg, rArg) -> { return lArg ^ rArg; });
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
                // is contained wholly within the first word.
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
            
        }
        
        restoreMargins(thisOriginalFirstWord, thisOriginalLastWord,
                thisFirstWordIndex, thisLastWordIndex,
                thisLeftMarginSize, thisRightMarginSize);
        
    }
    
    private boolean iEquals(int thisOffset, int length, BitString that, int thatOffset) {
        return iPredicate( (lArg, rArg) -> { return lArg == rArg; }, ONE_DFLT, ONE_FILL,
                thisOffset, length, that, thatOffset);
//        return iPredicate( (lArg, rArg) -> { return (lArg & rArg) == lArg; }, ONE_DFLT, ONE_FILL,
//                thisOffset, length, that, thatOffset);
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
    
    private void iCopyFromFrontOf(int thisOffset, int thisLength, BitString that, int thatOffset, int thatLength) {
        assert this.isValidOffset(thisOffset);
        assert this.isValidLength(thisOffset, thisLength);
        assert that.isValidOffset(thatOffset);
        assert that.isValidLength(thatOffset, thatLength);
        if (thisLength == 0) return;
        
//        final int thisFirstWordIndex = this.firstWordIndex(thisOffset);
//        final int thisLastWordIndex = this.lastWordIndex(thisOffset, thisLength);
//        final int thisLeftMarginSize = this.leftMarginSize(thisOffset);
//        final int thisRightMarginSize = this.rightMarginSize(thisOffset, thisLength);
      
//        final long thisOriginalLastWord = this.getWord(thisLastWordIndex);
        
        final int copyLength = Math.min(thisLength, thatLength);
        final int clearLength = thisLength - copyLength;
        
        iCopy(thisOffset, copyLength, that, thatOffset);
        iClear(thisOffset + copyLength, clearLength);
        
//        // copy bits from the front of that BitString into this BitString.
//        // insert the copied bits from the the front to the back of this BitString.
//        // if that  BitString is shorter than this BitString, pad this BitString with zeros.
//        long thatWord;
//        final int[] thatIterator = that.getIterator(thatOffset, thatLength);
//        for (int thisWordIndex = thisFirstWordIndex; thisWordIndex <= thisLastWordIndex; thisWordIndex++) {
//            if (that.hasNextIteratorWord(thatIterator)) {
//                thatWord = that.getNextIteratorFullWord(thatIterator);
//            } else {
//                thatWord = 0L;
//            }
//            if (thisLeftMarginSize > 0) {
//                this.setWord(thisWordIndex,
//                        (thatWord >>> thisLeftMarginSize)
//                      | (this.getWord(thisWordIndex) & (WORD_MASK << BITS_PER_WORD - thisLeftMarginSize)));
//                if (thisWordIndex+1 <= thisLastWordIndex) {
//                    this.setWord(thisWordIndex+1, (thatWord << BITS_PER_WORD - thisLeftMarginSize)); 
//                }
//            } else {
//                this.setWord(thisWordIndex, thatWord);
//            }
//        }
//        
//        this.restoreRightMargin(thisOriginalLastWord, thisLastWordIndex, thisRightMarginSize);
    }
    
    private void iCopyFromBackOf(int thisOffset, int thisLength, BitString that, int thatOffset, int thatLength) {
        assert this.isValidOffset(thisOffset);
        assert this.isValidLength(thisOffset, thisLength);
        assert that.isValidOffset(thatOffset);
        assert that.isValidLength(thatOffset, thatLength);
        if (thisLength == 0) return;
        
//        final int thisFirstWordIndex = this.firstWordIndex(thisOffset);
//        final int thisLastWordIndex = this.lastWordIndex(thisOffset, thisLength);
//        final int thisLeftMarginSize = this.leftMarginSize(thisOffset);
//        final int thisRightMarginSize = this.rightMarginSize(thisOffset, thisLength);
//      
//        final long thisOriginalFirstWord = this.getWord(thisFirstWordIndex);
        
        final int copyLength = Math.min(thisLength, thatLength);
        final int clearLength = thisLength - copyLength;
        
        iCopy(thisOffset + clearLength, copyLength, that, thatOffset);
        iClear(thisOffset, clearLength);
        
//        // copy bits from the back of that BitString into this BitString.
//        // insert the copied bits from the the back to the front of this BitString.
//        // if that  BitString is shorter than this BitString, pad this BitString with zeros.
//        long thatWord;
//        final int[] thatIterator = that.getIterator(thatOffset, thatLength);
//        for (int thisWordIndex = thisLastWordIndex; thisWordIndex >= thisFirstWordIndex; thisWordIndex--) {
//            if (that.hasPreviousIteratorWord(thatIterator)) {
//                thatWord = that.getPreviousIteratorFullWord(thatIterator);
//            } else {
//                thatWord = 0L;
//            }
//            if (thisRightMarginSize > 0) {
//                this.setWord(thisWordIndex,
//                        (thatWord << thisRightMarginSize)
//                      | (this.getWord(thisWordIndex) & (WORD_MASK >>> BITS_PER_WORD - thisRightMarginSize)));
//                if (thisWordIndex-1 >= thisFirstWordIndex) {
//                    this.setWord(thisWordIndex-1, (thatWord >>> BITS_PER_WORD - thisRightMarginSize)); 
//                } 
//            } else {
//                this.setWord(thisWordIndex, thatWord);
//            }
//        }
//        
//        this.restoreLeftMargin(thisOriginalFirstWord, thisFirstWordIndex, thisLeftMarginSize);
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
        final BitString spill = new BitString(shift);
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
        final BitString spill = new BitString(shift);
        
        int thatShift = shift > thatLength ? thatLength : shift;
        int thisShift = shift - thatShift;
        that.iShiftLeft(thatShift, ZERO_FILL, thatOffset, thatShift, spill, 0, shift);
        if (thisShift > 0) {
            this.iShiftLeft(thisShift, ZERO_FILL, thisOffset, thisShift, spill, 0, shift);
        }
        
        this.iShiftLeft(shift, ZERO_FILL, thisOffset, thisLength, that, thatOffset, thatLength);
        
        thisShift = shift > thisLength ? thisLength : shift;
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
        final BitString spill = new BitString(shift);
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
        final BitString spill = new BitString(shift);
        
        int thatShift = shift > thatLength ? thatLength : shift;
        int thisShift = shift - thatShift;
        that.iShiftRight(thatShift, ZERO_FILL, thatOffset+thatLength-thatShift, thatShift, spill, 0, shift);
        if (thisShift > 0) {
            this.iShiftRight(thisShift, ZERO_FILL, thisOffset+thisLength-thisShift, thisShift, spill, 0, shift);
        }
        
        this.iShiftRight(shift, ZERO_FILL, thisOffset, thisLength, that, thatOffset, thatLength);
        
        thisShift = shift > thisLength ? thisLength : shift;
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
        //if (nBits < 0) iShiftRight(-nBits, fill, offset, length);
        if (nBits < 0) {
            if (nBits == Integer.MIN_VALUE) {
                iShiftRight(1, fill, offset, length);
                iShiftRight(Integer.MAX_VALUE, fill, offset, length);
            } else {
                iShiftRight(-nBits, fill, offset, length); 
            }
            return;
        }
        
        final int firstWordIndex = firstWordIndex(offset);
        final int lastWordIndex = lastWordIndex(offset, length);
        final int leftMarginSize = leftMarginSize(offset);
        final int rightMarginSize = rightMarginSize(offset, length);
        
        final long originalFirstWord = getWord(firstWordIndex);
        final long originalLastWord = getWord(lastWordIndex);
        
        final long copyfromFirstBitIndex = firstBitIndex(offset) + nBits;
        final int copyfromFirstWordIndex = wordIndex(copyfromFirstBitIndex);
        final int copyfromFirstWordBitIndex = wordBitIndex(copyfromFirstBitIndex);
        
        final long copytoLastBitIndex = lastBitIndex(offset, length) - nBits;
        final int copytoLastWordIndex = wordIndex(copytoLastBitIndex);
        
        int newLeftMarginSize = leftMarginSize;
        
        final int nFullWords = copyfromFirstWordIndex - firstWordIndex;
        if (nFullWords > 0) {
            newLeftMarginSize = leftMarginSizeOfWordBitIndex(copyfromFirstWordBitIndex);
            for (int wordIndex = firstWordIndex; wordIndex <= lastWordIndex; wordIndex++) {
                final int copyFromIndex = wordIndex + nFullWords;
                if (copyFromIndex <= lastWordIndex) {
                    setWord(wordIndex, getWord(copyFromIndex));
                } else if (fill) {
                    setWord(wordIndex, WORD_MASK);
                } else {
                    setWord(wordIndex, 0L);
                }
            }
        }
        
        int shift = firstWordBitIndex(offset) - copyfromFirstWordBitIndex;
        if (shift > 0) {
            for (int wordIndex = copytoLastWordIndex; wordIndex >= firstWordIndex; wordIndex--) {
                setWord(wordIndex, shiftWordRight(shift, fill, wordIndex, firstWordIndex, newLeftMarginSize));
            }
        }
        if (shift < 0) {
            for (int wordIndex = firstWordIndex; wordIndex <= copytoLastWordIndex; wordIndex++) {
                setWord(wordIndex, shiftWordLeft(-shift, fill, wordIndex, copytoLastWordIndex, rightMarginSize));
            }
        }
        
        restoreMargins(originalFirstWord, originalLastWord,
                firstWordIndex, lastWordIndex,
                leftMarginSize, rightMarginSize);
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
            iShiftLeft(nBits, fill, thisOffset, thisLength);
            return;
        }
        //if (nBits < 0) iShiftRight(-nBits, fill, thisOffset, thisLength, that, thatOffset, thatLength);
        if (nBits < 0) {
            if (nBits == Integer.MIN_VALUE) {
                iShiftRight(1, fill, thisOffset, thisLength, that, thatOffset, thatLength);
                iShiftRight(Integer.MAX_VALUE, fill, thisOffset, thisLength, that, thatOffset, thatLength);
            } else {
                iShiftRight(-nBits, fill, thisOffset, thisLength, that, thatOffset, thatLength); 
            }
            return;
        }
        
        final int thatFirstWordIndex = that.firstWordIndex(thatOffset);
        final int thatLastWordIndex = that.lastWordIndex(thatOffset, thatLength);
        final int thatLeftMarginSize = that.leftMarginSize(thatOffset);
        final int thatRightMarginSize = that.rightMarginSize(thatOffset, thatLength);
        
        final long thatOriginalFirstWord = that.getWord(thatFirstWordIndex);
        final long thatOriginalLastWord = that.getWord(thatLastWordIndex);
        
        final long thatNewLastBitIndex = lastBitIndex(thatOffset, thatLength) - nBits;
        final int thatNewLastWordIndex = wordIndex(thatNewLastBitIndex);
        final int thatNewLastWordBitIndex = wordBitIndex(thatNewLastBitIndex);
        
        that.iShiftLeft(nBits, fill, thatOffset, thatLength);
        
        // copy bits from the front of this BitString into that BitString
        // insert the copied bits after the original last bit, of that BitString,
        // which has been shifted left nBits
        int thatWordIndex = thatNewLastWordIndex;
        final int[] iterator = this.getIterator(thisOffset, thisLength);
        while (this.hasNextIteratorWord(iterator)) {
            if (thatWordIndex > thatLastWordIndex) break;
            final long thisWord = this.getNextIteratorFullWord(iterator, fill);
            if (thatWordIndex >= thatFirstWordIndex && thatNewLastWordBitIndex < BITS_PER_WORD-1) {
                that.setWord(thatWordIndex,
                        (thisWord >>> (thatNewLastWordBitIndex+1))
                      | (that.getWord(thatWordIndex) & (WORD_MASK << BITS_PER_WORD - thatNewLastWordBitIndex - 1)));
            }
            if (thatWordIndex+1 >= thatFirstWordIndex && thatWordIndex+1 <= thatLastWordIndex) {
                that.setWord(thatWordIndex+1, thisWord << BITS_PER_WORD - thatNewLastWordBitIndex - 1); 
            }
            thatWordIndex++;
        }
        
        that.restoreMargins(thatOriginalFirstWord, thatOriginalLastWord,
                thatFirstWordIndex, thatLastWordIndex,
                thatLeftMarginSize, thatRightMarginSize);
        
        this.iShiftLeft(nBits, fill, thisOffset, thisLength);
    }
    
    private void iShiftRight(int nBits, boolean fill, int offset, int length) {
        assert nBits != Integer.MIN_VALUE;
        assert isValidOffset(offset);
        assert isValidLength(offset, length);
        if (nBits == 0 || length == 0) return;
        //if (nBits < 0) iShiftLeft(-nBits, fill, offset, length);
        if (nBits < 0) {
            if (nBits == Integer.MIN_VALUE) {
                iShiftLeft(1, fill, offset, length);
                iShiftLeft(Integer.MAX_VALUE, fill, offset, length);
            } else {
                iShiftLeft(-nBits, fill, offset, length); 
            }
            return;
        }
        
        final int firstWordIndex = firstWordIndex(offset);
        final int lastWordIndex = lastWordIndex(offset, length);
        final int leftMarginSize = leftMarginSize(offset);
        final int rightMarginSize = rightMarginSize(offset, length);
        
        final long originalFirstWord = getWord(firstWordIndex);
        final long originalLastWord = getWord(lastWordIndex);
        
        final long copyfromLastBitIndex = lastBitIndex(offset, length) - nBits;
        final int copyfromLastWordIndex = wordIndex(copyfromLastBitIndex);
        final int copyfromLastWordBitIndex = wordBitIndex(copyfromLastBitIndex);
        
        final long copytoFirstBitIndex = firstBitIndex(offset) + nBits;
        final int copytoFirstWordIndex = wordIndex(copytoFirstBitIndex);
        
        int newRightMarginSize = rightMarginSize;
        
        final int nFullWords = lastWordIndex - copyfromLastWordIndex;
        if (nFullWords > 0) {
            newRightMarginSize = rightMarginSizeOfWordBitIndex(copyfromLastWordBitIndex);
            for (int wordIndex = lastWordIndex; wordIndex >= firstWordIndex; wordIndex--) {
                final int copyFromIndex = wordIndex - nFullWords;
                if (copyFromIndex >= firstWordIndex) {
                    setWord(wordIndex, getWord(copyFromIndex));
                } else if (fill) {
                    setWord(wordIndex, WORD_MASK);
                } else {
                    setWord(wordIndex, 0L);
                }
            }
        }
        
        int shift = lastWordBitIndex(offset, length) - copyfromLastWordBitIndex;
        if (shift > 0) {
            for (int wordIndex = lastWordIndex; wordIndex >= copytoFirstWordIndex; wordIndex--) {
                setWord(wordIndex, shiftWordRight(shift, fill, wordIndex, copytoFirstWordIndex, leftMarginSize));
            }
        }
        if (shift < 0) {
            for (int wordIndex = copytoFirstWordIndex; wordIndex <= lastWordIndex; wordIndex++) {
                setWord(wordIndex, shiftWordLeft(-shift, fill, wordIndex, lastWordIndex, newRightMarginSize));
            }
        }
        
        restoreMargins(originalFirstWord, originalLastWord,
                firstWordIndex, lastWordIndex,
                leftMarginSize, rightMarginSize);
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
            iShiftLeft(nBits, fill, thisOffset, thisLength);
            return;
        }
        //if (nBits < 0) iShiftLeft(-nBits, fill, thisOffset, thisLength, that, thatOffset, thatLength);
        if (nBits < 0) {
            if (nBits == Integer.MIN_VALUE) {
                iShiftLeft(1, fill, thisOffset, thisLength, that, thatOffset, thatLength);
                iShiftLeft(Integer.MAX_VALUE, fill, thisOffset, thisLength, that, thatOffset, thatLength);
            } else {
                iShiftLeft(-nBits, fill, thisOffset, thisLength, that, thatOffset, thatLength); 
            }
            return;
        }
        
        final int thatFirstWordIndex = that.firstWordIndex(thatOffset);
        final int thatLastWordIndex = that.lastWordIndex(thatOffset, thatLength);
        final int thatLeftMarginSize = that.leftMarginSize(thatOffset);
        final int thatRightMarginSize = that.rightMarginSize(thatOffset, thatLength);
        
        final long thatOriginalFirstWord = that.getWord(thatFirstWordIndex);
        final long thatOriginalLastWord = that.getWord(thatLastWordIndex);
        
        final long thatNewFirstBitIndex = that.firstBitIndex(thatOffset) + nBits;
        final int thatNewFirstWordIndex = wordIndex(thatNewFirstBitIndex);
        final int thatNewFirstWordBitIndex = wordBitIndex(thatNewFirstBitIndex);
        
        that.iShiftRight(nBits, fill, thatOffset, thatLength);
        
        // copy bits from the end of this BitString into that BitString
        // insert the copied bits before the original first bit, of that BitString,
        // which has been shifted right nBits
        int thatWordIndex = thatNewFirstWordIndex;
        final int[] iterator = this.getIterator(thisOffset, thisLength);
        while (this.hasPreviousIteratorWord(iterator)) {
            if (thatWordIndex < thatFirstWordIndex) break;
            final long thisWord = this.getPreviousIteratorFullWord(iterator, fill);
            if (thatWordIndex <= thatLastWordIndex && thatNewFirstWordBitIndex > 0) {
                that.setWord(thatWordIndex,
                        (thisWord << BITS_PER_WORD - thatNewFirstWordBitIndex)
                      | (that.getWord(thatWordIndex) & (WORD_MASK >>> thatNewFirstWordBitIndex)));
            }
            if (thatWordIndex-1 <= thatLastWordIndex && thatWordIndex-1 >= thatFirstWordIndex) {
                that.setWord(thatWordIndex-1, (thisWord >>> thatNewFirstWordBitIndex)); 
            }
            thatWordIndex--;
        }
        
        that.restoreMargins(thatOriginalFirstWord, thatOriginalLastWord,
                thatFirstWordIndex, thatLastWordIndex,
                thatLeftMarginSize, thatRightMarginSize);
        
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
    
//    private void iPutPrimitive(int offset, int size, BitString primitiveBits) {
//        assert isValidOffset(offset);
//        assert size <= BITS_PER_WORD;
//        iCopy(offset, size, primitiveBits, 0);
//    }
    
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
    
    public BitString append(BitString that, int thatOffset) {
        that.checkArgOffset(thatOffset);
        final int thatLength = that.length() - thatOffset;
        checkBaseLengthIncrease(thatLength);
        iAppend(that, thatOffset, thatLength);
        return this;
    }
    
    public BitString append(BitString that, int thatOffset, int thatLength) {
        that.checkArgOffset(thatOffset);
        that.checkArgLength(thatOffset, thatLength);
        checkBaseLengthIncrease(thatLength);
        iAppend(that, thatOffset, thatLength);
        return this;
    }
    
//    public BitString appendBoolean(boolean primitive) {
//        checkBaseLength(1);
//        iAppend(temp(primitive), 0, 1);
//        return this;
//    }
    
//    public BitString appendByte(byte primitive) {
//        checkBaseLengthIncrease(Byte.SIZE);
//        iAppend(temp(primitive), 0, Byte.SIZE);
//        return this;
//    }
//    
//    public BitString appendChar(byte primitive) {
//        checkBaseLengthIncrease(Character.SIZE);
//        iAppend(temp(primitive), 0, Character.SIZE);
//        return this;
//    }
//    
//    public BitString appendDouble(double primitive) {
//        checkBaseLengthIncrease(Long.SIZE);
//        iAppend(temp(Double.doubleToRawLongBits(primitive)), 0, Long.SIZE);
//        return this;
//    }
//    
//    public BitString appendFloat(float primitive) {
//        checkBaseLengthIncrease(Integer.SIZE);
//        iAppend(temp(Float.floatToRawIntBits(primitive)), 0, Integer.SIZE);
//        return this;
//    }
//    
//    public BitString appendInt(int primitive) {
//        checkBaseLengthIncrease(Integer.SIZE);
//        iAppend(temp(primitive), 0, Integer.SIZE);
//        return this;
//    }
//    
//    public BitString appendInts(int[] primitives) {
//        checkBaseLengthIncrease(primitives.length * Integer.SIZE);
//        final int primitivesPerLong = Long.SIZE / Integer.SIZE;
//        for (int i = 0; i < primitives.length; i += primitivesPerLong) {
//            final long word = wrapIntsIntoLong(primitives, i);
//            iAppend(temp(word), 0, Long.SIZE);
//        }
//        return this;
//    }
//    
//    public BitString appendLong(long primitive) {
//        checkBaseLengthIncrease(Long.SIZE);
//        iAppend(temp(primitive), 0, Long.SIZE);
//        return this;
//    }
//    
//    public BitString appendShort(short primitive) {
//        checkBaseLengthIncrease(Short.SIZE);
//        iAppend(temp(primitive), 0, Short.SIZE);
//        return this;
//    }
    
    public BitString delete() {
        final int length = length();
        iDelete(firstBitIndex(0), length);
        return this;
    }
    
    public BitString delete(int offset) {
        checkThisOffset(offset);
        final int length = length() - offset;
        iDelete(firstBitIndex(offset), length);
        return this;
    }
    
    public BitString delete(int offset, int length) {
        checkThisOffset(offset);
        checkThisLength(offset, length);
        iDelete(firstBitIndex(offset), length);
        return this;
    }
    
    public BitString insert(int position, BitString that) {
        checkThisPosition(position);
        final int thatLength = that.length();
        checkBaseLengthIncrease(thatLength);
        iInsert(bitIndex(position), that, 0, thatLength);
        return this;
    }
    
    public BitString insert(int position, BitString that, int thatOffset) {
        checkThisPosition(position);
        that.checkArgOffset(thatOffset);
        final int thatLength = that.length() - thatOffset;
        checkBaseLengthIncrease(thatLength);
        iInsert(bitIndex(position), that, thatOffset, thatLength);
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
    
//    public BitString insertByte(int position, byte primitive) {
//        checkThisPosition(position);
//        checkBaseLengthIncrease(Byte.SIZE);
//        iInsert(bitIndex(position), temp(primitive), 0, Byte.SIZE);
//        return this;
//    }
//    
//    public BitString insertChar(int position, byte primitive) {
//        checkThisPosition(position);
//        checkBaseLengthIncrease(Character.SIZE);
//        iInsert(bitIndex(position), temp(primitive), 0, Character.SIZE);
//        return this;
//    }
//    
//    public BitString insertDouble(int position, double primitive) {
//        checkThisPosition(position);
//        checkBaseLengthIncrease(Long.SIZE);
//        iInsert(bitIndex(position), temp(Double.doubleToRawLongBits(primitive)), 0, Long.SIZE);
//        return this;
//    }
//    
//    public BitString insertFloat(int position, float primitive) {
//        checkThisPosition(position);
//        checkBaseLengthIncrease(Integer.SIZE);
//        iInsert(bitIndex(position), temp(Float.floatToRawIntBits(primitive)), 0, Integer.SIZE);
//        return this;
//    }
//    
//    public BitString insertInt(int position, int primitive) {
//        checkThisPosition(position);
//        checkBaseLengthIncrease(Integer.SIZE);
//        iInsert(bitIndex(position), temp(primitive), 0, Integer.SIZE);
//        return this;
//    }
//    
//    public BitString insertLong(int position, long primitive) {
//        checkThisPosition(position);
//        checkBaseLengthIncrease(Long.SIZE);
//        iInsert(bitIndex(position), temp(primitive), 0, Long.SIZE);
//        return this;
//    }
//    
//    public BitString insertShort(int position, short primitive) {
//        checkThisPosition(position);
//        checkBaseLengthIncrease(Short.SIZE);
//        iInsert(bitIndex(position), temp(primitive), 0, Short.SIZE);
//        return this;
//    }
    
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
        return clear(field.offset(), field.length());
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
    
    public BitString clearBit(int bitOffset, int offset) {
        checkThisOffset(offset);
        checkRelativeOffset(bitOffset, length() - offset);
        return clearBit(offset + bitOffset);
    }
    
    public BitString clearBit(int bitOffset, int offset, int length) {
        checkThisOffset(offset);
        checkThisLength(offset, length);
        checkRelativeOffset(bitOffset, length);
        return clearBit(offset + bitOffset);
    }
    
    public BitString clearBit(int bitOffset, Field field) {
        return clearBit(bitOffset, field.offset(), field.length());
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
     * This substring starts at offset 'offset' of this {@code BitString} and
     * extends to the end of this {@code BitString}.
     *
     * @param offset the start of this substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     */
    public BitString flip(int offset) {
        checkThisOffset(offset);
        iNot(offset, length() - offset);
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
        return flip(field.offset(), field.length());
    }
    
    /**
     * Sets the bit at the specified offset to the complement of its current value.
     * 
     * @param offset the offset of the bit to flip
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     */
    public BitString flipBit(int offset) {
        checkThisOffset(offset);
        int wordIndex = wordIndex(bitIndex(offset));
        setWord(wordIndex, getWord(wordIndex) ^ (BIT_MASK >>> wordBitIndex(bitIndex(offset))));
        return this;
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
        final BitString substring = new BitString(length);
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
        return get(field.offset(), field.length());
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
    
    public boolean getBit(int bitOffset, int offset) {
        checkThisOffset(offset);
        final int length = length() - offset;
        checkRelativeOffset(bitOffset, length);
        return getBit(offset + bitOffset);
    }
    
    public boolean getBit(int bitOffset, int offset, int length) {
        checkThisOffset(offset);
        checkThisLength(offset, length);
        checkRelativeOffset(bitOffset, length);
        return getBit(offset + bitOffset);
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
        return set(field.offset(), field.length());
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
        return set(bit, field.offset(), field.length());
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
        int wordIndex = wordIndex(bitIndex);
        setWord(wordIndex, getWord(wordIndex) | (BIT_MASK >>> wordBitIndex(bitIndex)));
        return this;
    }
    
    public BitString setBit(int bitOffset, int offset) {
        checkThisOffset(offset);
        checkRelativeOffset(bitOffset, length() - offset);
        return setBit(offset + bitOffset);
    }
    
    public BitString setBit(int bitOffset, int offset, int length) {
        checkThisOffset(offset);
        checkThisLength(offset, length);
        checkRelativeOffset(bitOffset, length);
        return setBit(offset + bitOffset);
    }
    
    public BitString setBit(int bitOffset, Field field) {
        return setBit(bitOffset, field.offset(), field.length());
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
    
    public BitString setBit(boolean bit, int bitOffset, int offset) {
        return bit ? setBit(bitOffset, offset) : clearBit(bitOffset, offset);
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
     * The bits in This {@code BitString} are modified according to the following
     * logic table. But basically, for each {@code ZERO} bit in the argument, the
     * corresponding bit in this {@code BitString} is set to {@code ZERO}, otherwise,
     * the bit is left unchanged.
     * This {@code BitString} is modified so that each bit in it has the value
     * {@code ONE} if and only if it both initially had the value {@code ONE} and
     * the corresponding bit in the specified bit string also had the value
     * {@code ONE}. In other words, the value of each bit in this {@code BitString},
     * whose corresponding bit in the argument has the value {@code ZERO}, is set to
     * {@code ZERO}, otherwise, it is left unchanged.
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
     * Performs a logical <b>AND</b> of this {@code BitString} with a substring of
     * the specified bit string (arg).
     * 
     * This {@code BitString} is modified by this operation (see
     * {@link #and(BitString)}).
     * 
     * The substring argument starts at offset 'argOffset' of the specified bit
     * string and extends to the end of the specified bit string.
     * 
     * The length of the operation is equal to the smaller of the length of this
     * {@code BitString} or the length of the substring argument.
     * 
     * @param arg       bit string argument
     * @param argOffset the start of the argument substring
     * @return this {@code BitString} with the results of the operation
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code argOffset < 0 || argOffset > 0 && argOffset >= arg.length()}
     */
    public BitString and(BitString arg, int argOffset) {
        arg.checkArgOffset(argOffset);
        iAnd(0, Math.min(this.length(), arg.length() - argOffset), arg, argOffset);
        return this;
    }
    
    /**
     * Performs a logical <b>AND</b> of this {@code BitString} with a substring of
     * the specified bit string (arg).
     * 
     * This {@code BitString} is modified by this operation (see
     * {@link #and(BitString)}).
     * 
     * The substring argument starts at offset 'argOffset' of the specified bit
     * string and has a length of 'argLength'.
     * 
     * The length of the operation is equal to the smaller of the length of this
     * {@code BitString} or the length of the substring argument.
     * 
     * @param arg       bit string argument
     * @param argOffset the start of the argument substring
     * @param argLength the length of the argument substring
     * @return this {@code BitString} with the results of the operation
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code argOffset < 0 || argOffset > 0 && argOffset >= arg.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code argLength < 0 || argLength > arg.length() - argOffset}
     */
    public BitString and(BitString arg, int argOffset, int argLength) {
        arg.checkArgOffset(argOffset);
        arg.checkArgLength(argOffset, argLength);
        iAnd(0, Math.min(this.length(), argLength), arg, argOffset);
        return this;
    }
    
    /**
     * Performs a logical <b>AND</b> of this {@code BitString} with a Field of the
     * specified bit string (arg).
     * 
     * This {@code BitString} is modified by this operation (see
     * {@link #and(BitString)}).
     * 
     * The length of the operation is equal to the smaller of the length of this
     * {@code BitString} or the length of the specified Field.
     * 
     * @param arg      bit string argument
     * @param argField Field of the bit string argument
     * @return this {@code BitString} with the results of the operation
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code argField.offset() > 0 && argField.offset() >= arg.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code argField.length() > arg.length() - argField.offset()}
     */
    public BitString and(BitString arg, Field argField) {
        return and(arg, argField.offset(), argField.length());
    } 
    
    /**
     * Performs a logical <b>AND</b> of a substring of this {@code BitString} with
     * the specified bit string (arg).
     * 
     * This {@code BitString} is modified by this operation (see
     * {@link #and(BitString)}).
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * extends to the end of this {@code BitString}.
     * 
     * The length of the operation is equal to the smaller of the length of this
     * substring or the length of the specified bit string.
     * 
     * @param thisOffset the start of this substring
     * @param arg        bit string argument
     * @return this {@code BitString} with the results of the operation
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     */
    public BitString and(int thisOffset, BitString arg) {
        checkThisOffset(thisOffset);
        iAnd(thisOffset, Math.min(this.length() - thisOffset, arg.length()), arg, 0);
        return this;
    }
    
    /**
     * Performs a logical <b>AND</b> of a substring of this {@code BitString} with a
     * substring of the specified bit string (arg).
     * 
     * This {@code BitString} is modified by this operation (see
     * {@link #and(BitString)}).
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * extends to the end of this {@code BitString}.
     * 
     * The substring argument starts at offset 'argOffset' of the specified bit
     * string and and extends to the end of the specified bit string.
     * 
     * The length of the operation is equal to the smaller of the length of this
     * substring or the length of the substring argument.
     * 
     * @param thisOffset the start of this substring
     * @param arg        bit string argument
     * @param argOffset  the start of the argument substring
     * @return this {@code BitString} with the results of the operation
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code argOffset < 0 || argOffset > 0 && argOffset >= arg.length()}
     */
    public BitString and(int thisOffset, BitString arg, int argOffset) {
        checkThisOffset(thisOffset);
        arg.checkArgOffset(argOffset);
        iAnd(thisOffset, Math.min(this.length() - thisOffset, arg.length( ) - argOffset), arg, argOffset);
        return this;
    }
    
    /**
     * Performs a logical <b>AND</b> of a substring of this {@code BitString} with a
     * substring of the specified bit string (arg).
     * 
     * This {@code BitString} is modified by this operation (see
     * {@link #and(BitString)}).
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * extends to the end of this {@code BitString}.
     * 
     * The substring argument starts at offset 'argOffset' of the specified bit
     * string and has a length of 'argLength'.
     * 
     * The length of the operation is equal to the smaller of the length of this
     * substring or the length of the substring argument.
     * 
     * @param thisOffset the start of this substring
     * @param arg        bit string argument
     * @param argOffset  the start of the argument substring
     * @param argLength  the length of the argument substring
     * @return this {@code BitString} with the results of the operation
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code argOffset < 0 || argOffset > 0 && argOffset >= arg.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code argLength < 0 || argLength > arg.length() - argOffset}
     */
    public BitString and(int thisOffset, BitString arg, int argOffset, int argLength) {
        checkThisOffset(thisOffset);
        arg.checkArgOffset(argOffset);
        arg.checkArgLength(argOffset, argLength);
        iAnd(thisOffset, Math.min(this.length( ) - thisOffset, argLength), arg, argOffset);
        return this;
    }
    
    /**
     * Performs a logical <b>AND</b> of a substring of this {@code BitString} with a
     * Field of the specified bit string (arg).
     * 
     * This {@code BitString} is modified by this operation (see
     * {@link #and(BitString)}).
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * extends to the end of this {@code BitString}.
     * 
     * The length of the operation is equal to the smaller of the length of this
     * substring or the length of the specified Field.
     * 
     * @param thisOffset the start of this substring
     * @param arg        bit string argument
     * @param argField   Field of the bit string argument
     * @return this {@code BitString} with the results of the operation
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code argField.offset() > 0 && argField.offset() >= arg.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code argField.length() > arg.length() - argField.offset()}
     *
     */
    public BitString and(int thisOffset, BitString arg, Field argField) {
        return and(thisOffset, arg, argField.offset(), argField.length());
    }
    
    /**
     * Performs a logical <b>AND</b> of a substring of this {@code BitString} with
     * the specified bit string (arg).
     * 
     * This {@code BitString} is modified by this operation (see
     * {@link #and(BitString)}).
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * has a length of 'thisLength'.
     * 
     * The length of the operation is equal to the smaller of the length of this
     * substring or the length of the specified bit string.
     * 
     * @param thisOffset the start of this substring
     * @param thisLength the length of this substring
     * @param arg        bit string argument
     * @return this {@code BitString} with the results of the operation
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisLength < 0 || thisLength > this.length() - thisOffset}
     * 
     */
    public BitString and(int thisOffset, int thisLength, BitString arg) {
        checkThisOffset(thisOffset);
        checkThisLength(thisOffset, thisLength);
        iAnd(thisOffset, Math.min(thisLength, arg.length()), arg, 0);
        return this;
    }
    
    /**
     * Performs a logical <b>AND</b> of a substring of this {@code BitString} with a
     * substring of the specified bit string (arg).
     * 
     * This {@code BitString} is modified by this operation (see
     * {@link #and(BitString)}).
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * has a length of 'thisLength'.
     * 
     * The substring argument starts at offset 'argOffset' of the specified bit
     * string and and extends to the end of the specified bit string.
     * 
     * The length of the operation is equal to the smaller of the length of this
     * substring or the length of the substring argument.
     * 
     * @param thisOffset the start of this substring
     * @param thisLength the length of this substring
     * @param arg        bit string argument
     * @param argOffset  the start of the argument substring
     * @return this {@code BitString} with the results of the operation
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code argOffset < 0 || argOffset > 0 && argOffset >= arg.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisLength < 0 || thisLength > this.length() - thisOffset}
     */
    public BitString and(int thisOffset, int thisLength, BitString arg, int argOffset) {
        checkThisOffset(thisOffset);
        checkThisLength(thisOffset, thisLength);
        arg.checkArgOffset(argOffset);
        iAnd(thisOffset, Math.min(thisLength, arg.length() - argOffset), arg, argOffset);
        return this;
    }
    
    /**
     * Performs a logical <b>AND</b> of a substring of this {@code BitString} with a
     * substring of the specified bit string (arg).
     * 
     * This {@code BitString} is modified by this operation (see
     * {@link #and(BitString)}).
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * has a length of 'thisLength'.
     * 
     * The substring argument starts at offset 'argOffset' of the specified bit
     * string and has a length of 'argLength'.
     * 
     * The length of the operation is equal to the smaller of the length of this
     * substring or the length of the substring argument.
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
     * Performs a logical <b>AND</b> of a substring of this {@code BitString} with a
     * Field of the specified bit string (arg).
     * 
     * This {@code BitString} is modified by this operation (see
     * {@link #and(BitString)}).
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * has a length of 'thisLength'.
     * 
     * The length of the operation is equal to the smaller of the length of this
     * substring or the length of the specified Field.
     * 
     * @param thisOffset the start of this substring
     * @param thisLength the length of this substring
     * @param arg        bit string argument
     * @param argField   Field of the bit string argument
     * @return this {@code BitString} with the results of the operation
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code argField.offset() > 0 && argField.offset() >= arg.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisLength < 0 || thisLength > this.length() - thisOffset}
     *                                         or
     *                                         {@code argField.length() > arg.length() - argField.offset()}
     */
    public BitString and(int thisOffset, int thisLength, BitString arg, Field argField) {
        return and(thisOffset, thisLength, arg, argField.offset(), argField.length());
    }
    
    /**
     * Performs a logical <b>AND</b> of a Field of this {@code BitString} with the
     * specified bit string (arg).
     * 
     * This {@code BitString} is modified by this operation (see
     * {@link #and(BitString)}).
     * 
     * The length of the operation is equal to the smaller of the length of this
     * Field or the length of the specified bit string.
     * 
     * @param thisField Field of this {@code BitString}
     * @param arg       bit string argument
     * @return this {@code BitString} with the results of the operation
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisField.offset() > 0 && thisField.offset() >= this.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisField.length() > this.length() - thisField.offset()}
     */
    public BitString and(Field thisField, BitString arg) {
        return and(thisField.offset(), thisField.length(), arg);
    }
    
    /**
     * Performs a logical <b>AND</b> of a Field of this {@code BitString} with a
     * substring of the specified bit string (arg).
     * 
     * This {@code BitString} is modified by this operation (see
     * {@link #and(BitString)}).
     * 
     * The substring argument starts at offset 'argOffset' of the specified bit
     * string and and extends to the end of the specified bit string.
     * 
     * The length of the operation is equal to the smaller of the length of this
     * Field or the length of the substring argument.
     * 
     * @param thisField Field of this {@code BitString}
     * @param arg       bit string argument
     * @param argOffset the start of the argument substring
     * @return this {@code BitString} with the results of the operation
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisField.offset() > 0 && thisField.offset() >= this.length()}
     *                                         or
     *                                         {@code argOffset < 0 || argOffset > 0 && argOffset >= arg.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisField.length() > this.length() - thisField.offset()}
     */
    public BitString and(Field thisField, BitString arg, int argOffset) {
        return and(thisField.offset(), thisField.length(), arg, argOffset);
    }
    
    /**
     * Performs a logical <b>AND</b> of a Field of this {@code BitString} with a
     * substring of the specified bit string (arg).
     * 
     * This {@code BitString} is modified by this operation (see
     * {@link #and(BitString)}).
     * 
     * The substring argument starts at offset 'argOffset' of the specified bit
     * string and has a length of 'argLength'.
     * 
     * The length of the operation is equal to the smaller of the length of this
     * Field or the length of the substring argument.
     * 
     * @param thisField Field of this {@code BitString}
     * @param arg       bit string argument
     * @param argOffset the start of the argument substring
     * @param argLength the length of the argument substring
     * @return this {@code BitString} with the results of the operation
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisField.offset() > 0 && thisField.offset() >= this.length()}
     *                                         or
     *                                         {@code argOffset < 0 || argOffset > 0 && argOffset >= arg.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisField.length() > this.length() - thisField.offset()}
     *                                         or
     *                                         {@code argLength < 0 || argLength > arg.length() - argOffset}
     */
    public BitString and(Field thisField, BitString arg, int argOffset, int argLength) {
        return and(thisField.offset(), thisField.length(), arg, argOffset, argLength);
    }
    
    /**
     * Performs a logical <b>AND</b> of a Field of this {@code BitString} with a
     * Field of the specified bit string (arg).
     * 
     * This {@code BitString} is modified by this operation (see
     * {@link #and(BitString)}).
     * 
     * The length of the operation is equal to the smaller of the length of this
     * Field or the length of the specified Field.
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
        return and(thisField.offset(), thisField.length(), arg, argField.offset(), argField.length());
    }
    
    /**
     * Clears all of the bits in this {@code BitString} whose corresponding bit is
     * set in the specified bit string.
     * 
     * The length of the operation is equal to the smaller of the length of this
     * {@code BitString} or the length of the specified bit string.
     * <p>
     * This {@code BitString} is modified so that each bit in it has the value
     * {@code ONE} if and only if it both initially had the value {@code ONE} and
     * the corresponding bit in the specified bit string also had the value
     * {@code ONE}. In other words, the value of each bit in this {@code BitString},
     * whose corresponding bit in the argument has the value {@code ONE}, is set to
     * {@code ZERO}, otherwise, it is left unchanged.
     *
     * @param arg bit string argument used to mask this {@code BitString} 
     * @return this {@code BitString} with the results of the operation
     */
    public BitString andNot(BitString arg) {
        iAndNot(0, Math.min(this.length(), arg.length()), arg, 0);
        return this;
    }
    
    /**
     * Clears all of the bits in this {@code BitString} whose corresponding bit is
     * set in a substring of the specified bit string (arg).
     * 
     * This {@code BitString} is modified by this operation (see
     * {@link #andNot(BitString)}).
     * 
     * The substring argument starts at offset 'argOffset' of the specified bit
     * string and extends to the end of the specified bit string.
     * 
     * The length of the operation is equal to the smaller of the length of this
     * {@code BitString} or the length of the substring argument.
     * 
     * @param arg       bit string argument
     * @param argOffset the start of the argument substring
     * @return this {@code BitString} with the results of the operation
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code argOffset < 0 || argOffset > 0 && argOffset >= arg.length()}
     */
    public BitString andNot(BitString arg, int argOffset) {
        arg.checkArgOffset(argOffset);
        iAndNot(0, Math.min(this.length(), arg.length() - argOffset), arg, argOffset);
        return this;
    }
    
    /**
     * Clears all of the bits in this {@code BitString} whose corresponding bit is
     * set in a substring of the specified bit string (arg).
     * 
     * This {@code BitString} is modified by this operation (see
     * {@link #andNot(BitString)}).
     * 
     * The substring argument starts at offset 'argOffset' of the specified bit
     * string and has a length of 'argLength'.
     * 
     * The length of the operation is equal to the smaller of the length of this
     * {@code BitString} or the length of the substring argument.
     * 
     * @param arg       bit string argument
     * @param argOffset the start of the argument substring
     * @param argLength the length of the argument substring
     * @return this {@code BitString} with the results of the operation
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code argOffset < 0 || argOffset > 0 && argOffset >= arg.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code argLength < 0 || argLength > arg.length() - argOffset}
     */
    public BitString andNot(BitString arg, int argOffset, int argLength) {
        arg.checkArgOffset(argOffset);
        arg.checkArgLength(argOffset, argLength);
        iAndNot(0, Math.min(this.length(), argLength), arg, argOffset);
        return this;
    }
    
    /**
     * Clears all of the bits in this {@code BitString} whose corresponding bit is
     * set in a Field of the specified bit string (arg).
     * 
     * This {@code BitString} is modified by this operation (see
     * {@link #andNot(BitString)}).
     * 
     * The length of the operation is equal to the smaller of the length of this
     * {@code BitString} or the length of the specified Field.
     * 
     * @param arg      bit string argument
     * @param argField Field of the bit string argument
     * @return this {@code BitString} with the results of the operation
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code argField.offset() > 0 && argField.offset() >= arg.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code argField.length() > arg.length() - argField.offset()}
     */
    public BitString andNot(BitString arg, Field argField) {
        return andNot(arg, argField.offset(), argField.length());
    } 
    
    /**
     * Clears all of the bits in a substring of this {@code BitString} whose corresponding bit is
     * set in the specified bit string (arg).
     * 
     * This {@code BitString} is modified by this operation (see
     * {@link #andNot(BitString)}).
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * extends to the end of this {@code BitString}.
     * 
     * The length of the operation is equal to the smaller of the length of this
     * substring or the length of the specified bit string.
     * 
     * @param thisOffset the start of this substring
     * @param arg        bit string argument
     * @return this {@code BitString} with the results of the operation
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     */
    public BitString andNot(int thisOffset, BitString arg) {
        checkThisOffset(thisOffset);
        iAndNot(thisOffset, Math.min(this.length() - thisOffset, arg.length()), arg, 0);
        return this;
    }
    
    /**
     * Clears all of the bits in a substring of this {@code BitString} whose corresponding bit is
     * set in a substring of the specified bit string (arg).
     * 
     * This {@code BitString} is modified by this operation (see
     * {@link #andNot(BitString)}).
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * extends to the end of this {@code BitString}.
     * 
     * The substring argument starts at offset 'argOffset' of the specified bit
     * string and and extends to the end of the specified bit string.
     * 
     * The length of the operation is equal to the smaller of the length of this
     * substring or the length of the substring argument.
     * 
     * @param thisOffset the start of this substring
     * @param arg        bit string argument
     * @param argOffset  the start of the argument substring
     * @return this {@code BitString} with the results of the operation
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code argOffset < 0 || argOffset > 0 && argOffset >= arg.length()}
     */
    public BitString andNot(int thisOffset, BitString arg, int argOffset) {
        checkThisOffset(thisOffset);
        arg.checkArgOffset(argOffset);
        iAndNot(thisOffset, Math.min(this.length() - thisOffset, arg.length() - argOffset), arg, argOffset);
        return this;
    }
    
    /**
     * Clears all of the bits in a substring of this {@code BitString} whose corresponding bit is
     * set in a substring of the specified bit string (arg).
     * 
     * This {@code BitString} is modified by this operation (see
     * {@link #andNot(BitString)}).
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * extends to the end of this {@code BitString}.
     * 
     * The substring argument starts at offset 'argOffset' of the specified bit
     * string and has a length of 'argLength'.
     * 
     * The length of the operation is equal to the smaller of the length of this
     * substring or the length of the substring argument.
     * 
     * @param thisOffset the start of this substring
     * @param arg        bit string argument
     * @param argOffset  the start of the argument substring
     * @param argLength  the length of the argument substring
     * @return this {@code BitString} with the results of the operation
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code argOffset < 0 || argOffset > 0 && argOffset >= arg.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code argLength < 0 || argLength > arg.length() - argOffset}
     */
    public BitString andNot(int thisOffset, BitString arg, int argOffset, int argLength) {
        checkThisOffset(thisOffset);
        arg.checkArgOffset(argOffset);
        arg.checkArgLength(argOffset, argLength);
        iAndNot(thisOffset, Math.min(this.length() - thisOffset, argLength), arg, argOffset);
        return this;
    }
    
    /**
     * Clears all of the bits in a substring of this {@code BitString} whose corresponding bit is
     * set in a Field of the specified bit string (arg).
     * 
     * This {@code BitString} is modified by this operation (see
     * {@link #andNot(BitString)}).
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * extends to the end of this {@code BitString}.
     * 
     * The length of the operation is equal to the smaller of the length of this
     * substring or the length of the specified Field.
     * 
     * @param thisOffset the start of this substring
     * @param arg        bit string argument
     * @param argField   Field of the bit string argument
     * @return this {@code BitString} with the results of the operation
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code argField.offseargField.offset() > 0 && argField.offset() >= arg.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code argField.length() > arg.length() - argField.offset()}
     *
     */
    public BitString andNot(int thisOffset, BitString arg, Field argField) {
        return andNot(thisOffset, arg, argField.offset(), argField.length());
    }
    
    /**
     * Clears all of the bits in a substring of this {@code BitString} whose corresponding bit is
     * set in the specified bit string (arg).
     * 
     * This {@code BitString} is modified by this operation (see
     * {@link #andNot(BitString)}).
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * has a length of 'thisLength'.
     * 
     * The length of the operation is equal to the smaller of the length of this
     * substring or the length of the specified bit string.
     * 
     * @param thisOffset the start of this substring
     * @param thisLength the length of this substring
     * @param arg        bit string argument
     * @return this {@code BitString} with the results of the operation
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisLength < 0 || thisLength > this.length() - thisOffset}
     * 
     */
    public BitString andNot(int thisOffset, int thisLength, BitString arg) {
        checkThisOffset(thisOffset);
        checkThisLength(thisOffset, thisLength);
        iAndNot(thisOffset, Math.min(thisLength, arg.length()), arg, 0);
        return this;
    }
    
    /**
     * Clears all of the bits in a substring of this {@code BitString} whose corresponding bit is
     * set in a substring of the specified bit string (arg).
     * 
     * This {@code BitString} is modified by this operation (see
     * {@link #andNot(BitString)}).
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * has a length of 'thisLength'.
     * 
     * The substring argument starts at offset 'argOffset' of the specified bit
     * string and and extends to the end of the specified bit string.
     * 
     * The length of the operation is equal to the smaller of the length of this
     * substring or the length of the substring argument.
     * 
     * @param thisOffset the start of this substring
     * @param thisLength the length of this substring
     * @param arg        bit string argument
     * @param argOffset  the start of the argument substring
     * @return this {@code BitString} with the results of the operation
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code argOffset < 0 || argOffset > 0 && argOffset >= arg.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisLength < 0 || thisLength > this.length() - thisOffset}
     */
    public BitString andNot(int thisOffset, int thisLength, BitString arg, int argOffset) {
        checkThisOffset(thisOffset);
        checkThisLength(thisOffset, thisLength);
        arg.checkArgOffset(argOffset);
        iAndNot(thisOffset, Math.min(thisLength, arg.length() - argOffset), arg, argOffset);
        return this;
    }
    
    /**
     * Clears all of the bits in a substring of this {@code BitString} whose corresponding bit is
     * set in a substring of the specified bit string (arg).
     * 
     * This {@code BitString} is modified by this operation (see
     * {@link #andNot(BitString)}).
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * has a length of 'thisLength'.
     * 
     * The substring argument starts at offset 'argOffset' of the specified bit
     * string and has a length of 'argLength'.
     * 
     * The length of the operation is equal to the smaller of the length of this
     * substring or the length of the substring argument.
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
     * Clears all of the bits in a substring of this {@code BitString} whose corresponding bit is
     * set in a Field of the specified bit string (arg).
     * 
     * This {@code BitString} is modified by this operation (see
     * {@link #andNot(BitString)}).
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * has a length of 'thisLength'.
     * 
     * The length of the operation is equal to the smaller of the length of this
     * substring or the length of the specified Field.
     * 
     * @param thisOffset the start of this substring
     * @param thisLength the length of this substring
     * @param arg        bit string argument
     * @param argField   Field of the bit string argument
     * @return this {@code BitString} with the results of the operation
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code argField.offset() > 0 && argField.offset() >= arg.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisLength < 0 || thisLength > this.length() - thisOffset}
     *                                         or
     *                                         {@code argField.length() > arg.length() - argField.offset()}
     */
    public BitString andNot(int thisOffset, int thisLength, BitString arg, Field argField) {
        return andNot(thisOffset, thisLength, arg, argField.offset(), argField.length());
    }
    
    /**
     * Clears all of the bits in a Field of this {@code BitString} whose corresponding bit is
     * set in the specified bit string (arg).
     * 
     * This {@code BitString} is modified by this operation (see
     * {@link #andNot(BitString)}).
     * 
     * The length of the operation is equal to the smaller of the length of this
     * Field or the length of the specified bit string.
     * 
     * @param thisField Field of this {@code BitString}
     * @param arg       bit string argument
     * @return this {@code BitString} with the results of the operation
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisField.offset() > 0 && thisField.offset() >= this.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisField.length() > this.length() - thisField.offset()}
     */
    public BitString andNot(Field thisField, BitString arg) {
        return andNot(thisField.offset(), thisField.length(), arg);
    }
    
    /**
     * Clears all of the bits in a Field of this {@code BitString} whose corresponding bit is
     * set in a substring of the specified bit string (arg).
     * 
     * This {@code BitString} is modified by this operation (see
     * {@link #andNot(BitString)}).
     * 
     * The substring argument starts at offset 'argOffset' of the specified bit
     * string and and extends to the end of the specified bit string.
     * 
     * The length of the operation is equal to the smaller of the length of this
     * Field or the length of the substring argument.
     * 
     * @param thisField Field of this {@code BitString}
     * @param arg       bit string argument
     * @param argOffset the start of the argument substring
     * @return this {@code BitString} with the results of the operation
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisField.offset() > 0 && thisField.offset() >= this.length()}
     *                                         or
     *                                         {@code argOffset < 0 || argOffset > 0 && argOffset >= arg.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisField.length() > this.length() - thisField.offset()}
     */
    public BitString andNot(Field thisField, BitString arg, int argOffset) {
        return andNot(thisField.offset(), thisField.length(), arg, argOffset);
    }
    
    /**
     * Clears all of the bits in a Field of this {@code BitString} whose corresponding bit is
     * set in a substring of the specified bit string (arg).
     * 
     * This {@code BitString} is modified by this operation (see
     * {@link #andNot(BitString)}).
     * 
     * The substring argument starts at offset 'argOffset' of the specified bit
     * string and has a length of 'argLength'.
     * 
     * The length of the operation is equal to the smaller of the length of this
     * Field or the length of the substring argument.
     * 
     * @param thisField Field of this {@code BitString}
     * @param arg       bit string argument
     * @param argOffset the start of the argument substring
     * @param argLength the length of the argument substring
     * @return this {@code BitString} with the results of the operation
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisField.offset() > 0 && thisField.offset() >= this.length()}
     *                                         or
     *                                         {@code argOffset < 0 || argOffset > 0 && argOffset >= arg.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisField.length() > this.length() - thisField.offset()}
     *                                         or
     *                                         {@code argLength < 0 || argLength > arg.length() - argOffset}
     */
    public BitString andNot(Field thisField, BitString arg, int argOffset, int argLength) {
        return andNot(thisField.offset(), thisField.length(), arg, argOffset, argLength);
    }
    
    /**
     * Clears all of the bits in a Field of this {@code BitString} whose corresponding bit is
     * set in a Field of the specified bit string (arg).
     * 
     * This {@code BitString} is modified by this operation (see
     * {@link #andNot(BitString)}).
     * 
     * The length of the operation is equal to the smaller of the length of this
     * Field or the length of the specified Field.
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
        return andNot(thisField.offset(), thisField.length(), arg, argField.offset(), argField.length());
    }
    
    /**
     * Performs a logical <b>OR</b> of this {@code BitString} with the specified bit
     * string (arg).
     * 
     * The length of the operation is equal to the smaller of the length of this
     * {@code BitString} or the length of the specified bit string.
     * <p>
     * This {@code BitString} is modified so that a bit in it has the value
     * {@code ONE} if and only if it either already had the value {@code ONE} or the
     * corresponding bit in the bit set argument has the value {@code ONE}. In other
     * words, the value of each bit in this {@code BitString}, whose corresponding
     * bit in the argument has the value {@code ONE}, is set to a {@code ONE},
     * otherwise, it is left unchanged.
     *
     * @param arg bit string argument
     * @return this {@code BitString} with the results of the operation
     */
    public BitString or(BitString arg) {
        iOr(0, Math.min(this.length(), arg.length()), arg, 0);
        return this;
    }
    
    /**
     * Performs a logical <b>OR</b> of this {@code BitString} with a substring of
     * the specified bit string (arg).
     * 
     * This {@code BitString} is modified by this operation (see
     * {@link #or(BitString)}).
     * 
     * The substring argument starts at offset 'argOffset' of the specified bit
     * string and extends to the end of the specified bit string.
     * 
     * The length of the operation is equal to the smaller of the length of this
     * {@code BitString} or the length of the substring argument.
     * 
     * @param arg       bit string argument
     * @param argOffset the start of the argument substring
     * @return this {@code BitString} with the results of the operation
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code argOffset < 0 || argOffset > 0 && argOffset >= arg.length()}
     */
    public BitString or(BitString arg, int argOffset) {
        arg.checkArgOffset(argOffset);
        iOr(0, Math.min(this.length(), arg.length() - argOffset), arg, argOffset);
        return this;
    }
    
    /**
     * Performs a logical <b>OR</b> of this {@code BitString} with a substring of
     * the specified bit string (arg).
     * 
     * This {@code BitString} is modified by this operation (see
     * {@link #or(BitString)}).
     * 
     * The substring argument starts at offset 'argOffset' of the specified bit
     * string and has a length of 'argLength'.
     * 
     * The length of the operation is equal to the smaller of the length of this
     * {@code BitString} or the length of the substring argument.
     * 
     * @param arg       bit string argument
     * @param argOffset the start of the argument substring
     * @param argLength the length of the argument substring
     * @return this {@code BitString} with the results of the operation
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code argOffset < 0 || argOffset > 0 && argOffset >= arg.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code argLength < 0 || argLength > arg.length() - argOffset}
     */
    public BitString or(BitString arg, int argOffset, int argLength) {
        arg.checkArgOffset(argOffset);
        arg.checkArgLength(argOffset, argLength);
        iOr(0, Math.min(this.length(), argLength), arg, argOffset);
        return this;
    }
    
    /**
     * Performs a logical <b>OR</b> of this {@code BitString} with a Field of the
     * specified bit string (arg).
     * 
     * This {@code BitString} is modified by this operation (see
     * {@link #or(BitString)}).
     * 
     * The length of the operation is equal to the smaller of the length of this
     * {@code BitString} or the length of the specified Field.
     * 
     * @param arg      bit string argument
     * @param argField Field of the bit string argument
     * @return this {@code BitString} with the results of the operation
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code rgField.offset() > 0 && argField.offset() >= arg.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code argField.length() > arg.length() - argField.offset()}
     */
    public BitString or(BitString arg, Field argField) {
        return or(arg, argField.offset(), argField.length());
    } 
    
    /**
     * Performs a logical <b>OR</b> of a substring of this {@code BitString} with
     * the specified bit string (arg).
     * 
     * This {@code BitString} is modified by this operation (see
     * {@link #or(BitString)}).
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * extends to the end of this {@code BitString}.
     * 
     * The length of the operation is equal to the smaller of the length of this
     * substring or the length of the specified bit string.
     * 
     * @param thisOffset the start of this substring
     * @param arg        bit string argument
     * @return this {@code BitString} with the results of the operation
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     */
    public BitString or(int thisOffset, BitString arg) {
        checkThisOffset(thisOffset);
        iOr(thisOffset, Math.min(this.length() - thisOffset, arg.length()), arg, 0);
        return this;
    }
    
    /**
     * Performs a logical <b>OR</b> of a substring of this {@code BitString} with a
     * substring of the specified bit string (arg).
     * 
     * This {@code BitString} is modified by this operation (see
     * {@link #or(BitString)}).
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * extends to the end of this {@code BitString}.
     * 
     * The substring argument starts at offset 'argOffset' of the specified bit
     * string and and extends to the end of the specified bit string.
     * 
     * The length of the operation is equal to the smaller of the length of this
     * substring or the length of the substring argument.
     * 
     * @param thisOffset the start of this substring
     * @param arg        bit string argument
     * @param argOffset  the start of the argument substring
     * @return this {@code BitString} with the results of the operation
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code argOffset < 0 || argOffset > 0 && argOffset >= arg.length()}
     */
    public BitString or(int thisOffset, BitString arg, int argOffset) {
        checkThisOffset(thisOffset);
        arg.checkArgOffset(argOffset);
        iOr(thisOffset, Math.min(this.length() - thisOffset, arg.length() - argOffset), arg, argOffset);
        return this;
    }
    
    /**
     * Performs a logical <b>OR</b> of a substring of this {@code BitString} with a
     * substring of the specified bit string (arg).
     * 
     * This {@code BitString} is modified by this operation (see
     * {@link #or(BitString)}).
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * extends to the end of this {@code BitString}.
     * 
     * The substring argument starts at offset 'argOffset' of the specified bit
     * string and has a length of 'argLength'.
     * 
     * The length of the operation is equal to the smaller of the length of this
     * substring or the length of the substring argument.
     * 
     * @param thisOffset the start of this substring
     * @param arg        bit string argument
     * @param argOffset  the start of the argument substring
     * @param argLength  the length of the argument substring
     * @return this {@code BitString} with the results of the operation
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code argOffset < 0 || argOffset > 0 && argOffset >= arg.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code argLength < 0 || argLength > arg.length() - argOffset}
     */
    public BitString or(int thisOffset, BitString arg, int argOffset, int argLength) {
        checkThisOffset(thisOffset);
        arg.checkArgOffset(argOffset);
        arg.checkArgLength(argOffset, argLength);
        iOr(thisOffset, Math.min(this.length() - thisOffset, argLength), arg, argOffset);
        return this;
    }
    
    /**
     * Performs a logical <b>OR</b> of a substring of this {@code BitString} with a
     * Field of the specified bit string (arg).
     * 
     * This {@code BitString} is modified by this operation (see
     * {@link #or(BitString)}).
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * extends to the end of this {@code BitString}.
     * 
     * The length of the operation is equal to the smaller of the length of this
     * substring or the length of the specified Field.
     * 
     * @param thisOffset the start of this substring
     * @param arg        bit string argument
     * @param argField   Field of the bit string argument
     * @return this {@code BitString} with the results of the operation
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code argField.offset() > 0 && argField.offset() >= arg.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code argField.length() > arg.length() - argField.offset()}
     *
     */
    public BitString or(int thisOffset, BitString arg, Field argField) {
        return or(thisOffset, arg, argField.offset(), argField.length());
    }
    
    /**
     * Performs a logical <b>OR</b> of a substring of this {@code BitString} with
     * the specified bit string (arg).
     * 
     * This {@code BitString} is modified by this operation (see
     * {@link #or(BitString)}).
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * has a length of 'thisLength'.
     * 
     * The length of the operation is equal to the smaller of the length of this
     * substring or the length of the specified bit string.
     * 
     * @param thisOffset the start of this substring
     * @param thisLength the length of this substring
     * @param arg        bit string argument
     * @return this {@code BitString} with the results of the operation
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisLength < 0 || thisLength > this.length() - thisOffset}
     * 
     */
    public BitString or(int thisOffset, int thisLength, BitString arg) {
        checkThisOffset(thisOffset);
        checkThisLength(thisOffset, thisLength);
        iOr(thisOffset, Math.min(thisLength, arg.length()), arg, 0);
        return this;
    }
    
    /**
     * Performs a logical <b>OR</b> of a substring of this {@code BitString} with a
     * substring of the specified bit string (arg).
     * 
     * This {@code BitString} is modified by this operation (see
     * {@link #or(BitString)}).
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * has a length of 'thisLength'.
     * 
     * The substring argument starts at offset 'argOffset' of the specified bit
     * string and and extends to the end of the specified bit string.
     * 
     * The length of the operation is equal to the smaller of the length of this
     * substring or the length of the substring argument.
     * 
     * @param thisOffset the start of this substring
     * @param thisLength the length of this substring
     * @param arg        bit string argument
     * @param argOffset  the start of the argument substring
     * @return this {@code BitString} with the results of the operation
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code argOffset < 0 || argOffset > 0 && argOffset >= arg.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisLength < 0 || thisLength > this.length() - thisOffset}
     */
    public BitString or(int thisOffset, int thisLength, BitString arg, int argOffset) {
        checkThisOffset(thisOffset);
        checkThisLength(thisOffset, thisLength);
        arg.checkArgOffset(argOffset);
        iOr(thisOffset, Math.min(thisLength, arg.length() - argOffset), arg, argOffset);
        return this;
    }
    
    /**
     * Performs a logical <b>OR</b> of a substring of this {@code BitString} with a
     * substring of the specified bit string (arg).
     * 
     * This {@code BitString} is modified by this operation (see
     * {@link #or(BitString)}).
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * has a length of 'thisLength'.
     * 
     * The substring argument starts at offset 'argOffset' of the specified bit
     * string and has a length of 'argLength'.
     * 
     * The length of the operation is equal to the smaller of the length of this
     * substring or the length of the substring argument.
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
     * Performs a logical <b>OR</b> of a substring of this {@code BitString} with a
     * Field of the specified bit string (arg).
     * 
     * This {@code BitString} is modified by this operation (see
     * {@link #or(BitString)}).
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * has a length of 'thisLength'.
     * 
     * The length of the operation is equal to the smaller of the length of this
     * substring or the length of the specified Field.
     * 
     * @param thisOffset the start of this substring
     * @param thisLength the length of this substring
     * @param arg        bit string argument
     * @param argField   Field of the bit string argument
     * @return this {@code BitString} with the results of the operation
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code argField.offset() > 0 && argField.offset() >= arg.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisLength < 0 || thisLength > this.length() - thisOffset}
     *                                         or
     *                                         {@code argField.length() > arg.length() - argField.offset()}
     */
    public BitString or(int thisOffset, int thisLength, BitString arg, Field argField) {
        return or(thisOffset, thisLength, arg, argField.offset(), argField.length());
    }
    
    /**
     * Performs a logical <b>OR</b> of a Field of this {@code BitString} with the
     * specified bit string (arg).
     * 
     * This {@code BitString} is modified by this operation (see
     * {@link #or(BitString)}).
     * 
     * The length of the operation is equal to the smaller of the length of this
     * Field or the length of the specified bit string.
     * 
     * @param thisField Field of this {@code BitString}
     * @param arg       bit string argument
     * @return this {@code BitString} with the results of the operation
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisField.offset() > 0 && thisField.offset() >= this.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisField.length() > this.length() - thisField.offset()}
     */
    public BitString or(Field thisField, BitString arg) {
        return or(thisField.offset(), thisField.length(), arg);
    }
    
    /**
     * Performs a logical <b>OR</b> of a Field of this {@code BitString} with a
     * substring of the specified bit string (arg).
     * 
     * This {@code BitString} is modified by this operation (see
     * {@link #or(BitString)}).
     * 
     * The substring argument starts at offset 'argOffset' of the specified bit
     * string and and extends to the end of the specified bit string.
     * 
     * The length of the operation is equal to the smaller of the length of this
     * Field or the length of the substring argument.
     * 
     * @param thisField Field of this {@code BitString}
     * @param arg       bit string argument
     * @param argOffset the start of the argument substring
     * @return this {@code BitString} with the results of the operation
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisField.offset() > 0 && thisField.offset() >= this.length()}
     *                                         or
     *                                         {@code argOffset < 0 || argOffset > 0 && argOffset >= arg.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisField.length() > this.length() - thisField.offset()}
     */
    public BitString or(Field thisField, BitString arg, int argOffset) {
        return or(thisField.offset(), thisField.length(), arg, argOffset);
    }
    
    /**
     * Performs a logical <b>OR</b> of a Field of this {@code BitString} with a
     * substring of the specified bit string (arg).
     * 
     * This {@code BitString} is modified by this operation (see
     * {@link #or(BitString)}).
     * 
     * The substring argument starts at offset 'argOffset' of the specified bit
     * string and has a length of 'argLength'.
     * 
     * The length of the operation is equal to the smaller of the length of this
     * Field or the length of the substring argument.
     * 
     * @param thisField Field of this {@code BitString}
     * @param arg       bit string argument
     * @param argOffset the start of the argument substring
     * @param argLength the length of the argument substring
     * @return this {@code BitString} with the results of the operation
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisField.offset() > 0 && thisField.offset() >= this.length()}
     *                                         or
     *                                         {@code argOffset < 0 || argOffset > 0 && argOffset >= arg.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisField.length() > this.length() - thisField.offset()}
     *                                         or
     *                                         {@code argLength < 0 || argLength > arg.length() - argOffset}
     */
    public BitString or(Field thisField, BitString arg, int argOffset, int argLength) {
        return or(thisField.offset(), thisField.length(), arg, argOffset, argLength);
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
        return or(thisField.offset(), thisField.length(), arg, argField.offset(), argField.length());
    }
    
    /**
     * Performs a logical <b>XOR</b> of this {@code BitString} with the specified
     * bit string (arg).
     * 
     * The length of the operation is equal to the smaller of the length of this
     * {@code BitString} or the length of the specified bit string.
     * <p>
     * This {@code BitString} is modified so that a bit in it has the value
     * {@code ONE} if and only if one of the following statements holds:
     * <ul>
     * <li>The bit initially has the value {@code ONE}, and the corresponding bit in
     * the argument has the value {@code ZERO}.
     * <li>The bit initially has the value {@code ZERO}, and the corresponding bit
     * in the argument has the value {@code ONE}.
     * </ul>
     * In other words, the value of each bit in this {@code BitString}, whose
     * corresponding bit in the argument has the value {@code ONE}, is flipped,
     * otherwise, it is left unchanged.
     *
     * @param arg bit string argument
     * @return this {@code BitString} with the results of the operation
     */
    public BitString xor(BitString arg) {
        iXor(0, Math.min(this.length(), arg.length()), arg, 0);
        return this;
    }
    
    /**
     * Performs a logical <b>XOR</b> of this {@code BitString} with a substring of
     * the specified bit string (arg).
     * 
     * This {@code BitString} is modified by this operation (see
     * {@link #xor(BitString)}).
     * 
     * The substring argument starts at offset 'argOffset' of the specified bit
     * string and extends to the end of the specified bit string.
     * 
     * The length of the operation is equal to the smaller of the length of this
     * {@code BitString} or the length of the substring argument.
     * 
     * @param arg       bit string argument
     * @param argOffset the start of the argument substring
     * @return this {@code BitString} with the results of the operation
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code argOffset < 0 || argOffset > 0 && argOffset >= arg.length()}
     */
    public BitString xor(BitString arg, int argOffset) {
        arg.checkArgOffset(argOffset);
        iXor(0, Math.min(this.length(), arg.length() - argOffset), arg, argOffset);
        return this;
    }
    
    /**
     * Performs a logical <b>XOR</b> of this {@code BitString} with a substring of
     * the specified bit string (arg).
     * 
     * This {@code BitString} is modified by this operation (see
     * {@link #xor(BitString)}).
     * 
     * The substring argument starts at offset 'argOffset' of the specified bit
     * string and has a length of 'argLength'.
     * 
     * The length of the operation is equal to the smaller of the length of this
     * {@code BitString} or the length of the substring argument.
     * 
     * @param arg       bit string argument
     * @param argOffset the start of the argument substring
     * @param argLength the length of the argument substring
     * @return this {@code BitString} with the results of the operation
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code argOffset < 0 || argOffset > 0 && argOffset >= arg.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code argLength < 0 || argLength > arg.length() - argOffset}
     */
    public BitString xor(BitString arg, int argOffset, int argLength) {
        arg.checkArgOffset(argOffset);
        arg.checkArgLength(argOffset, argLength);
        iXor(0, Math.min(this.length(), argLength), arg, argOffset);
        return this;
    }
    
    /**
     * Performs a logical <b>XOR</b> of this {@code BitString} with a Field of the
     * specified bit string (arg).
     * 
     * This {@code BitString} is modified by this operation (see
     * {@link #xor(BitString)}).
     * 
     * The length of the operation is equal to the smaller of the length of this
     * {@code BitString} or the length of the specified Field.
     * 
     * @param arg      bit string argument
     * @param argField Field of the bit string argument
     * @return this {@code BitString} with the results of the operation
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code argField.offset() > 0 && argField.offset() >= arg.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code argField.length() > arg.length() - argField.offset()}
     */
    public BitString xor(BitString arg, Field argField) {
        return xor(arg, argField.offset(), argField.length());
    } 
    
    /**
     * Performs a logical <b>XOR</b> of a substring of this {@code BitString} with
     * the specified bit string (arg).
     * 
     * This {@code BitString} is modified by this operation (see
     * {@link #xor(BitString)}).
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * extends to the end of this {@code BitString}.
     * 
     * The length of the operation is equal to the smaller of the length of this
     * substring or the length of the specified bit string.
     * 
     * @param thisOffset the start of this substring
     * @param arg        bit string argument
     * @return this {@code BitString} with the results of the operation
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     */
    public BitString xor(int thisOffset, BitString arg) {
        checkThisOffset(thisOffset);
        iXor(thisOffset, Math.min(this.length() - thisOffset, arg.length()), arg, 0);
        return this;
    }
    
    /**
     * Performs a logical <b>XOR</b> of a substring of this {@code BitString} with a
     * substring of the specified bit string (arg).
     * 
     * This {@code BitString} is modified by this operation (see
     * {@link #xor(BitString)}).
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * extends to the end of this {@code BitString}.
     * 
     * The substring argument starts at offset 'argOffset' of the specified bit
     * string and and extends to the end of the specified bit string.
     * 
     * The length of the operation is equal to the smaller of the length of this
     * substring or the length of the substring argument.
     * 
     * @param thisOffset the start of this substring
     * @param arg        bit string argument
     * @param argOffset  the start of the argument substring
     * @return this {@code BitString} with the results of the operation
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code argOffset < 0 || argOffset > 0 && argOffset >= arg.length()}
     */
    public BitString xor(int thisOffset, BitString arg, int argOffset) {
        checkThisOffset(thisOffset);
        arg.checkArgOffset(argOffset);
        iXor(thisOffset, Math.min(this.length() - thisOffset, arg.length() - argOffset), arg, argOffset);
        return this;
    }
    
    /**
     * Performs a logical <b>XOR</b> of a substring of this {@code BitString} with a
     * substring of the specified bit string (arg).
     * 
     * This {@code BitString} is modified by this operation (see
     * {@link #xor(BitString)}).
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * extends to the end of this {@code BitString}.
     * 
     * The substring argument starts at offset 'argOffset' of the specified bit
     * string and has a length of 'argLength'.
     * 
     * The length of the operation is equal to the smaller of the length of this
     * substring or the length of the substring argument.
     * 
     * @param thisOffset the start of this substring
     * @param arg        bit string argument
     * @param argOffset  the start of the argument substring
     * @param argLength  the length of the argument substring
     * @return this {@code BitString} with the results of the operation
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code argOffset < 0 || argOffset > 0 && argOffset >= arg.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code argLength < 0 || argLength > arg.length() - argOffset}
     */
    public BitString xor(int thisOffset, BitString arg, int argOffset, int argLength) {
        checkThisOffset(thisOffset);
        arg.checkArgOffset(argOffset);
        arg.checkArgLength(argOffset, argLength);
        iXor(thisOffset, Math.min(this.length() - thisOffset, argLength), arg, argOffset);
        return this;
    }
    
    /**
     * Performs a logical <b>XOR</b> of a substring of this {@code BitString} with a
     * Field of the specified bit string (arg).
     * 
     * This {@code BitString} is modified by this operation (see
     * {@link #xor(BitString)}).
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * extends to the end of this {@code BitString}.
     * 
     * The length of the operation is equal to the smaller of the length of this
     * substring or the length of the specified Field.
     * 
     * @param thisOffset the start of this substring
     * @param arg        bit string argument
     * @param argField   Field of the bit string argument
     * @return this {@code BitString} with the results of the operation
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code argField.offset() > 0 && argField.offset() >= arg.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code argField.length() > arg.length() - argField.offset()}
     *
     */
    public BitString xor(int thisOffset, BitString arg, Field argField) {
        return xor(thisOffset, arg, argField.offset(), argField.length());
    }
    
    /**
     * Performs a logical <b>XOR</b> of a substring of this {@code BitString} with
     * the specified bit string (arg).
     * 
     * This {@code BitString} is modified by this operation (see
     * {@link #xor(BitString)}).
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * has a length of 'thisLength'.
     * 
     * The length of the operation is equal to the smaller of the length of this
     * substring or the length of the specified bit string.
     * 
     * @param thisOffset the start of this substring
     * @param thisLength the length of this substring
     * @param arg        bit string argument
     * @return this {@code BitString} with the results of the operation
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisLength < 0 || thisLength > this.length() - thisOffset}
     * 
     */
    public BitString xor(int thisOffset, int thisLength, BitString arg) {
        checkThisOffset(thisOffset);
        checkThisLength(thisOffset, thisLength);
        iXor(thisOffset, Math.min(thisLength, arg.length()), arg, 0);
        return this;
    }
    
    /**
     * Performs a logical <b>XOR</b> of a substring of this {@code BitString} with a
     * substring of the specified bit string (arg).
     * 
     * This {@code BitString} is modified by this operation (see
     * {@link #xor(BitString)}).
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * has a length of 'thisLength'.
     * 
     * The substring argument starts at offset 'argOffset' of the specified bit
     * string and and extends to the end of the specified bit string.
     * 
     * The length of the operation is equal to the smaller of the length of this
     * substring or the length of the substring argument.
     * 
     * @param thisOffset the start of this substring
     * @param thisLength the length of this substring
     * @param arg        bit string argument
     * @param argOffset  the start of the argument substring
     * @return this {@code BitString} with the results of the operation
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code argOffset < 0 || argOffset > 0 && argOffset >= arg.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisLength < 0 || thisLength > this.length() - thisOffset}
     */
    public BitString xor(int thisOffset, int thisLength, BitString arg, int argOffset) {
        checkThisOffset(thisOffset);
        checkThisLength(thisOffset, thisLength);
        arg.checkArgOffset(argOffset);
        iXor(thisOffset, Math.min(thisLength, arg.length() - argOffset), arg, argOffset);
        return this;
    }
    
    /**
     * Performs a logical <b>XOR</b> of a substring of this {@code BitString} with a
     * substring of the specified bit string (arg).
     * 
     * This {@code BitString} is modified by this operation (see
     * {@link #xor(BitString)}).
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * has a length of 'thisLength'.
     * 
     * The substring argument starts at offset 'argOffset' of the specified bit
     * string and has a length of 'argLength'.
     * 
     * The length of the operation is equal to the smaller of the length of this
     * substring or the length of the substring argument.
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
     * Performs a logical <b>XOR</b> of a substring of this {@code BitString} with a
     * Field of the specified bit string (arg).
     * 
     * This {@code BitString} is modified by this operation (see
     * {@link #xor(BitString)}).
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * has a length of 'thisLength'.
     * 
     * The length of the operation is equal to the smaller of the length of this
     * substring or the length of the specified Field.
     * 
     * @param thisOffset the start of this substring
     * @param thisLength the length of this substring
     * @param arg        bit string argument
     * @param argField   Field of the bit string argument
     * @return this {@code BitString} with the results of the operation
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code argField.offset() > 0 && argField.offset() >= arg.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisLength < 0 || thisLength > this.length() - thisOffset}
     *                                         or
     *                                         {@code argField.length() > arg.length() - argField.offset()}
     */
    public BitString xor(int thisOffset, int thisLength, BitString arg, Field argField) {
        return xor(thisOffset, thisLength, arg, argField.offset(), argField.length());
    }
    
    /**
     * Performs a logical <b>XOR</b> of a Field of this {@code BitString} with the
     * specified bit string (arg).
     * 
     * This {@code BitString} is modified by this operation (see
     * {@link #xor(BitString)}).
     * 
     * The length of the operation is equal to the smaller of the length of this
     * Field or the length of the specified bit string.
     * 
     * @param thisField Field of this {@code BitString}
     * @param arg       bit string argument
     * @return this {@code BitString} with the results of the operation
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisField.offset() > 0 && thisField.offset() >= this.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisField.length() > this.length() - thisField.offset()}
     */
    public BitString xor(Field thisField, BitString arg) {
        return xor(thisField.offset(), thisField.length(), arg);
    }
    
    /**
     * Performs a logical <b>XOR</b> of a Field of this {@code BitString} with a
     * substring of the specified bit string (arg).
     * 
     * This {@code BitString} is modified by this operation (see
     * {@link #xor(BitString)}).
     * 
     * The substring argument starts at offset 'argOffset' of the specified bit
     * string and and extends to the end of the specified bit string.
     * 
     * The length of the operation is equal to the smaller of the length of this
     * Field or the length of the substring argument.
     * 
     * @param thisField Field of this {@code BitString}
     * @param arg       bit string argument
     * @param argOffset the start of the argument substring
     * @return this {@code BitString} with the results of the operation
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisField.offset() > 0 && thisField.offset() >= this.length()}
     *                                         or
     *                                         {@code argOffset < 0 || argOffset > 0 && argOffset >= arg.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisField.length() > this.length() - thisField.offset()}
     */
    public BitString xor(Field thisField, BitString arg, int argOffset) {
        return xor(thisField.offset(), thisField.length(), arg, argOffset);
    }
    
    /**
     * Performs a logical <b>XOR</b> of a Field of this {@code BitString} with a
     * substring of the specified bit string (arg).
     * 
     * This {@code BitString} is modified by this operation (see
     * {@link #xor(BitString)}).
     * 
     * The substring argument starts at offset 'argOffset' of the specified bit
     * string and has a length of 'argLength'.
     * 
     * The length of the operation is equal to the smaller of the length of this
     * Field or the length of the substring argument.
     * 
     * @param thisField Field of this {@code BitString}
     * @param arg       bit string argument
     * @param argOffset the start of the argument substring
     * @param argLength the length of the argument substring
     * @return this {@code BitString} with the results of the operation
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisField.offset() > 0 && thisField.offset() >= this.length()}
     *                                         or
     *                                         {@code argOffset < 0 || argOffset > 0 && argOffset >= arg.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisField.length() > this.length() - thisField.offset()}
     *                                         or
     *                                         {@code argLength < 0 || argLength > arg.length() - argOffset}
     */
    public BitString xor(Field thisField, BitString arg, int argOffset, int argLength) {
        return xor(thisField.offset(), thisField.length(), arg, argOffset, argLength);
    }
    
    /**
     * Performs a logical <b>XOR</b> of a Field of this {@code BitString} with a
     * Field of the specified bit string (arg).
     * 
     * This {@code BitString} is modified by this operation (see
     * {@link #xor(BitString)}).
     * 
     * The length of the operation is equal to the smaller of the length of this
     * Field or the length of the specified Field.
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
        return xor(thisField.offset(), thisField.length(), arg, argField.offset(), argField.length());
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
     * Copy bits from a substring of the specified bit string (that) into this
     * {@code BitString}.
     * 
     * The bits are copied from front to back. if that substring is shorter in
     * length than this {@code BitString}, this {@code BitString} is padded (on the
     * right) with {@code ZEROS}. If that substring is longer in length than this
     * {@code BitString}, the copy is truncated to fit this {@code BitString}.
     * 
     * That substring starts at offset 'thatOffset' of the specified bit string and
     * extends to the end of the specified bit string.
     * 
     * @param that       the bit string to copy
     * @param thatOffset the start of the that substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thatOffset < 0 || thatOffset > 0 && thatOffset >= that.length()}
     */
    public BitString copyFrom(BitString that, int thatOffset) {
        that.checkArgOffset(thatOffset);
        iCopyFromFrontOf(0, this.length(), that, thatOffset, that.length() - thatOffset);
        return this;
    }
    
    /**
     * Copy bits from a substring of the specified bit string (that) into this
     * {@code BitString}.
     * 
     * The bits are copied from front to back. if that substring is shorter in
     * length than this {@code BitString}, this {@code BitString} is padded (on the
     * right) with {@code ZEROS}. If that substring is longer in length than this
     * {@code BitString}, the copy is truncated to fit this {@code BitString}.
     * 
     * That substring starts at offset 'thatOffset' of the specified bit string and
     * has a length of 'thatLength'.
     * 
     * @param that       the bit string to copy
     * @param thatOffset the start of the that substring
     * @param thatLength the length of the that substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thatOffset < 0 || thatOffset > 0 && thatOffset >= that.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thatLength < 0 || thatLength > that.length() - thatOffset}
     */
    public BitString copyFrom(BitString that, int thatOffset, int thatLength) {
        that.checkArgOffset(thatOffset);
        that.checkArgLength(thatOffset, thatLength);
        iCopyFromFrontOf(0, this.length(), that, thatOffset, thatLength);
        return this;
    }
    
    /**
     * Copy bits from a Field of the specified bit string (that) into this
     * {@code BitString}.
     * 
     * The bits are copied from front to back. if that Field is shorter in length
     * than this {@code BitString}, this {@code BitString} is padded (on the right)
     * with {@code ZEROS}. If that Field is longer in length than this
     * {@code BitString}, the copy is truncated to fit this {@code BitString}.
     * 
     * @param that      the bit string to copy
     * @param thatField a Field of the that bit string
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thatField.offset() > 0 && thatField.offset() >= that.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thatField.length() > that.length() - thatField.offset()}
     */
    public BitString copyFrom(BitString that, Field thatField) {
        return copyFrom(that, thatField.offset(), thatField.length());
    }
    
    /**
     * Copy bits from the specified bit string (that) into a substring of this
     * {@code BitString}.
     * 
     * The bits are copied from front to back. if that bit string is shorter in
     * length than this substring, this substring is padded (on the right) with
     * {@code ZEROS}. If that bit string is longer in length than this substring,
     * the copy is truncated to fit this substring.
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * extends to the end of this {@code BitString}.
     * 
     * @param thisOffset the start of this substring
     * @param that       the bit string to copy
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     */
    public BitString copyFrom(int thisOffset, BitString that) {
        checkThisOffset(thisOffset);
        iCopyFromFrontOf(thisOffset, this.length() - thisOffset, that, 0, that.length());
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
     * extends to the end of this {@code BitString}.
     * 
     * That substring starts at offset 'thatOffset' of the specified bit string and
     * extends to the end of the specified bit string.
     * 
     * @param thisOffset the start of this substring
     * @param that       the bit string to copy
     * @param thatOffset the start of the that substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code thatOffset < 0 || thatOffset > 0 && thatOffset >= that.length()}
     */
    public BitString copyFrom(int thisOffset, BitString that, int thatOffset) {
        checkThisOffset(thisOffset);
        that.checkArgOffset(thatOffset);
        iCopyFromFrontOf(thisOffset, this.length() - thisOffset, that, thatOffset, that.length() - thatOffset);
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
     * extends to the end of this {@code BitString}.
     * 
     * That substring starts at offset 'thatOffset' of the specified bit string and
     * has a length of 'thatLength'.
     * 
     * @param thisOffset the start of this substring
     * @param that       the bit string to copy
     * @param thatOffset the start of the that substring
     * @param thatLength the length of the that substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code thatOffset < 0 || thatOffset > 0 && thatOffset >= that.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thatLength < 0 || thatLength > that.length() - thatOffset}
     */
    public BitString copyFrom(int thisOffset, BitString that, int thatOffset, int thatLength) {
        checkThisOffset(thisOffset);
        that.checkArgOffset(thatOffset);
        that.checkArgLength(thatOffset, thatLength);
        iCopyFromFrontOf(thisOffset, this.length() - thisOffset, that, thatOffset, thatLength);
        return this;
    }
    
    /**
     * Copy bits from a Field of the specified bit string (that) into a substring of
     * this {@code BitString}.
     * 
     * The bits are copied from front to back. if that Field is shorter in length
     * than this substring, this substring is padded (on the right) with
     * {@code ZEROS}. If that Field is longer in length than this substring, the
     * copy is truncated to fit this substring.
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * extends to the end of this {@code BitString}.
     * 
     * @param thisOffset the start of this substring
     * @param that       the bit string to copy
     * @param thatField  a Field of that bit string
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code thatField.offset() > 0 && thatField.offset() >= that.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thatField.length() > that.length() - thatField.offset()}
     */
    public BitString copyFrom(int thisOffset, BitString that, Field thatField) {
        return copyFrom(thisOffset, that, thatField.offset(), thatField.length());
    }
    
    /**
     * Copy bits from the specified bit string (that) into a substring of this
     * {@code BitString}.
     * 
     * The bits are copied from front to back. if that bit string is shorter in
     * length than this substring, this substring is padded (on the right) with
     * {@code ZEROS}. If that bit string is longer in length than this substring,
     * the copy is truncated to fit this substring.
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * has a length of 'thisLength'.
     * 
     * @param thisOffset the start of this substring
     * @param thisLength the length of this substring
     * @param that       the bit string to copy
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisLength < 0 || thisLength > this.length() - thisOffset}
     */
    public BitString copyFrom(int thisOffset, int thisLength, BitString that) {
        checkThisOffset(thisOffset);
        checkThisLength(thisOffset, thisLength);
        iCopyFromFrontOf(thisOffset, thisLength, that, 0, that.length());
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
     * extends to the end of the specified bit string.
     * 
     * @param thisOffset the start of this substring
     * @param thisLength the length of this substring
     * @param that       the bit string to copy
     * @param thatOffset the start of the that substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code thatOffset < 0 || thatOffset > 0 && thatOffset >= that.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisLength < 0 || thisLength > this.length() - thisOffset}
     */
    public BitString copyFrom(int thisOffset, int thisLength, BitString that, int thatOffset) {
        checkThisOffset(thisOffset);
        checkThisLength(thisOffset, thisLength);
        that.checkArgOffset(thatOffset);
        iCopyFromFrontOf(thisOffset, thisLength, that, thatOffset, that.length() - thatOffset);
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
     * Copy bits from a Field of the specified bit string (that) into a substring of
     * this {@code BitString}.
     * 
     * The bits are copied from front to back. if that Field is shorter in length
     * than this substring, this substring is padded (on the right) with
     * {@code ZEROS}. If that Field is longer in length than this substring, the
     * copy is truncated to fit this substring.
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * has a length of 'thisLength'.
     * 
     * @param thisOffset the start of this substring
     * @param thisLength the length of this substring
     * @param that       the bit string to copy
     * @param thatField  a Field of that substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code thatField.offset() > 0 && thatField.offset() >= that.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisLength < 0 || thisLength > this.length() - thisOffset}
     *                                         or
     *                                         {@code thatField.length() > that.length() - thatField.offset()}
     */
    public BitString copyFrom(int thisOffset, int thisLength, BitString that, Field thatField) {
        return copyFrom(thisOffset, thisLength, that, thatField.offset(), thatField.length());
    }
    
    /**
     * Copy bits from the specified bit string (that) into a Field of this
     * {@code BitString}.
     * 
     * The bits are copied from front to back. if that bit string is shorter in
     * length than this Field, this Field is padded (on the right) with
     * {@code ZEROS}. If that bit string is longer in length than this Field, the
     * copy is truncated to fit this Field.
     * 
     * @param thisField a Field of this {@code BitString}
     * @param that      the bit string to copy
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisField.offset() > 0 && thisField.offset() >= this.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisField.length() > this.length() - thisField.offset()}
     */
    public BitString copyFrom(Field thisField, BitString that) {
        return copyFrom(thisField.offset(), thisField.length(), that);
    }
    
    /**
     * Copy bits from a substring of the specified bit string (that) into a Field of
     * this {@code BitString}.
     * 
     * The bits are copied from front to back. if that substring is shorter in
     * length than this Field, this Field is padded (on the right) with
     * {@code ZEROS}. If that substring is longer in length than this Field, the
     * copy is truncated to fit this Field.
     * 
     * That substring starts at offset 'thatOffset' of the specified bit string and
     * extends to the end of the specified bit string.
     * 
     * @param thisField  a Field of this {@code BitString}
     * @param that       the bit string to copy
     * @param thatOffset the start of the that substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisField.offset() > 0 && thisField.offset() >= this.length()}
     *                                         or
     *                                         {@code thatOffset < 0 || thatOffset > 0 && thatOffset >= that.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisField.length() > this.length() - thisField.offset()}
     */
    public BitString copyFrom(Field thisField, BitString that, int thatOffset) {
        return copyFrom(thisField.offset(), thisField.length(), that, thatOffset);
    }
    
    /**
     * Copy bits from a substring of the specified bit string (that) into a Field of
     * this {@code BitString}.
     * 
     * The bits are copied from front to back. if that substring is shorter in
     * length than this Field, this Field is padded (on the right) with
     * {@code ZEROS}. If that substring is longer in length than this Field, the
     * copy is truncated to fit this Field.
     * 
     * That substring starts at offset 'thatOffset' of the specified bit string and
     * has a length of 'thatLength'.
     * 
     * @param thisField  a Field of this {@code BitString}
     * @param that       the bit string to copy
     * @param thatOffset the start of the that substring
     * @param thatLength the length of the that substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisField.offset() > 0 && thisField.offset() >= this.length()}
     *                                         or
     *                                         {@code thatOffset < 0 || thatOffset > 0 && thatOffset >= that.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisField.length() > this.length() - thisField.offset()}
     *                                         or
     *                                         {@code thatLength < 0 || thatLength > that.length() - thatOffset}
     */
    public BitString copyFrom(Field thisField, BitString that, int thatOffset, int thatLength) {
        return copyFrom(thisField.offset(), thisField.length(), that, thatOffset, thatLength);
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
        return copyFrom(thisField.offset(), thisField.length(), that, thatField.offset(), thatField.length());
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
     * into this {@code BitString}.
     * 
     * The bits are copied from back to front. if that substring is shorter in
     * length than this {@code BitString}, this {@code BitString} is padded (on the
     * left) with {@code ZEROS}. If that substring is longer in length than this
     * {@code BitString}, the copy is truncated to fit this {@code BitString}.
     * 
     * That substring starts at offset 'thatOffset' of the specified bit string and
     * extends to the end of the specified bit string.
     * 
     * @param that       the bit string to copy
     * @param thatOffset the start of the that substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thatOffset < 0 || thatOffset > 0 && thatOffset >= that.length()}
     */
    public BitString copyFromBackOf(BitString that, int thatOffset) {
        that.checkArgOffset(thatOffset);
        iCopyFromBackOf(0, this.length(), that, thatOffset, that.length() - thatOffset);
        return this;
    }
    
    /**
     * Copy bits from the back of a substring of the specified bit string (that)
     * into this {@code BitString}.
     * 
     * The bits are copied from back to front. if that substring is shorter in
     * length than this {@code BitString}, this {@code BitString} is padded (on the
     * left) with {@code ZEROS}. If that substring is longer in length than this
     * {@code BitString}, the copy is truncated to fit this {@code BitString}.
     * 
     * That substring starts at offset 'thatOffset' of the specified bit string and
     * has a length of 'thatLength'.
     * 
     * @param that       the bit string to copy
     * @param thatOffset the start of the that substring
     * @param thatLength the length of the that substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thatOffset < 0 || thatOffset > 0 && thatOffset >= that.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thatLength < 0 || thatLength > that.length() - thatOffset}
     */
    public BitString copyFromBackOf(BitString that, int thatOffset, int thatLength) {
        that.checkArgOffset(thatOffset);
        that.checkArgLength(thatOffset, thatLength);
        iCopyFromBackOf(0, this.length(), that, thatOffset, thatLength);
        return this;
    }
    
    /**
     * Copy bits from the back of a Field of the specified bit string (that) into
     * this {@code BitString}.
     * 
     * The bits are copied from back to front. if that Field is shorter in length
     * than this {@code BitString}, this {@code BitString} is padded (on the left)
     * with {@code ZEROS}. If that Field is longer in length than this
     * {@code BitString}, the copy is truncated to fit this {@code BitString}.
     * 
     * @param that      the bit string to copy
     * @param thatField a Field of that bit string
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thatField.offset() > 0 && thatField.offset() >= that.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thatField.length() > that.length() - thatField.offset()}
     */
    public BitString copyFromBackOf(BitString that, Field thatField) {
        return copyFromBackOf(that, thatField.offset(), thatField.length());
    }
    
    /**
     * Copy bits from the back of the specified bit string (that) into a substring
     * of this {@code BitString}.
     * 
     * The bits are copied from back to front. if that nit string is shorter in
     * length than this substring, this substring is padded (on the left) with
     * {@code ZEROS}. If that bit string is longer in length than this substring,
     * the copy is truncated to fit this substring.
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * extends to the end of this {@code BitString}.
     * 
     * @param thisOffset the start of this substring
     * @param that       the bit string to copy
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     */
    public BitString copyFromBackOf(int thisOffset, BitString that) {
        checkThisOffset(thisOffset);
        iCopyFromBackOf(thisOffset, this.length() - thisOffset, that, 0, that.length());
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
     * extends to the end of this {@code BitString}.
     * 
     * That substring starts at offset 'thatOffset' of the specified bit string and
     * extends to the end of the specified bit string.
     * 
     * @param thisOffset the start of this substring
     * @param that       the bit string to copy
     * @param thatOffset the start of the that substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code thatOffset < 0 || thatOffset > 0 && thatOffset >= that.length()}
     */
    public BitString copyFromBackOf(int thisOffset, BitString that, int thatOffset) {
        checkThisOffset(thisOffset);
        that.checkArgOffset(thatOffset);
        iCopyFromBackOf(thisOffset, this.length() - thisOffset, that, thatOffset, that.length() - thatOffset);
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
     * extends to the end of this {@code BitString}.
     * 
     * That substring starts at offset 'thatOffset' of the specified bit string and
     * has a length of 'thatLength'.
     * 
     * @param thisOffset the start of this substring
     * @param that       the bit string to copy
     * @param thatOffset the start of the that substring
     * @param thatLength the length of the that substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code thatOffset < 0 || thatOffset > 0 && thatOffset >= that.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thatLength < 0 || thatLength > that.length() - thatOffset}
     */
    public BitString copyFromBackOf(int thisOffset, BitString that, int thatOffset, int thatLength) {
        checkThisOffset(thisOffset);
        that.checkArgOffset(thatOffset);
        that.checkArgLength(thatOffset, thatLength);
        iCopyFromBackOf(thisOffset, this.length() - thisOffset, that, thatOffset, thatLength);
        return this;
    }
    
    /**
     * Copy bits from the back of a Field of the specified bit string (that) into a
     * substring of this {@code BitString}.
     * 
     * The bits are copied from back to front. if that Field is shorter in length
     * than this substring, this substring is padded (on the left) with
     * {@code ZEROS}. If that Field is longer in length than this substring, the
     * copy is truncated to fit this substring.
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * extends to the end of this {@code BitString}.
     * 
     * @param thisOffset the start of this substring
     * @param that       the bit string to copy
     * @param thatField  a Field of that bit string
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code thatField.offset() > 0 && thatField.offset() >= that.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thatField.length() > that.length() - thatField.offset()}
     */
    public BitString copyFromBackOf(int thisOffset, BitString that, Field thatField) {
         return copyFromBackOf(thisOffset, that, thatField.offset(), thatField.length());
    }
    
    /**
     * Copy bits from the back of the specified bit string (that) into a substring
     * of this {@code BitString}.
     * 
     * The bits are copied from back to front. if that bit string is shorter in
     * length than this substring, this substring is padded (on the left) with
     * {@code ZEROS}. If that bit stringg is longer in length than this substring,
     * the copy is truncated to fit this substring.
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * has a length of 'thisLength'.
     * 
     * @param thisOffset the start of this substring
     * @param thisLength the length of this substring
     * @param that       the bit string to copy
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisLength < 0 || thisLength > this.length() - thisOffset}
     */
    public BitString copyFromBackOf(int thisOffset, int thisLength, BitString that) {
        checkThisOffset(thisOffset);
        checkThisLength(thisOffset, thisLength);
        iCopyFromBackOf(thisOffset, thisLength, that, 0, that.length());
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
     * extends to the end of the specified bit string.
     * 
     * @param thisOffset the start of this substring
     * @param thisLength the length of this substring
     * @param that       the bit string to copy
     * @param thatOffset the start of the that substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code thatOffset < 0 || thatOffset > 0 && thatOffset >= that.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisLength < 0 || thisLength > this.length() - thisOffset}
     */
    public BitString copyFromBackOf(int thisOffset, int thisLength, BitString that, int thatOffset) {
        checkThisOffset(thisOffset);
        checkThisLength(thisOffset, thisLength);
        that.checkArgOffset(thatOffset);
        iCopyFromBackOf(thisOffset, thisLength, that, thatOffset, that.length() - thatOffset);
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
     * substring of this {@code BitString}.
     * 
     * The bits are copied from back to front. if that Field is shorter in length
     * than this substring, this substring is padded (on the left) with
     * {@code ZEROS}. If that Field is longer in length than this substring, the
     * copy is truncated to fit this substring.
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * has a length of 'thisLength'.
     * 
     * @param thisOffset the start of this substring
     * @param thisLength the length of this substring
     * @param that       the bit string to copy
     * @param thatField  a Field of that bit string
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code thatField.offset() > 0 && thatField.offset() >= that.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisLength < 0 || thisLength > this.length() - thisOffset}
     *                                         or
     *                                         {@code thatField.length() > that.length() - thatField.offset()}
     */
    public BitString copyFromBackOf(int thisOffset, int thisLength, BitString that, Field thatField) {
        return copyFromBackOf(thisOffset, thisLength, that, thatField.offset(), thatField.length());
    }
    
    /**
     * Copy bits from the back of the specified bit string (that) into a Field of
     * this {@code BitString}.
     * 
     * The bits are copied from back to front. if that bit string is shorter in
     * length than this Field, this Field is padded (on the left) with
     * {@code ZEROS}. If that bit string is longer in length than this Field, the
     * copy is truncated to fit this Field.
     * 
     * @param thisField a Field of this {@code BitString}
     * @param that      the bit string to copy
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisField.offset() > 0 && thisField.offset() >= this.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisField.length() > this.length() - thisField.offset()}
     */
    public BitString copyFromBackOf(Field thisField, BitString that) {
        return copyFromBackOf(thisField.offset(), thisField.length(), that);
    }
    
    /**
     * Copy bits from the back of a substring of the specified bit string (that)
     * into a Field of this {@code BitString}.
     * 
     * The bits are copied from back to front. if that substring is shorter in
     * length than this Field, this Field is padded (on the left) with
     * {@code ZEROS}. If that substring is longer in length than this Field, the
     * copy is truncated to fit this Field.
     * 
     * That substring starts at offset 'thatOffset' of the specified bit string and
     * extends to the end of the specified bit string.
     * 
     * @param thisField  a Field of this {@code BitString}
     * @param that       the bit string to copy
     * @param thatOffset the start of the that substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisField.offset() > 0 && thisField.offset() >= this.length()}
     *                                         or
     *                                         {@code thatOffset < 0 || thatOffset > 0 && thatOffset >= that.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisField.length() > this.length() - thisField.offset()}
     */
    public BitString copyFromBackOf(Field thisField, BitString that, int thatOffset) {
        return copyFromBackOf(thisField.offset(), thisField.length(), that, thatOffset);
    }
    
    /**
     * Copy bits from the back of a substring of the specified bit string (that)
     * into a Field of this {@code BitString}.
     * 
     * The bits are copied from back to front. if that substring is shorter in
     * length than this Field, this Field is padded (on the left) with
     * {@code ZEROS}. If that substring is longer in length than this Field, the
     * copy is truncated to fit this Field.
     * 
     * That substring starts at offset 'thatOffset' of the specified bit string and
     * has a length of 'thatLength'.
     * 
     * @param thisField  a Field of this {@code BitString}
     * @param that       the bit string to copy
     * @param thatOffset the start of the that substring
     * @param thatLength the length of the that substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisField.offset() > 0 && thisField.offset() >= this.length()}
     *                                         or
     *                                         {@code thatOffset < 0 || thatOffset > 0 && thatOffset >= that.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisField.length() > this.length() - thisField.offset()}
     *                                         or
     *                                         {@code thatLength < 0 || thatLength > that.length() - thatOffset}
     */
    public BitString copyFromBackOf(Field thisField, BitString that, int thatOffset, int thatLength) {
        return copyFromBackOf(thisField.offset(), thisField.length(), that, thatOffset, thatLength);
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
        return copyFromBackOf(thisField.offset(), thisField.length(), that, thatField.offset(), thatField.length());
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
     * Returns {@code true} if this {@code BitString} and a substring of the
     * specified bit string (that) are equal. This {@code BitString} and that
     * substring are equal if and only if both this {@code BitString} and that
     * substring have the same length, and both have the same set of bits set to
     * {@code ONE}. That is, for every nonnegative {@code int} offset {@code k} less
     * than the length of the comparison,
     * 
     * <pre>
     * this.getBit(k) == that.getBit(k, thatOffset)
     * </pre>
     * 
     * must be true.
     * 
     * That substring starts at offset 'thatOffset' of the specified bit string and
     * extends to the end of the specified bit string.
     * 
     * @param that       the bit string to compare against
     * @param thatOffset the start of the that substring
     * @return {@code true} if this substring and that substring are equal
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thatOffset < 0 || thatOffset > 0 && thatOffset >= that.length()}
     */
    public boolean equals(BitString that, int thatOffset) {
        if (that == null) return false;
        that.checkArgOffset(thatOffset);
        if (this.length() != (that.length() - thatOffset)) return false;
        return iEquals(0, this.length(), that, thatOffset);
    }
    
    /**
     * Returns {@code true} if this {@code BitString} and a substring of the
     * specified bit string (that) are equal. This {@code BitString} and that
     * substring are equal if and only if both this {@code BitString} and that
     * substring have the same length, and both have the same set of bits set to
     * {@code ONE}. That is, for every nonnegative {@code int} offset {@code k} less
     * than the length of the comparison,
     * 
     * <pre>
     * this.getBit(k) == that.getBit(k, thatOffset, thatLength)
     * </pre>
     * 
     * must be true.
     * 
     * That substring starts at offset 'thatOffset' of the specified bit string and
     * has a length of 'thatLength'.
     * 
     * @param that       the bit string to compare against
     * @param thatOffset the start of the that substring
     * @param thatLength the length of the that substring
     * @return {@code true} if this substring and that substring are equal
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thatOffset < 0 || thatOffset > 0 && thatOffset >= that.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thatLength < 0 || thatLength > that.length() - thatOffset}
     */
    public boolean equals(BitString that, int thatOffset, int thatLength) {
        if (that == null) return false;
        that.checkArgOffset(thatOffset);
        that.checkArgLength(thatOffset, thatLength);
        if (this.length() != thatLength) return false;
        return iEquals(0, this.length(), that, thatOffset);
    }
    
    
    /**
     * Returns {@code true} if this {@code BitString} and a Field of the
     * specified bit string (that) are equal. This {@code BitString} and that
     * field are equal if and only if both this {@code BitString} and that
     * field have the same length, and both have the same set of bits set to
     * {@code ONE}. That is, for every nonnegative {@code int} offset {@code k} less
     * than the length of the comparison,
     * 
     * <pre>
     * this.getBit(k) == that.getBit(k, field)
     * </pre>
     * 
     * must be true.
     * 
     * @param that       the bit string to compare against
     * @param thatField a Field of that bit string
     * @return {@code true} if this substring and that field are equal
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thatField.offset() > 0 && thatField.offset() >= that.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thatField.length() > that.length() - thatField.offset()}
     */
    public boolean equals(BitString that, Field thatField) {
        return equals(that, thatField.offset(), thatField.length());
    } 
    
    /**
     * Returns {@code true} if a substring of this {@code BitString} and the
     * specified bit string (that) are equal. This substring and that bit string are
     * equal if and only if both this substring and that bit string have the the
     * same length, and both have the same set of bits set to {@code ONE}. That is,
     * for every nonnegative {@code int} offset {@code k} less than the length of
     * the comparison,
     * 
     * <pre>
     * this.getBit(k, thisOffset) == that.getBit(k)
     * </pre>
     * 
     * must be true.
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * extends to the end of this {@code BitString}.
     * 
     * @param thisOffset the start of this substring
     * @param that       the bit string to compare against
     * @return {@code true} if this substring and that bit string are equal
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     */
    public boolean equals(int thisOffset, BitString that) {
        checkThisOffset(thisOffset);
        if (that == null) return false;
        if ((this.length() - thisOffset) != that.length()) return false;
        return iEquals(thisOffset, this.length(), that, 0);
    }
    
    /**
     * Returns {@code true} if a substring of this {@code BitString} and a substring
     * of the specified bit string (that) are equal. The two substrings are equal if
     * and only if both substrings have the the same length, and both substrings
     * have the same set of bits set to {@code ONE}. That is, for every nonnegative
     * {@code int} offset {@code k} less than the length of the comparison,
     * 
     * <pre>
     * this.getBit(k, thisOffset) == that.getBit(k, thatOffset)
     * </pre>
     * 
     * must be true.
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * extends to the end of this {@code BitString}.
     * 
     * That substring starts at offset 'thatOffset' of the specified bit string and
     * extends to the end of the specified bit string.
     * 
     * @param thisOffset the start of this substring
     * @param that       the bit string to compare against
     * @param thatOffset the start of the that substring
     * @return {@code true} if this substring and that substring are equal
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code thatOffset < 0 || thatOffset > 0 && thatOffset >= that.length()}
     */
    public boolean equals(int thisOffset, BitString that, int thatOffset) {
        checkThisOffset(thisOffset);
        if (that == null) return false;
        that.checkArgOffset(thatOffset);
        if ((this.length() - thisOffset) != (that.length() - thatOffset)) return false;
        return iEquals(thisOffset, this.length(), that, thatOffset);
    }
    
    /**
     * Returns {@code true} if a substring of this {@code BitString} and a substring
     * of the specified bit string (that) are equal. The two substrings are equal if
     * and only if both substrings have the the same length, and both substrings have
     * the same set of bits set to {@code ONE}. That is, for every nonnegative
     * {@code int} offset {@code k} less than the length of the comparison,
     * 
     * <pre>
     * this.getBit(k, thisOffset) == that.getBit(k, thatOffset, thatLength)
     * </pre>
     * 
     * must be true.
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * extends to the end of this {@code BitString}.
     * 
     * That substring starts at offset 'thatOffset' of the specified bit string and
     * has a length of 'thatLength'.
     * 
     * @param thisOffset the start of this substring
     * @param that       the bit string to compare against
     * @param thatOffset the start of the that substring
     * @param thatLength the length of the that substring
     * @return {@code true} if this substring and that substring are equal
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code thatOffset < 0 || thatOffset > 0 && thatOffset >= that.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thatLength < 0 || thatLength > that.length() - thatOffset}
     */
    public boolean equals(int thisOffset, BitString that, int thatOffset, int thatLength) {
        checkThisOffset(thisOffset);
        if (that == null) return false;
        that.checkArgOffset(thatOffset);
        that.checkArgLength(thatOffset, thatLength);
        if ((this.length() - thisOffset) != thatLength) return false;
        return iEquals(thisOffset, this.length(), that, thatOffset);
    }
    
    /**
     * Returns {@code true} if a substring of this {@code BitString} and a Field of
     * the specified bit string (that) are equal. This substring and that field are
     * equal if and only if both this substring and that field have the the same
     * length, and both have the same set of bits set to {@code ONE}. That is, for
     * every nonnegative {@code int} offset {@code k} less than the length of the
     * comparison,
     * 
     * <pre>
     * this.getBit(k, thisOffset) == that.getBit(k, thatield)
     * </pre>
     * 
     * must be true.
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * extends to the end of this {@code BitString}.
     * 
     * @param thisOffset the start of this substring
     * @param that       the bit string to compare against
     * @param thatField  a Field of that bit string
     * @return {@code true} if this substring and that field are equal
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code thatField.offset() > 0 && thatField.offset() >= that.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thatField.length() > that.length() - thatField.offset()}
     */
    public boolean equals(int thisOffset, BitString that, Field thatField) {
        return equals(thisOffset, that, thatField.offset(), thatField.length());
    }
    
    /**
     * Returns {@code true} if a substring of this {@code BitString} and the
     * specified bit string (that) are equal. This substring and that bit string are
     * equal if and only if both this substring and that bit string have the same
     * length, and both have the same set of bits set to {@code ONE}. That is, for
     * every nonnegative {@code int} offset {@code k} less than the length of the
     * comparison,
     * 
     * <pre>
     * this.getBit(k, thisOffset, thisLength) == that.getBit(k)
     * </pre>
     * 
     * must be true.
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * has a length of 'thisLength'.
     * 
     * @param thisOffset the start of this substring
     * @param thisLength the length of this substring
     * @param that       the bit string to compare against
     * @return {@code true} if this substring and that bit string are equal
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisLength < 0 || thisLength > this.length() - thisOffset}
     */
    public boolean equals(int thisOffset, int thisLength, BitString that) {
        checkThisOffset(thisOffset);
        checkThisLength(thisOffset, thisLength);
        if (that == null) return false;
        if (thisLength != that.length()) return false;
        return iEquals(thisOffset, thisLength, that, 0);
    }
    
    /**
     * Returns {@code true} if a substring of this {@code BitString} and a substring
     * of the specified bit string (that) are equal. The two substrings are equal if
     * and only if both substrings have the the same length, and both substrings have
     * the same set of bits set to {@code ONE}. That is, for every nonnegative
     * {@code int} offset {@code k} less than the length of the comparison,
     * 
     * <pre>
     * this.getBit(k, thisOffset, thisLength) == that.getBit(k, thatOffset)
     * </pre>
     * 
     * must be true.
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * has a length of 'thisLength'.
     * 
     * That substring starts at offset 'thatOffset' of the specified bit string and
     * extends to the end of the specified bit string.
     * 
     * @param thisOffset the start of this substring
     * @param thisLength the length of this substring
     * @param that       the bit string to compare against
     * @param thatOffset the start of the that substring
     * @return {@code true} if this substring and that substring are equal
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code thatOffset < 0 || thatOffset > 0 && thatOffset >= that.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisLength < 0 || thisLength > this.length() - thisOffset}
     */
    public boolean equals(int thisOffset, int thisLength, BitString that, int thatOffset) {
        checkThisOffset(thisOffset);
        checkThisLength(thisOffset, thisLength);
        if (that == null) return false;
        that.checkArgOffset(thatOffset);
        if (thisLength != (that.length() - thatOffset)) return false;
        return iEquals(thisOffset, thisLength, that, thatOffset);
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
     * Returns {@code true} if a substring of this {@code BitString} and a Field of
     * the specified bit string (that) are equal. This substring and that field are
     * equal if and only if both this substring and that field have the the same
     * length, and both have the same set of bits set to {@code ONE}. That is, for
     * every nonnegative {@code int} offset {@code k} less than the length of the
     * comparison,
     * 
     * <pre>
     * this.getBit(k, thisOffset, thisLength) == that.getBit(k, thatField)
     * </pre>
     * 
     * must be true.
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * has a length of 'thisLength'.
     * 
     * @param thisOffset the start of this substring
     * @param thisLength the length of this substring
     * @param that       the bit string to compare against
     * @param thatField  a Field of that bit string
     * @return {@code true} if this substring and that substring are equal
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code thatField.offset() > 0 && thatField.offset() >= that.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisLength < 0 || thisLength > this.length() - thisOffset}
     *                                         or
     *                                         {@code thatField.length() > that.length() - thatField.offset()}
     */
    public boolean equals(int thisOffset, int thisLength, BitString that, Field thatField) {
        return equals(thisOffset, thisLength, that, thatField.offset(), thatField.length());
    }
    
    /**
     * Returns {@code true} if a Field of this {@code BitString} and the specified
     * bit string (that) are equal. This field and that bit string are equal if and
     * only if both this field and that bit string have the the same length, and
     * both have the same set of bits set to {@code ONE}. That is, for every
     * nonnegative {@code int} offset {@code k} less than the length of the
     * comparison,
     * 
     * <pre>
     * this.getBit(k, thisField) == that.getBit(k)
     * </pre>
     * 
     * must be true.
     * 
     * That substring starts at offset 'thatOffset' of the specified bit string and
     * extends to the end of the specified bit string.
     * 
     * @param thisField a Field of this {@code BitString}
     * @param that      the bit string to compare against
     * @return {@code true} if this field and that bit string are equal
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisField.offset() > 0 && thisField.offset() >= this.length()}
     *                                         or
     *                                         {@code thatOffset < 0 || thatOffset > 0 && thatOffset >= that.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisField.length() > this.length() - thisField.offset()}
     */
    public boolean equals(Field thisField, BitString that) {
        return equals(thisField.offset(), thisField.length(), that);
    }
    
    /**
     * Returns {@code true} if a Field of this {@code BitString} and a substring of
     * the specified bit string (that) are equal. This field and that substring are
     * equal if and only if both this field and that substring have the the same
     * length, and both have the same set of bits set to {@code ONE}. That is, for
     * every nonnegative {@code int} offset {@code k} less than the length of the
     * comparison,
     * 
     * <pre>
     * this.getBit(k, thisField) == that.getBit(k, thatOffset)
     * </pre>
     * 
     * must be true.
     * 
     * That substring starts at offset 'thatOffset' of the specified bit string and
     * extends to the end of the specified bit string.
     * 
     * @param thisField  a Field of this {@code BitString}
     * @param that       the bit string to compare against
     * @param thatOffset the start of the that substring
     * @return {@code true} if this field and that substring are equal
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisField.offset() > 0 && thisField.offset() >= this.length()}
     *                                         or
     *                                         {@code thatOffset < 0 || thatOffset > 0 && thatOffset >= that.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisField.length() > this.length() - thisField.offset()}
     */
    public boolean equals(Field thisField, BitString that, int thatOffset) {
        return equals(thisField.offset(), thisField.length(), that, thatOffset);
    }
    
    /**
     * Returns {@code true} if a Field of this {@code BitString} and a substring of
     * the specified bit string (that) are equal. This field and that substring are
     * equal if and only if both this field and that substring have the the same
     * length, and both have the same set of bits set to {@code ONE}. That is, for
     * every nonnegative {@code int} offset {@code k} less than the length of the
     * comparison,
     * 
     * <pre>
     * this.getBit(k, thisField) == that.getBit(k, thatOffset, thatLength)
     * </pre>
     * 
     * must be true.
     * 
     * That substring starts at offset 'thatOffset' of the specified bit string and
     * has a length of 'thatLength'.
     * 
     * @param thisField  a Field of this {@code BitString}
     * @param that       the bit string to compare against
     * @param thatOffset the start of the that substring
     * @param thatLength the length of the that substring
     * @return {@code true} if this field and that substring are equal
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisField.offset() > 0 && thisField.offset() >= this.length()}
     *                                         or
     *                                         {@code thatOffset < 0 || thatOffset > 0 && thatOffset >= that.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisField.length() > this.length() - thisField.offset()}
     *                                         or
     *                                         {@code thatLength < 0 || thatLength > that.length() - thatOffset}
     */
    public boolean equals(Field thisField, BitString that, int thatOffset, int thatLength) {
        return equals(thisField.offset(), thisField.length(), that, thatOffset, thatLength);
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
        return equals(thisField.offset(), thisField.length(), that, thatField.offset(), thatField.length());
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
     * Returns {@code true} if this {@code BitString} intersects with a substring of
     * the specified bit string (that). This {@code BitString} and that substring
     * intersect if they have any bits, at the same offset, that are set to
     * {@code ONE}.
     * 
     * That substring starts at offset 'thatOffset' of the specified bit string and
     * extends to the end of the specified bit string.
     * 
     * @param that       that bit string
     * @param thatOffset the start of that substring
     * @return {@code true} if this {@code BitString} intersects with that substring
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thatOffset < 0 || thatOffset > 0 && thatOffset >= that.length()}
     */
    public boolean intersects(BitString that, int thatOffset) {
        that.checkArgOffset(thatOffset);
        return iIntersects(0, Math.min(this.length(), that.length() - thatOffset), that, thatOffset);
    }
    
    /**
     * Returns {@code true} if this {@code BitString} intersects with a substring of
     * the specified bit string (that). This {@code BitString} and that substring
     * intersect if they have any bits, at the same offset, that are set to
     * {@code ONE}.
     * 
     * That substring starts at offset 'thatOffset' of the specified bit string and
     * has a length of 'thatLength'.
     * 
     * @param that       that bit string
     * @param thatOffset the start of that substring
     * @param thatLength the length of that substring
     * @return {@code true} if this {@code BitString} intersects with that substring
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thatOffset < 0 || thatOffset > 0 && thatOffset >= that.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thatLength < 0 || thatLength > that.length() - thatOffset}
     */
    public boolean intersects(BitString that, int thatOffset, int thatLength) {
        that.checkArgOffset(thatOffset);
        that.checkArgLength(thatOffset, thatLength);
        return iIntersects(0, Math.min(this.length(), thatLength), that, thatOffset);
    }
    
    /**
     * Returns {@code true} if this {@code BitString} intersects with a Field of the
     * specified bit string (that). This {@code BitString} and that field intersect
     * if they have any bits, at the same offset, that are set to {@code ONE}.
     * 
     * @param that      that bit string
     * @param thatField a Field of that bit string
     * @return {@code true} if this {@code BitString} intersects with that field
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thatField.offset() > 0 && thatField.offset() >= that.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thatLength > that.length() - thatField.offset()}
     */
    public boolean intersects(BitString that, Field thatField) {
        return intersects(that, thatField.offset(), thatField.length());
    } 
    
    /**
     * Returns {@code true} if a substring of this {@code BitString} intersects with
     * the specified bit string (that). This substring and that bit string intersect
     * if they have any bits, at the same offset, that are set to {@code ONE}.
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * extends to the end of this {@code BitString}.
     * 
     * @param thisOffset the start of this substring
     * @param that       that bit string
     * @return {@code true} if this substring intersects with that bit string
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     */
    public boolean intersects(int thisOffset, BitString that) {
        checkThisOffset(thisOffset);
        return iIntersects(thisOffset, Math.min(this.length() - thisOffset, that.length()), that, 0);
    }
    
    /**
     * Returns {@code true} if a substring of this {@code BitString} intersects with
     * a substring of the specified bit string (that). The two bit substrings
     * intersect if they have any bits, at the same offset, that are set to
     * {@code ONE}.
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * extends to the end of this {@code BitString}.
     * 
     * That substring starts at offset 'thatOffset' of the specified bit string and
     * extends to the end of that bit string.
     * 
     * @param thisOffset the start of this substring
     * @param that       that bit string
     * @param thatOffset the start of that substring
     * @return {@code true} if this substring intersects with that substring
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code thatOffset < 0 || thatOffset > 0 && thatOffset >= that.length()}
     */
    public boolean intersects(int thisOffset, BitString that, int thatOffset) {
        checkThisOffset(thisOffset);
        that.checkArgOffset(thatOffset);
        return iIntersects(thisOffset, Math.min(this.length() - thisOffset, that.length() - thatOffset), that, thatOffset);
    }
    
    /**
     * Returns {@code true} if a substring of this {@code BitString} intersects with
     * a substring of the specified bit string (that). The two bit substrings
     * intersect if they have any bits, at the same offset, that are set to
     * {@code ONE}.
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * extends to the end of this {@code BitString}.
     * 
     * That substring starts at offset 'thatOffset' of the specified bit string and
     * has a length of 'thatLength'.
     * 
     * @param thisOffset the start of this substring
     * @param that       that bit string
     * @param thatOffset the start of that substring
     * @param thatLength the length of that substring
     * @return {@code true} if this substring intersects with that substring
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code thatOffset < 0 || thatOffset > 0 && thatOffset >= that.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thatLength < 0 || thatLength > that.length() - thatOffset}
     */
    public boolean intersects(int thisOffset, BitString that, int thatOffset, int thatLength) {
        checkThisOffset(thisOffset);
        that.checkArgOffset(thatOffset);
        that.checkArgLength(thatOffset, thatLength);
        return iIntersects(thisOffset, Math.min(this.length() - thisOffset, thatLength), that, thatOffset);
    }
    
    /**
     * Returns {@code true} if a substring of this {@code BitString} intersects with
     * a substring of the specified bit string (that). The two bit substrings
     * intersect if they have any bits, at the same offset, that are set to
     * {@code ONE}.
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * extends to the end of this {@code BitString}.
     * 
     * That substring starts at offset 'thatOffset' of the specified bit string and
     * has a length of 'thatLength'.
     * 
     * @param thisOffset the start of this substring
     * @param that       that bit string
     * @param thatField  a Field of that bit string
     * @return {@code true} if this substring intersects with that field
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code thatField.offset() > 0 && thatField.offset() >= that.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thatField.length() > that.length() - thatField.offset()}
     */
    public boolean intersects(int thisOffset, BitString that, Field thatField) {
        return intersects(thisOffset, that, thatField.offset(), thatField.length());
    }
    
    /**
     * Returns {@code true} if a substring of this {@code BitString} intersects with
     * the specified bit string (that). This substring and that bit string intersect
     * if they have any bits, at the same offset, that are set to {@code ONE}.
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * has a length of 'thisLength'.
     * 
     * @param thisOffset the start of this substring
     * @param thisLength the length of this substring
     * @param that       that bit string
     * @return {@code true} if this substring intersects with that bit string
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisLength < 0 || thisLength > this.length() - thisOffset}
     */
    public boolean intersects(int thisOffset, int thisLength, BitString that) {
        checkThisOffset(thisOffset);
        checkThisLength(thisOffset, thisLength);
        return iIntersects(thisOffset, Math.min(thisLength, that.length()), that, 0);
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
     * extends to the end of that bit string.
     * 
     * @param thisOffset the start of this substring
     * @param thisLength the length of this substring
     * @param that       that bit string
     * @param thatOffset the start of that substring
     * @return {@code true} if this substring intersects with that substring
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code thatOffset < 0 || thatOffset > 0 && thatOffset >= that.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisLength < 0 || thisLength > this.length() - thisOffset}
     */
    public boolean intersects(int thisOffset, int thisLength, BitString that, int thatOffset) {
        checkThisOffset(thisOffset);
        checkThisLength(thisOffset, thisLength);
        that.checkArgOffset(thatOffset);
        return iIntersects(thisOffset, Math.min(thisLength, that.length() - thatOffset), that, thatOffset);
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
     * Returns {@code true} if a substring of this {@code BitString} intersects with
     * a Field of the specified bit string (that). this substring and that field
     * intersect if they have any bits, at the same offset, that are set to
     * {@code ONE}.
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * has a length of 'thisLength'.
     * 
     * @param thisOffset the start of this substring
     * @param thisLength the length of this substring
     * @param that       that bit string
     * @param thatField  a Field of that bit string
     * @return {@code true} if this substring intersects with that field
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code thatField.offset() > 0 && thatField.offset() >= that.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisLength < 0 || thisLength > this.length() - thisOffset}
     *                                         or
     *                                         {@code thatField.length() > that.length() - thatField.offset()}
     */
    public boolean intersects(int thisOffset, int thisLength, BitString that, Field thatField) {
        return intersects(thisOffset, thisLength, that, thatField.offset(), thatField.length());
    }
    
    /**
     * Returns {@code true} if a Field of this {@code BitString} intersects with the
     * specified bit string (that). This field and that bit string intersect if they
     * have any bits, at the same offset, that are set to {@code ONE}.
     * 
     * @param thisField a Field of this {@code BitString}
     * @param that      that bit string
     * @return {@code true} if this field intersects with that bit string
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisField.offset() > 0 && thisField.offset() >= this.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisField.length() > this.length() - thisField.offset()}
     */
    public boolean intersects(Field thisField, BitString that) {
        return intersects(thisField.offset(), thisField.length(), that);
    }
    
    /**
     * Returns {@code true} if a Field of this {@code BitString} intersects with a
     * substring of the specified bit string (that). This field and that substring
     * intersect if they have any bits, at the same offset, that are set to
     * {@code ONE}.
     * 
     * That substring starts at offset 'thatOffset' of the specified bit string and
     * extends to the end of that bit string.
     * 
     * @param thisField  a Field of this {@code BitString}
     * @param that       that bit string
     * @param thatOffset the start of that substring
     * @return {@code true} if this field intersects with that substring
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisField.offset() > 0 && thisField.offset() >= this.length()}
     *                                         or
     *                                         {@code thatOffset < 0 || thatOffset > 0 && thatOffset >= that.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisField.length() > this.length() - thisField.offset()}
     */
    public boolean intersects(Field thisField, BitString that, int thatOffset) {
        return intersects(thisField.offset(), thisField.length(), that, thatOffset);
    }
    
    /**
     * Returns {@code true} if a Field of this {@code BitString} intersects with a
     * substring of the specified bit string (that). This field and that substring
     * intersect if they have any bits, at the same offset, that are set to
     * {@code ONE}.
     * 
     * That substring starts at offset 'thatOffset' of the specified bit string and
     * has a length of 'thatLength'.
     * 
     * @param thisField  a Field of this {@code BitString}
     * @param that       that bit string
     * @param thatOffset the start of that substring
     * @param thatLength the length of that substring
     * @return {@code true} if this field intersects with that substring
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisField.offset() > 0 && thisField.offset() >= this.length()}
     *                                         or
     *                                         {@code thatOffset < 0 || thatOffset > 0 && thatOffset >= that.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisField.length() > this.length() - thisField.offset()}
     *                                         or
     *                                         {@code thatLength < 0 || thatLength > that.length() - thatOffset}
     */
    public boolean intersects(Field thisField, BitString that, int thatOffset, int thatLength) {
        return intersects(thisField.offset(), thisField.length(), that, thatOffset, thatLength);
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
        return intersects(thisField.offset(), thisField.length(), that, thatField.offset(), thatField.length());
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
     * and extends to the end of this {@code BitString}.
     * 
     * @param offset the start of the substring
     * @return the number of leading {@code ONES} of the substring
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     */
    public int numberOfLeadingOnes(int offset) {
        checkThisOffset(offset);
        return iNumberOfLeadingOnes(offset, length() - offset);
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
        return numberOfLeadingOnes(field.offset(), field.length());
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
     * and extends to the end of this {@code BitString}.
     * 
     * @param offset the start of the substring
     * @return the number of leading {@code ZEROS} of the substring
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     */
    public int numberOfLeadingZeros(int offset) {
        checkThisOffset(offset);
        return iNumberOfLeadingZeros(offset, length() - offset);
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
        return numberOfLeadingZeros(field.offset(), field.length());
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
     * and extends to the end of this {@code BitString}.
     * 
     * @param offset the start of the substring
     * @return the number of {@code ONES} in the specified substring
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     */
    public int numberOfOnes(int offset) {
        checkThisOffset(offset);
        return numberOfOnes(offset, length() - offset);
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
        return numberOfOnes(field.offset(), field.length());
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
     * and extends to the end of this {@code BitString}.
     * 
     * @param offset the start of the substring
     * @return the number of trailing {@code ONES} of the substring
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     */
    public int numberOfTrailingOnes(int offset) {
        checkThisOffset(offset);
        return iNumberOfTrailingOnes(offset, length() - offset);
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
        return numberOfTrailingOnes(field.offset(), field.length());
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
     * and extends to the end of this {@code BitString}.
     * 
     * @param offset the start of the substring
     * @return the number of trailing {@code ZEROS} of the substring
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     */
    public int numberOfTrailingZeros(int offset) {
        checkThisOffset(offset);
        return iNumberOfTrailingZeros(offset, length() - offset);
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
        return numberOfTrailingZeros(field.offset(), field.length());
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
     * and extends to the end of this {@code BitString}.
     * 
     * @param offset the start of the substring
     * @return the number of {@code ZEROS} in the specified substring
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     */
    public int numberOfZeros(int offset) {
        checkThisOffset(offset);
        return numberOfZeros(offset, length() - offset);
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
        return numberOfZeros(field.offset(), field.length());
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
     * and extends to the of this {@code BitString}.
     * 
     * @param offset the start of the substring
     * @return the offset of the first {@code ONE} bit or -1 if no bits are set to
     *         {@code ONE}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     */
    public int offsetOfFirstOne(int offset) {
        checkThisOffset(offset);
        return iOffsetOfFirstOne(offset, length() - offset);
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
        return offsetOfFirstOne(field.offset(), field.length());
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
     * and extends to the end of this {@code BitString}.
     * 
     * @param offset the start of the substring
     * @return the offset of the first {@code ZERO} bit or -1 if no bits are set to
     *         {@code ZERO}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     */
    public int offsetOfFirstZero(int offset) {
        checkThisOffset(offset);
        return iOffsetOfFirstZero(offset, length() - offset);
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
        return offsetOfFirstZero(field.offset(), field.length());
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
     * and extends to the end of this {@code BitString}.
     * 
     * @param offset the start of the substring
     * @return the offset of the last {@code ONE} bit or -1 if no bits are set to
     *         {@code ONE}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     */
    public int offsetOfLastOne(int offset) {
        checkThisOffset(offset);
        return iOffsetOfLastOne(offset, length() - offset);
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
        return offsetOfLastOne(field.offset(), field.length());
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
     * and extends to the end of this {@code BitString}.
     * 
     * @param offset the start of the substring
     * @return the offset of the last {@code ZERO} bit or -1 if no bits are set to
     *         {@code ZERO}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
     */
    public int offsetOfLastZero(int offset) {
        checkThisOffset(offset);
        return iOffsetOfLastZero(offset, length() - offset);
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
        return offsetOfLastZero(field.offset(), field.length());
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
        checkThisLength(field.offset(), field.length());
        if (startOffset == field.length()) return -1;
        checkRelativeOffset(startOffset, field.length());
        final int bitOffset = iOffsetOfFirstOne(field.offset() + startOffset, field.length() - startOffset);
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
        checkThisLength(field.offset(), field.length());
        if (startOffset == field.length()) return -1;
        checkRelativeOffset(startOffset, field.length());
        final int bitOffset = iOffsetOfFirstZero(field.offset() + startOffset, field.length() - startOffset);
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
        //if (startOffset < 0 || startOffset >= length()) throw new IllegalArgumentException();
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
        //if (startOffset < 0 || startOffset >= length() - offset) throw new IllegalArgumentException();
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
        //if (startOffset < 0 || startOffset >= length) throw new IllegalArgumentException();
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
        checkThisLength(field.offset(), field.length());
        if (startOffset == -1) return -1;
        checkRelativeOffset(startOffset, field.length());
        //if (startOffset < 0 || startOffset >= field.length()) throw new IllegalArgumentException();
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
        checkThisLength(field.offset(), field.length());
        if (startOffset == -1) return -1;
        checkRelativeOffset(startOffset, field.length());
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
        return range(field.offset(), field.length());
    }
    
    public BitString reverse() {
        iReverse(0, length());
        return this;
    }
    
    public BitString reverse(int offset) {
        checkThisOffset(offset);
        iReverse(offset, length() - offset);
        return this;
    }
    
    public BitString reverse(int offset, int length) {
        checkThisOffset(offset);
        checkThisLength(offset, length);
        iReverse(offset, length);
        return this;
    }
    
    public BitString reverse(Field field) {
        return reverse(field.offset(), field.length());
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
     * The substring starts at offset 'offset' of this {@code BitString} and extends
     * to the end of this {@code BitString}.
     * 
     * Any bits rotated out on the left are rotated back into this {@code BitString}
     * on the right.
     * 
     * @param nBits  the number of bits to rotate
     * @param offset the start of this substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= this.length()}
     */
    public BitString rotateLeft(int nBits, int offset) {
        iRotateLeft(nBits, offset, length() - offset);
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
        return rotateLeft(nBits, field.offset(), field.length());
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
     * Rotates left this {@code BitString} and a substring of the specified bit
     * string (other) by the specified number of bits (nBits).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a rotateRight(|nBits|...) is
     * performed instead of a rotateLeft.
     * 
     * This {@code BitString} and the other bit string are modified by this
     * operation (see {@link #rotateLeft(int, BitString)} for details).
     * 
     * The other substring starts at offset 'otherOffset' of the specified bit
     * string and extends to the end of the specified bit string.
     * 
     * @param nBits       number of bits to rotate
     * @param other       the other bit string
     * @param otherOffset the start of the other substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code otherOffset < 0 || otherOffset > 0 && otherOffset >= other.length()}
     */
    public BitString rotateLeft(int nBits, BitString other, int otherOffset) {
        other.checkArgOffset(otherOffset);
        iRotateLeft(nBits, 0, this.length(), other, otherOffset, other.length() - otherOffset);
        return this;
    }
    
    /**
     * Rotates left this {@code BitString} and a substring of the specified bit
     * string (other) by the specified number of bits (nBits).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a rotateRight(|nBits|...) is
     * performed instead of a rotateLeft.
     * 
     * This {@code BitString} and the other bit string are modified by this
     * operation (see {@link #rotateLeft(int, BitString)} for details).
     * 
     * The other substring starts at offset 'otherOffset' of the specified bit
     * string and has a length of 'otherLength'.
     * 
     * @param nBits       number of bits to rotate
     * @param other       the other bit string
     * @param otherOffset the start of the other substring
     * @param otherLength the length of the other substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code otherOffset < 0 || otherOffset > 0 && otherOffset >= other.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code otherLength < 0 || otherLength > other.length() - otherOffset}
     */
    public BitString rotateLeft(int nBits, BitString other, int otherOffset, int otherLength) {
        other.checkArgOffset(otherOffset);
        other.checkArgLength(otherOffset, otherLength);
        iRotateLeft(nBits, 0, this.length(), other, otherOffset, otherLength);
        return this;
    }
    
    /**
     * Rotates left this {@code BitString} and a Field of the specified bit string
     * (other) by the specified number of bits (nBits).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a rotateRight(|nBits|...) is
     * performed instead of a rotateLeft.
     * 
     * This {@code BitString} and the other bit string are modified by this
     * operation (see {@link #rotateLeft(int, BitString)} for details).
     * 
     * @param nBits      number of bits to rotate
     * @param other      the other bit string
     * @param otherField a Field of the other bit string
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code otherField.offset() > 0 && otherField.Offset() >= other.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code otherField.length() > other.length() - otherField.offset()}
     */
    public BitString rotateLeft(int nBits, BitString other, Field otherField) {
        return rotateLeft(nBits, other, otherField.offset(), otherField.length());
    }
    
    /**
     * Rotates left a substring of this {@code BitString} and the specified bit
     * string (other) by the specified number of bits (nBits).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a rotateRight(|nBits|...) is
     * performed instead of a rotateLeft.
     * 
     * This {@code BitString} and the other bit string are modified by this
     * operation (see {@link #rotateLeft(int, BitString)} for details).
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * extends to the end of this {@code BitString}.
     * 
     * @param nBits      number of bits to rotate
     * @param thisOffset the start of this substring
     * @param other      the other bit string
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     */
    public BitString rotateLeft(int nBits,
            int thisOffset,
            BitString other) {
        checkThisOffset(thisOffset);
        iRotateLeft(nBits, thisOffset, this.length() - thisOffset, other, 0, other.length());
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
     * operation (see {@link #rotateRight(int, BitString)} for details).
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * extends to the end of this {@code BitString}.
     * 
     * The other substring starts at offset 'otherOffset' of the specified bit
     * string and extends to the end of the specified bit string.
     * 
     * @param nBits       number of bits to rotate
     * @param thisOffset  the start of this substring
     * @param other       the other bit string
     * @param otherOffset the start of the other substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code otherOffset < 0 || otherOffset > 0 && otherOffset >= other.length()}
     */
    public BitString rotateLeft(int nBits,
            int thisOffset,
            BitString other, int otherOffset) {
        checkThisOffset(thisOffset);
        other.checkArgOffset(otherOffset);
        iRotateLeft(nBits, thisOffset, this.length() - thisOffset, other, otherOffset, other.length() - otherOffset);
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
     * extends to the end of this {@code BitString}.
     * 
     * The other substring starts at offset 'otherOffset' of the specified bit
     * string and has a length of 'otherLength'.
     * 
     * @param nBits       number of bits to rotate
     * @param thisOffset  the start of this substring
     * @param other       the other bit string
     * @param otherOffset the start of the other substring
     * @param otherLength the length of the other substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code otherOffset < 0 || otherOffset > 0 && otherOffset >= other.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code otherLength < 0 || otherLength > other.length() - otherOffset}
     */ 
    public BitString rotateLeft(int nBits,
            int thisOffset,
            BitString other, int otherOffset, int otherLength) {
        checkThisOffset(thisOffset);
        other.checkArgOffset(otherOffset);
        other.checkArgLength(otherOffset, otherLength);
        iRotateLeft(nBits, thisOffset, this.length() - thisOffset, other, otherOffset, otherLength);
        return this;
    }
    
    /**
     * Rotates left a substring of this {@code BitString} and a Field of the
     * specified bit string (other) by the specified number of bits (nBits).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a rotateRight(|nBits|...) is
     * performed instead of a rotateLeft.
     * 
     * This {@code BitString} and the other bit string are modified by this
     * operation (see {@link #rotateLeft(int, BitString)} for details).
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * and extends to the end of this {@code BitString}.
     * 
     * @param nBits      number of bits to rotate
     * @param thisOffset the start of this substring
     * @param other      the other bit string
     * @param otherField a Field of the other bit string
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code otherField.offset() > 0 && otherField.Offset() >= other.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code otherField.length() > other.length() - otherField.offset()}
     */
    public BitString rotateLeft(int nBits,
            int thisOffset,
            BitString other, Field otherField) {
        return rotateLeft(nBits, thisOffset, other, otherField.offset(), otherField.length());
    }
    
    /**
     * Rotates left a substring of this {@code BitString} and the specified bit
     * string (other) by the specified number of bits (nBits).
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
     * @param nBits      number of bits to rotate
     * @param thisOffset the start of this substring
     * @param thisLength the length of this substring
     * @param other      the other bit string
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisLength < 0 || thisLength > this.length() - thisOffset}
     */
    public BitString rotateLeft(int nBits,
            int thisOffset, int thisLength,
            BitString other) {
        checkThisOffset(thisOffset);
        checkThisLength(thisOffset, thisLength);
        iRotateLeft(nBits, thisOffset, thisLength, other, 0, other.length());
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
     * string and extends to the end of the specified bit string.
     * 
     * @param nBits       number of bits to rotate
     * @param thisOffset  the start of this substring
     * @param thisLength  the length of this substring
     * @param other       the other bit string
     * @param otherOffset the start of the other substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code otherOffset < 0 || otherOffset > 0 && otherOffset >= other.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisLength < 0 || thisLength > this.length() - thisOffset}
     */
    public BitString rotateLeft(int nBits,
            int thisOffset, int thisLength,
            BitString other, int otherOffset) {
        checkThisOffset(thisOffset);
        checkThisLength(thisOffset, thisLength);
        other.checkArgOffset(otherOffset);
        iRotateLeft(nBits, thisOffset, thisLength, other, otherOffset, other.length() - otherOffset);
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
     * Rotates left a substring of this {@code BitString} and a Field of the
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
     * @param nBits      number of bits to rotate
     * @param thisOffset the start of this substring
     * @param thisLength the length of this substring
     * @param other      the other bit string
     * @param otherField a Field of the other bit string
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code otherField.offset() > 0 && otherField.Offset() >= other.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisLength < 0 || thisLength > this.length() - thisOffset}
     *                                         or
     *                                         {@code otherField.length() > other.length() - otherField.offset()}
     */
    public BitString rotateLeft(int nBits,
            int thisOffset, int thisLength,
            BitString other, Field otherField) {
        return rotateLeft(nBits, thisOffset, thisLength, other, otherField.offset(), otherField.length());
    }
    
    /**
     * Rotates left a Field of this {@code BitString} and a substring of the
     * specified bit string (other) by the specified number of bits (nBits).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a rotateRight(|nBits|...) is
     * performed instead of a rotateLeft.
     * 
     * This {@code BitString} and the other bit string are modified by this
     * operation (see {@link #rotateLeft(int, BitString)} for details).
     * 
     * @param nBits     number of bits to rotate
     * @param thisField a Field of this {@code BitString}
     * @param other     the other bit string
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisField.offset() > 0 && thisField.offset() >= this.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisField.length() > this.length() - thisField.offset()}
     */
    public BitString rotateLeft(int nBits,
            Field thisField,
            BitString other) {
        return rotateLeft(nBits, thisField.offset(), thisField.length(), other);
    }
    
    /**
     * Rotates left a Field of this {@code BitString} and a substring of the
     * specified bit string (other) by the specified number of bits (nBits).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a rotateRight(|nBits|...) is
     * performed instead of a rotateLeft.
     * 
     * This {@code BitString} and the other bit string are modified by this
     * operation (see {@link #rotateLeft(int, BitString)} for details).
     * 
     * The other substring starts at offset 'otherOffset' of the specified bit
     * string and extends to the end of the specified bit string.
     * 
     * @param nBits       number of bits to rotate
     * @param thisField   a Field of this {@code BitString}
     * @param other       the other bit string
     * @param otherOffset the start of the other substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisField.offset() > 0 && thisField.offset() >= this.length()}
     *                                         or
     *                                         {@code otherOffset < 0 || otherOffset > 0 && otherOffset >= other.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisField.length() > this.length() - thisField.offset()}
     */
    public BitString rotateLeft(int nBits,
            Field thisField,
            BitString other, int otherOffset) {
        return rotateLeft(nBits, thisField.offset(),
                thisField.length(),
                other, otherOffset);
    }
    
    /**
     * Rotates left a Field of this {@code BitString} and a substring of the
     * specified bit string (other) by the specified number of bits (nBits).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a rotateRight(|nBits|...) is
     * performed instead of a rotateLeft.
     * 
     * This {@code BitString} and the other bit string are modified by this
     * operation (see {@link #rotateLeft(int, BitString)} for details).
     * 
     * The other substring starts at offset 'otherOffset' of the specified bit
     * string and has a length of 'otherLength'.
     * 
     * @param nBits       number of bits to rotate
     * @param thisField   a Field of this {@code BitString}
     * @param other       the other bit string
     * @param otherOffset the start of the other substring
     * @param otherLength the length of the other substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisField.offset() > 0 && thisField.offset() >= this.length()}
     *                                         or
     *                                         {@code otherOffset < 0 || otherOffset > 0 && otherOffset >= other.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisField.length() > this.length() - thisField.offset()}
     *                                         or
     *                                         {@code otherLength < 0 || otherLength > other.length() - otherOffset}
     */
    public BitString rotateLeft(int nBits,
            Field thisField,
            BitString other, int otherOffset, int otherLength) {
        return rotateLeft(nBits, thisField.offset(), thisField.length(), other, otherOffset, otherLength);
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
        return rotateLeft(nBits, thisField.offset(), thisField.length(), other, otherField.offset(), otherField.length());
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
     * The substring starts at offset 'offset' of this {@code BitString} and extends
     * to the end of {@code BitString}.
     * 
     * Any bits rotated out on the right are rotated back into this
     * {@code BitString} on the left.
     * 
     * @param nBits  the number of bits to rotate
     * @param offset the start of this substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= this.length()}
     */
    public BitString rotateRight(int nBits, int offset) {
        checkThisOffset(offset);
        iRotateRight(nBits, offset, length() - offset);
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
        return rotateRight(nBits, field.offset(), field.length());
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
     * Rotates right this {@code BitString} and a substring of the specified bit
     * string (other) by the specified number of bits (nBits).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a rotateLeft(|nBits|...) is
     * performed instead of a rotateRight.
     * 
     * This {@code BitString} and the other bit string are modified by this
     * operation (see {@link #rotateRight(int, BitString)} for details).
     * 
     * The other substring starts at offset 'otherOffset' of the specified bit
     * string and extends to the end of the specified bit string.
     * 
     * @param nBits       number of bits to rotate
     * @param other       the other bit string
     * @param otherOffset the start of the other substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code otherOffset < 0 || otherOffset > 0 && otherOffset >= other.length()}
     */
    public BitString rotateRight(int nBits,
            BitString other, int otherOffset) {
        other.checkArgOffset(otherOffset);
        iRotateRight(nBits, 0, this.length(), other, otherOffset, other.length() - otherOffset);
        return this;
    }
    
    /**
     * Rotates right this {@code BitString} and a substring of the specified bit
     * string (other) by the specified number of bits (nBits).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a rotateLeft(|nBits|...) is
     * performed instead of a rotateRight.
     * 
     * This {@code BitString} and the other bit string are modified by this
     * operation (see {@link #rotateRight(int, BitString)} for details).
     * 
     * The other substring starts at offset 'otherOffset' of the specified bit
     * string and has a length of 'otherLength'.
     * 
     * @param nBits       number of bits to rotate
     * @param other       the other bit string
     * @param otherOffset the start of the other substring
     * @param otherLength the length of the other substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code otherOffset < 0 || otherOffset > 0 && otherOffset >= other.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code otherLength < 0 || otherLength > other.length() - otherOffset}
     */
    public BitString rotateRight(int nBits,
            BitString other, int otherOffset, int otherLength) {
        other.checkArgOffset(otherOffset);
        other.checkArgLength(otherOffset, otherLength);
        iRotateRight(nBits, 0, this.length(), other, otherOffset, otherLength);
        return this;
    }
    
    /**
     * Rotates right this {@code BitString} and a Field of the specified bit string
     * (other) by the specified number of bits (nBits).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a rotateLeft(|nBits|...) is
     * performed instead of a rotateRight.
     * 
     * This {@code BitString} and the other bit string are modified by this
     * operation (see {@link #rotateRight(int, BitString)} for details).
     * 
     * @param nBits      number of bits to rotate
     * @param other      the other bit string
     * @param otherField a Field of the other bit string
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code otherField.offset() > 0 && otherField.Offset() >= other.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code otherField.length() > other.length() - otherField.offset()}
     */
    public BitString rotateRight(int nBits,
            BitString other, Field otherField) {
        return rotateRight(nBits, other, otherField.offset(), otherField.length());
    }
    
    /**
     * Rotates right a substring of this {@code BitString} and the specified bit
     * string (other) by the specified number of bits (nBits).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a rotateLeft(|nBits|...) is
     * performed instead of a rotateRight.
     * 
     * This {@code BitString} and the other bit string are modified by this
     * operation (see {@link #rotateRight(int, BitString)} for details).
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * extends to the end of this {@code BitString}.
     * 
     * @param nBits      number of bits to rotate
     * @param thisOffset the start of this substring
     * @param other      the other bit string
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     */
    public BitString rotateRight(int nBits,
            int thisOffset,
            BitString other) {
        checkThisOffset(thisOffset);
        iRotateRight(nBits, thisOffset, this.length() - thisOffset, other, 0, other.length());
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
     * extends to the end of this {@code BitString}.
     * 
     * The other substring starts at offset 'otherOffset' of the specified bit
     * string and extends to the end of the specified bit string.
     * 
     * @param nBits       number of bits to rotate
     * @param thisOffset  the start of this substring
     * @param other       the other bit string
     * @param otherOffset the start of the other substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code otherOffset < 0 || otherOffset > 0 && otherOffset >= other.length()}
     */
    public BitString rotateRight(int nBits,
            int thisOffset,
            BitString other, int otherOffset) {
        checkThisOffset(thisOffset);
        other.checkArgOffset(otherOffset);
        iRotateRight(nBits, thisOffset, this.length() - thisOffset, other, otherOffset, other.length() - otherOffset);
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
     * extends to the end of this {@code BitString}.
     * 
     * The other substring starts at offset 'otherOffset' of the specified bit
     * string and has a length of 'otherLength'.
     * 
     * @param nBits       number of bits to rotate
     * @param thisOffset  the start of this substring
     * @param other       the other bit string
     * @param otherOffset the start of the other substring
     * @param otherLength the length of the other substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code otherOffset < 0 || otherOffset > 0 && otherOffset >= other.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code otherLength < 0 || otherLength > other.length() - otherOffset}
     */
    public BitString rotateRight(int nBits,
            int thisOffset,
            BitString other, int otherOffset, int otherLength) {
        checkThisOffset(thisOffset);
        other.checkArgOffset(otherOffset);
        other.checkArgLength(otherOffset, otherLength);
        iRotateRight(nBits, thisOffset, this.length() - thisOffset, other, otherOffset, otherLength);
        return this;
    }
    
    /**
     * Rotates right a substring of this {@code BitString} and a Field of the
     * specified bit string (other) by the specified number of bits (nBits).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a rotateLeft(|nBits|...) is
     * performed instead of a rotateRight.
     * 
     * This {@code BitString} and the other bit string are modified by this
     * operation (see {@link #rotateRight(int, BitString)} for details).
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * and extends to the end of this {@code BitString}.
     * 
     * @param nBits      number of bits to rotate
     * @param thisOffset the start of this substring
     * @param other      the other bit string
     * @param otherField a Field of the other bit string
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code otherField.offset() > 0 && otherField.Offset() >= other.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code otherField.length() > other.length() - otherField.offset()}
     */
    public BitString rotateRight(int nBits,
            int thisOffset,
            BitString other, Field otherField) {
        return rotateRight(nBits, thisOffset, other, otherField.offset(), otherField.length());
    }
    
    /**
     * Rotates right a substring of this {@code BitString} and the specified bit
     * string (other) by the specified number of bits (nBits).
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
     * @param nBits      number of bits to rotate
     * @param thisOffset the start of this substring
     * @param thisLength the length of this substring
     * @param other      the other bit string
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisLength < 0 || thisLength > this.length() - thisOffset}
     */
    public BitString rotateRight(int nBits,
            int thisOffset, int thisLength,
            BitString other) {
        checkThisOffset(thisOffset);
        checkThisLength(thisOffset, thisLength);
        iRotateRight(nBits, thisOffset, thisLength, other, 0, other.length());
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
     * string and extends to the end of the specified bit string.
     * 
     * @param nBits       number of bits to rotate
     * @param thisOffset  the start of this substring
     * @param thisLength  the length of this substring
     * @param other       the other bit string
     * @param otherOffset the start of the other substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code otherOffset < 0 || otherOffset > 0 && otherOffset >= other.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisLength < 0 || thisLength > this.length() - thisOffset}
     */
    public BitString rotateRight(int nBits,
            int thisOffset, int thisLength,
            BitString other, int otherOffset) {
        checkThisOffset(thisOffset);
        checkThisLength(thisOffset, thisLength);
        other.checkArgOffset(otherOffset);
        iRotateRight(nBits, thisOffset, thisLength, other, otherOffset, other.length() - otherOffset);
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
     * Rotates right a substring of this {@code BitString} and a Field of the
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
     * @param nBits      number of bits to rotate
     * @param thisOffset the start of this substring
     * @param thisLength the length of this substring
     * @param other      the other bit string
     * @param otherField a Field of the other bit string
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code otherField.offset() > 0 && otherField.Offset() >= other.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisLength < 0 || thisLength > this.length() - thisOffset}
     *                                         or
     *                                         {@code otherField.length() > other.length() - otherField.offset()}
     */
    public BitString rotateRight(int nBits,
            int thisOffset, int thisLength,
            BitString other, Field otherField) {
        return rotateRight(nBits, thisOffset, thisLength, other, otherField.offset(), otherField.length());
    }
    
    /**
     * Rotates right a Field of this {@code BitString} and a substring of the
     * specified bit string (other) by the specified number of bits (nBits).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a rotateLeft(|nBits|...) is
     * performed instead of a rotateRight.
     * 
     * This {@code BitString} and the other bit string are modified by this
     * operation (see {@link #rotateRight(int, BitString)} for details).
     * 
     * @param nBits     number of bits to rotate
     * @param thisField a Field of this {@code BitString}
     * @param other     the other bit string
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisField.offset() > 0 && thisField.offset() >= this.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisField.length() > this.length() - thisField.offset()}
     */
    public BitString rotateRight(int nBits,
            Field thisField,
            BitString other) {
        return rotateRight(nBits, thisField.offset(), thisField.length(), other);
    }
    
    /**
     * Rotates right a Field of this {@code BitString} and a substring of the
     * specified bit string (other) by the specified number of bits (nBits).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a rotateLeft(|nBits|...) is
     * performed instead of a rotateRight.
     * 
     * This {@code BitString} and the other bit string are modified by this
     * operation (see {@link #rotateRight(int, BitString)} for details).
     * 
     * The other substring starts at offset 'otherOffset' of the specified bit
     * string and extends to the end of the specified bit string.
     * 
     * @param nBits       number of bits to rotate
     * @param thisField   a Field of this {@code BitString}
     * @param other       the other bit string
     * @param otherOffset the start of the other substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisField.offset() > 0 && thisField.offset() >= this.length()}
     *                                         or
     *                                         {@code otherOffset < 0 || otherOffset > 0 && otherOffset >= other.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisField.length() > this.length() - thisField.offset()}
     */
    public BitString rotateRight(int nBits,
            Field thisField,
            BitString other, int otherOffset) {
        return rotateRight(nBits, thisField.offset(), thisField.length(), other, otherOffset);
    }
    
    /**
     * Rotates right a Field of this {@code BitString} and a substring of the
     * specified bit string (other) by the specified number of bits (nBits).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a rotateLeft(|nBits|...) is
     * performed instead of a rotateRight.
     * 
     * This {@code BitString} and the other bit string are modified by this
     * operation (see {@link #rotateRight(int, BitString)} for details).
     * 
     * The other substring starts at offset 'otherOffset' of the specified bit
     * string and has a length of 'otherLength'.
     * 
     * @param nBits       number of bits to rotate
     * @param thisField   a Field of this {@code BitString}
     * @param other       the other bit string
     * @param otherOffset the start of the other substring
     * @param otherLength the length of the other substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisField.offset() > 0 && thisField.offset() >= this.length()}
     *                                         or
     *                                         {@code otherOffset < 0 || otherOffset > 0 && otherOffset >= other.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisField.length() > this.length() - thisField.offset()}
     *                                         or
     *                                         {@code otherLength < 0 || otherLength > other.length() - otherOffset}
     */
    public BitString rotateRight(int nBits,
            Field thisField,
            BitString other, int otherOffset, int otherLength) {
        return rotateRight(nBits, thisField.offset(), thisField.length(), other, otherOffset, otherLength);
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
        return rotateRight(nBits, thisField.offset(), thisField.length(), other, otherField.offset(), otherField.length());
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
     * The substring starts at offset 'offset' of this {@code BitString} and extends
     * to the end of this {@code BitString}.
     * 
     * @param nBits  number of bits to shift
     * @param offset the start of this substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= this.length()}
     */
    public BitString shiftLeft(int nBits, int offset) {
        return shiftLeft(nBits, ZERO_FILL, offset);
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
        return shiftLeft(nBits, field.offset(), field.length());
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
     * The substring starts at offset 'offset' of this {@code BitString} and extends
     * to the end of this {@code BitString}.
     * 
     * @param nBits  number of bits to shift
     * @param fill   the fill value
     * @param offset the start of this substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= this.length()}
     */
    public BitString shiftLeft(int nBits, boolean fill, int offset) {
        checkThisOffset(offset);
        iShiftLeft(nBits, fill, offset, this.length() - offset);
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
        return shiftLeft(nBits, fill, field.offset(), field.length());
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
     * Shifts left this {@code BitString} and the specified bit string (other) by
     * the specified number of bits (nBits).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a shiftRight(|nbits|...) is
     * performed instead of a shiftLeft.
     * 
     * This {@code BitString} and the other bit string are modified by this
     * operation (see {@link #shiftLeft(int, BitString)} for details).
     * 
     * The other substring starts at offset 'otherOffset' of the specified bit
     * string and extends to the end of the specified bit string.
     * 
     * @param nBits       number of bits to shift
     * @param other       the other bit string
     * @param otherOffset the start of the other substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code otherOffset < 0 || otherOffset > 0 && otherOffset >= other.length()}
     */
    public BitString shiftLeft(int nBits, BitString other, int otherOffset) {
        return shiftLeft(nBits, ZERO_FILL, other, otherOffset);
    }
    
    /**
     * Shifts left this {@code BitString} and a substring of the specified bit
     * string (other) by the specified number of bits (nBits).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a shiftRight(|nbits|...) is
     * performed instead of a shiftLeft.
     * 
     * This {@code BitString} and the other bit string are modified by this
     * operation (see {@link #shiftLeft(int, BitString)} for details).
     * 
     * The other substring starts at offset 'otherOffset' of the specified bit
     * string and has a length of 'otherLength'.
     * 
     * @param nBits       number of bits to shift
     * @param other       the other bit string
     * @param otherOffset the start of the other substring
     * @param otherLength the length of the other substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code otherOffset < 0 || otherOffset > 0 && otherOffset >= other.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code otherLength < 0 || otherLength > other.length() - otherOffset}
     */
    public BitString shiftLeft(int nBits, BitString other, int otherOffset, int otherLength) {
        return shiftLeft(nBits, ZERO_FILL, other, otherOffset, otherLength);
    }
    
    /**
     * Shifts left this {@code BitString} and a Field of the specified bit string
     * (other) by the specified number of bits (nBits).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a shiftRight(|nbits|...) is
     * performed instead of a shiftLeft.
     * 
     * This {@code BitString} and the other bit string are modified by this
     * operation (see {@link #shiftLeft(int, BitString)} for details).
     * 
     * @param nBits      number of bits to shift
     * @param other      the other bit string
     * @param otherField a Field of the other substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code otherField.offset() > 0 && otherField.offset() >= other.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code otherField.length() > other.length() - otherField.offset()}
     */
    public BitString shiftLeft(int nBits, BitString other, Field otherField) {
        return shiftLeft(nBits, other, otherField.offset(), otherField.length());
    }
    
    /**
     * Shifts left a substring of this {@code BitString} and the specified bit
     * string (other) by the specified number of bits (nBits).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a shiftRight(|nbits|...) is
     * performed instead of a shiftLeft.
     * 
     * This {@code BitString} and the other bit string are modified by this
     * operation (see {@link #shiftLeft(int, BitString)} for details).
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * extends to the end of this {@code BitString}.
     * 
     * @param nBits      number of bits to shift
     * @param thisOffset the start of this substring
     * @param other      the other bit string
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     */
    public BitString shiftLeft(int nBits, int thisOffset, BitString other) {
        return shiftLeft(nBits, ZERO_FILL, thisOffset, other);
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
     * extends to the end of this {@code BitString}.
     * 
     * The other substring starts at offset 'otherOffset' of the specified bit
     * string and extends to the end of the specified bit string.
     * 
     * @param nBits       number of bits to shift
     * @param thisOffset  the start of this substring
     * @param other       the other bit string
     * @param otherOffset the start of the other substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code otherOffset < 0 || otherOffset > 0 && otherOffset >= other.length()}
     */
    public BitString shiftLeft(int nBits, int thisOffset, BitString other, int otherOffset) {
        return shiftLeft(nBits, ZERO_FILL, thisOffset, other, otherOffset);
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
     * extends to the end of this {@code BitString}.
     * 
     * The other substring starts at offset 'otherOffset' of the specified bit
     * string and has a length of 'otherLength'.
     * 
     * @param nBits       number of bits to shift
     * @param thisOffset  the start of this substring
     * @param other       the other bit string
     * @param otherOffset the start of the other substring
     * @param otherLength the length of the other substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code otherOffset < 0 || otherOffset > 0 && otherOffset >= other.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code otherLength < 0 || otherLength > other.length() - otherOffset}
     */
    public BitString shiftLeft(int nBits,
            int thisOffset,
            BitString other, int otherOffset, int otherLength) {
        return shiftLeft(nBits, ZERO_FILL, thisOffset, other, otherOffset, otherLength);
    }
    
    /**
     * Shifts left a substring of this {@code BitString} and a Field of the
     * specified bit string (other) by the specified number of bits (nBits).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a shiftRight(|nbits|...) is
     * performed instead of a shiftLeft.
     * 
     * This {@code BitString} and the other bit string are modified by this
     * operation (see {@link #shiftLeft(int, BitString)} for details).
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * extends to the end of this {@code BitString}.
     * 
     * @param nBits      number of bits to shift
     * @param thisOffset the start of this substring
     * @param other      the other bit string
     * @param otherField a Field of the other substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code otherField.offset() > 0 && otherField.offset() >= other.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code otherField.length() > other.length() - otherField.offset()}
     */
    public BitString shiftLeft(int nBits,
            int thisOffset,
            BitString other, Field otherField) {
        return shiftLeft(nBits, thisOffset, other, otherField.offset(), otherField.length());
    }
    
    /**
     * Shifts left a substring of this {@code BitString} and the specified bit
     * string (other) by the specified number of bits (nBits).
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
     * @param nBits      number of bits to shift
     * @param thisOffset the start of this substring
     * @param thisLength the length of this substring
     * @param other      the other bit string
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisLength < 0 || thisLength > this.length() - thisOffset}
     */
    public BitString shiftLeft(int nBits,
            int thisOffset, int thisLength,
            BitString other) {
        return shiftLeft(nBits, ZERO_FILL, thisOffset, thisLength, other);
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
     * string and extends to the end of the specified bit string.
     * 
     * @param nBits       number of bits to shift
     * @param thisOffset  the start of this substring
     * @param thisLength  the length of this substring
     * @param other       the other bit string
     * @param otherOffset the start of the other substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code otherOffset < 0 || otherOffset > 0 && otherOffset >= other.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisLength < 0 || thisLength > this.length() - thisOffset}
     */
    public BitString shiftLeft(int nBits,
            int thisOffset, int thisLength,
            BitString other, int otherOffset) {
        return shiftLeft(nBits, ZERO_FILL, thisOffset, thisLength, other, otherOffset);
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
     * Shifts left a substring of this {@code BitString} and a Field of the
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
     * @param nBits      number of bits to shift
     * @param thisOffset the start of this substring
     * @param thisLength the length of this substring
     * @param other      the other bit string
     * @param otherField a Field of the other substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code otherField.offset() > 0 && otherField.offset() >= other.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisLength < 0 || thisLength > this.length() - thisOffset}
     *                                         or
     *                                         {@code otherField.length() > other.length() - otherField.offset()}
     */
    public BitString shiftLeft(int nBits, int thisOffset, int thisLength, BitString other, Field otherField) {
        return shiftLeft(nBits, thisOffset, thisLength, other, otherField.offset(), otherField.length());
    }
    
    /**
     * Shifts left a Field of this {@code BitString} and the specified bit string
     * (other) by the specified number of bits (nBits).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a shiftRight(|nbits|...) is
     * performed instead of a shiftLeft.
     * 
     * This {@code BitString} and the other bit string are modified by this
     * operation (see {@link #shiftLeft(int, BitString)} for details).
     * 
     * @param nBits     number of bits to shift
     * @param thisField a Field of this {@code BitString}
     * @param other     the other bit string
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisField.offset() > 0 && thisField.offset() >= this.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisField.length() > this.length() - thisField.offset()}
     */
    public BitString shiftLeft(int nBits, Field thisField, BitString other) {
        return shiftLeft(nBits, thisField.offset(), thisField.length(), other);
    }
    
    /**
     * Shifts left a Field of this {@code BitString} and a substring of the
     * specified bit string (other) by the specified number of bits (nBits).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a shiftRight(|nbits|...) is
     * performed instead of a shiftLeft.
     * 
     * This {@code BitString} and the other bit string are modified by this
     * operation (see {@link #shiftLeft(int, BitString)} for details).
     * 
     * The other substring starts at offset 'otherOffset' of the specified bit
     * string and extends to the end of the specified bit string.
     * 
     * @param nBits       number of bits to shift
     * @param thisField   a Field of this {@code BitString}
     * @param other       the other bit string
     * @param otherOffset the start of the other substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisField.offset() > 0 && thisField.offset() >= this.length()}
     *                                         or
     *                                         {@code otherOffset < 0 || otherOffset > 0 && otherOffset >= other.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisField.length() > this.length() - thisField.offset()}
     */
    public BitString shiftLeft(int nBits,
            Field thisField,
            BitString other, int otherOffset) {
        return shiftLeft(nBits, thisField.offset(), thisField.length(), other, otherOffset);
    }
    
    /**
     * Shifts left a Field of this {@code BitString} and a substring of the
     * specified bit string (other) by the specified number of bits (nBits).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a shiftRight(|nbits|...) is
     * performed instead of a shiftLeft.
     * 
     * This {@code BitString} and the other bit string are modified by this
     * operation (see {@link #shiftLeft(int, BitString)} for details).
     * 
     * The other substring starts at offset 'otherOffset' of the specified bit
     * string and has a length of 'otherLength'.
     * 
     * @param nBits       number of bits to shift
     * @param thisField   a Field of this {@code BitString}
     * @param other       the other bit string
     * @param otherOffset the start of the other substring
     * @param otherLength the length of the other substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisField.offset() > 0 && thisField.offset() >= this.length()}
     *                                         or
     *                                         {@code otherOffset < 0 || otherOffset > 0 && otherOffset >= other.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisField.length() > this.length() - thisField.offset()}
     *                                         or
     *                                         {@code otherLength < 0 || otherLength > other.length() - otherOffset}
     */
    public BitString shiftLeft(int nBits,
            Field thisField,
            BitString other, int otherOffset, int otherLength) {
        return shiftLeft(nBits, thisField.offset(), thisField.length(), other, otherOffset, otherLength);
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
        return shiftLeft(nBits, thisField.offset(), thisField.length(), other, otherField.offset(), otherField.length());
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
     * Shifts left this {@code BitString} and the specified bit string (other) by
     * the specified number of bits (nBits), filling any vacated positions by the
     * specified fill value (fill).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a shiftRight(|nbits|...) is
     * performed instead of a shiftLeft.
     * 
     * This {@code BitString} and the other bit string are modified by this
     * operation (see {@link #shiftLeft(int, boolean, BitString)} for details).
     * 
     * The other substring starts at offset 'otherOffset' of the specified bit
     * string and extends to the end of the specified bit string.
     * 
     * @param nBits       number of bits to shift
     * @param fill        the fill value
     * @param other       the other bit string
     * @param otherOffset the start of the other substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code otherOffset < 0 || otherOffset > 0 && otherOffset >= other.length()}
     */
    public BitString shiftLeft(int nBits, boolean fill,
            BitString other, int otherOffset) {
        other.checkArgOffset(otherOffset);
        iShiftLeft(nBits, fill, 0, this.length(), other, otherOffset, other.length() - otherOffset);
        return this;
    }
    
    /**
     * Shifts left this {@code BitString} and a substring of the specified bit
     * string (other) by the specified number of bits (nBits), filling any vacated
     * positions by the specified fill value (fill).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a shiftRight(|nbits|...) is
     * performed instead of a shiftLeft.
     * 
     * This {@code BitString} and the other bit string are modified by this
     * operation (see {@link #shiftLeft(int, boolean, BitString)} for details).
     * 
     * The other substring starts at offset 'otherOffset' of the specified bit
     * string and has a length of 'otherLength'.
     * 
     * @param nBits       number of bits to shift
     * @param fill        the fill value
     * @param other       the other bit string
     * @param otherOffset the start of the other substring
     * @param otherLength the length of the other substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code otherOffset < 0 || otherOffset > 0 && otherOffset >= other.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code otherLength < 0 || otherLength > other.length() - otherOffset}
     */
    public BitString shiftLeft(int nBits, boolean fill,
            BitString other, int otherOffset, int otherLength) {
        other.checkArgOffset(otherOffset);
        other.checkArgLength(otherOffset, otherLength);
        iShiftLeft(nBits, fill, 0, this.length(), other, otherOffset, otherLength);
        return this;
    }
    
    /**
     * Shifts left this {@code BitString} and a Field of the specified bit string
     * (other) by the specified number of bits (nBits), filling any vacated
     * positions by the specified fill value (fill).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a shiftRight(|nbits|...) is
     * performed instead of a shiftLeft.
     * 
     * This {@code BitString} and the other bit string are modified by this
     * operation (see {@link #shiftLeft(int, boolean, BitString)} for details).
     * 
     * @param nBits      number of bits to shift
     * @param fill       the fill value
     * @param other      the other bit string
     * @param otherField a Field of the other substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code otherField.offset() > 0 && otherField.offset() >= other.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code otherField.length() > other.length() - otherField.offset()}
     */
    public BitString shiftLeft(int nBits, boolean fill,
            BitString other, Field otherField) {
        return shiftLeft(nBits, fill, other, otherField.offset(), otherField.length());
    }
    
    /**
     * Shifts left a substring of this {@code BitString} and the specified bit
     * string (other) by the specified number of bits (nBits), filling any vacated
     * positions by the specified fill value (fill).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a shiftRight(|nbits|...) is
     * performed instead of a shiftLeft.
     * 
     * This {@code BitString} and the other bit string are modified by this
     * operation (see {@link #shiftLeft(int, boolean, BitString)} for details).
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * extends to the end of this {@code BitString}.
     * 
     * @param nBits      number of bits to shift
     * @param fill       the fill value
     * @param thisOffset the start of this substring
     * @param other      the other bit string
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     */
    public BitString shiftLeft(int nBits, boolean fill,
            int thisOffset,
            BitString other) {
        checkThisOffset(thisOffset);
        iShiftLeft(nBits, fill, thisOffset, this.length() - thisOffset, other, 0, other.length());
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
     * extends to the end of this {@code BitString}.
     * 
     * The other substring starts at offset 'otherOffset' of the specified bit
     * string and extends to the end of the specified bit string.
     * 
     * @param nBits       number of bits to shift
     * @param fill        the fill value
     * @param thisOffset  the start of this substring
     * @param other       the other bit string
     * @param otherOffset the start of the other substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code otherOffset < 0 || otherOffset > 0 && otherOffset >= other.length()}
     */
    public BitString shiftLeft(int nBits, boolean fill,
            int thisOffset,
            BitString other, int otherOffset) {
        checkThisOffset(thisOffset);
        other.checkArgOffset(otherOffset);
        iShiftLeft(nBits, fill, thisOffset, this.length() - thisOffset, other, otherOffset, other.length() - otherOffset);
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
     * extends to the end of this {@code BitString}.
     * 
     * The other substring starts at offset 'otherOffset' of the specified bit
     * string and has a length of 'otherLength'.
     * 
     * @param nBits       number of bits to shift
     * @param fill        the fill value
     * @param thisOffset  the start of this substring
     * @param other       the other bit string
     * @param otherOffset the start of the other substring
     * @param otherLength the length of the other substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code otherOffset < 0 || otherOffset > 0 && otherOffset >= other.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code otherLength < 0 || otherLength > other.length() - otherOffset}
     */
    public BitString shiftLeft(int nBits, boolean fill,
            int thisOffset,
            BitString other, int otherOffset, int otherLength) {
        checkThisOffset(thisOffset);
        other.checkArgOffset(otherOffset);
        other.checkArgLength(otherOffset, otherLength);
        iShiftLeft(nBits, fill, thisOffset, this.length() - thisOffset, other, otherOffset, otherLength);
        return this;
    }
    
    /**
     * Shifts left a substring of this {@code BitString} and a Field of the
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
     * extends to the end of this {@code BitString}.
     * 
     * @param nBits      number of bits to shift
     * @param fill       the fill value
     * @param thisOffset the start of this substring
     * @param other      the other bit string
     * @param otherField a Field of the other substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code otherField.offset() > 0 && otherField.offset() >= other.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code otherField.length() > other.length() - otherField.offset()}
     */
    public BitString shiftLeft(int nBits, boolean fill,
            int thisOffset,
            BitString other, Field otherField) {
        return shiftLeft(nBits, fill, thisOffset, other, otherField.offset(), otherField.length());
    }
    
    /**
     * Shifts left a substring of this {@code BitString} and the specified bit
     * string (other) by the specified number of bits (nBits), filling any vacated
     * positions by the specified fill value (fill).
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
     * @param nBits      number of bits to shift
     * @param fill       the fill value
     * @param thisOffset the start of this substring
     * @param thisLength the length of this substring
     * @param other      the other bit string
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisLength < 0 || thisLength > this.length() - thisOffset}
     */
    public BitString shiftLeft(int nBits, boolean fill,
            int thisOffset, int thisLength,
            BitString other) {
        checkThisOffset(thisOffset);
        checkThisLength(thisOffset, thisLength);
        iShiftLeft(nBits, fill, thisOffset, thisLength, other, 0, other.length());
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
     * string and extends to the end of the specified bit string.
     * 
     * @param nBits       number of bits to shift
     * @param fill        the fill value
     * @param thisOffset  the start of this substring
     * @param thisLength  the length of this substring
     * @param other       the other bit string
     * @param otherOffset the start of the other substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code otherOffset < 0 || otherOffset > 0 && otherOffset >= other.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisLength < 0 || thisLength > this.length() - thisOffset}
     */
    public BitString shiftLeft(int nBits, boolean fill,
            int thisOffset, int thisLength,
            BitString other, int otherOffset) {
        checkThisOffset(thisOffset);
        checkThisLength(thisOffset, thisLength);
        other.checkArgOffset(otherOffset);
        iShiftLeft(nBits, fill, thisOffset, thisLength, other, otherOffset, other.length() - otherOffset);
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
     * Shifts left a substring of this {@code BitString} and a Field of the
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
     * @param nBits      number of bits to shift
     * @param fill       the fill value
     * @param thisOffset the start of this substring
     * @param thisLength the length of this substring
     * @param other      the other bit string
     * @param otherField a Field of the other substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code otherField.offset() > 0 && otherField.offset() >= other.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisLength < 0 || thisLength > this.length() - thisOffset}
     *                                         or
     *                                         {@code otherField.length() > other.length() - otherField.offset()}
     */
    public BitString shiftLeft(int nBits, boolean fill,
            int thisOffset, int thisLength,
            BitString other, Field otherField) {
        return shiftLeft(nBits, fill, thisOffset, thisLength, other, otherField.offset(), otherField.length());
    }
    
    /**
     * Shifts left a Field of this {@code BitString} and the specified bit string
     * (other) by the specified number of bits (nBits), filling any vacated
     * positions by the specified fill value (fill).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a shiftRight(|nbits|...) is
     * performed instead of a shiftLeft.
     * 
     * This {@code BitString} and the other bit string are modified by this
     * operation (see {@link #shiftLeft(int, boolean, BitString)} for details).
     * 
     * @param nBits     number of bits to shift
     * @param fill      the fill value
     * @param thisField a Field of this {@code BitString}
     * @param other     the other bit string
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisField.offset() > 0 && thisField.offset() >= this.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisField.length() > this.length() - thisField.offset()}
     */
    public BitString shiftLeft(int nBits, boolean fill,
            Field thisField,
            BitString other) {
        return shiftLeft(nBits, fill, thisField.offset(), thisField.length(), other);
    }
    
    /**
     * Shifts left a Field of this {@code BitString} and a substring of the
     * specified bit string (other) by the specified number of bits (nBits), filling
     * any vacated positions by the specified fill value (fill).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a shiftRight(|nbits|...) is
     * performed instead of a shiftLeft.
     * 
     * This {@code BitString} and the other bit string are modified by this
     * operation (see {@link #shiftLeft(int, boolean, BitString)} for details).
     * 
     * The other substring starts at offset 'otherOffset' of the specified bit
     * string and extends to the end of the specified bit string.
     * 
     * @param nBits       number of bits to shift
     * @param fill        the fill value
     * @param thisField   a Field of this {@code BitString}
     * @param other       the other bit string
     * @param otherOffset the start of the other substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisField.offset() > 0 && thisField.offset() >= this.length()}
     *                                         or
     *                                         {@code otherOffset < 0 || otherOffset > 0 && otherOffset >= other.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisField.length() > this.length() - thisField.offset()}
     */
    public BitString shiftLeft(int nBits, boolean fill,
            Field thisField,
            BitString other, int otherOffset) {
        return shiftLeft(nBits, fill, thisField.offset(), thisField.length(), other, otherOffset);
    }
    
    /**
     * Shifts left a Field of this {@code BitString} and a substring of the
     * specified bit string (other) by the specified number of bits (nBits), filling
     * any vacated positions by the specified fill value (fill).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a shiftRight(|nbits|...) is
     * performed instead of a shiftLeft.
     * 
     * This {@code BitString} and the other bit string are modified by this
     * operation (see {@link #shiftLeft(int, boolean, BitString)} for details).
     * 
     * The other substring starts at offset 'otherOffset' of the specified bit
     * string and has a length of 'otherLength'.
     * 
     * @param nBits       number of bits to shift
     * @param fill        the fill value
     * @param thisField   a Field of this {@code BitString}
     * @param other       the other bit string
     * @param otherOffset the start of the other substring
     * @param otherLength the length of the other substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisField.offset() > 0 && thisField.offset() >= this.length()}
     *                                         or
     *                                         {@code otherOffset < 0 || otherOffset > 0 && otherOffset >= other.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisField.length() > this.length() - thisField.offset()}
     *                                         or
     *                                         {@code otherLength < 0 || otherLength > other.length() - otherOffset}
     */
    public BitString shiftLeft(int nBits, boolean fill,
            Field thisField,
            BitString other, int otherOffset, int otherLength) {
        return shiftLeft(nBits, fill, thisField.offset(), thisField.length(), other, otherOffset, otherLength);
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
        return shiftLeft(nBits, fill, thisField.offset(), thisField.length(), other, otherField.offset(), otherField.length());
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
     * If nBits is negative (including Integer.MIN_VALUE), a shiftLeft(|nbits|...)
     * is performed instead of a shiftRight.
     * 
     * This {@code BitString} is modified by this operation (see
     * {@link #shiftRight(int)}.
     * 
     * The substring starts at offset 'offset' of this {@code BitString} and extends
     * to the end of this {@code BitString}.
     * 
     * @param nBits  number of bits to shift
     * @param offset the start of this substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= this.length()}
     */
    public BitString shiftRight(int nBits, int offset) {
        return shiftRight(nBits, ZERO_FILL, offset);
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
        return shiftRight(nBits, field.offset(), field.length());
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
     * The substring starts at offset 'offset' of this {@code BitString} and extends
     * to the end of this {@code BitString}.
     * 
     * @param nBits  number of bits to shift
     * @param fill   the fill value
     * @param offset the start of this substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code offset < 0 || offset > 0 && offset >= this.length()}
     */
    public BitString shiftRight(int nBits, boolean fill, int offset) {
        checkThisOffset(offset);
        iShiftRight(nBits, fill, offset, length() - offset);
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
        return shiftRight(nBits, fill, field.offset(), field.length());
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
     * Shifts right this {@code BitString} and the specified bit string (other) by
     * the specified number of bits (nBits).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a shiftLeft(|nbits|...) is
     * performed instead of a shiftRight.
     * 
     * This {@code BitString} and the other bit string are modified by this
     * operation (see {@link #shiftRight(int, BitString)} for details).
     * 
     * The other substring starts at offset 'otherOffset' of the specified bit
     * string and extends to the end of the specified bit string.
     * 
     * @param nBits       number of bits to shift
     * @param other       the other bit string
     * @param otherOffset the start of the other substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code otherOffset < 0 || otherOffset > 0 && otherOffset >= other.length()}
     */
    public BitString shiftRight(int nBits, BitString other, int otherOffset) {
        return shiftRight(nBits, ZERO_FILL, other, otherOffset);
    }
    
    /**
     * Shifts right this {@code BitString} and a substring of the specified bit
     * string (other) by the specified number of bits (nBits).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a shiftLeft(|nbits|...) is
     * performed instead of a shiftRight.
     * 
     * This {@code BitString} and the other bit string are modified by this
     * operation (see {@link #shiftRight(int, BitString)} for details).
     * 
     * The other substring starts at offset 'otherOffset' of the specified bit
     * string and has a length of 'otherLength'.
     * 
     * @param nBits       number of bits to shift
     * @param other       the other bit string
     * @param otherOffset the start of the other substring
     * @param otherLength the length of the other substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code otherOffset < 0 || otherOffset > 0 && otherOffset >= other.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code otherLength < 0 || otherLength > other.length() - otherOffset}
     */
    public BitString shiftRight(int nBits, BitString other, int otherOffset, int otherLength) {
        return shiftRight(nBits, ZERO_FILL, other, otherOffset, otherLength);
    }
    
    /**
     * Shifts right this {@code BitString} and a Field of the specified bit string
     * (other) by the specified number of bits (nBits).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a shiftLeft(|nbits|...) is
     * performed instead of a shiftRight.
     * 
     * This {@code BitString} and the other bit string are modified by this
     * operation (see {@link #shiftRight(int, BitString)} for details).
     * 
     * @param nBits      number of bits to shift
     * @param other      the other bit string
     * @param otherField a Field of the other substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code otherField.offset() > 0 && otherField.offset() >= other.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code otherField.length() > other.length() - otherField.offset()}
     */
    public BitString shiftRight(int nBits, BitString other, Field otherField) {
        return shiftRight(nBits, other, otherField.offset(), otherField.length());
    }
    
    /**
     * Shifts Right a substring of this {@code BitString} and the specified bit
     * string (other) by the specified number of bits (nBits).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a shiftLeft(|nbits|...) is
     * performed instead of a shiftRight.
     * 
     * This {@code BitString} and the other bit string are modified by this
     * operation (see {@link #shiftRight(int, BitString)} for details).
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * extends to the end of this {@code BitString}.
     * 
     * @param nBits      number of bits to shift
     * @param thisOffset the start of this substring
     * @param other      the other bit string
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     */
    public BitString shiftRight(int nBits, int thisOffset, BitString other) {
        return shiftRight(nBits, ZERO_FILL, thisOffset, other);
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
     * extends to the end of this {@code BitString}.
     * 
     * The other substring starts at offset 'otherOffset' of the specified bit
     * string and extends to the end of the specified bit string.
     * 
     * @param nBits       number of bits to shift
     * @param thisOffset  the start of this substring
     * @param other       the other bit string
     * @param otherOffset the start of the other substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code otherOffset < 0 || otherOffset > 0 && otherOffset >= other.length()}
     */
    public BitString shiftRight(int nBits,
            int thisOffset,
            BitString other, int otherOffset) {
        return shiftRight(nBits, ZERO_FILL, thisOffset, other, otherOffset);
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
     * extends to the end of this {@code BitString}.
     * 
     * The other substring starts at offset 'otherOffset' of the specified bit
     * string and has a length of 'otherLength'.
     * 
     * @param nBits       number of bits to shift
     * @param thisOffset  the start of this substring
     * @param other       the other bit string
     * @param otherOffset the start of the other substring
     * @param otherLength the length of the other substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code otherOffset < 0 || otherOffset > 0 && otherOffset >= other.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code otherLength < 0 || otherLength > other.length() - otherOffset}
     */
    public BitString shiftRight(int nBits,
            int thisOffset,
            BitString other, int otherOffset, int otherLength) {
        return shiftRight(nBits, ZERO_FILL, thisOffset, other, otherOffset, otherLength);
    }
    
    /**
     * Shifts right a substring of this {@code BitString} and a Field of the
     * specified bit string (other) by the specified number of bits (nBits).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a shiftLeft(|nbits|...) is
     * performed instead of a shiftRight.
     * 
     * This {@code BitString} and the other bit string are modified by this
     * operation (see {@link #shiftRight(int, BitString)} for details).
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * extends to the end of this {@code BitString}.
     * 
     * @param nBits      number of bits to shift
     * @param thisOffset the start of this substring
     * @param other      the other bit string
     * @param otherField a Field of the other substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code otherField.offset() > 0 && otherField.offset() >= other.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code otherField.length() > other.length() - otherField.offset()}
     */
    public BitString shiftRight(int nBits,
            int thisOffset,
            BitString other, Field otherField) {
        return shiftRight(nBits, thisOffset, other, otherField.offset(), otherField.length());
    }
    
    /**
     * Shifts Right a substring of this {@code BitString} and the specified bit
     * string (other) by the specified number of bits (nBits).
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
     * @param nBits      number of bits to shift
     * @param thisOffset the start of this substring
     * @param thisLength the length of this substring
     * @param other      the other bit string
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisLength < 0 || thisLength > this.length() - thisOffset}
     */
    public BitString shiftRight(int nBits,
            int thisOffset, int thisLength,
            BitString other) {
        return shiftRight(nBits, ZERO_FILL, thisOffset, thisLength, other);
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
     * string and extends to the end of the specified bit string.
     * 
     * @param nBits       number of bits to shift
     * @param thisOffset  the start of this substring
     * @param thisLength  the length of this substring
     * @param other       the other bit string
     * @param otherOffset the start of the other substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code otherOffset < 0 || otherOffset > 0 && otherOffset >= other.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisLength < 0 || thisLength > this.length() - thisOffset}
     */
    public BitString shiftRight(int nBits,
            int thisOffset, int thisLength,
            BitString other, int otherOffset) {
        return shiftRight(nBits, ZERO_FILL, thisOffset, thisLength, other, otherOffset);
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
     * Shifts right a substring of this {@code BitString} and a Field of the
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
     * @param nBits      number of bits to shift
     * @param thisOffset the start of this substring
     * @param thisLength the length of this substring
     * @param other      the other bit string
     * @param otherField a Field of the other substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code otherField.offset() > 0 && otherField.offset() >= other.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisLength < 0 || thisLength > this.length() - thisOffset}
     *                                         or
     *                                         {@code otherField.length() > other.length() - otherField.offset()}
     */
    public BitString shiftRight(int nBits,
            int thisOffset, int thisLength,
            BitString other, Field otherField) {
        return shiftRight(nBits, thisOffset, thisLength, other, otherField.offset(), otherField.length());
    }
    
    /**
     * Shifts right a Field of this {@code BitString} and the specified bit string
     * (other) by the specified number of bits (nBits).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a shiftLeft(|nbits|...) is
     * performed instead of a shiftRight.
     * 
     * This {@code BitString} and the other bit string are modified by this
     * operation (see {@link #shiftRight(int, BitString)} for details).
     * 
     * @param nBits     number of bits to shift
     * @param thisField a Field of this {@code BitString}
     * @param other     the other bit string
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisField.offset() > 0 && thisField.offset() >= this.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisField.length() > this.length() - thisField.offset()}
     */
    public BitString shiftRight(int nBits, Field thisField, BitString other) {
        return shiftRight(nBits, thisField.offset(), thisField.length(), other);
    }
    
    /**
     * Shifts right a Field of this {@code BitString} and a substring of the
     * specified bit string (other) by the specified number of bits (nBits).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a shiftLeft(|nbits|...) is
     * performed instead of a shiftRight.
     * 
     * This {@code BitString} and the other bit string are modified by this
     * operation (see {@link #shiftRight(int, BitString)} for details).
     * 
     * The other substring starts at offset 'otherOffset' of the specified bit
     * string and extends to the end of the specified bit string.
     * 
     * @param nBits       number of bits to shift
     * @param thisField   a Field of this {@code BitString}
     * @param other       the other bit string
     * @param otherOffset the start of the other substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisField.offset() > 0 && thisField.offset() >= this.length()}
     *                                         or
     *                                         {@code otherOffset < 0 || otherOffset > 0 && otherOffset >= other.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisField.length() > this.length() - thisField.offset()}
     */
    public BitString shiftRight(int nBits,
            Field thisField,
            BitString other, int otherOffset) {
        return shiftRight(nBits, thisField.offset(), thisField.length(), other, otherOffset);
    }
    
    /**
     * Shifts right a Field of this {@code BitString} and a substring of the
     * specified bit string (other) by the specified number of bits (nBits).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a shiftLeft(|nbits|...) is
     * performed instead of a shiftRight.
     * 
     * This {@code BitString} and the other bit string are modified by this
     * operation (see {@link #shiftRight(int, BitString)} for details).
     * 
     * The other substring starts at offset 'otherOffset' of the specified bit
     * string and has a length of 'otherLength'.
     * 
     * @param nBits       number of bits to shift
     * @param thisField   a Field of this {@code BitString}
     * @param other       the other bit string
     * @param otherOffset the start of the other substring
     * @param otherLength the length of the other substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisField.offset() > 0 && thisField.offset() >= this.length()}
     *                                         or
     *                                         {@code otherOffset < 0 || otherOffset > 0 && otherOffset >= other.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisField.length() > this.length() - thisField.offset()}
     *                                         or
     *                                         {@code otherLength < 0 || otherLength > other.length() - otherOffset}
     */
    public BitString shiftRight(int nBits,
            Field thisField,
            BitString other, int otherOffset, int otherLength) {
        return shiftRight(nBits, thisField.offset(), thisField.length(), other, otherOffset, otherLength);
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
        return shiftRight(nBits, thisField.offset(), thisField.length(), other, otherField.offset(), otherField.length());
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
     * Shifts right this {@code BitString} and the specified bit string (other) by
     * the specified number of bits (nBits), filling any vacated positions by the
     * specified fill value (fill).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a shiftLeft(|nbits|...) is
     * performed instead of a shiftRight.
     * 
     * This {@code BitString} and the other bit string are modified by this
     * operation (see {@link #shiftRight(int, boolean, BitString)} for details).
     * 
     * The other substring starts at offset 'otherOffset' of the specified bit
     * string and extends to the end of the specified bit string.
     * 
     * @param nBits       number of bits to shift
     * @param fill        the fill value
     * @param other       the other bit string
     * @param otherOffset the start of the other substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code otherOffset < 0 || otherOffset > 0 && otherOffset >= other.length()}
     */
    public BitString shiftRight(int nBits, boolean fill,
            BitString other, int otherOffset) {
        other.checkArgOffset(otherOffset);
        iShiftRight(nBits, fill, 0, this.length(), other, otherOffset, other.length() - otherOffset);
        return this;
    }
    
    /**
     * Shifts right this {@code BitString} and a substring of the specified bit
     * string (other) by the specified number of bits (nBits), filling any vacated
     * positions by the specified fill value (fill).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a shiftLeft(|nbits|...) is
     * performed instead of a shiftRight.
     * 
     * This {@code BitString} and the other bit string are modified by this
     * operation (see {@link #shiftRight(int, boolean, BitString)} for details).
     * 
     * The other substring starts at offset 'otherOffset' of the specified bit
     * string and has a length of 'otherLength'.
     * 
     * @param nBits       number of bits to shift
     * @param fill        the fill value
     * @param other       the other bit string
     * @param otherOffset the start of the other substring
     * @param otherLength the length of the other substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code otherOffset < 0 || otherOffset > 0 && otherOffset >= other.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code otherLength < 0 || otherLength > other.length() - otherOffset}
     */
    public BitString shiftRight(int nBits, boolean fill,
            BitString other, int otherOffset, int otherLength) {
        other.checkArgOffset(otherOffset);
        other.checkArgLength(otherOffset, otherLength);
        iShiftRight(nBits, fill, 0, this.length(), other, otherOffset, otherLength);
        return this;
    }
    
    /**
     * Shifts right this {@code BitString} and a Field of the specified bit string
     * (other) by the specified number of bits (nBits), filling any vacated
     * positions by the specified fill value (fill).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a shiftLeft(|nbits|...) is
     * performed instead of a shiftRight.
     * 
     * This {@code BitString} and the other bit string are modified by this
     * operation (see {@link #shiftRight(int, boolean, BitString)} for details).
     * 
     * @param nBits      number of bits to shift
     * @param fill       the fill value
     * @param other      the other bit string
     * @param otherField a Field of the other substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code otherField.offset() > 0 && otherField.offset() >= other.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code otherField.length() > other.length() - otherField.offset()}
     */
    public BitString shiftRight(int nBits, boolean fill,
            BitString other, Field otherField) {
        return shiftRight(nBits, fill, other, otherField.offset(), otherField.length());
    }
    
    /**
     * Shifts Right a substring of this {@code BitString} and the specified bit
     * string (other) by the specified number of bits (nBits), filling any vacated
     * positions by the specified fill value (fill).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a shiftLeft(|nbits|...) is
     * performed instead of a shiftRight.
     * 
     * This {@code BitString} and the other bit string are modified by this
     * operation (see {@link #shiftRight(int, boolean, BitString)} for details).
     * 
     * This substring starts at offset 'thisOffset' of this {@code BitString} and
     * extends to the end of this {@code BitString}.
     * 
     * @param nBits      number of bits to shift
     * @param fill       the fill value
     * @param thisOffset the start of this substring
     * @param other      the other bit string
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     */
    public BitString shiftRight(int nBits, boolean fill,
            int thisOffset,
            BitString other) {
        checkThisOffset(thisOffset);
        iShiftRight(nBits, fill, thisOffset, this.length() - thisOffset, other, 0, other.length());
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
     * extends to the end of this {@code BitString}.
     * 
     * The other substring starts at offset 'otherOffset' of the specified bit
     * string and extends to the end of the specified bit string.
     * 
     * @param nBits       number of bits to shift
     * @param fill        the fill value
     * @param thisOffset  the start of this substring
     * @param other       the other bit string
     * @param otherOffset the start of the other substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code otherOffset < 0 || otherOffset > 0 && otherOffset >= other.length()}
     */
    public BitString shiftRight(int nBits, boolean fill,
            int thisOffset,
            BitString other, int otherOffset) {
        checkThisOffset(thisOffset);
        other.checkArgOffset(otherOffset);
        iShiftRight(nBits, fill, thisOffset, this.length() - thisOffset, other, otherOffset, other.length() - otherOffset);
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
     * extends to the end of this {@code BitString}.
     * 
     * The other substring starts at offset 'otherOffset' of the specified bit
     * string and has a length of 'otherLength'.
     * 
     * @param nBits       number of bits to shift
     * @param fill        the fill value
     * @param thisOffset  the start of this substring
     * @param other       the other bit string
     * @param otherOffset the start of the other substring
     * @param otherLength the length of the other substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code otherOffset < 0 || otherOffset > 0 && otherOffset >= other.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code otherLength < 0 || otherLength > other.length() - otherOffset}
     */
    public BitString shiftRight(int nBits, boolean fill,
            int thisOffset,
            BitString other, int otherOffset, int otherLength) {
        checkThisOffset(thisOffset);
        other.checkArgOffset(otherOffset);
        other.checkArgLength(otherOffset, otherLength);
        iShiftRight(nBits, fill, thisOffset, this.length() - thisOffset, other, otherOffset, otherLength);
        return this;
    }
    
    /**
     * Shifts right a substring of this {@code BitString} and a Field of the
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
     * extends to the end of this {@code BitString}.
     * 
     * @param nBits      number of bits to shift
     * @param fill       the fill value
     * @param thisOffset the start of this substring
     * @param other      the other bit string
     * @param otherField a Field of the other substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code otherField.offset() > 0 && otherField.offset() >= other.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code otherField.length() > other.length() - otherField.offset()}
     */
    public BitString shiftRight(int nBits, boolean fill,
            int thisOffset,
            BitString other, Field otherField) {
        return shiftRight(nBits, fill, thisOffset, other, otherField.offset(), otherField.length());
    }
    
    /**
     * Shifts Right a substring of this {@code BitString} and the specified bit
     * string (other) by the specified number of bits (nBits), filling any vacated
     * positions by the specified fill value (fill).
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
     * @param nBits      number of bits to shift
     * @param fill       the fill value
     * @param thisOffset the start of this substring
     * @param thisLength the length of this substring
     * @param other      the other bit string
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisLength < 0 || thisLength > this.length() - thisOffset}
     */
    public BitString shiftRight(int nBits, boolean fill,
            int thisOffset, int thisLength,
            BitString other) {
        checkThisOffset(thisOffset);
        checkThisLength(thisOffset, thisLength);
        iShiftRight(nBits, fill, thisOffset, thisLength, other, 0, other.length());
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
     * string and extends to the end of the specified bit string.
     * 
     * @param nBits       number of bits to shift
     * @param fill        the fill value
     * @param thisOffset  the start of this substring
     * @param thisLength  the length of this substring
     * @param other       the other bit string
     * @param otherOffset the start of the other substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code otherOffset < 0 || otherOffset > 0 && otherOffset >= other.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisLength < 0 || thisLength > this.length() - thisOffset}
     */
    public BitString shiftRight(int nBits, boolean fill,
            int thisOffset, int thisLength,
            BitString other, int otherOffset) {
        checkThisOffset(thisOffset);
        checkThisLength(thisOffset, thisLength);
        other.checkArgOffset(otherOffset);
        iShiftRight(nBits, fill, thisOffset, thisLength, other, otherOffset, other.length() - otherOffset);
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
     * Shifts right a substring of this {@code BitString} and a Field of the
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
     * @param nBits      number of bits to shift
     * @param fill       the fill value
     * @param thisOffset the start of this substring
     * @param thisLength the length of this substring
     * @param other      the other bit string
     * @param otherField a Field of the other substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisOffset < 0 || thisOffset > 0 && thisOffset >= this.length()}
     *                                         or
     *                                         {@code otherField.offset() > 0 && otherField.offset() >= other.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisLength < 0 || thisLength > this.length() - thisOffset}
     *                                         or
     *                                         {@code otherField.length() > other.length() - otherField.offset()}
     */
    public BitString shiftRight(int nBits, boolean fill,
            int thisOffset, int thisLength,
            BitString other, Field otherField) {
        return shiftRight(nBits, fill, thisOffset, thisLength, other, otherField.offset(), otherField.length());
    }
    
    /**
     * Shifts right a Field of this {@code BitString} and the specified bit string
     * (other) by the specified number of bits (nBits), filling any vacated
     * positions by the specified fill value (fill).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a shiftLeft(|nbits|...) is
     * performed instead of a shiftRight.
     * 
     * This {@code BitString} and the other bit string are modified by this
     * operation (see {@link #shiftRight(int, boolean, BitString)} for details).
     * 
     * @param nBits     number of bits to shift
     * @param fill      the fill value
     * @param thisField a Field of this {@code BitString}
     * @param other     the other bit string
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisField.offset() > 0 && thisField.offset() >= this.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisField.length() > this.length() - thisField.offset()}
     */
    public BitString shiftRight(int nBits, boolean fill,
            Field thisField,
            BitString other) {
        return shiftRight(nBits, fill, thisField.offset(), thisField.length(), other);
    }
    
    /**
     * Shifts right a Field of this {@code BitString} and a substring of the
     * specified bit string (other) by the specified number of bits (nBits), filling
     * any vacated positions by the specified fill value (fill).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a shiftLeft(|nbits|...) is
     * performed instead of a shiftRight.
     * 
     * This {@code BitString} and the other bit string are modified by this
     * operation (see {@link #shiftRight(int, boolean, BitString)} for details).
     * 
     * The other substring starts at offset 'otherOffset' of the specified bit
     * string and extends to the end of the specified bit string.
     * 
     * @param nBits       number of bits to shift
     * @param fill        the fill value
     * @param thisField   a Field of this {@code BitString}
     * @param other       the other bit string
     * @param otherOffset the start of the other substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisField.offset() > 0 && thisField.offset() >= this.length()}
     *                                         or
     *                                         {@code otherOffset < 0 || otherOffset > 0 && otherOffset >= other.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisField.length() > this.length() - thisField.offset()}
     */
    public BitString shiftRight(int nBits, boolean fill,
            Field thisField,
            BitString other, int otherOffset) {
        return shiftRight(nBits, fill, thisField.offset(), thisField.length(), other, otherOffset);
    }
    
    /**
     * Shifts right a Field of this {@code BitString} and a substring of the
     * specified bit string (other) by the specified number of bits (nBits), filling
     * any vacated positions by the specified fill value (fill).
     * 
     * If nBits is negative (including Integer.MIN_VALUE), a shiftLeft(|nbits|...) is
     * performed instead of a shiftRight.
     * 
     * This {@code BitString} and the other bit string are modified by this
     * operation (see {@link #shiftRight(int, boolean, BitString)} for details).
     * 
     * The other substring starts at offset 'otherOffset' of the specified bit
     * string and has a length of 'otherLength'.
     * 
     * @param nBits       number of bits to shift
     * @param fill        the fill value
     * @param thisField   a Field of this {@code BitString}
     * @param other       the other bit string
     * @param otherOffset the start of the other substring
     * @param otherLength the length of the other substring
     * @return this {@code BitString}
     * @throws StringIndexOutOfBoundsException if
     *                                         {@code thisField.offset() > 0 && thisField.offset() >= this.length()}
     *                                         or
     *                                         {@code otherOffset < 0 || otherOffset > 0 && otherOffset >= other.length()}
     * @throws IllegalArgumentException        if
     *                                         {@code thisField.length() > this.length() - thisField.offset()}
     *                                         or
     *                                         {@code otherLength < 0 || otherLength > other.length() - otherOffset}
     */
    public BitString shiftRight(int nBits, boolean fill,
            Field thisField,
            BitString other, int otherOffset, int otherLength) {
        other.checkArgOffset(otherOffset);
        other.checkArgLength(otherOffset, otherLength);
        iShiftRight(nBits, fill, thisField.offset(), thisField.length(), other, otherOffset, otherLength);
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
        return shiftRight(nBits, fill, thisField.offset(), thisField.length(), other, otherField.offset(), otherField.length());
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
        final BitString substring = new BitString(length);
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
        return substring(field.offset(), field.length());
    }
    
    public boolean[] toBooleanArray() {
        return unwrapBooleans(0, length());
    }
    
    public boolean[] toBooleanArray(int offset) {
        checkThisOffset(offset);
        return unwrapBooleans(offset, length() - offset);
    }
    
    public boolean[] toBooleanArray(int offset, int length) {
        checkThisOffset(offset);
        checkThisLength(offset, length);
        return unwrapBooleans(offset, length);
    }
    
    public boolean[] toBooleanArray(Field field) {
        return toBooleanArray(field.offset(), field.length());
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
        return unwrapBytes(0, length());
    }
    
    public byte[] toByteArray(int offset) {
        checkThisOffset(offset);
        return unwrapBytes(offset, length() - offset);
    }
    
    public byte[] toByteArray(int offset, int length) {
        checkThisOffset(offset);
        checkThisLength(offset, length);
        return unwrapBytes(offset, length);
    }
    
    public byte[] toByteArray(Field field) {
        return toByteArray(field.offset(), field.length());
    }
    
    public char[] toCharArray() {
        return unwrapChars(0, length());
    }
    
    public char[] toCharArray(int offset) {
        checkThisOffset(offset);
        return unwrapChars(offset, length() - offset);
    }
    
    public char[] toCharArray(int offset, int length) {
        checkThisOffset(offset);
        checkThisLength(offset, length);
        return unwrapChars(offset, length);
    }
    
    public char[] toCharArray(Field field) {
        return toCharArray(field.offset(), field.length());
    }
    
    public double[] toDoubleArray() {
        return unwrapDoubles(0, length());
    }
    
    public double[] toDoubleArray(int offset) {
        checkThisOffset(offset);
        return unwrapDoubles(offset, length() - offset);
    }
    
    public double[] toDoubleArray(int offset, int length) {
        checkThisOffset(offset);
        checkThisLength(offset, length);
        return unwrapDoubles(offset, length);
    }
    
    public double[] toDoubleArray(Field field) {
        return toDoubleArray(field.offset(), field.length());
    }
    
    public float[] toFloatArray() {
        return unwrapFloats(0, length());
    }
    
    public float[] toFloatrArray(int offset) {
        checkThisOffset(offset);
        return unwrapFloats(offset, length() - offset);
    }
    
    public float[] toFloatArray(int offset, int length) {
        checkThisOffset(offset);
        checkThisLength(offset, length);
        return unwrapFloats(offset, length);
    }
    
    public float[] toFloatArray(Field field) {
        return toFloatArray(field.offset(), field.length());
    }
    
    public int[] toIntArray() {
        return unwrapInts(0, length());
    }
    
    public int[] toIntArray(int offset) {
        checkThisOffset(offset);
        return unwrapInts(offset, length() - offset);
    }
    
    public int[] toIntArray(int offset, int length) {
        checkThisOffset(offset);
        checkThisLength(offset, length);
        return unwrapInts(offset, length);
    }
    
    public int[] toIntArray(Field field) {
        return toIntArray(field.offset(), field.length());
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
        return unwrapLongs(0, length());
    }
    
    public long[] toLongArray(int offset) {
        checkThisOffset(offset);
        return unwrapLongs(offset, length() - offset);
    }
    
    public long[] toLongArray(int offset, int length) {
        checkThisOffset(offset);
        checkThisLength(offset, length);
        return unwrapLongs(offset, length);
    }
    
    public long[] toLongArray(Field field) {
        return toLongArray(field.offset(), field.length());
    }
    
    public short[] toShortArray() {
        return unwrapShorts(0, length());
    }
    
    public short[] toShortArray(int offset) {
        checkThisOffset(offset);
        return unwrapShorts(offset, length() - offset);
    }
    
    public short[] toShortArray(int offset, int length) {
        checkThisOffset(offset);
        checkThisLength(offset, length);
        return unwrapShorts(offset, length);
    }
    
    public short[] toShortArray(Field field) {
        return toShortArray(field.offset(), field.length());
    }
    
    @Override
    public String toString() {
        return toString(0);
    }
    
    public String toString(int offset) {
        checkThisOffset(offset);
        return toString(offset, this.length() - offset);
    }
    
    public String toString(int offset, int length) {
        checkThisOffset(offset);
        checkThisLength(offset, length);
        if (length == 0) return "";
        final StringBuilder string = new StringBuilder(wordIndex(length)+1);
        final int[] iterator = getIterator(offset, length);
        while (hasNextIteratorWord(iterator)) {
            long word = getNextIteratorFullWord(iterator);
            String wordString = String.format("%64s", Long.toBinaryString(word)).replace(' ', '0');
            int wordBitCount = getIteratorWordBitCount(iterator);
            if (wordBitCount < BITS_PER_WORD) wordString = wordString.substring(0, wordBitCount);
            string.append(wordString);
        }
        return string.toString();
    }
    
    public String toString(Field field) {
        return toString(field.offset(), field.length());
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
        return hashCode(0, length());
    }
    
    public int hashCode(int offset) {
        checkThisOffset(offset);
        return hashCode(offset, length() - offset);
    }
    
    public int hashCode(int offset, int length) {
        checkThisOffset(offset);
        checkThisLength(offset, length);
        long hashcode = 1234;
        final int[] iterator = getIterator(offset, length);
        while (hasNextIteratorWord(iterator)) {
            final long word = getNextIteratorFullWord(iterator);
            hashcode ^= word * (getIteratorWordIndex(iterator) + 1);
        }
        return (int)((hashcode >> 32) ^ hashcode);
    }
    
    public int hashCode(Field field) {
        return hashCode(field.offset(), field.length());
    }

    public static class Field {
        
        private final int offset;
        private final int length;
        
        public Field(int offset, int length) {
            this.offset = offset;
            this.length = length;
        }
        
        public int offset() {
            return this.offset;
        }
        
        public int length() {
            return this.length;
        }
        
        private void checkOffset(int offset) {
            if (offset < 0 || offset >= length()) {
                throw new StringIndexOutOfBoundsException(
                        "specified offset is invalid for this field; offset=" + offset + ", field length=" + length()); 
            }
        }
        
        public static Field indexRange(int fromIndex, int toIndex) {
            if (fromIndex < 0) throw new IndexOutOfBoundsException("fromIndex is negative; index="+fromIndex);
            if (toIndex < 0) throw new IndexOutOfBoundsException("toIndex is negative; index="+toIndex);
            if (fromIndex > toIndex) throw new IndexOutOfBoundsException("fromIndex > toIndex; from="+fromIndex+", to="+toIndex);
            return new Field(fromIndex, toIndex-fromIndex);
        }
        
    }
    
    private static class Constant extends BitString implements java.io.Externalizable {
        
        private final long constant;
        
        private Constant(long constant) {
            super(0);
            this.constant = constant;
        }
        
        @Override
        public Object clone() {
            return (Constant)super.clone();
        }
        
        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            throw new java.io.InvalidObjectException("not serializable");
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
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
        public int length() {
            return Integer.MAX_VALUE;
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
    
    private static class Range extends BitString implements java.io.Externalizable {
        
        private final BitString base;
        private final BitString parent;
        private final int firstBitIndex;
        private int length;
        private long expectantModCount;
        
        private Range(BitString base, BitString parent, int firstBitIndex, int length) {
            super(0);
            this.base = base;
            this.parent = parent;
            this.firstBitIndex = firstBitIndex;
            this.length = length;
            this.expectantModCount = modCount();
        }
        
        @Override
        public Object clone() {
            checkForModificationException();
            return (Range)super.clone();
        }
        
        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            throw new java.io.InvalidObjectException("not serializable");
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
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
        
        private String noCapacityMsg() {
            return "BitString Ranges have no capacity";
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
        public int length() {
            checkForModificationException();
            return this.length;
        }
        
        @Override
        int baseLength() {
            return base.baseLength();
        }
        
//        @Override
//        void iSetLength(int newLength) {
//            checkForModificationException();
//            base.iSetLength(newLength);
//        }
        
        @Override
        public void setLength(int newLength) {
            checkForModificationException();
            if (newLength < 0) throw new IllegalArgumentException("specified length is negative: "+newLength);
            final int lengthDelta = newLength - length();
            int bitIndex = super.lastBitIndex() + 1;
            if (lengthDelta < 0) bitIndex += lengthDelta;
            parent.setRangeLength(lengthDelta, bitIndex);
            updateExpectantModCountAndLength(lengthDelta);
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
        
        void updateExpectantModCountAndLength(int lengthDelta) {
            this.length += lengthDelta;
            this.expectantModCount = modCount();
        }
        
        void checkForModificationException() {
            if (this.expectantModCount != modCount()) {
                throw new ConcurrentModificationException();
            }
        }
        
        @Override
        int bitIndex(int offset) {
            return firstBitIndex + offset;
        }
        
//        @Override
//        public byte[] toByteArray(int offset, int length) {
//            checkForModificationException();
//            return super.toByteArray(offset, length);
//        }
        
//        @Override
//        public long[] toLongArray(int offset, int length) {
//            checkForModificationException();
//            return super.toLongArray(offset, length);
//        }
        
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
        
//        @Override
//        void iOp(LongBinaryOperator op, boolean fill,
//                int thisOffset, int length, BitString that, int thatOffset) {
//            checkForModificationException();
//            super.iOp(op, fill, thisOffset, length, that, thatOffset);
//        }
        
//        @Override
//        boolean iPredicate(LongBiPredicate op, boolean dflt, boolean fill,
//                int thisOffset, int length, BitString that, int thatOffset) {
//            checkForModificationException();
//            return super.iPredicate(op, dflt, fill, thisOffset, length, that, thatOffset);
//            
//        }
        
//        @Override
//        void iCopyFromFrontOf(int thisOffset, int thisLength, BitString that, int thatOffset, int thatLength) {
//            checkForModificationException();
//            super.iCopyFromFrontOf(thisOffset, thisLength, that, thatOffset, thatLength);
//        }
        
//        @Override
//        void iCopyFromBackOf(int thisOffset, int thisLength, BitString that, int thatOffset, int thatLength) {
//            checkForModificationException();
//            super.iCopyFromBackOf(thisOffset, thisLength, that, thatOffset, thatLength);
//        }
        
//        @Override
//        void iReverse(int offset, int length) {
//            checkForModificationException();
//            super.iReverse(offset, length);
//        }
        
//        @Override
//        void iRotateLeft(int nBits, int offset, int length) {
//            checkForModificationException();
//            super.iRotateLeft(nBits, offset, length);
//        }
        
//        @Override
//        void iRotateLeft(int nBits, int thisOffset, int thisLength, BitString that, int thatOffset, int thatLength) {
//            checkForModificationException();
//            super.iRotateLeft(nBits, thisOffset, thisLength, that, thatOffset, thatLength);
//        }
        
//        @Override
//        void iRotateRight(int nBits, int offset, int length) {
//            checkForModificationException();
//            super.iRotateRight(nBits, offset, length);
//        }
        
//        @Override
//        void iRotateRight(int nBits, int thisOffset, int thisLength, BitString that, int thatOffset, int thatLength) {
//            checkForModificationException();
//            super.iRotateRight(nBits, thisOffset, thisLength, that, thatOffset, thatLength);
//        }
        
//        @Override
//        void iShiftLeft(int nBits, boolean fill, int offset, int length) {
//            checkForModificationException();
//            super.iShiftLeft(nBits, fill, offset, length);
//        }
        
//        @Override
//        void iShiftLeft(int nBits, boolean fill,
//                int thisOffset, int thisLength,
//                BitString that, int thatOffset, int thatLength) {
//            checkForModificationException();
//            super.iShiftLeft(nBits, fill, thisOffset, thisLength, that, thatOffset, thatLength);
//        }
        
//        @Override
//        void iShiftRight(int nBits, boolean fill, int offset, int length) {
//            checkForModificationException();
//            super.iShiftRight(nBits, fill, offset, length);
//        }
        
//        @Override
//        void iShiftRight(int nBits, boolean fill,
//                int thisOffset, int thisLength,
//                BitString that, int thatOffset, int thatLength) {
//            checkForModificationException();
//            super.iShiftRight(nBits, fill, thisOffset, thisLength, that, thatOffset, thatLength);
//        }
        
//        @Override
//        long iGetPrimitive(int offset, int size, String name) {
//            checkForModificationException();
//            return super.iGetPrimitive(offset, size, name);
//        }
        
//        @Override
//        void iPutPrimitive(int offset, int size, String name, BitString primitiveBits) {
//            checkForModificationException();
//            super.iPutPrimitive(offset, size, name, primitiveBits);
//        }
        
        @Override
        public BitString append(BitString that, int thatOffset, int thatLength) {
            that.checkArgOffset(thatOffset);
            that.checkArgLength(thatOffset, thatLength);
            base.checkBaseLengthIncrease(thatLength);
            iInsert(super.lastBitIndex()+1, that, thatOffset, thatLength);
            return this;
        }
        
//        @Override
//        public BitString clearBit(int offset) {
//            checkForModificationException();
//            return super.clearBit(offset);
//        }
        
//        @Override
//        public BitString flipBit(int offset) {
//            checkForModificationException();
//            return super.flipBit(offset);
//        }
        
//        @Override
//        public BitString get(int offset, int length) {
//            checkForModificationException();
//            return super.get(offset, length);
//        }
        
//        @Override
//        public Bit getBit(int offset) {
//            checkForModificationException();
//            return super.getBit(offset); 
//        }
        
//        @Override
//        public BitString setBit(int offset) {
//            checkForModificationException();
//            return super.setBit(offset);
//        }
        
//        @Override
//        public int bitCount(int offset, int length) {
//            checkForModificationException();
//            return super.bitCount(offset, length);
//        }
        
//        @Override
//        public int nextClearBit(int fromIndex, int length) {
//            checkForModificationException();
//            return super.nextClearBit(fromIndex, length);
//        }
        
//        @Override
//        public int nextSetBit(int fromIndex, int length) {
//            checkForModificationException();
//            return super.nextSetBit(fromIndex, length);
//        }
        
//        @Override
//        public int previousClearBit(int fromIndex, int length) {
//            checkForModificationException();
//            return super.previousClearBit(fromIndex, length);
//        }
        
//        @Override
//        public int previousSetBit(int fromIndex, int length) {
//            checkForModificationException();
//            return super.previousSetBit(fromIndex, length);
//        }
        
        @Override
        public BitString range(int offset, int length) {
            checkForModificationException();
            super.checkThisOffset(offset);
            super.checkThisLength(offset, length);
            return new Range(base, this, super.firstBitIndex(offset), length);
        }
        
//        @Override
//        public BitString substring(int offset, int length) {
//            checkForModificationException();
//            return super.substring(offset, length);
//        }
        
//        @Override
//        public String toString(int offset, int length) {
//            checkForModificationException();
//            return super.toString(offset, length);
//        }
        
//        @Override
//        public int hashCode(int offset, int length) {
//            checkForModificationException();
//            return super.hashCode(offset, length);
//        }
        
    }

}
