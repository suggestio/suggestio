package io.suggest.ad.edit.v

import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.ad.edit.m.{DocBodyClick, MAeRoot}
import io.suggest.ad.edit.m.edit.MAddS
import io.suggest.ad.edit.v.edit.{AddR, QdEditR}
import io.suggest.ad.edit.v.edit.strip.StripEditR
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.jd.render.m.MJdArgs
import io.suggest.jd.render.v.{JdCss, JdCssR, JdR}
import io.suggest.quill.v.QuillCss
import japgolly.scalajs.react.{BackendScope, Callback, ScalaComponent}
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.css.ScalaCssDefaults._
import io.suggest.i18n.{MMessage, MsgCodes}
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.sjs.common.i18n.Messages

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
                     addR               : AddR,
                     val stripEditR     : StripEditR,
                     lkAdEditCss        : LkAdEditCss,
                     quillCssFactory    : => QuillCss,
                     val qdEditR        : QdEditR
                   ) {

  import MJdArgs.MJdWithArgsFastEq
  import stripEditR.StripEditRPropsValFastEq
  import MAddS.MAddSFastEq
  import qdEditR.QdEditRPropsValFastEq

  type Props = ModelProxy[MAeRoot]

  /** Состояние компонента содержит model-коннекшены для подчинённых компонентов. */
  protected case class State(
                              jdPreviewArgsC    : ReactConnectProxy[MJdArgs],
                              jdCssArgsC        : ReactConnectProxy[JdCss],
                              addC              : ReactConnectProxy[Option[MAddS]],
                              stripEdOptC       : ReactConnectProxy[Option[stripEditR.PropsVal]],
                              qdEditOptC        : ReactConnectProxy[Option[qdEditR.PropsVal]],
                              errorsC           : ReactConnectProxy[List[MMessage]]
                            )

  protected class Backend($: BackendScope[Props, State]) {

    private def _onClick: Callback = {
      dispatchOnProxyScopeCB($, DocBodyClick)
    }

    def render(p: Props, s: State): VdomElement = {
      val LCSS = lkAdEditCss.Layout
      <.div(
        ^.`class` := Css.Overflow.HIDDEN,

        // TODO Opt спиливать onClick, когда по состоянию нет ни одного открытого modal'а, например открытого color-picker'а.
        ^.onClick --> _onClick,

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

            // Рендер ошибок редактирования.
            s.errorsC { errorsProxy =>
              val errors = errorsProxy.value
              errors.headOption.whenDefinedEl { _ =>
                <.div(
                  <.strong(
                    Messages( MsgCodes.`Error` ),
                    ":"
                  ),

                  <.ul(
                    errors.iterator.zipWithIndex.toVdomArray { case (mmsg, i) =>
                      <.li(
                        ^.key     := i.toString,
                        ^.`class` := Css.Colors.RED,
                        Messages( mmsg )
                      )
                    }
                  )
                )
              }
            },

            // Редактор strip'а
            s.stripEdOptC { stripEditR.apply },

            // Редактор текста
            s.qdEditOptC { qdEditR.apply },

            <.br,

            // Форма добавления новых элементов.
            s.addC { addR.apply }

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

        addC = p.connect { mroot =>
          mroot.doc.addS
        }(OptFastEq.Wrapped),

        stripEdOptC = p.connect { mroot =>
          for {
            stripEd <- mroot.doc.stripEd
            selJd   <- mroot.doc.jdArgs.selectedTag
          } yield {
            stripEditR.PropsVal(
              strip         = selJd,
              edS           = stripEd,
              colorsState   = mroot.doc.colorsState,
              bgImgFileOpt  = selJd.props1
                .bgImg
                .flatMap { ei =>
                  mroot.doc.files.get(ei.edgeUid)
                }
            )
          }
        }( OptFastEq.Wrapped ),

        qdEditOptC = p.connect { mroot =>
          for {
            qdEdit <- mroot.doc.qdEdit
            selJd  <- mroot.doc.jdArgs.selectedTag
          } yield {
            qdEditR.PropsVal(
              qdEdit      = qdEdit,
              bgColor     = selJd.props1.bgColor,
              colorsState = mroot.doc.colorsState
            )
          }
        }( OptFastEq.Wrapped ),

        errorsC = p.connect { mroot =>
          mroot.doc.errors
        }
      )
    }
    .renderBackend[Backend]
    .build

  def apply(rootProxy: Props) = component(rootProxy)

}
