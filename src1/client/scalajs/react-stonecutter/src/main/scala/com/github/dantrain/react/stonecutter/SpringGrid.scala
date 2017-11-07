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

  def mkSjsComponent(from: js.Object) = JsComponent[SpringGridProps, Children.Varargs, js.Object]( from )
  val component = mkSjsComponent( SpringGridJs )

  /** Собрать несмонтированный компонент на базе стандартного (без доп.hoc'ов).
    *
    * @param props Пропертисы [[SpringGridProps]].
    * @param children Дочерние элементы.
    * @return unmounted component.
    */
  def apply(props: SpringGridProps)(children: VdomNode*) = applyCustom(component)(props)(children: _*)

  /** Собрать кастомный компонент на базе переданного js-компонента.
    * Использовать, когда необходимо накатить hoc'и на компонент перед использованием.
    *
    * @param comp Кастомный компонент на базе [[SpringGridJs]] и каких-либо hoc'ов.
    * @param props Пропертисы [[SpringGridProps]].
    * @param children Дочерние элементы.
    * @return unmounted component.
    */
  def applyCustom(comp: component.type)(props: SpringGridProps)(children: VdomNode*) = {
    comp(props)(children: _*)
  }

}


/** JS component. */
@js.native
@JSImport("react-stonecutter", "SpringGrid")
object SpringGridJs extends js.Object


/** Properties for [[SpringGrid]] component. */
trait SpringGridProps extends PropsCommon {

  /**
    * Config for react-motion spring.
    * Default: { stiffness: 60, damping: 14, precision: 0.1 }.
    */
  val springConfig: js.UndefOr[SpringConfig] = js.undefined

}
