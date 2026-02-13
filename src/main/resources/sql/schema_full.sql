-- ============================================
-- HELPME BACKEND - FULL DATABASE SCHEMA
-- Generated: 2026-02-12
-- PostgreSQL 15+ with PostGIS
-- ============================================

-- ============================================
-- EXTENSIONS
-- ============================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS pg_trgm;  -- For text search (optional)


-- ============================================
-- DROP EXISTING TABLES (if recreating)
-- ============================================

-- Uncomment to drop all tables and start fresh
-- DROP TABLE IF EXISTS flyway_schema_history CASCADE;
-- DROP TABLE IF EXISTS ratings CASCADE;
-- DROP TABLE IF EXISTS add_ons CASCADE;
-- DROP TABLE IF EXISTS quotes CASCADE;
-- DROP TABLE IF EXISTS orders CASCADE;
-- DROP TABLE IF EXISTS otps CASCADE;
-- DROP TABLE IF EXISTS providers CASCADE;
-- DROP TABLE IF EXISTS users CASCADE;


-- ============================================
-- ENUMS & TYPES
-- ============================================

-- User roles
DO $$ BEGIN
    CREATE TYPE user_role AS ENUM ('DRIVER', 'PROVIDER', 'ADMIN');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Order status
DO $$ BEGIN
    CREATE TYPE order_status AS ENUM (
        'BROADCASTING',
        'MATCHED',
        'EN_ROUTE',
        'ARRIVED',
        'IN_SERVICE',
        'COMPLETED',
        'CANCELLED'
    );
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Quote status
DO $$ BEGIN
    CREATE TYPE quote_status AS ENUM ('PENDING', 'ACCEPTED', 'REJECTED', 'EXPIRED');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Add-on status
DO $$ BEGIN
    CREATE TYPE addon_status AS ENUM ('PENDING', 'APPROVED', 'REJECTED');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- KYC status
DO $$ BEGIN
    CREATE TYPE kyc_status AS ENUM ('PENDING', 'APPROVED', 'REJECTED');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;


-- ============================================
-- TABLE: users
-- ============================================

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phone VARCHAR(20) UNIQUE NOT NULL,
    name VARCHAR(100),
    email VARCHAR(100),
    role user_role NOT NULL,
    avatar_url TEXT,
    push_token VARCHAR(255),
    push_token_updated_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_users_phone ON users(phone);
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);
CREATE INDEX IF NOT EXISTS idx_users_push_token ON users(push_token) WHERE push_token IS NOT NULL;

COMMENT ON TABLE users IS 'User accounts (drivers and providers)';
COMMENT ON COLUMN users.push_token IS 'Expo push notification token for mobile app';


-- ============================================
-- TABLE: providers
-- ============================================

CREATE TABLE IF NOT EXISTS providers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID UNIQUE NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    org_name VARCHAR(200),
    is_online BOOLEAN DEFAULT false,
    kyc_status kyc_status DEFAULT 'PENDING',
    base_location GEOGRAPHY(Point, 4326),
    live_location GEOGRAPHY(Point, 4326),
    live_location_updated_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_providers_user_id ON providers(user_id);
CREATE INDEX IF NOT EXISTS idx_providers_base_location ON providers USING GIST(base_location);
CREATE INDEX IF NOT EXISTS idx_providers_live_location ON providers USING GIST(live_location);
CREATE INDEX IF NOT EXISTS idx_providers_live_location_updated ON providers(live_location_updated_at);
CREATE INDEX IF NOT EXISTS idx_providers_online ON providers(is_online) WHERE is_online = true;

COMMENT ON TABLE providers IS 'Service providers (mechanics, tow trucks)';
COMMENT ON COLUMN providers.base_location IS 'Fixed garage/office location (permanent)';
COMMENT ON COLUMN providers.live_location IS 'Real-time location when EN_ROUTE (fallback for Redis)';


-- ============================================
-- TABLE: orders
-- ============================================

CREATE TABLE IF NOT EXISTS orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    driver_id UUID NOT NULL REFERENCES users(id),
    provider_id UUID REFERENCES users(id),
    status order_status NOT NULL DEFAULT 'BROADCASTING',
    service_type VARCHAR(50) NOT NULL,
    description TEXT,
    pickup_location GEOGRAPHY(Point, 4326) NOT NULL,
    pickup_lat DOUBLE PRECISION NOT NULL,
    pickup_lng DOUBLE PRECISION NOT NULL,
    media_urls TEXT[],
    final_amount DECIMAL(10,2) DEFAULT 0,
    accepted_quote_id UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_orders_driver ON orders(driver_id);
CREATE INDEX IF NOT EXISTS idx_orders_provider ON orders(provider_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status);
CREATE INDEX IF NOT EXISTS idx_orders_location ON orders USING GIST(pickup_location);
CREATE INDEX IF NOT EXISTS idx_orders_created ON orders(created_at DESC);

COMMENT ON TABLE orders IS 'Roadside assistance requests';


-- ============================================
-- TABLE: quotes
-- ============================================

CREATE TABLE IF NOT EXISTS quotes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    provider_id UUID NOT NULL REFERENCES users(id),
    base_price DECIMAL(10,2) NOT NULL,
    distance_price DECIMAL(10,2) DEFAULT 0,
    material_price DECIMAL(10,2) DEFAULT 0,
    total_est DECIMAL(10,2) NOT NULL,
    location_source VARCHAR(20) DEFAULT 'BASE',
    eta_minutes INTEGER,
    notes TEXT,
    status quote_status DEFAULT 'PENDING',
    accepted BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_quotes_order ON quotes(order_id);
CREATE INDEX IF NOT EXISTS idx_quotes_provider ON quotes(provider_id);
CREATE INDEX IF NOT EXISTS idx_quotes_status ON quotes(status, accepted);

COMMENT ON TABLE quotes IS 'Price quotes from providers';


-- ============================================
-- TABLE: add_ons
-- ============================================

CREATE TABLE IF NOT EXISTS add_ons (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    reason_code VARCHAR(50) NOT NULL,
    reason_text TEXT,
    amount DECIMAL(10,2) NOT NULL,
    status addon_status DEFAULT 'PENDING',
    evidence_image TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    approved_at TIMESTAMP,
    rejected_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_addons_order ON add_ons(order_id);
CREATE INDEX IF NOT EXISTS idx_addons_status ON add_ons(status);

COMMENT ON TABLE add_ons IS 'Additional charges during service';


-- ============================================
-- TABLE: ratings
-- ============================================

CREATE TABLE IF NOT EXISTS ratings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID UNIQUE NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    provider_id UUID NOT NULL REFERENCES users(id),
    driver_id UUID NOT NULL REFERENCES users(id),
    rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ratings_provider ON ratings(provider_id);
CREATE INDEX IF NOT EXISTS idx_ratings_order ON ratings(order_id);

COMMENT ON TABLE ratings IS 'Provider ratings from drivers';


-- ============================================
-- TABLE: otps
-- ============================================

CREATE TABLE IF NOT EXISTS otps (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phone VARCHAR(20) NOT NULL,
    code VARCHAR(6) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_otps_phone ON otps(phone);
CREATE INDEX IF NOT EXISTS idx_otps_expires ON otps(expires_at);
CREATE INDEX IF NOT EXISTS idx_otps_phone_code ON otps(phone, code);

COMMENT ON TABLE otps IS 'OTP storage (fallback when Redis disabled)';


-- ============================================
-- FUNCTIONS
-- ============================================

-- Cleanup expired OTPs
CREATE OR REPLACE FUNCTION cleanup_expired_otps()
RETURNS void AS $$
BEGIN
    DELETE FROM otps WHERE expires_at < NOW();
    RAISE NOTICE 'Cleaned up expired OTPs';
END;
$$ LANGUAGE plpgsql;

-- Update timestamps automatically
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Triggers for updated_at
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_providers_updated_at BEFORE UPDATE ON providers
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_orders_updated_at BEFORE UPDATE ON orders
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================
-- VERIFY INSTALLATION
-- ============================================

-- Show all tables
DO $$
DECLARE
    table_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO table_count 
    FROM information_schema.tables 
    WHERE table_schema = 'public' 
    AND table_type = 'BASE TABLE';
    
    RAISE NOTICE '✅ Created % tables', table_count;
END $$;

-- Show extensions
SELECT extname, extversion FROM pg_extension WHERE extname IN ('uuid-ossp', 'postgis');

-- Show table sizes
SELECT 
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;