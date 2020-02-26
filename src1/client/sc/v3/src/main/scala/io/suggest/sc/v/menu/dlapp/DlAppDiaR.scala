package io.suggest.sc.v.menu.dlapp

import com.github.zpao.qrcode.react.{ReactQrCode, ReactQrCodeProps}
import com.materialui.{Mui, MuiButton, MuiButtonProps, MuiButtonSizes, MuiButtonVariants, MuiCircularProgress, MuiCircularProgressProps, MuiDialog, MuiDialogActions, MuiDialogContent, MuiDialogMaxWidths, MuiDialogProps, MuiDialogTitle, MuiDivider, MuiExpansionPanel, MuiExpansionPanelDetails, MuiExpansionPanelProps, MuiExpansionPanelSummary, MuiFormControlClasses, MuiLink, MuiLinkProps, MuiList, MuiListItem, MuiListItemText, MuiMenuItem, MuiMenuItemProps, MuiProgressVariants, MuiTable, MuiTableBody, MuiTableCell, MuiTableCellProps, MuiTableRow, MuiTextField, MuiTextFieldProps, MuiTypoGraphy, MuiTypoGraphyProps, MuiTypoGraphyVariants}
import diode.react.ReactPot._
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.empty.OptionUtil
import io.suggest.dev.{MOsFamilies, MOsFamily, OsFamiliesR}
import io.suggest.ext.svc.MExtServices
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.msg.JsFormatUtil
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.routes.ScJsRoutes
import io.suggest.sc.app.MScAppDlInfo
import io.suggest.sc.m.menu.{ExpandDlApp, MDlAppDia, OpenCloseAppDl, PlatformSetAppDl}
import io.suggest.sc.m.{MScReactCtx, MScRoot}
import io.suggest.sc.styl.ScCssStatic
import io.suggest.sjs.common.empty.JsOptionUtil.Implicits._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.univeq._

import scala.scalajs.js.URIUtils
import scala.scalajs.js.annotation.JSName
import scalajs.LinkingInfo.developmentMode

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.02.2020 16:45
  * Description: Диалог скачивания нативного приложения.
  */
class DlAppDiaR(
                 osFamiliesR        : OsFamiliesR,
                 crCtxProv          : React.Context[MCommonReactCtx],
                 scReactCtxP        : React.Context[MScReactCtx],
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
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, OpenCloseAppDl( opened = false ) )
    }

    private val _onOsFamilyChange = ReactCommonUtil.cbFun1ToJsCb { e: ReactEventFromInput =>
      val osFamily2 = MOsFamilies.withValue( e.target.value )
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, PlatformSetAppDl( osFamily2 ) )
    }

    private def _onExpandChanged(index: Int) = ReactCommonUtil.cbFun2ToJsCb { (_: ReactEvent, isExpanded: Boolean) =>
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, ExpandDlApp( index, isExpanded ) )
    }

    /*
    private def _onDownLoadBtnClick(dlInfo: MScAppDlInfo) = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, DlApp(dlInfo) )
    }
    */


    def render(s: State): VdomElement = {
      // Отрендерить список платформ в дефолтовом порядке:
      val chooseMsgCode = MsgCodes.`Choose...`
      val chooseText = crCtxProv.message( chooseMsgCode )
      val platformsRows: Seq[VdomNode] = MuiMenuItem.component.withKey( chooseMsgCode )(
        new MuiMenuItemProps {
          override val value = chooseMsgCode
          val disabled = true
        }
      )(
        chooseText
      ) :: osFamiliesR.osFamiliesMenuItems

      val _osFamilySelectCss = new MuiFormControlClasses {
        override val root = ScCssStatic.AppDl.osFamily.htmlClass
      }

      // Содержимое диалога
      val diaContent = crCtxProv.consume { crCtx =>
        lazy val bytesMsg = crCtx.messages( MsgCodes.`B._Bytes` )
        lazy val fileMsg = crCtx.messages( MsgCodes.`File` )
        lazy val sizeMsg = crCtx.messages( MsgCodes.`Size` ): VdomNode

        // "Скачать файл"
        lazy val dlFileMsg = crCtx.messages( MsgCodes.`Download.0`, fileMsg.toLowerCase() ): VdomNode

        lazy val noDlsAvailMsg = crCtx.messages( MsgCodes.`No.downloads.available` ): VdomNode
        lazy val installMsg = crCtx.messages( MsgCodes.`Install` ): VdomNode
        lazy val pleaseWaitMsg = crCtx.messages( MsgCodes.`Please.wait` ): VdomNode

        // Кнопка "Установить" для установки на iOS.
        lazy val iosFileInstall = s.iosFileDlInfoC { iosFileDlInfoOptProxy =>
          iosFileDlInfoOptProxy
            .value
            .whenDefinedEl { iosFileDlInfo =>
              val manifestUrl = ScJsRoutes.controllers.ScApp
                .iosInstallManifest( iosFileDlInfo.fromNodeIdOpt.toUndef )
                .absoluteURL( secure = true )
              val itunesUrl = "itms-services://?action=download-manifest&url=" + URIUtils.encodeURIComponent( manifestUrl )
              MuiButton {
                new MuiButtonProps {
                  override val href = itunesUrl
                  override val component = "a"
                  override val size = MuiButtonSizes.large
                  override val variant = MuiButtonVariants.contained
                }
              } (
                installMsg,
              )
            }
        }

        React.Fragment(

          MuiDialogTitle()(
            crCtx.messages( MsgCodes.`Download.application` ),
            // TODO Кнопка сокрытия.
          ),

          s.dlAppDiaC { dlAppDiaProxy =>
            val dlAppDia = dlAppDiaProxy.value
            val osFamilyStr = dlAppDia.platform
              .fold( chooseMsgCode )(_.value)

            MuiDialogContent()(
              MuiTextField(
                new MuiTextFieldProps {
                  override val select   = true
                  override val value    = osFamilyStr
                  override val onChange = _onOsFamilyChange
                  override val classes  = _osFamilySelectCss
                  override val disabled = dlAppDia.getReq.isPending
                }
              )(
                platformsRows: _*
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
                  <.div(
                    (for {
                      (dlInfo, index) <- resp
                        .dlInfos
                        .iterator
                        .zipWithIndex
                    } yield {
                      val isIosFileDl = dlInfo.isIosFileDl

                      MuiExpansionPanel.component.withKey( index )(
                        new MuiExpansionPanelProps {
                          @JSName("onChange")
                          override val onChange2 = _onExpandChanged( index )
                          override val expanded = dlAppDia.expanded contains index
                        }
                      )(

                        // Заголовок панели.
                        MuiExpansionPanelSummary()(
                          // Левая иконка:
                          dlInfo.extSvc.fold[VdomNode](
                            Mui.SvgIcons.GetApp()()
                          ) {
                            case MExtServices.GooglePlay =>
                              Mui.SvgIcons.Shop()()
                            case MExtServices.AppleITunes =>
                              Mui.SvgIcons.Apple()()
                            case _ =>
                              EmptyVdom
                          },
                          // Что откуда качать:
                          dlInfo.extSvc.fold[VdomNode](
                            dlFileMsg,
                          )( _.nameI18N ),
                        ),

                        // Раскрытая часть панели.
                        MuiExpansionPanelDetails()(

                          MuiTable()(
                            MuiTableBody()(

                              // Имя файла
                              dlInfo.fileName.whenDefinedNode { fileName =>
                                MuiTableRow()(
                                  MuiTableCell()( fileMsg ),
                                  MuiTableCell()( fileName ),
                                )
                              },

                              // Размер файла
                              dlInfo.fileSizeB.whenDefinedNode { fileSizeB =>
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

                              // TODO контрольная сумма SHA-1

                              MuiTableRow()(
                                MuiTableCell(
                                  new MuiTableCellProps {
                                    val colspan = 2
                                  }
                                )(
                                  // Если файл для iOS, то отрендерить ссылку для непосредственной установки через манифест:
                                  ReactCommonUtil.maybeNode( isIosFileDl )(
                                    <.div(
                                      iosFileInstall,
                                      <.br,
                                      <.br,
                                    )
                                  ),

                                  // Кнопка "Скачать" отображается всегда.
                                  if (isIosFileDl) {
                                    // Ссылка, чтобы не оттягивала внимания.
                                    MuiLink(
                                      new MuiLinkProps {
                                        val href = dlInfo.url
                                      }
                                    )( dlFileMsg )
                                  } else {
                                    MuiButton(
                                      new MuiButtonProps {
                                        override val href = dlInfo.url
                                        override val component = "a"
                                        // TODO val target = "_blank" для переходов в play/appstore
                                        override val size = MuiButtonSizes.large
                                        override val variant = MuiButtonVariants.contained
                                      }
                                    )(
                                      dlInfo.extSvc.fold {
                                        dlFileMsg
                                      } { extSvc =>
                                        crCtx.messages( MsgCodes.`Open.0`, extSvc.nameI18N )
                                      }
                                    )
                                  },

                                ),
                              ),

                              // qr-код:
                              MuiTableRow()(
                                MuiTableCell(
                                  new MuiTableCellProps {
                                    val colspan = 2
                                  }
                                )(
                                  MuiListItem()(
                                    MuiListItemText()(
                                      ReactQrCode(
                                        new ReactQrCodeProps {
                                          override val value = dlInfo.url
                                          override val renderAs = ReactQrCode.RenderAs.SVG
                                          override val size = 270
                                        }
                                      ),
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


          MuiDialogActions()(
            MuiButton(
              new MuiButtonProps {
                override val onClick = _onCloseBtnClick
              }
            )(
              crCtx.messages( MsgCodes.`Close` ),
            )
          ),

        )
      }


      // Открытость диалога.
      s.diaOpenedSomeC { diaOpenedSomeProxy =>
        MuiDialog(
          new MuiDialogProps {
            override val open       = diaOpenedSomeProxy.value.value
            override val onClose    = _onCloseBtnClick
            override val maxWidth   = MuiDialogMaxWidths.xs
            override val fullWidth  = true
          }
        )( diaContent )
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(
        diaOpenedSomeC = propsProxy.connect { props =>
          OptionUtil.SomeBool( props.index.menu.dlApp.opened )
        },
        dlAppDiaC = propsProxy.connect( _.index.menu.dlApp )( MDlAppDia.MDlAppDiaFeq ),
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
