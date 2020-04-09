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
class LkAdEditCss
  extends StyleSheet.Inline
{

  import dsl._

  /** Короткий код для создания стилей-алиасов, указывающих на внешние стили. */
  private def _classNameStyle(cn: String) = {
    style(
      addClassName( cn )
    )
  }


  object Title {

    val titleInp = style(
      width( 700.px ),
      paddingBottom( 20.px ),
    )

  }


  /** Стили элементов управление высотой/шириной блока. */
  object WhControls {

    val slider = style(
      width( 93.%% ),
      marginBottom( 10.px ),
    )

    val marginLeft0 = style(
      marginLeft( 0.px ).important
    )

    val marginRight40 = style(
      marginRight( 40.px ).important
    )

  }


  object Layout {

    private def _PREFIX = "lk-ad-block-edit-form"

    val outerCont = style(
      addClassName( _PREFIX ),
      display.tableRow
    )

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
      overflow.hidden,
      transition := {
        val t = Css.Anim.Transition
        t.all(0.1, t.TimingFuns.EASE_OUT)
      },
      // Без min-height, выпадающие списки шрифтов и размеров рендерятся обрезанными снизу:
      minHeight( 500.px )
    )

    val addCont = style(
      padding( 10.px ),
    )

    val fabIcon = style(
      margin( 2.px )
    )

    val scaleInputCont = style(
      addClassNames( Css.Input.INPUT, "ad-ed_scale-cont" ),
      maxWidth( 300.px )
    )

  }


  object TextShadow {
    val cont = style(
      height( 70.px ),
    )

    val first = style(
      marginTop( -30.px ).important,
    )
    val second = style(
      marginTop( 0.px ),
    )
    val third = style(
      marginTop( 30.px ),
    )
  }


  /** Стили для кропа. */
  object Crop {

    val popup = style(
      maxWidth( 600.px ),
      overflow.visible
    )

  }

  /** Стили для элементов управления главным/заглавным блоком. */
  object StripMain {

    /** Стиль неактивной ссылки "показать все". */
    val showAll = style(
      textDecorationLine.underline,
      textDecorationStyle.dashed
    )

  }


  initInnerObjects(
    Title.titleInp,
    WhControls.marginRight40,
    Layout.editorsCont,
    TextShadow.second,
    Crop.popup,
    StripMain.showAll,
  )

}
