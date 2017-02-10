create table conf_user (
  id number(11) not null,
  user_code varchar2(32) default null,
  user_name varchar2(32) not null,
  password varchar2(32) not null,
  delete_flag number(1) default '0',
  create_time date default null,
  update_time date default null
);
alter table conf_user add constraint primary_conf_user primary key (id);

create table conf_project (
  id number(11) not null,
  proj_code varchar2(32) default null,
  proj_name varchar2(32) default null,
  owner_id number(11) default null,
  development_version number(11) default 0  null,
  production_version number(11) default 0  null,
  test_version number(11) default 0  null,
  delete_flag number(1) default '0',
  create_time date default null,
  update_time date default null
);
alter table conf_project add constraint primary_conf_project primary key (id);

create table conf_project_user (
  proj_id number(11) not null,
  user_id number(11) not null
) ;
alter table conf_project_user add constraint primary_conf_project_user primary key (proj_id,user_id);

create table conf_project_module (
  module_id number(11) not null,
  proj_id number(11) not null,
  module_name varchar2(32) default null
) ;
alter table conf_project_module add constraint primary_conf_project_module primary key (module_id);

create table conf_project_user_role (
  proj_id number(11) not null,
  user_id number(11) not null,
  role_code varchar2(32) not null
) ;
alter table conf_project_user_role add constraint primary_conf_project_user_role primary key (proj_id,user_id,role_code);

create table conf_project_config (
  config_id number(11) not null,
  config_key varchar2(64) not null,
  config_value varchar2(256) not null,
  config_desc varchar2(256) default null,
  project_id number(11) not null,
  module_id number(11) not null,
  delete_flag number(1) default '0',
  opt_user varchar2(32) default null,
  opt_time date default null,
  production_value varchar2(256) not null,
  production_user varchar2(32) default null,
  production_time date default null,
  test_value varchar2(256) not null,
  test_user varchar2(32) default null,
  test_time date default null,
  build_value varchar2(256) not null,
  build_user varchar2(32) default null,
  build_time date default null
) ;
alter table conf_project_config add constraint primary_conf_project_config primary key (config_id);