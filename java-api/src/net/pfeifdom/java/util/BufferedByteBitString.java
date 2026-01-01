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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;

public class BufferedByteBitString extends ByteBitString implements BufferedBitString {
    
    /**
     * 
     */
    private static final long serialVersionUID = -4473224767145978262L;
    
    private final ByteBuffer buffer;
    
    private Mode mode;
    
    private int expectantPosition;
    private int expectantLimit;
    
    public BufferedByteBitString(ByteBuffer bb) {
        this(bb, Mode.ABSOLUTE);
    }
    
    public BufferedByteBitString(ByteBuffer bb, Mode mode) {
        super(bb.array(), bb.limit() * Byte.SIZE);
        this.buffer = bb;
        //checkNewBitStringLength(bb.array().length * (long)Byte.SIZE);
        if (bb.array().length > MAX_BYTES) throw new IllegalArgumentException("ByteBuffer's array is too large");
        //if (bb.array().length == MAX_BYTES) stringLength = Integer.MAX_VALUE;
        this.mode = mode;
        this.expectantPosition = buffer.position();
        this.expectantLimit = buffer.limit();
    }
    
    public Mode mode() {
        return this.mode;
    }
    
    public void setMode(Mode newMode) {
        if (newMode == mode()) return;
        switch (newMode) {
        case ABSOLUTE:
            //stringLength = (buffer.arrayOffset() + buffer.limit()) * Byte.SIZE;
            stringLength = absPosition();
            break;
        case RELATIVE:
            break;
        default:
            throw new InternalError("unhandled BufferedBitString.Mode: " + newMode.toString());
        }
        this.mode = newMode;
        incrementModCount();
    }
    
    @Override
    void resizeBackingArray(int capacity) {
        final int newLength = byteIndex(capacity-1) + 1;
        final int currentLength = byteIndex(capacity()-1) + 1;
        if (newLength == currentLength) return;
        throw new UnsupportedOperationException("The capacity of Buffered BitStrings cannot be changed");
    }
    
    @Override
    public BufferedByteBitString clone() {
        //return (BufferedByteBitString) super.clone();
        return new BufferedByteBitString(buffer);
        //throw new CloneNotSupportedException();
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
    public ByteBuffer buffer() {
        return buffer;
    }
    
    @Override
    public int limit() {
        return buffer.limit();
    }
    
    private void setLimit(int length) {
        buffer.limit(byteIndex(length - 1) + 1);
    }
    
    private int absPosition() {
        return (buffer.arrayOffset() + buffer.position()) * Byte.SIZE;
    }
    
    private void checkForBufferMods() {
        if (buffer.position() != this.expectantPosition || buffer.limit() != this.expectantLimit) incrementModCount();
        this.expectantPosition = buffer.position();
        this.expectantLimit = buffer.limit();
    }
    
    @Override
    public int position() {
        if (mode == Mode.ABSOLUTE) {
            final int position = buffer.position() + buffer.arrayOffset();
            return (position == MAX_BYTES) ? Integer.MAX_VALUE : position * Byte.SIZE;
        }
        checkForBufferMods();
        return 0;
    }
    
    @Override
    public int capacity() {
        if (mode == Mode.ABSOLUTE) return super.capacity();
        //return super.capacity() - (buffer.arrayOffset() + buffer.position()) * Byte.SIZE;
        checkForBufferMods();
        return super.capacity() - absPosition();
    }
    
    @Override
    public int length() {
        if (mode == Mode.ABSOLUTE) return super.length();
        checkForBufferMods();
        return (buffer.remaining() == MAX_BYTES) ? Integer.MAX_VALUE : buffer.remaining() * Byte.SIZE;
    }
    
    @Override
    int baseLength() {
        return length();
    }
    
    @Override
    public void setLength(int newLength) {
        if (mode == Mode.ABSOLUTE) {
            super.setLength(newLength);
        } else {
            if (newLength == this.length()) return;
            if (newLength < 0) throw new IllegalArgumentException("specified length is negative: " + newLength);
            ensureCapacity(newLength);
            setLimit((newLength == 0) ? 0 : (newLength - 1) / Byte.SIZE + 1);
            final int oldLength = this.length();
            if (newLength > oldLength) iClear(oldLength, newLength - oldLength);
            if (newLength < oldLength) incrementModCount(); // do not invalidate Ranges if appending  
        }
    }
    
//    @Override
//    void setRangeLength(int lengthDelta, int bitIndex) {
//        if (mode == Mode.ABSOLUTE) {
//            super.setRangeLength(lengthDelta, bitIndex);
//        } else {
//            setLimit(length() + lengthDelta);
//        }
//    }
    
    @Override
    int bitIndex(int offset) {
        if (mode == Mode.ABSOLUTE) return super.bitIndex(offset);
        //return super.bitIndex((buffer.arrayOffset() + buffer.position()) * Byte.SIZE + offset);
        checkForBufferMods();
        return super.bitIndex(absPosition() + offset);
    }

}
