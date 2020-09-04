package io.suggest.cordova.fetch

import cordova.plugins.fetch.Response
import io.suggest.i18n.MsgCodes
import io.suggest.log.Log
import io.suggest.msg.ErrorMsgs
import io.suggest.proto.http.HttpConst
import io.suggest.proto.http.model.HttpResp
import org.scalajs.dom
import org.scalajs.dom.experimental.ResponseInit

import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.09.2020 22:49
  * Description: Реализация HttpResp поверх выхлопа
  */
case class CdvFetchHttpResp(cdvFetchResp: Response)
  extends HttpResp
  with Log
{

  // cdv-fetch может возращать undefined вместо статуса. Это баг в плагине, TODO подменяем ошибочное значение:
  override def status = cdvFetchResp.status getOrElse HttpConst.Status.NOT_ACCEPTABLE
  override def statusText = cdvFetchResp.statusText getOrElse MsgCodes.`Error`

  override def isFromInnerCache = false

  override def getHeader(headerName: String): Seq[String] = {
    cdvFetchResp.headers
      .getAll( headerName )
      .iterator
      .flatMap { hdrV =>
        // headers.get(): Несколько заголовков Set-Cookie (или иных) приходят сюда одной строкой как ",\n"-delimited string.
        // Это удобно, хоть и нарушает fetch-спеку, требующую разделителя ", ".
        // Зато быстро и легко можно дробить значение на несколько хидеров, не обращаяя внимания на ISO-даты, тоже содержащие ", " внутри.
        // Не ясно только, нужно ли это дробление в getAll(). Делаем на всякий случай:
        hdrV.split(",[\n\r]+")
      }
      .toSeq
  }

  override def headers: Iterator[(String, String)] = {
    for {
      (k, vs) <- cdvFetchResp.headers.map.iterator
      v <- vs.iterator
      // vs: cordova-plugin-fetch подбрасывает \n внутри значения заголовка, надо это исправлять:
      v2 = v.replaceAll("[\n\r]+", " ")
    } yield {
      k -> v2
    }
  }

  override def bodyUsed: Boolean = cdvFetchResp.bodyUsed

  override def text() = cdvFetchResp.text().toFuture
  override def arrayBuffer() = cdvFetchResp.arrayBuffer().toFuture
  override def blob() = cdvFetchResp.blob().toFuture

  override def toDomResponse(): Option[dom.experimental.Response] = {
    val domResp = new dom.experimental.Response(
      content = cdvFetchResp._bodyInit.orNull,
      init = ResponseInit(
        _status = status,
        _statusText = statusText,
        _headers = {
          val hdrs = new dom.experimental.Headers()
          for {
            (k, vs) <- cdvFetchResp.headers.map
            v <- vs
            // vs: cordova-plugin-fetch подбрасывает \n внутри значения заголовка, надо это исправлять:
            v2 = v.replaceAll("[\n\r]+", " ")
            // По идее, любой заголовок проскакивает.
            ex <- Try( hdrs.append(k, v2) ).failed
          } {
            logger.warn( ErrorMsgs.HTTP_HEADER_PROBLEM, ex, (k, v2) )
          }
          hdrs
        },
      ),
    )

    Some(domResp)
  }

}
