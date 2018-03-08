package com.xinqushi.controller;

import org.springframework.stereotype.Controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;



@Controller
public class IndexController {
	
	@RequestMapping("/member")
	public String path(){
		return "forward:/index.html";
	}
	
	@RequestMapping("/member/{page}")
	public String page(@PathVariable String page){
		page = "forward:/" + page + ".html";
		return page;
	}
	
}
