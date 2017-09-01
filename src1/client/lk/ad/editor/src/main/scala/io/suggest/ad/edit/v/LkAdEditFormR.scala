package io.suggest.ad.edit.v

import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.ad.edit.m.MAdEditRoot
import io.suggest.ad.edit.v.edit.strip.StripEditR
import io.suggest.jd.render.m.MJdArgs
import io.suggest.jd.render.v.{JdCss, JdCssR, JdR}
import io.suggest.jd.tags.Strip
import io.suggest.quill.v.QuillEditorR
import japgolly.scalajs.react.{BackendScope, ScalaComponent}
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.sjs.common.spa.OptFastEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.08.17 21:56
  * Description: React-компонент всей формы react-редактора карточек.
  */
class LkAdEditFormR(
                     jdCssR             : JdCssR,
                     jdR                : JdR,
                     stripEditR         : StripEditR,
                     val quillEditorR   : QuillEditorR
                   ) {

  import MJdArgs.MJdWithArgsFastEq
  import quillEditorR.PropsValFastEq

  type Props = ModelProxy[MAdEditRoot]

  /** Состояние компонента содержит model-коннекшены для подчинённых компонентов. */
  protected case class State(
                              jdPreviewArgsC    : ReactConnectProxy[MJdArgs],
                              jdCssArgsC        : ReactConnectProxy[JdCss],
                              currStripOptC     : ReactConnectProxy[Option[Strip]],
                              textPropsOptC     : ReactConnectProxy[Option[quillEditorR.PropsVal]]
                            )

  protected class Backend($: BackendScope[Props, State]) {

    def render(p: Props, s: State): VdomElement = {
      <.div(

        // Рендер css
        s.jdCssArgsC { jdCssR.apply },

        // Рендер preview
        s.jdPreviewArgsC { jdR.apply },


        // Рендер редакторов
        // Редактор strip'а
        s.currStripOptC { stripEditR.apply },

        // Редактор текста
        s.textPropsOptC { quillEditorR.apply }

      )
    }

  }


  val component = ScalaComponent.builder[Props]("AdEd")
    .initialStateFromProps { p =>
      State(
        jdPreviewArgsC = p.connect { mroot =>
          mroot.doc.jdArgs
        },

        jdCssArgsC = p.connect { mroot =>
          mroot.doc.jdArgs.jdCss
        },

        currStripOptC = p.connect { mroot =>
          mroot.doc.jdArgs.selectedTag.flatMap {
            case s: Strip => Some(s)
            case _ => None
          }
        }( OptFastEq.Plain ),

        textPropsOptC = p.connect { mroot =>
          for {
            qDelta  <- mroot.doc.qDelta
          } yield {
            quillEditorR.PropsVal(
              qDelta = qDelta
            )
          }
        }( OptFastEq.Wrapped )
      )
    }
    .renderBackend[Backend]
    .build

  def apply(rootProxy: Props) = component(rootProxy)

}
