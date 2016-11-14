package io.suggest.sjs.common.img.crop

import io.suggest.common.geom.d2.ISize2di
import io.suggest.img.crop.CropConstants
import io.suggest.popup.PopupConstants
import io.suggest.sjs.common.controller.routes
import io.suggest.sjs.common.model.Route
import io.suggest.sjs.common.model.ex.XhrFailedException
import org.scalajs.jquery._
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx

import scala.concurrent.{Future, Promise}
import scala.scalajs.js.{Any, Dictionary}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.04.15 16:36
 * Description: Встраиваемый код для запроса формы кроппинга изображения.
 */
trait CropFormRequestT extends ISize2di {

  /** Отсюда считывать img id. */
  def imgIdInput: JQuery

  /** id текущей картинки. */
  def imgId: String = imgIdInput.`val`().toString

  /** Роута из jsRouter'а. */
  def route: Route = {
    routes.controllers.Img.imgCropForm(
      imgId = imgId,
      width = width,
      height = height
    )
  }

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
      "dataType" -> "html",
      "success"  -> { (html: Any, textStatus: String, xhr: JQueryXHR) =>
        p success xhr
      },
      "error"  -> { (jqXHR: JQueryXHR, textStatus: String, errorThrow: String) =>
        p failure XhrFailedException(jqXHR, errorThrow)
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
        body = xhr.responseText,
        id   = xhr.getResponseHeader( PopupConstants.HTTP_HDR_POPUP_ID )
      )
    }
  }

}


object CropUtil {

  /** Очистить DOM от всех открытых кропов. */
  def removeAllCropFrames(): Unit = {
    jQuery("#" + CropConstants.CROPPER_DIV_ID)
      .remove()
  }

}


case class CropFormResp(body: Any, id: String)

