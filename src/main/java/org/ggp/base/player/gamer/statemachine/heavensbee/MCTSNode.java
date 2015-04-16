package org.ggp.base.player.gamer.statemachine.heavensbee;

import java.util.ArrayList;
import java.util.List;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;

public class MCTSNode {
	public MachineState node_state;
	public List<Move> node_action_list;
	public MCTSNode node_parent;
	public int visits;
	public List<MCTSNode> children;
	public double utility;
	public double base_utility;
	public double max_utility;
	public int generation;
	public boolean expanded;
	public boolean fully_expanded;

	public MCTSNode(MachineState state, List<Move> action_list, MCTSNode parent){
		/* Save off the variables */
		node_state = state;
		node_action_list = action_list;
		node_parent = parent;
		visits = 0;
		children = new ArrayList<MCTSNode>();
		utility = 0;
		base_utility = 0;
		max_utility = 0;
		expanded = false;
		fully_expanded = false;

		if (parent == null)
		{
			generation = 0;
		}
		else
		{
			generation = parent.generation + 1;
		}
	}

	@Override
	public String toString() {
		String fs = "MCTSNode(gen " + generation + ", fe "+fully_expanded+" u " + utility + ", state " + node_state.hashCode() + ", act "+node_action_list;
		return fs;
	}
}
