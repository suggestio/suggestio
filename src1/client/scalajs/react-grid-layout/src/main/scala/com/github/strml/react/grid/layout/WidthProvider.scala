package com.github.strml.react.grid.layout

import japgolly.scalajs.react._

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.11.17 19:44
  * Description: WidthProvider HOC scalajs APIs.
  */
object WidthProvider {

  // TODO Add types. And they must be inferred from js-component somehow.
  def apply/*[P <: js.Object, C <: Children, S <: js.Object]*/(composedComponent: js.Object) = {
    val rawComp = WidthProviderJs(composedComponent)
    JsComponent[js.Object, Children.Varargs, js.Object](rawComp)
  }

}


@js.native
@JSImport("react-grid-layout", "WidthProvider")
object WidthProviderJs extends js.Function1[js.Object, WidthProviderJsComp] {

  override def apply(arg1: js.Object): WidthProviderJsComp = js.native

}


/** WidthProvider js-component interface. */
@js.native
sealed trait WidthProviderJsComp extends js.Object


/** Props for instantiating new [[WidthProviderJsComp]]. */
trait WidthProviderProps extends js.Object {

  val measureBeforeMount: Boolean

  val style: js.UndefOr[js.Object] = js.undefined

}


/** [[WidthProviderJsComp]] state interface. */
@js.native
sealed trait WidthProviderState extends js.Object {
  val width: Double = js.native
}
