package org.ggp.base.util.statemachine.implementation.heavensbeeold;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.logging.GamerLogger;
import org.ggp.base.util.propnet.architecture.Component;
import org.ggp.base.util.propnet.architecture.PropNet;
import org.ggp.base.util.propnet.architecture.components.Proposition;
import org.ggp.base.util.statemachine.MachineState;

public class HeavensbeePropnet {
	private PropNet prop;

	/** References to every Proposition in the PropNet. */
	private List<HeavensbeeProposition> hb_propositions;
	private HashMap<GdlSentence, Integer> hb_prop_map;

	/* References to the Connectives in the propnet */


	private List<Component> visited_components;
	private BitSet propnet_state;
	private int num_base_props;

	public HeavensbeePropnet(PropNet pn)
	{
		prop = pn;
		hb_prop_map = new HashMap<GdlSentence, Integer>();

		/* Convert propositions to HeavensbeePropositions */
		int num_props = prop.getPropositions().size();
		int counter = 0;

		/* Make an array for hb props */
		hb_propositions = new ArrayList<HeavensbeeProposition>(num_props);

		/* Make propositions for base propositions. */
		Collection<Proposition> baseprops = prop.getBasePropositions().values();
		num_base_props = baseprops.size();

		/* Assign indices to the base propositions.  Use the BaseProposition class */
		for (Proposition p: baseprops)
		{
			HeavensbeeProposition hbp = new HeavensbeeBaseProposition(counter, p, propnet_state);
			hb_propositions.add(counter, hbp);
			hb_prop_map.put(p.getName(), counter);
			counter++;
		}

		/* Assign indices for input propositions */
		Collection<Proposition> input_props = prop.getInputPropositions().values();
		for (Proposition p: input_props)
		{
			HeavensbeeProposition hbp = new HeavensbeeInputProposition(counter, p, propnet_state);
			hb_propositions.add(counter, hbp);
			hb_prop_map.put(p.getName(), counter);
			counter++;
		}

		/* Assign indices for other propositions */
		for (Proposition p: prop.getPropositions())
		{
			if (!baseprops.contains(p) && !input_props.contains(p))
			{
				HeavensbeeProposition hbp = new HeavensbeeProposition(counter, p, propnet_state);
				hb_propositions.add(counter, hbp);
				hb_prop_map.put(p.getName(), counter);
				counter++;
			}
		}

		assert(counter == num_props);

		/* Perform a depth first visitation of the propnet, recording the visited components */
		visited_components = new ArrayList<Component>();
		Component first_node = pn.getInitProposition();
		depth_first_visit(first_node, visited_components);

		/* Do a reverse post-ordering of the connectives; */
		List <Component> rev_postorder = new ArrayList<Component>();
		for (int ii = visited_components.size() - 1; ii >= 0; ii--)
		{
			Component pc = visited_components.get(ii);
			if ((pc instanceof Proposition) && !rev_postorder.contains(pc))
			{
				rev_postorder.add(pc);
			}
		}

		for (Component c: rev_postorder)
		{
			GamerLogger.emitToConsole("RPO " + c + "\n");
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

	public void markbases(MachineState state)
	{


	}
}
