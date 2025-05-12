package org.apache.dubbo.mcp.server;

import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServer;
import jakarta.annotation.PostConstruct;
import org.apache.dubbo.common.beanutil.JavaBeanSerializeUtil;
import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.config.annotation.DubboService;
import org.apache.dubbo.mcp.server.mcp.DubboMcpSseTransportProvider;


@DubboService
public class MCPSseService implements McpService{

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

    private DubboMcpSseTransportProvider getTransportProvider(){
        return new DubboMcpSseTransportProvider(JSON);
    }

    @PostConstruct
    public void init(){
        mcpAsyncServer = McpServer.async(transportProvider).build();
    }
}
