package org.ggp.base.player.gamer.statemachine.heavensbee;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.player.gamer.statemachine.sample.SampleGamer;
import org.ggp.base.util.logging.GamerLogger;
import org.ggp.base.util.match.Match;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TimeToGetMovingException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class HeavensbeeMCTSMP extends SampleGamer {
	public Logger log = null;
	private int depth_limit = 0;
	private long hard_deadline;

	private StateMachine cur_sm;
	private MachineState cur_state;
	private Role cur_role;
	private MCTSMPNode2 cur_node = null;
	public Map<Role, Integer> game_role_indices;
	public int game_role_index;
	private ConsoleHandler console_handler = null;
	private boolean added_console_handler = false;
	private FileHandler match_handler = null;
	public int game_type;

	public HeavensbeeMCTSMP() {
		log = Logger.getLogger("MonteCarloTree");
		log.setUseParentHandlers(false);

	    console_handler = new ConsoleHandler();
	    console_handler.setFormatter(new SimpleFormatter());
	    console_handler.setLevel(Level.WARNING);
	    log.setLevel(Level.WARNING);
	}

	@Override
	public void stateMachineMetaGame(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException,
			GoalDefinitionException {

		log.log(Level.WARNING, "started stateMachineMetaGame with timeout {0} ", new Object[]{timeout});

		Match cur_match = getMatch();
		String log_for_match = "E:/ggplogs/Heavensbee." + cur_match.getMatchId() + ".log";

		GamerLogger.startFileLogging(cur_match, getRole().toString());
		GamerLogger.setFileToDisplay("debug.log");

		if (!added_console_handler)
		{
			log.addHandler(console_handler);
			added_console_handler = true;
		}

		if (match_handler != null)
		{
			log.removeHandler(match_handler);
			match_handler.close();
			match_handler = null;
		}

		try {
			match_handler = new FileHandler(log_for_match);
			match_handler.setFormatter(new SimpleFormatter());
			match_handler.setLevel(Level.ALL);
		    log.addHandler(match_handler);
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		game_type = -1;

		cur_sm = getStateMachine();

		/*
		 * Set up things that are true for every move.
		 */
		game_role_indices = cur_sm.getRoleIndices();
		cur_role = getRole();

		game_role_index = game_role_indices.get(cur_role);
		log.log(Level.FINE, "Game role index is {0}", game_role_index);

		/*
		 * Clear up any old state
		 */
		cur_node = null;

		/*
		 * Call into stateMachineSelectMove to pregame the system.
		 */
		stateMachineSelectMove(timeout + PREFERRED_PLAY_BUFFER - PREFERRED_METAGAME_BUFFER - 1000);
	}

	@Override
	public Move stateMachineSelectMove(long timeout) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException
	{
		// We get the current start time
		long start = System.currentTimeMillis();
		hard_deadline = timeout - PREFERRED_PLAY_BUFFER - 1000;

		long remaining = hard_deadline - start;
		log.log( Level.FINE, "Timeout is {0}", timeout);
		log.log( Level.WARNING, "start stateMachineSelectMove at {2} with timeout {1} and {0} milliseconds to think",
				new Object[]{remaining, timeout, start});

		/**
		 * We put in memory the list of legal moves from the
		 * current state. The goal of every stateMachineSelectMove()
		 * is to return one of these moves. The choice of which
		 * Move to play is the goal of GGP.
		 */
		cur_sm = getStateMachine();
		cur_state = getCurrentState();

		log.log(Level.FINE, "Current state is {0}", cur_state);

		List<Move> moves = cur_sm.getLegalMoves(cur_state, cur_role);
		Move selection = moves.get(0);

		int num_simulations = 0;

		if (cur_node == null)
		{
			/*
			 * Create the initial node
			 */
			cur_node = new MCTSMPNode2(cur_state, null, this);
		}
		else if (cur_node.getCurrent_state().equals(cur_state))
		{
			log.log( Level.INFO, "## The current node already defines this state");
		}
		else
		{
			/*
			 * Cull the states we're not in, and make the current node the state we are in.
			 */
			cur_node = cur_node.cull(cur_state);
		}

		/*
		 * Select the node which we're going to be using.
		 */
		MCTSMPNode2 chosen_node;
		long now = System.currentTimeMillis();

		/*
		 * Only operate while there's time left before the deadline.
		 */
		while ((now < hard_deadline))
		{
			if (cur_node.fully_expanded())
			{
				log.log(Level.WARNING, "@@ Tree is fully expanded from this point");
			}
			else
			{
				/*
				 * Select a node to expand.
				 */
				chosen_node = cur_node.select();

				/*
				 * Expand that chosen node.
				 */
				chosen_node.expand();

				try
				{
					/*
					 * Simulate this node.
					 */
					double[] utilities = node_simulate(chosen_node);
					num_simulations++;

					/*
					 * Backpropagate that node's utility.
					 */
					chosen_node.backpropagate(utilities);
				}
				catch (TimeToGetMovingException ttgm)
				{
					log.log(Level.WARNING, "Interrupted a depth charge");
				}
			}

			if (cur_node.fully_expanded())
			{
				break;
			}

			/*
			 * Update the time
			 */
			now = System.currentTimeMillis();
		}

		/**
		 * Select the most appropriate move.
		 */
		selection = cur_node.most_appropriate_move();

		// We get the end time
		// It is mandatory that stop<timeout
		long stop = System.currentTimeMillis();
		log.log(Level.WARNING, "Chose move {0}", new Object[]{selection});
		log.log(Level.WARNING, "Managed {0} simulations in {1} milliseconds", new Object[]{num_simulations, stop - start});

		/**
		 * These are functions used by other parts of the GGP codebase
		 * You shouldn't worry about them, just make sure that you have
		 * moves, selection, stop and start defined in the same way as
		 * this example, and copy-paste these two lines in your player
		 */
		notifyObservers(new GamerSelectedMoveEvent(moves, selection, stop - start));
		log.log( Level.WARNING, "end stateMachineSelectMove }");
		return selection;
	}

	public double[] node_simulate(MCTSMPNode2 chosen_node)
			throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException, TimeToGetMovingException
	{
		log.log(Level.INFO, "Simulating node {0} (visits {1})", new Object[]{chosen_node, /*chosen_node.node_visits*/ 0});
		double[] utility_array = montecarlo(cur_role, chosen_node.getCurrent_state(), 5);
		return utility_array;
	}

	public double[] montecarlo(Role role, MachineState state, int count) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException, TimeToGetMovingException
	{
		double[] goals = new double[game_role_indices.size()];

		log.log(Level.FINE, "  Starting montecarlo in state {0}", state);

		if (cur_sm.isTerminal(state))
		{
			for (Map.Entry<Role, Integer> entry: game_role_indices.entrySet())
			{
				int role_index = entry.getValue();
				goals[role_index] = cur_sm.getGoal(state, entry.getKey());
				goals[role_index] /= 100;
			}

			log.log(Level.FINE, "    Terminal state: scores are {0}", goals);
			return goals;
		}

		for (int ii = 0; ii < game_role_indices.size(); ii++)
		{
			goals[ii] = 0;
		}

		double total = 0;
		int[] depth = new int[1];

		double sum_goals = 0;
		double goalvalue;

		for (int ii = 0; ii < count; ii++)
		{
			MachineState stateForCharge = state.clone();
//			log.log(Level.FINER, "    Performing depth charge {0}", ii);

			stateForCharge = cur_sm.performTimedDepthCharge(stateForCharge, depth, hard_deadline);

			//stateForCharge = log_performDepthCharge(stateForCharge, depth);
//			log.log(Level.FINER, "    Finished depth charge {0}", ii);
//			try
//			{


				for (Map.Entry<Role, Integer> entry: game_role_indices.entrySet())
				{
					int role_index = entry.getValue();
					goalvalue = cur_sm.getGoal(stateForCharge, entry.getKey());
					goalvalue /= 100;

					/**
					 * Add the value / count as it's the average of "count"
					 * runs.
					 */

					goals[role_index] += (goalvalue / count);

					if (ii == 0)
					{
						sum_goals += goalvalue;
					}
				}
//				goal = cur_sm.getGoal(stateForCharge, role);
//				goal /= 100;
//			}
//			catch (GoalDefinitionException gde)
//			{
//				log.log(Level.SEVERE, "Goal definition exception" + gde);
//				goal = 0;
//			}

//			total = total + goal;
		}

//		double rc = total / count;

		if (game_type == -1)
		{
			/* Determine game type */
			if (game_role_indices.size() == 1)
			{
				/* Single player game */
				log.log(Level.WARNING, "Detected single player game");
				game_type = 0;
			}
			else
			{
				if (Math.abs(sum_goals - 1) < 0.00001)
				{
					/* Zero sum game */
					log.log(Level.WARNING, "Detected zero sum game as sum goals v close to 1");
					game_type = 1;
				}
				else
				{
					/* Can both score well */
					log.log(Level.WARNING, "Detected combative game (not zero sum)");
					game_type = 2;
				}
			}
		}
		log.log( Level.FINE, "  Montecarlo returned {0}", Arrays.toString(goals) );
		return goals;
	}

    public MachineState log_performDepthCharge(MachineState state, final int[] theDepth) throws TransitionDefinitionException, MoveDefinitionException {
        int nDepth = 0;
        while(!cur_sm.isTerminal(state)) {
            nDepth++;
            if (nDepth > 50)
            {
            	throw new RuntimeException("Got out of hand");
            }

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

    public MCTSMPNode minimax(MCTSMPNode node_to_test, int depth)
    {
    	log.log(Level.WARNING, "Node {0}", new Object[]{node_to_test});

    	if (node_to_test.node_main_role_index == game_role_index)
    	{
    		/* This is a max node */
        	log.log(Level.WARNING, "We are the movers here, so we pick the biggest of the minimal nodes");


    	}


    	throw new RuntimeException("no minimax yet");
    	//return null;
    }

	@Override
	public void stateMachineStop() {
		// Sample gamers do no special cleanup when the match ends normally.
		cur_node = null;
	}
}