# --- First database schema

# --- !Ups

CREATE TABLE OAuth1Info (
    userId SERIAL PRIMARY KEY,
    token VARCHAR(255) NOT NULL,
    secret VARCHAR(255) NOT NULL
);

CREATE TABLE OAuth2Info (
    id SERIAL PRIMARY KEY,
    accessToken VARCHAR(255) NOT NULL,
    tokenType VARCHAR(255),
    expiresIn BIGINT UNSIGNED,
    refreshToken VARCHAR(255)
);

CREATE TABLE PasswordInfo (
    id SERIAL PRIMARY KEY,
    hasher VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    salt VARCHAR(255) NULL
);

CREATE TABLE UserProfile (
    userId SERIAL PRIMARY KEY,
    providerId VARCHAR(255) NOT NULL,
    firstName VARCHAR(255) NULL,
    lastName VARCHAR(255) NULL,
    email VARCHAR(255) NULL,
    avatarUrl VARCHAR(255) NULL,
    authMethod VARCHAR(12) NULL,

    oAuth1InfoId BIGINT UNSIGNED NULL,
    oAuth2InfoId BIGINT UNSIGNED NULL,
    passwordInfoId BIGINT UNSIGNED NULL,

    FOREIGN KEY (oAuth1InfoId) REFERENCES OAuth1Info(id)
    FOREIGN KEY (oAuth2InfoId) REFERENCES OAuth2Info(id)
    FOREIGN KEY (passwordInfoId) REFERENCES PasswordInfo(id)
);

# --- !Downs

DROP TABLE IF EXISTS UserProfile;
DROP TABLE IF EXISTS PasswordInfo;
DROP TABLE IF EXISTS OAuth2Info;
DROP TABLE IF EXISTS OAuth1Info;
