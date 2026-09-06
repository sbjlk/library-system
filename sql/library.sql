-- =====================================================
-- 图书管理系统数据库初始化脚本
-- 说明：默认密码均为 123456（MD5: e10adc3949ba59abbe56e057f20f883e）
-- =====================================================

-- 设置连接字符集，避免中文乱码
SET NAMES utf8mb4;

DROP DATABASE IF EXISTS library;
CREATE DATABASE library DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE library;

-- -----------------------------------------------------
-- 用户表
-- -----------------------------------------------------
CREATE TABLE `user` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username`   VARCHAR(50)  NOT NULL COMMENT '用户名',
  `password`   VARCHAR(100) NOT NULL COMMENT '密码(MD5)',
  `nickname`   VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
  `role`       VARCHAR(20)  NOT NULL DEFAULT 'USER' COMMENT '角色 USER/ADMIN',
  `created_at` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户表';

-- -----------------------------------------------------
-- 图书表
-- -----------------------------------------------------
CREATE TABLE `book` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '图书ID',
  `title`           VARCHAR(100) NOT NULL COMMENT '书名',
  `author`          VARCHAR(100) DEFAULT NULL COMMENT '作者',
  `isbn`            VARCHAR(30)  DEFAULT NULL COMMENT 'ISBN',
  `publisher`       VARCHAR(100) DEFAULT NULL COMMENT '出版社',
  `category`        VARCHAR(50)  DEFAULT NULL COMMENT '分类',
  `total_count`     INT          NOT NULL DEFAULT 1 COMMENT '馆藏总数',
  `available_count` INT          NOT NULL DEFAULT 1 COMMENT '可借数量',
  `status`          TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1上架 0下架',
  `created_at`      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_isbn` (`isbn`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '图书表';

-- -----------------------------------------------------
-- 借阅记录表
-- -----------------------------------------------------
CREATE TABLE `borrow_record` (
  `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '借阅ID',
  `user_id`     BIGINT   NOT NULL COMMENT '用户ID',
  `book_id`     BIGINT   NOT NULL COMMENT '图书ID',
  `borrow_time` DATETIME DEFAULT NULL COMMENT '借出时间',
  `due_time`    DATETIME DEFAULT NULL COMMENT '应还时间',
  `return_time` DATETIME DEFAULT NULL COMMENT '实际归还时间',
  `status`      TINYINT  NOT NULL DEFAULT 0 COMMENT '状态 0借出中 1已归还',
  `created_at`  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_book_id` (`book_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '借阅记录表';

-- -----------------------------------------------------
-- 初始化数据
-- -----------------------------------------------------
INSERT INTO `user` (`username`, `password`, `nickname`, `role`) VALUES
('admin',    'e10adc3949ba59abbe56e057f20f883e', '管理员', 'ADMIN'),
('zhangsan', 'e10adc3949ba59abbe56e057f20f883e', '张三',   'USER');

INSERT INTO `book` (`title`, `author`, `isbn`, `publisher`, `category`, `total_count`, `available_count`, `status`) VALUES
('Java核心技术', 'Cay S. Horstmann', '9787111612490', '机械工业出版社', '计算机',   5, 5, 1),
('Spring实战',  'Craig Walls',      '9787115417305', '人民邮电出版社', '计算机',   3, 3, 1),
('三体',        '刘慈欣',           '9787536692930', '重庆出版社',     '科幻小说', 4, 4, 1),
('活着',        '余华',             '9787506365437', '作家出版社',     '文学',     6, 6, 1);
