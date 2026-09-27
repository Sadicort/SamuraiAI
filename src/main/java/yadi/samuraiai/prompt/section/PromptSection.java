package yadi.samuraiai.prompt.section;

import yadi.samuraiai.context.AIContext;

/**
 * One labelled block of the system prompt. Sections are composed in order by
 * {@link yadi.samuraiai.prompt.PromptBuilder}; returning an empty string
 * means "nothing to say here" and the section is skipped entirely, so a
 * missing personality or an empty world never leaves a dangling header.
 */
public interface PromptSection {

    String build(AIContext context);

}
