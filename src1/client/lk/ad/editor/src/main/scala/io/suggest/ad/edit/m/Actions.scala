package io.suggest.ad.edit.m

import io.suggest.ad.blk.{IBlockSize, IBlockSizes, MBlockExpandMode}
import io.suggest.common.MHand
import io.suggest.common.html.HtmlConstants
import io.suggest.dev.MSzMult
import io.suggest.jd.tags.JdTag
import io.suggest.scalaz.StringValidationNel
import io.suggest.spa.DAction
import io.suggest.text.StringUtil
import org.scalajs.dom.Blob

import scala.util.Try
import scalaz.Tree

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.08.17 21:39
  * Description: Экшены редактора карточки.
  */
sealed trait ILkEditAction extends DAction


trait IBlockAction extends ILkEditAction

/** Клик по какой-то кнопке управления размером блока.
  *
  * @param model Модель, указывающая на ширину или высоту блока?
  * @param direction Направление: увеличить или уменьшить.
  */
case class BlockSizeBtnClick(model: IBlockSizes[_ <: IBlockSize], value: Int) extends IBlockAction


sealed trait IStripDeleteAction extends IBlockAction
/** Экшен клика по кнопке удаления текущего выделенного strip'а.
  *
  * @param confirmed true - Юзер подтверждает удаление.
  *                  false -- Юзер первый раз нажал на "удалить".
  */
case class StripDelete(confirmed: Boolean) extends IStripDeleteAction

/** Экшен отказа от удаления блока. */
case object StripDeleteCancel extends IStripDeleteAction


sealed trait IAddAction extends ILkEditAction
/** Клик по кнопке добавления контента. */
case object AddContentClick extends IAddAction
/** Клик по кнопке добавления стрипа. */
case object AddStripClick extends IAddAction


/** Сообщение о завершении фоновой конвертации из base64 data-URL в натуральный блоб. */
case class B64toBlobDone(b64Url: String, blob: Blob) extends ILkEditAction {
  override def toString = s"$productPrefix(${StringUtil.strLimitLen(b64Url, 16, HtmlConstants.ELLIPSIS)}${HtmlConstants.COMMA}$blob)"
}


/** Сигнал изменения масштаба рендера. */
case class SetScale(szMult: MSzMult) extends ILkEditAction


/** Сигнал вертикального скроллинга. */
case class HandleVScroll(y: Double) extends ILkEditAction


/** Сигнал о любом изменении документа.
  * Это обычно результат мониторинга документа на уровне circuit.
  */
case object JdDocChanged extends ILkEditAction

/** Сигнал о завершении валидации. */
case class JdVldResult( vldRes: StringValidationNel[Tree[JdTag]] ) extends ILkEditAction


/** Результат запроса сохранения карточки на сервер. */
case class SaveAdResp(timestamp: Long, tryResp: Try[MAdEditFormInit]) extends ILkEditAction


/** Выставление режима широкоформатного рендера блока. */
case class BlockExpand( expandMode: Option[MBlockExpandMode] ) extends ILkEditAction


/** Сигнал изменения флага текущего стрипа. */
case class MainStripChange(isMain: Boolean) extends ILkEditAction
/** Сигнал к сокрытию/отображению главных стрипов. */
case class ShowMainStrips(showing: Boolean) extends ILkEditAction


/** Клик по кнопке удаления рекламной карточки. Приводит к рендеру попапа подтверждения. */
case object DeleteAdClick extends ILkEditAction
/** Ответ сервера на запрос удаления рекламной карточки. */
case class DeleteAdResp( tryResp: Try[String] ) extends ILkEditAction


/** Переключательство галочки вращения элемента. */
case class RotateSet( degrees: Option[Int] ) extends ILkEditAction

/** Редактирование межстрочного интервала. */
case class LineHeightSet( lineHeight: Option[Int] ) extends ILkEditAction

/** Управление тенью текста. */
case class SetTextShadowEnabled(enabled: Boolean) extends ILkEditAction

/** Выставление горизонтального сдвига тени. */
case class SetHorizOffTextShadow(offset: Int) extends ILkEditAction
/** Выставление вертикального сдвига тени. */
case class SetVertOffTextShadow(offset: Int) extends ILkEditAction
case class SetBlurTextShadow(blur: Int) extends ILkEditAction
