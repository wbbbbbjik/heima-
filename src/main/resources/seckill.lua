-- 秒杀券id
local voucherId = ARGV[1]
-- 用户id
local userId = ARGV[2]
-- 订单id
local id = ARGV[3]

-- 库存key
local stockKey = 'seckill:stock:' .. voucherId
-- 订单key
local orderKey = 'seckill:order:' .. voucherId

-- 获取库存（关键修复：处理nil值，默认0）
local stock = tonumber(redis.call('get', stockKey) or "0")  -- 若为nil，默认转为0

-- 库存是否充足
if (stock <= 0) then
    return 1  -- 库存不足
end

-- 判断用户是否已下单（sismember对不存在的键返回0，无需额外处理nil）
if (redis.call('sismember', orderKey, userId) == 1) then
    return 2  -- 重复下单
end

-- 扣减库存
redis.call('incrby', stockKey, -1)
-- 记录用户下单
redis.call('sadd', orderKey, userId)
-- 发送消息到Stream
redis.call('xadd', 'stream.orders', '*', 'userId', userId, 'voucherId', voucherId, 'id', id)

return 0  -- 成功