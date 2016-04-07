package util

import com.google.inject.{Inject, Provider, Singleton}

import scala.collection.JavaConversions._
import io.suggest.util.SioEsClient
import org.elasticsearch.client.Client
import play.api.Configuration
import play.api.inject.ApplicationLifecycle

import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.04.13 17:54
 * Description: Фунцкии для поддержки работы c ES на стороне веб-морды. В частности,
 * генерация запросов и поддержка инстанса клиента.
 * Тут по сути нарезка необходимого из sio_elastic_search, sio_lucene_query, sio_m_page_es.
 */

@Singleton
class SiowebEsUtil @Inject() (
  configuration   : Configuration,
  lifecycle       : ApplicationLifecycle,
  implicit val ec : ExecutionContext
)
  extends SioEsClient
  with Provider[Client]
{

  ensureNode()

  override def get(): Client = client

  /** Имя кластера elasticsearch, к которому будет коннектиться клиент. */
  override def getEsClusterName = configuration.getString("es.cluster.name")
    .getOrElse(super.getEsClusterName)

  /** Если нужен юникаст, то нужно передать непустой список из host или host:port */
  override def unicastHosts: List[String] = {
    configuration.getStringList("es.unicast.hosts")
      .map(_.toSeq)
      .filter(_.nonEmpty)
      .fold(super.unicastHosts)(_.toList)
  }


  lifecycle.addStopHook { () =>
    Future {
      stopNode()
    }
  }

}
