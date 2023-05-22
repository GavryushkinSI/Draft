--liquibase formatted sql

--changeset draft:1
CREATE TABLE ARTICLES
(
    "id"             SERIAL unique not null,
    "content" varchar(4000),
    CONSTRAINT "Articles_pk" PRIMARY KEY ("id")
);
--rollback DROP ARTICLES

--changeset draft:2
CREATE TABLE COMMENTS (
                                   "id"    SERIAL unique not null,
                                   "comment" varchar(200),
                                   "article_id" SERIAL,
                                   "user_id" SERIAL,
                                   "parent_id" serial,
                                   CONSTRAINT "Comments_pk" PRIMARY KEY ("id")
);
--rollback DROP COMMENTS

ALTER TABLE COMMENTS ADD CONSTRAINT "Comments_fk0" FOREIGN KEY (article_id) REFERENCES ARTICLES(id);
ALTER TABLE COMMENTS ADD CONSTRAINT "Comments_fk1" FOREIGN KEY (user_id) REFERENCES USERS(id);
ALTER TABLE COMMENTS ADD CONSTRAINT "Comments_fk2" FOREIGN KEY (parent_id) REFERENCES COMMENTS(id);
alter table comments alter column parent_id drop not null;
alter table comments alter column parent_id drop default;






