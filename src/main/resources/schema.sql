-- 1) Guilds
CREATE TABLE IF NOT EXISTS guilds
(
    guild_name VARCHAR(100) NOT NULL,
    points     INT          NOT NULL DEFAULT 0,
    guild_type VARCHAR(50)  NOT NULL,
    hq_world   VARCHAR(50),
    hq_x       DOUBLE,
    hq_y       DOUBLE,
    hq_z       DOUBLE,
    PRIMARY KEY (guild_name)
);

-- 2) Guild Members
CREATE TABLE IF NOT EXISTS guild_members
(
    guild_name         VARCHAR(100) NOT NULL,
    player_uuid        VARCHAR(36)  NOT NULL,
    contributed_points INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (guild_name, player_uuid),
    FOREIGN KEY (guild_name) REFERENCES guilds (guild_name) ON DELETE CASCADE
);

-- 3) Unified Guild Tasks (both daily & monthly)
CREATE TABLE IF NOT EXISTS guild_tasks
(
    task_id         INT AUTO_INCREMENT PRIMARY KEY,
    guild_name      VARCHAR(100) NOT NULL,
    periodicity     VARCHAR(10)  NOT NULL, -- e.g., 'DAILY' or 'MONTHLY'
    description     VARCHAR(255) NOT NULL,
    material_or_mob VARCHAR(50)  NOT NULL,
    required_amount INT          NOT NULL,
    points_reward   INT          NOT NULL,
    money_reward    DOUBLE       NOT NULL,
    assigned_date   DATE         NOT NULL,
    event_type      VARCHAR(50)  NOT NULL,
    FOREIGN KEY (guild_name) REFERENCES guilds (guild_name) ON DELETE CASCADE
);

-- 4) Guild Task Progress (per-player)
CREATE TABLE IF NOT EXISTS guild_task_progress
(
    task_id      INT         NOT NULL,
    player_uuid  VARCHAR(36) NOT NULL,
    progress     INT         NOT NULL DEFAULT 0,
    completed_at TIMESTAMP   NULL     DEFAULT NULL,
    claimed_at   TIMESTAMP   NULL     DEFAULT NULL,
    PRIMARY KEY (task_id, player_uuid),
    FOREIGN KEY (task_id) REFERENCES guild_tasks (task_id) ON DELETE CASCADE
);

-- 5) Task Resets (tracks last reset for daily or monthly)
CREATE TABLE IF NOT EXISTS task_resets
(
    reset_type VARCHAR(20) NOT NULL, -- e.g. 'DAILY' or 'MONTHLY'
    last_reset TIMESTAMP   NOT NULL,
    PRIMARY KEY (reset_type)
);
