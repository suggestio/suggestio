package io.suggest.ad.edit.v.edit.text

import com.github.zenoamaro.react.quill._
import com.quilljs.delta.Delta
import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.ad.edit.m.TextChanged
import io.suggest.css.Css
import io.suggest.jd.tags.Text
import io.suggest.react.ReactCommonUtil
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.common.msg.ErrorMsgs
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, ScalaComponent}

import scala.scalajs.js
import scala.scalajs.js.JSON

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.08.17 21:50
  * Description: react-компонент для редактирования текстового тега.
  */
class TextEditR extends Log {

  case class PropsVal(
                       jdText   : Text,
                       qDelta   : Delta
                     )
  implicit object PropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.jdText eq b.jdText) &&
        (a.qDelta eq b.qDelta)
    }
  }


  type Props = ModelProxy[Option[PropsVal]]

  protected case class State(
                              quillValueC: ReactConnectProxy[ContentValue_t]
                            )


  class Backend($: BackendScope[Props, State]) {

    /** Callback реагирования на изменение текста в редакторе. */
    private def onTextChanged(html: String, changeset: Delta, source: Source_t,
                              editorProxy: QuillUnpriveledged): Callback = {
      dispatchOnProxyScopeCB($, TextChanged(
        fullDelta = editorProxy.getContents(),
        html      = html
      ))
    }

    private val _onTextChangedF = ReactCommonUtil.cbFun4ToJsCb( onTextChanged )

    def render(propsProxy: Props, s: State): VdomElement = {
      val props = propsProxy.value

      <.div(
        ^.classSet(
          Css.Display.HIDDEN -> props.isEmpty
        ),

        // tinymce: редактор работает как сингтон, в котором появляется или исчезает содержимое.
        try {
          s.quillValueC { quillValueProxy =>
            ReactQuill(
              new ReactQuillPropsR {
                override val value    = quillValueProxy.value
                override val onChange = _onTextChangedF
              }
            )
          }
        } catch {
          case ex: Throwable =>
            LOG.error(ErrorMsgs.EXT_COMP_INIT_FAILED, ex, props)
            EmptyVdom
        }
      )
    }

  }


  val component = ScalaComponent.builder[Props]("TextEd")
    .initialStateFromProps { propsOptProxy =>
      State(
        quillValueC = propsOptProxy.connect { propsOpt =>
          propsOpt.fold[ContentValue_t]("")(_.qDelta)
        }
      )
    }
    .renderBackend[Backend]
    .build

  def apply(textOptProxy: Props) = component( textOptProxy )

}
