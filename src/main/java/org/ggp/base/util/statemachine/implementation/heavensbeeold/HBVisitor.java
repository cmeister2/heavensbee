package org.ggp.base.util.statemachine.implementation.heavensbeeold;

import java.util.HashSet;
import java.util.Set;

public class HBVisitor
{
    /** The inputs to the component. */
    private final Set<HBVisitor> inputs;
    /** The outputs of the component. */
    private final Set<HBVisitor> outputs;

    /**
     * Creates a new Component with no inputs or outputs.
     */
    public HBVisitor()
    {
        this.inputs = new HashSet<HBVisitor>();
        this.outputs = new HashSet<HBVisitor>();
    }
}
