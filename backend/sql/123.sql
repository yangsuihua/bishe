/*
SQLyog Professional v12.14 (64 bit)
MySQL - 8.0.32 : Database - video_platform
*********************************************************************
*/


/*!40101 SET NAMES utf8 */;

/*!40101 SET SQL_MODE=''*/;

/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
CREATE DATABASE /*!32312 IF NOT EXISTS*/`video_platform` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `video_platform`;

/*Table structure for table `comment` */

DROP TABLE IF EXISTS `comment`;

CREATE TABLE `comment` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '评论ID',
  `video_id` bigint unsigned NOT NULL COMMENT '视频ID',
  `user_id` bigint unsigned NOT NULL COMMENT '评论用户ID',
  `parent_id` bigint unsigned DEFAULT '0' COMMENT '父评论ID(0为顶级评论)',
  `reply_user_id` bigint unsigned DEFAULT NULL COMMENT '回复的用户ID',
  `content` text NOT NULL COMMENT '评论内容',
  `like_count` int DEFAULT '0' COMMENT '点赞数',
  `status` tinyint DEFAULT '1' COMMENT '状态: 0-已删除, 1-正常',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_video_id` (`video_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='评论表';

/*Data for the table `comment` */

insert  into `comment`(`id`,`video_id`,`user_id`,`parent_id`,`reply_user_id`,`content`,`like_count`,`status`,`created_at`) values 

(1,1,3,0,NULL,'很棒的vlog，很有生活气息！',5,1,'2025-11-29 12:17:24'),

(3,2,2,0,NULL,'讲解很详细，适合初学者',8,1,'2025-11-29 12:17:24');

/*Table structure for table `message` */

DROP TABLE IF EXISTS `message`;

CREATE TABLE `message` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `from_user_id` bigint unsigned NOT NULL COMMENT '发送者ID',
  `to_user_id` bigint unsigned NOT NULL COMMENT '接收者ID',
  
  `type` tinyint NOT NULL DEFAULT '0' COMMENT '消息类型: 0-纯文本, 1-视频分享',
  
  `content` text COMMENT '内容: type=0时为聊天文本; type=1时可存视频标题/封面JSON快照',
  `related_id` bigint unsigned DEFAULT NULL COMMENT '关联ID: type=1时存储video_id',
  
  `is_read` tinyint DEFAULT '0' COMMENT '是否已读: 0-未读, 1-已读',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  
  PRIMARY KEY (`id`),
  
  -- 索引优化 --
  -- 用于快速查询某个用户的未读消息数量
  KEY `idx_to_read` (`to_user_id`, `is_read`),
  
  -- 用于快速拉取两个用户之间的聊天历史记录 (A发给B，或B发给A)
  KEY `idx_from_to` (`from_user_id`, `to_user_id`),
  
  -- 用于按时间排序
  KEY `idx_created_at` (`created_at`)
  
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='聊天消息表';

CREATE TABLE friendship (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id_1 BIGINT UNSIGNED NOT NULL, -- 必须加上 UNSIGNED
    user_id_2 BIGINT UNSIGNED NOT NULL, -- 必须加上 UNSIGNED
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- 约束：user_id_1 必须小于 user_id_2 (可选，建议在代码层控制，或者MySQL 8.0+可加CHECK约束)
    -- CONSTRAINT check_order CHECK (user_id_1 < user_id_2),

    -- 联合唯一索引，防止重复添加好友
    UNIQUE KEY uk_friendship (user_id_1, user_id_2),

    -- 外键约束
    CONSTRAINT fk_friendship_user_1 FOREIGN KEY (user_id_1) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_friendship_user_2 FOREIGN KEY (user_id_2) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

/*Table structure for table `operation_log` */

DROP TABLE IF EXISTS `operation_log`;

CREATE TABLE `operation_log` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `user_id` bigint unsigned DEFAULT NULL COMMENT '操作用户ID',
  `module` varchar(50) DEFAULT NULL COMMENT '模块名称',
  `operation` varchar(100) DEFAULT NULL COMMENT '操作类型',
  `method` varchar(200) DEFAULT NULL COMMENT '请求方法',
  `params` text COMMENT '请求参数',
  `ip` varchar(50) DEFAULT NULL COMMENT 'IP地址',
  `location` varchar(200) DEFAULT NULL COMMENT 'IP归属地',
  `status` tinyint DEFAULT '1' COMMENT '状态: 0-失败, 1-成功',
  `error_msg` text COMMENT '错误信息',
  `cost_time` int DEFAULT NULL COMMENT '耗时(ms)',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='操作日志表';

/*Data for the table `operation_log` */

/*Table structure for table `sys_config` */

DROP TABLE IF EXISTS `sys_config`;

CREATE TABLE `sys_config` (
  `id` int unsigned NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `config_key` varchar(100) NOT NULL COMMENT '配置键',
  `config_value` text COMMENT '配置值',
  `description` varchar(500) DEFAULT NULL COMMENT '描述',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统配置表';

/*Data for the table `sys_config` */

/*Table structure for table `sys_log` */

DROP TABLE IF EXISTS `sys_log`;

CREATE TABLE `sys_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `admin_id` bigint NOT NULL COMMENT '操作管理员ID',
  `video_id` bigint NOT NULL COMMENT '被审核的视频ID',
  `audit_status` tinyint(1) NOT NULL COMMENT '审核结果：1-通过，0-不通过',
  `reason` varchar(255) DEFAULT NULL COMMENT '原因文本（不通过时必填）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_admin_id` (`admin_id`),
  KEY `idx_video_id` (`video_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='后台审核日志表';

/*Data for the table `sys_log` */

/*Table structure for table `sys_user` */

DROP TABLE IF EXISTS `sys_user`;

CREATE TABLE `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `password` varchar(100) NOT NULL COMMENT '密码(加密存储)',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态：1-正常，0-禁用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='后台管理员表';

/*Data for the table `sys_user` */

/*Table structure for table `user` */

DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `nickname` varchar(50) NOT NULL COMMENT '昵称',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `password` varchar(255) NOT NULL COMMENT '密码(BCrypt加密)',
   `avatar` varchar(1024) DEFAULT '' COMMENT '头像URL',
  

  
  `bio` text COMMENT '个人简介',
  `gender` tinyint DEFAULT '0' COMMENT '性别: 0-未知, 1-男, 2-女',
  `birthday` date DEFAULT NULL COMMENT '生日',
  

  `is_favorites_visible` tinyint DEFAULT '1' COMMENT '收藏列表是否公开: 0-私密, 1-公开',
  `is_likes_visible` tinyint DEFAULT '0' COMMENT '点赞列表是否公开: 0-私密, 1-公开',
  
  `status` tinyint DEFAULT '1' COMMENT '状态: 0-禁用, 1-正常',
  `role` varchar(20) DEFAULT 'user' COMMENT '角色: user-普通用户, admin-管理员',
  `follower_count` int DEFAULT '0' COMMENT '粉丝数',
  `following_count` int DEFAULT '0' COMMENT '关注数',
  `like_count` bigint DEFAULT '0' COMMENT '获赞总数',
  `video_count` int DEFAULT '0' COMMENT '作品数',
  `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
  `last_login_ip` varchar(50) DEFAULT NULL COMMENT '最后登录IP',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除: 0-未删除, 1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_email` (`email`),
  UNIQUE KEY `uk_phone` (`phone`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';
/*Data for the table `user` */

insert  into `user`(`id`,`username`,`nickname`,`email`,`phone`,`password`,`avatar`,`bio`,`gender`,`birthday`,`is_favorites_visible`,`is_likes_visible``status`,`role`,`follower_count`,`following_count`,`like_count`,`video_count`,`last_login_time`,`last_login_ip`,`created_at`,`updated_at`,`deleted`) values 

(2,'test','test','test@qq.com','13416334318','$2a$10$zWDK8gO1QOKwCBB0lB.Ede8RaHHiSMmjbCkw9iYlN.cpRPSi/T8VK',NULL,NULL,0,NULL,0,0,1,'user',4,4,0,0,'2025-12-30 16:15:15',NULL,NULL,'2025-11-29 12:17:24',0),

(3,'fff','fff','1743002518@qq.com','','$2a$10$VJUUP9PRcfU/0nZurEasjevIBoIN37GZbHfOO1H0kKaebVvZYlinm',NULL,NULL,0,NULL,0,0,1,'user',4,3,0,0,'2025-12-30 00:33:39',NULL,NULL,'2025-11-29 12:17:24',0);

/*Table structure for table `user_favorite` */

DROP TABLE IF EXISTS `user_favorite`;

CREATE TABLE `user_favorite` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint unsigned NOT NULL COMMENT '用户ID',
  `video_id` bigint unsigned NOT NULL COMMENT '视频ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_video` (`user_id`,`video_id`),
  KEY `idx_video_id` (`video_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户收藏表';

/*Data for the table `user_favorite` */

insert  into `user_favorite`(`id`,`user_id`,`video_id`,`created_at`) values 

(3,3,1,'2025-11-29 12:17:24');

/*Table structure for table `user_follow` */

DROP TABLE IF EXISTS `user_follow`;

CREATE TABLE `user_follow` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint unsigned NOT NULL COMMENT '用户ID',
  `follow_user_id` bigint unsigned NOT NULL COMMENT '被关注用户ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '关注时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_follow` (`user_id`,`follow_user_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_follow_user_id` (`follow_user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户关注表';

/*Data for the table `user_follow` */

insert  into `user_follow`(`id`,`user_id`,`follow_user_id`,`created_at`) values 

(1,2,3,'2025-11-29 12:17:24'),

(5,3,2,'2025-11-29 12:17:24');

/*Table structure for table `user_history` */

DROP TABLE IF EXISTS `user_history`;

CREATE TABLE `user_history` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint unsigned NOT NULL COMMENT '用户ID',
  `video_id` bigint unsigned NOT NULL COMMENT '视频ID',
  `watch_duration` int DEFAULT '0' COMMENT '观看时长(秒)',
  `watch_progress` decimal(5,2) DEFAULT '0.00' COMMENT '观看进度(百分比)',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '观看时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_video` (`user_id`,`video_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户观看历史表';

/*Data for the table `user_history` */

insert  into `user_history`(`id`,`user_id`,`video_id`,`watch_duration`,`watch_progress`,`created_at`,`updated_at`) values 

(1,2,1,280,'87.50','2025-11-29 12:17:24','2025-11-29 12:17:24'),

(4,3,2,780,'91.80','2025-11-29 12:17:24','2025-11-29 12:17:24');

/*Table structure for table `user_like` */

DROP TABLE IF EXISTS `user_like`;

CREATE TABLE `user_like` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint unsigned NOT NULL COMMENT '用户ID',
  `video_id` bigint unsigned NOT NULL COMMENT '视频ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_video` (`user_id`,`video_id`),
  KEY `idx_video_id` (`video_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户点赞表';

/*Data for the table `user_like` */

insert  into `user_like`(`id`,`user_id`,`video_id`,`created_at`) values 

(4,3,1,'2025-11-29 12:17:24');

/*Table structure for table `video` */

DROP TABLE IF EXISTS `video`;

CREATE TABLE `video` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '视频ID',
  `user_id` bigint unsigned NOT NULL COMMENT '发布用户ID',
  `title` varchar(200) NOT NULL COMMENT '视频标题',
  `description` text COMMENT '视频描述',
  `cover_url` varchar(2048) DEFAULT NULL,
  `video_url` varchar(2048) DEFAULT NULL,
  `duration` int DEFAULT NULL COMMENT '视频时长(秒)',
  `width` int DEFAULT NULL COMMENT '视频宽度',
  `height` int DEFAULT NULL COMMENT '视频高度',
  `size` bigint DEFAULT NULL COMMENT '文件大小(字节)',
  `category_id` int DEFAULT NULL COMMENT '分类ID',
  `status` tinyint DEFAULT '0' COMMENT '状态: 0-待审核, 1-已审核发布, 2-审核失败 '
  `is_private` tinyint DEFAULT '0' COMMENT '是否私密: 0-公开, 1-私密',
  `view_count` bigint DEFAULT '0' COMMENT '播放量',
  `like_count` int DEFAULT '0' COMMENT '点赞数',
  `comment_count` int DEFAULT '0' COMMENT '评论数',
  `share_count` int DEFAULT '0' COMMENT '分享数',
  `favorite_count` int DEFAULT '0' COMMENT '收藏数',
  `published_at` datetime DEFAULT NULL COMMENT '发布时间',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_status` (`status`),
  KEY `idx_published_at` (`published_at`),
  KEY `idx_view_count` (`view_count`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='视频表';

/*Data for the table `video` */

insert  into `video`(`id`,`user_id`,`title`,`description`,`cover_url`,`video_url`,`duration`,`width`,`height`,`size`,`category_id`,`status`,`is_private`,`view_count`,`like_count`,`comment_count`,`share_count`,`favorite_count`,`published_at`,`created_at`,`updated_at`,`deleted`) values 

(1,2,'周末城市漫步VLOG','记录周末在城市中的悠闲时光','http://localhost:9090/api/v1/download-shared-object/aHR0cDovLzEyNy4wLjAuMTo5MDkwL3ZpZGVvLXBsYXRmb3JtL3ZpZGVvLzEyLmpwZz9YLUFtei1BbGdvcml0aG09QVdTNC1ITUFDLVNIQTI1NiZYLUFtei1DcmVkZW50aWFsPVpXRjdJRk9VMFBYU0M0RFBBR1hLJTJGMjAyNTEyMDUlMkZ1cy1lYXN0LTElMkZzMyUyRmF3czRfcmVxdWVzdCZYLUFtei1EYXRlPTIwMjUxMjA1VDE0MTkwN1omWC1BbXotRXhwaXJlcz00MzIwMCZYLUFtei1TZWN1cml0eS1Ub2tlbj1leUpoYkdjaU9pSklVelV4TWlJc0luUjVjQ0k2SWtwWFZDSjkuZXlKaFkyTmxjM05MWlhraU9pSmFWMFkzU1VaUFZUQlFXRk5ETkVSUVFVZFlTeUlzSW1WNGNDSTZNVGMyTkRrNE5qa3pPQ3dpY0dGeVpXNTBJam9pYldsdWFXOWhaRzFwYmlKOS5xVEotR2tyQ2dYaEdGdTZnaUlyYXFjc0VQbFV3ZEJYRnZTa1M1dko5eml3NWMwbTdmWGtNMi01SXBTNjZaREdlakZRTmxnNVk2RVdJVWQ4Ukg1anF6USZYLUFtei1TaWduZWRIZWFkZXJzPWhvc3QmdmVyc2lvbklkPW51bGwmWC1BbXotU2lnbmF0dXJlPTVmOTUwZjc2OGZhZDU3NDQ3ZTliZjczMDMyMDQyMzhjOGJiZDFkNTg0ZWE1MGVlYzlmYjI5ZGMzNjhjMTZkYjk','http://localhost:9090/api/v1/download-shared-object/aHR0cDovLzEyNy4wLjAuMTo5MDkwL3ZpZGVvLXBsYXRmb3JtL3ZpZGVvLyVFNiU5NiVCMCVFNyU4OSU4NyVFNSU5QyVCQSVFNyVCNCVBMCVFNiU5RCU5MElELUIzR3drajZuTTV0Rkg0LSVFNSVBNiU4NyVFNCVCQSVBNyVFNSU4QyVCQiVFOSU5OSVBMiVFNCVCQSVBNyVFNSU5MCU4RSVFNyU5NyU4NSVFNiU4OCVCRiVFNyU5QSU4NCVFNiU5NiVCMCVFNyU5NCU5RiVFNyU4OCVCNiVFNiVBRiU4RCVFMyU4MCU4Mi5tcDQ_WC1BbXotQWxnb3JpdGhtPUFXUzQtSE1BQy1TSEEyNTYmWC1BbXotQ3JlZGVudGlhbD1aV0Y3SUZPVTBQWFNDNERQQUdYSyUyRjIwMjUxMjA1JTJGdXMtZWFzdC0xJTJGczMlMkZhd3M0X3JlcXVlc3QmWC1BbXotRGF0ZT0yMDI1MTIwNVQxNDE1MjhaJlgtQW16LUV4cGlyZXM9NDMxOTkmWC1BbXotU2VjdXJpdHktVG9rZW49ZXlKaGJHY2lPaUpJVXpVeE1pSXNJblI1Y0NJNklrcFhWQ0o5LmV5SmhZMk5sYzNOTFpYa2lPaUphVjBZM1NVWlBWVEJRV0ZORE5FUlFRVWRZU3lJc0ltVjRjQ0k2TVRjMk5EazROamt6T0N3aWNHRnlaVzUwSWpvaWJXbHVhVzloWkcxcGJpSjkucVRKLUdrckNnWGhHRnU2Z2lJcmFxY3NFUGxVd2RCWEZ2U2tTNXZKOXppdzVjMG03ZlhrTTItNUlwUzY2WkRHZWpGUU5sZzVZNkVXSVVkOFJINWpxelEmWC1BbXotU2lnbmVkSGVhZGVycz1ob3N0JnZlcnNpb25JZD1udWxsJlgtQW16LVNpZ25hdHVyZT02MTJkOTM3OGU2MjBlZmQ4ZjM4OGEyMDJiODYzZTdjMDNiOGZhZGNmMWU0MWFkZTFkMzcyOTE4MzdkNTI3NDQ3',320,1920,1080,1024000,1,1,0,1200,49,14,8,35,'2025-11-20 10:00:00','2025-11-29 12:17:24','2025-12-05 22:19:38',0),

(2,3,'Java编程入门教程','从零开始学习Java编程语言','http://localhost:9090/api/v1/download-shared-object/aHR0cDovLzEyNy4wLjAuMTo5MDkwL3ZpZGVvLXBsYXRmb3JtL3ZpZGVvLyVFNSVCMSU4RiVFNSVCOSU5NSVFNiU4OCVBQSVFNSU5QiVCRSUyMDIwMjUtMDItMTMlMjAxMTQ2MDQuanBnP1gtQW16LUFsZ29yaXRobT1BV1M0LUhNQUMtU0hBMjU2JlgtQW16LUNyZWRlbnRpYWw9WldGN0lGT1UwUFhTQzREUEFHWEslMkYyMDI1MTIwNSUyRnVzLWVhc3QtMSUyRnMzJTJGYXdzNF9yZXF1ZXN0JlgtQW16LURhdGU9MjAyNTEyMDVUMTQxOTQ1WiZYLUFtei1FeHBpcmVzPTQzMjAwJlgtQW16LVNlY3VyaXR5LVRva2VuPWV5SmhiR2NpT2lKSVV6VXhNaUlzSW5SNWNDSTZJa3BYVkNKOS5leUpoWTJObGMzTkxaWGtpT2lKYVYwWTNTVVpQVlRCUVdGTkRORVJRUVVkWVN5SXNJbVY0Y0NJNk1UYzJORGs0Tmprek9Dd2ljR0Z5Wlc1MElqb2liV2x1YVc5aFpHMXBiaUo5LnFUSi1Ha3JDZ1hoR0Z1NmdpSXJhcWNzRVBsVXdkQlhGdlNrUzV2Sjl6aXc1YzBtN2ZYa00yLTVJcFM2NlpER2VqRlFObGc1WTZFV0lVZDhSSDVqcXpRJlgtQW16LVNpZ25lZEhlYWRlcnM9aG9zdCZ2ZXJzaW9uSWQ9bnVsbCZYLUFtei1TaWduYXR1cmU9MDZhNjdhYWQ3YjcyZjUxY2U3Y2M2YjlkYjM4YWYzNjQyZGQyZmYyZmFkZGRlODFmMGM0MzM3ZjI3MzM2OGQ1Ng','http://localhost:9090/api/v1/download-shared-object/aHR0cDovLzEyNy4wLjAuMTo5MDkwL3ZpZGVvLXBsYXRmb3JtL3ZpZGVvLyVFNiU5NiVCMCVFNyU4OSU4NyVFNSU5QyVCQSVFNyVCNCVBMCVFNiU5RCU5MElELUMybEI4dFg3NFp1YUV5LSVFNCVCQSU5QSVFNiVCNCVCMiVFNyVCRSU4RSVFNSVBNSVCMyVFNSU5QyVBOCVFNSU4QSU5RSVFNSU4NSVBQyVFNSVBRSVBNCVFNSU5MiU4QyVFNCVCQSVCQSVFNCVCQSU4QiVFNyVCQiU4RiVFNyU5MCU4NiVFOCVCMCU4OCVFNSVCNyVBNSVFNCVCRCU5QyVFOSU5RCVBMiVFOCVBRiU5NSVFMyU4MCU4MiVFNiU5QyU4OSVFOSVBRCU4NSVFNSU4QSU5QiVFNyU5QSU4NCVFNSVBNSVCMyVFNiU4MCVBNyVFNSU5RCU5MCVFNSU5QyVBOCVFNiVBMSU4QyVFNSVBRCU5MCVFNCVCOCU4QSVFRiVCQyU4QyVFNiU4QSU4QSVFNyVBRSU4MCVFNSU4RSU4NiVFNyVCQiU5OSVFNCVCQSVCQSVFNSU4QSU5QiVFOCVCNSU4NCVFNiVCQSU5MCVFNyVCQiU4RiVFNyU5MCU4NiVFNCVCOCU5QSVFNSU4QSVBMSVFNiU4QiU5QiVFOCU4MSU5OCVFNCVCQSVCQSVFNSU5MSU5OCVFNyU5QSU4NCVFNSVCNyVBNSVFNCVCRCU5QyVFNSU5QyVCQSVFNiU4OSU4MCVFMyU4MCU4Mi5tb3Y_WC1BbXotQWxnb3JpdGhtPUFXUzQtSE1BQy1TSEEyNTYmWC1BbXotQ3JlZGVudGlhbD1aV0Y3SUZPVTBQWFNDNERQQUdYSyUyRjIwMjUxMjA1JTJGdXMtZWFzdC0xJTJGczMlMkZhd3M0X3JlcXVlc3QmWC1BbXotRGF0ZT0yMDI1MTIwNVQxNDE3NDlaJlgtQW16LUV4cGlyZXM9NDMyMDAmWC1BbXotU2VjdXJpdHktVG9rZW49ZXlKaGJHY2lPaUpJVXpVeE1pSXNJblI1Y0NJNklrcFhWQ0o5LmV5SmhZMk5sYzNOTFpYa2lPaUphVjBZM1NVWlBWVEJRV0ZORE5FUlFRVWRZU3lJc0ltVjRjQ0k2TVRjMk5EazROamt6T0N3aWNHRnlaVzUwSWpvaWJXbHVhVzloWkcxcGJpSjkucVRKLUdrckNnWGhHRnU2Z2lJcmFxY3NFUGxVd2RCWEZ2U2tTNXZKOXppdzVjMG03ZlhrTTItNUlwUzY2WkRHZWpGUU5sZzVZNkVXSVVkOFJINWpxelEmWC1BbXotU2lnbmVkSGVhZGVycz1ob3N0JnZlcnNpb25JZD1udWxsJlgtQW16LVNpZ25hdHVyZT0xNzk4Y2IzZmQyODgxYTQ4YTgxMzY2NTI2N2Q5NTQyZjNiOWNjMzhkNWE2MmYxM2UyY2JjYTc0MmM5OTgzMGIx',850,1920,1080,2048000,4,1,0,2500,92,26,15,70,'2025-11-18 14:30:00','2025-11-29 12:17:24','2025-12-05 22:19:52',0);

/*Table structure for table `video_category` */

DROP TABLE IF EXISTS `video_category`;

CREATE TABLE `video_category` (
  `id` int unsigned NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分类名称',
  `parent_id` int DEFAULT '0' COMMENT '父分类ID',
  `sort` int DEFAULT '0' COMMENT '排序',
  `icon` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '图标',
  `status` tinyint DEFAULT '1' COMMENT '状态: 0-禁用, 1-启用',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='视频分类表';

/*Data for the table `video_category` */

insert  into `video_category`(`id`,`name`,`parent_id`,`sort`,`icon`,`status`,`created_at`) values 

(1,'生活',0,1,'Coffee',1,'2025-11-23 18:04:39'),

(2,'娱乐',0,2,'Clapperboard',1,'2025-11-23 18:04:39'),

(3,'游戏',0,3,'Gamepad2',1,'2025-11-23 18:04:39'),

(4,'科技',0,4,'Cpu',1,'2025-11-23 18:04:39'),

(5,'音乐',0,5,'Music',1,'2025-11-23 18:04:39'),

(6,'美食',0,6,'Utensils',1,'2025-11-23 18:04:39'),

(7,'运动',0,7,'Trophy',1,'2025-11-23 18:04:39'),

(8,'教育',0,8,'GraduationCap',1,'2025-11-23 18:04:39');

/*Table structure for table `video_tag` */

DROP TABLE IF EXISTS `video_tag`;

CREATE TABLE `video_tag` (
  `id` int unsigned NOT NULL AUTO_INCREMENT COMMENT '标签ID',
  `name` varchar(50) NOT NULL COMMENT '标签名称',
  `video_count` int DEFAULT '0' COMMENT '视频数量',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='视频标签表';

/*Data for the table `video_tag` */

insert  into `video_tag`(`id`,`name`,`video_count`,`created_at`) values 

(1,'vlog',5,'2025-11-23 17:49:12'),

(2,'搞笑',5,'2025-11-23 17:49:12'),

(3,'美食',5,'2025-11-23 17:49:12'),

(4,'旅游',5,'2025-11-23 17:49:12'),

(5,'教程',5,'2025-11-23 17:49:12'),

(6,'游戏解说',5,'2025-11-23 17:49:12'),

(7,'音乐',5,'2025-11-23 17:49:12'),

(8,'舞蹈',5,'2025-11-23 17:49:12'),

(9,'运动',5,'2025-11-23 17:49:12'),

(10,'科技',5,'2025-11-23 17:49:12');

/*Table structure for table `video_tag_relation` */

DROP TABLE IF EXISTS `video_tag_relation`;

CREATE TABLE `video_tag_relation` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `video_id` bigint unsigned NOT NULL COMMENT '视频ID',
  `tag_id` int unsigned NOT NULL COMMENT '标签ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_video_tag` (`video_id`,`tag_id`),
  KEY `idx_tag_id` (`tag_id`)
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='视频标签关联表';

/*Data for the table `video_tag_relation` */

insert  into `video_tag_relation`(`id`,`video_id`,`tag_id`,`created_at`) values 

(1,1,1,'2025-11-29 13:27:35'),

(2,1,4,'2025-11-29 13:27:35'),

(3,2,5,'2025-11-29 13:27:35'),

(4,2,10,'2025-11-29 13:27:35');

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
