--liquibase formatted sql

--changeset draft:1
ALTER TABLE strategy
    ADD COLUMN minLot numeric(20,5);