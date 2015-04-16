package org.ggp.base.util.propnet.architecture.components;

import java.util.BitSet;
import java.util.Set;

import org.ggp.base.util.logging.GamerLogger;
import org.ggp.base.util.propnet.architecture.Component;

/**
 * The Constant class is designed to represent nodes with fixed logical values.
 */
@SuppressWarnings("serial")
public final class Constant extends Component
{
	/** The value of the constant. */
	private final boolean value;

	/**
	 * Creates a new Constant with value <tt>value</tt>.
	 *
	 * @param value
	 *            The value of the Constant.
	 */
	public Constant(boolean value)
	{
		this.value = value;
	}

	/**
	 * Returns the value that the constant was initialized to.
	 *
	 * @see org.ggp.base.util.propnet.architecture.Component#getValue()
	 */
	@Override
	public boolean getValue()
	{
		return this.value;
	}

	/**
	 * @see org.ggp.base.util.propnet.architecture.Component#toString()
	 */
	@Override
	public String toString()
	{
		//return toDot("doublecircle", "grey", Boolean.toString(value).toUpperCase());
		return Boolean.toString(value).toUpperCase() + " " + hashCode();
	}

	@Override
	public void printTree(int indent) {
		GamerLogger.emitToConsole(makeindent(indent) + "Constant " + this.hashCode() + " " + this.getValue() + "\n");
	}

	@Override
	public void augmentBitSets(BitSet current_state, BitSet next_state) {
		curstate = current_state;
	}

	@Override
	public void update_and_fprop(Set<Component> worklist, Set<Component> checkset, boolean tracing)
	{
		if (this.value != curstate.get(index))
		{
			if (tracing)
			{
				GamerLogger.emitToConsole("Marking and propping " + this + " to " + this.value + "\n");
			}

			curstate.set(index, this.value);
			for (Component c: getOutputs())
			{
				if (checkset != null && checkset.contains(c))
				{
					GamerLogger.emitToConsole("!!! Component " + c + " is already in the checked set and isn't set \n");
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
			GamerLogger.emitToConsole("Already propped " + this + " \n");
		}
	}

	@Override
	public String toDotFormat() {
		return helptoDot("doublecircle", "grey", Boolean.toString(value).toUpperCase());
	}
}