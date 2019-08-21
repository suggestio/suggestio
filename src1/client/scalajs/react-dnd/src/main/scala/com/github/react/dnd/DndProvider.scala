package com.github.react.dnd

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import japgolly.scalajs.react._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.08.2019 15:45
  * Description:
  * @see [[http://react-dnd.github.io/react-dnd/docs/api/dnd-provider]]
  */
object DndProvider {

  val component = JsComponent[DndProviderProps, Children.Varargs, Null]( DndProviderJs )

}


@js.native
@JSImport(DND_PACKAGE, "DndProvider")
object DndProviderJs extends js.Object


trait DndProviderProps extends js.Object {
  val backend: IDndBackend
  val context: js.UndefOr[js.Any] = js.undefined
  val options: js.UndefOr[js.Object] = js.undefined
}

