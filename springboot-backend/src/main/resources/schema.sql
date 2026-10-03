-- ImageShare 建表脚本（幂等，启动时自动执行）
CREATE TABLE IF NOT EXISTS users (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    username     VARCHAR(64)  NOT NULL COMMENT '登录名',
    password     VARCHAR(100) NOT NULL COMMENT 'BCrypt加密密码',
    display_name VARCHAR(64)  NOT NULL COMMENT '显示名称',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '用户表';

CREATE TABLE IF NOT EXISTS shared_images (
    id           VARCHAR(36)  NOT NULL COMMENT '图片UUID',
    sender_id    BIGINT       NOT NULL COMMENT '发送者ID',
    recipient_id BIGINT       NOT NULL COMMENT '接收者ID',
    image_path   VARCHAR(255) NOT NULL COMMENT '图片存储路径',
    is_received  TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否已接收',
    PRIMARY KEY (id),
    KEY idx_recipient_received (recipient_id, is_received)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '共享图片表';
