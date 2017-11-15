package io.suggest.jd

import diode.FastEq
import io.suggest.ad.blk.{BlockPadding, BlockPaddings, IBlockSize}
import io.suggest.dev.MSzMult
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.08.17 14:59
  * Description: Модель общей конфигурации рендерера.
  */

object MJdConf {

  /** Поддержка FastEq для инстансов [[MJdConf]]. */
  implicit object MJdConfFastEq extends FastEq[MJdConf] {
    override def eqv(a: MJdConf, b: MJdConf): Boolean = {
      (a.isEdit ==* b.isEdit) &&
        (a.szMult ===* b.szMult) &&
        (a.blockPadding ===* b.blockPadding) &&
        (a.gridColumnsCount ==* b.gridColumnsCount)
    }
  }

  implicit def univEq: UnivEq[MJdConf] = UnivEq.force

}


/** Класс модели общей конфигурации рендеринга.
  *
  * @param isEdit Рендерить для редактора карточки.
  *                 Это означает, например, что некоторые элементы становятся перемещаемыми
  *                 и генерят соотв.события.
  * @param szMult Мультипликатор размера карточки.
  *               Его можно переопределить на уровне каждого конкретного блока.
  * @param blockPadding Настройка интервала между блоками плитки. Пока не реализована нормально.
  * @param gridColumnsCount Кол-во колонок в плитке.
  */
case class MJdConf(
                    isEdit              : Boolean,
                    szMult              : MSzMult,
                    blockPadding        : BlockPadding = BlockPaddings.default,
                    gridColumnsCount    : Int
                  ) {

  def withIsEdit(isEdit: Boolean)           = copy(isEdit = isEdit)
  def withSzMult(szMult: MSzMult)           = copy(szMult = szMult)
  def withBlockPadding(blockPadding: BlockPadding) = copy(blockPadding = blockPadding)
  def withGridColumnsCount(gridColumnsCount: Int)  = copy(gridColumnsCount = gridColumnsCount)

  /** Рассчитать коэфф.масштабирования блоков плитки. */
  val blkSzMultOpt = IBlockSize.szMultPaddedOpt(szMult, blockPadding)
  def blkSzMult = blkSzMultOpt.getOrElse( szMult )

}

