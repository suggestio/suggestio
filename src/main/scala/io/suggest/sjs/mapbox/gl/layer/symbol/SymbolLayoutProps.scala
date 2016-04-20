package io.suggest.sjs.mapbox.gl.layer.symbol

import io.suggest.sjs.common.model.FromDict
import io.suggest.sjs.mapbox.gl.layer.LayoutProps

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.04.16 16:10
  * Description: Model of Symbol layout properties.
  */
object SymbolLayoutProps extends FromDict {
  override type T = SymbolLayoutProps
}


@js.native
trait SymbolLayoutProps extends LayoutProps {

  @JSName("text-field")
  var textField: String = js.native

}
