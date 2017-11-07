package com.github.dantrain.react.stonecutter

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.11.17 18:13
  * Description: Props for enter/entered/exit function results.
  */
trait EnterExitFunRes extends js.Object {

  val opacity,
    translateX,
    translateY,
    translateZ,
    skew,
    skewX,
    skewY,
    scale,
    scaleX,
    scaleY,
    rotate,
    rotateX,
    rotateY: js.UndefOr[Double] = js.undefined

}
