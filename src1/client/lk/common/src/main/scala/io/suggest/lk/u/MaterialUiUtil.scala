package io.suggest.lk.u

import com.materialui.{Mui, MuiTheme, MuiThemeProvider}
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.01.2021 15:37
  * Description: Дополнительная sio-утиль для material-ui.
  */
object MaterialUiUtil {

  /** Default material-ui theme instance. */
  lazy val defaultTheme = Mui.Styles.createTheme()


  /** Wrap children-VDOM into theme provider. */
  def provideTheme(muiTheme: MuiTheme = defaultTheme)(children: VdomNode*) = {
    MuiThemeProvider.component(
      new MuiThemeProvider.Props {
        override val theme = muiTheme
      }
    )(children: _*)
  }


  /** Какие-то дополнительные плюшки поверх ВСЕХ форм и выдачи.
    *
    * @param appComp Компонент верхнего уровня всей выдачи или целиковой react-формы.
    * @return
    */
  def postprocessTopLevel(appComp: VdomElement): VdomElement = {
    // Add default theme:
    provideTheme()(
      postprocessTopLevelOnlyStyles( appComp ),
    )
  }


  /** Additional top-level wrap components may be defined here.
    * During v4 => v5 migration, there was some stuff for JSS/StyledComponents contexts.
    */
  def postprocessTopLevelOnlyStyles(appComp: VdomElement): VdomElement = {
    // 2021-09-03 Nothing to do here, by now.
    appComp
  }

}
