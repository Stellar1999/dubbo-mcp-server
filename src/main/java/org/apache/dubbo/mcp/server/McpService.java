package org.apache.dubbo.mcp.server;


import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.remoting.http12.message.ServerSentEvent;
import org.apache.dubbo.remoting.http12.rest.Mapping;
import org.apache.dubbo.remoting.http12.rest.Param;
import org.apache.dubbo.remoting.http12.rest.ParamType;

@Mapping("")
public interface McpService{

    @Mapping("/mcp/sse")
    void get(StreamObserver<ServerSentEvent<String>> responseObserver);

    @Mapping("/mcp/message")
    void post(StreamObserver<ServerSentEvent<String>> responseObserver);
}
