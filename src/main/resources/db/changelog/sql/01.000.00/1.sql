--liquibase formatted sql

--changeset draft:1
CREATE TABLE USERS
(
    id         BIGINT             NOT NULL,
    login      VARCHAR(20) UNIQUE NOT NULL,
    password   VARCHAR(255)       NOT NULL,
    chartId    VARCHAR(255),
    email      VARCHAR(255)       NOT NULL,
    last_visit TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT PK_USER PRIMARY KEY (id)
);
--rollback DROP TABLE USERS

--changeset draft:2
CREATE TABLE STRATEGY
(
    id        BIGINT  NOT NULL,
    user_id   BIGINT  NOT NULL,
    name      VARCHAR(20),
    ticker    VARCHAR(255),
    figi      VARCHAR(255),
    position  INTEGER NOT NULL DEFAULT '0',
    direction VARCHAR(10),
    active    BOOLEAN NOT NULL DEFAULT 'false',
    CONSTRAINT PK_STRATEGY PRIMARY KEY (id)
);

ALTER TABLE STRATEGY
    ADD CONSTRAINT FK_STRATEGY_ON_USR_1 FOREIGN KEY (user_id) REFERENCES USERS (id);
--rollback DROP TABLE STRATEGY
