package io.suggest.ad.edit.v.edit

import diode.FastEq
import diode.react.ModelProxy
import io.suggest.ad.edit.m.{MainStripChange, ShowMainStrips}
import io.suggest.ad.edit.v.LkAdEditCss
import io.suggest.common.html.HtmlConstants
import io.suggest.common.html.HtmlConstants.{`(`, `)`}
import io.suggest.css.Css
import io.suggest.i18n.MsgCodes
import io.suggest.msg.Messages
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, ReactEventFromInput, ScalaComponent}
import japgolly.univeq._

import scalacss.ScalaCssReact._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.11.17 12:57
  * Description: React-компонент чекбокса с управлением главными блоками.
  */
class UseAsMainR(
                  lkAdEditCss: LkAdEditCss
                ) {

  /** Модель пропертисов компонента.
    *
    * @param checked Текущее состояние чек-бокса.
    * @param mainDefined Кол-во блоков в карточке, которые могут быть использованы как заглавные.
    */
  case class PropsVal(
                       checked      : Boolean,
                       mainDefined  : Boolean
                     )
  /** Поддержка FastEq для инстансов PropsVal. */
  implicit object UseAdMainPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.checked ==* b.checked) &&
        (a.mainDefined ==* b.mainDefined)
    }
  }


  /** Тип приходящих пропертисов. */
  type Props = ModelProxy[Option[PropsVal]]


  class Backend($: BackendScope[Props, Unit]) {

    private def _onCheckedChange(e: ReactEventFromInput): Callback = {
      val isChecked = e.target.checked
      dispatchOnProxyScopeCB($, MainStripChange(isChecked))
    }

    private def _onShowAllMouseEnterLeave(isEnter: Boolean): Callback = {
      dispatchOnProxyScopeCB($, ShowMainStrips(isEnter))
    }

    def render(propsOptProxy: Props): VdomElement = {
      propsOptProxy.value.whenDefinedEl { props =>
        <.div(

          // Управляющий чекбокс.
          <.label(
            ^.`class` := Css.CLICKABLE,
            ^.title := Messages( MsgCodes.`Main.blocks.are.used.to.display.ad.inside.the.grid` ),

            <.input(
              ^.`type`    := HtmlConstants.Input.checkbox,
              ^.checked   := props.checked,
              ^.onChange ==> _onCheckedChange
            ),

            <.span(
              ^.`class` := Css.Input.STYLED_CHECKBOX
            ),

            <.span(
              ^.`class` := Css.flat(Css.Input.CHECKBOX_TITLE, Css.Buttons.MAJOR),
              Messages( MsgCodes.`Main.block` )
            )
          ),

          // Пояснение по поводу галочки.
          <.div(
            ^.`class` := Css.Lk.SM_NOTE,

            Messages {
              // Карточка содержит максимум один главный блок.
              if (!props.mainDefined) {
                MsgCodes.`No.main.blocks.First.block.is.main`
              } else if (props.checked) {
                // Этот блок -- едиственный главный
                MsgCodes.`This.block.is.the.only.main`
              } else {
                // Какой-то другой блок является главным.
                MsgCodes.`This.block.is.not.main`
              }
            },

            HtmlConstants.SPACE,

            // Управление подсветкой текущих стрипов
            <.span(
              lkAdEditCss.StripMain.showAll,
              ^.onMouseEnter --> _onShowAllMouseEnterLeave(true),
              ^.onMouseLeave --> _onShowAllMouseEnterLeave(false),
              `(`, Messages( MsgCodes.`Show` ), `)`
            )

          )

        )
      }

    }

  }


  val component = ScalaComponent.builder[Props]("UseAsMain")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(propsOptProxy: Props) = component( propsOptProxy )

}
