package io.suggest.jd.render.u

import com.github.dantrain.react.stonecutter.{CssGridProps, EnterExitStyle, GridComponent_t}
import io.suggest.ad.blk.BlockWidths
import io.suggest.grid.build.{GridBuilderJs, MGridBuildResult}
import io.suggest.jd.MJdConf

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.11.17 16:57
  * Description: Утиль для сборки плитки на jd-карточках.
  */
class JdGridUtil(
                  gridBuilder: GridBuilderJs
                ) {

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
    val gridLayoutF = gridBuilder.stoneCutterLayout( gbRes ) _

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
