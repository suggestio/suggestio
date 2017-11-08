package com.github.dantrain.react.stonecutter

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomNode

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.11.17 15:24
  * Description: React-sjs facade for SpringGrid component.
  */
object SpringGrid {

  def mkSjsComponent(jsComponent: JsComponentRoot) = JsComponent[SpringGridProps, Children.Varargs, js.Object]( jsComponent )
  val component = mkSjsComponent( SpringGridJs )

  /** Собрать несмонтированный компонент на базе стандартного (без доп.hoc'ов).
    *
    * @param props Пропертисы [[SpringGridProps]].
    * @param children Дочерние элементы.
    * @return unmounted component.
    */
  def apply(props: SpringGridProps)(children: VdomNode*) = component(props)(children: _*)

}


/** JS component. */
@js.native
@JSImport(REACT_STONECUTTER, "SpringGrid")
object SpringGridJs extends JsComponentRoot


/** Properties for [[SpringGrid]] component. */
trait SpringGridProps extends PropsCommon {

  /**
    * Config for react-motion spring.
    * Default: { stiffness: 60, damping: 14, precision: 0.1 }.
    */
  val springConfig: js.UndefOr[SpringConfig] = js.undefined

}
