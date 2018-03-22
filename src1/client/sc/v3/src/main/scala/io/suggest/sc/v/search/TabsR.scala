package io.suggest.sc.v.search

import diode.react.{ModelProxy, ReactConnectProps}
import io.suggest.msg.Messages
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.sc.m.search.{MSearchTab, MSearchTabs, SwitchTab}
import io.suggest.sc.styl.GetScCssF
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, ScalaComponent}
import scalacss.ScalaCssReact._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.07.17 14:13
  * Description: Переключалка табов поисковой панели.
  */
class TabsR( getScCssF: GetScCssF ) {

  type Props_t = MSearchTab
  type Props = ModelProxy[Props_t]

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

  def _apply( p: Props ) = component( p )
  val apply: ReactConnectProps[Props_t] = _apply

}
