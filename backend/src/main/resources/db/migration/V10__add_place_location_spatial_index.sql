-- Add location column as a STORED Generated Column pointing to POINT coordinates
-- MySQL Point syntax uses POINT(longitude latitude)
-- SRID 4326 defaults to latitude/longitude axis order in MySQL 8, so force long-lat for WKT.
-- MySQL SPATIAL indexes require indexed columns to be NOT NULL.
ALTER TABLE places ADD COLUMN location POINT AS (ST_PointFromText(CONCAT('POINT(', longitude, ' ', latitude, ')'), 4326, 'axis-order=long-lat')) STORED NOT NULL;

-- Create Spatial Index on the new location column
ALTER TABLE places ADD SPATIAL INDEX idx_places_location (location);
