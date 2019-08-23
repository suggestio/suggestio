package com.github.react.dnd

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.08.2019 16:59
  * @see [[http://react-dnd.github.io/react-dnd/docs/api/drop-target]]
  */
object DropTarget {

  /** Сборка react-компонента, который надо затем завернуть в j.s.react.JsComponent() самостоятельно.
    *
    * @param itemType Типы ожидаемых item'ов.
    * @param spec Спека.
    * @param collect Collecting function.
    * @param jsComp react-компонент, который будет дорабатываться для перетаскивания.
    * @return Компонент с dnd-обёрткой.
    */
  def apply(itemType: DropAccept_t, spec: DropTargetSpec = new DropTargetSpec {}, collect: (DropTargetConnector, DropTargetMonitor) => js.Object)
           (jsComp: js.Any): js.Any = {
    DropTargetJs(itemType, spec, collect)(jsComp)
  }

}


/** DropTarget(Types.CARD, specJson, collectF)(DropZoneClass) => RawReactComponent. */
@js.native
@JSImport(DND_PACKAGE, "DropTarget")
object DropTargetJs extends js.Function3[DropAccept_t, DropTargetSpec, js.Function2[DropTargetConnector, DropTargetMonitor, js.Object], js.Function1[js.Any, js.Any]] {
  override def apply(itemType: DropAccept_t, spec: DropTargetSpec, collect: js.Function2[DropTargetConnector, DropTargetMonitor, js.Object]): js.Function1[js.Any, js.Any] = js.native
}


trait DropTargetSpec extends js.Object {
  // (props: js.Object, monitor: DropTargetMonitor, component: js.Any): js.Object
  val drop: js.UndefOr[js.Function3[js.Object, DropTargetMonitor, js.Any, js.UndefOr[js.Object]]] = js.undefined
  // (props: js.Object, monitor: DropTargetMonitor, component: js.Any): Unit
  val hover: js.UndefOr[js.Function3[js.Object, DropTargetMonitor, js.Any, Unit]] = js.undefined
  // (props: js.Object, monitor: DropTargetMonitor): Boolean
  val canDrop: js.UndefOr[js.Function2[js.Object, DropTargetMonitor, Boolean]] = js.undefined
}
