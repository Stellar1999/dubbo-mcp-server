package org.apache.dubbo.mcp.server.mcp;

import com.fasterxml.jackson.core.type.TypeReference;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpServerTransport;
import reactor.core.publisher.Mono;

public class DubboMcpSessionTransport implements McpServerTransport {
    @Override
    public void close() {
        McpServerTransport.super.close();
    }

    @Override
    public Mono<Void> closeGracefully() {
        return null;
    }

    @Override
    public Mono<Void> sendMessage(McpSchema.JSONRPCMessage message) {
        return null;
    }

    @Override
    public <T> T unmarshalFrom(Object data, TypeReference<T> typeRef) {
        return null;
    }
}
