package com.videoplatform.search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.videoplatform.common.entity.Video;
import com.videoplatform.common.entity.VideoTag;
import com.videoplatform.common.entity.VideoTagRelation;
import com.videoplatform.search.entity.VideoDoc;
import com.videoplatform.search.mapper.VideoMapper;
import com.videoplatform.search.mapper.VideoTagMapper;
import com.videoplatform.search.mapper.VideoTagRelationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataSyncService {

    private final VideoMapper videoMapper;
    private final VideoTagMapper videoTagMapper;
    private final VideoTagRelationMapper videoTagRelationMapper;
    private final ElasticsearchClient elasticsearchClient;
    private final com.videoplatform.common.feign.UserFeignClient userFeignClient;

    private static final String INDEX_NAME = "videos";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 全量同步 MySQL 数据到 ES
     */
    public void syncAll() {
        log.info("开始全量同步视频数据到 ES...");

        List<Video> videos = videoMapper.selectList(new LambdaQueryWrapper<Video>()
                .eq(Video::getStatus, 1)
                .eq(Video::getDeleted, 0));

        if (videos.isEmpty()) {
            log.info("没有需要同步的视频数据。");
            return;
        }

        // 批量获取用户信息
        List<Long> userIds = videos.stream().map(Video::getUserId).distinct().collect(Collectors.toList());
        Map<Long, com.videoplatform.common.dto.UserProfileVO> userMap = Collections.emptyMap();
        try {
            com.videoplatform.common.result.Result<List<com.videoplatform.common.dto.UserProfileVO>> userResult = userFeignClient.getUsersByIds(userIds);
            if (userResult.getCode() == 200 && userResult.getData() != null) {
                userMap = userResult.getData().stream().collect(Collectors.toMap(com.videoplatform.common.dto.UserProfileVO::getId, u -> u));
            }
        } catch (Exception e) {
            log.warn("全量同步中批量获取用户信息失败: {}", e.getMessage());
        }

        List<VideoTag> allTags = videoTagMapper.selectList(null);
        Map<Integer, String> tagMap = allTags.stream()
                .collect(Collectors.toMap(VideoTag::getId, VideoTag::getName));

        List<VideoTagRelation> allRelations = videoTagRelationMapper.selectList(null);
        Map<Long, List<String>> videoTagsMap = allRelations.stream()
                .collect(Collectors.groupingBy(
                        VideoTagRelation::getVideoId,
                        Collectors.mapping(r -> tagMap.get(r.getTagId()), Collectors.toList())
                ));

        List<BulkOperation> operations = new ArrayList<>();
        for (Video video : videos) {
            List<String> tags = videoTagsMap.getOrDefault(video.getId(), new ArrayList<>());
            com.videoplatform.common.dto.UserProfileVO user = userMap.get(video.getUserId());
            VideoDoc doc = buildVideoDoc(video, tags, user);
            operations.add(BulkOperation.of(b -> b.index(i -> i.id(video.getId().toString()).document(doc))));
        }

        try {
            BulkRequest bulkRequest = BulkRequest.of(b -> b.index(INDEX_NAME).operations(operations));
            elasticsearchClient.bulk(bulkRequest);
            log.info("成功同步 {} 条视频数据到 ES。", videos.size());
        } catch (Exception e) {
            log.error("批量同步到 ES 失败", e);
            throw new RuntimeException("同步失败", e);
        }
    }

    /**
     * 同步单个视频到 ES
     */
    public void syncOne(Long videoId) {
        log.info("同步单条视频数据到 ES, videoId: {}", videoId);
        
        Video video = videoMapper.selectById(videoId);
        if (video == null || video.getDeleted() == 1 || video.getStatus() != 1) {
            deleteById(videoId);
            return;
        }

        // 获取用户信息
        com.videoplatform.common.dto.UserProfileVO user = null;
        try {
            com.videoplatform.common.result.Result<com.videoplatform.common.dto.UserProfileVO> userResult = userFeignClient.getUserProfile(video.getUserId());
            if (userResult.getCode() == 200) {
                user = userResult.getData();
            }
        } catch (Exception e) {
            log.warn("同步单条视频时获取用户信息失败, videoId: {}", videoId);
        }

        List<VideoTagRelation> relations = videoTagRelationMapper.selectList(
                new LambdaQueryWrapper<VideoTagRelation>().eq(VideoTagRelation::getVideoId, videoId));
        
        List<String> tags = new ArrayList<>();
        if (!relations.isEmpty()) {
            List<Integer> tagIds = relations.stream().map(VideoTagRelation::getTagId).collect(Collectors.toList());
            List<VideoTag> videoTags = videoTagMapper.selectBatchIds(tagIds);
            tags = videoTags.stream().map(VideoTag::getName).collect(Collectors.toList());
        }

        VideoDoc doc = buildVideoDoc(video, tags, user);

        try {
            elasticsearchClient.index(i -> i.index(INDEX_NAME).id(videoId.toString()).document(doc));
            log.info("单条视频同步成功, videoId: {}", videoId);
        } catch (Exception e) {
            log.error("同步单条视频失败, videoId: {}", videoId, e);
        }
    }

    /**
     * 从 ES 删除单个视频
     */
    public void deleteById(Long videoId) {
        try {
            elasticsearchClient.delete(d -> d.index(INDEX_NAME).id(videoId.toString()));
            log.info("从 ES 删除视频成功, videoId: {}", videoId);
        } catch (Exception e) {
            log.error("从 ES 删除视频失败, videoId: {}", videoId, e);
        }
    }

    private VideoDoc buildVideoDoc(Video video, List<String> tags, com.videoplatform.common.dto.UserProfileVO user) {
        return VideoDoc.builder()
                .id(video.getId())
                .userId(video.getUserId())
                .title(video.getTitle())
                .description(video.getDescription())
                .tags(tags)
                .viewCount(video.getViewCount())
                .createdAt(video.getPublishedAt() != null ? video.getPublishedAt().format(DATE_FORMATTER) : null)
                .coverUrl(video.getCoverUrl())
                .username(user != null ? user.getUsername() : null)
                .avatar(user != null ? user.getAvatar() : null)
                .suggestion(new VideoDoc.Suggestion(
                        getSuggestionInputs(video.getTitle(), tags),
                        video.getViewCount() != null ? video.getViewCount() : 0L
                ))
                .build();
    }

    private List<String> getSuggestionInputs(String title, List<String> tags) {
        List<String> inputs = new ArrayList<>();
        if (title != null) inputs.add(title);
        if (tags != null) inputs.addAll(tags);
        return inputs.stream()
                .filter(s -> s != null && !s.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }
}
