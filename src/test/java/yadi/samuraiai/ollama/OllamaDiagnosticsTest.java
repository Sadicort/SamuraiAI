package yadi.samuraiai.ollama;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.nio.charset.StandardCharsets;

class OllamaDiagnosticsTest {
    HttpServer server;
    @AfterEach void stop() { if(server!=null) server.stop(0); }
    private OllamaDiagnostics.Result serve(int status,String body,String model) throws Exception {
        server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/api/tags",exchange->{ byte[] data=body.getBytes(StandardCharsets.UTF_8);exchange.sendResponseHeaders(status,data.length);exchange.getResponseBody().write(data);exchange.close();});
        server.start();
        return new OllamaDiagnostics().check(URI.create("http://127.0.0.1:"+server.getAddress().getPort()+"/api/tags"),model,Duration.ofSeconds(1)).get(2,TimeUnit.SECONDS);
    }
    @Test void availableModel() throws Exception { assertEquals(OllamaDiagnostics.Status.AVAILABLE,serve(200,"{\"models\":[{\"name\":\"llama3.2:latest\"}]}","llama3.2").status()); }
    @Test void missingModel() throws Exception { assertEquals(OllamaDiagnostics.Status.MODEL_NOT_FOUND,serve(200,"{\"models\":[]}","missing").status()); }
    @Test void invalidJson() throws Exception { assertEquals(OllamaDiagnostics.Status.JSON_ERROR,serve(200,"not-json","x").status()); }
    @Test void httpError() throws Exception { assertEquals(OllamaDiagnostics.Status.HTTP_ERROR,serve(503,"down","x").status()); }
    @Test void unavailableEndpoint() throws Exception {
        int port;try(var socket=new java.net.ServerSocket(0)){port=socket.getLocalPort();}
        assertEquals(OllamaDiagnostics.Status.UNAVAILABLE,new OllamaDiagnostics().check(
                URI.create("http://127.0.0.1:"+port+"/api/tags"),"x",Duration.ofMillis(200)).get(2,TimeUnit.SECONDS).status());
    }
}
