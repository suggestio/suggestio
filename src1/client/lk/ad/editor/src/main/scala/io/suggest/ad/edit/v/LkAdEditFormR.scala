package io.suggest.ad.edit.v

import com.github.react.dnd.backend.html5.Html5Backend
import com.github.react.dnd.backend.touch.TouchBackend
import com.github.react.dnd.{DndProvider, DndProviderProps}
import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.ad.blk.{BlockHeights, BlockMeta, BlockWidths, MBlockExpandMode}
import io.suggest.ad.edit.c.DocEditAh
import io.suggest.ad.edit.m.MAeRoot
import io.suggest.ad.edit.m.edit.{MStripEdS, SlideBlockKeys}
import io.suggest.ad.edit.v.edit.strip.{DeleteStripBtnR, PlusMinusControlsR, ShowWideR}
import io.suggest.ad.edit.v.edit._
import io.suggest.ad.edit.v.edit.content.{ContentEditCssR, ContentLayersR}
import io.suggest.color.IColorPickerMarker
import io.suggest.common.empty.OptionUtil
import io.suggest.common.geom.coord.MCoords2di
import io.suggest.scalaz.ZTreeUtil._
import io.suggest.css.{Css, CssR}
import io.suggest.css.ScalaCssDefaults._
import io.suggest.dev.MSzMults
import io.suggest.jd.render.m.MJdRrrProps
import io.suggest.jd.render.v.{JdCss, JdCssStatic}
import io.suggest.quill.v.{QuillCss, QuillEditorR}
import io.suggest.common.html.HtmlConstants.{COMMA, `(`, `)`}
import io.suggest.file.up.MFileUploadS
import io.suggest.i18n.MsgCodes
import io.suggest.jd.edit.v.JdEditR
import io.suggest.jd.tags.{MJdShadow, MJdTagName, MJdTagNames}
import io.suggest.lk.m.{CropOpen, DocBodyClick}
import io.suggest.lk.r.{LkCss, SaveR, SlideBlockR, TouchSwitchR, UploadStatusR}
import io.suggest.lk.r.color.{ColorCheckBoxR, ColorPickerR, ColorsSuggestR}
import io.suggest.lk.r.img.{CropBtnR, ImgEditBtnPropsVal, ImgEditBtnR}
import io.suggest.msg.Messages
import io.suggest.react.ReactDiodeUtil
import io.suggest.spa.{FastEqUtil, OptFastEq}
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import japgolly.univeq._
import scalacss.ScalaCssReact._


/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.08.17 21:56
  * Description: React-компонент всей формы react-редактора карточек.
  */
class LkAdEditFormR(
                     jdCssStatic                : JdCssStatic,
                     jdEditR                    : JdEditR,
                     addR                       : AddR,
                     lkAdEditCss                : LkAdEditCss,
                     lkCss                      : LkCss,
                     quillCssFactory            : => QuillCss,
                     val scaleR                 : ScaleR,
                     uploadStatusR              : UploadStatusR,
                     imgEditBtnR                : ImgEditBtnR,
                     val colorsSuggestR         : ColorsSuggestR,
                     val cropBtnR               : CropBtnR,
                     val saveR                  : SaveR,
                     val useAsMainR             : UseAsMainR,
                     val deleteBtnR             : DeleteBtnR,
                     val plusMinusControlsR     : PlusMinusControlsR,
                     deleteStripBtnR            : DeleteStripBtnR,
                     val showWideR              : ShowWideR,
                     val colorCheckBoxR         : ColorCheckBoxR,
                     val rotateR                : RotateR,
                     val slideBlockR            : SlideBlockR,
                     val colorPickerR           : ColorPickerR,
                     val quillEditorR           : QuillEditorR,
                     val contentEditCssR        : ContentEditCssR,
                     val contentLayersR         : ContentLayersR,
                     val textShadowR            : TextShadowR,
                     val touchSwitchR           : TouchSwitchR,
                   ) {

  type Props = ModelProxy[MAeRoot]

  /** Состояние компонента содержит model-коннекшены для подчинённых компонентов. */
  protected case class State(
                              jdPreviewArgsC                  : ReactConnectProxy[MJdRrrProps],
                              jdCssArgsC                      : ReactConnectProxy[JdCss],
                              scalePropsOptC                  : ReactConnectProxy[Option[scaleR.PropsVal]],
                              upStateOptC                     : ReactConnectProxy[Option[MFileUploadS]],
                              colSuggPropsOptC                : ReactConnectProxy[Option[colorsSuggestR.PropsVal]],
                              cropBtnPropsOptC                : ReactConnectProxy[cropBtnR.Props_t],
                              rightYOptC                      : ReactConnectProxy[Option[Int]],
                              savePropsC                      : ReactConnectProxy[saveR.PropsVal],
                              useAsMainStripPropsOptC         : ReactConnectProxy[Option[useAsMainR.PropsVal]],
                              deletePropsOptC                 : ReactConnectProxy[Option[deleteBtnR.PropsVal]],
                              heightPropsOptC                 : ReactConnectProxy[Option[plusMinusControlsR.PropsVal]],
                              widthPropsOptC                  : ReactConnectProxy[Option[plusMinusControlsR.PropsVal]],
                              stripEdSOptC                    : ReactConnectProxy[Option[MStripEdS]],
                              blockExpandModeOptC             : ReactConnectProxy[Option[MBlockExpandMode]],
                              slideBlocks                     : SlideBlocksState,
                              colors                          : ColorsState,
                              quillEdOptC                     : ReactConnectProxy[Option[quillEditorR.PropsVal]],
                              rotateOptC                      : ReactConnectProxy[Option[rotateR.PropsVal]],
                              contentEditCssC                 : ReactConnectProxy[contentEditCssR.Props_t],
                              contentLayersC                  : ReactConnectProxy[contentLayersR.Props_t],
                              textShadowC                     : ReactConnectProxy[textShadowR.Props_t],
                              isTouchDevSomeC                 : ReactConnectProxy[Some[Boolean]],
                            )

  case class SlideBlocksState(
                               block    : ReactConnectProxy[Option[slideBlockR.PropsVal]],
                               blockBg  : ReactConnectProxy[Option[slideBlockR.PropsVal]],
                               content  : ReactConnectProxy[Option[slideBlockR.PropsVal]],
                               create   : ReactConnectProxy[Option[slideBlockR.PropsVal]]
                             )

  case class ColorsState(
                          picker            : ReactConnectProxy[Option[colorPickerR.PropsVal]],
                          stripBgCbOptC     : ReactConnectProxy[Option[colorCheckBoxR.PropsVal]],
                          contentBgCbOptC   : ReactConnectProxy[Option[colorCheckBoxR.PropsVal]]
                        )


  protected class Backend($: BackendScope[Props, State]) {

    /** Любой клик где-то в форме. Нужно для вычисления кликов за пределами каких-либо элементов. */
    private val _onBodyClick: Callback =
      ReactDiodeUtil.dispatchOnProxyScopeCB($, DocBodyClick)


    def render(p: Props, s: State): VdomElement = {
      val LCSS = lkAdEditCss.Layout

      val contentDiv = <.div(
        ^.`class` := Css.Overflow.HIDDEN,

        // TODO Opt спиливать onClick, когда по состоянию нет ни одного открытого modal'а, например открытого color-picker'а.
        ^.onClick --> _onBodyClick,

        // Отрендерить доп.стили для quill-редактора.
        <.styleTag(
          quillCssFactory.render[String]
        ),

        // Отрендерить стили редактора:
        p.wrap(_ => lkCss)(CssR.apply)(implicitly, FastEq.AnyRefEq),
        p.wrap(_ => lkAdEditCss)(CssR.apply)(implicitly, FastEq.AnyRefEq),

        // Рендер jd-css
        p.wrap(_ => jdCssStatic)(CssR.apply)(implicitly, FastEq.AnyRefEq),
        s.jdCssArgsC { CssR.apply },

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
              s.jdPreviewArgsC { jdEditR.JdRrrEdit.documentDndComp.apply },

              <.div(
                ^.`class` := Css.CLEAR
              ),

              // Кнопка переключения touch-режима.
              s.isTouchDevSomeC { touchSwitchR.apply },
            )
          ),

          // Рендер редакторов: собираем все редакторы вне контекста функции общего div'а.
          {
            // Редактор strip'а
            val slideBlockBodyDiv = <.div(
              // Кнопки управление шириной и высотой блока.
              <.div(
                // Галочка широкого рендера фона.
                lkAdEditCss.WhControls.outer,

                s.heightPropsOptC { plusMinusControlsR.apply },
                s.widthPropsOptC { plusMinusControlsR.apply },
                <.br,
              ),

              <.br,
              s.blockExpandModeOptC { showWideR.apply },
              <.br, <.br, <.br,

              // Управление main-блоками.
              s.useAsMainStripPropsOptC { useAsMainR.apply },
              <.br,

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
              //^.onDrop ==> _onBlockBgDrop,

              // Выбор цвета фона блока.
              s.colors.stripBgCbOptC { colorCheckBoxR.apply },

              // Рендер цветов текущей картинки
              s.colSuggPropsOptC { colorsSuggestR.apply },

              // Рендерить картинку и управление ей, обернув в поддержку таскания файлов:s
              p.wrap { mroot =>
                val bgEdgeOpt = mroot.doc.jdDoc.jdArgs.selJdt.bgEdgeDataOpt
                ImgEditBtnPropsVal(
                  edge   = bgEdgeOpt,
                  resKey = mroot.doc.jdDoc.jdArgs.selJdt.bgEdgeDataFrk,
                )
              }(imgEditBtnR.apply)(implicitly, ImgEditBtnPropsVal.ImgEditBtnRPropsValFastEq ),

              // Кнопка запуска кропа для картинки:
              s.cropBtnPropsOptC { cropBtnR.apply },

              // Отрендерить данные процесса загрузки:
              s.upStateOptC { uploadStatusR.apply },

            )
            val blockBgSlide = s.slideBlocks.blockBg { propsOpt =>
              slideBlockR(propsOpt)( blockBgBodyDiv )
            }

            val contentBodyDiv = <.div(
              // Доп.стили для редактора контента:
              s.contentEditCssC { contentEditCssR.apply },

              // Редактор контента
              s.quillEdOptC { quillEditorR.apply },
              <.br,

              // Цвет фона контента.
              s.colors.contentBgCbOptC { colorCheckBoxR.apply },

              // Вращение: галочка + опциональный слайдер.
              s.rotateOptC { rotateR.apply },

              <.br,
              // Управление тенью текста:
              s.textShadowC { textShadowR.apply },

              // Управление слоями
              s.contentLayersC { contentLayersR.apply },

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
            val saveBtn = <.div(
              ^.`class` := Css.Floatt.RIGHT,
              s.savePropsC { saveR.apply }
            )

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

      // Провайдер поддержки drag-drop:
      s.isTouchDevSomeC { isTouchDevSomeProxy =>
        val isTouchDev = isTouchDevSomeProxy.value.value
        val _backend =
          if (isTouchDev) TouchBackend
          else Html5Backend

        val innerContent = contentDiv(
          touchSwitchR.autoSwitch( isTouchDevSomeProxy )
        )

        DndProvider.component(
          new DndProviderProps {
            override val backend = _backend
          }
        )(
          innerContent
        )
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { p =>

      // Фунция с дедублицированным кодом сборки коннекшена до пропертисов plus-minus control'ов (для стрипов).
      def __mkStripBmC[T](f: BlockMeta => Option[T])(implicit tFeq: FastEq[T]) = {
        p.connect { mroot =>
          for {
            selJdtTreeLoc <- mroot.doc.jdDoc.jdArgs.selJdt.treeLocOpt
            selJdt = selJdtTreeLoc.getLabel
            if selJdt.name ==* MJdTagNames.STRIP
            bm     <- selJdt.props1.bm
            t      <- f(bm)
          } yield {
            t
          }
        }( OptFastEq.Wrapped(tFeq) )
      }

      val MSG_BG_COLOR = Messages( MsgCodes.`Bg.color` )
      // Фунция сборки коннекшена до состояния чекбокса выбора цвета.
      def __mkBgColorCbC(jdtName: MJdTagName with IColorPickerMarker) = {
        val jdtNameSome = Some(jdtName)
        p.connect { mroot =>
          for {
            selJdtTreeLoc <- mroot.doc.jdDoc.jdArgs.selJdt.treeLocOpt
            selJdt = selJdtTreeLoc.getLabel
            if selJdt.name ==* jdtName
          } yield {
            colorCheckBoxR.PropsVal(
              color         = selJdt.props1.bgColor,
              label         = MSG_BG_COLOR,
              marker        = jdtNameSome
            )
          }
        }( OptFastEq.Wrapped(colorCheckBoxR.ColorCheckBoxPropsValFastEq) )
      }

      State(
        jdPreviewArgsC = p.connect(_.doc.jdDoc.toRrrProps)( MJdRrrProps.MJdRrrPropsFastEq ),

        jdCssArgsC = p.connect(_.doc.jdDoc.jdArgs.jdRuntime.jdCss)( JdCss.JdCssFastEq ),

        scalePropsOptC = {
          val variants = MSzMults.forAdEditor
          p.connect { mroot =>
            val propsVal = scaleR.PropsVal(
              current  = mroot.doc.jdDoc.jdArgs.conf.szMult,
              variants = variants
            )
            Some(propsVal): Option[scaleR.PropsVal]
          }( OptFastEq.Wrapped(scaleR.ScaleRPropsValFastEq) )
        },

        rightYOptC = p.connect { mroot =>
          mroot.layout.rightPanelY
        }( OptFastEq.OptValueEq ),

        savePropsC = p.connect { mroot =>
          saveR.PropsVal(
            currentReq = mroot.save.saveReq
          )
        }(saveR.SaveRPropsValFastEq),

        useAsMainStripPropsOptC = p.connect { mroot =>
          for {
            // Доступно только при редактировании стрипа.
            _       <- mroot.doc.editors.stripEd
            selJdt  <- mroot.doc.jdDoc.jdArgs.selJdt.treeLocOpt.toLabelOpt
          } yield {
            import io.suggest.common.empty.OptionUtil.BoolOptOps
            useAsMainR.PropsVal(
              checked = selJdt.props1.isMain.getOrElseFalse,
              mainDefined = mroot.doc.jdDoc.jdArgs.data.doc.template
                .subForest
                .exists( _.rootLabel.props1.isMain.getOrElseFalse )
            )
          }
        }( OptFastEq.Wrapped(useAsMainR.UseAdMainPropsValFastEq) ),

        deletePropsOptC = p.connect { mroot =>
          for {
            _ <- mroot.conf.adId
          } yield {
            deleteBtnR.PropsVal(
              deleteConfirm = mroot.popups.deleteConfirm
            )
          }
        }( OptFastEq.Wrapped(deleteBtnR.DeleteBtnRPropsValFastEq) ),

        heightPropsOptC = __mkStripBmC { bm =>
          val r = plusMinusControlsR.PropsVal(
            labelMsgCode  = MsgCodes.`Height`,
            contCss       = lkAdEditCss.WhControls.contHeight,
            model         = BlockHeights,
            current       = bm.h
          )
          Some(r)
        }( plusMinusControlsR.PlusMinusControlsPropsValFastEq ),

        widthPropsOptC = __mkStripBmC { bm =>
          val r = plusMinusControlsR.PropsVal(
            labelMsgCode  = MsgCodes.`Width`,
            contCss       = lkAdEditCss.WhControls.contWidth,
            model         = BlockWidths,
            current       = bm.w
          )
          Some(r)
        }( plusMinusControlsR.PlusMinusControlsPropsValFastEq ),

        stripEdSOptC = p.connect { mroot =>
          mroot.doc.editors.stripEd
        }( OptFastEq.Wrapped(MStripEdS.MStripEdSFastEq) ),

        blockExpandModeOptC = __mkStripBmC ( _.expandMode )(FastEqUtil.AnyRefFastEq),

        slideBlocks = SlideBlocksState(
          block = {
            val title = Messages( MsgCodes.`Block` )
            p.connect { mroot =>
              for {
                _ <- mroot.doc.editors.stripEd
              } yield {
                val k = SlideBlockKeys.BLOCK
                slideBlockR.PropsVal(
                  title = title,
                  expanded = mroot.doc.editors.slideBlocks.expanded contains k,
                  key = Some(k)
                )
              }
            }( OptFastEq.Wrapped( slideBlockR.SlideBlockPropsValFastEq ) )
          },
          blockBg = {
            val title = Messages( MsgCodes.`Background` )
            p.connect { mroot =>
              for {
                _ <- mroot.doc.editors.stripEd
              } yield {
                val k = SlideBlockKeys.BLOCK_BG
                slideBlockR.PropsVal(
                  title     = title,
                  expanded  = mroot.doc.editors.slideBlocks.expanded contains k,
                  key       = Some(k)
                )
              }
            }( OptFastEq.Wrapped( slideBlockR.SlideBlockPropsValFastEq ) )
          },
          content = {
            val title = Messages( MsgCodes.`Content` )
            p.connect { mroot =>
              for {
                _ <- mroot.doc.editors.qdEdit
              } yield {
                val k = SlideBlockKeys.CONTENT
                slideBlockR.PropsVal(
                  title = title,
                  expanded = mroot.doc.editors.slideBlocks.expanded contains k,
                  key      = Some(k)
                )
              }
            }( OptFastEq.Wrapped( slideBlockR.SlideBlockPropsValFastEq ) )
          },
          create = {
            val k = SlideBlockKeys.ADD
            val title = Messages( MsgCodes.`Create` )
            p.connect { mroot =>
              Some(
                slideBlockR.PropsVal(
                  title     = title,
                  expanded  = mroot.doc.editors.slideBlocks.expanded contains k,
                  key       = Some(k)
                )
              ): Option[slideBlockR.PropsVal]
            }( OptFastEq.Wrapped( slideBlockR.SlideBlockPropsValFastEq ) )
          }
        ),

        colors = ColorsState(
          picker = {
            // Класс элемента color-picker'а. По идее, неизменный, поэтому живёт снаружи.
            val cssClassOpt = Some( lkCss.ColorOptPicker.pickerCont.htmlClass )
            p.connect { mroot =>
              for {
                pickerS         <- mroot.doc.editors.colorsState.picker
                selJdtTreeLoc   <- mroot.doc.jdDoc.jdArgs.selJdt.treeLocOpt
                bgColor         <- {
                  val p1 = selJdtTreeLoc.getLabel.props1
                  pickerS.marker match {
                    case Some(MJdShadow.ColorMarkers.TextShadow) =>
                      p1.textShadow.flatMap(_.color)
                    case _ =>
                      p1.bgColor
                  }
                }
              } yield {
                val topY = pickerS.shownAt.y - 235
                colorPickerR.PropsVal(
                  color         = bgColor,
                  colorPresets  = mroot.doc.editors.colorsState.colorPresets,
                  cssClass      = cssClassOpt,
                  topLeftPx     = Some( MCoords2di.y.set(topY)(pickerS.shownAt) )
                )
              }
            }( OptFastEq.Wrapped(colorPickerR.ColorPickerPropsValFastEq) )
          },

          stripBgCbOptC   = __mkBgColorCbC( MJdTagNames.STRIP ),
          contentBgCbOptC = __mkBgColorCbC( MJdTagNames.QD_CONTENT )
        ),

        // Коннекшен до пропертисов для QuillEditor-компонента.
        quillEdOptC = p.connect { mroot =>
          for {
            qdS   <- mroot.doc.editors.qdEdit
          } yield {
            quillEditorR.PropsVal(
              initDelta = qdS.initDelta,
              realDelta = qdS.realDelta
            )
          }
        }( OptFastEq.Wrapped(quillEditorR.QuillEditorPropsValFastEq) ),

        rotateOptC = p.connect { mroot =>
          for {
            selJdtLoc <- mroot.doc.jdDoc.jdArgs.selJdt.treeLocOpt
            selJdt = selJdtLoc.getLabel
            if selJdt.name ==* MJdTagNames.QD_CONTENT
          } yield {
            rotateR.PropsVal(
              value = selJdt.props1.rotateDeg
            )
          }
        }( OptFastEq.Wrapped(rotateR.RotateRPropsValFastEq) ),

        upStateOptC = p.connect { mroot =>
          mroot.doc.jdDoc.jdArgs.selJdt.bgEdgeDataOpt
            .flatMap(_._2.fileJs)
            .flatMap(_.upload)
        }( OptFastEq.Wrapped( MFileUploadS.MFileUploadSFastEq ) ),

        colSuggPropsOptC = p.connect { mroot =>
          for {
            bgEdge  <- mroot.doc.jdDoc.jdArgs.selJdt.bgEdgeDataOpt
            fileSrv <- bgEdge._2.jdEdge.fileSrv
            hist    <- mroot.doc.editors.colorsState.histograms.get( fileSrv.nodeId )
          } yield {
            colorsSuggestR.PropsVal(
              titleMsgCode = MsgCodes.`Suggested.bg.colors`,
              colors       = hist.sorted
            )
          }
        }( OptFastEq.Wrapped(colorsSuggestR.ColorsSuggestPropsValFastEq) ),

        cropBtnPropsOptC = p.connect { mroot =>
          for {
            bgEdge <- mroot.doc.jdDoc.jdArgs.selJdt.bgEdgeDataOpt
            if bgEdge._2.imgSrcOpt.nonEmpty
            // Вычислить размер контейнера. Это размер блока, для которого выбираем фон:
            cropContSz <- mroot.doc.jdDoc.jdArgs
              .selJdt
              .treeLocOpt
              .flatMap(_.getLabel.props1.bm)
          } yield {
            CropOpen( mroot.doc.jdDoc.jdArgs.selJdt.bgEdgeDataFrk, cropContSz )
          }
        }( OptFastEq.Wrapped(CropOpen.CropOpenFastEq) ),

        contentEditCssC = p.connect { mroot =>
          contentEditCssR.PropsVal(
            bgColor = {
              for {
                selJdtTreeLoc <- mroot.doc.jdDoc.jdArgs.selJdt.treeLocOpt
                selJdt = selJdtTreeLoc.getLabel
                if selJdt.name ==* MJdTagNames.QD_CONTENT
                r <- {
                  // Найти цвет фона в текущем или в родительских тегах.
                  (Iterator.single(selJdt) ++ selJdtTreeLoc.parents.iterator.map(_._2))
                    .flatMap(_.props1.bgColor)
                    .toStream
                    .headOption
                }
              } yield {
                r
              }
            }
          )
        }(contentEditCssR.ContentEditCssRPropsValFastEq),

        contentLayersC = p.connect { mroot =>
          val jdArgs = mroot.doc.jdDoc.jdArgs
          for {
            selJdtTreeLoc <- jdArgs.selJdt.treeLocOpt
            selJdt = selJdtTreeLoc.getLabel
            if selJdt.name ==* MJdTagNames.QD_CONTENT
            // selPath - по идее можно использовать .get, но делаем всё красово:
            selPath       <- jdArgs.renderArgs.selPath
            position      <- selPath.lastOption
          } yield {
            contentLayersR.PropsVal(
              position = position,
              max      = selJdtTreeLoc.lefts.length + selJdtTreeLoc.rights.length,
              isQdBl   = DocEditAh.isQdBlockless( selJdtTreeLoc ),
            )
          }
        }( OptFastEq.OptValueEq ),

        textShadowC = p.connect { mroot =>
          for {
            loc <- mroot.doc.jdDoc.jdArgs.selJdt.treeLocOpt
            textShadow <- loc.getLabel.props1.textShadow
          } yield {
            textShadowR.PropsVal(
              jdShadow = textShadow
            )
          }
        }( OptFastEq.Wrapped(textShadowR.TextShadowRPropsValFastEq) ),

        isTouchDevSomeC = p.connect{ mroot =>
          OptionUtil.SomeBool( mroot.conf.touchDev )
        }( FastEq.AnyRefEq ),

      )
    }
    .renderBackend[Backend]
    .build

  def apply(rootProxy: Props) = component(rootProxy)

}
