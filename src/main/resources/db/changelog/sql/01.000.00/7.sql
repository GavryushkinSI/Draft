--liquibase formatted sql

--changeset draft:1
CREATE TABLE orders (
                        "id" serial NOT NULL UNIQUE,
                        "strategy_id" bigint NOT NULL,
                        "price" numeric(20,5) NOT NULL,
                        "quantity" numeric(20,5) NOT NULL,
                        "direction" VARCHAR(255) NOT NULL,
                        "date" VARCHAR(255) NOT NULL,
                        CONSTRAINT "orders_pk" PRIMARY KEY ("id")
);

ALTER TABLE orders
    ADD CONSTRAINT FK_ORDERS_ON_STRATEGY_1 FOREIGN KEY (strategy_id) REFERENCES strategy (id);
--rollback DROP orders

--changeset draft:2
alter table strategy
    add consumers varchar(255);

--changeset draft:3
alter table strategy
    add enter_average_price varchar(255) default '0;0';
