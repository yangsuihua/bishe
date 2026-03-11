package com.videoplatform.video.service.impl;

import com.videoplatform.video.service.VideoCompressionService;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.builder.FFmpegOutputBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * 视频压缩服务实现
 */
@Slf4j
@Service
public class VideoCompressionServiceImpl implements VideoCompressionService {

    @Value("${ffmpeg.path:ffmpeg}")
    private String ffmpegPath;

    @Value("${ffmpeg.ffprobe-path:ffprobe}")
    private String ffprobePath;

    @Value("${ffmpeg.compression.enabled:true}")
    private boolean compressionEnabled;

    @Value("${ffmpeg.compression.video-bitrate:1000000}")
    private long videoBitrate;

    @Value("${ffmpeg.compression.audio-bitrate:128000}")
    private long audioBitrate;

    @Value("${ffmpeg.compression.video-codec:libx264}")
    private String videoCodec;

    @Value("${ffmpeg.compression.audio-codec:aac}")
    private String audioCodec;

    @Value("${ffmpeg.compression.format:mp4}")
    private String outputFormat;

    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir");

    @Override
    public File compressVideo(MultipartFile inputFile) throws IOException {
        if (!compressionEnabled) {
            log.info("视频压缩功能已禁用，跳过压缩");
            return null;
        }

        // 创建临时文件保存原始视频
        String originalFileName = inputFile.getOriginalFilename();
        String extension = originalFileName != null && originalFileName.contains(".") 
            ? originalFileName.substring(originalFileName.lastIndexOf('.')) 
            : ".mp4";
        
        File tempInputFile = File.createTempFile("video_input_", extension, new File(TEMP_DIR));
        File tempOutputFile = File.createTempFile("video_output_", "." + outputFormat, new File(TEMP_DIR));
        try {
            // 将MultipartFile保存到临时文件
            inputFile.transferTo(tempInputFile);
            
            // 压缩视频
            return compressVideo(tempInputFile, tempOutputFile);
        } catch (Exception e) {
            // 清理临时文件
            deleteTempFile(tempInputFile);
            deleteTempFile(tempOutputFile);
            throw new IOException("视频压缩失败: " + e.getMessage(), e);
        }
    }

    @Override
    public File compressVideo(File inputFile) throws IOException {
        if (!compressionEnabled) {
            log.info("视频压缩功能已禁用，跳过压缩");
            return null;
        }

        File tempOutputFile = File.createTempFile("video_output_", "." + outputFormat, new File(TEMP_DIR));
        
        try {
            return compressVideo(inputFile, tempOutputFile);
        } catch (Exception e) {
            deleteTempFile(tempOutputFile);
            throw new IOException("视频压缩失败: " + e.getMessage(), e);
        }
    }

    private File compressVideo(File inputFile, File outputFile) throws IOException {
        try {
            log.info("开始压缩视频: {} -> {}", inputFile.getAbsolutePath(), outputFile.getAbsolutePath());
            
            // 初始化FFmpeg和FFprobe
            FFmpeg ffmpeg = new FFmpeg(ffmpegPath);
            FFprobe ffprobe = new FFprobe(ffprobePath);
            
            // 构建FFmpeg命令（0.7.0 版本 API）
            FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(inputFile.getAbsolutePath())
                .overrideOutputFiles(true);

            FFmpegOutputBuilder output = builder.addOutput(outputFile.getAbsolutePath())
                .setFormat(outputFormat)          // 输出格式
                .setVideoCodec(videoCodec)        // 视频编码器
                .setVideoBitRate(videoBitrate)    // 视频码率
                .setAudioCodec(audioCodec)        // 音频编码器
                .setAudioBitRate(audioBitrate);   // 音频码率

            // CRF 质量参数（18~28，越小质量越高）
            output.addExtraArgs("-crf", "23");
            // 预设，平衡压缩速度与质量
            output.addExtraArgs("-preset", "medium");
            // 严格模式
            output.setStrict(FFmpegBuilder.Strict.EXPERIMENTAL);
            
            // 执行压缩
            FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
            executor.createJob(builder).run();
            
            // 检查输出文件是否存在且大小合理
            if (!outputFile.exists() || outputFile.length() == 0) {
                throw new IOException("压缩后的视频文件不存在或为空");
            }
            
            long originalSize = inputFile.length();
            long compressedSize = outputFile.length();
            double compressionRatio = (1.0 - (double) compressedSize / originalSize) * 100;
            
            log.info("视频压缩完成: 原始大小={}MB, 压缩后大小={}MB, 压缩率={:.2f}%", 
                originalSize / 1024.0 / 1024.0, 
                compressedSize / 1024.0 / 1024.0, 
                compressionRatio);
            
            // 删除原始临时文件
            deleteTempFile(inputFile);
            
            return outputFile;
        } catch (Exception e) {
            log.error("视频压缩失败", e);
            throw new IOException("视频压缩失败: " + e.getMessage(), e);
        }
    }

    /**
     * 删除临时文件
     */
    private void deleteTempFile(File file) {
        if (file != null && file.exists()) {
            try {
                boolean deleted = file.delete();
                if (!deleted) {
                    log.warn("临时文件删除失败: {}", file.getAbsolutePath());
                    // 尝试在JVM退出时删除
                    file.deleteOnExit();
                }
            } catch (Exception e) {
                log.warn("删除临时文件时出错: {}", file.getAbsolutePath(), e);
                file.deleteOnExit();
            }
        }
    }
}
