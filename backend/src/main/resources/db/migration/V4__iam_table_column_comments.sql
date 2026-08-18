-- 补齐 iam 业务表与 SAS 表的表、列注释。
--
-- V2 建表时只写了字段约束没写 COMMENT，数据库侧无法从 SQL 看出每列含义。
-- Flyway 已执行过的迁移不能改动（checksum 校验），所以用本迁移以 ALTER 方式补注释。
--
-- 注释取舍：业务表与客户端表逐列注释；oauth2_authorization / oauth2_authorization_consent
-- 是 Spring Authorization Server 的标准存储表，token 相关 blob 列的结构由 SAS 版本决定、
-- 我们不直接读写，只给关键列注释，其余在表注释中说明，避免注释随 SAS 升级而过时。

-- 用户账号表
ALTER TABLE iam.`user` COMMENT = '用户账号表：IAM 登录主体，密码只存 BCrypt 哈希';
ALTER TABLE iam.`user`
    MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户主键，自增',
    MODIFY COLUMN username VARCHAR(50) NOT NULL COMMENT '登录用户名，全表唯一',
    MODIFY COLUMN `password` VARCHAR(100) NOT NULL COMMENT 'BCrypt 密码哈希，不存明文',
    MODIFY COLUMN phone VARCHAR(20) NOT NULL COMMENT '中国大陆手机号，全表唯一',
    MODIFY COLUMN email VARCHAR(255) DEFAULT NULL COMMENT '可选邮箱，未填写为 NULL',
    MODIFY COLUMN display_name VARCHAR(100) DEFAULT NULL COMMENT '对外展示名称，默认等于用户名',
    MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '账号状态：ACTIVE 正常 / DISABLED 禁用',
    MODIFY COLUMN created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
    MODIFY COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间，行更新自动刷新',
    MODIFY COLUMN version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号，MyBatis-Plus 更新时自增';

-- 角色表
ALTER TABLE iam.role COMMENT = '角色表：RBAC 权限模型中的角色定义';
ALTER TABLE iam.role
    MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '角色主键，自增',
    MODIFY COLUMN code VARCHAR(32) NOT NULL COMMENT '角色编码，全表唯一，如 LEARNER / TEACHER / ADMIN',
    MODIFY COLUMN name VARCHAR(50) NOT NULL COMMENT '角色展示名称，如 学习者';

-- 用户角色关联表
ALTER TABLE iam.user_role COMMENT = '用户角色关联表：一个用户可有多个角色';
ALTER TABLE iam.user_role
    MODIFY COLUMN user_id BIGINT NOT NULL COMMENT '用户主键，关联 iam.user.id',
    MODIFY COLUMN role_id BIGINT NOT NULL COMMENT '角色主键，关联 iam.role.id';

-- 字典类型表
ALTER TABLE iam.dict_type COMMENT = '字典类型表：定义一组字典项的类型';
ALTER TABLE iam.dict_type
    MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '字典类型主键，自增',
    MODIFY COLUMN code VARCHAR(50) NOT NULL COMMENT '类型编码，全表唯一，如 COURSE_CATEGORY',
    MODIFY COLUMN name VARCHAR(100) NOT NULL COMMENT '类型名称，如 课程分类';

-- 字典项表
ALTER TABLE iam.dict_item COMMENT = '字典项表：某个类型下的具体取值';
ALTER TABLE iam.dict_item
    MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '字典项主键，自增',
    MODIFY COLUMN type_code VARCHAR(50) NOT NULL COMMENT '所属类型编码，关联 iam.dict_type.code',
    MODIFY COLUMN code VARCHAR(50) NOT NULL COMMENT '取值编码，类型内唯一，如 FRONTEND',
    MODIFY COLUMN label VARCHAR(100) NOT NULL COMMENT '展示名称，如 前端开发',
    MODIFY COLUMN sort INT NOT NULL DEFAULT 0 COMMENT '排序值，越小越靠前',
    MODIFY COLUMN enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用，禁用项不对外展示';

-- OAuth2 客户端表（Spring Authorization Server 标准表）
ALTER TABLE iam.oauth2_registered_client COMMENT = 'OAuth2 客户端表：SAS 标准存储结构，客户端种子见 OAuth2ClientSeeder';
ALTER TABLE iam.oauth2_registered_client
    MODIFY COLUMN id varchar(100) NOT NULL COMMENT '客户端记录主键（UUID）',
    MODIFY COLUMN client_id varchar(100) NOT NULL COMMENT '客户端 ID，唯一，Web 端为 learning-web',
    MODIFY COLUMN client_id_issued_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '客户端 ID 签发时间',
    MODIFY COLUMN client_secret varchar(200) DEFAULT NULL COMMENT '客户端密钥哈希，公开客户端为 NULL',
    MODIFY COLUMN client_secret_expires_at timestamp DEFAULT NULL COMMENT '客户端密钥过期时间',
    MODIFY COLUMN client_name varchar(200) NOT NULL COMMENT '客户端展示名称',
    MODIFY COLUMN client_authentication_methods varchar(1000) NOT NULL COMMENT '客户端认证方式，JSON 序列化',
    MODIFY COLUMN authorization_grant_types varchar(1000) NOT NULL COMMENT '允许的授权类型，JSON 序列化',
    MODIFY COLUMN redirect_uris varchar(1000) DEFAULT NULL COMMENT '回调地址列表，JSON 序列化',
    MODIFY COLUMN post_logout_redirect_uris varchar(1000) DEFAULT NULL COMMENT '登出后回调地址列表，JSON 序列化',
    MODIFY COLUMN scopes varchar(1000) NOT NULL COMMENT '可申请的 scope，JSON 序列化',
    MODIFY COLUMN client_settings varchar(2000) NOT NULL COMMENT '客户端设置，JSON 序列化，如是否强制 PKCE',
    MODIFY COLUMN token_settings varchar(2000) NOT NULL COMMENT 'token 设置，JSON 序列化，如 access token 有效期';

-- 授权记录表（Spring Authorization Server 标准表）
ALTER TABLE iam.oauth2_authorization COMMENT = '授权记录表：SAS 标准存储结构，存一次授权产生的各阶段 token。token 相关 blob 列由 SAS 框架读写，不直接操作';
ALTER TABLE iam.oauth2_authorization
    MODIFY COLUMN id varchar(100) NOT NULL COMMENT '授权记录主键（UUID）',
    MODIFY COLUMN registered_client_id varchar(100) NOT NULL COMMENT '所属客户端主键，关联 oauth2_registered_client.id',
    MODIFY COLUMN principal_name varchar(200) NOT NULL COMMENT '登录主体名（用户名）',
    MODIFY COLUMN authorization_grant_type varchar(100) NOT NULL COMMENT '授权类型，如 authorization_code',
    MODIFY COLUMN authorized_scopes varchar(1000) DEFAULT NULL COMMENT '用户同意授权的 scope，JSON 序列化',
    MODIFY COLUMN state varchar(500) DEFAULT NULL COMMENT '授权状态参数，用于校验回调来源';

-- 授权确认表（Spring Authorization Server 标准表）
ALTER TABLE iam.oauth2_authorization_consent COMMENT = '授权确认表：SAS 标准存储结构，记录用户已同意的 scope，避免每次重新授权';
ALTER TABLE iam.oauth2_authorization_consent
    MODIFY COLUMN registered_client_id varchar(100) NOT NULL COMMENT '所属客户端主键，关联 oauth2_registered_client.id',
    MODIFY COLUMN principal_name varchar(200) NOT NULL COMMENT '登录主体名（用户名）',
    MODIFY COLUMN authorities varchar(1000) NOT NULL COMMENT '已同意的 scope 列表，JSON 序列化';
