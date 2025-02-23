export const DEMO_SQL = "\n" +
  "create table sys_user (\n" +
  "  user_id bigserial not null,\n" +
  "  dept_id int8,\n" +
  "  user_name varchar(30) not null,\n" +
  "  nick_name varchar(30) not null,\n" +
  "  user_type varchar(2) default '00',\n" +
  "  email varchar(50) default '',\n" +
  "  phonenumber varchar(11) default '',\n" +
  "  sex char(1) default '0',\n" +
  "  avatar varchar(100) default '',\n" +
  "  \"password\" varchar(100) default '',\n" +
  "  status char(1) default '0',\n" +
  "  del_flag char(1) default '0',\n" +
  "  login_ip varchar(128) default '',\n" +
  "  login_date timestamp,\n" +
  "  create_by varchar(64) default '',\n" +
  "  create_time timestamp,\n" +
  "  update_by varchar(64) default '',\n" +
  "  update_time timestamp,\n" +
  "  remark varchar(500),\n" +
  "  primary key (user_id)\n" +
  ");\n" +
  "comment on table sys_user is '用户信息表';\n" +
  "comment on column sys_user.user_id is '用户ID';\n" +
  "comment on column sys_user.dept_id is '部门ID';\n" +
  "comment on column sys_user.user_name is '用户账号';\n" +
  "comment on column sys_user.nick_name is '用户昵称';\n" +
  "comment on column sys_user.user_type is '用户类型（00系统用户）';\n" +
  "comment on column sys_user.email is '用户邮箱';\n" +
  "comment on column sys_user.phonenumber is '手机号码';\n" +
  "comment on column sys_user.sex is '用户性别（0男 1女 2未知）';\n" +
  "comment on column sys_user.avatar is '头像地址';\n" +
  "comment on column sys_user.\"password\" is '密码';\n" +
  "comment on column sys_user.status is '帐号状态（0正常 1停用）';\n" +
  "comment on column sys_user.del_flag is '删除标志（0代表存在 2代表删除）';\n" +
  "comment on column sys_user.login_ip is '最后登录IP';\n" +
  "comment on column sys_user.login_date is '最后登录时间';\n" +
  "comment on column sys_user.create_by is '创建者';\n" +
  "comment on column sys_user.create_time is '创建时间';\n" +
  "comment on column sys_user.update_by is '更新者';\n" +
  "comment on column sys_user.update_time is '更新时间';\n" +
  "comment on column sys_user.remark is '备注';\n";
