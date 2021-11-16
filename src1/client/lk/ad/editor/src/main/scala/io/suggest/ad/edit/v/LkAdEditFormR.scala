package io.suggest.ad.edit.v

import com.github.react.dnd.backend.html5.Html5Backend
import com.github.react.dnd.backend.touch.TouchBackend
import com.github.react.dnd.{DndProvider, DndProviderProps}
import diode.FastEq
import diode.data.Pot
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.ad.blk.{BlockHeights, BlockWidths, MBlockExpandMode}
import io.suggest.ad.edit.c.DocEditAh
import io.suggest.ad.edit.m.{MAdEditFormInit, MAeRoot, Save}
import io.suggest.ad.edit.m.edit.{MStripEdS, SlideBlockKeys}
import io.suggest.ad.edit.v.edit.strip.{DeleteStripBtnR, PlusMinusControlsR, ShowWideR}
import io.suggest.ad.edit.v.edit._
import io.suggest.ad.edit.v.edit.content.{ContentEditCssR, ContentLayersR}
import io.suggest.ad.edit.v.edit.events.EventsR
import io.suggest.color.{IColorPickerMarker, MHistogram}
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
import io.suggest.i18n.MsgCodes
import io.suggest.jd.edit.v.JdEditR
import io.suggest.jd.tags.{JdTag, MJdShadow, MJdTagName, MJdTagNames}
import io.suggest.lk.m.color.MColor2PickerCtx
import io.suggest.lk.m.{CropOpen, DocBodyClick}
import io.suggest.lk.r.{LkCss, SaveR, SlideBlockR, TouchSwitchR, UploadStatusR}
import io.suggest.lk.r.color.{ColorCheckBoxR, ColorPickerR, ColorsSuggestR}
import io.suggest.lk.r.img.{CropBtnR, ImgEditBtnPropsVal, ImgEditBtnR}
import io.suggest.lk.u.MaterialUiUtil
import io.suggest.msg.Messages
import io.suggest.react.ReactDiodeUtil
import io.suggest.react.ReactDiodeUtil.Implicits.ModelProxyExt
import io.suggest.spa.{FastEqUtil, OptFastEq}
import io.suggest.up.MFileUploadS
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import japgolly.univeq._
import org.scalajs.dom
import scalacss.ScalaCssReact._
import scalaz.{EphemeralStream, TreeLoc}


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
                     rotateR                    : RotateR,
                     outLineR                   : OutLineR,
                     widthPxOptR                : WidthPxOptR,
                     lineHeightR                : LineHeightR,
                     val slideBlockR            : SlideBlockR,
                     val colorPickerR           : ColorPickerR,
                     val quillEditorR           : QuillEditorR,
                     val contentEditCssR        : ContentEditCssR,
                     val contentLayersR         : ContentLayersR,
                     eventsR                    : EventsR,
                     titleR                     : TitleR,
                     textShadowR                : TextShadowR,
                     val touchSwitchR           : TouchSwitchR,
                     colorPickerCtxP            : React.Context[MColor2PickerCtx],
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
                              savePropsC                      : ReactConnectProxy[Pot[MAdEditFormInit]],
                              useAsMainStripPropsOptC         : ReactConnectProxy[Option[useAsMainR.PropsVal]],
                              deletePropsOptC                 : ReactConnectProxy[Option[deleteBtnR.PropsVal]],
                              heightPropsOptC                 : ReactConnectProxy[Option[Int]],
                              widthPropsOptC                  : ReactConnectProxy[Option[Int]],
                              stripEdSOptC                    : ReactConnectProxy[Option[MStripEdS]],
                              expandModeOptC                  : ReactConnectProxy[Option[MBlockExpandMode]],
                              slideBlocks                     : SlideBlocksState,
                              colors                          : ColorsState,
                              quillEdOptC                     : ReactConnectProxy[Option[quillEditorR.PropsVal]],
                              contentEditCssC                 : ReactConnectProxy[contentEditCssR.Props_t],
                              contentLayersC                  : ReactConnectProxy[contentLayersR.Props_t],
                              isTouchDevSomeC                 : ReactConnectProxy[Some[Boolean]],
                              colorPaletteC                   : ReactConnectProxy[MHistogram],
                            )

  case class SlideBlocksState(
                               block    : ReactConnectProxy[Option[slideBlockR.PropsVal]],
                               blockBg  : ReactConnectProxy[Option[slideBlockR.PropsVal]],
                               content  : ReactConnectProxy[Option[slideBlockR.PropsVal]],
                               events   : ReactConnectProxy[Option[slideBlockR.PropsVal]],
                               create   : ReactConnectProxy[Option[slideBlockR.PropsVal]],
                             )

  case class ColorsState(
                          picker            : ReactConnectProxy[Option[colorPickerR.PropsVal]],
                          stripBgCbOptC     : ReactConnectProxy[Option[colorCheckBoxR.PropsVal]],
                          contentBgCbOptC   : ReactConnectProxy[Option[colorCheckBoxR.PropsVal]]
                        )


  protected class Backend($: BackendScope[Props, State]) {

    /** Preview container-tag ref. Used to dump rendered HTML-version of ad blocks.
      * In next-gen editor, only HTML-version will be submitted and parsed server-side. */
    private val _previewContainerRef = Ref[dom.html.Div]

    /** Любой клик где-то в форме. Нужно для вычисления кликов за пределами каких-либо элементов. */
    private val _onBodyClick: Callback =
      ReactDiodeUtil.dispatchOnProxyScopeCB($, DocBodyClick)


    def render(p: Props, s: State): VdomElement = {
      val LCSS = lkAdEditCss.Layout
      val br = <.br: VdomNode

      val contentDiv = <.div(
        ^.`class` := Css.Overflow.HIDDEN,

        // TODO Opt спиливать onClick, когда по состоянию нет ни одного открытого modal'а, например открытого color-picker'а.
        ^.onClick --> _onBodyClick,

        // Отрендерить доп.стили для quill-редактора.
        CssR.component( quillCssFactory ),

        // Отрендерить стили редактора:
        CssR.compProxied(
          p.resetZoom( lkCss ),
        ),
        CssR.compProxied(
          p.resetZoom( lkAdEditCss ),
        ),

        // Рендер jd-css
        CssR.compProxied(
          p.resetZoom( jdCssStatic ),
        ),
        s.jdCssArgsC { CssR.compProxied.apply },

        // Редактирование заголовка.
        <.div(
          p.wrap( _.doc.jdDoc.jdArgs.data.title )( titleR.component.apply ),
        ),

        <.div(
          LCSS.outerCont,

          // Рендер preview
          <.div(
            LCSS.previewOuterCont,

            // Селектор масштаба карточки.
            s.scalePropsOptC { scaleR.component.apply },

            <.div(
              LCSS.previewInnerCont,

              // Тело превьюшки в виде плитки.
              <.div.withRef( _previewContainerRef )(
                s.jdPreviewArgsC { jdEditR.JdRrrEdit.DocumentDnd.apply },
              ),

              <.div(
                ^.`class` := Css.CLEAR
              ),

              // Кнопка переключения touch-режима.
              s.isTouchDevSomeC { touchSwitchR.component.apply },
            )
          ),

          // Рендер редакторов: собираем все редакторы вне контекста функции общего div'а.
          {
            val plusMinusOptFeq = OptFastEq.Wrapped(plusMinusControlsR.PlusMinusControlsPropsValFastEq)

            // Редактор strip'а
            val slideBlockBodyDiv = <.div(
              // Кнопки управление шириной и высотой блока.
              <.div(
                // Галочка широкого рендера фона.
                //lkAdEditCss.WhControls.outer,
                ^.`class` := Css.Overflow.HIDDEN,

                s.heightPropsOptC { heightPxOptProxy =>
                  heightPxOptProxy.wrap { heightPxOpt =>
                    for {
                      heightPx  <- heightPxOpt
                      h         <- BlockHeights.withValueOpt(heightPx)
                    } yield {
                      plusMinusControlsR.PropsVal(
                        labelMsgCode  = MsgCodes.`Height`,
                        model         = BlockHeights,
                        current       = h,
                      )
                    }
                  }(plusMinusControlsR.component.apply)(implicitly, plusMinusOptFeq)
                },

                s.widthPropsOptC { widthPxOptProxy =>
                  widthPxOptProxy.wrap { widthPxOpt =>
                    for {
                      widthPx <- widthPxOpt
                      w       <- BlockWidths.withValueOpt(widthPx)
                    } yield {
                      plusMinusControlsR.PropsVal(
                        labelMsgCode  = MsgCodes.`Width`,
                        model         = BlockWidths,
                        current       = w,
                      )
                    }
                  }( plusMinusControlsR.component.apply )( implicitly, plusMinusOptFeq )
                },

              ),

              s.expandModeOptC { showWideR.component.apply },
              br,
              br,

              // Управление обводкой данного блока.
              p.wrap( _blockFiltered(_.getLabel.props1.outline) )( outLineR.component.apply ),
              br, br, br,

              // Управление main-блоками.
              s.useAsMainStripPropsOptC { useAsMainR.component.apply },
              br,

              // Кнопка удаления текущего блока.
              s.stripEdSOptC { deleteStripBtnR.component.apply },
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
              s.colors.stripBgCbOptC { colorCheckBoxR.component.apply },

              // Рендер цветов текущей картинки
              s.colSuggPropsOptC { colorsSuggestR.component.apply },

              // Рендерить картинку и управление ей, обернув в поддержку таскания файлов:s
              p.wrap { mroot =>
                val bgEdgeOpt = mroot.doc.jdDoc.jdArgs.selJdt.bgEdgeDataOpt
                ImgEditBtnPropsVal(
                  edge   = bgEdgeOpt,
                  resKey = mroot.doc.jdDoc.jdArgs.selJdt.bgEdgeDataFrk,
                )
              }(imgEditBtnR.apply)(implicitly, ImgEditBtnPropsVal.ImgEditBtnRPropsValFastEq ),

              // Кнопка запуска кропа для картинки:
              s.cropBtnPropsOptC { cropBtnR.component.apply },

              // Отрендерить данные процесса загрузки:
              s.upStateOptC { uploadStatusR.component.apply },

            )
            val blockBgSlide = s.slideBlocks.blockBg { propsOpt =>
              slideBlockR(propsOpt)( blockBgBodyDiv )
            }

            /** Дедубликация кода доступа к текущему выделенному jd-тегу. */
            def __qdContentSelected[T](mroot: MAeRoot)(f: JdTag => T): Option[T] = {
              for {
                selJdtLoc <- mroot.doc.jdDoc.jdArgs.selJdt.treeLocOpt
                selJdt = selJdtLoc.getLabel
                if selJdt.name ==* MJdTagNames.QD_CONTENT
              } yield f(selJdt)
            }

            val contentBodyDiv = <.div(
              // Доп.стили для редактора контента:
              s.contentEditCssC { contentEditCssR.apply },

              // Редактор контента
              s.quillEdOptC { quillEditorR.apply },
              br,

              // Цвет фона контента.
              s.colors.contentBgCbOptC { colorCheckBoxR.apply },

              // Вращение: галочка + опциональный слайдер.
              p.wrap {
                __qdContentSelected(_)(_.props1.rotateDeg)
              }(rotateR.apply)( implicitly, FastEq.ValueEq ),

              // Галочка управления шириной.
              p.wrap {
                __qdContentSelected(_)(_.props1.widthPx)
              }(widthPxOptR.apply)(implicitly, FastEq.ValueEq),

              p.wrap {
                __qdContentSelected(_)(_.props1.lineHeight)
              }(lineHeightR.apply)(implicitly, FastEq.ValueEq),

              br,
              // Управление тенью текста:
              p.wrap { mroot =>
                mroot.doc.jdDoc.jdArgs.selJdt
                  .treeLocOpt
                  .flatMap { loc =>
                    loc.getLabel.props1.textShadow
                  }
              }( textShadowR.apply )( implicitly, FastEq.AnyRefEq ),

              // Управление слоями
              s.contentLayersC { contentLayersR.apply },
            )
            // Редактор контента (текста)
            val contentSlideBlock = s.slideBlocks.content { propsOpt =>
              slideBlockR(propsOpt)( contentBodyDiv )
            }

            // Управление событиями.
            val eventsBodyDiv = <.div(
              p.wrap(_.doc)( eventsR.component.apply ),
            )
            val eventsSlideBlock = s.slideBlocks.events { propsOpt =>
              slideBlockR(propsOpt)( eventsBodyDiv )
            }

            // Форма создания новых объектов.
            val createSlideBody = addR(p)
            val createSlideBlock = s.slideBlocks.create { propsOpt =>
              slideBlockR(propsOpt)( createSlideBody )
            }

            // Кнопка сохранения карточки.
            val saveBtn = <.div(
              ^.`class` := Css.Floatt.RIGHT,
              s.savePropsC { potProxy =>
                saveR.component(
                  potProxy.zoom { currentReqPot =>
                    saveR.PropsVal(
                      currentReqPot,
                      onClick = CallbackTo.lazily {
                        _previewContainerRef.get.asCallback.map { divOpt =>
                          Save(
                            // Grab html-version of current ad: TODO Grab non-editor version (conf.isEdit = false)?
                            innerHtml = () => {
                              for {
                                div <- divOpt
                                firstChildNode <- Option( div.firstChild )
                                firstChild = firstChildNode.asInstanceOf[dom.html.Element]
                                innerHtml <- Option( firstChild.innerHTML )
                                if innerHtml.nonEmpty
                              } yield {
                                innerHtml
                              }
                            },
                          )
                        }
                      },
                    )
                  }
                )
              }
            )

            val editors = <.div(
              LCSS.editorsCont,
              slideBlockVdom,
              blockBgSlide,
              contentSlideBlock,
              eventsSlideBlock,
              createSlideBlock,
              saveBtn,
            )
            // Редакторы собраны снаружи от этого коннекшена, от которого пока не понятно, как избавляться.
            s.rightYOptC { rightYOptProxy =>
              editors(
                rightYOptProxy.value.whenDefined { rightY =>
                  ^.transform := (Css.Anim.Transform.TRANSLATE + `(` + 0.px + COMMA + rightY.px + `)`)
                },
              )
            }
          },

          // ПослеБаяние: впихнуть сюда абсолютно-плавающие color-picker'ы.
          s.colors.picker { colorPickerR.apply }

        ),

        br,

        <.div(
          ^.`class` := Css.flat( Css.Lk.Submit.SUBMIT_W, Css.Size.M ),

          // Кнопка удаления карточки.
          s.deletePropsOptC { deleteBtnR.apply }

        )
      )

      // Провайдер поддержки drag-drop:
      val rDndCtx = s.isTouchDevSomeC { isTouchDevSomeProxy =>
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

      // Пробросить color palette через контекст внутрь color2 picker'ов.
      val result = s.colorPaletteC { colorPaletteProxy =>
        val histed = MColor2PickerCtx( colorPaletteProxy.value )
        colorPickerCtxP.provide( histed )(
          rDndCtx
        )
      }

      MaterialUiUtil.postprocessTopLevel( result )
    }

  }


  private def _blockFiltered[T](f: TreeLoc[JdTag] => Option[T])
                               (mroot: MAeRoot): Option[T] = {
    // Без for, чтобы финальные Option-инстансы не пересобирались из-за финального yield/.map()
    mroot.doc.jdDoc.jdArgs
      .selJdt
      .treeLocOpt
      .filter(_.getLabel.name ==* MJdTagNames.STRIP)
      .flatMap(f)
  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { p =>

      // Фунция с дедублицированным кодом сборки коннекшена до пропертисов plus-minus control'ов (для стрипов).
      def __mkBlockFilteredC[T](f: TreeLoc[JdTag] => Option[T])
                               (implicit feq: FastEq[T]): ReactConnectProxy[Option[T]] = {
        p.connect( _blockFiltered(f)  )( OptFastEq.Wrapped( feq ) )
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

      val intFastEq = FastEqUtil.AnyValueEq[Int]

      val slideBlockPropsOptFeq = OptFastEq.Wrapped( slideBlockR.SlideBlockPropsValFastEq )

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

        savePropsC = p.connect { _.save.saveReq },

        useAsMainStripPropsOptC = p.connect { mroot =>
          for {
            // Доступно только при редактировании стрипа.
            _       <- mroot.doc.editors.stripEd
            selJdt  <- mroot.doc.jdDoc.jdArgs.selJdt.treeLocOpt.toLabelOpt
          } yield {
            import io.suggest.common.empty.OptionUtil.BoolOptOps
            useAsMainR.PropsVal(
              checked = selJdt.props1.isMain.getOrElseFalse,
              mainDefined = !mroot.doc.jdDoc.jdArgs.data.doc.template
                .subForest
                .filter( _.rootLabel.props1.isMain.getOrElseFalse )
                .isEmpty
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

        heightPropsOptC = __mkBlockFilteredC( _.getLabel.props1.heightPx )(intFastEq),

        widthPropsOptC = __mkBlockFilteredC( _.getLabel.props1.widthPx )(intFastEq),

        stripEdSOptC = p.connect( _.doc.editors.stripEd )( OptFastEq.Wrapped(MStripEdS.MStripEdSFastEq) ),

        expandModeOptC = __mkBlockFilteredC(_.getLabel.props1.expandMode)( FastEqUtil.AnyRefFastEq ),

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
            }( slideBlockPropsOptFeq )
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
            }( slideBlockPropsOptFeq )
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
            }( slideBlockPropsOptFeq )
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
            }( slideBlockPropsOptFeq )
          },
          events = {
            val k = SlideBlockKeys.EVENTS
            val title = Messages( MsgCodes.`Events` )
            p.connect { mroot =>
              for {
                treeLoc <- mroot.doc.jdDoc.jdArgs.selJdt.treeLocOpt
                jdt = treeLoc.getLabel
                if jdt.name.isEventsAllowed
              } yield {
                slideBlockR.PropsVal(
                  title     = title,
                  expanded  = mroot.doc.editors.slideBlocks.expanded contains k,
                  key       = Some(k),
                )
              }
            }( slideBlockPropsOptFeq )
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
                val topLeft2= if (selJdtTreeLoc.getLabel.name ==* MJdTagNames.STRIP)
                  pickerS.shownAt
                else
                  MCoords2di.y.modify(_ - 235)(pickerS.shownAt)

                colorPickerR.PropsVal(
                  color         = bgColor,
                  colorPresets  = mroot.doc.editors.colorsState.colorPresets.colors,
                  cssClass      = cssClassOpt,
                  topLeftPx     = Some( topLeft2 ),
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

        upStateOptC = p.connect { mroot =>
          mroot.doc.jdDoc.jdArgs.selJdt.bgEdgeDataOpt
            .flatMap(_._2.upload)
        }( OptFastEq.Wrapped( MFileUploadS.MFileUploadSFastEq ) ),

        colSuggPropsOptC = p.connect { mroot =>
          val selJdt = mroot.doc.jdDoc.jdArgs.selJdt
          for {
            bgEdge  <- selJdt.bgEdgeDataOpt
            jdEdge   = bgEdge._2.jdEdge
            nodeId  <- jdEdge.nodeId
            hist    <- mroot.doc.editors.colorsState.histograms.get( nodeId )
            // Определить colorpicker-маркер:
            selLoc  <- selJdt.treeLocOpt
            markerOpt = selLoc.getLabel.name match {
              case m: IColorPickerMarker => Some(m)
              case _ => None
            }
            if markerOpt.nonEmpty
          } yield {
            colorsSuggestR.PropsVal(
              titleMsgCode = MsgCodes.`Suggested.bg.colors`,
              colors       = hist.colors,
              marker       = markerOpt
            )
          }
        }( OptFastEq.Wrapped(colorsSuggestR.ColorsSuggestPropsValFastEq) ),

        cropBtnPropsOptC = p.connect { mroot =>
          for {
            bgEdge <- mroot.doc.jdDoc.jdArgs.selJdt.bgEdgeDataOpt
            if bgEdge._2.imgSrcOpt.nonEmpty
            // Вычислить размер контейнера. Это размер блока, для которого выбираем фон:
            treeLoc <- mroot.doc.jdDoc.jdArgs.selJdt.treeLocOpt
            wh <- treeLoc.getLabel.props1.wh
          } yield {
            CropOpen( mroot.doc.jdDoc.jdArgs.selJdt.bgEdgeDataFrk, wh )
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
                  EphemeralStream
                    .toIterable( selJdt ##:: selJdtTreeLoc.parents.map(_._2) )
                    .iterator
                    .flatMap(_.props1.bgColor)
                    .nextOption()
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

        isTouchDevSomeC = p.connect { mroot =>
          OptionUtil.SomeBool( mroot.conf.touchDev )
        }( FastEq.AnyRefEq ),

        colorPaletteC = p.connect( _.doc.editors.colorsState.colorPresets ),

      )
    }
    .renderBackend[Backend]
    .build

}
