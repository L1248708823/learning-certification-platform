-- 首期-03 IAM 业务表和 Spring Authorization Server JDBC 表。
-- 所有 DDL 使用 iam schema，Flyway history 仍落在 JDBC 默认 schema iam。

CREATE TABLE iam.`user` (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    `password` VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(255) DEFAULT NULL,
    display_name VARCHAR(100) DEFAULT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_iam_user_username (username),
    UNIQUE KEY uk_iam_user_phone (phone)
);

CREATE TABLE iam.role (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(32) NOT NULL,
    name VARCHAR(50) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_iam_role_code (code)
);

CREATE TABLE iam.user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_iam_user_role_user FOREIGN KEY (user_id) REFERENCES iam.`user` (id),
    CONSTRAINT fk_iam_user_role_role FOREIGN KEY (role_id) REFERENCES iam.role (id)
);

CREATE TABLE iam.dict_type (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_iam_dict_type_code (code)
);

CREATE TABLE iam.dict_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    type_code VARCHAR(50) NOT NULL,
    code VARCHAR(50) NOT NULL,
    label VARCHAR(100) NOT NULL,
    sort INT NOT NULL DEFAULT 0,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    UNIQUE KEY uk_iam_dict_item_type_code (type_code, code),
    KEY idx_iam_dict_item_type_enabled_sort (type_code, enabled, sort, id),
    CONSTRAINT fk_iam_dict_item_type FOREIGN KEY (type_code) REFERENCES iam.dict_type (code)
);

CREATE TABLE iam.oauth2_registered_client (
    id varchar(100) NOT NULL,
    client_id varchar(100) NOT NULL,
    client_id_issued_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
    client_secret varchar(200) DEFAULT NULL,
    client_secret_expires_at timestamp DEFAULT NULL,
    client_name varchar(200) NOT NULL,
    client_authentication_methods varchar(1000) NOT NULL,
    authorization_grant_types varchar(1000) NOT NULL,
    redirect_uris varchar(1000) DEFAULT NULL,
    post_logout_redirect_uris varchar(1000) DEFAULT NULL,
    scopes varchar(1000) NOT NULL,
    client_settings varchar(2000) NOT NULL,
    token_settings varchar(2000) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_iam_oauth2_registered_client_client_id (client_id)
);

CREATE TABLE iam.oauth2_authorization (
    id varchar(100) NOT NULL,
    registered_client_id varchar(100) NOT NULL,
    principal_name varchar(200) NOT NULL,
    authorization_grant_type varchar(100) NOT NULL,
    authorized_scopes varchar(1000) DEFAULT NULL,
    attributes blob DEFAULT NULL,
    state varchar(500) DEFAULT NULL,
    authorization_code_value blob DEFAULT NULL,
    authorization_code_issued_at timestamp DEFAULT NULL,
    authorization_code_expires_at timestamp DEFAULT NULL,
    authorization_code_metadata blob DEFAULT NULL,
    access_token_value blob DEFAULT NULL,
    access_token_issued_at timestamp DEFAULT NULL,
    access_token_expires_at timestamp DEFAULT NULL,
    access_token_metadata blob DEFAULT NULL,
    access_token_type varchar(100) DEFAULT NULL,
    access_token_scopes varchar(1000) DEFAULT NULL,
    oidc_id_token_value blob DEFAULT NULL,
    oidc_id_token_issued_at timestamp DEFAULT NULL,
    oidc_id_token_expires_at timestamp DEFAULT NULL,
    oidc_id_token_metadata blob DEFAULT NULL,
    refresh_token_value blob DEFAULT NULL,
    refresh_token_issued_at timestamp DEFAULT NULL,
    refresh_token_expires_at timestamp DEFAULT NULL,
    refresh_token_metadata blob DEFAULT NULL,
    user_code_value blob DEFAULT NULL,
    user_code_issued_at timestamp DEFAULT NULL,
    user_code_expires_at timestamp DEFAULT NULL,
    user_code_metadata blob DEFAULT NULL,
    device_code_value blob DEFAULT NULL,
    device_code_issued_at timestamp DEFAULT NULL,
    device_code_expires_at timestamp DEFAULT NULL,
    device_code_metadata blob DEFAULT NULL,
    PRIMARY KEY (id),
    KEY idx_iam_oauth2_authorization_state (state),
    KEY idx_iam_oauth2_authorization_code (authorization_code_value(255)),
    KEY idx_iam_oauth2_access_token (access_token_value(255)),
    KEY idx_iam_oauth2_refresh_token (refresh_token_value(255))
);

CREATE TABLE iam.oauth2_authorization_consent (
    registered_client_id varchar(100) NOT NULL,
    principal_name varchar(200) NOT NULL,
    authorities varchar(1000) NOT NULL,
    PRIMARY KEY (registered_client_id, principal_name)
);

INSERT INTO iam.role (code, name)
VALUES ('LEARNER', '学习者'), ('TEACHER', '老师'), ('ADMIN', '管理员');

INSERT INTO iam.dict_type (code, name)
VALUES ('COURSE_CATEGORY', '课程分类');

INSERT INTO iam.dict_item (type_code, code, label, sort, enabled)
VALUES
    ('COURSE_CATEGORY', 'FRONTEND', '前端开发', 10, TRUE),
    ('COURSE_CATEGORY', 'BACKEND', '后端开发', 20, TRUE),
    ('COURSE_CATEGORY', 'AI', '人工智能', 30, TRUE);

-- 种子账号统一使用开发密码 Password123!，仅用于本地演示。
INSERT INTO iam.`user` (username, `password`, phone, email, display_name, status)
VALUES
    ('learner1', '$2a$10$7EqJtq98hPqEX7fNZaFWoO5gHfJYdVg0m7fGq7eQxP4Y7L6yN3K5K', '13800000001', NULL, '学习者一号', 'ACTIVE'),
    ('teacher1', '$2a$10$7EqJtq98hPqEX7fNZaFWoO5gHfJYdVg0m7fGq7eQxP4Y7L6yN3K5K', '13800000002', NULL, '老师一号', 'ACTIVE'),
    ('admin1', '$2a$10$7EqJtq98hPqEX7fNZaFWoO5gHfJYdVg0m7fGq7eQxP4Y7L6yN3K5K', '13800000003', NULL, '管理员一号', 'ACTIVE');

INSERT INTO iam.user_role (user_id, role_id)
SELECT u.id, r.id FROM iam.`user` u CROSS JOIN iam.role r
WHERE u.username = 'learner1' AND r.code = 'LEARNER';

INSERT INTO iam.user_role (user_id, role_id)
SELECT u.id, r.id FROM iam.`user` u CROSS JOIN iam.role r
WHERE u.username = 'teacher1' AND r.code = 'TEACHER';

INSERT INTO iam.user_role (user_id, role_id)
SELECT u.id, r.id FROM iam.`user` u CROSS JOIN iam.role r
WHERE u.username = 'admin1' AND r.code = 'ADMIN';
