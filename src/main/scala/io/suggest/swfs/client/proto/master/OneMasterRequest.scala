package io.suggest.swfs.client.proto.master

import io.suggest.swfs.client.play.SwfsClientWs
import io.suggest.swfs.client.proto.IToQs
import io.suggest.util.MacroLogsI
import play.api.libs.json.Reads
import play.api.libs.ws.{WSResponse, WSClient}

import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.10.15 15:46
 * Description: Бывает, что нужно отработать реквест на каком-то мастере
 * с простеньким fail-over'ом на последующие мастеры.
 */
trait OneMasterRequestT extends MacroLogsI {

  type Args_t <: IToQs
  type Res_t

  def _method: String
  def _args: Args_t

  def _ws: WSClient

  def _ec: ExecutionContext

  def _mkUrl(master: String): String

  def mkOp(restMasters: List[String]): Future[Res_t] = {
    implicit val ec = _ec
    if (restMasters.isEmpty) {
      Future failed new NoSuchElementException("No more masters to failover")

    } else {
      val master = restMasters.head
      var fut1 = mkOp(master)
      val restMasters2 = restMasters.tail

      if (restMasters2.nonEmpty) {
        fut1 = fut1.recoverWith { case ex: Throwable =>
          mkOp(restMasters2)
        }
      }
      fut1.onFailure { case ex: Throwable =>
        val msg = s"mkOp($master) failed, args was = ${_args}"
        if (ex.isInstanceOf[NoSuchElementException])
          LOGGER.warn(msg)
        else
          LOGGER.warn(msg, ex)
      }

      fut1
    }
  }

  def _isRetryNextMaster(ex: Throwable): Boolean = true

  def _isStatusValid(status: Int): Boolean = {
    SwfsClientWs.isStatus2xx( status )
  }

  def mkOp(master: String): Future[Res_t] = {
    val url = _mkUrl(master)
    val fut = _ws.url( url )
      .execute(_method)
    // Логгируем ответы на запросы трейсом
    if (LOGGER.underlying.isTraceEnabled()) {
      fut.onSuccess { case resp =>
        LOGGER.trace(s"${_method} $url =>\n ${resp.body}")
      }(_ec)
    }

    // Отправляем реквест на итоговую обработку.
    val resFut = _handleResp(url, fut)
    resFut
  }


  def _handleResp(url: String, fut: Future[WSResponse]): Future[Res_t]

}


/** Обычно abstract class'а достаточно. */
abstract class OneMasterRequest extends OneMasterRequestT
