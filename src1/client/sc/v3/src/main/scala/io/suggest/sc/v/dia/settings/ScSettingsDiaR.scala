package io.suggest.sc.v.dia.settings

import com.materialui.{MuiButton, MuiButtonProps, MuiButtonSizes, MuiButtonVariants, MuiDialog, MuiDialogActions, MuiDialogClasses, MuiDialogContent, MuiDialogMaxWidths, MuiDialogProps, MuiList, MuiListItem, MuiListItemProps, MuiListItemText, MuiListItemTextProps}
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.empty.OptionUtil
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.lk.r.plat.{PlatformComponents, PlatformCssStatic}
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.sc.m.{MScRoot, SetDebug, SettingsDiaOpen}
import io.suggest.sc.v.menu.VersionR
import io.suggest.spa.DiodeUtil
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.03.2020 18:37
  * Description: wrap-компонент диалога настроек выдачи.
  * Т.к. настройки раскиданы по всему состоянию выдачи, компонент зависит от корневого состояния.
  */
class ScSettingsDiaR(
                      geoLocSettingR          : GeoLocSettingR,
                      blueToothSettingR       : BlueToothSettingR,
                      blueToothUnAvailInfoR   : BlueToothUnAvailInfoR,
                      unsafeOffsetSettingR    : UnsafeOffsetSettingR,
                      notificationSettingsR   : NotificationSettingsR,
                      langSettingR            : LangSettingR,
                      platformComponents      : PlatformComponents,
                      onOffSettingR           : OnOffSettingR,
                      versionR                : VersionR,
                      crCtxProv               : React.Context[MCommonReactCtx],
                      platformCssP            : React.Context[PlatformCssStatic],
                    ) {

  type Props_t = MScRoot
  type Props = ModelProxy[Props_t]


  case class State(
                    openedSomeC       : ReactConnectProxy[Some[Boolean]],
                    debugSomeC        : ReactConnectProxy[Some[Boolean]],
                  )

  class Backend($: BackendScope[Props, State]) {

    private val _onCloseDialog = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, SettingsDiaOpen(opened = false) )
    }

    def render(p: Props, s: State): VdomElement = {
      val diaChildren = List[VdomElement](

        // Заголовок диалога.
        platformComponents.diaTitle(Nil)(
          platformComponents.diaTitleText(
            crCtxProv.message( MsgCodes.`Settings` ),
          ),
        ),

        // Сами настройки.
        MuiDialogContent()(
          MuiList()(

            // Переключалка геолокации
            p.wrap(_.dev.geoLoc.switch)( geoLocSettingR.component.apply ),

            // Переключалка bluetooth
            p.wrap(_.dev.beaconer)( blueToothSettingR.component.apply ),

            // Плашка об отсутствии bluetooth.
            p.wrap(_.dev)( blueToothUnAvailInfoR.component.apply ),

            // Переключалка уведомлений.
            p.wrap(_.dev.osNotify)( notificationSettingsR.component.apply ),

            // UI language switcher:
            p.wrap(_.internals.info.reactCtx)( langSettingR.component.apply ),

            // debug-функции не должны тормозить работу выдачи, если нет флага при запуске.
            s.debugSomeC { isDebugEnabled =>
              ReactCommonUtil.maybeEl( isDebugEnabled.value.value ) {
                React.Fragment(

                  // Разделитель.
                  MuiListItem(
                    new MuiListItemProps {
                      override val dense = true
                      override val divider = true
                    }
                  )(),

                  // Переключалка-выключалка debug mode
                  onOffSettingR.component(
                    onOffSettingR.PropsVal(
                      text  = MsgCodes.`Debug` ,
                      onOff = Left( SetDebug ),
                      isCheckedProxy = p.zoom { mroot =>
                        DiodeUtil.Bool( mroot.internals.conf.debug )
                      },
                    )
                  ),

                  // Управление unsafe-offsets, и данные по экрану устройства.
                  p.wrap(_.dev.screen.info)( unsafeOffsetSettingR.component.apply ),

                  // Версия системы (та же, что и на левой панели меню внизу, но тут по-крупному):
                  MuiListItem()(
                    MuiListItemText(
                      new MuiListItemTextProps {
                        override val primary   = MsgCodes.`Version`
                        override val secondary = versionR.VERSION_STRING
                      }
                    )(),
                  ),

                )
              }
            },

          ),
        ),

        // Кнопки внизу диалога.
        platformCssP.consume { platformCss =>
          MuiDialogActions {
            platformComponents.diaActionsProps()(platformCss)
          } (
            // Кнопка закрытия диалога
            MuiButton(
              new MuiButtonProps {
                override val onClick = _onCloseDialog
                override val variant = MuiButtonVariants.text
                override val size    = MuiButtonSizes.large
              }
            )(
              crCtxProv.message( MsgCodes.`Close` ),
            ),
          )
        },

      )

      platformCssP.consume { platformCss =>
        val diaStyle = new MuiDialogClasses {
          override val paper = platformCss.Dialogs.paper.htmlClass
        }
        s.openedSomeC { openedSomeProxy =>
          MuiDialog(
            new MuiDialogProps {
              override val open = openedSomeProxy.value.value
              override val onClose = _onCloseDialog
              override val maxWidth = MuiDialogMaxWidths.xs
              override val fullWidth = true
              override val classes = diaStyle
            }
          )( diaChildren: _* )
        }
      }

    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(

        openedSomeC = propsProxy.connect { mroot =>
          OptionUtil.SomeBool( mroot.dialogs.settings.opened )
        },

        debugSomeC = propsProxy.connect { mroot =>
          OptionUtil.SomeBool( mroot.internals.isScDebugEnabled() )
        },

      )
    }
    .renderBackend[Backend]
    .build

}
