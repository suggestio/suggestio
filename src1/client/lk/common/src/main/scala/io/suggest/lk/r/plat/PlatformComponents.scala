package io.suggest.lk.r.plat

import com.materialui.{Mui, MuiDialogActionsClasses, MuiDialogActionsProps, MuiDialogTitle, MuiDialogTitleClasses, MuiDialogTitleProps, MuiSwitch, MuiTypoGraphy, MuiTypoGraphyClasses, MuiTypoGraphyProps, MuiTypoGraphyVariants}
import com.mui.treasury.styles.switch
import io.suggest.css.Css
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.07.2020 18:52
  * Description: Набор react-компонентов и утили, реализующих платформо-зависимый дизайн.
  */
class PlatformComponents(
                          getPlatformCss       : () => PlatformCssStatic,
                        ) {

  /** Компонент для MuiSwitch.
    * Если Apple, то надо использовать маковский дизайн.
    */
  lazy val muiSwitch = {
    if (getPlatformCss().isRenderIos) {
      MuiSwitch.mkComponent {
        Mui.Styles.withStylesF( switch.Ios.iosSwitchStyles )( Mui.Switch )
      }
    } else {
      MuiSwitch.component
    }
  }


  /** Компонент заголовка диалогового окна под iOS и остальные платформы.
    * Заголовки окон на iOS выделены жирным и выравниваются по центру.
    */
  lazy val diaTitle = ScalaComponent
    .builder[List[String]]( "ScDiaTitle" )
    .stateless
    .render_PC { (diaTitleClasses, children) =>
      val platCss = getPlatformCss()

      MuiDialogTitle {
        val diaTitleCss = new MuiDialogTitleClasses {
          override val root = Css.flat1(
            platCss.Dialogs.title.htmlClass :: diaTitleClasses
          )
        }
        new MuiDialogTitleProps {
          override val classes = diaTitleCss
          override val disableTypography = true
        }
      } (
        MuiTypoGraphy {
          val mtgCss = new MuiTypoGraphyClasses {
            override val root = platCss.Dialogs.title.htmlClass
          }
          new MuiTypoGraphyProps {
            override val variant = MuiTypoGraphyVariants.h6
            override val classes = mtgCss
          }
        } (
          children
        )
      )
    }
    .build


  /** Пропертисы для MuiDialogActions(), чтобы на iOS было выравнивание элементов по ширине.
    *
    * @param classes Опциональный список css-классов.
    * @param platformCss Стили.
    * @return JSON-пропертисы.
    */
  def diaActionsProps(classes: List[String] = Nil)(platformCss: PlatformCssStatic): MuiDialogActionsProps = {
    val diaActionsCss = new MuiDialogActionsClasses {
      override val root = Css.flat1(
        platformCss.Dialogs.actions.htmlClass :: classes
      )
    }
    new MuiDialogActionsProps {
      override val classes = diaActionsCss
    }
  }

}
