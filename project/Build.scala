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
 

  // Gzip app/assets using gzip-assets sbt command
  // https://groups.google.com/forum/#!msg/play-framework/dSJEKhYiMDE/u-N50plw6hwJ
  val gzippableAssets = SettingKey[PathFinder]("gzippable-assets", "Defines the files to gzip")

  // custom task to gzip static assets, will be available from the Assets controller
  // when run in prod mode if the client accepts gzip
  val gzipAssets = TaskKey[Seq[File]]("gzip-assets", "GZIP all assets")
  lazy val gzipAssetsSetting = gzipAssets <<= gzipAssetsTask
  lazy val gzipAssetsTask = (gzippableAssets, streams) map {
    case (finder: PathFinder, s: TaskStreams) => {
      finder.get.map { file =>
        val gzTarget = new File(file.getAbsolutePath + ".gz")
        IO.gzip(file, gzTarget)
        s.log.info("Compressed " + file.getName + " " + file.length / 1000 + " k => " + gzTarget.getName + " " + gzTarget.length / 1000 + " k")
        gzTarget
      }
    }
  }


  val main = play.Project(appName, appVersion, appDependencies).settings(
    
    resolvers ++= Seq(
      "local m2" at   "file://" + Path.userHome.absolutePath + "/.m2/repository",
      Resolver.file("LocalIvy", file(Path.userHome + File.separator + ".ivy2" + File.separator + "local"))(Resolver.ivyStylePatterns)
    ),
    gzippableAssets <<= (resourceManaged in (ThisProject))(dir => ((dir ** "*.js") +++ (dir ** "*.css"))),
    gzipAssetsSetting

  )

}

