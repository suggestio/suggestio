package io.suggest.ad.edit.v.v.edit.text

import com.github.zenoamaro.react.quill.{ContentValue_t, ReactQuill, ReactQuillPropsR}
import com.quilljs.delta.Delta
import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.css.Css
import io.suggest.jd.tags.Text
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.common.msg.ErrorMsgs
import japgolly.scalajs.react.{BackendScope, ScalaComponent}
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._

import scala.scalajs.js.UndefOr

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
                override val value: UndefOr[ContentValue_t] = quillValueProxy.value
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
