package io.suggest.sc.v.menu

import com.materialui.{MuiListItem, MuiListItemText}
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.proto.http.client.HttpClient
import io.suggest.ueq.UnivEqUtil._
import japgolly.scalajs.react.{BackendScope, React, ScalaComponent}
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.routes.IJsRouter
import io.suggest.sc.m.MScRoot
import io.suggest.sc.v.styl.{ScCss, ScCssStatic}
import scalacss.ScalaCssReact._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.03.18 18:39
  * Description: Кнопка редактирования текущей открытой карточки.
  */
class EditAdR(
               menuItemR     : MenuItemR,
               scCssP        : React.Context[ScCss],
               crCtxProv     : React.Context[MCommonReactCtx],
               jsRouterOptP  : React.Context[Option[IJsRouter]],
             ) {

  type Props_t = MScRoot
  type Props = ModelProxy[Props_t]

  case class State(
                    adIdOptC : ReactConnectProxy[Option[String]],
                  )


  class Backend($: BackendScope[Props, State]) {

    def render(s: State): VdomElement = {
      import ScCssStatic.Menu.{Rows => R}

      lazy val content = MuiListItem(
        menuItemR.MENU_LIST_ITEM_PROPS
      )(
        MuiListItemText()(
          {
            val span0 = <.span(
              R.rowContent,
              crCtxProv.message( MsgCodes.`Edit` ),
            )
            scCssP.consume { scCss =>
              span0(
                scCss.fgColor,
              )
            }
          }
        )
      )

      // Ссылка на вход или на личный кабинет
      jsRouterOptP.consume { jsRouterOpt =>
        jsRouterOpt.whenDefinedEl { jsRouter =>
          s.adIdOptC { adIdOptProxy =>
            adIdOptProxy.value.whenDefinedEl { adId =>
              <.a(
                R.rowLink,
                ^.href := HttpClient.mkAbsUrlIfPreferred( jsRouter.controllers.LkAdEdit.editAd( adId ).url ),
                content,
              )
            }
          }
        }
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(

        adIdOptC = propsProxy.connect { props =>
          val gridAds = props.grid.core.ads
          (for {
            focLoc <- gridAds.interactAdOpt
            scAd   = focLoc.getLabel
            adData <- scAd.data.toOption
            if adData.info.canEdit
          } yield {
            // Чтобы гарантировать референсную целостность результата, не раскрываем финальный Option nodeId.
            adData.doc.tagId.nodeId
          })
            .flatten
        },

      )
    }
    .renderBackend[Backend]
    .build

}
