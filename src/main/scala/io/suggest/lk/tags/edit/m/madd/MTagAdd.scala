package io.suggest.lk.tags.edit.m.madd

import io.suggest.lk.router.jsRoutes
import io.suggest.sjs.common.msg.ErrorMsgs
import io.suggest.sjs.common.xhr.Xhr
import org.scalajs.dom.FormData

import scala.concurrent.{Future, ExecutionContext}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.09.15 18:05
 * Description: Модель для dry-run запроса добавления тега в форму на экране.
 */
object MTagAdd {

  def add(index: Int, body: FormData)(implicit ec: ExecutionContext): Future[Either[String, String]] = {
    val route = jsRoutes.controllers.MarketAd.tagEditorAddTag(index)
    val respFut = Xhr.send(
      method  = route.method,
      url     = route.url,
      headers = Seq(Xhr.HDR_ACCEPT -> Xhr.MIME_TEXT_HTML),
      body    = Some(body)
    )
    respFut map { xhr =>
      if (xhr.status == 200)
        Right( xhr.responseText )
      else if (xhr.status == 416)
        Left( xhr.responseText )
      else
        throw new RuntimeException( ErrorMsgs.XHR_UNEXPECTED_RESP )
    }
  }

}
