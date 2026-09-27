package yadi.samuraiai.ai.knowledge.model;

public enum LearnMethod { OBSERVATION, EXPERIENCE, CONVERSATION, TEACHING, RUMOR, DEDUCTION, CULTURE, PUBLIC_EVENT, ADMIN;
    /** Whether the NPC saw it happen itself (or was there when it became known to all). */
    public boolean direct() { return this == OBSERVATION || this == EXPERIENCE || this == PUBLIC_EVENT || this == ADMIN; }
}
