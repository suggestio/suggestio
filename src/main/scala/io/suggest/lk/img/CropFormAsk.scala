package io.suggest.lk.img

import io.suggest.lk.old.Market
import io.suggest.lk.popup.Popup
import io.suggest.sjs.common.controller.IInit
import io.suggest.sjs.common.img.crop.{CropUtil, CropFormRequestT, CropFormResp}
import io.suggest.sjs.common.util.{SjsLogger, ISjsLogger}
import org.scalajs.dom.raw.HTMLElement
import org.scalajs.jquery.{jQuery, JQueryEventObject}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.05.15 10:28
 * Description: Поддержка сборки событий, порождающих запросы за формой кропа.
 */
trait CropFormAsk extends ISjsLogger {

  protected def _cropFormRequester(el: HTMLElement, e: JQueryEventObject): CropFormRequestT = {
    val input = jQuery(e.currentTarget)
      .parent()
      .find("input")
    new CropFormRequestT with SjsLogger {
      override def whInput = input
      override def imgIdInput = input
    }
  }
  
  /** Клик по кнопке кропа на одном из изображений галереи узла. */
  protected def _imgCropClick(el: HTMLElement, e: JQueryEventObject): Unit = {
    e.preventDefault()
    val asker = _cropFormRequester(el, e)
    val fut = asker.ask
    CropUtil.removeAllCropFrames()
    fut.map { resp =>
      _cropFormResponse(asker, resp)
    }
  }

  /** Отрендерить и отобразить попап кропа. */
  protected def _cropFormResponse(asker: CropFormRequestT, resp: CropFormResp): Unit = {
    Popup.appendPopup(resp.body)
    Popup.showPopups("#" + resp.id)
    // Инициализировать кроппер.
    // TODO Нужен нормальный кроппер на scala.js, работающий без многократного перебора всего DOM и принимающий на вход ещё и контейнер поля.
    val imgNameOpt = asker.imgIdInput.attr("name").toOption
    imgNameOpt match {
      case Some(imgName) =>
        Market.img.crop.init(imgName)
      case None =>
        error("Cannot init img cropper, because imgName attr is undefined.")
    }
  }

}


/** Гибрид инициализации и [[CropFormAsk]]. */
trait CropFormAskInit extends CropFormAsk with IInit {

  /** Запуск инициализации текущего модуля. */
  abstract override def init(): Unit = {
    super.init()
    initCropFormAskListener()
  }
  
  /** Нужно заимплементить этот метод, чтобы он слушал необходимые события формы и дергал _imgCropClick(). */
  protected def initCropFormAskListener(): Unit
  
}
