alter session set container = XEPDB1;

-- Apps
insert into adm_owner.app (app_id, name, doc_url) values (adm_owner.app_id.nextval, 'dtm', 'https://github.com/jeffersonlab/dtm');
insert into adm_owner.app (app_id, name, doc_url) values (adm_owner.app_id.nextval, 'workmap', 'https://github.com/jeffersonlab/workmap');

-- App Envs
insert into adm_owner.app_env(env_id, app_id, name, authorized_groupname, service_username, hostname, port, deploy_command) values (adm_owner.env_id.nextval, 1, 'prod', 'deployer-group', 'wildfly-service', 'prodhost1.example.com', 22, '/opt/wildfly/deploy.sh dtm');
insert into adm_owner.app_env(env_id, app_id, name, authorized_groupname, service_username, hostname, port, deploy_command) values (adm_owner.env_id.nextval, 1, 'dev', 'deployer-group', 'wildfly-service', 'devhost1.example.com', 22, '/opt/wildfly/deploy.sh dtm');
insert into adm_owner.app_env(env_id, app_id, name, authorized_groupname, service_username, hostname, port, deploy_command) values (adm_owner.env_id.nextval, 2, 'dev', 'deployer-group', 'wildfly-service', 'devhost2.example.com', 22, '/opt/wildfly/deploy.sh workmap');
insert into adm_owner.app_env(env_id, app_id, name, authorized_groupname, service_username, hostname, port, deploy_command) values (adm_owner.env_id.nextval, 2, 'local-proxy', 'deployer-group', 'wildfly-service', 'localhost', 1234, 'touch /tmp/hello && echo');