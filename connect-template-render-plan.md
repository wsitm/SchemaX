# 连接配置模板渲染改造计划（待确认）

- 日期：2026-02-15
- 项目：`SchemaX`
- 环境约束：
  - JDK17：`C:\env\jdk-17.0.3.1`
  - Maven：`C:\env\maven-3.9.9`

## 1. 全局代码结构扫描结论

- 后端模块：`schemax-code`（Spring Boot 3 + MyBatis + H2）
  - 连接配置：`ConnectInfoController` + `ConnectInfoServiceImpl` + `ConnectInfoMapper`
  - 模板管理：`TemplateInfoController` + `TemplateInfoServiceImpl` + `TemplateInfoMapper`
  - 表结构缓存：`TableMetaMapper`（`dim_table_meta`）
- 前端模块：`schemax-ui`（Vue3 + Element Plus + Univer + CodeMirror）
  - 连接配置页：`src/views/rdbms/connect/index.vue`
  - 连接详情页：`src/views/rdbms/connect/TableInfo.vue`
  - 导出入口：连接配置页“更多 -> 导出”
  - 模板管理页：`src/views/rdbms/template/index.vue`
- 当前现状：
  - 模板管理仅做模板 CRUD（excel/markdown），尚未与连接配置详情和导出联动。
  - 连接详情“表结构”固定渲染（非模板）。
  - 导出表结构固定导出 Excel（非模板）。

## 2. 需求拆解

1. 新增连接与模板关联表：`dim_connect_template_link`。
2. 打开连接详情查看表结构时：
   - 优先渲染默认模板（`is_def=1`）
   - 没有默认时渲染第一个模板
   - 支持切换模板
3. 导出表结构时：
   - 同样默认模板优先
   - 支持切换模板
4. 连接配置列表操作栏新增“模板”按钮：
   - 可勾选关联模板
   - 可配置默认模板

## 3. 技术方案（拟实施）

### 3.1 数据库与模型改造

- 在 `schema.sql` 新增表：
  - `dim_connect_template_link(connect_id, tp_id, is_def)`
  - 联合唯一：`(connect_id, tp_id)`
- 新增后端实体/VO/Mapper：
  - `ConnectTemplateLink`（domain）
  - `ConnectTemplateLinkVO`（含模板名称、类型、默认标识）
  - `ConnectTemplateLinkMapper` + XML
- 约束策略：
  - 同一连接只允许一个默认模板（服务层校验 + 更新时先清空后设置）。
  - 删除模板时，自动清理关联表中的孤儿记录。

### 3.2 后端接口改造

在 `ConnectInfoController` 增加模板关联与渲染相关接口：

- `GET /rdbms/connect/{connectId}/templates`
  - 查询该连接已关联模板（含默认标记）
- `PUT /rdbms/connect/{connectId}/templates`
  - 保存关联模板与默认模板
- `GET /rdbms/connect/render/{connectId}/tableInfo`
  - 按默认/指定模板返回渲染结果（用于详情页展示）
  - 支持参数：`tpId`、`filterType`、`wildcard`
- 复用导出接口：`POST /rdbms/connect/export/{connectId}/tableInfo`
  - 新增参数：`tpId`（可选）
  - 不传时按“默认模板 -> 第一个模板 -> 系统默认渲染”策略

新增服务层：

- `TemplateRenderService`
  - 输入：`TableVO` 列表 + 模板内容 + 模板类型
  - 输出：渲染结果对象（excel/markdown）
- 选择模板逻辑统一在服务层实现，前端仅传可选 `tpId`。

### 3.3 前端页面改造

#### A. 连接配置列表页 `connect/index.vue`

- 操作栏新增“模板”按钮（放在“更多”下拉中）。
- 新增模板关联弹框：
  - 左侧模板列表（多选）
  - 默认模板单选（必须属于已勾选项）
  - 保存调用 `PUT /{connectId}/templates`

#### B. 连接详情页 `connect/TableInfo.vue`

- “表结构”页签顶部新增模板下拉（仅显示该连接已关联模板）。
- 打开详情时自动按默认模板渲染。
- 切换模板时调用 render 接口刷新视图。

#### C. 导出弹框（在 `connect/index.vue`）

- 增加模板选择下拉。
- 打开导出弹框默认选中默认模板。
- 导出请求带 `tpId` 参数。

### 3.4 模板渲染规则（建议，需你确认）

为兼容现有占位符（`${tableName}`、`${columnName}` 等），采用以下规则：

- 表级占位符：直接替换（如 `${schema}`、`${tableComment}`）。
- 列级占位符：
  - Markdown：包含列占位符的行，对每列重复一行。
  - Excel：包含列占位符的行，对每列复制一行。
- 多表渲染：按“表”为单位串联输出（模板块按每张表重复）。

如果你同意此规则，我会按该规则实现渲染器。

## 4. 预计改动文件清单

后端（新增/修改）：

- `schemax-code/src/main/resources/schema.sql`
- `schemax-code/src/main/java/org/wsitm/schemax/web/ConnectInfoController.java`
- `schemax-code/src/main/java/org/wsitm/schemax/service/IConnectInfoService.java`
- `schemax-code/src/main/java/org/wsitm/schemax/service/impl/ConnectInfoServiceImpl.java`
- `schemax-code/src/main/java/org/wsitm/schemax/mapper/ConnectInfoMapper.java`（如需补字段）
- `schemax-code/src/main/resources/mapper/ConnectInfoMapper.xml`（如需补字段）
- `schemax-code/src/main/java/org/wsitm/schemax/mapper/ConnectTemplateLinkMapper.java`（新增）
- `schemax-code/src/main/resources/mapper/ConnectTemplateLinkMapper.xml`（新增）
- `schemax-code/src/main/java/org/wsitm/schemax/entity/domain/ConnectTemplateLink.java`（新增）
- `schemax-code/src/main/java/org/wsitm/schemax/entity/vo/ConnectTemplateLinkVO.java`（新增）
- `schemax-code/src/main/java/org/wsitm/schemax/service/impl/TemplateRenderService.java`（新增）

前端（新增/修改）：

- `schemax-ui/src/api/rdbms/connect.js`
- `schemax-ui/src/views/rdbms/connect/index.vue`
- `schemax-ui/src/views/rdbms/connect/TableInfo.vue`
- `schemax-ui/src/views/rdbms/connect/data.js`（必要时抽取渲染工具）
- `schemax-ui/src/views/rdbms/template/data.js`（必要时补占位符说明）

## 5. 实施步骤

1. 建表与 Mapper：落地关联表及 CRUD。  
2. 后端模板关联接口：查询/保存默认模板与关联模板。  
3. 渲染服务：实现默认模板选择、占位符替换、切换渲染。  
4. 详情页联动：加载默认模板并支持切换。  
5. 导出联动：导出弹框支持模板选择并带 `tpId` 导出。  
6. 回归测试：连接详情、导出、模板关联、默认模板边界场景。

## 6. 验收标准

- 每个连接可维护多模板关联且最多一个默认模板。
- 详情页打开时自动使用默认模板渲染；无默认则首个模板。
- 详情页可切换模板，切换后渲染内容即时变化。
- 导出默认按默认模板，且可手动切换模板后导出。
- 模板解绑或模板删除后，不出现脏关联数据。

## 7. 需你确认

1. 是否按“表级替换 + 列级行复制”的渲染规则实现？  
2. 导出格式是否按模板类型输出：excel 模板导出 xlsx，markdown 模板导出 md？  
3. `word(tpType=2)` 当前前端未启用，是否本次明确不做？

---

确认后我再开始代码改造。
