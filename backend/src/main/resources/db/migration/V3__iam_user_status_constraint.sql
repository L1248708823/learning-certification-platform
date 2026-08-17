-- 用户账号状态约束。
-- 持久化编码与 com.learningplatform.iam.user.UserStatus 保持一致；新增状态必须同时追加迁移。

ALTER TABLE iam.`user`
    ADD CONSTRAINT ck_iam_user_status
        CHECK (status IN ('ACTIVE', 'DISABLED'));
