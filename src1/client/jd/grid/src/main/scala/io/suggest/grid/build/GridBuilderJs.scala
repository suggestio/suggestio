package io.suggest.grid.build

import com.github.dantrain.react.stonecutter.{ItemProps, LayoutFunRes, PropsCommon}

import scala.scalajs.js
import scala.scalajs.js.JSConverters._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.11.17 14:46
  * Description: JS-модуль для связывания GridBuilder'а и js-компонентов плитки.
  */
class GridBuilderJs {

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

}

