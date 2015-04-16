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

public final class HeavensbeeGamer extends SampleGamer {
	private Logger log = null;

	public HeavensbeeGamer() throws SecurityException, IOException {

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
//	    log.info("Starting instance of HeavensbeeGamer");
	}

	@Override
	public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		// We get the current start time
		long start = System.currentTimeMillis();

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

			selection = best_multi_move(state, role, opponent, moves);
		}
		else
		{
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

		for (int ii=0; ii < actions.size(); ii++)
		{
			Move action = actions.get(ii);
			List<Move> moves = new ArrayList<Move>();
			moves.add(action);
			MachineState next_state = getStateMachine().getNextState(state, moves);

			int result = maxscore(role, next_state);
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

	public Move best_multi_move(MachineState state, Role role, Role opponent, List<Move> actions) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException
	{
		Move bestaction = actions.get(0);
		int score = 0;

		int alpha = 0;
		int beta = 100;

		log.log( Level.FINE, "Choosing best of {0} moves", actions.size() );

		for (int ii=0; ii < actions.size(); ii++)
		{
			Move action = actions.get(ii);

			log.log( Level.FINE, "Action {0}: {1} - calculating min score", new Object[]{ii, action} );

			int result = mp_min_score(role, opponent, action, state, alpha, beta);

			log.log( Level.FINE, "Action {0}: {1} scored {2}", new Object[]{ii, action, result} );

			alpha = Math.max(alpha, result);

			if (result > score)
			{
				bestaction = action;
				score = result;
			}
		}

		return bestaction;
	}

	public int maxscore(Role role, MachineState state) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException
	{
		StateMachine state_machine = getStateMachine();

		if (state_machine.isTerminal(state))
		{
			return state_machine.getGoal(state, role);
		}

		List<Move> actions = state_machine.getLegalMoves(state, role);
		int score = 0;

		for (int ii=0; ii < actions.size(); ii++)
		{
			Move action = actions.get(ii);
			List<Move> moves = new ArrayList<Move>();
			moves.add(action);
			MachineState next_state = state_machine.getNextState(state, moves);

			int result = maxscore(role, next_state);
			if (result > score)
			{
				score = result;
			}
		}

		return score;
	}

	public int mp_min_score(Role role, Role opponent, Move action, MachineState state, int alpha, int beta)
			throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException
	{
		log.log( Level.FINE, "Starting mp_min_score with alpha {0} beta {1}", new Object[]{alpha, beta} );

		StateMachine sm = getStateMachine();

		List<Role> roles = sm.getRoles();
		List<Move> opp_moves = sm.getLegalMoves(state, opponent);
		log.log( Level.FINE, "Opponent {1} has {0} moves in this state", new Object[]{ opp_moves.size(), opponent} );

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

			log.log( Level.FINE, "Checking moveset {0}", current_move );

			MachineState next_state = sm.getNextState(state, current_move);

			log.log( Level.FINE, "Opponent action {0}: {1} - calculating max score", new Object[]{ii, opp_moves.get(ii)} );

			int result = mp_max_score(role, opponent, next_state, alpha, beta);

			log.log( Level.FINE, "Opponent action {0}: {1} max score {2}", new Object[]{ii, opp_moves.get(ii), result} );

			beta = Math.min(beta, result);
			if (beta <= alpha)
			{
				log.log( Level.FINE, "Beta was {0} but we could keep to alpha {1}", new Object[]{beta, alpha} );
				return alpha;
			}
		}

		return beta;
	}

	public int mp_max_score(Role role, Role opponent, MachineState state, int alpha, int beta)
			throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException
	{
		log.log( Level.FINE, "Starting mp_max_score with alpha {0} beta {1}", new Object[]{alpha, beta} );


		StateMachine sm = getStateMachine();

		if (sm.isTerminal(state))
		{
			return sm.getGoal(state, role);
		}

		List<Move> actions = sm.getLegalMoves(state, role);

		log.log( Level.FINE, "{0} has {1} actions in this state", new Object[]{role, actions.size()} );

		for (int ii=0; ii < actions.size(); ii++)
		{
			Move action = actions.get(ii);
			log.log( Level.FINE, "Action {0}: {1} - calculating min score", new Object[]{ii, action} );
			int result = mp_min_score(role, opponent, action, state, alpha, beta);
			log.log( Level.FINE, "Action {0}: {1} scored {2}", new Object[]{ii, action, result} );

			alpha = Math.max(alpha, result);

			if (alpha >= beta)
			{
				log.log( Level.FINE, "Alpha was {0} but opponent could keep to beta {1}", new Object[]{alpha, beta} );
				return beta;
			}
		}

		return alpha;
	}
}
