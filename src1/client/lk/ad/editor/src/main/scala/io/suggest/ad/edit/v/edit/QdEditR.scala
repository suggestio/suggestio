package io.suggest.ad.edit.v.edit

import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.ad.edit.m.edit.color.{MColorPick, MColorsState}
import io.suggest.ad.edit.m.edit.MQdEditS
import io.suggest.ad.edit.v.LkAdEditCss
import io.suggest.ad.edit.v.edit.color.ColorCheckboxR
import io.suggest.color.MColorData
import io.suggest.i18n.MsgCodes
import io.suggest.quill.v.QuillEditorR
import io.suggest.sjs.common.i18n.Messages
import io.suggest.spa.OptFastEq
import japgolly.scalajs.react.{BackendScope, ScalaComponent}
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.09.17 22:33
  * Description: react-компонент редактирования quill-контента.
  */
class QdEditR(
               val quillEditorR   : QuillEditorR,
               val colorCheckboxR : ColorCheckboxR,
             ) {

  import quillEditorR.PropsValFastEq
  import colorCheckboxR.ColorCheckboxPropsValFastEq

  case class PropsVal(
                       qdEdit       : MQdEditS,
                       bgColor      : Option[MColorData]
                     )
  implicit object QdEditRPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.qdEdit eq b.qdEdit) &&
        (a.bgColor eq b.bgColor)
    }
  }

  type Props = ModelProxy[Option[PropsVal]]

  /** Класс модели внутреннего состояния компонента. */
  protected case class State(
                              quillEdOptC                   : ReactConnectProxy[Option[quillEditorR.PropsVal]],
                              bgColorCheckBoxPropsOptC      : ReactConnectProxy[Option[colorCheckboxR.PropsVal]]
                            )


  /** Ядро всего компонента. */
  class Backend($: BackendScope[Props, State]) {

    def render(p: Props, s: State): VdomElement = {
      <.div(

        // Редактор текста
        s.quillEdOptC { quillEditorR.apply },

        <.br,

        // Цвет фона контента.
        s.bgColorCheckBoxPropsOptC {
          colorCheckboxR(_)(
            Messages( MsgCodes.`Bg.color` )
          )
        }

      )
    }

  }


  val component = ScalaComponent.builder[Props]("QdEd")
    .initialStateFromProps { propsOptProxy =>
      State(
        // Коннекшен до дельты редактора
        quillEdOptC = propsOptProxy.connect { propsOpt =>
          for {
            props   <- propsOpt
          } yield {
            quillEditorR.PropsVal(
              initDelta = props.qdEdit.initDelta,
              realDelta = props.qdEdit.realDelta
            )
          }
        }( OptFastEq.Wrapped ),

        /*
        colorPickOptS = propsOptProxy.connect { propsOpt =>
          for (props <- propsOpt) yield {
            MColorPick(
              colorOpt    = props.bgColor,
              colorsState = props.colorsState,
              pickS       = props.qdEdit.bgColorPick
            )
          }
        }( OptFastEq.Wrapped )
        */

        bgColorCheckBoxPropsOptC = propsOptProxy.connect { propsOpt =>
          for (props <- propsOpt) yield {
            colorCheckboxR.PropsVal(
              color = props.bgColor
            )
          }
        }( OptFastEq.Wrapped )

      )
    }
    .renderBackend[Backend]
    .build

  def apply(qdEditOptProxy: Props) = component(qdEditOptProxy)

}
