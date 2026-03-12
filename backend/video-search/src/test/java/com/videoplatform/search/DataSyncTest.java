package com.videoplatform.search;

import com.videoplatform.search.service.DataSyncService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.List;

@SpringBootTest
public class DataSyncTest {

    @Autowired
    private DataSyncService dataSyncService;

    @Autowired
    private com.videoplatform.search.mapper.VideoMapper videoMapper;

    @Autowired
    private co.elastic.clients.elasticsearch.ElasticsearchClient client;

    @Test
    public void testSyncAll() {
        dataSyncService.syncAll();
        System.out.println("数据同步完成！");
    }

    @Test
    public void testVerifyData() throws Exception {
        // 查询索引中的文档总数
        co.elastic.clients.elasticsearch.core.CountResponse count = client.count(c -> c.index("videos"));
        System.out.println("ES 索引中的视频总数: " + count.count());

        // 查询前 3 条看一眼内容
        co.elastic.clients.elasticsearch.core.SearchResponse<com.videoplatform.search.entity.VideoDoc> search = client.search(s -> s
                .index("videos")
                .size(3), com.videoplatform.search.entity.VideoDoc.class);
        
        search.hits().hits().forEach(hit -> {
            System.out.println("同步成功的视频: " + hit.source().getTitle() + " | 标签: " + hit.source().getTags());
        });
    }

    @Test
    public void testSearchDebug() throws Exception {
        String keyword = "弹吉他";
        co.elastic.clients.elasticsearch.core.SearchResponse<com.videoplatform.search.entity.VideoDoc> search = client.search(s -> s
                .index("videos")
                .query(q -> q
                    .multiMatch(m -> m.query(keyword).fields("title", "description"))
                ), com.videoplatform.search.entity.VideoDoc.class);
        
        System.out.println("搜索关键词 [" + keyword + "] 的结果数: " + search.hits().total().value());
        search.hits().hits().forEach(hit -> {
            System.out.println("找到视频: " + hit.source().getTitle() + " (ID: " + hit.source().getId() + ")");
        });
    }

    @Test
    public void testCheckDb() {
        String keyword = "弹吉他";
        System.out.println("--- 检查数据库中包含 [" + keyword + "] 的视频 ---");
        List<com.videoplatform.common.entity.Video> videos = videoMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.videoplatform.common.entity.Video>()
                .like(com.videoplatform.common.entity.Video::getTitle, keyword));
        
        if (videos.isEmpty()) {
            System.out.println("数据库中未找到相关视频！");
        } else {
            videos.forEach(v -> {
                System.out.println("视频: " + v.getTitle() + " | ID: " + v.getId() + " | Status: " + v.getStatus() + " | Deleted: " + v.getDeleted());
            });
        }
    }
}
