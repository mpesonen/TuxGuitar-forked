/*
 * Created on 23-nov-2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.herac.tuxguitar.song.models;

import org.herac.tuxguitar.song.factory.TGFactory;

/**
 * @author julian
 * 
 * TODO To change the template for this generated type comment go to Window - Preferences - Java - Code Style - Code Templates
 */
public abstract class TGNote {
	private int value;
	private int velocity;
	private int string;
	private boolean tiedNote;
	private TGNoteEffect effect;
	private TGVoice voice;
	private int leftFingering;
	private int rightFingering;
	
	public TGNote(TGFactory factory) {
		this.value = 0;
		this.velocity = TGVelocities.DEFAULT;
		this.string = 1;
		this.tiedNote = false;
		this.effect = factory.newEffect();
		this.leftFingering = -1;
		this.rightFingering = -1;
	}
	
	public int getValue() {
		return this.value;
	}
	
	public void setValue(int value) {
		this.value = value;
	}
	
	public int getVelocity() {
		return this.velocity;
	}
	
	public void setVelocity(int velocity) {
		this.velocity = velocity;
	}
	
	public int getString() {
		return this.string;
	}
	
	public void setString(int string) {
		this.string = string;
	}
	
	public boolean isTiedNote() {
		return this.tiedNote;
	}
	
	public void setTiedNote(boolean tiedNote) {
		this.tiedNote = tiedNote;
	}
	
	public TGNoteEffect getEffect() {
		return this.effect;
	}
	
	public void setEffect(TGNoteEffect effect) {
		this.effect = effect;
	}
	
	public TGVoice getVoice() {
		return this.voice;
	}
	
	public void setVoice(TGVoice voice) {
		this.voice = voice;
	}

	public int getLeftFingering() { return this.leftFingering; }

	public int getRightFingering() { return this.rightFingering; }

	public void setLeftFingering(int fingering ) { this.leftFingering = fingering; }

	public void setRightFingering(int fingering ) { this.rightFingering = fingering; }

	public boolean isFingeringSpecifiedNote() { return this.getLeftFingering() > -1 || this.getRightFingering() > -1; }
	
	public TGNote clone(TGFactory factory){
		TGNote note = factory.newNote();
		note.setValue(getValue());
		note.setVelocity(getVelocity());
		note.setString(getString());
		note.setTiedNote(isTiedNote());
		note.setEffect(getEffect().clone(factory));
		note.setLeftFingering( getLeftFingering() );
		note.setRightFingering( getRightFingering() );
		return note;
	}
}