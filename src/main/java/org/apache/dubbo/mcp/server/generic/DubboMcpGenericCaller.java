package org.apache.dubbo.mcp.server.generic;


import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.service.GenericService;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class DubboMcpGenericCaller {
    private static final Logger logger = LoggerFactory.getLogger(DubboMcpGenericCaller.class);

    private final ApplicationConfig applicationConfig;

    private final Map<String, GenericService> serviceCache = new ConcurrentHashMap<>();

    public DubboMcpGenericCaller(ApplicationModel applicationModel) {
        if (applicationModel == null) {
            logger.error("ApplicationModel cannot be null for DubboMcpGenericCaller.");
            throw new IllegalArgumentException("ApplicationModel cannot be null.");
        }
        this.applicationConfig = applicationModel.getCurrentConfig();
        if (this.applicationConfig == null) {
            String errMsg = "ApplicationConfig is null in the provided ApplicationModel. Application Name: " +
                           (applicationModel.getApplicationName() != null ? applicationModel.getApplicationName() : "N/A");
            logger.error(errMsg);
            throw new IllegalStateException(errMsg);
        }
    }


    public Object execute(
            String interfaceName,
            String methodName,
            List<String> orderedJavaParameterNames,
            Class<?>[] parameterJavaTypes,
            Map<String, Object> mcpProvidedParameters,
            String group,
            String version) {
        String cacheKey = interfaceName + ":" + (group == null ? "" : group) + ":" + (version == null ? "" : version);
        GenericService genericService = serviceCache.get(cacheKey);
        if (genericService == null) {
            ReferenceConfig<GenericService> reference = new ReferenceConfig<>();
            reference.setApplication(this.applicationConfig);
            reference.setInterface(interfaceName);
            reference.setGeneric("true"); // 默认为 "bean" 或 "true" (pojo) 泛化

            // If no registries are configured, assume local JVM call is intended or possible.
            if (this.applicationConfig.getRegistries() == null || this.applicationConfig.getRegistries().isEmpty()) {
                logger.info("No registries configured. Setting scope to 'local' for interface: " + interfaceName +
                        (group != null ? ", group: " + group : "") +
                        (version != null ? ", version: " + version : ""));
                reference.setScope("local");
            }

            if (group != null && !group.isEmpty()) {
                reference.setGroup(group);
            }
            if (version != null && !version.isEmpty()) {
                reference.setVersion(version);
            }

            try {
                genericService = reference.get();
                if (genericService != null) {
                    serviceCache.put(cacheKey, genericService);
                } else {
                    String errorMessage = "Failed to obtain GenericService instance for " + interfaceName +
                            (group != null ? " group " + group : "") +
                            (version != null ? " version " + version : "");
                    logger.error(errorMessage);
                    throw new IllegalStateException(errorMessage);
                }
            } catch (Exception e) {
                String errorMessage = "Error obtaining GenericService for " + interfaceName + ": " + e.getMessage();
                logger.error(errorMessage, e);
                throw new RuntimeException(errorMessage, e);
            }
        }

        String[] invokeParameterTypes =  new String[parameterJavaTypes.length];
        for (int i = 0; i < parameterJavaTypes.length; i++) {
            invokeParameterTypes[i] = parameterJavaTypes[i].getName();
        }

        Object[] invokeArgs = new Object[orderedJavaParameterNames.size()];
        for (int i = 0; i < orderedJavaParameterNames.size(); i++) {
            String paramName = orderedJavaParameterNames.get(i);
            if (mcpProvidedParameters.containsKey(paramName)) {
                invokeArgs[i] = mcpProvidedParameters.get(paramName);
            }else  {
                invokeArgs[i] = null;
                logger.warn("Parameter '{}' not found in MCP provided parameters for method '{}' of interface '{}'. Will use null.",
                        paramName, methodName, interfaceName);
            }
        }

        try{
            logger.info("Executing generic call: interface='{}', method='{}', group='{}', version='{}', paramTypes={}, args={}",
                    interfaceName, methodName, group, version, invokeParameterTypes, invokeArgs);
            Object result = genericService.$invoke(methodName, invokeParameterTypes, invokeArgs);
            logger.info("Generic call result: {}", result);
            return result;
        }catch (Exception e){
            String errorMessage = "GenericService $invoke failed for method '" + methodName + "' on interface '" + interfaceName + "': " + e.getMessage();
            logger.error(errorMessage, e);
            throw new RuntimeException(errorMessage, e);
        }
    }
}