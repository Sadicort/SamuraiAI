package yadi.samuraiai.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import yadi.samuraiai.logging.SamuraiLogger;

public final class SamuraiForgeConfig {
    public static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.ConfigValue<String> ollamaHost;
    private static final ForgeConfigSpec.ConfigValue<String> ollamaModel;
    private static final ForgeConfigSpec.ConfigValue<Integer> connectTimeoutSeconds;
    private static final ForgeConfigSpec.ConfigValue<Integer> requestTimeoutSeconds;
    private static final ForgeConfigSpec.ConfigValue<Integer> maxConcurrentRequests;
    private static final ForgeConfigSpec.ConfigValue<Integer> aiTimeoutSeconds;
    private static final ForgeConfigSpec.ConfigValue<Integer> queueSize;
    private static final ForgeConfigSpec.ConfigValue<Integer> queueWaitMillis;
    private static final ForgeConfigSpec.ConfigValue<Integer> dialogueQueueSize;
    private static final ForgeConfigSpec.ConfigValue<Integer> maxMessageLength;
    private static final ForgeConfigSpec.ConfigValue<Integer> memoryMessages;
    private static final ForgeConfigSpec.ConfigValue<Integer> responseLength;
    private static final ForgeConfigSpec.ConfigValue<Integer> promptLimit;
    private static final ForgeConfigSpec.ConfigValue<Integer> playerCooldownMillis;
    private static final ForgeConfigSpec.ConfigValue<Integer> npcCooldownMillis;
    private static final ForgeConfigSpec.ConfigValue<Integer> conversationCooldownMillis;
    private static final ForgeConfigSpec.ConfigValue<Integer> maxActiveNpcs;
    private static final ForgeConfigSpec.ConfigValue<Integer> brainTickInterval;
    private static final ForgeConfigSpec.ConfigValue<Double> perceptionRadius;
    private static final ForgeConfigSpec.ConfigValue<Double> chatRadius;
    private static final ForgeConfigSpec.ConfigValue<Boolean> answerPublicChat;
    private static final ForgeConfigSpec.ConfigValue<Boolean> emotionDecay;
    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        ollamaHost = b.define("ollama.host", SamuraiSettings.defaults().ollamaHost());
        ollamaModel = b.define("ollama.model", SamuraiSettings.defaults().ollamaModel());
        connectTimeoutSeconds = b.defineInRange("ollama.connectTimeoutSeconds", SamuraiSettings.defaults().connectTimeoutSeconds(), 1, 120);
        requestTimeoutSeconds = b.defineInRange("ollama.requestTimeoutSeconds", SamuraiSettings.defaults().requestTimeoutSeconds(), 1, 600);
        maxConcurrentRequests = b.defineInRange("ai.maxConcurrentRequests", SamuraiSettings.defaults().maxConcurrentRequests(), 1, 64);
        aiTimeoutSeconds = b.defineInRange("ai.requestTimeoutSeconds", SamuraiSettings.defaults().aiTimeoutSeconds(), 1, 600);
        queueSize = b.defineInRange("ai.queueSize", SamuraiSettings.defaults().queueSize(), 1, 10000);
        queueWaitMillis = b.defineInRange("ai.queueWaitMillis", SamuraiSettings.defaults().queueWaitMillis(), 1, 600000);
        dialogueQueueSize = b.defineInRange("ai.dialogueQueueSize", SamuraiSettings.defaults().dialogueQueueSize(), 1, 256);
        maxMessageLength = b.defineInRange("ai.maxPlayerMessageLength", SamuraiSettings.defaults().maxMessageLength(), 16, 8000);
        memoryMessages = b.defineInRange("ai.conversationMemoryMessages", SamuraiSettings.defaults().memoryMessages(), 2, 200);
        responseLength = b.defineInRange("ai.responseLength", SamuraiSettings.defaults().responseLength(), 32, 2000);
        promptLimit = b.defineInRange("ai.promptCharacters", SamuraiSettings.defaults().promptLimit(), 1000, 100000);
        playerCooldownMillis = b.defineInRange("ai.perPlayerCooldownMillis", SamuraiSettings.defaults().playerCooldownMillis(), 0, 60000);
        npcCooldownMillis = b.defineInRange("ai.perNpcCooldownMillis", SamuraiSettings.defaults().npcCooldownMillis(), 0, 60000);
        conversationCooldownMillis = b.defineInRange("ai.perConversationCooldownMillis", SamuraiSettings.defaults().conversationCooldownMillis(), 0, 60000);
        maxActiveNpcs = b.defineInRange("npc.maxActive", SamuraiSettings.defaults().maxActiveNpcs(), 1, 1000);
        brainTickInterval = b.defineInRange("npc.brainTickIntervalTicks", SamuraiSettings.defaults().brainTickInterval(), 1, 1200);
        perceptionRadius = b.defineInRange("npc.perceptionRadius", SamuraiSettings.defaults().perceptionRadius(), 1D, 256D);
        chatRadius = b.defineInRange("npc.chatRadius", SamuraiSettings.defaults().chatRadius(), 1D, 256D);
        answerPublicChat = b.define("npc.answerPublicChat", SamuraiSettings.defaults().answerPublicChat());
        emotionDecay = b.define("npc.emotionDecay", SamuraiSettings.defaults().emotionDecay());
        SPEC = b.build();
    }
    private SamuraiForgeConfig() {}
    public static void onLoad(ModConfigEvent.Loading event) { if (event.getConfig().getSpec() == SPEC) pull(); }
    public static void onReload(ModConfigEvent.Reloading event) { if (event.getConfig().getSpec() == SPEC) pull(); }
    private static void pull() {
        SamuraiSettings.apply(new SamuraiSettings.Values(ollamaHost.get(),
                ollamaModel.get(),
                connectTimeoutSeconds.get(),
                requestTimeoutSeconds.get(),
                maxConcurrentRequests.get(),
                aiTimeoutSeconds.get(),
                queueSize.get(),
                queueWaitMillis.get(),
                dialogueQueueSize.get(),
                maxMessageLength.get(),
                memoryMessages.get(),
                responseLength.get(),
                promptLimit.get(),
                playerCooldownMillis.get(),
                npcCooldownMillis.get(),
                conversationCooldownMillis.get(),
                maxActiveNpcs.get(),
                brainTickInterval.get(),
                perceptionRadius.get(),
                chatRadius.get(),
                answerPublicChat.get(),
                emotionDecay.get()));
        SamuraiLogger.CONFIG.info("Configuration snapshot published");
        yadi.samuraiai.ai.AIRequestQueue.getInstance().reconfigure();
    }
}
