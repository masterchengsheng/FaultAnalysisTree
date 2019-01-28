package com.cs.faultAnalysisTree.model;

import java.io.Serializable;
import java.math.BigDecimal;

public class EventParams implements Serializable {

    /**
     * 真实名称
     */
    private String name;

    /**
     * 参数名
     */
    private String param;

    /**
     * 概率
     */
    private BigDecimal probability;

    /**
     * 重要度
     */
    private BigDecimal importanceRate;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getParam() {
        return param;
    }

    public void setParam(String param) {
        this.param = param;
    }

    public EventParams(String name, String param) {
        this.name = name;
        this.param = param;
    }

    public EventParams() {
    }

    public EventParams(String name, String param, BigDecimal probability) {
        this.name = name;
        this.param = param;
        this.probability = probability;
    }

    public BigDecimal getProbability() {
        return probability;
    }

    public void setProbability(BigDecimal probability) {
        this.probability = probability;
    }

    public BigDecimal getImportanceRate() {
        return importanceRate;
    }

    public void setImportanceRate(BigDecimal importanceRate) {
        this.importanceRate = importanceRate;
    }
}
