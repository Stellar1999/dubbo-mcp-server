package org.apache.dubbo.mcp.server.sdk.transport;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.*;
import io.netty.util.internal.StringUtil;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.common.utils.IOUtils;
import org.apache.dubbo.remoting.http12.HttpMethods;
import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.remoting.http12.HttpResponse;
import org.apache.dubbo.remoting.http12.HttpStatus;
import org.apache.dubbo.remoting.http12.message.ServerSentEvent;
import org.apache.dubbo.rpc.RpcContext;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

    public void handleRequest(StreamObserver<ServerSentEvent<String>> responseObserver) {
        // Handle the request and return the response
        HttpRequest request = RpcContext.getServiceContext().getRequest(HttpRequest.class);
        if (HttpMethods.isGet(request.method())) {
            handleSseConnection(responseObserver);
        }
        if (HttpMethods.isPost(request.method())) {
            handleMessage();
        }
        return;
    }

    public void handleMessage() {
        HttpRequest request = RpcContext.getServiceContext().getRequest(HttpRequest.class);
        String sessionId = request.parameter("sessionId");
        HttpResponse response = RpcContext.getServiceContext().getResponse(HttpResponse.class);
        if (StringUtil.isNullOrEmpty(sessionId)) {
            response.setStatus(HttpStatus.BAD_REQUEST.getCode());
            response.setBody(new McpError("Session ID missing in message endpoint"));
            return;
        }

        McpServerSession session = sessions.get(sessionId);
        if (session == null) {
            response.setStatus(HttpStatus.NOT_FOUND.getCode());
            response.setBody(new McpError("Unknown sessionId: " + sessionId));
            return;
        }
        try {
            McpSchema.JSONRPCMessage message = McpSchema.deserializeJsonRpcMessage(objectMapper, IOUtils.read(request.inputStream(), String.valueOf(StandardCharsets.UTF_8)));
            session.handle(message).block();
            response.setStatus(HttpStatus.OK.getCode());
        } catch (IOException e) {
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.getCode());
            response.setBody(new McpError("Invalid message format"));
            return;
        }
    }

    private void handleSseConnection(StreamObserver<ServerSentEvent<String>> responseObserver){
        // Handle the SSE connection
        // This is where you would set up the SSE stream and send events to the client
        DubboMcpSessionTransport dubboMcpSessionTransport = new DubboMcpSessionTransport(responseObserver, objectMapper);
        McpServerSession mcpServerSession = sessionFactory.create(dubboMcpSessionTransport);
        sessions.put(mcpServerSession.getId(), mcpServerSession);
        sendEvent(responseObserver,
                ENDPOINT_EVENT_TYPE,  "/mcp/message" + "?sessionId=" + mcpServerSession.getId());
    }

    private void sendEvent(StreamObserver<ServerSentEvent<String>> responseObserver, String eventType, String data) {
        responseObserver.onNext(ServerSentEvent.<String>builder().event(eventType).data(data).build());
    }

    private class DubboMcpSessionTransport implements McpServerTransport {

        private final ObjectMapper JSON;

        private final StreamObserver<ServerSentEvent<String>> responseObserver;

        public DubboMcpSessionTransport(StreamObserver<ServerSentEvent<String>> responseObserver, ObjectMapper objectMapper) {
            this.responseObserver = responseObserver;
            this.JSON = objectMapper;
        }

        @Override
        public void close() {
            responseObserver.onCompleted();
        }

        @Override
        public Mono<Void> closeGracefully() {
            return Mono.fromRunnable(responseObserver::onCompleted);
        }

        @Override
        public Mono<Void> sendMessage(McpSchema.JSONRPCMessage message) {
            return Mono.fromRunnable(() -> {
                try {
                    String jsonText = JSON.writeValueAsString(message);
                    responseObserver.onNext(ServerSentEvent.<String>builder().event(MESSAGE_EVENT_TYPE).data(jsonText).build());
                }
                catch (Exception e) {
                    responseObserver.onError(new McpError(e));
                }
            });
        }

        @Override
        public <T> T unmarshalFrom(Object data, TypeReference<T> typeRef) {
            return JSON.convertValue(data, typeRef);
        }
    }


}
