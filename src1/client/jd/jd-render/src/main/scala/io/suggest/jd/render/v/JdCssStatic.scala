package io.suggest.jd.render.v

import io.suggest.css.ScalaCssDefaults._
import io.suggest.css.{Css, ScalaCssUtil}
import io.suggest.font.MFonts
import io.suggest.text.MTextAligns
import scalacss.internal.mutable.StyleSheet

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.10.2019 12:06
  * Description:
  */

/** Статические стили JdCss, которые не изменяются во время работы. */
class JdCssStatic extends StyleSheet.Inline {

  import dsl._


  /** Текущий выбранный тег выделяется на картинке. */
  val selectedTag = {
    style(
      outline.dashed,
      zIndex(10)
    )
  }


  /** Поддержка горизонтального ресайза. */
  val horizResizable = style(
    // TODO Хотелось с &.hover(...), но в хроме полный ахтунг и погибель
    resize.horizontal,
  )

  val hvResizable = style(
    resize.both
  )

  val wideBlockStyle = {
    val zeroPx = 0.px
    style(
      // TODO Переместить top в smBlock?
      top(zeroPx)
      //left(zeroPx)
    )
  }

  /** Общий стиль для всех AbsPos-тегов. */
  val absPosStyleAll = style(
    position.absolute,
    zIndex(5)
  )

  /** Для qd-blockless используется position:relative, чтобы текст под углом не разъезжался сильно. */
  val qdBl = style(
    position.relative,
  )

  // Для строк и групп строк стили более точечные.
  // -------------------------------------------------------------------------------
  // text-align

  /** styleF допустимых выравниваний текста. */
  val textAlignsStyleF = {
    val taAttr = textAlign
    styleF(
      new Domain.OverSeq( MTextAligns.values )
    )(
      { align =>
        val av = align match {
          case MTextAligns.Left    => taAttr.left
          case MTextAligns.Center  => taAttr.center
          case MTextAligns.Right   => taAttr.right
          case MTextAligns.Justify => taAttr.justify
        }
        styleS( av )
      },
      ScalaCssUtil.valueEnumEntryDomainNameF
    )
  }

  // -------------------------------------------------------------------------------
  // text indents.

  /** Стили сдвигов выравниваний. */
  val indentStyleF = {
    // quill допускает сдвиги от 1 до 8 включительно.
    styleF.int(1 to 9) { indentLevel =>
      styleS(
        paddingLeft( (indentLevel * 3).em )
      )
    }
  }


  /** Статические стили для контента. */
  val contentOuterS = style(
    // Дефолтовые настройки шрифтов внутри блока:
    fontFamily.attr := Css.quoted( MFonts.default.cssFontFamily ),
    color.black
  )

  /** Статический стиль для блока. Надо не забывать про contentOuterS. */
  val smBlockS = style(
    addClassName( contentOuterS.htmlClass ),
    // Без addClassName("sm-block"), т.к. это ненужные transition и уже неактуальные стили (кроме overflow:hidden).
    overflow.hidden,
  )

}
