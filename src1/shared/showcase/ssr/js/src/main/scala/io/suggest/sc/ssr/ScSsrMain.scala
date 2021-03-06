package io.suggest.sc.ssr

import io.suggest.react.ReactBrowserDOMServer
import japgolly.scalagraal.Pickled
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.scalajs.js.annotation.JSExportTopLevel

object ScSsrMain {

  lazy val component = ScSsrModule.scRootRendered

  /** API function to call from JVM. */
  @JSExportTopLevel( ScSsrProto.Manifest.RenderActionSync )
  def renderActionSync(argsPick: Pickled[MScSsrArgs]): String = {
    val args = argsPick.value

    // Notify showcase circuit about new index/grid state:
    ScSsrModule.sc3Circuit.dispatch( args.action )

    // Execute HTML rendering:
    ReactBrowserDOMServer.renderToString( component.rawElement )
  }

}
