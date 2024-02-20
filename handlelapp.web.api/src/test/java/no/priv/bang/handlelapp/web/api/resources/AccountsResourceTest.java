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
package no.priv.bang.handlelapp.web.api.resources;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import org.junit.jupiter.api.Test;

import no.priv.bang.handlelapp.services.HandlelappService;
import no.priv.bang.handlelapp.services.beans.Account;

class AccountsResourceTest {

    @Test
    void testGetAccounts() {
        var resource = new AccountsResource();
        var handlelapp = mock(HandlelappService.class);
        var account = Account.with().accountId(123).build();
        when(handlelapp.getAccounts()).thenReturn(Collections.singletonList(account));
        resource.handlelapp = handlelapp;
        var accounts = resource.getAccounts();
        assertNotNull(accounts);
        assertThat(accounts).isNotEmpty();
    }

}
