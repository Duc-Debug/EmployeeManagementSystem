-- Backfill allocation changes written after the original notification-center migration.
-- Preserve read state and delayed delivery, and retain recipients already migrated.
INSERT INTO notification_events (event_type, level, title, message, related_entity_type,
                                 related_entity_id, source_event_key, created_at)
SELECT n.type, 'TRUNG_BINH', n.title, COALESCE(n.content, ''), 'PROJECT_ALLOCATION',
       CAST(n.target_id AS CHAR), CONCAT('LEGACY:NOTIF:', n.id), n.created_at
FROM notifications n
WHERE n.type = 'ALLOCATION_CHANGED'
  AND NOT EXISTS (SELECT 1 FROM notification_events e WHERE e.source_event_key = CONCAT('LEGACY:NOTIF:', n.id));

INSERT INTO notification_recipients (notification_event_id, recipient_user_id, is_read, read_at,
                                     is_deleted, deleted_at, created_at, available_at)
SELECT e.id, n.recipient_id, n.is_read, CASE WHEN n.is_read = TRUE THEN n.created_at ELSE NULL END,
       FALSE, NULL, n.created_at, COALESCE(n.available_at, n.created_at)
FROM notifications n
JOIN notification_events e ON e.source_event_key = CONCAT('LEGACY:NOTIF:', n.id)
WHERE n.type = 'ALLOCATION_CHANGED'
  AND NOT EXISTS (SELECT 1 FROM notification_recipients r
                  WHERE r.notification_event_id = e.id AND r.recipient_user_id = n.recipient_id);
