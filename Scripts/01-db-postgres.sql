CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

CREATE OR REPLACE FUNCTION update_updated_at_column()
    RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TYPE user_type_enum AS ENUM ('GRADUATE', 'ADMIN', 'VERIFIER', 'DIRECTOR');
CREATE TYPE participation_level_enum AS ENUM ('ATTENDEE', 'SPEAKER', 'VOTER');
CREATE TYPE funding_enum AS ENUM ('SCHOLARSHIP', 'SELF_FUNDED');
CREATE TYPE survey_status_enum AS ENUM ('DRAFT', 'ACTIVE', 'CLOSED', 'ARCHIVED');
CREATE TYPE response_status_enum AS ENUM ('IN_PROGRESS', 'COMPLETED');
CREATE TYPE assignment_type_enum AS ENUM ('ALL', 'CAREER', 'GRADUATION_YEAR', 'USER_TYPE', 'SPECIFIC_USERS');
CREATE TYPE conversation_type_enum AS ENUM ('PRIVATE', 'GROUP');
CREATE TYPE notification_type_enum AS ENUM ('NEW_SURVEY', 'REMINDER', 'CLOSING_SOON');
CREATE TYPE notification_medium_enum AS ENUM ('EMAIL', 'PUSH', 'BOTH');
CREATE TYPE event_type_enum AS ENUM ('VIRTUAL', 'PHYSICAL');
CREATE TYPE event_status_enum AS ENUM ('SCHEDULED', 'CANCELLED', 'COMPLETED');
CREATE TYPE reaction_type_enum AS ENUM ('LIKE', 'CELEBRATE', 'LOVE');
CREATE TYPE disability_type_enum AS ENUM ('PHYSICAL', 'VISUAL', 'AUDITORY', 'INTELLECTUAL', 'PSYCHOSOCIAL', 'MULTIPLE', 'OTHER');
CREATE TYPE certification_type_enum AS ENUM ('COURSE', 'CERTIFICATION', 'DIPLOMA', 'WORKSHOP');
CREATE TYPE connection_status_enum AS ENUM ('PENDING', 'ACCEPTED', 'REJECTED', 'BLOCKED');
CREATE TYPE general_notification_type_enum AS ENUM ('CONNECTION_REQUEST', 'CONNECTION_ACCEPTED', 'POST_REACTION', 'POST_COMMENT', 'MESSAGE', 'EVENT_INVITATION', 'JOB_POSTED', 'ENDORSEMENT_RECEIVED', 'RECOMMENDATION_RECEIVED', 'MENTION', 'PROFILE_VIEW', 'NEW_FOLLOWER', 'EVENT_REMINDER');
CREATE TYPE report_type_enum AS ENUM ('SPAM', 'HARASSMENT', 'INAPPROPRIATE', 'FALSE_INFO', 'OTHER');
CREATE TYPE report_status_enum AS ENUM ('PENDING', 'REVIEWED', 'ACTION_TAKEN', 'DISMISSED');
CREATE TYPE audit_action_enum AS ENUM ('INSERT', 'UPDATE', 'DELETE', 'LOGIN_SUCCESS', 'LOGIN_SUCCESS_MFA', 'LOGIN_FAILED', 'LOGOUT', 'PASSWORD_CHANGE', 'PASSWORD_CHANGE_FAILED', 'PASSWORD_RESET_REQUESTED', 'MFA_ENABLED', 'MFA_SETUP_INITIATED', 'MFA_VERIFICATION_FAILED', 'MFA_DISABLED', 'ADMIN_MFA_DISABLE', 'ADMIN_ROLE_CHANGE', 'VERIFICATION_INITIATED', 'VERIFICATION_APPROVED', 'VERIFICATION_REJECTED', 'PROFILE_IMAGE_UPDATED', 'PROFILE_UPDATED', 'ACCOUNT_DEACTIVATED', 'SURVEY_CREATED', 'SURVEY_PUBLISHED', 'SURVEY_CLOSED', 'SURVEY_RESPONDED', 'SURVEY_ASSIGNED', 'SURVEY_MODIFIED', 'CONNECTION_REQUEST', 'CONNECTION_ACCEPTED', 'POST_CREATED', 'POST_DELETED', 'CONTENT_REPORTED', 'REPORT_REVIEWED');

CREATE TABLE countries (
                           id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                           name VARCHAR(100) UNIQUE NOT NULL,
                           iso_code CHAR(2) UNIQUE,
                           created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_countries_name ON countries(name);

CREATE TABLE universities (
                              id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                              name VARCHAR(150) NOT NULL,
                              country_id BIGINT NOT NULL REFERENCES countries(id) ON DELETE RESTRICT,
                              website VARCHAR(255),
                              created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_universities_country ON universities(country_id);
CREATE INDEX idx_universities_name ON universities USING gin(name gin_trgm_ops);

CREATE TABLE careers (
                         id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                         name VARCHAR(150) NOT NULL,
                         university_id BIGINT NOT NULL REFERENCES universities(id) ON DELETE RESTRICT,
                         code VARCHAR(20),
                         created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_careers_university ON careers(university_id);
CREATE INDEX idx_careers_name ON careers USING gin(name gin_trgm_ops);

CREATE TABLE graduates (
                           id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                           student_id VARCHAR(20) UNIQUE,
                           first_name VARCHAR(100) NOT NULL,
                           last_name VARCHAR(100) NOT NULL,
                           identity_document VARCHAR(20) UNIQUE,
                           admission_year INT,
                           graduation_year INT,
                           total_years INT,
                           gpa NUMERIC(3,2),
                           career_id BIGINT REFERENCES careers(id) ON DELETE RESTRICT,
                           degree_image VARCHAR(255),
                           verified BOOLEAN DEFAULT FALSE,
                           created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                           CONSTRAINT chk_graduate_identifier CHECK (
                               student_id IS NOT NULL OR identity_document IS NOT NULL
                               )
);

CREATE INDEX idx_graduates_student_id ON graduates(student_id) WHERE student_id IS NOT NULL;
CREATE INDEX idx_graduates_identity_doc ON graduates(identity_document) WHERE identity_document IS NOT NULL;
CREATE INDEX idx_graduates_verification ON graduates(verified, graduation_year);

CREATE TABLE users (
                       id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                       username VARCHAR(50) UNIQUE,
                       email VARCHAR(100) UNIQUE NOT NULL,
                       password VARCHAR(255) NOT NULL,
                       user_type user_type_enum DEFAULT 'GRADUATE' NOT NULL,
                       active BOOLEAN DEFAULT TRUE NOT NULL,
                       email_verified BOOLEAN DEFAULT FALSE,
                       verification_token VARCHAR(100),
                       token_expiration TIMESTAMP WITH TIME ZONE,
                       has_disability BOOLEAN DEFAULT FALSE NOT NULL,
                       disability_type disability_type_enum,
                       disability_details VARCHAR(255),
                       profile_completion_percentage INT DEFAULT 0,
                       email_notification_enabled BOOLEAN DEFAULT TRUE,
                       push_notification_enabled BOOLEAN DEFAULT TRUE,
                       account_deactivated_at TIMESTAMP WITH TIME ZONE,
                       deactivation_reason TEXT,
                       last_login TIMESTAMP WITH TIME ZONE,
                       created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                       source_graduate_id BIGINT REFERENCES graduates(id) ON DELETE SET NULL,
                       registered_with VARCHAR(20),
                       password_changed_at TIMESTAMP WITH TIME ZONE,
                       password_must_change BOOLEAN DEFAULT FALSE,
                       CONSTRAINT chk_user_identifier CHECK (email IS NOT NULL OR username IS NOT NULL),
                       CONSTRAINT chk_disability_type CHECK (
                           (has_disability = FALSE AND disability_type IS NULL) OR
                           (has_disability = TRUE)
                           )
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_type_active ON users(user_type, active);
CREATE INDEX idx_users_verification ON users(email_verified, active);
CREATE INDEX idx_users_last_login ON users(last_login DESC NULLS LAST);
CREATE UNIQUE INDEX idx_users_graduate_unique ON users(source_graduate_id) WHERE source_graduate_id IS NOT NULL;

CREATE TRIGGER tr_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

CREATE TABLE profiles (
                          user_id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
                          first_name VARCHAR(100) NOT NULL,
                          last_name VARCHAR(100) NOT NULL,
                          phone VARCHAR(15),
                          profile_picture VARCHAR(255),
                          bio TEXT,
                          student_id VARCHAR(20),
                          identity_document VARCHAR(20),
                          graduation_year INT,
                          graduation_gpa NUMERIC(3,2),
                          career_id BIGINT REFERENCES careers(id) ON DELETE SET NULL,
                          linkedin_url VARCHAR(255),
                          website_url VARCHAR(255),
                          address TEXT,
                          city VARCHAR(100),
                          country_id BIGINT REFERENCES countries(id) ON DELETE SET NULL,
                          privacy_settings JSONB DEFAULT '{
                            "show_email": false,
                            "show_phone": false,
                            "show_current_job": true,
                            "show_graduation_year": true,
                            "show_gpa": false,
                            "profile_visibility": "ALUMNI_ONLY",
                            "show_connections": true,
                            "allow_connection_requests": true
                          }'::JSONB,
                          search_vector tsvector,
                          created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_profiles_name ON profiles(last_name, first_name);
CREATE INDEX idx_profiles_career_year ON profiles(career_id, graduation_year) WHERE career_id IS NOT NULL;
CREATE INDEX idx_profiles_graduation_year ON profiles(graduation_year) WHERE graduation_year IS NOT NULL;
CREATE INDEX idx_profiles_country ON profiles(country_id) WHERE country_id IS NOT NULL;
CREATE INDEX idx_profiles_search ON profiles USING gin((first_name || ' ' || last_name) gin_trgm_ops);
CREATE INDEX idx_profiles_search_vector ON profiles USING gin(search_vector);
CREATE INDEX idx_profiles_privacy ON profiles USING gin(privacy_settings);

CREATE TRIGGER tr_profiles_updated_at
    BEFORE UPDATE ON profiles
    FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER tr_profiles_search_vector
    BEFORE INSERT OR UPDATE ON profiles
    FOR EACH ROW
EXECUTE FUNCTION tsvector_update_trigger(search_vector, 'pg_catalog.spanish', first_name, last_name, bio);

CREATE TABLE login_attempts (
                                id BIGSERIAL PRIMARY KEY,
                                email VARCHAR(100) NOT NULL UNIQUE,
                                failed_attempts INTEGER NOT NULL DEFAULT 0,
                                last_failed_attempt TIMESTAMP WITH TIME ZONE,
                                locked_until TIMESTAMP WITH TIME ZONE,
                                last_success TIMESTAMP WITH TIME ZONE,
                                created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_login_attempts_email ON login_attempts(email);
CREATE INDEX idx_login_attempts_locked ON login_attempts(locked_until) WHERE locked_until IS NOT NULL;

CREATE TABLE user_mfa (
                          id BIGSERIAL PRIMARY KEY,
                          user_id BIGINT NOT NULL UNIQUE,
                          secret_key VARCHAR(32) NOT NULL,
                          is_enabled BOOLEAN NOT NULL DEFAULT FALSE,
                          backup_codes TEXT,
                          created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                          enabled_at TIMESTAMP WITH TIME ZONE,
                          FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_user_mfa_user_id ON user_mfa(user_id);

CREATE TABLE password_reset_tokens (
                                       id BIGSERIAL PRIMARY KEY,
                                       user_id BIGINT NOT NULL,
                                       token VARCHAR(100) NOT NULL UNIQUE,
                                       expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                       used BOOLEAN NOT NULL DEFAULT FALSE,
                                       used_at TIMESTAMP WITH TIME ZONE,
                                       created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                       FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_password_reset_token ON password_reset_tokens(token);
CREATE INDEX idx_password_reset_user_id ON password_reset_tokens(user_id);
CREATE INDEX idx_password_reset_expires ON password_reset_tokens(expires_at) WHERE used = FALSE;

CREATE TABLE refresh_tokens (
                                id BIGSERIAL PRIMARY KEY,
                                user_id BIGINT NOT NULL,
                                token VARCHAR(500) NOT NULL UNIQUE,
                                expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                revoked BOOLEAN NOT NULL DEFAULT FALSE,
                                revoked_at TIMESTAMP WITH TIME ZONE,
                                created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_refresh_token ON refresh_tokens(token);
CREATE INDEX idx_refresh_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_expires ON refresh_tokens(expires_at) WHERE revoked = FALSE;

CREATE TABLE connections (
                             id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                             requester_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                             addressee_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                             status connection_status_enum DEFAULT 'PENDING',
                             message TEXT,
                             created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                             responded_at TIMESTAMP WITH TIME ZONE,
                             CONSTRAINT uk_connection_pair UNIQUE (requester_id, addressee_id),
                             CONSTRAINT chk_not_self_connect CHECK (requester_id != addressee_id)
);

CREATE INDEX idx_connections_requester ON connections(requester_id, status);
CREATE INDEX idx_connections_addressee ON connections(addressee_id, status);
CREATE INDEX idx_connections_pending ON connections(status, created_at) WHERE status = 'PENDING';
CREATE INDEX idx_connections_accepted ON connections(requester_id, addressee_id) WHERE status = 'ACCEPTED';

CREATE TABLE followers (
                           id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                           follower_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                           following_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                           created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                           CONSTRAINT uk_follower_following UNIQUE (follower_id, following_id),
                           CONSTRAINT chk_not_self_follow CHECK (follower_id != following_id)
);

CREATE INDEX idx_followers_follower ON followers(follower_id);
CREATE INDEX idx_followers_following ON followers(following_id);

CREATE TABLE skills (
                        id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                        name VARCHAR(100) UNIQUE NOT NULL,
                        category VARCHAR(50),
                        usage_count INT DEFAULT 0,
                        created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_skills_name ON skills(name);
CREATE INDEX idx_skills_category ON skills(category);
CREATE INDEX idx_skills_popular ON skills(usage_count DESC);

CREATE TABLE user_skills (
                             id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                             user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                             skill_id BIGINT NOT NULL REFERENCES skills(id) ON DELETE CASCADE,
                             proficiency_level INT CHECK (proficiency_level BETWEEN 1 AND 5),
                             years_of_experience INT,
                             created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                             CONSTRAINT uk_user_skill UNIQUE (user_id, skill_id)
);

CREATE INDEX idx_user_skills_user ON user_skills(user_id);
CREATE INDEX idx_user_skills_skill ON user_skills(skill_id);

CREATE TABLE skill_endorsements (
                                    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                    user_skill_id BIGINT NOT NULL REFERENCES user_skills(id) ON DELETE CASCADE,
                                    endorsed_by BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                    CONSTRAINT uk_endorsement UNIQUE (user_skill_id, endorsed_by)
);

CREATE INDEX idx_endorsements_skill ON skill_endorsements(user_skill_id);
CREATE INDEX idx_endorsements_by ON skill_endorsements(endorsed_by);

CREATE TABLE recommendations (
                                 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                 recipient_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                 recommender_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                 relationship VARCHAR(100),
                                 recommendation_text TEXT NOT NULL,
                                 is_visible BOOLEAN DEFAULT TRUE,
                                 created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                 updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                 CONSTRAINT chk_not_self_recommend CHECK (recipient_id != recommender_id)
);

CREATE INDEX idx_recommendations_recipient ON recommendations(recipient_id, is_visible);
CREATE INDEX idx_recommendations_recommender ON recommendations(recommender_id);

CREATE TRIGGER tr_recommendations_updated_at
    BEFORE UPDATE ON recommendations
    FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

CREATE TABLE work_experience (
                                 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                 user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                 company_name VARCHAR(150) NOT NULL,
                                 country_id BIGINT REFERENCES countries(id) ON DELETE SET NULL,
                                 sector VARCHAR(100),
                                 position VARCHAR(100),
                                 job_description TEXT,
                                 achievements TEXT[],
                                 skills_used VARCHAR(100)[],
                                 salary_range VARCHAR(50),
                                 start_date DATE,
                                 end_date DATE,
                                 is_current BOOLEAN DEFAULT FALSE,
                                 verified BOOLEAN DEFAULT FALSE,
                                 created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                 updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_work_user ON work_experience(user_id, start_date DESC);
CREATE INDEX idx_work_current ON work_experience(user_id, is_current) WHERE is_current = TRUE;
CREATE INDEX idx_work_company ON work_experience USING gin(company_name gin_trgm_ops);
CREATE INDEX idx_work_sector ON work_experience(sector) WHERE sector IS NOT NULL;

CREATE TRIGGER tr_work_experience_updated_at
    BEFORE UPDATE ON work_experience
    FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

CREATE TABLE postgraduate_studies (
                                      id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                      user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                      degree_title VARCHAR(150) NOT NULL,
                                      university_id BIGINT REFERENCES universities(id) ON DELETE SET NULL,
                                      country_id BIGINT REFERENCES countries(id) ON DELETE SET NULL,
                                      admission_year INT,
                                      completion_year INT,
                                      funding funding_enum DEFAULT 'SELF_FUNDED',
                                      degree_image VARCHAR(255),
                                      verified BOOLEAN DEFAULT FALSE,
                                      created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                      updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_postgrad_user ON postgraduate_studies(user_id, completion_year DESC);
CREATE INDEX idx_postgrad_university ON postgraduate_studies(university_id);

CREATE TRIGGER tr_postgraduate_updated_at
    BEFORE UPDATE ON postgraduate_studies
    FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

CREATE TABLE certifications (
                                id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                name VARCHAR(150) NOT NULL,
                                issuing_organization VARCHAR(150) NOT NULL,
                                issue_date DATE,
                                expiration_date DATE,
                                credential_id VARCHAR(100),
                                credential_url VARCHAR(512),
                                image_url VARCHAR(512),
                                type certification_type_enum DEFAULT 'COURSE',
                                verified BOOLEAN DEFAULT FALSE,
                                created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_certifications_user ON certifications(user_id, issue_date DESC);
CREATE INDEX idx_certifications_type ON certifications(type);
CREATE INDEX idx_certifications_org ON certifications USING gin(issuing_organization gin_trgm_ops);

CREATE TRIGGER tr_certifications_updated_at
    BEFORE UPDATE ON certifications
    FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

CREATE TABLE participations (
                                id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                program VARCHAR(150) NOT NULL,
                                participation_level participation_level_enum DEFAULT 'ATTENDEE',
                                start_date DATE,
                                end_date DATE,
                                available_for_teaching BOOLEAN DEFAULT FALSE,
                                verified BOOLEAN DEFAULT FALSE,
                                created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_participations_user ON participations(user_id, start_date DESC);
CREATE INDEX idx_participations_teaching ON participations(available_for_teaching) WHERE available_for_teaching = TRUE;

CREATE TABLE tags (
                      id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                      name VARCHAR(50) UNIQUE NOT NULL,
                      usage_count INT DEFAULT 0,
                      created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_tags_name ON tags(name);
CREATE INDEX idx_tags_popularity ON tags(usage_count DESC);

CREATE TABLE posts (
                       id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                       user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                       content TEXT NOT NULL,
                       is_public BOOLEAN DEFAULT TRUE,
                       deleted_at TIMESTAMP WITH TIME ZONE,
                       created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_posts_feed ON posts(created_at DESC) WHERE is_public = TRUE AND deleted_at IS NULL;
CREATE INDEX idx_posts_user_feed ON posts(user_id, created_at DESC);
CREATE INDEX idx_posts_active ON posts(created_at DESC) WHERE deleted_at IS NULL;

CREATE TRIGGER tr_posts_updated_at
    BEFORE UPDATE ON posts
    FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

CREATE TABLE post_tags (
                           post_id BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
                           tag_id BIGINT NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
                           PRIMARY KEY (post_id, tag_id)
);

CREATE INDEX idx_post_tags_tag ON post_tags(tag_id);

CREATE TABLE post_media (
                            id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                            post_id BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
                            media_url VARCHAR(512) NOT NULL,
                            media_type VARCHAR(50),
                            display_order INT DEFAULT 0,
                            created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_post_media_post ON post_media(post_id, display_order);

CREATE TABLE post_reactions (
                                id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                post_id BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
                                user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                reaction_type reaction_type_enum NOT NULL,
                                created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                CONSTRAINT uk_post_user_reaction UNIQUE (post_id, user_id)
);

CREATE INDEX idx_reactions_post ON post_reactions(post_id, reaction_type);
CREATE INDEX idx_reactions_user ON post_reactions(user_id, created_at DESC);

CREATE TRIGGER tr_post_reactions_updated_at
    BEFORE UPDATE ON post_reactions
    FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

CREATE TABLE comments (
                          id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                          post_id BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
                          user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                          content TEXT NOT NULL,
                          deleted_at TIMESTAMP WITH TIME ZONE,
                          created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_comments_post ON comments(post_id, created_at ASC) WHERE deleted_at IS NULL;
CREATE INDEX idx_comments_user ON comments(user_id, created_at DESC);

CREATE TRIGGER tr_comments_updated_at
    BEFORE UPDATE ON comments
    FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

CREATE TABLE conversations (
                               id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                               type conversation_type_enum NOT NULL DEFAULT 'PRIVATE',
                               group_name VARCHAR(100),
                               group_description TEXT,
                               created_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
                               created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                               updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_conversations_type ON conversations(type);

CREATE TABLE conversation_participants (
                                           conversation_id BIGINT NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
                                           user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                           joined_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                           last_read_at TIMESTAMP WITH TIME ZONE,
                                           PRIMARY KEY (conversation_id, user_id)
);

CREATE INDEX idx_participants_user ON conversation_participants(user_id, joined_at DESC);

CREATE TABLE messages (
                          id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                          conversation_id BIGINT NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
                          user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                          content TEXT NOT NULL,
                          created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_messages_conversation ON messages(conversation_id, created_at DESC);
CREATE INDEX idx_messages_user ON messages(user_id, created_at DESC);

CREATE TABLE notifications (
                               id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                               user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                               type general_notification_type_enum NOT NULL,
                               title VARCHAR(200) NOT NULL,
                               content TEXT,
                               reference_id BIGINT,
                               reference_type VARCHAR(50),
                               is_read BOOLEAN DEFAULT FALSE,
                               created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                               read_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_notifications_user_unread ON notifications(user_id, is_read, created_at DESC);
CREATE INDEX idx_notifications_reference ON notifications(reference_type, reference_id);

CREATE TABLE job_opportunities (
                                   id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                   title VARCHAR(200) NOT NULL,
                                   company_name VARCHAR(150),
                                   description TEXT NOT NULL,
                                   requirements TEXT,
                                   how_to_apply TEXT NOT NULL,
                                   country_id BIGINT REFERENCES countries(id) ON DELETE SET NULL,
                                   city VARCHAR(100),
                                   is_remote BOOLEAN DEFAULT FALSE,
                                   job_type VARCHAR(50),
                                   experience_level VARCHAR(50),
                                   salary_range VARCHAR(100),
                                   posted_by_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
                                   created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                   updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                   expires_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_jobs_posted_by ON job_opportunities(posted_by_user_id);
CREATE INDEX idx_jobs_created ON job_opportunities(created_at DESC);
CREATE INDEX idx_jobs_expires ON job_opportunities(expires_at) WHERE expires_at IS NOT NULL;
CREATE INDEX idx_jobs_not_expired ON job_opportunities(expires_at, created_at DESC) WHERE expires_at IS NOT NULL;

CREATE TRIGGER tr_job_opportunities_updated_at
    BEFORE UPDATE ON job_opportunities
    FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

CREATE TABLE events (
                        id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                        title VARCHAR(200) NOT NULL,
                        description TEXT NOT NULL,
                        event_type event_type_enum NOT NULL,
                        location_or_url VARCHAR(512) NOT NULL,
                        start_date_time TIMESTAMP WITH TIME ZONE NOT NULL,
                        end_date_time TIMESTAMP WITH TIME ZONE NOT NULL,
                        status event_status_enum NOT NULL DEFAULT 'SCHEDULED',
                        max_participants INT,
                        current_participants INT DEFAULT 0,
                        created_by_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
                        created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                        CONSTRAINT chk_event_dates CHECK (end_date_time > start_date_time),
                        CONSTRAINT chk_event_capacity CHECK (max_participants IS NULL OR max_participants > 0)
);

CREATE INDEX idx_events_start_date ON events(start_date_time);
CREATE INDEX idx_events_status ON events(status, start_date_time);
CREATE INDEX idx_events_type ON events(event_type, start_date_time);
CREATE INDEX idx_events_created_by ON events(created_by_user_id);
CREATE INDEX idx_events_scheduled ON events(start_date_time) WHERE status = 'SCHEDULED';

CREATE TRIGGER tr_events_updated_at
    BEFORE UPDATE ON events
    FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

CREATE TABLE event_registrations (
                                     id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                     event_id BIGINT NOT NULL REFERENCES events(id) ON DELETE CASCADE,
                                     user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                     registration_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                     attended BOOLEAN DEFAULT NULL,
                                     feedback TEXT,
                                     CONSTRAINT uk_event_user_registration UNIQUE (event_id, user_id)
);

CREATE INDEX idx_registrations_event ON event_registrations(event_id);
CREATE INDEX idx_registrations_user ON event_registrations(user_id, registration_date DESC);
CREATE INDEX idx_registrations_attended ON event_registrations(event_id, attended);

CREATE TABLE outstanding_alumni (
                                    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
                                    reason TEXT NOT NULL,
                                    recognition_date DATE NOT NULL,
                                    awarded_by_user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
                                    reference_url VARCHAR(512),
                                    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_outstanding_date ON outstanding_alumni(recognition_date DESC);
CREATE INDEX idx_outstanding_awarded_by ON outstanding_alumni(awarded_by_user_id);

CREATE TABLE surveys (
                         id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                         title VARCHAR(200) NOT NULL,
                         description TEXT,
                         json_schema JSONB NOT NULL,
                         created_by BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
                         status survey_status_enum DEFAULT 'DRAFT',
                         start_date TIMESTAMP WITH TIME ZONE,
                         end_date TIMESTAMP WITH TIME ZONE,
                         is_anonymous BOOLEAN DEFAULT FALSE,
                         allow_multiple_responses BOOLEAN DEFAULT FALSE,
                         created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                         CONSTRAINT chk_survey_dates CHECK (start_date IS NULL OR end_date IS NULL OR end_date > start_date)
);

CREATE INDEX idx_surveys_status ON surveys(status, start_date);
CREATE INDEX idx_surveys_created_by ON surveys(created_by);
CREATE INDEX idx_surveys_active ON surveys(status, start_date, end_date) WHERE status = 'ACTIVE';
CREATE INDEX idx_surveys_schema ON surveys USING gin(json_schema);

CREATE TRIGGER tr_surveys_updated_at
    BEFORE UPDATE ON surveys
    FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

CREATE TABLE survey_assignments (
                                    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                    survey_id BIGINT NOT NULL REFERENCES surveys(id) ON DELETE CASCADE,
                                    assignment_type assignment_type_enum NOT NULL,
                                    career_id BIGINT REFERENCES careers(id) ON DELETE CASCADE,
                                    graduation_year_start INT,
                                    graduation_year_end INT,
                                    user_type user_type_enum,
                                    assigned_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_assignments_survey ON survey_assignments(survey_id);
CREATE INDEX idx_assignments_type ON survey_assignments(assignment_type);

CREATE TABLE survey_assigned_users (
                                       id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                       assignment_id BIGINT NOT NULL REFERENCES survey_assignments(id) ON DELETE CASCADE,
                                       user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                       notified BOOLEAN DEFAULT FALSE,
                                       notification_date TIMESTAMP WITH TIME ZONE,
                                       CONSTRAINT uk_assignment_user UNIQUE (assignment_id, user_id)
);

CREATE INDEX idx_assigned_users_user ON survey_assigned_users(user_id, notified);
CREATE INDEX idx_assigned_users_assignment ON survey_assigned_users(assignment_id);

CREATE TABLE survey_responses (
                                  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                  survey_id BIGINT NOT NULL REFERENCES surveys(id) ON DELETE CASCADE,
                                  user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
                                  status response_status_enum DEFAULT 'IN_PROGRESS',
                                  response_json JSONB NOT NULL,
                                  total_time_seconds INT,
                                  ip_address INET,
                                  user_agent TEXT,
                                  started_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                  completed_at TIMESTAMP WITH TIME ZONE,
                                  CONSTRAINT chk_response_dates CHECK (completed_at IS NULL OR completed_at >= started_at)
);

CREATE INDEX idx_responses_survey ON survey_responses(survey_id, status);
CREATE INDEX idx_responses_user ON survey_responses(user_id, completed_at DESC NULLS LAST);
CREATE INDEX idx_responses_completed ON survey_responses(survey_id, completed_at) WHERE status = 'COMPLETED';
CREATE INDEX idx_responses_payload ON survey_responses USING gin(response_json);

CREATE TABLE survey_response_details (
                                         id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                         response_id BIGINT NOT NULL REFERENCES survey_responses(id) ON DELETE CASCADE,
                                         survey_id BIGINT NOT NULL,
                                         question_id VARCHAR(100) NOT NULL,
                                         question_text TEXT,
                                         question_type VARCHAR(50),
                                         answer_value TEXT NOT NULL,
                                         answered_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_response_details_response ON survey_response_details(response_id);
CREATE INDEX idx_response_details_survey_question ON survey_response_details(survey_id, question_id);
CREATE INDEX idx_response_details_type ON survey_response_details(question_type);

CREATE TABLE survey_statistics (
                                   id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                   survey_id BIGINT NOT NULL REFERENCES surveys(id) ON DELETE CASCADE,
                                   question_id VARCHAR(100) NOT NULL,
                                   option_value VARCHAR(500),
                                   total_responses INT DEFAULT 0,
                                   percentage NUMERIC(5,2),
                                   last_updated TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                   CONSTRAINT uk_survey_question_option UNIQUE (survey_id, question_id, option_value)
);

CREATE INDEX idx_statistics_survey ON survey_statistics(survey_id);

CREATE TABLE survey_notifications (
                                      id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                      survey_id BIGINT NOT NULL REFERENCES surveys(id) ON DELETE CASCADE,
                                      user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                      notification_type notification_type_enum NOT NULL,
                                      medium notification_medium_enum DEFAULT 'PUSH',
                                      sent BOOLEAN DEFAULT FALSE,
                                      sent_at TIMESTAMP WITH TIME ZONE,
                                      created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notifications_user_sent ON survey_notifications(user_id, sent);
CREATE INDEX idx_notifications_survey ON survey_notifications(survey_id);
CREATE INDEX idx_notifications_pending ON survey_notifications(created_at) WHERE sent = FALSE;

CREATE TABLE content_reports (
                                 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                 reporter_id BIGINT NOT NULL REFERENCES users(id) ON DELETE SET NULL,
                                 reported_user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
                                 content_type VARCHAR(50) NOT NULL,
                                 content_id BIGINT NOT NULL,
                                 report_type report_type_enum NOT NULL,
                                 description TEXT,
                                 status report_status_enum DEFAULT 'PENDING',
                                 reviewed_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
                                 reviewed_at TIMESTAMP WITH TIME ZONE,
                                 action_taken TEXT,
                                 created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_reports_status ON content_reports(status, created_at DESC);
CREATE INDEX idx_reports_content ON content_reports(content_type, content_id);
CREATE INDEX idx_reports_reporter ON content_reports(reporter_id);
CREATE INDEX idx_reports_reviewed_by ON content_reports(reviewed_by) WHERE reviewed_by IS NOT NULL;

CREATE TABLE search_history (
                                id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
                                search_query TEXT NOT NULL,
                                search_type VARCHAR(50),
                                results_count INT,
                                created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_search_history_user ON search_history(user_id, created_at DESC);
CREATE INDEX idx_search_trending ON search_history(search_query, created_at DESC);

CREATE TABLE rate_limits (
                             id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                             user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                             action_type VARCHAR(50) NOT NULL,
                             action_count INT DEFAULT 1,
                             window_start TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                             CONSTRAINT uk_rate_limit UNIQUE (user_id, action_type, window_start)
);

CREATE INDEX idx_rate_limits_user_action ON rate_limits(user_id, action_type, window_start DESC);

CREATE TABLE audit_logs (
                            id BIGINT GENERATED ALWAYS AS IDENTITY,
                            created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                            user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
                            username VARCHAR(100),
                            action_type audit_action_enum NOT NULL,
                            table_name VARCHAR(100),
                            record_id VARCHAR(255),
                            details JSONB,
                            ip_address INET,
                            PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);

CREATE TABLE audit_logs_2025 PARTITION OF audit_logs FOR VALUES FROM ('2025-01-01') TO ('2026-01-01');
CREATE TABLE audit_logs_2026 PARTITION OF audit_logs FOR VALUES FROM ('2026-01-01') TO ('2027-01-01');
CREATE TABLE audit_logs_2027 PARTITION OF audit_logs FOR VALUES FROM ('2027-01-01') TO ('2028-01-01');
CREATE TABLE audit_logs_2028 PARTITION OF audit_logs FOR VALUES FROM ('2028-01-01') TO ('2029-01-01');
CREATE TABLE audit_logs_default PARTITION OF audit_logs DEFAULT;

CREATE INDEX idx_audit_user ON audit_logs(user_id, created_at DESC);
CREATE INDEX idx_audit_action ON audit_logs(action_type, created_at DESC);
CREATE INDEX idx_audit_table ON audit_logs(table_name, record_id);
CREATE INDEX idx_audit_details ON audit_logs USING gin(details);

CREATE TABLE activities (
                            id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                            user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                            activity_type VARCHAR(50) NOT NULL,
                            context JSONB,
                            is_public BOOLEAN DEFAULT TRUE,
                            created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_activities_user_date ON activities(user_id, created_at DESC);
CREATE INDEX idx_activities_public ON activities(created_at DESC) WHERE is_public = TRUE;
CREATE INDEX idx_activities_type ON activities(activity_type, created_at DESC);

CREATE MATERIALIZED VIEW mv_user_available_surveys AS
SELECT DISTINCT
    e.id AS survey_id,
    e.title,
    e.description,
    e.status,
    e.start_date,
    e.end_date,
    e.is_anonymous,
    u.id AS user_id,
    u.user_type,
    p.career_id,
    p.graduation_year,
    CASE
        WHEN er.id IS NOT NULL AND er.status = 'COMPLETED' THEN 'COMPLETED'
        WHEN er.id IS NOT NULL AND er.status = 'IN_PROGRESS' THEN 'IN_PROGRESS'
        ELSE 'PENDING'
        END AS user_status
FROM surveys e
         CROSS JOIN users u
         LEFT JOIN profiles p ON u.id = p.user_id
         LEFT JOIN survey_assignments sa ON e.id = sa.survey_id
         LEFT JOIN survey_assigned_users sau ON sa.id = sau.assignment_id AND sau.user_id = u.id
         LEFT JOIN survey_responses er ON e.id = er.survey_id AND er.user_id = u.id
WHERE e.status = 'ACTIVE'
  AND (e.start_date IS NULL OR e.start_date <= CURRENT_TIMESTAMP)
  AND (e.end_date IS NULL OR e.end_date >= CURRENT_TIMESTAMP)
  AND u.active = TRUE
  AND (
    sa.assignment_type = 'ALL'
        OR (sa.assignment_type = 'CAREER' AND sa.career_id = p.career_id)
        OR (sa.assignment_type = 'GRADUATION_YEAR' AND p.graduation_year BETWEEN sa.graduation_year_start AND sa.graduation_year_end)
        OR (sa.assignment_type = 'USER_TYPE' AND sa.user_type = u.user_type)
        OR (sa.assignment_type = 'SPECIFIC_USERS' AND sau.user_id = u.id)
    );

CREATE UNIQUE INDEX idx_mv_user_surveys ON mv_user_available_surveys(survey_id, user_id);
CREATE INDEX idx_mv_user_surveys_user ON mv_user_available_surveys(user_id, user_status);

CREATE MATERIALIZED VIEW mv_survey_analytics AS
SELECT
    r.survey_id,
    data.key AS question_key,
    data.value AS answer_value,
    COUNT(*) AS total_count,
    ROUND(COUNT(*) * 100.0 / SUM(COUNT(*)) OVER (PARTITION BY r.survey_id, data.key), 2) AS percentage
FROM survey_responses r,
     LATERAL jsonb_each_text(r.response_json) data
WHERE r.status = 'COMPLETED'
GROUP BY r.survey_id, data.key, data.value;

CREATE INDEX idx_mv_survey_analytics ON mv_survey_analytics(survey_id, question_key);

CREATE MATERIALIZED VIEW mv_alumni_statistics AS
SELECT
    p.career_id,
    c.name as career_name,
    p.graduation_year,
    COUNT(DISTINCT u.id) as total_alumni,
    COUNT(DISTINCT CASE WHEN w.is_current = TRUE THEN u.id END) as employed_alumni,
    COUNT(DISTINCT CASE WHEN pg.id IS NOT NULL THEN u.id END) as with_postgraduate,
    AVG(p.graduation_gpa) as avg_gpa,
    COUNT(DISTINCT CASE WHEN oa.id IS NOT NULL THEN u.id END) as outstanding_alumni
FROM users u
         INNER JOIN profiles p ON u.id = p.user_id
         LEFT JOIN careers c ON p.career_id = c.id
         LEFT JOIN work_experience w ON u.id = w.user_id AND w.is_current = TRUE
         LEFT JOIN postgraduate_studies pg ON u.id = pg.user_id
         LEFT JOIN outstanding_alumni oa ON u.id = oa.user_id
WHERE u.active = TRUE AND u.user_type = 'GRADUATE'
GROUP BY p.career_id, c.name, p.graduation_year;

CREATE INDEX idx_mv_alumni_stats_career ON mv_alumni_statistics(career_id, graduation_year);

CREATE MATERIALIZED VIEW mv_connection_network AS
SELECT
    u.id as user_id,
    COUNT(DISTINCT c1.addressee_id) + COUNT(DISTINCT c2.requester_id) as total_connections,
    COUNT(DISTINCT f1.following_id) as following_count,
    COUNT(DISTINCT f2.follower_id) as follower_count
FROM users u
         LEFT JOIN connections c1 ON u.id = c1.requester_id AND c1.status = 'ACCEPTED'
         LEFT JOIN connections c2 ON u.id = c2.addressee_id AND c2.status = 'ACCEPTED'
         LEFT JOIN followers f1 ON u.id = f1.follower_id
         LEFT JOIN followers f2 ON u.id = f2.following_id
WHERE u.active = TRUE
GROUP BY u.id;

CREATE INDEX idx_mv_connection_network_user ON mv_connection_network(user_id);

CREATE MATERIALIZED VIEW mv_engagement_metrics AS
SELECT
    u.id as user_id,
    COUNT(DISTINCT p.id) as total_posts,
    COUNT(DISTINCT c.id) as total_comments,
    COUNT(DISTINCT pr.id) as total_reactions,
    COUNT(DISTINCT er.id) as events_attended,
    MAX(p.created_at) as last_post_date,
    MAX(u.last_login) as last_login_date
FROM users u
         LEFT JOIN posts p ON u.id = p.user_id AND p.deleted_at IS NULL
         LEFT JOIN comments c ON u.id = c.user_id AND c.deleted_at IS NULL
         LEFT JOIN post_reactions pr ON u.id = pr.user_id
         LEFT JOIN event_registrations er ON u.id = er.user_id
WHERE u.active = TRUE
GROUP BY u.id;

CREATE INDEX idx_mv_engagement_user ON mv_engagement_metrics(user_id);

CREATE OR REPLACE FUNCTION validate_graduate_registration(
    p_student_id VARCHAR(20),
    p_identity_document VARCHAR(20)
)
    RETURNS TABLE (
                      is_valid BOOLEAN,
                      graduate_id BIGINT,
                      first_name VARCHAR(100),
                      last_name VARCHAR(100),
                      career_id BIGINT,
                      graduation_year INT,
                      gpa NUMERIC(3,2)
                  ) AS $$
BEGIN
    RETURN QUERY
        SELECT
            TRUE as is_valid,
            g.id,
            g.first_name,
            g.last_name,
            g.career_id,
            g.graduation_year,
            g.gpa
        FROM graduates g
        WHERE
            (p_student_id IS NOT NULL AND g.student_id = p_student_id)
           OR (p_identity_document IS NOT NULL AND g.identity_document = p_identity_document)
        LIMIT 1;

    IF NOT FOUND THEN
        RETURN QUERY SELECT FALSE, NULL::BIGINT, NULL::VARCHAR, NULL::VARCHAR, NULL::BIGINT, NULL::INT, NULL::NUMERIC;
    END IF;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION can_register_for_event(p_event_id BIGINT, p_user_id BIGINT)
    RETURNS BOOLEAN AS $$
DECLARE
    v_max_participants INT;
    v_current_participants INT;
    v_already_registered BOOLEAN;
BEGIN
    SELECT EXISTS (
        SELECT 1 FROM event_registrations WHERE event_id = p_event_id AND user_id = p_user_id
    ) INTO v_already_registered;

    IF v_already_registered THEN
        RETURN FALSE;
    END IF;

    SELECT max_participants, current_participants
    INTO v_max_participants, v_current_participants
    FROM events WHERE id = p_event_id;

    IF v_max_participants IS NULL THEN
        RETURN TRUE;
    END IF;

    RETURN v_current_participants < v_max_participants;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION update_event_participants()
    RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        UPDATE events SET current_participants = current_participants + 1 WHERE id = NEW.event_id;
    ELSIF TG_OP = 'DELETE' THEN
        UPDATE events SET current_participants = GREATEST(current_participants - 1, 0) WHERE id = OLD.event_id;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tr_event_registration_count
    AFTER INSERT OR DELETE ON event_registrations
    FOR EACH ROW
EXECUTE FUNCTION update_event_participants();

CREATE OR REPLACE FUNCTION update_survey_statistics()
    RETURNS TRIGGER AS $$
BEGIN
    IF NEW.question_type IN ('radiogroup', 'checkbox', 'rating', 'dropdown') THEN
        INSERT INTO survey_statistics (survey_id, question_id, option_value, total_responses)
        VALUES (NEW.survey_id, NEW.question_id, NEW.answer_value, 1)
        ON CONFLICT (survey_id, question_id, option_value)
            DO UPDATE SET
                          total_responses = survey_statistics.total_responses + 1,
                          last_updated = CURRENT_TIMESTAMP;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tr_update_survey_stats
    AFTER INSERT ON survey_response_details
    FOR EACH ROW
EXECUTE FUNCTION update_survey_statistics();

CREATE OR REPLACE FUNCTION update_skill_usage_count()
    RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        UPDATE skills SET usage_count = usage_count + 1 WHERE id = NEW.skill_id;
    ELSIF TG_OP = 'DELETE' THEN
        UPDATE skills SET usage_count = GREATEST(usage_count - 1, 0) WHERE id = OLD.skill_id;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tr_skill_usage_count
    AFTER INSERT OR DELETE ON user_skills
    FOR EACH ROW
EXECUTE FUNCTION update_skill_usage_count();

CREATE OR REPLACE FUNCTION update_tag_usage_count()
    RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        UPDATE tags SET usage_count = usage_count + 1 WHERE id = NEW.tag_id;
    ELSIF TG_OP = 'DELETE' THEN
        UPDATE tags SET usage_count = GREATEST(usage_count - 1, 0) WHERE id = OLD.tag_id;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tr_tag_usage_count
    AFTER INSERT OR DELETE ON post_tags
    FOR EACH ROW
EXECUTE FUNCTION update_tag_usage_count();

CREATE OR REPLACE FUNCTION create_audit_log_partition()
    RETURNS void AS $$
DECLARE
    partition_year INT;
    partition_name TEXT;
    start_date TEXT;
    end_date TEXT;
BEGIN
    partition_year := EXTRACT(YEAR FROM CURRENT_DATE) + 1;
    partition_name := 'audit_logs_' || partition_year;
    start_date := partition_year || '-01-01';
    end_date := (partition_year + 1) || '-01-01';

    EXECUTE format(
            'CREATE TABLE IF NOT EXISTS %I PARTITION OF audit_logs FOR VALUES FROM (%L) TO (%L)',
            partition_name, start_date, end_date
            );
END;
$$ LANGUAGE plpgsql;

INSERT INTO countries (name, iso_code) VALUES
                                           ('El Salvador', 'SV'),
                                           ('Guatemala', 'GT'),
                                           ('Honduras', 'HN'),
                                           ('Nicaragua', 'NI'),
                                           ('Costa Rica', 'CR'),
                                           ('Panama', 'PA'),
                                           ('United States', 'US'),
                                           ('Canada', 'CA'),
                                           ('Mexico', 'MX'),
                                           ('Spain', 'ES')
ON CONFLICT (name) DO NOTHING;

INSERT INTO universities (name, country_id)
SELECT 'University of El Salvador', id FROM countries WHERE iso_code = 'SV'
UNION ALL
SELECT 'Dr. José Matías Delgado University', id FROM countries WHERE iso_code = 'SV'
UNION ALL
SELECT 'Universidad de San Carlos de Guatemala', id FROM countries WHERE iso_code = 'GT'
UNION ALL
SELECT 'Massachusetts Institute of Technology', id FROM countries WHERE iso_code = 'US'
ON CONFLICT DO NOTHING;

INSERT INTO careers (name, university_id, code)
SELECT 'Systems Engineering', id, 'SYS-ENG' FROM universities WHERE name = 'University of El Salvador'
UNION ALL
SELECT 'Industrial Engineering', id, 'IND-ENG' FROM universities WHERE name = 'University of El Salvador'
UNION ALL
SELECT 'Architecture', id, 'ARCH' FROM universities WHERE name = 'University of El Salvador'
UNION ALL
SELECT 'Computer Systems Engineering', id, 'COMP-SYS' FROM universities WHERE name = 'University of El Salvador'
UNION ALL
SELECT 'Medicine', id, 'MED' FROM universities WHERE name = 'University of El Salvador'
ON CONFLICT DO NOTHING;

INSERT INTO graduates (student_id, first_name, last_name, identity_document, admission_year, graduation_year, total_years, gpa, career_id, verified)
SELECT 'EX12345', 'Carlos', 'Lopez', '12345678-9', 2018, 2022, 4, 8.75, c.id, TRUE FROM careers c WHERE c.code = 'SYS-ENG'
UNION ALL
SELECT 'EX12346', 'Maria', 'Garcia', '98765432-1', 2017, 2021, 4, 9.25, c.id, TRUE FROM careers c WHERE c.code = 'IND-ENG'
UNION ALL
SELECT 'VH17009', 'Mauricio', 'Hernandez', '22222222-2', 2017, 2025, 8, 7.25, c.id, TRUE FROM careers c WHERE c.code = 'COMP-SYS'
UNION ALL
SELECT 'EV18006', 'Nilson', 'Escobar', '33333333-3', 2018, 2024, 6, 9.15, c.id, TRUE FROM careers c WHERE c.code = 'SYS-ENG'
ON CONFLICT (student_id) DO NOTHING;






ALTER TABLE messages
    ADD COLUMN IF NOT EXISTS reply_to_id BIGINT REFERENCES messages(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS attachment_url TEXT,     -- Para fotos/docs
    ADD COLUMN IF NOT EXISTS attachment_type VARCHAR(50), -- 'IMAGE', 'PDF', 'VOICE'
    ADD COLUMN IF NOT EXISTS is_edited BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE, -- Borrado lógico
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'SENT'; -- SENT, DELIVERED, READ


ALTER TABLE conversation_participants
    ADD COLUMN IF NOT EXISTS is_muted BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS is_pinned BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS is_archived BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS unread_count INT DEFAULT 0;




CREATE INDEX IF NOT EXISTS idx_messages_reply_to ON messages(reply_to_id);









CREATE TABLE IF NOT EXISTS read_receipts (
                                             id BIGSERIAL PRIMARY KEY,
                                             message_id BIGINT NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
                                             user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                             conversation_id BIGINT NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
                                             read_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
                                             UNIQUE(message_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_read_receipts_message ON read_receipts(message_id);
CREATE INDEX IF NOT EXISTS idx_read_receipts_user_conv ON read_receipts(user_id, conversation_id);

CREATE TABLE IF NOT EXISTS user_public_keys (
                                                id BIGSERIAL PRIMARY KEY,
                                                user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                                public_key TEXT NOT NULL,
                                                created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
                                                updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
                                                UNIQUE(user_id)
);

ALTER TABLE messages ADD COLUMN IF NOT EXISTS is_encrypted BOOLEAN DEFAULT FALSE;
ALTER TABLE messages ADD COLUMN IF NOT EXISTS encrypted_content TEXT;
ALTER TABLE messages ADD COLUMN IF NOT EXISTS init_vector TEXT;
ALTER TABLE messages ADD COLUMN IF NOT EXISTS ephemeral_public_key TEXT;

CREATE INDEX IF NOT EXISTS idx_messages_conv_created ON messages(conversation_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_messages_conv_id ON messages(conversation_id, id DESC);



ALTER TABLE profiles
    ADD COLUMN IF NOT EXISTS display_name    VARCHAR(150),
    ADD COLUMN IF NOT EXISTS linkedin_url    VARCHAR(255),
    ADD COLUMN IF NOT EXISTS website_url     VARCHAR(255),
    ADD COLUMN IF NOT EXISTS address         VARCHAR(255),
    ADD COLUMN IF NOT EXISTS city            VARCHAR(100),
    ADD COLUMN IF NOT EXISTS country_id      BIGINT,
    ADD COLUMN IF NOT EXISTS privacy_settings JSONB;

-- Poblar display_name en perfiles existentes
UPDATE profiles
SET display_name = first_name || ' ' || last_name
WHERE display_name IS NULL;

-- ── Users: campos nuevos ──────────────────────────────────────
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS email_notification_enabled  BOOLEAN DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS push_notification_enabled   BOOLEAN DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS profile_completion_percentage INT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS has_disability              BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS disability_type             VARCHAR(30),
    ADD COLUMN IF NOT EXISTS disability_details          TEXT,
    ADD COLUMN IF NOT EXISTS deactivation_reason         TEXT,
    ADD COLUMN IF NOT EXISTS deactivated_at              TIMESTAMP;



CREATE TYPE verification_status_enum AS ENUM ('PENDING', 'IN_REVIEW', 'APPROVED', 'REJECTED');

CREATE TABLE user_verifications (
                                    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                    status verification_status_enum NOT NULL DEFAULT 'PENDING',
                                    verified_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
                                    name_match BOOLEAN DEFAULT FALSE,
                                    student_id_match BOOLEAN DEFAULT FALSE,
                                    document_match BOOLEAN DEFAULT FALSE,
                                    match_score INT DEFAULT 0,
                                    observations TEXT,
                                    started_at TIMESTAMP WITH TIME ZONE,
                                    resolved_at TIMESTAMP WITH TIME ZONE,
                                    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                    CONSTRAINT uk_user_verification UNIQUE (user_id)
);

CREATE INDEX idx_verification_status ON user_verifications(status);
CREATE INDEX idx_verification_user ON user_verifications(user_id);
CREATE INDEX idx_verification_verifier ON user_verifications(verified_by);






ALTER TABLE profiles ADD COLUMN IF NOT EXISTS admission_year INT;

CREATE UNIQUE INDEX idx_unique_survey_response ON survey_responses (survey_id, user_id) WHERE status = 'COMPLETED';








































