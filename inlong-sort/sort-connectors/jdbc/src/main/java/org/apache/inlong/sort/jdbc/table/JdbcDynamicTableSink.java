/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sort.jdbc.table;

import org.apache.flink.annotation.Internal;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.connector.jdbc.JdbcExecutionOptions;
import org.apache.flink.connector.jdbc.internal.options.JdbcDmlOptions;
import org.apache.flink.connector.jdbc.internal.options.JdbcOptions;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.connector.ChangelogMode;
import org.apache.flink.table.connector.sink.DynamicTableSink;
import org.apache.flink.table.connector.sink.SinkFunctionProvider;
import org.apache.flink.table.data.RowData;
import org.apache.flink.types.RowKind;
import org.apache.inlong.sort.jdbc.internal.GenericJdbcSinkFunction;

import java.util.Objects;

import static org.apache.flink.util.Preconditions.checkState;

/**
 * Copy from org.apache.flink:flink-connector-jdbc_2.11:1.13.5
 *
 * A {@link DynamicTableSink} for JDBC.
 * Add an option `sink.ignore.changelog` to support insert-only mode without primaryKey.
 */
@Internal
public class JdbcDynamicTableSink implements DynamicTableSink {

    private final JdbcOptions jdbcOptions;
    private final JdbcExecutionOptions executionOptions;
    private final JdbcDmlOptions dmlOptions;
    private final TableSchema tableSchema;
    private final String dialectName;

    private final String inLongMetric;
    private final boolean appendMode;

    public JdbcDynamicTableSink(
            JdbcOptions jdbcOptions,
            JdbcExecutionOptions executionOptions,
            JdbcDmlOptions dmlOptions,
            TableSchema tableSchema,
            boolean appendMode,
            String inLongMetric) {
        this.jdbcOptions = jdbcOptions;
        this.executionOptions = executionOptions;
        this.dmlOptions = dmlOptions;
        this.tableSchema = tableSchema;
        this.dialectName = dmlOptions.getDialect().dialectName();
        this.appendMode = appendMode;
        this.inLongMetric = inLongMetric;
    }

    @Override
    public ChangelogMode getChangelogMode(ChangelogMode requestedMode) {
        validatePrimaryKey(requestedMode);
        return ChangelogMode.newBuilder()
                .addContainedKind(RowKind.INSERT)
                .addContainedKind(RowKind.DELETE)
                .addContainedKind(RowKind.UPDATE_AFTER)
                .build();
    }

    private void validatePrimaryKey(ChangelogMode requestedMode) {
        checkState(
                ChangelogMode.insertOnly().equals(requestedMode)
                        || dmlOptions.getKeyFields().isPresent() || appendMode,
                "please declare primary key or appendMode for sink table when query contains update/delete record.");
    }

    @Override
    public SinkRuntimeProvider getSinkRuntimeProvider(Context context) {
        final TypeInformation<RowData> rowDataTypeInformation =
                context.createTypeInformation(tableSchema.toRowDataType());
        final JdbcDynamicOutputFormatBuilder builder = new JdbcDynamicOutputFormatBuilder();

        builder.setAppendMode(appendMode);
        builder.setJdbcOptions(jdbcOptions);
        builder.setJdbcDmlOptions(dmlOptions);
        builder.setJdbcExecutionOptions(executionOptions);
        builder.setRowDataTypeInfo(rowDataTypeInformation);
        builder.setFieldDataTypes(tableSchema.getFieldDataTypes());
        builder.setInLongMetric(inLongMetric);
        return SinkFunctionProvider.of(
                new GenericJdbcSinkFunction<>(builder.build()), jdbcOptions.getParallelism());
    }

    @Override
    public DynamicTableSink copy() {
        return new JdbcDynamicTableSink(jdbcOptions, executionOptions, dmlOptions,
                tableSchema, appendMode, inLongMetric);
    }

    @Override
    public String asSummaryString() {
        return "JDBC:" + dialectName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof JdbcDynamicTableSink)) {
            return false;
        }
        JdbcDynamicTableSink that = (JdbcDynamicTableSink) o;
        return Objects.equals(jdbcOptions, that.jdbcOptions)
                && Objects.equals(executionOptions, that.executionOptions)
                && Objects.equals(dmlOptions, that.dmlOptions)
                && Objects.equals(tableSchema, that.tableSchema)
                && Objects.equals(dialectName, that.dialectName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jdbcOptions, executionOptions, dmlOptions, tableSchema, dialectName);
    }
}