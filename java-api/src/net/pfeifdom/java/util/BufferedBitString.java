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

import java.nio.Buffer;

import net.pfeifdom.java.util.BitString.Field;

public interface BufferedBitString {
    
    public enum Mode { ABSOLUTE, RELATIVE }
    
    public Mode mode();
    
    public void setMode(Mode newMode);
    
    public BufferedBitString clone();
    
    public Buffer buffer();
    
    public int limit();

    public int position();
    
    public int capacity();
    
    public int ensureCapacity(int capacity);
    
    public int trimToLength();
    
    public int length();
    
    public boolean isEmpty();
    
    public Field all();
    
    public Field all(int offset);
    
    public void setLength(int newLength);
    
    public BitString append(BitString that);
    
    public BitString append(BitString that, int thatOffset, int thatLength);
    
    public BitString append(BitString that, Field thatField);
    
    public BitString delete();
    
    public BitString delete(int offset, int length);
    
    public BitString delete(Field field);
    
    public BitString insert(int position, BitString that);
    
    public BitString insert(int position, BitString that, int thatOffset, int thatLength);
    
    public BitString insert(int position, BitString that, Field thatField);
    
    public BitString replace(BitString that);
    
    public BitString replace(int thisOffset, int thisLength, BitString that, int thatOffset, int thatLength);
    
    public BitString replace(Field thisField, BitString that, Field thatField);
    
    public BitString clear();
    
    public BitString clear(int offset);
    
    public BitString clear(int offset, int length);
    
    public BitString clear(Field field);
    
    public BitString clearBit(int bitOffset);
    
    public BitString clearBit(int bitOffset, int offset, int length);
    
    public BitString clearBit(int bitOffset, Field field);
    
    public BitString flip();
    
    public BitString flip(int offset, int length);
    
    public BitString flip(Field field);
    
    public BitString flipBit(int bitOffset);
    
    public BitString flipBit(int bitOffset, int offset, int length);
    
    public BitString flipBit(int bitOffset, Field field);
    
    public BitString get();
    
    public BitString get(int offset);
    
    public BitString get(int offset, int length);
    
    public BitString get(Field field);
    
    public boolean getBit(int bitOffset);
    
    public boolean getBit(int bitOffset, int offset, int length);
    
    public boolean getBit(int bitOffset, Field field);
    
    public boolean getBoolean(int offset);
    
    public boolean[] getBooleanArray(int offset, int count);
    
    public byte getByte(int offset);

    public byte[] getByteArray(int offset, int count);
    
    public char getChar(int offset);

    public char[] getCharArray(int offset, int count);
    
    public double getDouble(int offset);

    public double[] getDoubleArray(int offset, int count);
    
    public float getFloat(int offset);

    public float[] getFloatArray(int offset, int count);
    
    public int getInt(int offset);

    public int[] getIntArray(int offset, int count);
    
    public long getLong(int offset);

    public Long[] getLongArray(int offset, int count);
    
    public short getShort(int offset);

    public short[] getShortArray(int offset, int count);
    
    public BitString put(BitString that);
    
    public BitString put(int offset, BitString that);
    
    public BitString put(int offset, BitString that, int thatOffset, int thatLength);
    
    public BitString put(int offset, BitString that, Field thatField);
    
    public BitString putBoolean(int offset, boolean primitive);
    
    public BitString putBooleanArray(int offset, boolean[] booleans);
    
    public BitString putByte(int offset, byte primitive);

    public BitString putByteArray(int offset, byte[] bytes);
    
    public BitString putChar(int offset, char primitive);

    public BitString putCharArray(int offset, char[] chars);
    
    public BitString putDouble(int offset, double primitive);

    public BitString putDoubleArray(int offset, double[] doubles);
    
    public BitString putFloat(int offset, float primitive);

    public BitString putFloatArray(int offset, float[] floats);
    
    public BitString putInt(int offset, int primitive);

    public BitString putIntArray(int offset, int[] ints);
    
    public BitString putLong(int offset, long primitive);

    public BitString putLongArray(int offset, long[] longs);
    
    public BitString putShort(int offset, short primitive);

    public BitString putShortArray(int offset, short[] shorts);
    
    public BitString set();
    
    public BitString set(int offset);
    
    public BitString set(int offset, int length);
    
    public BitString set(Field field);
    
    public BitString set(boolean bit);
    
    public BitString set(boolean bit, int offset);
    
    public BitString set(boolean bit, int offset, int length);
    
    public BitString set(boolean bit, Field field);
    
    public BitString setBit(int bitOffset);
    
    public BitString setBit(int bitOffset, int offset, int length);
    
    public BitString setBit(int bitOffset, Field field);
    
    public BitString setBit(boolean bit, int bitOffset);
    
    public BitString setBit(boolean bit, int bitOffset, int offset, int length);
    
    public BitString setBit(boolean bit, int bitOffset, Field field);
    
    public BitString and(BitString arg);
    
    public BitString and(int thisOffset, int thisLength, BitString arg, int argOffset, int argLength);
    
    public BitString and(Field thisField, BitString arg, Field argField);
    
    public BitString andNot(BitString arg);
    
    public BitString andNot(int thisOffset, int thisLength, BitString arg, int argOffset, int argLength);
    
    public BitString andNot(Field thisField, BitString arg, Field argField);
    
    public BitString nand(BitString arg);
    
    public BitString nand(int thisOffset, int thisLength, BitString arg, int argOffset, int argLength);
    
    public BitString nand(Field thisField, BitString arg, Field argField);
    
    public BitString nandNot(BitString arg);
    
    public BitString nandNot(int thisOffset, int thisLength, BitString arg, int argOffset, int argLength);
    
    public BitString nandNot(Field thisField, BitString arg, Field argField);
    
    public BitString nor(BitString arg);
    
    public BitString nor(int thisOffset, int thisLength, BitString arg, int argOffset, int argLength);
    
    public BitString nor(Field thisField, BitString arg, Field argField);
    
    public BitString norNot(BitString arg);
    
    public BitString norNot(int thisOffset, int thisLength, BitString arg, int argOffset, int argLength);
    
    public BitString norNot(Field thisField, BitString arg, Field argField);
    
    public BitString or(BitString arg);
    
    public BitString or(int thisOffset, int thisLength, BitString arg, int argOffset, int argLength);
    
    public BitString or(Field thisField, BitString arg, Field argField);
    
    public BitString orNot(BitString arg);
    
    public BitString orNot(int thisOffset, int thisLength, BitString arg, int argOffset, int argLength);
    
    public BitString orNot(Field thisField, BitString arg, Field argField);
    
    public BitString xor(BitString arg);
    
    public BitString xor(int thisOffset, int thisLength, BitString arg, int argOffset, int argLength);
    
    public BitString xor(Field thisField, BitString arg, Field argField);
    
    public BitString xnor(BitString arg);
    
    public BitString xnor(int thisOffset, int thisLength, BitString arg, int argOffset, int argLength);
    
    public BitString xnor(Field thisField, BitString arg, Field argField);
    
    public BitString copyFrom(BitString that);
    
    public BitString copyFrom(int thisOffset, int thisLength, BitString that, int thatOffset, int thatLength);
    
    public BitString copyFrom(Field thisField, BitString that, Field thatField);
    
    public BitString copyFromBackOf(BitString that);
    
    public BitString copyFromBackOf(int thisOffset, int thisLength, BitString that, int thatOffset, int thatLength);
    
    public BitString copyFromBackOf(Field thisField, BitString that, Field thatField);
    
    public BitString copyNotFrom(BitString that);
    
    public BitString copyNotFrom(int thisOffset, int thisLength, BitString that, int thatOffset, int thatLength);
    
    public BitString copyNotFrom(Field thisField, BitString that, Field thatField);
    
    public BitString copyNotFromBackOf(BitString that);
    
    public BitString copyNotFromBackOf(int thisOffset, int thisLength, BitString that, int thatOffset, int thatLength);
    
    public BitString copyNotFromBackOf(Field thisField, BitString that, Field thatField);
    
    public boolean equals(Object obj);
    
    public boolean equals(BitString that);
    
    public boolean equals(int thisOffset, int thisLength, BitString that, int thatOffset, int thatLength);
    
    public boolean equals(Field thisField, BitString that, Field thatField);
    
    public boolean intersects(BitString that);
    
    public boolean intersects(int thisOffset, int thisLength, BitString that, int thatOffset, int thatLength);
    
    public boolean intersects(Field thisField, BitString that, Field thatField);
    
    public int numberOfLeadingOnes();
    
    public int numberOfLeadingOnes(int offset, int length);
    
    public int numberOfLeadingOnes(Field field);
    
    public int numberOfLeadingZeros();
    
    public int numberOfLeadingZeros(int offset, int length);
    
    public int numberOfLeadingZeros(Field field);
    
    public int numberOfOnes();
    
    public int numberOfOnes(int offset, int length);
    
    public int numberOfOnes(Field field);
    
    public int numberOfTrailingOnes();
    
    public int numberOfTrailingOnes(int offset, int length);
    
    public int numberOfTrailingOnes(Field field);
    
    public int numberOfTrailingZeros();
    
    public int numberOfTrailingZeros(int offset, int length);
    
    public int numberOfTrailingZeros(Field field);
    
    public int numberOfZeros();
    
    public int numberOfZeros(int offset, int length);
    
    public int numberOfZeros(Field field);
    
    public int offsetOfFirstOne();
    
    public int offsetOfFirstOne(int offset, int length);
    
    public int offsetOfFirstOne(Field field);
    
    public int offsetOfFirstZero();
    
    public int offsetOfFirstZero(int offset, int length);
    
    public int offsetOfFirstZero(Field field);
    
    public int offsetOfLastOne();
    
    public int offsetOfLastOne(int offset, int length);
    
    public int offsetOfLastOne(Field field);
    
    public int offsetOfLastZero();
    
    public int offsetOfLastZero(int offset, int length);
    
    public int offsetOfLastZero(Field field);
    
    public int offsetOfNextOne(int startOffset);
    
    public int offsetOfNextOne(int startOffset, int offset);
    
    public int offsetOfNextOne(int startOffset, int offset, int length);
    
    public int offsetOfNextOne(int startOffset, Field field);
    
    public int offsetOfNextZero(int startOffset);
    
    public int offsetOfNextZero(int startOffset, int offset);
    
    public int offsetOfNextZero(int startOffset, int offset, int length);
    
    public int offsetOfNextZero(int startOffset, Field field);
    
    public int offsetOfPreviousOne(int startOffset);
    
    public int offsetOfPreviousOne(int startOffset, int offset);
    
    public int offsetOfPreviousOne(int startOffset, int offset, int length);
    
    public int offsetOfPreviousOne(int startOffset, Field field);
    
    public int offsetOfPreviousZero(int startOffset);
    
    public int offsetOfPreviousZero(int startOffset, int offset);
    
    public int offsetOfPreviousZero(int startOffset, int offset, int length);
    
    public int offsetOfPreviousZero(int startOffset, Field field);
    
    public BitString range(int offset);
    
    public BitString range(int offset, int length);
    
    public BitString range(Field field);
    
    public BitString reverse();
    
    public BitString reverse(int offset, int length);
    
    public BitString reverse(Field field);
    
    public BitString rotateLeft(int nBits);
    
    public BitString rotateLeft(int nBits, int offset, int length);
    
    public BitString rotateLeft(int nBits, Field field);
    
    public BitString rotateLeft(int nBits, BitString other);
    
    public BitString rotateLeft(int nBits, int thisOffset, int thisLength, BitString other, int otherOffset, int otherLength);
    
    public BitString rotateLeft(int nBits, Field thisField, BitString other, Field otherField);
    
    public BitString rotateRight(int nBits);
    
    public BitString rotateRight(int nBits, int offset, int length);
    
    public BitString rotateRight(int nBits, Field field);
    
    public BitString rotateRight(int nBits, BitString other);
    
    public BitString rotateRight(int nBits, int thisOffset, int thisLength, BitString other, int otherOffset, int otherLength);
    
    public BitString rotateRight(int nBits, Field thisField, BitString other, Field otherField);
    
    public BitString shiftLeft(int nBits);
    
    public BitString shiftLeft(int nBits, int offset, int length);
    
    public BitString shiftLeft(int nBits, Field field);
    
    public BitString shiftLeft(int nBits, boolean fill);
    
    public BitString shiftLeft(int nBits, boolean fill, int offset, int length);
    
    public BitString shiftLeft(int nBits, boolean fill, Field field);
    
    public BitString shiftLeft(int nBits, BitString other);
    
    public BitString shiftLeft(int nBits, int thisOffset, int thisLength, BitString other, int otherOffset, int otherLength);
    
    public BitString shiftLeft(int nBits, Field thisField, BitString other, Field otherField);
    
    public BitString shiftLeft(int nBits, boolean fill, BitString other);
    
    public BitString shiftLeft(int nBits, boolean fill, int thisOffset, int thisLength, BitString other, int otherOffset, int otherLength);
    
    public BitString shiftLeft(int nBits, boolean fill, Field thisField, BitString other, Field otherField);
    
    public BitString shiftRight(int nBits);
    
    public BitString shiftRight(int nBits, int offset, int length);
    
    public BitString shiftRight(int nBits, Field field);
    
    public BitString shiftRight(int nBits, boolean fill);
    
    public BitString shiftRight(int nBits, boolean fill, int offset, int length);
    
    public BitString shiftRight(int nBits, boolean fill, Field field);
    
    public BitString shiftRight(int nBits, BitString other);
    
    public BitString shiftRight(int nBits, int thisOffset, int thisLength, BitString other, int otherOffset, int otherLength);
    
    public BitString shiftRight(int nBits, Field thisField, BitString other, Field otherField);
    
    public BitString shiftRight(int nBits, boolean fill, BitString other);
    
    public BitString shiftRight(int nBits, boolean fill, int thisOffset, int thisLength, BitString other, int otherOffset, int otherLength);
    
    public BitString shiftRight(int nBits, boolean fill, Field thisField, BitString other, Field otherField);
    
    public BitString substring();
    
    public BitString substring(int offset);
    
    public BitString substring(int offset, int length);
    
    public BitString subString(Field field);
    
    public boolean[] toBooleanArray();
    
    public boolean[] toBooleanArray(int offset, int length);
    
    public boolean[] toBooleanArray(Field field);
    
    public byte[] toByteArray();
    
    public byte[] toByteArray(int offset, int length);
    
    public byte[] toByteArray(Field field);
    
    public char[] toCharArray();
    
    public char[] toCharArray(int offset, int length);
    
    public char[] toCharArray(Field field);
    
    public double[] toDoubleArray();
    
    public double[] toDoubleArray(int offset, int length);
    
    public double[] toDoubleArray(Field field);
    
    public float[] toFloatArray();
    
    public float[] toFloatArray(int offset, int length);
    
    public float[] toFloatArray(Field field);
    
    public int[] toIntArray();
    
    public int[] toIntArray(int offset, int length);
    
    public int[] toIntArray(Field field);
    
    public long[] toLongArray();
    
    public long[] toLongArray(int offset, int length);
    
    public long[] toLongArray(Field field);
    
    public short[] toShortArray();
    
    public short[] toShortArray(int offset, int length);
    
    public short[] toShortArray(Field field);
    
    public String toString();
    
    public String toString(int offset, int length);
    
    public String toString(Field field);
    
    public int hashCode();
    
}
