alter session set container = XEPDB1;

-- Apps
insert into adm_owner.app (app_id, name, doc_url) values (adm_owner.app_id.nextval, 'dtm', 'https://github.com/jeffersonlab/dtm');
insert into adm_owner.app (app_id, name, doc_url) values (adm_owner.app_id.nextval, 'workmap', 'https://github.com/jeffersonlab/workmap');

-- App Envs
insert into adm_owner.app_env(env_id, app_id, name, service_username, hostname, deploy_command) values (adm_owner.env_id.nextval, 1, 'prod', 'wildfly-deployer', 'prodhost1.example.com', '/opt/wildfly/deploy.sh dtm');
insert into adm_owner.app_env(env_id, app_id, name, service_username, hostname, deploy_command) values (adm_owner.env_id.nextval, 1, 'dev', 'wildfly-deployer', 'devhost1.example.com', '/opt/wildfly/deploy.sh dtm');
insert into adm_owner.app_env(env_id, app_id, name, service_username, hostname, deploy_command) values (adm_owner.env_id.nextval, 2, 'dev', 'wildfly-deployer', 'devhost2.example.com', '/opt/wildfly/deploy.sh workmap');