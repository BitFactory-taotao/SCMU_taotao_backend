-- Add media-related columns for chat message compatibility (minimal intrusive change)
ALTER TABLE chat_message
    ADD COLUMN content_type VARCHAR(16) NULL COMMENT 'TEXT/IMAGE/AUDIO' AFTER msg_content,
    ADD COLUMN media_url VARCHAR(512) NULL COMMENT 'media resource url' AFTER content_type,
    ADD COLUMN media_name VARCHAR(255) NULL COMMENT 'media file name' AFTER media_url,
    ADD COLUMN media_size BIGINT NULL COMMENT 'media file size in bytes' AFTER media_name,
    ADD COLUMN media_duration INT NULL COMMENT 'media duration in seconds (audio)' AFTER media_size;

