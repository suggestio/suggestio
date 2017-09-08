package io.suggest.quill.v

import com.github.zenoamaro.react.quill._
import com.quilljs.delta.Delta
import diode.FastEq
import diode.react.ModelProxy
import io.suggest.css.Css
import io.suggest.quill.m.TextChanged
import io.suggest.quill.u.QuillInit
import io.suggest.react.ReactCommonUtil
import io.suggest.react.ReactCommonUtil.Implicits._
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
  * Description: react-компонент для редактирования текста.
  */
class QuillEditorR(
                    quillInit: QuillInit
                  )
  extends Log {

  case class PropsVal(
                       qDelta   : Delta
                     )
  implicit object PropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      a.qDelta eq b.qDelta
    }
  }


  type Props = ModelProxy[Option[PropsVal]]


  class Backend($: BackendScope[Props, Unit]) {

    /** Callback реагирования на изменение текста в редакторе. */
    private def onTextChanged(html: String, changeset: Delta, source: Source_t,
                              editorProxy: QuillUnpriveledged): Callback = {
      dispatchOnProxyScopeCB($, TextChanged(
        fullDelta = editorProxy.getContents()
      ))
    }

    private val _onTextChangedF = ReactCommonUtil.cbFun4ToJsCb( onTextChanged )

    private val _quillModulesConf = quillInit.adEditorModules

    def render(propsProxy: Props): VdomElement = {
      val propsOpt = propsProxy.value

      propsOpt.whenDefinedEl { props =>
        <.div(
          ^.classSet(
            Css.Display.HIDDEN -> propsOpt.isEmpty
          ),

          // Защита от нечитабельных или непонятных багрепортов в console при проблемах инициализации компонента.
          try {
            ReactQuill(
              new ReactQuillPropsR {
                override val value    = props.qDelta
                override val onChange = _onTextChangedF
                override val modules  = _quillModulesConf
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
