-- V6__create_ratings_table.sql
CREATE TABLE ratings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id UUID UNIQUE NOT NULL,
    provider_id UUID NOT NULL,
    rating INT CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_ratings_provider_id ON ratings(provider_id);
CREATE INDEX idx_ratings_order_id ON ratings(order_id);

COMMENT ON TABLE ratings IS 'Đánh giá provider sau khi hoàn thành đơn hàng';