package Bertrahm.de.hexagonsoftware.pg.audio;

public class Listener {
	private float[] pos;
	private float[] ori;
	private float[] vel;
	
	public Listener(float[] pos, float[] ori, float[] vel) {
		this.pos = pos;
		this.ori = ori;
		this.vel = vel;
	}
	
	public float[] getPos() { return this.pos; }
	public float[] getOri() { return this.ori; }
	public float[] getVel() { return this.vel; }
}

