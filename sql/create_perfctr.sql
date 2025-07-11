CREATE TABLE PERFCOUNTER (
 ID                 INTEGER UNSIGNED NOT NULL,
 NAME               VARCHAR(24) NOT NULL,
 VALUE              INTEGER UNSIGNED NOT NULL DEFAULT 0,
 PRIMARY KEY (ID, NAME),
 FOREIGN KEY (ID) REFERENCES FLIGHTS(ID) ON DELETE CASCADE ON UPDATE CASCADE
) CHARACTER SET latin1;