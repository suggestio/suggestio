package com.github.dantrain.react.stonecutter

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.11.17 18:28
  * Description: Basic configuration interface for the React-Motion spring.
  *
  * @see [[https://github.com/chenglou/react-motion#helpers]]
  */

// TODO Moveto or use separate sjs project for this.
trait SpringConfig extends js.Object {

  val stiffness,
      damping,
      precision: js.UndefOr[Double] = js.undefined

}
