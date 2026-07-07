CREATE TABLE `alerts` (
	`id` int AUTO_INCREMENT NOT NULL,
	`parentId` int NOT NULL,
	`childId` int NOT NULL,
	`alertType` enum('limit_exceeded','blocked_app_attempt','consultant_response','system') NOT NULL,
	`title` varchar(255) NOT NULL,
	`message` longtext NOT NULL,
	`isRead` boolean NOT NULL DEFAULT false,
	`relatedData` longtext,
	`createdAt` timestamp NOT NULL DEFAULT (now()),
	CONSTRAINT `alerts_id` PRIMARY KEY(`id`)
);
--> statement-breakpoint
CREATE TABLE `app_time_limits` (
	`id` int AUTO_INCREMENT NOT NULL,
	`childId` int NOT NULL,
	`appName` varchar(255) NOT NULL,
	`packageName` varchar(255) NOT NULL,
	`limitMinutes` int NOT NULL DEFAULT 0,
	`isEnabled` boolean NOT NULL DEFAULT false,
	`createdAt` timestamp NOT NULL DEFAULT (now()),
	`updatedAt` timestamp NOT NULL DEFAULT (now()) ON UPDATE CURRENT_TIMESTAMP,
	CONSTRAINT `app_time_limits_id` PRIMARY KEY(`id`)
);
--> statement-breakpoint
CREATE TABLE `blocked_apps` (
	`id` int AUTO_INCREMENT NOT NULL,
	`childId` int NOT NULL,
	`appName` varchar(255) NOT NULL,
	`packageName` varchar(255) NOT NULL,
	`isBlocked` boolean NOT NULL DEFAULT true,
	`blockedAt` timestamp NOT NULL DEFAULT (now()),
	`updatedAt` timestamp NOT NULL DEFAULT (now()) ON UPDATE CURRENT_TIMESTAMP,
	CONSTRAINT `blocked_apps_id` PRIMARY KEY(`id`)
);
--> statement-breakpoint
CREATE TABLE `chat_messages` (
	`id` int AUTO_INCREMENT NOT NULL,
	`conversationId` varchar(255) NOT NULL,
	`senderId` int NOT NULL,
	`recipientId` int NOT NULL,
	`message` longtext NOT NULL,
	`messageType` enum('text','system') NOT NULL DEFAULT 'text',
	`isRead` boolean NOT NULL DEFAULT false,
	`createdAt` timestamp NOT NULL DEFAULT (now()),
	CONSTRAINT `chat_messages_id` PRIMARY KEY(`id`)
);
--> statement-breakpoint
CREATE TABLE `consultant_sessions` (
	`id` int AUTO_INCREMENT NOT NULL,
	`parentId` int NOT NULL,
	`consultantId` int NOT NULL,
	`topic` varchar(255),
	`status` enum('active','closed','pending') NOT NULL DEFAULT 'pending',
	`notes` longtext,
	`createdAt` timestamp NOT NULL DEFAULT (now()),
	`updatedAt` timestamp NOT NULL DEFAULT (now()) ON UPDATE CURRENT_TIMESTAMP,
	CONSTRAINT `consultant_sessions_id` PRIMARY KEY(`id`)
);
--> statement-breakpoint
CREATE TABLE `conversations` (
	`id` varchar(255) NOT NULL,
	`participantIds` varchar(1000) NOT NULL,
	`conversationType` enum('parent_child','parent_consultant','child_consultant') NOT NULL,
	`lastMessage` longtext,
	`lastMessageAt` timestamp,
	`createdAt` timestamp NOT NULL DEFAULT (now()),
	`updatedAt` timestamp NOT NULL DEFAULT (now()) ON UPDATE CURRENT_TIMESTAMP,
	CONSTRAINT `conversations_id` PRIMARY KEY(`id`)
);
--> statement-breakpoint
CREATE TABLE `parent_child_relationships` (
	`id` int AUTO_INCREMENT NOT NULL,
	`parentId` int NOT NULL,
	`childId` int NOT NULL,
	`nickname` varchar(255),
	`createdAt` timestamp NOT NULL DEFAULT (now()),
	CONSTRAINT `parent_child_relationships_id` PRIMARY KEY(`id`)
);
--> statement-breakpoint
CREATE TABLE `parenting_advice` (
	`id` int AUTO_INCREMENT NOT NULL,
	`title` varchar(255) NOT NULL,
	`content` longtext NOT NULL,
	`category` varchar(100) NOT NULL,
	`source` enum('ai_generated','curated') NOT NULL DEFAULT 'curated',
	`isActive` boolean NOT NULL DEFAULT true,
	`createdAt` timestamp NOT NULL DEFAULT (now()),
	`updatedAt` timestamp NOT NULL DEFAULT (now()) ON UPDATE CURRENT_TIMESTAMP,
	CONSTRAINT `parenting_advice_id` PRIMARY KEY(`id`)
);
--> statement-breakpoint
CREATE TABLE `usage_reports` (
	`id` int AUTO_INCREMENT NOT NULL,
	`childId` int NOT NULL,
	`reportDate` timestamp NOT NULL,
	`totalScreenTime` int NOT NULL DEFAULT 0,
	`appUsageData` longtext,
	`createdAt` timestamp NOT NULL DEFAULT (now()),
	CONSTRAINT `usage_reports_id` PRIMARY KEY(`id`)
);
--> statement-breakpoint
ALTER TABLE `users` MODIFY COLUMN `role` enum('parent','child','consultant') NOT NULL;--> statement-breakpoint
ALTER TABLE `users` ADD `profilePicture` text;--> statement-breakpoint
ALTER TABLE `users` ADD `bio` text;--> statement-breakpoint
ALTER TABLE `alerts` ADD CONSTRAINT `alerts_parentId_users_id_fk` FOREIGN KEY (`parentId`) REFERENCES `users`(`id`) ON DELETE no action ON UPDATE no action;--> statement-breakpoint
ALTER TABLE `alerts` ADD CONSTRAINT `alerts_childId_users_id_fk` FOREIGN KEY (`childId`) REFERENCES `users`(`id`) ON DELETE no action ON UPDATE no action;--> statement-breakpoint
ALTER TABLE `app_time_limits` ADD CONSTRAINT `app_time_limits_childId_users_id_fk` FOREIGN KEY (`childId`) REFERENCES `users`(`id`) ON DELETE no action ON UPDATE no action;--> statement-breakpoint
ALTER TABLE `blocked_apps` ADD CONSTRAINT `blocked_apps_childId_users_id_fk` FOREIGN KEY (`childId`) REFERENCES `users`(`id`) ON DELETE no action ON UPDATE no action;--> statement-breakpoint
ALTER TABLE `chat_messages` ADD CONSTRAINT `chat_messages_senderId_users_id_fk` FOREIGN KEY (`senderId`) REFERENCES `users`(`id`) ON DELETE no action ON UPDATE no action;--> statement-breakpoint
ALTER TABLE `chat_messages` ADD CONSTRAINT `chat_messages_recipientId_users_id_fk` FOREIGN KEY (`recipientId`) REFERENCES `users`(`id`) ON DELETE no action ON UPDATE no action;--> statement-breakpoint
ALTER TABLE `consultant_sessions` ADD CONSTRAINT `consultant_sessions_parentId_users_id_fk` FOREIGN KEY (`parentId`) REFERENCES `users`(`id`) ON DELETE no action ON UPDATE no action;--> statement-breakpoint
ALTER TABLE `consultant_sessions` ADD CONSTRAINT `consultant_sessions_consultantId_users_id_fk` FOREIGN KEY (`consultantId`) REFERENCES `users`(`id`) ON DELETE no action ON UPDATE no action;--> statement-breakpoint
ALTER TABLE `parent_child_relationships` ADD CONSTRAINT `parent_child_relationships_parentId_users_id_fk` FOREIGN KEY (`parentId`) REFERENCES `users`(`id`) ON DELETE no action ON UPDATE no action;--> statement-breakpoint
ALTER TABLE `parent_child_relationships` ADD CONSTRAINT `parent_child_relationships_childId_users_id_fk` FOREIGN KEY (`childId`) REFERENCES `users`(`id`) ON DELETE no action ON UPDATE no action;--> statement-breakpoint
ALTER TABLE `usage_reports` ADD CONSTRAINT `usage_reports_childId_users_id_fk` FOREIGN KEY (`childId`) REFERENCES `users`(`id`) ON DELETE no action ON UPDATE no action;