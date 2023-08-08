package com.bookmarkchina.module.plugin.api.service;

import java.util.List;

import com.bookmarkchina.module.plugin.api.bean.SBookmark;
import com.bookmarkchina.module.search.bean.QueryCondition;

public interface IPluginSearchService {

	public  List<SBookmark> getBookmarkSearch(QueryCondition condition) throws Exception;
}
