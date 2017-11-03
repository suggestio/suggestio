package com.github.strml.react.grid.layout

import scala.scalajs.js
import scala.scalajs.js.UndefOr

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.11.17 17:30
  * Description:
  * Layout is an array of object with the format:
  * {x: number, y: number, w: number, h: number}
  *
  * The index into the layout must match the key used on each item component.
  * If you choose to use custom keys, you can specify that key in the layout array objects like so:
  * {i: string, x: number, y: number, w: number, h: number}
  */
trait Layout extends js.Object {

  val i: UndefOr[String] = js.undefined

  val x: UndefOr[Int] = js.undefined

  val y: UndefOr[Int] = js.undefined

  val w: UndefOr[Int] = js.undefined

  val h: UndefOr[Int] = js.undefined

}
