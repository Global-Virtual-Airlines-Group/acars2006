CREATE TABLE BANDWIDTH (
 PERIOD					DATETIME NOT NULL,
 DURATION				INTEGER UNSIGNED NOT NULL DEFAULT 1,
 CONS					SMALLINT UNSIGNED NOT NULL DEFAULT 0,
 BYTES_IN				INTEGER UNSIGNED NOT NULL DEFAULT 0,
 BYTES_OUT				INTEGER UNSIGNED NOT NULL DEFAULT 0,
 MSGS_IN				INTEGER UNSIGNED NOT NULL DEFAULT 0,
 MSGS_OUT				INTEGER UNSIGNED NOT NULL DEFAULT 0,
 PEAK_CONS				INTEGER UNSIGNED NOT NULL DEFAULT 0,
 PEAK_BYTES				INTEGER UNSIGNED NOT NULL DEFAULT 0,
 PEAK_MSGS				INTEGER UNSIGNED NOT NULL DEFAULT 0,
 ERRORS					SMALLINT UNSIGNED NOT NULL DEFAULT 0,
 BYTES_SAVED            INTEGER NOT NULL DEFAULT 0,
 PRIMARY KEY (PERIOD)
);