package org.apache.dubbo.mcp.server;


import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.remoting.http12.message.ServerSentEvent;
import org.apache.dubbo.remoting.http12.rest.Mapping;

@Mapping("")
public interface McpService{

    @Mapping("/mcp/sse")
    void get(StreamObserver<ServerSentEvent<String>> responseObserver);

    @Mapping("/mcp/message")
    void post();
}
