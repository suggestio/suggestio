package io.suggest.grid.build

import com.github.dantrain.react.stonecutter.{ItemProps, LayoutFunRes, PropsCommon}
import io.suggest.common.geom.d2.MSize2di
import io.suggest.jd.MJdConf
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx

import scala.concurrent.Future
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
  def stoneCutterLayout(args: MGridBuildArgsJs)(flatJsItems: js.Array[ItemProps], props: PropsCommon): LayoutFunRes = {
    val buildRes = GridBuilderUtil.buildGrid(
      MGridBuildArgs(
        columnsCount  = props.columns,
        itemsExtDatas = args.itemsExtDatas,
        jdConf        = args.jdConf,
        offY          = args.offY
      )
    )

    // Сконвертить координаты в js.Array-представление, понятное для stonecutter.
    val coordPositions = buildRes.coords
      .map { mcoord =>
        js.Array( mcoord.x, mcoord.y )
      }
      .toJSArray

    // Помимо координат, надо вычислить итоговые размеры плитки.
    val res = new LayoutFunRes {
      override val positions  = coordPositions
      override val gridHeight = buildRes.gridWh.height
      override val gridWidth  = buildRes.gridWh.width
    }

    // Передать результат сборки плитки в side-effect-функцию, если она задана.
    for (notifyF <- args.onLayout) {
      Future {
        // На раннем этапе нужны были только фактические размеры плитки.
        // Поэтому собираем отдельный безопасный инстанс с этими размерами и отправляем в функцию.
        notifyF( buildRes.gridWh )
      }
    }

    res
  }

}


/** Модель доп.аргументов вызова функции, которые мы передаём вне react.
  *
  * В изначальной реализации была пародия на monkey-patching, что вызывало негодование
  * со стороны react, и добавляло неопределённости относительно надёжности и долговечности такого решения.
  *
  * @param itemsExtDatas Итератор или коллекция доп.данных для исходного массива ItemProps, длина должна совпадать.
  * @param onLayout Callback для сайд-эффектов по итогам рассчёта плитки.
  * @param offY Сдвиг сетки по вертикали, если требуется.
  */
case class MGridBuildArgsJs(
                             itemsExtDatas : TraversableOnce[ItemPropsExt],
                             jdConf        : MJdConf,
                             onLayout      : Option[MSize2di => _] = None,
                             offY          : Int = 0
                           )
