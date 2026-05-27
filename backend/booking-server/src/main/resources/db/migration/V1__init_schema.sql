-- ============================================================
-- Flyway Migration V1 — Init Schema
-- WeChat Room Booking System
-- ============================================================

CREATE EXTENSION IF NOT EXISTS btree_gist;

-- -----------------------------------------------------------
-- 1. organization
-- -----------------------------------------------------------
CREATE TABLE organization (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(200)  NOT NULL,
    logo_url    VARCHAR(500),
    contact     VARCHAR(100),
    phone       VARCHAR(30),
    address     TEXT,
    status      VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED')),
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT now()
);

-- -----------------------------------------------------------
-- 2. venue
-- -----------------------------------------------------------
CREATE TABLE venue (
    id              BIGSERIAL PRIMARY KEY,
    organization_id BIGINT        NOT NULL REFERENCES organization(id),
    name            VARCHAR(200)  NOT NULL,
    address         TEXT          NOT NULL,
    contact_person  VARCHAR(100),
    phone           VARCHAR(30),
    description     TEXT,
    images_json     JSONB,
    latitude        DOUBLE PRECISION,
    longitude       DOUBLE PRECISION,
    status          VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'MAINTENANCE')),
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT now()
);

-- -----------------------------------------------------------
-- 3. room
-- -----------------------------------------------------------
CREATE TABLE room (
    id              BIGSERIAL PRIMARY KEY,
    venue_id        BIGINT        NOT NULL REFERENCES venue(id),
    name            VARCHAR(200)  NOT NULL,
    description     TEXT,
    capacity        INT           NOT NULL DEFAULT 0,
    area_sqm        NUMERIC(8,2),
    floor           INT,
    price_per_hour  NUMERIC(10,2) NOT NULL DEFAULT 0,
    price_per_halfday NUMERIC(10,2),
    price_per_day   NUMERIC(10,2),
    status          VARCHAR(20)   NOT NULL DEFAULT 'AVAILABLE'
        CHECK (status IN ('AVAILABLE', 'MAINTENANCE', 'OFFLINE')),
    sort_order      INT           NOT NULL DEFAULT 0,
    version         INT           NOT NULL DEFAULT 1,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT now()
);

-- -----------------------------------------------------------
-- 4. amenity
-- -----------------------------------------------------------
CREATE TABLE amenity (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(100)  NOT NULL,
    icon       VARCHAR(100),
    category   VARCHAR(50),
    sort_order INT           NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ   NOT NULL DEFAULT now()
);

-- -----------------------------------------------------------
-- 5. room_amenity (junction)
-- -----------------------------------------------------------
CREATE TABLE room_amenity (
    id         BIGSERIAL PRIMARY KEY,
    room_id    BIGINT NOT NULL REFERENCES room(id) ON DELETE CASCADE,
    amenity_id BIGINT NOT NULL REFERENCES amenity(id) ON DELETE CASCADE,
    UNIQUE (room_id, amenity_id)
);

-- -----------------------------------------------------------
-- 6. room_image
-- -----------------------------------------------------------
CREATE TABLE room_image (
    id          BIGSERIAL PRIMARY KEY,
    room_id     BIGINT       NOT NULL REFERENCES room(id) ON DELETE CASCADE,
    url         VARCHAR(500) NOT NULL,
    sort_order  INT          NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- -----------------------------------------------------------
-- 7. "user" (quoted — user is a PostgreSQL reserved word)
-- -----------------------------------------------------------
CREATE TABLE "user" (
    id          BIGSERIAL PRIMARY KEY,
    openid      VARCHAR(100)  NOT NULL,
    unionid     VARCHAR(100),
    nickname    VARCHAR(100),
    avatar_url  VARCHAR(500),
    phone       VARCHAR(30),
    email       VARCHAR(200),
    status      VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'BLACKLISTED', 'INACTIVE')),
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT now()
);

-- -----------------------------------------------------------
-- 8. booking
-- -----------------------------------------------------------
CREATE TABLE booking (
    id            BIGSERIAL PRIMARY KEY,
    booking_no    VARCHAR(50)   NOT NULL,
    user_id       BIGINT        NOT NULL REFERENCES "user"(id),
    room_id       BIGINT        NOT NULL REFERENCES room(id),
    booking_date  DATE          NOT NULL,
    start_time    TIME          NOT NULL,
    end_time      TIME          NOT NULL,
    time_slot     TSRANGE       NOT NULL,
    title         VARCHAR(300),
    purpose       TEXT,
    attendee_count INT,
    status        VARCHAR(20)   NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'CONFIRMED', 'CHECKED_IN', 'COMPLETED',
                          'CANCELLED', 'EXPIRED', 'REJECTED')),
    remark        TEXT,
    version       INT           NOT NULL DEFAULT 1,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT check_time_order CHECK (start_time < end_time)
);

-- ============================================================
-- CRITICAL: Exclusion constraint prevents overlapping bookings
-- for the same room, excluding cancelled / expired bookings.
-- ============================================================
ALTER TABLE booking
    ADD CONSTRAINT excl_booking_no_overlap
    EXCLUDE USING GIST (room_id WITH =, time_slot WITH &&)
    WHERE (status NOT IN ('CANCELLED', 'EXPIRED'));

-- -----------------------------------------------------------
-- 9. recurring_rule
-- -----------------------------------------------------------
CREATE TABLE recurring_rule (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT       NOT NULL REFERENCES "user"(id),
    room_id       BIGINT       NOT NULL REFERENCES room(id),
    title         VARCHAR(300),
    purpose       TEXT,
    attendee_count INT,
    start_date    DATE         NOT NULL,
    end_date      DATE,
    start_time    TIME         NOT NULL,
    end_time      TIME         NOT NULL,
    repeat_type   VARCHAR(20)  NOT NULL DEFAULT 'WEEKLY'
        CHECK (repeat_type IN ('DAILY', 'WEEKLY', 'BIWEEKLY', 'MONTHLY')),
    repeat_days   INT[],  -- ISO day numbers: 1=Mon … 7=Sun
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'PAUSED', 'STOPPED')),
    remark        TEXT,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- -----------------------------------------------------------
-- 10. booking_check_in
-- -----------------------------------------------------------
CREATE TABLE booking_check_in (
    id            BIGSERIAL PRIMARY KEY,
    booking_id    BIGINT       NOT NULL REFERENCES booking(id),
    check_in_time TIMESTAMPTZ  NOT NULL DEFAULT now(),
    check_out_time TIMESTAMPTZ,
    operator_id   BIGINT       REFERENCES "user"(id),
    remark        TEXT
);

-- -----------------------------------------------------------
-- 11. availability_rule
-- -----------------------------------------------------------
CREATE TABLE availability_rule (
    id            BIGSERIAL PRIMARY KEY,
    room_id       BIGINT       NOT NULL REFERENCES room(id) ON DELETE CASCADE,
    day_of_week   INT          NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    open_time     TIME         NOT NULL,
    close_time    TIME         NOT NULL,
    is_active     BOOLEAN      NOT NULL DEFAULT true,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT check_rule_time_order CHECK (open_time < close_time),
    UNIQUE (room_id, day_of_week)
);

-- -----------------------------------------------------------
-- 12. blocked_time_slot
-- -----------------------------------------------------------
CREATE TABLE blocked_time_slot (
    id            BIGSERIAL PRIMARY KEY,
    room_id       BIGINT       REFERENCES room(id) ON DELETE CASCADE,
    venue_id      BIGINT       REFERENCES venue(id) ON DELETE CASCADE,
    title         VARCHAR(300),
    reason        TEXT,
    time_slot     TSRANGE      NOT NULL,
    blocked_date  DATE,
    start_time    TIME,
    end_time      TIME,
    created_by    BIGINT       REFERENCES "user"(id),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT check_blocked_time_order
        CHECK (start_time IS NULL OR end_time IS NULL OR start_time < end_time),
    CONSTRAINT check_blocked_target
        CHECK (room_id IS NOT NULL OR venue_id IS NOT NULL)
);

-- ============================================================
-- Indexes
-- ============================================================

-- organization
CREATE INDEX idx_org_status ON organization(status);

-- venue
CREATE INDEX idx_venue_org    ON venue(organization_id);
CREATE INDEX idx_venue_status ON venue(status);

-- room
CREATE INDEX idx_room_venue    ON room(venue_id);
CREATE INDEX idx_room_status   ON room(status);
CREATE INDEX idx_room_capacity ON room(capacity);

-- room_image
CREATE INDEX idx_room_image_room ON room_image(room_id);

-- user
CREATE UNIQUE INDEX idx_user_openid  ON "user"(openid);
CREATE INDEX        idx_user_unionid ON "user"(unionid);

-- booking
CREATE INDEX idx_booking_status_date ON booking(status, booking_date);
CREATE INDEX idx_booking_user        ON booking(user_id);
CREATE INDEX idx_booking_room_date   ON booking(room_id, booking_date);
CREATE UNIQUE INDEX idx_booking_no   ON booking(booking_no);

-- recurring_rule
CREATE INDEX idx_rrule_user   ON recurring_rule(user_id);
CREATE INDEX idx_rrule_status ON recurring_rule(status);

-- availability_rule
CREATE INDEX idx_avail_rule_room ON availability_rule(room_id);

-- blocked_time_slot
CREATE INDEX idx_blocked_room     ON blocked_time_slot(room_id);
CREATE INDEX idx_blocked_venue    ON blocked_time_slot(venue_id);
CREATE INDEX idx_blocked_time_slot ON blocked_time_slot USING GIST(time_slot);
