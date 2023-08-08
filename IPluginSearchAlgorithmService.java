package com.bookmarkchina.module.plugin.api.service;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;

import com.bookmarkchina.module.search.bean.QueryCondition;

public interface IPluginSearchAlgorithmService {

	public Map<String, Object> searchBookmarkAlgorithm(QueryCondition condition,IndexSearcher searcher) throws ParseException, IOException;
}
