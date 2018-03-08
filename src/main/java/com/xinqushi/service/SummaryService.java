package com.xinqushi.service;


import java.text.ParseException;
import java.util.List;


import javax.servlet.http.HttpServletRequest;

import com.xinqushi.common.pojo.CommentInfo;
import com.xinqushi.common.pojo.EUDataGridResult;
import com.xinqushi.common.pojo.SummaryInfo;
import com.xinqushi.utils.MemberResult;
import com.xinqushi.web.pojo.AllNames;

public interface SummaryService {
    MemberResult saveSummary(String contents,HttpServletRequest request) throws ParseException;
    MemberResult getCurrentSummary(HttpServletRequest request) throws ParseException;
    EUDataGridResult getSummaryList(int page, HttpServletRequest request, int rows);
    SummaryInfo getSummaryById(int id, HttpServletRequest request);
    MemberResult addComment(String comment, int sumid, HttpServletRequest request);
    List<CommentInfo> getComment(int sumid);
    String nextSummaryIdByUserId(int sumid);
    String preSummaryIdByUserId(int sumid);
    String currentSummaryIdByUserId(int sumid);
    String firstSummaryIdByUserId(int sumid);
    String getSummarysByNameWithPage(String checkType, String ownerType, String weekType, String title, int page2,int rows) throws ParseException;
    List<AllNames> getAllNames();
    MemberResult referralSumarry(int id);
    MemberResult cancelSumarrySumarry(int id);
}
