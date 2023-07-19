package com.xrbpowered.hexit;

import java.util.Arrays;

public class ByteBuffer {

	private byte[] buffer;
	private int length;
	
	public ByteBuffer() {
		buffer = new byte[1024];
		length = 0;
	}
	
	public ByteBuffer(byte[] bytes) {
		buffer = bytes;
		length = bytes.length;
	}
	
	public int get(int addr) {
		return (int)buffer[addr] & 0xff;
	}
	
	public byte[] getBytes() {
		trimBuffer();
		return buffer;
	}
	
	public int size() {
		return length;
	}

	protected void resizeBuffer(int length) {
		buffer = Arrays.copyOf(buffer, length);
	}
	
	protected void resizeBufferFor(int length) {
		if(length>buffer.length || length<buffer.length-2048)
			resizeBuffer(((length+1024)/1024)*1024);
	}
	
	protected void trimBuffer() {
		if(buffer.length>length)
			resizeBuffer(length);
	}
	
	public void modifyByte(int addr, byte b) {
		buffer[addr] = b;
	}
	
	public void modifyByte(int addr, int hex, boolean low) {
		int b = get(addr);
		if(low)
			b = b&0xf0 | hex&0x0f;
		else
			b = b&0x0f | (hex<<4)&0xf0;
		modifyByte(addr, (byte)b);
	}
	
	public void move(int to, int from, int len) {
		if(to<from) {
			int s = from;
			int d = to;
			for(int i=0; i<len; i++) {
				buffer[d] = buffer[s];
				s++;
				d++;
			}
		}
		else {
			int s = from+len-1;
			int d = to+len-1;
			for(int i=0; i<len; i++) {
				buffer[d] = buffer[s];
				s--;
				d--;
			}
		}
	}

	public void fill(int addr, int len, byte b) {
		for(int i=0; i<len; i++) {
			buffer[addr] = b;
			addr++;
		}
	}

	public void modify(int addr, byte[] bytes) {
		for(int i=0; i<bytes.length; i++) {
			buffer[addr] = bytes[i];
			addr++;
		}
	}
	
	public int modify(int before, byte[] add, int after) {
		// text = text.substring(0, before) + add + text.substring(after);
		int addlen = add==null ? 0 : add.length;
		int delta = before-after+addlen;
		resizeBufferFor(length+delta);
		if(delta!=0)
			move(before+addlen, after, length-after);
		if(addlen>0)
			modify(before, add);
		length += delta;
		return delta;
	}

}
