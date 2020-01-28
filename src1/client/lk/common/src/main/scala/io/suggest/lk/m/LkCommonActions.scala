package io.suggest.lk.m

import com.github.dominictobias.react.image.crop.{PercentCrop, PixelCrop}
import diode.FastEq
import io.suggest.color.{IColorPickerMarker, MColorData}
import io.suggest.common.geom.coord.MCoords2di
import io.suggest.common.geom.d2.{ISize2di, MSize2di}
import io.suggest.crypto.hash.MHash
import io.suggest.file.up.MUploadResp
import io.suggest.form.MFormResourceKey
import io.suggest.lk.m.captcha.MCaptchaData
import io.suggest.n2.edge.EdgeUid_t
import io.suggest.proto.http.model.{HttpRespHolder, HttpRespMapped}
import io.suggest.spa.DAction
import io.suggest.url.MHostUrl
import org.scalajs.dom.File

import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.03.17 17:35
  * Description: Diode-экшены для нужд lk-common-sjs.
  */

sealed trait ILkCommonAction extends DAction
sealed trait ILkCommonPopupCloseAction extends ILkCommonAction
/** Трейт-маркер закрытия "бесполезного" попапа, т.к. не содержащего ожидаемый контект, а какую-то малополезную инфу. */
sealed trait ILkCommonUselessPopupCloseAction extends ILkCommonPopupCloseAction

/** Экшен закрытия попапа с инфой по узлу. */
case object NodeInfoPopupClose extends ILkCommonPopupCloseAction

/** Клик по кнопке закрытия окна-попапа ожидания. */
case object PleaseWaitPopupCloseClick extends ILkCommonUselessPopupCloseAction

/** Экшен закрытия попапа с ошибкой. */
case object ErrorPopupCloseClick extends ILkCommonUselessPopupCloseAction

/** Экшен закрытия вообще всех попапов. */
case object CloseAllPopups extends ILkCommonPopupCloseAction


/** Клик по кнопке ОК в попапе подтверждения удаления чего-то. */
case object DeleteConfirmPopupOk extends ILkCommonAction

/** Клик по кнопке отмены в попапе подтверждения удаления чего-либо. */
case object DeleteConfirmPopupCancel extends ILkCommonPopupCloseAction

/** Клик по заголовку slide-блока. */
case class SlideBlockClick(key: String) extends ILkCommonAction

/** Изменился цвет в color picker'е.
  * @param isCompleted Окончательное выставление цвета?
  *                    Если false, то юзер пока ещё выбирает.
  * @param forceTransform Если цвет пришёл из ColorSuggest или иного источника, то выставляется true.
  * @param marker Маркер для мимикрирования под какой-то color-picker.
  */
case class ColorChanged(mcd: MColorData,
                        isCompleted: Boolean,
                        forceTransform: Boolean = false,
                        marker: Option[IColorPickerMarker] = None,
                       ) extends ILkCommonAction

/** Клик на "цвете" для выбора цвета. */
case class ColorBtnClick(vpXy: MCoords2di, marker: Option[IColorPickerMarker]) extends ILkCommonAction

/** Изменилось состояние галочки напротив color picker'а для выставления bgColor тега. */
case class ColorCheckboxChange(isEnabled: Boolean, marker: Option[IColorPickerMarker]) extends ILkCommonAction


/** Клик просто где-то в body. */
case object DocBodyClick extends ILkCommonAction



/** Изменилось file-поле выбора картинки. */
case class UploadFile(files: collection.Seq[File], resKey: MFormResourceKey) extends ILkCommonAction


sealed trait IPictureCropAction extends ILkCommonAction

/** Сигнал к отрытию попапа редактирования изображения. */
case class CropOpen(resKey: MFormResourceKey, cropContSz: ISize2di) extends IPictureCropAction
object CropOpen {
  implicit object CropOpenFastEq extends FastEq[CropOpen] {
    override def eqv(a: CropOpen, b: CropOpen): Boolean = {
      MFormResourceKey.MFormImgKeyFastEq.eqv( a.resKey, b.resKey ) &&
        (a.cropContSz eq b.cropContSz)
    }
  }
}

/** Сигнал к закрытию попапа кропа изображения. */
case class CropCancel(resKey: MFormResourceKey) extends IPictureCropAction
/** Измение кропа текущего изображения. */
case class CropChanged(percentCrop: PercentCrop, pixelCrop: PixelCrop, resKey: MFormResourceKey) extends IPictureCropAction
/** Подтверждение сохранения кропа. */
case class CropSave(resKey: MFormResourceKey) extends IPictureCropAction



/** Экшен запуска хеширования файла в указанном эдже.
  * Хэширование с последующим аплоадом может запускаться из разных мест: quill, strip editor, etc. */
case class FileHashStart(edgeUid: EdgeUid_t, blobUrl: String) extends ILkCommonAction

/** Завершение асинхронного хэширования файла. */
case class FileHashRes(src: FileHashStart, hash: MHash, hex: Try[String]) extends ILkCommonAction

/** Завершён запрос подготовки сервера к аплоаду файла. */
case class PrepUploadResp(tryRes: Try[MUploadResp], src: FileHashStart) extends ILkCommonAction

/** Запущен (но ещё не исполнен) запрос фактической закачки файла на сервер.
  * Запрос делается в два шага, чтобы пробросить в состояние request-holder для возможности доступа к abort/progress-функциям.
  */
case class UploadReqStarted(respHolder: HttpRespMapped[MUploadResp], src: FileHashStart, hostUrl: MHostUrl) extends ILkCommonAction

/** Завершён запрос заливки файла на сервер. */
case class UploadRes(tryRes: Try[MUploadResp], src: FileHashStart, hostUrl: MHostUrl) extends ILkCommonAction
// TODO Объеденить оба case class'а?


/** Экшен для запуска какой-то реакции на событие появления новой гистограммы в карте оных.
  * Испускается из PictureAh, и попадает в DocEditAh для выставления bgColor на jd-элементах,
  * связанных с соответствующей узлу картинкой.
  */
case class HandleNewHistogramInstalled(nodeId: String) extends ILkCommonAction

/** Команда принудительной прочистки эджей, не исходит от юзера, а является продуктом работы других контроллеров. */
case object PurgeUnusedEdges extends ILkCommonAction


/** Экшен сохранения документа. */
case object Save extends ILkCommonAction


/** Запустить инициализации капчи. */
case object CaptchaInit extends ILkCommonAction
/** Возвращается результат (пере-)инициализации капчи. */
case class CaptchaInitRes(tryResp: Try[MCaptchaData], timeStampMs: Long) extends ILkCommonAction

case object CaptchaHide extends ILkCommonAction

/** Происходит ввода значения капчи.
  * @param typed Введенное пользоватем значение.
  */
case class CaptchaTyped(typed: String) extends ILkCommonAction
/** Потеря фокуса в поле ввода капчи. */
case object CaptchaInputBlur extends ILkCommonAction


/** Редактирование поля смс-кода. */
case class SmsCodeSet( smsCode: String ) extends ILkCommonAction
/** Расфокусировка поля смс-кода. */
case object SmsCodeBlur extends ILkCommonAction


/** Изменение состояния флага конфигурации isTouchDev. */
case class TouchDevSet(isTouchDev: Boolean) extends ILkCommonAction


/** Уведомить систему о ширине и длине загруженной картинки. */
case class SetImgWh(edgeUid: EdgeUid_t, wh: MSize2di) extends ILkCommonAction
