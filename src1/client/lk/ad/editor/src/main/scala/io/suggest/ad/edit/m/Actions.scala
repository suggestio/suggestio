package io.suggest.ad.edit.m

import com.github.dominictobias.react.image.crop.{PercentCrop, PixelCrop}
import io.suggest.ad.blk.{IBlockSize, IBlockSizes}
import io.suggest.common.MHand
import io.suggest.common.geom.d2.MSize2di
import io.suggest.common.html.HtmlConstants
import io.suggest.model.n2.edge.EdgeUid_t
import io.suggest.model.n2.node.meta.colors.MColorData
import io.suggest.sjs.common.spa.DAction
import io.suggest.text.StringUtil
import org.scalajs.dom.{Blob, File}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.08.17 21:39
  * Description: Экшены редактора карточки.
  */
sealed trait ILkEditAction extends DAction


trait IStripAction extends ILkEditAction

/** Клик по какой-то кнопке управления размером блока.
  *
  * @param model Модель, указывающая на ширину или высоту блока?
  * @param direction Направление: увеличить или уменьшить.
  */
case class BlockSizeBtnClick(model: IBlockSizes[_ <: IBlockSize], direction: MHand) extends IStripAction


sealed trait IStripDeleteAction extends IStripAction
/** Экшен клика по кнопке удаления текущего выделенного strip'а.
  *
  * @param confirmed true - Юзер подтверждает удаление.
  *                  false -- Юзер первый раз нажал на "удалить".
  */
case class StripDelete(confirmed: Boolean) extends IStripDeleteAction

/** Экшен отказа от удаления блока. */
case object StripDeleteCancel extends IStripDeleteAction


sealed trait IAddAction extends ILkEditAction
/** Клик по кнопке добавления (открытия формы добавления). */
case object AddBtnClick extends IAddAction
/** Клик по кнопке добавления контента. */
case object AddContentClick extends IAddAction
/** Клик по кнопке добавления стрипа. */
case object AddStripClick extends IAddAction
/** Клик по кнопке отмены добавления чего-либо. */
case object AddCancelClick extends IAddAction


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


sealed trait IPictureCropAction extends ILkEditAction

/** Сигнал к отрытию попапа редактирования изображения. */
case object CropOpen extends IPictureCropAction
/** Сигнал к закрытию попапа кропа изображения. */
case object CropCancel extends IPictureCropAction
/** Измение кропа текущего изображения. */
case class CropChanged(percentCrop: PercentCrop, pixelCrop: PixelCrop) extends IPictureCropAction
/** Подтверждение сохранения кропа. */
case object CropSave extends IPictureCropAction


/** Сообщение о завершении фоновой конвертации из base64 data-URL в натуральный блоб. */
case class B64toBlobDone(b64Url: String, blob: Blob) extends ILkEditAction {
  override def toString = s"$productPrefix(${StringUtil.strLimitLen(b64Url, 16, HtmlConstants.ELLIPSIS)}${HtmlConstants.COMMA}$blob)"
}


/** Команда принудительной прочистки эджей, не исходит от юзера, а является продуктом работы других контроллеров. */
case object PurgeUnusedEdges extends ILkEditAction
