package com.github.react.dnd

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import japgolly.scalajs.react.{Children, CtorType, JsComponent, raw}

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
    * @tparam P0 JSON обёртка над исходными пропертисами jsComp
    * @tparam PC JSON-пропертисы от collect функции.
    * @tparam I Тип properties для drop item'а.
    * @tparam C Children.
    * @return Компонент с dnd-обёрткой.
    */
  def apply[P0 <: js.Object, PC <: js.Object, I <: js.Object, C <: Children]
      (itemType: DropAccept_t,
       spec: DragSourceSpec[P0 with PC, I],
       collect: (DragSourceConnector, DragSourceMonitor) => PC)
      (jsComp: raw.React.ComponentType[P0 with PC])
      (implicit summoner: CtorType.Summoner[P0, C]) =
  {
    val jsCompRaw = DragSourceJs(itemType, spec, collect)(jsComp)
    JsComponent[P0, C, Null](jsCompRaw)
  }

}


/** DragSource(Types.CARD, specJson, collectF)(CardClass) => RawReactComponent. */
@js.native
@JSImport(DND_PACKAGE, "DragSource")
object DragSourceJs extends js.Function {
  def apply[P0 <: js.Object, PC <: js.Object, I <: js.Object]
      (itemType: DropAccept_t,
       spec: DragSourceSpec[P0 with PC, I],
       collect: js.Function2[DragSourceConnector, DragSourceMonitor, PC],
      ): js.Function1[raw.React.ComponentType[P0 with PC], js.Any] = js.native
}


/** Описание спеки пропертисов.
  *
  * @tparam P Тип пропертисов компонента..
  * @tparam I Тип drag-значения.
  */
trait DragSourceSpec[P <: js.Object, I <: js.Object] extends js.Object {
  val beginDrag: js.Function3[P, DragSourceMonitor, js.Any, I]
  val endDrag: js.UndefOr[js.Function3[P, DragSourceMonitor, js.Any, Unit]] = js.undefined
  val canDrag: js.UndefOr[js.Function2[P, DragSourceMonitor, Boolean]] = js.undefined
  val isDragging: js.UndefOr[js.Function2[P, DragSourceMonitor, Boolean]] = js.undefined
}


