package io.suggest.react

import japgolly.scalajs.react.{JsComponentType, React, ReactNode, TopNode}

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.12.16 16:48
  * Description: React wrapper generators.
  */

/** Абстрактный трейт без apply() для сборки более конкретных трейтов врапперов. */
trait IJsWrapperR[Props <: js.Any, +Node <: TopNode] {

  protected def _rawComponent: js.Dynamic

  def factory = {
    React.createFactory(
      // Тип внутреннего состояния пока приравнян к js.Object, для простоты.
      _rawComponent.asInstanceOf[ JsComponentType[Props, js.Object, Node] ]
    )
  }

}

/** Интерфейс для поля props произвольного типа. */
trait IProps[Props <: js.Any] {
  val props: Props
}
/** Пустая реализация [[IProps]]. */
trait IPropsEmpty extends IProps[js.Any] {
  override val props = js.Object()
}


/** Обычный враппер, допускающий любое кол-во children (0, 1, ...). */
trait JsWrapperR[Props <: js.Any, +Node <: TopNode] extends IJsWrapperR[Props, Node] with IProps[Props] {
  def apply(children : ReactNode*) = factory(props, children: _*)
}
/** Обычный враппер, допускающий любое кол-во children (0, 1, ...), но без props вообще. */
trait JsWrapperNoPropsR[+Node <: TopNode] extends JsWrapperR[js.Any, Node] with IPropsEmpty


/** Враппер, не допускающий children. */
trait JsWrapper0R[Props <: js.Any, +Node <: TopNode] extends IJsWrapperR[Props, Node] with IProps[Props] {
  def apply() = factory(props)
}


/** Враппер, допускающий ровно один child. */
trait JsWrapper1R[Props <: js.Any, +Node <: TopNode] extends IJsWrapperR[Props, Node] with IProps[Props] {
  def apply(child: ReactNode) = factory(props, child)
}
