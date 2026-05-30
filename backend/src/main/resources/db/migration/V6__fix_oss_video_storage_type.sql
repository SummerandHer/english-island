-- 修复：有 play_url 且无 embed_bvid 的自托管视频被误标为 embed
UPDATE `video`
SET `storage_type` = 'oss', `provider` = 'self'
WHERE `play_url` IS NOT NULL
  AND `play_url` != ''
  AND (`embed_bvid` IS NULL OR `embed_bvid` = '');
