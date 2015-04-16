package org.ggp.base.player.gamer.statemachine.heavensbee;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.player.gamer.statemachine.sample.SampleGamer;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public final class HeavensbeeMonteCarloTree extends SampleGamer {
	private Logger log = null;
	private int depth_limit = 0;
	private long hard_deadline;

	private StateMachine cur_sm;
	private MachineState cur_state;
	private Role cur_role;
	private MCTSNode cur_node = null;

	@Override
	public void stateMachineMetaGame(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException,
			GoalDefinitionException {

		if (log == null)
		{
			log = Logger.getLogger("MonteCarloTree");
			log.setUseParentHandlers(false);

			try {
				FileHandler filehandler = new FileHandler("./Heavensbee.log");
			    filehandler.setFormatter(new SimpleFormatter());
			    filehandler.setLevel(Level.ALL);
			    log.addHandler(filehandler);
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		    ConsoleHandler handler = new ConsoleHandler();
		    handler.setFormatter(new SimpleFormatter());
		    handler.setLevel(Level.ALL);
		    log.addHandler(handler);
		    log.setLevel(Level.FINE);
		}
	}

	@Override
	public Move stateMachineSelectMove(long timeout) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException
	{
		// We get the current start time
		long start = System.currentTimeMillis();
		hard_deadline = timeout - PREFERRED_PLAY_BUFFER;
		log.log( Level.FINE, "Timeout is {0}", timeout);

		/**
		 * We put in memory the list of legal moves from the
		 * current state. The goal of every stateMachineSelectMove()
		 * is to return one of these moves. The choice of which
		 * Move to play is the goal of GGP.
		 */
		cur_sm = getStateMachine();
		cur_state = getCurrentState();
		cur_role = getRole();

		Map<Role, Integer> role_indices = cur_sm.getRoleIndices();
		int role_index = role_indices.get(cur_role);
		log.log(Level.FINE, "Current role index is {0}", role_index);

		List<Move> moves = cur_sm.getLegalMoves(cur_state, cur_role);
		Move selection = moves.get(0);

		/*
		 * Select the node which we're going to be using.
		 */
		if (cur_node == null)
		{
			/*
			 * Create the initial node
			 */
			cur_node = new MCTSNode(cur_state, null, null);
		}

		MCTSNode chosen_node;

		long now = System.currentTimeMillis();

		MCTSNode best_child = null;

		while ((now < hard_deadline))
		{
			if (cur_node.fully_expanded)
			{
				log.log(Level.WARNING, "@@ Tree is fully expanded from this point - utility is {0}", cur_node.max_utility);
			}
			else
			{
				chosen_node = node_select(cur_node);
				node_expand(chosen_node);

				/*
				 * Simulate this node.
				 */
				double utility = node_simulate_1player(chosen_node);
				node_backpropagate(chosen_node, utility);
			}

			/*
			 * Work out the current best child.
			 */
			double best_utility = 0;
			double test_utility;
			for (int ii = 0; ii < cur_node.children.size(); ii++)
			{
				MCTSNode child = cur_node.children.get(ii);

				if (child.fully_expanded)
				{
					/*
					 * The child is fully expanded, so the test utility is the max utility.
					 */
					test_utility = child.max_utility;
				}
				else
				{
					/*
					 * The child is not fully expanded so use the propagated utility.
					 */
					test_utility = child.utility;
				}

				log.log(Level.FINE, "Child {0} testutility {1}", new Object[]{child, test_utility});
				if ((best_child == null) || (test_utility > best_utility))
				{
					best_child = child;
					best_utility = test_utility;
				}
			}

			/*
			 * Get the best move in this situation.
			 */
			log.log(Level.INFO, "Current best move is {0}, with a best_utility of {1}",
					new Object[]{best_child.node_action_list.get(role_index), best_utility});

			if (cur_node.fully_expanded)
			{
				break;
			}

			/*
			 * Update the time
			 */
			now = System.currentTimeMillis();
		}

		/*
		 * We've chosen a node. We can delete the other nodes in the tree if we like.
		 * Update the current node info.
		 */
		selection = best_child.node_action_list.get(role_index);
		cur_node = best_child;
		// Make it a root node so we don't propagate all the way back up.
		cur_node.node_parent = null;

		// We get the end time
		// It is mandatory that stop<timeout
		long stop = System.currentTimeMillis();

		/**
		 * These are functions used by other parts of the GGP codebase
		 * You shouldn't worry about them, just make sure that you have
		 * moves, selection, stop and start defined in the same way as
		 * this example, and copy-paste these two lines in your player
		 */
		notifyObservers(new GamerSelectedMoveEvent(moves, selection, stop - start));
		return selection;
	}

	private void node_backpropagate(MCTSNode chosen_node, double utility) {
		chosen_node.visits++;
		chosen_node.base_utility += utility;
		chosen_node.utility = chosen_node.base_utility / chosen_node.visits;
		chosen_node.max_utility = Math.max(chosen_node.max_utility, utility);

		log.log(Level.INFO,
				"Backprop node {0} utility {1}: new utility {2} visits {3} max_utility {4}",
				new Object[]{ chosen_node, utility, chosen_node.utility, chosen_node.visits, chosen_node.max_utility });
		if (chosen_node.node_parent != null)
		{
			node_backpropagate(chosen_node.node_parent, utility);
		}
	}

	public MCTSNode node_select(MCTSNode node)
	{
		log.log(Level.INFO, "Selecting node from {0}", node);

		if (node.visits == 0)
		{
			/*
			 * Select this node because it's not been visited yet.
			 */
			return node;
		}

		int num_children = node.children.size();
		for (int ii = 0; ii < num_children; ii++)
		{
			MCTSNode child = node.children.get(ii);
			if (child.visits == 0)
			{
				/*
				 * Select this child because it's not been visited yet.
				 */
				return child;
			}
		}

		/*
		 * Look for a child to expand more.
		 */
		int score = 0;
		MCTSNode result = null;
		for (int ii = 0; ii < node.children.size(); ii++)
		{
			MCTSNode child = node.children.get(ii);
			if ((result == null) && (!child.fully_expanded))
			{
				result = child;
			}

			int new_score = node_selectfn(child);
			if ((new_score > score) && (!child.fully_expanded))
			{
				score = new_score;
				result = child;
			}
		}

		return node_select(result);
	}

	private void node_expand(MCTSNode node_to_expand) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		if (node_to_expand.expanded)
		{
			return;
		}

		if (cur_sm.isTerminal(node_to_expand.node_state))
		{
			node_to_expand.expanded = true;
			log.log(Level.INFO, "Node {0} is terminal, not expanding further", node_to_expand);
			check_fully_expanded(node_to_expand);
			return;
		}

		List<List<Move>> move_sets = cur_sm.getLegalJointMoves(node_to_expand.node_state);

		log.log(Level.INFO, "Expanding node {0}", node_to_expand);
		log.log(Level.INFO, "  {0} legal movesets in this state", move_sets.size());

		for (int ii = 0; ii < move_sets.size(); ii++)
		{
			/*
			 * Work out the result of applying these movesets.
			 */
			List<Move> move_set = move_sets.get(ii);
			log.log(Level.FINE, "  MoveSet {0}: {1}", new Object[]{ii, move_set});

			MachineState new_state = cur_sm.getNextState(node_to_expand.node_state, move_set);
			MCTSNode new_node = new MCTSNode(new_state, move_set, node_to_expand);

			log.log(Level.FINE, "  Added expanded node {0}", new_node);
			node_to_expand.children.add(new_node);
		}
		node_to_expand.expanded = true;
	}
	public int node_selectfn(MCTSNode child){
		if (child.visits == 0)
		{
			return 0;
		}

		int uct_value = (int) (child.utility + Math.sqrt(2 * Math.log(child.node_parent.visits) / child.visits));
		return uct_value;
	}

	public double node_simulate_1player(MCTSNode node_to_simulate) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException
	{
		log.log(Level.INFO, "Simulating node {0} in 1 player", node_to_simulate);
		//int utility = maxscore(cur_role, node_to_simulate.node_state, 0);
		double utility = montecarlo(cur_role, node_to_simulate.node_state, 5);
		return utility;
	}

	public double maxscore(Role role, MachineState state, int depth) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException
	{
		long now = System.currentTimeMillis();

		log.log( Level.FINE, "  Starting maxscore with depth {0}: time {1} deadline {2}", new Object[]{depth, now, hard_deadline} );

		if (cur_sm.isTerminal(state))
		{
			return cur_sm.getGoal(state, role);
		}

		if (depth >= depth_limit)
		{
			double monte_score = montecarlo(role, state, 4);
			log.log( Level.FINE, "  {0} is >= depth limit {1}; montecarlo score is {2}", new Object[]{depth, depth_limit, monte_score} );
			return monte_score;
		}

		if (now > hard_deadline)
		{
			log.log( Level.FINE, "  Timing out! {0}", new Object[]{hard_deadline} );
			return 0;
		}

		List<Move> actions = cur_sm.getLegalMoves(state, role);
		double score = 0;

		for (int ii=0; ii < actions.size(); ii++)
		{
			Move action = actions.get(ii);
			List<Move> moves = new ArrayList<Move>();
			moves.add(action);
			MachineState next_state = cur_sm.getNextState(state, moves);

			log.log( Level.FINE, "  Checking moveset {0}", moves );

			double result = maxscore(role, next_state, depth + 1);
			if (result > score)
			{
				score = result;
			}
		}

		return score;
	}

	public double montecarlo(Role role, MachineState state, int count) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		log.log(Level.FINE, "    Starting montecarlo in state {0}", state);

		if (cur_sm.isTerminal(state))
		{
			double goal = cur_sm.getGoal(state, role);
			goal /= 100;
			log.log(Level.FINE, "    Terminal state: score is {0}", goal);
			return goal;
		}

		double total = 0;
		int[] depth = new int[1];
		double goal;
		for (int ii = 0; ii < count; ii++)
		{
			MachineState stateForCharge = state.clone();
			log.log(Level.FINER, "    Performing depth charge {0}", ii);
			stateForCharge = cur_sm.performDepthCharge(stateForCharge, depth);
			log.log(Level.FINER, "    Finished depth charge {0}", ii);
			try
			{
				goal = cur_sm.getGoal(stateForCharge, role);
				goal /= 100;
			}
			catch (GoalDefinitionException gde)
			{
				goal = 0;
			}

			total = total + goal;
		}

		double rc = total / count;
		log.log( Level.FINE, "  Montecarlo returned {0}", new Object[]{rc} );
		return rc;
	}

    public MachineState log_performDepthCharge(MachineState state, final int[] theDepth) throws TransitionDefinitionException, MoveDefinitionException {
        int nDepth = 0;
        while(!cur_sm.isTerminal(state)) {
            nDepth++;
			log.log(Level.FINER, "      Depth {0}", nDepth);
			log.log(Level.FINER, "      Getting random set");
			List<Move> random_set = cur_sm.getRandomJointMove(state);
			log.log(Level.FINER, "      Got {0}", random_set);

			log.log(Level.FINER, "      Getting next state");
            state = cur_sm.getNextStateDestructively(state, random_set);
			log.log(Level.FINER, "      Got next state {0}", state);
        }
        if(theDepth != null)
            theDepth[0] = nDepth;
        return state;
    }

	public void check_fully_expanded(MCTSNode node) throws GoalDefinitionException
	{
		if (node.fully_expanded){
			log.log(Level.FINE, "    Node {0} is already fully expanded", node);
			return;
		}
		if (node.children.size() == 0)
		{
			node.fully_expanded = true;
			log.log(Level.FINE, "!!  Node {0} has no children; fe; therefore max_utility is {1}", new Object[]{node, node.max_utility});
			if (node.node_parent != null)
			{
				check_fully_expanded(node.node_parent);
			}
			return;
		}

		double max_utility = 0;

		for (int ii = 0; ii < node.children.size(); ii++)
		{
			if (!node.children.get(ii).fully_expanded)
			{
				return;
			}
			max_utility = Math.max(max_utility, node.children.get(ii).utility);
		}

		/* All children are fully expanded, so so are we. */
		node.fully_expanded = true;
		log.log(Level.FINE, "!!  Node {0} + children fe; therefore max_utility is {1}", new Object[]{node, node.max_utility});
		if (node.node_parent != null)
		{
			check_fully_expanded(node.node_parent);
		}
		return;
	}
}