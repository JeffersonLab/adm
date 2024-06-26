alter session set container = XEPDB1;

CREATE SEQUENCE ADM_OWNER.APP_ID
    INCREMENT BY 1
    START WITH 1
    NOCYCLE
    NOCACHE
    ORDER;

CREATE SEQUENCE ADM_OWNER.ENV_ID
    INCREMENT BY 1
    START WITH 1
    NOCYCLE
    NOCACHE
    ORDER;

CREATE TABLE ADM_OWNER.APP
(
    APP_ID              INTEGER NOT NULL ,
    NAME                VARCHAR2(128 CHAR) NOT NULL ,
    DOC_URL             VARCHAR2(512 CHAR) NULL ,
    CONSTRAINT APP_PK PRIMARY KEY (APP_ID) ,
    CONSTRAINT APP_AK1 UNIQUE (NAME)
);

CREATE TABLE ADM_OWNER.APP_ENV
(
    ENV_ID               INTEGER NOT NULL ,
    APP_ID               INTEGER NOT NULL ,
    NAME                 VARCHAR2(128 CHAR) NOT NULL ,
    REQUEST_SERVICE_USERNAME VARCHAR2(128 CHAR) NOT NULL ,
    RUN_SERVICE_USERNAME VARCHAR2(128 CHAR) NOT NULL ,
    HOSTNAME             VARCHAR2(128 CHAR) NOT NULL ,
    PORT                 INTEGER NOT NULL ,
    DEPLOY_COMMAND       VARCHAR2(256 CHAR) NOT NULL ,
    CONSTRAINT APP_ENV_PK PRIMARY KEY (ENV_ID) ,
    CONSTRAINT APP_ENV_FK1 FOREIGN KEY (APP_ID) REFERENCES ADM_OWNER.APP (APP_ID) ON DELETE CASCADE ,
    CONSTRAINT APP_ENV_AK1 UNIQUE (APP_ID,NAME)
);