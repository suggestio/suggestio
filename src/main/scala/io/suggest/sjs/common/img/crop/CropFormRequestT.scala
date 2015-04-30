package io.suggest.sjs.common.img.crop

import io.suggest.img.crop.CropConstants
import io.suggest.popup.PopupConstants
import io.suggest.sjs.common.controller.routes
import io.suggest.sjs.common.model.ex.XhrFailedException
import org.scalajs.jquery._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow

import scala.concurrent.{Future, Promise}
import scala.scalajs.js.{Any, Dictionary}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.04.15 16:36
 * Description: Встраиваемый код для запроса формы кроппинга изображения.
 */
trait CropFormRequestT {

  /** Прямой родитель, чтобы найти input. */
  def parent: JQuery

  /** Инпут, откуда можно прочесть данные для запроса. */
  // Нужно перезаписывать через val или lazy val.
  def input: JQuery = parent.find("input")

  /** id текущей картинки. */
  def imgId: String = input.`val`().toString

  /** Имя поля */
  def fieldName = input.attr("name")

  /** Ширина для кропа. */
  def width = input.data("width").toString.toInt

  /** Высота для кропа. */
  def height = input.data("height").toString.toInt

  /** Роута из jsRouter'а. */
  def route = routes.controllers.Img.imgCropForm(
    imgId = imgId,
    width = width,
    height = height
  )

  /**
   * Запустить ajax запрос.
   * @return Фьючерс с телом ответа или с ошибкой.
   */
  def ajax: Future[JQueryXHR] = {
    val p = Promise[JQueryXHR]()
    val args = Dictionary[Any](
      "url"      -> route.url,
      "method"   -> "GET",
      "async"    -> true,
      "complete" -> { (xhr: JQueryXHR, textStatus: String) =>
        if (xhr.status == 200) {
          p success xhr
        } else {
          p failure XhrFailedException(xhr)
        }
      }
    )
    try {
      val xhrSettings = args.asInstanceOf[JQueryAjaxSettings]
      jQuery.ajax(xhrSettings)
    } catch {
      case ex: Throwable =>  p failure ex
    }
    p.future
  }

  /**
   * Сделать обращение и распарсить результат.
   * @return Экземпляр [[CropFormResp]].
   */
  def ask: Future[CropFormResp] = {
    ajax.map { xhr =>
      CropFormResp(
        body = xhr.response,
        id   = xhr.getResponseHeader( PopupConstants.HTTP_HDR_POPUP_ID )
      )
    }
  }

}


object CropUtil {

  /** Очистить DOM от всех открытых кропов. */
  def removeAllCropFrames(): Unit = {
    jQuery("#" + CropConstants)
      .remove()
  }

}


case class CropFormResp(body: Any, id: String)

