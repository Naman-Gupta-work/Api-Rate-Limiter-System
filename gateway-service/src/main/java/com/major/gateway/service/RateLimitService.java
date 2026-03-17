package com.major.gateway.service;

import com.major.gateway.model.Plan;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class RateLimitService {

    private final StringRedisTemplate redisTemplate;



    private final String LUA_SCRIPT = """
        local key = KEYS[1]
        local capacity = tonumber(ARGV[1])
        local refill_rate = tonumber(ARGV[2])
        local current_time = tonumber(ARGV[3])
        local requested = tonumber(ARGV[4])

        local data = redis.call("HMGET", key, "tokens", "last_refill")

        local tokens = tonumber(data[1])
        local last_refill = tonumber(data[2])

        if tokens == nil then
            tokens = capacity
            last_refill = current_time
        end

        local delta = math.max(0, current_time - last_refill)
        local refill = delta * refill_rate
        tokens = math.min(capacity, tokens + refill)

        if tokens < requested then
            return 0
        else
            tokens = tokens - requested
            redis.call("HMSET", key, "tokens", tokens, "last_refill", current_time)
            redis.call("EXPIRE", key, 60)
            return 1
        end
    """;

    public RateLimitService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isAllowed(String apiKey,String p) {
        Plan plan;
        try {
            plan = Plan.valueOf(p);
        }
        catch (Exception E){
            plan =Plan.FREE;
        }
        String key = "token_bucket:" + apiKey;
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(LUA_SCRIPT);
        script.setResultType(Long.class);

        Long result = redisTemplate.execute(
                script,
                Collections.singletonList(key),
                String.valueOf(plan.getCapacity()),
                String.valueOf(plan.getRefillRate()),
                String.valueOf(System.currentTimeMillis()/1000),
                "1"
        );

        return result != null && result == 1;
    }
}