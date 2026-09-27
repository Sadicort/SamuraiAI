package yadi.samuraiai.ai.navigation.chunks;

import java.util.List;

/**
 * Chunks crossed by the straight line from start to goal. {@code firstUnloadedIndex} is -1 when the whole
 * corridor is loaded; otherwise the walker can only plan up to the chunk before it.
 */
public record ChunkRoute(List<long[]> chunks, int firstUnloadedIndex) {
    public boolean fullyLoaded() { return firstUnloadedIndex < 0; }
}
