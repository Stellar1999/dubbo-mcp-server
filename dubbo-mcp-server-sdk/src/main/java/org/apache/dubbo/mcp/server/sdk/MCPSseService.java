package org.apache.dubbo.mcp.server.sdk;

import org.apache.dubbo.common.resource.Disposable;
import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.mcp.server.sdk.registry.McpApplicationDeployListener;
import org.apache.dubbo.mcp.server.sdk.transport.DubboMcpSseTransportProvider;
import org.apache.dubbo.remoting.http12.message.ServerSentEvent;

public class MCPSseService implements McpService,Disposable {

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

    @Override
    public void destroy() {

    }
}
