package org.apache.dubbo.mcp.server.registry;

import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ProviderModel;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.DefaultOpenAPIService;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

public class DubboServiceToolRegistry {

    private static final Logger logger = LoggerFactory.getLogger(DubboServiceToolRegistry.class);

    private final McpAsyncServer mcpServer;
    private final DubboOpenApiToolConverter toolConverter;
    private final Map<String, McpServerFeatures.AsyncToolSpecification> registeredTools = new ConcurrentHashMap<>();

    public DubboServiceToolRegistry(McpAsyncServer mcpServer) {
        this.mcpServer = mcpServer;
        this.toolConverter = new DubboOpenApiToolConverter(ApplicationModel.defaultModel().getBean(DefaultOpenAPIService.class));
    }

    public void registerService(ProviderModel providerModel) {
        ServiceDescriptor serviceDescriptor = providerModel.getServiceModel();
        List<URL> statedURLs = providerModel.getServiceUrls();
        
        if (statedURLs == null || statedURLs.isEmpty()) {
            logger.warn("No URLs found for service: " + serviceDescriptor.getInterfaceName());
            return;
        }

        try {
            // 使用第一个URL作为服务URL
            URL url = statedURLs.get(0);
            
            // 转换服务为工具
            Map<String, McpSchema.Tool> tools = toolConverter.convertToTools(serviceDescriptor, url);

            if (tools.isEmpty()) {
                logger.info("No tools found for service: " + serviceDescriptor.getInterfaceName());
                return;
            }

            // 注册每个工具
            for (Map.Entry<String, McpSchema.Tool> entry : tools.entrySet()) {
                String toolId = entry.getValue().name(); // 直接用 tool 的 name 字段
                McpSchema.Tool tool = entry.getValue();
            
                // 注册前判断
                if (registeredTools.containsKey(toolId)) {
                    logger.warn("Tool with name '" + toolId + "' 已注册，跳过。");
                    continue;
                }
            
                try {
                    McpServerFeatures.AsyncToolSpecification toolSpec = createToolSpecification(tool);
                    mcpServer.addTool(toolSpec).block();
                    registeredTools.put(toolId, toolSpec);
                    logger.info("Registered MCP tool: " + toolId);
                } catch (Exception e) {
                    logger.error("Failed to register MCP tool: " + toolId, e);
                }
            }

        } catch (Exception e) {
            logger.error("Failed to register service as MCP tools: " + serviceDescriptor.getInterfaceName(), e);
        }
    }

    private McpServerFeatures.AsyncToolSpecification createToolSpecification(McpSchema.Tool tool) {
        // TODO: 这里需要实现真正的工具调用逻辑，目前只返回一个空结果
        BiFunction<McpAsyncServerExchange, Map<String, Object>, Mono<McpSchema.CallToolResult>> callFunction =
                (exchange, args) -> {
                    // 空实现，直接返回空结果
                    return Mono.just(new McpSchema.CallToolResult("工具执行功能尚未实现", false));
                };

        return new McpServerFeatures.AsyncToolSpecification(tool, callFunction);
    }

    public void clearRegistry() {
        for (String toolId : registeredTools.keySet()) {
            try {
                mcpServer.removeTool(toolId).block();
                logger.info("Unregistered MCP tool: " + toolId);
            } catch (Exception e) {
                logger.error("Failed to unregister MCP tool: " + toolId, e);
            }
        }
        registeredTools.clear();
    }

    public int getRegisteredToolCount() {
        return registeredTools.size();
    }
}