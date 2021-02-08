package Bertrahm.de.hexagonsoftware.pg.audio;

import java.nio.ByteBuffer;

public class Sound {
	public int id;
	
	public int[] buffer;
	public int[] size;
	public int[] freq;
	public int[] loop;
	public int[] format;
	
	public ByteBuffer[] data;
	
	public Sound(int id) {
		this.id     = id;
		this.buffer = new int[1];
		this.size   = new int[1];
		this.freq   = new int[1];
		this.loop   = new int[1];
		this.format = new int[1];
		this.data   = new ByteBuffer[1];
	}
}

