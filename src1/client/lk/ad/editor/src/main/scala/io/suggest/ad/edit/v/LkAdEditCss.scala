package io.suggest.ad.edit.v

import io.suggest.css.Css
import io.suggest.css.ScalaCssDefaults._
import io.suggest.font.MFonts

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
    val previewOuterCont = _classNameStyle( _PREVIEW_OUTER_CONT_PREFIX )

    val previewInnerCont = _classNameStyle( _PREVIEW_OUTER_CONT_PREFIX + "_container" )

    val editorsCont = style(
      addClassName( _PREFIX + "__editor" ),
      position.fixed,
      minHeight( (MFonts.values.size * 30).px ),
      left( 400.px )
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


  initInnerObjects(
    WhControls.contWidth,
    Layout.editorsCont,
    BgColorOptPicker.colorRound
  )

}
