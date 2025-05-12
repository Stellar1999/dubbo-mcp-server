package org.apache.dubbo.mcp.server;

import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.config.annotation.DubboService;
import org.apache.dubbo.mcp.server.mcp.DubboMcpSseTransportProvider;


@DubboService
public class MCPSseService implements McpService{

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
}
