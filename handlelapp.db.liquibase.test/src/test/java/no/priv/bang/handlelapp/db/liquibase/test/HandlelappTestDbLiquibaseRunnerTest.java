/*
 * Copyright 2024 Steinar Bang
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations
 * under the License.
 */
package no.priv.bang.handlelapp.db.liquibase.test;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import java.sql.SQLException;
import java.util.Properties;

import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.ops4j.pax.jdbc.derby.impl.DerbyDataSourceFactory;
import org.osgi.service.jdbc.DataSourceFactory;

class HandlelappTestDbLiquibaseRunnerTest {

    @Test
    void testCreateAndVerifySomeDataInSomeTables() throws Exception {
        var datasource = createDataSource("handlelapp");

        var runner = new HandlelappTestDbLiquibaseRunner();
        runner.activate();
        runner.prepare(datasource);
        assertAccounts(datasource);
    }

    @Test
    void testFailInGettingConnectionWhenCreatingInitialSchema() throws Exception {
        var datasource = mock(DataSource.class);
        when(datasource.getConnection()).thenThrow(new SQLException("Failed to get connection"));

        var runner = new HandlelappTestDbLiquibaseRunner();
        runner.activate();
        var e = assertThrows(
            SQLException.class,
            () -> runner.prepare(datasource));
        assertThat(e.getMessage()).startsWith("Failed to get connection");
    }

    @Test
    void testFailWhenCreatingInitialSchema() throws Exception {
        var connection = spy(createDataSource("handlelapp1").getConnection());
        var datasource = mock(DataSource.class);
        when(datasource.getConnection()).thenReturn(connection);

        var runner = new HandlelappTestDbLiquibaseRunner();
        runner.activate();
        var e = assertThrows(
            SQLException.class,
            () -> runner.prepare(datasource));
        assertThat(e.getMessage()).startsWith("Error creating handlelapp test database schema");
    }

    @Test
    void testFailWhenAddingMockData() throws Exception {
        var connection = spy(createDataSource("handlelapp1").getConnection());
        var datasource = spy(createDataSource("handlelapp2"));
        when(datasource.getConnection())
            .thenCallRealMethod()
            .thenReturn(connection);

        var runner = new HandlelappTestDbLiquibaseRunner();
        runner.activate();
        var e = assertThrows(
            SQLException.class,
            () -> runner.prepare(datasource));
        assertThat(e.getMessage()).startsWith("Error inserting handlelapp test database mock data");
    }

    @Test
    void testFailWhenGettingConnectionForUpdatingSchema() throws Exception {
        var datasource = spy(createDataSource("handlelapp3"));
        when(datasource.getConnection())
            .thenCallRealMethod()
            .thenCallRealMethod()
            .thenThrow(new SQLException("Failed to get connection"));

        var runner = new HandlelappTestDbLiquibaseRunner();
        runner.activate();
        var e = assertThrows(
            SQLException.class,
            () -> runner.prepare(datasource));
        assertThat(e.getMessage()).startsWith("Failed to get connection");
    }

    @Test
    void testFailWhenUpdatingSchema() throws Exception {
        var connection = spy(createDataSource("handlelapp4").getConnection());
        var datasource = spy(createDataSource("handlelapp4"));
        when(datasource.getConnection())
            .thenCallRealMethod()
            .thenCallRealMethod()
            .thenReturn(connection);

        var runner = new HandlelappTestDbLiquibaseRunner();
        runner.activate();
        var e = assertThrows(
            SQLException.class,
            () -> runner.prepare(datasource));
        assertThat(e.getMessage()).startsWith("Error updating handlelapp test database schema");
    }

    private void assertAccounts(DataSource datasource) throws Exception {
        int resultcount = 0;
        try (var connection = datasource.getConnection()) {
            try(var statement = connection.prepareStatement("select * from handlelapp_accounts")) {
                try (var results = statement.executeQuery()) {
                    while (results.next()) {
                        ++resultcount;
                    }
                }
            }
        }
        assertEquals(0, resultcount);
    }

    private DataSource createDataSource(String dbname) throws SQLException {
        var dataSourceFactory = new DerbyDataSourceFactory();
        var properties = new Properties();
        properties.setProperty(DataSourceFactory.JDBC_URL, "jdbc:derby:memory:" + dbname + ";create=true");
        return dataSourceFactory.createDataSource(properties);
    }

}
