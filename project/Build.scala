import sbt._
import Keys._
import play.Project._
import java.io.File

object ApplicationBuild extends Build {

  val appName         = "sioweb21"
  val appVersion      = "1.0-SNAPSHOT"
  
  val appDependencies = {
    val esVsn         = "0.20.6"
    val hadoopVsn     = "1.1.1"
    val cascadingVsn  = "2.0.7"
    Seq(
      // Add your project dependencies here,
      jdbc,
      anorm,
      "org.elasticsearch" % "elasticsearch" % esVsn,
      "org.apache.hadoop" % "hadoop-core" % hadoopVsn,
      "org.apache.hadoop" % "hadoop-client" % hadoopVsn,
      // spat4j: костыль для http://elasticsearch-users.115913.n3.nabble.com/Compile-error-with-0-20-2-td4028743.html :
      "com.spatial4j" % "spatial4j" % "0.3",
      "io.suggest" %% "util" % "0.1",
      // hadoop
      "org.apache.hadoop" % "hadoop-core" % hadoopVsn,
      "org.apache.hadoop" % "hadoop-client" % hadoopVsn,
      // cascading
      "cascading" % "cascading-core" % cascadingVsn,
      "cascading" % "cascading-hadoop" % cascadingVsn,
      "com.scaleunlimited" % "cascading.utils" % "2.1-SNAPSHOT",
      // akka - for siobix direct calls
      "com.typesafe.akka" %% "akka-actor" % "2.1.0"
    )
  }
 
  val main = play.Project(appName, appVersion, appDependencies).settings(
    
    resolvers ++= Seq(
      "local m2" at   "file://" + Path.userHome.absolutePath + "/.m2/repository",
      Resolver.file("LocalIvy", file(Path.userHome + File.separator + ".ivy2" + File.separator + "local"))(Resolver.ivyStylePatterns)
    )

  )

}
