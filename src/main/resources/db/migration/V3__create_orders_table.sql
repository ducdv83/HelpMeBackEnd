-- V3__create_orders_table.sql
CREATE TABLE orders (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    driver_id UUID NOT NULL,
    provider_id UUID,
    status VARCHAR(20) DEFAULT 'BROADCASTING',
    service_type VARCHAR(50),
    pickup_location GEOGRAPHY(POINT, 4326),
    description TEXT,
    media_urls JSONB,
    final_amount DECIMAL(10,2) DEFAULT 0,
    broadcast_radius INT DEFAULT 10000,  -- Bán kính broadcast (meters)
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_orders_driver_id ON orders(driver_id);
CREATE INDEX idx_orders_provider_id ON orders(provider_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_created_at ON orders(created_at);
CREATE INDEX idx_orders_pickup_location ON orders USING GIST(pickup_location);

COMMENT ON TABLE orders IS 'Bảng quản lý đơn hàng cứu hộ';
COMMENT ON COLUMN orders.status IS 'BROADCASTING, MATCHED, EN_ROUTE, ARRIVED, IN_SERVICE, COMPLETED, CANCELLED';
COMMENT ON COLUMN orders.pickup_location IS 'Vị trí sự cố (dùng PostGIS)';
COMMENT ON COLUMN orders.broadcast_radius IS 'Bán kính tìm kiếm provider (mặc định 10km)';