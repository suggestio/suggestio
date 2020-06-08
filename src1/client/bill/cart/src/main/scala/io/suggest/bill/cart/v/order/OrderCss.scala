package io.suggest.bill.cart.v.order

import io.suggest.css.ScalaCssDefaults._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.09.18 22:25
  * Description: Статические стили для компонента корзины.
  */
class OrderCss extends StyleSheet.Inline {

  import dsl._

  // Стили для таблицы item'ов.
  object ItemsTable {

    /** Колонка с превьюшкой карточки. */
    object NodePreviewColumn {

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

      /** Стиль для картинки логотипа adn-узла.*/
      val adnLogo = style(
        width( 75.px )
      )

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


    /** Стили, используемые для рендера тела таблицы: item'ов. */
    object TBody {

      val fullRowCell = style(
        textAlign.center
      )

    }

  }


  object PayBtn {

    /** Стили для кнопки перехода к оплате. */
    val root = style(
      float.right
    )

  }


  initInnerObjects(
    ItemsTable.NodePreviewColumn.body,
    ItemsTable.ToolBar.spacer,
    ItemsTable.TBody.fullRowCell,
    PayBtn.root,
  )

}
