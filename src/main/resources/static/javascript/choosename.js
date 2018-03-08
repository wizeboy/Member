var curWwwPath=window.document.location.href;  
var pathName=window.document.location.pathname;  
var pos=curWwwPath.indexOf(pathName);  
var localhostPath=curWwwPath.substring(0,pos);
$(function(){
    //创建26个字母数组
    var a = new Array("A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z"); 
    var line="";
    for(var i=0;i<a.length;i++){
        line+="<a href='javascript:void(0)' class='num'  value='" + a[i] + "' id='" + a[i] +"'><font size=5px>" + a[i] + "</font></a>&nbsp;&nbsp;";
    }
    //控制隐藏和显示div
    var current=document.getElementById("menu1"); 
    if($("#member").val()=="")  
     {  
       current.style.display="none";  
     }
    var name=null;
    $("#tag").append(line);
    $(".num").click(function(){
        //var theEvent = window.event || arguments.callee.caller.arguments[0]; 
        //alert(theEvent.target.id)
        $(".num").css("color","#BFBFBF");
        $(this).css("color","#212122");
        var letter=$(this).text();
        getName(letter);
        function getName(letter){
            //alert(letter);
            var name=null;
            $("#tabsC").html("");
            $.post(localhostPath + "/summary/getAllNames",{"letter":letter},function(data){
                $("#tabsC").append("<ul>")
                for(var i=0;i<data.length;i++){
                    //$("#tabsC").append("<li><a href=${pageContext.request.contextPath}/member/setMember.action?name=" + data[i] + "><span>" + data[i] + "</span></a></li>");  
                    $("#tabsC").append("<li><a href='javascript:void(0)' class='setMember'><span>" + data[i].name + "</span></a></li>");
                    
                }
                $("#tabsC").append("<li><a href='javascript:void(0)' class='setMember'><span>所有人</span></a></li>");
                $("#tabsC").append("</ul>")
                $(".setMember").on('click',function(){

                    var reStripTags = /<\/?.*?>/g;
                    var textOnly = this.innerHTML.replace(reStripTags, ''); //只有文字的结果
                    var index = parent.layer.getFrameIndex(window.name);
                    parent.$("#scopetext").val(textOnly);
                    parent.layer.close(index);
                })

            })
        }
    })
    $(".setMember").on('click',function(){
        var reStripTags = /<\/?.*?>/g;
        var textOnly = this.innerHTML.replace(reStripTags, ''); //只有文字的结果
        var index = parent.layer.getFrameIndex(window.name);
        parent.$("#scopetext").val(textOnly);
        parent.layer.close(index);
    })
})