--liquibase formatted sql

--changeset draft:1
ALTER TABLE strategy
    ADD COLUMN description VARCHAR(2000);

--changeset draft:2
update users set chartid='1046703420' where login = 'Admin';