CREATE TABLE actions (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(255),
    params JSONB
);
