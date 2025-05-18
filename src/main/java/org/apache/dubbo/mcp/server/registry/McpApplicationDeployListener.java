package org.apache.dubbo.mcp.server.registry;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.spec.McpSchema;
import org.apache.dubbo.common.deploy.ApplicationDeployListener;
import org.apache.dubbo.mcp.server.generic.DubboMcpGenericCaller;
import org.apache.dubbo.mcp.server.transport.DubboMcpSseTransportProvider;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ProviderModel;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.DefaultOpenAPIService;

import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

public class McpApplicationDeployListener implements ApplicationDeployListener {
    private static final Logger logger = Logger.getLogger(McpApplicationDeployListener.class.getName());
    private DubboServiceToolRegistry toolRegistry;
    private static DubboMcpSseTransportProvider dubboMcpSseTransportProvider;

    @Override
    public void onInitialize(ApplicationModel scopeModel) {}

    @Override
    public void onStarting(ApplicationModel applicationModel) {}

    public static DubboMcpSseTransportProvider getDubboMcpSseTransportProvider() {
        return dubboMcpSseTransportProvider;
    }

    @Override
    public void onStarted(ApplicationModel applicationModel) {
        try {
            logger.info("McpApplicationDeployListener: Application started. Initializing MCP server and tools...");

            dubboMcpSseTransportProvider = new DubboMcpSseTransportProvider(new ObjectMapper());
            McpSchema.ServerCapabilities.ToolCapabilities toolCapabilities = new McpSchema.ServerCapabilities.ToolCapabilities(true);
            McpSchema.ServerCapabilities serverCapabilities = new McpSchema.ServerCapabilities(null, null, null, null, toolCapabilities);
            
            McpAsyncServer mcpAsyncServer = McpServer.async(dubboMcpSseTransportProvider)
                    .capabilities(serverCapabilities)
                    .build();

            FrameworkModel frameworkModel = applicationModel.getFrameworkModel();
            DefaultOpenAPIService defaultOpenAPIService = new DefaultOpenAPIService(frameworkModel);

            DubboOpenApiToolConverter toolConverter = new DubboOpenApiToolConverter(defaultOpenAPIService);

            DubboMcpGenericCaller genericCaller = new DubboMcpGenericCaller(applicationModel);

            toolRegistry = new DubboServiceToolRegistry(mcpAsyncServer, toolConverter, genericCaller);

            Collection<ProviderModel> providerModels = applicationModel.getApplicationServiceRepository().allProviderModels();
            logger.info("Found " + providerModels.size() + " provider models. Starting tool registration...");
            for (ProviderModel pm : providerModels) {
                logger.info("Processing ProviderModel: " + pm.getServiceKey() + ", module: " + pm.getModuleModel().getDesc());
                toolRegistry.registerService(pm);
            }
            logger.info("MCP service initialization complete. Registered " + toolRegistry.getRegisteredToolCount() + " tools.");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "MCP service initialization failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void onStopping(ApplicationModel applicationModel) {
        if (toolRegistry != null) {
            logger.info("MCP service stopping. Clearing tool registry...");
            toolRegistry.clearRegistry();
        }
    }

    @Override
    public void onStopped(ApplicationModel applicationModel) {}

    @Override
    public void onFailure(ApplicationModel applicationModel, Throwable cause) {}
}