package io.suggest.ad.edit.v.edit.strip

import diode.react.ModelProxy
import io.suggest.ad.edit.m.edit.MStripEdS
import io.suggest.ad.edit.m.{StripDelete, StripDeleteCancel}
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.i18n.MsgCodes
import io.suggest.msg.Messages
import japgolly.scalajs.react.{BackendScope, Callback, ScalaComponent}
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.react.ReactCommonUtil.VdomNullElement
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.09.17 22:43
  * Description: React-компонент кнопки удаления текущего блока.
  */
class DeleteStripBtnR {

  type Props_t = Option[MStripEdS]
  type Props = ModelProxy[Props_t]

  class Backend($: BackendScope[Props, Unit]) {

    /** callback клика по кнопке удаления узла. */
    private def onDeleteBtnClick(confirmed: Boolean): Callback = {
      dispatchOnProxyScopeCB($, StripDelete(confirmed))
    }

    private def onCancelBtnClick: Callback = {
      dispatchOnProxyScopeCB($, StripDeleteCancel)
    }

    private def _delBtn(text: String, confirmed: Boolean) = {
      val C = Css.Buttons
      <.a(
        ^.`class` := Css.flat( C.BTN, Css.Size.M, C.NEGATIVE ),
        ^.onClick --> onDeleteBtnClick(confirmed),
        text
      )
    }


    def render(p: Props): VdomElement = {
      p.value.whenDefinedEl { stripEd =>
        if (stripEd.isLastStrip) {
          VdomNullElement
        } else {
          <.div(
            ^.`class` := Css.Display.INLINE_BLOCK,

            if (stripEd.confirmingDelete) {
              // Идёт подтверждение удаления текущего блока.
              val C = Css.Buttons
              <.div(
                Messages( MsgCodes.`Are.you.sure` ),
                <.a(
                  ^.`class` := Css.flat( C.BTN, Css.Size.M, C.MINOR ),
                  ^.onClick --> onCancelBtnClick,
                  Messages( MsgCodes.`Cancel` )
                ),
                _delBtn(
                  Messages(MsgCodes.`Yes.delete.it`),
                  confirmed = true
                )
              )

            } else {
              // Просто отобразить кнопку удаления текущего блока, диалог подтверждения удаления сейчас отсутствует.
              // TODO Вместо Delete надо бы Delete.current.strip
              _delBtn(
                Messages(MsgCodes.`Delete.block`) + HtmlConstants.ELLIPSIS,
                confirmed = false
              )
            }
          )
        }
      }

    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

  def apply(stripEdSOptProxy: Props) = component( stripEdSOptProxy )

}
