package io.suggest.n2.edge.edit.v

import scalacss.internal.mutable.StyleSheet
import io.suggest.css.ScalaCssDefaults._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.01.2020 22:42
  * Description: Стили для редактора эджей.
  */
object EdgeEditCss extends StyleSheet.Inline {

  import dsl._

  val input = style(
    margin( 16.px ),
  )

}
