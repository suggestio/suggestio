Common.settingsOrg

name := "svg-util"

version := "0.0.0-SNAPSHOT"

libraryDependencies ++= {
  val baticVsn = Common.Vsn.APACHE_BATIK
  val xgOrg = "org.apache.xmlgraphics"
  Seq(
    Common.ORG %% "logs-macro"     % "0.0.0-SNAPSHOT",
    // batik довольно кривой, exclude(batik-ext) не пашет, приходится сочинять чудеса.
    xgOrg       % "batik-svg-dom"  % baticVsn intransitive(),
    xgOrg       % "batik-dom"      % baticVsn intransitive(),
    xgOrg       % "batik-css"      % baticVsn intransitive(),
    xgOrg       % "batik-i18n"     % baticVsn intransitive(),
    xgOrg       % "batik-constants" % baticVsn intransitive(),
    xgOrg       % "batik-xml"      % baticVsn intransitive(),
    xgOrg       % "batik-util"     % baticVsn intransitive(),
    xgOrg       % "batik-parser"   % baticVsn intransitive(),
    xgOrg       % "batik-anim"     % baticVsn intransitive(),
    xgOrg       % "batik-awt-util" % baticVsn intransitive(),
    "xml-apis"  % "xml-apis"       % "1.4.01",
    "xml-apis"  % "xml-apis-ext"   % "1.3.04",
    // test
    "org.scalatest" %% "scalatest" % Common.scalaTestVsn % "test"
  )
}

