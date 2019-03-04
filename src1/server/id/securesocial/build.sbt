Common.settingsOrg

name := "securesocial"

version := "3.4.0sio"


//generateRefReverseRouter := false

libraryDependencies ++= Seq(
  cacheApi,
  ws,
  "com.typesafe.play" %% "filters-helpers" % Common.playVsn
)

resolvers ++= Seq(
  Resolver.typesafeRepo("releases")
)

organizationName := "SecureSocial"

organizationHomepage := Some(new URL("http://www.securesocial.ws"))

startYear := Some(2012)

description := "An authentication module for Play Framework applications supporting OAuth, OAuth2, OpenID, Username/Password and custom authentication schemes."

licenses := Seq("The Apache Software License, Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))

scalacOptions := Seq("-encoding", "UTF-8", "-Xlint", "-deprecation", "-unchecked", "-feature")

// not adding -Xlint:unchecked for now, will do it once I improve the Java API
javacOptions ++= Seq("-source", "1.6", "-target", "1.6", "-encoding", "UTF-8",  "-Xlint:-options")

//packagedArtifacts += ((artifact in playPackageAssets).value -> playPackageAssets.value)

