package io.suggest.lk.adn.edit.init

import io.suggest.lk.img.CropFormAskInit
import org.scalajs.dom.raw.HTMLElement
import org.scalajs.jquery.{JQueryEventObject, jQuery}
import io.suggest.adn.edit.NodeEditConstants._
import io.suggest.img.crop.CropConstants._

import scala.scalajs.js.ThisFunction

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.05.15 15:51
 * Description: Инициализация поддержки галереи в редакторе узла.
 */

trait NodeGalleryInit extends CropFormAskInit {

  /** Нужно заимплементить этот метод, чтобы он слушал необходимые события формы и дергал _imgCropClick(). */
  override protected def initCropFormAskListener(): Unit = {
    val cont = jQuery("#" + NODE_GALLERY_DIV_ID)
    cont.on("click",  "." + CROP_IMAGE_BTN_CLASS,  {
      (that: HTMLElement, e: JQueryEventObject) =>
        _imgCropClick(that, e)
    }: ThisFunction)
  }

}
