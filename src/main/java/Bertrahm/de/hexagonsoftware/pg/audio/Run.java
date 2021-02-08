package Bertrahm.de.hexagonsoftware.pg.audio;

import Bertrahm.de.hexagonsoftware.pg.audio.AudioEngine;

public class Run {
    public static void main(String[] args){
        AudioEngine a = new AudioEngine();
        a.loadSound("boing", "BOOOING.WAV");
        a.loadSound("ahah", "AHAH.WAV");

        // Position of the source sound.
        float[] pos = { 0.0f, 0.0f, 0.0f };
        // Velocity of the source sound.
        float[] vel = { 0.0f, 0.0f, 0.0f };
        int boingSource = a.createSource("boing", pos, vel, 1.0f /* gain */ , 1.0f /* pitch */);
        int ahahSource = a.createSource("ahah", pos, vel, 1.0f /* gain */ , 1.0f /* pitch */);       
 
        a.playSource(boingSource);

        try {
            Thread.sleep(400);    
        } catch (Exception e) {
            //TODO: handle exception
        }
        
        a.playSource(ahahSource);
         try {
            Thread.sleep(3000);  
        } catch (Exception e) {
            //TODO: handle exception
        }
 
    }
}
