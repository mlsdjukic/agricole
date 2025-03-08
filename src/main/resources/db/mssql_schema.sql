-- SQL Server Schema for Alarms Application

-- Create the database
IF NOT EXISTS (SELECT * FROM sys.databases WHERE name = 'alarms')
BEGIN
    CREATE DATABASE [alarms];
END
GO

USE [alarms];
GO

-- Create accounts table
CREATE TABLE accounts (
    id bigint IDENTITY(1,1) NOT NULL,
    username nvarchar(50) NOT NULL,
    password nvarchar(1000) NOT NULL,
    CONSTRAINT accounts_pkey PRIMARY KEY (id)
);

-- Create actions table
CREATE TABLE actions (
    id bigint IDENTITY(1,1) NOT NULL,
    type nvarchar(50) NOT NULL,
    params nvarchar(max) NOT NULL,
    user_id bigint NOT NULL,
    created_date datetime2 NOT NULL DEFAULT GETDATE(),
    last_modified_date datetime2 NOT NULL DEFAULT GETDATE(),
    CONSTRAINT actions_pkey PRIMARY KEY (id)
);

-- Create reservations table
CREATE TABLE reservations (
    id bigint IDENTITY(1,1) NOT NULL,
    status nvarchar(20) NOT NULL,
    locked_by nvarchar(36) NULL,
    action_id bigint NOT NULL,
    created_date datetime2 NOT NULL DEFAULT GETDATE(),
    locked_at datetime2 NULL,
    CONSTRAINT reservations_pkey PRIMARY KEY (id)
);

-- Create rules table
CREATE TABLE rules (
    id bigint IDENTITY(1,1) NOT NULL,
    action_id bigint NOT NULL,
    name nvarchar(100) NOT NULL,
    definition nvarchar(max) NOT NULL,
    created_date datetime2 NOT NULL DEFAULT GETDATE(),
    last_modified_date datetime2 NOT NULL DEFAULT GETDATE(),
    CONSTRAINT rules_pkey PRIMARY KEY (id),
    CONSTRAINT fk_actions_rule_actions_id FOREIGN KEY (action_id)
        REFERENCES actions (id)
);

-- Create alarms table
CREATE TABLE alarms (
    id bigint IDENTITY(1,1) NOT NULL,
    rule_id bigint NULL,
    message nvarchar(max) NOT NULL,
    created_date datetime2 NOT NULL DEFAULT GETDATE(),
    CONSTRAINT alarms_pkey PRIMARY KEY (id)
);

-- Create reactions table
CREATE TABLE reactions (
    id bigint IDENTITY(1,1) NOT NULL,
    rule_id bigint NOT NULL,
    name nvarchar(max) NOT NULL,
    params nvarchar(max) NOT NULL,
    created_date datetime2 NOT NULL DEFAULT GETDATE(),
    last_modified_date datetime2 NOT NULL DEFAULT GETDATE(),
    CONSTRAINT reactions_pkey PRIMARY KEY (id),
    CONSTRAINT fk_rule_alarm_rule_id FOREIGN KEY (rule_id)
        REFERENCES rules (id)
);