package yadi.samuraiai.ollama.model;

/**
 * Per-request generation knobs sent to Ollama.
 *
 * <p>{@code num_predict} is the important one for this mod: without a cap the
 * model happily writes several paragraphs, which an NPC then has to say in
 * Minecraft chat. Field names are snake_case because that is what Ollama's
 * API expects and Gson maps them verbatim.
 */
public class OllamaOptions {

    private double temperature;

    private double top_p;

    private int num_predict;

    public OllamaOptions(double temperature, double topP, int numPredict) {
        this.temperature = temperature;
        this.top_p = topP;
        this.num_predict = numPredict;
    }

    /**
     * Defaults tuned for in-character NPC dialogue: warm enough to not sound
     * robotic, short enough to fit in a chat line or two.
     */
    public static OllamaOptions forDialogue() {
        return new OllamaOptions(0.8D, 0.9D, 160);
    }

    public double getTemperature() {
        return temperature;
    }

    public double getTopP() {
        return top_p;
    }

    public int getNumPredict() {
        return num_predict;
    }
}
