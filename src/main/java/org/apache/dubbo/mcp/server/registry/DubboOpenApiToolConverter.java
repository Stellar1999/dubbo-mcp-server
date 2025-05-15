package org.apache.dubbo.mcp.server.registry;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.http12.HttpMethods;
import org.apache.dubbo.remoting.http12.rest.OpenAPIRequest;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.DefaultOpenAPIService;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.*;

import java.util.HashMap;
import java.util.Map;

public class DubboOpenApiToolConverter {

    private final DefaultOpenAPIService openAPIService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public DubboOpenApiToolConverter(DefaultOpenAPIService openAPIService) {
        this.openAPIService = openAPIService;
    }

    public Map<String, McpSchema.Tool> convertToTools(ServiceDescriptor serviceDescriptor, URL serviceURL) {
        // 创建OpenAPI请求
        OpenAPIRequest request = new OpenAPIRequest();
        String interfaceName = serviceDescriptor.getInterfaceName();
        request.setService(new String[]{interfaceName});
        OpenAPI openAPI = openAPIService.getOpenAPI(request);
        System.out.println(openAPI);
         
        if (openAPI == null || openAPI.getPaths() == null) {
            return new HashMap<>();
        }

        // 转换服务中的每个操作为MCP工具
        Map<String, McpSchema.Tool> tools = new HashMap<>();

        for (Map.Entry<String, PathItem> pathEntry : openAPI.getPaths().entrySet()) {
            String path = pathEntry.getKey();
            PathItem pathItem = pathEntry.getValue();
            if (pathItem.getOperations() != null) {
                for (Map.Entry<HttpMethods, Operation> opEntry : pathItem.getOperations().entrySet()) {
                    HttpMethods method = opEntry.getKey();
                    Operation operation = opEntry.getValue();
                    String toolId = operation.getOperationId();
                    McpSchema.Tool tool = convertOperationToTool(path, method, operation, serviceURL);
                    if (tool != null) {
                        tools.put(toolId, tool);
                    }
                }
            }
        }
        return tools;
    }

    /**
     * 将单个API操作转换为MCP工具
     */
    private McpSchema.Tool convertOperationToTool(String path, HttpMethods method,
                                                  Operation operation, URL serviceURL) {
        if (operation == null || operation.getOperationId() == null) {
            return null;
        }

        // 工具ID
        String toolId = operation.getOperationId();

        // 提取描述信息
        String description = operation.getSummary();
        if (description == null) {
            description = operation.getDescription();
        }
        if (description == null) {
            description = "执行 " + method + " " + path;
        }

        // 提取参数模式（请求体或参数）
        Map<String, Object> parameterSchema = extractParameterSchema(operation);

        // 将参数模式转换为JSON字符串
        String schemaJson;
        try {
            schemaJson = objectMapper.writeValueAsString(parameterSchema);
        } catch (Exception e) {
            schemaJson = "{\"type\":\"object\",\"properties\":{}}";
        }

        return new McpSchema.Tool(toolId, description, schemaJson);
    }

    private Map<String, Object> extractParameterSchema(Operation operation) {
        Map<String, Object> schema = new HashMap<>();
        Map<String, Object> properties = new HashMap<>();

        if (operation.getParameters() != null) {
            operation.getParameters().forEach(parameter -> {
                String name = parameter.getName();
                Schema paramSchema = parameter.getSchema();
                if (paramSchema != null) {
                    properties.put(name, convertSchemaToMap(paramSchema));
                }
            });
        }

        // 处理请求体
        if (operation.getRequestBody() != null && operation.getRequestBody().getContents() != null) {
            for (Map.Entry<String, MediaType> entry : operation.getRequestBody().getContents().entrySet()) {
                MediaType mediaType = entry.getValue();
                if (mediaType.getSchema() != null) {
                    Map<String, Object> bodySchema = convertSchemaToMap(mediaType.getSchema());
                    // 如果是复杂对象，合并其属性
                    if (bodySchema.containsKey("properties")) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> bodyProps = (Map<String, Object>) bodySchema.get("properties");
                        properties.putAll(bodyProps);
                    } else {
                        properties.put("body", bodySchema);
                    }
                    break; // 只处理第一个媒体类型
                }
            }
        }

        schema.put("type", "object");
        schema.put("properties", properties);

        return schema;
    }

    private Map<String, Object> convertSchemaToMap(Schema schema) {
        Map<String, Object> result = new HashMap<>();

        // 基本类型属性
        if (schema.getType() != null) {
            result.put("type", schema.getType().toString().toLowerCase());
        }

        if (schema.getFormat() != null) {
            result.put("format", schema.getFormat());
        }

        if (schema.getDescription() != null) {
            result.put("description", schema.getDescription());
        }

        if (Schema.Type.OBJECT.equals(schema.getType()) && schema.getProperties() != null) {
            Map<String, Object> properties = new HashMap<>();
            schema.getProperties().forEach((name, propSchema) ->
                    properties.put(name, convertSchemaToMap(propSchema))
            );
            result.put("properties", properties);
        }

        // 数组类型特有属性
        if (Schema.Type.ARRAY.equals(schema.getType()) && schema.getItems() != null) {
            result.put("items", convertSchemaToMap(schema.getItems()));
        }

        return result;
    }

}
