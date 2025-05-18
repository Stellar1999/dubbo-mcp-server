package org.apache.dubbo.mcp.server.demo.demo;

import org.apache.dubbo.config.annotation.DubboService;

@DubboService
public class HelloServiceImpl implements HelloService {
    @Override
    public String sayHello(String name) {
        System.out.println("HelloServiceImpl.sayHello called with: " + name);
        if (name == null || name.trim().isEmpty()) {
            return "Hello, guest!";
        }
        return "Hello, " + name + "!";
    }

    @Override
    public ComplexResponse greetComplex(ComplexRequest request) {
        System.out.println("HelloServiceImpl.greetComplex called with: " + request);
        if (request == null) {
            return new ComplexResponse("Error: Request was null", false, 400);
        }
        String message = "Received: " + request.getGreeting() +
                         ". Count: " + request.getCount() +
                         ". Active: " + request.isActive() +
                         ". Detail: " + (request.getNestedDetail() != null ? request.getNestedDetail().getDetailInfo() : "N/A") +
                         ". Tags: " + (request.getTags() != null ? String.join(", ", request.getTags()) : "None") +
                         ". Attributes: " + (request.getAttributes() != null ? request.getAttributes().toString() : "None");
        return new ComplexResponse(message, true, 200);
    }
} 