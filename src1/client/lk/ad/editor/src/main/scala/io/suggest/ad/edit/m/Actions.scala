package io.suggest.ad.edit.m

import io.suggest.ad.blk.{IBlockSize, IBlockSizes}
import io.suggest.common.MHand
import io.suggest.model.n2.node.meta.colors.MColorData
import io.suggest.sjs.common.spa.DAction
import org.scalajs.dom.File

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


/** Экшен клика по кнопке удаления текущего выделенного strip'а.
  *
  * @param confirmed true - Юзер подтверждает удаление.
  *                  false -- Юзер первый раз нажал на "удалить".
  */
case class StripDelete(confirmed: Boolean) extends ILkEditAction

/** Экшен отказа от удаления блока. */
case object StripDeleteCancel extends ILkEditAction


/** Клик по кнопке добавления (открытия формы добавления). */
case object AddBtnClick extends ILkEditAction
/** Клик по кнопке добавления контента. */
case object AddContentClick extends ILkEditAction
/** Клик по кнопке добавления стрипа. */
case object AddStripClick extends ILkEditAction
/** Клик по кнопке отмены добавления чего-либо. */
case object AddCancelClick extends ILkEditAction


/** Изменилось состояние галочки напротив color picker'а для выставления bgColor тега. */
case class ColorCheckboxChange(isEnabled: Boolean) extends ILkEditAction

/** Изменился цвет в bg color picker'е.
  * @param isCompleted Окончательное выставление цвета?
  *                    Если false, то юзер пока ещё выбирает.
  */
case class ColorChanged(mcd: MColorData, isCompleted: Boolean) extends ILkEditAction

/** Клик на "цвете" для выбора цвета. */
case object ColorBtnClick extends ILkEditAction

/** Клик просто где-то в body. */
case object DocBodyClick extends ILkEditAction


/** Изменилось file-поле выбора картинки. */
case class PictureFileChanged(files: Seq[File]) extends ILkEditAction
