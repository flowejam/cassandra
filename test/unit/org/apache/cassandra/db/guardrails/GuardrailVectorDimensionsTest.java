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

package org.apache.cassandra.db.guardrails;

import java.util.List;
import java.util.function.Supplier;

import org.junit.Test;

import static java.lang.String.format;

/**
 * Tests the guardrail for the number of dimensions of a vector, {@link Guardrails#vectorDimensions}.
 */
public class GuardrailVectorDimensionsTest extends ThresholdTester
{
    private static final int WARN_THRESHOLD = 2;
    private static final int FAIL_THRESHOLD = 4;

    public GuardrailVectorDimensionsTest()
    {
        super(WARN_THRESHOLD,
              FAIL_THRESHOLD,
              Guardrails.vectorDimensions,
              Guardrails::setVectorDimensionsThreshold,
              Guardrails::getVectorDimensionsWarnThreshold,
              Guardrails::getVectorDimensionsFailThreshold);
    }

    static final List<String> CREATE_TABLE_CQL = List.of(
        // different poisitions (partition key, clustering key, static column, regular column)
        "CREATE TABLE %s (v vector<int, %%d> PRIMARY KEY)",
        "CREATE TABLE %s (v vector<int, %%d>, c int, PRIMARY KEY(v, c))",
        "CREATE TABLE %s (v vector<int, %%d>, c int, PRIMARY KEY((v, c)))",
        "CREATE TABLE %s (k int, v vector<int, %%d>, PRIMARY KEY(k, v))",
        "CREATE TABLE %s (k int, c int, v vector<int, %%d>, PRIMARY KEY(k, c, v))",
        "CREATE TABLE %s (k int, c int, v vector<int, %%d> static, PRIMARY KEY(k, c))",
        "CREATE TABLE %s (k int PRIMARY KEY, v vector<int, %%d>)",

        // multivalued data types
        "CREATE TABLE %s (k int PRIMARY KEY, v list<vector<int, %%d>>)",
        "CREATE TABLE %s (k int PRIMARY KEY, v set<vector<int, %%d>>)",
        "CREATE TABLE %s (k int PRIMARY KEY, v map<int, vector<int, %%d>>)",
        "CREATE TABLE %s (k int PRIMARY KEY, v map<vector<int, %%d>, int>)",
        "CREATE TABLE %s (k int PRIMARY KEY, v tuple<vector<int, %%d>, int>)",
        "CREATE TABLE %s (k int PRIMARY KEY, v tuple<int, vector<int, %%d>>)",

        // nested multivalued data types
        "CREATE TABLE %s (k int PRIMARY KEY, v set<frozen<list<vector<int, %%d>>>>)",
        "CREATE TABLE %s (k int PRIMARY KEY, v list<frozen<set<vector<int, %%d>>>>)",
        "CREATE TABLE %s (k int PRIMARY KEY, v map<int, frozen<set<vector<int, %%d>>>>)",
        "CREATE TABLE %s (k int PRIMARY KEY, v map<frozen<set<vector<int, %%d>>>, int>)",
        "CREATE TABLE %s (k int PRIMARY KEY, v tuple<frozen<tuple<vector<int, %%d>, int>>, int>)"
    );

    @Test
    public void testCreateTable() throws Throwable
    {
        for (String cql : CREATE_TABLE_CQL)
            testCreateTable(cql);
    }

    private void testCreateTable(String query) throws Throwable
    {
        testColumn(() -> format(query, createTableName()));
    }

    static final List<String> CREATE_TYPE_CQL = List.of(
        "CREATE TYPE %s (c int, v vector<int, %%d>)",
        "CREATE TYPE %s (c int, v list<vector<int, %%d>>)",
        "CREATE TYPE %s (c int, v set<vector<int, %%d>>)",
        "CREATE TYPE %s (c int, v map<int, vector<int, %%d>>)",
        "CREATE TYPE %s (c int, v map<vector<int, %%d>, int>)",
        "CREATE TYPE %s (c int, v tuple<vector<int, %%d>, int>)",
        "CREATE TYPE %s (c int, v tuple<int, vector<int, %%d>>)"
    );

    @Test
    public void testCreateType() throws Throwable
    {
        for (String cql : CREATE_TYPE_CQL)
            testCreateType(cql);;
    }

    private void testCreateType(String query) throws Throwable
    {
        testField(() -> format(query, createTypeName()));
    }

    static final List<String> ALTER_TABLE_CQL = List.of(
        "ALTER TABLE %s ADD v vector<int, %%d>",
        "ALTER TABLE %s ADD v list<vector<int, %%d>>",
        "ALTER TABLE %s ADD v set<vector<int, %%d>>",
        "ALTER TABLE %s ADD v map<int, vector<int, %%d>>",
        "ALTER TABLE %s ADD v map<vector<int, %%d>, int>",
        "ALTER TABLE %s ADD v tuple<vector<int, %%d>, int>",
        "ALTER TABLE %s ADD v tuple<int, vector<int, %%d>>"
    );

    @Test
    public void testAlterTable() throws Throwable
    {
        for (String cql : ALTER_TABLE_CQL)
            testAlterTable(cql);
    }

    private void testAlterTable(String query) throws Throwable
    {
        testColumn(() -> {
            createTable("CREATE TABLE %s (k int PRIMARY KEY)");
            return format(query, currentTable());
        });
    }

    static final List<String> ALTER_TYPE_CQL = List.of(
        "ALTER TYPE %s ADD v vector<int, %%d>",
        "ALTER TYPE %s ADD v list<vector<int, %%d>>",
        "ALTER TYPE %s ADD v set<vector<int, %%d>>",
        "ALTER TYPE %s ADD v map<int, vector<int, %%d>>",
        "ALTER TYPE %s ADD v map<vector<int, %%d>, int>",
        "ALTER TYPE %s ADD v tuple<vector<int, %%d>, int>",
        "ALTER TYPE %s ADD v tuple<int, vector<int, %%d>>"
    );

    @Test
    public void testAlterType() throws Throwable
    {
        for (String cql : ALTER_TYPE_CQL)
            testAlterType(cql);
    }

    private void testAlterType(String query) throws Throwable
    {
        testField(() -> {
            String name = createType("CREATE TYPE %s (c int)");
            return format(query, name);
        });
    }

    @Test
    public void testExcludedUsers() throws Throwable
    {
        testExcludedUsers(() -> format("CREATE TABLE %s (k int PRIMARY KEY, v vector<int, 1000>)", createTableName()),
                          () -> format("CREATE TYPE %s (c int, v vector<int, 1000>)", createTypeName()),
                          () -> format("ALTER TABLE %s ADD v vector<int, 1000>",
                                       createTable("CREATE TABLE %s (k int PRIMARY KEY)")),
                          () -> format("ALTER TYPE %s ADD v vector<int, 1000>",
                                       createType("CREATE TYPE %s (c int)")));
    }

    private void testColumn(Supplier<String> query) throws Throwable
    {
        testGuardrail(query, "Column v");
    }

    private void testField(Supplier<String> query) throws Throwable
    {
        testGuardrail(query, "Field v");
    }

    private void testGuardrail(Supplier<String> query, String element) throws Throwable
    {
        assertValid(query.get(), 1);
        assertValid(query.get(), WARN_THRESHOLD);
        assertWarns(query.get(), element, WARN_THRESHOLD + 1);
        assertWarns(query.get(), element, FAIL_THRESHOLD);
        assertFails(query.get(), element, FAIL_THRESHOLD + 1);
        assertFails(query.get(), element, Integer.MAX_VALUE);
    }

    private void assertValid(String query, int dimensions) throws Throwable
    {
        super.assertValid(format(query, dimensions));
    }

    private void assertWarns(String query, String what, int dimensions) throws Throwable
    {
        assertWarns(format(query, dimensions),
                    format(what + " has a vector of %s dimensions, this exceeds the warning threshold of %s.",
                           dimensions, WARN_THRESHOLD));
    }

    private void assertFails(String query, String what, int dimensions) throws Throwable
    {
        assertFails(format(query, dimensions),
                    format(what + " has a vector of %s dimensions, this exceeds the failure threshold of %s.",
                           dimensions, FAIL_THRESHOLD));
    }
}
