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
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.stream.Collectors;

import javax.ws.rs.WebApplicationException;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import no.priv.bang.osgi.service.mocks.logservice.MockLogService;
import no.priv.bang.handlelapp.services.HandlelappService;
import no.priv.bang.handlelapp.services.beans.LocaleBean;

class LocalizationResourceTest {
    private final static Locale NB_NO = Locale.forLanguageTag("nb-no");

    @Test
    void testDefaultLocale() {
        HandlelappService handlelapp = mock(HandlelappService.class);
        when(handlelapp.defaultLocale()).thenReturn(NB_NO);
        LocalizationResource resource = new LocalizationResource();
        resource.handlelapp = handlelapp;
        Locale defaultLocale = resource.defaultLocale();
        assertEquals(NB_NO, defaultLocale);
    }

    @Test
    void testAvailableLocales() {
        HandlelappService handlelapp = mock(HandlelappService.class);
        when(handlelapp.defaultLocale()).thenReturn(NB_NO);
        when(handlelapp.availableLocales()).thenReturn(Arrays.asList(Locale.forLanguageTag("nb-NO"), Locale.UK).stream().map(l -> LocaleBean.with().locale(l).build()).collect(Collectors.toList()));
        LocalizationResource resource = new LocalizationResource();
        resource.handlelapp = handlelapp;
        List<LocaleBean> availableLocales = resource.availableLocales();
        assertThat(availableLocales).isNotEmpty().contains(LocaleBean.with().locale(handlelapp.defaultLocale()).build());
    }

    @Test
    void testDisplayTextsForDefaultLocale() {
        HandlelappService handlelapp = mock(HandlelappService.class);
        when(handlelapp.defaultLocale()).thenReturn(NB_NO);
        Map<String, String> texts = new HashMap<>();
        texts.put("date", "Dato");
        when(handlelapp.displayTexts(any())).thenReturn(texts);
        LocalizationResource resource = new LocalizationResource();
        resource.handlelapp = handlelapp;
        Map<String, String> displayTexts = resource.displayTexts(handlelapp.defaultLocale().toString());
        assertThat(displayTexts).isNotEmpty();
    }

    @Test
    void testDisplayTextsWithUnknownLocale() {
        HandlelappService handlelapp = mock(HandlelappService.class);
        when(handlelapp.displayTexts(any())).thenThrow(MissingResourceException.class);
        LocalizationResource resource = new LocalizationResource();
        MockLogService logservice = new MockLogService();
        resource.setLogservice(logservice);
        resource.handlelapp = handlelapp;
        assertThrows(WebApplicationException.class, () -> resource.displayTexts("en_UK"));
    }

}
