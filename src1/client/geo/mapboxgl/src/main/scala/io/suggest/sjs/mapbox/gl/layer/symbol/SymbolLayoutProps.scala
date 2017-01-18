package io.suggest.sjs.mapbox.gl.layer.symbol

import io.suggest.sjs.mapbox.gl.layer.LayoutProps

import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scala.scalajs.js.annotation.ScalaJSDefined

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.04.16 16:10
  * Description: Model of Symbol layout properties.
  */

@ScalaJSDefined
trait SymbolLayoutProps extends LayoutProps {

  val `text-field`: UndefOr[String] = js.undefined

  val `text-size`: UndefOr[Int] = js.undefined

}
