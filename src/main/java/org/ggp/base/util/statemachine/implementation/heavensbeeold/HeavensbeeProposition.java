package org.ggp.base.util.statemachine.implementation.heavensbeeold;

import java.util.BitSet;

import org.ggp.base.util.logging.GamerLogger;
import org.ggp.base.util.propnet.architecture.components.Proposition;

public class HeavensbeeProposition
{

	private int s_index;
	private Proposition s_proposition;
	private BitSet ref_propstate;

	public HeavensbeeProposition(int index, Proposition p, BitSet propnet_state) {
		s_index = index;
		s_proposition = p;
		ref_propstate = propnet_state;

		GamerLogger.emitToConsole("Creating proposition " + s_index + " as " + s_proposition.getName() + "\n");
	}

	public boolean getValue()
	{
		return ref_propstate.get(s_index);
	}
}
