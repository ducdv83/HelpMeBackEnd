-- V2__create_providers_table.sql
CREATE TABLE providers (
    id UUID PRIMARY KEY,  -- Trùng với users.id
    org_name VARCHAR(150),
    base_location GEOGRAPHY(POINT, 4326),
    service_types TEXT[],
    rating_avg DECIMAL(2,1) DEFAULT 0.0,
    kyc_status VARCHAR(20) DEFAULT 'PENDING',
    is_online BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Spatial index cho base_location
CREATE INDEX idx_provider_location ON providers USING GIST(base_location);
CREATE INDEX idx_provider_online ON providers(is_online);

COMMENT ON TABLE providers IS 'Thông tin mở rộng cho đối tác cứu hộ';
COMMENT ON COLUMN providers.base_location IS 'Vị trí garage/trụ sở chính (dùng cho tìm kiếm PostGIS)';
COMMENT ON COLUMN providers.service_types IS 'Các loại dịch vụ: [TOWING, TIRE_CHANGE, BATTERY_JUMP, ...]';
COMMENT ON COLUMN providers.kyc_status IS 'Trạng thái xác minh: PENDING, APPROVED, REJECTED';