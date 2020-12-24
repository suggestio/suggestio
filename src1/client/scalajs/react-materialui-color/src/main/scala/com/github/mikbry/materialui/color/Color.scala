package com.github.mikbry.materialui.color

import scala.scalajs.js
import scala.scalajs.js.|

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.12.2020 8:12
  * @see [[https://github.com/mikbry/material-ui-color/blob/master/src/helpers/commonTypes.js#L10]]
  */
@js.native
trait Color extends js.Object {

  def css: js.UndefOr[js.Object] = js.native
  def value: js.UndefOr[Double] = js.native
  val hex: String = js.native
  val raw: String | js.Array[Double] | Double | js.Object = js.native
  val name: js.UndefOr[String] = js.native
  val alpha: Double = js.native
  val rgb: js.Array[Double] = js.native
  val hsv: js.Array[Double] = js.native
  val hsl: js.Array[Double] = js.native

}
