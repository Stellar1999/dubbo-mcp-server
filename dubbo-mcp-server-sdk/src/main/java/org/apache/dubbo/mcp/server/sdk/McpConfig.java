package org.apache.dubbo.mcp.server.sdk;

import java.io.Serializable;

public class McpConfig implements Serializable {

    private Boolean enabled;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
