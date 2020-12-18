package io.suggest.lk.tags.edit.r

import com.materialui.{MuiChip, MuiChipProps, MuiChipVariants}
import diode.react.ModelProxy
import io.suggest.lk.tags.edit.m.RmTag
import io.suggest.react.ReactCommonUtil
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.12.16 21:16
  * Description: Компонент списка текущих, добавленных в заказ, тегов.
  */
final class TagsExistsR {

  type Props = ModelProxy[Set[String]]

  class Backend($: BackendScope[Props, Unit]) {

    private def _onTagDeleteCb(tagName: String) = {
      ReactCommonUtil.cbFun1ToJsCb { _: ReactUIEventFromHtml =>
        dispatchOnProxyScopeCB($, RmTag(tagName))
      }
    }


    def render(tagsExists: Props): VdomElement = {
      <.div(
        tagsExists().toVdomArray { tagName =>
          MuiChip(
            new MuiChipProps {
              // TODO variant=outlined для уже установленных тегов на текущий момент.
              override val variant    = MuiChipVariants.default
              override val onDelete   = _onTagDeleteCb( tagName )
              override val label      = tagName
              // icon тега не указываем, чтобы визуально разгрузить отображение одинаковыми картинками.
            }
          )
        }
      )
    }

  }

  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

}
