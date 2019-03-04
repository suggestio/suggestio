Common.settingsOrg

name := "svg-util"

version := "0.0.0"

libraryDependencies ++= {
  val baticVsn = Common.Vsn.APACHE_BATIK
  val xgOrg = "org.apache.xmlgraphics"
  Seq(
    // batik довольно кривой, exclude(batik-ext) не пашет, приходится сочинять чудеса.
    // TODO Эти костыли ещё актуальны? Вроде бы версия 1.9 может быть уже нормальна.
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
    // Для GVT tree builder:
    xgOrg       % "batik-bridge"   % baticVsn intransitive(),
    xgOrg       % "batik-gvt"      % baticVsn intransitive(),
    xgOrg       % "batik-script"   % baticVsn intransitive(),
    xgOrg       % "batik-ext"      % baticVsn intransitive(),

    xgOrg       % "xmlgraphics-commons" % "2.2" intransitive(),

    "xml-apis"  % "xml-apis"       % "1.4.01",
    "xml-apis"  % "xml-apis-ext"   % "1.3.04",
    // test
    "org.scalatest" %% "scalatest" % Common.scalaTestVsn % "test"
  )
}

