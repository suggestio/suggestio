package io.suggest.dev

import com.materialui.{Mui, MuiMenuItem, MuiMenuItemProps}
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.02.2020 2:00
  */
class OsFamiliesR {

  /** Рендер списка ОСей. */
  def osFamiliesMenuItems: List[VdomElement] = {
    MOsFamilies
      .values
      .iterator
      .map { osPlatform =>
        MuiMenuItem.component
          .withKey(osPlatform.value)(
            new MuiMenuItemProps {
              override val value = osPlatform.value
            }
          )(
            // Иконка
            osPlatform match {
              case MOsFamilies.Android   => Mui.SvgIcons.Android()()
              case MOsFamilies.Apple_iOS => Mui.SvgIcons.Apple()()
              case _ => EmptyVdom   // TODO отступ? пустая иконка или ...?
            },
            osPlatform.value,
          ): VdomElement
      }
      .toList
  }

}
