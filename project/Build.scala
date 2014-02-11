import sbt._
import Keys._
import play.Project._
import java.io.File
import patience.assets.StylusPlugin.stylusSettings

object ApplicationBuild extends Build {

  val appName         = "sioweb21"
  val appVersion      = "1.0-SNAPSHOT"

  // play-framework local SNAPSHOTs
  
  val appDependencies = {
    Seq(
      jdbc,
      anorm,
      cache,      // play-2.2+
      json,       // play-2.3+
      ws,
      "org.postgresql" % "postgresql" % "9.3-1100-jdbc41",
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

  // Проект энтерпрайза.
  lazy val sioweb21 = play.Project(
    name = appName,
    applicationVersion = appVersion,
    dependencies = appDependencies,
    settings = stylusSettings ++ Seq(
      // Удалить из резолверов исходный резолвер play чтобы можно было задействовать кэш artifactory.
      resolvers ~= {
        rs => rs filter {_.name != "Typesafe Releases Repository" }
      },
      // Добавить резолверы, в т.ч. кэш-резолвер для отфильтрованной выше репы.
      resolvers ++= Seq(
        "typesafe-releases" at "https://ivy2-internal.cbca.ru/artifactory/typesafe-releases",
        "sonatype-oss-releases" at "https://ivy2-internal.cbca.ru/artifactory/sonatype-oss-snapshots",
        "apache-releases" at "https://ivy2-internal.cbca.ru/artifactory/apache-releases",
        "conjars-repo" at "https://ivy2-internal.cbca.ru/artifactory/conjars-repo",
        "maven-twttr-com" at "https://ivy2-internal.cbca.ru/artifactory/maven-twttr-com"
      ),
      // Сжимать текстовые asset'ы при сборке проекта.
      gzippableAssets <<= (resourceManaged in ThisProject)(dir => ((dir ** "*.js") +++ (dir ** "*.css"))),
      gzipAssetsSetting
    )
  )

}

