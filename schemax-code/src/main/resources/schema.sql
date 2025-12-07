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
    column_list text,
    index_list text
);
create index if not exists dim_table_meta_connect_id_index on dim_table_meta (connect_id);
