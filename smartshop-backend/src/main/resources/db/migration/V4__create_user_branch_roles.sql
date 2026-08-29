CREATE TABLE user_branch_roles (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    shop_id uuid REFERENCES shops(id),
    branch_id uuid REFERENCES branches(id),
    role_id uuid NOT NULL REFERENCES roles(id),
    is_active boolean NOT NULL DEFAULT true,
    created_at timestamp NOT NULL DEFAULT now(),
    updated_at timestamp NOT NULL DEFAULT now(),
    CONSTRAINT uq_user_branch_role UNIQUE (user_id, branch_id, role_id)
);

CREATE INDEX idx_ubr_user ON user_branch_roles (user_id);
CREATE INDEX idx_ubr_branch ON user_branch_roles (branch_id);