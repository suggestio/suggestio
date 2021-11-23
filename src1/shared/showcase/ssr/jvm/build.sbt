// This JVM-stuff depends on GraalVM SDK.
libraryDependencies ++= Seq(
  "com.github.japgolly.scala-graal" %% "core"          % Common.Vsn.SCALA_GRAAL,

  "com.github.japgolly.scala-graal" %% "core-js"       % Common.Vsn.SCALA_GRAAL,

  "com.github.japgolly.scala-graal" %% "ext-boopickle" % Common.Vsn.SCALA_GRAAL,
)
