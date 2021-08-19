/*
 * Copyright 2015-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
package org.forgerock.openicf.connectors.subscribersdb

import java.sql.Connection

import org.forgerock.openicf.connectors.groovy.OperationType
import org.forgerock.openicf.connectors.scriptedsql.ScriptedSQLConfiguration
import org.identityconnectors.common.logging.Log
import org.identityconnectors.common.security.GuardedString
import org.identityconnectors.common.security.SecurityUtil
import org.identityconnectors.framework.common.exceptions.InvalidPasswordException

import groovy.sql.Sql

/**
 * Built-in accessible objects
 **/

// OperationType is AUTHENTICATION for this script
def operation = operation as OperationType

// The configuration class created specifically for this connector
def configuration = configuration as ScriptedSQLConfiguration

// Default logging facility
def log = log as Log

/**
 * Script action - Customizable
 *
 * Must either return an int or String convertible to a Uid object or throw an exception
 **/

def authId = null;

/**
 * Params not generated custom connector tool
 **/

// subscribername used in the sql query
def subscribername = subscribername as String

// password used in the sql query
def password = password as GuardedString;

// connection used for connecting to the SQL repo
def connection = connection as Connection

log.info("Entering " + operation + " Script");

// create connection to SQL
def sql = new Sql(connection);

// do select with provided, subscribername/password
sql.eachRow("SELECT id FROM subscribers WHERE uid = ? AND password = sha1(?)", [subscribername, SecurityUtil.decrypt(password)]) {
    authId = String.valueOf(it.id)
}

// check if subscriber was returned..would imply authenticated
if (authId == null) {
    throw new InvalidPasswordException("Authentication Failed")
}

return authId
