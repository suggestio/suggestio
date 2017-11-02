package io.suggest.ad.edit.m

import com.github.dominictobias.react.image.crop.{PercentCrop, PixelCrop}
import io.suggest.ad.blk.{IBlockSize, IBlockSizes}
import io.suggest.color.MColorData
import io.suggest.common.MHand
import io.suggest.common.html.HtmlConstants
import io.suggest.crypto.hash.MHash
import io.suggest.dev.MSzMult
import io.suggest.file.up.MUploadResp
import io.suggest.jd.tags.JdTag
import io.suggest.model.n2.edge.EdgeUid_t
import io.suggest.scalaz.StringValidationNel
import io.suggest.spa.DAction
import io.suggest.text.StringUtil
import io.suggest.url.MHostUrl
import org.scalajs.dom.{Blob, File}

import scala.util.Try
import scalaz.{Tree, ValidationNel}

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
case class ColorChanged(mcd: MColorData, isCompleted: Boolean, forceTransform: Boolean = false) extends ILkEditAction

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


/** Экшен запуска хеширования файла в указанном эдже.
  * Хэширование с последующим аплоадом может запускаться из разных мест: quill, strip editor, etc. */
case class FileHashStart(edgeUid: EdgeUid_t, blobUrl: String) extends ILkEditAction

/** Завершение асинхронного хэширования файла. */
case class FileHashRes(edgeUid: EdgeUid_t, blobUrl: String, hash: MHash, hex: Try[String]) extends ILkEditAction


/** Завершён запрос подготовки сервера к аплоаду файла. */
case class PrepUploadResp(tryRes: Try[MUploadResp], edgeUid_t: EdgeUid_t, blobUrl: String) extends ILkEditAction
/** Завершён запрос заливки файла на сервер. */
case class UploadRes(tryRes: Try[MUploadResp], edgeUid_t: EdgeUid_t, blobUrl: String, hostUrl: MHostUrl) extends ILkEditAction
// TODO Объеденить оба case class'а?


/** Экшен для запуска какой-то реакции на событие появления новой гистограммы в карте оных.
  * Испускается из PictureAh, и попадает в DocEditAh для выставления bgColor на jd-элементах,
  * связанных с соответствующей узлу картинкой.
  */
case class HandleNewHistogramInstalled(nodeId: String) extends ILkEditAction


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


/** Экшен сохранения документа. */
case object SaveAd extends ILkEditAction

/** Результат запроса сохранения карточки на сервер. */
case class SaveAdResp(timestamp: Long, tryResp: Try[MAdEditFormInit]) extends ILkEditAction


/** Выставление галочки широкоформатного рендера блока. */
case class StripStretchAcross(isWide: Boolean) extends ILkEditAction

