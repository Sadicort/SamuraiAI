package yadi.samuraiai.ai.scheduler.engine;

import yadi.samuraiai.ai.scheduler.personality.ResponseKind;
import yadi.samuraiai.ai.scheduler.routine.RoutineType;
import yadi.samuraiai.ai.scheduler.zone.Place;

/** What an NPC intends to do: a routine of its day, or a response to something that happened. Exactly one is set. */
public record Intent(RoutineType routine, ResponseKind response, Place place) {
    public Intent {
        if ((routine == null) == (response == null)) throw new IllegalArgumentException("an intent is either a routine or a response");
    }

    public static Intent of(RoutineType routine, Place place) { return new Intent(routine, null, place); }
    public static Intent respond(ResponseKind response, Place place) { return new Intent(null, response, place); }
    public String key() { return routine != null ? "R:" + routine : "S:" + response; }
    public String label() { return routine != null ? routine.name() : response.name(); }
    public boolean isResponse() { return response != null; }
}
