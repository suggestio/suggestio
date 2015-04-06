import play.PlayImport.PlayKeys._
import play.sbtplugin.routes.RoutesKeys._

name := "securesocial"

organization := "io.suggest"

version := "3.4.0-SNAPSHOT"

scalaVersion := "2.11.6"


//generateRefReverseRouter := false

libraryDependencies ++= Seq(
  cache,
  ws,
  "com.typesafe.play" %% "play-mailer" % "3.0.0-SNAPSHOT",
  "com.typesafe.play" %% "filters-helpers" % "2.4.0-M3",
  "org.mindrot" % "jbcrypt" % "0.3m",
  "org.specs2" %% "specs2" % "2.3.12" % "test",
  "org.mockito" % "mockito-all" % "1.9.5" % "test"
)

resolvers ++= Seq(
  Resolver.typesafeRepo("releases")
)

organizationName := "SecureSocial"

organizationHomepage := Some(new URL("http://www.securesocial.ws"))

startYear := Some(2012)

description := "An authentication module for Play Framework applications supporting OAuth, OAuth2, OpenID, Username/Password and custom authentication schemes."

licenses := Seq("The Apache Software License, Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))

homepage := Some(url("http://www.securesocial.ws"))

pomExtra := (
  <scm>
    <url>https://github.com/jaliss/securesocial</url>
    <connection>scm:git:git@github.com:jaliss/securesocial.git</connection>
    <developerConnection>scm:git:https://github.com/jaliss/securesocial.git</developerConnection>
  </scm>
  <developers>
    <developer>
      <id>jaliss</id>
      <name>Jorge Aliss</name>
      <email>jaliss [at] gmail.com</email>
      <url>https://twitter.com/jaliss</url>
    </developer>
  </developers>
)

scalacOptions := Seq("-encoding", "UTF-8", "-Xlint", "-deprecation", "-unchecked", "-feature")

// not adding -Xlint:unchecked for now, will do it once I improve the Java API
javacOptions ++= Seq("-source", "1.6", "-target", "1.6", "-encoding", "UTF-8",  "-Xlint:-options")

//packagedArtifacts += ((artifact in playPackageAssets).value -> playPackageAssets.value)

