package com.github.dantrain.react.stonecutter

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.11.17 17:11
  * Description: Result, returned by any layout function.
  */
trait LayoutFunRes extends js.Object {

  /** an Array of [x, y] coordinate pairs like this: [[0, 0], [20, 0], [0, 30]] */
  val positions: js.Array[js.Array[Int]]

  /** width of the entire grid (Number). */
  val gridWidth: Int

  /** height of the entire grid (Number). */
  val gridHeight: Int

}
