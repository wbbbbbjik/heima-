package com.hmdp.service.impl;
import com.google.gson.Gson;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryCache() {

        //根据redis查询缓存数据
        List<String> redisType = stringRedisTemplate.opsForList().range(RedisConstants.CACHE_SHOP_TYPE_KEY, 0, -1);
        Gson gson = new Gson();


        if (redisType!=null&& !redisType.isEmpty()){
            List<ShopType> collect = redisType.stream().map(type -> {
                ShopType shopType = gson.fromJson(type, ShopType.class);
                return shopType;
            }).collect(Collectors.toList());
           return Result.ok(collect);

        }
        //不存在就查询在存入
        List<ShopType> sort = query().orderByAsc("sort").list();
        if (sort==null|| sort.isEmpty()){
            List<ShopType> shopTypes = Collections.emptyList();
            return Result.fail("数据为空");

        }
        List<String> collect = sort.stream().map(shopType -> {
            String json = gson.toJson(shopType);
            return json;
        }).collect(Collectors.toList());

        //插入redis
        stringRedisTemplate.opsForList().rightPushAll(RedisConstants.CACHE_SHOP_TYPE_KEY,collect);
        return Result.ok(sort);
    }
}
