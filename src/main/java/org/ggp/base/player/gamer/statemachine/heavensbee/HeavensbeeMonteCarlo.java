package org.ggp.base.player.gamer.statemachine.heavensbee;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.player.gamer.statemachine.sample.SampleGamer;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public final class HeavensbeeMonteCarlo extends SampleGamer {
	private Logger log = null;
	private int depth_limit = 0;
	private long hard_deadline;
	private int max_game_actions;
	private int most_awesome_score;

	public HeavensbeeMonteCarlo() throws SecurityException, IOException {

//		if (log == null)
//		{
//			 log = Logger.getLogger(ClassName.class.getName());
//			// TODO Auto-generated constructor stub
//		    ConsoleHandler handler = new ConsoleHandler();
//		    FileHandler filehandler = new FileHandler("./Heavensbee.log");
//		    handler.setFormatter(new SimpleFormatter());
//		    handler.setLevel(Level.ALL);
//		    log.addHandler(handler);
//		    filehandler.setFormatter(new SimpleFormatter());
//		    filehandler.setLevel(Level.ALL);
//		    log.addHandler(filehandler);
//		    log.setLevel(Level.ALL);
//		}
//
//	    log.log(Level.INFO, "Starting instance of {0}", ClassName.class.getName());
	}

	@Override
	public void stateMachineMetaGame(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException,
			GoalDefinitionException {
		max_game_actions = 0;
	}

	@Override
	public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		// We get the current start time
		long start = System.currentTimeMillis();
		hard_deadline = timeout - 500;
		log.log( Level.FINE, "Timeout is {0}", timeout);

		/**
		 * We put in memory the list of legal moves from the
		 * current state. The goal of every stateMachineSelectMove()
		 * is to return one of these moves. The choice of which
		 * Move to play is the goal of GGP.
		 */
		StateMachine sm = getStateMachine();
		MachineState state = getCurrentState();
		Role role = getRole();
		List<Move> moves = getStateMachine().getLegalMoves(state, role);

		List<Role> roles = sm.getRoles();
		Move selection;

		if (roles.size() == 2){
			// Do multiplayer

			log.log( Level.FINE, "Running 2 player game - i am playing {0}", role);

			Role opponent;

			log.log( Level.FINE, "Role 0: {0}  Role 1: {1}", new Object[]{ roles.get(0), roles.get(1)});

			if (role.equals(roles.get(0)))
			{
				log.log (Level.FINE, "Role 0 == role");
				opponent = roles.get(1);
			}
			else
			{
				log.log (Level.FINE, "Role 0 != role");
				opponent = roles.get(0);
			}

			depth_limit = 4;
			selection = best_multi_move(state, role, opponent, moves, 0);
		}
		else
		{
			log.log( Level.FINE, "Running 1 player game - i am playing {0}", role);
			depth_limit = 8;
			selection = best_move(state, role, moves);
		}

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

	public Move best_move(MachineState state, Role role, List<Move> actions) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException
	{
		Move bestaction = actions.get(0);
		int score = 0;
		int depth = 0;

		for (int ii=0; ii < actions.size(); ii++)
		{
			Move action = actions.get(ii);
			List<Move> moves = new ArrayList<Move>();
			moves.add(action);
			MachineState next_state = getStateMachine().getNextState(state, moves);

			int result = maxscore(role, next_state, depth + 1);
			if (result == 100)
			{
				bestaction = action;
				break;
			}
			else if (result > score)
			{
				bestaction = action;
				score = result;
			}
		}

		return bestaction;
	}

	public Move best_multi_move(MachineState state, Role role, Role opponent, List<Move> actions, int depth)
			throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException
	{
		Move bestaction = actions.get(0);
		most_awesome_score = 0;

		log.log( Level.FINE, "Choosing best of {0} moves", actions.size() );

		if (actions.size() == 1)
		{
			return actions.get(0);
		}

		for (int ii=0; ii < actions.size(); ii++)
		{
			Move action = actions.get(ii);

			log.log( Level.FINE, "Action {0}: {1} - calculating min score", new Object[]{ii, action} );

			int result = mp_min_score(role, opponent, action, state, depth);

			log.log( Level.FINE, "Action {0}: {1} scored {2}", new Object[]{ii, action, result} );

			if (result > most_awesome_score)
			{
				bestaction = action;
				most_awesome_score = result;
			}
		}

		return bestaction;
	}

	public int maxscore(Role role, MachineState state, int depth) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException
	{
		StateMachine state_machine = getStateMachine();

		long now = System.currentTimeMillis();

		log.log( Level.FINE, "Starting max_score with depth {0}: time {1} deadline {2}", new Object[]{depth, now, hard_deadline} );

		if (state_machine.isTerminal(state))
		{
			return state_machine.getGoal(state, role);
		}

		if (depth >= depth_limit)
		{
			int monte_score = montecarlo(role, state, 4);
			log.log( Level.FINE, "{0} is >= depth limit {1}; montecarlo score is {2}", new Object[]{depth, depth_limit, monte_score} );
			return monte_score;
		}

		if (now > hard_deadline)
		{
			log.log( Level.FINE, "Timing out! {0}", new Object[]{hard_deadline} );
			return 0;
		}

		List<Move> actions = state_machine.getLegalMoves(state, role);
		if (actions.size() > max_game_actions)
		{
			max_game_actions = actions.size();
		}

		int score = 0;

		for (int ii=0; ii < actions.size(); ii++)
		{
			Move action = actions.get(ii);
			List<Move> moves = new ArrayList<Move>();
			moves.add(action);
			MachineState next_state = state_machine.getNextState(state, moves);

			log.log( Level.FINE, "Checking moveset {0}", moves );

			int result = maxscore(role, next_state, depth + 1);
			if (result > score)
			{
				score = result;
			}
		}

		return score;
	}

	public int mp_min_score(Role role, Role opponent, Move action, MachineState state, int depth)
			throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException
	{
		log.log( Level.FINE, "[{1}]: Starting mp_min_score with depth {0}", new Object[]{depth, most_awesome_score} );

		StateMachine sm = getStateMachine();

		List<Role> roles = sm.getRoles();
		List<Move> opp_moves = sm.getLegalMoves(state, opponent);
		log.log( Level.FINE, "Opponent {1} has {0} moves in this state", new Object[]{ opp_moves.size(), opponent} );

		int score = 100;

		for (int ii = 0; ii < opp_moves.size(); ii++)
		{
			List<Move> current_move = new ArrayList<Move>();
			if (role.equals(roles.get(0)))
			{
				current_move.add(action);
				current_move.add(opp_moves.get(ii));
			}
			else
			{
				current_move.add(opp_moves.get(ii));
				current_move.add(action);
			}

			//log.log( Level.FINE, "Checking moveset {0}", current_move );

			MachineState next_state = sm.getNextState(state, current_move);

			log.log( Level.FINE, "Opponent action {0}: {1} - calculating max score", new Object[]{ii, opp_moves.get(ii)} );

			int result = mp_max_score(role, opponent, next_state, depth + 1);

			log.log( Level.FINE, "Opponent action {0}: {1} max score {2}", new Object[]{ii, opp_moves.get(ii), result} );

			if (result == 0)
			{
				return 0;
			}

			if (result < score)
			{
				score = result;
			}
			log.log( Level.FINE, "Current min-score: {0}", new Object[]{score} );
		}

		return score;
	}

	public int mp_max_score(Role role, Role opponent, MachineState state, int depth)
			throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException
	{
		long now = System.currentTimeMillis();

		log.log( Level.FINE, "[{3}]: Starting mp_max_score with depth {0}: time {1} deadline {2}", new Object[]{depth, now, hard_deadline, most_awesome_score} );

		StateMachine sm = getStateMachine();

		if (sm.isTerminal(state))
		{
			return sm.getGoal(state, role);
		}
		if (depth >= depth_limit)
		{
			int monte_score = montecarlo(role, state, 4);
			log.log( Level.FINE, "{0} is >= depth limit {1}; montecarlo score is {2}", new Object[]{depth, depth_limit, monte_score} );
			return monte_score;
		}

		if (now > hard_deadline)
		{
			log.log( Level.FINE, "Timing out!");
			return 0;
		}

		int score = 0;

		List<Move> actions = sm.getLegalMoves(state, role);
		if (actions.size() > max_game_actions)
		{
			max_game_actions = actions.size();
		}

		log.log( Level.FINE, "{0} has {1} actions in this state", new Object[]{role, actions.size()} );

		for (int ii=0; ii < actions.size(); ii++)
		{
			Move action = actions.get(ii);
			log.log( Level.FINE, "Action {0}: {1} - calculating min score", new Object[]{ii, action} );
			int result = mp_min_score(role, opponent, action, state, depth);
			log.log( Level.FINE, "Action {0}: {1} scored {2}", new Object[]{ii, action, result} );

			if (result == 100)
			{
				log.log( Level.FINE, "Returning 100!", new Object[]{result} );
				return 100;
			}

			if (result > score)
			{
				score = result;
			}
			log.log( Level.FINE, "Current max-score: {0}", new Object[]{score} );
		}

		return score;
	}

	public int montecarlo(Role role, MachineState state, int count) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		StateMachine sm = getStateMachine();
		int total = 0;
		int[] depth = new int[1];
		int goal;
		for (int ii = 0; ii < count; ii++)
		{
			MachineState stateForCharge = state.clone();
			stateForCharge = sm.performDepthCharge(stateForCharge, depth);

			try
			{
				goal = sm.getGoal(stateForCharge, role);
			}
			catch (GoalDefinitionException gde)
			{
				goal = 0;
			}

			total = total + goal;

			long now = System.currentTimeMillis();
			if (now > hard_deadline)
			{
				log.log( Level.FINE, "Timing out!");
				break;
			}
		}

		int rc = total / count;
		log.log( Level.FINE, "Montecarlo returned {0}", new Object[]{rc} );
		return rc;
	}
}