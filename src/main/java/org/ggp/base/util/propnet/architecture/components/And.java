package org.ggp.base.util.propnet.architecture.components;

import java.util.BitSet;
import java.util.Set;

import org.ggp.base.util.logging.GamerLogger;
import org.ggp.base.util.propnet.architecture.Component;

/**
 * The And class is designed to represent logical AND gates.
 */
@SuppressWarnings("serial")
public final class And extends Component
{
	/**
	 * Returns true if and only if every input to the and is true.
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
		//return toDot("invhouse", "grey", "AND");
		return "AND " + hashCode();
	}

	@Override
	public void printTree(int indent) {
		GamerLogger.emitToConsole(makeindent(indent) + "And " + this.hashCode() + " " + this.getValue() + "\n");
		for ( Component component : getInputs() )
		{
			component.printTree(indent + 2);
		}
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

		boolean have_a_negative_input = false;

		for (Component c: getInputs())
		{
			if (!c.getValue())
			{
				have_a_negative_input = true;
				break;
			}
		}

		boolean and_value = !have_a_negative_input;

		if (curstate.get(index) != and_value)
		{
			if (tracing)
			{
				GamerLogger.emitToConsole("Change in state to "+ and_value +"; ");
			}
			curstate.set(index, and_value);
			for (Component c: getOutputs())
			{
				if (checkset != null && checkset.contains(c))
				{
					//GamerLogger.emitToConsole("!!! Component " + c + " is already in the checked set;");
					//throw new RuntimeException();
				}
				if (tracing)
				{
					GamerLogger.emitToConsole("  Adding '" + c.toString() + "' to worklist");
				}

				worklist.add(c);
			}
		}
		else if (tracing)
		{
			GamerLogger.emitToConsole("no change in state");
		}
		if (tracing)
		{
			GamerLogger.emitToConsole("\n");
		}
	}

	@Override
	public String toDotFormat() {
		return helptoDot("invhouse", "grey", "AND");
	}
}
