package io.suggest.sjs.mapbox.gl.window

import org.scalajs.dom.Window

import scala.scalajs.js
import scala.scalajs.js.UndefOr
import io.suggest.sjs.mapbox.gl.{mapboxgl => api}

import scala.language.implicitConversions

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.04.16 18:53
  * Description: Support for access to mapboxgl via window.mapboxgl.
  * Also suitable for checking persistence of mapboxgl.js in runtime.
  */
@js.native
sealed trait IMbglWindow extends Window {

  var mapboxgl: UndefOr[api.type] = js.native

}


object IMbglWindow {

  implicit def wnd2mpglWnd(wnd: Window): IMbglWindow = {
    wnd.asInstanceOf[IMbglWindow]
  }

}

