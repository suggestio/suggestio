package io.suggest.lk.adn.edit.init

import io.suggest.sjs.common.controller.{routes, InitController, InitRouter}
import io.suggest.sjs.common.img.{CropUtil, CropFormRequestT}
import io.suggest.sjs.common.util.{ISjsLogger, SafeSyncVoid, SjsLogger}
import org.scalajs.jquery.{JQuery, JQueryEventObject, jQuery}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.04.15 15:16
 * Description: JS для формы редактирования узла.
 */

trait NodeEditInitRouter extends InitRouter {

  /** Поиск init-контроллера с указанным именем. */
  override protected def getController(name: String): Option[InitController] = {
    if (name contains "LkAdnEdit") {
      Some(new NodeEditInitController)
    } else {
      super.getController(name)
    }
  }

}


/**
 * Контроллер инициализации js для формы редактирования узла.
 * - #welcomeImage: Нужно повесить поддержку заливки и удаления картинок.
 * - #profileGallery: загрузка, кроп, удаление картинок.
 * - #logo: Загрузка, удаление картинок.
 * - Выбор цветов (bg и fg).
 */
class NodeEditInitController extends InitController with SjsLogger with SafeSyncVoid with ProfileGallery {

  /** Синхронная инициализация контроллера. */
  override def riInit(): Unit = {
    super.riInit()
    _safeSyncVoid { () =>
      init()
    }
  }

  def init(): Unit = {
    initNodeGallery()
  }

}


/** Поддержка node для gallery. */
sealed trait ProfileGallery extends ISjsLogger with SafeSyncVoid {

  def NODE_GALLERY_DIV_ID = "profileGallery"
  def CROP_IMAGE_BTN_CLASS = "js-crop-image-btn"

  def initNodeGallery(): Unit = {
    jQuery("#" + NODE_GALLERY_DIV_ID)
      .on("click", "." + CROP_IMAGE_BTN_CLASS, {(e: JQueryEventObject) =>
        e.preventDefault()
        val asker = new CropFormRequestT {
          override val parent = jQuery(e.currentTarget).parent()
          override val input  = super.input
        }
        val fut = asker.ajax()
        jQuery("#" + CropUtil.CROP_DIV_ID)
          .remove()
        fut.map { resp =>
          // TODO Залить контент в popup container.
          // TODO Отобразить попап.
          ???
        }
      })
  }

}
