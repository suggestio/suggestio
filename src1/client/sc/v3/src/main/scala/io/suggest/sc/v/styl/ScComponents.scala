package io.suggest.sc.v.styl

import com.materialui.{Mui, MuiDialogActions, MuiDialogActionsClasses, MuiDialogActionsProps, MuiDialogTitle, MuiDialogTitleClasses, MuiDialogTitleProps, MuiSwitch, MuiTypoGraphy, MuiTypoGraphyClasses, MuiTypoGraphyProps, MuiTypoGraphyVariants}
import com.mui.treasury.styles.switch
import io.suggest.css.Css
import japgolly.univeq._
import io.suggest.dev.MPlatformS
import io.suggest.sc.m.MScReactCtx
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.07.2020 18:52
  * Description: Набор компонентов для
  */
class ScComponents(
                    mPlatform       : () => MPlatformS,
                    scReactCtxP     : React.Context[MScReactCtx],
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


  /** Компонент заголовка диалогового окна под iOS и остальные платформы.
    * Заголовки окон на iOS выделены жирным и выравниваются по центру.
    */
  lazy val diaTitle = {
    ScalaComponent
      .builder[List[String]]( "ScDiaTitle" )
      .stateless
      .render_PC { (diaTitleClasses, children) =>
        scReactCtxP.consume { scReactCtx =>
          MuiDialogTitle {
            val diaTitleCss = new MuiDialogTitleClasses {
              override val root = Css.flat1(
                scReactCtx.scCssSemiStatic.Dialogs.title.htmlClass :: diaTitleClasses
              )
            }
            new MuiDialogTitleProps {
              override val classes = diaTitleCss
              override val disableTypography = true
            }
          } (
            MuiTypoGraphy {
              val mtgCss = new MuiTypoGraphyClasses {
                override val root = scReactCtx.scCssSemiStatic.Dialogs.title.htmlClass
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
      }
      .build
  }


  def diaActionsProps(classes: List[String] = Nil)(scReactCtx: MScReactCtx): MuiDialogActionsProps = {
    val diaActionsCss = new MuiDialogActionsClasses {
      override val root = Css.flat1(
        scReactCtx.scCssSemiStatic.Dialogs.actions.htmlClass :: classes
      )
    }
    new MuiDialogActionsProps {
      override val classes = diaActionsCss
    }
  }

}
