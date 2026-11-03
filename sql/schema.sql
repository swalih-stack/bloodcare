-- Reference copy of the schema. The app also creates these automatically (Db.init),
-- so running this file by hand is optional.
CREATE DATABASE IF NOT EXISTS bloodcare;
USE bloodcare;

CREATE TABLE IF NOT EXISTS users (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(120) NOT NULL UNIQUE,
    phone         VARCHAR(20),
    blood_group   VARCHAR(3)   NOT NULL,
    city          VARCHAR(60),
    password_hash VARCHAR(200) NOT NULL,          -- "salt:sha256(salt+password)"
    role          VARCHAR(10)  NOT NULL DEFAULT 'USER',   -- USER or ADMIN
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS blood_requests (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    user_id      INT NOT NULL,
    patient_name VARCHAR(100) NOT NULL,
    blood_group  VARCHAR(3)   NOT NULL,
    units        INT NOT NULL,
    hospital     VARCHAR(120) NOT NULL,
    city         VARCHAR(60)  NOT NULL,
    urgency      VARCHAR(10)  NOT NULL DEFAULT 'NORMAL',  -- NORMAL or EMERGENCY
    status       VARCHAR(12)  NOT NULL DEFAULT 'PENDING', -- PENDING, APPROVED, FULFILLED, REJECTED, CANCELLED
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS donations (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    user_id       INT NOT NULL,
    blood_group   VARCHAR(3) NOT NULL,
    units         INT NOT NULL,
    donation_date DATE NOT NULL,
    donor_age     INT NOT NULL DEFAULT 0,     -- used by eligibility verification
    weight_kg     INT NOT NULL DEFAULT 0,
    status        VARCHAR(10) NOT NULL DEFAULT 'PENDING', -- PENDING, APPROVED, REJECTED
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS blood_banks (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(120) NOT NULL,
    city       VARCHAR(60)  NOT NULL,
    phone      VARCHAR(20),
    address    VARCHAR(200),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Stock ledger: units > 0 added (approved donation / admin), units < 0 removed (fulfilled request).
-- Stock of a blood group = SUM(units).
CREATE TABLE IF NOT EXISTS blood_inventory (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    bank_id     INT NULL,
    blood_group VARCHAR(3) NOT NULL,
    units       INT NOT NULL,
    note        VARCHAR(160),
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (bank_id) REFERENCES blood_banks(id) ON DELETE SET NULL
);

-- a donor's answer to an approved request (one answer per donor per request)
CREATE TABLE IF NOT EXISTS request_responses (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    request_id INT NOT NULL,
    donor_id   INT NOT NULL,
    status     VARCHAR(10) NOT NULL,           -- ACCEPTED or DECLINED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY one_answer (request_id, donor_id),
    FOREIGN KEY (request_id) REFERENCES blood_requests(id) ON DELETE CASCADE,
    FOREIGN KEY (donor_id) REFERENCES users(id) ON DELETE CASCADE
);

-- chat between the requester and an accepted donor (the conversation is the response row)
CREATE TABLE IF NOT EXISTS chat_messages (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    response_id INT NOT NULL,
    sender_id   INT NOT NULL,
    body        VARCHAR(500) NOT NULL,
    is_read     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (response_id) REFERENCES request_responses(id) ON DELETE CASCADE,
    FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE
);

-- messages a user writes to the admin
CREATE TABLE IF NOT EXISTS messages (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    user_id    INT NOT NULL,
    body       VARCHAR(500) NOT NULL,
    is_read    BOOLEAN NOT NULL DEFAULT FALSE,
    is_admin_reply BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- user_id NULL = admin inbox
CREATE TABLE IF NOT EXISTS notifications (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    user_id    INT NULL,
    message    VARCHAR(300) NOT NULL,
    is_read    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
