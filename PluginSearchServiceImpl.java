package com.bookmarkchina.module.plugin.api.service.impl;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bookmarkchina.base.bean.mysql.CopywriteProtect;
import com.bookmarkchina.base.bean.mysql.DeadLink;
import com.bookmarkchina.base.bean.mysql.UrlsWithBLOBs;
import com.bookmarkchina.base.constant.Constant;
import com.bookmarkchina.base.util.ImgUtil;
import com.bookmarkchina.base.util.MD5Util;
import com.bookmarkchina.base.util.URLUtil;
import com.bookmarkchina.dao.mysql.CopywriteProtectMapper;
import com.bookmarkchina.dao.mysql.DeadLinkMapper;
import com.bookmarkchina.dao.mysql.IllegalUrlMapper;
import com.bookmarkchina.dao.mysql.UrlsMapper;
import com.bookmarkchina.module.plugin.api.bean.SBookmark;
import com.bookmarkchina.module.plugin.api.service.IPluginSearchAlgorithmService;
import com.bookmarkchina.module.plugin.api.service.IPluginSearchService;
import com.bookmarkchina.module.search.bean.QueryCondition;
import com.bookmarkchina.module.supervision.util.IllegalHelperUtil;

@Service
public class PluginSearchServiceImpl implements IPluginSearchService {
	
	@Autowired
	private IPluginSearchAlgorithmService algorithms;
	
	@Autowired
	private UrlsMapper urlsMapper;
	
	@Autowired
	private IllegalUrlMapper illegalUrlMapper;
	
	@Autowired
	private DeadLinkMapper deadLinkMapper;
	
	@Autowired
	private CopywriteProtectMapper copywriteProtectMapper;

	private static IndexSearcher searcher = null;

	static{
		init();
	}
	
	public synchronized static void init(){
		try {
			
			Constant.DIR_URL=FSDirectory.open(Paths.get(Constant.DISC_URL_URL));//url
			Constant.READER_URL = DirectoryReader.open(Constant.DIR_URL);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public synchronized static IndexSearcher getSearcher() {
		try {
			if(Constant.READER_URL==null) {
				Constant.READER_URL = DirectoryReader.open(Constant.DIR_URL);
			} else {
				DirectoryReader tr = DirectoryReader.openIfChanged(Constant.READER_URL) ;
				if(tr!=null) {
					Constant.READER_URL.close();
					Constant.READER_URL=null;
					Constant.READER_URL = tr;
					searcher=null;//数据已经改变，所以重新new searcher
				}
			}
			if(searcher==null||!Constant.READER_URL.isCurrent()){
				searcher=new IndexSearcher(Constant.READER_URL);
			}
			return searcher;
		} catch (CorruptIndexException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public List<SBookmark> getBatchCleanBookmark(List<SBookmark> list){
		
		List<SBookmark> newList = new ArrayList<SBookmark>();
		
		if(list==null || list.isEmpty()){return newList;}
		
		List<String> domainMd5List =new ArrayList<String>();
		List<String> urlMd5List =new ArrayList<String>();
		
		list.forEach((item)->{
			
			if(item!=null){
				String url=item.getUrl();
				if(url.startsWith("http")){
					String domain="";
					try {
						domain = URLUtil.getDomainName(url);
					} catch (MalformedURLException e) {
						e.printStackTrace();
					}
					String domainMd5=MD5Util.encode2hex(domain);
					String urlMd5=MD5Util.encode2hex(url);
					domainMd5List.add(domainMd5);
					urlMd5List.add(urlMd5);
					
					//添加两个属性
					item.setDomainMd5(domainMd5);
					item.setUrlMd5(urlMd5);;
				}
			}
		});
		
		//非法内容（色情，政治，赌博，版权等）数量
		List<String> md5List=illegalUrlMapper.selectDomainMd5InDomainMd5List(domainMd5List);
		
		//版权内容列表
		List<CopywriteProtect> copywriteProtectList=copywriteProtectMapper.selectCopywriteProtectInUrlMd5List(urlMd5List);
		
		//死链接列表
		List<DeadLink> deadLinkList=deadLinkMapper.selectDeadLinkInUrlMd5List(urlMd5List);
		
		list.forEach((item)->{

			if(item!=null){

				//非法内容踢除
				if(md5List.contains(item.getDomainMd5())){
					return;//return 只停止当前循环
				}
				
				//版权内容踢除
				if(!copywriteProtectList.isEmpty()){
					for(CopywriteProtect copywriteProtect :copywriteProtectList){//当前书签下的某个链接（bookmarkid->url）
						if(copywriteProtect.getUrlMd5()!=null&&copywriteProtect.getUrlMd5().equals(item.getUrlMd5())){
							return;
						}
					}
				}
				
				//死链接踢除
				if(!deadLinkList.isEmpty()){
					for(DeadLink deadLink:deadLinkList){
						String urlMd5=deadLink.getUrlMd5();
						if(item.getUrlMd5()!=null&&item.getUrlMd5().equals(urlMd5)){
							int deadTotal=deadLink.getDayMax();
							Double days=deadTotal*1.0d/Constant.MAX_ALLOW_DEAD_PER_DAY;
							if(Math.ceil(days)>=Constant.MAX_ALLOW_DEAD_DAY){
								return;
							}
						}
					}
				}
				
				newList.add(item);

			}
		});
		
		return newList;
	}
	
	@Override
	public List<SBookmark> getBookmarkSearch(QueryCondition condition) throws Exception {
		
		List<SBookmark> list=new ArrayList<SBookmark>();
		IndexSearcher searcher=getSearcher();
	 	
		try{
			Map<String,Object> output=new HashMap<String,Object>();
			output=algorithms.searchBookmarkAlgorithm(condition, searcher);
			if(output!=null){
				
				TopDocs tds = (TopDocs) output.get("tds");
				ScoreDoc[] sd = tds.scoreDocs;
				
				//处理folder，查询一次即可
				List<Long> ids=new ArrayList<Long>();
				for (int i = 0; i < sd.length; i++) {
					 Document doc = searcher.doc(sd[i].doc);
					 
					 Long id=Long.parseLong(doc.get("id"));
					 ids.add(id);
				}
				
				Map<Long,List<UrlsWithBLOBs>> urlBy = urlsMapper.searchByIdsList(ids).stream().collect(Collectors.groupingBy(UrlsWithBLOBs::getId));
				for(Long id:ids){
					
					UrlsWithBLOBs item=urlBy.get(id).get(0);
					String icon=ImgUtil.getFavicon(item.getSkydriverRoot(), item.getIconPath());
					SBookmark sBookmark=new SBookmark(item.getName(),item.getUrl(),icon==null?Constant.DEFAULT_ICON:icon,1);
					
					//clean
					if(IllegalHelperUtil.getCleanBookmark(sBookmark)!=null){
						list.add(sBookmark);
					}
				}
			}
			
		}catch(Exception e){
			e.printStackTrace();
		}
	 	return getBatchCleanBookmark(list);
	}
}
