package com.example.interceptors;

import com.example.utils.JwtUtil;
import com.example.utils.ThreadLocalUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.TimeUnit;

// src/main/java/com/example/interceptors/LoginInterceptor.java

@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;  // 改为 RedisTemplate

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 如果是OPTIONS请求，直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return true;
        }

        // 令牌验证
        String token = request.getHeader("Authorization");

        // 验证token
        try {
            // 从Redis获取相同的token
            ValueOperations<String, String> operations = redisTemplate.opsForValue();
            String redisToken = operations.get(token);

            // 如果 Redis 中不存在该 token，说明已失效
            if (redisToken == null) {
                response.setStatus(401);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":401,\"message\":\"token已失效，请重新登录\"}");
                return false;
            }

            Map<String, Object> claims = JwtUtil.parseToken(token);

            // 刷新token过期时间
            operations.set(token, token, 30, TimeUnit.MINUTES);

            // 把业务数据存储到ThreadLocal中
            ThreadLocalUtil.set(claims);
            return true;
        } catch (Exception e) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"token无效，请重新登录\"}");
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        ThreadLocalUtil.remove();
    }
}
