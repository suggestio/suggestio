package org.js.react.leaflet.core

import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.01.2021 23:10
  */

@js.native
@JSImport(PACKAGE_NAME, "addClassName")
object addClassName extends js.Function2[dom.html.Element, String, Unit] {
  override def apply(element: dom.html.Element,
                     className: String,
                    ): Unit = js.native
}


@js.native
@JSImport(PACKAGE_NAME, "removeClassName")
object removeClassName extends js.Function2[dom.html.Element, String, Unit] {
  override def apply(element: dom.html.Element,
                     className: String,
                    ): Unit = js.native
}


@js.native
@JSImport(PACKAGE_NAME, "updateClassName")
object updateClassName extends js.Function3[dom.html.Element, String, String, Unit] {
  override def apply(element: dom.html.Element,
                     prevClassName: String,
                     nextClassName: String,
                    ): Unit = js.native
}
