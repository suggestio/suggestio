package io.suggest.lk.r

import io.suggest.css.ScalaCssDefaults._
import io.suggest.css.Css
import io.suggest.lk.r.color.ColorBtnR
import scalacss.internal.mutable.StyleSheet

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.12.18 22:42
  * Description: ScalaCSS-стили для нужд lk-common.
  */

class LkCss extends StyleSheet.Inline {

  import dsl._

  object SlideBlock {

    import io.suggest.css.Css.Lk.SlideBlocks._


    val outer = style(
      addClassName( OUTER ),
      minWidth( 350.px )
    )

    val title = style(
      addClassName( TITLE )
    )

    val opened = style(
      addClassName( OPENED )
    )

    val titleBtn = style(
      addClassName( TITLE_BTN )
    )

    val bodyWrap = {
      val T = Css.Anim.Transition
      style(
        addClassName( Css.Lk.SlideBlocks.BODY ),
        height(0.px),
        background := "none",
        overflow.hidden,
        transition := T.all(0.2, T.TimingFuns.EASE_IN)
      )
    }

    val bodyWrapExpanded = style(
      height.auto,
      paddingBottom( 5.px )
    )

  }


  object RangeInput {

    val slider = style(
      verticalAlign.middle,
      width(150.px).important,
    )

    val textInput = style(
      width(60.px).important
    )

  }


  /** Стили для опционального цвета фона. */
  object ColorOptPicker {

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
        addClassNames( ColorBtnR.defaultCssClasses: _* ),
        width( whPx ),
        height( whPx ),
        verticalAlign.middle,
      )
    }

    val pickerCont = style(
      position.fixed
    )

  }


  initInnerObjects(
    SlideBlock.bodyWrap,
    RangeInput.slider,
    ColorOptPicker.colorRound,
  )

}

