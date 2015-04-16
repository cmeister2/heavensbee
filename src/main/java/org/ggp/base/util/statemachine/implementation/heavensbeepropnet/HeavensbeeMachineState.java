package org.ggp.base.util.statemachine.implementation.heavensbeepropnet;

import java.util.BitSet;
import java.util.Map;

import org.ggp.base.util.logging.GamerLogger;
import org.ggp.base.util.propnet.architecture.components.Proposition;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Role;

public class HeavensbeeMachineState extends MachineState {
	private BitSet bs;
	public boolean is_terminal;
	public Map<Role, Proposition> rolemap;
	private BitSet cache_current_state;
	private BitSet cache_next_state;
	private boolean cache_active;

	public HeavensbeeMachineState(BitSet b) {
		bs = (BitSet) b.clone();
//		is_terminal = qterminal;
//		rolemap = qrolemap;
		cache_active = false;
	}

	@Override
	public String toString() {
		return "MachineState[" + bs + "]";
	}

	@Override
	public int hashCode()
    {
        return bs.hashCode();
    }

	public BitSet get_bs()
	{
		return bs;
	}

	@Override
	public MachineState clone() {
//		if (is_terminal)
//		{
//			return new HeavensbeeMachineState(bs, is_terminal, new HashMap<Role, Proposition>(rolemap));
//		}
//		else
//		{
//			return new HeavensbeeMachineState(bs, is_terminal, null);
//		}
		return new HeavensbeeMachineState(bs);
	}

    @Override
	public boolean equals(Object o)
    {
        if ((o != null) && (o instanceof HeavensbeeMachineState))
        {
        	HeavensbeeMachineState state = (HeavensbeeMachineState) o;
            return state.get_bs().equals(get_bs());
        }

        return false;
    }

    public BitSet get_current_cache()
    {
    	return cache_current_state;
    }

    public BitSet get_next_cache()
    {
    	return cache_next_state;
    }

    public boolean is_cached()
    {
    	return cache_active;
    }

    public void check_cache(BitSet current_state, BitSet next_state)
    {
		//GamerLogger.emitToConsole("Reference current set " + current_state + "\n");
		//GamerLogger.emitToConsole("Cache set " + cache_current_state + "\n");

		if (!current_state.equals(cache_current_state))
		{
			GamerLogger.emitToConsole("!! Current states do not match \n");
		}
		//GamerLogger.emitToConsole("Reference next set " + next_state + "\n");
		//GamerLogger.emitToConsole("Cache set " + cache_next_state + "\n");

		if (!next_state.equals(cache_next_state))
		{
			GamerLogger.emitToConsole("!! Next states do not match \n");
		}

    }

    /**
     * This function is called by the propnet to cache the BitSet after
     * forward propping.
     */
	public void set_cache(BitSet current_state, BitSet next_state) {
		// TODO Auto-generated method stub
    	cache_active = true;
    	cache_current_state = (BitSet) current_state.clone();
    	cache_next_state = (BitSet) next_state.clone();
	}
}
