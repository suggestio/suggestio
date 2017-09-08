package io.suggest.ad.edit.v.edit.strip

import diode.react.ModelProxy
import io.suggest.ad.edit.m.{DeleteCancel, DeleteStrip}
import io.suggest.ad.edit.m.edit.strip.MStripEdS
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.i18n.MsgCodes
import japgolly.scalajs.react.{BackendScope, Callback, ScalaComponent}
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.sjs.common.i18n.Messages

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.09.17 22:43
  * Description: React-компонент кнопки удаления текущего блока.
  */
class DeleteStripBtnR {

  type Props = ModelProxy[Option[MStripEdS]]

  class Backend($: BackendScope[Props, Unit]) {

    /** callback клика по кнопке удаления узла. */
    private def onDeleteBtnClick: Callback = {
      dispatchOnProxyScopeCB($, DeleteStrip)
    }

    private def onCancelBtnClick: Callback = {
      dispatchOnProxyScopeCB($, DeleteCancel)
    }

    private def _delBtn(text: String) = {
      val C = Css.Buttons
      <.a(
        ^.`class` := Css.flat( C.BTN, Css.Size.M, C.NEGATIVE ),
        ^.onClick --> onDeleteBtnClick,
        text
      )
    }

    def render(p: Props): VdomElement = {
      p.value.whenDefinedEl { stripEd =>
        <.div(

          if (stripEd.confirmDelete) {
            // Идёт подтверждение удаления текущего блока.
            val C = Css.Buttons
            <.div(
              Messages( MsgCodes.`Are.you.sure` ),
              _delBtn( Messages(MsgCodes.`Yes.delete.it`) ),
              <.a(
                ^.`class` := Css.flat( C.BTN, Css.Size.M, C.MINOR ),
                ^.onClick --> onCancelBtnClick,
                Messages( MsgCodes.`Cancel` )
              )
            )

          } else {
            // Просто отобразить кнопку удаления текущего блока, диалог подтверждения удаления сейчас отсутствует.
            // TODO Вместо Delete надо бы Delete.current.strip
            _delBtn( Messages(MsgCodes.`Delete`) + HtmlConstants.ELLIPSIS )
          }
        )
      }

    }

  }


  val component = ScalaComponent.builder[Props]("StripDel")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(stripEdSOptProxy: Props) = component( stripEdSOptProxy )

}
