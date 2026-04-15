package org.example.myextension.extension;


import cn.hutool.crypto.digest.MD5;
import com.alibaba.fastjson2.JSON;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.pagehelper.PageInfo;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.example.myextension.exception.BizException;
import org.example.myextension.utils.SpringContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;

/**
 * 基础扩展工具类
 *
 * 汇集项目中常用的工具方法：JSON mapper 配置、日志获取、Bean 属性拷贝、金额单位转换、PageInfo 转换等。
 * 所有方法均为无状态静态方法，便于在各种上下文中复用。
 */
public class BaseExtensions {

    // 获取配置好的ObjectMapper
    public static ObjectMapper jsonMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setTimeZone(TimeZone.getTimeZone("GMT+8:00"));
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

    // 获取日志对象
    public static <T> Logger logger(Class<T> clazz) {
        return LoggerFactory.getLogger(clazz);
    }

    // Boolean.then 扩展
    public static boolean then(boolean condition, Runnable block) {
        if (condition) {
            block.run();
        }
        return condition;
    }

    // Boolean.otherwise 扩展
    public static boolean otherwise(boolean condition, Runnable block) {
        if (!condition) {
            block.run();
        }
        return condition;
    }

    // 参数MD5加密
    @SafeVarargs
    public static <T> String paramHex(T... elements) {
        if (elements == null || elements.length == 0) {
            return null;
        }
        return MD5.create().digestHex(JSON.toJSONString(elements));
    }

    // 获取自身类型的Spring Bean
    public static <T> T selfBean(T obj) {
        return SpringContextUtil.getApplicationContext().getBean((Class<T>) obj.getClass());
    }

    // 根据Class获取Spring Bean
    public static <T> T getBean(Class<T> clazz) {
        return SpringContextUtil.getApplicationContext().getBean(clazz);
    }

    // 获取Cookie中的kdtId（非空）
    public static long getCookieKdtId(HttpServletRequest request) {
        Long kdtId = getCookieKdtIdNullable(request);
        if (kdtId == null) {
            throw new BizException("无法获取当前店铺信息");
        }
        return kdtId;
    }

    // 获取Cookie中的kdtId（可空）
    public static Long getCookieKdtIdNullable(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        return Arrays.stream(request.getCookies())
                .filter(cookie -> "kdt_id".equals(cookie.getName()))
                .findFirst()
                .map(cookie -> {
                    try {
                        return Long.parseLong(cookie.getValue());
                    } catch (NumberFormatException e) {
                        return null;
                    }
                })
                .orElse(null);
    }

    // 获取客户端IP
    public static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            int index = ip.indexOf(',');
            return index != -1 ? ip.substring(0, index) : ip;
        }

        return request.getRemoteAddr();
    }

    // 对象属性拷贝
    public static <T, R> R mapType(T source, Class<R> targetClass, String... ignores) {
        if (source == null) {
            return null;
        }
        try {
            R target = targetClass.getDeclaredConstructor().newInstance();
            BeanUtils.copyProperties(source, target, ignores);
            return target;
        } catch (Exception e) {
            throw new RuntimeException("对象转换失败", e);
        }
    }

    // 集合对象属性拷贝
    public static <T, R> List<R> mapTypeList(List<T> sourceList, Class<R> targetClass) {
        return sourceList.stream()
                .map(item -> mapType(item, targetClass))
                .collect(Collectors.toList());
    }

    // 分转元
    public static BigDecimal fen2Yuan(Long fen) {
        if (fen == null) {
            return null;
        }
        BigDecimal oneHundred = new BigDecimal(100);
        return new BigDecimal(fen).divide(oneHundred, 2, RoundingMode.HALF_UP);
    }

    // 元转分
    public static Long yuan2fen(BigDecimal yuan) {
        if (yuan == null) {
            return null;
        }
        BigDecimal oneHundred = new BigDecimal(100);
        return yuan.multiply(oneHundred).longValue();
    }

    // PageInfo转换
    public static <E, T> PageInfo<T> convert(PageInfo<E> sourcePage, java.util.function.Function<E, T> converter) {
        List<T> targetList = sourcePage.getList().stream()
                .map(converter)
                .collect(Collectors.toList());

        PageInfo<T> targetPage = new PageInfo<>(targetList);
        targetPage.setPageNum(sourcePage.getPageNum());
        targetPage.setPageSize(sourcePage.getPageSize());
        targetPage.setStartRow(sourcePage.getStartRow());
        targetPage.setEndRow(sourcePage.getEndRow());
        targetPage.setTotal(sourcePage.getTotal());
        targetPage.setPages(sourcePage.getPages());
        return targetPage;
    }
}