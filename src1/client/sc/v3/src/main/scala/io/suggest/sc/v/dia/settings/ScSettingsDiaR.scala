package io.suggest.sc.v.dia.settings

import com.materialui.{MuiButton, MuiButtonProps, MuiButtonSizes, MuiButtonVariants, MuiDialog, MuiDialogActions, MuiDialogClasses, MuiDialogContent, MuiDialogMaxWidths, MuiDialogProps, MuiList}
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.empty.OptionUtil
import io.suggest.dev.MOsFamily
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.lk.r.plat.{PlatformComponents, PlatformCssStatic}
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.sc.m.{MScRoot, SettingsDiaOpen}
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
                      platformComponents      : PlatformComponents,
                      crCtxProv               : React.Context[MCommonReactCtx],
                      platformCssP            : React.Context[PlatformCssStatic],
                    ) {

  type Props_t = MScRoot
  type Props = ModelProxy[Props_t]


  case class State(
                    openedSomeC       : ReactConnectProxy[Some[Boolean]],
                    btMissOnOsC       : ReactConnectProxy[Option[MOsFamily]],
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
            p.wrap(_.dev.platform)( blueToothUnAvailInfoR.component.apply ),

            // Переключалка уведомлений.
            p.wrap(_.dev.osNotify)( notificationSettingsR.component.apply ),

            // debug-функции не должны тормозить работу выдачи, если нет флага при запуске.
            s.debugSomeC { isDebugEnabled =>
              ReactCommonUtil.maybeEl( isDebugEnabled.value.value ) {
                p.wrap(_.dev.screen.info)( unsafeOffsetSettingR.component.apply )
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

        btMissOnOsC = propsProxy.connect { mroot =>
          val p = mroot.dev.platform
          OptionUtil.maybeOpt( p.hasBle )( p.osFamily )
        },

        debugSomeC = propsProxy.connect { mroot =>
          OptionUtil.SomeBool( mroot.internals.isScDebugEnabled() )
        },

      )
    }
    .renderBackend[Backend]
    .build

}
