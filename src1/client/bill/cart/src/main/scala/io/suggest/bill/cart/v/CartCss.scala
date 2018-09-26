package io.suggest.bill.cart.v

import io.suggest.css.ScalaCssDefaults._
import scalacss.internal.mutable.StyleSheet

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.09.18 22:25
  * Description: Статические стили для компонента корзины.
  */
class CartCss extends StyleSheet.Inline {

  import dsl._

  // Стили для таблицы item'ов.
  object ItemsTable {

    /** Колонка с превьюшкой карточки. */
    object AdPreviewColumn {

      /** Стиль для ячейки заголовка колонки. */
      val head = style(
        width( 70.px ),
        padding( 0.px )
      )

      /** Стиль для ячейки контента колонки. */
      val body = {
        val fivePx = 5.px
        style(
          padding(fivePx, fivePx),
          verticalAlign.middle,
          textAlign.center,
        )
      }

    }


    // Стили для тул-бара над таблицей.
    object ToolBar {

      /** Распорка между левым и правым элементами. */
      val spacer = style(
        flex := "1 1 100%"
      )

      /** Контейнер элементов тулбара. */
      val root = style(
        paddingRight( 32.px ).important,
        paddingLeft( 70.px ).important
      )

      /** Контейнер заголовка на тулбаре. */
      val title = style(
        flex := "0 0 auto"
      )

    }

  }


  initInnerObjects(
    ItemsTable.AdPreviewColumn.body,
    ItemsTable.ToolBar.spacer
  )

}
