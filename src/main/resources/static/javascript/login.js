$(function(){
	//通过这个函数传递url中的参数名就可以获取到参数的值
	(function ($) {
        $.getUrlParam = function (name) {
            var reg = new RegExp("(^|&)" + name + "=([^&]*)(&|$)");
            var r = window.location.search.substr(1).match(reg);
            if (r != null) return unescape(r[2]); return null;
        }
    })(jQuery);
	
	var redirect = $.getUrlParam('redirect');
	
	$(".btn").click(function(){
		var name=$.trim($("[name='name']").val());
		if(name==""){
			alert('用户名未填写!');
			$("[name='name']").focus();
			return false;
		}

		var password=$.trim($("[name='password']").val());
		if(password==""){
			alert('密码不能为空!');
			$("[name='pwd']").focus();
			return false;
		}

		var curWwwPath=window.document.location.href;  
		var pathName=window.document.location.pathname;  
		var pos=curWwwPath.indexOf(pathName);  
		//获取主机地址，如： http://localhost:7070  
		var localhostPath=curWwwPath.substring(0,pos); 
		var src = "/user/login";
		$.ajax({  
			type : "post",  
			url : localhostPath  + src,  
			data : {username:name,password:password},  
			async : false,  
			success : function(data){
				if(data.status == 500){
					alert(data.msg);
				}else if(data.status == 200){
					src = "/member/index.html";
					if(redirect != null){
						top.window.location = redirect;
					}else{
						top.window.location = localhostPath + src ;
					}
				}else{
					alert("用户名或密码错误！");
				}
			}  
		}); 
	});
	$("#weeklyLogin").click(function(){
		var name=$.trim($("[name='name']").val());
		if(name==""){
			alert('用户名未填写!');
			$("[name='name']").focus();
			return false;
		}

		var password=$.trim($("[name='password']").val());
		if(password==""){
			alert('密码不能为空!');
			$("[name='pwd']").focus();
			return false;
		}

		var curWwwPath=window.document.location.href;  
		var pathName=window.document.location.pathname;  
		var pos=curWwwPath.indexOf(pathName);  
		//获取主机地址，如： http://localhost:7070  
		var localhostPath=curWwwPath.substring(0,pos); 
		var src = "/user/login";
		$.ajax({  
			type : "post",  
			url : localhostPath  + src,  
			data : {username:name,password:password},  
			async : false,  
			success : function(data){
				if(data.status == 200){
					src = "/summary/write-weekly.html";
					top.window.location = localhostPath + src ;
				}else{
					alert("用户名或密码错误！");
				}
			}  
		}); 
	});
});
