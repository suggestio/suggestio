package com.github.react.dnd

import japgolly.scalajs.react.{Children, CtorType, JsComponent, raw}

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
  def apply[P0 <: js.Object, PC <: js.Object, I <: js.Object, C <: Children]
      (itemType: DropAccept_t,
       spec: DropTargetSpec[P0 with PC, I] = new DropTargetSpec[P0 with PC, I] {},
       collect: (DropTargetConnector, DropTargetMonitor) => PC)
      (jsComp: raw.React.ComponentType[P0 with PC])
      (implicit summoner: CtorType.Summoner[P0, C]) =
  {
    val rawJsComp = DropTargetJs(itemType, spec, collect)(jsComp)
    JsComponent[P0, C, Null]( rawJsComp )
  }

}


/** DropTarget(Types.CARD, specJson, collectF)(DropZoneClass) => RawReactComponent. */
@js.native
@JSImport(DND_PACKAGE, "DropTarget")
object DropTargetJs extends js.Function {
  def apply[P0 <: js.Object, PC <: js.Object, I <: js.Object]
      (itemType: DropAccept_t,
       spec: DropTargetSpec[P0 with PC, I],
       collect: js.Function2[DropTargetConnector, DropTargetMonitor, PC]
      ): js.Function1[raw.React.ComponentType[P0 with PC], js.Any] = js.native
}


trait DropTargetSpec[P <: js.Object, I <: js.Object] extends js.Object {
  // (props: js.Object, monitor: DropTargetMonitor, component: js.Any): js.Object
  val drop: js.UndefOr[js.Function3[P, DropTargetMonitor, js.Any, js.UndefOr[I]]] = js.undefined
  // (props: js.Object, monitor: DropTargetMonitor, component: js.Any): Unit
  val hover: js.UndefOr[js.Function3[P, DropTargetMonitor, js.Any, Unit]] = js.undefined
  // (props: js.Object, monitor: DropTargetMonitor): Boolean
  val canDrop: js.UndefOr[js.Function2[P, DropTargetMonitor, Boolean]] = js.undefined
}
