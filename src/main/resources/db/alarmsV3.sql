-- Step 2: Add new columns to actions
ALTER TABLE actions
ADD alarm_type_id BIGINT NULL,
    alarm_class_id BIGINT NULL;

-- Step 3: Add foreign key constraints to actions
ALTER TABLE actions
ADD CONSTRAINT fk_alarm_type_actions
    FOREIGN KEY (alarm_type_id) REFERENCES alarm_type(id);

ALTER TABLE actions
ADD CONSTRAINT fk_alarm_class_actions
    FOREIGN KEY (alarm_class_id) REFERENCES alarm_class(id);

-- Step 4: Add new columns to alarms
ALTER TABLE alarms
ADD status NVARCHAR(100) NULL,
    archived BIT NULL,
    created_from NVARCHAR(255) NULL,
    metadata NVARCHAR(MAX) NULL,
    relation NVARCHAR(255) NULL,
    updated_at DATETIME2 NULL;

