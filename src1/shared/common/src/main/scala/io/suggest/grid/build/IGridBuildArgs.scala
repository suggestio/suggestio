package io.suggest.grid.build

import io.suggest.ad.blk.BlockMeta
import io.suggest.common.geom.coord.MCoords2di
import io.suggest.common.geom.d2.MSize2di
import io.suggest.jd.MJdConf

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.12.17 14:11
  * Description: Модели для взаимодействия с grid-builder'ом.
  */


/** Модель аргументов для вызова [[GridBuilderUtil]].buildGrid().
  *
  * @param columnsCount Кол-во колонок плитки.
  * @param itemsExtDatas Данные по item'мам рассчитываемой плитки.
  * @param jdConf Конфигурация рендера.
  * @param offY Сдвиг по Y.
  * @param iter2coordsF Функция сборки результата рассчёта координат.
  *                     На вход получает итератор координат, и должна НЕ лениво собрать возвращаемый результат.
  *                     Вынести за пределы args пока нельзя, потому что выполнение неленивой функции имеет сайд-эффекты
  *                     при финальном рассчётё плитки (ширина/высота плитки).
  * @tparam Coords_t Тип результата рассчёта координат, возвращаемый из iter2coordsF().
  */
case class MGridBuildArgs[Coords_t](
                                     columnsCount  : Int,
                                     itemsExtDatas : List[IGbBlockPayload],
                                     jdConf        : MJdConf,
                                     offY          : Int,
                                     iter2coordsF  : Iterator[MCoords2di] => Coords_t
                                   )


/** Интерфейс для контейнеров с вариантами элементов плитки. */
sealed trait IGbBlockPayload {
  def isBlock: Boolean
  def isSubBlocks: Boolean = !isBlock
  def fold[T](blockF: MGbBlock => T, subBlocksF: MGbSubItems => T): T


  /** Вернуть параметры первого блока. */
  def firstBlockMeta: BlockMeta = {
    fold(_.bm, _.subItems.head.firstBlockMeta)
  }

}

/** Контейнер для одного блока.
  * wide-блоки могут передать здесь инфу о фоновой картинке.
  *
  * @param bm Описание одного блока.
  * @param wideBgSz Размер широкой фоновой картинки.
  * @param orderN внутренний порядковый номер, заполняется и используется внутри [[GridBuilderUtil]].
  */
case class MGbBlock(
                      bm                            : BlockMeta,
                      wideBgSz                      : Option[MSize2di]  = None,
                      private[build] val orderN     : Option[Int]       = None
                   )
  extends IGbBlockPayload {

  override def isBlock = true

  override def fold[T](blockF: MGbBlock => T, subBlocksF: MGbSubItems => T): T = {
    blockF(this)
  }

}

/** Контейнер для под-блоков. */
case class MGbSubItems(
                        subItems: List[IGbBlockPayload]
                      )
  extends IGbBlockPayload {

  override def isBlock = false

  override def fold[T](blockF: MGbBlock => T, subBlocksF: MGbSubItems => T): T = {
    subBlocksF(this)
  }

}


/** Результат сборки плитки.
  *
  * @param coords Итератор координат блоков.
  * @param gridWh Размеры собранной плитки.
  */
case class MGridBuildResult[Coords_t](
                                       coords   : Coords_t,
                                       gridWh   : MSize2di
                                     )