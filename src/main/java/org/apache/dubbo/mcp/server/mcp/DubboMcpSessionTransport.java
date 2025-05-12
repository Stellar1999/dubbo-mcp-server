package org.apache.dubbo.mcp.server.mcp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpServerTransport;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.stream.StreamObserver;
import reactor.core.publisher.Mono;


public class DubboMcpSessionTransport implements McpServerTransport {

    private Logger logger = LoggerFactory.getLogger(DubboMcpSessionTransport.class);

    private final ObjectMapper JSON;

    private StreamObserver responseObserver;

    public DubboMcpSessionTransport(StreamObserver responseObserver, ObjectMapper objectMapper) {
        this.responseObserver = responseObserver;
        this.JSON = objectMapper;
    }

    @Override
    public void close() {
        responseObserver.onCompleted();
    }

    @Override
    public Mono<Void> closeGracefully() {
        return Mono.fromRunnable(()->{responseObserver.onCompleted();});
    }

    @Override
    public Mono<Void> sendMessage(McpSchema.JSONRPCMessage message) {
        return Mono.fromRunnable(() -> {
            try {
                String jsonText = JSON.writeValueAsString(message);
                responseObserver.onNext(jsonText);
            }
            catch (Exception e) {
            }
        });
    }

    @Override
    public <T> T unmarshalFrom(Object data, TypeReference<T> typeRef) {
        return JSON.convertValue(data, typeRef);
    }
}
