alter session set container = XEPDB1;

ALTER SYSTEM SET db_create_file_dest = '/opt/oracle/oradata';

create tablespace ADM;

create user "ADM_OWNER" profile "DEFAULT" identified by "password" default tablespace "ADM" account unlock;

grant connect to ADM_OWNER;
grant unlimited tablespace to ADM_OWNER;

grant create view to ADM_OWNER;
grant create sequence to ADM_OWNER;
grant create table to ADM_OWNER;
grant create procedure to ADM_OWNER;
grant create type to ADM_OWNER;