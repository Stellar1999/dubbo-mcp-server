package org.apache.dubbo.mcp.server.registry;

import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.mcp.server.generic.DubboMcpGenericCaller;
import org.apache.dubbo.rpc.model.ProviderModel;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.MethodMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ParameterMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ServiceMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Operation;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

public class DubboServiceToolRegistry {

    private static final Logger logger = LoggerFactory.getLogger(DubboServiceToolRegistry.class);

    private final McpAsyncServer mcpServer;
    private final DubboOpenApiToolConverter toolConverter;
    private final DubboMcpGenericCaller genericCaller;
    private final Map<String, McpServerFeatures.AsyncToolSpecification> registeredTools = new ConcurrentHashMap<>();

    public DubboServiceToolRegistry(McpAsyncServer mcpServer,DubboOpenApiToolConverter toolConverter, DubboMcpGenericCaller genericCaller) {
        this.mcpServer = mcpServer;
        this.toolConverter = toolConverter;
        this.genericCaller = genericCaller;
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
            Map<String, McpSchema.Tool> tools = toolConverter.convertToTools(serviceDescriptor, url);

            if (tools.isEmpty()) {
                logger.info("No tools found for service: " + serviceDescriptor.getInterfaceName());
                return;
            }

            // 注册每个工具
            for (Map.Entry<String, McpSchema.Tool> entry : tools.entrySet()) {
                McpSchema.Tool tool = entry.getValue();
                String toolId = tool.name();
            
                // 注册前判断
                if (registeredTools.containsKey(toolId)) {
                    logger.warn("Tool with name '" + toolId + "' 已注册，跳过。");
                    continue;
                }
            
                try {
                    Operation operation = toolConverter.getOperationByToolName(toolId);
                    if (operation == null) {
                        logger.error("Could not find Operation metadata for tool: {}. Skipping registration.", tool);
                        continue;
                    }
                    McpServerFeatures.AsyncToolSpecification toolSpec = createToolSpecification(tool, operation, url);
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

    private McpServerFeatures.AsyncToolSpecification createToolSpecification(McpSchema.Tool mcpApiTool, Operation operation, URL serviceUrlContext) {
        final MethodMeta methodMeta = operation.getMeta();
        if (methodMeta == null) {
            throw new IllegalStateException("MethodMeta not found in Operation for tool: " + mcpApiTool.name());
        }
        final ServiceMeta serviceMeta = methodMeta.getServiceMeta();
        final String interfaceName = serviceMeta.getServiceInterface();
        final String methodName = methodMeta.getMethod().getName();
        final Class<?>[] parameterClasses = methodMeta.getMethod().getParameterTypes();

        final List<String> orderedJavaParameterNames = new ArrayList<>();
        if (methodMeta.getParameters() != null) {
            for (ParameterMeta javaParamMeta : methodMeta.getParameters()) {
                orderedJavaParameterNames.add(javaParamMeta.getName());
            }
        }

        final String group = serviceMeta.getUrl() != null ? serviceMeta.getUrl().getGroup() : (serviceUrlContext != null ? serviceUrlContext.getGroup() : null);
        final String version = serviceMeta.getUrl() != null ? serviceMeta.getUrl().getVersion() : (serviceUrlContext != null ? serviceUrlContext.getVersion() : null);

        BiFunction<McpAsyncServerExchange, Map<String, Object>, Mono<McpSchema.CallToolResult>> callFunction =
                (exchange, mcpProvidedParameters) -> {
                    try {
                        Object result = genericCaller.execute(
                                interfaceName,
                                methodName,
                                orderedJavaParameterNames,
                                parameterClasses,
                                mcpProvidedParameters,
                                group,
                                version
                        );
                        String resultJson = (result != null) ? result.toString() : "null"; // TODO: Proper JSON serialization
                        return Mono.just(new McpSchema.CallToolResult(resultJson, true));
                    } catch (Exception e) {
                        logger.error("Error executing tool {} (interface: {}, method: {}): {}",
                                mcpApiTool.name(), interfaceName, methodName, e.getMessage(), e);
                        return Mono.just(new McpSchema.CallToolResult("Tool execution failed: " + e.getMessage(), false));
                    }
                };
        return new McpServerFeatures.AsyncToolSpecification(mcpApiTool, callFunction);
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