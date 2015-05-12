package io.suggest.lk.ad.form.init

import io.suggest.ad.form.AdFormConstants
import io.suggest.img.ImgConstants
import io.suggest.img.crop.CropConstants
import io.suggest.lk.ad.form.input.AdFormWhInput
import io.suggest.lk.img.CropFormAskInit
import io.suggest.ad.form.AdFormConstants._
import io.suggest.lk.old.Market
import io.suggest.sjs.common.img.crop.{CropFormResp, CropFormRequestT}
import io.suggest.sjs.common.util.{SjsLogger, TouchUtil}
import org.scalajs.dom.raw.HTMLElement
import org.scalajs.jquery.{JQueryEventObject, jQuery}

import scala.scalajs.js.ThisFunction

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.05.15 10:36
 * Description: Инициализация запуска кропа картинок в форме рекламной карточки.
 */
trait AdFormCropFormAskInit extends CropFormAskInit {

  override protected def _cropFormRequester(el: HTMLElement, e: JQueryEventObject): CropFormRequestT = {
    val _imgIdInput = jQuery("#" + AdFormConstants.BG_IMG_CONTAINER_ID + " input." + ImgConstants.JS_IMG_ID_CLASS)
    new CropFormRequestT with SjsLogger with AdFormWhInput {
      override def imgIdInput = _imgIdInput
    }
  }

  /** Нужно заимплементить этот метод, чтобы он слушал необходимые события формы и дергал _imgCropClick(). */
  override protected def initCropFormAskListener(): Unit = {
    jQuery("#" + BG_IMG_CONTROLS_CONTAINER_ID + " ." + CropConstants.CROP_IMAGE_BTN_CLASS)
      .on(TouchUtil.clickEvtName, {
        (that: HTMLElement, e: JQueryEventObject) =>
          _imgCropClick(that, e)
      }: ThisFunction)
  }

}
