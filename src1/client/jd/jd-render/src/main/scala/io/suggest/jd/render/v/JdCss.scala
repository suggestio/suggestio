package io.suggest.jd.render.v

import io.suggest.color.MColorData
import io.suggest.css.Css
import io.suggest.css.ScalaCssDefaults._
import io.suggest.css.ScalaCssUtil.Implicits._
import io.suggest.font.{MFontSizes, MFonts}
import io.suggest.jd.render.m.{MEmuCropCssArgs, MJdCssArgs}
import io.suggest.jd.tags.IDocTag
import io.suggest.jd.tags.qd.MQdOp
import io.suggest.primo.ISetUnset
import io.suggest.text.MTextAligns
import japgolly.univeq._

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

object JdCss {
  implicit def univEq: UnivEq[JdCss] = UnivEq.derive
}

case class JdCss( jdCssArgs: MJdCssArgs ) extends StyleSheet.Inline {

  import dsl._

  val szMultD = jdCssArgs.conf.szMult.toDouble

  // TODO Вынести статические стили в object ScCss?
  /** Все блоки помечаются этим классом. */
  val smBlock = style(
    addClassName("sm-block"),
    // Дефолтовые настройки шрифтов.
    fontFamily.attr := Css.quoted( MFonts.default.cssFontFamily ),
    fontSize( (MFontSizes.default.value * szMultD).px )
  )


  /** Текущий выбранный тег выделяется на картинке. */
  val selectedTag = style(
    outline.dashed,
    zIndex(10)
  )

  private def _allJdTagsIter: Iterator[IDocTag] = {
    jdCssArgs
      .templates
      .iterator
      .flatMap( _.flatten )
  }


  // -------------------------------------------------------------------------------
  // Strip

  /** Стили контейнеров полосок, описываемых через props1.BlockMeta. */
  val bmStyleF = {
    val strips = _allJdTagsIter
      .filter(_.props1.bm.nonEmpty)
      .toIndexedSeq

    val stripsDomain = new Domain.OverSeq( strips )
    //println( _allJdTagsIter.mkString(", ") )

    styleF(stripsDomain) { strip =>
      // Стиль размеров блока-полосы.
      strip.props1.bm.whenDefinedStyleS { bm =>
        styleS(
          width( (bm.width * szMultD).px ),
          height( (bm.height * szMultD).px )
        )
      }
    }
  }


  /** Цвет фона бывает у разнотипных тегов, поэтому выносим CSS для цветов фона в отдельный каталог стилей. */
  val bgColorOptStyleF = {
    val bgColorsHex1 = _allJdTagsIter
      .flatMap(_.props1.bgColor)
      .map(_.hexCode)
      .toSet
      .toIndexedSeq
    val bgColorsDomain = new Domain.OverSeq( bgColorsHex1 )
    styleF( bgColorsDomain ) { bgColorHex =>
      styleS(
        backgroundColor( Color(bgColorHex) )
      )
    }
  }


  // -------------------------------------------------------------------------------
  // AbsPos

  /** Общий стиль для всех AbsPos-тегов. */
  val absPosStyleAll = style(
    position.absolute,
    zIndex(5)
  )

  /** Стили для элементов, отпозиционированных абсолютно. */
  val absPosStyleF = {
    val absPosDomain = {
      val tags = _allJdTagsIter
        .filter(_.props1.topLeft.nonEmpty)
        .toIndexedSeq

      new Domain.OverSeq(tags)
    }
    styleF(absPosDomain) { jdt =>
      jdt.props1.topLeft.whenDefinedStyleS { topLeft =>
        styleS(
          top( (topLeft.y * szMultD).px ),
          left( (topLeft.x * szMultD).px )
        )
      }
    }
  }


  // -------------------------------------------------------------------------------
  // fonts

  private def _qdOpsIter: Iterator[MQdOp] = {
    _allJdTagsIter
      .flatMap(_.qdProps)
  }


  /** styleF для стилей текстов. */
  val textStyleF = {
    // Домен стилей для текстов.
    val _textStylesDomain = {
      val textAttrsSeq = _qdOpsIter
        .flatMap(_.attrsText.iterator)
        .filter(_.isCssStyled)
        .toIndexedSeq
      new Domain.OverSeq( textAttrsSeq )
    }

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
      for (fontSU <- attrsText.font; font <- fontSU) {
        val av = _fontFamilyAttr := Css.quoted( font.cssFontFamily )
        acc ::= av
      }

      // Если задан font-size, то нужно отрендерить его вместе с сопуствующими аттрибутами.
      for (fontSizeSU <- attrsText.size; fontSize <- fontSizeSU) {
        // Рендер размера шрифта
        acc ::= _lineHeightAttr( (fontSize.lineHeight * szMultD).px )
        // Отрендерить размер шрифта
        acc ::= _fontSizeAttr( (fontSize.value * szMultD).px )
      }

      // Вернуть скомпонованный стиль.
      styleS(
        acc: _*
      )
    }
  }


  // -------------------------------------------------------------------------------
  // -------------------------------------------------------------------------------
  // Для строк и групп строк стили более точечные.

  // TODO Opt Унести стили ниже в статические, рендерящиеся однократно. Их рендер не зависит ни от чего, они рендерятся всегда одинаково.

  // -------------------------------------------------------------------------------
  // text-align

  /** styleF допустимых выравниваний текста. */
  val textAlignsStyleF = {
     // Домен допустимых выравниваний текста по горизонтали.
    val textAlignsDomain = new Domain.OverSeq( MTextAligns.values )

    styleF( textAlignsDomain ) { align =>
      val taAttr = textAlign
      val av = align match {
        case MTextAligns.Left    => taAttr.left
        case MTextAligns.Center  => taAttr.center
        case MTextAligns.Right   => taAttr.right
        case MTextAligns.Justify => taAttr.justify
      }
      styleS(
        av
      )
    }
  }


  // -------------------------------------------------------------------------------
  // text indents.

  /** Стили сдвигов выравниваний. */
  val indentStyleF = {
    // quill допускает сдвиги от 1 до 8 включительно.
    val indentLevelsDomain = Domain.ofRange(1 to 8)
    styleF( indentLevelsDomain ) { indentLevel =>
      styleS(
        paddingLeft( (indentLevel * 3).em )
      )
    }
  }


  // -------------------------------------------------------------------------------
  // text indents.

  val embedAttrStyleF = {
    val embedAttrsSeq = _qdOpsIter
      .flatMap( _.attrsEmbed )
      .filter( _.nonEmpty )
      .toIndexedSeq

    val embedAttrsDomain = new Domain.OverSeq( embedAttrsSeq )

    styleF( embedAttrsDomain ) { embedAttrs =>
      var acc = List.empty[ToStyle]

      for (heightSU <- embedAttrs.height; heightPx <- heightSU )
        acc ::= height( (heightPx * szMultD).px )
      for (widthSU <- embedAttrs.width; widthPx <- widthSU)
        acc ::= width( (widthPx * szMultD).px )

      styleS(
        acc: _*
      )
    }
  }


  // -------------------------------------------------------------------------------
  // images + crop.

  // Делаем имитацию кропа прямо на экране, без участия сервера, с помощью css
  // https://stackoverflow.com/a/493329

  // Закомменчено пока: стили для контейнера не требуются, поэтому пропускаем из мимо ушей.
  /*
  val imgCropContainerF = {
    // Сборка области допустимых значений кропов.
    val imgCropsDomain = {
      val crops = _allJdTagsIter
        .flatMap { s =>
          s.props1
            .bgImg
            .flatMap(_.crop)
        }
        .toIndexedSeq
      new Domain.OverSeq( crops )
    }
    // Функция стиля.
    styleF( imgCropsDomain ) { mcrop =>
      styleS(
        width( mcrop.width.px ),
        height( mcrop.height.px ),
        overflow.hidden
      )
    }
  }
  */

  /** Стили для эмуляции кропа на фоновом изображении блока. */
  val blkBgImgCropEmuF = {
    val emuCrops = {
      val cropsIter = for {
        jdt       <- _allJdTagsIter
        bm        <- jdt.props1.bm
        bgImg     <- jdt.props1.bgImg
        mcrop     <- bgImg.crop
        e         <- jdCssArgs.edges.get( bgImg.imgEdge.edgeUid )
        fileJs    <- e.fileJs
        origWh    <- fileJs.whPx
      } yield {
        MEmuCropCssArgs(mcrop, origWh, bm)
      }
      cropsIter.toIndexedSeq
    }

    val cropsDomain = new Domain.OverSeq( emuCrops )

    styleF(cropsDomain) { ecArgs =>
      // Нужно рассчитать параметры margin, w, h изображения, чтобы оно имитировало заданный кроп.
      // margin: -20px 0px 0px -16px; -- сдвиг вверх и влево.
      // Для этого надо вписать размеры кропа в размеры блока

      // Вычисляем отношение стороны кропа к стороне блока. Считаем, что обе стороны соотносятся одинаково.
      val outer2cropRatio = ecArgs.outerWh.height.toDouble / ecArgs.crop.height.toDouble

      // Проецируем это отношение на натуральные размеры картинки, top и left:
      styleS(
        width     ( (ecArgs.origWh.width  * outer2cropRatio * szMultD).px ),
        height    ( (ecArgs.origWh.height * outer2cropRatio * szMultD).px ),
        marginLeft( -(ecArgs.crop.offX * outer2cropRatio * szMultD).px ),
        marginTop ( -(ecArgs.crop.offY * outer2cropRatio * szMultD).px )
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
