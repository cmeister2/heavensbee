package org.ggp.base.util.statemachine.exceptions;

import org.ggp.base.util.statemachine.MachineState;

@SuppressWarnings("serial")
public final class TimeToGetMovingException extends Exception
{

	private final MachineState state;

	public TimeToGetMovingException(MachineState state)
	{
		this.state = state;
	}

	public MachineState getState()
	{
		return state;
	}

	@Override
	public String toString()
	{
		return "Timed out for " + state;
	}

}
