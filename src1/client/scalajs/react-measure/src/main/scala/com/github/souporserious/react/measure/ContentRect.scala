package com.github.souporserious.react.measure

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.08.2019 17:21
  * Description:
  */
trait ContentRect extends js.Object {

  val bounds: js.UndefOr[Bounds] = js.undefined
  val client: js.UndefOr[Client] = js.undefined
  val offset: js.UndefOr[Offset] = js.undefined
  val scroll: js.UndefOr[Scroll] = js.undefined
  val margin: js.UndefOr[Margin] = js.undefined

  val entry: js.UndefOr[ResizeObserverEntry] = js.undefined

}


sealed trait WhJs extends js.Object {
  val width: Double
  val height: Double
}

trait Bounds extends WhJs

trait Client extends WhJs

trait Offset extends WhJs

trait Scroll extends WhJs

trait Margin extends WhJs