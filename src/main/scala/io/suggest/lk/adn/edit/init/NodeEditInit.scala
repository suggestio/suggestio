package io.suggest.lk.adn.edit.init

import io.suggest.lk.popup.Popup
import io.suggest.sjs.common.controller.InitRouter
import io.suggest.sjs.common.img.crop.{CropUtil, CropFormRequestT}
import io.suggest.sjs.common.util.{SafeSyncVoid, SjsLogger}
import org.scalajs.jquery.{JQueryEventObject, jQuery}
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.04.15 15:16
 * Description: JS для формы редактирования узла.
 */

trait NodeEditInitRouter extends InitRouter {

  override protected def routeInitTarget(itg: MInitTarget): Future[_] = {
    if (itg == MInitTargets.LkNodeEditForm) {
      Future {
        new LkNodeEditFormEvents()
          .init()
      }
    } else {
      super.routeInitTarget(itg)
    }
  }

}


/** Поддержка node для gallery. */
class LkNodeEditFormEvents extends SjsLogger with SafeSyncVoid {

  def NODE_GALLERY_DIV_ID = "profileGallery"
  def CROP_IMAGE_BTN_CLASS = "js-crop-image-btn"

  /** Инициализация всей формы. */
  def init(): Unit = {
    initNodeGallery()
  }

  /** Инициализация js для подредактора галереи. */
  def initNodeGallery(): Unit = {
    jQuery("#" + NODE_GALLERY_DIV_ID)
      .on("click", "." + CROP_IMAGE_BTN_CLASS, galCropClick(_))
  }

  /** Клик по кнопке кропа на одном из изображений галереи узла. */
  def galCropClick(e: JQueryEventObject): Unit = {
    e.preventDefault()
    val asker = new CropFormRequestT {
      override val parent = jQuery(e.currentTarget).parent()
      override val input  = super.input
    }
    val fut = asker.ask
    CropUtil.removeAllCropFrames()
    fut.map { resp =>
      // Отрендерить и отобразить попап кропа.
      Popup.appendPopup(resp.body)
      Popup.showPopups("#" + resp.id)
      // TODO Инициализировать кроппер.
      //val imgName = asker.input.attr("name")
      // market.img.crop.init( img_name )
      ???
    }
  }

}
