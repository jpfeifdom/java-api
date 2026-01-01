/* Copyright (C) 2026 James R. Pfeifer. All rights reserved.
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

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.Arrays;

public class LongBitString extends BitString {
    
    /**
     * 
     */
    private static final long serialVersionUID = -2708453565589332571L;
    
    private static final int ADDRESS_BITS_PER_WORD = 6;
    
    /**
     * The internal field corresponding to the serialField "bits".
     */
    private long[] backingArray;

    /**
     * Creates a new {@code LongBitString} of length 0. The capacity of the new
     * {@code LongBitString} will equal Long.SIZE (64 bits).
     */
    public LongBitString() {
        super(0);
        initBackingArray(Long.SIZE);
     }

     /**
      * Creates a new {@code LongBitString} with the specified length. The capacity of
      * the new {@code LongBitString} will equal the nearest multiple of Long.SIZE (64
      * bits) greater than or equal to the length. All bits are initially set to
      * {@code ZERO}.
      *
      * @param length the initial length of the new {@code BitString}
      * @throws IllegalArgumentException if the specified length is negative
      */
    public LongBitString(int length) {
        super(length);
        initBackingArray(length);
    }
    
    /**
     * Creates a new {@code LongBitString} with the specified length and capacity. The
     * specified capacity must be equal to or greater than the specified length. The
     * actual capacity is rounded up to the nearest multiple of Long.SIZE (64
     * bits). All bits are initially set to {@code ZERO}.
     * 
     * @param length   the initial length of the new {@code BitString}
     * @param capacity the initial capacity of the new {@code BitString}
     * @throws IllegalArgumentException if the specified length is negative, or if
     *                                  the specified capacity is less than the
     *                                  length
     */
    public LongBitString(int length, int capacity) {
        super(length);
        if (capacity < length) {
            throw new IllegalArgumentException("capacity (" + capacity + ") < length (" + length + ")");
        }
        initBackingArray(capacity);
    }
    
    /**
     * Creates a bit string using longs as the internal representation.
     */
    LongBitString(long[] backingArray, int length) {
        super(length);
        this.backingArray = backingArray;
    }
    
    @Override
    LongBitString newBitString(int length) {
        return new LongBitString(length);
    }

    private void initBackingArray(int capacity) {
        this.backingArray = new long[longIndex(capacity-1) + 1];
    }
    
    @Override
    void resizeBackingArray(int capacity) {
        final int newArrayLength = longIndex(capacity-1) + 1;
        if (newArrayLength == backingArray.length) return;
        backingArray = Arrays.copyOf(backingArray, newArrayLength);
    }
    
    @Override
    public int capacity() {
        return backingArray.length >= MAX_LONGS
                ? Integer.MAX_VALUE
                : backingArray.length * Long.SIZE;
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
    public LongBitString clone() {
        LongBitString clone = (LongBitString) super.clone();
        clone.backingArray = backingArray.clone();
        return clone;
    }
//    public LongBitString clone() {
//        try {
//            LongBitString clone = (LongBitString) super.clone();
//            clone.backingArray = backingArray.clone();
//            return clone;
//        } catch (CloneNotSupportedException e) {
//            throw new InternalError(e);
//        }
//    }
    
    /**
     * Returns a new BitString containing all the bits in the given boolean array.
     * <p>
     * More precisely, <br>
     * {@code BitString.valueOf(booleans).getBit(n) == booleans[n]}<br>
     * for all {@code n < booleans.length}.
     * <p>
     * The length of the new BitString will equal the length of the boolean array.
     * The capacity will be the least multiple of Long.SIZE (64 bits) equal to
     * or greater than the length.
     *
     * @param booleans a boolean array representing a sequence of bits to be used as
     *                 the initial bits of the new BitString
     * @return a {@code BitString} containing all the bits in the boolean array
     */
    public static LongBitString valueOf(boolean[] booleans) {
        return new LongBitString(packBooleans(booleans), booleans.length);
    }
    
    /**
     * Returns a new BitString containing all the bits in the given byte array.
     * <p>
     * More precisely, <br>
     * {@code BitString.valueOf(bytes).getBit(n) == ((bytes[n/8] & (0x80>>>(n%8))) != 0)}<br>
     * for all {@code n < 8 * bytes.length}.
     * <p>
     * The length of the new BitString will equal the length of the byte array x 8.
     * The capacity will be the least multiple of Long.SIZE (64 bits) equal to
     * or greater than the length.
     * <p>
     * This method is equivalent to {@code BitSet.valueOf(ByteBuffer.wrap(bytes))}.
     *
     * @param bytes a byte array containing a big-endian representation of a
     *              sequence of bits to be used as the initial bits of the new
     *              BitString
     * @return a {@code BitString} containing all the bits in the byte array
     */
    public static LongBitString valueOf(byte[] bytes) {
        //checkNewBitStringLength(bytes.length * (long)Byte.SIZE);
        if (bytes.length > MAX_BYTES) throw new IllegalArgumentException("byte array is too large");
        final int length = (bytes.length == MAX_BYTES) ? Integer.MAX_VALUE : bytes.length * Byte.SIZE;
        return new LongBitString(packBytes(bytes), length);
    }
    
    /**
     * Returns a new BitString containing all the bits in the given char array.
     * <p>
     * More precisely, <br>
     * {@code BitString.valueOf(chars).getBit(n) == ((chars[n/16] & (0x8000>>>(n%16))) != 0)}<br>
     * for all {@code n < 16 * chars.length}.
     * <p>
     * This method is equivalent to {@code BitSet.valueOf(CharBuffer.wrap(chars))}.
     *
     * @param chars a char array containing a big-endian representation of a
     *              sequence of bits to be used as the initial bits of the new
     *              BitString
     * @return a {@code BitString} containing all the bits in the char array
     */
    public static LongBitString valueOf(char[] chars) {
        //checkNewBitStringLength(chars.length * (long)Character.SIZE);
        if (chars.length > MAX_CHARS) throw new IllegalArgumentException("char array is too large");
        final int length = (chars.length == MAX_CHARS) ? Integer.MAX_VALUE : chars.length * Character.SIZE;
        return new LongBitString(packChars(chars), length);
    }
    
    /**
     * Returns a new BitString containing all the bits in the given double array.
     * <p>
     * More precisely, <br>
     * {@code BitString.valueOf(doubles).getBit(n) == ((doubles[n/64] & (0x8000000000000000L>>>(n%64))) != 0)}<br>
     * for all {@code n < 64 * doubles.length}.
     * <p>
     * This method is equivalent to
     * {@code BitSet.valueOf(DoubleBuffer.wrap(doubles))}.
     *
     * @param doubles a double array containing a big-endian representation of a
     *                sequence of bits to be used as the initial bits of the new
     *                BitString
     * @return a {@code BitString} containing all the bits in the double array
     */
    public static LongBitString valueOf(double[] doubles) {
        //checkNewBitStringLength(doubles.length * (long)Double.SIZE);
        if (doubles.length > MAX_DOUBLES) throw new IllegalArgumentException("double array is too large");
        final int length = (doubles.length == MAX_DOUBLES) ? Integer.MAX_VALUE : doubles.length * Double.SIZE;
        return new LongBitString(packDoubles(doubles), length);
    }
    
    /**
     * Returns a new BitString containing all the bits in the given float array.
     * <p>
     * More precisely, <br>
     * {@code BitString.valueOf(floats).getBit(n) == ((floats[n/32] & (0x80000000>>>(n%32))) != 0)}<br>
     * for all {@code n < 32 * floats.length}.
     * <p>
     * This method is equivalent to
     * {@code BitSet.valueOf(FloatBuffer.wrap(floats))}.
     *
     * @param floats a float array containing a big-endian representation of a
     *               sequence of bits to be used as the initial bits of the new
     *               BitString
     * @return a {@code BitString} containing all the bits in the float array
     */
    public static LongBitString valueOf(float[] floats) {
        //checkNewBitStringLength(floats.length * (long)Float.SIZE);
        if (floats.length > MAX_FLOATS) throw new IllegalArgumentException("float array is too large");
        final int length = (floats.length == MAX_FLOATS) ? Integer.MAX_VALUE : floats.length * Float.SIZE;
        return new LongBitString(packFloats(floats), length);
    }
    
    /**
     * Returns a new BitString containing all the bits in the given int array.
     * <p>
     * More precisely, <br>
     * {@code BitString.valueOf(ints).getBit(n) == ((ints[n/32] & (0x80000000>>>(n%32))) != 0)}<br>
     * for all {@code n < 32 * ints.length}.
     * <p>
     * This method is equivalent to {@code BitSet.valueOf(IntBuffer.wrap(ints))}.
     *
     * @param ints a int array containing a big-endian representation of a sequence
     *             of bits to be used as the initial bits of the new BitString
     * @return a {@code BitString} containing all the bits in the int array
     */
    public static LongBitString valueOf(int[] ints) {
        //checkNewBitStringLength(ints.length * (long)Integer.SIZE);
        if (ints.length > MAX_INTS) throw new IllegalArgumentException("int array is too large");
        final int length = (ints.length == MAX_INTS) ? Integer.MAX_VALUE : ints.length * Integer.SIZE;
        return new LongBitString(packInts(ints), length);
    }
    
    /**
     * Returns a new BitString containing all the bits in the given long array.
     * <p>
     * More precisely, <br>
     * {@code BitString.valueOf(longs).getBit(n) == ((longs[n/64] & (0x8000000000000000L>>>(n%64))) != 0)}<br>
     * for all {@code n < 64 * longs.length}.
     * <p>
     * This method is equivalent to {@code BitSet.valueOf(LongBuffer.wrap(longs))}.
     *
     * @param longs a long array containing a big-endian representation of a
     *              sequence of bits to be used as the initial bits of the new
     *              BitString
     * @return a {@code BitString} containing all the bits in the long array
     */
    public static LongBitString valueOf(long[] longs) {
        //checkNewBitStringLength(longs.length * (long)Long.SIZE);
        if (longs.length > MAX_LONGS) throw new IllegalArgumentException("long array is too large");
        final int length = (longs.length == MAX_LONGS) ? Integer.MAX_VALUE : longs.length * Long.SIZE;
        return new LongBitString(Arrays.copyOf(longs, longs.length), length);
    }
    
    /**
     * Returns a new BitString containing all the bits in the given short array.
     * <p>
     * More precisely, <br>
     * {@code BitString.valueOf(shorts).getBit(n) == ((shorts[n/16] & (x08000>>>(n%16))) != 0)}<br>
     * for all {@code n < 16 * shorts.length}.
     * <p>
     * This method is equivalent to
     * {@code BitSet.valueOf(ShortBuffer.wrap(shorts))}.
     *
     * @param shorts a short array containing a big-endian representation of a
     *               sequence of bits to be used as the initial bits of the new
     *               BitString
     * @return a {@code BitString} containing all the bits in the short array
     */
    public static LongBitString valueOf(short[] shorts) {
        //checkNewBitStringLength(shorts.length * (long)Short.SIZE);
        if (shorts.length > MAX_SHORTS) throw new IllegalArgumentException("short array is too large");
        final int length = (shorts.length == MAX_SHORTS) ? Integer.MAX_VALUE : shorts.length * Short.SIZE;
        return new LongBitString(packShorts(shorts), length);
    }
    
    /**
     * Returns a new BitString containing all the bits in the given long buffer
     * between its position and limit.
     * <p>
     * More precisely, <br>
     * {@code BitString.valueOf(lb).getBit(n) == ((lb.get(lb.position()+n/64) & (0x8000000000000000L>>>(n%64))) != 0)}
     * <br>
     * for all {@code n < 64 * lb.remaining()}.
     * <p>
     * The long buffer is not modified by this method, and no reference to the
     * buffer is retained by the BitString.
     *
     * @param lb a long buffer containing a big-endian representation of a sequence
     *           of bits between its position and limit, to be used as the initial
     *           bits of the new BitString
     * @return a {@code BitString} containing all the bits in the buffer in the
     *         specified range
     */
    public static LongBitString valueOf(LongBuffer lb) {
        lb = lb.slice();
        //checkNewBitStringLength(lb.remaining() * (long)Long.SIZE);
        if (lb.remaining() > MAX_LONGS) throw new IllegalArgumentException("LongBuffer array is too large");
        long[] longs = new long[lb.remaining()];
        lb.get(longs);
        final int length = (longs.length == MAX_LONGS) ? Integer.MAX_VALUE : longs.length * Long.SIZE;
        return new LongBitString(longs, length);
    }

    /**
     * Returns a new BitString containing all the bits in the given byte buffer
     * between its position and limit.
     * <p>
     * >More precisely, <br>
     * {@code BitString.valueOf(bb).getBit(n) == ((bb.get(bb.position()+n/8) & (0x80>>>(n%8))) != 0)}
     * <br>
     * for all {@code n < 8 * bb.remaining()}.
     * <p>
     * The byte buffer is not modified by this method, and no reference to the
     * buffer is retained by the BitString.
     *
     * @param bb a byte buffer containing a big-endian representation of a sequence
     *           of bits between its position and limit, to be used as the initial
     *           bits of the new BitString
     * @return a {@code BitString} containing all the bits in the buffer in the
     *         specified range
     */
    public static LongBitString valueOf(ByteBuffer bb) {
        bb = bb.slice();
        //checkNewBitStringLength(bb.remaining() * (long)Byte.SIZE);
        if (bb.remaining() > MAX_BYTES) throw new IllegalArgumentException("ByteBuffer array is too large");
        byte[] bytes = new byte[bb.remaining()];
        bb.get(bytes);
        final int length = (bytes.length == MAX_BYTES) ? Integer.MAX_VALUE : bytes.length * Byte.SIZE;
        return new LongBitString(packBytes(bytes), length);
    }
    
    static int longIndex(int bitIndex) {
        return bitIndex >> ADDRESS_BITS_PER_WORD;
    }
    
    @Override
    long getWord(int wordIndex) {
        assert wordIndex >= 0 && wordIndex < this.backingArray.length;
        return this.backingArray[wordIndex];
    }
    
    @Override
    void setWord(int wordIndex, long word) {
        assert wordIndex >= 0 && wordIndex < this.backingArray.length;
        backingArray[wordIndex] = word;
    }
    
//    /**
//     * Returns a substring of this {@code BitString}.
//     *
//     * The substring starts at offset 'offset' of this {@code BitString} and has a
//     * length of 'length'.
//     * 
//     * @param offset the start of this substring
//     * @param length the length of this substring
//     * @return a substring of this {@code BitString}
//     * @throws StringIndexOutOfBoundsException if
//     *                                         {@code offset < 0 || offset > 0 && offset >= length()}
//     * @throws IllegalArgumentException        if
//     *                                         {@code length < 0 || length > length() - offset}
//     */
//    @Override
//    public LongBitString substring(int offset, int length) {
//        checkThisOffset(offset);
//        checkThisLength(offset, length);
//        final LongBitString substring = new LongBitString(length);
//        substring.iCopy(0, length, this, offset);
//        return substring;
//    }

}
