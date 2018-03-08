$(function(){
	//获取主机地址，如： http://localhost:8081  
	var curWwwPath=window.document.location.href;  
	var pathName=window.document.location.pathname;  
	var pos=curWwwPath.indexOf(pathName);  
	var localhostPath=curWwwPath.substring(0,pos);
	var mid = "";
	
	(function ($) {
        $.getUrlParam = function (name) {
            var reg = new RegExp("(^|&)" + name + "=([^&]*)(&|$)");
            var r = window.location.search.substr(1).match(reg);
            if (r != null) return unescape(r[2]); return null;
        }
	})(jQuery);
    var userType=null;
    var userID=null;
    var id = $.getUrlParam('id');
    getSum(id);
    getComs(id);
    
    // 通过url中获取的id得到查看周报页面的信息
    function getSum(id){
        $.ajaxSetup({async:false});
        
        var provid = "";
        var seat_provid = "";
        $.post(localhostPath + "/summary/getSummaryById",{"id":id},function(data){
        	if(data==0){
        		layer.msg('登录失效，请重新登录', {
				    icon: 1,
				    time: 2000
				});
        		top.window.location = localhostPath + "/login.html?redirect="+ localhostPath + "/summary/all-weekly.html";
        	}
        	var summary = eval("("+data+")");
        	
            $("#cont").html(summary.content);
			//取年月日
			$(".commentcdivtime").html(summary.time.substring(0,11)); 
			$(".commentcdivassistant").html("学习助手："+summary.adminName);	
			$(".commentcdivname").html(summary.name);
			$(".commentcdivseatprovince").html("所在地:" + summary.province);
			$("#imgdiv").html("<img src='data: image/jpeg;base64," + summary.picture + "' alt='用户头像' class='img'>");
			if(summary.recommend == 1)
			{
				$("#images").html("<img src='/images/119.png' class='img-responsive' alt='Responsive image'>");
			}
			// 判断是否为管理员，是否推荐，显示推荐或取消推荐按钮
			if(summary.checkReco == "1"){
				$("#recommendtd").css('display','block');
				$("#recommendtd").html("<a href='javascript:;' id='cancelreco' ><span class='btn  btn-warning'>取消推荐</span></a>");
			}
			if(summary.checkReco == "0"){
				$("#recommendtd").css('display','block');
				$("#recommendtd").html("<a href='javascript:;' id='reco' ><span class='btn  btn-success' style='font-size: 15px;' >推荐</span></a>");
			}
			reco();
			cancelreco();
			// 显示访问者名字
			var visitString = "";
			var visit="";
			if(summary.visit == null){
				$("#visit").html(null);
			}else{
				$.each(summary.visit,function(i) {
					visit += "<span class='label label-info'>" + summary.visit[i].name + "</span>\t";
		        });
				visitString += "<div class='commcontendivcod'><div class='commcontendivcodd'><br />";
				visitString += visit;
				visitString += "<br /><br /></div></div>";
		        $("#visit").html(visitString);
			}
        });
    }
    
    // 会员评论				
	$("#comment").click(function(){	
		$.ajaxSetup({async:false});
		var comment=$("#editor").val();
		//评论不能为空
		if(comment == ""){
			alert("评论内容不能为空");
			return;
		}
		$.post(localhostPath + "/summary/addComment",{"comment":comment,"sumid":id},function(data){
			if(data=="1"){
				//改变summary表中remind字段
				//$.post("${pageContext.request.contextPath}/summary/remind.action",{"sumid":sumId});
				$("#editor").val("");
			}
			else{
				alert("登陆失效，请重新登录！");
				top.window.location = localhostPath + "/login.html?redirect="+localhostPath+"/summary/all-weekly.html" ;
			}		
		});
		getComs(id);
	});
	
	// 获取评论
    function getComs(id){
    	$.ajaxSetup({async:false});
		$.post(localhostPath + "/summary/getComment",{"sumid":id},function(data){
			if(data !="")
			{
				var comment = eval("("+data+")");
				var commentsString="";
				$.each(comment,function(i) {
					var memberid=0;
					var experienceid=0;
					var contentname;
					var contenttime=comment[i].time;
					var content=comment[i].content;
					if(comment[i].member!=null)
					{
						memberid=comment[i].member.id;
						contentname=comment[i].member.name;
					}
					if(comment[i].experience!=null)
					{
						experienceid=comment[i].experience.id;
						contentname=comment[i].experience.name;
					}
					if(comment[i].admin !=null)
					{
						contentname="<font color='red'><b>"+comment[i].admin.realname+"</b></font>";					
					}
					var commentString="<div class='commcontendivcod'>";
					commentString+="<span class='commcontendivcods'>"+contentname+"&nbsp;&nbsp;"+"</span>";
					commentString+="<span class='commcontendivcodsins'>"+contenttime+"</span>";
					// 删除修改评论功能未实现
//					if($("#isAdmin").val()=="admin"||memberid==$("#memberId").val()||experienceid==$("#experienceId").val()){
//						commentString+="<span><a class='icon-delete-small' href='javascript:;' lang="+data.summaries[i].id+"><i class='Hui-iconfont'>&#xe6e2;</i></a></span>";
//					}	
//					if(($("#isAdmin").val()=="admin"&&memberid==0&&experienceid==0)||memberid==$("#memberId").val()||experienceid==$("#experienceId").val()){
//						commentString+="<span><a class='icon-update-small' href='javascript:;' lang="+data.summaries[i].id+"><i class='Hui-iconfont'>&#xe60c;</i></a></span>";
//					}
					commentString+="<div class='commcontendivcodd'>";
					commentString+="<br>";
					commentString+="</div>";	
					commentString+="<div style='width:100%;text-indent:10%'>"+content+"</div>";
					commentString+="<br>";
					
					commentString+="</div>";						
					commentsString+=commentString;	
				});	
				$("#comments").html(commentsString);
				
			}
			else{
				
				var commentString="<div class='commcontendivcod'>";
				commentString+="<div class='commcontendivcodd2'>";
				commentString+="<div class='nocondiv'>";
				commentString+="<br>";
				commentString+="<span class='nocontent'>"+"暂无评论......"+"</span>";
				commentString+="<br>";	
				commentString+="</div>";	
				commentString+="</div>";
				commentString+="</div>";
				$("#comments").html(commentString);
			};
		});
		
    }
    
    // 获取该用户上一周的周报 
	$("#presummary").click(function(){
		$.post(localhostPath + "/summary/getPreSummaryIdByUserId",{"sumid":id},function(data){
			if(data!=null && data!=""){ 
				if(data!="0"){
					location.href = localhostPath+'/summary/look-summary-layer.html?id='+data;		
				}else{
					alert("已是第一周！");
				}
			}
		});
	});
	// 获取该用户下一周的周报
	$("#nextsummary").click(function(){
		$.post(localhostPath + "/summary/getNextSummaryIdByUserId",{"sumid":id},function(data){
			if(data!=null && data!="" ){
				if(data!="0"){
					location.href = localhostPath+'/summary/look-summary-layer.html?id='+data;
				}else{
					alert("已是最后一周");
				}
			}
		}); 
	});
	// 获取该用户当前周（最近一周）的周报
	$("#currentsummary").click(function(){
		$.post(localhostPath + "/summary/getCurrentSummaryIdByUserId",{"sumid":id},function(data){
			if(data!=null && data!="" && data!="0"){
				location.href = localhostPath+'/summary/look-summary-layer.html?id='+data;			
			}
		});
	});	
	// 获取该用户第一周的周报
	$("#firstsummary").click(function(){
		$.post(localhostPath + "/summary/getFirstSummaryIdByUserId",{"sumid":id},function(data){
			if(data!=null && data!="" && data!="0"){
				location.href = localhostPath+'/summary/look-summary-layer.html?id='+data;		
			}
		});
	});
	
	// 推荐周报
	function reco(){
		$("#reco").click(function(){
			$.ajaxSetup({async:false});
			$.post(localhostPath + "/summary/referralSumarry",{"sumid":id},function(data){
				getSum(id);			
				cancelreco();
			});					
		});
	}	
	// 取消推荐
	function cancelreco(){			
		$("#cancelreco").click(function(){
			$.ajaxSetup({async:false});
			$.post(localhostPath + "/summary/cancelSumarry",{"sumid":id},function(data){
				getSum(id);	
				reco();
			});
			location.reload();   // 需刷新本页面，否则图标不消失
		});
	}		
});
