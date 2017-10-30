package io.suggest.lk.img

import io.suggest.img.ImgConstants
import io.suggest.js.UploadConstants
import io.suggest.lk.popup.Popup
import io.suggest.popup.PopupConstants
import io.suggest.sjs.common.controller.IInit
import io.suggest.sjs.common.util.IContainers
import org.scalajs.dom.raw.HTMLElement
import org.scalajs.jquery.{JQuery, JQueryEventObject, jQuery}

import scala.scalajs.js.ThisFunction

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.05.15 13:52
 * Description: Аддон для поддержки удаления картинок из форм.
 */
trait JsRemoveImgT extends IContainers {

  /** CSS-селектор контейнера поля удаления картинки. */
  protected def _imgRmCssSel = "." + ImgConstants.JS_REMOVE_IMG_CLASS

  /** Инициализация поддержки удаления картинки. */
  protected def initJsRemoveImg(): Unit = {
    val imgRmCssSel = _imgRmCssSel
    val conts = Iterator.single(Popup.container) ++ _imgInputContainers.toIterator
    conts.foreach { cont =>
      cont.on("click", imgRmCssSel, { (that: HTMLElement, e: JQueryEventObject) =>
        _imgRmClick(that, cont, e)
      } : ThisFunction)
    }
  }

  /** Реакция на клик по кнопке удаления картинки. */
  protected def _imgRmClick(that: HTMLElement, cont: JQuery, e: JQueryEventObject): Unit = {
    e.preventDefault()
    val el = jQuery(that)

    // Аттрибут data-for выставляется при инициализации js-кроппера.
    val dataForOpt = el.attr("data-for")
      .toOption
      .filter(!_.isEmpty)
    val previewSel = "." + ImgConstants.JS_PREVIEW_CLASS
    val (input, preview) = dataForOpt match {
      case Some(dataFor) =>
        val sel = "input[name = '" + dataFor + "']"
        var _input = cont.find()
        // TODO Костыль для поиска инпута не зная контейнера. С новым кроппером эта проблема должна бы быть решена.
        if (_input.length == 0)
          _input = jQuery(sel)
        val _preview = _input.parent(previewSel)
        (_input, _preview)
      case None =>
        val _preview = el.parent(previewSel)
        val _input = _preview.find("." + ImgConstants.JS_IMG_ID_CLASS)
        (_input, _preview)
    }

    // Сбросить значение file-инпута.
    input.value("")

    // Находим кнопку для загрузки изображении для этого поля.
    val name = input.attr("name")
    // Отобразить на экран что-то, относящееся к кнопке загрузки нового изображения.
    cont.find("." + UploadConstants.JS_FILE_UPLOAD_CLASS + "[data-name = '" + name + "']")
      .parent("." + UploadConstants.JS_IMG_UPLOAD_CLASS)
      .show()

    // Превьюшку старого изображения стереть.
    preview.remove()

    // TODO market.ad_form.queue_block_preview_request()

    if (dataForOpt.isDefined) {
      val popup = jQuery("." + PopupConstants.JS_POPUP_CLASS)
      if (popup.length > 0) {
        Popup.hidePopups(popup)
        // TODO Проверить, вроде бы анимация сокрытия идет асинхронно, и может не успевать начаться из-за remove()
        popup.remove()
      }
    }
  }

}

/** Поддержка инициализации для вышеуказанного трейта. */
trait JsRemoveImgInitT extends JsRemoveImgT with IInit {
  abstract override def init(): Unit = {
    super.init()
    initJsRemoveImg()
  }
}
