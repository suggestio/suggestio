package io.suggest.lk.u

import com.materialui.MuiStylesProvider
import japgolly.scalajs.react.vdom.VdomElement

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.01.2021 15:37
  * Description: Дополнительная sio-утиль для material-ui.
  */
object MaterialUiUtil {

  /** Какие-то дополнительные плюшки поверх ВСЕХ форм и выдачи.
    *
    * @param appComp Компонент верхнего уровня всей выдачи или целиковой react-формы.
    * @return
    */
  def postprocessTopLevel(appComp: VdomElement): VdomElement = {

    // MuiStylesProvider: На время миграции v4-v5 требуется явно-заданный StylesProvider:
    // https://github.com/mui-org/material-ui/blob/next/CHANGELOG.md#material-uicorev500-alpha17
    // By default, emotion injects its style after JSS, this breaks the computed styles.
    // In order to get the correct CSS injection order until all the components are migrated, you need to wrap the root of your application
    // TODO После material-ui v5 final release, скорее всего можно удалить явный StylesProvider отсюда.
    MuiStylesProvider.component(
      // Нет смысла делать эти пропертисы явно-статическими: этот код вызывается максимум один раз в любой форме (т.к. монтирование корня формы идёт через circuit.wrap() )
      new MuiStylesProvider.Props {
        override val injectFirst = true
      }
    )(
      appComp,
    )

  }

}
