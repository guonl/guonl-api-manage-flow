CREATE database if NOT EXISTS `db_api_manage` default character set utf8mb4 collate utf8mb4_general_ci;
use `db_api_manage`;

DROP TABLE IF EXISTS `api_project`;
CREATE TABLE `api_project` (
  `project_id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `project_name` varchar(50) NOT NULL COMMENT '项目名称',
  `project_desc` varchar(100) DEFAULT NULL COMMENT '项目描述',
  `manage_url` varchar(100) NOT NULL COMMENT 'url地址',
  `project_status` int(3) NOT NULL DEFAULT '1' COMMENT '0-禁用 1-启用',
  `create_time` timestamp DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint(1) DEFAULT '0' COMMENT '删除标识，0-未删除，1-已删除',
  PRIMARY KEY (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目列表';


DROP TABLE IF EXISTS `api_group`;
CREATE TABLE `api_group` (
  `group_id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `project_id` int(11) NOT NULL COMMENT '项目id',
  `controller_name` varchar(255) NOT NULL COMMENT '全名称',
  `controller_short_name` varchar(100) NOT NULL COMMENT '简称',
  `controller_desc` varchar(255) DEFAULT NULL COMMENT '描述',
  `controller_status` int(3) NOT NULL DEFAULT '1' COMMENT '0-禁用 1-启用',
  `create_time` timestamp DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint(1) DEFAULT '0' COMMENT '删除标识，0-未删除，1-已删除',
  PRIMARY KEY (`group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='controller列表';


DROP TABLE IF EXISTS `api_url`;
CREATE TABLE `api_url` (
  `url_id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `group_id` int(11) NOT NULL COMMENT 'group id',
  `request_url` varchar(255) NOT NULL COMMENT 'url',
  `method_name` varchar(100) NOT NULL COMMENT '方法名',
  `method_type` varchar(50) NOT NULL COMMENT '方法类型',
  `method_content_type` varchar(50) NOT NULL COMMENT '请求方式',
  `request_param_map` text DEFAULT NULL COMMENT '请求参数，json',
  `return_param_class` varchar(255) DEFAULT NULL COMMENT '返回对象class',
  `return_param_map` text DEFAULT NULL COMMENT '返回参数，json',
  `url_status` int(3) NOT NULL DEFAULT '1' COMMENT '0-禁用 1-启用',
  `url_label` varchar(50) DEFAULT NULL DEFAULT '' COMMENT '符合规范，不规范等',
  `method_desc` varchar(255) DEFAULT NULL COMMENT '描述',
  `create_time` timestamp DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint(1) DEFAULT '0' COMMENT '删除标识，0-未删除，1-已删除',
  PRIMARY KEY (`url_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='controller列表';

DROP TABLE IF EXISTS `api_url_doc`;
CREATE TABLE `api_url_doc` (
  `url_id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'url_id',
  `request_headers` text COMMENT 'Request Headers：Map-JSON格式字符串',
  `query_params` text COMMENT 'Query String Parameters：VO-JSON格式字符串',
  `success_resp_example` text COMMENT 'Response Content：成功接口',
  `fail_resp_example` text COMMENT 'Response Content：失败接口',
  `create_time` timestamp DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` tinyint(1) DEFAULT '0' COMMENT '删除标识，0-未删除，1-已删除',
  PRIMARY KEY (`url_id`)
)ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='url文档描述表';

COMMIT;