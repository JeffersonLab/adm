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

CREATE SEQUENCE ADM_OWNER.REMOTE_COMMAND_RESULT_ID
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

CREATE TABLE ADM_OWNER.REMOTE_COMMAND_RESULT
(
    REMOTE_COMMAND_RESULT_ID INTEGER NOT NULL,
    ENV_ID                   INTEGER NOT NULL ,
    EXIT_CODE                INTEGER NULL ,
    OUT                      VARCHAR2(4000 BYTE) NULL ,
    ERR                      VARCHAR2(4000 BYTE) NULL ,
    STACK_TRACE              VARCHAR2(4000 BYTE) NULL ,
    JOB_START                DATE NOT NULL , -- Does not handle DST as local time (oh well - it's complicated to do so in JPA)
    JOB_END                  DATE NULL , -- Does not handle DST
    CONSTRAINT REMOTE_COMMAND_RESULT_PK PRIMARY KEY (REMOTE_COMMAND_RESULT_ID) ,
    CONSTRAINT ENV_FK1 FOREIGN KEY (ENV_ID) REFERENCES ADM_OWNER.APP_ENV (ENV_ID) ON DELETE CASCADE
);