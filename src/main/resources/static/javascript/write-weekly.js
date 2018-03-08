$(function(){
	//获取主机地址，如： http://localhost:8081  
	var curWwwPath=window.document.location.href;  
	var pathName=window.document.location.pathname;  
	var pos=curWwwPath.indexOf(pathName);  
	var localhostPath=curWwwPath.substring(0,pos);
	
	// 编写周报页面加载时：1.根据数据库获取已保存周报。2.登录时效时保存在session的周报
	window.onload = function () {
		$.post(localhostPath + "/summary/getTextIntoSession",function(data){
			if(data == ""){
			    $.post(localhostPath + "/summary/getCurrentSummary",function(data){
					quill.setText(data);
				});	
			}else{
				quill.setText(data);
			}
		});
		
	};
	
	// 保存周报，登录失效弹窗登录后，返回原页面并将之前编写好的周报填充进去
	$("#btn").click(function(){
		var contents= quill.getText();
		if(quill.getLength() == 1){
			alert("周报内容不能为空");
			return false;
		}
		$.ajaxSetup({async:false});
		$.post(localhostPath + "/summary/save",{"contents":contents},function(data){
			if(data=="1"){
				layer.msg('保存成功', {
				    icon: 1,
				    time: 1000
				});
			}else if(data=="2"){
				layer.msg('修改成功', {
				    icon: 1,
				    time: 1000
				});
			}else{
				var summary = quill.getText();
				$.post(localhostPath + "/summary/saveTextIntoSession",{"summary":summary});
				layer.open({
				  type: 2,
				  offset: 'auto',
				  title: '登录',
				  area: ['90%', '80%'],
				  closeBtn: 1, //显示关闭按钮
				  shift: 1,
				  shade: 0.5, //开启遮罩关闭
				  content: localhostPath + '/weeklyLogin.html',
				});
			}
		});
	});
});

