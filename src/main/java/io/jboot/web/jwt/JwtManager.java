/**
 * Copyright (c) 2015-2018, Michael Yang 杨福海 (fuhai999@gmail.com).
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.jboot.web.jwt;

import com.jfinal.json.FastJson;
import com.jfinal.kit.Base64Kit;
import io.jboot.Jboot;
import io.jboot.utils.StringUtils;
import io.jsonwebtoken.*;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Michael Yang 杨福海 （fuhai999@gmail.com）
 * @version V1.0
 * @Package io.jboot.web.jwt
 */
public class JwtManager {

    private static final JwtManager me = new JwtManager();

    public static JwtManager me() {
        return me;
    }

    private JwtConfig jwtConfig = Jboot.config(JwtConfig.class);

    private ThreadLocal<Map> jwtThreadLocal = new ThreadLocal<>();

    public void holdJwts(Map map) {
        jwtThreadLocal.set(map);
    }

    public void releaseJwts() {
        jwtThreadLocal.remove();
    }

    public <T> T getPara(String key) {
        Map map = jwtThreadLocal.get();
        return map == null ? null : (T) map.get(key);
    }

    public Map getParas() {
        return jwtThreadLocal.get();
    }

    public boolean isEnable() {
        return jwtConfig.isEnable();
    }

    public String getHttpHeaderName() {
        return jwtConfig.getHttpHeaderName();
    }

    public Map parseJwtToken(String token) {
        SecretKey secretKey = generalKey();
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(secretKey)
                    .parseClaimsJws(token).getBody();

            String subject = claims.getSubject();

            if (StringUtils.isBlank(subject)) {
                return null;
            }

            return FastJson.getJson().parse(subject, HashMap.class);

        } catch (SignatureException | MalformedJwtException e) {
            // don't trust the JWT!
            // jwt 签名错误或解析错误，可能是伪造的，不能相信
        } catch (ExpiredJwtException e) {
            // jwt 已经过期
        } catch (Throwable ex) {
            //其他错误
        }

        return null;
    }

    public String createJwtToken(Map map) {

        String subject = FastJson.getJson().toJson(map);
        SecretKey secretKey = generalKey();

        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;
        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);

        JwtBuilder builder = Jwts.builder()
                .setIssuedAt(now)
                .setSubject(subject)
                .signWith(signatureAlgorithm, secretKey);

        if (jwtConfig.getValidityPeriod() > 0) {
            long expMillis = nowMillis + jwtConfig.getValidityPeriod();
            builder.setExpiration(new Date(expMillis));
        }

        return builder.compact();
    }


    private SecretKey generalKey() {
        byte[] encodedKey = Base64Kit.decode(jwtConfig.getSecret());
        SecretKey key = new SecretKeySpec(encodedKey, 0, encodedKey.length, "AES");
        return key;
    }


}
