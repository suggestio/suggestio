package io.suggest.sjs.mapbox.gl.anim

import scala.scalajs.js
import scala.scalajs.js.UndefOr

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.04.16 12:31
  * Description: Animation options API.
  */

trait AnimationOptions extends js.Object {

  val duration  : UndefOr[Double]                         = js.undefined

  val easing    : UndefOr[js.Function1[Double, Double]]   = js.undefined

  val offset    : UndefOr[js.Array[js.Any]]               = js.undefined

  val animate   : UndefOr[Boolean]                        = js.undefined

}
