package io.suggest.ad.edit.v

import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.ad.edit.m.edit.MAddS
import io.suggest.ad.edit.m.{DocBodyClick, MAeRoot}
import io.suggest.ad.edit.v.edit.strip.StripEditR
import io.suggest.ad.edit.v.edit.{AddR, PictureR, QdEditR, ScaleR}
import io.suggest.scalaz.ZTreeUtil._
import io.suggest.css.Css
import io.suggest.css.ScalaCssDefaults._
import io.suggest.dev.MSzMults
import io.suggest.jd.render.m.MJdArgs
import io.suggest.jd.render.v.{JdCss, JdCssR, JdR}
import io.suggest.quill.v.QuillCss
import io.suggest.common.html.HtmlConstants.{`(`, `)`, COMMA}
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.spa.OptFastEq
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, ScalaComponent}

import scalacss.ScalaCssReact._

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
                     val qdEditR        : QdEditR,
                     val scaleR         : ScaleR,
                     val pictureR       : PictureR
                   ) {

  import MAddS.MAddSFastEq
  import MJdArgs.MJdWithArgsFastEq
  import qdEditR.QdEditRPropsValFastEq
  import stripEditR.StripEditRPropsValFastEq
  import scaleR.ScaleRPropsValFastEq
  import pictureR.PictureRPropsValFastEq

  type Props = ModelProxy[MAeRoot]

  /** Состояние компонента содержит model-коннекшены для подчинённых компонентов. */
  protected case class State(
                              jdPreviewArgsC    : ReactConnectProxy[MJdArgs],
                              jdCssArgsC        : ReactConnectProxy[JdCss],
                              addC              : ReactConnectProxy[Option[MAddS]],
                              stripEdOptC       : ReactConnectProxy[Option[stripEditR.PropsVal]],
                              picPropsOptC      : ReactConnectProxy[Option[pictureR.PropsVal]],
                              qdEditOptC        : ReactConnectProxy[Option[qdEditR.PropsVal]],
                              scalePropsOptC    : ReactConnectProxy[Option[scaleR.PropsVal]],
                              rightYOptC        : ReactConnectProxy[Option[Int]]
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
          s.rightYOptC { rightYOptProxy =>
            <.div(
              LCSS.editorsCont,

              rightYOptProxy.value.whenDefined { rightY =>
                //^.paddingTop := rightY.px
                ^.transform := (Css.Anim.Transform.TRANSLATE + `(` + 0.px + COMMA + rightY.px + `)`)
              },

              // Редактор strip'а
              s.stripEdOptC { stripPropsOptProxy =>
                stripEditR(stripPropsOptProxy)(
                  // Управление картинкой, если доступно:
                  s.picPropsOptC { pictureR.apply }
                )
              },

              // Редактор текста
              s.qdEditOptC { qdEditR.apply },

              <.br,

              // Форма добавления новых элементов.
              s.addC { addR.apply },

              // Селектор масштаба карточки.
              s.scalePropsOptC { scaleR.apply }

            )
          }

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
            selJdt  <- mroot.doc.jdArgs.selectedTagLoc.toLabelOpt
          } yield {
            stripEditR.PropsVal(
              strip         = selJdt,
              edS           = stripEd,
              colorsState   = mroot.doc.colorsState
            )
          }
        }( OptFastEq.Wrapped ),

        picPropsOptC = p.connect { mroot =>
          for {
            selJdt   <- mroot.doc.jdArgs.selectedTagLoc.toLabelOpt
            if selJdt.name.isBgImgAllowed
          } yield {
            val bgEdgeOpt = selJdt.props1
              .bgImg
              .flatMap { ei =>
                mroot.doc.jdArgs.renderArgs.edges
                  .get(ei.imgEdge.edgeUid)
              }
            pictureR.PropsVal(
              imgSrcOpt = bgEdgeOpt
                .flatMap(_.imgSrcOpt),
              uploadState = bgEdgeOpt
                .flatMap(_.fileJs)
                .flatMap(_.upload),
              histogram = bgEdgeOpt
                .flatMap(_.jdEdge.fileSrv)
                .flatMap { fileSrv =>
                  mroot.doc.colorsState.histograms.get(fileSrv.nodeId)
                }
            )
          }
        }( OptFastEq.Wrapped ),

        qdEditOptC = p.connect { mroot =>
          for {
            qdEdit <- mroot.doc.qdEdit
            selJd  <- mroot.doc.jdArgs.selectedTagLoc.toLabelOpt
          } yield {
            qdEditR.PropsVal(
              qdEdit      = qdEdit,
              bgColor     = selJd.props1.bgColor,
              colorsState = mroot.doc.colorsState
            )
          }
        }( OptFastEq.Wrapped ),

        scalePropsOptC = {
          val variants = MSzMults.forAdEditor
          p.connect { mroot =>
            val propsVal = scaleR.PropsVal(
              current  = mroot.doc.jdArgs.conf.szMult,
              variants = variants
            )
            Some(propsVal): Option[scaleR.PropsVal]
          }( OptFastEq.Wrapped )
        },

        rightYOptC = p.connect { mroot =>
          mroot.layout.rightPanelY
        }( OptFastEq.PlainVal )

      )
    }
    .renderBackend[Backend]
    .build

  def apply(rootProxy: Props) = component(rootProxy)

}
