package yadi.samuraiai.foundation;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import yadi.samuraiai.foundation.async.AsyncEngine;
import yadi.samuraiai.foundation.thread.ThreadReportWriter;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class ThreadReportWriterTest {
    @TempDir Path directory;
    @Test void writesParseableJsonAndMarkdownWithoutStartingWork() throws Exception {
        try(var engine=new AsyncEngine(1,2,"SamuraiAI-ReportTest")) {
            new ThreadReportWriter().write(engine,directory);
            var json=JsonParser.parseString(Files.readString(directory.resolve("THREAD_STATUS.json"))).getAsJsonObject();
            assertEquals("READY",json.get("status").getAsString());assertEquals(0,json.get("deadlocks").getAsInt());
            assertTrue(Files.readString(directory.resolve("THREAD_AUDIT.md")).contains("Deadlocks: 0"));
        }
    }
}
