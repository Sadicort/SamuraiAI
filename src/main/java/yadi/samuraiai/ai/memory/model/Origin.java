package yadi.samuraiai.ai.memory.model;

import java.util.UUID;

/** Where an experience came from: the source system, the world event behind it and the trace that follows it through the engines. */
public record Origin(String source, String event, UUID traceId) {
    public Origin {
        source = source == null ? "" : source;
        event = event == null ? "" : event;
    }
}
