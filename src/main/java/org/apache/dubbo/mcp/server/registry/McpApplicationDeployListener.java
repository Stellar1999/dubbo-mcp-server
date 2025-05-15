package org.apache.dubbo.mcp.server.registry;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.spec.McpSchema;
import org.apache.dubbo.common.deploy.ApplicationDeployListener;
import org.apache.dubbo.mcp.server.transport.DubboMcpSseTransportProvider;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ProviderModel;

import java.util.Collection;
import java.util.logging.Logger;

public class McpApplicationDeployListener implements ApplicationDeployListener {
    private static final Logger logger = Logger.getLogger(McpApplicationDeployListener.class.getName());
    private DubboServiceToolRegistry toolRegistry;
    private static DubboMcpSseTransportProvider dubboMcpSseTransportProvider;

    @Override
    public void onInitialize(ApplicationModel scopeModel) {

    }

    @Override
    public void onStarting(ApplicationModel applicationModel) {
        // 这里不做任何事，因为服务还未暴露
    }

    public static DubboMcpSseTransportProvider getDubboMcpSseTransportProvider() {
        return dubboMcpSseTransportProvider;
    }

    @Override
    public void onStarted(ApplicationModel applicationModel) {
        // 应用完全启动后执行，此时所有服务已暴露完成
        try {
            // 1. 创建McpServer
            dubboMcpSseTransportProvider = new DubboMcpSseTransportProvider(new ObjectMapper());
            McpSchema.ServerCapabilities.ToolCapabilities toolCapabilities = new McpSchema.ServerCapabilities.ToolCapabilities(true);
            McpSchema.ServerCapabilities serverCapabilities = new McpSchema.ServerCapabilities(null, null, null, null, toolCapabilities);
            McpAsyncServer mcpAsyncServer = McpServer.async(dubboMcpSseTransportProvider)
                    .capabilities(serverCapabilities)
                    .build();

            // 2. 创建工具注册器
            toolRegistry = new DubboServiceToolRegistry(mcpAsyncServer);

            // 3. 获取所有已暴露服务并注册
            Collection<ProviderModel> providerModels = applicationModel.getApplicationServiceRepository().allProviderModels();
            for (ProviderModel pm : providerModels) {
                System.out.println("ProviderModel: " + pm.getServiceKey() + ", module: " + pm.getModuleModel().getDesc());
                toolRegistry.registerService(pm);
            }

            logger.info("MCP服务初始化完成，成功注册");
        } catch (Exception e) {
            logger.severe("MCP服务初始化失败：" + e.getMessage());
        }
    }

    @Override
    public void onStopping(ApplicationModel applicationModel) {
        // 应用停止前执行，可以在这里清理资源
        if (toolRegistry != null) {
            // 如果需要，执行清理逻辑
            logger.info("MCP服务停止中...");
        }
    }

    @Override
    public void onStopped(ApplicationModel applicationModel) {
        // 应用已停止
    }

    @Override
    public void onFailure(ApplicationModel applicationModel, Throwable cause) {
        logger.severe("应用启动失败：" + cause.getMessage());
    }
}