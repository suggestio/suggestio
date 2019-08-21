package com.github.react.dnd

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.08.2019 10:27
  */

@js.native
@JSImport(DND_PACKAGE, "useDragLayer")
object useDragLayerJs extends js.Function1[DragLayerSpec, js.Object] {
  override def apply(arg1: DragLayerSpec): js.Object = js.native
}


trait DragLayerSpec extends js.Object {
  /**
    * The collecting function. It should return a plain object of the props to return for injection into your component.
    * It receives two parameters, monitor and props.
    */
  val collect: js.Function2[DragLayerMonitor, js.Object, js.Object]
}
