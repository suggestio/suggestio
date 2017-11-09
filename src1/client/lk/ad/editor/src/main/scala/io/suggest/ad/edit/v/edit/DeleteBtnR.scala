package io.suggest.ad.edit.v.edit

import diode.FastEq
import diode.react.ModelProxy
import io.suggest.ad.edit.m.DeleteAdClick
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.i18n.MsgCodes
import io.suggest.lk.m.MDeleteConfirmPopupS
import io.suggest.lk.r.LkPreLoaderR
import japgolly.scalajs.react.{BackendScope, Callback, ScalaComponent}
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.sjs.common.i18n.Messages
import io.suggest.ueq.UnivEqUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.11.17 18:38
  * Description: Компонент кнопки удаления рекламной карточки.
  */
class DeleteBtnR {

  type Props = ModelProxy[Option[PropsVal]]

  case class PropsVal(
                       deleteConfirm: Option[MDeleteConfirmPopupS]
                     )

  implicit object DeleteBtnRPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      a.deleteConfirm ===* b.deleteConfirm
    }
  }

  class Backend($: BackendScope[Props, Unit]) {

    private def _onDeleteBtnClick: Callback = {
      dispatchOnProxyScopeCB($, DeleteAdClick)
    }

    def render(p: Props): VdomElement = {
      p.value
        .filterNot(_.deleteConfirm.exists(_.request.isReady))
        .whenDefinedEl { props =>
        val isPending = props.deleteConfirm.exists(_.request.isPending)

          <.div(
            ^.`class` := Css.Floatt.LEFT,

            <.a(
              ^.classSet1(
                Css.flat( Css.Buttons.BTN, Css.Size.M ),
                Css.Buttons.NEGATIVE  -> !isPending,
                Css.Buttons.MINOR     -> isPending
              ),

              // НЕ слать экшены, если сейчас идёт запрос.
              if (isPending) {
                EmptyVdom
              } else {
                ^.onClick --> _onDeleteBtnClick
              },

              // Какой текст выводить, когда идёт сохранение.
              if (isPending) {
                <.span(
                  LkPreLoaderR.AnimSmall
                )
              } else {
                <.span(
                  Messages( MsgCodes.`Delete` ),
                  HtmlConstants.ELLIPSIS
                )
              }
            ),

            props.deleteConfirm
              .flatMap(_.request.exceptionOption)
              .whenDefined { ex =>
                <.span(
                  ^.title := ex.getClass.getName + HtmlConstants.SPACE + ex.getMessage,
                  ^.`class` := Css.Colors.RED,
                  Messages( MsgCodes.`Error` ),
                  HtmlConstants.SPACE,
                  ex.getMessage
                )
              }

          )
      }
    }
  }

  val component = ScalaComponent.builder[Props]("DelBtn")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(propsOptProxy: Props) = component( propsOptProxy )

}
