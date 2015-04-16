package org.ggp.base.util.propnet.architecture.components;

import java.util.BitSet;
import java.util.Set;

import org.ggp.base.util.logging.GamerLogger;
import org.ggp.base.util.propnet.architecture.Component;

/**
 * The Not class is designed to represent logical NOT gates.
 */
@SuppressWarnings("serial")
public final class Not extends Component
{
	/**
	 * Returns the inverse of the input to the not.
	 *
	 * @see org.ggp.base.util.propnet.architecture.Component#getValue()
	 */
	@Override
	public boolean getValue()
	{
		return curstate.get(index);
	}

	/**
	 * @see org.ggp.base.util.propnet.architecture.Component#toString()
	 */
	@Override
	public String toString()
	{
		//return toDot("invtriangle", "grey", "NOT");
		return "NOT " + hashCode();
	}

	@Override
	public void printTree(int indent) {
		GamerLogger.emitToConsole(makeindent(indent) + "Not " + this.hashCode() + " " + this.getValue() + "\n");
	}

	@Override
	public void augmentBitSets(BitSet current_state, BitSet next_state) {
		curstate = current_state;
	}

	@Override
	public void update_and_fprop(Set<Component> worklist, Set<Component> checkset, boolean tracing)
	{
		if (tracing)
		{
			GamerLogger.emitToConsole("Checking " + this + ": ");
		}

		boolean not_input = !getSingleInput().getValue();

		if (not_input != curstate.get(index))
		{
			if (tracing)
			{
				GamerLogger.emitToConsole("change in state to " + not_input + "; ");
			}
			curstate.set(index, not_input);
			for (Component c: getOutputs())
			{
				if (checkset != null && checkset.contains(c))
				{
					//GamerLogger.emitToConsole("!!! Component " + c + " is already in the checked set and isn't set \n");
					//throw new RuntimeException();
				}
				if (tracing)
				{
					GamerLogger.emitToConsole("Adding '" + c.toString() + "' to worklist \n");
				}
				worklist.add(c);
			}
		}
		else if (tracing)
		{
			GamerLogger.emitToConsole("no change in state; ");
		}
	}

	@Override
	public String toDotFormat() {
		// TODO Auto-generated method stub
		return helptoDot("invtriangle", "grey", "NOT");
	}
}