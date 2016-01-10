# --- First database schema

# --- !Ups

CREATE TABLE OAuth1Info (
    id SERIAL PRIMARY KEY,
    token VARCHAR(255) NOT NULL,
    secret VARCHAR(255) NOT NULL
);

CREATE TABLE OAuth2Info (
    id SERIAL PRIMARY KEY,
    accessToken VARCHAR(255) NOT NULL,
    tokenType VARCHAR(255),
    expiresIn BIGINT,
    refreshToken VARCHAR(255)
);

CREATE TABLE PasswordInfo (
    id SERIAL PRIMARY KEY,
    hasher VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    salt VARCHAR(255) NULL
);

CREATE TABLE UserProfile (
    id SERIAL PRIMARY KEY,
    userId VARCHAR(255) NOT NULL,
    providerId VARCHAR(255) NOT NULL,
    firstName VARCHAR(255) NULL,
    lastName VARCHAR(255) NULL,
    email VARCHAR(255) NULL,
    avatarUrl VARCHAR(255) NULL,
    authMethod VARCHAR(12) NULL,

    oAuth1InfoId BIGINT NULL REFERENCES OAuth1Info(id),
    oAuth2InfoId BIGINT NULL REFERENCES OAuth2Info(id),
    passwordInfoId BIGINT NULL REFERENCES PasswordInfo(id)
  );

# --- !Downs

DROP TABLE IF EXISTS UserProfile;
DROP TABLE IF EXISTS PasswordInfo;
DROP TABLE IF EXISTS OAuth2Info;
DROP TABLE IF EXISTS OAuth1Info;
