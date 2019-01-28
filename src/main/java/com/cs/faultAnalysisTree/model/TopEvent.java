package com.cs.faultAnalysisTree.model;

import com.alibaba.fastjson.annotation.JSONField;
import com.cs.faultAnalysisTree.utils.CalculationUtils;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.cs.faultAnalysisTree.utils.CalculationUtils.*;

/**
 * 顶级事件
 */
@Slf4j
public class TopEvent extends IntermediateEventDTO {

    public String type = "Top";
    /**
     * 存放参数
     */
    @JSONField(serialize = false)
    public List<BaseEventDTO> baseEventList = new ArrayList<>();

    /**
     * 计算重要度表达式
     */
    public String expression;

    //    @JSONField(serialize = false)
    private List<EventParams> params;

    Map<String, String> paramsMap = new HashMap<>();

    /**
     * 递归获取底事件
     *
     * @param events
     */
    private void recursive(List<BaseEventDTO> events) {
        if (CollectionUtils.isEmpty(events)) {
            return;
        }
        for (BaseEventDTO event : events) {
            if (!(event instanceof IntermediateEventDTO)) {
                baseEventList.add(event);
            } else {
                IntermediateEventDTO eventDTO = (IntermediateEventDTO) event;
                if (CollectionUtils.isNotEmpty(eventDTO.getAndBaseEvents())) {
                    recursive(eventDTO.getAndBaseEvents());
                } else if (CollectionUtils.isNotEmpty(eventDTO.getOrBaseEvents())) {
                    recursive(eventDTO.getOrBaseEvents());
                } else if (CollectionUtils.isNotEmpty(eventDTO.getNoBaseEvents())) {
                    recursive(eventDTO.getNoBaseEvents());
                } else {
                    recursive(eventDTO.getOXRBaseEvents());
                }
            }
        }
    }

    /**
     * 获取所有的底事件
     */
    public void baseEventList() {
        if (CollectionUtils.isNotEmpty(andBaseEvents)) {
            recursive(andBaseEvents);
        } else if (CollectionUtils.isNotEmpty(orBaseEvents)) {
            recursive(orBaseEvents);
        } else if (CollectionUtils.isNotEmpty(noBaseEvents)) {
            recursive(noBaseEvents);
        } else if (CollectionUtils.isNotEmpty(OXRBaseEvents)) {
            recursive(OXRBaseEvents);
        }
    }

    /**
     * 生成对应的参数集合
     * 底事件Name + 系统设置的参数 从X1到Xn + 当前底事件的概率
     * 为后续求偏导数 提供相应的数据支撑
     */
    public void createParams() {
        if (CollectionUtils.isEmpty(baseEventList)) {
            baseEventList();
        }
        if (CollectionUtils.isNotEmpty(baseEventList)) {
            AtomicInteger atomicInteger = new AtomicInteger(1);
            //去重 去掉相同的底事件
            Set<BaseEventDTO> envets = baseEventList.stream().collect(Collectors.toSet());
            params = Flux.fromIterable(envets).map(item -> new EventParams(item.getName(), PARAM_PREFIX + atomicInteger.getAndIncrement(), item.getProbability())).collectList().block();
            paramsMap = params.stream().collect(Collectors.toMap(EventParams::getName, EventParams::getParam));
        }
    }

    /**
     * 遍历故障树 生成相关的表达式
     */
    public void expressionRecursive() {
        StringBuilder builder = new StringBuilder();
        if (CollectionUtils.isNotEmpty(andBaseEvents)) {
            for (BaseEventDTO event : andBaseEvents) {
                if (!(event instanceof IntermediateEventDTO)) {
                    String name = event.getName();
                    builder.append(paramsMap.get(name)).append(MULTIPLY);
                } else {
                    builder.append(expressionRecursive(event));
                }
            }
            builder.delete(builder.lastIndexOf(MULTIPLY), builder.length());
        } else if (CollectionUtils.isNotEmpty(orBaseEvents)) {
            for (BaseEventDTO event : orBaseEvents) {
                if (!(event instanceof IntermediateEventDTO)) {
                    String name = event.getName();
                    builder.append(paramsMap.get(name)).append(ADD);
                } else {
                    builder.append(expressionRecursive(event)).append(ADD);
                }
            }
            builder.delete(builder.lastIndexOf(ADD), builder.length());
        } else if (CollectionUtils.isNotEmpty(noBaseEvents)) {
            BaseEventDTO event = noBaseEvents.get(0);
            if (!(event instanceof IntermediateEventDTO)) {
                String name = event.getName();
                builder.append("1-").append(paramsMap.get(name));
            } else {
                builder.append("1-").append(expressionRecursive(event));
            }
        } else if (CollectionUtils.isNotEmpty(OXRBaseEvents)) {

        }
        expression = builder.toString();
        expressionResolver();
        params = CalculationUtils.calculationPartialDerivative(expression, params);
        setParams(params);
    }

    /**
     * 生成对应的FS表达式
     */
    private void expressionResolver() {
        List<String> list = Arrays.stream(expression.split(EMPTY)).collect(Collectors.toList());
        List<Object> expressionList = Lists.newArrayList();
        CalculationUtils.recursive2(list, 1, expressionList);
        List<String> calculations = CalculationUtils.combination2(expressionList, 0);
        StringBuilder builder = new StringBuilder();
        calculations = calculations.stream().filter(item -> !Objects.equals(ADD, item)).collect(Collectors.toList());
        builder.append("1-");
        for (String calculation : calculations) {
            builder.append(LEFT_PARENTHESIS).append("1-").append(calculation).append(RIGHT_PARENTHESIS).append(MULTIPLY);
        }
        builder.delete(builder.lastIndexOf(MULTIPLY), builder.length());
        expression = builder.toString();
        log.info("expression:{}", expression);
    }

    public String expressionRecursive(BaseEventDTO eventDTO) {
        StringBuilder builder = new StringBuilder();
        builder.append(LEFT_PARENTHESIS);
        if (eventDTO instanceof IntermediateEventDTO) {
            IntermediateEventDTO e = (IntermediateEventDTO) eventDTO;
            if (CollectionUtils.isNotEmpty(e.getAndBaseEvents())) {
                for (BaseEventDTO event : e.getAndBaseEvents()) {
                    if (!(event instanceof IntermediateEventDTO)) {
                        String name = event.getName();
                        builder.append(paramsMap.get(name)).append(MULTIPLY);
                    } else {
                        builder.append(expressionRecursive(event)).append(MULTIPLY);
                    }
                }
                builder.delete(builder.lastIndexOf(MULTIPLY), builder.length());
            } else if (CollectionUtils.isNotEmpty(e.getOrBaseEvents())) {
                for (BaseEventDTO event : e.getOrBaseEvents()) {
                    if (!(event instanceof IntermediateEventDTO)) {
                        String name = event.getName();
                        builder.append(paramsMap.get(name)).append(ADD);
                    } else {
                        builder.append(expressionRecursive(event)).append(ADD);
                    }
                }
                builder.delete(builder.lastIndexOf(ADD), builder.length());
            } else if (CollectionUtils.isNotEmpty(e.getNoBaseEvents())) {
                builder.append("1-");
                for (BaseEventDTO event : e.getOrBaseEvents()) {
                    if (event instanceof IntermediateEventDTO) {
                        expressionRecursive(event);
                    } else {
                        String name = event.getName();
                        builder.append(paramsMap.get(name));
                    }
                }
            } else if (CollectionUtils.isNotEmpty(e.getOXRBaseEvents())) {

            }
        } else {
            String name = eventDTO.getName();
            return paramsMap.get(name);
        }
        builder.append(RIGHT_PARENTHESIS);
        return builder.toString();
    }


    public List<EventParams> getParams() {
        if (CollectionUtils.isEmpty(params)) {
            createParams();
        }
        return params;
    }

    public void setParams(List<EventParams> params) {
        this.params = params;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<BaseEventDTO> getBaseEventList() {
        return baseEventList;
    }

    public void setBaseEventList(List<BaseEventDTO> baseEventList) {
        this.baseEventList = baseEventList;
    }

    public String getExpression() {
        expressionRecursive();
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public Map<String, String> getParamsMap() {
        return paramsMap;
    }

    public void setParamsMap(Map<String, String> paramsMap) {
        this.paramsMap = paramsMap;
    }
}
