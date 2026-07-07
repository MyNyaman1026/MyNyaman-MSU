-- Safe migration: Only update the role enum without dropping tables
ALTER TABLE `users` MODIFY COLUMN `role` enum('user','admin','parent','child','consultant') NOT NULL DEFAULT 'user';
