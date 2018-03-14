package io.suggest.sc.v.menu

import diode.UseValueEq
import diode.react.ModelProxy
import io.suggest.i18n.MsgCodes
import io.suggest.msg.Messages
import io.suggest.routes.scRoutes
import io.suggest.sc.styl.GetScCssF
import io.suggest.sjs.common.model.Route
import japgolly.scalajs.react.{BackendScope, ScalaComponent}
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._

import scalacss.ScalaCssReact._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.03.18 18:19
  * Description: Компонент строки меню с ссылкой для логина в s.io.
  */
class EnterLkRowR(
                   getScCssF: GetScCssF
                 ) {

  type Props = ModelProxy[PropsVal]


  case class PropsVal(
                       isLoggedIn     : Boolean,
                       isMyAdnNodeId  : Option[String]
                     )
    extends UseValueEq


  class Backend($: BackendScope[Props, Unit]) {
    def render(propsProxy: Props): VdomElement = {

      val menuCss = getScCssF().Menu
      val props = propsProxy.value

      val (hrefRoute, msgCode) = props.isMyAdnNodeId.fold [(Route, String)] {
        if (props.isLoggedIn) {
          // Ссылка на личный кабинет
          val r = scRoutes.controllers.Ident.rdrUserSomewhere()
          val txt = MsgCodes.`Personal.cabinet`
          (r, txt)

        } else {
          // Ссылка на вход в s.io
          val r = scRoutes.controllers.Ident.mySioStartPage()
          val txt = MsgCodes.`Login.to.sio`
          (r, txt)
        }
      } { myAdnNodeId =>
        // Ссылка прямо в личный кабинет узла.
        val r = scRoutes.controllers.MarketLkAdn.showNodeAds( myAdnNodeId )
        val txt = MsgCodes.`Go.to.node.ads`
        (r, txt)
      }

      // Ссылка на вход или на личный кабинет
      <.div(
        menuCss.Rows.rowOuter,

        <.a(
          menuCss.Rows.rowLink,

          // Сборка ссылки для входа туда, куда наверное хочет попасть пользователь.
          ^.href := hrefRoute.url,

          <.div(
            menuCss.Rows.rowContent,
            Messages( msgCode )
          )
        )
      )

    }
  }

  val component = ScalaComponent.builder[Props]("EnterLk")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(propsValProxy: Props) = component(propsValProxy)

}
