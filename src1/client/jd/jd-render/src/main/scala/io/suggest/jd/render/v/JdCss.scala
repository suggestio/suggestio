package io.suggest.jd.render.v

import diode.FastEq
import enumeratum.values.ValueEnumEntry
import io.suggest.ad.blk.BlockPaddings
import io.suggest.color.MColorData
import io.suggest.common.geom.d2.{ISize2di, MSize2di}
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.css.ScalaCssDefaults._
import io.suggest.css.ScalaCssUtil.Implicits._
import io.suggest.font.{MFontSizes, MFonts}
import io.suggest.jd.JdConst
import io.suggest.jd.render.m.MJdCssArgs
import io.suggest.jd.tags.{JdTag, MJdTagNames}
import io.suggest.jd.tags.qd.MQdOp
import io.suggest.primo.ISetUnset
import io.suggest.text.MTextAligns
import japgolly.univeq._
import scalacss.internal.DslBase.ToStyle
import scalacss.internal.DslMacros
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

  @inline implicit def univEq: UnivEq[JdCss] = UnivEq.derive


  private[v] def valueEnumEntryDomainNameF[T] = {
    (vee: ValueEnumEntry[T], _: Int) =>
      vee.value
  }

}


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


  // Для строк и групп строк стили более точечные.
  // -------------------------------------------------------------------------------
  // text-align

  /** styleF допустимых выравниваний текста. */
  val textAlignsStyleF = {
    styleF(
      new Domain.OverSeq( MTextAligns.values )
    )(
      { align =>
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
      },
      JdCss.valueEnumEntryDomainNameF
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


}


case class JdCss( jdCssArgs: MJdCssArgs ) extends StyleSheet.Inline {

  import dsl._

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
    // TODO Возможно, position.relative (или др.) сможет это пофиксить. Заодно можно будет удалить props-флаг quirks.
    if (jdCssArgs.quirks) position.absolute else position.relative,

    // Дефолтовые настройки шрифтов внутри блока:
    fontFamily.attr := Css.quoted( MFonts.default.cssFontFamily ),
    fontSize( (MFontSizes.default.value * blkSzMultD).px ),
    color.black
  )


  private def _allJdTagsIter: Iterator[JdTag] = {
    jdCssArgs
      .templates
      .iterator
      .flatMap( _.flatten )
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

  /** Внутри wide-блока находится контейнер контентов (это strip). Ширина wide-стрипа задаётся здесь: */
  //private lazy val wideBlockWidthPx = jdCssArgs.conf.gridWidthPx * 0.70

  /** Стили контейнеров полосок, описываемых через props1.BlockMeta. */
  val bmStyleF = {
    lazy val pc50 = 50.%%.value
    // Для wide - ширина и длина одинаковые.
    /*
    lazy val (wideLeftAv, wideWidthAv) = {
      val wLeftPx  = (jdCssArgs.conf.gridWidthPx * 0.15).px
      (left(wLeftPx), width(wideBlockWidthPx.px))
    }
    */

    styleF(
      new Domain.OverSeq(
        _allJdTagsIter
          .filter(_.props1.bm.nonEmpty)
          .toIndexedSeq
      )
    ) { strip =>
      var accS = List.empty[ToStyle]

      // Стиль размеров блока-полосы.
      for (bm <- strip.props1.bm) {
        val szMulted = bmStyleWh(bm)
        accS ::= height( szMulted.height.px )
        accS ::= width ( szMulted.width.px  )

        // Выравнивание блока внутри внешнего контейнера:
        if (bm.wide && !jdCssArgs.conf.isEdit) {
          // Если wide, то надо отцентровать блок внутри wide-контейнера.
          // Формула по X банальна: с середины внешнего контейнера вычесть середину smBlock и /2.
          import io.suggest.common.html.HtmlConstants._
          accS ::= {
            val calcFormula = pc50 + SPACE + MINUS + SPACE + (szMulted.width / 2).px.value
            left.attr := Css.Calc( calcFormula )
          }

          //accS ::= wideLeftAv
          //accS ::= wideWidthAv

        } else {
          accS ::= left(0.px)
        }
      }

      styleS( accS: _* )
    }
  }


  /** Стили для фоновых картинок стрипов. */
  val stripBgStyleF =
    styleF(
      new Domain.OverSeq(
        _allJdTagsIter
          .filter { jdt =>
            // Интересуют только стрипы c bgImg, но без wide
            val p1 = jdt.props1
            p1.bm.nonEmpty &&
              (jdt.name ==* MJdTagNames.STRIP) &&
              p1.bgImg.nonEmpty
          }
          .toIndexedSeq
      )
    ) { strip =>
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


  /** Стили контейнера блока с широким фоном. */
  val wideContStyleF =
    styleF(
      new Domain.OverSeq(
        _allJdTagsIter
          .filter(_.props1.bm.exists(_.wide))
          .toIndexedSeq
      )
    ) { strip =>
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


  /** Цвет фона бывает у разнотипных тегов, поэтому выносим CSS для цветов фона в отдельный каталог стилей. */
  val bgColorOptStyleF =
    styleF(
      new Domain.OverSeq(
        _allJdTagsIter
          .flatMap(_.props1.bgColor)
          .map(_.hexCode)
          .toSet
          .toIndexedSeq
      )
    ) (
      { bgColorHex =>
        styleS(
          backgroundColor( Color(bgColorHex) )
        )
      },
      (colorHex, _) => MColorData.stripDiez(colorHex)
    )


  // -------------------------------------------------------------------------------
  // AbsPos

  /** Стили для элементов, отпозиционированных абсолютно. */
  val absPosStyleF = {
    styleF(
      new Domain.OverSeq({
        val iter = for {
          // topTree может быть как DOCUMENT, так и STRIP.
          topJdTree <- jdCssArgs.templates.iterator
          topJdt = topJdTree.rootLabel
          // Собираем только стрипы:
          stripTree <- topJdt.name match {
            case MJdTagNames.STRIP => topJdTree :: Nil
            case MJdTagNames.DOCUMENT => topJdTree.subForest
            case _ => Nil
          }
          strip = stripTree.rootLabel
          if (strip.name ==* MJdTagNames.STRIP)
          jdTagTree <- stripTree.subForest
          jdt = jdTagTree.rootLabel
          if (jdt.name ==* MJdTagNames.QD_CONTENT) &&
             jdt.props1.topLeft.nonEmpty
        } yield {
          val jdt = jdTagTree.rootLabel
          (strip, jdt)
        }
        iter.toIndexedSeq
      })
    ) { case (parentJdt, jdt) =>
      // 2019-03-06 Для позиционирования внутри wide-блока используется поправка по горизонтали, чтобы "растянуть" контент.
      jdt.props1.topLeft.whenDefinedStyleS { topLeft =>
        // Внутри wide-контейнера надо растянуть контент по горизонтали. Для этого домножаем left на отношение parent-ширины к ширине фактической.
        var leftMult = blkSzMultD
        // TODO Перепилить алгоритм растяжения, чтобы растягивал пропорционально всё, ужимая по вертикали.
        //for (bm <- parentJdt.props1.bm if bm.wide) {
        //  leftMult = leftMult * ( wideBlockWidthPx / (bm.width * blkSzMultD) ) * 1.2
        //}
        styleS(
          top( (topLeft.y * blkSzMultD).px ),
          left( (topLeft.x * leftMult).px )
        )
      }
    }
  }


  /** Стили ширин для элементов, у которых задана принудительная ширина. */
  val forcedWidthStyleF =
    styleF(
      new Domain.OverSeq(
        _allJdTagsIter
          .filter(_.props1.widthPx.nonEmpty)
          .toIndexedSeq
      )
    ) { jdt =>
      jdt.props1.widthPx.whenDefinedStyleS { widthPx =>
        styleS(
          width( szMultedSide(widthPx).px )
        )
      }
    }


  // -------------------------------------------------------------------------------
  // fonts

  private def _qdOpsIter: Iterator[MQdOp] = {
    _allJdTagsIter
      .flatMap(_.qdProps)
  }

  /** Тени текста. */
  val contentShadowF = {
    val szMult = jdCssArgs.conf.szMult.toDouble
    styleF(
      new Domain.OverSeq(
        _allJdTagsIter
          .filter(_.props1.textShadow.nonEmpty)
          .toIndexedSeq
      )
    ) { jdt =>
      val shadow = jdt.props1.textShadow.get
      var acc: List[String] = Nil
      for (mcd <- shadow.color)
        acc ::= mcd.hexCode
      for (blur <- shadow.blur)
        acc ::= (blur.toDouble / JdConst.Shadow.TextShadow.BLUR_FRAC).px.value
      acc ::= (shadow.vOffset * szMult).px.value
      acc ::= (shadow.hOffset * szMult).px.value
      styleS(
        textShadow := acc.mkString( HtmlConstants.SPACE )
      )
    }
  }

  /** styleF для стилей текстов. */
  val textStyleF = {
    // Получаем на руки инстансы, чтобы по-быстрее использовать их в цикле и обойтись без lazy call-by-name cssAttr в __applyToColor().
    val _colorAttr = color
    val _bgColorAttr = backgroundColor
    val _fontFamilyAttr = fontFamily.attr
    val _fontSizeAttr = fontSize
    val _lineHeightAttr = lineHeight

    styleF(
      new Domain.OverSeq(
        _qdOpsIter
          .flatMap(_.attrsText.iterator)
          .filter(_.isCssStyled)
          .toIndexedSeq
      )
    ) { attrsText =>
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

      // Если задан font-size, то нужно отрендерить его вместе с сопутствующими аттрибутами.
      for (fontSizeSU <- attrsText.size; fontSizePx <- fontSizeSU) {
        // Рендер размера шрифта
        acc ::= _lineHeightAttr( (fontSizePx.lineHeight * blkSzMultD).px )
        // Отрендерить размер шрифта
        acc ::= _fontSizeAttr( (fontSizePx.value * blkSzMultD).px )

        if (fontSizePx.forceRenderBlockHtml5)
          acc ::= display.block
      }

      // Фикс межстрочки для мелкого текста и HTML5. Можно это не рендерить для шрифтов, которые крупнее 18px

      // Вернуть скомпонованный стиль.
      styleS(
        acc: _*
      )
    }
  }


  // -------------------------------------------------------------------------------
  // text indents.

  val embedAttrStyleF =
    styleF(
      new Domain.OverSeq(
        _qdOpsIter
          .flatMap( _.attrsEmbed )
          .filter( _.nonEmpty )
          .toIndexedSeq
      )
    ) { embedAttrs =>
      var acc = List.empty[ToStyle]

      for (heightSU <- embedAttrs.height; heightPx <- heightSU)
        acc ::= height( Math.round(heightPx * blkSzMultD).px )
      for (widthSU <- embedAttrs.width; widthPx <- widthSU)
        acc ::= width( Math.round(widthPx * blkSzMultD).px )

      styleS(
        acc: _*
      )
    }


  val rotateF =
    styleF.apply(
      new Domain.OverSeq(
        _allJdTagsIter
          .flatMap(_.props1.rotateDeg)
          // Порядок не важен, но нужно избегать одинаковых углов поворота в списке допустимых значений:
          .toSet
          .toIndexedSeq
      )
    )(
      {rotateDeg =>
        styleS(
          transform := ("rotate(" + rotateDeg + "deg)" )
        )
      },
      DslMacros.defaultStyleFClassNameSuffixI
    )


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
