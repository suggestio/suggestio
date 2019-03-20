package io.suggest.sc.v.menu

import chandu0101.scalajs.react.components.materialui.{MuiListItem, MuiListItemProps, MuiListItemText}
import diode.FastEq
import diode.react.ModelProxy
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.proto.http.model.Route
import io.suggest.routes.IJsRouter
import io.suggest.sc.styl.ScCssStatic
import japgolly.scalajs.react.{BackendScope, React, ScalaComponent}
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.sc.m.MScReactCtx
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
                   scReactCtxP            : React.Context[MScReactCtx],
                   commonReactCtxProv     : React.Context[MCommonReactCtx],
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
        val R = ScCssStatic.Menu.Rows

        val (hrefRoute, msgCode) = props.isMyAdnNodeId.fold [(Route, String)] {
          if (props.isLoggedIn) {
            // Ссылка на личный кабинет
            val r = props.scJsRouter.controllers.Ident.rdrUserSomewhere()
            val txt = MsgCodes.`Personal.cabinet`
            (r, txt)

          } else {
            // Ссылка на вход в s.io.
            val r = props.scJsRouter.controllers.Ident.loginFormPage()
            val txt = MsgCodes.`Login.page.title`
            (r, txt)
          }
        } { myAdnNodeId =>
          // Ссылка прямо в личный кабинет узла.
          //val r = props.scJsRouter.controllers.MarketLkAdn.showNodeAds( myAdnNodeId )
          // TODO Nodes Тут опасность: id узла используется как ключ узла.
          val r = props.scJsRouter.controllers.LkAds.adsPage( myAdnNodeId )
          val txt = MsgCodes.`Go.to.node.ads`
          (r, txt)
        }

        // Сборка ссылки для входа туда, куда наверное хочет попасть пользователь.
        scReactCtxP.consume { scReactCtx =>
          <.a(
            R.rowLink,
            ^.href := hrefRoute.url,

            // Ссылка на вход или на личный кабинет
            MuiListItem(
              new MuiListItemProps {
                override val disableGutters = true
                override val button = true
              }
            )(
              MuiListItemText()(
                <.span(
                  R.rowContent,
                  scReactCtx.scCss.fgColor,
                  commonReactCtxProv.consume { crCtx =>
                    crCtx.messages( msgCode )
                  }
                )
              )
            )
          )
        }

      }

    }
  }

  val component = ScalaComponent.builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

  def apply(propsValProxy: Props) = component(propsValProxy)

}
