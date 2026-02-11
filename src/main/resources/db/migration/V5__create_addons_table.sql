-- V5__create_addons_table.sql
CREATE TABLE add_ons (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id UUID NOT NULL,
    reason_code VARCHAR(50) NOT NULL,
    reason_text TEXT,
    amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    evidence_image TEXT,  -- Relative path: addons/order-id/image.jpg
    created_at TIMESTAMP DEFAULT NOW(),
    approved_at TIMESTAMP,
    rejected_at TIMESTAMP
);

CREATE INDEX idx_addons_order_id ON add_ons(order_id);
CREATE INDEX idx_addons_status ON add_ons(status);

COMMENT ON TABLE add_ons IS 'Chi phí phát sinh trong quá trình sửa chữa';
COMMENT ON COLUMN add_ons.reason_code IS 'Mã lý do: EXTRA_PARTS, TOWING_DISTANCE, LABOR_TIME, ...';
COMMENT ON COLUMN add_ons.evidence_image IS 'Ảnh chứng minh (lưu local)';