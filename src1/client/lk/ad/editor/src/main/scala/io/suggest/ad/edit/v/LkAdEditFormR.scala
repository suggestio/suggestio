package io.suggest.ad.edit.v

import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.ad.blk.{BlockHeights, BlockMeta, BlockWidths}
import io.suggest.ad.edit.m.edit.strip.MStripEdS
import io.suggest.ad.edit.m.{MAeRoot, SlideBlockKeys}
import io.suggest.ad.edit.v.edit.strip.{DeleteStripBtnR, PlusMinusControlsR, ShowWideR}
import io.suggest.ad.edit.v.edit._
import io.suggest.ad.edit.v.edit.color.ColorCheckboxR
import io.suggest.scalaz.ZTreeUtil._
import io.suggest.css.Css
import io.suggest.css.ScalaCssDefaults._
import io.suggest.dev.MSzMults
import io.suggest.jd.render.m.MJdArgs
import io.suggest.jd.render.v.{JdCss, JdCssR, JdR}
import io.suggest.quill.v.{QuillCss, QuillEditorR}
import io.suggest.common.html.HtmlConstants.{COMMA, `(`, `)`}
import io.suggest.file.up.MFileUploadS
import io.suggest.i18n.MsgCodes
import io.suggest.jd.tags.{MJdTagName, MJdTagNames}
import io.suggest.lk.m.{DocBodyClick, MFormResourceKey}
import io.suggest.lk.r.{SlideBlockR, UploadStatusR}
import io.suggest.lk.r.color.{ColorPickerR, ColorsSuggestR}
import io.suggest.lk.r.crop.CropBtnR
import io.suggest.lk.r.img.ImgEditBtnR
import io.suggest.msg.Messages
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
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
                     val scaleR                 : ScaleR,
                     uploadStatusR              : UploadStatusR,
                     val imgEditBtnR            : ImgEditBtnR,
                     val colorsSuggestR         : ColorsSuggestR,
                     cropBtnR                   : CropBtnR,
                     val saveR                  : SaveR,
                     val useAsMainR             : UseAsMainR,
                     val deleteBtnR             : DeleteBtnR,
                     val plusMinusControlsR     : PlusMinusControlsR,
                     deleteStripBtnR            : DeleteStripBtnR,
                     val showWideR              : ShowWideR,
                     val colorCheckboxR         : ColorCheckboxR,
                     val rotateR                : RotateR,
                     val slideBlockR            : SlideBlockR,
                     val colorPickerR           : ColorPickerR,
                     val quillEditorR           : QuillEditorR
                   ) {

  import MJdArgs.MJdArgsFastEq
  import scaleR.ScaleRPropsValFastEq
  import MFileUploadS.MFileUploadSFastEq
  import colorsSuggestR.ColorsSuggestPropsValFastEq
  import imgEditBtnR.ImgEditBtnRPropsValFastEq
  import MFormResourceKey.MFormImgKeyFastEq
  import saveR.SaveRPropsValFastEq
  import useAsMainR.UseAdMainPropsValFastEq
  import deleteBtnR.DeleteBtnRPropsValFastEq
  import plusMinusControlsR.PlusMinusControlsPropsValFastEq
  import MStripEdS.MStripEdSFastEq
  import showWideR.ShowWideRPropsValFastEq
  import colorCheckboxR.ColorCheckboxPropsValFastEq
  import rotateR.RotateRPropsValFastEq
  import slideBlockR.SlideBlockPropsValFastEq
  import quillEditorR.QuillEditorPropsValFastEq

  type Props = ModelProxy[MAeRoot]

  /** Состояние компонента содержит model-коннекшены для подчинённых компонентов. */
  protected case class State(
                              jdPreviewArgsC                  : ReactConnectProxy[MJdArgs],
                              jdCssArgsC                      : ReactConnectProxy[JdCss],
                              scalePropsOptC                  : ReactConnectProxy[Option[scaleR.PropsVal]],
                              imgEditBtnPropsC                : ReactConnectProxy[imgEditBtnR.PropsVal],
                              upStateOptC                     : ReactConnectProxy[Option[MFileUploadS]],
                              colSuggPropsOptC                : ReactConnectProxy[Option[colorsSuggestR.PropsVal]],
                              cropBtnPropsOptC                : ReactConnectProxy[Option[MFormResourceKey]],
                              rightYOptC                      : ReactConnectProxy[Option[Int]],
                              savePropsC                      : ReactConnectProxy[saveR.PropsVal],
                              useAsMainStripPropsOptC         : ReactConnectProxy[Option[useAsMainR.PropsVal]],
                              deletePropsOptC                 : ReactConnectProxy[Option[deleteBtnR.PropsVal]],
                              heightPropsOptC                 : ReactConnectProxy[Option[plusMinusControlsR.PropsVal]],
                              widthPropsOptC                  : ReactConnectProxy[Option[plusMinusControlsR.PropsVal]],
                              stripEdSOptC                    : ReactConnectProxy[Option[MStripEdS]],
                              showWidePropsOptC               : ReactConnectProxy[Option[showWideR.PropsVal]],
                              slideBlocks                     : SlideBlocksState,
                              colors                          : ColorsState,
                              quillEdOptC                     : ReactConnectProxy[Option[quillEditorR.PropsVal]],
                              rotateOptC                      : ReactConnectProxy[Option[rotateR.PropsVal]]
                            )

  case class SlideBlocksState(
                               block    : ReactConnectProxy[Option[slideBlockR.PropsVal]],
                               blockBg  : ReactConnectProxy[Option[slideBlockR.PropsVal]],
                               content  : ReactConnectProxy[Option[slideBlockR.PropsVal]],
                               create   : ReactConnectProxy[Option[slideBlockR.PropsVal]]
                             )

  case class ColorsState(
                          picker            : ReactConnectProxy[Option[colorPickerR.PropsVal]],
                          stripBgCbOptC     : ReactConnectProxy[Option[colorCheckboxR.PropsVal]],
                          contentBgCbOptC   : ReactConnectProxy[Option[colorCheckboxR.PropsVal]]
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

              // Тело превьюшки в виде плитки.
              s.jdPreviewArgsC { jdR.apply },

              <.div(
                ^.`class` := Css.CLEAR
              )
            )
          ),

          // Рендер редакторов: собираем все редакторы вне контекста функции общего div'а.
          {
            // Редактор strip'а
            val slideBlockBodyDiv = <.div(
              // Кнопки управление шириной и высотой блока.
              <.div(
                lkAdEditCss.WhControls.outer,
                s.heightPropsOptC { plusMinusControlsR.apply },
                s.widthPropsOptC { plusMinusControlsR.apply }
              ),

              // Управление main-блоками.
              s.useAsMainStripPropsOptC { useAsMainR.apply },

              // Кнопка удаления текущего блока.
              s.stripEdSOptC { deleteStripBtnR.apply }

            )
            // Слайд редактор блока.
            val slideBlockVdom = s.slideBlocks.block { propsOpt =>
              slideBlockR(propsOpt)( slideBlockBodyDiv )
            }

            // Настройка фона блока.
            val blockBgBodyDiv = <.div(
              ^.`class` := Css.Overflow.HIDDEN,

              // Выбор цвета фона блока.
              s.colors.stripBgCbOptC { colorCheckboxR.apply },

              // Рендер цветов текущей картинки
              s.colSuggPropsOptC { colorsSuggestR.apply },

              // Рендерить картинку и управление ей:
              s.imgEditBtnPropsC { imgEditBtnR.apply },

              // Кнопка запуска кропа для картинки:
              s.cropBtnPropsOptC { cropBtnR.apply },

              // Отрендерить данные процесса загрузки:
              s.upStateOptC { uploadStatusR.apply },

              <.br,

              // Галочка широкого рендера фона.
              s.showWidePropsOptC { showWideR.apply }
            )
            val blockBgSlide = s.slideBlocks.blockBg { propsOpt =>
              slideBlockR(propsOpt)( blockBgBodyDiv )
            }

            val contentBodyDiv = <.div(
              // Редактор текста
              s.quillEdOptC { quillEditorR.apply },
              <.br,

              // Цвет фона контента.
              s.colors.contentBgCbOptC { colorCheckboxR.apply },

              // Вращение: галочка + опциональный слайдер.
              s.rotateOptC { rotateR.apply }
            )
            // Редактор контента (текста)
            val contentSlideBlock = s.slideBlocks.content { propsOpt =>
              slideBlockR(propsOpt)( contentBodyDiv )
            }

            // Форма создания новых объектов.
            val createSlideBody = addR(p)
            val createSlideBlock = s.slideBlocks.create { propsOpt =>
              slideBlockR(propsOpt)( createSlideBody )
            }

            // Кнопка сохранения карточки.
            val saveBtn = s.savePropsC { saveR.apply }

            // Редакторы собраны снаружи от этого коннекшена, от которого пока не понятно, как избавляться.
            s.rightYOptC { rightYOptProxy =>
              <.div(
                LCSS.editorsCont,
                rightYOptProxy.value.whenDefined { rightY =>
                  ^.transform := (Css.Anim.Transform.TRANSLATE + `(` + 0.px + COMMA + rightY.px + `)`)
                },
                slideBlockVdom,
                blockBgSlide,
                contentSlideBlock,
                createSlideBlock,
                saveBtn
              )
            }
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
            selJdtTreeLoc <- mroot.doc.jdArgs.selJdt.treeLocOpt
            selJdt = selJdtTreeLoc.getLabel
            if selJdt.name ==* MJdTagNames.STRIP
            bm     <- selJdt.props1.bm
          } yield {
            f(bm)
          }
        }( OptFastEq.Wrapped )
      }

      val MSG_BG_COLOR = Messages( MsgCodes.`Bg.color` )
      // Фунция сборки коннекшена до состояния чекбокса выбора цвета.
      def __mkBgColorCbC(jdtName: MJdTagName) = {
        p.connect { mroot =>
          for {
            selJdtTreeLoc <- mroot.doc.jdArgs.selJdt.treeLocOpt
            selJdt = selJdtTreeLoc.getLabel
            if selJdt.name ==* jdtName
          } yield {
            colorCheckboxR.PropsVal(
              color         = selJdt.props1.bgColor,
              label         = MSG_BG_COLOR
            )
          }
        }( OptFastEq.Wrapped )
      }

      // Один общий ключ на все отображаемые ресурсы, которые не требуют уникального ключа.
      val formResKey = MFormResourceKey.empty

      State(
        jdPreviewArgsC = p.connect { mroot =>
          mroot.doc.jdArgs
        },

        jdCssArgsC = p.connect { mroot =>
          mroot.doc.jdArgs.jdCss
        },

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
        }( OptFastEq.OptValueEq ),

        savePropsC = p.connect { mroot =>
          saveR.PropsVal(
            currentReq = mroot.save.saveReq
          )
        },

        useAsMainStripPropsOptC = p.connect { mroot =>
          for {
            // Доступно только при редактировании стрипа.
            _       <- mroot.doc.stripEd
            selJdt  <- mroot.doc.jdArgs.selJdt.treeLocOpt.toLabelOpt
          } yield {
            import io.suggest.common.empty.OptionUtil.BoolOptOps
            useAsMainR.PropsVal(
              checked = selJdt.props1.isMain.getOrElseFalse,
              mainDefined = mroot.doc.jdArgs.template
                .subForest
                .exists( _.rootLabel.props1.isMain.getOrElseFalse )
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
          picker = {
            // Класс элемента color-picker'а. По идее, неизменный, поэтому живёт снаружи.
            val cssClassOpt = Some( lkAdEditCss.BgColorOptPicker.pickerCont.htmlClass )
            p.connect { mroot =>
              for {
                pickerS <- {
                  mroot.doc.stripEd
                    .orElse {
                      mroot.doc.qdEdit
                    }
                    .map(_.bgColorPick)
                }
                if pickerS.shownAt.isDefined
                selJdtTreeLoc   <- mroot.doc.jdArgs.selJdt.treeLocOpt
                bgColor         <- selJdtTreeLoc.getLabel.props1.bgColor
              } yield {
                colorPickerR.PropsVal(
                  color         = bgColor,
                  colorPresets  = mroot.doc.colorsState.colorPresets,
                  cssClass      = cssClassOpt,
                  topLeftPx     = pickerS.shownAt
                )
              }
            }
          },

          stripBgCbOptC   = __mkBgColorCbC( MJdTagNames.STRIP ),
          contentBgCbOptC = __mkBgColorCbC( MJdTagNames.QD_CONTENT )
        ),

        // Коннекшен до пропертисов для QuillEditor-компонента.
        quillEdOptC = p.connect { mroot =>
          for {
            qdS   <- mroot.doc.qdEdit
          } yield {
            quillEditorR.PropsVal(
              initDelta = qdS.initDelta,
              realDelta = qdS.realDelta
            )
          }
        }( OptFastEq.Wrapped ),

        rotateOptC = p.connect { mroot =>
          for {
            selJdtLoc <- mroot.doc.jdArgs.selJdt.treeLocOpt
            selJdt = selJdtLoc.getLabel
            if selJdt.name ==* MJdTagNames.QD_CONTENT
          } yield {
            rotateR.PropsVal(
              value = selJdt.props1.rotateDeg
            )
          }
        }( OptFastEq.Wrapped ),

        imgEditBtnPropsC = p.connect { mroot =>
          imgEditBtnR.PropsVal(
            src = mroot.doc.jdArgs.selJdt.bgEdgeDataOpt
              .flatMap(_.imgSrcOpt)
          )
        },

        upStateOptC = p.connect { mroot =>
          mroot.doc.jdArgs.selJdt.bgEdgeDataOpt
            .flatMap(_.fileJs)
            .flatMap(_.upload)
        }(OptFastEq.Wrapped),

        colSuggPropsOptC = p.connect { mroot =>
          for {
            bgEdge  <- mroot.doc.jdArgs.selJdt.bgEdgeDataOpt
            fileSrv <- bgEdge.jdEdge.fileSrv
            hist    <- mroot.doc.colorsState.histograms.get( fileSrv.nodeId )
          } yield {
            colorsSuggestR.PropsVal(
              titleMsgCode = MsgCodes.`Suggested.bg.colors`,
              colors       = hist.sorted
            )
          }
        }( OptFastEq.Wrapped ),

        cropBtnPropsOptC = p.connect { mroot =>
          for {
            bgEdge <- mroot.doc.jdArgs.selJdt.bgEdgeDataOpt
            if bgEdge.imgSrcOpt.nonEmpty
          } yield {
            formResKey
          }
        }( OptFastEq.Wrapped )

      )
    }
    .renderBackend[Backend]
    .build

  def apply(rootProxy: Props) = component(rootProxy)

}
