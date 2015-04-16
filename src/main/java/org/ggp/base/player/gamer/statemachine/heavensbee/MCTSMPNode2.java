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
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class MCTSMPNode2 {
	private HeavensbeeMCTSMP node_gamer;
	private MachineState node_current_state;
	private MCTSMPNode2 node_parent;

	/**
	 * How many times has this node been visited?
	 */
	private int node_visits;

	/**
	 * A map of Moves for this state to the child states created.
	 */
	private Map<List<Move>, MCTSMPNode2> node_moves_map;

	/**
	 * Has this node been expanded yet?
	 */
	private boolean node_expanded;

	/**
	 * Has this node been fully expanded yet?
	 */
	private boolean node_fully_expanded;

	/**
	 * Is this a terminal state node?
	 */
	private boolean node_terminal;

	/**
	 * What numbered role is the _most_ active player in this state?
	 */
	private int node_active_player;

	/**
	 * What generation is this node?
	 */
	private int node_generation;

	/**
	 * The "base" utility of this node.  It's the sum of all back propagated
	 * utilities for the active player of this node.
	 */
	/*private double node_base_utility;*/
	private double[] node_base_utilities;

	/**
	 * The average utility of this node; equal to
	 * node_base_utility / node_visits.
	 */
	/*private double node_utility;*/
	private double[] node_utilities;

	/**
	 * The "base" player utility of this node.  It's the sum of all back
	 * propagated utilities for the player.
	 */
	/*private double node_base_utility_player;*/

	/**
	 * The average utility of this node for the player; equal to
	 * node_base_utility / node_visits.
	 */
	/*private double node_utility_player;*/

	/**
	 * The utilities that the active player would likely choose in this
	 * scenario.
	 */
	private double[] node_chosen_utilities;

	/**
	 * The child that the active player would likely choose in this scenario
	 */
	private MCTSMPNode2 node_ap_chosen_child;

	/**
	 * The move we chose in the chosen utilities state.
	 */
	private Move node_ng_chosen_move;

	/**
	 * We should check our expandedness during backpropagation.
	 */
	private boolean node_should_check_expandedness;
	private int num_players;


	private double player_combat_utility;
	private double best_opponent_combat_utility;

	public MCTSMPNode2(MachineState current_state,
					   MCTSMPNode2 parent,
					   HeavensbeeMCTSMP gamer) throws MoveDefinitionException{

		/* Save off the variables */
		node_gamer = gamer;
		node_current_state = current_state;
		node_parent = parent;

		/**
		 * Initialize some variables.
		 */
		node_visits = 0;
		node_expanded = false;
		node_fully_expanded = false;
		node_should_check_expandedness = false;
		node_terminal = false;
		node_active_player = -1;
		node_chosen_utilities = null;
		node_ap_chosen_child = null;
		node_ng_chosen_move = null;

//		node_base_utility = 0;
//		node_utility = 0;

		num_players = node_gamer.game_role_indices.size();
		node_base_utilities = new double[num_players];
		node_utilities = new double[num_players];

		for (int ii = 0; ii < num_players; ii++)
		{
			node_base_utilities[ii] = 0;
			node_utilities[ii] = 0;
		}

//		node_base_utility_player = 0;
//		node_utility_player = 0;

		/**
		 * Create the map for the moves of this state.
		 */
		node_moves_map = new HashMap<List<Move>, MCTSMPNode2>();

		/**
		 * Work out the generation of this node.
		 */
		if (node_parent != null)
		{
			node_generation = node_parent.node_generation + 1;
		}
		else
		{
			node_generation = 0;
		}
	}

	public void expand() throws MoveDefinitionException,
	                            TransitionDefinitionException
	{
		/**
		 * During expansion, we do lots of things.
		 * Start by checking whether we've already expanded this node, and
		 * return if so.
		 */
		if (node_expanded)
		{
			return;
		}

		/**
		 * Determine whether this node is terminal or not.
		 */
		StateMachine sm = node_gamer.getStateMachine();
		node_terminal = sm.isTerminal(node_current_state);

		/**
		 * Determine whose move it is in this state. If the state is terminal
		 * we treat it as being our move.
		 */
		if (node_terminal)
		{
			node_active_player = node_gamer.game_role_index;
		}
		else if (node_gamer.game_role_indices.size() == 1)
		{
			/**
			 * We're the only player in this game.
			 */
			node_active_player = node_gamer.game_role_index;
		}
		else
		{
			/**
			 * Multiplayer.
			 * Map roles to the number of non-noop moves for this node.
			 */
			Map<Integer, List<Integer>> map_size_to_role_index =
					                     new HashMap<Integer, List<Integer>>();
			int max_size = 0;

			for (Map.Entry<Role, Integer> entry:
				                       node_gamer.game_role_indices.entrySet())
			{
				/**
				 * Get the list of legal moves for this role.
				 */
				List<Move> legals = sm.getLegalMoves(node_current_state,
						                             entry.getKey());
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

			/**
			 * Pick the first of the largest moved people as the person who
			 * moves in this state.
			 */
			node_active_player = map_size_to_role_index.get(max_size).get(0);
		}

		if (node_terminal)
		{
			/**
			 * If the state we've been given is terminal, then we can't do any
			 * more here.  We'll trigger as fully expanded in backpropagation.
			 */
			node_expanded = true;
			node_should_check_expandedness = true;
			return;
		}

		/**
		 * Get a list of joint moves for this state.
		 */
		List<List<Move>> move_sets = sm.getLegalJointMoves(node_current_state);

		/**
		 * For each set of moves, create a new node with those values.
		 */
		for(List<Move> move_set: move_sets)
		{
			MachineState child_state = sm.getNextState(node_current_state,
													   move_set);
			MCTSMPNode2 child_node = new MCTSMPNode2(child_state,
													 this,
													 node_gamer);
			/**
			 * Add the new child to our internal map.
			 */
			node_moves_map.put(move_set, child_node);
		}

		/**
		 * Have now successfully expanded this node.
		 */
		node_expanded = true;
	}

	public void backpropagate(double[] utilities)
	{
		/**
		 * Record that we've visited this node
		 */
		node_visits++;

		/**
		 * Propagate the utilities.
		 */
		for (int ii = 0; ii < num_players; ii++)
		{
			node_base_utilities[ii] += utilities[ii];
			node_utilities[ii] = node_base_utilities[ii] / node_visits;
		}

//		/**
//		 * Propagate the utility for the active player to this node.
//		 */
//		node_base_utility += utilities[node_active_player];
//		node_utility = node_base_utility / node_visits;

//		/**
//		 * Propagate the utility for the gamer to this node.
//		 */
//		node_base_utility_player += utilities[node_gamer.game_role_index];
//		node_utility_player = node_base_utility_player / node_visits;

		/**
		 * If necessary, check for fully expandedness.
		 */
		if (node_should_check_expandedness)
		{
			check_fully_expanded();
		}

		/**
		 * This node will now have been marked as fully expanded or not.
		 *
		 * If it's fully expanded we can determine what the best utility
		 * for the active player is.
		 */
		if (fully_expanded())
		{
			determine_best_utility_for_active_player(utilities);
		}

		/**
		 * Backpropagate these utilities
		 */
		if (node_parent != null)
		{
			node_parent.backpropagate(utilities);
		}
	}

	private void determine_best_utility_for_active_player(double[] utilities)
	{
		/**
		 * The way to determine the best utility depends on whether this is
		 * terminal or not.
		 */
		if (node_terminal)
		{
			/**
			 * Because this is a terminal node, the best utilities are
			 * those passed in.
			 */
			node_chosen_utilities = utilities.clone();
			return;
		}

		/**
		 * Not terminal.  Thus, we should go through the child's utilities
		 * and work out the best utility for the active player.  They should
		 * all be fully expanded otherwise we wouldn't be here.
		 */
		double best_utility_for_ap = 0;
		double[] child_utilities;

		for (Map.Entry<List<Move>, MCTSMPNode2> entry:
			                                         node_moves_map.entrySet())
		{
			MCTSMPNode2 child = entry.getValue();
			Move child_move = entry.getKey().get(node_gamer.game_role_index);
			child_utilities = child.node_chosen_utilities;

			if (node_chosen_utilities == null ||
				child_utilities[node_active_player] > best_utility_for_ap)
			{
				/**
				 * This set of utilities is the best for the active player so
				 * far, so they're likely to pick this.  We choose our move.
				 */
				node_chosen_utilities = child_utilities.clone();
				node_ap_chosen_child = child;
				node_ng_chosen_move = child_move;
				best_utility_for_ap = child_utilities[node_active_player];
			}
		}
	}

	public double uct()
	{
		if (node_visits == 0)
		{
			return 0;
		}

		double uct_value = (node_utilities[node_active_player] +
							Math.sqrt(2 * Math.log(node_parent.node_visits)
									                           / node_visits));

		return uct_value;
	}

	@Override
	public String toString() {
		String fs = "MCTSNode(gen " + node_generation + ")";
		return fs;
	}

	public void check_fully_expanded()
	{
		if (node_terminal)
		{
			set_fully_expanded();
			return;
		}

		/**
		 * Check on the children of this node; if they're all fully expanded,
		 * then this is also fully expanded.
		 */
		for (MCTSMPNode2 child: node_moves_map.values())
		{
			if (!child.fully_expanded())
			{
				/**
				 * One child is not fully expanded, so neither are we.
				 */
				return;
			}
		}

		/**
		 * All children were fully expanded. That means this is fully expanded.
		 */
		set_fully_expanded();

	}

	public MachineState getCurrent_state() {
		return node_current_state;
	}

	public MCTSMPNode2 select() {
		if (node_visits == 0)
		{
			/**
			 * Select ourselves because we've not yet been visited.
			 */
			return this;
		}

		/**
		 * Work out if we should visit any of our child states.
		 */
		Collection<MCTSMPNode2> node_children = node_moves_map.values();

		for (MCTSMPNode2 child: node_children)
		{
			if (child.node_visits == 0)
			{
				/**
				 * Select this child as it has not been visited yet.
				 */
				return child;
			}
		}

		/**
		 * We've expanded all of our immediate children. Expand a child again,
		 * bearing in mind its UCT value.  We should always pick the highest
		 * UCT-valued child, because our opponents will probably pick their
		 * best child, and we want to work out how to win in this situation.
		 */
		double score = 0;
		double child_score;
		MCTSMPNode2 node_to_select = null;

		for (MCTSMPNode2 child: node_children)
		{
			if (node_to_select == null && !child.fully_expanded())
			{
				node_to_select = child;
			}

			child_score = child.uct();

			if (child_score > score && !child.fully_expanded())
			{
				node_to_select = child;
				score = child_score;
			}
		}

		/**
		 * Return the result of selecting on the chosen child.
		 */
		return node_to_select.select();
	}

	boolean fully_expanded() {
		return node_fully_expanded;
	}

	private void set_fully_expanded()
	{
		node_fully_expanded = true;

		/**
		 * If there's a parent, set the flag for checking expandedness
		 */
		if (node_parent != null)
		{
			node_parent.node_should_check_expandedness = true;
		}
	}

	Move most_appropriate_move()
	{
		Move ma_move = null;

		if (node_gamer.game_type == 0)
		{
			ma_move = most_appropriate_move_single_player();
		}
		else if (node_gamer.game_type == 1)
		{
			ma_move = most_appropriate_move_zero_sum();
		}
		else if (node_gamer.game_type == 2)
		{
			ma_move = most_appropriate_move_combat();
		}

		return ma_move;
	}

	private Move most_appropriate_move_combat() {
		Move ma_move = null;

		if (fully_expanded())
		{
			node_gamer.log.log(Level.WARNING,
	"CB: Tree is fully expanded; the chosen move was {0} (utilities {1})",
	new Object[]{ node_ng_chosen_move, Arrays.toString(node_chosen_utilities)}
			);

			return node_ng_chosen_move;
		}

		/**
		 * Do MCTS to work out the best move.
		 */
		double test_utility;
		double best_utility = 0;

		for (Map.Entry<List<Move>, MCTSMPNode2> entry:
			                                         node_moves_map.entrySet())
		{
			/**
			 * We want to select the best utility for the player.
			 */
			MCTSMPNode2 cn = entry.getValue();

			test_utility = cn.combat_utility();

			//test_utility = cn.node_utilities[node_gamer.game_role_index];

			node_gamer.log.log(Level.WARNING, "CB: Checking node {0} ({3}): our combat utility {1} utilities {2}",
					new Object[]{entry.getValue(), test_utility, Arrays.toString(cn.node_utilities), entry.getKey()});

			if (ma_move == null || test_utility > best_utility)
			{
				ma_move = entry.getKey().get(node_gamer.game_role_index);
				best_utility = test_utility;

				node_gamer.log.log(Level.WARNING, "CB:  Chose this node, best utility now {0}", best_utility);
			}
			else
			{
				node_gamer.log.log(Level.WARNING, "CB:  Didn't choose node");
			}
		}

		node_gamer.log.log(Level.WARNING,
"CB: Tree is partially expanded; the chosen move was {0} (utility {1})",
new Object[]{ ma_move, best_utility}
		);

		return ma_move;
	}

	private Move most_appropriate_move_zero_sum() {
		Move ma_move = null;

		if (fully_expanded())
		{
			node_gamer.log.log(Level.WARNING,
	"ZS: Tree is fully expanded; the chosen move was {0} (utilities {1})",
	new Object[]{ node_ng_chosen_move, Arrays.toString(node_chosen_utilities)}
			);

			return node_ng_chosen_move;
		}

		/**
		 * Do MCTS to work out the best move.
		 */
		double test_utility;
		double best_utility = 0;

		for (Map.Entry<List<Move>, MCTSMPNode2> entry:
			                                         node_moves_map.entrySet())
		{
			/**
			 * We want to select the best utility for the player.
			 */
			MCTSMPNode2 cn = entry.getValue();

			test_utility = cn.combat_utility();
			//test_utility = cn.node_utilities[node_gamer.game_role_index];

			node_gamer.log.log(Level.WARNING, "ZS: Checking node {0} ({3}): our combat utility {1} utilities {2}",
					new Object[]{entry.getValue(), test_utility, Arrays.toString(cn.node_utilities), entry.getKey()});

			if (ma_move == null || test_utility > best_utility)
			{
				ma_move = entry.getKey().get(node_gamer.game_role_index);
				best_utility = test_utility;

				node_gamer.log.log(Level.WARNING, "ZS:  Chose this node, best utility now {0}", best_utility);
			}
			else
			{
				node_gamer.log.log(Level.WARNING, "ZS:  Didn't choose node");
			}
		}

		node_gamer.log.log(Level.WARNING,
"ZS: Tree is partially expanded; the chosen move was {0} (utility {1})",
new Object[]{ ma_move, best_utility}
		);

		return ma_move;
	}

	private Move most_appropriate_move_single_player() {
		Move ma_move = null;

		if (fully_expanded())
		{
			node_gamer.log.log(Level.WARNING,
	"SP: Tree is fully expanded; the chosen move was {0} (utilities {1})",
	new Object[]{ node_ng_chosen_move, Arrays.toString(node_chosen_utilities)}
			);

			return node_ng_chosen_move;
		}

		/**
		 * Do MCTS to work out the best move.
		 */
		double test_utility;
		double best_utility = 0;

		for (Map.Entry<List<Move>, MCTSMPNode2> entry:
			                                         node_moves_map.entrySet())
		{
			/**
			 * We want to select the best utility for the player.
			 */
			MCTSMPNode2 cn = entry.getValue();

			test_utility = cn.node_utilities[node_gamer.game_role_index];

			node_gamer.log.log(Level.WARNING, "SP: Checking node {0} ({2}): our utility {1}",
					new Object[]{entry.getValue(), test_utility, entry.getKey()});

			if (ma_move == null || test_utility > best_utility)
			{
				ma_move = entry.getKey().get(node_gamer.game_role_index);
				best_utility = test_utility;

				node_gamer.log.log(Level.WARNING, "SP:  Chose this node, best utility now {0}", best_utility);
			}
			else
			{
				node_gamer.log.log(Level.WARNING, "SP:  Didn't choose node");
			}
		}

		node_gamer.log.log(Level.WARNING,
"SP: Tree is partially expanded; the chosen move was {0} (utility {1})",
new Object[]{ ma_move, best_utility}
		);

		return ma_move;
	}

	public MCTSMPNode2 cull(MachineState cur_state)
	{
		MCTSMPNode2 new_cur_node = null;

		for (MCTSMPNode2 child: node_moves_map.values())
		{
			if (child.node_current_state.equals(cur_state))
			{
				new_cur_node = child;
				break;
			}
		}

		/**
		 * This must work. Remove the link to the parent node, no need to
		 * propagate up to that level now.
		 */
		new_cur_node.node_parent = null;
		return new_cur_node;
	}

	public double combat_utility()
	{
		double combat_utility = 0;
		player_combat_utility = 0;
		best_opponent_combat_utility = 0;

		for (int ii = 0; ii < num_players; ii++)
		{
			if (ii == node_gamer.game_role_index)
			{
				player_combat_utility = node_utilities[ii];
			}
			else if (node_utilities[ii] > best_opponent_combat_utility)
			{
				best_opponent_combat_utility = node_utilities[ii];
			}
		}

		combat_utility = player_combat_utility - best_opponent_combat_utility;
		return combat_utility;
	}
}
