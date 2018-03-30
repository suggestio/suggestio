package io.suggest.sc.v.menu

import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProps}
import io.suggest.i18n.MsgCodes
import io.suggest.msg.Messages
import io.suggest.routes.IJsRouter
import io.suggest.sc.styl.GetScCssF
import io.suggest.sjs.common.model.Route
import japgolly.scalajs.react.{BackendScope, ScalaComponent}
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.react.ReactCommonUtil.Implicits._
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._
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

  type Props_t = Option[PropsVal]
  type Props = ModelProxy[Props_t]


  case class PropsVal(
                       isLoggedIn     : Boolean,
                       isMyAdnNodeId  : Option[String],
                       scJsRouter     : IJsRouter
                     )

  implicit object EnterLkRowRPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.isLoggedIn ==* b.isLoggedIn) &&
        (a.isMyAdnNodeId ===* b.isMyAdnNodeId) &&
        (a.scJsRouter eq b.scJsRouter)
    }
  }


  class Backend($: BackendScope[Props, Unit]) {

    def render(propsProxy: Props): VdomElement = {
      propsProxy.value.whenDefinedEl { props =>
        val menuRowsCss = getScCssF().Menu.Rows

        val (hrefRoute, msgCode) = props.isMyAdnNodeId.fold [(Route, String)] {
          if (props.isLoggedIn) {
            // Ссылка на личный кабинет
            val r = props.scJsRouter.controllers.Ident.rdrUserSomewhere()
            val txt = MsgCodes.`Personal.cabinet`
            (r, txt)

          } else {
            // Ссылка на вход в s.io
            val r = props.scJsRouter.controllers.Ident.mySioStartPage()
            val txt = MsgCodes.`Login.page.title`
            (r, txt)
          }
        } { myAdnNodeId =>
          // Ссылка прямо в личный кабинет узла.
          //val r = props.scJsRouter.controllers.MarketLkAdn.showNodeAds( myAdnNodeId )
          // TODO Тут опасность: id узла используется как ключ узла.
          val r = props.scJsRouter.controllers.LkAds.adsPage( myAdnNodeId )
          val txt = MsgCodes.`Go.to.node.ads`
          (r, txt)
        }

        // Ссылка на вход или на личный кабинет
        <.div(
          menuRowsCss.rowOuter,

          <.a(
            menuRowsCss.rowLink,

            // Сборка ссылки для входа туда, куда наверное хочет попасть пользователь.
            ^.href := hrefRoute.url,

            <.div(
              menuRowsCss.rowContent,
              Messages( msgCode )
            )
          )
        )
      }

    }
  }

  val component = ScalaComponent.builder[Props]("EnterLk")
    .stateless
    .renderBackend[Backend]
    .build

  private def _apply(propsValProxy: Props) = component(propsValProxy)
  val apply: ReactConnectProps[Props_t] = _apply

}
