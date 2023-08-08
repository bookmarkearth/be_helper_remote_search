package com.bookmarkchina.module.search.service;

public interface IndexUrlSearchService {
    
    public void indexInit();

    public void indexEnd();

	void updateFullIndex(String tableName, String serverName);

	void deleteAll();
}
