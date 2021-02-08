package Bertrahm.de.hexagonsoftware.pg.audio;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.jogamp.openal.AL;
import com.jogamp.openal.ALFactory;
import com.jogamp.openal.util.ALut;

public class AudioEngine {
	private AudioEngine AE;
	private AL AE_AL;
	
	private Map<String, Sound> AE_SOUNDS;
	private ArrayList<Source> AE_SOURCES;
	
	public Listener AE_LISTENER;
	
	public AudioEngine() {
		this.AE_AL 		 = ALFactory.getAL();
		this.AE_SOUNDS   = new HashMap<>();
		this.AE_SOURCES  = new ArrayList<>();
		
		ALut.alutInit();
		AE_AL.alGetError();
	}
	
	public AudioEngine getInstance() {
		if (AE == null) AE = new AudioEngine();
		
		return AE;
	}

	//////////////////////////////////////////////////
	
	public void loadSound(String name, String file) {
		Sound s = new Sound(1);
		
		AE_AL.alGenBuffers(1, s.buffer, 0);
		if (AE_AL.alGetError() != AL.AL_NO_ERROR) {
			System.out.println(String.format("An error occured whilst loading sound: NAME \"%s\"; FILE \"%t\"", name, file));
			return;
		}
		
		ALut.alutLoadWAVFile(getClass().getClassLoader().getResourceAsStream(file), s.format, s.data, s.size, s.freq, s.loop);
		AE_AL.alBufferData(s.buffer[0], s.format[0], s.data[0], s.size[0], s.freq[0]);
		
		AE_SOUNDS.put(name, s);
	}
	
	public int createSource(String sound, float[] pos, float[] vel, float gain, float pitch) {
		// Gen Sources
		Sound s = AE_SOUNDS.get(sound);
		Source src = new Source(pos, vel);
		AE_AL.alGenSources(1, src.source, 0);
		
		int err = AE_AL.alGetError();
		if (err != AL.AL_NO_ERROR) {
			System.out.println(String.format("An error occured whilst creating sound source: NAME \"%s\" ; %s (alGenSources)", sound, getErrorName(err)));
			return -1;
		}
		
		// Set Source Parameters
		
		AE_AL.alSourcei (src.source[0], AL.AL_BUFFER,   s.buffer[0]   );
		AE_AL.alSourcef (src.source[0], AL.AL_PITCH,    gain     );
		AE_AL.alSourcef (src.source[0], AL.AL_GAIN,     pitch     );
		AE_AL.alSourcefv(src.source[0], AL.AL_POSITION, src.getPos(), 0);
		AE_AL.alSourcefv(src.source[0], AL.AL_VELOCITY, src.getVel(), 0);
		AE_AL.alSourcei (src.source[0], AL.AL_LOOPING,  s.loop[0]     );
		
		err = AE_AL.alGetError();
		if (err != AL.AL_NO_ERROR) {
			System.out.println(String.format("An error occured whilst creating sound source: NAME \"%s\" ; %s (setting source parameters)", sound, getErrorName(err)));
			return -1;
		}
		
		AE_SOURCES.add(src);
		return src.source[0];
	}
	
	public void playSource(int source) {
		AE_AL.alSourcePlay(source);
	}
	
	public void createListener(float[] pos, float[] ori, float[] vel) {
		if (AE_LISTENER != null) { 
			System.out.println("A listener object already exists!");
			AE_LISTENER = null; 
		}
		
		AE_LISTENER = new Listener(pos, ori, vel);

        AE_AL.alListenerfv(AL.AL_POSITION,    AE_LISTENER.getPos(), 0);
        AE_AL.alListenerfv(AL.AL_VELOCITY,    AE_LISTENER.getOri(), 0);
        AE_AL.alListenerfv(AL.AL_ORIENTATION, AE_LISTENER.getVel(), 0);
	}
	
	public void updateListener() {
		AE_AL.alListenerfv(AL.AL_POSITION,    AE_LISTENER.getPos(), 0);
        AE_AL.alListenerfv(AL.AL_VELOCITY,    AE_LISTENER.getOri(), 0);
        AE_AL.alListenerfv(AL.AL_ORIENTATION, AE_LISTENER.getVel(), 0);
	}
	
	public void killALData() {
		AE_SOUNDS.forEach((k, v) -> AE_AL.alDeleteBuffers(AE_SOUNDS.keySet().size(), v.buffer, 0));
		AE_SOURCES.forEach((v) -> AE_AL.alDeleteSources(AE_SOURCES.size(), v.source, 0));
		ALut.alutExit();
	}
	
	public static String getErrorName(int err) {
		switch (err) {
		  case AL.AL_NO_ERROR: return "AL_NO_ERROR";
		  case AL.AL_INVALID_NAME: return "AL_INVALID_NAME";
		  case AL.AL_INVALID_ENUM: return "AL_INVALID_ENUM";
		  case AL.AL_INVALID_VALUE: return "AL_INVALID_VALUE";
		  case AL.AL_OUT_OF_MEMORY: return "AL_OUT_OF_MEMORY";
		  default:
		    return "Unknown error code "+err;
		}
	}
}

