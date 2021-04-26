package io.suggest.sc.v.dia.dlapp

import com.github.zpao.qrcode.react.{ReactQrCode, ReactQrCodeProps}
import com.materialui.{Mui, MuiAccordion, MuiAccordionDetails, MuiAccordionProps, MuiAccordionSummary, MuiAccordionSummaryClasses, MuiAccordionSummaryProps, MuiButton, MuiButtonClasses, MuiButtonProps, MuiButtonSizes, MuiButtonVariants, MuiCircularProgress, MuiCircularProgressProps, MuiDialog, MuiDialogActions, MuiDialogClasses, MuiDialogContent, MuiDialogMaxWidths, MuiDialogProps, MuiFormControlClasses, MuiLink, MuiLinkClasses, MuiLinkProps, MuiMenuItem, MuiMenuItemClasses, MuiMenuItemProps, MuiProgressVariants, MuiSelectClasses, MuiSelectProps, MuiTable, MuiTableBody, MuiTableCell, MuiTableCellClasses, MuiTableCellProps, MuiTableRow, MuiTextField, MuiTextFieldProps, MuiTypoGraphy, MuiTypoGraphyProps, MuiTypoGraphyVariants}
import diode.react.ReactPot._
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.empty.OptionUtil
import io.suggest.common.html.HtmlConstants
import io.suggest.crypto.hash.HashesHex
import io.suggest.css.Css
import io.suggest.dev.{MOsFamilies, MOsFamily, OsFamiliesR}
import io.suggest.ext.svc.MExtServices
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.lk.r.plat.{PlatformComponents, PlatformCssStatic}
import io.suggest.msg.JsFormatUtil
import io.suggest.n2.media.MFileMetaHash
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.routes.routes
import io.suggest.sc.app.{MScAppDlInfo, MScAppManifestQs}
import io.suggest.sc.m.menu._
import io.suggest.sc.m.MScRoot
import io.suggest.sc.v.styl.ScCssStatic
import io.suggest.xplay.json.PlayJsonSjsUtil
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import play.api.libs.json.Json
import scalacss.ScalaCssReact._

import scala.scalajs.LinkingInfo.developmentMode
import scala.scalajs.js.{URIUtils, UndefOr}
import scala.scalajs.js.annotation.JSName

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.02.2020 16:45
  * Description: Диалог скачивания нативного приложения.
  */
class DlAppDiaR(
                 osFamiliesR        : OsFamiliesR,
                 platformComponents : PlatformComponents,
                 crCtxP             : React.Context[MCommonReactCtx],
                 platformCssP       : React.Context[PlatformCssStatic],
               ) {

  type Props_t = MScRoot
  type Props = ModelProxy[Props_t]

  case class State(
                    diaOpenedSomeC    : ReactConnectProxy[Some[Boolean]],
                    dlAppDiaC         : ReactConnectProxy[MDlAppDia],
                    iosFileDlInfoC    : ReactConnectProxy[Option[MScAppDlInfo]],
                  )

  class Backend( $: BackendScope[Props, State] ) {

    private val _onCloseBtnClick = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, DlAppOpen( opened = false ) )
    }

    private val _onOsFamilyChange = ReactCommonUtil.cbFun1ToJsCb { e: ReactEventFromInput =>
      val osFamily2 = MOsFamilies.withValue( e.target.value )
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, PlatformSetAppDl( osFamily2 ) )
    }

    private def _onExpandChanged(index: Int) = ReactCommonUtil.cbFun2ToJsCb { (_: ReactEvent, isExpanded: Boolean) =>
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, ExpandDlApp( index, isExpanded ) )
    }

    private lazy val _onShowTechInfoClick = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      ReactDiodeUtil.dispatchOnProxyScopeCBf($) { propsProxy: Props =>
        TechInfoDlAppShow( !propsProxy.value.index.menu.dlApp.showTechInfo )
      }
    }

    private lazy val _onQrCodeExpandClick = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      ReactDiodeUtil.dispatchOnProxyScopeCBf($) { propsProxy: Props =>
        QrCodeExpand( !propsProxy.value.index.menu.dlApp.qrCodeExpanded )
      }
    }


    def render(s: State): VdomElement = {
      // Отрендерить список платформ в дефолтовом порядке:
      val chooseMsgCode = MsgCodes.`Choose...`
      val chooseText = crCtxP.message( chooseMsgCode )
      val menuRowCss = ScCssStatic.Menu.Rows.mui5ListItem.htmlClass
      val platformsRows: Seq[VdomNode] = MuiMenuItem.component.withKey( chooseMsgCode )(
        new MuiMenuItemProps {
          override val value = chooseMsgCode
          override val disabled = true
          override val classes = new MuiMenuItemClasses {
            override val root = menuRowCss
          }
        }
      )(
        chooseText
      ) :: osFamiliesR.osFamiliesMenuItems(
        itemCss = new MuiMenuItemClasses {
          override val root = Css.flat( ScCssStatic.flexCenter.htmlClass, menuRowCss )
        },
        textCss = ScCssStatic.thinText.htmlClass,
      )

      val _osFamilySelectCss = new MuiFormControlClasses {
        override val root = ScCssStatic.AppDl.osFamily.htmlClass
      }
      val _selectProps = new MuiSelectProps {
        override val classes = new MuiSelectClasses {
          override val selectMenu = ScCssStatic.flexCenter.htmlClass
        }
        override val variant = MuiTextField.Variants.standard
      }

      // Содержимое диалога
      val diaContent = crCtxP.consume { crCtx =>
        lazy val bytesMsg = crCtx.messages( MsgCodes.`B._Bytes` )
        lazy val fileMsg = crCtx.messages( MsgCodes.`File` )
        lazy val sizeMsg = crCtx.messages( MsgCodes.`Size` ): VdomNode

        // "Скачать файл"
        lazy val dlFileMsg = crCtx.messages( MsgCodes.`Download.0`, fileMsg.toLowerCase() ): VdomNode

        lazy val noDlsAvailMsg = crCtx.messages( MsgCodes.`No.downloads.available` ): VdomNode
        lazy val installMsg = crCtx.messages( MsgCodes.`Install` ): VdomNode
        lazy val pleaseWaitMsg = crCtx.messages( MsgCodes.`Please.wait` ): VdomNode

        // tech-info
        lazy val infoMsg = crCtx.messages( MsgCodes.`Information` ): VdomNode
        lazy val typeMsg = crCtx.messages( MsgCodes.`Type` ): VdomNode

        // css для кнопки скачивания/перехода/установки.
        val goDlInstBtnCss = new MuiButtonClasses {
          override val root = ScCssStatic.AppDl.dlLinkOrBtn.htmlClass
        }
        // css для вторичной ссылки (неприоритетной ссылки по сравнению с основной кнопкой).
        lazy val dlLinkCss = new MuiLinkClasses {
          override val root = ScCssStatic.AppDl.dlLinkOrBtn.htmlClass
        }

        // Кнопка "Установить" для установки на iOS.
        lazy val iosFileInstall = s.iosFileDlInfoC { iosFileDlInfoOptProxy =>
          iosFileDlInfoOptProxy
            .value
            .whenDefinedEl { iosFileDlInfo =>
              val manifestQs = MScAppManifestQs(
                onNodeId = iosFileDlInfo.fromNodeIdOpt,
                hashesHex = iosFileDlInfo.fileMeta
                  .fold[HashesHex](Map.empty) { fm =>
                    MFileMetaHash.toHashesHex( fm.hashesHex )
                  },
              )
              val manifestUrl = routes.controllers.ScApp
                .iosInstallManifest(
                  PlayJsonSjsUtil.toNativeJsonObj(
                    Json.toJsObject( manifestQs ))
                )
                .absoluteURL( secure = true )
              val itunesUrl = "itms-services://?action=download-manifest&url=" + URIUtils.encodeURIComponent( manifestUrl )
              val setupIcon = Mui.SvgIcons.PhonelinkSetup()()
              MuiButton {
                new MuiButtonProps {
                  override val href = itunesUrl
                  override val component = "a"
                  override val size = MuiButtonSizes.large
                  override val variant = MuiButtonVariants.contained
                  override val classes = goDlInstBtnCss
                  override val startIcon = setupIcon.rawNode
                }
              } (
                installMsg,
              )
            }
        }

        val fmCellCss = new MuiTableCellClasses {
          override val root = ScCssStatic.AppDl.hardWordWrap.htmlClass
        }
        val fmCellProps = new MuiTableCellProps {
          override val classes = fmCellCss
        }

        lazy val getAppIcon = Mui.SvgIcons.GetApp()(): VdomElement

        React.Fragment(

          platformComponents.diaTitle( Nil )(
            platformComponents.diaTitleText(
              crCtx.messages( MsgCodes.`Download.application` ),
            ),
            // TODO Кнопка сокрытия?
          ),

          s.dlAppDiaC { dlAppDiaProxy =>
            val dlAppDia = dlAppDiaProxy.value
            val osFamilyStr = dlAppDia.platform
              .fold( chooseMsgCode )(_.value)

            MuiDialogContent()(
              <.div(
                ScCssStatic.flexCenter,
                ScCssStatic.justifyCenter,
                MuiTextField(
                  new MuiTextFieldProps {
                    override val select   = true
                    override val value    = osFamilyStr
                    override val onChange = _onOsFamilyChange
                    override val classes  = _osFamilySelectCss
                    override val SelectProps = _selectProps
                    override val disabled = dlAppDia.getReq.isPending
                    override val variant  = MuiTextField.Variants.standard
                  }
                )(
                  platformsRows: _*
                ),
              ),

              <.br,

              // Отрендерить кнопки-ссылки для скачивания, данные для ссылок приходят запросом с сервера.
              dlAppDia.getReq.render { resp =>
                if (resp.dlInfos.isEmpty) {
                  MuiTypoGraphy(
                    new MuiTypoGraphyProps {
                      override val variant = MuiTypoGraphyVariants.h6
                    }
                  )(
                    noDlsAvailMsg
                  )

                } else {
                  val summaryCss = new MuiAccordionSummaryClasses {
                    override val content = Css.flat( ScCssStatic.thinText.htmlClass, ScCssStatic.flexCenter.htmlClass )
                  }
                  val summaryProps = new MuiAccordionSummaryProps {
                    override val classes = summaryCss
                  }
                  <.div(
                    (for {
                      (dlInfo, index) <- resp
                        .dlInfos
                        .iterator
                        .zipWithIndex
                    } yield {
                      val isIosFileDl = dlInfo.isIosFileDl

                      val extSvcIconOpt = dlInfo.extSvc.collect[VdomElement] {
                        case MExtServices.GooglePlay =>
                          Mui.SvgIcons.Shop()()
                        case MExtServices.AppleITunes =>
                          Mui.SvgIcons.Apple()()
                      }

                      MuiAccordion.component.withKey( index )(
                        new MuiAccordionProps {
                          @JSName("onChange")
                          override val onChange2 = _onExpandChanged( index )
                          override val expanded = dlAppDia.expanded contains index
                        }
                      )(

                        // Что откуда качать:
                        MuiAccordionSummary( summaryProps )(
                          // Левая иконка:
                          if (dlInfo.extSvc.isEmpty) {
                            getAppIcon
                          } else {
                            extSvcIconOpt getOrElse EmptyVdom
                          },
                          HtmlConstants.NBSP_STR,
                          HtmlConstants.NBSP_STR,
                          // Заголовок панели.
                          dlInfo.extSvc.fold[VdomNode](
                            dlFileMsg,
                          )( _.nameI18N ),
                        ),

                        // Раскрытая часть панели.
                        MuiAccordionDetails()(

                          MuiTable()(
                            MuiTableBody()(

                              // Имя файла
                              dlInfo.fileName.whenDefinedNode { fileName =>
                                MuiTableRow()(
                                  MuiTableCell()( fileMsg ),
                                  MuiTableCell(fmCellProps)( fileName ),
                                )
                              },

                              // Размер файла
                              dlInfo.fileMeta.flatMap(_.sizeB).whenDefinedNode { fileSizeB =>
                                MuiTableRow()(
                                  MuiTableCell()( sizeMsg ),
                                  MuiTableCell()(
                                    JsFormatUtil.formatKilMegGigTer(
                                      value     = fileSizeB,
                                      baseUnits = bytesMsg,
                                      use1024   = true,
                                    )(crCtx.messages)
                                  ),
                                )
                              },

                              // Если это файл, то отрендерить доступ техническую информацию.
                              ReactCommonUtil.maybeNode( dlInfo.extSvc.isEmpty ) {
                                React.Fragment(
                                  MuiTableRow()(
                                    MuiTableCell()( infoMsg ),
                                    MuiTableCell(fmCellProps)(
                                      MuiLink(
                                        new MuiLinkProps {
                                          override val onClick = _onShowTechInfoClick
                                        }
                                      )(
                                        crCtx.messages( if (dlAppDia.showTechInfo) MsgCodes.`Hide` else MsgCodes.`Show` ),
                                      )
                                    ),
                                  ),

                                  // контрольная сумма SHA-1
                                  ReactCommonUtil.maybeNode( dlAppDia.showTechInfo ) {
                                    React.Fragment(
                                      dlInfo.fileMeta.flatMap(_.mime).whenDefinedNode { contentType =>
                                        MuiTableRow()(
                                          MuiTableCell()( typeMsg ),
                                          MuiTableCell(fmCellProps)( contentType ),
                                        )
                                      },

                                      dlInfo.fileMeta.flatMap(_.hashesHex.dlHash).whenDefinedNode { dlHash =>
                                        MuiTableRow()(
                                          MuiTableCell()( dlHash.hType.fullStdName ),
                                          MuiTableCell(fmCellProps)( dlHash.hexValue ),
                                        )
                                      },
                                    )
                                  },
                                )
                              },

                              // Ряд с кнопками перехода/скачивания/установки.
                              MuiTableRow()(
                                MuiTableCell(
                                  new MuiTableCellProps {
                                    val colSpan = 2
                                  }
                                )(
                                  // Если файл для iOS, то отрендерить ссылку для непосредственной установки через манифест:
                                  ReactCommonUtil.maybeNode( isIosFileDl )( iosFileInstall ),

                                  // Кнопка "Скачать" отображается всегда.
                                  if (isIosFileDl) {
                                    // Ссылка, чтобы не оттягивала внимания.
                                    React.Fragment(
                                      // TODO Нужно выровнять через css.
                                      <.br,
                                      <.br,
                                      <.br,
                                      MuiLink(
                                        new MuiLinkProps {
                                          val href = dlInfo.url
                                          override val classes = dlLinkCss
                                        }
                                      )( dlFileMsg ),
                                    )
                                  } else {
                                    MuiButton(
                                      new MuiButtonProps {
                                        override val href = dlInfo.url
                                        override val component = "a"
                                        // TODO val target = "_blank" для переходов в play/appstore
                                        override val size = MuiButtonSizes.large
                                        override val variant = MuiButtonVariants.contained
                                        override val classes = goDlInstBtnCss
                                      }
                                    )(
                                      dlInfo.extSvc.fold[VdomNode] {
                                        React.Fragment(
                                          getAppIcon,
                                          dlFileMsg,
                                        )
                                      } { extSvc =>
                                        React.Fragment(
                                          extSvcIconOpt.whenDefinedNode,
                                          crCtx.messages( MsgCodes.`Open.0`, extSvc.nameI18N ),
                                        )
                                      }
                                    )
                                  },

                                ),
                              ),

                              // qr-код:
                              MuiTableRow()(
                                MuiTableCell(
                                  new MuiTableCellProps {
                                    val colSpan = 2
                                  }
                                )(
                                  MuiButton(
                                    new MuiButtonProps {
                                      override val variant = if (dlAppDia.qrCodeExpanded) MuiButtonVariants.text else MuiButtonVariants.outlined
                                      override val onClick = _onQrCodeExpandClick
                                    }
                                  )(
                                    ReactQrCode(
                                      new ReactQrCodeProps {
                                        override val value = dlInfo.url
                                        override val renderAs = ReactQrCode.RenderAs.SVG
                                        override val size = if (dlAppDia.qrCodeExpanded) 270 else 70
                                      }
                                    ),
                                  ),
                                ),
                              ),

                            ),
                          ),

                        ),

                      )
                    })
                      .toVdomArray
                  )
                }
              },

              dlAppDia.getReq.renderPending { _ =>
                React.Fragment(
                  MuiCircularProgress(
                    new MuiCircularProgressProps {
                      override val variant = MuiProgressVariants.indeterminate
                    }
                  ),
                  MuiTypoGraphy()(
                    pleaseWaitMsg,
                  ),
                )
              },

            )
          },


          platformCssP.consume { platformCss =>
            MuiDialogActions {
              platformComponents.diaActionsProps()(platformCss)
            } (
              MuiButton(
                new MuiButtonProps {
                  override val onClick = _onCloseBtnClick
                  override val variant = MuiButtonVariants.text
                  override val size    = MuiButtonSizes.large
                }
              )(
                crCtx.messages( MsgCodes.`Close` ),
              )
            )
          },

        )
      }


      platformCssP.consume { platformCss =>
        val muiDiaCss = new MuiDialogClasses {
          override val paper = platformCss.Dialogs.paper.htmlClass
        }
        // Открытость диалога.
        s.diaOpenedSomeC { diaOpenedSomeProxy =>
          MuiDialog(
            new MuiDialogProps {
              override val open       = diaOpenedSomeProxy.value.value
              override val onClose    = _onCloseBtnClick
              override val maxWidth   = MuiDialogMaxWidths.xs
              override val fullWidth  = true
              override val classes    = muiDiaCss
            }
          )( diaContent )
        }
      }
    }

  }


  // lazy, т.к. внутри cordova-приложения оно скрыто.
  lazy val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(
        diaOpenedSomeC = propsProxy.connect { props =>
          OptionUtil.SomeBool( props.index.menu.dlApp.opened )
        },
        dlAppDiaC = propsProxy.connect( _.index.menu.dlApp ),
        iosFileDlInfoC = propsProxy.connect { props =>
          for {
            dlInfosResp <- props.index.menu.dlApp.getReqOpt
            if (props.dev.platform.osFamily contains[MOsFamily] MOsFamilies.Apple_iOS) || developmentMode
            iosFileInfo <- dlInfosResp.iosFileDl
          } yield iosFileInfo
        },
      )
    }
    .renderBackend[Backend]
    .build

}
