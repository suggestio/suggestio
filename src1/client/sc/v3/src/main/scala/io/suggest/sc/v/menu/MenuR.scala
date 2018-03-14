package io.suggest.sc.v.menu

import diode.FastEq
import diode.react.ModelProxy
import io.suggest.color.MColorData
import io.suggest.sc.m.menu.MMenuS
import io.suggest.sc.styl.GetScCssF
import io.suggest.sc.v.hdr.LeftR
import japgolly.scalajs.react.{BackendScope, ScalaComponent}
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._

import scalacss.ScalaCssReact._
import io.suggest.ueq.UnivEqUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.03.18 21:44
  * Description: Главный компонент левой панели меню выдачи.
  */
class MenuR(
             leftR          : LeftR,
             getScCssF      : GetScCssF
           ) {

  type Props = ModelProxy[PropsVal]

  /** Поддержка FastEq для PropsVal. */
  implicit object MenuRPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      a.menuS ===* b.menuS
    }
  }

  /** Контейнер данных для рендера этого компонента-шаблона.
    *
    * @param menuS Инстанс модели состояния меню.
    */
  case class PropsVal(
                       menuS: MMenuS
                     )


  /** Состояние, т.к. model-proxy-коннекшены для используемых компонентов. */
  protected[this] case class State()


  class Backend($: BackendScope[Props, State]) {
    def render(p: Props, s: State): VdomElement = {
      val scCss = getScCssF()
      val menuCss = scCss.Menu
      <.div(
        p.wrap {_ => Option(MColorData.Examples.WHITE) } ( leftR.apply ),
        menuCss.panel
      )
    }
  }


  val component = ScalaComponent.builder[Props]("Menu")
    .initialStateFromProps { propsProxy =>
      State()
    }
    .renderBackend[Backend]
    .build

  def apply(menuProxy: Props) = component(menuProxy)

}
