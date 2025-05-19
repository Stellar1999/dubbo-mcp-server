package org.apache.dubbo.mcp.server.sdk.registry;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.spec.McpSchema;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.deploy.ApplicationDeployListener;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.threadpool.manager.FrameworkExecutorRepository;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.bootstrap.builders.InternalServiceConfigBuilder;
import org.apache.dubbo.mcp.server.sdk.MCPSseService;
import org.apache.dubbo.mcp.server.sdk.McpConstant;
import org.apache.dubbo.mcp.server.sdk.McpService;
import org.apache.dubbo.mcp.server.sdk.generic.DubboMcpGenericCaller;
import org.apache.dubbo.mcp.server.sdk.transport.DubboMcpSseTransportProvider;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ProviderModel;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.DefaultOpenAPIService;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ExecutorService;

import static org.apache.dubbo.metadata.util.MetadataServiceVersionUtils.V1;

public class McpApplicationDeployListener implements ApplicationDeployListener {
    private static final Logger logger = LoggerFactory.getLogger(McpApplicationDeployListener.class);
    private DubboServiceToolRegistry toolRegistry;
    private boolean mcpEnable = true;

    private volatile ServiceConfig<McpService> serviceConfig;

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
        if (mcpEnable) {
            logger.debug("McpApplicationDeployListener: MCP service is enabled.");
        } else {
            logger.debug("McpApplicationDeployListener: MCP service is disabled. Skipping initialization.");
            return;
        }
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
            exportMcpService(applicationModel);
            logger.info("MCP service initialization complete. Registered " + toolRegistry.getRegisteredToolCount() + " tools.");
        } catch (Exception e) {
            logger.error("MCP service initialization failed: " + e.getMessage(), e);
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

    private void exportMcpService(ApplicationModel applicationModel) {
        MCPSseService mcpSseService = applicationModel.getBeanFactory().getOrRegisterBean(MCPSseService.class);

        ExecutorService internalServiceExecutor = applicationModel
                .getFrameworkModel()
                .getBeanFactory()
                .getBean(FrameworkExecutorRepository.class)
                .getInternalServiceExecutor();

        this.serviceConfig = InternalServiceConfigBuilder.<McpService>newBuilder(applicationModel)
                .interfaceClass(McpService.class)
                .protocol(CommonConstants.TRIPLE, McpConstant.MCP_SERVICE_PROTOCOL)
                .port(getRegisterPort(), McpConstant.MCP_SERVICE_PORT)
                .registryId("internal-mcp-registry")
                .executor(internalServiceExecutor)
                .ref(mcpSseService)
                .version(V1)
                .build();
        serviceConfig.export();
        logger.info("The MCP service exports urls : " + serviceConfig.getExportedUrls());
    }

    // TODO:Should be user config
    private Integer getRegisterPort(){
        ApplicationModel applicationModel = ApplicationModel.defaultModel();
        Collection<ProtocolConfig> protocolConfigs = applicationModel.getApplicationConfigManager().getProtocols();
        if (CollectionUtils.isNotEmpty(protocolConfigs)) {
            for (ProtocolConfig protocolConfig : protocolConfigs) {
                if ("tri".equals(protocolConfig.getName())){
                    return protocolConfig.getPort();
                }
            }
        }
        return NetUtils.getAvailablePort();
    }
}