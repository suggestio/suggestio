import sbt._
import Keys._
import play.Project._
import java.io.File
//import com.jmparsons.plugin.LesscPlugin._

object ApplicationBuild extends Build {

  val appName         = "sioweb21"
  val appVersion      = "1.0-SNAPSHOT"

  // play-framework local SNAPSHOTs
  
  val appDependencies = {
    Seq(
      cache,      // play-2.2+
      json,       // play-2.3+
      ws,
      "com.typesafe" %% "play-plugins-mailer" % "2.2.0",
      "com.googlecode.owasp-java-html-sanitizer" % "owasp-java-html-sanitizer" % "r173", // html-фильтр для пользовательского контента.
      // io.suggest stuff
      "io.suggest" %% "util"      % "0.6.0-SNAPSHOT" changing() exclude("org.jruby", "jruby-complete"),
      "io.suggest" %% "util-play" % "0.6.0-SNAPSHOT" exclude("org.jruby", "jruby-complete"),
      // coffeescript-компилятор используем свой заместо компилятора play по ряду причин (последний прибит гвоздями к sbt-plugin, например).
      "org.jcoffeescript" % "jcoffeescript" % "1.6-SNAPSHOT",
      // for domain validation:
      "net.databinder.dispatch" %% "dispatch-core" % "0.10.0",
      "org.apache.httpcomponents" % "httpcore" % "4.1.3",
      "com.google.guava" % "guava" % "14.0.1",
      "org.erlang.otp" % "jinterface" % "1.5.6"              // Для импорт данных из sio v1 кластера. Выпилить вместе с util.compat.v1 после запуска
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

  // Компилировать эти less-файлы через Lessc вместо Rhino.
  //def customLessEntryPoints(base: File): PathFinder = (
  //    (base / "app" / "assets" / "stylesheets" * "*.less")
  //)

  val main = play.Project(appName, appVersion, appDependencies)
    //.settings(lesscSettings: _*)
    .settings(
      // резолверы определены цепочкой в этом конфиге:
      externalIvySettings(baseDirectory(_ / "project" / "ivysettings.xml")),

      // использовать lessc вместо встроенного less-компилятора.
      //lessEntryPoints := Nil,
      //lesscEntryPoints in Compile <<= baseDirectory(customLessEntryPoints)

      // Сжимать текстовые asset'ы при сборке проекта.
      gzippableAssets <<= (resourceManaged in (ThisProject))(dir => ((dir ** "*.js") +++ (dir ** "*.css"))),
      gzipAssetsSetting
    )
}

