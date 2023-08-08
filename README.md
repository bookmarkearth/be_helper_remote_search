# be_helper_remote_search

#### 这部分代码记录了书签地球助手（也就是书签地球插件，下面称为助手）远程搜索书签的索引、搜索、违规过滤、呈现等

#### 有几个点注意下

#### 1、助手的索引、搜索技术使用了Lucene
#### 2、其中bookmark_entity.png 记录了用户上传的书签标识id为：2519，用户上传内容的数据库标识origin=2
#### 3、bookmark_folder.png 记录了用户上传书签的第一个文件夹id为：27936，这也是为什么IndexUrlSearchServiceImpl.java 文件中 private final String indexCondition="id > 27935";//用户上传数据的id起点
#### 4、PluginSearchAlgorithmServiceImpl.java 为链接的搜索算法，这个过程为搜索词的匹配过程，不做过多干预
#### 5、PluginSearchServiceImpl.java 记录了搜索过程以及对政治、色情、版权、赌博等违规内容的过滤

#### 助手的书签远程搜索将坚持只对用户分享内容做索引搜索并完成审核义务，不做中间跳转界面，不对搜索结果做任何干预，保持绝对的技术中立

#### 以上搜索代码完全开源，任何人或者单位都可以复制、借用、改动且无需做任何标注

