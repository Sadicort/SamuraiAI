package yadi.samuraiai.ai.relationship.engine;

import java.util.List;
import yadi.samuraiai.ai.cognition.model.PersonalityView;
import yadi.samuraiai.ai.relationship.model.SocialEvidence;

/** What bends a raw effect before it is applied: who the NPC is, how it feels right now (-1..+1) and the evidence itself. */
public record Modulation(PersonalityView personality, double mood, SocialEvidence evidence, List<TraitRule> rules) { }
