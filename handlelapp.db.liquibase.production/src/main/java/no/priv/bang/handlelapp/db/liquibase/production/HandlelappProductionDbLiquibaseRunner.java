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
package no.priv.bang.handlelapp.db.liquibase.production;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;

import org.ops4j.pax.jdbc.hook.PreHook;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

import liquibase.exception.LiquibaseException;
import no.priv.bang.handlelapp.db.liquibase.HandlelappLiquibase;

@Component(immediate=true, property = "name=handlelappdb")
public class HandlelappProductionDbLiquibaseRunner implements PreHook {

    @Activate
    public void activate() {
        // Called after all injections have been satisfied and before the PreHook service is exposed
    }

    @Override
    public void prepare(DataSource datasource) throws SQLException {
        var handlelappLiquibase = new HandlelappLiquibase();
        try (var connect = datasource.getConnection()) {
            handlelappLiquibase.createInitialSchema(connect);
        } catch (SQLException e) {
            throw e;
        } catch (Exception e) {
            throw new SQLException("Failed to create schema in handlelapp PostgreSQL database", e);
        }

        try (var connect = datasource.getConnection()) {
            insertInitialData(connect, handlelappLiquibase);
        } catch (Exception e) {
            throw new SQLException("Failed to insert initial data into handlelapp PostgreSQL database", e);
        }

        try (var connect = datasource.getConnection()) {
            handlelappLiquibase.updateSchema(connect);
        } catch (SQLException e) {
            throw e;
        } catch (Exception e) {
            throw new SQLException("Failed to modify schema of handlelapp PostgreSQL database", e);
        }
    }

    public void insertInitialData(Connection connect, HandlelappLiquibase handlelappLiquibase) throws LiquibaseException {
        handlelappLiquibase.applyLiquibaseChangelist(connect, "sql/data/db-changelog.xml", getClass().getClassLoader());
    }

}
