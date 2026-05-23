-- Add location column as a STORED Generated Column pointing to POINT coordinates
-- MySQL Point syntax uses POINT(longitude latitude)
ALTER TABLE places ADD COLUMN location POINT AS (ST_PointFromText(CONCAT('POINT(', longitude, ' ', latitude, ')'), 4326)) STORED;

-- Create Spatial Index on the new location column
ALTER TABLE places ADD SPATIAL INDEX idx_places_location (location);
