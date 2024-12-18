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
package no.priv.bang.handlelapp.backend;

import static no.priv.bang.handlelapp.services.HandlelappConstants.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import javax.sql.DataSource;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.log.LogService;
import org.osgi.service.log.Logger;

import no.priv.bang.handlelapp.services.HandlelappService;
import no.priv.bang.handlelapp.services.beans.Account;
import no.priv.bang.handlelapp.services.beans.CounterBean;
import no.priv.bang.handlelapp.services.beans.CounterIncrementStepBean;
import no.priv.bang.handlelapp.services.beans.LocaleBean;
import no.priv.bang.osgiservice.users.Role;
import no.priv.bang.osgiservice.users.UserManagementService;

@Component(service=HandlelappService.class, immediate=true, property= { "defaultlocale=nb_NO" })
public class HandlelappServiceProvider implements HandlelappService {

    private static final String DISPLAY_TEXT_RESOURCES = "i18n.Texts";
    private Logger logger;
    private DataSource datasource;
    private UserManagementService useradmin;
    private Locale defaultLocale;

    @Reference
    public void setLogservice(LogService logservice) {
        this.logger = logservice.getLogger(HandlelappServiceProvider.class);
    }

    @Reference(target = "(osgi.jndi.service.name=jdbc/handlelapp)")
    public void setDatasource(DataSource datasource) {
        this.datasource = datasource;
    }

    @Reference
    public void setUseradmin(UserManagementService useradmin) {
        this.useradmin = useradmin;
    }

    @Activate
    public void activate(Map<String, Object> config) {
        defaultLocale = Locale.forLanguageTag(((String) config.get("defaultlocale")).replace('_', '-'));
        addRolesIfNotpresent();
    }

    @Override
    public boolean lazilyCreateAccount(String username) {
        try(var connection = datasource.getConnection()) {
            var accountid = findAccount(connection, username);

            if (accountid != -1) {
                return false;
            }

            try(var createAccount = connection.prepareStatement("insert into handlelapp_accounts (username) values (?)")) {
                createAccount.setString(1, username);
                createAccount.executeUpdate();
            }

            accountid = findAccount(connection, username);
            try(var createIncrementStep = connection.prepareStatement("insert into counter_increment_steps (account_id) values (?)")) {
                createIncrementStep.setInt(1, accountid);
                createIncrementStep.executeUpdate();
            }

            try(var createCounter = connection.prepareStatement("insert into counters (account_id) values (?)")) {
                createCounter.setInt(1, accountid);
                createCounter.executeUpdate();
            }

            return true;
        } catch (SQLException e) {
            logger.warn("Failed to create handlelapp account for username \"{}\"", username, e);
        }

        return false;
    }

    @Override
    public List<Account> getAccounts() {
        var accounts = new ArrayList<Account>();
        try(var connection = datasource.getConnection()) {
            try(var statement = connection.createStatement()) {
                try(var results = statement.executeQuery("select account_id, username from handlelapp_accounts")) {
                    while(results.next()) {
                        var accountId = results.getInt("account_id");
                        var username = results.getString("username");
                        var user = useradmin.getUser(username);
                        var account = Account.with().accountId(accountId).user(user).build();
                        accounts.add(account);
                    }
                }
            }

        } catch (SQLException e) {
            logger.error("Ingen handlelapp", e);
        }

        return accounts;
    }

    @Override
    public Optional<CounterIncrementStepBean> getCounterIncrementStep(String username) {
        try(var connection = datasource.getConnection()) {
            var counterIncrementStep = findCounterIncrementStep(connection, username);
            if (counterIncrementStep != null) {
                var bean = CounterIncrementStepBean.with()
                    .username(username)
                    .counterIncrementStep(counterIncrementStep)
                    .build();
                return Optional.of(bean);
            }
        } catch (SQLException e) {
            logger.error("No increment steps could be found for user \"{}\"", username, e);
        }

        return Optional.empty();
    }

    @Override
    public Optional<CounterIncrementStepBean> updateCounterIncrementStep(CounterIncrementStepBean updatedIncrementStep) {
        var username = updatedIncrementStep.username();
        try(var connection = datasource.getConnection()) {
            try(var statement = connection.prepareStatement("update counter_increment_steps set counter_increment_step=? where account_id in (select account_id from handlelapp_accounts where username=?)")) {
                statement.setInt(1, updatedIncrementStep.counterIncrementStep());
                statement.setString(2, username);
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            logger.error("Unable to update increment step for user \"{}\"", username, e);
            return Optional.empty();
        }

        return getCounterIncrementStep(username);
    }

    @Override
    public Optional<CounterBean> getCounter(String username) {
        try(var connection = datasource.getConnection()) {
            return findAndCreateCounterBean(connection, username);
        } catch (SQLException e) {
            logger.error("No counter could be found for user \"{}\"", username, e);
        }

        return Optional.empty();
    }

    @Override
    public Optional<CounterBean> incrementCounter(String username) {
        try(var connection = datasource.getConnection()) {
            var incrementStep = findCounterIncrementStep(connection, username);
            var counter = findCounter(connection, username);

            try(var statement = connection.prepareStatement("update counters set counter=? where account_id in (select account_id from handlelapp_accounts where username=?)")) {
                statement.setInt(1, counter + incrementStep);
                statement.setString(2, username);
                statement.executeUpdate();
            }

            return findAndCreateCounterBean(connection, username);
        } catch (SQLException e) {
            logger.warn("Failed to increment counter for user \"{}\"", username, e);
        }

        return Optional.empty();
    }

    @Override
    public Optional<CounterBean> decrementCounter(String username) {
        try(var connection = datasource.getConnection()) {
            var incrementStep = findCounterIncrementStep(connection, username);
            var counter = findCounter(connection, username);

            try(var statement = connection.prepareStatement("update counters set counter=? where account_id in (select account_id from handlelapp_accounts where username=?)")) {
                statement.setInt(1, counter - incrementStep);
                statement.setString(2, username);
                statement.executeUpdate();
            }

            return findAndCreateCounterBean(connection, username);
        } catch (SQLException e) {
            logger.warn("Failed to decrement counter for user \"{}\"", username, e);
        }

        return Optional.empty();
    }

    @Override
    public Locale defaultLocale() {
        return defaultLocale;
    }

    @Override
    public List<LocaleBean> availableLocales() {
        return Arrays.asList(Locale.forLanguageTag("nb-NO"), Locale.UK).stream().map(l -> LocaleBean.with().locale(l).build()).toList();
    }

    @Override
    public Map<String, String> displayTexts(Locale locale) {
        return transformResourceBundleToMap(locale);
    }

    @Override
    public String displayText(String key, String locale) {
        var active = locale == null || locale.isEmpty() ? defaultLocale : Locale.forLanguageTag(locale.replace('_', '-'));
        var bundle = ResourceBundle.getBundle(DISPLAY_TEXT_RESOURCES, active);
        return bundle.getString(key);
    }

    private int findAccount(Connection connection, String username) throws SQLException {
        try(var findAccount = connection.prepareStatement("select account_id from handlelapp_accounts where username=?")) {
            findAccount.setString(1, username);
            try(var results = findAccount.executeQuery()) {
                while (results.next()) {
                    return results.getInt("account_id");
                }
            }
        }

        return -1;
    }

    private Integer findCounterIncrementStep(Connection connection, String username) throws SQLException {
        try(var statement = connection.prepareStatement("select counter_increment_step from counter_increment_steps c join handlelapp_accounts a on c.account_id=a.account_id where a.username=?")) {
            statement.setString(1, username);
            try(var results = statement.executeQuery()) {
                while(results.next()) {
                    return results.getInt("counter_increment_step");
                }
            }
        }

        return null;
    }

    private Integer findCounter(Connection connection, String username) throws SQLException {
        try(var statement = connection.prepareStatement("select counter from counters c join handlelapp_accounts a on c.account_id=a.account_id where a.username=?")) {
            statement.setString(1, username);
            try(var results = statement.executeQuery()) {
                while(results.next()) {
                    return results.getInt("counter");
                }
            }
        }

        return null;
    }

    private Optional<CounterBean> findAndCreateCounterBean(Connection connection, String username) throws SQLException {
        var counter = findCounter(connection, username);
        return counter != null ?
            Optional.of(CounterBean.with().counter(counter).build()) :
            Optional.empty();
    }

    private void addRolesIfNotpresent() {
        var handlelapproles = Map.of(HANDLELAPPUSER_ROLE, "Bruker av applikasjonen handlelapp");
        var existingroles = useradmin.getRoles().stream().map(Role::rolename).collect(Collectors.toSet());
        handlelapproles.entrySet().stream()
            .filter(r -> !existingroles.contains(r.getKey()))
            .forEach(r ->  useradmin.addRole(Role.with().id(-1).rolename(r.getKey()).description(r.getValue()).build()));
    }

    Map<String, String> transformResourceBundleToMap(Locale locale) {
        var map = new HashMap<String, String>();
        var bundle = ResourceBundle.getBundle(DISPLAY_TEXT_RESOURCES, locale);
        var keys = bundle.getKeys();
        while(keys.hasMoreElements()) {
            String key = keys.nextElement();
            map.put(key, bundle.getString(key));
        }

        return map;
    }

}
