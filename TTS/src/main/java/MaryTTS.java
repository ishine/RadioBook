import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.sound.sampled.AudioInputStream;

import marytts.LocalMaryInterface;
import marytts.MaryInterface;
import marytts.exceptions.MaryConfigurationException;
import marytts.exceptions.SynthesisException;
import marytts.modules.synthesis.Voice;
import marytts.signalproc.effects.AudioEffect;
import marytts.signalproc.effects.AudioEffects;
import marytts.util.data.audio.AudioPlayer;

/**
 * @author GOXR3PLUS
 *
 */
public class MaryTTS {
	
	private AudioPlayer audioPlayer;
	private MaryInterface marytts;
	
	/**
	 * Constructor
	 */
	public MaryTTS() {
		try {
			marytts = new LocalMaryInterface();
			
		} catch (MaryConfigurationException ex) {
			Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
		}
	}
	
	//----------------------GENERAL METHODS---------------------------------------------------//
	
	/**
	 * Transform text to speech
	 * 
	 * @param text
	 *            The text that will be transformed to speech
	 * @param daemon
	 *            <br>
	 *            <b>True</b> The thread that will start the text to speech Player will be a daemon Thread <br>
	 *            <b>False</b> The thread that will start the text to speech Player will be a normal non daemon Thread
	 * @param join
	 *            <br>
	 *            <b>True</b> The current Thread calling this method will wait(blocked) until the Thread which is playing the Speech finish <br>
	 *            <b>False</b> The current Thread calling this method will continue freely after calling this method
	 */
	public void speak(String text , float gainValue , boolean daemon , boolean join) {
		
		// Stop the previous player
		stopSpeaking();
		
		try (AudioInputStream audio = marytts.generateAudio(text)) {
			
			// Player is a thread(threads can only run one time) so it can be
			// used has to be initiated every time
			audioPlayer = new AudioPlayer();
			audioPlayer.setAudio(audio);
			//audioPlayer.setGain(gainValue);
			audioPlayer.setDaemon(daemon);
			audioPlayer.start();
			if (join)
				audioPlayer.join();
			
		} catch (SynthesisException ex) {
			Logger.getLogger(getClass().getName()).log(Level.WARNING, "Error saying phrase.", ex);
		} catch (IOException ex) {
			Logger.getLogger(getClass().getName()).log(Level.WARNING, "IO Exception", ex);
		} catch (InterruptedException ex) {
			Logger.getLogger(getClass().getName()).log(Level.WARNING, "Interrupted ", ex);
			audioPlayer.interrupt();
		}
	}
	
	public AudioInputStream record(String text , float gainValue , boolean daemon , boolean join) 
	{
		
		// Stop the previous player
		stopSpeaking();
		AudioInputStream audio = null;
		try 
		{
			audio = marytts.generateAudio(text);
			// Player is a thread(threads can only run one time) so it can be
			// used has to be initiated every time
			audioPlayer = new AudioPlayer();
			audioPlayer.setAudio(audio);
			if (join)
				audioPlayer.join();
			
			
		} catch (SynthesisException ex) {
			Logger.getLogger(getClass().getName()).log(Level.WARNING, "Error saying phrase.", ex);
		} catch (InterruptedException ex) {
			Logger.getLogger(getClass().getName()).log(Level.WARNING, "Interrupted ", ex);
			audioPlayer.interrupt();
		}
		return audio;
	}
	
	/**
	 * Stop the MaryTTS from Speaking
	 */
	public void stopSpeaking() {
		// Stop the previous player
		if (audioPlayer != null)
			audioPlayer.cancel();
	}
	
	//----------------------GETTERS---------------------------------------------------//
	
	/**
	 * Available voices in String representation
	 * 
	 * @return The available voices for MaryTTS
	 */
	public Collection<Voice> getAvailableVoices() {
		return Voice.getAvailableVoices();
	}
	
	/**
	 * @return the marytts
	 */
	public MaryInterface getMarytts() {
		return marytts;
	}
	
	/**
	 * Return a list of available audio effects for MaryTTS
	 * 
	 * @return
	 */
	public List<AudioEffect> getAudioEffects() {
		return StreamSupport.stream(AudioEffects.getEffects().spliterator(), false).collect(Collectors.toList());
	}
	
	//----------------------SETTERS---------------------------------------------------//
	
	/**
	 * Change the default voice of the MaryTTS
	 * 
	 * @param voice
	 */
	public void setVoice(String voice) {
		marytts.setVoice(voice);
	}

	public AudioInputStream generateAudio(String quote) {
		// TODO Auto-generated method stub
		return null;
	}
	
}