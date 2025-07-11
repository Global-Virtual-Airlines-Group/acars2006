CREATE TABLE ROUTES (
 ID					INTEGER UNSIGNED NOT NULL AUTO_INCREMENT,
 AUTHOR				INTEGER UNSIGNED NOT NULL,
 AIRLINE			CHAR(3) NOT NULL,
 AIRPORT_D			CHAR(3) NOT NULL,
 AIRPORT_A			CHAR(3) NOT NULL,
 AIRPORT_L			CHAR(3),
 CREATEDON			DATETIME NOT NULL,
 LASTUSED			DATETIME,
 USED				SMALLINT UNSIGNED NOT NULL DEFAULT 0,
 ACTIVE				BOOLEAN NOT NULL DEFAULT TRUE,
 SID				VARCHAR(32),
 STAR				VARCHAR(32),
 ALTITUDE			VARCHAR(5) NOT NULL,
 BUILD				SMALLINT UNSIGNED NOT NULL,
 REMARKS			TEXT,
 ROUTE				TEXT NOT NULL,
 PRIMARY KEY (ID),
 FOREIGN KEY (AIRPORT_D) REFERENCES common.AIRPORTS(IATA) ON UPDATE CASCADE ON DELETE CASCADE,
 FOREIGN KEY (AIRPORT_A) REFERENCES common.AIRPORTS(IATA) ON UPDATE CASCADE ON DELETE CASCADE,
 FOREIGN KEY (AIRLINE) REFERENCES common.AIRLINES(CODE) ON UPDATE CASCADE
);
 