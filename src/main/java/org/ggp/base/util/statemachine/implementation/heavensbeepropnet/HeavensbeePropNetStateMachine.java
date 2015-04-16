package org.ggp.base.util.statemachine.implementation.heavensbeepropnet;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlRelation;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.logging.GamerLogger;
import org.ggp.base.util.propnet.architecture.Component;
import org.ggp.base.util.propnet.architecture.PropNet;
import org.ggp.base.util.propnet.architecture.components.Proposition;
import org.ggp.base.util.propnet.factory.OptimizingPropNetFactory;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.query.ProverQueryBuilder;


public class HeavensbeePropNetStateMachine extends StateMachine {
    /** The underlying proposition network  */
    private PropNet propNet;
    /** The topological ordering of the propositions */
    private List<Proposition> ordering;
    /** The player roles */
    private List<Role> roles;

    private boolean enable_tracing;

	private Comparator<Proposition> propcompare = new Comparator<Proposition>()
	{
		@Override
		public int compare(Proposition p1, Proposition p2)
		{
			if (p2.priority < p1.priority)
			{
				return 1;
			}
			else if (p2.priority > p1.priority)
			{
				return -1;
			}
			else
			{
				return 0;
			}
		}
	};

    /**
     * Initializes the PropNetStateMachine. You should compute the topological
     * ordering here. Additionally you may compute the initial state here, at
     * your discretion.
     */
    @Override
    public void initialize(List<Gdl> description) {
    	//GamerLogger.setSuppressLoggerOutput(true);

        try {
			propNet = OptimizingPropNetFactory.create(description);
	        roles = propNet.getRoles();

	        enable_tracing = false;

	        //hb_propnet = new HeavensbeePropnet(propNet);

	        /*ordering = getOrdering();*/
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
    }

	/**
	 * Computes if the state is terminal. Should return the value
	 * of the terminal proposition for the state.
	 */
	@Override
	public boolean isTerminal(MachineState state) {
		//GamerLogger.emitToConsole("\n\nStart isTerminal {\n");

		SortedSet<Component> worklist;

//		if (!propNet.use_cache_if_there(state))
//		{
			// Mark the base propositions.
			worklist = propNet.mark_things(state);

			// Forward propagate the bases.
			propNet.forwardprop(worklist, enable_tracing);

			// Do cache checking here.
//			propNet.cache_marked_bases(state);
//		}

		// Run the propmark algorithm on the terminal prop
		Proposition termprop = propNet.getTerminalProposition();

		boolean isTerminal = termprop.getValue();

		// Check the terminal node
		//GamerLogger.emitToConsole("End isTerminal, returning " + isTerminal + " }\n\n\n");
		return isTerminal;
	}

	/**
	 * Computes the goal for a role in the current state.
	 * Should return the value of the goal proposition that
	 * is true for that role. If there is not exactly one goal
	 * proposition true for that role, then you should throw a
	 * GoalDefinitionException because the goal is ill-defined.
	 */
	@Override
	public int getGoal(MachineState state, Role role)
	throws GoalDefinitionException {
		//GamerLogger.emitToConsole("\n\nStart getGoal {\n");

		Set<Proposition> goalprops = propNet.getGoalPropositions().get(role);

		SortedSet<Component> worklist;

//		if (!propNet.use_cache_if_there(state))
//		{
			// Mark the base propositions.
			worklist = propNet.mark_things(state);

			// Forward propagate the bases.
			propNet.forwardprop(worklist, enable_tracing);

			// Do cache checking here.
//			propNet.cache_marked_bases(state);
//		}

		int truegoals = 0;
		Proposition trueprop = null;

		for (Proposition p: goalprops)
		{
			if (p.getValue())
			{
				//GamerLogger.emitToConsole("Goal proposition " + p.getName() + " is true\n");
				trueprop = p;
				truegoals++;
			}
		}

		if (truegoals != 1)
		{
			GamerLogger.emitToConsole("Number true goals: " + truegoals + "\n");
			propNet.reveal_hbstate(state);
			throw new GoalDefinitionException(state, role);
		}

		int reward = getGoalValue(trueprop);

		//GamerLogger.emitToConsole("End getGoal, returning " + reward + " }\n\n");
		return reward;
	}

	/**
	 * Returns the initial state. The initial state can be computed
	 * by only setting the truth value of the INIT proposition to true,
	 * and then computing the resulting state.
	 */
	@Override
	public MachineState getInitialState() {
		MachineState initstate = propNet.mark_init();
		GamerLogger.emitToConsole("Initial state is " + initstate.toString() + "\n");
		return initstate;
	}

	/**
	 * Computes the legal moves for role in state.
	 */
	@Override
	public List<Move> getLegalMoves(MachineState state, Role role)
	throws MoveDefinitionException {
 		//GamerLogger.emitToConsole("\n\n\nStart getLegalMoves {\n");

		SortedSet<Component> worklist;

//		if (!propNet.use_cache_if_there(state))
//		{
			// Mark the base propositions.
			worklist = propNet.mark_things(state);

			// Forward propagate the bases.
			propNet.forwardprop(worklist, enable_tracing);

			// Do cache checking here.
//			propNet.cache_marked_bases(state);
//		}

		// Get the legal propositions for this role.
		Set<Proposition> legalprops = propNet.getLegalPropositions().get(role);

		//GamerLogger.emitToConsole("Legal props: " + legalprops.toString() + "\n");

		List<Move> legalmoves = new ArrayList<Move>();

		for (Proposition p: legalprops)
		{
			if (p.getValue())
			{

//			}
//
//			Component s = p.getSingleInput();
//
//			if (s.getValue())
//			{
				//GamerLogger.emitToConsole("  LEGAL: Proposition " + p.toString() + "\n");
				Move m = getMoveFromProposition(p);
				//GamerLogger.emitToConsole("  Move is " + m + "\n");
				legalmoves.add(m);
			}
		}

		if (legalmoves.size() == 0)
		{
			GamerLogger.emitToConsole("Unexpectedly 0 legal moves for role\n");
			propNet.reveal_hbstate(state);

			// Mark the base propositions.
			worklist = propNet.mark_things(state);

			// Forward propagate the bases.
			propNet.forwardprop(worklist, true);

			// Check the legal props
			propNet.check_legal_props(role);

			throw new RuntimeException("This is not ok");
		}

		//GamerLogger.emitToConsole("Legal moves: " + legalmoves + " " + legalmoves.size() + "\n");

		//GamerLogger.emitToConsole("End getLegalMoves }\n\n\n");
		return legalmoves;
	}

	/**
	 * Computes the next state given state and the list of moves.
	 */
	@Override
	public MachineState getNextState(MachineState state, List<Move> moves)
	throws TransitionDefinitionException {
		//GamerLogger.emitToConsole("\n\n\nStart getNextState {\n");
		//GamerLogger.emitToConsole("Existing state: ");
		//propNet.reveal_hbstate(state);

		// Convert the list of moves into propositions.
		List<GdlSentence> doeses = toDoes(moves);

		// Mark the base propositions and the inputs.
		SortedSet<Component> worklist = propNet.mark_things(state,
				                		                    doeses);

		// Forward propagate the propositions.
		propNet.forwardprop(worklist, enable_tracing);

		// Check that the propnet is set up correctly.
//		GamerLogger.emitToConsole("Show transitions\n");
//		propNet.check_transitions();

//		for (Proposition p : propNet.getBasePropositions().values())
//		{
//			GamerLogger.emitToConsole("Show logic tree for " + p.getName() + "\n");
//			p.printTree(0);
//		}

		// Get the next state from the true bases
		MachineState nextstate = propNet.getStateFromBase();

		//GamerLogger.emitToConsole("Next state: ");
		//propNet.reveal_hbstate(nextstate);

//		GamerLogger.emitToConsole("Next state:\n");
//		for (GdlSentence p: nextstate.getContents())
//		{
//			GamerLogger.emitToConsole("  " + p + "\n");
//		}
//
		//GamerLogger.emitToConsole("End getNextState }\n\n\n");
		//throw new RuntimeException("Whoops!");
		return nextstate;
	}

	/**
	 * This should compute the topological ordering of propositions.
	 * Each component is either a proposition, logical gate, or transition.
	 * Logical gates and transitions only have propositions as inputs.
	 *
	 * The base propositions and input propositions should always be exempt
	 * from this ordering.
	 *
	 * The base propositions values are set from the MachineState that
	 * operations are performed on and the input propositions are set from
	 * the Moves that operations are performed on as well (if any).
	 *
	 * @return The order in which the truth values of propositions need to be set.
	 */
	public List<Proposition> getOrdering()
	{
	    // List to contain the topological ordering.
	    List<Proposition> order = new LinkedList<Proposition>();

		// All of the components in the PropNet
		List<Component> components = new ArrayList<Component>(propNet.getComponents());

		// All of the propositions in the PropNet.
		List<Proposition> propositions = new ArrayList<Proposition>(propNet.getPropositions());

		// Get all the base and input propositions.
		Map<GdlSentence, Proposition> baseprops = propNet.getBasePropositions();
		Map<GdlSentence, Proposition> inputprops = propNet.getInputPropositions();



//	    // TODO: Compute the topological ordering.
//		for (Proposition p: baseprops.values())
//		{
//			p.propagate_priority(0);
//		}
//		for (Proposition p: inputprops.values())
//		{
//			p.propagate_priority(0);
//		}
//
//		Collections.sort(propositions, propcompare);
//
//		for (Proposition p: propositions)
//		{
//			if (!baseprops.containsKey(p) && !inputprops.containsKey(p))
//			{
//				order.add(p);
//			}
//		}

		throw new RuntimeException("No");
		//return order;
	}

	/* Already implemented for you */
	@Override
	public List<Role> getRoles() {
		return roles;
	}

	/* Helper methods */

	/**
	 * The Input propositions are indexed by (does ?player ?action).
	 *
	 * This translates a list of Moves (backed by a sentence that is simply ?action)
	 * into GdlSentences that can be used to get Propositions from inputPropositions.
	 * and accordingly set their values etc.  This is a naive implementation when coupled with
	 * setting input values, feel free to change this for a more efficient implementation.
	 *
	 * @param moves
	 * @return
	 */
	private List<GdlSentence> toDoes(List<Move> moves)
	{
		if (moves.size() < roles.size())
		{
			throw new RuntimeException("Not enough moves for role: " + moves);
		}

		List<GdlSentence> doeses = new ArrayList<GdlSentence>(moves.size());
		Map<Role, Integer> roleIndices = getRoleIndices();

		for (int i = 0; i < roles.size(); i++)
		{
			int index = roleIndices.get(roles.get(i));
			doeses.add(ProverQueryBuilder.toDoes(roles.get(i), moves.get(index)));
		}
		return doeses;
	}

	/**
	 * Takes in a Legal Proposition and returns the appropriate corresponding Move
	 * @param p
	 * @return a PropNetMove
	 */
	public static Move getMoveFromProposition(Proposition p)
	{
		return new Move(p.getName().get(1));
	}

	/**
	 * Helper method for parsing the value of a goal proposition
	 * @param goalProposition
	 * @return the integer value of the goal proposition
	 */
    private int getGoalValue(Proposition goalProposition)
	{
		GdlRelation relation = (GdlRelation) goalProposition.getName();
		GdlConstant constant = (GdlConstant) relation.get(1);
		return Integer.parseInt(constant.toString());
	}

	/**
	 * A Naive implementation that computes a PropNetMachineState
	 * from the true BasePropositions.  This is correct but slower than more advanced implementations
	 * You need not use this method!
	 * @return PropNetMachineState
	 */
/*	public MachineState getStateFromBase()
	{
		Set<GdlSentence> contents = new HashSet<GdlSentence>();
		for (Proposition p : propNet.getBasePropositions().values())
		{
			//GamerLogger.emitToConsole("Checking inputinput of proposition " + p.getName() + " \n");
			if (propmarkp(p.getSingleInput().getSingleInput(), 0))
			{

//			p.setValue(p.getSingleInput().getValue());
//			if (p.getValue())
//			{
				//GamerLogger.emitToConsole("Proposition " + p.getName() + "is true! \n\n");
				contents.add(p.getName());
			}
			else
			{
				//GamerLogger.emitToConsole("Proposition " + p.getName() + "is false \n\n");
//				if (p.getName().toString().contains("step "))
//				{
//					p.printTree(0);
//				}
			}
		}
		throw new RuntimeException("nope");
		//return new HeavensbeeMachineState();
	}*/

	private void markbases(MachineState state)
	{



		Map<GdlSentence, Proposition> baseprops = propNet.getBasePropositions();
		for (GdlSentence s: state.getContents())
		{
			Proposition p = baseprops.get(s);
			//GamerLogger.emitToConsole("  Marking proposition " + s + " true\n");
			p.setValue(true);
		}
	}

	private void markactions(List<GdlSentence> doeses)
	{
		Map<GdlSentence, Proposition> inputprops = propNet.getInputPropositions();

		for (GdlSentence smove: doeses)
		{
			Proposition p = inputprops.get(smove);
			//GamerLogger.emitToConsole("  Marking input prop true " + smove + "\n");
			p.setValue(true);
		}
	}

//	public void run_update()
//	{
//		for (Proposition p : ordering)
//		{
//			if (p.getInputs().size() > 0)
//			{
//				//GamerLogger.emitToConsole("  Updating prop " + p + " " + p.hashCode() + "\n");
//				p.setValue(p.getSingleInput().getValue());
//			}
//		}
//	}

/*	public boolean propmarkp(Component c, int indent)
	{
		boolean propval = false;
		//String indentstr = c.makeindent(indent);

		//GamerLogger.emitToConsole(indentstr + "Component " + c.toString() + "{\n");

		if (c instanceof Proposition)
		{
			Proposition p = (Proposition) c;

			if (p.isBaseProposition() || p.isInputProposition())
			{
				//GamerLogger.emitToConsole(indentstr + "is a base or input proposition\n");
				propval = p.getValue();
			}
			else if (p.isViewProposition())
			{
				//GamerLogger.emitToConsole(indentstr + "is a view proposition\n");
				propval = propmarkp(p.getSingleInput(), indent + 2);
			}
			else if (p.getInputs().size() == 0)
			{
				//GamerLogger.emitToConsole(indentstr + "is a input proposition, probably an init\n");
				propval = p.getValue();
			}
			else
			{
				//GamerLogger.emitToConsole(indentstr + "is some other kind of proposition with " + p.getInputs().size() + " inputs\n");
			}
		}
		else if (c instanceof Not)
		{
			//GamerLogger.emitToConsole(indentstr + "is a negation\n");
			propval = !propmarkp(c.getSingleInput(), indent + 2);
		}
		else if (c instanceof And)
		{
			//GamerLogger.emitToConsole(indentstr + "is a conjunction\n");

			for (Component child: c.getInputs())
			{
				if (!propmarkp(child, indent + 2))
				{
					//GamerLogger.emitToConsole(indentstr + "returning false }\n");
					return false;
				}
			}

			propval = true;
		}
		else if (c instanceof Or)
		{
			//GamerLogger.emitToConsole(indentstr + "is a disjunction\n");

			for (Component child: c.getInputs())
			{
				if (propmarkp(child, indent + 2))
				{
					propval = true;
					break;
				}
			}
		}
		else
		{
			//GamerLogger.emitToConsole(indentstr + "is a " + c.getClass().toString() + " }\n");
		}

		//GamerLogger.emitToConsole(indentstr + "returning " + propval + " }\n");

		return propval;
	}*/


}