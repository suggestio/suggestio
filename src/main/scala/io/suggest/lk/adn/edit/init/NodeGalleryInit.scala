package io.suggest.lk.adn.edit.init

import io.suggest.lk.popup.Popup
import io.suggest.sjs.common.controller.IInit
import io.suggest.sjs.common.img.crop.{CropFormResp, CropUtil, CropFormRequestT}
import org.scalajs.jquery.{JQueryEventObject, jQuery}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import io.suggest.adn.edit.NodeEditConstants._
import io.suggest.img.crop.CropConstants._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.05.15 15:51
 * Description: Инициализация поддержки галереи в редакторе узла.
 */

trait NodeGalleryInit extends IInit {

  /** Инициализация всей формы. */
  abstract override def init(): Unit = {
    super.init()
    initNodeGallery()
  }

  /** Инициализация js для подредактора галереи. */
  def initNodeGallery(): Unit = {
    jQuery("#" + NODE_GALLERY_DIV_ID)
      .on("click", "." + CROP_IMAGE_BTN_CLASS, galImgEditClick(_))
  }

  /** Клик по кнопке кропа на одном из изображений галереи узла. */
  protected def galImgEditClick(e: JQueryEventObject): Unit = {
    e.preventDefault()
    val asker = new CropFormRequestT {
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
    // TODO Инициализировать кроппер.
    //val imgName = asker.input.attr("name")
    // market.img.crop.init( img_name )
  }

}
