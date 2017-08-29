package io.suggest.ad.edit.v.v.edit.text

import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.css.Css
import io.suggest.jd.tags.Text
import io.suggest.sjs.common.log.Log
import japgolly.scalajs.react.{BackendScope, ScalaComponent}
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import react.tinymce.{TinyMcePropsR, TinyMceR}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.08.17 21:50
  * Description: react-компонент для редактирования текстового тега.
  */
class TextEditR extends Log {

  type Props = ModelProxy[Option[Text]]

  protected case class State(
                              htmlContentStrC     : ReactConnectProxy[String]
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
          s.htmlContentStrC { htmlContentStrProxy =>
            TinyMceR(
              new TinyMcePropsR {
                override val content = htmlContentStrProxy.value
              }
            )
          }
        } catch {
          case ex: Throwable =>
            LOG.error("TinyMCE!", ex, props)
            EmptyVdom
        }

      )
    }

  }

  val component = ScalaComponent.builder[Props]("TextEd")
    .initialStateFromProps { propsOptProxy =>
      State(
        htmlContentStrC = propsOptProxy.connect { propsOpt =>
          propsOpt.fold("")(_.toString)
        }
      )
    }
    .renderBackend[Backend]
    .build

  def apply(textOptProxy: Props) = component( textOptProxy )

}
