package com.lzx.redisdemo.service;

import org.redisson.api.*;
import org.redisson.api.map.event.EntryExpiredListener;
import org.redisson.api.map.event.MapEntryListener;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

@Service
public class TestService implements InitializingBean {
    private final RedissonClient redissonClient;
    private final RMapCache<String,String> map;
    private final RSetCache<String> set;
    private final String instanceId;

    TestService(RedissonClient redissonClient
                ,ApplicationContext context){
        this.redissonClient = redissonClient;
        instanceId = context.getEnvironment().getProperty("instanceId");
        map = redissonClient.getMapCache("redis-demo-map"+instanceId);
        map.addListener((EntryExpiredListener<String,String>) event -> {
            System.out.println("expire key:"+event.getKey()+",expire value:"+event.getValue());
        });
        set = redissonClient.getSetCache("redis-demo-set"+instanceId);
    }

    public void test(){
        RLock lock = redissonClient.getLock("lock_test");
        lock.lock(20,TimeUnit.SECONDS);
        RLock lock1 = redissonClient.getLock("lock_test");
        lock1.lock(20,TimeUnit.SECONDS);
        map.put("msg","hello",20,TimeUnit.SECONDS);
        set.add("a",20,TimeUnit.SECONDS);
        lock1.unlock();
        lock.unlock();
    }

    @Scheduled(cron = "0/1 * * * * ?")
    public void scheduledTask(){
        map.entrySet().forEach(entry -> {
            System.out.println("refresh key:"+entry.getKey()+" expire time");
            map.put(entry.getKey(),entry.getValue(),20,TimeUnit.SECONDS);
        });
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if(!map.isEmpty()){
            map.entrySet().forEach(entry -> {
                System.out.println("remove key:"+entry.getKey());
                map.remove(entry.getKey());
            });
        }
    }
}
