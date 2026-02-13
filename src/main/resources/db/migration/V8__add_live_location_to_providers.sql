-- Add live location tracking field (fallback when Redis down)
ALTER TABLE providers 
ADD COLUMN live_location GEOGRAPHY(Point, 4326);

-- Timestamp of last live location update
ALTER TABLE providers 
ADD COLUMN live_location_updated_at TIMESTAMP;

-- Spatial index for performance
CREATE INDEX idx_providers_live_location 
ON providers USING GIST(live_location);

-- Regular index for timestamp filtering
CREATE INDEX idx_providers_live_location_updated 
ON providers(live_location_updated_at);

-- Comments
COMMENT ON COLUMN providers.base_location IS 'Fixed garage/office location (permanent)';
COMMENT ON COLUMN providers.live_location IS 'Real-time location when EN_ROUTE (fallback for Redis)';
COMMENT ON COLUMN providers.live_location_updated_at IS 'Timestamp of last live location update';