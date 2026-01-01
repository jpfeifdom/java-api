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
import java.util.function.IntToLongFunction;

public class ByteBitString extends BitString {
    
    /**
     * 
     */
    private static final long serialVersionUID = -721880741388640972L;
    
    private static final int ADDRESS_BITS_PER_WORD = 3;
    
    /**
     * The internal field corresponding to the serialField "bits".
     */
    private byte[] backingArray;

    /**
     * Creates a new {@code BitString} of length 0. The capacity of the new
     * {@code BitString} will equal the word size (64 bits).
     */
    public ByteBitString() {
        super(0);
        initBackingArray(Byte.SIZE);
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
    public ByteBitString(int length) {
        super(length);
        initBackingArray(length);
    }
    
    /**
     * Creates a new {@code BitString} with the specified length and capacity. The
     * specified capacity must be equal to or greater than the specified length. The
     * actual capacity is rounded up to the nearest multiple of the word size (64
     * bits). All bits are initially set to {@code ZERO}.
     * 
     * @param length   the initial length of the new {@code BitString}
     * @param capacity the initial capacity of the new {@code BitString}
     * @throws IllegalArgumentException if the specified length is negative, or if
     *                                  the specified capacity is less than the
     *                                  length
     */
    public ByteBitString(int length, int capacity) {
        super(length);
        if (capacity < length) {
            throw new IllegalArgumentException("capacity (" + capacity + ") < length (" + length + ")");
        }
        initBackingArray(capacity);
    }
    
    /**
     * Creates a bit string using words as the internal representation.
     */
    ByteBitString(byte[] backingArray, int length) {
        super(length);
        this.backingArray = backingArray;
    }
    
    @Override
    ByteBitString newBitString(int length) {
        return new ByteBitString(length);
    }

    private void initBackingArray(int capacity) {
        this.backingArray = new byte[byteIndex(capacity-1) + 1];
    }
    
    @Override
    void resizeBackingArray(int capacity) {
        final int newArrayLength = byteIndex(capacity-1) + 1;
        if (newArrayLength == backingArray.length) return;
        backingArray = Arrays.copyOf(backingArray, newArrayLength);
    }
    
    @Override
    public int capacity() {
        return backingArray.length >= MAX_BYTES
                ? Integer.MAX_VALUE
                : backingArray.length * Byte.SIZE;
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
    public ByteBitString clone() {
        ByteBitString clone = (ByteBitString) super.clone();
        clone.backingArray = backingArray.clone();
        return clone;
    }
//    public ByteBitString clone() {
//        try {
//            ByteBitString clone = (ByteBitString) super.clone();
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
     * The capacity will be the least multiple of the word size (64 bits) equal to
     * or greater than the length.
     *
     * @param booleans a boolean array representing a sequence of bits to be used as
     *                 the initial bits of the new BitString
     * @return a {@code BitString} containing all the bits in the boolean array
     */
    public static ByteBitString valueOf(boolean[] booleans) {
        final byte[] bytes = new byte[(booleans.length + Byte.SIZE - 1) / Byte.SIZE];
        for (int i = 0, b = 0, p = 0;
                i < booleans.length;
                b = ++i / Byte.SIZE, p = (p+1) % Byte.SIZE) {
            if (booleans[i]) bytes[b] |= 0x80 >>> p;
        }
        return new ByteBitString(bytes, booleans.length);
    }
    
    /**
     * Returns a new BitString containing all the bits in the given byte array.
     * <p>
     * More precisely, <br>
     * {@code BitString.valueOf(bytes).getBit(n) == ((bytes[n/8] & (0x80>>>(n%8))) != 0)}<br>
     * for all {@code n < 8 * bytes.length}.
     * <p>
     * The length of the new BitString will equal the length of the byte array x 8.
     * The capacity will be the least multiple of the word size (64 bits) equal to
     * or greater than the length.
     * <p>
     * This method is equivalent to {@code BitSet.valueOf(ByteBuffer.wrap(bytes))}.
     *
     * @param bytes a byte array containing a big-endian representation of a
     *              sequence of bits to be used as the initial bits of the new
     *              BitString
     * @return a {@code BitString} containing all the bits in the byte array
     */
    public static ByteBitString valueOf(byte[] bytes) {
        //checkNewBitStringLength(bytes.length * (long)Byte.SIZE);
        if (bytes.length > MAX_BYTES) throw new IllegalArgumentException("byte array is too large");
        final int length = (bytes.length == MAX_BYTES) ? Integer.MAX_VALUE : bytes.length * Byte.SIZE;
        return new ByteBitString(Arrays.copyOf(bytes, bytes.length), length);
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
    public static ByteBitString valueOf(char[] chars) {
        //checkNewBitStringLength(chars.length * (long)Character.SIZE);
        if (chars.length > MAX_CHARS) throw new IllegalArgumentException("char array is too large");
        final int length = (chars.length == MAX_CHARS) ? Integer.MAX_VALUE : chars.length * Character.SIZE;
        return new ByteBitString(unpackChars(chars), length);
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
    public static ByteBitString valueOf(double[] doubles) {
        //checkNewBitStringLength(doubles.length * (long)Double.SIZE);
        if (doubles.length > MAX_DOUBLES) throw new IllegalArgumentException("double array is too large");
        final int length = (doubles.length == MAX_DOUBLES) ? Integer.MAX_VALUE : doubles.length * Double.SIZE;
        return new ByteBitString(unpackDoubles(doubles), length);
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
    public static ByteBitString valueOf(float[] floats) {
        //checkNewBitStringLength(floats.length * (long)Float.SIZE);
        if (floats.length > MAX_FLOATS) throw new IllegalArgumentException("float array is too large");
        final int length = (floats.length == MAX_FLOATS) ? Integer.MAX_VALUE : floats.length * Float.SIZE;
        return new ByteBitString(unpackFloats(floats), length);
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
    public static ByteBitString valueOf(int[] ints) {
        //checkNewBitStringLength(ints.length * (long)Integer.SIZE);
        if (ints.length > MAX_INTS) throw new IllegalArgumentException("int array is too large");
        final int length = (ints.length == MAX_INTS) ? Integer.MAX_VALUE : ints.length * Integer.SIZE;
        return new ByteBitString(unpackInts(ints), length);
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
    public static ByteBitString valueOf(long[] longs) {
        //checkNewBitStringLength(longs.length * (long)Long.SIZE);
        if (longs.length > MAX_LONGS) throw new IllegalArgumentException("long array is too large");
        final int length = (longs.length == MAX_LONGS) ? Integer.MAX_VALUE : longs.length * Long.SIZE;
        return new ByteBitString(unpackLongs(longs), length);
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
    public static ByteBitString valueOf(short[] shorts) {
        //checkNewBitStringLength(shorts.length * (long)Short.SIZE);
        if (shorts.length > MAX_SHORTS) throw new IllegalArgumentException("short array is too large");
        final int length = (shorts.length == MAX_SHORTS) ? Integer.MAX_VALUE : shorts.length * Short.SIZE;
        return new ByteBitString(unpackShorts(shorts), length);
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
    public static ByteBitString valueOf(LongBuffer lb) {
        lb = lb.slice();
        //checkNewBitStringLength(lb.remaining() * (long)Long.SIZE);
        if (lb.remaining() > MAX_LONGS) throw new IllegalArgumentException("LongBuffer array is too large");
        long[] longs = new long[lb.remaining()];
        lb.get(longs);
        final int length = (longs.length == MAX_LONGS) ? Integer.MAX_VALUE : longs.length * Long.SIZE;
        return new ByteBitString(unpackLongs(longs), length);
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
    public static ByteBitString valueOf(ByteBuffer bb) {
        bb = bb.slice();
        //checkNewBitStringLength(bb.remaining() * (long)Byte.SIZE);
        if (bb.remaining() > MAX_BYTES) throw new IllegalArgumentException("ByteBuffer array is too large");
        byte[] bytes = new byte[bb.remaining()];
        bb.get(bytes);
        final int length = (bytes.length == MAX_BYTES) ? Integer.MAX_VALUE : bytes.length * Byte.SIZE;
        return new ByteBitString(bytes, length);
    }
    
    //    /**
    //     * Returns a new BitString containing all the bits in the given long buffer
    //     * between its position and limit.
    //     * <p>
    //     * More precisely, <br>
    //     * {@code BitString.valueOf(lb).getBit(n) == ((lb.get(lb.position()+n/64) & (0x8000000000000000L>>>(n%64))) != 0)}
    //     * <br>
    //     * for all {@code n < 64 * lb.remaining()}.
    //     * <p>
    //     * The long buffer is not modified by this method, and no reference to the
    //     * buffer is retained by the BitString.
    //     *
    //     * @param lb a long buffer containing a big-endian representation of a sequence
    //     *           of bits between its position and limit, to be used as the initial
    //     *           bits of the new BitString
    //     * @return a {@code BitString} containing all the bits in the buffer in the
    //     *         specified range
    //     */
    //    public static ByteBitString valueOf(LongBuffer lb) {
    //        lb = lb.slice();
    //        checkNewBitStringLength(lb.remaining() * (long)Long.SIZE);
    //        long[] words = new long[lb.remaining()];
    //        lb.get(words);
    //        return new ByteBitString(words, words.length * Long.SIZE);
    //    }

    static int byteIndex(int bitIndex) {
        return bitIndex >> ADDRESS_BITS_PER_WORD;
    }
    
    @Override
    long getWord(int wordIndex) {
        int byteIndex = wordIndex * Long.BYTES;
        assert byteIndex >= 0 && byteIndex < this.backingArray.length;
        long word = 0L;
        for (int i = byteIndex, p = Long.BYTES - 1; p >= 0 && i < backingArray.length; i++, p--) {
            word |= Byte.toUnsignedLong(backingArray[i]) << (p * Byte.SIZE);
        }
        return word;
    }
    
    @Override
    void setWord(int wordIndex, long word) {
        int byteIndex = wordIndex * Long.BYTES;
        assert byteIndex >= 0 && byteIndex < this.backingArray.length;
        for (int i = byteIndex, p = Long.BYTES - 1; p >= 0 && i < backingArray.length; i++, p--) {
            backingArray[i] = (byte)(word >>> (p * Byte.SIZE));
        }
    }
    
    private static byte[] unpackChars(char[] chars) {
        return unpack(chars.length, Character.BYTES,
                (index) -> { return (long)(chars[index]); });
    }
    
    private static byte[] unpackDoubles(double[] doubles) {
        return unpack(doubles.length, Long.BYTES,
                (index) -> { return Double.doubleToRawLongBits(doubles[index]); });
    }
    
    private static byte[] unpackFloats(float[] floats) {
        return unpack(floats.length, Integer.BYTES,
                (index) -> { return Integer.toUnsignedLong(Float.floatToRawIntBits(floats[index])); });
    }
    
    private static byte[] unpackInts(int[] ints) {
        return unpack(ints.length, Integer.BYTES,
                (index) -> { return Integer.toUnsignedLong(ints[index]); });
    }
    
    private static byte[] unpackLongs(long[] longs) {
        return unpack(longs.length, Long.BYTES,
                (index) -> { return longs[index]; });
    }
    
    private static byte[] unpackShorts(short[] shorts) {
        return unpack(shorts.length, Short.BYTES,
                (index) -> { return Short.toUnsignedLong(shorts[index]); });
    }
    
    private static byte[] unpack(int primitiveCount, int primitiveBytes,
            IntToLongFunction getPrimitiveAsUnsignedLong) {
        final byte[] bytes = new byte[primitiveCount * primitiveBytes];
        int byteIndex = 0;
        for (int index = 0; index < primitiveCount; index++) {
            final long primitive = getPrimitiveAsUnsignedLong.applyAsLong(index);
            for (int p = primitiveBytes - 1; p >= 0; p--) {
                bytes[byteIndex++] = (byte)(primitive >>> (p * Byte.SIZE));
            }
        }
        return bytes;
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
//    public ByteBitString substring(int offset, int length) {
//        checkThisOffset(offset);
//        checkThisLength(offset, length);
//        final ByteBitString substring = new ByteBitString(length);
//        substring.iCopy(0, length, this, offset);
//        return substring;
//    }

}
