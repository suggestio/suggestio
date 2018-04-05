package io.suggest.adn.edit.v

import io.suggest.css.Css
import scalacss.internal.mutable.StyleSheet
import io.suggest.css.ScalaCssDefaults._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.04.18 11:50
  * Description: Стили для формы редактирования ADN-узла.
  */
class LkAdnEditCss extends StyleSheet.Inline {

  import dsl._

  private val C = Css.Lk.Adn.Edit

  val logoBar = style(
    addClassName( C.Logo.LOGO_BAR )
  )

  val infoBar = style(
    addClassName( C.Info.INFO_BAR )
  )

}
