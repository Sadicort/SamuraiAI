package yadi.samuraiai.ai.navigation.graph;

/** Receives the outgoing edges of a node without allocating a list per expansion. */
@FunctionalInterface
public interface EdgeSink {
    void accept(NavPos to, EdgeType type, double baseCost);
}
