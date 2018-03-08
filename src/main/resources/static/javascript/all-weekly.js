//获取主机地址，如： http://localhost:8081  
var curWwwPath=window.document.location.href;  
var pathName=window.document.location.pathname;  
var pos=curWwwPath.indexOf(pathName);  
var localhostPath=curWwwPath.substring(0,pos);
//第一次点击进来的默认值
var checkType="all";
var ownerType="所有人";
var weekType="current";
var title="";
var page2=1;

$(function(){
	getData(checkType,ownerType,weekType,title,page2);
	sync(checkType,ownerType,weekType,title,page2);
	$("#search-summary").click(function(){
		checkType=$("[name='checkType']").val();
		ownerType=$("[name='ownerType']").val();
		weekType=$("[name='weekType']").val();
		title=$("[name='title']").val();
		page2=1;
		//设置weekType
		if($("#weekType-1").is(":checked"))
		{
			//alert($("#weekType-1").val());
			weekType=$("#weekType-1").val();
		}
		if($("#weekType-2").is(":checked"))
		{
			//alert($("#weekType-2").val());
			weekType=$("#weekType-2").val();
		}
		if($("#weekType-3").is(":checked"))
		{
			//alert($("#weekType-3").val());
			weekType=$("#weekType-3").val();
		}
		if($("#weekType-4").is(":checked"))
		{
			//alert($("#weekType-4").val());
			weekType=$("#weekType-4").val();
		}
		//alert(checkType);
		//alert(ownerType);
		//alert(weekType);
		//alert(title);
		//alert(page2);
		getData(checkType,ownerType,weekType,title,page2);
		sync(checkType,ownerType,weekType,title,page2);
	});	
	function setWeekTypeCheck(weekType)
	{
		if(weekType=="all"){
			$("#weekType-1").attr("checked",true);
		}
		if(weekType=="previous"){
			$("#weekType-2").attr("checked",true);
		}
		if(weekType=="current"){
			$("#weekType-3").attr("checked",true);
		}
		if(weekType=="next"){
			$("#weekType-4").attr("checked",true);
		}
	}
	
	//更改按钮点击事件
	$("#btnscope").click(function(){
		layer.open({
			  type: 2,
			  title: '选择人员',
			  area: ['100%', '500px'],
			 // closeBtn: 0, //不显示关闭按钮
			  shift: 1,
			  shade: 0.5, //开启遮罩关闭
			  content: localhostPath+'/summary/choosename-layer.html',
			  end: function(){
			  }
		});
	});
});

//获取周报列表
function getData(checkType,ownerType,weekType,title,page2){
	$.ajaxSetup({async:false});
	$.post(localhostPath + "/summary/getSummarysByNameWithPage",{"checkType":checkType,"ownerType":ownerType,"weekType":weekType,"title":title,"page2":page2},function(data){
		var dataObj = eval("("+data+")");
		var tatolCount=dataObj.returnMap.totalCount;
		var list=dataObj.returnMap.list;
		var title=dataObj.returnMap.title;
		var titleTip=dataObj.returnMap.titleTip;
		$("#titleTip").html(titleTip);
		$("#titleTip1").html(titleTip);
		$("#TATOLCOUNT").html(tatolCount);
		$("[name='title']").val(title);
		var data = list;
		var line="";
		line=line + "<thead>";
		line=line + "<tr class='info text-center'>";
		line=line + "<th class='ck hidden-xs'><input type='checkbox' name='' value=''></th>";
		line=line + "<th class='info xh text-center'>序号</th>";
		line=line + "<th class='info xm text-center'>姓名</th>";
		line=line + "<th class='info zbbt hidden-xs text-center'>周报标题</th>";	
		line=line + "<th class='info tjsj hidden-xs text-center'>提交时间</th>";
		line=line + "<th class='info xzs hidden-xs text-center'>小助手</th>";
		line=line + "<th class='info zt text-center'>状态</th>";
		line=line + "<th class='info cz text-center'>操作</th>";
		line=line + "</tr>";
		line=line + "</thead>";
		line=line + "<tbody>";
		for(i=0;i<data.length;i++){	
			var title="";
			var time="";
			var assistant="";
			var operation="";
			var  status="";
			var recom="";
			if(data[i].id==0){
				title=$("[name='title']").val();
				status="<span class='label label-warning radius'>未提交</span>";
				if(data[i].arealname!=null){
					assistant=data[i].arealname;
				}			
					
			}else{
				title=data[i].title;
				time=data[i].time;
				if(data[i].arealname!=null){
					assistant=data[i].arealname;
				}			
				if(data[i].ischeck == '0')
				{
					status="<span class='label label-danger radius'>未审核</span>";
				}else{
					status="<span class='label label-success radius'>已审核</span>";
				}
				if(data[i].recommend == '1')
				{
					recom="<span class='layui-badge layui-bg-orange'>荐</span>";

				}
				else
				{
					recom="&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";
				}	
				operation="<a href='javascript:void(0)' class='tit' lang='"+data[i].id+"'>查看</a>";	
			}
			line=line + "<tr class='text-c tr' lang='"+data[i].id+"'>";
			line=line + "<td class='noclick hidden-xs'>" + "<input type='checkbox' name='' value=''>" + "</td>";
			line=line + "<td>" + (i+1) + "</td>";
			line=line + "<td>" + data[i].name + "</td>";
			line=line + "<td class='hidden-xs'>" + title + "</td>";
			line=line + "<td class='hidden-xs'>" + time + "&nbsp;&nbsp;</td>";
			line=line + "<td class='hidden-xs'>" + assistant + "</td>";
			line=line +"<td class='td-status' >";
			line=line+status+ "&nbsp;&nbsp;"+ recom ;
			line=line +"</td>";	
			line=line + "<td class='noclick'>"+operation+"</td>";
			line=line + "</tr>";
		}
		line=line + "</tbody>";
		$("#period").html(line);
	});
	titclick();
	trclick();
}
function titclick(){
	$(".tit").click(function(){
		var id=this.lang;
		showSum(id);
	});
}
function trclick(){
	//火狐对last不支持，在不该被点的td里面机上noclick的class
	//$("tr td:not(':first,:last')").click(function(){
	$("tr td:not(.noclick)").click(function(){
		var id=this.parentNode.lang;
		showSum(id);
	});
}
//查看周报	
function showSum(id){
		//location.href=localhostPath + "/summary/membersumcomment1.jsp?page2="+page2+"&ownerType="+ownerType+"&checkType="+checkType+"&weekType="+weekType+"&title="+title+"&id="+id;
		//window.open(localhostPath + "/summary/membersumcomment1.jsp?id="+id);
	layer.open({
		  type: 2,
		  title: '查看周报',
		  area: ['90%', '500px'],
		 // closeBtn: 0, //不显示关闭按钮
		  shift: 1,
		  shade: 0.5, //开启遮罩关闭
		  content: localhostPath+'/summary/look-summary-layer.html?id='+id,
	});
}
//点击搜索时使用搜索条件，即刷新导航栏
function sync(checkType,ownerType,weekType,title,page2){
	$.post(localhostPath + "/summary/getNavigationSync",{"checkType":checkType,"ownerType":ownerType,"weekType":weekType,"title":title,"page2":page2},function(data){
		$(".page-nav").html(data);
	});
}
//点击导航栏时不使用搜索条件,刷新数据,不刷新导航栏(由页面调用，需放在$(function(){})外面，sync，getDate都放在外面才可正常触发点击与显示)
function NoSync(checkType,ownerType,weekType,title,page2){
	getData(checkType,ownerType,weekType,title,page2);
	$.post(localhostPath + "/summary/getNavigationNoSync",{"checkType":checkType,"ownerType":ownerType,"weekType":weekType,"title":title,"page2":page2},function(data){
		$(".page-nav").html(data);
	});
} 


