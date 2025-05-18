package org.apache.dubbo.mcp.server.sdk;


import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.remoting.http12.HttpMethods;
import org.apache.dubbo.remoting.http12.message.ServerSentEvent;
import org.apache.dubbo.remoting.http12.rest.Mapping;

@Mapping("")
public interface McpService{

    @Mapping(value = "/mcp/sse", method = HttpMethods.GET)
    void get(StreamObserver<ServerSentEvent<String>> responseObserver);

    @Mapping(value = "/mcp/message", method = HttpMethods.POST)
    void post();
}
