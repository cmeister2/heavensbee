package org.ggp.base.util.propnet.architecture;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlPool;
import org.ggp.base.util.gdl.grammar.GdlProposition;
import org.ggp.base.util.gdl.grammar.GdlRelation;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.ggp.base.util.logging.GamerLogger;
import org.ggp.base.util.propnet.architecture.components.And;
import org.ggp.base.util.propnet.architecture.components.Constant;
import org.ggp.base.util.propnet.architecture.components.Not;
import org.ggp.base.util.propnet.architecture.components.Or;
import org.ggp.base.util.propnet.architecture.components.Proposition;
import org.ggp.base.util.propnet.architecture.components.Transition;
import org.ggp.base.util.propnet.architecture.components.VisitorComp;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.implementation.heavensbeepropnet.HeavensbeeMachineState;


class TopologicalComparator implements Comparator<Component> {
    @Override
	public int compare(Component c1, Component c2) {
        return c1.topological_index - c2.topological_index;
    }
}

/**
 * The PropNet class is designed to represent Propositional Networks.
 *
 * A propositional network (also known as a "propnet") is a way of representing
 * a game as a logic circuit. States of the game are represented by assignments
 * of TRUE or FALSE to "base" propositions, each of which represents a single
 * fact that can be true about the state of the game. For example, in a game of
 * Tic-Tac-Toe, the fact (cell 1 1 x) indicates that the cell (1,1) has an 'x'
 * in it. That fact would correspond to a base proposition, which would be set
 * to TRUE to indicate that the fact is true in the current state of the game.
 * Likewise, the base corresponding to the fact (cell 1 1 o) would be false,
 * because in that state of the game there isn't an 'o' in the cell (1,1).
 *
 * A state of the game is uniquely determined by the assignment of truth values
 * to the base propositions in the propositional network. Every assignment of
 * truth values to base propositions corresponds to exactly one unique state of
 * the game.
 *
 * Given the values of the base propositions, you can use the connections in
 * the network (AND gates, OR gates, NOT gates) to determine the truth values
 * of other propositions. For example, you can determine whether the terminal
 * proposition is true: if that proposition is true, the game is over when it
 * reaches this state. Otherwise, if it is false, the game isn't over. You can
 * also determine the value of the goal propositions, which represent facts
 * like (goal xplayer 100). If that proposition is true, then that fact is true
 * in this state of the game, which means that xplayer has 100 points.
 *
 * You can also use a propositional network to determine the next state of the
 * game, given the current state and the moves for each player. First, you set
 * the input propositions which correspond to each move to TRUE. Once that has
 * been done, you can determine the truth value of the transitions. Each base
 * proposition has a "transition" component going into it. This transition has
 * the truth value that its base will take on in the next state of the game.
 *
 * For further information about propositional networks, see:
 *
 * "Decomposition of Games for Efficient Reasoning" by Eric Schkufza.
 * "Factoring General Games using Propositional Automata" by Evan Cox et al.
 *
 * @author Sam Schreiber
 */

public final class PropNet
{
	/** References to every component in the PropNet. */
	private Set<Component> components;

	/** References to every Proposition in the PropNet. */
	private final Set<Proposition> propositions;

	/** References to every BaseProposition in the PropNet, indexed by name. */
	private final Map<GdlSentence, Proposition> basePropositions;

	/** References to every InputProposition in the PropNet, indexed by name. */
	private final Map<GdlSentence, Proposition> inputPropositions;

	/** References to every LegalProposition in the PropNet, indexed by role. */
	private final Map<Role, Set<Proposition>> legalPropositions;

	/** References to every GoalProposition in the PropNet, indexed by role. */
	private final Map<Role, Set<Proposition>> goalPropositions;

	/** A reference to the single, unique, InitProposition. */
	private final Proposition initProposition;

	/** A reference to the single, unique, TerminalProposition. */
	private final Proposition terminalProposition;

	/** A helper mapping between input/legal propositions. */
	private final Map<Proposition, Proposition> legalInputMap;

	/** A helper list of all of the roles. */
	private final List<Role> roles;

	/** Propcounter to index propositions */
	private int compcounter;

	/** BitSets to represent current state and next state */
	public BitSet current_state;
	public BitSet next_state;

	/** Visitor component to link together all reachable nodes */
	private Component visitor_root;

	/** Reverse postordering */
	private List<Component> topological_ordering;

	/** References to every Component in the PropNet, indexed by index. */
	private final Map<Integer, Component> idx_comps;

	/**
	 * Pristine BitSet state.  This is the state of the machine at the end
	 * of the initial propagation, where all the Nots and Constants have been
	 * propagated and no BaseProposition, input propositions or the init
	 * proposition have been set.  We use this pristine state at the start
	 * of some functions.
	 */
	private BitSet pristine_state;

	private String dot_timestamp;

	/**
	 * Creates a new PropNet from a list of Components, along with indices over
	 * those components.
	 *
	 * @param components
	 *            A list of Components.
	 */
	public PropNet(List<Role> roles, Set<Component> components)
	{
		dot_timestamp = "E:/ggplogs/" + System.currentTimeMillis();

		/* Set up the roles and components */
	    this.roles = roles;
		this.components = components;

		renderToFile(dot_timestamp + "_original.dot");

		/**
		 * Before starting calculation, try doing some pruning.
		 */
		remove_single_output_view_props();
		find_non_connected_components();

		/**
		 * Continue setup
		 */
		this.visitor_root = new VisitorComp();
		this.idx_comps = new HashMap<Integer, Component>();

		/* Set up bit states for current and next */
		current_state = new BitSet();
		next_state = new BitSet();

		/* Set up propositions */
		compcounter = 0;
		this.propositions = recordPropositions();

		/* Do base propositions first */
		this.basePropositions = recordBasePropositions();

		/* Do input propositions next */
		this.inputPropositions = recordInputPropositions();

		/* Setup the remaining indices */
		setupRemainingIndices();

		/* Carry on recording props */
		this.legalPropositions = recordLegalPropositions();
		this.goalPropositions = recordGoalPropositions();
		this.initProposition = recordInitProposition();
		this.terminalProposition = recordTerminalProposition();
		this.legalInputMap = makeLegalInputMap();

		/* Set up the visitor root to make visiting the nodes easy */
		setupVisitorRoot();

		/* Actually do the visitation */
		setupVisitationList();

		/* Augment the components so they're running on BitSets */
		augmentComponents();

		/* Do the initial propagation so that we are in the pristine state */
		initial_propagate();
	}

	private void remove_single_output_view_props() {
		/**
		 * We do not need single output view props.
		 */
		int removed_components = 0;

		List<Component> to_remove = new LinkedList<Component>();

		for (Component c: components)
		{
			if (c instanceof Proposition)
			{
				Proposition p = (Proposition) c;
				if (p.isViewProposition() && p.getOutputs().size() == 1)
				{
					/**
					 * Remove this pointless proposition from the net.
					 */
					Component p_input = p.getSingleInput();
					Component p_output = p.getSingleOutput();

//					GamerLogger.log("debug.log",
//							"Removing component '" + c
//							+ "' joining '" + p_input
//							+ "' and '" + p_output
//							+ "'", GamerLogger.LOG_LEVEL_IMPORTANT);

					p_input.removeOutput(c);
					c.removeInput(p_input);

					p_output.removeInput(c);
					c.removeOutput(p_output);

					p_input.addOutput(p_output);
					p_output.addInput(p_input);

					to_remove.add(c);

					removed_components++;
				}
			}
		}

		for (Component c: to_remove)
		{
			components.remove(c);
		}

		GamerLogger.log("debug.log",
				"Removed "+removed_components + " during cull",
				GamerLogger.LOG_LEVEL_IMPORTANT);

		renderToFile(dot_timestamp + "_postcull.dot");
	}

	private void find_non_connected_components() {
		/**
		 * Starting at the terminal node, traverse the graph. Any non-visited
		 * components are not connected and can be removed.
		 */
		Set<Component> visited_components = new HashSet<Component>();
		List<Component> to_visit = new LinkedList<Component>();

		/**
		 * Find the terminal proposition.
		 */
		Component terminal = null;

		for (Component c: components)
		{
			if (c instanceof Proposition)
			{
				Proposition p = (Proposition) c;
				if ( p.getName() instanceof GdlProposition )
				{
					GdlConstant constant = ((GdlProposition) p.getName()).getName();
					if ( constant.getValue().equals("terminal") )
					{
						terminal = c;
						break;
					}
				}
			}
		}

		if (terminal == null)
		{
			GamerLogger.log("debug.log",
					"Error while pruning");
			return;
		}

		to_visit.add(terminal);

		while (!to_visit.isEmpty())
		{
			/**
			 * Remove the top element of the list and traverse its siblings
			 */
			Component to_process = to_visit.get(0);
			to_visit.remove(0);

			visited_components.add(to_process);

			for (Component c: to_process.getInputs())
			{
				if (!visited_components.contains(c))
				{
					to_visit.add(c);
				}
			}

			for (Component c: to_process.getOutputs())
			{
				if (!visited_components.contains(c))
				{
					to_visit.add(c);
				}
			}
		}

		GamerLogger.log("debug.log",
				"Visited " + visited_components.size() + " components out " +
		        "of " + components.size(), GamerLogger.LOG_LEVEL_IMPORTANT);

		if (visited_components.size() < components.size())
		{
			GamerLogger.log("debug.log", "These components are removed:"
					, GamerLogger.LOG_LEVEL_IMPORTANT);

			for (Component c: components)
			{
				if (!visited_components.contains(c))
				{
					GamerLogger.log("debug.log", "  " + c
							, GamerLogger.LOG_LEVEL_IMPORTANT);
				}
			}

			GamerLogger.log("debug.log",
					        "Replacing components set with visited");
			this.components = visited_components;
		}

		renderToFile(dot_timestamp + "_postvisit.dot");
	}

	public void addComponent(Component c)
	{
		components.add(c);
		if (c instanceof Proposition) propositions.add((Proposition)c);
	}

	private void augmentComponents() {
		for (Component c: components)
		{
			c.augmentBitSets(current_state, next_state);
		}
	}

	private void setupVisitationList() {
		ArrayList<Component> visited_components = new ArrayList<Component>();
		depth_first_visit(visitor_root, visited_components);

		topological_ordering = new ArrayList<Component>();

		int topological_counter = 0;

		for (int ii = visited_components.size() - 1; ii >= 0; ii--)
		{
			Component pc = visited_components.get(ii);
			if (!topological_ordering.contains(pc) && (pc != visitor_root))
			{
				topological_ordering.add(pc);
				pc.setTopoIndex(topological_counter);
				topological_counter++;
			}
		}

		if (topological_ordering.size() != components.size())
		{
			GamerLogger.log("debug.log", "Topological size: " + topological_ordering.size() + "; components: " + components.size() + "\n");

			for (Component c: components)
			{
				if (!topological_ordering.contains(c))
				{
					GamerLogger.log("error.log", "Topo order doesn't contain " + c + "\n");
				}
			}
			/* Try and carry on */
		}
	}

	private void depth_first_visit(Component c, List<Component> viscomp)
	{
		/* If the child has already been visited, don't visit it again */
		if (viscomp.contains(c))
		{
			return;
		}

		/* Visit the component */
		viscomp.add(c);

		/* For each child, visit that child, then add c as another visit. */
		for (Component cc: c.getOutputs())
		{
			depth_first_visit(cc, viscomp);
			viscomp.add(c);
		}
	}

	public List<Role> getRoles()
	{
	    return roles;
	}

	public Map<Proposition, Proposition> getLegalInputMap()
	{
		return legalInputMap;
	}

	private Map<Proposition, Proposition> makeLegalInputMap() {
		Map<Proposition, Proposition> legalInputMap = new HashMap<Proposition, Proposition>();
		// Create a mapping from Body->Input.
		Map<List<GdlTerm>, Proposition> inputPropsByBody = new HashMap<List<GdlTerm>, Proposition>();
		for(Proposition inputProp : inputPropositions.values()) {
			List<GdlTerm> inputPropBody = (inputProp.getName()).getBody();
			inputPropsByBody.put(inputPropBody, inputProp);
		}
		// Use that mapping to map Input->Legal and Legal->Input
		// based on having the same Body proposition.
		for(Set<Proposition> legalProps : legalPropositions.values()) {
			for(Proposition legalProp : legalProps) {
				List<GdlTerm> legalPropBody = (legalProp.getName()).getBody();
				if (inputPropsByBody.containsKey(legalPropBody)) {
    				Proposition inputProp = inputPropsByBody.get(legalPropBody);
    				legalInputMap.put(inputProp, legalProp);
    				legalInputMap.put(legalProp, inputProp);
				}
			}
		}
		return legalInputMap;
	}

	/**
	 * Getter method.
	 *
	 * @return References to every BaseProposition in the PropNet, indexed by
	 *         name.
	 */
	public Map<GdlSentence, Proposition> getBasePropositions()
	{
		return basePropositions;
	}

	/**
	 * Getter method.
	 *
	 * @return References to every Component in the PropNet.
	 */
	public Set<Component> getComponents()
	{
		return components;
	}

	/**
	 * Getter method.
	 *
	 * @return References to every GoalProposition in the PropNet, indexed by
	 *         player name.
	 */
	public Map<Role, Set<Proposition>> getGoalPropositions()
	{
		return goalPropositions;
	}

	/**
	 * Getter method. A reference to the single, unique, InitProposition.
	 *
	 * @return
	 */
	public Proposition getInitProposition()
	{
		return initProposition;
	}

	/**
	 * Getter method.
	 *
	 * @return References to every InputProposition in the PropNet, indexed by
	 *         name.
	 */
	public Map<GdlSentence, Proposition> getInputPropositions()
	{
		return inputPropositions;
	}

	/**
	 * Getter method.
	 *
	 * @return References to every LegalProposition in the PropNet, indexed by
	 *         player name.
	 */
	public Map<Role, Set<Proposition>> getLegalPropositions()
	{
		return legalPropositions;
	}

	/**
	 * Getter method.
	 *
	 * @return References to every Proposition in the PropNet.
	 */
	public Set<Proposition> getPropositions()
	{
		return propositions;
	}

	/**
	 * Getter method.
	 *
	 * @return A reference to the single, unique, TerminalProposition.
	 */
	public Proposition getTerminalProposition()
	{
		return terminalProposition;
	}

	/**
	 * Returns a representation of the PropNet in .dot format.
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();

		sb.append("digraph propNet\n{\n");
		for ( Component component : components )
		{
			sb.append("\t" + component.toDotFormat() + "\n");
		}
		sb.append("}");

		return sb.toString();
	}

	/**
     * Outputs the propnet in .dot format to a particular file.
     * This can be viewed with tools like Graphviz and ZGRViewer.
     *
     * @param filename the name of the file to output to
     */
    public void renderToFile(String filename) {
        try {
            File f = new File(filename);
            FileOutputStream fos = new FileOutputStream(f);
            OutputStreamWriter fout = new OutputStreamWriter(fos, "UTF-8");
            fout.write(toString());
            fout.close();
            fos.close();
        } catch(Exception e) {
            GamerLogger.logStackTrace("StateMachine", e);
        }
    }

	/**
	 * Builds an index over the BasePropositions in the PropNet.
	 *
	 * This is done by going over every single-input proposition in the network,
	 * and seeing whether or not its input is a transition, which would mean that
	 * by definition the proposition is a base proposition.
	 *
	 * @return An index over the BasePropositions in the PropNet.
	 */
	private Map<GdlSentence, Proposition> recordBasePropositions()
	{
		Map<GdlSentence, Proposition> basePropositions = new HashMap<GdlSentence, Proposition>();
		for (Proposition proposition : propositions) {
			if (proposition.isBaseProposition())
			{
				/* Set the index for this proposition */
				proposition.setIndex(compcounter);
				basePropositions.put(proposition.getName(), proposition);
				idx_comps.put(compcounter, proposition);
				compcounter++;
			}
		}

		return basePropositions;
	}

	/**
	 * Builds an index over the GoalPropositions in the PropNet.
	 *
	 * This is done by going over every function proposition in the network
     * where the name of the function is "goal", and extracting the name of the
     * role associated with that goal proposition, and then using those role
     * names as keys that map to the goal propositions in the index.
	 *
	 * @return An index over the GoalPropositions in the PropNet.
	 */
	private Map<Role, Set<Proposition>> recordGoalPropositions()
	{
		Map<Role, Set<Proposition>> goalPropositions = new HashMap<Role, Set<Proposition>>();
		for (Proposition proposition : propositions)
		{
		    // Skip all propositions that aren't GdlRelations.
		    if (!(proposition.getName() instanceof GdlRelation))
		        continue;

			GdlRelation relation = (GdlRelation) proposition.getName();
			if (!relation.getName().getValue().equals("goal"))
			    continue;

			Role theRole = new Role((GdlConstant) relation.get(0));
			if (!goalPropositions.containsKey(theRole)) {
				goalPropositions.put(theRole, new HashSet<Proposition>());
			}
			goalPropositions.get(theRole).add(proposition);
		}

		return goalPropositions;
	}

	/**
	 * Returns a reference to the single, unique, InitProposition.
	 *
	 * @return A reference to the single, unique, InitProposition.
	 */
	private Proposition recordInitProposition()
	{
		for (Proposition proposition : propositions)
		{
		    // Skip all propositions that aren't GdlPropositions.
			if (!(proposition.getName() instanceof GdlProposition))
			    continue;

			GdlConstant constant = ((GdlProposition) proposition.getName()).getName();
			if (constant.getValue().toUpperCase().equals("INIT")) {
				return proposition;
			}
		}
		return null;
	}

	/**
	 * Builds an index over the InputPropositions in the PropNet.
	 *
	 * @return An index over the InputPropositions in the PropNet.
	 */
	private Map<GdlSentence, Proposition> recordInputPropositions()
	{
		Map<GdlSentence, Proposition> inputPropositions = new HashMap<GdlSentence, Proposition>();
		for (Proposition proposition : propositions)
		{
			if (proposition.isInputProposition())
			{
				proposition.setIndex(compcounter);
				inputPropositions.put(proposition.getName(), proposition);
				idx_comps.put(compcounter, proposition);
				compcounter++;
			}
		}

		return inputPropositions;
	}

	/**
	 * Builds an index over the LegalPropositions in the PropNet.
	 *
	 * @return An index over the LegalPropositions in the PropNet.
	 */
	private Map<Role, Set<Proposition>> recordLegalPropositions()
	{
		Map<Role, Set<Proposition>> legalPropositions = new HashMap<Role, Set<Proposition>>();
		for (Proposition proposition : propositions)
		{
		    // Skip all propositions that aren't GdlRelations.
			if (!(proposition.getName() instanceof GdlRelation))
			    continue;

			GdlRelation relation = (GdlRelation) proposition.getName();
			if (relation.getName().getValue().equals("legal")) {
				GdlConstant name = (GdlConstant) relation.get(0);
				Role r = new Role(name);
				if (!legalPropositions.containsKey(r)) {
					legalPropositions.put(r, new HashSet<Proposition>());
				}
				legalPropositions.get(r).add(proposition);
			}
		}

		return legalPropositions;
	}

	/**
	 * Builds an index over the Propositions in the PropNet.
	 *
	 * @return An index over Propositions in the PropNet.
	 */
	private Set<Proposition> recordPropositions()
	{
		Set<Proposition> propositions = new HashSet<Proposition>();
		for (Component component : components)
		{
			if (component instanceof Proposition) {
				propositions.add((Proposition) component);
			}
		}
		return propositions;
	}

	/**
	 * Records a reference to the single, unique, TerminalProposition.
	 *
	 * @return A reference to the single, unqiue, TerminalProposition.
	 */
	private Proposition recordTerminalProposition()
	{
		for ( Proposition proposition : propositions )
		{
			if ( proposition.getName() instanceof GdlProposition )
			{
				GdlConstant constant = ((GdlProposition) proposition.getName()).getName();
				if ( constant.getValue().equals("terminal") )
				{
					return proposition;
				}
			}
		}

		return null;
	}

	private void setupRemainingIndices()
	{
		for ( Proposition p : propositions )
		{
			if (!basePropositions.containsValue(p) && !inputPropositions.containsValue(p))
			{
				p.setIndex(compcounter);
				idx_comps.put(compcounter, p);
				compcounter++;
			}
		}

		for (Component c: components)
		{
			if (!(c instanceof Proposition))
			{
				c.setIndex(compcounter);
				idx_comps.put(compcounter, c);
				compcounter++;
			}
		}
	}

	private void setupVisitorRoot()
	{
		for ( Component c : components )
		{
			if (c.getInputs().size() == 0 && c.getOutputs().size() > 0)
			{
				/* GamerLogger.log("debug.log", "Adding zero input component " + c);*/

				/* Add this component as an output to the visitor root */
				visitor_root.addOutput(c);
			}
		}

		GamerLogger.log("debug.log", "Added " +
		                visitor_root.getOutputs().size() + " zero inputs");
	}

	public int getSize() {
		return components.size();
	}

	public int getNumAnds() {
		int andCount = 0;
		for(Component c : components) {
			if(c instanceof And)
				andCount++;
		}
		return andCount;
	}

	public int getNumOrs() {
		int orCount = 0;
		for(Component c : components) {
			if(c instanceof Or)
				orCount++;
		}
		return orCount;
	}

	public int getNumNots() {
		int notCount = 0;
		for(Component c : components) {
			if(c instanceof Not)
				notCount++;
		}
		return notCount;
	}

	public int getNumLinks() {
		int linkCount = 0;
		for(Component c : components) {
			linkCount += c.getOutputs().size();
		}
		return linkCount;
	}

	/**
	 * Removes a component from the propnet. Be very careful when using
	 * this method, as it is not thread-safe. It is highly recommended
	 * that this method only be used in an optimization period between
	 * the propnet's creation and its initial use, during which it
	 * should only be accessed by a single thread.
	 *
	 * The INIT and terminal components cannot be removed.
	 */
	public void removeComponent(Component c) {


		//Go through all the collections it could appear in
		if(c instanceof Proposition) {
			Proposition p = (Proposition) c;
			GdlSentence name = p.getName();
			if(basePropositions.containsKey(name)) {
				basePropositions.remove(name);
			} else if(inputPropositions.containsKey(name)) {
				inputPropositions.remove(name);
				//The map goes both ways...
				Proposition partner = legalInputMap.get(p);
				if(partner != null) {
					legalInputMap.remove(partner);
					legalInputMap.remove(p);
				}
			} else if(name == GdlPool.getProposition(GdlPool.getConstant("INIT"))) {
				throw new RuntimeException("The INIT component cannot be removed. Consider leaving it and ignoring it.");
			} else if(name == GdlPool.getProposition(GdlPool.getConstant("terminal"))) {
				throw new RuntimeException("The terminal component cannot be removed.");
			} else {
				for(Set<Proposition> propositions : legalPropositions.values()) {
					if(propositions.contains(p)) {
						propositions.remove(p);
						Proposition partner = legalInputMap.get(p);
						if(partner != null) {
							legalInputMap.remove(partner);
							legalInputMap.remove(p);
						}
					}
				}
				for(Set<Proposition> propositions : goalPropositions.values()) {
					propositions.remove(p);
				}
			}
			propositions.remove(p);
		}
		components.remove(c);

		//Remove all the local links to the component
		for(Component parent : c.getInputs())
			parent.removeOutput(c);
		for(Component child : c.getOutputs())
			child.removeInput(c);
		//These are actually unnecessary...
		//c.removeAllInputs();
		//c.removeAllOutputs();
	}

	public MachineState getStateFromBase()
	{
		return new HeavensbeeMachineState(next_state);
	}

	public MachineState mark_init()
	{
		/* Mark the init node and propagate */
		 SortedSet<Component> worklist = mark_things(true);

		/* Now forward propagate */
		forwardprop(worklist, false);

		/* The state is this state */
		MachineState state = getStateFromBase();

		/**
		 * Log out this initial state.
		 */
		/*reveal_hbstate(state);*/

		return state;
	}

	public void forwardprop(SortedSet<Component> worklist, boolean tracing)
	{
		int workcounter = 0;

		while (!worklist.isEmpty())
		{
			/**
			 * Get the top item in the workset to work on.
			 */
			Component top_item = worklist.first();
			worklist.remove(top_item);

			if (tracing)
			{
				GamerLogger.log("debug.log", "Working on " + top_item +
						    " (index " + top_item.topological_index + " ) \n");
			}

			top_item.update_and_fprop(worklist, null, tracing);

			workcounter++;
		}

		if (tracing)
		{
			GamerLogger.log("debug.log", "Processed " + workcounter
					                  + "items in fprop\n");
		}
	}

	public void reveal_hbstate(MachineState state)
	{
		GamerLogger.log("debug.log", "Revealing state:" + state + "\n");
		HeavensbeeMachineState hbstate = (HeavensbeeMachineState)state;
		BitSet bs = hbstate.get_bs();
		for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i+1))
		{
			Component c = idx_comps.get(i);
			GamerLogger.log("debug.log", "  Component " + c + "\n");
		}
	}

	public void check_transitions()
	{
		for (Component c: getComponents())
		{
			if (c instanceof Transition)
			{
				check_transition((Transition)c, 0);
			}
		}
	}

	public void check_legal_props(Role r)
	{
		for (Proposition p: getLegalPropositions().get(r))
		{
			check_component(p, 0);
		}
	}

	private void check_transition(Transition c, int indent) {
		// Iterate back up the tree to see what the values for these transitions are.
		String strindent = c.makeindent(indent);

//		GamerLogger.emitToConsole(strindent + c + ": " + c.getValue() + " (transition is for " + c.getSingleOutput() + ")\n");
//		for (Component cc: c.getInputs())
//		{
//			check_component(cc, indent + 2);
//		}
		GamerLogger.log("debug.log", strindent + c + ": " + c.getSingleInput().getValue()
				+ "; target = " + next_state.get(c.getSingleOutput().index) + " = " + c.getSingleOutput() +  "\n");
		if (c.getSingleInput().getValue())
		{
			for (Component cc: c.getInputs())
			{
				check_component(cc, indent + 2);
			}
		}
	}

	private void check_component(Component c, int indent) {
		String strindent = c.makeindent(indent);

		GamerLogger.log("debug.log", strindent + c + ": " + c.getValue() + "\n");
		for (Component cc: c.getInputs())
		{
			if (! (cc instanceof Transition))
			{
				check_component(cc, indent + 2);
			}
		}
	}

	public void initial_propagate()
	{
		GamerLogger.log("debug.log", "Starting initial propagation\n");

		/**
		 * Start the initial propagation by updating and propping all Nots
		 * and Constants; this adds their values to the blank state but
		 * doesn't propagate.  This state is the "pristine" state; we
		 * revert to this when working out next values, before propagating.
		 */
		SortedSet<Component> worklist = getnewWorklist();

		for (Component c: getComponents())
		{
			if (c instanceof Not || c instanceof Constant)
			{
				c.update_and_fprop(worklist, null, true);
			}
		}

		GamerLogger.log("debug.log", "After initial update, the following states were set\n");
		GamerLogger.log("debug.log", current_state.toString());

		/**
		 * Save off the pristine state
		 */
		pristine_state = (BitSet) current_state.clone();

		GamerLogger.log("debug.log", "The pristine state is as follows\n");
		for (int i = pristine_state.nextSetBit(0); i >= 0; i = pristine_state.nextSetBit(i+1))
		{
			Component c = idx_comps.get(i);
			GamerLogger.log("debug.log", "  Component " + c + "\n");
		}

		/* Now forward propagate */
		GamerLogger.log("debug.log", "Now forward propping\n");
		forwardprop(worklist, true);
		GamerLogger.log("debug.log", "Ending initial propagation\n");
	}

	public SortedSet<Component> getnewWorklist()
	{
		Comparator<Component> compy = new TopologicalComparator();
		SortedSet<Component> wl = new TreeSet<Component>(compy);
		return wl;
	}

	/**
	 * This function marks base propositions, and clears any inputs and the
	 * init proposition.
	 * @param state
	 */
	private BitSet get_bitset_from_state(MachineState state)
	{
		if (!(state instanceof HeavensbeeMachineState))
		{
			throw new RuntimeException(
					                 "State is not a Heavensbee MachineState");
		}

		HeavensbeeMachineState hbstate = (HeavensbeeMachineState)state;
		BitSet bs = hbstate.get_bs();
		return bs;
	}

	public SortedSet<Component> mark_things(MachineState state)
	{
		return mark_things(state, null);
	}


	public SortedSet<Component> mark_things(MachineState state,
											 List<GdlSentence> doeses)
	{
		return mark_things(state, doeses, false);
	}

	public SortedSet<Component> mark_things(boolean mark_init)
	{
	return mark_things(null, null, mark_init);
	}

	public SortedSet<Component> mark_things(MachineState state,
			                                List<GdlSentence> doeses,
			                                boolean mark_init)
	{
		/**
		 * Copy the pristine state that we have to our target
		 */
		BitSet target_state = (BitSet) pristine_state.clone();

		//GamerLogger.emitToConsole("Target State " + target_state + "\n");

		if (state != null)
		{
			/**
			 * Get the bitset from the machine state with the bases.
			 */
			BitSet base_set = get_bitset_from_state(state);
			//GamerLogger.emitToConsole("Base set " + base_set + "\n");
			//reveal_hbstate(state);

			/**
			 * Apply the base set to the target state.  This sets the base props
			 */
			target_state.or(base_set);

			//GamerLogger.emitToConsole("Target State w/ base " + target_state + "\n");
		}

		if (doeses != null)
		{
			/**
			 * There's GDL things to do
			 */
			for (GdlSentence smove: doeses)
			{
				Proposition p = getInputPropositions().get(smove);
				//GamerLogger.emitToConsole("Setting input bit " + p.index + "\n");

				if (p != null)
				{
					target_state.set(p.index);
				}
				else
				{
					/**
					 * If p was null, then we removed the input proposition
					 * for having no effect on the propnet; so actually there's
					 * no point in setting the bit.
					 */
//					GamerLogger.log("debug.log", "Ignoring irrelevant move "
//					 + smove);
				}
			}
		}

		if (mark_init)
		{
			Proposition ip = getInitProposition();
			//GamerLogger.emitToConsole("Setting init bit " + ip.index + "\n");
			target_state.set(ip.index);
		}

		/**
		 * Now, take our target state, copy it, and xor it with our current
		 * state, to get a list of differing states.
		 */
		//GamerLogger.emitToConsole("Current State " + current_state + "\n");


		BitSet differing_bits = (BitSet) target_state.clone();
		differing_bits.xor(current_state);

		//GamerLogger.emitToConsole("Differing bits " + differing_bits + "\n");

		/**
		 * Ok, now override the current state, and add any different
		 * states to a work list to update those states.
		 */
		current_state = target_state;

		SortedSet<Component> worklist = getnewWorklist();

		for (int i = differing_bits.nextSetBit(0);
			 i >= 0;
			 i = differing_bits.nextSetBit(i+1))
		{
			Component c = idx_comps.get(i);

			/**
			 * If this component is a proposition, we need to set the value
			 * we think it should be in here.  This only affects base props?
			 */
			if (c instanceof Proposition)
			{
				Proposition p = (Proposition) c;
				boolean p_value = target_state.get(i);

//				GamerLogger.emitToConsole(
//					"  Component "+c+" is proposition " + p
//					+ "; setting to " + p_value + "\n");
				p.setValue(p_value);
			}

//			GamerLogger.emitToConsole("  Adding component " + i +
//					                  " " + c + " to worklist \n");
			worklist.add(c);
		}

		return worklist;
	}

	public void cache_marked_bases(MachineState state)
	{
		if (!(state instanceof HeavensbeeMachineState))
		{
			throw new RuntimeException(
					                 "State is not a Heavensbee MachineState");
		}

		HeavensbeeMachineState hbstate = (HeavensbeeMachineState)state;

		if (!hbstate.is_cached())
		{
//			GamerLogger.emitToConsole("After forward propping, we ended up in this current state\n");
//			for (int i = current_state.nextSetBit(0); i >= 0; i = current_state.nextSetBit(i+1))
//			{
//				Component c = idx_comps.get(i);
//				GamerLogger.emitToConsole("  Component " + c + "\n");
//			}
//
//			GamerLogger.emitToConsole("After forward propping, we ended up in this next state\n");
//			for (int i = next_state.nextSetBit(0); i >= 0; i = next_state.nextSetBit(i+1))
//			{
//				Component c = idx_comps.get(i);
//				GamerLogger.emitToConsole("  Component " + c + "\n");
//			}
//
//			GamerLogger.emitToConsole("Checking our transitions\n");
//			check_transitions();
//
//			GamerLogger.emitToConsole("\n\n\n");

			hbstate.set_cache(current_state, next_state);
		}
		else
		{
			hbstate.check_cache(current_state, next_state);
			//throw new RuntimeException("Didn't need to redo this");
		}
	}

	public boolean use_cache_if_there(MachineState state)
	{
		return false;

//		if (!(state instanceof HeavensbeeMachineState))
//		{
//			throw new RuntimeException(
//					                 "State is not a Heavensbee MachineState");
//		}
//
//		HeavensbeeMachineState hbstate = (HeavensbeeMachineState)state;
//
//		boolean retval;
//
//		if (hbstate.is_cached())
//		{
//			retval = true;
//			current_state = (BitSet) hbstate.get_current_cache().clone();
//			next_state = (BitSet) hbstate.get_next_cache().clone();
//		}
//		else
//		{
//			retval = false;
//		}
//
//		return retval;
	}
}