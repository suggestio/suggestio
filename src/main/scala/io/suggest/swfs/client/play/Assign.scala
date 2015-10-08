package io.suggest.swfs.client.play

import io.suggest.swfs.client.proto.assign.{IAssignRequest, AssignResponse}
import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.10.15 12:46
 * Description: Кусок реализации play.ws-клиента seaweedfs для поддержки операции /dir/assign.
 */
object Assign {

  /** Название conf-ключа со списком доступных мастер-серверов seaweedfs. */
  def MASTERS_CK = "swfs.masters"

}


import Assign._


trait Assign extends ISwfsClientWs {

  /**
   * Список weed-мастеров.
   * @return ["localhost:9300", "127.5.5.5:9301"]
   */
  val MASTERS: List[String] = {
    conf.getStringSeq(MASTERS_CK).get.toList
  }

  override def assign(args: IAssignRequest)(implicit ec: ExecutionContext): Future[AssignResponse] = {
    val ms = MASTERS
    assignOn(ms.head, args, ms.tail)
  }

  /**
   * Ассингновать у мастера fid нового файла.
   * @param master Мастер-хост.
   * @param args Параметры запроса.
   * @param restMasters Запасные мастера.
   * @return Фьючерс с распарсенным ответом сервера.
   */
  def assignOn(master: String, args: IAssignRequest, restMasters: List[String] = Nil)
              (implicit ec: ExecutionContext): Future[AssignResponse] = {
    lazy val logPrefix = s"assignOn($master):"
    LOGGER.trace(s"$logPrefix args=$args restMasters=$restMasters")

    val url = "http://" + master + "/dir/assign" + args.toQs
    val method = "POST"
    var fut = ws.url(url)
      .execute(method)
      .filter { resp =>
        LOGGER.trace(s"$method $url replied HTTP ${resp.status} ${resp.statusText}\n ${resp.body}")
        SwfsClientWs.isStatus2xx( resp.status )
      }
      .map { resp =>
        val jsvr = resp.json.validate[AssignResponse]
        if (jsvr.isError)
          LOGGER.error(s"$logPrefix Cannot parse master's reply: $jsvr\n  ${resp.body}")
        jsvr.get
      }

    // Если есть запасные мастеры, то повторить попытку.
    if (restMasters.nonEmpty) {
      fut = fut recoverWith {
        case ex: Throwable =>
          assignOn(restMasters.head, args, restMasters.tail)
      }
    }

    fut onFailure { case ex: Throwable =>
      val msg = s"$logPrefix failed, args was = $args"
      if (ex.isInstanceOf[NoSuchElementException])
        LOGGER.warn(msg)
      else
        LOGGER.warn(msg, ex)
    }

    fut
  }

}
