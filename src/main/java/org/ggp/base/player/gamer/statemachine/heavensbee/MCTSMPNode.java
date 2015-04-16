package org.ggp.base.player.gamer.statemachine.heavensbee;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;

public class MCTSMPNode {
	private HeavensbeeMCTSMP node_gamer;
	public MachineState node_state;
	public List<Move> node_action_list;
	public MCTSMPNode node_parent;
	public List<MCTSMPNode> children;
	public int generation;
	public boolean expanded;
	public boolean fully_expanded;
	/*public boolean maximal_node;*/

/*	public double max_utility_seen;
	public double min_utility_seen;*/

	public int node_visits;
  	public double node_base_utility;
	public double node_utility;

	private Collection<Integer> roleidxs;

//	public double[] max_utilities_seen;
//	public double[] min_utilities_seen;
//	public double[] node_base_utilities;
//	public double[] node_utilities;

	/*public double[] best_utilities_seen_for_role;*/
	public double[] best_utilities_seen_for_player;

	public int node_main_role_index;

	public MCTSMPNode(MachineState resulting_state,
			          List<Move> action_list,
			          MCTSMPNode parent,
			          HeavensbeeMCTSMP gamer) throws MoveDefinitionException{
		node_gamer = gamer;
		/*
		 * Get a reference to the statemachine and roles
		 */
		StateMachine sm = node_gamer.getStateMachine();
		List<Role> roles = sm.getRoles();

		roleidxs = node_gamer.game_role_indices.values();

		/* Save off the variables */
		node_state = resulting_state;
		node_action_list = action_list;
		node_parent = parent;

		/*
		 * Set up sensible defaults for the utility.
		 */
//		max_utilities_seen = new double[roles.size()];
//		min_utilities_seen = new double[roles.size()];
//		node_base_utilities = new double[roles.size()];
//		node_utilities = new double[roles.size()];

		best_utilities_seen_for_player = new double[roles.size()];
		node_base_utility = 0;
		node_utility = 0;

		for (int ridx: roleidxs)
		{
//			max_utilities_seen[ridx] = 0;
//			min_utilities_seen[ridx] = 1;
//			node_base_utilities[ridx] = 0;
//			node_utilities[ridx] = 0;
			best_utilities_seen_for_player[ridx] = 0;
		}

		node_visits = 0;

		children = new ArrayList<MCTSMPNode>();

		/*
		 * This node hasn't yet been fully expanded.
		 */
		expanded = false;
		fully_expanded = false;

		/*
		 * Work out the generation of this node for logging purposes.
		 */
		if (parent == null)
		{
			generation = 0;
		}
		else
		{
			generation = parent.generation + 1;
		}

		/*
		 * Work out who's responsible for moving in this node
		 */
		if (sm.isTerminal(resulting_state))
		{
			/* We're the mover if it's terminal */
			node_main_role_index = node_gamer.game_role_index;
		}
		else
		{
			/* Map roles to the number of non-noop moves for this node */
			Map<Integer, List<Integer>> map_size_to_role_index = new HashMap<Integer, List<Integer>>();
			int max_size = 0;

			for (Map.Entry<Role, Integer> entry: node_gamer.game_role_indices.entrySet())
			{
				/* Get the list of legal moves for this role */
				List<Move> legals = sm.getLegalMoves(resulting_state, entry.getKey());
				int legals_size = legals.size();
				List<Integer> size_arr;
				if (!map_size_to_role_index.containsKey(legals_size))
				{
					size_arr = new ArrayList<Integer>();
					map_size_to_role_index.put(legals_size, size_arr);
				}
				size_arr = map_size_to_role_index.get(legals_size);
				size_arr.add(entry.getValue());

				if (legals_size > max_size)
				{
					max_size = legals_size;
				}
			}

			/* Pick the first of the largest moved people as the person who moves in this state */
			node_main_role_index = map_size_to_role_index.get(max_size).get(0);
		}
	}

	public void backpropagate(double[] utilities)
	{
		/*node_gamer.log.log(Level.FINE, "Backpropagating {0}", Arrays.toString(utilities));*/
		node_visits++;

		if (utilities[node_gamer.game_role_index] > best_utilities_seen_for_player[node_gamer.game_role_index])
		{
			node_gamer.log.log(Level.FINE, "This utility {0} is better than {1} for role {2}",
					           new Object[]{utilities[node_gamer.game_role_index],
					                        best_utilities_seen_for_player[node_gamer.game_role_index],
					                        node_gamer.game_role_index
					                        });
			best_utilities_seen_for_player = utilities.clone();
		}

		/* Propagate the value in the vector */
		node_base_utility += utilities[node_main_role_index];
		node_utility = node_base_utility / node_visits;

		/*
		 * Backpropagate these utilities.
//		 *   If this is a maximal node, backpropagate:
//		 *   - the utility for our role.
//		 *   - the minimum utilities for each other role
//		 *
//		 *   If this is a minimal node, backpropagate:
//		 *   - the minimum utility for our role
//		 *   - the utilities for each other role.
		 */

//			if (maximal_node && (ridx != node_gamer.game_role_index))
//			{
//				node_gamer.log.log(Level.FINE, "  Max node, {0} not us, using min val", ridx);
//				cloneutils[ridx] = min_utilities_seen[ridx];
//			}
//			else if (!maximal_node && (ridx == node_gamer.game_role_index))
//			{
//				node_gamer.log.log(Level.FINE, "  Min node, {0} is us, using min val", ridx);
//				cloneutils[ridx] = min_utilities_seen[ridx];
//			}
//		}

		if (node_parent != null)
		{
			node_parent.backpropagate(utilities);
		}
	}

	public double uct()
	{
//		double[] uct_values = new double[roleidxs.size()];
//
		if (node_visits == 0)
		{
			return 0;
//			for (int ridx: roleidxs)
//			{
//				uct_values[ridx] = 0;
//			}
		}
		else
		{
			double uct_value = node_utility + Math.sqrt(2 * Math.log(node_parent.node_visits) / node_visits);

//			for (int ridx: roleidxs)
//			{
//				uct_values[ridx] = node_utilities[ridx] + Math.sqrt(2 * Math.log(node_parent.node_visits) / node_visits);
//			}
			return uct_value;
		}
	}

	public double get_utility()
	{
//		if (fully_expanded)
//		{
//			return best_utilities_seen_for_role[node_gamer.game_role_index];
//
//
//			if (maximal_node)
//			{
//				double[] minutils = min_utilities_seen.clone();
//				minutils[node_gamer.game_role_index] = max_utilities_seen[node_gamer.game_role_index];
//				return minutils;
//			}
//			else
//			{
//				double[] maxutils = max_utilities_seen.clone();
//				maxutils[node_gamer.game_role_index] = min_utilities_seen[node_gamer.game_role_index];
//				return maxutils;
//			}
//		}
//		else
//		{
			return node_utility;
//		}
	}

	@Override
	public String toString() {
		String fs = "MCTSNode(gen " + generation +
					        " fe " + fully_expanded +
					        " act " + node_action_list +
					        " role " + node_main_role_index +
					        " util( u " + node_utility +
					        ", bestp " + Arrays.toString(best_utilities_seen_for_player) +
					        "  ))";
		return fs;
	}

	public void check_fully_expanded() {
		if (fully_expanded){
			node_gamer.log.log(Level.FINE, "    Node {0} is already fully expanded", this);
			return;
		}

		if (children.size() == 0)
		{
			fully_expanded = true;
			node_gamer.log.log(Level.FINE, "!!  Node {0} has no children; fe; therefore utility is {1}",
					           new Object[]{ this, get_utility()});
			if (node_parent != null)
			{
				node_parent.check_fully_expanded();
			}
			return;
		}

		for (int ii = 0; ii < children.size(); ii++)
		{
			if (!children.get(ii).fully_expanded)
			{
				return;
			}
		}

		/* All children are fully expanded, so so are we. */
		fully_expanded = true;
		node_gamer.log.log(Level.FINE, "!!  Node {0} + children fe; therefore utilities are {1}",
				           new Object[]{this, get_utility()});
		if (node_parent != null)
		{
			node_parent.check_fully_expanded();
		}
		return;
	}
}
