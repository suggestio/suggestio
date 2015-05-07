package io.suggest.lk.adn.edit.init

import io.suggest.lk.old.Market
import io.suggest.lk.popup.Popup
import io.suggest.sjs.common.controller.IInit
import io.suggest.sjs.common.img.crop.{CropFormResp, CropUtil, CropFormRequestT}
import io.suggest.sjs.common.util.{ISjsLogger, SjsLogger}
import org.scalajs.dom.raw.HTMLElement
import org.scalajs.jquery.{JQueryEventObject, jQuery}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import io.suggest.adn.edit.NodeEditConstants._
import io.suggest.img.crop.CropConstants._

import scala.scalajs.js.ThisFunction

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.05.15 15:51
 * Description: Инициализация поддержки галереи в редакторе узла.
 */

trait NodeGalleryInit extends IInit with ISjsLogger {

  /** Инициализация всей формы. */
  abstract override def init(): Unit = {
    super.init()
    initNodeGallery()
  }

  /** Инициализация js для подредактора галереи. */
  def initNodeGallery(): Unit = {
    val cont = jQuery("#" + NODE_GALLERY_DIV_ID)
    cont.on("click",  "." + CROP_IMAGE_BTN_CLASS,  {
        (that: HTMLElement, e: JQueryEventObject) =>
          galImgEditClick(that, e)
      }: ThisFunction)
  }

  /** Клик по кнопке кропа на одном из изображений галереи узла. */
  protected def galImgEditClick(el: HTMLElement, e: JQueryEventObject): Unit = {
    e.preventDefault()
    val asker = new CropFormRequestT with SjsLogger {
      override val parent = jQuery(e.currentTarget).parent()
      override val input  = super.input
    }
    val fut = asker.ask
    CropUtil.removeAllCropFrames()
    fut.map { resp =>
      galCropFormResponse(asker, resp)
    }
  }

  /** Отрендерить и отобразить попап кропа. */
  protected def galCropFormResponse(asker: CropFormRequestT, resp: CropFormResp): Unit = {
    Popup.appendPopup(resp.body)
    Popup.showPopups("#" + resp.id)
    // Инициализировать кроппер.
    // TODO Нужен нормальный кроппер на scala.js, работающий без многократного перебора всего DOM и принимающий на вход ещё и контейнер поля.
    val imgNameOpt = asker.input.attr("name").toOption
    imgNameOpt match {
      case Some(imgName) =>
        Market.img.crop.init(imgName)
      case None =>
        error("Cannot init img cropper, because imgName attr is undefined.")
    }
  }

}
