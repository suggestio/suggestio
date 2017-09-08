package io.suggest.ad.edit.v

import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.ad.edit.m.MAdEditRoot
import io.suggest.ad.edit.v.edit.strip.StripEditR
import io.suggest.css.Css
import io.suggest.jd.render.m.MJdArgs
import io.suggest.jd.render.v.{JdCss, JdCssR, JdR}
import io.suggest.jd.tags.Strip
import io.suggest.quill.v.{QuillCss, QuillEditorR}
import japgolly.scalajs.react.{BackendScope, ScalaComponent}
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.css.ScalaCssDefaults._

import scalacss.ScalaCssReact._
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
                     val stripEditR     : StripEditR,
                     lkAdEditCss        : LkAdEditCss,
                     quillCssFactory    : => QuillCss,
                     val quillEditorR   : QuillEditorR
                   ) {

  import MJdArgs.MJdWithArgsFastEq
  import quillEditorR.PropsValFastEq
  import stripEditR.StripEditRPropsValFastEq

  type Props = ModelProxy[MAdEditRoot]

  /** Состояние компонента содержит model-коннекшены для подчинённых компонентов. */
  protected case class State(
                              jdPreviewArgsC    : ReactConnectProxy[MJdArgs],
                              jdCssArgsC        : ReactConnectProxy[JdCss],
                              stripEdOptC       : ReactConnectProxy[Option[stripEditR.PropsVal]],
                              quillEdOptC       : ReactConnectProxy[Option[quillEditorR.PropsVal]]
                            )

  protected class Backend($: BackendScope[Props, State]) {

    def render(p: Props, s: State): VdomElement = {
      val LCSS = lkAdEditCss.Layout
      <.div(
        ^.`class` := Css.Overflow.HIDDEN,

        // Отрендерить доп.стили для quill-редактора.
        <.styleTag(
          quillCssFactory.render[String]
        ),

        // Отрендерить стили редактора.
        <.styleTag(
          lkAdEditCss.render[String]
        ),

        // Рендер css
        s.jdCssArgsC { jdCssR.apply },

        <.div(
          LCSS.outerCont,

          // Рендер preview
          <.div(
            LCSS.previewOuterCont,

            <.div(
              LCSS.previewInnerCont,

              // Тело превьюшки
              s.jdPreviewArgsC { jdR.apply },

              <.div(
                ^.`class` := Css.CLEAR
              )
            )
          ),

          // Рендер редакторов
          <.div(
            LCSS.editorsCont,

            // Редактор strip'а
            s.stripEdOptC { stripEditR.apply },

            // Редактор текста
            s.quillEdOptC { quillEditorR.apply }
          )

        )
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

        stripEdOptC = p.connect { mroot =>
          for (stripEd <- mroot.doc.stripEd; selJd <- mroot.doc.jdArgs.selectedTag) yield {
            stripEditR.PropsVal(
              strip = selJd.asInstanceOf[Strip],
              edS   = stripEd
            )
          }
        }( OptFastEq.Wrapped ),

        quillEdOptC = p.connect { mroot =>
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
