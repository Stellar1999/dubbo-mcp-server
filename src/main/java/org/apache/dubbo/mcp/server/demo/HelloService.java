package org.apache.dubbo.mcp.server.demo;


import org.apache.dubbo.remoting.http12.rest.Mapping;

@Mapping("")
public interface HelloService {
    @Mapping("/hello")
    String sayHello(String name);

    @Mapping("/greetComplex")
    ComplexResponse greetComplex(ComplexRequest request);
} 