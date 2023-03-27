--liquibase formatted sql

--changeset draft:1
insert into users
values (1, 'Admin',
        '$2a$10$AB/QSx9ck0P9tUhVRMIVYOmww2z3.CeYhTNH4Aq18HxuAQzWlnoBq',
        null,
        'parsesignal@yandex.ru',
        null);
commit;
