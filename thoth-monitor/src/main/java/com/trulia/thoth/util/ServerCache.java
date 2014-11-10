package com.trulia.thoth.util;

import com.trulia.thoth.pojo.ServerDetail;

import java.util.HashMap;

/**
 * User: dbraga - Date: 8/23/14
 */
public class ServerCache {
  private HashMap<String,ServerDetail> serverList;


  public void init() {
    this.serverList = new HashMap<String, ServerDetail>();
  }

  public boolean contains(String hostname, String coreName){
    return serverList.containsKey(hostname + " " + coreName);
  }

  public ServerDetail fetchDetails(String hostname, String coreName){
    System.out.println("ServerCache: fetching hostname("+hostname+") core("+coreName+") from the cache.");
    return serverList.get(hostname + " " + coreName);
  }

  public void add(ServerDetail detail){
    String hostname = detail.getName();
    String coreName = detail.getCore();
    String key = hostname +" "+coreName;
    serverList.put(key, detail);
    System.out.println("ServerCache: added hostname("+hostname+") core("+coreName+") to the cache.");
  }

}
