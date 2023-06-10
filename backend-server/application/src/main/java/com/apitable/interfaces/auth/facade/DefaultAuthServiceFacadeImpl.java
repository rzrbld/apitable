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

package com.apitable.interfaces.auth.facade;

import com.apitable.interfaces.auth.model.AuthParam;
import com.apitable.interfaces.auth.model.UserAuth;
import com.apitable.interfaces.auth.model.UserLogout;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import com.apitable.auth.service.IAuthService;
import com.apitable.user.service.IUserService;
import com.apitable.auth.service.impl.AuthServiceImpl;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Random;


@Component
public class DefaultAuthServiceFacadeImpl implements AuthServiceFacade {

    @Value("${OIDC_URI_GRANT_TYPE_PASSWORD:empty_string}")
    private String oidcUriGrantTypePassword;

//    public String gimmieSomeRandomStringForPassword() {
//        int leftLimit = 48; // numeral '0'
//        int rightLimit = 122; // letter 'z'
//        int targetStringLength = 25;
//        Random random = new Random();
//
//        String generatedString = random.ints(leftLimit, rightLimit + 1)
//                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
//                .limit(targetStringLength)
//                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
//                .toString();
//
//        return generatedString;
//    }

    public Boolean isThisUserAbleToAuth(AuthParam param) throws IOException {
        String UserName = param.getUsername();
        String UserPwd = param.getPassword();
        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
        RequestBody body = RequestBody.create(mediaType, "client_id=login-app&username="+UserName+"&password="+UserPwd+"&grant_type=password");
        Request request = new Request.Builder()
                .url(oidcUriGrantTypePassword)
                .method("POST", body)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build();
        Response response = client.newCall(request).execute();
        System.out.println(response.body().string());
//        put your custom validation code here
        return response.code() == 200;
    }
    @Resource
    private IAuthService iAuthService;
    @Resource
    private IUserService iUserService;
    @Override
    public UserAuth ssoLogin(AuthParam param) throws IOException {
        Boolean IsUserAuthSuccess = isThisUserAbleToAuth(param);
        Long existedUserId = iUserService.getUserIdByEmail(param.getUsername());


        if (IsUserAuthSuccess){
            if (existedUserId!=null){
                return new UserAuth(existedUserId);
            } else {
                String pwd = AuthServiceImpl.gimmieSomeRandomStringForPassword();
                Long userId = iAuthService.register(param.getUsername(), pwd);
                return new UserAuth(userId);
            }
        } else {
            return null;
        }
    }

    @Override
    public UserLogout logout(UserAuth userAuth) {
        // Implement logout logic if needed
        return null;
    }
}
