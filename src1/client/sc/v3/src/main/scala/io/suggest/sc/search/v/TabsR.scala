package io.suggest.sc.search.v

import diode.react.ModelProxy
import io.suggest.sc.search.m.{MSearchTab, MSearchTabs, SwitchTab}
import japgolly.scalajs.react.{BackendScope, Callback, ScalaComponent}
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.sjs.common.i18n.Messages
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.sc.styl.GetScCssF

import scalacss.ScalaCssReact._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.07.17 14:13
  * Description: Переключалка табов поисковой панели.
  */
class TabsR( getScCssF: GetScCssF ) {

  type Props = ModelProxy[MSearchTab]

  class Backend( $: BackendScope[Props, Unit] ) {

    private def _onClick(mtab: MSearchTab): Callback = {
      dispatchOnProxyScopeCB( $, SwitchTab(mtab) )
    }

    def render(propsProxy: Props): VdomElement = {
      val scCss = getScCssF()
      val CSS = scCss.Search.Tabs
      val SCSS = CSS.Single

      val currTab = propsProxy()

      <.div(
        CSS.tabs,

        <.div(
          CSS.tabsWrapper,
          // Пройти по всем табам и отрендерить их:
          MSearchTabs.values.toVdomArray { mtab =>
            <.div(
              SCSS.tabOuter,
              ^.key := mtab.name,

              // Клики по табам переключают табы:
              ^.onClick --> _onClick( mtab ),

              <.div(
                SCSS.tabInner,

                // Для неактивного класса приписать __inactive.
                SCSS.inactive
                  .unless( mtab == currTab ),

                // Для первой табы -- скруглять углы слева.
                SCSS.Rounded.left
                  .when( mtab.isFirst ),
                // Для последней табы -- скруглять справа.
                SCSS.Rounded.right
                  .when( mtab.isLast ),

                // Наконец, рендерить отображаемое название таба:
                Messages( mtab.name )
              )
            )
          },

          // TODO Понять, надо ли это вообще?
          <.div(
            scCss.clear
          )

        )
      )
    }

  }


  val component = ScalaComponent.builder[Props]("Tabs")
    .stateless
    .renderBackend[Backend]
    .build

  def apply( p: Props ) = component( p )

}
