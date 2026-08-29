CREATE TABLE roles (
    id uuid PRIMARY KEY,
    name varchar(50) NOT NULL UNIQUE,
    description varchar(255),
    created_at timestamp NOT NULL DEFAULT now(),
    updated_at timestamp NOT NULL DEFAULT now()
);

CREATE TABLE users (
    id uuid PRIMARY KEY,
    first_name varchar(100) NOT NULL,
    last_name varchar(100) NOT NULL,
    email varchar(255) NOT NULL UNIQUE,
    phone varchar(20),
    password_hash varchar(255) NOT NULL,
    profile_image_url varchar(500),
    status varchar(20) NOT NULL DEFAULT 'ACTIVE',
    created_at timestamp NOT NULL DEFAULT now(),
    updated_at timestamp NOT NULL DEFAULT now(),
    CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE','BLOCKED','SUSPENDED'))
);