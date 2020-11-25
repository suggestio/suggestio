package com.github.react.dnd

import japgolly.scalajs.react.{Children, CtorType, JsComponent, raw}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.11.2020 14:45
  * Description: DragLayer lets you perform the rendering of the drag preview yourself using only the React components.
  *
  * @see [[https://react-dnd.github.io/react-dnd/docs/api/drag-layer]]
  */
object DragLayer {

  /** Scala API для сборки DragLayer-компонента в scala-обёртке.
    *
    * @param collect The collecting function.
    *                It should return a plain object of the props to inject into your component.
    *                It receives two parameters, monitor and props.
    * @param jsComp Компонент для рендера в preview image.
    * @tparam P0 Пропертисы изначальные.
    * @tparam PC Дополнения для исходных пропертисов для проброса в нижележащий компонент.
    * @tparam C Children.
    * @return Компонент в DragLayer-обёртке.
    */
  def apply[P0 <: js.Object, PC <: js.Object, C <: Children]
           (collect: (DragSourceMonitor, P0) => PC)
           (jsComp: raw.React.ComponentType[P0 with PC])
           (implicit summoner: CtorType.Summoner[P0, C]) =
  {
    val jsCompRaw = DragLayerJs( collect )(jsComp)
    JsComponent[P0, C, Null](jsCompRaw)
  }

}


@js.native
@JSImport(DND_PACKAGE, "DragLayer")
object DragLayerJs extends js.Function {

  def apply[P0 <: js.Object, PC <: js.Object]
           (collect: js.Function2[DragSourceMonitor, P0, PC])
           : js.Function1[raw.React.ComponentType[P0 with PC], js.Any]
           = js.native

}

