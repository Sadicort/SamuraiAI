package yadi.samuraiai.ai.navigation.graph;

public record NavEdge(NavPos from, NavPos to, EdgeType type, double cost) { }
