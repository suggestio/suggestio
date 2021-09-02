package io.suggest.lk.u

import com.materialui.{Mui, MuiStyledEngineProvider, MuiStylesProvider, MuiThemeProvider}
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.01.2021 15:37
  * Description: Дополнительная sio-утиль для material-ui.
  */
object MaterialUiUtil {

  lazy val defaultTheme = Mui.Styles.createTheme()

  def defaultThemeProvider(children: VdomNode*) = {
    MuiThemeProvider.component(
      new MuiThemeProvider.Props {
        override val theme = defaultTheme
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
    defaultThemeProvider(
      postprocessTopLevelOnlyStyles( appComp ),
    )
  }


  def postprocessTopLevelOnlyStyles(appComp: VdomElement): VdomElement = {
    // (emoution mui-v5) - это Mui.StyledEngineProvider. Требуется рендерить по-выше в head, чтобы старые стили оказались ниже и имели приоритет над emotion.
    /*MuiStyledEngineProvider.component(
      new MuiStyledEngineProvider.Props {
        override val injectFirst = true
      }
    )(

      // (JSS mui-v4) - StylesProvider: На время миграции v4-v5 требуется ручное управление StylesProvider,
      // чтобы старые стили имели приоритет над emoution.
      // https://github.com/mui-org/material-ui/pull/24693 - [Switch] migrate to emoution
      MuiStylesProvider.component(
        // Нет смысла делать эти пропертисы явно-статическими: этот код вызывается максимум один раз в любой форме (т.к. монтирование корня формы идёт через circuit.wrap() )
        new MuiStylesProvider.Props {
          override val injectFirst = false
        }
      )(*/
        appComp
      //),

    //)
  }

}
