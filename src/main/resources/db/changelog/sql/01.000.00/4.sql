--liquibase formatted sql

--changeset draft:1
ALTER TABLE strategy
    ADD COLUMN minLot INTEGER;