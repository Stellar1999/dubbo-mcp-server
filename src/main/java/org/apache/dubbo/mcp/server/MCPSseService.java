package org.apache.dubbo.mcp.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpAsyncServer;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.config.annotation.DubboService;
import org.apache.dubbo.mcp.server.registry.McpApplicationDeployListener;
import org.apache.dubbo.mcp.server.transport.DubboMcpSseTransportProvider;
import org.apache.dubbo.remoting.http12.message.ServerSentEvent;

@DubboService
public class MCPSseService implements McpService {

    private static ObjectMapper JSON = new ObjectMapper();

    private DubboMcpSseTransportProvider transportProvider = getTransportProvider();

    @Override
    public void get(StreamObserver<ServerSentEvent<String>> responseObserver) {
        if (transportProvider == null) {
            transportProvider = getTransportProvider();
        }
        transportProvider.handleRequest(responseObserver);
    }

    @Override
    public void post() {
        if (transportProvider == null) {
            transportProvider = getTransportProvider();
        }
        transportProvider.handleRequest(null);
    }

    private DubboMcpSseTransportProvider getTransportProvider() {
        return McpApplicationDeployListener.getDubboMcpSseTransportProvider();
    }
}
