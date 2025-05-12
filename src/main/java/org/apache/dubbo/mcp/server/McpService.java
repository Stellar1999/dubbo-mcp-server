package org.apache.dubbo.mcp.server;


import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.remoting.http12.rest.Mapping;
public interface McpService{

    @Mapping("/mcp/sse")
    void get(StreamObserver<String> responseObserver);

    void post();
}
