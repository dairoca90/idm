/*
 * Copyright 2010-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */


import java.sql.Connection

import org.forgerock.openicf.connectors.groovy.OperationType
import org.identityconnectors.common.logging.Log
import org.identityconnectors.framework.common.exceptions.ConnectorException

import groovy.sql.Sql

// Parameters:
// The connector sends us the following:
// connection : SQL connection
// action: String correponding to the action ("RUNSCRIPTONCONNECTOR" here)
// log: a handler to the Log facility
// options: a handler to the OperationOptions Map
//
// Arguments can be passed to the script in the REST call, e.g.:
//
// curl -k --header "X-OpenIDM-subscribername: openidm-admin" \
// --header "X-OpenIDM-Password: openidm-admin" \
// --header "Content-Type: application/json" \
// --request POST "https://localhost:8443/openidm/system/subscribersdb?_action=script&scriptId=ResetDatabase" \
// -d "{\"arg1\":\"foo\",\"arg2\":\"bar\"}"
//
// These arguments can be accessed here by name, e.g.
//
// def firstArg = arg1 as String;
//
// Note that these can be complex types; Arguments are passed in as Object type.

def operation = operation as OperationType
def connection = connection as Connection
def sql = new Sql(connection);
def log = log as Log

log.info("Entering " + operation + " Script");

// Create and use the db if it's not present and clear out old tables if they exist
try {
    try {
        sql.execute("CREATE DATABASE IF NOT EXISTS subscribersdb CHARACTER SET utf8 COLLATE utf8_bin;")
        sql.execute("USE subscribersdb;")
    } catch (Exception e) {
    }

    sql.execute("DROP TABLE IF EXISTS movie;")
    sql.execute("DROP TABLE IF EXISTS groups_subscribers;")
    sql.execute("DROP TABLE IF EXISTS subscribers;")
    sql.execute("DROP TABLE IF EXISTS groups;")
    sql.execute("DROP TABLE IF EXISTS subscriptions;")
} catch (Exception e) {
}

// Create our tables
sql.execute("""
CREATE TABLE subscribers(
        id int(11) NOT NULL AUTO_INCREMENT PRIMARY KEY,
        uid char(32) NOT NULL,
        password char(128),
        firstname varchar(32) NOT NULL DEFAULT '',
        lastname varchar(32) NOT NULL DEFAULT '',
        fullname varchar(32),
        email varchar(128),
        subscription varchar(32),
        timestamp timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
""")

sql.execute("""
CREATE TABLE movie(
        subscribers_id int(11) NOT NULL,
        year varchar(4) NOT NULL,
        title varchar(32) NOT NULL,
        description varchar(255) NOT NULL,
        rating varchar(32) NOT NULL,
        FOREIGN KEY (subscribers_id) REFERENCES subscribers(id) ON DELETE CASCADE
);
""")

sql.execute("""
CREATE TABLE groups(
        id int(11) NOT NULL AUTO_INCREMENT PRIMARY KEY,
        gid char(32) NOT NULL,
        name varchar(32) NOT NULL DEFAULT '',
        description varchar(32),
        timestamp timestamp DEFAULT now()
);
""")

sql.execute("""
CREATE TABLE groups_subscribers(
        subscribers_id int(11) NOT NULL,
        groups_id int(11) NOT NULL,
        FOREIGN KEY (subscribers_id) REFERENCES subscribers(id) ON DELETE CASCADE,
        FOREIGN KEY (groups_id) REFERENCES groups(id) ON DELETE CASCADE
);
""")

sql.execute("""
CREATE TABLE subscriptions(
        id int(11) NOT NULL AUTO_INCREMENT PRIMARY KEY,
        name varchar(32) NOT NULL DEFAULT '',
        description varchar(32),
        timestamp timestamp DEFAULT now()
);
""")

// Now populate the tables
sql.execute("""
INSERT INTO subscribers
( uid, password, firstname, lastname, fullname, email, subscription, timestamp )
VALUES
("bfleming",sha1("Welcome1"),"Bob", "Fleming","Bob Fleming","Bob.Fleming@example.com","Premium",CURRENT_TIMESTAMP),
("scarter",sha1("Welcome1"),"Sam", "Carter","Sam Carter","Sam.Carter@example.com","Stardard",CURRENT_TIMESTAMP),
("rbirkin",sha1("Welcome1"),"Rowley","Birkin","Rowley Birkin","Rowley.Birkin@example.com","Premium",CURRENT_TIMESTAMP),
("lbalfour",sha1("Welcome1"),"Louis", "Balfour","Louis Balfour","Louis.Balfor@example.com","Premium",CURRENT_TIMESTAMP),
("jsmith",sha1("Welcome1"),"John", "Smith","John Smith","John.Smith@example.com","Premium",CURRENT_TIMESTAMP),
("jdoe",sha1("Welcome1"),"John", "Doe","John Doe","John.Doe@example.com","Premium",CURRENT_TIMESTAMP);
""")

sql.execute("""
INSERT INTO movie (subscribers_id,year,title,description,rating) VALUES
(1,"1979","Gone with the Wined","Story about a couple that gets lost at sea after the drinking too much wine.","PG-13"),
(2,"2013","Running Wild","Documentary of lions on the plains of Africa", "G"),
(2,"2010","Java Spills the Beans","Spy movie with too much caffine", "PG-13"),
(3,"2001","Grasping at Straws","A desparate attempt by an amatuer producer to name movies.", "G"),
(4,"2009","Hanns Anderssen and the Who","A dreamer musician who fantasies playing lead guitar for a well-known band called the who. Who was that band?", "PG-13"),
(4,"2011","Godzilla Meets Mozilla","An animated epic battle between two foes.", "G"),
(5,"1987","Zillarious Starts with a Z","A funny look at the letter zed.", "PG"),
(6,"1927","Chaplin and Me","A story of a child that meets Charlie.", "PG");
""")

sql.execute("""
INSERT INTO groups VALUES ("0","100","admin","Admin group",CURRENT_TIMESTAMP);
""")

sql.execute("""
INSERT INTO groups VALUES ("0","101","subscribers","subscribers group",CURRENT_TIMESTAMP);
""")

sql.execute("""
INSERT INTO groups_subscribers (subscribers_id, groups_id) SELECT id, 1 FROM subscribers where subscription='Trial';
""")

sql.execute("""
INSERT INTO groups_subscribers (subscribers_id, groups_id) SELECT id, 2 FROM subscribers where subscription <> 'Trial';
""")

sql.execute("""
INSERT INTO subscriptions VALUES ("0","Trial","Trial subscription",CURRENT_TIMESTAMP);
""")

sql.execute("""
INSERT INTO subscriptions VALUES ("0","Standard","Standard subscription",CURRENT_TIMESTAMP);
""")

sql.execute("""
INSERT INTO subscriptions VALUES ("0","Premium","Premium subscription",CURRENT_TIMESTAMP);
""")

sql.execute("""
INSERT INTO subscriptions VALUES ("0","Experimental","Experimental subscription",CURRENT_TIMESTAMP);
""")

//sql.execute("grant all on subscribersdb.* to root@'%' IDENTIFIED BY 'cangetin';")

// do a query to check it all worked ok
//def results = sql.firstRow("select firstname, lastname from subscribers where id=1").firstname
//def expected = "Bob"

//if (results != expected) {
//    throw new ConnectorException("Reset of subscribersdb was not successful");
//}

return "Database reset of subscribersdb successful."
