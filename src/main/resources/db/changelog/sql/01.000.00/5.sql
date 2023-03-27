--liquibase formatted sql

--changeset draft:1
CREATE TABLE LAST_PRICE
(
    "id"             SERIAL unique not null,
    "figi"           varchar(255),
    "name_subscriber" varchar(20),
    "price"          integer,
    "update_time"     TIMESTAMP,
    CONSTRAINT "LastPrice_pk" PRIMARY KEY ("id")
);
--rollback DROP LAST_PRICE
