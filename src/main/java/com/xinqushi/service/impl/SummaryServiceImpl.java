package com.xinqushi.service.impl;

import java.text.ParseException;



import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.xinqushi.common.pojo.CommentInfo;
import com.xinqushi.common.pojo.EUDataGridResult;
import com.xinqushi.common.pojo.SummaryInfo;
import com.xinqushi.common.pojo.SummaryList;
import com.xinqushi.entity.Admin;
import com.xinqushi.entity.AdminExample;
import com.xinqushi.entity.Clue;
import com.xinqushi.entity.ClueExample;
import com.xinqushi.entity.Experience;
import com.xinqushi.entity.ExperienceExample;
import com.xinqushi.entity.LoginUserInfo;
import com.xinqushi.entity.Member;
import com.xinqushi.entity.MemberExample;
import com.xinqushi.entity.PictureExample;
import com.xinqushi.entity.Province;
import com.xinqushi.entity.Summary;
import com.xinqushi.entity.SummaryExample;
import com.xinqushi.entity.SummaryVisit;
import com.xinqushi.entity.SummaryVisitExample;
import com.xinqushi.entity.SummaryVisitExample.Criteria;
import com.xinqushi.mapper.AdminMapper;
import com.xinqushi.mapper.ClueMapper;
import com.xinqushi.mapper.ExperienceMapper;
import com.xinqushi.mapper.MemberMapper;
import com.xinqushi.mapper.PictureMapper;
import com.xinqushi.mapper.ProvinceMapper;
import com.xinqushi.mapper.SummaryMapper;
import com.xinqushi.mapper.SummaryVisitMapper;
import com.xinqushi.service.SummaryService;
import com.xinqushi.tools.SummaryTitle;
import com.xinqushi.utils.CookieUtils;
import com.xinqushi.utils.JsonUtils;
import com.xinqushi.utils.MemberResult;
import com.xinqushi.web.pojo.AllNames;

import net.sf.jsqlparser.expression.operators.arithmetic.Addition;
@Service
public class SummaryServiceImpl implements SummaryService {
    @Value("${COOKIE_TOKEN_KEY}")
    private String COOKIE_TOKEN_KEY;
    @Value("${SESSION_PRE}")
    private String SESSION_PRE;
    @Value("${SUMMARY_LIST}")
    private String SUMMARY_LIST;
    @Value("${MEMBER_GET}")
    private String MEMBER_GET;
    @Value("${SUMMARY_NOW}")
    private String SUMMARY_NOW;
    @Value("${SUMMARY_INFO}")
    private String SUMMARY_INFO;
    @Value("${COMMENT_GET}")
    private String COMMENT_GET;
    @Value("${SUMMARY_SESSION_TIME}")
    private long SUMMARY_SESSION_TIME;
    @Value("${SUMMARY_NEED_LOGIN}")
    private long SUMMARY_NEED_LOGIN;
    @Autowired
    private SummaryMapper summaryMapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private MemberMapper memberMapper;
    @Autowired
    private AdminMapper adminMapper;
    @Autowired
    private ProvinceMapper provinceMapper;
    @Autowired
    private PictureMapper pictureMapper;
    @Autowired
    private SummaryVisitMapper summaryVisitMapper;
    @Autowired
    private ClueMapper clueMapper;
    @Autowired
    private ExperienceMapper experienceMapper;
    
    /*
     * 保存及修改周报功能，若该用户当周未填写周报，则为保存，若已填写，则为修改
     * @see com.xinqushi.service.SummaryService#saveSummary(java.lang.String, javax.servlet.http.HttpServletRequest)
     */
    @Override
    public MemberResult saveSummary(String contents, HttpServletRequest request) throws ParseException {
        // 从cookie获取token
        String token = CookieUtils.getCookieValue(request, COOKIE_TOKEN_KEY);
        if(getMemberByToken(token) == null) {
            return null;
        }
        Summary summary=getMemberByToken(token);
        summary.setPid(0);
        summary.setTime(SummaryTitle.getTime());
        summary.setTitle(SummaryTitle.getWriteTitle());
        summary.setContent(contents);
        summary.setIscheck(false);
        // 通过mid,aid,cid,eid,pid和title查询summary得到的size，来决定新增周报还是修改周报，size=0则新增，否则为修改
        SummaryExample summaryExample=new SummaryExample();
        com.xinqushi.entity.SummaryExample.Criteria summaryCriteria = summaryExample.createCriteria();
        summaryCriteria.andMidEqualTo(summary.getMid());
        summaryCriteria.andAidEqualTo(summary.getAid());
        summaryCriteria.andEidEqualTo(summary.getEid());
        summaryCriteria.andCidEqualTo(summary.getCid());
        summaryCriteria.andPidEqualTo(0);
        summaryCriteria.andTitleEqualTo(SummaryTitle.getWriteTitle());
        List<Summary> summarys = summaryMapper.selectByExample(summaryExample);
        if(summarys.size() == 0) {
            summaryMapper.insert(summary);
            // 新增周报后删除周报列表redis
            try {
                stringRedisTemplate.delete(SUMMARY_LIST+":"+token);
                stringRedisTemplate.delete(SUMMARY_NOW+":"+token);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return MemberResult.build(200, "保存周报成功！");
        }else {
            summaryMapper.updateByExampleSelective(summary, summaryExample);
            // 修改周报后删除周报redis
            try {
                stringRedisTemplate.delete(SUMMARY_NOW+":"+token);
                stringRedisTemplate.delete(SUMMARY_INFO+":"+summarys.get(0).getId());
            } catch (Exception e) {
                e.printStackTrace();
            }
            return MemberResult.build(300, "修改周报成功！");
        }
    }
    
    /*
     * 获取当周周报，若该用户已保存，则可自动获取已保存周报，方便修改！
     * @see com.xinqushi.service.SummaryService#getCurrentSummary(javax.servlet.http.HttpServletRequest)
     */
    @Override
    public MemberResult getCurrentSummary(HttpServletRequest request) throws ParseException {
        // 从cookie获取token
        String token = CookieUtils.getCookieValue(request, COOKIE_TOKEN_KEY);
        if(getMemberByToken(token) == null) {
            return null;
        }
        Summary summary=getMemberByToken(token);
        // 通过token从redis中获取当前周报
        try {
            String string = stringRedisTemplate.opsForValue().get(SUMMARY_NOW+":"+token);
            if(!StringUtils.isEmpty(string)) {
                Summary summaryRedis = JsonUtils.jsonToPojo(string, Summary.class);
                stringRedisTemplate.expire(SUMMARY_NOW+":"+token, SUMMARY_SESSION_TIME, TimeUnit.SECONDS);
                return MemberResult.ok(summaryRedis);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 通过mid,aid,cid,eid,pid和title查询summary得到content
        SummaryExample summaryExample=new SummaryExample();
        com.xinqushi.entity.SummaryExample.Criteria summaryCriteria = summaryExample.createCriteria();
        summaryCriteria.andMidEqualTo(summary.getMid());
        summaryCriteria.andAidEqualTo(summary.getAid());
        summaryCriteria.andEidEqualTo(summary.getEid());
        summaryCriteria.andCidEqualTo(summary.getCid());
        summaryCriteria.andPidEqualTo(0);
        summaryCriteria.andTitleEqualTo(SummaryTitle.getWriteTitle());
        // 使用selectByExample会获取不到content
        List<Summary> summarys = summaryMapper.selectByExampleWithBLOBs(summaryExample);
        // 将周报存入redis
        if(summarys != null && summarys.size() > 0) {
            String json = JsonUtils.objectToJson(summarys.get(0));
            try {
                stringRedisTemplate.opsForValue().set(SUMMARY_NOW+":"+token, json, SUMMARY_SESSION_TIME, TimeUnit.SECONDS);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if(summarys.size()>0) {
            return MemberResult.ok(summarys.get(0));
        }
        return null;
    }
    
    /*
     * 获取我的周报列表，包含姓名，标题，时间，状态，周报id等
     * @see com.xinqushi.service.SummaryService#getSummaryList(int, int)
     */
    @Override
    public EUDataGridResult getSummaryList(int page, HttpServletRequest request, int rows) {
        // 从cookie获取token
        String token = CookieUtils.getCookieValue(request, COOKIE_TOKEN_KEY);
        if(getMemberByToken(token) == null) {
            return null;
        }
        // 从redis中获取周报列表
        try {
            String string = stringRedisTemplate.opsForValue().get(SUMMARY_LIST+":"+token);
            if(!StringUtils.isEmpty(string)) {
                EUDataGridResult euDataGridResult = JsonUtils.jsonToPojo(string, EUDataGridResult.class);
                stringRedisTemplate.expire(SUMMARY_LIST+":"+token, SUMMARY_SESSION_TIME, TimeUnit.SECONDS);
                return euDataGridResult;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 通过token查询summary表，并进行分页。其中不含用户姓名
        Summary summary=getMemberByToken(token);
        SummaryExample example=new SummaryExample();
        com.xinqushi.entity.SummaryExample.Criteria createCriteria = example.createCriteria();
        createCriteria.andMidEqualTo(summary.getMid());
        createCriteria.andAidEqualTo(summary.getAid());
        createCriteria.andCidEqualTo(summary.getCid());
        createCriteria.andEidEqualTo(summary.getEid());
        createCriteria.andPidEqualTo(0);
        PageHelper.startPage(page, rows, "id desc");
        List<Summary> list=summaryMapper.selectByExampleWithBLOBs(example);
        // 遍历list，将包含用户姓名的周报列表存入新的list中
        List<SummaryList> list2=new ArrayList<>();
        for(Summary summary1:list) {
            // 新增SummaryList的pojo，继承summary并包含name和realname属性
            SummaryList summaryList = new SummaryList();
            summaryList.setId(summary1.getId());
            summaryList.setTitle(summary1.getTitle());
            summaryList.setTime(summary1.getTime());
            summaryList.setRecommend(summary1.getRecommend());
            summaryList.setIscheck(summary1.getIscheck());
            // 根据summary得到当前用户的姓名，需筛选mid，cid，eid及aid
            if(summary1.getMid() !=0 && summary1.getMid() != null) {
                Member member = memberMapper.selectByPrimaryKey(summary1.getMid());
                summaryList.setName(member.getName());
            }else if (summary1.getEid() != 0 && summary1.getEid() != null) {
                Experience experience = experienceMapper.selectByPrimaryKey(summary1.getEid());
                summaryList.setName(experience.getName());
            }else if(summary1.getCid() !=0 && summary1.getCid() != null) {
                Clue clue = clueMapper.selectByPrimaryKey(summary1.getCid());
                summaryList.setName(clue.getRealname());
            }else {
                Admin admin = adminMapper.selectByPrimaryKey(summary1.getAid());
                summaryList.setName(admin.getName());
            }
            list2.add(summaryList);
        }
        // 新增pojo，其中包含周报总数及周报列表list
        EUDataGridResult result=new EUDataGridResult();
        result.setRows(list2);
        PageInfo<Summary> pageInfo=new PageInfo<>(list);
        result.setTotal(pageInfo.getTotal());
        // 将周报列表存入redis
        if(result != null) {
            String json = JsonUtils.objectToJson(result);
            try {
                stringRedisTemplate.opsForValue().set(SUMMARY_LIST+":"+token, json, SUMMARY_SESSION_TIME, TimeUnit.SECONDS);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }
    
    /*
     * 根据token设置summary的mid,cid,eid，供其他方法调用
     */
    public Summary getMemberByToken(String token) {
        // 根据token从redis获取信息
        String json = "";
        try {
            json = stringRedisTemplate.opsForValue().get(SESSION_PRE+":"+token);
            // 把json转换成loginuserinfo
            if (null == json || json.isEmpty() || json == "") {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        LoginUserInfo loginUser = JsonUtils.jsonToPojo(json, LoginUserInfo.class);
        Summary summary =new Summary();
        // 判断用户类型，分别存入mid，eid，aid
        if(loginUser.getUserType().getValue() == 0 || loginUser.getUserType().getValue() == 1) {
            summary.setAid(loginUser.getId());
            summary.setMid(0);
            summary.setCid(0);
            summary.setEid(0);
        }else if(loginUser.getUserType().getValue() == 2){
            MemberExample memberExample = new MemberExample();
            memberExample.createCriteria().andUidEqualTo(loginUser.getId());
            List<Member> members = memberMapper.selectByExample(memberExample);
            summary.setMid(members.get(0).getId());
            summary.setAid(0);
            summary.setCid(0);
            summary.setEid(0);
        }else if(loginUser.getUserType().getValue() == 3){
            summary.setEid(loginUser.getId());
            summary.setAid(0);
            summary.setCid(0);
            summary.setMid(0);
        }else {
            summary.setCid(loginUser.getId());
            summary.setAid(0);
            summary.setEid(0);
            summary.setMid(0);
        }
        return summary;
    }
    
    /*
     * 通过周报id获取周报
     */
    @Override
    public SummaryInfo getSummaryById(int id, HttpServletRequest request) {
        String token = CookieUtils.getCookieValue(request, COOKIE_TOKEN_KEY);
        // 若配置文件中设置为登录才可查看周报，则对登录状态进行判断
        String json = "";
        if(SUMMARY_NEED_LOGIN == 1) {
            try {
                // 根据token从redis获取信息
                json = stringRedisTemplate.opsForValue().get(SESSION_PRE+":"+token);
                // 把json转换成loginuserinfo
                if (json == "") {
                    return null;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // 新建SummaryInfo的pojo，其中包含查看周报页面相关信息
        SummaryInfo summaryInfo=new SummaryInfo();
        Member member = new Member();
        Summary summary = summaryMapper.selectByPrimaryKey(id);
        // 从redis中获取周报页面数据
        String str = stringRedisTemplate.opsForValue().get(SUMMARY_INFO+":"+id);
        if(!StringUtils.isEmpty(str)) {
            summaryInfo = JsonUtils.jsonToPojo(str, SummaryInfo.class);
            stringRedisTemplate.expire(SUMMARY_INFO+":"+id, SUMMARY_SESSION_TIME, TimeUnit.SECONDS);
        }else {
            byte[] picture = new byte[0];
            Admin admin = new Admin();
            Province province = new Province();
            province.setName("");
            // 根据summary表的mid，aid，eid，cid取到对应member，experience，clue，admin信息
            if(summary.getMid() !=0 && summary.getMid() != null) {
                member = memberMapper.selectByPrimaryKey(summary.getMid());
                admin = adminMapper.selectByPrimaryKey(member.getAid());
                province = provinceMapper.selectByPrimaryKey(member.getProvid());
                PictureExample example=new PictureExample();
                com.xinqushi.entity.PictureExample.Criteria createCriteria = example.createCriteria();
                createCriteria.andUidEqualTo(member.getUid());
                createCriteria.andCoverEqualTo(true);
                picture = pictureMapper.selectByExampleWithBLOBs(example).get(0).getPhoto();
                summaryInfo.setName(member.getName());
            }else if (summary.getEid() != 0 && summary.getEid() != null) {
                Experience experience = experienceMapper.selectByPrimaryKey(summary.getEid());
                admin = adminMapper.selectByPrimaryKey(experience.getAid());
                province =  provinceMapper.selectByPrimaryKey(experience.getSeatProvid());
                summaryInfo.setName(experience.getName());
            }else if(summary.getCid() !=0 && summary.getAid() !=null){
                Clue clue = clueMapper.selectByPrimaryKey(summary.getCid());
                admin = adminMapper.selectByPrimaryKey(clue.getAid());
                summaryInfo.setName(clue.getRealname());
            }else {
                admin= adminMapper.selectByPrimaryKey(summary.getAid());
                summaryInfo.setName(admin.getRealname());
                admin.setRealname("");
            }
            summaryInfo.setMid(summary.getMid());
            summaryInfo.setContent(summary.getContent());
            summaryInfo.setTime(summary.getTime());
            summaryInfo.setAdminName(admin.getRealname());
            summaryInfo.setProvince(province.getName());
            summaryInfo.setPicture(picture);
            if(summary.getRecommend() != null) {
                summaryInfo.setRecommend(summary.getRecommend());
            }
            // 将周报页面数据存入redis
            if(summaryInfo != null) {
                String string = JsonUtils.objectToJson(summaryInfo);
                try {
                    stringRedisTemplate.opsForValue().set(SUMMARY_INFO+":"+id, 
                            string, SUMMARY_SESSION_TIME, TimeUnit.SECONDS);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        // 配置文件中设置不登录查看周报时，若已登录，则显示访问者
        if(SUMMARY_NEED_LOGIN == 0) {
            json = stringRedisTemplate.opsForValue().get(SESSION_PRE+":"+token);
            if(json == null || json == "" || StringUtils.isEmpty(json)) {
                summaryInfo.setVisit(null);
                return summaryInfo;
            }
        }
        LoginUserInfo loginUser = JsonUtils.jsonToPojo(json, LoginUserInfo.class);
        // 判断是否为管理员，并判断是否推荐
        if (loginUser.getUserType().getValue() == 0 || loginUser.getUserType().getValue() == 1) {
            if(summary.getRecommend() != null && summary.getRecommend() == 1) {
                summaryInfo.setCheckReco("1");
            }else {
                summaryInfo.setCheckReco("0");
            }
        }
        // 取访问记录(由于在一个方法中对访问记录进行了存入读取，所以不存入redis)
        SummaryVisitExample example2=new SummaryVisitExample();
        com.xinqushi.entity.SummaryVisitExample.Criteria createCriteria2 = example2.createCriteria();
        createCriteria2.andSidEqualTo(id);
        List<SummaryVisit> visitors = summaryVisitMapper.selectByExample(example2);
        // 将浏览记录存入数据库
        SummaryVisit summaryVisit=new SummaryVisit();
        summaryVisit.setName(loginUser.getName());
        summaryVisit.setFlag(2);
        summaryVisit.setSid(id);
        // 防止浏览记录重复存入
        SummaryVisitExample example3 = new SummaryVisitExample();
        Criteria createCriteria3 = example3.createCriteria();
        createCriteria3.andSidEqualTo(id);
        createCriteria3.andNameEqualTo(loginUser.getName());
        List<SummaryVisit> visits = summaryVisitMapper.selectByExample(example3);
        if(visits == null || visits.size() == 0) {
            summaryVisitMapper.insertSelective(summaryVisit);
            visitors.add(summaryVisit);
        }
        summaryInfo.setVisit(visitors);
        return summaryInfo;
    }
    
    /*
     * 通过周报id和comment保存周报评论
     */
    @Override
    public MemberResult addComment(String comment, int sumid, HttpServletRequest request) {
        // 从cookie获取token
        String token = CookieUtils.getCookieValue(request, COOKIE_TOKEN_KEY);
        if(getMemberByToken(token) == null) {
            return null;
        }
        Summary summary = new Summary();
        summary.setTime(SummaryTitle.getTime());
        summary.setPid(sumid);
        summary.setContent(comment);
        summary.setAid(0);
        // 根据token从redis获取信息
        String json = "";
        try {
            json = stringRedisTemplate.opsForValue().get(SESSION_PRE+":"+token);
            // 把json转换成loginuserinfo
            if (null == json || json.isEmpty() || json == "") {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        LoginUserInfo loginUser = JsonUtils.jsonToPojo(json, LoginUserInfo.class);
        // 判断用户类型，分别存入mid，eid，aid
        if(loginUser.getUserType().getValue() == 0 || loginUser.getUserType().getValue() == 1) {
            summary.setAid(loginUser.getId());
        }else if(loginUser.getUserType().getValue() == 2){
            summary.setMid(loginUser.getId());
        }else if(loginUser.getUserType().getValue() == 3){
            summary.setEid(loginUser.getId());
        }else {
            summary.setCid(loginUser.getId());
        }
        summaryMapper.insertSelective(summary);
        // 若评论者为管理员，则已审核
        Summary summary2 = new Summary();
        if(getMemberByToken(token).getAid() != 0 && getMemberByToken(token).getAid() != null) {
            summary2.setIscheck(true);
            summary2.setId(sumid);
            summaryMapper.updateByPrimaryKeySelective(summary2);
        }
        // 删除redis中评论信息
        try {
            stringRedisTemplate.delete(COMMENT_GET+":"+sumid);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return MemberResult.ok();
    }
    
    /*
     * 通过周报id获取评论
     */
    @Override
    public List<CommentInfo> getComment(int sumid) {
        // 从redis中获取评论信息
        try {
            String str = stringRedisTemplate.opsForValue().get(COMMENT_GET+":"+sumid);
            if(!StringUtils.isEmpty(str)) {
                List<CommentInfo> listRedis = JsonUtils.jsonToList(str, CommentInfo.class);
                stringRedisTemplate.expire(COMMENT_GET+":"+sumid, SUMMARY_SESSION_TIME, TimeUnit.SECONDS);
                return listRedis;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        SummaryExample example = new SummaryExample();
        com.xinqushi.entity.SummaryExample.Criteria createCriteria = example.createCriteria();
        createCriteria.andPidEqualTo(sumid);
        List<Summary> summaries = summaryMapper.selectByExampleWithBLOBs(example);
        List<CommentInfo> list = new ArrayList<>();
        // 因为summaries中不包含评论者名字，需通过admin，experience，clue，member表查询
        for(Summary summary:summaries) {
            // 新增CommentInfo的pojo，其中包含评论者名字，内容，时间相关信息
            CommentInfo commentInfo = new CommentInfo();
            commentInfo.setTime(summary.getTime());
            commentInfo.setContent(summary.getContent());
            // 通过判断aid，cid，eid，mid确定评论者身份并读取出评论者信息，存入list
            if(summary.getAid() != null && summary.getAid() != 0) {
                Admin admin = adminMapper.selectByPrimaryKey(summary.getAid());
                commentInfo.setAdmin(admin);
                list.add(commentInfo);
            }else if(summary.getCid() != null && summary.getCid() != 0) {
                Clue clue = clueMapper.selectByPrimaryKey(summary.getCid());
                commentInfo.setClue(clue);
                list.add(commentInfo);
            }else if(summary.getEid() != null && summary.getEid() != 0) {
                Experience experience = experienceMapper.selectByPrimaryKey(summary.getEid());
                commentInfo.setExperience(experience);
                list.add(commentInfo);
            }else {
                MemberExample example2 = new MemberExample();
                example2.createCriteria().andUidEqualTo(summary.getMid());
                List<Member> members = memberMapper.selectByExample(example2);
                commentInfo.setMember(members.get(0));
                list.add(commentInfo);
            }
        }
        if(list == null || list.size() == 0) {
            return null;
        }
        // 将评论存入redis
        if(list != null) {
            String string = JsonUtils.objectToJson(list);
            try {
                stringRedisTemplate.opsForValue().set(COMMENT_GET+":"+sumid, 
                        string, SUMMARY_SESSION_TIME, TimeUnit.SECONDS);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return list;
    }
    
    // 获取当前用户下一篇周报
    @Override
    public String nextSummaryIdByUserId(int sumid) {
        Summary sum = summaryMapper.selectByPrimaryKey(sumid);
        SummaryExample example = new SummaryExample();
        com.xinqushi.entity.SummaryExample.Criteria createCriteria = example.createCriteria();
        createCriteria.andPidEqualTo(0);
        // 设置查询条件，并解决老数据库cid，mid,eid可能为null的bug
        if(sum.getMid() != null) {
            createCriteria.andMidEqualTo(sum.getMid());
        }
        if(sum.getAid() != null) {
            createCriteria.andAidEqualTo(sum.getAid());
        }
        if(sum.getCid() != null) {
            createCriteria.andCidEqualTo(sum.getCid());
        }
        if(sum.getEid() != null) {
            createCriteria.andEidEqualTo(sum.getEid());
        }
        // 通过aid，cid，mid，eid，pid查询该用户所有周报
        List<Summary> list = summaryMapper.selectByExampleWithBLOBs(example);
        Iterator<Summary> it= list.iterator();
        // 遍历周报查询结果，若当前用户当前周报存在下一篇周报，则返回下一篇周报id
        while(it.hasNext()) {
            Summary summary = it.next();
            if(summary.getId() == sumid) {
                if(it.hasNext()) {
                    return it.next().getId()+"";
                } 
            }
        }
        return "0";
    }
    
    // 获取当前用户上一篇周报
    @Override
    public String preSummaryIdByUserId(int sumid) { 
        Summary sum = summaryMapper.selectByPrimaryKey(sumid);
        SummaryExample example = new SummaryExample();
        com.xinqushi.entity.SummaryExample.Criteria createCriteria = example.createCriteria();
        createCriteria.andPidEqualTo(0);
        // 设置查询条件，并解决老数据库cid，mid,eid可能为null的bug
        if(sum.getMid() != null) {
            createCriteria.andMidEqualTo(sum.getMid());
        }
        if(sum.getAid() != null) {
            createCriteria.andAidEqualTo(sum.getAid());
        }
        if(sum.getCid() != null) {
            createCriteria.andCidEqualTo(sum.getCid());
        }
        if(sum.getEid() != null) {
            createCriteria.andEidEqualTo(sum.getEid());
        }
        // 通过aid，cid，mid，eid，pid查询该用户所有周报
        List<Summary> list = summaryMapper.selectByExampleWithBLOBs(example);
        int i;
        // 若当前周报为第一篇周报，则返回"0",前端通过返回值判断是否为第一篇周报
        if(list.get(0).getId() == sumid) {
            return "0";
        }else {
            for(i=0;i<list.size();i++) {
                if(list.get(i).getId() == sumid) {
                    // 取当前用户当前周报的上一篇周报id
                    String string = list.get(i-1).getId()+"";
                    return string;
                }
            }
        }
        return "0";
    }
    
    // 获取当前用户当前周报
    @Override
    public String currentSummaryIdByUserId(int sumid) {
        Summary sum = summaryMapper.selectByPrimaryKey(sumid);
        SummaryExample example = new SummaryExample();
        com.xinqushi.entity.SummaryExample.Criteria createCriteria = example.createCriteria();
        createCriteria.andPidEqualTo(0);
        // 设置查询条件，并解决老数据库cid，mid,eid可能为null的bug
        if(sum.getMid() != null) {
            createCriteria.andMidEqualTo(sum.getMid());
        }
        if(sum.getAid() != null) {
            createCriteria.andAidEqualTo(sum.getAid());
        }
        if(sum.getCid() != null) {
            createCriteria.andCidEqualTo(sum.getCid());
        }
        if(sum.getEid() != null) {
            createCriteria.andEidEqualTo(sum.getEid());
        }
        // 通过aid，cid，mid，eid，pid查询该用户所有周报
        List<Summary> list = summaryMapper.selectByExampleWithBLOBs(example);
        // 获取当前用户最后一篇周报id
        String string =list.get(list.size()-1).getId()+"";
        return string;
    }
    
    // 获取当前用户第一篇周报
    @Override
    public String firstSummaryIdByUserId(int sumid) {
        Summary sum = summaryMapper.selectByPrimaryKey(sumid);
        SummaryExample example = new SummaryExample();
        com.xinqushi.entity.SummaryExample.Criteria createCriteria = example.createCriteria();
        createCriteria.andPidEqualTo(0);
        // 设置查询条件，并解决老数据库cid，mid,eid可能为null的bug
        if(sum.getMid() != null) {
            createCriteria.andMidEqualTo(sum.getMid());
        }
        if(sum.getAid() != null) {
            createCriteria.andAidEqualTo(sum.getAid());
        }
        if(sum.getCid() != null) {
            createCriteria.andCidEqualTo(sum.getCid());
        }
        if(sum.getEid() != null) {
            createCriteria.andEidEqualTo(sum.getEid());
        }
        // 通过aid，cid，mid，eid，pid查询该用户所有周报
        List<Summary> list = summaryMapper.selectByExampleWithBLOBs(example);
        // 获取当前用户第一篇周报id
        String string = list.get(0).getId()+"";
        return string;
    }
    
    //  根据审核状态，周报所属，周报标题，页码查询周报列表
    @Override
    public String getSummarysByNameWithPage(String checkType, String ownerType, String weekType, String title, int page2,int rows)
            throws ParseException {
        // 设置是否审核的查询条件
        boolean ischeck = false;
        if(checkType.equals("checked")) {
            ischeck = true;
        }
        // 设置周报状态(全部，上一周，本周，下一周)的查询条件
        String newTitle = "";
        if (weekType.equals("previous")) {
            if (title.equals("")) {
                newTitle = SummaryTitle.getPreWeekTitleSec(SummaryTitle.getWriteTitle());
            } else {
                newTitle = SummaryTitle.getPreWeekTitleSec(title);
            }
        }
        if (weekType.equals("current")) {
            newTitle = SummaryTitle.getWriteTitle();
        }
        if (weekType.equals("next")) {
            if (title.equals("")) {
                newTitle = SummaryTitle.getNextWeekTitleSec(SummaryTitle.getWriteTitle());
            } else {
                newTitle = SummaryTitle.getNextWeekTitleSec(title);
            }
        }
        SummaryExample example = new SummaryExample();
        com.xinqushi.entity.SummaryExample.Criteria createCriteria = example.createCriteria();
        if(!weekType.equals("all")) {
            createCriteria.andTitleEqualTo(newTitle);
        }
        if(!checkType.equals("all")) {
            createCriteria.andIscheckEqualTo(ischeck);
        }
        if(!ownerType.equals("所有人")) {
            // 根据前端传来的名字查询对应的mid，aid，cid，eid，并根据查询结果设置对应id的查询条件
            MemberExample memberExample = new MemberExample();
            memberExample.createCriteria().andNameEqualTo(ownerType);
            List<Member> members = memberMapper.selectByExample(memberExample);
            // 设置mid为查询条件
            if(members != null && members.size() !=0) {
                createCriteria.andMidEqualTo(members.get(0).getId());
            }else{
                AdminExample adminExample = new AdminExample();
                adminExample.createCriteria().andRealnameEqualTo(ownerType);
                List<Admin> admins = adminMapper.selectByExample(adminExample);
                // 设置aid为查询条件
                if(admins != null && admins.size() != 0) {
                    createCriteria.andAidEqualTo(admins.get(0).getId());
                }else {
                    ExperienceExample experienceExample = new ExperienceExample();
                    experienceExample.createCriteria().andNameEqualTo(ownerType);
                    List<Experience> experiences = experienceMapper.selectByExample(experienceExample);
                    // 设置eid为查询条件
                    if(experiences != null && experiences.size() !=0) {
                        createCriteria.andEidEqualTo(experiences.get(0).getId());
                    }else {
                        ClueExample clueExample = new ClueExample();
                        clueExample.createCriteria().andRealnameEqualTo(ownerType);
                        List<Clue> clues = clueMapper.selectByExample(clueExample);
                        // 设置cid为查询条件
                        if(clues != null && clues.size() !=0) {
                            createCriteria.andEidEqualTo(clues.get(0).getId());
                        }
                    }
                }
            } 
        }
        createCriteria.andPidEqualTo(0);
        PageHelper.startPage(page2, rows, "id desc");
        List<Summary> list=summaryMapper.selectByExampleWithBLOBs(example);
        // 遍历summary的list，将包含用户姓名及小助手名字的周报列表存入新的list中
        List<SummaryList> list2=new ArrayList<>();
        for(Summary summary1:list) {
            SummaryList summaryList = new SummaryList();
            summaryList.setId(summary1.getId());
            summaryList.setTitle(summary1.getTitle());
            summaryList.setTime(summary1.getTime());
            summaryList.setRecommend(summary1.getRecommend());
            summaryList.setIscheck(summary1.getIscheck());
            // 若该用户为member，则设置对应名字和小助手名字
            if(summary1.getMid() !=0 && summary1.getMid() != null) {
                Member member = memberMapper.selectByPrimaryKey(summary1.getMid());
                if(member != null && member.getName() != null) {
                    summaryList.setName(member.getName());
                    Admin admin = adminMapper.selectByPrimaryKey(member.getAid());
                    if(admin != null && admin.getRealname() !=null) {
                        summaryList.setArealname(admin.getRealname());
                    }else {
                        summaryList.setArealname("");
                    }
                }
             // 若该用户为experience，则设置对应名字和小助手名字
            }else if (summary1.getEid() != 0 && summary1.getEid() != null) {
                Experience experience = experienceMapper.selectByPrimaryKey(summary1.getEid());
                if(experience != null && experience.getName() != null) {
                    summaryList.setName(experience.getName());
                    Admin admin = adminMapper.selectByPrimaryKey(experience.getAid());
                    if(admin != null && admin.getRealname() !=null) {
                        summaryList.setArealname(admin.getRealname());
                    }else {
                        summaryList.setArealname("");
                    }
                }
             // 若该用户为clue，则设置对应名字和小助手名字
            }else if(summary1.getCid() !=0 && summary1.getCid() != null) {
                Clue clue = clueMapper.selectByPrimaryKey(summary1.getCid());
                if(clue != null && clue.getRealname() != null) {
                    summaryList.setName(clue.getRealname());
                    Admin admin = adminMapper.selectByPrimaryKey(clue.getAid());
                    if(admin != null && admin.getRealname() !=null) {
                        summaryList.setArealname(admin.getRealname());
                    }else {
                        summaryList.setArealname("");
                    }
                }
             // 若该用户为admin，则设置对应名字和小助手名字
            }else {
                Admin admin = adminMapper.selectByPrimaryKey(summary1.getAid());
                if(admin != null && admin.getRealname() != null) {
                    summaryList.setName(admin.getRealname());
                    summaryList.setArealname("");
                }
            }
            list2.add(summaryList);
        }
        PageInfo<Summary> pageInfo=new PageInfo<>(list);
        // 新建一个map，存入查询周报页面的周报列表信息
        HashMap<String, Object> returnMap = new HashMap<String, Object>();
        returnMap.put("totalCount", pageInfo.getTotal());
        returnMap.put("list", list2);
        returnMap.put("title", newTitle);
        String titleTip = "";
        if (!title.equals("all")) {
            titleTip = "当前周报时间：" + newTitle;
        }
        returnMap.put("titleTip", titleTip);
        JSONObject json = new JSONObject();
        try {
            json.put("returnMap", returnMap);
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return json.toString();
    }
    
    /*
     *  查找周报页面， 获取所有人名字.且排除重复的名字！
     */
    @Override
    public List<AllNames> getAllNames() {
        List<Member> mnames = memberMapper.getAllIdAndName();
        List<Admin> anames = adminMapper.getAllIdAndName();
        List<Clue> cnames = clueMapper.getAllIdAndName();
        List<Experience> enames = experienceMapper.getAllIdAndName();
        List<AllNames> allNames = new ArrayList<>();
        // 1.新建AllNames的pojo，含有name属性，先存入member名字
        for(Member member:mnames) {
            AllNames allName = new AllNames(); 
            if(member.getName() != null && member.getName() != "" ) {
                allName.setName(member.getName());
                allNames.add(allName);
            }
        }
        // 2.遍历并存入admin名字，并排除list中已有名字
        for(Admin admin:anames) {
            AllNames newName = new AllNames();
            if(admin.getRealname() != null && admin.getRealname() != "" ) {
                List<AllNames> allNames1 = allNames;
                boolean flag = false;
                for(AllNames allName1:allNames1) {
                    if(allName1.getName().equals(admin.getRealname())) {
                        flag = true;
                        break;
                    }
                }
                if(!flag) {
                    newName.setName(admin.getRealname());
                    allNames.add(newName);
                }
            }
        }
        // 3.遍历并存入clun名字，并排除list中已有名字
        for(Clue clue:cnames) {
            AllNames newName = new AllNames();
            if(clue.getRealname() != null && clue.getRealname() != "" ) {
                List<AllNames> allNames1 = allNames;
                boolean flag = false;
                for(AllNames allName1:allNames1) {
                    if(allName1.getName().equals(clue.getRealname())) {
                        flag = true;
                        break;
                    }
                }
                if(!flag) {
                    newName.setName(clue.getRealname());
                    allNames.add(newName);
                }
            }
        }
        // 4.遍历并存入experience名字，并排除list中已有名字
        for(Experience experience:enames) {
            AllNames newName = new AllNames();
            if(experience.getName() != null && experience.getName() != "" ) {
                List<AllNames> allNames1 = allNames;
                boolean flag = false;
                for(AllNames allName1:allNames1) {
                    if(allName1.getName().equals(experience.getName())) {
                        flag = true;
                        break;
                    }
                }
                if(!flag) {
                    newName.setName(experience.getName());
                    allNames.add(newName);
                }
            }
        }
        return allNames;
    }
    /*
     * 推荐周报
     */
    @Override
    public MemberResult referralSumarry(int id) {
        Summary summary =new Summary();
        summary.setId(id);
        summary.setRecommend(1);
        summaryMapper.updateByPrimaryKeySelective(summary);
        stringRedisTemplate.delete(SUMMARY_INFO+":"+id);
        return MemberResult.ok();
    }
    /*
     * 取消推荐周报
     */
    @Override
    public MemberResult cancelSumarrySumarry(int id) {
        Summary summary =new Summary();
        summary.setId(id);
        summary.setRecommend(0);
        summaryMapper.updateByPrimaryKeySelective(summary);
        stringRedisTemplate.delete(SUMMARY_INFO+":"+id);
        return MemberResult.ok();
    }
}
