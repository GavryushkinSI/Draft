--liquibase formatted sql

--changeset draft:1
CREATE TABLE usr
(
    id         VARCHAR(255) NOT NULL,
    name       VARCHAR(255),
    email      VARCHAR(255),
    last_visit TIMESTAMP WITHOUT TIME ZONE,
    is_active  BOOLEAN,
    CONSTRAINT pk_usr PRIMARY KEY (id)
);
--rollback DROP TABLE usr

--changeset draft:2
CREATE TABLE strategy
(
    id        BIGINT NOT NULL,
    producer  VARCHAR(255),
    ticker    VARCHAR(255),
    position  VARCHAR(255),
    slippage  VARCHAR(255),
    consumer  VARCHAR(255),
    is_active VARCHAR(255),
    usr_id    VARCHAR(255),
    CONSTRAINT pk_strategy PRIMARY KEY (id)
);

ALTER TABLE strategy
    ADD CONSTRAINT FK_STRATEGY_ON_USR FOREIGN KEY (usr_id) REFERENCES usr (id);

--rollback DROP TABLE strategy