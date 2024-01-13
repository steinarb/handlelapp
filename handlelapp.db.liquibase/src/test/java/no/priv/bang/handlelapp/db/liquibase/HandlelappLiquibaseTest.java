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
package no.priv.bang.handlelapp.db.liquibase;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.ops4j.pax.jdbc.derby.impl.DerbyDataSourceFactory;
import org.osgi.service.jdbc.DataSourceFactory;

import liquibase.exception.LiquibaseException;

class HandlelappLiquibaseTest {
    DataSourceFactory derbyDataSourceFactory = new DerbyDataSourceFactory();

    @Test
    void testCreateSchema() throws Exception {
        HandlelappLiquibase handlelappLiquibase = new HandlelappLiquibase();

        handlelappLiquibase.createInitialSchema(createConnection("handlelapp"));

        try(var connection = createConnection("handlelapp")) {
            addAccounts(connection);
            assertAccounts(connection);
            addCounterIncrementSteps(connection);
            assertCounterIncrementSteps(connection);
            int accountIdNotMatchingAccount = 375;
            assertThrows(SQLException.class,() -> addCounterIncrementStep(connection, accountIdNotMatchingAccount, 10));
            addCounters(connection);
            assertCounters(connection);
            assertThrows(SQLException.class,() -> addCounter(connection, accountIdNotMatchingAccount, 4));
        }

        handlelappLiquibase.updateSchema(createConnection("handlelapp"));
    }

    @Test
    void testCreateSchemaAndFail() throws Exception {
        var connection = spy(createConnection("handlelapp1"));
        // A Derby JDBC connection wrapped in a Mockito spy() fails om Connection.setAutoClosable()

        var handlelappLiquibase = new HandlelappLiquibase();

        var ex = assertThrows(
            LiquibaseException.class,
            () -> handlelappLiquibase.createInitialSchema(connection));
        assertThat(ex.getMessage()).startsWith("java.sql.SQLException: Cannot set Autocommit On when in a nested connection");
    }

    @Test
    void testCreateSchemaAndFailOnConnectionClose() throws Exception {
        var connection = spy(createConnection("handlelapp2"));
        doNothing().when(connection).setAutoCommit(anyBoolean());
        doThrow(Exception.class).when(connection).close();

        var handlelappLiquibase = new HandlelappLiquibase();

        var ex = assertThrows(
            LiquibaseException.class,
            () -> handlelappLiquibase.createInitialSchema(connection));
        assertThat(ex.getMessage()).startsWith("java.lang.Exception");
    }

    private void addAccounts(Connection connection) throws Exception {
        addAccount(connection, "admin");
    }

    private void assertAccounts(Connection connection) throws Exception {
        String sql = "select count(*) from handlelapp_accounts";
        try(PreparedStatement statement = connection.prepareStatement(sql)) {
            try(ResultSet results = statement.executeQuery()) {
                if (results.next()) {
                    int count = results.getInt(1);
                    assertEquals(1, count);
                }
            }
        }
    }

    private void addCounterIncrementSteps(Connection connection) throws Exception {
        addCounterIncrementStep(connection, findAccountId(connection, "admin"), 10);
    }

    private void assertCounterIncrementSteps(Connection connection) throws Exception {
        try(Statement statement = connection.createStatement()) {
            try(ResultSet results = statement.executeQuery("select * from counter_increment_steps")) {
                assertTrue(results.next());
                assertEquals(findAccountId(connection, "admin"), results.getInt(2));
                assertEquals(10, results.getInt(3));
            }
        }
    }

    private void addCounters(Connection connection) throws Exception {
        addCounter(connection, findAccountId(connection, "admin"), 3);
    }

    private void assertCounters(Connection connection) throws Exception {
        try(Statement statement = connection.createStatement()) {
            try(ResultSet results = statement.executeQuery("select * from counters")) {
                assertTrue(results.next());
                assertEquals(findAccountId(connection, "admin"), results.getInt(2));
                assertEquals(3, results.getInt(3));
            }
        }
    }

    private int addAccount(Connection connection, String username) throws Exception {
        String sql = "insert into handlelapp_accounts (username) values (?)";
        try(PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            statement.executeUpdate();
        }

        return findAccountId(connection, username);
    }

    private void addCounterIncrementStep(Connection connection, int accountid, int counterIncrementStep) throws Exception {
        String sql = "insert into counter_increment_steps (account_id, counter_increment_step) values (?, ?)";
        try(PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, accountid);
            statement.setInt(2, counterIncrementStep);
            statement.executeUpdate();
        }
    }

    private void addCounter(Connection connection, int accountid, int count) throws Exception {
        String sql = "insert into counters (account_id, counter) values (?, ?)";
        try(PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, accountid);
            statement.setInt(2, count);
            statement.executeUpdate();
        }
    }

    private int findAccountId(Connection connection, String username) throws Exception {
        String sql = "select account_id from handlelapp_accounts where username=?";
        try(PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try(ResultSet results = statement.executeQuery()) {
                if (results.next()) {
                    return results.getInt(1);
                }
            }
        }

        return -1;
    }

    private Connection createConnection(String dbname) throws Exception {
        Properties properties = new Properties();
        properties.setProperty(DataSourceFactory.JDBC_URL, "jdbc:derby:memory:" + dbname + ";create=true");
        DataSource dataSource = derbyDataSourceFactory.createDataSource(properties);
        return dataSource.getConnection();
    }

}