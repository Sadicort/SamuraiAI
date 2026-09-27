package yadi.samuraiai.living.family.model;

/**
 * Which kinship words to use for a person ("padre"/"madre" or the neutral "progenitor"). It affects wording only: succession,
 * inheritance and headship never depend on it, and an unspecified person gets the neutral words.
 */
public enum KinGender { MASCULINE, FEMININE, UNSPECIFIED }
