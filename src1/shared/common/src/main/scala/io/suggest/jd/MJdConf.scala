package io.suggest.jd

import io.suggest.ad.blk.{BlockPadding, IBlockSize}
import io.suggest.dev.MSzMult
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.08.17 14:59
  * Description: Модель общей конфигурации рендерера.
  */

object MJdConf {

  implicit def univEq: UnivEq[MJdConf] = UnivEq.force

}


/** Класс модели общей конфигурации рендеринга.
  *
  * @param isEdit Рендерить для редактора карточки.
  *                 Это означает, например, что некоторые элементы становятся перемещаемыми
  *                 и генерят соотв.события.
  * @param szMult Мультипликатор размера карточки.
  *               Его можно переопределить на уровне каждого конкретного блока.
  * @param oneJdGrid Использовать grid-механизмы для рендера блоков одного jd-документа?
  * @param blockPadding Настройка интервала между блоками плитки. Пока не реализована нормально.
  * @param gridColumnsCount Кол-во колонок в плитке.
  */
case class MJdConf(
                    isEdit              : Boolean,
                    szMult              : MSzMult,
                    oneJdGrid           : Boolean,
                    blockPadding        : BlockPadding,
                    gridColumnsCount    : Int
                  ) {

  def withIsEdit(isEdit: Boolean)           = copy(isEdit = isEdit)
  def withSzMult(szMult: MSzMult)           = copy(szMult = szMult)
  def withOneJdGrid(oneJdGrid: Boolean)     = copy(oneJdGrid = oneJdGrid)
  def withBlockPadding(blockPadding: BlockPadding) = copy(blockPadding = blockPadding)
  def withGridColumnsCount(gridColumnsCount: Int)  = copy(gridColumnsCount = gridColumnsCount)

  /** Рассчитать коэфф.масштабирования блоков плитки. */
  val blkSzMultOpt = IBlockSize.szMultPaddedOpt(szMult, blockPadding)
  def blkSzMult = blkSzMultOpt.getOrElse( szMult )

}
