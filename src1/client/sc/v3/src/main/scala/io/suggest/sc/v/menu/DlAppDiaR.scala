package io.suggest.sc.v.menu

import com.materialui.{Mui, MuiButton, MuiButtonBaseCommonProps, MuiButtonProps, MuiCircularProgress, MuiCircularProgressProps, MuiDialog, MuiDialogActions, MuiDialogContent, MuiDialogMaxWidths, MuiDialogProps, MuiDialogTitle, MuiFormControlClasses, MuiList, MuiListItem, MuiListItemIcon, MuiListItemProps, MuiListItemText, MuiListItemTextProps, MuiMenuItem, MuiMenuItemProps, MuiProgressVariants, MuiTextField, MuiTextFieldProps, MuiTypoGraphy, MuiTypoGraphyProps, MuiTypoGraphyVariants}
import diode.data.Pot
import diode.react.ReactPot._
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.empty.OptionUtil
import io.suggest.dev.{MOsFamilies, MOsFamily, OsFamiliesR}
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.sc.app.MScAppGetResp
import io.suggest.sc.m.{MScReactCtx, MScRoot}
import ReactCommonUtil.Implicits._
import io.suggest.common.html.HtmlConstants.`.`
import io.suggest.ext.svc.MExtServices
import io.suggest.msg.JsFormatUtil
import io.suggest.sc.m.menu.{MDlAppDia, OpenCloseAppDl, PlatformSetAppDl}
import io.suggest.sc.styl.ScCssStatic
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.univeq._

import scala.scalajs.js

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
                    devPlatformOptC   : ReactConnectProxy[Option[MOsFamily]],
                    appDlRespPotC     : ReactConnectProxy[Pot[MScAppGetResp]],
                  )

  class Backend( $: BackendScope[Props, State] ) {

    private val _onCloseBtnClick = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, OpenCloseAppDl( opened = false ) )
    }

    private val _onOsFamilyChange = ReactCommonUtil.cbFun1ToJsCb { e: ReactEventFromInput =>
      val osFamily2 = MOsFamilies.withValue( e.target.value )
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, PlatformSetAppDl( osFamily2 ) )
    }

    /*
    private def _onDownLoadBtnClick(dlInfo: MScAppDlInfo) = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, DlApp(dlInfo) )
    }
    */


    def render(s: State): VdomElement = {
      // Отрендерить список платформ в дефолтовом порядке:
      val choosePlatformKey = MsgCodes.`Choose...`
      val chooseText = crCtxProv.message( choosePlatformKey )
      val platformsRows: Seq[VdomNode] = MuiMenuItem.component.withKey( choosePlatformKey )(
        new MuiMenuItemProps {
          override val value = choosePlatformKey
          val disabled = true
        }
      )(
        chooseText
      ) :: osFamiliesR.osFamiliesMenuItems

      // Содержимое диалога
      val diaContent = crCtxProv.consume { crCtx =>
        lazy val bytesMsg = crCtx.messages( MsgCodes.`B._Bytes` )

        React.Fragment(

          MuiDialogTitle()(
            crCtx.messages( MsgCodes.`Download.application` ),
            // TODO Кнопка сокрытия.
          ),

          MuiDialogContent()(
            {
              val _osFamilyCss = new MuiFormControlClasses {
                override val root = ScCssStatic.AppDl.osFamily.htmlClass
              }

              s.dlAppDiaC { dlAppDiaProxy =>
                val dlAppDia = dlAppDiaProxy.value
                val osFamilyStr = dlAppDia.platform
                  .fold( choosePlatformKey )(_.value)

                MuiTextField(
                  new MuiTextFieldProps {
                    override val select   = true
                    override val value    = osFamilyStr
                    override val onChange = _onOsFamilyChange
                    override val classes  = _osFamilyCss
                    override val disabled = dlAppDia.getReq.isPending
                  }
                )(
                  platformsRows: _*
                )
              }
            },

            <.br,

            // Отрендерить кнопки-ссылки для скачивания, данные для ссылок приходят запросом с сервера.
            s.appDlRespPotC { appDlRespPotProxy =>
              val appDlRespPot = appDlRespPotProxy.value

              React.Fragment(

                appDlRespPot.render { resp =>
                  if (resp.dlInfos.isEmpty) {
                    MuiTypoGraphy(
                      new MuiTypoGraphyProps {
                        override val variant = MuiTypoGraphyVariants.h6
                      }
                    )(
                      crCtx.messages( MsgCodes.`No.downloads.available` )
                    )

                  } else {
                    MuiList()(
                      (for (dlInfo <- resp.dlInfos) yield {
                        MuiListItem.component.withKey( dlInfo.predicate.value + `.` + dlInfo.extSvc.fold("")(_.value) )(
                          new MuiListItemProps with MuiButtonBaseCommonProps {
                            override val disabled = false
                            override val button = true
                            override val component = js.defined( "a" )
                            //override val onClick = _onDownLoadBtnClick( dlInfo )
                            val href = dlInfo.url
                          }
                        )(

                          // Иконка, если есть.
                          MuiListItemIcon()(
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
                          ),

                          {
                            // Первая строка - что откуда качать:
                            val _primary = dlInfo.extSvc.fold[VdomNode](
                              // "Скачать файл"
                              crCtx.messages(
                                MsgCodes.`Download.0`,
                                crCtx.messages( MsgCodes.`File` ).toLowerCase(),
                              )
                            )( _.nameI18N )

                            // Отрендерить название файла, размер файла.
                            val _secondary = React.Fragment(
                              dlInfo.fileName.whenDefinedNode,
                              <.span(
                                ^.float.right,
                                dlInfo.fileSizeB.whenDefined { sizeB =>
                                  JsFormatUtil.formatKilMegGigTer(
                                    value     = sizeB,
                                    baseUnits = bytesMsg,
                                    use1024   = true,
                                  )(crCtx.messages)
                                },
                              )
                            )

                            MuiListItemText(
                              new MuiListItemTextProps {
                                override val primary = _primary.rawNode
                                override val secondary = _secondary.rawNode
                              }
                            )()
                          },

                        ): VdomElement
                      }): _*
                    )
                  }
                },

                /*
                appDlRespPot.renderFailed { ex =>
                  <.div(
                    MuiTypoGraphy()(
                      crCtx.messages( MsgCodes.`Something.gone.wrong` )
                    ),
                    <.br,
                    MuiTypoGraphy()(
                      ex.getMessage,
                    )
                  )
                },
                */

                appDlRespPot.renderPending { _ =>
                  React.Fragment(
                    MuiCircularProgress(
                      new MuiCircularProgressProps {
                        override val variant = MuiProgressVariants.indeterminate
                      }
                    ),
                    MuiTypoGraphy()(
                      crCtx.messages( MsgCodes.`Please.wait` ),
                    ),
                  )
                },

              )
            }

          ),

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
        devPlatformOptC = propsProxy.connect( _.dev.platform.osFamily ),
        appDlRespPotC = propsProxy.connect( _.index.menu.dlApp.getReq ),
      )
    }
    .renderBackend[Backend]
    .build

}
