create table if not exists dim_jdbc_info (
    jdbc_id integer auto_increment primary key,
    jdbc_name varchar(255),
    jdbc_file varchar(512),
    driver_class varchar(512),
    create_time timestamp
);

create table if not exists dim_connect_info (
    connect_id integer auto_increment primary key,
    connect_name varchar(255),
    jdbc_id integer,
    jdbc_url varchar(2048),
    username varchar(255),
    password varchar(255),
    filter_type integer default 1,
    wildcard varchar(4096),
    create_time timestamp
);

create table if not exists dim_table_meta (
    id bigint auto_increment primary key,
    connect_id integer,
    schema_name varchar(512),
    catalog_name varchar(512),
    table_name varchar(512),
    comment text,
    num_rows integer,
    column_list_json text,
    index_list_json text
);
create index if not exists dim_table_meta_connect_id_index on dim_table_meta (connect_id);

create table if not exists dim_template_info (
    tp_id integer auto_increment primary key,
    tp_name varchar(256) not null,
    tp_type smallint not null,
    tp_content text not null,
    create_time timestamp
);

create table if not exists dim_connect_template_link (
    connect_id integer not null,
    tp_id integer not null,
    is_def integer default 0,
    unique (connect_id, tp_id)
);

create table if not exists dim_type_mapping_rule (
    rule_id integer auto_increment primary key,
    rule_name varchar(255),
    source_database varchar(128),
    target_database varchar(128),
    source_type varchar(128),
    target_type varchar(128),
    length_strategy varchar(32),
    length_value integer,
    precision_strategy varchar(32),
    precision_value integer,
    scale_strategy varchar(32),
    scale_value integer,
    priority integer default 100,
    enabled integer default 1,
    builtin integer default 0,
    remark varchar(1024),
    create_time timestamp,
    update_time timestamp
);
create index if not exists dim_type_mapping_rule_match_index
    on dim_type_mapping_rule (source_database, target_database, source_type, enabled, priority);
