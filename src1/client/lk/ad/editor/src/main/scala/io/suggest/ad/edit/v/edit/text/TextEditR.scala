package io.suggest.ad.edit.v.edit.text

import com.github.zenoamaro.react.quill._
import com.quilljs.delta.Delta
import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.ad.edit.m.TextChanged
import io.suggest.css.Css
import io.suggest.react.ReactCommonUtil, ReactCommonUtil.Implicits._
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.common.msg.ErrorMsgs
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, ScalaComponent}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.08.17 21:50
  * Description: react-компонент для редактирования текстового тега.
  */
class TextEditR extends Log {

  case class PropsVal(
                       qDelta   : Delta
                     )
  implicit object PropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      true //(a.qDelta eq b.qDelta)
    }
  }


  type Props = ModelProxy[Option[PropsVal]]

  class Backend($: BackendScope[Props, Unit]) {

    /** Callback реагирования на изменение текста в редакторе. */
    private def onTextChanged(html: String, changeset: Delta, source: Source_t,
                              editorProxy: QuillUnpriveledged): Callback = {
      dispatchOnProxyScopeCB($, TextChanged(
        fullDelta = editorProxy.getContents(),
        html      = html
      ))
    }

    private val _onTextChangedF = ReactCommonUtil.cbFun4ToJsCb( onTextChanged )

    def render(propsProxy: Props): VdomElement = {
      val propsOpt = propsProxy.value

      propsOpt.whenDefinedEl { props =>
        <.div(
          ^.classSet(
            Css.Display.HIDDEN -> propsOpt.isEmpty
          ),

          // Защита нечитабельных багрепортов в console.
          try {
            ReactQuill(
              new ReactQuillPropsR {
                override val defaultValue = props.qDelta
                override val onChange = _onTextChangedF
              }
            )
          } catch {
            case ex: Throwable =>
              LOG.error(ErrorMsgs.EXT_COMP_INIT_FAILED, ex, props)
              EmptyVdom
          }
        )
      }

    }

  }


  val component = ScalaComponent.builder[Props]("TextEd")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(textOptProxy: Props) = component( textOptProxy )

}
