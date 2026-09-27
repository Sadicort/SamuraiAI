package yadi.samuraiai.living.calendar.events;

/** A harvest began and its yield was fixed by the season's weather ({@code verdict} GREAT, NORMAL or BAD). */
public record HarvestOutlookEvent(long minute, String cell, String crop, int year, double yieldFactor, String verdict) implements CalendarEngineEvent { }
