# wsitm-RDBMS v1.0.0

RDBMS简易工具

[![RDBMS v1.0.0](https://img.shields.io/badge/RDBMS-v1.0.0-brightgreen.svg)](https://gitee.com/wsitm/wsitm-RDBMS) 
[![License](https://img.shields.io/github/license/mashape/apistatus.svg)](https://gitee.com/wsitm/wsitm-RDBMS/blob/master/LICENSE)

## RDBMS简易工具简介

通过上传驱动文件并配置对应的JDBC驱动包，然后配置数据库连接，可以直接导出表结构、在线查看表结构、跨数据库方言生成DDL语句。由于驱动包可以自定义上传，所以理论上支持所有关系型数据库。

迁移数据库时，异构数据库的DDL语句转换非常麻烦。DDL转换功能可以解决这个问题，理论上可以转换市面上常见的数据库方言的DDL语句。

## 功能简介

### 一、驱动管理

- 驱动管理功能允许用户上传并配置各类数据库驱动包。无论是常见的MySQL、Oracle、PostgreSQL、SQL Server，还是其他小众数据库类型，只需上传相应的驱动包，即可进行配置。
- 简化了驱动安装与更新的繁琐过程，实现与各类数据库的无缝对接。

### 二、连接管理

- 连接管理工具支持用户配置JDBC URL，通过简单的设置即可连接到目标数据库。
- 表结构导出：一键导出当前数据库中的所有表结构信息。
- 表信息查看：展示数据库中的所有表基本信息。
- 表结构查看：在线查看当前数据库中的所有表结构信息。
- DDL查看与切换：支持查看所有表的DDL语句，并且用户可以根据需要轻松切换DDL的数据库方言，为不同数据库之间的迁移和转换提供便利。

### 三、DDL转换

- DDL转换功能可以兼容输入各种类型的DDL语句，并根据目标数据库的方言进行转换。
- 用户只需输入原始的DDL语句，并指定目标数据库的方言，工具即可将其转换为相应的DDL语句。

## 演示图

### DDL转换

- **DDL切换数据库方言**
  ![DDL转换@01](doc/DDL转换@01.png)

- **DDL转换表结构**
  ![DDL转换@02](doc/DDL转换@02.png)

### 连接配置

- **连接配置列表**
  ![连接配置@01](doc/连接配置@01.png)

- **连接配置-导出表结构**
  ![连接配置@02](doc/连接配置@02.png)

- **连接配置-查看详情-基本列表**
  ![连接配置@03](doc/连接配置@03.png)

- **连接配置-查看详情-表结构信息**
  ![连接配置@04](doc/连接配置@04.png)

- **连接配置-查看详情-查看DDL语句**
  ![连接配置@05](doc/连接配置@05.png)

### 驱动管理

- **驱动列表**
  ![驱动管理@01](doc/驱动管理@01.png)

- **添加/修改驱动**
  ![驱动管理@02](doc/驱动管理@02.png)

## 实现原理

- 使用动态加载的ClassLoader实现JDBC驱动的动态装卸。
- 基于[Hutool](https://gitee.com/dromara/hutool)间接操作JDBC驱动读取表信息。
- 基于[jdialects](https://gitee.com/drinkjava2/jdialects)根据表结构信息生成DDL语句。
- 基于[jsqlparser](https://github.com/JSQLParser/JSqlParser.git)逆向解析DDL语句为表结构信息。
- 前端使用[Univer](https://gitee.com/dream-num/univer)在线渲染Sheet表格，展示表结构信息。
- 前端使用[codemirror](http://github.com/marijnh/CodeMirror.git)作为文本编辑器渲染DDL语句。
- 前端使用[sqltools](https://github.com/mtxr/vscode-sqltools)格式化SQL。
- 前端使用[xe-utils](https://gitee.com/x-extends/xe-utils)作为函数库和工具类。