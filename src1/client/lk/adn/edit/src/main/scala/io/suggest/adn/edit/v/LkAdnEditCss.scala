package io.suggest.adn.edit.v

import io.suggest.adn.edit.NodeEditConstants
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

  val colorTdVerticalHr = style(
    addClassNames( Css.Lk.HrDelim.DELIMITER, Css.Lk.HrDelim.LIGHT ),
    height( 43.px ),
    width( 1.px )
  )

  val colorPicker = style(
    position.fixed
  )

  /** Если не ограничивать кроп-попап по ширине, то он займёт размер картинки. */
  val galImgCropPopup = style(
    maxWidth( NodeEditConstants.Gallery.WIDTH_PX.px ),
    overflow.visible
  )

}
