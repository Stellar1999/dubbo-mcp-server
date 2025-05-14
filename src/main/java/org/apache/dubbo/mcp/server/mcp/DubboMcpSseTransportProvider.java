package org.apache.dubbo.mcp.server.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpServerSession;
import io.modelcontextprotocol.spec.McpServerTransportProvider;
import io.netty.util.internal.StringUtil;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.remoting.http12.HttpMethods;
import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.rpc.RpcContext;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public class DubboMcpSseTransportProvider implements McpServerTransportProvider {

    private static final Logger logger = LoggerFactory.getLogger(DubboMcpSseTransportProvider.class);

    /**
     * Event type for JSON-RPC messages sent through the SSE connection.
     */
    public static final String MESSAGE_EVENT_TYPE = "message";

    /**
     * Event type for sending the message endpoint URI to clients.
     */
    public static final String ENDPOINT_EVENT_TYPE = "endpoint";

    private McpServerSession.Factory sessionFactory;

    private final ObjectMapper objectMapper;

    ConcurrentHashMap<String, McpServerSession> sessions = new ConcurrentHashMap<>();

    public DubboMcpSseTransportProvider(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void setSessionFactory(McpServerSession.Factory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public Mono<Void> notifyClients(String method, Object params) {
        if (sessions.isEmpty()) {
            return Mono.empty();
        }
        return Flux.fromIterable(sessions.values())
                .flatMap(session -> session.sendNotification(method, params)
                        .doOnError(
                                e -> logger.error("Failed to send message to session {}: {}", session.getId(), e.getMessage()))
                        .onErrorComplete())
                .then();
    }

    @Override
    public Mono<Void> closeGracefully() {
        return  Flux.fromIterable(sessions.values()).flatMap(McpServerSession::closeGracefully).then();
    }

    public void handleRequest(StreamObserver<String> responseObserver) {
        // Handle the request and return the response
        HttpRequest request = RpcContext.getServiceContext().getRequest(HttpRequest.class);
        if (HttpMethods.isGet(request.method())) {
            handleSseConnection(responseObserver);
        }
        if (HttpMethods.isPost(request.method())) {
            handleMessage(responseObserver);
        }
        return;
    }

    public void handleMessage(StreamObserver<String> responseObserver) {
        HttpRequest request = RpcContext.getServiceContext().getRequest(HttpRequest.class);
        String sessionId = request.parameter("sessionId");
        if (StringUtil.isNullOrEmpty(sessionId)) {
            responseObserver.onError(new McpError("Session ID missing in message endpoint"));
            return;
        }

        McpServerSession session = sessions.get(sessionId);
        if (session == null) {
            responseObserver.onError(new McpError("Unknown sessionId: " + sessionId));
            return;
        }
        try {
            McpSchema.JSONRPCMessage message = McpSchema.deserializeJsonRpcMessage(objectMapper, request.accept());
            session.handle(message).flatMap(response -> {
                responseObserver.onNext(null);
                return Mono.empty();
            }).onErrorResume(error -> {
                responseObserver.onError(new McpError(error.getMessage()));
                return Mono.empty();
            });
        } catch (IOException e) {
            responseObserver.onError(new McpError("Invalid message format"));
            return;
        }
    }

    private void handleSseConnection(StreamObserver<String> responseObserver){
        // Handle the SSE connection
        // This is where you would set up the SSE stream and send events to the client
        DubboMcpSessionTransport dubboMcpSessionTransport = new DubboMcpSessionTransport(responseObserver, objectMapper);
        McpServerSession mcpServerSession = sessionFactory.create(dubboMcpSessionTransport);
        sessions.put(mcpServerSession.getId(), mcpServerSession);
        sendEvent(responseObserver,
                ENDPOINT_EVENT_TYPE, "http://localhost:50052/org.apache.dubbo.mcp.server.McpService" + "/mcp/message" + "?sessionId=" + mcpServerSession.getId());
    }

    private void sendEvent(StreamObserver<String> responseObserver, String eventType, String data) {
        String body = "";
        body += "event: " + eventType + "\n";
        body += "data: " + data + "\n";
        responseObserver.onNext(body);
    }


}
