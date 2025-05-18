package org.apache.dubbo.mcp.server.demo;

import java.util.List;
import java.util.Map;

public class ComplexRequest {
    private String greeting;
    private int count;
    private boolean active;
    private NestedDetail nestedDetail;
    private List<String> tags;
    private Map<String, String> attributes;

    // Default constructor (important for deserialization)
    public ComplexRequest() {
    }

    // Getters and Setters
    public String getGreeting() {
        return greeting;
    }

    public void setGreeting(String greeting) {
        this.greeting = greeting;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public NestedDetail getNestedDetail() {
        return nestedDetail;
    }

    public void setNestedDetail(NestedDetail nestedDetail) {
        this.nestedDetail = nestedDetail;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String toString() {
        return "ComplexRequest{" +
                "greeting='" + greeting + '\'' +
                ", count=" + count +
                ", active=" + active +
                ", nestedDetail=" + nestedDetail +
                ", tags=" + tags +
                ", attributes=" + attributes +
                '}';
    }
} 