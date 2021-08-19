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
import org.identityconnectors.framework.common.exceptions.ConnectorException
import org.identityconnectors.framework.common.objects.Attribute
import org.identityconnectors.framework.common.objects.AttributesAccessor
import org.identityconnectors.framework.common.objects.ObjectClass
import org.identityconnectors.framework.common.objects.Uid

import groovy.sql.Sql

/**
 * Built-in accessible objects
 **/

// OperationType is UPDATE for this script
def operation = operation as OperationType

// The configuration class created specifically for this connector
def configuration = configuration as ScriptedSQLConfiguration

// Default logging facility
def log = log as Log

// Set of attributes describing the object to be updated
def updateAttributes = new AttributesAccessor(attributes as Set<Attribute>)

// The Uid of the object to be updated
def uid = uid as Uid

// The objectClass of the object to be updated, e.g. ACCOUNT or GROUP
def objectClass = objectClass as ObjectClass

/**
 * Script action - Customizable
 *
 * Update an object in the external source.  Connectors that do not support this should
 * throw an UnsupportedOperationException.
 *
 * This script should return the Uid of the updated object
 **/

/* Log something to demonstrate this script executed */
log.info("Create script, operation = " + operation.toString())

def connection = connection as Connection
def sql = new Sql(connection)
def ORG = new ObjectClass("subscription")

switch (operation) {
    case OperationType.UPDATE:
        switch (objectClass) {
            case ObjectClass.ACCOUNT:
                // Prepare statement and params for subscribers UPDATE
                def updateParams = []
                def updateStatement = "UPDATE subscribers SET"

                if (updateAttributes.hasAttribute("fullname")) {
                    updateStatement += " fullname = ?,"
                    updateParams.push(updateAttributes.findString("fullname"))
                }
                if (updateAttributes.hasAttribute("firstname")) {
                    updateStatement += " firstname = ?,"
                    updateParams.push(updateAttributes.findString("firstname"))
                }
                if (updateAttributes.hasAttribute("lastname")) {
                    updateStatement += " lastname = ?,"
                    updateParams.push(updateAttributes.findString("lastname"))
                }
                if (updateAttributes.hasAttribute("email")) {
                    updateStatement += " email = ?,"
                    updateParams.push(updateAttributes.findString("email"))
                }
                if (updateAttributes.hasAttribute("subscription")) {
                    updateStatement += " subscription = ?,"
                    updateParams.push(updateAttributes.findString("subscription"))
                }
                if (updateAttributes.hasAttribute("password")) {
                    updateStatement += " password = coalesce(sha1(?), password),"
                    updateParams.push(updateAttributes.findString("password"))
                }

                if (updateParams.size() > 0) {
                    updateStatement += " timestamp = now() WHERE id = ?"
                    updateParams.push(uid.uidValue)

                    // Execute UPDATE for subscribers
                    sql.executeUpdate("${updateStatement}", updateParams)
                }

                // Execute UPDATE for movie if 'movies' attribute exists
                if (updateAttributes.hasAttribute("movies")) {
                    sql.executeUpdate("DELETE FROM movie WHERE subscribers_id=?",
                            [
                                    uid.uidValue
                            ]
                    )
                    updateAttributes.findList("movies").each {
                        sql.executeInsert(
                                "INSERT INTO movie (subscribers_id,year,title,description,rating) VALUES (?,?,?,?,?)",
                                [
                                        uid.uidValue,
                                        it.year,
                                        it.title,
                                        it.description,
                                        it.rating
                                ]
                        )
                    }
                }
                break
            case ObjectClass.GROUP:
                // Prepare statement and params for groups UPDATE
                def updateStatement = "UPDATE groups SET"
                def updateParams = []

                if (updateAttributes.hasAttribute("description")) {
                    updateStatement += " description = ?,"
                    updateParams.push(updateAttributes.findString("description"))
                }
                if (updateAttributes.hasAttribute("gid")) {
                    updateStatement += " gid = ?,"
                    updateParams.push(updateAttributes.findString("gid"))
                }

                if (updateParams.size() > 0) {
                    updateStatement += " timestamp = now() WHERE id = ?"
                    updateParams.push(uid.uidValue)

                    // Execute UPDATE for groups
                    sql.executeUpdate("${updateStatement}", updateParams)
                }

                // Execute UPDATE for groups_subscribers if 'subscribers' attribute exists
                if (updateAttributes.hasAttribute("subscribers")) {
                    sql.executeUpdate("DELETE FROM groups_subscribers WHERE groups_id=?",
                            [
                                    uid.uidValue
                            ]
                    )
                    updateAttributes.findList("subscribers").each {
                        sql.executeInsert(
                                "INSERT INTO groups_subscribers (subscribers_id,groups_id) SELECT id,? FROM subscribers WHERE id=?",
                                [
                                        uid.uidValue,
                                        it.uid
                                ]
                        )
                    }
                }
                break
            case ORG:
                // If the only attribute other than _NAME_ is present
                if (updateAttributes.hasAttribute("description")) {
                    // Execute UPDATE for Subscriptions
                    sql.executeUpdate("""
                        UPDATE
                            Subscriptions
                        SET
                            description = ?,
                            timestamp = now()
                        WHERE
                            id = ?
                        """,
                            [
                                    updateAttributes.findString("description"),
                                    uid.uidValue
                            ]
                    )
                }
                break
            default:
                throw new ConnectorException("UpdateScript can not handle object type: " + objectClass.objectClassValue)
        }
        return uid.uidValue
    case OperationType.ADD_ATTRIBUTE_VALUES:
        throw new UnsupportedOperationException(operation.name() + " operation of type:" +
                objectClass.objectClassValue + " is not supported.")
    case OperationType.REMOVE_ATTRIBUTE_VALUES:
        throw new UnsupportedOperationException(operation.name() + " operation of type:" +
                objectClass.objectClassValue + " is not supported.")
    default:
        throw new ConnectorException("UpdateScript can not handle operation:" + operation.name())
}
