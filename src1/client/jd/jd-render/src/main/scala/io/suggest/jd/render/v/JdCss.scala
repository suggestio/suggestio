package io.suggest.jd.render.v

import diode.FastEq
import enumeratum.values.ValueEnumEntry
import io.suggest.ad.blk.BlockPaddings
import io.suggest.color.MColorData
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.css.ScalaCssDefaults._
import io.suggest.css.ScalaCssUtil.Implicits._
import io.suggest.dev.MSzMult
import io.suggest.font.{MFontSizes, MFonts}
import io.suggest.jd.{JdConst, MJdTagId}
import io.suggest.jd.render.m.MJdCssArgs
import io.suggest.jd.tags.{JdTag, MJdTagNames}
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

  private val _jdIdToStringF = {
    (jdId: MJdTagId, i: Int) =>
      jdId.toString
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


  val smBlockS = style(
    // Без addClassName("sm-block"), т.к. это ненужные transition и уже неактуальные стили (кроме overflow:hidden).
    overflow.hidden,
    // Дефолтовые настройки шрифтов внутри блока:
    fontFamily.attr := Css.quoted( MFonts.default.cssFontFamily ),
    color.black
  )

}


/** Динамическая часть стилей, рендерится при обновлении плиток. */
final case class JdCss( jdCssArgs: MJdCssArgs ) extends StyleSheet.Inline {

  import dsl._


  /** Мультипликация стороны на указанные пиксели. */
  private val _szMulted = MSzMult.szMultedF( jdCssArgs.conf.szMult )


  /** Стиль выделения группы блоков. */
  val blockGroupOutline = style(
    outlineStyle.solid,
    outlineWidth( _szMulted( BlockPaddings.default.outlinePx ).px )
  )

  // TODO Вынести статические стили в object ScCss?
  /** Все блоки помечаются этим классом. */
  val smBlock = style(
    // Без absolute, невлезающие элементы (текст/контент) будут вылезать за пределы границы div'а.
    // TODO Возможно, position.relative (или др.) сможет это пофиксить. Заодно можно будет удалить props-флаг quirks.
    if (jdCssArgs.quirks) position.absolute
    else position.relative,

    // Дефолтовые настройки шрифтов внутри блока:
    fontSize( _szMulted(MFontSizes.default.value).px ),
  )


  private def _allJdTagsIter: Iterator[JdTag] = {
    jdCssArgs
      .jdTagsById
      .valuesIterator
  }

  private def _filteredTagIds(filter: JdTag => Boolean): IndexedSeq[MJdTagId] = {
    jdCssArgs.jdTagsById.iterator
      .filter { jdtWithId =>
        filter( jdtWithId._2 )
      }
      .map(_._1)
      .toIndexedSeq
  }

  // -------------------------------------------------------------------------------
  // Strip

  /** Внутри wide-блока находится контейнер контентов (это strip). Ширина wide-стрипа задаётся здесь: */
  //private lazy val wideBlockWidthPx = jdCssArgs.conf.gridWidthPx * 0.70

  /** Стили контейнеров полосок, описываемых через props1.BlockMeta. */
  val bmStyleF = {
    // Для wide - ширина и длина одинаковые.
    /*
    lazy val (wideLeftAv, wideWidthAv) = {
      val wLeftPx  = (jdCssArgs.conf.gridWidthPx * 0.15).px
      (left(wLeftPx), width(wideBlockWidthPx.px))
    }
    */
    val pc50 = 50.%%.value
    val left0px = left( 0.px )

    styleF(
      new Domain.OverSeq(
        _filteredTagIds( _.props1.bm.nonEmpty )
      )
    )(
      {stripId =>
        val strip = jdCssArgs.jdTagsById( stripId )
        var accS = List.empty[ToStyle]

        // Стиль размеров блока-полосы.
        for (bm <- strip.props1.bm) {
          val wideSzMultOpt = jdCssArgs.jdtWideSzMults.get( strip )

          val widthPx = _szMulted( bm.width, wideSzMultOpt )
          accS ::= height( _szMulted( bm.height, wideSzMultOpt ).px )
          accS ::= width ( widthPx.px )

          // Перезаписать дефолтовый размер шрифта в wide-блоке с доп.мультипликатором размера
          for (_ <- wideSzMultOpt) {
            accS ::= fontSize( _szMulted(MFontSizes.default.value, wideSzMultOpt).px )
          }

          // Выравнивание блока внутри внешнего контейнера:
          if (bm.expandMode.nonEmpty && !jdCssArgs.conf.isEdit) {
            // Если wide, то надо отцентровать блок внутри wide-контейнера.
            // Формула по X банальна: с середины внешнего контейнера вычесть середину smBlock и /2.
            import io.suggest.common.html.HtmlConstants._
            accS ::= {
              val calcFormula = pc50 + SPACE + MINUS + SPACE + (widthPx / 2).px.value
              left.attr := Css.Calc( calcFormula )
            }

            //accS ::= wideLeftAv
            //accS ::= wideWidthAv

          } else {
            // TODO Opt сделать единый стиль для left:0px, а здесь просто Style.empty делать?
            accS ::= left0px
          }
        }

        styleS( accS: _* )
      },
      JdCss._jdIdToStringF,
    )
  }


  /** Стили для фоновых картинок стрипов. */
  val stripBgStyleF = styleF(
    new Domain.OverSeq(
      _filteredTagIds { jdt =>
        // Интересуют только стрипы c bgImg, но без wide
        val p1 = jdt.props1
        p1.bm.nonEmpty &&
          (jdt.name ==* MJdTagNames.STRIP) &&
          p1.bgImg.nonEmpty
      }
    )
  ) (
    {stripId =>
      val strip = jdCssArgs.jdTagsById( stripId )
      strip.props1.bm.whenDefinedStyleS { bm =>
        val wideSzMultOpt = jdCssArgs.jdtWideSzMults.get( strip )
        styleS(
          // Записываем одну из двух сторон картинки.
          if (bm.expandMode.nonEmpty) {
            // wide-картинки можно прессовать только по высоте блока
            height( _szMulted( bm.height, wideSzMultOpt ).px )
          } else {
            // Избегаем расплющивания картинок, пусть лучше обрезка будет. Здесь только width.
            width( _szMulted( bm.width, wideSzMultOpt ).px )
          }
        )
      }
    },
    JdCss._jdIdToStringF,
  )


  /** Стили контейнера блока с широким фоном. */
  val wideContStyleF = styleF(
    new Domain.OverSeq(
      _filteredTagIds { jdt =>
        jdt.props1.bm.hasExpandMode
      }
    )
  ) (
    {stripId =>
      val strip = jdCssArgs.jdTagsById( stripId )
      var accS: List[ToStyle] = Nil
      // Уточнить размеры wide-блока:
      for (bm <- strip.props1.bm) {
        val wideSzMultOpt = jdCssArgs.jdtWideSzMults.get( strip )
        accS ::= (height( _szMulted(bm.height, wideSzMultOpt).px ): ToStyle)

        // Даже если есть фоновая картинка, но всё равно надо, чтобы ширина экрана была занята.
        accS ::= minWidth(
          if (jdCssArgs.conf.isEdit)
            _szMulted(bm.width, wideSzMultOpt).px
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
    },
    JdCss._jdIdToStringF,
  )


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
  val absPosStyleF = styleF(
    new Domain.OverSeq({
      _filteredTagIds { _.props1.topLeft.nonEmpty }
    })
  ) (
    { jdtId =>
      val jdt = jdCssArgs.jdTagsById( jdtId )
      // 2019-03-06 Для позиционирования внутри wide-блока используется поправка по горизонтали, чтобы "растянуть" контент.
      jdt.props1.topLeft.whenDefinedStyleS { topLeft =>
        val wideSzMultOpt = jdCssArgs.jdtWideSzMults.get( jdt )
        // Внутри wide-контейнера надо растянуть контент по горизонтали. Для этого домножаем left на отношение parent-ширины к ширине фактической.
        styleS(
          top( _szMulted(topLeft.y, wideSzMultOpt).px ),
          left( _szMulted(topLeft.x, wideSzMultOpt).px ),
        )
      }
    },
    JdCss._jdIdToStringF,
  )



  /** Стили ширин для элементов, у которых задана принудительная ширина. */
  val forcedWidthStyleF = styleF(
    new Domain.OverSeq(
      _filteredTagIds { _.props1.widthPx.nonEmpty }
    )
  ) (
    {jdtId =>
      val jdt = jdCssArgs.jdTagsById( jdtId )
      jdt.props1.widthPx.whenDefinedStyleS { widthPx =>
        val wideSzMultOpt = jdCssArgs.jdtWideSzMults.get( jdt )
        styleS(
          width( _szMulted(widthPx, wideSzMultOpt).px )
        )
      }
    },
    JdCss._jdIdToStringF,
  )


  // -------------------------------------------------------------------------------
  // fonts

  /** Тени текста. */
  val contentShadowF = styleF(
    new Domain.OverSeq(
      _filteredTagIds { _.props1.textShadow.nonEmpty }
    )
  ) (
    {jdtId =>
      val jdt = jdCssArgs.jdTagsById( jdtId )
      val shadow = jdt.props1.textShadow.get
      var acc: List[String] = Nil
      for (mcd <- shadow.color)
        acc ::= mcd.hexCode
      for (blur <- shadow.blur)
        acc ::= (blur.toDouble / JdConst.Shadow.TextShadow.BLUR_FRAC).px.value
      val wideSzMultOpt = jdCssArgs.jdtWideSzMults.get( jdt )
      acc ::= _szMulted(shadow.vOffset, wideSzMultOpt).px.value
      acc ::= _szMulted(shadow.hOffset, wideSzMultOpt).px.value
      styleS(
        textShadow := acc.mkString( HtmlConstants.SPACE )
      )
    },
    JdCss._jdIdToStringF,
  )


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
        _filteredTagIds { jdt =>
          jdt.qdProps.exists { qdOp =>
            qdOp.attrsText
              .exists(_.isCssStyled)
          }
        }
      )
    ) (
      {jdtId =>
        val jdt = jdCssArgs.jdTagsById( jdtId )
        val attrsText = jdt.qdProps.get.attrsText.get

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
          val wideSzMultOpt = jdCssArgs.jdtWideSzMults.get( jdt )

          // Рендер размера шрифта
          acc ::= _lineHeightAttr( _szMulted(fontSizePx.lineHeight, wideSzMultOpt).px )
          // Отрендерить размер шрифта
          acc ::= _fontSizeAttr( _szMulted(fontSizePx.value, wideSzMultOpt).px )

          // Фикс межстрочки для мелкого текста и HTML5. Можно это не рендерить для шрифтов, которые крупнее 18px
          if (fontSizePx.forceRenderBlockHtml5)
            acc ::= display.block
        }

        //!!! При добавлении поддержки сюда новых аттрибутов attrsText, надо не забывать про набивку .isCssStyled .

        // Вернуть скомпонованный стиль.
        styleS( acc: _* )
      },
      JdCss._jdIdToStringF,
    )
  }


  // -------------------------------------------------------------------------------
  // text indents.

  val embedAttrStyleF = {
    styleF(
      new Domain.OverSeq(
        _filteredTagIds( _.qdProps.exists(_.attrsEmbed.exists(_.nonEmpty)) )
      )
    ) (
      {jdtId =>
        val jdt = jdCssArgs.jdTagsById( jdtId )

        val embedAttrs = jdt.qdProps.get.attrsEmbed.get
        var acc = List.empty[ToStyle]
        val wideSzMultOpt = jdCssArgs.jdtWideSzMults.get( jdt )

        for (heightSU <- embedAttrs.height; heightPx <- heightSU)
          acc ::= height( _szMulted(heightPx, wideSzMultOpt).px )
        for (widthSU <- embedAttrs.width; widthPx <- widthSU)
          acc ::= width( _szMulted(widthPx, wideSzMultOpt).px )

        styleS(
          acc: _*
        )
      },
      JdCss._jdIdToStringF,
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
      width ( _szMulted(whDflt.width).px ),
      height( _szMulted(whDflt.height).px )
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
