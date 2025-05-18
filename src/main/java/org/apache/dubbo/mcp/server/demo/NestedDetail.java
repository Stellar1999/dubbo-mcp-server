package org.apache.dubbo.mcp.server.demo;

public class NestedDetail {
    private String detailInfo;
    private Double value;

    // Default constructor
    public NestedDetail() {
    }

    // Getters and Setters
    public String getDetailInfo() {
        return detailInfo;
    }

    public void setDetailInfo(String detailInfo) {
        this.detailInfo = detailInfo;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "NestedDetail{" +
                "detailInfo='" + detailInfo + '\'' +
                ", value=" + value +
                '}';
    }
} 