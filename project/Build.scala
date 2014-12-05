import sbt._, Keys._
import play._
import com.typesafe.sbt.web._

object ApplicationBuild extends Build {
  
  lazy val web21 = (project in file("."))
    .enablePlugins(PlayScala, SbtWeb)

}

