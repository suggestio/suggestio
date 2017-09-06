package io.suggest.jd.render.v

import io.suggest.css.Css

import scalacss.DevDefaults._
import io.suggest.jd.render.m.MJdCssArgs
import io.suggest.jd.tags.qd.QdTag
import io.suggest.jd.tags.{AbsPos, IDocTag, Strip}
import io.suggest.model.n2.node.meta.colors.MColorData
import io.suggest.primo.ISetUnset

import scala.reflect.ClassTag
import scalacss.internal.CssEntry.FontFace
import scalacss.internal.DslBase.ToStyle
import scalacss.internal.ValueT.TypedAttr_Color
import scalacss.internal.mutable.StyleSheet

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.08.17 15:40
  * Description: Динамические CSS-стили для рендера блоков плитки.
  *
  * Таблица стилей плоская для всех документов сразу.
  */

class JdCss( jdCssArgs: MJdCssArgs )
  extends StyleSheet.Inline {

  import dsl._

  // TODO Вынести статические стили в object ScCss?
  /** Все блоки помечаются этим классом. */
  val smBlock = style(
    addClassName("sm-block")
  )

  /** Ширина и длина -- 100%. */
  val wh100 = {
    val pc100 = 100.%%
    style(
      width(pc100),
      height(pc100)
    )
  }

  /** Текущий выбранный тег выделяется на картинке. */
  val selectedTag = style(
    outline.dashed,
    zIndex(10)
  )


  /** Итератор тегов указанного типа (класса) со всех уровней. */
  private def _jdTagsIter[T <: IDocTag : ClassTag]: Iterator[T] = {
    jdCssArgs
      .templates
      .iterator
      .flatMap(_.deepChildrenOfTypeIter[T])
  }

  /** Сборка домена для всех указанных тегов из всех документов. */
  private def _mkJdTagDomain[T <: IDocTag : ClassTag]: Domain[T] = {
    val absPoses = _jdTagsIter[T]
      .toIndexedSeq
    new Domain.OverSeq( absPoses )
  }



  // -------------------------------------------------------------------------------
  // Strip
  private val _stripsDomain = _mkJdTagDomain[Strip]

  /** Стили контейнеров полосок. */
  val stripOuterStyleF = styleF(_stripsDomain) { strip =>
    // Стиль фона полосы
    val stylBg = strip.bgColor.fold(StyleS.empty) { mcd =>
      styleS(
        backgroundColor(Color(mcd.hexCode))
      )
    }

    // Стиль размеров блока-полосы.
    val stylWh = strip.bm.fold(StyleS.empty) { bm =>
      styleS(
        width( bm.width.px ),
        height( bm.height.px )
      )
    }

    // Объединить все стили одного стрипа.
    stylWh.compose( stylBg )
  }



  // -------------------------------------------------------------------------------
  // AbsPos

  private val _absPosDomain = _mkJdTagDomain[AbsPos]

  /** Общий стиль для всех AbsPos-тегов. */
  val absPosStyleAll = style(
    position.absolute,
    zIndex(5)
  )

  /** Стили для элементов, отпозиционированных абсолютно. */
  val absPosStyleF = styleF(_absPosDomain) { absPos =>
    styleS(
      top( absPos.topLeft.y.px ),
      left( absPos.topLeft.x.px )
    )
  }


  // -------------------------------------------------------------------------------
  // fonts

  /** Домен стилей для текстов. */
  private val _textStylesDomain = {
    val textAttrsSeq = _jdTagsIter[QdTag]
      .flatMap(_.ops)
      .flatMap(_.attrsText.iterator)
      .filter(_.isCssStyled)
      .toIndexedSeq

    new Domain.OverSeq( textAttrsSeq )
  }

  /** styleF для стилей текстов. */
  val textStyleF = {
    // Получаем на руки инстансы, чтобы по-быстрее использовать их в цикле и обойтись без lazy call-by-name cssAttr в __applyToColor().
    val _colorAttr = color
    val _bgColorAttr = backgroundColor
    val _fontFamilyAttr = fontFamily.attr
    val _fontSizeAttr = fontSize
    val _lineHeightAttr = lineHeight

    styleF( _textStylesDomain ) { attrsText =>
      var acc = List.empty[ToStyle]

      // Отрендерить аттрибут одного цвета.
      // cssAttr не всегда обязателен, но его обязательная передача компенсируется через _colorAttr и _bgColorAttr.
      def __applyToColor(cssAttr: TypedAttr_Color,  mcdSuOpt: Option[ISetUnset[MColorData]]): Unit = {
        for (colorSU <- mcdSuOpt; color <- colorSU)
          acc ::= cssAttr( Color(color.hexCode) )
      }

      __applyToColor( _colorAttr, attrsText.color )
      __applyToColor( _bgColorAttr, attrsText.background )

      // Если задан font, то нужно отрендерить font-family:
      for (fontSU <- attrsText.font; font <- fontSU)
        acc ::= ( _fontFamilyAttr := Css.quoted(font.cssFontFamily) )

      // Если задан font-size, то нужно отрендерить его вместе с сопуствующими аттрибутами.
      for (fontSizeSU <- attrsText.size; fontSize <- fontSizeSU) {
        // Рендер размера шрифта
        acc ::= _lineHeightAttr( fontSize.lineHeight.px )
        // Отрендерить размер шрифта
        acc ::= _fontSizeAttr( fontSize.value.px )
      }

      // Вернуть скомпонованный стиль.
      styleS(
        acc: _*
      )
    }
  }

}


import com.softwaremill.macwire._

/** DI-factory для сборки инстансов [[JdCss]]. */
class JdCssFactory {

  def mkJdCss( jdCssArgs: MJdCssArgs ): JdCss = {
    wire[JdCss]
  }

}
