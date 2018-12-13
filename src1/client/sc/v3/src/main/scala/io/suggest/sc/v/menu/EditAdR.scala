package io.suggest.sc.v.menu

import chandu0101.scalajs.react.components.materialui.{MuiListItem, MuiListItemProps, MuiListItemText}
import diode.FastEq
import diode.react.ModelProxy
import io.suggest.i18n.MsgCodes
import io.suggest.msg.Messages
import io.suggest.sc.styl.{GetScCssF, ScCssStatic}
import io.suggest.ueq.UnivEqUtil._
import japgolly.scalajs.react.{BackendScope, ScalaComponent}
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.routes.IJsRouter
import scalacss.ScalaCssReact._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.03.18 18:39
  * Description: Кнопка редактирования текущей открытой карточки.
  */
class EditAdR(
               getScCssF  : GetScCssF
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
        val scCss = getScCssF()
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
              <.span(
                R.rowContent,
                scCss.fgColor,
                Messages( MsgCodes.`Edit` )
              )
            )
          )
        )

      }
    }
  }


  val component = ScalaComponent.builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

  def apply(propsValProxy: Props) = component( propsValProxy )

}
