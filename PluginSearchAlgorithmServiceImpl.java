package com.bookmarkchina.module.plugin.api.service.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopFieldCollector;
import org.springframework.stereotype.Service;

import com.bookmarkchina.base.constant.Constant;
import com.bookmarkchina.base.util.lucene.LuceneUtil;
import com.bookmarkchina.module.plugin.api.service.IPluginSearchAlgorithmService;
import com.bookmarkchina.module.search.bean.QueryCondition;

@Service
public class PluginSearchAlgorithmServiceImpl implements IPluginSearchAlgorithmService {

	private static Analyzer analyzer = new SmartChineseAnalyzer();
	
	@Override
	public Map<String, Object> searchBookmarkAlgorithm(QueryCondition condition,IndexSearcher searcher) throws ParseException, IOException {
		
		Map<String,Object> map =new HashMap<String,Object>();
		
		Integer currentPage=condition.getCurrentPage();
		Integer pageSize=condition.getPageSize();
		String keyword=condition.getKeyWord();
		
	    int start = (currentPage - 1) * pageSize;
	    int hm = start + pageSize;

	    Sort sort = new Sort(); // 排序
		sort.setSort(SortField.FIELD_SCORE);
	    TopFieldCollector res = TopFieldCollector.create(sort,hm,Constant.MAX_RESULT_NUM); 
	    
	    String []fieldsList={"name","url"};
	    QueryParser queryParser = new MultiFieldQueryParser(fieldsList,analyzer);
		queryParser.setDefaultOperator(QueryParser.AND_OPERATOR);
		Query query = queryParser.parse(LuceneUtil.escapeExprSpecialWord(keyword));
        
		BooleanQuery.Builder booleanQueryBuilder= new BooleanQuery.Builder();
	    booleanQueryBuilder.add(query,BooleanClause.Occur.SHOULD);
	    
	    BooleanQuery booleanQuery=booleanQueryBuilder.build();
		searcher.search(booleanQuery,res);
		
		long amount = res.getTotalHits();
		
		TopDocs tds = res.topDocs(start, pageSize);
        map.put("amount",amount);
        map.put("tds",tds);
        return map;
		
	}
}
