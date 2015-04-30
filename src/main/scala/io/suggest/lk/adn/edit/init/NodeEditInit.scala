package io.suggest.lk.adn.edit.init

import io.suggest.img.ImgConstants
import io.suggest.js.UploadConstants
import io.suggest.lk.popup.Popup
import io.suggest.sjs.common.controller.InitRouter
import io.suggest.sjs.common.img.crop.{CropUtil, CropFormRequestT}
import io.suggest.sjs.common.util.{SafeSyncVoid, SjsLogger}
import org.scalajs.jquery.{JQueryEventObject, jQuery}
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import io.suggest.adn.edit.NodeEditConstants._
import io.suggest.img.crop.CropConstants._

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

  /** Инициализация всей формы. */
  def init(): Unit = {
    initNodeGallery()
  }

  /** Инициализация js для подредактора галереи. */
  def initNodeGallery(): Unit = {
    jQuery("#" + NODE_GALLERY_DIV_ID)
      .on("click", "." + CROP_IMAGE_BTN_CLASS, galImgEditClick(_))
  }

  /** Клик по кнопке кропа на одном из изображений галереи узла. */
  def galImgEditClick(e: JQueryEventObject): Unit = {
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

  /** Делегировать обработку кликов по кнопкам удаления статическим контейнерам редактора. */
  def initJsRemoveImg(): Unit = {
    val imgRmCssSel = "." + ImgConstants.JS_REMOVE_IMG_CLASS
    Seq(NODE_GALLERY_DIV_ID, NODE_LOGO_DIV_ID, NODE_WELCOME_DIV_ID) foreach { contId =>
      val cont = jQuery( "#" + contId )
      cont.on("click", imgRmCssSel, { e: JQueryEventObject =>
        e.preventDefault()
        val el = jQuery( e.currentTarget )

        // Аттрибут data-for выставляется при инициализации js-кроппера.
        val dataForOpt = Option( el.attr("data-for") )
          .filter(!_.isEmpty)
        val previewSel = "." + ImgConstants.JS_PREVIEW_CLASS
        val (input, preview) = dataForOpt match {
          case Some(dataFor) =>
            val _input = cont.find("input[name = '" + dataFor + "']")
            val _preview = _input.parent(previewSel)
            (_input, _preview)
          case None =>
            val _preview = el.parent(previewSel)
            val _input = _preview.find("." + ImgConstants.JS_IMG_ID_CLASS)
            (_input, _preview)
        }

        // Находим кнопку для загрузки изображении для этого поля.
        val name = input.attr("name")
        // TODO Остановились на mx_cof: 308
        // TODO Провести JS_FILE_UPLOAD_CLASS в шаблоны.
        cont.find("." + UploadConstants.JS_FILE_UPLOAD_CLASS + "[data-name = '" + name + "']")
        ???
      })
    }
  }

}
