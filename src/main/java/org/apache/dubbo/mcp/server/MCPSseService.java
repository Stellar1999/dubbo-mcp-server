package org.apache.dubbo.mcp.server;

import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServer;
import jakarta.annotation.PostConstruct;
import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.config.annotation.DubboService;
import org.apache.dubbo.mcp.server.mcp.DubboMcpSseTransportProvider;


@DubboService
public class MCPSseService implements McpService{

    private McpAsyncServer mcpAsyncServer;

    private final DubboMcpSseTransportProvider transportProvider = getTransportProvider();

    @Override
    public void get(StreamObserver<String> responseObserver) {
        transportProvider.handleRequest(responseObserver);
    }

    @Override
    public void post() {

    }

    private DubboMcpSseTransportProvider getTransportProvider(){
        return new DubboMcpSseTransportProvider();
    }

    @PostConstruct
    public void init(){
        mcpAsyncServer = McpServer.async(transportProvider).build();
    }
}
