/*
 * Copyright 2021 Steinar Bang
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
package no.priv.bang.handlelapp.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class LoginresultatTest {

    @Test
    void testCreate() {
        boolean suksess = true;
        boolean authorized = true;
        String feilmelding = "Feil passord";
        String originalRequestUrl = "http://localhost:8181/handlelapp/hurtigregistrering";
        String username = "jod";
        Loginresult bean = Loginresult.with()
            .suksess(suksess)
            .feilmelding(feilmelding)
            .authorized(authorized)
            .username(username)
            .originalRequestUrl(originalRequestUrl)
            .build();
        assertTrue(bean.getSuksess());
        assertEquals(feilmelding, bean.getFeilmelding());
        assertTrue(bean.isAuthorized());
        assertEquals(username, bean.getUsername());
        assertEquals(originalRequestUrl, bean.getOriginalRequestUrl());
    }

    @Test
    void testNoargsConstructor() {
        Loginresult bean = Loginresult.with().build();
        assertFalse(bean.getSuksess());
        assertNull(bean.getFeilmelding());
        assertFalse(bean.isAuthorized());
        assertNull(bean.getOriginalRequestUrl());
    }

    @Test
    void testToString() {
        Loginresult bean = Loginresult.with().build();
        assertThat(bean.toString()).startsWith("Loginresultat [");
    }

}
