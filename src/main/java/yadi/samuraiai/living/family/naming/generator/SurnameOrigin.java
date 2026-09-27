package yadi.samuraiai.living.family.naming.generator;

/**
 * Where a generated surname came from, kept for inspection ({@code /samuraiai living family names}) and for choosing how a
 * surname is built. {@code CURATED} draws a hand-authored, always-valid name from the culture's pool (the common case).
 * {@code COMPOUND} builds one from the culture's prefix and suffix roots, validated before it is used.
 * {@code TOPONYMIC} derives one from a place name (a founding village, a region) — available to callers that have a place
 * name in hand (house naming, the debug generator); the automatic roll for a brand-new family does not use it, so a family's
 * surname does not silently change if the village around it is renamed. {@code ANCESTRAL}, {@code SYMBOLIC}, {@code HONORIFIC}
 * and {@code HISTORICAL} name how a surname could be explained even when {@code CURATED}/{@code COMPOUND} produced it — most
 * of the curated pool already reads as one of these; {@code GENERATED} is the honest label for a compound with no deeper
 * story yet.
 */
public enum SurnameOrigin { CURATED, COMPOUND, TOPONYMIC, OCCUPATIONAL, ANCESTRAL, SYMBOLIC, HONORIFIC, HISTORICAL, GENERATED }
