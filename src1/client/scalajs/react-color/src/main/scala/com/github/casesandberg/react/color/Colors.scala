package com.github.casesandberg.react.color

import scala.scalajs.js
import scala.scalajs.js.|

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.09.17 10:28
  * Description:
  */

@js.native
sealed trait RgbHslColorBase extends js.Object {
  val a: js.UndefOr[Double | String] = js.undefined
}

/** RGB color. */
@js.native
trait Rgb extends RgbHslColorBase {
  val r: Int
  val g: Int
  val b: Int
}

/** HSL color. */
@js.native
trait Hsl extends RgbHslColorBase {
  val h: Int
  val s: Double
  val l: Double
}


/** Color from picker. */
@js.native
trait Color extends js.Object {
  val hex: String = js.native
  val rgb: Rgb = js.native
  val hsl: Hsl = js.native
}


@js.native
trait NamedColor extends js.Object {
  val color: String
  val title: js.UndefOr[String] = js.undefined
}
