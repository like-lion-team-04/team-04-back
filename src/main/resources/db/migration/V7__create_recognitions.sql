create table recognition_images (
    id uuid primary key,
    member_id uuid not null,
    object_key varchar(300) not null,
    content_type varchar(30) not null,
    size_bytes bigint not null,
    sha256 varchar(64) not null,
    stored_at timestamp with time zone not null,
    constraint uk_recognition_images_object_key unique (object_key),
    constraint fk_recognition_images_member foreign key (member_id) references members(id)
);

create table recognitions (
    id uuid primary key,
    member_id uuid not null,
    image_id uuid not null,
    idempotency_key uuid not null,
    request_hash varchar(64) not null,
    image_type varchar(30),
    status varchar(20) not null,
    result_json text,
    error_code varchar(50),
    error_message varchar(300),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint uk_recognitions_image unique (image_id),
    constraint fk_recognitions_member foreign key (member_id) references members(id),
    constraint fk_recognitions_image foreign key (image_id) references recognition_images(id)
);

create index idx_recognitions_member_key_created on recognitions(member_id, idempotency_key, created_at desc);
create index idx_recognitions_member_created on recognitions(member_id, created_at desc);
