package io.suggest.ad.edit.m

import io.suggest.ad.blk.{IBlockSize, IBlockSizes}
import io.suggest.common.MHand
import io.suggest.sjs.common.spa.DAction

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.08.17 21:39
  * Description: Экшены редактора карточки.
  */
sealed trait ILkEditAction extends DAction


/** Клик по какой-то кнопке управления размером блока.
  *
  * @param model Модель, указывающая на ширину или высоту блока?
  * @param direction Направление: увеличить или уменьшить.
  */
case class BlockSizeBtnClick(model: IBlockSizes[_ <: IBlockSize], direction: MHand) extends ILkEditAction


/** Экшен клика по кнопке удаления текущего выделенного strip'а. */
case object DeleteStrip extends ILkEditAction

/** Экшен отказа от удаления блока. */
case object DeleteCancel extends ILkEditAction
