package io.suggest.id.login.v.stuff

import chandu0101.scalajs.react.components.materialui.{MuiLinearProgress, MuiLinearProgressProps, MuiProgressVariants}
import diode.react.ModelProxy
import io.suggest.css.Css
import io.suggest.id.login.v.LoginFormCss
import io.suggest.sjs.common.empty.JsOptionUtil
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{React, ScalaComponent}
import scalacss.ScalaCssReact._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.03.19 22:29
  * Description: connect-компонент прогрессбара ожидания.
  */
class LoginProgressR(
                      loginFormCssCtx             : React.Context[LoginFormCss],
                    ) {

  type Props_t = Some[Boolean]
  type Props = ModelProxy[Props_t]

  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .render_P { propsProxy =>
      val isPending = propsProxy.value.value
      val classesSet = ^.classSet(
        Css.Display.VISIBLE   -> isPending,
        Css.Display.INVISIBLE -> !isPending,
      )
      val progress = MuiLinearProgress(
        new MuiLinearProgressProps {
          override val variant = {
            if (isPending) MuiProgressVariants.indeterminate
            else MuiProgressVariants.determinate
          }
          override val value = JsOptionUtil.maybeDefined( !isPending )(0)
        }
      )
      loginFormCssCtx.consume { loginFormCss =>
        <.div(
          // Сокрытие или отображение крутилки ожидания.
          loginFormCss.progressBar,
          classesSet,
          progress
        )
      }
    }
    .build

  def apply( isShownSomeProxy: Props ) = component( isShownSomeProxy )

}
