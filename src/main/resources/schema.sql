-- Example schema file
CREATE TABLE IF NOT EXISTS guilds (
    guild_name VARCHAR(100) NOT NULL,
    points INT NOT NULL DEFAULT 0,
    guild_type VARCHAR(50) NOT NULL,
    hq_world VARCHAR(50),
    hq_x DOUBLE,
    hq_y DOUBLE,
    hq_z DOUBLE,
    PRIMARY KEY (guild_name)
);

CREATE TABLE IF NOT EXISTS guild_members (
    guild_name VARCHAR(100) NOT NULL,
    player_uuid VARCHAR(36) NOT NULL,
    PRIMARY KEY (guild_name, player_uuid),
    FOREIGN KEY (guild_name) REFERENCES guilds(guild_name) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS guild_tasks (
    guild_name VARCHAR(100) NOT NULL,
    description VARCHAR(255) NOT NULL,
    material_or_mob VARCHAR(50) NOT NULL,
    required_amount INT NOT NULL,
    points_reward INT NOT NULL,
    money_reward DOUBLE NOT NULL,
    assigned_date DATE NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    PRIMARY KEY (guild_name, description),
    FOREIGN KEY (guild_name) REFERENCES guilds(guild_name) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS guild_task_progress (
    guild_name VARCHAR(100) NOT NULL,
    description VARCHAR(255) NOT NULL,
    player_uuid VARCHAR(36) NOT NULL,
    progress INT NOT NULL,
    completed_at TIMESTAMP NULL DEFAULT NULL,
    claimed_at TIMESTAMP NULL DEFAULT NULL,
    PRIMARY KEY (guild_name, description, player_uuid),
    FOREIGN KEY (guild_name,description) REFERENCES guild_tasks(guild_name, description) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS task_resets (
    reset_type VARCHAR(20) NOT NULL,        -- e.g. 'DAILY' or 'MONTHLY'
    last_reset TIMESTAMP NOT NULL,
    PRIMARY KEY (reset_type)
);

CREATE TABLE IF NOT EXISTS guild_monthly_tasks (
    guild_name VARCHAR(100) NOT NULL,
    description VARCHAR(255) NOT NULL,
    material_or_mob VARCHAR(50) NOT NULL,
    required_amount INT NOT NULL,
    points_reward INT NOT NULL,
    money_reward DOUBLE NOT NULL,
    assigned_date DATE NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    PRIMARY KEY (guild_name, description),
    FOREIGN KEY (guild_name) REFERENCES guilds(guild_name) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS guild_monthly_progress (
    guild_name VARCHAR(100) NOT NULL,
    description VARCHAR(255) NOT NULL,
    progress INT NOT NULL DEFAULT 0,
    completed_at TIMESTAMP NULL DEFAULT NULL,
    PRIMARY KEY (guild_name, description),
    FOREIGN KEY (guild_name, description) REFERENCES guild_monthly_tasks(guild_name, description) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS guild_monthly_claim (
    guild_name VARCHAR(100) NOT NULL,
    description VARCHAR(255) NOT NULL,
    player_uuid VARCHAR(36) NOT NULL,
    claimed_at TIMESTAMP NULL DEFAULT NULL,
    PRIMARY KEY (guild_name, description, player_uuid),
    FOREIGN KEY (guild_name, description) REFERENCES guild_monthly_tasks(guild_name, description) ON DELETE CASCADE
);
