package io.suggest.sc.ssr

import io.suggest.sc.Sc3Module
import japgolly.scalagraal.Pickled
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.scalajs.js.annotation.JSExportTopLevel

object ScSsrMain {

  val sc3Modules = new Sc3Module

  val component: VdomElement = sc3Modules.sc3SpaRouter.state.router()

  /** API function to call from JVM. */
  @JSExportTopLevel( ScSsrProto.Manifest.RenderActionSync )
  def renderActionSync(args: Pickled[MScSsrArgs]): String = {
    // Notify showcase circuit about new state:
    sc3Modules.sc3Circuit.dispatch( args.value.action )

    // Execute HTML rendering:
    ReactDOMServer.renderToString( component )
  }

}
