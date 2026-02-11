-- V4__create_quotes_table.sql
CREATE TABLE quotes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id UUID NOT NULL,
    provider_id UUID NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    base_price DECIMAL(10,2) NOT NULL,
    distance_price DECIMAL(10,2) DEFAULT 0,
    material_price DECIMAL(10,2) DEFAULT 0,
    total_est DECIMAL(10,2) NOT NULL,
    location_source VARCHAR(10),  -- BASE hoặc LIVE
    eta_minutes INT,
    notes TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    CONSTRAINT unique_provider_per_order UNIQUE(order_id, provider_id)
);

CREATE INDEX idx_quotes_order_id ON quotes(order_id);
CREATE INDEX idx_quotes_provider_id ON quotes(provider_id);
CREATE INDEX idx_quotes_status ON quotes(status);

COMMENT ON TABLE quotes IS 'Bảng báo giá từ các provider';
COMMENT ON COLUMN quotes.location_source IS 'BASE: từ garage, LIVE: từ vị trí hiện tại';
COMMENT ON COLUMN quotes.eta_minutes IS 'Thời gian dự kiến đến (phút)';