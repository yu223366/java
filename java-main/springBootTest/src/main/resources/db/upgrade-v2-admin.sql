-- ============================================================
--  用户管理：山籍软注销标记
--  对应实体 User.deleted（0=正常 1=已注销）
--  采用「查 information_schema 再按需 ALTER」的方式，幂等可重复执行。
--  后台执事可对山友做封禁 / 解封 / 重置暗语 / 调整资源 / 升降执事 / 注销 / 恢复。
-- ============================================================
DELIMITER //

CREATE PROCEDURE IF NOT EXISTS wushan_add_user_deleted()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'wushan'
          AND table_name   = 'wushan_user'
          AND column_name  = 'deleted'
    ) THEN
        ALTER TABLE wushan_user
            ADD COLUMN deleted INT NOT NULL DEFAULT 0
            COMMENT '0=正常 1=已注销(软注销，可恢复)';
    END IF;
END //

DELIMITER ;

CALL wushan_add_user_deleted();
DROP PROCEDURE IF EXISTS wushan_add_user_deleted;
