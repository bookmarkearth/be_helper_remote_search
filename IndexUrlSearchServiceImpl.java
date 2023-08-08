package com.bookmarkchina.module.search.service.impl;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import com.bookmarkchina.base.bean.mysql.UrlsWithBLOBs;
import com.bookmarkchina.base.constant.Constant;
import com.bookmarkchina.base.util.lucene.LucenePrepareData;
import com.bookmarkchina.module.search.bean.QueryCondition;
import com.bookmarkchina.module.search.service.IndexUrlSearchService;
import com.bookmarkchina.module.search.util.CreateIndex;
import com.bookmarkchina.module.search.util.IndexConditionUtils;

@Service
public class IndexUrlSearchServiceImpl implements IndexUrlSearchService{
	
	private Logger logger = Logger.getLogger(IndexUrlSearchServiceImpl.class);
	
	private final String indexCondition="id > 27935";//用户上传数据的id起点
	
	private LucenePrepareData prepareData=null;
	
	private final String groupBy=null;
	
	private CreateIndex createIndex=new CreateIndex();
	
	private final Integer pieces=300;

    public void indexInit(){//数据库+lcene初始化
    	createIndex.indexInit(Constant.DISC_URL_URL);
    }

	@Override
	public void indexEnd() {
		createIndex.indexEnd();
		
	}
	
	public List<UrlsWithBLOBs> prepareIndexList(ResultSet rs) throws SQLException{
		
		List<UrlsWithBLOBs> list=new ArrayList<UrlsWithBLOBs>();
		while(rs.next()){
	   		try{
   				
				long folderId=rs.getLong("id");
				ResultSet folderSet=prepareData.getFodlerContent(folderId,"u.id,u.name,u.url,u.update_time");
				List<String> urlList = new ArrayList<String>();
				while(folderSet.next()){
					urlList.add(folderSet.getString("url"));
				}
				
				String folderName=folderSet.getString("name");
				if(IndexConditionUtils.isHitted(urlList,prepareData,folderName,1)){continue;}
				
				while(folderSet.next()){
					
					UrlsWithBLOBs  urlsWithBLOBs= new UrlsWithBLOBs();

	   				//校验文件夹内容，过滤重复
	   				long id=folderSet.getLong("id");
	   				String url=folderSet.getString("url");
	   				Long upTime=folderSet.getTimestamp("update_time").getTime();
	   				
	   				try{
	   					URI uri=new URI(url);
	   					url=uri.getHost()+uri.getPath()+uri.getQuery();
	   				}
	   				catch(Exception e){
	   					;
	   				}

	   				Date updateTime=new Date(upTime); 
	   				urlsWithBLOBs.setId(id);
	   				urlsWithBLOBs.setName(folderName);
	   				urlsWithBLOBs.setUrl(url);
	   				urlsWithBLOBs.setUpdateTime(updateTime);
	   				list.add(urlsWithBLOBs);
				}
			}
			catch(Exception e){
				e.printStackTrace();
			}
		} 
	   	return list;
    }

	public List<UrlsWithBLOBs> getSourceListFull(QueryCondition query,String tableName){
	  	try {
			ResultSet rs=prepareData.getTableDataFull(query.getStart(),query.getPieces(),tableName,indexCondition,groupBy);
			return prepareIndexList(rs);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	  	return null;
  	}
	
	public void indexDocumentFull(QueryCondition query,String tableName){
  		List<UrlsWithBLOBs> list=getSourceListFull(query,tableName);
		for(int i=0;i<list.size();i++){
			UrlsWithBLOBs urlsWithBLOBs=list.get(i);
			createIndex.updateIndex(urlsWithBLOBs);
		}
		list=null;
  	}
  
	@Override
	public void updateFullIndex(String tableName, String serverName) {
		
		try{
	  		
	  		prepareData=new LucenePrepareData("id,folder");
	  		
	  		//初始化
	  		indexInit();
	  		prepareData.initDatebase(serverName);
	  		
	  		Long amount=prepareData.getTableDataFullAmount(tableName,indexCondition,"*");
	  		
	  		System.out.println("total amount is："+amount);
	  		QueryCondition query=new QueryCondition();
	  		Long start=0L;
	  		query.setPieces(pieces);
	  		while(start<=amount){
	  			query.setStart(start);
	  			indexDocumentFull(query,tableName);
	  			start=start+pieces;
	  			createIndex.indexCommit();
	  			System.out.println("commit "+start+" data to index block.");
	  		}
	  		System.out.println("total "+amount+" data has commited, from table "+tableName);
	  		
	  	}
	  	catch(Exception e){
	  		e.printStackTrace();
	  	}
	  	finally{
	  		indexEnd();
	  		prepareData.endDatebase();
	  	}
	}

	@Override
	public void deleteAll() {
		indexInit();
      	createIndex.deleteAll();
      	createIndex.forceMerge();
      	indexEnd();
	}
}
