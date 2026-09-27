package yadi.samuraiai.personality;

public class NpcPersonality {

    private final String name;

    private final String description;

    public NpcPersonality(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

}