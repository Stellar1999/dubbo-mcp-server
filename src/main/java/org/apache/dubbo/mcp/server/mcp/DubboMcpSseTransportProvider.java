package org.apache.dubbo.mcp.server.mcp;

import io.modelcontextprotocol.spec.McpServerSession;
import io.modelcontextprotocol.spec.McpServerTransport;
import io.modelcontextprotocol.spec.McpServerTransportProvider;
import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.remoting.http12.HttpMethods;
import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.remoting.http12.HttpResponse;
import org.apache.dubbo.remoting.http12.HttpStatus;
import org.apache.dubbo.remoting.http12.message.MediaType;
import org.apache.dubbo.rpc.RpcContext;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DubboMcpSseTransportProvider implements McpServerTransportProvider {

    /**
     * Event type for JSON-RPC messages sent through the SSE connection.
     */
    public static final String MESSAGE_EVENT_TYPE = "message";

    /**
     * Event type for sending the message endpoint URI to clients.
     */
    public static final String ENDPOINT_EVENT_TYPE = "endpoint";

    private McpServerSession.Factory sessionFactory;

    ConcurrentHashMap<String, McpServerSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void setSessionFactory(McpServerSession.Factory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public Mono<Void> notifyClients(String method, Object params) {
        return null;
    }

    @Override
    public Mono<Void> closeGracefully() {
        return null;
    }

    public void handleRequest(StreamObserver<String> responseObserver) {
        // Handle the request and return the response
        HttpRequest request = RpcContext.getServiceContext().getRequest(HttpRequest.class);
        if (HttpMethods.isGet(request.method())){
            handleSseConnection(responseObserver);
        }
        return;
    }

    private void handleSseConnection(StreamObserver<String> responseObserver){
        // Handle the SSE connection
        // This is where you would set up the SSE stream and send events to the client
        DubboMcpSessionTransport dubboMcpSessionTransport = new DubboMcpSessionTransport();
        String sessionId = UUID.randomUUID().toString();
        sendEvent(responseObserver,
                ENDPOINT_EVENT_TYPE, "http://localhost:50052/org.apache.dubbo.mcp.server.McpService"+"/mcp/message"+"?sessionId="+sessionId);
    }

    private void sendEvent(StreamObserver<String> responseObserver, String eventType, String data){
        String body = "";
        body += "event: " + eventType + "\n";
        body += "data: " + data + "\n";
        responseObserver.onNext(body);
    }

    private McpServerSession createSession(McpServerTransport mcpServerTransport){
        return sessionFactory.create(mcpServerTransport);
    }
}
