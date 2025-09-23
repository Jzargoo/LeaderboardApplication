CREATE TABLE leaderboards_events (
    id SERIAL PRIMARY KEY
);

CREATE TABLE leaderboards_events_mapping (
    id SERIAL PRIMARY KEY,
    leaderboard_event_id INT NOT NULL,
    scoring_event_id INT NOT NULL,
    FOREIGN KEY (leaderboard_event_id) REFERENCES leaderboards_events(id) ON DELETE CASCADE,
    FOREIGN KEY (scoring_event_id) REFERENCES scoring_event(id) ON DELETE CASCADE
);

CREATE OR REPLACE FUNCTION delete_orphan_scoring_event()
RETURNS TRIGGER AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM leaderboards_events_mapping
        WHERE scoring_event_id = OLD.scoring_event_id
    ) THEN
DELETE FROM scoring_event WHERE id = OLD.scoring_event_id;
END IF;
RETURN OLD;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_delete_orphan_scoring
    AFTER DELETE ON leaderboards_events_mapping
    FOR EACH ROW
    EXECUTE FUNCTION delete_orphan_scoring_event();