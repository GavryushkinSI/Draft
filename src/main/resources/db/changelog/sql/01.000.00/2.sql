--liquibase formatted sql

--changeset draft:1
insert into users
values (1, 'Divnoyarsk', '$2a$10$EslDOAPsFkTu4RwrDY0CXeimrsyTYOoVnW3DcLyTf/QreCCi.ag.C', null,
        'divnoyarsk@gmail.com', null),
       (2, 'alex', '$2a$10$0NiB1WnD7SY.y/SvYgs4Tem2r3Mn7W2CO6L6/DDvyueRe0egDJzQC', null,
        'vgv@inbox.ru', null),
       (3, 'Admin', '$2a$10$AB/QSx9ck0P9tUhVRMIVYOmww2z3.CeYhTNH4Aq18HxuAQzWlnoBq', '1046703420',
        'parsesignal@yandex.ru', null),
       (4, 'oleg', '$2a$10$CbQ7OnB.wleBoj.JjUO6kO/cBRaDRZP5kheeOzwW4opBZ2L6Qwfvu', null,
        'i.shopleon@gmail.com', null),
       (5, 'Алексей', '$2a$10$nFSZSPomFK8BjP4t5VhDWuu7iyVtCcX0RbvtHi5x8tW1b.CHun/vG', null,
        '79054315301@mail.ru', null);
commit;
