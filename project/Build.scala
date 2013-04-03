import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "sioweb21"
  val appVersion      = "1.0-SNAPSHOT"
  
  val appDependencies = {
    val esVsn     = "0.20.6"
    val hadoopVsn = "1.1.1"
    Seq(
      // Add your project dependencies here,
      jdbc,
      anorm,
      "org.elasticsearch" % "elasticsearch" % esVsn,
      "org.apache.hadoop" % "hadoop-core" % hadoopVsn,
      "org.apache.hadoop" % "hadoop-client" % hadoopVsn,
      // spat4j: костыль для http://elasticsearch-users.115913.n3.nabble.com/Compile-error-with-0-20-2-td4028743.html :
      "com.spatial4j" % "spatial4j" % "0.3"
    )
  }
 
  val main = play.Project(appName, appVersion, appDependencies).settings(
    // Add your own project settings here      
  )

}
