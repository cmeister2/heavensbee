package org.ggp.base.util.propnet.architecture.components;

import java.util.BitSet;
import java.util.Set;

import org.ggp.base.util.logging.GamerLogger;
import org.ggp.base.util.propnet.architecture.Component;

/**
 * The Or class is designed to represent logical OR gates.
 */
@SuppressWarnings("serial")
public final class Or extends Component
{
	/**
	 * Returns true if and only if at least one of the inputs to the or is true.
	 *
	 * @see org.ggp.base.util.propnet.architecture.Component#getValue()
	 */
	@Override
	public boolean getValue()
	{
		for ( Component component : getInputs() )
		{
			if ( component.getValue() )
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * @see org.ggp.base.util.propnet.architecture.Component#toString()
	 */
	@Override
	public String toString()
	{
		//
		return "OR " + hashCode();
	}

	@Override
	public void printTree(int indent) {
		GamerLogger.emitToConsole(makeindent(indent) + "Or " + this.hashCode() + " " + this.getValue() + "\n");
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
		boolean have_a_positive_input = false;

		for (Component c: getInputs())
		{
			if (c.getValue())
			{
				have_a_positive_input = true;
				break;
			}
		}

		if (have_a_positive_input != curstate.get(index))
		{
			if (tracing)
			{
				GamerLogger.emitToConsole("Change in state to "+ have_a_positive_input +"; ");
			}
			curstate.set(index, have_a_positive_input);
			for (Component c: getOutputs())
			{
				if (checkset != null && checkset.contains(c))
				{
					//GamerLogger.emitToConsole("!!! Component " + c + " is already in the checked set and isn't set");
					//throw new RuntimeException();
				}
				if (tracing)
				{
					GamerLogger.emitToConsole("Adding '" + c.toString() + "' to worklist;");
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
		// TODO Auto-generated method stub
		return helptoDot("ellipse", "grey", "OR");
	}
}