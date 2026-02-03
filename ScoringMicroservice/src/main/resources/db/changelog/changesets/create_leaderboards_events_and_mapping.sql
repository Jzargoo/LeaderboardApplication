CREATE TABLE IF NOT EXISTS leaderboards_events (
    id SERIAL PRIMARY KEY,
    is_public BOOLEAN NOT NULL DEFAULT TRUE,
    metadata varchar(1024)
);

CREATE TABLE IF NOT EXISTS leaderboards_events_mapping (
    id SERIAL PRIMARY KEY,
    leaderboard_event_id INT NOT NULL,
    lb_events_type_id INT NOT NULL,
    FOREIGN KEY (leaderboard_event_id) REFERENCES leaderboards_events(id) ON DELETE CASCADE,
    FOREIGN KEY (lb_events_type_id) REFERENCES lb_events_type(id) ON DELETE CASCADE
);

CREATE OR REPLACE FUNCTION delete_orphan_type_event()
    RETURNS TRIGGER AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM leaderboards_events_mapping
        WHERE lb_events_type_id = OLD.lb_events_type_id
    ) THEN
        DELETE FROM lb_events_type WHERE id = OLD.lb_events_type_id;
    END IF;
    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_delete_orphan_scoring
    AFTER DELETE ON leaderboards_events_mapping
    FOR EACH ROW
EXECUTE FUNCTION delete_orphan_type_event();
