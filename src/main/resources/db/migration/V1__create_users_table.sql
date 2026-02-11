-- V1__create_users_table.sql
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    phone VARCHAR(15) UNIQUE NOT NULL,
    full_name VARCHAR(100),
    role VARCHAR(10) CHECK (role IN ('DRIVER', 'PROVIDER')),
    avatar_url TEXT,
    push_token TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_users_phone ON users(phone);
CREATE INDEX idx_users_role ON users(role);

COMMENT ON TABLE users IS 'Bảng quản lý người dùng (Tài xế & Đối tác)';
COMMENT ON COLUMN users.role IS 'DRIVER: Tài xế, PROVIDER: Đối tác cứu hộ';