package yadi.samuraiai.ai;

import yadi.samuraiai.config.SamuraiSettings;
import yadi.samuraiai.context.AIContext;
import yadi.samuraiai.ollama.*;
import yadi.samuraiai.ollama.model.*;
import yadi.samuraiai.prompt.PromptBuilder;
import java.util.concurrent.CompletableFuture;

public final class OllamaAIService implements AIService {
    private final OllamaClient client = new OllamaClient();
    private final PromptBuilder prompts = new PromptBuilder();
    @Override public CompletableFuture<AIResponse> generate(AIContext context) {
        OllamaRequest request = new OllamaRequest(OllamaConfig.model());
        request.setOptions(OllamaOptions.forDialogue());
        String system = prompts.build(context);
        int budget = SamuraiSettings.promptLimit();
        // Preserve current user turn and system constraints; retain the newest history fitting the remainder.
        String user = context.getPlayerMessage();
        if (user.length() >= budget / 2) user = user.substring(0, budget / 2);
        system = system.substring(0, Math.min(system.length(), budget - user.length()));
        request.addSystemMessage(system);
        int remaining = budget - system.length() - user.length();
        var selected = new java.util.ArrayDeque<ChatMessage>();
        if (context.getMemory() != null) {
            var history = context.getMemory().snapshot();
            for (int i = history.size()-1; i >= 0; i--) {
                ChatMessage message = history.get(i);
                if (message.getContent().length() > remaining) break;
                selected.addFirst(message); remaining -= message.getContent().length();
            }
            while (!selected.isEmpty() && selected.peekFirst().getRoleAsEnum() == ChatRole.ASSISTANT) selected.removeFirst();
        }
        request.addMessages(selected); request.addUserMessage(user);
        return CancellableFutures.map(client.chatAsync(request), (response,error) -> {
            if (error != null) return AIResponse.failure(AIError.classify(error));
            String clean = clean(response.getContent());
            return clean.isBlank() ? AIResponse.failure(AIError.JSON) : AIResponse.ok(clean);
        });
    }
    static String clean(String raw) {
        String text = raw == null ? "" : raw.replaceAll("\\p{Cc}", " ").replaceAll("\\*[^*]*\\*", "").replaceAll("\\s{2,}", " ").trim();
        if (text.length() > 1 && text.startsWith("\"") && text.endsWith("\"")) text = text.substring(1,text.length()-1).trim();
        int limit = SamuraiSettings.responseLength();
        return text.length() > limit ? text.substring(0,limit-3).trim() + "..." : text;
    }
}
