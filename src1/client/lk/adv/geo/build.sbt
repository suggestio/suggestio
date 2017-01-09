Common.settingsOrgJS

enablePlugins(ScalaJSPlugin)

name := "lk-adv-geo-tags-sjs"

version := "0.0.0-SNAPSHOT"

persistLauncher in Compile := false

persistLauncher in Test := false

// Show more comments when using dubious features
scalacOptions += "-feature"

libraryDependencies ++= Seq(
    Common.ORG      %%% "lk-adv-common-sjs"               % "0.0.0-SNAPSHOT",
    Common.ORG      %%% "scalajs-react-common"            % "0.0.0-SNAPSHOT",
    //Common.ORG      %%% "map-rad-sjs"                     % "0.0.0-SNAPSHOT",
    Common.ORG      %%% "lk-tags-edit-sjs"                % "0.0.0-SNAPSHOT",
    Common.ORG      %%% "scalajs-leaflet-markercluster"   % "0.0.0-SNAPSHOT",
    Common.ORG      %%% "scalajs-leaflet-react"           % "0.0.0-SNAPSHOT",

    // 2016.dec.14: diode, boopickle. Внедрение этих велосипедов началось для нужд унифицированной сериализации/десериализации.
    //"me.chrons"     %%% "diode-devtools"                  % Common.diodeVsn,
    "com.github.japgolly.scalajs-react" %%% "extra"       % Common.reactSjsVsn
)

