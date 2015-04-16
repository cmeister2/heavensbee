package org.ggp.base.player.gamer.statemachine.heavensbee;

import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.implementation.heavensbeepropnet.HeavensbeePropNetStateMachine;


public final class HeavensbeeMCTSMPPropnet extends HeavensbeeMCTSMP {
	@Override
	public StateMachine getInitialStateMachine() {
		// TODO Auto-generated method stub
		//return new CachedStateMachine(new HeavensbeePropNetStateMachine());
		return new HeavensbeePropNetStateMachine();
	}
}