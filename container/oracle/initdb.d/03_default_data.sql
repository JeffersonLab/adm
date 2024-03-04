alter session set container = XEPDB1;

insert into adm_owner.app (app_id, name, doc_url) values (adm_owner.app_id.nextval, 'dtm', 'https://github.com/jeffersonlab/dtm');
insert into adm_owner.app (app_id, name, doc_url) values (adm_owner.app_id.nextval, 'workmap', 'https://github.com/jeffersonlab/workmap');