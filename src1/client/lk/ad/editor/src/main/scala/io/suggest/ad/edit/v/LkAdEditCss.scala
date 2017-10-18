package io.suggest.ad.edit.v

import io.suggest.css.Css
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

    val outer = style(
      display.tableCell
    )

    val contWidth  = _classNameStyle( "__width" )
    val contHeight = _classNameStyle( "__height" )

    val btnsContainer = _classNameStyle( _CSS_PREFIX )

    val label = _classNameStyle( _CSS_PREFIX + "_label" )

    val btn = _classNameStyle( _CSS_PREFIX + "_btn" )

    val increase = _classNameStyle( "__increase" )
    val decrease = _classNameStyle( "__decrease" )

  }


  object Layout {

    private def _PREFIX = "lk-ad-block-edit-form"

    val outerCont = _classNameStyle( _PREFIX )

    private def _PREVIEW_OUTER_CONT_PREFIX = _PREFIX + "__preview"
    val previewOuterCont = {
      val px5 = 5.px
      style(
        addClassNames( _PREVIEW_OUTER_CONT_PREFIX, Css.Overflow.HIDDEN ),
        width.auto,
        padding( px5, 30.px, px5, px5 )
      )
    }

    val previewInnerCont = _classNameStyle( _PREVIEW_OUTER_CONT_PREFIX + "_container" )

    val editorsCont = style(
      addClassName( _PREFIX + "__editor" ),
      maxHeight( 67.vh ),
      overflow.auto
    )

  }


  /** Стили для опционального цвета фона. */
  object BgColorOptPicker {

    val container = style(
      addClassName( Css.Display.BLOCK ),
      height( 50.px ),
      lineHeight( 33.px )
    )

    val label = style(
      addClassName( Css.CLICKABLE ),
      verticalAlign.top
    )

    val colorRound = {
      val whPx = 30.px
      style(
        addClassNames(Css.Lk.COLOR, Css.Display.INLINE_BLOCK),
        width( whPx ),
        height( whPx )
      )
    }

  }


  /** Стили для кропа. */
  object Crop {

    val popup = {
      style(
        maxWidth( 600.px ),
        overflow.visible
      )
    }

  }


  initInnerObjects(
    WhControls.contWidth,
    Layout.editorsCont,
    BgColorOptPicker.colorRound,
    Crop.popup
  )

}
