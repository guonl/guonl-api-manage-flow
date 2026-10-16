-- =====================================================================
-- APIFlow v2 数据库脚本（Spring Boot 4.1.1 / JDK17 升级配套）
-- 目标库：db_api_manage（utf8mb4）
-- 用法二选一：
--   A. 全新环境：执行【PART A 全量新建】
--   B. 已有 v1 表结构（doc/sql/api-manage-mysql.sql 已执行）：
--      执行【PART B v1→v2 增量升级】 + 【PART C 公共新增表与种子】
-- 注意：PART A 会 DROP 旧表，生产环境请勿执行！
-- =====================================================================

-- =====================================================================
-- PART A 全量新建（全新环境执行）
-- =====================================================================
-- CREATE database if NOT EXISTS `db_api_manage` default character set utf8mb4 collate utf8mb4_general_ci;
-- use `db_api_manage`;

-- DROP TABLE IF EXISTS `api_project`;
CREATE TABLE IF NOT EXISTS `api_project` (
  `project_id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `project_name` varchar(100) NOT NULL COMMENT '项目名称',
  `project_desc` varchar(500) DEFAULT NULL COMMENT '项目描述',
  `manage_url` varchar(255) DEFAULT NULL COMMENT 'actuator管理地址，如 http://host:port/actuator',
  `project_status` tinyint NOT NULL DEFAULT '1' COMMENT '0-禁用 1-启用',
  `owner` varchar(50) DEFAULT NULL COMMENT '负责人',
  `system_name` varchar(100) DEFAULT NULL COMMENT '所属系统',
  `default_env_id` bigint DEFAULT NULL COMMENT '默认环境id',
  `create_time` timestamp DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint DEFAULT '0' COMMENT '删除标识，0-未删除，1-已删除',
  PRIMARY KEY (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目列表';

-- DROP TABLE IF EXISTS `api_group`;
CREATE TABLE IF NOT EXISTS `api_group` (
  `group_id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `project_id` bigint NOT NULL COMMENT '项目id',
  `controller_name` varchar(255) NOT NULL COMMENT 'controller全名',
  `controller_short_name` varchar(100) NOT NULL COMMENT 'controller简称（分组显示名）',
  `controller_desc` varchar(255) DEFAULT NULL COMMENT '描述',
  `controller_status` tinyint NOT NULL DEFAULT '1' COMMENT '0-禁用 1-启用',
  `group_sort` int DEFAULT '0' COMMENT '排序',
  `create_time` timestamp DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint DEFAULT '0' COMMENT '删除标识，0-未删除，1-已删除',
  PRIMARY KEY (`group_id`),
  KEY `idx_project` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='接口分组';

-- DROP TABLE IF EXISTS `api_url`;
CREATE TABLE IF NOT EXISTS `api_url` (
  `url_id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `group_id` bigint NOT NULL COMMENT 'group id',
  `project_id` bigint DEFAULT NULL COMMENT '冗余项目id（树查询加速）',
  `request_url` varchar(255) NOT NULL COMMENT '请求路径',
  `method_name` varchar(100) NOT NULL COMMENT '方法名',
  `method_type` varchar(50) NOT NULL COMMENT 'HTTP方法 GET/POST...',
  `method_content_type` varchar(50) NOT NULL DEFAULT 'none' COMMENT '请求体类型 form/json/none',
  `request_param_map` text COMMENT '请求参数 JSON {name:{type,desc}}',
  `return_param_class` varchar(255) DEFAULT NULL COMMENT '返回对象class',
  `return_param_map` text COMMENT '返回参数 JSON',
  `url_status` tinyint NOT NULL DEFAULT '1' COMMENT '0-禁用 1-启用 2-已下线',
  `url_label` varchar(100) DEFAULT '' COMMENT '标签',
  `method_desc` varchar(255) DEFAULT NULL COMMENT '描述',
  `source_type` tinyint NOT NULL DEFAULT '1' COMMENT '来源:1-同步 2-手工 3-调试另存',
  `last_sync_time` datetime DEFAULT NULL COMMENT '最近同步时间',
  `create_time` timestamp DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint DEFAULT '0' COMMENT '删除标识，0-未删除，1-已删除',
  PRIMARY KEY (`url_id`),
  UNIQUE KEY `uk_group_method_url` (`group_id`, `method_type`, `request_url`),
  KEY `idx_project` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='接口主表';

-- DROP TABLE IF EXISTS `api_url_doc`;
CREATE TABLE IF NOT EXISTS `api_url_doc` (
  `url_id` bigint NOT NULL COMMENT 'url_id，与api_url 1:1',
  `request_headers` text COMMENT 'Request Headers：Map-JSON格式字符串',
  `query_params` text COMMENT 'Query String Parameters：VO-JSON格式字符串',
  `success_resp_example` text COMMENT '成功响应示例',
  `fail_resp_example` text COMMENT '失败响应示例',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` timestamp DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint DEFAULT '0' COMMENT '删除标识，0-未删除，1-已删除',
  PRIMARY KEY (`url_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='接口文档扩展表';

-- =====================================================================
-- PART B v1→v2 增量升级（已有 v1 表时执行）
-- =====================================================================
-- ALTER TABLE `api_project`
--   ADD COLUMN `owner` varchar(50) DEFAULT NULL COMMENT '负责人' AFTER `manage_url`,
--   ADD COLUMN `system_name` varchar(100) DEFAULT NULL COMMENT '所属系统' AFTER `owner`,
--   ADD COLUMN `default_env_id` bigint DEFAULT NULL COMMENT '默认环境id' AFTER `system_name`,
--   MODIFY COLUMN `manage_url` varchar(255) DEFAULT NULL COMMENT 'actuator管理地址',
--   MODIFY COLUMN `project_desc` varchar(500) DEFAULT NULL COMMENT '项目描述';
--
-- ALTER TABLE `api_group`
--   ADD COLUMN `group_sort` int DEFAULT '0' COMMENT '排序' AFTER `controller_status`;
--
-- ALTER TABLE `api_url`
--   ADD COLUMN `project_id` bigint DEFAULT NULL COMMENT '冗余项目id' AFTER `group_id`,
--   ADD COLUMN `source_type` tinyint NOT NULL DEFAULT '1' COMMENT '来源:1-同步 2-手工 3-调试另存' AFTER `method_desc`,
--   ADD COLUMN `last_sync_time` datetime DEFAULT NULL COMMENT '最近同步时间' AFTER `source_type`,
--   MODIFY COLUMN `url_label` varchar(100) DEFAULT '' COMMENT '标签';
--   -- 加唯一索引前请先清理 (group_id, method_type, request_url) 重复数据：
--   -- ALTER TABLE `api_url` ADD UNIQUE INDEX `uk_group_method_url` (`group_id`, `method_type`, `request_url`);
--
-- ALTER TABLE `api_url_doc`
--   ADD COLUMN `remark` varchar(500) DEFAULT NULL COMMENT '备注' AFTER `fail_resp_example`;
--
-- -- v1 存量数据回填冗余项目id：
-- UPDATE `api_url` u JOIN `api_group` g ON u.group_id = g.group_id
--   SET u.project_id = g.project_id WHERE u.project_id IS NULL;

-- =====================================================================
-- PART C 公共：新增表（v2 全部为新表，A/B 均执行本段）
-- =====================================================================

-- DROP TABLE IF EXISTS `api_env`;
CREATE TABLE IF NOT EXISTS `api_env` (
  `env_id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `project_id` bigint NOT NULL COMMENT '项目id',
  `env_name` varchar(50) NOT NULL COMMENT '环境名 dev/test/pre/prod',
  `base_url` varchar(255) NOT NULL COMMENT 'Base URL http(s)://host[:port][/prefix]',
  `env_desc` varchar(255) DEFAULT NULL COMMENT '描述',
  `is_default` tinyint NOT NULL DEFAULT '0' COMMENT '1-默认环境',
  `create_time` timestamp DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint DEFAULT '0' COMMENT '删除标识，0-未删除，1-已删除',
  PRIMARY KEY (`env_id`),
  KEY `idx_project` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目环境';

-- DROP TABLE IF EXISTS `api_test_record`;
CREATE TABLE IF NOT EXISTS `api_test_record` (
  `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `project_id` bigint DEFAULT NULL COMMENT '项目id',
  `env_id` bigint DEFAULT NULL COMMENT '环境id',
  `url_id` bigint DEFAULT NULL COMMENT '关联接口id，空=自由调试',
  `request_method` varchar(20) NOT NULL COMMENT '请求方法',
  `request_url` varchar(500) NOT NULL COMMENT '实际请求完整URL',
  `request_headers` text COMMENT '请求头 JSON',
  `request_query` text COMMENT 'Query参数 JSON',
  `request_body` mediumtext COMMENT '请求体',
  `body_type` varchar(30) DEFAULT 'none' COMMENT 'none/json/form-data/x-www-form-urlencoded/raw',
  `response_status` int DEFAULT NULL COMMENT 'HTTP状态码',
  `response_headers` text COMMENT '响应头 JSON',
  `response_body` mediumtext COMMENT '响应体（按阈值截断）',
  `cost_ms` int DEFAULT NULL COMMENT '耗时ms',
  `success` tinyint NOT NULL DEFAULT '1' COMMENT '1-2xx成功 0-失败',
  `error_msg` varchar(500) DEFAULT NULL COMMENT '异常摘要',
  `operator` varchar(50) DEFAULT NULL COMMENT '操作人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`record_id`),
  KEY `idx_url` (`url_id`),
  KEY `idx_project_time` (`project_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='接口调试历史';

-- DROP TABLE IF EXISTS `flow_info`;
CREATE TABLE IF NOT EXISTS `flow_info` (
  `flow_id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `flow_name` varchar(100) NOT NULL COMMENT '流程名称',
  `flow_desc` varchar(500) DEFAULT NULL COMMENT '流程描述',
  `project_id` bigint DEFAULT NULL COMMENT '归属项目',
  `flow_status` tinyint NOT NULL DEFAULT '1' COMMENT '0-停用 1-启用',
  `create_time` timestamp DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint DEFAULT '0' COMMENT '删除标识，0-未删除，1-已删除',
  PRIMARY KEY (`flow_id`),
  KEY `idx_project` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程定义';

-- DROP TABLE IF EXISTS `flow_step`;
CREATE TABLE IF NOT EXISTS `flow_step` (
  `step_id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `flow_id` bigint NOT NULL COMMENT '流程id',
  `step_name` varchar(100) NOT NULL COMMENT '步骤名',
  `sort_order` int NOT NULL DEFAULT '1' COMMENT '顺序号',
  `step_type` varchar(20) NOT NULL DEFAULT 'HTTP' COMMENT '步骤类型（预留DELAY/SCRIPT）',
  `url_id` bigint DEFAULT NULL COMMENT '关联接口id',
  `request_method` varchar(20) DEFAULT 'GET' COMMENT '请求方法',
  `request_url` varchar(500) DEFAULT NULL COMMENT '请求URL，支持{{var}}',
  `request_headers` text COMMENT '请求头模板 JSON',
  `request_query` text COMMENT 'Query参数模板 JSON',
  `body_type` varchar(30) DEFAULT 'none' COMMENT '请求体类型',
  `request_body` mediumtext COMMENT '请求体模板，支持{{var}}',
  `extract_rules` text COMMENT '提取规则 JSON [{varName,jsonPath,desc}]',
  `assertions` text COMMENT '断言 JSON [{type,target,expect,desc}]',
  `on_failure` varchar(20) NOT NULL DEFAULT 'TERMINATE' COMMENT '失败策略 TERMINATE/CONTINUE',
  `delay_ms` int DEFAULT '0' COMMENT '执行前等待ms',
  `enabled` tinyint NOT NULL DEFAULT '1' COMMENT '1-启用 0-禁用',
  `create_time` timestamp DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint DEFAULT '0' COMMENT '删除标识，0-未删除，1-已删除',
  PRIMARY KEY (`step_id`),
  KEY `idx_flow` (`flow_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程步骤';

-- DROP TABLE IF EXISTS `flow_run`;
CREATE TABLE IF NOT EXISTS `flow_run` (
  `run_id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `flow_id` bigint NOT NULL COMMENT '流程id',
  `flow_name_snapshot` varchar(100) DEFAULT NULL COMMENT '流程名快照',
  `trigger_type` varchar(20) NOT NULL DEFAULT 'MANUAL' COMMENT '触发 MANUAL/DEBUG/RERUN/AI',
  `init_vars` text COMMENT '初始变量 JSON',
  `context_snapshot` mediumtext COMMENT '终态上下文快照 JSON',
  `run_status` varchar(20) NOT NULL DEFAULT 'RUNNING' COMMENT 'RUNNING/SUCCESS/FAILED',
  `total_cost_ms` int DEFAULT NULL COMMENT '总耗时ms',
  `error_msg` varchar(500) DEFAULT NULL COMMENT '失败摘要',
  `operator` varchar(50) DEFAULT NULL COMMENT '触发人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`run_id`),
  KEY `idx_flow` (`flow_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程运行记录';

-- DROP TABLE IF EXISTS `flow_run_step`;
CREATE TABLE IF NOT EXISTS `flow_run_step` (
  `run_step_id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `run_id` bigint NOT NULL COMMENT '运行id',
  `step_id` bigint DEFAULT NULL COMMENT '步骤id',
  `step_name` varchar(100) DEFAULT NULL COMMENT '步骤名快照',
  `sort_order` int NOT NULL DEFAULT '1' COMMENT '顺序号',
  `request_snapshot` mediumtext COMMENT '渲染后请求 JSON {method,url,headers,query,body}',
  `response_status` int DEFAULT NULL COMMENT 'HTTP状态码',
  `response_body` mediumtext COMMENT '响应体（截断）',
  `extract_result` text COMMENT '提取结果 JSON {varName:value}',
  `assert_result` text COMMENT '断言结果 JSON [{type,target,expect,actual,pass}]',
  `step_status` varchar(20) NOT NULL DEFAULT 'RUNNING' COMMENT 'RUNNING/SUCCESS/FAILED/SKIPPED',
  `cost_ms` int DEFAULT NULL COMMENT '耗时ms',
  `error_msg` varchar(500) DEFAULT NULL COMMENT '异常摘要',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`run_step_id`),
  KEY `idx_run` (`run_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程步骤运行明细';

-- DROP TABLE IF EXISTS `api_sync_log`;
CREATE TABLE IF NOT EXISTS `api_sync_log` (
  `sync_id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `project_id` bigint NOT NULL COMMENT '项目id',
  `sync_status` varchar(20) NOT NULL COMMENT 'SUCCESS/FAILED',
  `add_count` int DEFAULT '0' COMMENT '新增接口数',
  `update_count` int DEFAULT '0' COMMENT '更新接口数',
  `offline_count` int DEFAULT '0' COMMENT '下线接口数',
  `fail_msg` varchar(500) DEFAULT NULL COMMENT '失败信息',
  `operator` varchar(50) DEFAULT NULL COMMENT '操作人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`sync_id`),
  KEY `idx_project` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='actuator同步历史';

-- DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE IF NOT EXISTS `sys_config` (
  `config_key` varchar(100) NOT NULL COMMENT '配置键',
  `config_value` text COMMENT '配置值（ai.api-key 为AES密文）',
  `config_desc` varchar(255) DEFAULT NULL COMMENT '说明',
  `update_time` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置KV';

-- =====================================================================
-- PART D 种子数据
-- 说明：auth.password 由应用首次启动时写入（BCrypt 加密 admin123），
--       避免此处硬编码错误的哈希值。
-- =====================================================================
INSERT IGNORE INTO `sys_config` (`config_key`, `config_value`, `config_desc`) VALUES
  ('auth.username', 'admin', '登录账号'),
  ('ai.base-url', '', 'AI 网关地址（OpenAI兼容），如 https://api.deepseek.com'),
  ('ai.model', '', '模型名称，如 deepseek-chat'),
  ('ai.temperature', '0.7', '采样温度');

COMMIT;
