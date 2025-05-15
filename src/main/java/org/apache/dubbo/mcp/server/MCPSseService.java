package org.apache.dubbo.mcp.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpAsyncServer;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.config.annotation.DubboService;
import org.apache.dubbo.mcp.server.mcp.DubboMcpSseTransportProvider;

@DubboService
public class MCPSseService implements McpService {

    private static final Logger logger = LoggerFactory.getLogger(MCPSseService.class);

    private McpAsyncServer mcpAsyncServer;

    private static ObjectMapper JSON = new ObjectMapper();

    private final DubboMcpSseTransportProvider transportProvider = getTransportProvider();

    @Override
    public void get(StreamObserver<String> responseObserver) {
        transportProvider.handleRequest(responseObserver);
    }

    @Override
    public void post(StreamObserver<String> responseObserver) {
        transportProvider.handleRequest(responseObserver);
    }

    private DubboMcpSseTransportProvider getTransportProvider() {
        return new DubboMcpSseTransportProvider(JSON);
    }
}
