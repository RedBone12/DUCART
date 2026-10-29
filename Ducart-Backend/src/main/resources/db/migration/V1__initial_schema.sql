-- Initial schema for the hardened checkout and password-reset application.
-- Do not edit an applied migration; add a new version instead.

create table `brand` (`active` bit not null, `id` bigint not null auto_increment, `name` varchar(255), `pic` varchar(255), primary key (`id`)) engine=InnoDB;

create table `cart` (`price` float(53), `qty` integer, `stock_quantity` integer, `total` float(53), `id` bigint not null auto_increment, `product_id` bigint, `brand` varchar(255), `color` varchar(255), `name` varchar(255), `pic` varchar(255), `product` varchar(255), `size` varchar(255), `user` varchar(255), primary key (`id`)) engine=InnoDB;

create table `checkout` (`shipping` decimal(19,2), `subtotal` decimal(19,2), `total` decimal(19,2), `id` bigint not null auto_increment, `idempotency_key` varchar(64), `request_fingerprint` varchar(64), `date` varchar(255), `order_status` varchar(255), `payment_mode` varchar(255), `payment_status` varchar(255), `rppid` varchar(255), `user` varchar(255), `products_json` longtext, primary key (`id`)) engine=InnoDB;

create table `contactus` (`active` bit not null, `date` date, `id` bigint not null auto_increment, `email` varchar(255), `name` varchar(255), `phone` varchar(255), `subject` varchar(255), `message` longtext, primary key (`id`)) engine=InnoDB;

create table `maincategory` (`active` bit not null, `id` bigint not null auto_increment, `name` varchar(255), `pic` varchar(255), primary key (`id`)) engine=InnoDB;

create table `newsletter` (`active` bit not null, `id` bigint not null auto_increment, `email` varchar(255) not null, primary key (`id`)) engine=InnoDB;

create table `password_reset_limits` (`attempts` integer not null, `window_start` datetime(6) not null, `bucket_key` varchar(64) not null, primary key (`bucket_key`)) engine=InnoDB;

create table `password_reset_tokens` (`used` bit not null, `expires_at` datetime(6) not null, `user_id` bigint not null, `token_hash` varchar(64) not null, primary key (`user_id`)) engine=InnoDB;

create table `product` (`active` bit not null, `base_price` float(53), `discount` float(53), `final_price` float(53), `stock` bit not null, `stock_quantity` integer, `id` bigint not null auto_increment, `brand` varchar(255), `color` varchar(255), `maincategory` varchar(255), `name` varchar(255), `size` varchar(255), `subcategory` varchar(255), `description` longtext, primary key (`id`)) engine=InnoDB;

create table `product_pics` (`product_id` bigint not null, `pics` varchar(255)) engine=InnoDB;

create table `subcategory` (`active` bit not null, `id` bigint not null auto_increment, `name` varchar(255), `pic` varchar(255), primary key (`id`)) engine=InnoDB;

create table `testimonial` (`active` bit not null, `id` bigint not null auto_increment, `name` varchar(255), `pic` varchar(255), `message` longtext, primary key (`id`)) engine=InnoDB;

create table `users` (`userid` bigint not null auto_increment, `address` varchar(255), `city` varchar(255), `email` varchar(255) not null, `name` varchar(255) not null, `password` varchar(255) not null, `phone` varchar(255) not null, `pic` varchar(255), `pin` varchar(255), `role` varchar(255), `state` varchar(255), `username` varchar(255) not null, primary key (`userid`)) engine=InnoDB;

create table `wishlist` (`price` float(53), `stock_quantity` integer, `id` bigint not null auto_increment, `brand` varchar(255), `color` varchar(255), `name` varchar(255), `pic` varchar(255), `product` varchar(255), `size` varchar(255), `user` varchar(255), primary key (`id`)) engine=InnoDB;

alter table `checkout` add constraint uk_checkout_user_request unique (`user`, `idempotency_key`);

alter table `newsletter` add constraint `UKabct92t8t4itua0rvf4vtn6o8` unique (`email`);

create index idx_reset_limit_window on `password_reset_limits` (`window_start`);

alter table `password_reset_tokens` add constraint `UKeouy94p9xchcrat7pyvrsnfdq` unique (`token_hash`);

alter table `users` add constraint `UK6j5t70rd2eub907qysjvvd76n` unique (`email`);

alter table `users` add constraint `UKassj8wlmlev8obxew6ql3vtmt` unique (`phone`);

alter table `users` add constraint `UKlgkd7iin2rkv9xkrkvdf6do2v` unique (`username`);

alter table `product_pics` add constraint `FKnmijmi6t8y331k4dqxhjc760u` foreign key (`product_id`) references `product` (`id`);
