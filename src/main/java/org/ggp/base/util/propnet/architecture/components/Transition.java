package org.ggp.base.util.propnet.architecture.components;

import java.util.BitSet;
import java.util.Set;

import org.ggp.base.util.logging.GamerLogger;
import org.ggp.base.util.propnet.architecture.Component;

/**
 * The Transition class is designed to represent pass-through gates.
 */
@SuppressWarnings("serial")
public final class Transition extends Component
{
	private BitSet nxstate;

	/**
	 * Returns the value of the input to the transition.
	 *
	 * @see org.ggp.base.util.propnet.architecture.Component#getValue()
	 */
	@Override
	public boolean getValue()
	{
		return nxstate.get(index);

		// return getSingleInput().getValue();
	}

	/**
	 * @see org.ggp.base.util.propnet.architecture.Component#toString()
	 */
	@Override
	public String toString()
	{
		//
		return "TRANSITION " + hashCode();
	}

	@Override
	public void printTree(int indent) {
		GamerLogger.emitToConsole(makeindent(indent) + "Transition " + this.hashCode() + " " + this.getValue() + "\n");
		for ( Component component : getInputs() )
		{
			component.printTree(indent + 2);
		}
	}

	@Override
	public void augmentBitSets(BitSet current_state, BitSet next_state) {
		curstate = current_state;
		nxstate = next_state;
	}

	public void update_and_fprop(Set<Component> worklist, boolean tracing)
	{
		boolean in_value = getSingleInput().getValue();

		/* Get the index of our single output */
		int out_index = getSingleOutput().index;

		if (nxstate.get(out_index) != in_value)
		{
			if (tracing)
			{
				GamerLogger.emitToConsole("Transition " + this +
					" propping change " + in_value + " to next state (idx "
					+ out_index + " " + getSingleOutput() + "\n");
			}

			nxstate.set(out_index, in_value);
		}
		else if (tracing)
		{
			GamerLogger.emitToConsole("Transition " + this + " not propping "
					+ in_value +
					"as no change \n");
		}
	}

	@Override
	public void update_and_fprop(Set<Component> worklist, Set<Component> checkset, boolean tracing)
	{
		this.update_and_fprop(worklist, tracing);
	}

	@Override
	public String toDotFormat() {
		// TODO Auto-generated method stub
		return helptoDot("box", "grey", "TRANSITION");
	}
}