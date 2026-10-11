package no.priv.bang.handlelapp.web.api.resources;
/*
 * Copyright 2024-2026 Steinar Bang
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

import static no.priv.bang.handlelapp.services.HandlelappConstants.*;

import java.util.Base64;
import java.util.Optional;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.LockedAccountException;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.web.util.SavedRequest;
import org.apache.shiro.web.util.WebUtils;
import org.osgi.service.log.LogService;
import org.osgi.service.log.Logger;

import no.priv.bang.authservice.definitions.AuthserviceException;
import no.priv.bang.osgiservice.users.User;
import no.priv.bang.osgiservice.users.UserManagementService;
import no.priv.bang.handlelapp.services.HandlelappService;
import no.priv.bang.handlelapp.services.beans.Credentials;
import no.priv.bang.handlelapp.services.beans.Loginresult;

@Path("")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class LoginResource {

    private Logger logger;

    @Inject
    public ServletContext webcontext;

    @Context
    HttpServletRequest request;

    @Inject
    HandlelappService handlelapp;

    @Inject
    UserManagementService useradmin;

    @Inject
    void setLogservice(LogService logservice) {
        this.logger = logservice.getLogger(LoginResource.class);
    }

    @POST
    @Path("/login")
    public Loginresult login(@QueryParam("locale")String locale, Credentials credentials) {
        var subject = SecurityUtils.getSubject();
        var username = credentials.username();
        var decodedPassword = new String(Base64.getDecoder().decode(credentials.password()));

        var token = new UsernamePasswordToken(username, decodedPassword, true);
        try {
            subject.login(token);
            var savedRequest = Optional.ofNullable(WebUtils.getSavedRequest(request));
            var contextpath = webcontext.getContextPath();
            var originalRequestUrl =  savedRequest
                .map(SavedRequest::getRequestUrl)
                .map(url -> url.replace(contextpath, ""))
                .orElse("/");
            var authorized = subject.hasRole(HANDLELAPPUSER_ROLE);
            if (authorized) {
                handlelapp.lazilyCreateAccount(username);
            }

            var user = useradmin.getUser(username);

            return Loginresult.with()
                .success(true)
                .errormessage("")
                .authorized(authorized)
                .user(user)
                .originalRequestUrl(originalRequestUrl)
                .build();
        } catch(UnknownAccountException e) {
            logger.warn("Login error: unknown account", e);
            return Loginresult.with().success(false).errormessage(handlelapp.displayText("unknownaccount", locale)).build();
        } catch (IncorrectCredentialsException  e) {
            logger.warn("Login error: wrong password", e);
            return Loginresult.with().success(false).errormessage(handlelapp.displayText("wrongpassword", locale)).build();
        } catch (LockedAccountException  e) {
            logger.warn("Login error: locked account", e);
            return Loginresult.with().success(false).errormessage(handlelapp.displayText("lockedaccount", locale)).build();
        } catch (AuthenticationException e) {
            logger.warn("Login error: general authentication error", e);
            return Loginresult.with().success(false).errormessage(handlelapp.displayText("unknownerror", locale)).build();
        } catch (Exception e) {
            logger.error("Login error: internal server error", e);
            throw new InternalServerErrorException();
        } finally {
            token.clear();
        }
    }

    @GET
    @Path("/logout")
    public Loginresult logout(@QueryParam("locale")String locale) {
        var subject = SecurityUtils.getSubject();
        subject.logout();

        return Loginresult.with()
            .success(false)
            .errormessage(handlelapp.displayText("loggedout", locale))
            .user(User.with().build())
            .build();
    }

    @GET
    @Path("/loginstate")
    public Loginresult loginstate(@QueryParam("locale")String locale) {
        var subject = SecurityUtils.getSubject();
        var username = (String) subject.getPrincipal();
        var success = subject.isAuthenticated();
        var harRoleHandlelappuser = subject.hasRole(HANDLELAPPUSER_ROLE);
        var brukerLoggetInnMelding = handlelapp.displayText(harRoleHandlelappuser ? "userloggedinwithaccesses" : "userloggedinwithoutaccesses", locale);
        var melding = success ? brukerLoggetInnMelding : handlelapp.displayText("usernotloggedin", locale);
        var user = findUserSafely(username);
        return Loginresult.with()
            .success(success)
            .errormessage(melding)
            .authorized(harRoleHandlelappuser)
            .user(user)
            .build();
    }

    User findUserSafely(String username) {
        try {
            return useradmin.getUser(username);
        } catch (AuthserviceException e) {
            return User.with().build();
        }
    }

    String findOriginalRequestUrl() {
        var savedRequest = WebUtils.getSavedRequest(request);
        var contextPath = request.getContextPath();
        var originalRequestUrl = savedRequest != null ? savedRequest.getRequestUrl() : null;
        if (contextPath != null && originalRequestUrl != null) {
            return originalRequestUrl.replaceFirst(contextPath, "");
        }

        return originalRequestUrl;
    }

}
