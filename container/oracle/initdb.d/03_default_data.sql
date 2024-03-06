alter session set container = XEPDB1;

-- Special characters such as the ampersand will result in prompt without this directive.
SET DEFINE OFF;


-- Apps
insert into adm_owner.app (app_id, name, doc_url) values (adm_owner.app_id.nextval, 'testapp', NULL);
insert into adm_owner.app (app_id, name, doc_url) values (adm_owner.app_id.nextval, 'dtm', 'https://github.com/jeffersonlab/dtm');
insert into adm_owner.app (app_id, name, doc_url) values (adm_owner.app_id.nextval, 'workmap', 'https://github.com/jeffersonlab/workmap');

-- App Envs
insert into adm_owner.app_env(env_id, app_id, name, authorized_groupname, service_username, hostname, port, deploy_command) values (adm_owner.env_id.nextval, 1, 'local-dev', 'deployer-group', 'testuser', 'localhost', 1234, 'touch /tmp/hello && echo');
insert into adm_owner.app_env(env_id, app_id, name, authorized_groupname, service_username, hostname, port, deploy_command) values (adm_owner.env_id.nextval, 1, 'local-demo', 'deployer-group', 'testuser', 'sshd', 22, 'touch /tmp/hello && echo');
insert into adm_owner.app_env(env_id, app_id, name, authorized_groupname, service_username, hostname, port, deploy_command) values (adm_owner.env_id.nextval, 2, 'prod', 'deployer-group', 'wildfly-service', 'prodhost1.example.com', 22, '/opt/wildfly/deploy.sh dtm');
insert into adm_owner.app_env(env_id, app_id, name, authorized_groupname, service_username, hostname, port, deploy_command) values (adm_owner.env_id.nextval, 2, 'dev', 'deployer-group', 'wildfly-service', 'devhost1.example.com', 22, '/opt/wildfly/deploy.sh dtm');
insert into adm_owner.app_env(env_id, app_id, name, authorized_groupname, service_username, hostname, port, deploy_command) values (adm_owner.env_id.nextval, 3, 'dev', 'deployer-group', 'wildfly-service', 'devhost2.example.com', 22, '/opt/wildfly/deploy.sh workmap');