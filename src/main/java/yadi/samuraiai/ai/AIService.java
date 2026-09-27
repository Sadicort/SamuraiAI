package yadi.samuraiai.ai;

import yadi.samuraiai.context.AIContext;

import java.util.concurrent.CompletableFuture;

/**
 * A generative reasoning provider (Ollama today, possibly others later).
 * Never called directly by the Brain or by commands — always through
 * {@link AIRequestQueue}, which bounds concurrency and enforces a timeout
 * so a slow model can never block Minecraft's main thread or the caller.
 */
public interface AIService {

    CompletableFuture<AIResponse> generate(AIContext context);

}
