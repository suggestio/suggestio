package io.suggest.sc.v.menu

import com.materialui.{MuiListItem, MuiListItemProps, MuiListItemText}
import diode.FastEq
import diode.react.ModelProxy
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.sc.styl.ScCssStatic
import io.suggest.ueq.UnivEqUtil._
import japgolly.scalajs.react.{BackendScope, React, ScalaComponent}
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.routes.IJsRouter
import io.suggest.sc.m.MScReactCtx
import scalacss.ScalaCssReact._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.03.18 18:39
  * Description: Кнопка редактирования текущей открытой карточки.
  */
class EditAdR(
               scReactCtxP   : React.Context[MScReactCtx],
               crCtxProv     : React.Context[MCommonReactCtx],
             ) {

  type Props_t = Option[PropsVal]
  type Props = ModelProxy[Props_t]

  case class PropsVal(
                       adId         : String,
                       scRoutes     : IJsRouter
                     )

  implicit object EditAdRPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.adId ===* b.adId) &&
      (a.scRoutes eq b.scRoutes)
    }
  }


  class Backend($: BackendScope[Props, Unit]) {

    def render(propsValProxy: Props): VdomElement = {
      propsValProxy.value.whenDefinedEl { props =>
        val R = ScCssStatic.Menu.Rows
        <.a(
          R.rowLink,
          ^.href := props.scRoutes.controllers.LkAdEdit.editAd( props.adId ).url,

          // Ссылка на вход или на личный кабинет
          MuiListItem(
            new MuiListItemProps {
              override val disableGutters = true
              override val button = true
            }
          )(
            MuiListItemText()(
              scReactCtxP.consume { scReactCtx =>
                <.span(
                  R.rowContent,
                  scReactCtx.scCss.fgColor,
                  crCtxProv.message( MsgCodes.`Edit` ),
                )
              }
            )
          )
        )
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

  def apply(propsValProxy: Props) = component( propsValProxy )

}
