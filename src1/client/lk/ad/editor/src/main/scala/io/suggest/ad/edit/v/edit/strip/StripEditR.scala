package io.suggest.ad.edit.v.edit.strip

import diode.react.ModelProxy
import io.suggest.jd.tags.Strip
import japgolly.scalajs.react.BackendScope
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.react.ReactCommonUtil.Implicits._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.08.17 18:49
  * Description: React-компонент редактирования strip-а.
  */
class StripEditR {

  /** Алиас сложного типа для пропертисов. */
  type Props = ModelProxy[Option[Strip]]

  class Backend($: BackendScope[Props, Unit]) {

    def render(p: Props): VdomElement = {
      p().whenDefinedEl { strip =>
        <.div(

          // TODO Кнопки управление шириной и высотой блока.

          // TODO Загрузка фоновой картинки и выбора цвета фона.

        )
      }
    }

  }

}
