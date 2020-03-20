package io.suggest.grid

import com.github.dantrain.react.stonecutter.{CssGridProps, EnterExitStyle, GridComponent_t, ItemProps, LayoutFunRes, PropsCommon}
import io.suggest.ad.blk.{BlockWidths, MBlockExpandModes}
import io.suggest.grid.build.{GridBuilderUtil, MGbBlock, MGbSidePx, MGbSize, MGridBuildArgs, MGridBuildResult}
import io.suggest.jd.{MJdConf, MJdTagId}
import io.suggest.jd.render.m.{MJdArgs, MJdRuntime}
import io.suggest.jd.tags.{JdTag, MJdTagNames}
import scalaz.Tree
import japgolly.univeq._

import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.util.Random

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.11.17 14:46
  * Description: JS-модуль для связывания GridBuilder'а и js-компонентов плитки.
  */
object GridBuilderUtilJs {

  /** stateless вычисления координат для плитки для указанных основе исходных item'ов.
    * Создан, чтобы использовать как статическую layout-функцию, т.е. состояние билда живёт только внутри.
    *
    * @param flatJsItems Плоский массив элементов плитки, переданный через stonecutter.
    *                    Не используется напрямую: дерево данных по item'ам передаётся напрямую в args.
    * @param props Пропертисы компонента плитки.
    * @return Контейнер данных по расположению заданных элементов в плитке.
    */
  def stoneCutterLayout(buildRes: MGridBuildResult)(flatJsItems: js.Array[ItemProps], props: PropsCommon): LayoutFunRes = {
    // Привести список координат к формату stonecutter: массив из массивов-пар координат.
    val gbsPositions = buildRes.coords
      .iterator
      .map { gbRes =>
        val mcoord = gbRes.topLeft
        js.Array( mcoord.x, mcoord.y )
      }
      .toJSArray

    // Помимо координат, надо вычислить итоговые размеры плитки.
    new LayoutFunRes {
      override val positions  = gbsPositions
      override val gridHeight = buildRes.gridWh.height
      override val gridWidth  = buildRes.gridWh.width
    }
  }


  /** Сборка MGbSize из тега.
    *
    * @param jdt jd-тег.
    * @param jdRuntime Рантайм, нужен для qd-blockless.
    * @param jdConf Конфиг рендера. Нужен для qd-blockless.
    * @return MGbSize, описывающий размеры рендерящегося блока.
    */
  def gbSizeFromJdt(jdId: MJdTagId, jdt: JdTag, jdRuntime: MJdRuntime, jdConf: MJdConf): Option[MGbSize] = {
    jdt.name match {
      case MJdTagNames.STRIP =>
        val p1 = jdt.props1
        for {
          w <- p1.blockWidth
          heightPx <- p1.heightPx
        } yield {
          MGbSize(
            widthCells = w.relSz,
            heightPx   = MGbSidePx(
              sizePx      = heightPx,
              isSzMulted  = false,
            ),
            expandMode = p1.expandMode,
          )
        }

      case MJdTagNames.QD_CONTENT =>
        // Внеблоковые элементы. Надо узнать их высоту.
        val qdBlSzOpt = jdRuntime.data.qdBlockLess
          .get( jdId )
          .flatMap(_.toOption)

        lazy val jdtWidthPxGbSzOpt = for {
          widthPx <- jdt.props1.widthPx
        } yield {
          MGbSidePx(
            sizePx      = widthPx,
            isSzMulted  = false,
          )
        }

        val sz = MGbSize(
          widthCells = BlockWidths.max.relSz,
          widthPx = qdBlSzOpt
            .map { qdBl =>
              MGbSidePx(
                sizePx      = qdBl.bounds.width,
                isSzMulted  = true,
              )
            }
            .orElse {
              jdtWidthPxGbSzOpt
            },
          heightPx = qdBlSzOpt
            .map { qdBl =>
              MGbSidePx(
                sizePx      = qdBl.bounds.height,
                isSzMulted  = true,
              )
            }
            // Сейчас p1.heightPx всегда None для qd-контента, но на возможное будущее...
            .orElse {
              for (heightPx <- jdt.props1.heightPx) yield {
                MGbSidePx(
                  sizePx      = heightPx,
                  isSzMulted  = false,
                )
              }
            }
            // Нулевая высота - это нормально, когда высота ещё не измерена через react-measure.
            .getOrElse {
              MGbSidePx(
                sizePx      = 0,
                isSzMulted  = true,
              )
            },
          expandMode = Some( MBlockExpandModes.Wide ),
          widthUnRotatedPx = qdBlSzOpt
            .map { qdBlSz =>
              MGbSidePx(
                sizePx = qdBlSz.client.width,
                isSzMulted = true,
              )
            }
            .orElse {
              jdtWidthPxGbSzOpt
            },
        )
        Some(sz)

      case _ =>
        None
    }
  }


  /** Сборка простой плитки для одной карточки на основе MJdArgs.
    * Не подходит для выдачи, т.к. там много карточек (много разных MJdArgs одновременно).
    *
    * @param jdArgs Аргументы рендера JdR.
    * @return Результат сборки плитки.
    */
  def buildGridFromJdArgs(jdArgs: MJdArgs): MGridBuildResult = {
    val gbArgs = MGridBuildArgs(
      // Тривиальная конвертация списка шаблонов блоков в плоский список одноуровневых MGbBlock.
      itemsExtDatas = for {
        (jdtTree, i) <- jdArgs.data.doc.template
          .subForest
          .iterator
          .zipWithIndex
          .to(LazyList)
        jdt = jdtTree.rootLabel
      } yield {
        Tree.Leaf {
          val jdId = MJdTagId.selPathRev
            .modify(i :: _)(jdArgs.data.doc.tagId)
          MGbBlock(
            jdId    = jdId,
            size    = gbSizeFromJdt(jdId, jdt, jdArgs.jdRuntime, jdArgs.conf),
            jdt     = jdt,
            orderN  = Some(i),
          )
        }
      },
      jdConf          = jdArgs.conf,
      offY            = 0,
      jdtWideSzMults  = jdArgs.jdRuntime.data.jdtWideSzMults,
    )

    GridBuilderUtil.buildGrid( gbArgs )
  }


  /** Сборка пропертисов для запуска рендера CSSGrid.
    *
    * @param conf jd-конфиг рендера.
    * @return Инстанс CssGridProps, пригодный для передачи в CSSGrid(_)(...).
    */
  def mkCssGridArgs(
                     gbRes            : MGridBuildResult,
                     conf             : MJdConf,
                     tagName          : GridComponent_t
                   ): CssGridProps = {
    // Каррируем функцию вне тела new CssGridProps{}, чтобы sjs-компилятор меньше мусорил левыми полями.
    // https://github.com/scala-js/scala-js/issues/2748
    // Это снизит риск ругани react'а на неведомый хлам внутри props.
    val gridLayoutF = GridBuilderUtilJs.stoneCutterLayout( gbRes ) _

    val szMultD = conf.szMult.toDouble

    // Рассчёт расстояния между разными блоками.
    val cellPaddingPx = Math.round(conf.blockPadding.fullBetweenBlocksPx * szMultD).toInt

    val ees = if (!gbRes.nextRender.animate) {
      EnterExitStyle.simple
    } else if (gbRes.nextRender.animFromBottom) {
      // Требуется явный рендер снизу, чтобы юзер заметил, что наверху что-то появилось.
      EnterExitStyle.fromBottom
    } else {

      // Если среди блоков qd_blockless, то react-measure будет глючить на любых масштабируемых эффектах.
      // Поэтому надо оставить только эффекты без изменения размеров.
      val maxAnims = if (gbRes.coords.exists {
        // TODO Opt Надо бы ограничивать анимацию только на НОВЫХ, впервые добавленных в плитку, элементах qd-blockless, а не для любых.
        _.gbBlock.jdt.name ==* MJdTagNames.QD_CONTENT
      }) {
        2
      } else {
        // Только обычные блоки, без qd-blockless. Можно рендерить спокойно любой анимацией.
        7
      }
      new Random().nextInt(maxAnims + 1) match {
        case 0 => EnterExitStyle.fromTop
        case 1 => EnterExitStyle.fromBottom
        case 2 => EnterExitStyle.fromLeftToRight
        // Анимации ниже - трансформируют размер. Несовместимо с react-measure, которая используется для qd-blockless (всё будет глючить):
        case 3 => EnterExitStyle.fromCenter
        case 4 => EnterExitStyle.foldUp
        case 5 => EnterExitStyle.newspaper
        case 6 => EnterExitStyle.simple
        case _ => EnterExitStyle.skew
      }
    }

    new CssGridProps {
      override val duration     = if (gbRes.nextRender.animate) 600 else 0
      override val component    = tagName
      override val columns      = conf.gridColumnsCount
      override val columnWidth  = Math.round(BlockWidths.min.value * szMultD).toInt
      // Плитка и без этого gutter'а работает. Просто выставлено на всякий случай.
      override val gutterWidth  = cellPaddingPx
      override val gutterHeight = cellPaddingPx
      override val layout       = js.defined {
        gridLayoutF
      }
      override val enter        = ees.enter
      override val entered      = ees.entered
      override val exit         = ees.exit
    }
  }

}

