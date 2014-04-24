package com.yuanyu.upwardalarm.sensor;

import java.util.List;

import com.yuanyu.upwardalarm.sensor.MovementAnalysor.MovementListener;
import com.yuanyu.upwardalarm.sensor.MovementTracker.Sample;

abstract class Movement {

	List<MovementListener> mMovementListeners;
	
	protected int mMovementLevel;
	
	public Movement(List<MovementListener> listeners, int movementLevel) {
		mMovementListeners = listeners;
		mMovementLevel = movementLevel;
	}

	abstract void detectMovement(List<Sample> samples);
	
	protected void notifyMovementDetected() {
		if(mMovementListeners != null) {
			for(MovementListener listener : mMovementListeners) {
				listener.onMovementDetected();
			}
		}
	}
}
