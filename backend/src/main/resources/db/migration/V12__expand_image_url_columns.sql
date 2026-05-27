ALTER TABLE place_images
    MODIFY image_url VARCHAR(2048) NOT NULL;

ALTER TABLE visits
    MODIFY image_url VARCHAR(2048);

ALTER TABLE users
    MODIFY profile_image_url VARCHAR(2048);
