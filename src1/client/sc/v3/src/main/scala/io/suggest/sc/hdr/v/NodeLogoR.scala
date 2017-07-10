package io.suggest.sc.hdr.v

import diode.react.ModelProxy
import io.suggest.sc.m.MScNodeInfo
import japgolly.scalajs.react.BackendScope
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.react.ReactCommonUtil.Implicits._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.07.17 21:14
  * Description: Компонент логотипа узла.
  * В отличии от исходной концепции, здесь у нас узел может быть не совсем существующим:
  * например, просмотр карточек-предложений в каком-то теге.
  * Логотип узла как бы отражает эту сущность.
  */
object NodeLogoR {

  type Props = ModelProxy[Option[MScNodeInfo]]

  class Backend($: BackendScope[Props, _]) {

    def render(propsProxy: Props): VdomElement = {
      propsProxy().whenDefinedEl { nodeInfo =>

        // Нужно решить, что вообще надо рендерить: логотип, название узла или что-то иное?
        nodeInfo.logoOpt.fold {
          // Графический логотип не доступен. Попробовать отрендерить название узла.
          <.span(
            ???
          )

        } { logoInfo =>
          // Рендерим картинку графического логотипа узла.
          ???
        }

      }
    }

  }

}
