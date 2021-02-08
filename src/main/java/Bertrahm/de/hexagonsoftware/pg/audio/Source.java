package Bertrahm.de.hexagonsoftware.pg.audio;

public class Source {
	public  int[]   source;
	private float[] pos;
	private float[] vel;
	
	public Source(float[] pos, float[] vel) {
		this.source = new int[1];
		this.pos    = pos;
		this.vel    = vel;
	}

	public float[] getPos() { return this.pos; }
	public float[] getVel() { return this.vel; }
}

