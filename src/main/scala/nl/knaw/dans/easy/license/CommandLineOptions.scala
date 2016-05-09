/**
 * Copyright (C) 2016 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.easy.license

import java.io.File
import javax.naming.Context
import javax.naming.ldap.InitialLdapContext

import com.yourmediashelf.fedora.client.FedoraCredentials
import org.apache.commons.configuration.PropertiesConfiguration
import org.rogach.scallop._
import org.slf4j.LoggerFactory

class CommandLineOptions(args: Array[String]) extends ScallopConf(args) {
  import CommandLineOptions.log

  val fileMayNotExist = singleArgConverter(new File(_))
  val fileShouldExist = singleArgConverter(filename => {
    val file = new File(filename)
    if (!file.exists) {
      log.error(s"The directory '$filename' does not exist")
      throw new IllegalArgumentException(s"The directory '$filename' does not exist")
    }
    if (!file.isDirectory) {
      log.error(s"'$filename' is not a directory")
      throw new IllegalArgumentException(s"'$filename' is not a directory")
    }
    else file
  })

  printedName = "easy-license-creator"

  version(s"$printedName v${Version()}")
  banner(s"""
           |Create a license for the given datasetID and userID. The latter is optional as it can also
           | be retrieved from the datasetID. The license will be saved at the indicated location.
           |
           |Usage:
           |
           |$printedName -u <userID> <datasetID> <template-dir> <license-file>
           |
           |Options:
           |""".stripMargin)

  val userID = opt[UserID](name = "user-id", short = 'u',
    descr = "The userID of the depositor of this dataset")

  val datasetID = trailArg[DatasetID](name = "dataset-id",
    descr = "The ID of the dataset of which a license has to be created")

  val templateDir = trailArg[File](name = "template-dir", required = true,
    descr = "Directory containing the template components for the license.")(fileShouldExist)
  validateOpt(templateDir)(_.map(file =>
    if (!file.isDirectory)
      Left(s"Not a directory '$file'")
    else if (file.directoryContains(new File("license")) && new File(file, "license").isDirectory)
      Left(s"No subdirectory 'license' found in ${file.getAbsolutePath}")
    else if (file.directoryContains(new File("velocity-engine.properties")))
      Left(s"No properties file found in $file")
    else
      Right(()))
    .getOrElse(Left("Could not parse parameter template-dir")))

  val outputFile = trailArg[File](name = "license-file",
    descr = "The file location where the license needs to be stored")(fileMayNotExist)
}

object CommandLineOptions {

  val log = LoggerFactory.getLogger(getClass)

  def parse(args: Array[String]): Parameters = {
    log.debug("Loading application properties ...")
    val homeDir = new File(System.getProperty("app.home"))
    val props = {
      val ps = new PropertiesConfiguration()
      ps.setDelimiterParsingDisabled(true)
      ps.load(new File(homeDir, "cfg/application.properties"))

      ps
    }

    log.debug("Parsing command line ...")
    val opts = new CommandLineOptions(args)

    val params = Parameters(
      appHomeDir = homeDir,
      templateDir = opts.templateDir(),
      outputFile = opts.outputFile(),
      userID = opts.userID.get,
      datasetID = opts.datasetID(),
      fedora = new FedoraCredentials(
        props.getString("fcrepo.url"),
        props.getString("fcrepo.user"),
        props.getString("fcrepo.password")),
      ldap = {
        import java.{util => ju}

        val env = new ju.Hashtable[String, String]
        env.put(Context.PROVIDER_URL, props.getString("auth.ldap.url"))
        env.put(Context.SECURITY_AUTHENTICATION, "simple")
        env.put(Context.SECURITY_PRINCIPAL, props.getString("auth.ldap.user"))
        env.put(Context.SECURITY_CREDENTIALS, props.getString("auth.ldap.password"))
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory")

        new InitialLdapContext(env, null)
      })

    log.debug(s"Using the following settings: $params")

    params
  }
}
