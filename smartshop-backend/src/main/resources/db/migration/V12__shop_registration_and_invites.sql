CREATE TABLE shop_registrations (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    shop_name varchar(200) NOT NULL,
    pan_vat_number varchar(50) NOT NULL,
    phone varchar(20),
    address varchar(500),
    status varchar(20) NOT NULL DEFAULT 'PENDING',
    rejection_reason text,
    created_at timestamp NOT NULL DEFAULT now(),
    updated_at timestamp NOT NULL DEFAULT now(),
    CONSTRAINT chk_shop_registrations_status CHECK (status IN ('PENDING','APPROVED','REJECTED'))
);

CREATE INDEX idx_shop_registrations_user ON shop_registrations (user_id);
CREATE INDEX idx_shop_registrations_status ON shop_registrations (status);

CREATE TABLE shop_invite_codes (
    id uuid PRIMARY KEY,
    shop_id uuid NOT NULL REFERENCES shops(id) ON DELETE CASCADE,
    code varchar(20) NOT NULL UNIQUE,
    is_active boolean NOT NULL DEFAULT true,
    created_at timestamp NOT NULL DEFAULT now(),
    updated_at timestamp NOT NULL DEFAULT now()
);

CREATE INDEX idx_shop_invite_codes_shop ON shop_invite_codes (shop_id);
CREATE INDEX idx_shop_invite_codes_code ON shop_invite_codes (code);

CREATE TABLE staff_invitations (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    shop_id uuid NOT NULL REFERENCES shops(id) ON DELETE CASCADE,
    branch_id uuid REFERENCES branches(id),
    invite_code varchar(20) NOT NULL,
    requested_role varchar(50) NOT NULL,
    status varchar(20) NOT NULL DEFAULT 'PENDING',
    rejection_reason text,
    created_at timestamp NOT NULL DEFAULT now(),
    updated_at timestamp NOT NULL DEFAULT now(),
    CONSTRAINT chk_staff_invitations_status CHECK (status IN ('PENDING','APPROVED','REJECTED'))
);

CREATE INDEX idx_staff_invitations_user ON staff_invitations (user_id);
CREATE INDEX idx_staff_invitations_shop ON staff_invitations (shop_id);
CREATE INDEX idx_staff_invitations_status ON staff_invitations (status);
