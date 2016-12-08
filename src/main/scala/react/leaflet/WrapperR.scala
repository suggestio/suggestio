package react.leaflet

import japgolly.scalajs.react.{JsComponentType, React, ReactNode, TopNode}

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.12.16 16:48
  * Description: React wrapper generators.
  */

// TODO Move it to common-react-sjs.

/** Абстрактный трейт без apply() для сборки более конкретных трейтов врапперов. */
trait IWrapperR[Props <: js.Any, +Node <: TopNode] {

  val props: Props

  protected def _rawComponent: js.Dynamic

  def factory = {
    React.createFactory(
      // Тип внутреннего состояния пока приравнян к js.Object, для простоты.
      _rawComponent.asInstanceOf[ JsComponentType[Props, js.Object, Node] ]
    )
  }

}


/** Обычный враппер, допускающий любое кол-во children (0, 1, ...). */
trait WrapperR[Props <: js.Any, +Node <: TopNode] extends IWrapperR[Props, Node] {
  def apply(children : ReactNode*) = factory(props, children: _*)
}


/** Враппер, не допускающий children. */
trait Wrapper0R[Props <: js.Any, +Node <: TopNode] extends IWrapperR[Props, Node] {
  def apply() = factory(props)
}


/** Враппер, допускающий ровно один child. */
trait Wrapper1R[Props <: js.Any, +Node <: TopNode] extends IWrapperR[Props, Node] {
  def apply(child: ReactNode) = factory(props, child)
}
