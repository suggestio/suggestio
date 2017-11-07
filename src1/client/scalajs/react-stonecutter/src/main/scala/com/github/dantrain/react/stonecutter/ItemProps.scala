package com.github.dantrain.react.stonecutter

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.11.17 16:18
  * Description:
  */
@js.native
trait ItemProps extends js.Object {

  /** May be set directly via item attrs. */
  val itemHeight: js.UndefOr[Int] = js.undefined

  /** Set by measureItems HOC.  */
  val itemRect: js.UndefOr[ItemRect] = js.undefined

}


@js.native
trait ItemRect extends js.Object {

  val width: Int

  val height: Int

}
