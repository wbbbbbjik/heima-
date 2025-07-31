package com.hmdp.service.impl;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.dto.ScrollResult;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.Follow;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IFollowService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import static com.hmdp.utils.RedisConstants.BLOG_LIKED_KEY;
import static com.hmdp.utils.RedisConstants.FEED_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

   @Autowired
   private IUserService userService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private IFollowService followService;

    @Override
    public Blog getByUserId(Long id) {

        Blog blog = getById(id);
        Long userId = blog.getUserId();


        User user = userService.getById(userId);

        if (user!=null){
            blog.setIcon(user.getIcon());
            blog.setName(user.getNickName());
        }
       isBlogLiked(blog);


        return blog;
    }
    private void isBlogLiked(Blog blog) {
        // 1. 获取当前用户
        UserDTO user = UserHolder.getUser();

        // 2. 判断用户是否登录
        if (user == null) {
            // 用户未登录，默认未点赞
            blog.setIsLike(false);
            return;
        }

        // 3. 用户已登录，判断是否点赞
        Long userId = user.getId();
        String key = BLOG_LIKED_KEY+ blog.getId();
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        blog.setIsLike(score != null);
    }
    @Override
    public Result likeBlog(Long id) {

        //1.获取登录用户id
        Long userId= UserHolder.getUser().getId();
        //2.判断当前登录用户是否已经点赞

        String key=BLOG_LIKED_KEY+id;
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        //如果未点赞，可以点赞
        if (score==null){
            //数据库点赞数加一
            boolean isSuccess = update().setSql("liked = liked + 1").eq("id", id).update();
            // 保存用户到redis的2set集合
            if (isSuccess){
                stringRedisTemplate.opsForZSet().add(key,userId.toString(),System.currentTimeMillis());
            }
        }
else {
            //如果已点赞取消点赞
            boolean isSuccess = update().setSql("liked = liked - 1").eq("id", id).update();
            //数据库点赞数减一

            stringRedisTemplate.opsForZSet().remove(key,userId.toString());
        }





        // 修改点赞数量


       //根据id查询笔记
        Blog byId = getById(id);

        return Result.ok();
    }

    @Override
    public Result queryPage(Integer current) {
        // 根据用户查询
        Page<Blog> page =query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        // 查询用户
        records.forEach(blog ->{
            Long userId = blog.getUserId();
            User user = userService.getById(userId);
            blog.setName(user.getNickName());
            blog.setIcon(user.getIcon());
            isBlogLiked(blog);
        });
        return Result.ok(records);
    }

    @Override
    public Result likesBlog(Long id) {
        String key = BLOG_LIKED_KEY + id;
        // 1.查询top5的点赞用户 zrange key 0 4
        Set<String> top5 = stringRedisTemplate.opsForZSet().range(key, 0, 4);
        if (top5 == null || top5.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }
        // 2.解析出其中的用户id
        List<Long> ids = top5.stream().map(Long::valueOf).collect(Collectors.toList());
        String idStr = StrUtil.join(",", ids);
        // 3.根据用户id查询用户 WHERE id IN ( 5 , 1 ) ORDER BY FIELD(id, 5, 1)
        List<UserDTO> userDTOS = userService.query()
                .in("id", ids).last("ORDER BY FIELD(id," + idStr + ")").list()
                .stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());
        // 4.返回
        return Result.ok(userDTOS);
    }

    @Override
    public Result savaBlog(Blog blog) {
                // 获取登录用户
        UserDTO user = UserHolder.getUser();
        blog.setUserId(user.getId());
        // 保存探店博文
        boolean save = save(blog);
        if (!save){
            return Result.fail("新增笔记失败");
        }
        // 返回id
        //查询笔记粉丝
        List<Follow> followUserId = followService.query().eq("follow_user_id", user.getId()).list();
        for (Follow follow : followUserId) {
            Long userId = follow.getUserId();
            String key="feed:"+userId;
            stringRedisTemplate.opsForZSet().add(key,blog.getId().toString(),System.currentTimeMillis());
        }


        return Result.ok(blog.getId());
    }

    @Override
    public Result queryBlogOfFollow(Long max, Integer offset) {
        //获取收件箱
        Long userId = UserHolder.getUser().getId();
        String key=FEED_KEY+userId;
        Set<ZSetOperations.TypedTuple<String>> typedTuples = stringRedisTemplate.opsForZSet().reverseRangeByScoreWithScores(key, 0, max, offset, 3);
        if (typedTuples==null  | typedTuples.isEmpty()){
            return Result.ok();
        }

        //解析数据
        ArrayList<Long> ids = new ArrayList<>(typedTuples.size());
        long minTime=0;
        int os=1;
        for (ZSetOperations.TypedTuple<String> typedTuple : typedTuples) {
           long time = typedTuple.getScore().longValue();
           if (time==minTime){
               os++;
           }
           else {
               minTime=time;
               os=1;
           }
            String id = typedTuple.getValue();
            ids.add(Long.valueOf(id));
        }

        //根据id查询blog
        // 查询用户


        String join = StrUtil.join(",", ids);
        List<Blog> blogList = query().in("id", ids).last("ORDER BY FIELD(ID," + join + ")").list();

          blogList.forEach(blog ->{
                Long userIds = blog.getUserId();
                User user = userService.getById(userIds);
                blog.setName(user.getNickName());
                blog.setIcon(user.getIcon());
                isBlogLiked(blog);
            });


        ScrollResult scrollResult = new ScrollResult();
        scrollResult.setList(blogList);
        scrollResult.setMinTime(minTime);
        scrollResult.setOffset(offset);
        //封装blog
        return Result.ok(scrollResult);
    }
}
