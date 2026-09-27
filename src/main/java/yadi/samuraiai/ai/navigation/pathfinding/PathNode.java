package yadi.samuraiai.ai.navigation.pathfinding;

import yadi.samuraiai.ai.navigation.graph.EdgeType;
import yadi.samuraiai.ai.navigation.graph.NavPos;

/** One waypoint of a path and how the walker arrives at it from the previous one. */
public record PathNode(NavPos pos, EdgeType via) { }
