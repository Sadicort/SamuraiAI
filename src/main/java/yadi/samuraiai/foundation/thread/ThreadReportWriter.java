package yadi.samuraiai.foundation.thread;

import com.google.gson.GsonBuilder;
import yadi.samuraiai.foundation.async.*;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

public final class ThreadReportWriter {
    public void write(AsyncEngine engine, Path directory) throws IOException {
        Objects.requireNonNull(engine); Files.createDirectories(directory);
        long[] deadlocks = ManagementFactory.getThreadMXBean().findDeadlockedThreads();
        AsyncMetrics metrics = engine.metrics();
        String status = deadlocks != null && deadlocks.length > 0 ? "FAILED" : metrics.rejected() > 0 ? "WARNING" : "READY";
        List<Map<String,Object>> threads = Thread.getAllStackTraces().keySet().stream()
                .filter(thread -> thread.getName().startsWith("SamuraiAI-"))
                .sorted(Comparator.comparing(Thread::getName)).map(thread -> {
                    Map<String,Object> row=new LinkedHashMap<>();row.put("name",thread.getName());
                    row.put("state",thread.getState().name());row.put("daemon",thread.isDaemon());return row;
                }).toList();
        Map<String,Object> json=new LinkedHashMap<>();json.put("status",status);json.put("timestamp",Instant.now().toString());
        json.put("deadlocks",deadlocks==null?0:deadlocks.length);json.put("metrics",metrics);json.put("threads",threads);
        json.put("activeTasks",engine.activeTasks().stream().map(task -> Map.of("id",task.id().toString(),"module",task.module(),
                "owner",task.owner(),"priority",task.priority().name(),"state",task.state().name(),"elapsedMillis",task.elapsedMillis())).toList());
        String jsonText=new GsonBuilder().setPrettyPrinting().create().toJson(json);
        String markdown="# Foundation Thread Audit\n\nEstado: **"+status+"**\n\nDeadlocks: "+(deadlocks==null?0:deadlocks.length)+
                "\n\nTareas activas: "+engine.activeTasks().size()+"\n\nWorkers activos: "+metrics.activeThreads()+
                "\n\nEn cola: "+metrics.queued()+"\n\nCanceladas: "+metrics.cancelled()+"\n\nTimeouts: "+metrics.timedOut()+
                "\n\nRechazadas: "+metrics.rejected()+"\n";
        replace(directory.resolve("THREAD_STATUS.json"),jsonText);
        replace(directory.resolve("THREAD_AUDIT.md"),markdown);
    }
    private static void replace(Path target,String text) throws IOException {
        Path temp=Files.createTempFile(target.getParent(),target.getFileName().toString(),".tmp");
        try { Files.writeString(temp,text,StandardCharsets.UTF_8);Files.move(temp,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING); }
        finally { Files.deleteIfExists(temp); }
    }
}
