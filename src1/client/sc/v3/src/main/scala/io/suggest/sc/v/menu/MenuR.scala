package io.suggest.sc.v.menu

import diode.FastEq
import diode.react.ModelProxy
import io.suggest.color.MColorData
import io.suggest.sc.m.menu.MMenuS
import io.suggest.sc.styl.GetScCssF
import io.suggest.sc.v.hdr.LeftR
import japgolly.scalajs.react.{BackendScope, PropsChildren, ScalaComponent}
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
                       menuS        : MMenuS
                     )


  class Backend($: BackendScope[Props, Unit]) {
    def render(propsProxy: Props, children: PropsChildren): VdomElement = {
      val scCss = getScCssF()
      val menuCss = scCss.Menu

      <.div(
        menuCss.panel,

        // Фон панели.
        <.div(
          scCss.Root.panelBg
        ),

        // Контейнер для непосредственного контента панели.
        <.div(
          menuCss.content,

          // Кнопка сокрытия панели влево
          propsProxy.wrap {_ => Option(MColorData.Examples.WHITE) } ( leftR.applyReusable ),

          // Менюшка
          <.div(
            menuCss.Rows.rowsContainer,

            children
          )  // .rowsContainer
        )

      )    // .panel
    }
  }


  val component = ScalaComponent.builder[Props]("Menu")
    .stateless
    .renderBackendWithChildren[Backend]
    .build

  def apply(menuProxy: Props)(children: VdomNode*) = component(menuProxy)(children: _*)

}
