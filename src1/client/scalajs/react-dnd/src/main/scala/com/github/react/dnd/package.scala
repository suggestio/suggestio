package com.github.react

import scala.scalajs.js
import scala.scalajs.js.|

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.08.2019 15:46
  * Description:
  */
package object dnd {

  final val DND_PACKAGE = "react-dnd-cjs"

  type ItemType_t = String | js.Symbol

  type DropAccept_t_0 = String | js.Symbol
  type DropAccept_t_1 = DropAccept_t_0 | js.Array[DropAccept_t_0]
  type DropAccept_t = DropAccept_t_1 | js.Function1[js.Object, DropAccept_t_1]

}
