package io.suggest.dev

import com.materialui.{Mui, MuiMenuItem, MuiMenuItemClasses, MuiMenuItemProps, MuiTypoGraphy, MuiTypoGraphyClasses, MuiTypoGraphyProps}
import io.suggest.common.html.HtmlConstants
import japgolly.scalajs.react.vdom.html_<^._

import scala.scalajs.js
import scala.scalajs.js.JSConverters._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.02.2020 2:00
  */
class OsFamiliesR {

  /** Рендер списка ОСей. */
  def osFamiliesMenuItems(itemCss: js.UndefOr[MuiMenuItemClasses] = js.undefined,
                          textCss: js.UndefOr[String] = js.undefined): List[VdomElement] = {
    MOsFamilies
      .values
      .iterator
      .map { osPlatform =>
        MuiMenuItem.component
          .withKey(osPlatform.value)(
            new MuiMenuItemProps {
              override val value = osPlatform.value
              override val classes = itemCss
            }
          )(
            // Иконка
            osPlatform match {
              case MOsFamilies.Android   => Mui.SvgIcons.Android()()
              case MOsFamilies.Apple_iOS => Mui.SvgIcons.Apple()()
              case _ => EmptyVdom   // TODO отступ? пустая иконка или ...?
            },

            HtmlConstants.NBSP_STR,
            HtmlConstants.NBSP_STR,

            MuiTypoGraphy(
              new MuiTypoGraphyProps {
                override val classes = new MuiTypoGraphyClasses {
                  override val root = textCss
                }
              }
            )(
              osPlatform.value,
            )

          ): VdomElement
      }
      .toList
  }

}
