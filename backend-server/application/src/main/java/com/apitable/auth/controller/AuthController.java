/*
 * APITable <https://github.com/apitable/apitable>
 * Copyright (C) 2022 APITable Ltd. <https://apitable.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.apitable.auth.controller;

import cn.hutool.core.util.BooleanUtil;
import com.apitable.auth.dto.UserLoginDTO;
import com.apitable.auth.enums.LoginType;
import com.apitable.auth.ro.LoginRo;
import com.apitable.auth.ro.RegisterRO;
import com.apitable.auth.service.IAuthService;
import com.apitable.auth.vo.LoginResultVO;
import com.apitable.auth.vo.LogoutVO;
import com.apitable.core.support.ResponseData;
import com.apitable.interfaces.auth.facade.AuthServiceFacade;
import com.apitable.interfaces.auth.model.AuthParam;
import com.apitable.interfaces.auth.model.UserAuth;
import com.apitable.interfaces.auth.model.UserLogout;
import com.apitable.interfaces.eventbus.facade.EventBusFacade;
import com.apitable.interfaces.eventbus.model.UserLoginEvent;
import com.apitable.interfaces.security.facade.BlackListServiceFacade;
import com.apitable.interfaces.security.facade.HumanVerificationServiceFacade;
import com.apitable.interfaces.security.model.NonRobotMetadata;
import com.apitable.shared.component.scanner.annotation.ApiResource;
import com.apitable.shared.component.scanner.annotation.PostResource;
import com.apitable.shared.config.properties.CookieProperties;
import com.apitable.shared.context.SessionContext;
import com.apitable.shared.util.information.ClientOriginInfo;
import com.apitable.shared.util.information.InformationUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.apitable.user.service.IUserService;
import com.auth0.jwk.JwkException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import com.apitable.auth.service.impl.AuthServiceImpl;


/**
 * Authorization interface.
 */
@RestController
@Tag(name = "Authorization related interface")
@ApiResource(path = "/")
@Slf4j
public class AuthController {

    private static final String AUTH_DESC =
        "description:verifyType，available values:\npassword\nsms_code\nemail_code";

    @Resource
    private IAuthService iAuthService;

    @Resource
    private BlackListServiceFacade blackListServiceFacade;

    @Resource
    private CookieProperties cookieProperties;

    @Resource
    private HumanVerificationServiceFacade humanVerificationServiceFacade;

    @Resource
    private EventBusFacade eventBusFacade;

    @Resource
    private AuthServiceFacade authServiceFacade;

    @Value("${SKIP_REGISTER_VALIDATE:false}")
    private Boolean skipRegisterValidate;

    @Value("${OIDC_IMPLICIT_SUCCESS_REDIRECT:../../workbench}")
    private String oidcImplicitSuccessRedirect;

    @Value("${OIDC_IMPLICIT_JWKS_URI:NONE}")
    private String oidcImplicitJwksUri;

    @Value("${OIDC_IMPLICIT_IS_ENABLED:false}")
    private Boolean oidcImplicitIsEnabled;

    @Resource
    private IUserService iUserService;

    /**
     * Register.
     *
     * @param data Request Parameters
     * @return {@link ResponseData}
     * @author Chambers
     */
    @PostResource(path = "/register", requiredLogin = false)
    @Operation(summary = "register", description = "serving for community edition")
    public ResponseData<Void> register(@RequestBody @Valid final RegisterRO data) {
        if (BooleanUtil.isFalse(skipRegisterValidate)) {
            return ResponseData.error("Validate failure");
        }
        Long userId = iAuthService.register(data.getUsername(), data.getCredential());
        SessionContext.setUserId(userId);
        return ResponseData.success();
    }

    /**
     * callback router.
     *
     * @return {@link ResponseData}
     */

    @PostResource(path = "/oidccallback", requiredPermission = false, method =
            RequestMethod.POST, requiredLogin = false)
    @Operation(summary = "oidc implicit auth")

    public ModelAndView callback(final HttpServletRequest request) {
        String oidcCode = request.getParameter("access_token");
        ClientOriginInfo origin = InformationUtil.getClientOriginInfo(request,
                false, true);

        String[] chunks = oidcCode.split("\\.");
        Base64.Decoder decoder = Base64.getUrlDecoder();
//        String header = new String(decoder.decode(chunks[0]));
        String payload = new String(decoder.decode(chunks[1]));

        String userEmail;
        Long tokenExp;
        boolean IsUserAuthSuccess = false;
        Long finalUserId;
        long unixTime = System.currentTimeMillis() / 1000L;


        try {
            System.out.println("Validation URL ::"+oidcImplicitJwksUri);
            if(!Objects.equals(oidcImplicitJwksUri, "NONE")) {
                System.out.println("Validation called");
                AuthServiceImpl.ValidateJWTSignature(oidcCode, oidcImplicitJwksUri);
            }

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jwtJsonNode = objectMapper.readTree(payload);

            userEmail = jwtJsonNode.get("email").asText();
            tokenExp = jwtJsonNode.get("exp").asLong();

            if(!Objects.equals(userEmail, "") && tokenExp > unixTime){
                IsUserAuthSuccess = true;
            }

        } catch (JsonProcessingException | JwkException | MalformedURLException e) {
            throw new RuntimeException(e);
        }

        Long existedUserId = iUserService.getUserIdByEmail(userEmail);

        if (IsUserAuthSuccess) {
            if (existedUserId != null) {
                finalUserId = existedUserId;
            } else {
                String pwd = AuthServiceImpl.gimmieSomeRandomStringForPassword();
                finalUserId = iAuthService.register(userEmail, pwd);
            }
        } else {
            return null;
        }

        eventBusFacade.onEvent(new UserLoginEvent(finalUserId, LoginType.SSO_TOKEN, false, origin));
        // Banned account verification
        blackListServiceFacade.checkUser(finalUserId);
        // save session
        SessionContext.setUserId(finalUserId);
        request.setAttribute(
                View.RESPONSE_STATUS_ATTRIBUTE, HttpStatus.TEMPORARY_REDIRECT);
        return new ModelAndView("redirect:" + oidcImplicitSuccessRedirect);
    }


    /**
     * login router.
     *
     * @param data    login data
     * @param request request info
     * @return {@link ResponseData}
     */
    @PostResource(name = "Login", path = "/signIn", requiredLogin = false)
    @Operation(summary = "login", description = AUTH_DESC)
    public ResponseData<LoginResultVO> login(@RequestBody @Valid final LoginRo data,
                                    final HttpServletRequest request) {
        ClientOriginInfo origin = InformationUtil.getClientOriginInfo(request,
            false, true);
        // Login Type Routing
        Map<LoginType, Function<LoginRo, LoginResultVO>> loginActionFunc =
            new HashMap<>();
        // password login
        loginActionFunc.put(LoginType.PASSWORD, loginRo -> {
            if (oidcImplicitIsEnabled){
                return null;
            }
            // Password login requires human-machine authentication
            humanVerificationServiceFacade.verifyNonRobot(
                new NonRobotMetadata(loginRo.getData()));
            // Login processing
            Long userId = iAuthService.loginUsingPassword(loginRo);
            // event point - password login
            eventBusFacade.onEvent(
                new UserLoginEvent(userId, LoginType.PASSWORD, false,
                    origin));
            return LoginResultVO.builder().userId(userId).build();
        });
        // SMS verification code login
        loginActionFunc.put(LoginType.SMS_CODE, loginRo -> {
             if (oidcImplicitIsEnabled){
                return null;
            }
            UserLoginDTO result = iAuthService.loginUsingSmsCode(loginRo);
            // sensors point - Login or Register
            if (Boolean.TRUE.equals(result.getIsSignUp())) {
                eventBusFacade.onEvent(
                    new UserLoginEvent(result.getUserId(), LoginType.SMS_CODE,
                        true, origin));
            } else {
                eventBusFacade.onEvent(
                    new UserLoginEvent(result.getUserId(), LoginType.SMS_CODE,
                        false, origin));
            }
            return LoginResultVO.builder()
                .userId(result.getUserId())
                .isNewUser(Boolean.TRUE.equals(result.getIsSignUp()))
                .build();
        });
        // Email verification code login
        loginActionFunc.put(LoginType.EMAIL_CODE, loginRo -> {
             if (oidcImplicitIsEnabled){
                return null;
            }
            UserLoginDTO result = iAuthService.loginUsingEmailCode(loginRo);
            // sensors point - Login or Register
            if (Boolean.TRUE.equals(result.getIsSignUp())) {
                eventBusFacade.onEvent(
                    new UserLoginEvent(result.getUserId(), LoginType.EMAIL_CODE,
                        true, origin));
            } else {
                eventBusFacade.onEvent(
                    new UserLoginEvent(result.getUserId(), LoginType.EMAIL_CODE,
                        false, origin));
            }
            return LoginResultVO.builder()
                .userId(result.getUserId())
                .isNewUser(Boolean.TRUE.equals(result.getIsSignUp()))
                .build();
        });
        // SSO login (private user use)
        loginActionFunc.put(LoginType.SSO_AUTH, loginRo -> {
            if (oidcImplicitIsEnabled){
                return null;
            }
            UserAuth userAuth;
            try {
                userAuth = authServiceFacade.ssoLogin(
                    new AuthParam(data.getUsername(), data.getCredential()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            Long userId = userAuth != null ? userAuth.getUserId() : null;
            eventBusFacade.onEvent(new UserLoginEvent(userId, LoginType.SSO_AUTH, false, origin));
            return LoginResultVO.builder().userId(userId).build();
        });
        // Handling login logic
        LoginResultVO resultVO = loginActionFunc.get(data.getType()).apply(data);
        Long userId = resultVO.getUserId();
        // Banned account verification
        blackListServiceFacade.checkUser(userId);
        // save session
        SessionContext.setUserId(userId);
        return ResponseData.success(resultVO);
    }

    /**
     * logout router.
     *
     * @param request  HttpServletRequest
     * @param response HttpServletResponse
     * @return {@link LogoutVO}
     */
    @PostResource(name = "sign out", path = "/signOut", requiredPermission = false, method = {
        RequestMethod.GET,
        RequestMethod.POST}, requiredLogin = false)
    @Operation(summary = "sign out", description = "log out of current user")
    public ResponseData<LogoutVO> logout(final HttpServletRequest request,
                                         final HttpServletResponse response) {
        LogoutVO logoutVO = new LogoutVO();
        Long userId = SessionContext.getUserIdWithoutException();
        UserLogout userLogout = authServiceFacade.logout(new UserAuth(userId));
        if (userLogout != null) {
            logoutVO.setNeedRedirect(userLogout.isRedirect());
            logoutVO.setRedirectUri(userLogout.getRedirectUri());
        }
        SessionContext.cleanContext(request);
        SessionContext.removeCookie(response,
            cookieProperties.getI18nCookieName(),
            cookieProperties.getDomainName());
        return ResponseData.success(logoutVO);
    }
}
