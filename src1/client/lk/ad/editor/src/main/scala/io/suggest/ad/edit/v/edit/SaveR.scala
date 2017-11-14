package io.suggest.ad.edit.v.edit

import diode.FastEq
import diode.data.Pot
import diode.react.ModelProxy
import io.suggest.ad.edit.m.SaveAd
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.i18n.MsgCodes
import io.suggest.lk.r.LkPreLoaderR
import io.suggest.ueq.UnivEqUtil._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.sjs.common.i18n.Messages
import io.suggest.ueq.JsUnivEqUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.10.17 16:12
  * Description: React-компонент с кнопкой сохранения карточки.
  */
class SaveR {

  /** Пропертисы для рендера компонента кнопки.
    *
    * @param currentReq Pot текущего реквеста сохранения, если есть.
    */
  case class PropsVal(
                       currentReq: Pot[_]
                     )
  implicit object SaveRPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      a.currentReq ===* b.currentReq
    }
  }

  type Props = ModelProxy[PropsVal]

  class Backend($: BackendScope[Props, Unit]) {

    /** Клик по кнопке сохранения. */
    private def onSaveBtnClick: Callback = {
      dispatchOnProxyScopeCB($, SaveAd)
    }

    def render(propsProxy: Props): VdomElement = {
      val p = propsProxy.value
      val isPending = p.currentReq.isPending

      <.div(
        ^.`class` := Css.Floatt.RIGHT,

        <.a(
          ^.classSet1(
            Css.flat( Css.Buttons.BTN, Css.Size.M ),
            Css.Buttons.MAJOR -> !isPending,
            Css.Buttons.MINOR -> isPending
          ),

          // НЕ слать экшены, если сейчас идёт запрос.
          if (isPending) {
            EmptyVdom
          } else {
            ^.onClick --> onSaveBtnClick
          },

          // Какой текст выводить, когда идёт сохранение.
          if (isPending) {
            <.span(
              LkPreLoaderR.AnimSmall,
              HtmlConstants.SPACE,
              Messages(MsgCodes.`Saving`),
              HtmlConstants.ELLIPSIS
            )
          } else if (p.currentReq.isReady) {
            <.span(
              Messages( MsgCodes.`Saved` ),
              HtmlConstants.`.`
            )
          } else {
            Messages( MsgCodes.`Save` )
          }
        ),

        p.currentReq.exceptionOption.whenDefined { ex =>
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


  val component = ScalaComponent.builder[Props]( MsgCodes.`Save` )
    .stateless
    .renderBackend[Backend]
    .build

  def apply(propsProxy: Props) = component( propsProxy )

}
