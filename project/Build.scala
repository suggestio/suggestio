import com.typesafe.sbt.web.pipeline.Pipeline
import play.PlayScala
import sbt._
import Keys._
import com.typesafe.sbt.web._
import com.typesafe.sbt.web.SbtWeb.autoImport._
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import com.typesafe.sbt.SbtNativePackager._

object SiobixBuild extends Build {

  /** Куда складывать скомпиленные scalajs-результаты в web-проектах. */
  val scalajsOutputDir = Def.settingKey[File]("directory for javascript files output by scalajs") 

  /*lazy val cascadingEs2 = {
    val ces2 ="cascading-elasticsearch2"
    Project(
      id    = ces2,
      base  = file("bixo/" + ces2),
      dependencies = Seq(util)
    )
  }

  lazy val siobix = Project(
    id = "siobix",
    base = file("bixo"),
    dependencies = Seq(util, cascadingEs2)
  )*/

  lazy val modelEnumUtil = {
    val name = "model-enum-util"
    Project(
      id = name,
      base = file(name)
    )
  }

  lazy val modelEnumUtilPlay = {
    val name = "model-enum-util-play"
    Project(
      id    = name,
      base  = file(name),
      dependencies = Seq(modelEnumUtil)
    )
  }

  lazy val advExtCommon = {
    val name = "advext-common"
    Project(
      id = name,
      base = file(name),
      dependencies = Seq(modelEnumUtil)
    )
  }

  lazy val advExtSjsRunner = {
    val name = "advext-sjs-runner"
    Project(id = name, base = file(name))
      .dependsOn(advExtCommon)
      .settings(
        unmanagedSourceDirectories in Compile <++= unmanagedSourceDirectories in (advExtCommon, Compile),
        unmanagedSourceDirectories in Compile <++= unmanagedSourceDirectories in (modelEnumUtil, Compile)
      )
  }

  lazy val util = project
    .dependsOn(modelEnumUtil)

  /*lazy val utilPlay = {
    val name = "util-play"
    Project(
      id = name,
      base = file(name),
      dependencies = Seq(util)
    )
  }*/

  lazy val securesocial = project
    .enablePlugins(PlayScala, SbtWeb)


  def _sourceMapTask(tk: TaskKey[Attributed[File]]) = Def.task[Seq[File]] {
    val path = (sbt.Keys.artifactPath in(advExtSjsRunner, Compile, tk)).value.getAbsolutePath + ".map"
    Seq(new File(path))
  }

  def _sjsResultsDev = Def.task[Seq[File]] {
    // Не надо пытаться тут делать вынос "in (advExtSjsRunner, Compile)).value.data" за скобки: уже пробовал, не компилит.
    Seq(
      (packageScalaJSLauncher in (advExtSjsRunner, Compile)).value.data,
      (fastOptJS in (advExtSjsRunner, Compile)).value.data
    )
  }

  def _sjsResultsProd = Def.task[Seq[File]] {
    val acc = List(
      (fullOptJS in (advExtSjsRunner, Compile)).value.data
    )
    val depsFile = (packageJSDependencies in (advExtSjsRunner, Compile)).value
    if (depsFile.exists)  depsFile :: acc  else  acc
  }

  lazy val web21 = project
    .dependsOn(advExtCommon, util, securesocial, modelEnumUtilPlay)
    .aggregate(advExtSjsRunner)
    .enablePlugins(PlayScala, SbtWeb)
    .settings(
      Seq(
        // Подцепляем результаты fastOptJS к сборке sbt-web.
        scalajsOutputDir := (sourceManaged in Assets).value,
        sourceGenerators in Assets += _sourceMapTask(fastOptJS).taskValue,
        sourceGenerators in Assets += _sjsResultsDev.taskValue
      ) ++ inConfig(Universal)(Seq(
        // TODO (не пашет!) Надо только в режиме stage/dist собираем тяжелый fullOptJS. Шаблоны сами должны разруливать это через Play.isProd.
        sourceGenerators in Assets += _sourceMapTask(fullOptJS).taskValue,
        sourceGenerators in Assets += _sjsResultsProd.taskValue
      )) ++ (
        // advExtSjsRunner всегда должен складывать результаты финальной компиляции в web21/target/web поддиректорию вместо своей стандартной.
        Seq(packageJSDependencies, packageScalaJSLauncher, fastOptJS, fullOptJS) map { packageJSKey =>
          crossTarget in (advExtSjsRunner, Compile, packageJSKey) := scalajsOutputDir.value
        }
      ) : _*
    )
    

  lazy val root = Project(
    id = "root",
    base = file(".")
  )
  .aggregate(modelEnumUtil, modelEnumUtilPlay, advExtCommon, advExtSjsRunner, util, securesocial, web21)



  val copySourceMapsTask = Def.task {
    val scalaFiles = (Seq(advExtCommon.base, advExtSjsRunner.base) ** ("*.scala")).get
    for (scalaFile <- scalaFiles) {
      val target = new File((classDirectory in Compile).value, scalaFile.getPath)
      IO.copyFile(scalaFile, target)
    }
  }

}
