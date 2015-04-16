package org.ggp.base.util.propnet.architecture.components;

import java.util.BitSet;
import java.util.Set;

import org.ggp.base.util.propnet.architecture.Component;

@SuppressWarnings("serial")
public final class VisitorComp extends Component
{
	/**
	 * @see org.ggp.base.util.propnet.architecture.Component#getValue()
	 */
	@Override
	public boolean getValue()
	{
		return false;
	}

	/**
	 * @see org.ggp.base.util.propnet.architecture.Component#toString()
	 */
	@Override
	public String toString()
	{
		return "VisitorComp";
	}

	@Override
	public void printTree(int indent) {
	}

	@Override
	public void augmentBitSets(BitSet current_state, BitSet next_state) {
	}

	@Override
	public void update_and_fprop(Set<Component> worklist, Set<Component> checkset, boolean tracing)
	{
	}

	@Override
	public String toDotFormat() {
		// TODO Auto-generated method stub
		return null;
	}
}