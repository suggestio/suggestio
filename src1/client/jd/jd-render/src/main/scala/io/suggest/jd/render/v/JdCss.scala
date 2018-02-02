package io.suggest.jd.render.v

import diode.FastEq
import io.suggest.ad.blk.BlockPaddings
import io.suggest.color.MColorData
import io.suggest.common.geom.d2.{ISize2di, MSize2di}
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.css.ScalaCssDefaults._
import io.suggest.css.ScalaCssUtil.Implicits._
import io.suggest.font.{MFontSizes, MFonts}
import io.suggest.jd.render.m.MJdCssArgs
import io.suggest.jd.tags.{JdTag, MJdTagNames}
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

  implicit object JdCssFastEq extends FastEq[JdCss] {
    override def eqv(a: JdCss, b: JdCss): Boolean = {
      MJdCssArgs.MJdCssArgsFastEq.eqv(a.jdCssArgs, b.jdCssArgs)
    }
  }

  implicit def univEq: UnivEq[JdCss] = UnivEq.derive

}

case class JdCss( jdCssArgs: MJdCssArgs ) extends StyleSheet.Inline {

  import dsl._

  //val szMultD = jdCssArgs.conf.szMult.toDouble

  val blkSzMultD = jdCssArgs.conf.blkSzMult.toDouble

  /** Стиль выделения группы блоков. */
  val blockGroupOutline = style(
    outlineStyle.solid,
    outlineWidth( szMultedSide( BlockPaddings.default.outlinePx ).px )
  )

  // TODO Вынести статические стили в object ScCss?
  /** Все блоки помечаются этим классом. */
  val smBlock = style(
    // Без addClassName("sm-block"), т.к. это ненужные transition и уже неактуальные стили (кроме overflow:hidden).
    overflow.hidden,
    // Без absolute, невлезающие элементы (текст/контент) будут вылезать за пределы границы div'а.
    position.absolute,
    // Дефолтовые настройки шрифтов внутри блока:
    fontFamily.attr := Css.quoted( MFonts.default.cssFontFamily ),
    fontSize( (MFontSizes.default.value * blkSzMultD).px )
  )

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

  private def _allJdTagsIter: Iterator[JdTag] = {
    jdCssArgs
      .templates
      .iterator
      .flatMap( _.flatten )
  }

  val wideBlockStyle = {
    val zeroPx = 0.px
    style(
      // TODO Переместить top в smBlock?
      top(zeroPx)
      //left(zeroPx)
    )
  }

  // -------------------------------------------------------------------------------
  // Strip

  def szMultedSide(sizePx: Int) = Math.round(sizePx * blkSzMultD).toInt

  def bmStyleWh(bm: ISize2di): MSize2di = {
    MSize2di(
      width  = szMultedSide(bm.width),
      height = szMultedSide(bm.height)
    )
  }

  /** Стили контейнеров полосок, описываемых через props1.BlockMeta. */
  val bmStyleF = {
    val strips = _allJdTagsIter
      .filter(_.props1.bm.nonEmpty)
      .toIndexedSeq

    val stripsDomain = new Domain.OverSeq( strips )

    styleF(stripsDomain) { strip =>
      var accS = List.empty[ToStyle]

      // Стиль размеров блока-полосы.
      for (bm <- strip.props1.bm) {
        val szMulted = bmStyleWh(bm)
        accS ::= width ( szMulted.width.px )
        accS ::= height( szMulted.height.px )

        // Выравнивание блока внутри внешнего контейнера:
        if (bm.wide && !jdCssArgs.conf.isEdit) {
          // Если wide, то надо отцентровать блок внутри wide-контейнера.
          // Формула по X банальна: с середины внешнего контейнера вычесть серелину smBlock и /2.
          import io.suggest.common.html.HtmlConstants._
          val calcFormula = 50.%%.value + SPACE + MINUS + SPACE + (szMulted.width / 2).px.value
          val calcAV: ToStyle = {
            left.attr := Css.Calc( calcFormula )
          }
          accS ::= calcAV
        } else {
          accS ::= left(0.px)
        }
      }

      styleS( accS: _* )
    }
  }


  /** Стили для фоновых картинок стрипов. */
  val stripBgStyleF = {
    // Интересуют только стрипы c bgImg, но без wide
    val strips = _allJdTagsIter
      .filter { jdt =>
        val p1 = jdt.props1
        p1.bm.nonEmpty && (jdt.name ==* MJdTagNames.STRIP) && p1.bgImg.nonEmpty
      }
      .toIndexedSeq
    val stripsDomain = new Domain.OverSeq( strips )

    styleF(stripsDomain) { strip =>
      strip.props1.bm.whenDefinedStyleS { bm =>
        styleS(
          // Записываем одну из двух сторон картинки.
          if (bm.wide) {
            // wide-картинки можно прессовать только по высоте блока
            height( szMultedSide(bm.height).px )
          } else {
            // Избегаем расплющивания картинок, пусть лучше обрезка будет. Здесь только width.
            width( szMultedSide(bm.width).px )
          }
        )
      }
    }
  }


  /** Стили контейнера блока с широким фоном. */
  val bmWideStyleF = {
    // TODO Не используется в редакторе из-за проблем с вёрсткой. Но стили всё равно генерятся.
    val wideStrips = _allJdTagsIter
      .filter(_.props1.bm.exists(_.wide))
      .toIndexedSeq
    val wideStripsDomain = new Domain.OverSeq( wideStrips )
    styleF( wideStripsDomain ) { strip =>
      var accS: List[ToStyle] = Nil
      // Уточнить размеры wide-блока:
      for (bm <- strip.props1.bm) {
        accS ::= (height( szMultedSide(bm.height).px ): ToStyle)

        // Даже если есть фоновая картинка, но всё равно надо, чтобы ширина экрана была занята.
        accS ::= minWidth(
          if (jdCssArgs.conf.isEdit)
            szMultedSide(bm.width).px
          else
            jdCssArgs.conf.gridWidthPx.px
        )
      }
      // Цвет фона
      for (bgColor <- strip.props1.bgColor) {
        accS ::= (backgroundColor(Color(bgColor.hexCode)): ToStyle)
      }

      // Если нет фона, выставить ширину принудительно.
      if (strip.props1.bgImg.isEmpty  &&  !jdCssArgs.conf.isEdit) {
        accS ::= width( jdCssArgs.conf.plainWideBlockWidthPx.px )
      }
      // Объеденить все стили:
      styleS( accS: _* )
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
          top( (topLeft.y * blkSzMultD).px ),
          left( (topLeft.x * blkSzMultD).px )
        )
      }
    }
  }

  /** Стили ширин для элементов, у которых задана принудительная ширина. */
  val forcedWidthStyleF = {
    val widthsDomain = {
      val tags = _allJdTagsIter
        .filter(_.props1.widthPx.nonEmpty)
        .toIndexedSeq
      new Domain.OverSeq(tags)
    }
    styleF(widthsDomain) { jdt =>
      jdt.props1.widthPx.whenDefinedStyleS { widthPx =>
        styleS(
          width( widthPx.px )
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
        acc ::= _lineHeightAttr( (fontSize.lineHeight * blkSzMultD).px )
        // Отрендерить размер шрифта
        acc ::= _fontSizeAttr( (fontSize.value * blkSzMultD).px )
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

      for (heightSU <- embedAttrs.height; heightPx <- heightSU)
        acc ::= height( Math.round(heightPx * blkSzMultD).px )
      for (widthSU <- embedAttrs.width; widthPx <- widthSU)
        acc ::= width( Math.round(widthPx * blkSzMultD).px )

      styleS(
        acc: _*
      )
    }
  }


  val videoStyle = {
    val whDflt = HtmlConstants.Iframes.whCsspxDflt
    style(
      width ( Math.round(whDflt.width  * blkSzMultD).px ),
      height( Math.round(whDflt.height * blkSzMultD).px )
    )
  }

  /** Стили для видео-фреймов. */
  /*
  val videoStyleF = {
    val videoPred = MPredicates.JdContent.Video
    val videosIter = for {
      qdOp        <- _qdOpsIter
      eid         <- qdOp.edgeInfo
      dataEdge    <- jdCssArgs.edges.get(eid.edgeUid)
      if dataEdge.jdEdge.predicate ==>> videoPred
    } yield {
      (qdOp, dataEdge)
    }
    val videosSeq = videosIter.toIndexedSeq

    val videosDomain = new Domain.OverSeq( videosSeq )
    styleF( videosDomain ) { _ =>
      // Пока используем дефолтовые размеры видео-фрейма -- 300x150: https://stackoverflow.com/a/22844117
      styleS(
        width ( Math.round(300 * blkSzMultD).px ),
        height( Math.round(150 * blkSzMultD).px )
      )
    }
  }
  */

  // -------------------------------------------------------------------------------
  // images + crop.

  /** Стили для эмуляции кропа на фоновом изображении блока. */
  /*
  val blkBgImgCropEmuF = {
    val emuCrops = {
      val cropsIter = for {
        jdt       <- _allJdTagsIter
        bm        <- jdt.props1.bm
        bgImg     <- jdt.props1.bgImg
        mcrop     <- bgImg.crop
        e         <- jdCssArgs.edges.get( bgImg.imgEdge.edgeUid )
        origWh    <- e.origWh
      } yield {
        //val outerWh = bm.rePadded( jdCssArgs.conf.blockPadding )
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
        width     ( (ecArgs.origWh.width  * outer2cropRatio * blkSzMultD).px ),
        height    ( (ecArgs.origWh.height * outer2cropRatio * blkSzMultD).px ),
        marginLeft( -(ecArgs.crop.offX * outer2cropRatio * blkSzMultD).px ),
        marginTop ( -(ecArgs.crop.offY * outer2cropRatio * blkSzMultD).px )
      )
    }
  }
  */

}


import com.softwaremill.macwire._

/** DI-factory для сборки инстансов [[JdCss]]. */
class JdCssFactory {

  def mkJdCss( jdCssArgs: MJdCssArgs ): JdCss = {
    wire[JdCss]
  }

}
