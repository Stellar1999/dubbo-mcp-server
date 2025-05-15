package org.apache.dubbo.mcp.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServer;
import jakarta.annotation.PostConstruct;
import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.config.annotation.DubboService;
import org.apache.dubbo.mcp.server.transport.DubboMcpSseTransportProvider;
import org.apache.dubbo.remoting.http12.message.ServerSentEvent;


@DubboService
public class MCPSseService implements McpService{

    private McpAsyncServer mcpAsyncServer;

    private static ObjectMapper JSON = new ObjectMapper();

    private final DubboMcpSseTransportProvider transportProvider = getTransportProvider();

    @Override
    public void get(StreamObserver<ServerSentEvent<String>> responseObserver) {
        transportProvider.handleRequest(responseObserver);
    }

    @Override
    public void post(StreamObserver<ServerSentEvent<String>> responseObserver) {
        transportProvider.handleRequest(responseObserver);
    }

    private DubboMcpSseTransportProvider getTransportProvider(){
        return new DubboMcpSseTransportProvider(JSON);
    }

    @PostConstruct
    public void init(){
        mcpAsyncServer = McpServer.async(transportProvider).build();
    }
}
