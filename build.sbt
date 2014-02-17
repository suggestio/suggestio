organization := "io.suggest"

name := "sioweb21"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,      // play-2.2+
  json,       // play-2.3+
  ws,
  "org.postgresql" % "postgresql" % "9.3-1100-jdbc41",
  "com.typesafe" %% "play-plugins-mailer" % "2.2.+",
  "com.googlecode.owasp-java-html-sanitizer" % "owasp-java-html-sanitizer" % "r173", // html-фильтр для пользовательского контента.
  // io.suggest stuff
  "io.suggest" %% "util"      % "0.6.0-SNAPSHOT" changing() exclude("org.jruby", "jruby-complete"),
  "io.suggest" %% "util-play" % "0.6.0-SNAPSHOT" exclude("org.jruby", "jruby-complete"),
  // coffeescript-компилятор используем свой заместо компилятора play по ряду причин (последний прибит гвоздями к sbt-plugin, например).
  "org.jcoffeescript" % "jcoffeescript" % "1.6-SNAPSHOT",
  // for domain validation:
  "net.databinder.dispatch" %% "dispatch-core" % "0.11.+",
  "org.apache.httpcomponents" % "httpcore" % "4.1.+",
  "com.google.guava" % "guava" % "14.+",
  // Для импорт данных из sio v1 кластера. Выпилить вместе с util.compat.v1 после запуска
  "org.erlang.otp" % "jinterface" % "1.5.+"
)

play.Project.playScalaSettings

// После импорта настроек, typesafe-репа не кешируется. Это надо бы исправить.
resolvers ~= {
  rs => rs filter {_.name != "Typesafe Releases Repository" }
}

// Добавить резолверы, в т.ч. кэш-резолвер для отфильтрованной выше репы.
resolvers ++= Seq(
  "typesafe-releases" at "https://ivy2-internal.cbca.ru/artifactory/typesafe-releases",
  "sonatype-oss-releases" at "https://ivy2-internal.cbca.ru/artifactory/sonatype-oss-snapshots",
  "apache-releases" at "https://ivy2-internal.cbca.ru/artifactory/apache-releases",
  "conjars-repo" at "https://ivy2-internal.cbca.ru/artifactory/conjars-repo",
  "maven-twttr-com" at "https://ivy2-internal.cbca.ru/artifactory/maven-twttr-com"
)

patience.assets.StylusPlugin.stylusSettings


// Добавляем задачу сжатия всех сгенеренных js/css файлов.
lazy val gzipAssets = taskKey[Unit]("Gzip all js/css assets.")

gzipAssets := {
  val dir = resourceManaged.value
  println("Compressing js/ccs in directory: " + dir)
  ((dir ** "*.js") +++ (dir ** "*.css")).get.foreach { file =>
    val gzTarget = new File(file.getAbsolutePath + ".gz")
    IO.gzip(file, gzTarget)
    println("Compressed " + file.getName + " " + file.length / 1000F + " k => " + gzTarget.getName + " " + gzTarget.length / 1000F + " k")
    gzTarget
  }
}


