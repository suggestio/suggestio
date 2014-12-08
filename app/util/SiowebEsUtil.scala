package util

import scala.collection.JavaConversions._
import io.suggest.util.SioEsClient
import play.api.Play.{current, configuration}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.04.13 17:54
 * Description: Фунцкии для поддержки работы c ES на стороне веб-морды. В частности,
 * генерация запросов и поддержка инстанса клиента.
 * Тут по сути нарезка необходимого из sio_elastic_search, sio_lucene_query, sio_m_page_es.
 */

object SiowebEsUtil extends SioEsClient {

  /** Имя кластера elasticsearch, к которому будет коннектиться клиент. */
  override def getEsClusterName = configuration.getString("es.cluster.name") getOrElse super.getEsClusterName

  /** Если нужен юникаст, то нужно передать непустой список из host или host:port */
  override def unicastHosts: List[String] = {
    configuration.getStringList("es.unicast.hosts") match {
      case Some(uhs) if !uhs.isEmpty =>
        uhs.toList
      case _ =>
        super.unicastHosts
    }
  }

}
