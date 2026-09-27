package yadi.samuraiai.ai.relationship.engine;

import yadi.samuraiai.ai.relationship.model.Dimension;

/** Fear of a person: rises fast with threats and harm, fades quickly without them, and is damped by courage (a personality rule). */
public final class FearEngine extends DimensionEngine {
    public FearEngine() { super(Dimension.FEAR); }
}
