package io.suggest.sjs.common.img

import io.suggest.sjs.common.controller.routes
import io.suggest.sjs.common.model.ex.XhrFailedException
import org.scalajs.jquery._

import scala.concurrent.{Future, Promise}
import scala.scalajs.js.{Any, Dictionary}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.04.15 16:36
 * Description: Встраиваемый код для запроса формы кроппинга изображения.
 */
trait CropFormRequestT {

  /** Событие. */
  //def evt: JQueryEventObject

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
  def ajax(): Future[Any] = {
    val p = Promise[JQueryXHR]()
    val args = Dictionary[Any](
      "url"      -> route.url,
      "method"   -> "GET",
      "async"    -> true,
      "complete" -> { (xhr: JQueryXHR, textStatus: String) =>
        if (xhr.status == 200)
          p success xhr
        else
          p failure XhrFailedException(xhr)
      }
    )
    try {
      jQuery.ajax(args.asInstanceOf[JQueryAjaxSettings])
    } catch {
      case ex: Throwable =>  p failure ex
    }
    p.future
  }

}


object CropUtil {
  def CROP_DIV_ID = "imgCropTool"
}
