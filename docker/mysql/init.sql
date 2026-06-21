-- CropDeal — create all service databases
CREATE DATABASE IF NOT EXISTS cropdeal_users     CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS cropdeal_crops     CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS cropdeal_orders    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS cropdeal_payments  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS cropdeal_admin     CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS cropdeal_notifications CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Grant the app user access to all databases
GRANT ALL PRIVILEGES ON cropdeal_users.*         TO 'cropdeal'@'%';
GRANT ALL PRIVILEGES ON cropdeal_crops.*         TO 'cropdeal'@'%';
GRANT ALL PRIVILEGES ON cropdeal_orders.*        TO 'cropdeal'@'%';
GRANT ALL PRIVILEGES ON cropdeal_payments.*      TO 'cropdeal'@'%';
GRANT ALL PRIVILEGES ON cropdeal_admin.*         TO 'cropdeal'@'%';
GRANT ALL PRIVILEGES ON cropdeal_notifications.* TO 'cropdeal'@'%';

FLUSH PRIVILEGES;
