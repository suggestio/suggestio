import sbt._
import Keys._
import play.Play.autoImport._
import PlayKeys._
import play.twirl.sbt.Import._
import play.twirl.sbt._
import com.typesafe.sbt.web._
import com.typesafe.sbt.web.Import._
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import com.typesafe.sbt.packager.Keys._

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
    .enablePlugins(play.PlayScala, SbtWeb)

  lazy val web21 = project
    .dependsOn(advExtCommon, util, securesocial, modelEnumUtilPlay)
    .aggregate(advExtSjsRunner)
    .enablePlugins(play.PlayScala, SbtWeb)
    .settings(
      Seq(
        scalajsOutputDir := (sourceManaged in Assets).value,
        sourceGenerators in Assets += Def.task {
          // TODO List + :: не компилируется тут почему-то.
          // TODO Вынос "in (advExtSjsRunner, Compile)).value.data" за скобки не компилится.
          val acc = Seq(
            (packageScalaJSLauncher in (advExtSjsRunner, Compile)).value.data,
            (fastOptJS in (advExtSjsRunner, Compile)).value.data,
            (fullOptJS in (advExtSjsRunner, Compile)).value.data
          )
          val depsFile = (packageJSDependencies in (advExtSjsRunner, Compile)).value
          if (depsFile.exists)  acc ++ Seq(depsFile)  else  acc
        }.taskValue
      ) ++ (
        Seq(packageJSDependencies, packageScalaJSLauncher, fastOptJS, fullOptJS) map { packageJSKey =>
          // advExtSjsRunner должен компилить результаты в web21/target/web поддиректорию вместо своей стандартной.
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
