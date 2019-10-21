package io.suggest.grid

import com.github.dantrain.react.stonecutter.{CssGridProps, EnterExitStyle, GridComponent_t, ItemProps, LayoutFunRes, PropsCommon}
import io.suggest.ad.blk.{BlockMeta, BlockWidths, MBlockExpandModes}
import io.suggest.grid.build.{GridBuilderUtil, MGbBlock, MGbSize, MGridBuildArgs, MGridBuildResult}
import io.suggest.jd.{MJdConf, MJdTagId}
import io.suggest.jd.render.m.{MJdArgs, MJdRuntime}
import io.suggest.jd.tags.JdTag
import scalaz.Tree

import scala.scalajs.js
import scala.scalajs.js.JSConverters._

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
      .map { mcoord =>
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


  /** Сборка MGbSize на основе BlockMeta обычного блока. */
  def gbSizeFromBm(bm: BlockMeta): MGbSize = {
    MGbSize(
      widthCells  = bm.w.relSz,
      heightPx    = bm.height,
      expandMode  = bm.expandMode,
    )
  }


  /** Сборка MGbSize из тега.
    *
    * @param jdt jd-тег.
    * @param jdRuntime Рантайм, нужен для qd-blockless.
    * @param jdConf Конфиг рендера. Нужен для qd-blockless.
    * @return MGbSize, описывающий размеры рендерящегося блока.
    */
  def gbSizeFromJdt(jdId: MJdTagId, jdt: JdTag, jdRuntime: MJdRuntime, jdConf: MJdConf): MGbSize = {
    jdt.props1.bm
      .map(gbSizeFromBm)
      .getOrElse {
        // Внеблоковые элементы. Надо узнать их высоту.
        MGbSize(
          widthCells = BlockWidths.max.relSz,
          heightPx   = (for {
            qdBlSzPot   <- jdRuntime.data.qdBlockLess.get(jdId)
            qdBlSz      <- qdBlSzPot.toOption
          } yield {
            qdBlSz.bounds.height
          })
            .getOrElse(0),
          expandMode = Some( MBlockExpandModes.Wide ),
        )
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
          .zipWithIndex
        jdt = jdtTree.rootLabel
      } yield {
        val jdId = MJdTagId.selPathRev.modify(i :: _)(jdArgs.data.doc.jdId)
        Tree.Leaf(
          MGbBlock(
            size    = gbSizeFromJdt(jdId, jdt, jdArgs.jdRuntime, jdArgs.conf),
            nodeId  = None,
            jdtOpt  = Some(jdt),
            orderN  = Some(i),
          )
        )
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

    val ees = EnterExitStyle.fromTop

    new CssGridProps {
      override val duration     = 600
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

