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
import org.identityconnectors.framework.common.objects.Attribute
import org.identityconnectors.framework.common.objects.AttributesAccessor
import org.identityconnectors.framework.common.objects.ObjectClass
import org.identityconnectors.framework.common.objects.Uid

import groovy.sql.Sql

/**
 * Built-in accessible objects
 **/

// OperationType is CREATE for this script
def operation = operation as OperationType

// The configuration class created specifically for this connector
def configuration = configuration as ScriptedSQLConfiguration

// Default logging facility
def log = log as Log

// Set of attributes describing the object to be created
def createAttributes = new AttributesAccessor(attributes as Set<Attribute>)

// The Uid of the object to be created, usually null indicating the Uid should be generated
def uid = id as String

// The objectClass of the object to be created, e.g. ACCOUNT or GROUP
def objectClass = objectClass as ObjectClass

/**
 * Script action - Customizable
 *
 * Create a new object in the external source.  Connectors that do not support this should
 * throw an UnsupportedOperationException.
 *
 * This script should return a Uid object that represents the ID of the newly created object
 **/

/* Log something to demonstrate this script executed */
log.info("Create script, operation = " + operation.toString());

def ORG = new ObjectClass("subscription")
def connection = connection as Connection
def sql = new Sql(connection);

switch (objectClass) {
    case ObjectClass.ACCOUNT:
        def retUid
        def generatedKeys = sql.executeInsert(
                "INSERT INTO subscribers (uid,password,firstname,lastname,fullname,email,subscription) values (?,sha1(?),?,?,?,?,?)",
                [
                        uid,
                        createAttributes.hasAttribute("password") ? createAttributes.findString("firstname") : "",
                        createAttributes.hasAttribute("firstname") ? createAttributes.findString("firstname") : "",
                        createAttributes.hasAttribute("lastname") ? createAttributes.findString("lastname") : "",
                        createAttributes.hasAttribute("fullname") ? createAttributes.findString("fullname") : "",
                        createAttributes.hasAttribute("email") ? createAttributes.findString("email") : "",
                        createAttributes.hasAttribute("subscription") ? createAttributes.findString("subscription") : ""
                ]
        )
        retUid = new Uid(generatedKeys[0][0] as String)

        createAttributes.findList("movies").each {
            sql.executeInsert(
                    "INSERT INTO movie (subscribers_id,year,title,description,rating) VALUES (?,?,?,?,?)",
                    [
                            generatedKeys[0][0] as Integer,
                            it.year,
                            it.title,
                            it.description,
                            it.rating
                    ]
            )
        }

        return retUid
        break

    case ObjectClass.GROUP:
        def retUid
        def generatedKeys = sql.executeInsert(
                "INSERT INTO groups (name,gid,description) values (?,?,?)",
                [
                        uid,
                        createAttributes.hasAttribute("gid") ? createAttributes.findString("gid") : "",
                        createAttributes.hasAttribute("description") ? createAttributes.findString("description") : "",
                ]
        )
        retUid = new Uid(generatedKeys[0][0] as String)

        createAttributes.findList("subscribers").each {
            sql.executeInsert(
                    "INSERT INTO groups_subscribers (subscribers_id,groups_id) SELECT id,? FROM subscribers WHERE uid=?",
                    [
                            generatedKeys[0][0] as Integer,
                            it.uid
                    ]
            )
        }

        return retUid
        break

    case ORG:
        def generatedKeys = sql.executeInsert(
                "INSERT INTO subscriptions (name ,description) values (?,?)",
                [
                        uid,
                        createAttributes.hasAttribute("description") ? createAttributes.findString("description") : ""
                ]
        )
        return new Uid(generatedKeys[0][0] as String)

        break

    default:
        throw new UnsupportedOperationException(operation.name() + " operation of type:" +
                objectClass.objectClassValue + " is not supported.")
}
