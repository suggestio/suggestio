package io.suggest.jd.render.v

import com.github.dantrain.react.stonecutter.CssGridProps
import io.suggest.ad.blk.BlockWidths
import io.suggest.grid.build.{GridBuildArgs, GridBuilder, ItemPropsExt}
import io.suggest.jd.MJdConf
import io.suggest.jd.tags.JdTag

import scala.scalajs.js
import scalaz.Tree

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.11.17 16:57
  * Description: Утиль для сборки плитки на jd-карточках.
  */
class JdGridUtil(
                  gridBuilder: GridBuilder
                ) {

  /** Сборка пропертисов для запуска рендера CSSGrid.
    *
    * @param jds Jd-теги в исходном порядке.
    * @param conf jd-конфиг рендера.
    * @param gridBuildArgsF Функция сборки инстанса GridBuildArgs.
    *                       Создана для возможности проброса каких-либо аргументов напрямую в билдер.
    * @return Инстанс CssGridProps, пригодный для передачи в CSSGrid(_)(...).
    */
  def mkCssGridArgs(
                     jds: Seq[Tree[JdTag]],
                     conf: MJdConf,
                     gridBuildArgsF: TraversableOnce[ItemPropsExt] => GridBuildArgs
                   ): CssGridProps = {
    // Собрать аргументы для вызова layout-функции grid-builder'а.
    val gridBuildArgs = gridBuildArgsF(
      jds
        .iterator
        .flatMap(_.rootLabel.props1.bm)
        .map { bm =>
          ItemPropsExt(
            blockMeta = bm
          )
        }
    )

    // Каррируем функцию вне тела new CssGridProps{}, чтобы sjs-компилятор меньше мусорил левыми полями.
    // https://github.com/scala-js/scala-js/issues/2748
    // Это снизит риск ругани react'а на неведомый хлам внутри props.
    val gridLayoutF = gridBuilder.stoneCutterLayout( gridBuildArgs ) _

    val szMultD = conf.szMult.toDouble

    // Рассчёт расстояния между разными блоками.
    val cellPaddingPx = Math.round(conf.blockPadding.value * szMultD).toInt

    val blkSzMultD = conf.blkSzMult.toDouble

    new CssGridProps {
      override val duration     = 600
      override val columns      = conf.gridColumnsCount
      override val columnWidth  = Math.round(BlockWidths.min.value * blkSzMultD).toInt
      // Плитка и без этого gutter'а работает. Просто выставлено на всякий случай, т.к. в коде модулей grid'а это дело используется.
      override val gutterWidth  = cellPaddingPx
      override val gutterHeight = cellPaddingPx
      override val layout       = js.defined {
        gridLayoutF
      }
    }
  }

}
