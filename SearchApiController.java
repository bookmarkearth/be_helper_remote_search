package com.bookmarkchina.module.plugin.api.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.bookmarkchina.base.bean.mysql.User;
import com.bookmarkchina.base.constant.Constant;
import com.bookmarkchina.base.util.BodyRequestUtil;
import com.bookmarkchina.base.util.JSONUtils;
import com.bookmarkchina.module.base.BaseController;
import com.bookmarkchina.module.plugin.api.bean.SBookmark;
import com.bookmarkchina.module.plugin.api.service.IPluginSearchService;
import com.bookmarkchina.module.search.bean.QueryCondition;
import com.bookmarkchina.module.search.service.ISearchHelper;

import net.sf.json.JSONObject;

@Controller
@RequestMapping("/plugin/api")
public class SearchApiController extends BaseController{
	
	@Autowired
	private IPluginSearchService lch;
	
	@Autowired
	private ISearchHelper searchHelper;
	
	@ResponseBody
	@RequestMapping(value = "/bookmark/search", method = RequestMethod.POST)
	public Map<String, Object> searchBookmark(){

		Map<String, Object> map = new HashMap<String, Object>();
		String body=BodyRequestUtil.getBody(request);
		try{
			
			User user=getUserInSession();
			JSONObject json=JSONUtils.toJSONObject(body);
			String q=json.getString("q");
			
			QueryCondition searchInfo = new QueryCondition();
			searchInfo.setCurrentPage(1);
			searchInfo.setPageSize(Constant.PLUGIN_SEARCH_PAGESIZE);
			searchInfo.setKeyWord(q);
			
			List<SBookmark> list = lch.getBookmarkSearch(searchInfo);
			map.put("list",list);
			
			//记录搜索内容，该用新方式，顺便把没有的词记录了
			searchHelper.searchRecord(request, user,q,Constant.SEARCH_ORIGIN_PLUGIN);
		}
		catch(Exception e){
			e.printStackTrace();
		}
		return map;
	}
}