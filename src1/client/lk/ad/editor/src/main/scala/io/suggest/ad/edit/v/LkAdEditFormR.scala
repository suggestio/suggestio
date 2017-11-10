package io.suggest.ad.edit.v

import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.ad.blk.{BlockHeights, BlockMeta, BlockWidths}
import io.suggest.ad.edit.m.edit.color.MColorPickerS
import io.suggest.ad.edit.m.edit.strip.MStripEdS
import io.suggest.ad.edit.m.{DocBodyClick, MAeRoot, SlideBlockKeys}
import io.suggest.ad.edit.v.edit.strip.{DeleteStripBtnR, PlusMinusControlsR, ShowWideR}
import io.suggest.ad.edit.v.edit._
import io.suggest.ad.edit.v.edit.color.{ColorCheckboxR, ColorPickerR}
import io.suggest.scalaz.ZTreeUtil._
import io.suggest.css.Css
import io.suggest.css.ScalaCssDefaults._
import io.suggest.dev.MSzMults
import io.suggest.jd.render.m.MJdArgs
import io.suggest.jd.render.v.{JdCss, JdCssR, JdR}
import io.suggest.quill.v.QuillCss
import io.suggest.common.html.HtmlConstants.{COMMA, `(`, `)`}
import io.suggest.i18n.MsgCodes
import io.suggest.jd.tags.MJdTagNames
import io.suggest.lk.r.SlideBlockR
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.sjs.common.i18n.Messages
import io.suggest.spa.OptFastEq
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, ScalaComponent}
import japgolly.univeq._

import scalacss.ScalaCssReact._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.08.17 21:56
  * Description: React-компонент всей формы react-редактора карточек.
  */
class LkAdEditFormR(
                     jdCssR                     : JdCssR,
                     jdR                        : JdR,
                     addR                       : AddR,
                     lkAdEditCss                : LkAdEditCss,
                     quillCssFactory            : => QuillCss,
                     val qdEditR                : QdEditR,
                     val scaleR                 : ScaleR,
                     val pictureR               : PictureR,
                     val saveR                  : SaveR,
                     val useAsMainR             : UseAsMainR,
                     val deleteBtnR             : DeleteBtnR,
                     val plusMinusControlsR     : PlusMinusControlsR,
                     deleteStripBtnR            : DeleteStripBtnR,
                     val showWideR              : ShowWideR,
                     val colorCheckboxR         : ColorCheckboxR,
                     val slideBlockR            : SlideBlockR,
                     val colorPickerR           : ColorPickerR
                   ) {

  import MJdArgs.MJdWithArgsFastEq
  import qdEditR.QdEditRPropsValFastEq
  import scaleR.ScaleRPropsValFastEq
  import pictureR.PictureRPropsValFastEq
  import saveR.SaveRPropsValFastEq
  import useAsMainR.UseAdMainPropsValFastEq
  import deleteBtnR.DeleteBtnRPropsValFastEq
  import plusMinusControlsR.PlusMinusControlsPropsValFastEq
  import MStripEdS.MStripEdSFastEq
  import showWideR.ShowWideRPropsValFastEq
  import colorCheckboxR.ColorCheckboxPropsValFastEq
  import slideBlockR.SlideBlockPropsValFastEq

  type Props = ModelProxy[MAeRoot]

  /** Состояние компонента содержит model-коннекшены для подчинённых компонентов. */
  protected case class State(
                              jdPreviewArgsC                  : ReactConnectProxy[MJdArgs],
                              jdCssArgsC                      : ReactConnectProxy[JdCss],
                              picPropsOptC                    : ReactConnectProxy[Option[pictureR.PropsVal]],
                              qdEditOptC                      : ReactConnectProxy[Option[qdEditR.PropsVal]],
                              scalePropsOptC                  : ReactConnectProxy[Option[scaleR.PropsVal]],
                              rightYOptC                      : ReactConnectProxy[Option[Int]],
                              savePropsC                      : ReactConnectProxy[saveR.PropsVal],
                              useAsMainStripPropsOptC         : ReactConnectProxy[Option[useAsMainR.PropsVal]],
                              deletePropsOptC                 : ReactConnectProxy[Option[deleteBtnR.PropsVal]],
                              heightPropsOptC                 : ReactConnectProxy[Option[plusMinusControlsR.PropsVal]],
                              widthPropsOptC                  : ReactConnectProxy[Option[plusMinusControlsR.PropsVal]],
                              stripEdSOptC                    : ReactConnectProxy[Option[MStripEdS]],
                              showWidePropsOptC               : ReactConnectProxy[Option[showWideR.PropsVal]],
                              stripBgColorCheckBoxPropsOptC   : ReactConnectProxy[Option[colorCheckboxR.PropsVal]],
                              slideBlocks                     : SlideBlocksState,
                              colors                          : ColorsState
                            )

  case class SlideBlocksState(
                               block    : ReactConnectProxy[Option[slideBlockR.PropsVal]],
                               blockBg  : ReactConnectProxy[Option[slideBlockR.PropsVal]],
                               content  : ReactConnectProxy[Option[slideBlockR.PropsVal]],
                               create   : ReactConnectProxy[Option[slideBlockR.PropsVal]]
                             )

  case class ColorsState(
                          picker        : ReactConnectProxy[Option[colorPickerR.PropsVal]]
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

            // Селектор масштаба карточки.
            s.scalePropsOptC { scaleR.apply },

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

              // Редактор блока.
              s.slideBlocks.block { propsOpt =>
                slideBlockR(propsOpt)(
                  // Редактор strip'а
                  s.stripEdSOptC { _ =>
                    <.div(

                      // Кнопки управление шириной и высотой блока.
                      <.div(
                        lkAdEditCss.WhControls.outer,
                        s.heightPropsOptC { plusMinusControlsR.apply },
                        s.widthPropsOptC { plusMinusControlsR.apply }
                      ),

                      // Управление main-блоками.
                      s.useAsMainStripPropsOptC { useAsMainR.apply },

                      // Кнопка удаления текущего блока.
                      s.stripEdSOptC { deleteStripBtnR.apply },

                    )
                  }
                )
              },

              // Настройка фона блока.
              s.slideBlocks.blockBg { propsOpt =>
                slideBlockR(propsOpt)(
                  <.div(
                    // Выбор цвета фона блока.
                    s.stripBgColorCheckBoxPropsOptC { colorOptProxy =>
                      colorCheckboxR(colorOptProxy)(
                        // TODO Opt Рендер необязателен из-за Option, но список children не ленив. Можно ли это исправить, кроме как передавая суть children внутри props?
                        Messages( MsgCodes.`Bg.color` )
                      )
                    },

                    // Управление картинкой, если доступно:
                    s.picPropsOptC { pictureR.apply },

                    // Галочка широкого рендера фона.
                    s.showWidePropsOptC { showWideR.apply },
                  )
                )
              },

              // Редактор контента (текста)
              s.slideBlocks.content { propsOpt =>
                slideBlockR(propsOpt)(
                  s.qdEditOptC { qdEditR.apply }
                )
              },

              // Форма создания новых объектов.
              s.slideBlocks.create { propsOpt =>
                slideBlockR(propsOpt)(
                  addR(p)
                )
              },

              // Кнопка сохранения карточки.
              s.savePropsC { saveR.apply },

            )
          },

          // ПослеБаяние: впихнуть сюда абсолютно-плавающие color-picker'ы.
          s.colors.picker { colorPickerR.apply }

        ),

        <.br,

        <.div(
          ^.`class` := Css.flat( Css.Lk.Submit.SUBMIT_W, Css.Size.M ),

          // Кнопка удаления карточки.
          s.deletePropsOptC { deleteBtnR.apply }

        )
      )
    }

  }


  val component = ScalaComponent.builder[Props]("AdEd")
    .initialStateFromProps { p =>

      // Фунция с дедублицированным кодом сборки коннекшена до пропертисов plus-minus control'ов (для стрипов).
      def __mkStripBmC[T: FastEq](f: BlockMeta => T) = {
        p.connect { mroot =>
          for {
            selJdtTree <- mroot.doc.jdArgs.selectedTag
            selJdt = selJdtTree.rootLabel
            if selJdt.name ==* MJdTagNames.STRIP
            bm     <- selJdt.props1.bm
          } yield {
            f(bm)
          }
        }( OptFastEq.Wrapped )
      }

      // Функция сборки
      def __colorPickerState(f: MAeRoot => Option[MColorPickerS]) = {

      }

      State(
        jdPreviewArgsC = p.connect { mroot =>
          mroot.doc.jdArgs
        },

        jdCssArgsC = p.connect { mroot =>
          mroot.doc.jdArgs.jdCss
        },

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
              bgColor     = selJd.props1.bgColor
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
        }( OptFastEq.PlainVal ),

        savePropsC = p.connect { mroot =>
          saveR.PropsVal(
            currentReq = mroot.save.saveReq
          )
        },

        useAsMainStripPropsOptC = p.connect { mroot =>
          for {
            // Доступно только при редактировании стрипа.
            _       <- mroot.doc.stripEd
            selJdt  <- mroot.doc.jdArgs.selectedTagLoc.toLabelOpt
          } yield {
            import io.suggest.common.empty.OptionUtil.BoolOptOps
            useAsMainR.PropsVal(
              checked = selJdt.props1.isMain.getOrElseFalse,
              mainCount = mroot.doc.jdArgs.template
                .subForest
                .count( _.rootLabel.props1.isMain.getOrElseFalse )
            )
          }
        }( OptFastEq.Wrapped ),

        deletePropsOptC = p.connect { mroot =>
          for {
            _ <- mroot.conf.adId
          } yield {
            deleteBtnR.PropsVal(
              deleteConfirm = mroot.popups.deleteConfirm
            )
          }
        }( OptFastEq.Wrapped ),

        heightPropsOptC = __mkStripBmC { bm =>
          plusMinusControlsR.PropsVal(
            labelMsgCode  = MsgCodes.`Height`,
            contCss       = lkAdEditCss.WhControls.contHeight,
            model         = BlockHeights,
            current       = bm.h
          )
        },
        widthPropsOptC = __mkStripBmC { bm =>
          plusMinusControlsR.PropsVal(
            labelMsgCode  = MsgCodes.`Width`,
            contCss       = lkAdEditCss.WhControls.contWidth,
            model         = BlockWidths,
            current       = bm.w
          )
        },

        stripEdSOptC = p.connect { mroot =>
          mroot.doc.stripEd
        }( OptFastEq.Wrapped ),

        showWidePropsOptC = __mkStripBmC { bm =>
          showWideR.PropsVal(
            checked = bm.wide
          )
        },

        stripBgColorCheckBoxPropsOptC = p.connect { mroot =>
          for {
            _ <- mroot.doc.stripEd
            selJdtTree <- mroot.doc.jdArgs.selectedTag
            selJdt = selJdtTree.rootLabel
            if selJdt.name ==* MJdTagNames.STRIP
          } yield {
            colorCheckboxR.PropsVal(
              color         = selJdt.props1.bgColor
            )
          }
        }( OptFastEq.Wrapped ),

        slideBlocks = SlideBlocksState(
          block = {
            val title = Messages( MsgCodes.`Block` )
            p.connect { mroot =>
              for {
                _ <- mroot.doc.stripEd
              } yield {
                val k = SlideBlockKeys.BLOCK
                slideBlockR.PropsVal(
                  title = title,
                  expanded = mroot.doc.slideBlocks.expanded.contains(k),
                  key = Some(k)
                )
              }
            }( OptFastEq.Wrapped )
          },
          blockBg = {
            val title = Messages( MsgCodes.`Background` )
            p.connect { mroot =>
              for {
                _ <- mroot.doc.stripEd
              } yield {
                val k = SlideBlockKeys.BLOCK_BG
                slideBlockR.PropsVal(
                  title     = title,
                  expanded  = mroot.doc.slideBlocks.expanded.contains(k),
                  key       = Some(k)
                )
              }
            }( OptFastEq.Wrapped )
          },
          content = {
            val title = Messages( MsgCodes.`Content` )
            p.connect { mroot =>
              for {
                _ <- mroot.doc.qdEdit
              } yield {
                val k = SlideBlockKeys.CONTENT
                slideBlockR.PropsVal(
                  title = title,
                  expanded = mroot.doc.slideBlocks.expanded.contains(k),
                  key      = Some(k)
                )
              }
            }( OptFastEq.Wrapped )
          },
          create = {
            val k = SlideBlockKeys.ADD
            val title = Messages( MsgCodes.`Create` )
            p.connect { mroot =>
              Some(
                slideBlockR.PropsVal(
                  title     = title,
                  expanded  = mroot.doc.slideBlocks.expanded.contains(k),
                  key       = Some(k)
                )
              )
            }
          }
        ),

        colors = ColorsState(
          picker = p.connect { mroot =>
            for {
              pickerS <- {
                mroot.doc.stripEd
                  .orElse {
                    mroot.doc.qdEdit
                  }
                  .map(_.bgColorPick)
              }
              shownAt     <- pickerS.shownAt
              selJdtTree  <- mroot.doc.jdArgs.selectedTag
              bgColor     <- selJdtTree.rootLabel.props1.bgColor
            } yield {
              colorPickerR.PropsVal(
                color       = bgColor,
                colorsState = mroot.doc.colorsState,
                fixedXy     = shownAt
              )
            }
          }
        )

      )
    }
    .renderBackend[Backend]
    .build

  def apply(rootProxy: Props) = component(rootProxy)

}
