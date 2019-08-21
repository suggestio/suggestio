package com.github.react.dnd

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import japgolly.scalajs.react._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.08.2019 15:45
  * @see [[http://react-dnd.github.io/react-dnd/docs/api/drag-preview-image]]
  */
object DragPreviewImage {

  val component = JsComponent[DndProviderProps, Children.Varargs, Null]( DragPreviewImageJs )

}


@js.native
@JSImport(DND_PACKAGE, "DragPreviewImage")
object DragPreviewImageJs extends js.Object


trait DragPreviewImageProps extends js.Object {
  val connect: js.Function
}

