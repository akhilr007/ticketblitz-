-- user service database
CREATE DATABASE ticketblitz_users;

-- catalog service database
CREATE DATABASE ticketblitz_catalog;

-- booking service database
CREATE DATABASE ticketblitz_booking;

-- fulfillment service database
CREATE DATABASE ticketblitz_fulfillment;

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE ticketblitz TO ticketblitz;
GRANT ALL PRIVILEGES ON DATABASE ticketblitz_catalog TO ticketblitz;
GRANT ALL PRIVILEGES ON DATABASE ticketblitz_booking TO ticketblitz;
GRANT ALL PRIVILEGES ON DATABASE ticketblitz_fulfillment TO ticketblitz;
GRANT ALL PRIVILEGES ON DATABASE ticketblitz TO ticketblitz_users;