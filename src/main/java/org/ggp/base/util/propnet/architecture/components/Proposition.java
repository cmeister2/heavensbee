package org.ggp.base.util.propnet.architecture.components;

import java.util.BitSet;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.GdlRelation;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.logging.GamerLogger;
import org.ggp.base.util.propnet.architecture.Component;

/**
 * The Proposition class is designed to represent named latches.
 */
@SuppressWarnings("serial")
public final class Proposition extends Component
{
	/** The name of the Proposition. */
	private GdlSentence name;
	/** The value of the Proposition. */
	private boolean value;

	public int priority;
	private BitSet nxstate;

	/**
	 * Creates a new Proposition with name <tt>name</tt>.
	 *
	 * @param name
	 *            The name of the Proposition.
	 */
	public Proposition(GdlSentence name)
	{
		this.name = name;
		this.value = false;
		this.priority = 0;
	}

	/**
	 * Getter method.
	 *
	 * @return The name of the Proposition.
	 */
	public GdlSentence getName()
	{
		return name;
	}

    /**
     * Setter method.
     *
     * This should only be rarely used; the name of a proposition
     * is usually constant over its entire lifetime.
     *
     * @return The name of the Proposition.
     */
    public void setName(GdlSentence newName)
    {
        name = newName;
    }

	/**
	 * Returns the current value of the Proposition.
	 *
	 * @see org.ggp.base.util.propnet.architecture.Component#getValue()
	 */
	@Override
	public boolean getValue()
	{
		return curstate.get(index);
	}

	/**
	 * Setter method.
	 *
	 * @param value
	 *            The new value of the Proposition.
	 */
	public void setValue(boolean value)
	{
		this.value = value;
	}

	/**
	 * @see org.ggp.base.util.propnet.architecture.Component#toString()
	 */
	@Override
	public String toString()
	{
		//return
		return "PROP " + this.getName();
	}

	@Override
	public void printTree(int indent) {
		String identifier = this.getName().toString();
		boolean printmore = (indent < 30);
		if (identifier.equals("anon"))
		{
			identifier = "anon-" + this.hashCode();
			printmore = true;
		}
		else if (identifier.contains("next_"))
		{
			printmore = true;
		}

		GamerLogger.emitToConsole(makeindent(indent) + "Proposition '" + identifier + "' " + this.getValue() + "\n");
		if (printmore)
		{
			for ( Component component : getInputs() )
			{
				component.printTree(indent + 2);
			}
		}
	}

	public boolean isBaseProposition()
	{
	    // Skip all propositions without exactly one input.
	    if (getInputs().size() != 1)
	    {
	    	return false;
	    }

		Component component = getSingleInput();
		return (component instanceof Transition);
	}

	public boolean isInputProposition()
	{
	    // Skip all propositions that aren't GdlFunctions.
		if (!(getName() instanceof GdlRelation))
		{
			return false;
		}

		GdlRelation relation = (GdlRelation) getName();
		return (relation.getName().getValue().equals("does"));
	}

	public boolean isViewProposition()
	{
		if (isBaseProposition())
		{
			return false;
		}

		return (getInputs().size() == 1);
	}

	@Override
	public void setIndex(int index_to_set)
	{
		/*GamerLogger.emitToConsole("Proposition '" + this.getName() + "' is index " + index_to_set + "\n");*/
		this.index = index_to_set;
	}

	@Override
	public void augmentBitSets(BitSet current_state, BitSet next_state) {
		curstate = current_state;
		nxstate = next_state;
	}

	public boolean getNextValue()
	{
		return nxstate.get(index);
	}

	@Override
	public void update_and_fprop(Set<Component> worklist,
			                     Set<Component> checkset,
			                     boolean tracing)
	{
		boolean target_value;
		int num_inputs = getInputs().size();

		if (isViewProposition())
		{
			target_value = getSingleInput().getValue();
			if (tracing)
			{
				GamerLogger.emitToConsole("  ViewProp with 1 input; use that value " +
			            target_value + "; ");
			}
		}
		else
		{
			if (tracing)
			{
				GamerLogger.emitToConsole("  Not a ViewProp; use set value " +
			                              this.value + "; ");
			}
			target_value = this.value;
		}

		if (curstate.get(index) != target_value)
		{
			if (tracing)
			{
				GamerLogger.emitToConsole("Marking and propping "
			                               + this + " as " + target_value +
			                               "; ");
			}

			curstate.set(index, target_value);

			for (Component c: getOutputs())
			{
				if (tracing)
				{
					GamerLogger.emitToConsole("Adding '" + c.toString() +
							                  "' to worklist; ");
				}
				worklist.add(c);
			}
		}
		else if (tracing)
		{
				GamerLogger.emitToConsole(this + " is already "
			                              + target_value +  "; ");
		}

		if (tracing)
		{
			GamerLogger.emitToConsole("\n");
		}
	}

	@Override
	public String toDotFormat() {
		// TODO Auto-generated method stub
		return helptoDot("circle", value ? "red" : "white", name.toString());
	}
}