-- Migration V10: Fix subscription_plan column length in restaurants table
ALTER TABLE restaurants MODIFY COLUMN subscription_plan VARCHAR(50) NOT NULL DEFAULT 'STARTER';
