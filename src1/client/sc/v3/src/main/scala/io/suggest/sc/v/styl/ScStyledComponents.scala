package io.suggest.sc.v.styl

import com.materialui.{Mui, MuiSwitch}
import com.mui.treasury.styles.switch
import japgolly.univeq._
import io.suggest.dev.MPlatformS

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.07.2020 18:52
  * Description: Набор компонентов для
  */
class ScStyledComponents(
                          mPlatform: () => MPlatformS,
                        ) {

  /** Компонент для MuiSwitch.
    * Если Apple, то надо использовать маковский дизайн.
    */
  lazy val muiSwitch = {
    if (mPlatform().isUseIosStyles) {
      MuiSwitch.mkComponent {
        Mui.Styles.withStylesF( switch.Ios.iosSwitchStyles )( Mui.Switch )
      }
    } else {
      MuiSwitch.component
    }
  }

}
