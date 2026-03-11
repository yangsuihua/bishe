package com.videoplatform.video.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

/**
 * 视频压缩服务接口
 */
public interface VideoCompressionService {
    
    /**
     * 压缩视频文件
     * @param inputFile 原始视频文件
     * @return 压缩后的临时文件
     * @throws IOException 压缩失败时抛出异常
     */
    File compressVideo(MultipartFile inputFile) throws IOException;
    
    /**
     * 压缩视频文件（从临时文件）
     * @param inputFile 原始视频临时文件
     * @return 压缩后的临时文件
     * @throws IOException 压缩失败时抛出异常
     */
    File compressVideo(File inputFile) throws IOException;
}
