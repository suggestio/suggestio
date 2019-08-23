package com.github.react.dnd

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.08.2019 10:11
  * @see [[http://react-dnd.github.io/react-dnd/docs/api/drag-source]]
  */
object DragSource {

  /** Сборка react-компонента, который надо затем завернуть в j.s.react.JsComponent() самостоятельно.
    *
    * @param itemType Типы ожидаемых item'ов.
    * @param spec Спека.
    * @param collect Collecting function.
    * @param jsComp react-компонент, который будет дорабатываться для перетаскивания.
    * @return Компонент с dnd-обёрткой.
    */
  def apply(itemType: DropAccept_t, spec: DragSourceSpec, collect: (DragSourceConnector, DragSourceMonitor) => js.Object)
           (jsComp: js.Any): js.Any = {
    DragSourceJs(itemType, spec, collect)(jsComp)
  }

}


/** DragSource(Types.CARD, specJson, collectF)(CardClass) => RawReactComponent. */
@js.native
@JSImport(DND_PACKAGE, "DragSource")
object DragSourceJs extends js.Function3[DropAccept_t, DragSourceSpec, js.Function2[DragSourceConnector, DragSourceMonitor, js.Object], js.Function1[js.Any, js.Any]] {
  override def apply(itemType: DropAccept_t, spec: DragSourceSpec, collect: js.Function2[DragSourceConnector, DragSourceMonitor, js.Object]): js.Function1[js.Any, js.Any] = js.native
}


trait DragSourceSpec extends js.Object {
  def beginDrag(props: js.Object, monitor: DragSourceMonitor, component: js.Any): js.Object
  val endDrag: js.UndefOr[js.Function3[js.Object, DragSourceMonitor, js.Any, js.Object]] = js.undefined
  val canDrag: js.UndefOr[js.Function2[js.Object, DragSourceMonitor, Boolean]] = js.undefined
  val isDragging: js.UndefOr[js.Function2[js.Object, DragSourceMonitor, Boolean]] = js.undefined
}


