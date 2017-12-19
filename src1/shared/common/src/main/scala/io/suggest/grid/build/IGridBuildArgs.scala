package io.suggest.grid.build

import io.suggest.common.geom.coord.MCoords2di
import io.suggest.common.geom.d2.MSize2di
import io.suggest.jd.MJdConf

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.12.17 14:11
  * Description: Модели для взаимодействия с grid-builder'ом.
  */


/** Модель аргументов для вызова [[GridBuilderUtil]].buildGrid(). */
case class MGridBuildArgs(
                           columnsCount  : Int,
                           itemsExtDatas : TraversableOnce[ItemPropsExt],
                           jdConf        : MJdConf,
                           offY          : Int
                         )


/** Интерфейс для взаимодействия с состоянием плитки.
  * Позволяет зуммировать состояние над-плитки.
  */
trait IGridLevel {

  /** Элементы для обработки на текущем уровне. */
  def itemsExtDatas: TraversableOnce[ItemPropsExt]

  /** Кол-во колонок в текущей проекции. */
  def colsCount: Int

  /** Прочитать состояние указанной колонки */
  def colsInfo(ci: Int): MColumnState

  /** Обновить состояние указанной колонки. */
  def updateColsInfo(i: Int, mcs: MColumnState): Unit

  /** Поиск первой полностью свободной (от края до края) строки.
    * Очевидно, что после этой строки всё свободно.
    *
    * @return Исходный или иной экземпляр [[MWideLine]].
    */
  //def getWideLine(args: MWideLine): MWideLine

}


/** Результат сборки плитки.
  *
  * @param coords Итератор координат блоков.
  * @param gridWh Размеры собранной плитки.
  */
case class MGridBuildResult(
                             coords   : Seq[MCoords2di],
                             gridWh   : MSize2di
                           )