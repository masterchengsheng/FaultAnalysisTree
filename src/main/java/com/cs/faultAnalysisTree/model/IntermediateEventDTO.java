package com.cs.faultAnalysisTree.model;

import com.alibaba.fastjson.annotation.JSONField;
import org.apache.commons.collections4.CollectionUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 中间事件
 */
public class IntermediateEventDTO extends BaseEventDTO {

    public String type = "Intermediate";

    public String expression;

    /**
     * 与
     */
    @JSONField(ordinal = 5)
    protected List<BaseEventDTO> andBaseEvents;

    /**
     * 或
     */
    @JSONField(ordinal = 6)
    protected List<BaseEventDTO> orBaseEvents;

    /**
     * 非
     */
    @JSONField(ordinal = 7)
    protected List<BaseEventDTO> noBaseEvents;

    /**
     * 异或
     */
    @JSONField(ordinal = 8)
    protected List<BaseEventDTO> OXRBaseEvents;


    public void calculatedProbability() {
        if (CollectionUtils.isNotEmpty(andBaseEvents)) {
            for (BaseEventDTO andBaseEvent : andBaseEvents) {
                if (Objects.isNull(andBaseEvent.probability)) {
                    IntermediateEventDTO intermediateEventDTO = (IntermediateEventDTO) andBaseEvent;
                    intermediateEventDTO.calculatedProbability();
                }
            }
            List<BigDecimal> probabilityList = andBaseEvents.stream().map(BaseEventDTO::getProbability).collect(Collectors.toList());
            probability = BigDecimal.ONE;
            for (BigDecimal decimal : probabilityList) {
                probability = probability.multiply(decimal);
            }
        } else if (CollectionUtils.isNotEmpty(orBaseEvents)) {
            for (BaseEventDTO andBaseEvent : orBaseEvents) {
                if (Objects.isNull(andBaseEvent.probability)) {
                    IntermediateEventDTO intermediateEventDTO = (IntermediateEventDTO) andBaseEvent;
                    intermediateEventDTO.calculatedProbability();
                }
            }
            List<BigDecimal> probabilityList = orBaseEvents.stream().map(BaseEventDTO::getProbability).collect(Collectors.toList());
            probability = BigDecimal.ZERO;
            for (BigDecimal decimal : probabilityList) {
                probability = probability.add(decimal);
            }
        } else if (CollectionUtils.isNotEmpty(noBaseEvents)) {
            BaseEventDTO baseEvent = noBaseEvents.get(0);
            if (Objects.isNull(baseEvent.getProbability())) {
                ((IntermediateEventDTO) baseEvent).calculatedProbability();
            }
            probability = BigDecimal.ONE.subtract(baseEvent.getProbability());
        } else if (CollectionUtils.isNotEmpty(OXRBaseEvents)) {

        } else {
            this.probability = BigDecimal.ZERO;
        }

    }


    public List<BaseEventDTO> getAndBaseEvents() {
        return andBaseEvents;
    }

    public void setAndBaseEvents(List<BaseEventDTO> andBaseEvents) {
        this.andBaseEvents = andBaseEvents;
    }


    public List<BaseEventDTO> getOrBaseEvents() {
        return orBaseEvents;
    }

    public void setOrBaseEvents(List<BaseEventDTO> orBaseEvents) {
        this.orBaseEvents = orBaseEvents;
    }


    public List<BaseEventDTO> getNoBaseEvents() {
        return noBaseEvents;
    }

    public void setNoBaseEvents(List<BaseEventDTO> noBaseEvents) {
        this.noBaseEvents = noBaseEvents;
    }


    public List<BaseEventDTO> getOXRBaseEvents() {
        return OXRBaseEvents;
    }

    public void setOXRBaseEvents(List<BaseEventDTO> OXRBaseEvents) {
        this.OXRBaseEvents = OXRBaseEvents;
    }


    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }
}
