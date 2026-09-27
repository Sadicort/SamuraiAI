package yadi.samuraiai.ai.knowledge.worldmemory;

import java.util.UUID;
import yadi.samuraiai.ai.cognition.model.EntityRef;

/** What the world remembers of one person: how heroic and how treacherous their public acts were. */
public final class Legend {
    private final EntityRef subject;
    private double heroism, treachery;
    private int acts;

    public Legend(EntityRef subject, double heroism, double treachery, int acts) { this.subject = subject; this.heroism = heroism; this.treachery = treachery; this.acts = acts; }

    public EntityRef subject() { return subject; }
    public UUID id() { return subject.id(); }
    public double heroism() { return heroism; }
    public double treachery() { return treachery; }
    public int acts() { return acts; }
    void add(double hero, double traitor) { heroism += hero; treachery += traitor; acts++; }
    public boolean hero() { return heroism > treachery * 1.5D && heroism >= 0.5D; }
    public boolean traitor() { return treachery > heroism * 1.5D && treachery >= 0.5D; }
}
