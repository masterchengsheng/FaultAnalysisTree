package com.cs.faultAnalysisTree.model;

import com.alibaba.fastjson.annotation.JSONField;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * 基础事件(底事件)
 */
public class BaseEventDTO implements Serializable {

    @JSONField(ordinal = 3)
    public String type = "Base";

    /**
     * 名称
     */
    @JSONField(ordinal = 1)
    protected String name;

    /**
     * 概率
     */
    @JSONField(ordinal = 2)
    protected BigDecimal probability;

    /**
     * 相关数据
     */
    @JSONField(ordinal = 4)
    protected List<Object> relateData;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getProbability() {
        return probability;
    }

    public void setProbability(BigDecimal probability) {
        this.probability = probability;
    }

    public List<Object> getRelateData() {
        return relateData;
    }

    public void setRelateData(List<Object> relateData) {
        this.relateData = relateData;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BaseEventDTO)) return false;
        BaseEventDTO that = (BaseEventDTO) o;
        return Objects.equals(type, that.type) &&
                Objects.equals(getName(), that.getName()) &&
                Objects.equals(getProbability(), that.getProbability()) &&
                Objects.equals(getRelateData(), that.getRelateData());
    }

    @Override
    public int hashCode() {

        return Objects.hash(type, getName(), getProbability(), getRelateData());
    }
}
