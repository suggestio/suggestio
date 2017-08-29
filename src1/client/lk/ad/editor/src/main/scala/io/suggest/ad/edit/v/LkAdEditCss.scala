package io.suggest.ad.edit.v

import io.suggest.css.ScalaCssDefaults._
import scalacss.internal.mutable.StyleSheet

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.08.17 19:08
  * Description: CSS-стили для редактора карточек.
  */
class LkAdEditCss extends StyleSheet.Inline {

  import dsl._


  /** Стиль для поля редактора. */
  val editorFieldContainer = _classNameStyle( "edit-ad_block-field" )


  /** Короткий код для создания стилей-алиасов, указывающих на внешние стили. */
  private def _classNameStyle(cn: String) = {
    style(
      addClassName( cn )
    )
  }


  /** Стили элементов управление высотой/шириной блока. */
  object WhControls {

    private def _CSS_PREFIX = "block-height-editor"

    val contWidth  = _classNameStyle( "__width" )
    val contHeight = _classNameStyle( "__height" )

    val btnsContainer = _classNameStyle( _CSS_PREFIX )

    val label = _classNameStyle( _CSS_PREFIX + "_label" )

    val btn = _classNameStyle( _CSS_PREFIX + "_btn" )

    val increase = _classNameStyle( "__increase" )
    val decrease = _classNameStyle( "__decrease" )

  }

}
