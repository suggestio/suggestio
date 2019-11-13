package io.suggest.jd.render.v

import diode.FastEq
import io.suggest.ad.blk.{BlockPaddings, BlockWidths}
import io.suggest.color.MColorData
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.css.ScalaCssDefaults._
import io.suggest.css.ScalaCssUtil.Implicits._
import io.suggest.dev.MSzMult
import io.suggest.font.MFontSizes
import io.suggest.jd.{JdConst, MJdTagId}
import io.suggest.jd.render.m.MJdCssArgs
import io.suggest.jd.tags.{JdTag, MJdTagNames}
import io.suggest.primo.ISetUnset
import japgolly.univeq._
import monocle.macros.GenLens
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

  private val _jdIdToStringF = {
    (jdId: MJdTagId, _: Int) =>
      jdId.toString
  }

  val jdCssArgs = GenLens[JdCss](_.jdCssArgs)

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

  /** Стили контейнера контента: блок или qd-blockless. */
  val contentOuter = style(
    // Дефолтовые настройки шрифтов внутри блока:
    fontSize( _szMulted(MFontSizes.default.value).px ),
  )

  /** Все блоки помечаются этим классом. */
  val smBlock = style(
    // Без absolute, невлезающие элементы (текст/контент) будут вылезать за пределы границы div'а.
    // TODO Возможно, position.relative (или др.) сможет это пофиксить. Заодно можно будет удалить props-флаг quirks.
    if (jdCssArgs.quirks) position.absolute
    else position.relative,
  )


  private def _allJdTagsIter: Iterator[JdTag] = {
    jdCssArgs
      .data
      .jdTagsById
      .valuesIterator
  }

  private def _filteredTagIds(filter: JdTag => Boolean): IndexedSeq[MJdTagId] = {
    (for {
      (jdId, jdt) <- jdCssArgs.data
        .jdTagsById
        .iterator
      if filter(jdt)
    } yield {
      jdId
    })
      .toIndexedSeq
  }

  // -------------------------------------------------------------------------------
  // Strip

  /** Стили контейнеров полосок, описываемых через props1.BlockMeta. */
  val blockF = {
    // Для wide - ширина и длина одинаковые.
    val pc50 = 50.%%.value
    val left0px = left( 0.px )

    styleF(
      new Domain.OverSeq(
        _filteredTagIds { jdt =>
          val p1 = jdt.props1
          p1.widthPx.nonEmpty && p1.heightPx.nonEmpty
        }
      )
    )(
      {jdId =>
        var accS = List.empty[ToStyle]

        for (blk <- jdCssArgs.data.jdTagsById.get( jdId )) {
          val wideSzMultOpt = jdCssArgs.data.jdtWideSzMults.get( jdId )

          for (widthPx0 <- blk.props1.widthPx) {
            val widthPxMulted = _szMulted( widthPx0, wideSzMultOpt )
            accS ::= width( widthPxMulted.px )

            // Выравнивание блока внутри внешнего контейнера:
            if (blk.props1.expandMode.nonEmpty && !jdCssArgs.conf.isEdit) {
              // Если wide, то надо отцентровать блок внутри wide-контейнера.
              // Формула по X банальна: с середины внешнего контейнера вычесть середину smBlock и /2.
              import io.suggest.common.html.HtmlConstants._
              val calcFormula = pc50 + SPACE + MINUS + SPACE + (widthPxMulted / 2).px.value
              accS ::= {
                left.attr := Css.Calc( calcFormula )
              }

            } else {
              // TODO Opt сделать единый стиль для left:0px, а здесь просто Style.empty делать?
              accS ::= left0px
            }
          }

          for (heightPx0 <- blk.props1.heightPx)
            accS ::= height( _szMulted(heightPx0, wideSzMultOpt).px )
        }

        // Скомпилировать акк стилей:
        styleS( accS: _* )
      },
      JdCss._jdIdToStringF,
    )
  }


  /** Стили контейнера блока с широким фоном. */
  val wideContF = styleF(
    new Domain.OverSeq(
      _filteredTagIds { jdt =>
        val p1 = jdt.props1
        p1.expandMode.nonEmpty &&
        p1.widthPx.nonEmpty &&
        p1.heightPx.nonEmpty
      }
    )
  ) (
    {jdtId =>
      var accS: List[ToStyle] = Nil

      for {
        jdt <- jdCssArgs.data.jdTagsById.get(jdtId)
      } {
        val wideSzMultOpt = jdCssArgs.data.jdtWideSzMults.get( jdtId )

        for (h <- jdt.props1.heightPx)
          accS ::= height( _szMulted(h, wideSzMultOpt).px )

        for (w <- jdt.props1.widthPx) {
          // Даже если есть фоновая картинка, но всё равно надо, чтобы ширина экрана была занята.
          val minWidthCssPx =
            if (jdCssArgs.conf.isEdit) _szMulted(w, wideSzMultOpt)
            else jdCssArgs.conf.plainWideBlockWidthPx
          accS ::= minWidth( minWidthCssPx.px )
        }

        // Цвет фона
        for (bgColor <- jdt.props1.bgColor)
          accS ::= backgroundColor( Color(bgColor.hexCode) )

        // Перезаписать дефолтовый размер шрифта в wide-блоке с доп.мультипликатором размера
        if (wideSzMultOpt.nonEmpty)
          accS ::= fontSize( _szMulted(MFontSizes.default.value, wideSzMultOpt).px )

        // Если нет фона, выставить ширину принудительно.
        if (jdt.props1.bgImg.isEmpty  &&  !jdCssArgs.conf.isEdit)
          accS ::= width( jdCssArgs.conf.plainWideBlockWidthPx.px )
      }

      // Объеденить все стили:
      styleS( accS: _* )
    },
    JdCss._jdIdToStringF,
  )


  /** Цвет фона бывает у разнотипных тегов, поэтому выносим CSS для цветов фона в отдельный каталог стилей. */
  val bgColorF =
    styleF(
      new Domain.OverSeq(
        (for {
          jdt     <- _allJdTagsIter
          bgColor <- jdt.props1.bgColor
        } yield {
          bgColor.hexCode
        })
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
  val absPosF = styleF(
    new Domain.OverSeq(
      _filteredTagIds { _.name ==* MJdTagNames.QD_CONTENT }
    )
  ) (
    {jdtId =>
      var acc: List[ToStyle] = Nil

      for (jdt <- jdCssArgs.data.jdTagsById.get(jdtId)) {
        // 2019-03-06 Для позиционирования внутри wide-блока используется поправка по горизонтали, чтобы "растянуть" контент.
        jdt.props1.topLeft.fold[Unit] {
          // qd-blockless?
          for {
            qdBlSzPot <- jdCssArgs.data.qdBlockLess.get( jdtId )
            qdBlSz <- qdBlSzPot.toOption
          } {
            // Вертикальный отступ сверху, чтобы повёрнутый контент не наезжал на блоки вверху/внизу,
            // а повёрнутый на 45..90+45 градусов - чтобы автоматом смещался вверх.
            acc ::= marginTop( ((qdBlSz.bounds.height - qdBlSz.client.height) / 2).px )
            // 2019-10-23 Горизонтальной центровкой по ширине плитки занимается gridBuilder.
          }
        } { topLeft =>
          // Обычное ручное позиционирование.
          val wideSzMultOpt = jdCssArgs.data.jdtWideSzMults.get( jdtId )
          // Внутри wide-контейнера надо растянуть контент по горизонтали. Для этого домножаем left на отношение parent-ширины к ширине фактической.
          acc =
            (top( _szMulted(topLeft.y, wideSzMultOpt).px ): ToStyle) ::
            (left( _szMulted(topLeft.x, wideSzMultOpt).px ): ToStyle) ::
            acc
        }
      }

      styleS( acc: _* )
    },
    JdCss._jdIdToStringF,
  )


  /** Стили ширин для элементов, у которых задана принудительная ширина. */
  val contentWidthF = styleF(
    new Domain.OverSeq(
      _filteredTagIds { _.props1.widthPx.nonEmpty }
    )
  ) (
    {jdtId =>
      (for {
        jdt <- jdCssArgs.data.jdTagsById.get( jdtId )
        widthPx <- jdt.props1.widthPx
      } yield {
        val wideSzMultOpt = jdCssArgs.data.jdtWideSzMults.get( jdtId )
        val gridWidthPx = jdCssArgs.conf.gridInnerWidthPx
        styleS(
          width( Math.min(gridWidthPx, _szMulted(widthPx, wideSzMultOpt)).px )
        )
      })
        .whenDefinedStyleS(identity)
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
      var acc: List[String] = Nil
      for {
        jdt     <- jdCssArgs.data.jdTagsById.get( jdtId )
        shadow  <- jdt.props1.textShadow
      } yield {
        for (mcd <- shadow.color)
          acc ::= mcd.hexCode
        for (blur <- shadow.blur)
          acc ::= (blur.toDouble / JdConst.Shadow.TextShadow.BLUR_FRAC).px.value
        val wideSzMultOpt = jdCssArgs.data.jdtWideSzMults.get( jdtId )
        acc ::= _szMulted(shadow.vOffset, wideSzMultOpt).px.value
        acc ::= _szMulted(shadow.hOffset, wideSzMultOpt).px.value
      }
      styleS(
        textShadow := acc.mkString( HtmlConstants.SPACE )
      )
    },
    JdCss._jdIdToStringF,
  )


  /** Межстрочка может быть задана вручную, но и может быть задана в рамках размера шрифта. */
  val lineHeightF = {
    val _lineHeightAttr = lineHeight

    styleF(
      new Domain.OverSeq(
        (for {
          (_, jdt) <- jdCssArgs.data
            .jdTagsById
            .iterator
          lineHeightPx <- jdt.props1
            .lineHeight
            .orElse {
              for {
                qd          <- jdt.qdProps
                attrsText   <- qd.attrsText
                szSU        <- attrsText.size
                sz          <- szSU.toOption
              } yield sz.lineHeight
            }
        } yield lineHeightPx)
          .toSet
          .toIndexedSeq
      )
    )(
      {lineHeightPx =>
        styleS(
          _lineHeightAttr( lineHeightPx.px ),
        )
      },
      (lineHeight, _) => lineHeight.toString
    )
  }


  /** styleF для стилей текстов. */
  val textF = {
    // Получаем на руки инстансы, чтобы по-быстрее использовать их в цикле и обойтись без lazy call-by-name cssAttr в __applyToColor().
    val _colorAttr = color
    val _bgColorAttr = backgroundColor
    val _fontFamilyAttr = fontFamily.attr
    val _fontSizeAttr = fontSize

    styleF(
      new Domain.OverSeq(
        _filteredTagIds { jdt =>
          jdt.props1.isContentCssStyled || jdt.qdProps.exists { qdOp =>
            qdOp.attrsText
              .exists(_.isCssStyled)
          }
        }
      )
    ) (
      {jdtId =>
        var acc = List.empty[ToStyle]

        for {
          jdt       <- jdCssArgs.data.jdTagsById.get( jdtId )
          qdProps   <- jdt.qdProps
          attrsText <- qdProps.attrsText
        } {
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
            val wideSzMultOpt = jdCssArgs.data.jdtWideSzMults.get( jdtId )
            // Отрендерить размер шрифта
            acc ::= _fontSizeAttr( _szMulted(fontSizePx.value, wideSzMultOpt).px )

            // Фикс межстрочки для мелкого текста и HTML5. Можно это не рендерить для шрифтов, которые крупнее 18px
            if (fontSizePx.forceRenderBlockHtml5)
              acc ::= display.block
          }

          //!!! При добавлении поддержки сюда новых аттрибутов attrsText, надо не забывать про набивку .isCssStyled .
        }

        // Вернуть скомпонованный стиль.
        styleS( acc: _* )
      },
      JdCss._jdIdToStringF,
    )
  }


  // -------------------------------------------------------------------------------
  // text indents.

  val embedAttrF = {
    styleF(
      new Domain.OverSeq(
        _filteredTagIds( _.qdProps.exists(_.attrsEmbed.exists(_.nonEmpty)) )
      )
    ) (
      {jdtId =>
        var acc = List.empty[ToStyle]
        val wideSzMultOpt = jdCssArgs.data.jdtWideSzMults.get( jdtId )

        for {
          jdt <- jdCssArgs.data.jdTagsById.get( jdtId )
          embedAttrs <- jdt.qdProps.get.attrsEmbed
        } yield {
          for (heightSU <- embedAttrs.height; heightPx <- heightSU)
            acc ::= height( _szMulted(heightPx, wideSzMultOpt).px )
          for (widthSU <- embedAttrs.width; widthPx <- widthSU)
            acc ::= width( _szMulted(widthPx, wideSzMultOpt).px )
        }

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


  val video = {
    val whDflt = HtmlConstants.Iframes.whCsspxDflt
    style(
      width ( _szMulted(whDflt.width).px ),
      height( _szMulted(whDflt.height).px )
    )
  }

}
