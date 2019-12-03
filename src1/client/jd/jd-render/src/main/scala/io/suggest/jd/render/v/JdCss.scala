package io.suggest.jd.render.v

import diode.FastEq
import io.suggest.ad.blk.{BlockPaddings, BlockWidths}
import io.suggest.color.MColorData
import io.suggest.common.empty.OptionUtil
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.css.ScalaCssDefaults._
import io.suggest.css.ScalaCssUtil.Implicits._
import io.suggest.font.MFontSizes
import io.suggest.grid.GridCalc
import io.suggest.jd.{JdConst, MJdConf, MJdTagId}
import io.suggest.jd.render.m.MJdCssArgs
import io.suggest.jd.tags.{IJdTagGetter, JdTag, MJdTagName, MJdTagNames}
import io.suggest.primo.ISetUnset
import japgolly.univeq._
import monocle.macros.GenLens
import scalacss.internal.DslBase.ToStyle
import scalacss.internal.DslMacros
import scalacss.internal.ValueT.TypedAttr_Color
import scalacss.internal.mutable.StyleSheet
import scalaz.{Tree, TreeLoc}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.08.17 15:40
  * Description: Динамические CSS-стили для рендера блоков плитки.
  *
  * Таблица стилей плоская для всех документов сразу.
  *
  * 2019-11-14 Произведена оптимизация обхода дерева тегов, чтобы избежать множества лишних шагов обхода при опускании
  * на нижние уровни. Все стили, не касающиеся QD_OP-уровней, используют управляемый обход дерева, завёрнутый в unfold(),
  * что позволяет оставаться на наиболее верхних уровнях дерева.
  * Это даст значительный прирост генерации стилей на реальных больших-сложных карточках с кучей текстов-картинок-видео.
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


  /** Выдать коэфф.расширения контента в wide-блоке.
    *
    * @param jd источник jd-тега.
    * @param jdConf Конфиг рендера плитки.
    * @tparam From Тип источника jd-тега.
    * @return Коэффициент соотношения wide-блока.
    */
  def wideWidthRatio[From: IJdTagGetter](jd: From, jdConf: MJdConf): Option[Double] = {
    val jdt = jd: JdTag

    // Вычислить соотношение ширины блока и ширины плитки. ratio > 1.0, т.к. выше есть проверка кол-ва колонок плитки.
    for {
      widthPx <- jdt.props1.widthPx
      if {
        (jdt.name ==* MJdTagNames.STRIP) &&
        jdt.props1.expandMode.nonEmpty &&
        // Нет смысла считать отношение ширин блока-плитки, если
        (jdConf.gridColumnsCount > BlockWidths.max.relSz)
      }
    } yield {
      // Не домножаем widthPx на szMult, т.к. gridInnerWidth уже домножен, и коэффициент будет включать szMult в себя автоматом.
      jdConf.gridInnerWidthPx.toDouble / widthPx.toDouble
    }
  }


  /** Извлечь прямые подтеги из TreeLoc. */
  private def _directChildrenOfType[From: IJdTagGetter](loc: TreeLoc[From], name: MJdTagName): LazyList[From] = {
    loc.tree
      .subForest
      .iterator
      .map(_.rootLabel)
      .filter(_.name ==* name)
      .to( LazyList )
  }


  /** walk-функция для оптимального обхода дерева jd-тегов, выдающая блоки по наиболее короткому пути. */
  private def _blocksTreeWalker[From: IJdTagGetter](locOrNull0: TreeLoc[From]): Option[(LazyList[From], TreeLoc[From])] = {
    if (locOrNull0 == null) {
      None

    } else {
      val loc0 = locOrNull0
      val jd = loc0.getLabel

      val r = jd.name match {
        case MJdTagNames.STRIP =>
          val emit = jd #:: LazyList.empty
          val loc2 = loc0.right.orNull
          emit -> loc2
        case MJdTagNames.DOCUMENT =>
          val emit = _directChildrenOfType(loc0, MJdTagNames.STRIP)
          val loc2 = loc0.right.orNull
          emit -> loc2
        // На qd-уровни не должно опускаться, но всё же отработать:
        case _ =>
          val loc2 = loc0.parent.orNull
          LazyList.empty -> loc2
      }

      Some(r)
    }
  }


  /** walk-функция для оптимального обхода дерева jd-тегов, выдающая qd-content-теги по наиболее короткому пути. */
  private def _qdBlContentTreeWalker[From: IJdTagGetter](locOrNull0: TreeLoc[From]): Option[(LazyList[From], TreeLoc[From])] = {
    if (locOrNull0 == null) {
      None

    } else {
      val loc0 = locOrNull0
      val jd = loc0.getLabel

      val r = jd.name match {
        // Вернуть данный тег + перейти на следующий тег вправо.
        case MJdTagNames.QD_CONTENT =>
          val emit = jd #:: LazyList.empty
          val loc2 = loc0.right.orNull
          emit -> loc2
        // Документ - спустится на уровень блоков.
        case MJdTagNames.DOCUMENT =>
          val loc2 = loc0
            .firstChild
            .orElse( loc0.right )
            .orNull
          LazyList.empty -> loc2
        case _ =>
          val loc2 = loc0.right
            .orNull
          LazyList.empty -> loc2
      }

      Some(r)
    }
  }


  /** walk-функция для оптимального обхода дерева jd-тегов, выдающая qd-content-теги по наиболее короткому пути. */
  private def _qdContentTreeWalker[From: IJdTagGetter](locOrNull0: TreeLoc[From]): Option[(LazyList[From], TreeLoc[From])] = {
    if (locOrNull0 == null) {
      None

    } else {
      val loc0 = locOrNull0
      val jd = loc0.getLabel

      val r = jd.name match {
        // Блок - перейти на следующий тег вправо, вернув children'ы текущего блока.
        case MJdTagNames.STRIP =>
          val emit = _directChildrenOfType(loc0, MJdTagNames.QD_CONTENT)
          val loc2 = loc0.right.orNull
          emit -> loc2

        // Вернуть данный тег + перейти на следующий тег вправо.
        case MJdTagNames.QD_CONTENT =>
          val emit = jd #:: LazyList.empty
          val loc2 = loc0.right.orNull
          emit -> loc2

        // Документ - спустится на уровень блоков.
        case MJdTagNames.DOCUMENT =>
          val loc2 = loc0
            .firstChild
            .orElse( loc0.right )
            .orNull
          LazyList.empty -> loc2

        // Элемент контента - не должно этого быть тут. Просто подняться наверх:
        case MJdTagNames.QD_OP =>
          val loc2 = loc0
            .parent
            .orNull
          LazyList.empty -> loc2
      }

      Some(r)
    }
  }


  /** Функция управляемого максимально-оптимального обхода дерева jd-тегов.
    * с целью обойти как можно МЕНЬШЕ элементов ценой повышения средней сложности одного шага.
    * Позволяет НЕ опускаться на тяжелый нижний уровень QD_OP, а гарантированно оставаться выше.
    * @param walkF walk-функция из JdCss.
    * @param filterF Функция финальной фильтрации тегов, испускаемых walk-функцией.
    * @return jdId отфильтрованных тегов.
    */
  private def _walkerFiltered(tplsIndexed: Iterable[Tree[(MJdTagId, JdTag)]],
                              walkF: TreeLoc[(MJdTagId, JdTag)] => Option[(LazyList[(MJdTagId, JdTag)], TreeLoc[(MJdTagId, JdTag)])] )
                             (filterF: JdTag => Boolean): IndexedSeq[MJdTagId] = {
    tplsIndexed
      .iterator
      .flatMap { tpl =>
        // Надо извлечь из шаблона QD_CONTENT-теги и инфу по родительским wide-блокам:
        LazyList.unfold( tpl.loc ) { locOrNull0 =>
          for {
            r0 <- walkF( locOrNull0 )
          } yield {
            val (emit0, nextLoc) = r0

            val emit2 = for {
              (jdId, jdt) <- emit0
              if filterF(jdt)
            } yield jdId

            emit2 -> nextLoc
          }
        }
      }
      .flatten
      .toIndexedSeq
  }

  def lineHeightJdIdOpt(jdId: MJdTagId, jdt: JdTag): Option[MJdTagId] = {
    OptionUtil.maybe( GridCalc.mayHaveWideSzMult(jdt) )(jdId)
  }

}


/** Динамическая часть стилей, рендерится при обновлении плиток. */
final case class JdCss( jdCssArgs: MJdCssArgs ) extends StyleSheet.Inline {

  import dsl._


  /** Мультипликация стороны на указанные пиксели. */
  import jdCssArgs.conf.{szMultF => _szMulted}


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

  /** Для qd-blockless нужны поля, чтобы текст не упирался в край экрана/карточки. */
  val qdBlOuter = {
    val sidePx = _szMulted( jdCssArgs.conf.blockPadding.outlinePx ).px
    style(
      paddingLeft( sidePx ),
      paddingRight( sidePx ),
    )
  }

  /** Все блоки помечаются этим классом. */
  val smBlock = style(
    // Без absolute, невлезающие элементы (текст/контент) будут вылезать за пределы границы div'а.
    // TODO Возможно, position.relative (или др.) сможет это пофиксить. Заодно можно будет удалить props-флаг quirks.
    if (jdCssArgs.quirks) position.absolute
    else position.relative,
  )

  /** Оптимальный обход QD_CONTENT-тегов. */
  private def _qdContentFiltered(filterF: JdTag => Boolean): IndexedSeq[MJdTagId] =
    JdCss._walkerFiltered(jdCssArgs.tplsIndexed, JdCss._qdContentTreeWalker)(filterF)

  /** Оптимальный сбор блоков. */
  private def _blocksFiltered(filterF: JdTag => Boolean): IndexedSeq[MJdTagId] =
    JdCss._walkerFiltered(jdCssArgs.tplsIndexed, JdCss._blocksTreeWalker)(filterF)


  /** Пройтись по ВСЕМ тегам с использованием фильтра, вернув id тегов.
    * Пригодно для обхода уровня QD_OP или вообще всех уровней: быстро и просто. */
  private def _allTagsIdsFiltered(filter: JdTag => Boolean): IndexedSeq[MJdTagId] = {
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

    import io.suggest.common.html.HtmlConstants._
    val pc50 = 50.%%.value + SPACE + MINUS + SPACE
    val left0px = left( 0.px ): ToStyle

    //val innerWidth = jdCssArgs.conf.gridInnerWidthPx
    //val wideWidth = width( innerWidth.px ): ToStyle
    //val wideLeft = left( (jdCssArgs.conf.plainWideBlockWidthPx - innerWidth).px ): ToStyle

    styleF(
      new Domain.OverSeq(
        _blocksFiltered { jdt =>
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
            accS = if (blk.props1.expandMode.nonEmpty && !jdCssArgs.conf.isEdit) {
              // Если wide, то надо отцентровать блок внутри wide-контейнера.
              // Формула по X банальна: с середины внешнего контейнера вычесть середину smBlock и /2.
              val calcFormula = pc50 + (widthPxMulted / 2).px.value
              val wideLeft = {
                left.attr := Css.Calc( calcFormula ): ToStyle
              }
              wideLeft :: accS

            } else {
              left0px ::
              accS
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
      _blocksFiltered { jdt =>
        val p1 = jdt.props1
        p1.expandMode.nonEmpty &&
        p1.widthPx.nonEmpty &&
        p1.heightPx.nonEmpty
      }
    )
  )({
    val wideWidthPx = jdCssArgs.conf.plainWideBlockWidthPx

    jdtId =>
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
            else wideWidthPx
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
          accS ::= width( wideWidthPx.px )
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
        // Надо пройти и QD_CONTENT, и блоки.
        jdCssArgs
          .tplsIndexed
          .iterator
          .flatMap { tpl =>
            // Надо извлечь из шаблона QD_CONTENT-теги и инфу по родительским wide-блокам:
            LazyList.unfold( tpl.loc ) { locOrNull0 =>
              for {
                (emit0, nextLoc) <- JdCss._qdContentTreeWalker( locOrNull0 )
              } yield {
                // Эмиссия кода фонового цвета:
                val emit2 = (for {
                  // Пройти блоки + qd-content:
                  jdt <- {
                    val emit1 = emit0.map(_._2)
                    val jdtParent = locOrNull0.getLabel._2
                    // Добавить текущий блок:
                    if (jdtParent.name ==* MJdTagNames.STRIP)
                      jdtParent #:: emit1
                    else
                      emit1
                  }
                    .iterator
                  // Извлечь цвет фон.
                  bgColor <- jdt.props1.bgColor
                } yield {
                  bgColor.hexCode
                })
                  .to(LazyList)

                emit2 -> nextLoc
              }
            }
          }
          .flatten
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


  /** Костыли для qd-blockless. */
  val qdBlF = styleF(
    new Domain.OverSeq({
      jdCssArgs
        .tplsIndexed
        .iterator
        .flatMap { tpl =>
          LazyList.unfold(tpl.loc)( JdCss._qdBlContentTreeWalker(_) )
        }
        .flatten
        .map(_._1)
        .toIndexedSeq
    })
  )(
    {jdId =>
      var acc = List.empty[ToStyle]
      for {
        qdBlPot <- jdCssArgs.data.qdBlockLess.get( jdId )
        qdBl    <- qdBlPot.toOption
      } {
        // Для qd-blockless желательно указать высоту, чтобы при ротации не было наезда outline'а на последующие описания.
        acc ::= height( qdBl.bounds.height.px )
      }
      styleS( acc: _* )
    },
    JdCss._jdIdToStringF,
  )


  // -------------------------------------------------------------------------------
  // AbsPos

  /** Стили для элементов, отпозиционированных абсолютно. */
  val absPosF = styleF {
    new Domain.OverSeq({
      jdCssArgs
        .tplsIndexed
        .iterator
        .flatMap { tpl =>
          // Надо извлечь из шаблона QD_CONTENT-теги и инфу по родительским wide-блокам:
          LazyList.unfold(tpl.loc) { locOrNull0 =>
            for {
              (emit0, nextLoc) <- JdCss._qdContentTreeWalker(locOrNull0)
            } yield {
              val jdt = locOrNull0.getLabel._2

              // Узнать, является ли родительский элемент wide-блоком:
              val parentWideRatio: Option[Double] = jdt.name match {
                case MJdTagNames.STRIP =>
                  JdCss.wideWidthRatio(jdt, jdCssArgs.conf)
                case MJdTagNames.QD_CONTENT =>
                  locOrNull0
                    .parent
                    .flatMap { parentLoc =>
                      JdCss.wideWidthRatio(parentLoc.getLabel, jdCssArgs.conf)
                    }
                case _ =>
                  None
              }

              val emit2 = for (m <- emit0) yield m._1 -> parentWideRatio
              emit2 -> nextLoc
            }
          }
        }
        .flatten
        .toIndexedSeq
    })
  } (
    {case (jdtId, parentWideRatioOpt) =>
      var acc: List[ToStyle] = Nil

      for {
        jdt <- jdCssArgs.data.jdTagsById.get( jdtId )
      } {
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
          //val x2 = parentWideRatioOpt.fold[Int] {
          //  _szMulted(topLeft.x, wideSzMultOpt)
          //} { ratio =>
          //  (topLeft.x * ratio).toInt
          //}

          acc =
            (top( _szMulted(topLeft.y, wideSzMultOpt).px ): ToStyle) ::
            (left( _szMulted(topLeft.x, wideSzMultOpt).px ): ToStyle) ::
            acc
        }
      }

      styleS( acc: _* )
    },
    {(r, i) =>
      JdCss._jdIdToStringF( r._1, i )
    }
  )


  /** Стили ширин для элементов, у которых задана принудительная ширина. */
  val contentWidthF = styleF(
    new Domain.OverSeq(
      _qdContentFiltered(_.props1.widthPx.nonEmpty)
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
      _qdContentFiltered(_.props1.textShadow.nonEmpty)
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


  /** Межстрочка может быть задана вручную, но и может быть задана в рамках размера шрифта.
    * Поэтому именование двойственное: может быть число, а может быть и jd-id.
    */
  val lineHeightF = {
    val _lineHeightAttr = lineHeight

    styleF(
      new Domain.OverSeq(
        (for {
          (jdId, jdt) <- jdCssArgs.data
            .jdTagsById
            .iterator

          lineHeightWithJdIdOpt <- {
            jdt.props1
              .lineHeight
              .map { jdtLineHeight =>
                val jdIdOpt = JdCss.lineHeightJdIdOpt(jdId, jdt)
                jdtLineHeight -> jdIdOpt
              }
              .orElse {
                // TODO Надо ли рендерить межстрочку для дефолтовых размеров шрифта, которые не заданы исходнике?
                for {
                  qd          <- jdt.qdProps
                  attrsText   <- qd.attrsText
                  szSU        <- attrsText.size
                  sz          <- szSU.toOption
                } yield {
                  sz.lineHeight -> (None: Option[MJdTagId])
                }
              }
          }
        } yield {
          lineHeightWithJdIdOpt
        })
          // Можно в toSet загонять только с jdId=None, а потом объединить коллекции. Но не ясно, ускоряет ли это что-либо на самом деле.
          .toSet
          .toIndexedSeq
      )
    )(
      {case (lineHeightPx, jdIdOpt) =>
        val wideSzMultOpt = jdIdOpt
          .flatMap( jdCssArgs.data.jdtWideSzMults.get )

        styleS(
          _lineHeightAttr( _szMulted(lineHeightPx, wideSzMultOpt).px ),
        )
      },
      {case ((lineHeight, jdIdOpt), _) =>
        jdIdOpt
          .getOrElse( lineHeight )
          .toString
      }
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
        _allTagsIdsFiltered { jdt =>
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
        _allTagsIdsFiltered( _.qdProps.exists(_.attrsEmbed.exists(_.nonEmpty)) )
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

        styleS( acc: _* )
      },
      JdCss._jdIdToStringF,
    )
  }


  /** Стили вращения. Индексируется по значению угла поворота. */
  val rotateF = {
    styleF(
      new Domain.OverSeq(
        jdCssArgs
          .tplsIndexed
          .iterator
          .flatMap { tpl =>
            // Надо извлечь из шаблона QD_CONTENT-теги и инфу по родительским wide-блокам:
            LazyList.unfold( tpl.loc ) { locOrNull0 =>
              for {
                r0 <- JdCss._qdContentTreeWalker( locOrNull0 )
              } yield {
                val (emit0, nextLoc) = r0

                val emit2 = emit0
                  .flatMap(_._2.props1.rotateDeg)

                emit2 -> nextLoc
              }
            }
          }
          .flatten
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
  }


  val video = {
    val whDflt = HtmlConstants.Iframes.whCsspxDflt
    style(
      width ( _szMulted(whDflt.width).px ),
      height( _szMulted(whDflt.height).px )
    )
  }

}
