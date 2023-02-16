--liquibase formatted sql

--changeset draft:1
insert into users
values (6305166571322256210,
        'Admin',
        '$2a$10$AB/QSx9ck0P9tUhVRMIVYOmww2z3.CeYhTNH4Aq18HxuAQzWlnoBq',
        null,
        'parsesignal@yandex.ru',
        null);
commit;
