package com.cs.faultAnalysisTree.utils;

import com.alibaba.fastjson.JSONObject;
import com.cs.faultAnalysisTree.model.EventParams;
import com.cs.faultAnalysisTree.model.TempResultDTO;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class CalculationUtils {


    public static final String MULTIPLY = "*";
    public static final String ADD = "+";
    public static final String SUBTRACTION = "-";
    public static final String BOTTOM = "Bottom";

    public static final String LEFT_PARENTHESIS = "(";
    public static final String RIGHT_PARENTHESIS = ")";

    public static final String PARAM_PREFIX = "X";
    public static final int PRECISION = 5;
    public static final String SPLIT_MULTIPLY = "\\*";
    public static final String EMPTY = "";

    public static TempResultDTO recursive2(List<String> list, Integer index, List<Object> result) {
        FOR:
        for (int i = 0; i < list.size(); i++) {
            String item = list.get(i);
            if (Objects.equals(item, MULTIPLY) || Objects.equals(item, ADD)) {
                result.add(item);
                continue;
            }
            if (Objects.equals(item, LEFT_PARENTHESIS)) {
                List<String> temp = list.subList(i + 1, list.size());
                TempResultDTO resultDTO = recursive2(temp, index + 1, Lists.newArrayList());
                i += resultDTO.getIndex();
                result.add(resultDTO.getResult());
                i++;
                continue;
            }
            List<String> temp;
            for (int j = i; j < list.size(); j++) {
                String item1 = list.get(j);
                if (Objects.equals(item1, RIGHT_PARENTHESIS)) {
                    temp = list.subList(i, j);
                    if (CollectionUtils.isEmpty(temp)) {
                        return new TempResultDTO(result, j);
                    }
                    StringBuilder builder = new StringBuilder();
                    for (String s : temp) {
                        builder.append(s);
                    }
                    String src = builder.toString();
                    TempResultDTO resultDTO = new TempResultDTO(resolve(src), j);
                    return resultDTO;
                }
            }

            for (int j = i; j < list.size(); j++) {
                String item1 = list.get(j);
                if (Objects.equals(item1, MULTIPLY) || Objects.equals(item1, ADD)) {
                    temp = list.subList(i, j);
                    StringBuilder builder = new StringBuilder();
                    for (String s : temp) {
                        builder.append(s);
                    }
                    result.add(builder.toString());
                    i = j - 1;
                    continue FOR;
                }
            }
            temp = list.subList(i, list.size());
            if (!temp.contains(RIGHT_PARENTHESIS)) {
                StringBuilder builder = new StringBuilder();
                for (String s : temp) {
                    builder.append(s);
                }
                result.add(builder.toString());
                i = list.size();
            }
        }
        return null;
    }

    private static List<String> resolve(String src) {
        List<String> result = Lists.newArrayList();
        List<String> list = Arrays.stream(src.split(EMPTY)).collect(Collectors.toList());
        int marker = 0;
        for (int i = 0; i < list.size(); i++) {
            String item = list.get(i);
            if (Objects.equals(MULTIPLY, item) || Objects.equals(ADD, item)) {
                List<String> subList = list.subList(marker, i);
                marker = i + 1;
                StringBuilder builder = new StringBuilder();
                for (String s : subList) {
                    builder.append(s);
                }
                result.add(builder.toString());
                result.add(item);
            }
        }
        StringBuilder builder = new StringBuilder();
        list = list.subList(marker, list.size());
        for (String s : list) {
            builder.append(s);
        }
        result.add(builder.toString());
        return result;
    }


    public static List<String> combination2(List<Object> list, Integer index) {
        List<Object> lists = new ArrayList<>();
        if (index > list.size() - 1) {
            return Lists.newArrayList();
        }
        Object obj = list.get(index);
        if (obj instanceof List) {
            List<String> result = combination2((List<Object>) obj, 0);//
            if (CollectionUtils.isEmpty(result)) {
                return Lists.newArrayList();
            }
            if (result.size() == 1 && Objects.equals(result.get(0), BOTTOM)) {//当前数据是最下面一层
                lists.add(pretreatmentObj((List<String>) obj));
                if (index < list.size() - 1) {
                    lists.add(list.get(index + 1));
                    List<String> list1 = combination2(list, index + 2);
                    if (CollectionUtils.isNotEmpty(list1)) {
                        if (list1.size() == 1 && Objects.equals(list1.get(0), BOTTOM)) {
                            lists.add(list.get(index + 2));
                        } else {
                            lists.add(combination2(list, index + 2));
                        }
                    }
                }
                return calculationAnalysis(lists, index);
            } else {
                list.remove(obj);//将原先的数据替换掉
                list.add(index, result); //将新的数据插入
                for (int i = 0; i < list.size(); i = i + 2) {
                    if (i > list.size() - 2) {
                        break;
                    }
                    Object obj1 = list.get(i + 2);
                    if (obj1 instanceof List) {
                        List<String> list1 = combination2((List<Object>) list.get(i + 2), 0);
                        if (list1.size() == 1 && Objects.equals(list1.get(0), BOTTOM)) {
                            System.out.println(obj1);
                        } else {
                            list.remove(obj1);
                            list.add(i + 2, list1);
                        }
                    }
                }
                List<String> list1 = calculationAnalysis(list, 0);
                System.out.println(JSONObject.toJSONString(list1));
                return list1;

            }
        } else {
            return Lists.newArrayList(BOTTOM);
        }
    }


    private static List<String> calculationAnalysis(List<Object> lists, Integer index) {
        if (lists.size() == 1) {
            return (List<String>) lists.get(0);
        } else {
            List<Integer> multIndex = Lists.newArrayList();
            for (int i = 0; i < lists.size(); i++) {//定位乘号的位置  [x1+x2] + [x3+x4]*[x5+x6]*[x7+x8]+ [x9]
                if (Objects.equals(lists.get(i), MULTIPLY)) {
                    multIndex.add(i);
                }
            }
            if (CollectionUtils.isEmpty(multIndex)) {
                return add(lists);
            } else {
                List<Object> newLists = new ArrayList<>();
                for (int i = 1; i < lists.size(); i++) {
                    if (multIndex.contains(i)) {
                        if (i == 1) {
                            List<String> temp1 = (List<String>) lists.get(i - 1);
                            List<String> temp2 = (List<String>) lists.get(i + 1);
                            newLists.add(multiply(temp1, temp2));
                            i++;
                        } else {
                            Object object = newLists.get(newLists.size() - 1);
                            List<String> temp1;
                            if (object instanceof List) {
                                temp1 = (List<String>) object;
                            } else if (object instanceof String) {
                                temp1 = Lists.newArrayList((String) object);
                            } else {
                                continue;
                            }
                            List<String> temp2 = (List<String>) lists.get(i + 1);
                            newLists.add(multiply(temp1, temp2));
                        }
                    } else {
                        newLists.add(lists.get(i));
                    }
                }
                return add(newLists);
            }
        }
    }

    private static List<String> add(List<Object> lists) {
        List<String> calculationResult = new ArrayList<>();
        for (Object newList : lists) {
            if (newList instanceof List) {
                for (String s : (List<String>) newList) {
                    calculationResult.add(s);
                }
            } else {
                calculationResult.add((String) newList);
            }
        }
        return calculationResult;
    }

    /**
     * @param list
     * @return
     */
    public static List<String> pretreatmentObj(List<String> list) {
        List<Integer> multiplyIndex = Lists.newArrayList();
        for (int i = 0; i < list.size(); i++) {
            if (Objects.equals(MULTIPLY, list.get(i))) {
                multiplyIndex.add(i);
            }
        }
        if (CollectionUtils.isEmpty(multiplyIndex)) {
            return list;
        }
        List<String> newList = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            String item = list.get(i);
            if (multiplyIndex.contains(i)) {
                if (i == 1) {
                    newList.remove(0);
                    newList.add(list.get(0) + list.get(2));
                    i++;
                } else {
                    String lastItem = newList.get(newList.size() - 1);
                    newList.remove(lastItem);
                    newList.add(lastItem + list.get(i + 1));
                    i++;
                }
            } else {
                newList.add(item);
            }
        }
        return newList;
    }

    /**
     * 去除 （X1*X2） => X1X2
     *
     * @param list
     * @return
     */
    public static List<String> pretreatment(List<String> list) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            String item = list.get(i);
            if (Objects.equals(MULTIPLY, item)) {
                result.add(result.size() - 1 + list.get(i + 1));
                i++;
            } else {
                result.add(item);
            }
        }
        return result;
    }


    public static List<String> pretreatmentSubtraction(List<String> list) {
        List<String> resultList = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            String p = list.get(i);
            if (Objects.equals(p,SUBTRACTION)){
                resultList.add(SUBTRACTION);
                continue;
            }
            if (p.indexOf(SUBTRACTION) != -1) {
                List<String> result = Arrays.stream(p.split(SUBTRACTION)).collect(Collectors.toList());
                for (int i1 = 0; i1 < result.size(); i1++) {
                    if (i1 < result.size() - 1) {
                        resultList.add(result.get(i1));
                        resultList.add(SUBTRACTION);
                    } else {
                        resultList.add(result.get(i1));
                    }
                }
            }else{
                resultList.add(p);
            }

        }
        return resultList;
    }

    /**
     * 乘法 => 加法
     *
     * @param p1
     * @param p2
     * @return
     */
    public static List<String> multiply(List<String> p1, List<String> p2) {
        p1 = pretreatment(p1);
        p2 = pretreatment(p2);
        List<String> nP2 = pretreatmentSubtraction(p2);
        List<String> nP1 = pretreatmentSubtraction(p1);
        List<String> result = Lists.newArrayList();
        boolean flag1 = true;
        boolean marker = true;
        for (String s : nP1) {
            if (Objects.equals(s, ADD)) {
                flag1 = true;
                continue;
            } else if (Objects.equals(s, SUBTRACTION)) {
                flag1 = false;
                continue;
            }
            boolean flag2 = true;
            for (String s1 : nP2) {
                if (Objects.equals(s1, ADD)) {
                    flag2 = true;
                    continue;
                } else if (Objects.equals(s1, SUBTRACTION)) {
                    flag2 = false;
                    continue;
                }
                if (marker) {
                    marker = false;
                } else {
                    if (flag1 == true && flag2 == true || flag1 == false && flag2 == false) {
                        result.add(ADD);
                    } else {
                        result.add(SUBTRACTION);
                    }
                }
                if (Objects.equals(s,"1")){
                    result.add(s1);
                }else if (Objects.equals(s1,"1")){
                    result.add(s);
                }else {
                    result.add(s + s1);
                }
            }
        }
        log.info(JSONObject.toJSONString(result));
        return result;
    }


    /**
     * 求偏导数
     *
     * @param expression FS
     * @param params
     */
    public static List<EventParams> calculationPartialDerivative(String expression, List<EventParams> params) {
        Map<String, BigDecimal> map = params.stream().collect(Collectors.toMap(item -> item.getParam(), item -> item.getProbability()));
        expression = expression.substring(2, expression.length());
        List<String> list1 = Arrays.stream(expression.split(SPLIT_MULTIPLY)).collect(Collectors.toList());
        Map<String, String> expressionList = new HashMap<>();
        for (EventParams param : params) {
            BigDecimal result = BigDecimal.ONE;
            String p = param.getParam();
            String expression1 = "";
            for (String s : list1) {
                List<String> parameters = splitParameter(s);
                if (parameters.contains(p)) {
                    BigDecimal r1 = BigDecimal.ONE;
                    for (String parameter : parameters) {
                        if (Objects.equals(parameter, p)) {
                            continue;
                        } else {
                            BigDecimal probability = map.get(parameter);
                            r1 = r1.multiply(probability).setScale(PRECISION, RoundingMode.FLOOR);
                        }
                    }
                    if (r1.compareTo(BigDecimal.ONE) == 0) {
                        expression1 = "(1-" + p + RIGHT_PARENTHESIS;
                    } else {
                        expression1 = "(1-" + p + MULTIPLY + r1.setScale(PRECISION, RoundingMode.FLOOR).doubleValue() + RIGHT_PARENTHESIS;
                    }
                } else {
                    BigDecimal r1 = BigDecimal.ONE;
                    for (String parameter : parameters) {
                        r1 = r1.multiply(map.get(parameter)).setScale(PRECISION, RoundingMode.FLOOR);
                    }
                    r1 = BigDecimal.ONE.subtract(r1).setScale(PRECISION, RoundingMode.FLOOR);
                    result = result.multiply(r1);
                }
            }
            expression1 = "1-" + expression1 + MULTIPLY + result.setScale(PRECISION, RoundingMode.FLOOR);
            BigDecimal b = partialDerivative(expression1, map.get(p), p);
            param.setImportanceRate(b);
            expressionList.put(p, expression1);
        }
        return params;
    }

    /**
     * 将括号去掉
     *
     * @param expression 1-(1-0.2*X1)*(1-0.3*X1)*0.321 ==>  [1, -, [1, -, 0.2*X1], *, [1, -, 0.3*X1], *, 0.321]
     * @param value
     */
    private static BigDecimal partialDerivative(String expression, BigDecimal value, String param) {
        List<String> list = Arrays.stream(expression.split(EMPTY)).collect(Collectors.toList());
        List<Object> params = Lists.newArrayList();
        OUT:
        for (int i = 0; i < list.size(); i++) {
            String item = list.get(i);
            if (Objects.equals(SUBTRACTION, item) || Objects.equals(item, ADD) || Objects.equals(MULTIPLY, item)) {
                params.add(item);
            } else if (Objects.equals(LEFT_PARENTHESIS, item)) {
                List<String> innerList = new ArrayList<>();
                StringBuilder builder = new StringBuilder();
                for (int j = i + 1; j < list.size(); j++) {
                    item = list.get(j);
                    if (Objects.equals(RIGHT_PARENTHESIS, item)) {
                        innerList.add(builder.toString());
                        params.add(innerList);
                        i = j;
                        continue OUT;
                    }
                    if (Objects.equals(item, SUBTRACTION)) {
                        innerList.add(builder.toString());
                        builder.delete(0, builder.length());
                        innerList.add(item);
                    } else {
                        builder.append(item);
                    }
                }
            } else {
                StringBuilder builder = new StringBuilder();
                for (int j = i; j < list.size(); j++) {
                    item = list.get(j);
                    if (Objects.equals(SUBTRACTION, item) || Objects.equals(item, ADD) || Objects.equals(MULTIPLY, item)
                            || Objects.equals(LEFT_PARENTHESIS, item)) {
                        params.add(builder.toString());
                        i = j - 1;
                        continue OUT;
                    }
                    builder.append(list.get(j));
                    i = j;
                }
                params.add(builder.toString());
            }
        }
        String resultExpression = partialDerivativeExpression(params, 0);
        return lastDealWith(resultExpression, value, param);
    }


    private static BigDecimal lastDealWith(String resultExpression, BigDecimal value, String param) {
        List<String> list = new ArrayList<>();
        List<String> list2 = Arrays.stream(resultExpression.split(EMPTY)).collect(Collectors.toList());
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < list2.size(); i++) {
            String item = list2.get(i);
            if (Objects.equals(ADD, item) || Objects.equals(SUBTRACTION, item)) {
                list.add(builder.toString());
                builder.delete(0, builder.length());
                list.add(item);
                continue;
            }
            builder.append(item);
            if (i == list2.size() - 1) {
                list.add(builder.toString());
            }
        }
        boolean flag = true;
        BigDecimal result = BigDecimal.ZERO;
        for (int i = 0; i < list.size(); i++) {
            String item = list.get(i);
            if (NumberUtils.isParsable(item)) {
                continue;
            }
            if (Objects.equals(item, ADD)) {
                flag = true;
                continue;
            }
            if (Objects.equals(item, SUBTRACTION)) {
                flag = false;
                continue;
            }
            List<String> innerList = Arrays.stream(item.split(SPLIT_MULTIPLY)).collect(Collectors.toList());
            int count = checkParamCount(innerList);//参数的个数  X*X*x  求导 3*X*X
            boolean marker = true;
            BigDecimal b = BigDecimal.ONE;
            for (int j = 0; j < innerList.size(); j++) {
                String p = innerList.get(j);
                if (Objects.equals(p, param)) {
                    if (marker) {
                        marker = false;
                        b = b.multiply(new BigDecimal(count));
                        continue;
                    } else {
                        b = b.multiply(value);
                    }
                } else {
                    b = b.multiply(new BigDecimal(p));
                }
            }
            if (flag) {
                result = result.add(b);
            } else {
                result = result.subtract(b);
            }
        }
        return result;
    }

    /**
     * 参数的个数
     *
     * @param list
     * @return
     */
    private static int checkParamCount(List<String> list) {
        int i = 0;
        for (String s : list) {
            if (NumberUtils.isParsable(s)) {
                continue;
            }
            i++;
        }
        return i;
    }


    /**
     * 展开表达式的内容
     * [1, -, [1, -, 0.2*X1], *, [1, -, 0.3*X1], *, 0.321]
     *
     * @param params
     * @return
     */
    private static String partialDerivativeExpression(List<Object> params, Integer index) {
        String expression = "";
        StringBuilder builder = new StringBuilder();
        Object obj = params.get(index);
        if (obj instanceof String) {
            builder.append((String) obj);
            for (int i = index + 1; i < params.size(); i++) {
                if (params.get(i) instanceof String) {
                    builder.append(params.get(i));
                } else if (params.get(i) instanceof List) {
                    index = i;
                    String calculationResult = partialDerivativeExpression(params, index);
                    //因为前面都是 1- 所以这边将相关的+ 和 - 置换一下
                    List<String> list = Arrays.stream(calculationResult.split(EMPTY)).collect(Collectors.toList());
                    list = Flux.fromIterable(list).map(item -> {
                        if (Objects.equals(item, ADD)) {
                            return SUBTRACTION;
                        } else if (Objects.equals(item, SUBTRACTION)) {
                            return ADD;
                        } else {
                            return item;
                        }
                    }).collectList().block();
                    StringBuilder sb = new StringBuilder();
                    list.forEach(item -> sb.append(item));
                    builder.append(sb.toString());
                    return builder.toString();
                }
            }
            return builder.toString();
        } else if (obj instanceof List) {
            List<String> list1 = (List<String>) obj;
            List<String> list = new ArrayList<>();
            if (index < params.size() - 1) {
                index = index + 1;
                Object obj1 = params.get(index);
                if (obj1 instanceof String && Objects.equals(MULTIPLY, obj1)) {
                    index = index + 1;
                    Object obj2 = params.get(index);
                    list = multiplyExpression(list1, obj2);
                    for (int i = index; i < params.size(); i++) {
                        if (index < params.size() - 1) {
                            Object obj3 = params.get(index + 2);
                            list = multiplyExpression(list, obj3);
                        }
                    }
                } else if (obj1 instanceof List) {
                    index = index + 1;
                    Object obj2 = params.get(index);
                    list = multiplyExpression(list1, obj2);
                    for (int i = index; i < params.size(); i++) {
                        if (index < params.size() - 1) {
                            Object obj3 = params.get(index + 2);
                            list = multiplyExpression(list, obj3);
                        }
                    }
                }
            }
            StringBuilder sb = new StringBuilder();
            for (String s : list) {
                sb.append(s);
            }
            return sb.toString();
        }
        return expression;
    }

    private static List<String> multiplyExpression(List<String> list1, Object obj2) {
        List<String> result = new ArrayList<>();
        Boolean flag1 = true;
        if (obj2 instanceof List) {
            List<String> list2 = (List<String>) obj2;
            for (String s : list1) {
                if (Objects.equals(SUBTRACTION, s)) {
                    flag1 = false;
                    continue;
                }
                if (Objects.equals(ADD, s)) {
                    flag1 = true;
                    continue;
                }
                Boolean flag2 = true;
                for (String s1 : list2) {
                    if (Objects.equals(SUBTRACTION, s1)) {
                        flag2 = false;
                        continue;
                    }
                    if (Objects.equals(ADD, s1)) {
                        flag2 = true;
                        continue;
                    }
                    if (flag1 == true && flag2 == true) {
                        result.add(ADD);
                    } else if (flag1 == false && flag2 == false) {
                        result.add(ADD);
                    } else {
                        result.add(SUBTRACTION);
                    }
                    if (NumberUtils.isParsable(s)) {//如果s是数字
                        if (NumberUtils.isParsable(s1)) {
                            BigDecimal bigDecimal = new BigDecimal(s).multiply(new BigDecimal(s1));
                            result.add(bigDecimal.setScale(PRECISION, RoundingMode.FLOOR).toString());
                        } else {
                            result.add(multiplyResult(s, s1));
                        }
                    } else {
                        if (NumberUtils.isDigits(s1)) {//如果s1是数字
                            log.info(s);
                            result.add(multiplyResult(s1, s));
                        } else {
                            result.add(doubleSignMultiplyResult(s, s1));
                        }
                    }
                }
            }
            result.remove(0);
        } else if (obj2 instanceof String) {
            if (Objects.equals(obj2, MULTIPLY)) {
                return list1;
            }
            Boolean flag = false;
            BigDecimal b1 = BigDecimal.ONE;
            if (NumberUtils.isParsable((String) obj2)) {
                flag = true;//obj2是数字
                b1 = b1.multiply(new BigDecimal((String) obj2));
            }
            for (String s : list1) {
                if (Objects.equals(ADD, s) || Objects.equals(SUBTRACTION, s)) {
                    result.add(s);
                    continue;
                }
                if (NumberUtils.isParsable(s)) {//如果当前的是数字
                    if (flag) {//obj2 是数字
                        result.add(b1.multiply(new BigDecimal(s)).setScale(PRECISION, RoundingMode.FLOOR).toString());
                    } else {
                        result.add(multiplyResult(s, (String) obj2));
                    }
                } else {
                    if (flag) {//obj2 是数字
                        result.add(multiplyResult((String) obj2, s));
                    } else {
                        result.add(doubleSignMultiplyResult((String) obj2, s));
                    }
                }
            }
        }
        return result;
    }

    private static String doubleSignMultiplyResult(String s1, String s2) {
        List<String> list3 = Arrays.stream(s1.split(SPLIT_MULTIPLY)).collect(Collectors.toList());
        List<String> list4 = Arrays.stream(s2.split(SPLIT_MULTIPLY)).collect(Collectors.toList());
        BigDecimal b1 = BigDecimal.ONE;
        StringBuilder builder = new StringBuilder();
        for (String s3 : list3) {
            if (Objects.equals(MULTIPLY, s3)) {
                continue;
            }
            if (NumberUtils.isParsable(s3)) {
                b1 = b1.multiply(new BigDecimal(s3));
            } else {
                builder.append(s3).append(MULTIPLY);
            }
        }
        for (String s4 : list4) {
            if (Objects.equals(MULTIPLY, s4)) {
                continue;
            }
            if (NumberUtils.isParsable(s4)) {
                b1 = b1.multiply(new BigDecimal(s4));
            } else {
                builder.append(s4).append(MULTIPLY);
            }
        }
        builder.delete(builder.lastIndexOf(MULTIPLY), builder.length());
        String result = b1.setScale(PRECISION, RoundingMode.FLOOR).toString() + MULTIPLY + builder.toString();
        return result;
    }


    private static String multiplyResult(String s1, String s) {
        List<String> params = new ArrayList<>();
        BigDecimal b2 = new BigDecimal(s1);
        List<String> list = Arrays.stream(s.split(SPLIT_MULTIPLY)).collect(Collectors.toList());
        for (String s2 : list) {
            if (NumberUtils.isParsable(s2)) {
                BigDecimal b1 = new BigDecimal(s2);
                b2 = b2.multiply(b1);
            } else {
                params.add(s2);
            }
        }
        if (CollectionUtils.isNotEmpty(params)) {
            StringBuilder builder = new StringBuilder();
            if (b2.compareTo(BigDecimal.ONE) == 0) {
                for (String param : params) {
                    builder.append(param).append(MULTIPLY);
                }
            } else {
                builder.append(b2.setScale(PRECISION, RoundingMode.FLOOR).toString()).append(MULTIPLY);
                for (String param : params) {
                    builder.append(param).append(MULTIPLY);
                }
            }
            builder.delete(builder.lastIndexOf(MULTIPLY), builder.length());
            return builder.toString();
        } else {
            return b2.setScale(PRECISION, RoundingMode.FLOOR).toString();
        }
    }


    private static List<String> splitParameter(String src) {
        List<String> list = Arrays.stream(src.split(EMPTY)).collect(Collectors.toList());
        List<String> result = Lists.newArrayList();
        String item = null;
        for (int i = 0; i < list.size(); i++) {
            String s = list.get(i);
            if (Objects.equals(PARAM_PREFIX, s)) {
                if (StringUtils.isNotBlank(item)) {
                    result.add(item);
                }
                item = s;
            } else if (Objects.equals(MULTIPLY, s) || Objects.equals(ADD, s) || Objects.equals(SUBTRACTION, s)) {
                continue;
            } else {
                if (StringUtils.isNotBlank(item))
                    item += s;
            }
            if (i == list.size() - 1) {
                if (StringUtils.isNotBlank(item)) {
                    if (item.indexOf(RIGHT_PARENTHESIS) != -1) {
                        item = item.replace(RIGHT_PARENTHESIS, EMPTY);
                    }
                    result.add(item);
                }
            }
        }
        return result;
    }
}
