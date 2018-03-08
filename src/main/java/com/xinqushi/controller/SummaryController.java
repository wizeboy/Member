package com.xinqushi.controller;

import java.io.IOException;






import java.io.PrintWriter;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.websocket.server.PathParam;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.druid.util.StringUtils;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.xinqushi.common.pojo.CommentInfo;
import com.xinqushi.common.pojo.EUDataGridResult;
import com.xinqushi.common.pojo.SummaryInfo;
import com.xinqushi.entity.Summary;
import com.xinqushi.service.SummaryService;
import com.xinqushi.service.UserLoginService;
import com.xinqushi.utils.CookieUtils;
import com.xinqushi.utils.JsonUtils;
import com.xinqushi.utils.MemberResult;
import com.xinqushi.utils.PinYinUtil;
import com.xinqushi.web.pojo.AllNames;




@Controller
public class SummaryController {
    @Value("${SUMMARY_LIST2_ROWS}")
    private int SUMMARY_LIST2_ROWS;
    @Autowired
    private SummaryService summaryService;
    
    @RequestMapping("/summary/test")
    @ResponseBody
    public String test() {
        return "222";
    }
    @RequestMapping("/test1")
    @ResponseBody
    public String test1() {
        return "222";
    }

    /*
     * 保存及修改周报功能，若该用户当周未填写周报，则为保存，若已填写，则为修改
     */
    @RequestMapping(value="/summary/save")
    @ResponseBody
    public String  weeklySave(@RequestParam String contents, HttpServletResponse response, HttpServletRequest request) throws IOException, ParseException {
        MemberResult result=null;
        if(contents!=null) {
            result = summaryService.saveSummary(contents,request);
        }
        PrintWriter out = response.getWriter();
        if(result !=null) {
            if(result.getStatus() == 200) {
                out.print("1");
            }else if(result.getStatus() == 300){
                out.print("2");
            }else {
                out.println("3");
            }
        }else {
            out.println("3");
        }
        return null;
        
    }
    
    /*
     * 获取当周周报，若该用户已保存，则可自动获取已保存周报，方便修改！
     */
    @RequestMapping(value="/summary/getCurrentSummary")
    @ResponseBody
    public String getCurrentSummary( HttpServletRequest request) throws IOException, ParseException {
        MemberResult result=summaryService.getCurrentSummary(request);
        if(result != null) {
            String json = JsonUtils.objectToJson(result.getData());
            Summary summary = JsonUtils.jsonToPojo(json, Summary.class);
            String content = summary.getContent();
            return content;
        }
        return null;
    }
    /*
     * 将登录失效时已编辑好但未保存的周报存入session（防止填写周报时间过长，导致登录失效后重新登录时已写周报丢失）
     */
    @RequestMapping(value="/summary/saveTextIntoSession")
    @ResponseBody
    public String saveTextIntoSession( HttpSession session, @RequestParam String summary) throws IOException {
        session.setAttribute("summary", summary);
        return null;
    }
    /*
     * 从session取得登录失效时已编辑好但未保存的周报（登录失效重新登录后获取已填写周报）
     */
    @RequestMapping(value="/summary/getTextIntoSession")
    @ResponseBody
    public String getTextIntoSession( HttpSession session, HttpServletResponse response) throws IOException {
        String  summary = (String) session.getAttribute("summary");
        PrintWriter out = response.getWriter();
        if(summary != null) {
            //将返回的string的格式转换成iso8859-1，因为springmvc处理字符串编码为ISO-8859-1
            summary=new String(summary.getBytes("utf-8"),"iso8859-1");
            out.print(summary);
        }
        return null;
    }
    
    /*
     * 获取我的周报列表，包含姓名，标题，时间，状态，周报id等
     */
    @RequestMapping(value="/summary/getSummaryList")
    @ResponseBody
    public String  getSummaryList(@RequestParam int page, HttpServletRequest request) throws IOException {
        int rows = SUMMARY_LIST2_ROWS;
        EUDataGridResult result=summaryService.getSummaryList(page, request,rows);
        if(result == null) {
            return "0";
        }
        String summarys = JsonUtils.objectToJson(result);
        return summarys;
    }
    
    /*
     * 获取我的周报导航键
     */
    @RequestMapping("/summary/getNavigation")
    @ResponseBody
    public String getNavigation (@RequestParam int page, HttpServletRequest request) throws IOException {
        String summarys = getSummaryList(page, request);
        EUDataGridResult result = JsonUtils.jsonToPojo(summarys, EUDataGridResult.class);
        List<?> list = result.getRows();
        int PageCount =(int) (result.getTotal() / SUMMARY_LIST2_ROWS);
        if(result.getTotal() % SUMMARY_LIST2_ROWS != 0) {
            PageCount++;
        }
        int btns=4;
        btns=btns / 2;
        int start=page-btns+1;
        int end=page+btns; 
        if(start<1) {
            start=1; 
            end=start+btns*2-1; 
        }
        if(end>=PageCount) {
            end=PageCount; 
            start=end-btns*2+1;
            if(start<1) {
                start=1;
            }
        }
        StringBuilder str=new StringBuilder(); //建立一个有append方法的字符串对象
        str.append("<a href='javascript:void(0)' onclick='show("+1+")' class='btn btn-default btn-circle'>");
        str.append("<&nbsp;&nbsp;<");
        str.append("</a>");
        str.append("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
        for(int i=start;i<=end;i++) {
            if(i==page) {   //若为当前页，无超链接
                str.append("<font color='red' class='btn btn-default btn-circle'><b>");
                str.append(i); 
                str.append("</b></font>");
                str.append("&nbsp;&nbsp;");
            }else {
                str.append("<a href='javascript:void(0)' onclick='show("+i+")' class='btn btn-default btn-circle'>");
                str.append(i);
                str.append("</a>");
                str.append("&nbsp;&nbsp;");
            }
        }
        str.append("&nbsp;&nbsp;&nbsp;&nbsp;");
        str.append("<a href='javascript:void(0)' onclick='show("+PageCount+")' class='btn btn-default btn-circle'>");
        str.append(">&nbsp;&nbsp;>");
        str.append("</a>");
        str.append("<br/>");
        return str.toString();
    }
    /*
     * 通过周报id获取周报
     */
    @RequestMapping("/summary/getSummaryById")
    @ResponseBody
    public String getSummaryById(@RequestParam int id, HttpServletRequest request) {
        SummaryInfo summaryById = summaryService.getSummaryById(id, request);
        if(summaryById == null) {
            return "0";
        }
        String summary = JsonUtils.objectToJson(summaryById);
        return summary;
    }
    /*
     * 通过周报id和comment保存周报评论
     */
    @RequestMapping("/summary/addComment")
    @ResponseBody
    public String  addComment(@RequestParam String comment,@RequestParam int sumid, HttpServletRequest request) {
        MemberResult result = summaryService.addComment(comment, sumid, request);
        if(result != null) {
            return "1";
        }
        return "0";
    }
    /*
     * 通过周报id获取评论
     */
    @RequestMapping("/summary/getComment")
    @ResponseBody
    public String  getComment(@RequestParam int sumid) {
        List<CommentInfo> result = summaryService.getComment(sumid);
        if(result != null) {
            String json = JsonUtils.objectToJson(result);
            return json;
        }
        return null;
    }
    /*
     * 通过周报id获得当前用户下一周周报
     */
    @RequestMapping("/summary/getNextSummaryIdByUserId")
    @ResponseBody
    public String getNextSummaryIdByUserId(@RequestParam int sumid) {
        String string= summaryService.nextSummaryIdByUserId(sumid);
        return string;
    }
    /*
     * 通过周报id获得当前用户上一周周报
     */
    @RequestMapping("/summary/getPreSummaryIdByUserId")
    @ResponseBody
    public String getPreSummaryIdByUserId( @RequestParam int sumid) {
        String string= summaryService.preSummaryIdByUserId(sumid);
        return string;
    }    
    /*
     * 通过周报id获得当前用户当周周报
     */
    @RequestMapping("/summary/getCurrentSummaryIdByUserId")
    @ResponseBody
    public String getCurrentSummaryIdByUserId(@RequestParam int sumid) {
        String string= summaryService.currentSummaryIdByUserId(sumid);
        return string;
    }    
    /*
     * 通过周报id获得当前用户第一周周报
     */
    @RequestMapping("/summary/getFirstSummaryIdByUserId")
    @ResponseBody
    public String getFirstSummaryIdByUserId(@RequestParam int sumid) {
        String string= summaryService.firstSummaryIdByUserId(sumid);
        return string;
    }
    /*
     * 查找周报页面，根据审核状态，周报所属，周报标题，页码查询周报列表
     */
    @RequestMapping("/summary/getSummarysByNameWithPage")
    @ResponseBody
    public String getSummarysByNameWithPage(@RequestParam String checkType,@RequestParam String ownerType,@RequestParam String weekType,@RequestParam String title,@RequestParam int page2) {
        String string = "";
        try {
            int rows = SUMMARY_LIST2_ROWS;
            string = summaryService.getSummarysByNameWithPage(checkType,ownerType,weekType,title,page2,rows);
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return string;
    }
    /*
     * 查找周报页面，点击导航栏不刷新导航栏的周报页面导航键（防止选择页面时重新进行搜索）
     */
    @RequestMapping("/summary/getNavigationNoSync")
    @ResponseBody
    public String getNavigations (@RequestParam String checkType,@RequestParam String ownerType,@RequestParam String weekType,@RequestParam String title,@RequestParam int page2) throws IOException {
        String json = getSummarysByNameWithPage(checkType, ownerType, weekType, title, page2);
        JSONObject jsonObject = new JSONObject(json);
        Map returnMap = JsonUtils.jsonToPojo(jsonObject.get("returnMap").toString(), Map.class);
        List<?> list = (List<?>) returnMap.get("list");
        int totalCount = (int) returnMap.get("totalCount");
        int PageCount =(int) (totalCount / SUMMARY_LIST2_ROWS);
        if(totalCount % SUMMARY_LIST2_ROWS != 0) {
            PageCount++;
        }
        int btns=4;
        btns=btns / 2;
        int start=page2-btns+1;
        int end=page2+btns; 
        if(start<1) {
            start=1; 
            end=start+btns*2-1; 
        }
        if(end>=PageCount) {
            end=PageCount; 
            start=end-btns*2+1;
            if(start<1) {
                start=1;
            }
        }
        StringBuilder str=new StringBuilder(); //建立一个有append方法的字符串对象
        str.append("<a href='javascript:void(0)' onclick='NoSync(\""+checkType+"\",\""+ownerType+"\",\""+weekType+"\",\""+title+"\","+1+")' class='btn btn-default btn-circle'>");
        str.append("<&nbsp;&nbsp;<");
        str.append("</a>");
        str.append("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
        for(int i=start;i<=end;i++) {
            if(i==page2) {   //若为当前页，无超链接
                str.append("<font color='red' class='btn btn-default btn-circle'><b>");
                str.append(i); 
                str.append("</b></font>");
                str.append("&nbsp;&nbsp;");
            }else {
                str.append("<a href='javascript:void(0)' onclick='NoSync(\""+checkType+"\",\""+ownerType+"\",\""+weekType+"\",\""+title+"\","+i+")' class='btn btn-default btn-circle '>");
                str.append(i);
                str.append("</a>");
                str.append("&nbsp;&nbsp;");
            }
        }
        str.append("&nbsp;&nbsp;&nbsp;&nbsp;");
        str.append("<a href='javascript:void(0)' onclick='NoSync(\""+checkType+"\",\""+ownerType+"\",\""+weekType+"\",\""+title+"\","+PageCount+")'  class='btn btn-default btn-circle'>");
        str.append(">&nbsp;&nbsp;>");
        str.append("</a>");
        str.append("<br/>");
        return str.toString();
    }
    /*
     * 查找周报页面，点击查找周报，同时刷新导航栏
     */
    @RequestMapping("/summary/getNavigationSync")
    @ResponseBody
    public String getNavigationSync (@RequestParam String checkType,@RequestParam String ownerType,@RequestParam String weekType,@RequestParam String title,@RequestParam int page2) throws IOException, JSONException {
        String json = getSummarysByNameWithPage(checkType, ownerType, weekType, title, page2);
        JSONObject jsonObject = new JSONObject(json);
        Map returnMap = JsonUtils.jsonToPojo(jsonObject.get("returnMap").toString(), Map.class);
        List<?> list = (List<?>) returnMap.get("list");
        int totalCount = (int) returnMap.get("totalCount");
        int PageCount =(int) (totalCount / SUMMARY_LIST2_ROWS);
        if(totalCount % SUMMARY_LIST2_ROWS != 0) {
            PageCount++;
        }
        int btns=4;
        btns=btns / 2;
        int start=page2-btns+1;
        int end=page2+btns; 
        if(start<1) {
            start=1; 
            end=start+btns*2-1; 
        }
        if(end>=PageCount) {
            end=PageCount; 
            start=end-btns*2+1;
            if(start<1) {
                start=1;
            }
        }
        StringBuilder str=new StringBuilder(); //建立一个有append方法的字符串对象
        str.append("<a href='javascript:void(0)' onclick='NoSync(\""+checkType+"\",\""+ownerType+"\",\""+weekType+"\",\""+title+"\","+1+")' class='btn btn-default btn-circle'>");
        str.append("<&nbsp;&nbsp;<");
        str.append("</a>");
        str.append("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
        for(int i=start;i<=end;i++) {
            if(i==page2) {   //若为当前页，无超链接
                str.append("<font color='red' class='btn btn-default btn-circle'><b>");
                str.append(i); 
                str.append("</b></font>");
                str.append("&nbsp;&nbsp;");
            }else {
                str.append("<a href='javascript:void(0)' onclick='NoSync(\""+checkType+"\",\""+ownerType+"\",\""+weekType+"\",\""+title+"\","+i+")' class='btn btn-default btn-circle'>");
                str.append(i);
                str.append("</a>");
                str.append("&nbsp;&nbsp;");
            }
        }
        str.append("&nbsp;&nbsp;&nbsp;&nbsp;");
        str.append("<a href='javascript:void(0)' onclick='NoSync(\""+checkType+"\",\""+ownerType+"\",\""+weekType+"\",\""+title+"\","+PageCount+")' class='btn btn-default btn-circle'>");
        str.append(">&nbsp;&nbsp;>");
        str.append("</a>");
        str.append("<br/>");
        return str.toString();
    }
    /*
     * 查找周报页面，选择人名功能
     */
    @ResponseBody
    @RequestMapping("/summary/getAllNames")
    public List<AllNames> transformName(Character letter) {
        List<AllNames> list = summaryService.getAllNames();
        List<AllNames> returnlist = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            String pinYin = PinYinUtil.getPinYin(list.get(i).getName());
            char first = 0;
            if(!pinYin.isEmpty()&&pinYin!=null&&pinYin!=""&&!StringUtils.isEmpty(pinYin)) {
                first = pinYin.charAt(0);
            }
            if (letter == first)
                returnlist.add(list.get(i));
        }
        return returnlist;
    }
    /*
     * 推荐周报
     */
    @ResponseBody
    @RequestMapping("/summary/referralSumarry")
    public MemberResult referralSumarry(@RequestParam int sumid) {
        MemberResult result = summaryService.referralSumarry(sumid);
        return result;
    }
    /*
     * 取消推荐周报
     */
    @ResponseBody
    @RequestMapping("/summary/cancelSumarry")
    public MemberResult cancelSumarry(@RequestParam int sumid) {
        MemberResult result = summaryService.cancelSumarrySumarry(sumid);
        return result;
    }
}
