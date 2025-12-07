create table if not exists jdbc_info (
    jdbc_id varchar(255) primary key,
    jdbc_name varchar(255),
    jdbc_file varchar(255),
    driver_class varchar(255),
    create_time timestamp
);

create table if not exists connect_info (
    connect_id varchar(255) primary key,
    connect_name varchar(255),
    jdbc_id varchar(255),
    jdbc_url varchar(512),
    username varchar(255),
    password varchar(255),
    wildcard varchar(255),
    create_time timestamp
);

create table if not exists table_meta (
    id bigint auto_increment primary key,
    connect_id varchar(255),
    schema_name varchar(255),
    catalog_name varchar(255),
    table_name varchar(255),
    comment text,
    num_rows integer,
    column_list text,
    index_list text
);
create index if not exists table_meta_connect_id_index on table_meta (connect_id);
