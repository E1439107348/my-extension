package org.example.myextension.extension;


import org.apache.commons.lang3.time.DateFormatUtils;
import org.mybatis.dynamic.sql.BindableColumn;
import org.mybatis.dynamic.sql.SqlColumn;
import org.mybatis.dynamic.sql.render.RenderingStrategy;
import org.mybatis.dynamic.sql.render.TableAliasCalculator;

import java.sql.JDBCType;
import java.util.Date;
import java.util.Optional;

/**
 * Mybatis 相关扩展工具类。
 * <p>
 * 1. 提供用于构造复合列的 BindableColumn 实现（PairColumn / TripleColumn），
 *    可在 SQL 中将多列视作一个可绑定的参数来渲染（例如用于复合索引/联合查询场景）。
 * 2. 提供参数类型自动转换方法，方便将 Java 值按照 JDBCType 进行合适的 SQL 占位符渲染。
 * 3. 提供自定义的 RenderingStrategy，用以生成自定义占位符格式（形如 ${prefix.param,jdbcType=...,typeHandler=...}）。
 */
public class MybatisExtensions {

    /**
     * 三元列（由三个 SqlColumn 组成），用于在 SQL 中将三个字段组合为一个可绑定参数。
     * @param <L> 第一个字段类型
     * @param <M> 第二个字段类型
     * @param <R> 第三个字段类型
     */
    public static class TripleColumn<L, M, R> implements BindableColumn<Triple<L, M, R>> {
        private final SqlColumn<L> column1;
        private final SqlColumn<M> column2;
        private final SqlColumn<R> column3;

        private TripleColumn(SqlColumn<L> column1, SqlColumn<M> column2, SqlColumn<R> column3) {
            this.column1 = column1;
            this.column2 = column2;
            this.column3 = column3;
        }

        /**
         * 构造器方法，简化实例化调用。
         *
         * @param column1 第一个列
         * @param column2 第二个列
         * @param column3 第三个列
         * @param <L> 类型参数 - 第一个列的类型
         * @param <M> 类型参数 - 第二个列的类型
         * @param <R> 类型参数 - 第三个列的类型
         * @return TripleColumn 实例
         */
        public static <L, M, R> TripleColumn<L, M, R> of(SqlColumn<L> column1, SqlColumn<M> column2, SqlColumn<R> column3) {
            return new TripleColumn<>(column1, column2, column3);
        }

        @Override
        public Optional<String> alias() {
            // 不提供别名支持，返回空
            return Optional.empty();
        }

        @Override
        public BindableColumn<Triple<L, M, R>> as(String alias) {
            // as 操作返回新的实例（但不实际使用 alias），保持行为与简单列一致
            return new TripleColumn<>(column1, column2, column3);
        }

        @Override
        public String renderWithTableAlias(TableAliasCalculator tableAliasCalculator) {
            // 在 SQL 中按 (col1,col2,col3) 的形式渲染列名
            return String.format("(%s,%s,%s)", column1.name(), column2.name(), column3.name());
        }

        @Override
        public String convertParameterType(Triple<L, M, R> value) {
            // 将 Triple 内部的每个值按各自列的 JDBCType 进行转换并拼接为字符串参数表达
            return String.format("(%s,%s,%s)",
                    MybatisExtensions.autoConvertParameterType(column1, value.getFirst()),
                    MybatisExtensions.autoConvertParameterType(column2, value.getSecond()),
                    MybatisExtensions.autoConvertParameterType(column3, value.getThird()));
        }

        @Override
        public Optional<RenderingStrategy> renderingStrategy() {
            // 使用自定义渲染策略以保证占位符格式符合项目约定
            return Optional.of(new OriginalRenderingStrategy());
        }
    }

    /**
     * 二元列（由两个 SqlColumn 组成），用途与 TripleColumn 类似但仅包含两个字段。
     */
    public static class PairColumn<L, R> implements BindableColumn<Pair<L, R>> {
        private final SqlColumn<L> column1;
        private final SqlColumn<R> column2;

        private PairColumn(SqlColumn<L> column1, SqlColumn<R> column2) {
            this.column1 = column1;
            this.column2 = column2;
        }

        /**
         * 构造器方法，简化实例化调用。
         *
         * @param column1 第一个列
         * @param column2 第二个列
         * @param <L> 类型参数 - 第一个列的类型
         * @param <R> 类型参数 - 第二个列的类型
         * @return PairColumn 实例
         */
        public static <L, R> PairColumn<L, R> of(SqlColumn<L> column1, SqlColumn<R> column2) {
            return new PairColumn<>(column1, column2);
        }

        @Override
        public Optional<String> alias() {
            return Optional.empty();
        }

        @Override
        public BindableColumn<Pair<L, R>> as(String alias) {
            return new PairColumn<>(column1, column2);
        }

        @Override
        public String renderWithTableAlias(TableAliasCalculator tableAliasCalculator) {
            return String.format("(%s,%s)", column1.name(), column2.name());
        }

        @Override
        public String convertParameterType(Pair<L, R> value) {
            return String.format("(%s,%s)",
                    MybatisExtensions.autoConvertParameterType(column1, value.getFirst()),
                    MybatisExtensions.autoConvertParameterType(column2, value.getSecond()));
        }

        @Override
        public Optional<RenderingStrategy> renderingStrategy() {
            return Optional.of(new OriginalRenderingStrategy());
        }
    }

    /**
     * 根据列的 JDBCType 自动将 Java 值转换为 SQL 占位符所需的字符串表现形式。
     * - 字符串类型会被单引号包裹
     * - DATE/TIME 类型使用指定格式化器
     * - 其他类型交由列自身的 convertParameterType 处理
     *
     * @param column 列信息（含 JDBCType/JavaType/typeHandler 等元数据）
     * @param value  需要转换的值
     * @param <T>    值的类型
     * @return 转换后的参数字符串或列自身 convertParameterType 的返回值
     */
    public static <T> Object autoConvertParameterType(SqlColumn<T> column, T value) {
        Optional<JDBCType> jdbcTypeOpt = column.jdbcType();
        if (!jdbcTypeOpt.isPresent()) {
            return column.convertParameterType(value);
        }

        JDBCType jdbcType = jdbcTypeOpt.get();
        switch (jdbcType) {
            case CHAR:
            case VARCHAR:
            case LONGVARCHAR:
                return "'" + value.toString() + "'";
            case DATE:
                return "'" + DateFormatUtils.format((Date) value, "yyyy-MM-dd HH:mm:ss") + "'";
            case TIME:
                return "'" + DateFormatUtils.format((Date) value, "HH:mm:ss") + "'";
            default:
                return column.convertParameterType(value);
        }
    }

    /**
     * 自定义 RenderingStrategy：
     * - 生成自定义占位符，例如 ${prefix.param,jdbcType=...,typeHandler=...}
     * - 用于在动态 SQL 渲染时保持占位符与项目约定一致
     */
    public static class OriginalRenderingStrategy extends RenderingStrategy {
        @Override
        public String getFormattedJdbcPlaceholder(String prefix, String parameterName) {
            return String.format("${%s.%s}", prefix, parameterName);
        }

        @Override
        public String getFormattedJdbcPlaceholder(BindableColumn<?> column, String prefix, String parameterName) {
            return String.format("${%s.%s%s%s%s}",
                    prefix,
                    parameterName,
                    renderJdbcType(column),
                    renderJavaType(column),
                    renderTypeHandler(column));
        }

        private String renderTypeHandler(BindableColumn<?> column) {
            return column.typeHandler()
                    .map(th -> ",typeHandler=" + th)
                    .orElse("");
        }

        private String renderJdbcType(BindableColumn<?> column) {
            return column.jdbcType()
                    .map(jt -> ",jdbcType=" + jt.getName())
                    .orElse("");
        }

        private String renderJavaType(BindableColumn<?> column) {
            // 注意：这里返回的字段名使用 jdbcType 名称以保持与上层渲染方法一致（与原实现保持兼容）
            return column.javaType()
                    .map(jt -> ",jdbcType=" + jt.getName())
                    .orElse("");
        }
    }

    /**
     * 简单的 Triple 辅助类（用于替代 Kotlin 的 Triple），只包含常用 getter。
     */
    public static class Triple<L, M, R> {
        private final L first;
        private final M second;
        private final R third;

        public Triple(L first, M second, R third) {
            this.first = first;
            this.second = second;
            this.third = third;
        }

        public L getFirst() {
            return first;
        }

        public M getSecond() {
            return second;
        }

        public R getThird() {
            return third;
        }
    }

    /**
     * 简单的 Pair 辅助类（用于替代 Kotlin 的 Pair）。
     */
    public static class Pair<L, R> {
        private final L first;
        private final R second;

        public Pair(L first, R second) {
            this.first = first;
            this.second = second;
        }

        public L getFirst() {
            return first;
        }

        public R getSecond() {
            return second;
        }
    }
}
