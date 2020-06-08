package io.suggest.sc.v.menu

import diode.react.ModelProxy
import io.suggest.sc.m.MScReactCtx
import io.suggest.sc.m.menu.MMenuS
import io.suggest.sc.v.ScCssStatic
import io.suggest.sc.v.hdr.LeftR
import japgolly.scalajs.react._
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
             versionR       : VersionR,
             scReactCtxP    : React.Context[MScReactCtx],
           ) {

  type Props = ModelProxy[MMenuS]


  class Backend($: BackendScope[Props, Unit]) {

    def render(propsProxy: Props, children: PropsChildren): VdomElement = {
      val vsn = versionR.component(): VdomElement

      // Кнопка сокрытия панели влево
      val hideBtn = propsProxy.wrap(_ => None)( leftR.apply ): VdomElement

      val panelCommon = ScCssStatic.Root.panelCommon: TagMod
      val panelBg = ScCssStatic.Root.panelBg: TagMod

      val menuRows = <.div(
        ScCssStatic.Menu.Rows.rowsContainer,
        children,
      )

      scReactCtxP.consume { scReactCtx =>
        val menuCss = scReactCtx.scCss.Menu

        <.div(
          panelCommon,
          menuCss.panel,

          // Фон панели.
          <.div(
            panelBg,
            scReactCtx.scCss.bgColor,
          ),

          // Контейнер для непосредственного контента панели.
          <.div(
            menuCss.content,

            hideBtn,

            // Менюшка
            menuRows,
          ),

          vsn,
        )    // .panel
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackendWithChildren[Backend]
    .build

  def apply(menuProxy: Props)(children: VdomNode*) = component(menuProxy)(children: _*)

}
