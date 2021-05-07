package io.suggest.bill.cart.v.order

import io.suggest.css.ScalaCssDefaults._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.09.18 22:25
  * Description: Static CSS styles for Cart Form.
  */
class OrderCss extends StyleSheet.Inline {

  import dsl._

  /** Styles for order items table. */
  object ItemsTable {

    /** Ad preview or node logo column styles. */
    object NodePreviewColumn {

      /** Preview cell's header column style. */
      val head = style(
        width( 70.px ),
        padding( 0.px )
      )

      /** Preview cell body style. */
      val body = {
        val fivePx = 5.px
        style(
          padding(fivePx, fivePx),
          verticalAlign.middle,
          textAlign.center,
        )
      }

      /** Node logo image style. */
      val adnLogo = style(
        width( 75.px )
      )

    }


    /** Toolbar styles. */
    object ToolBar {

      /** Delimiter between left/right toolbar parts style. */
      val spacer = style(
        flex := "1 1 100%"
      )

      /** Toolbar container style. */
      val root = style(
        paddingRight( 32.px ).important,
        paddingLeft( 70.px ).important
      )

      /** Toolbar title container style. */
      val title = style(
        flex := "0 0 auto"
      )

    }


    /** Order item table body styles. */
    object TBody {

      val fullRowCell = style(
        textAlign.center
      )

    }

  }


  object PayBtn {

    /** Style for PAY button. */
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
