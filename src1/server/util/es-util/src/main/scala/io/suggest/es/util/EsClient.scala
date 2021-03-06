package io.suggest.es.util

import java.net.InetSocketAddress

import com.google.inject.ImplementedBy
import javax.inject.{Inject, Provider, Singleton}
import io.suggest.util.logs.MacroLogsImpl
import org.elasticsearch.client.Client
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.transport.TransportAddress
import play.api.Configuration
import play.api.inject.{ApplicationLifecycle, Injector}
import io.suggest.conf.PlayConfigUtil._
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.transport.client.PreBuiltTransportClient

import scala.concurrent.{ExecutionContext, Future}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.08.16 12:11
  * Description: Поддержка ES-клиента, инжектируемого через DI.
  */

object EsClientUtil {

  def TRANSPORT_PORT_DFTL = 9300

  def parseHostPortStr(hostPortStr: String): InetSocketAddress = {
    // TODO Поддержка чтения адреса без порта.
    val Array(host, portStr) = hostPortStr.split(':')
    val port = portStr.toInt
    new InetSocketAddress(host, port)
  }


}

/** Интерфейс для di-поля с es-клиентом. */
@ImplementedBy( classOf[TransportEsClient] )
trait IEsClient extends Provider[Client] {

  /** Инстанс стандартного elasticsearch java client'а. */
  implicit def esClient: Client

  override def get(): Client = {
    val _esClient = esClient
    if (_esClient == null)
      throw new NoSuchElementException("No es-client available/configured")
    else
      _esClient
  }

}

/** Затычка для тестов при отсутствии клиента. */
class IEsClientMock extends IEsClient {
  override implicit def esClient = throw new UnsupportedOperationException
}


/**
  * Реализация инжектируемого ES-клиента на базе транспортного клиента.
  *
  * Начиная с elasticsearch 2.x и вышие рекомендуется использовать транспортный клиент
  * на стороне приложения + локальную ноду, к которой он присоединён.
  * Это снизит объёмы паразитной болтовни в кластере.
  */
@Singleton
final class TransportEsClient @Inject() (
                                          injector        : Injector,
                                        )
  extends IEsClient
  with MacroLogsImpl
{

  private var _trClient: TransportClient = _
  override implicit def esClient: Client = _trClient

  /**
    * Собрать новый transport client для прозрачной связи с внешним es-кластером.
    *
    * @param addrs Адреса узлов.
    * @param clusterName Название удалённого кластера. Если None, то клиент будет игнорить проверку имени кластера.
    * @return TransportClient
    */
  def newTransportClient(addrs: Seq[TransportAddress], clusterName: Option[String]): TransportClient = {
    val settingsBuilder = Settings.builder()

    clusterName.fold {
      settingsBuilder.put("client.transport.ignore_cluster_name", true)
    } { _clusterName =>
      settingsBuilder.put("cluster.name", _clusterName)
    }

    val settings = settingsBuilder.build()
    new PreBuiltTransportClient(settings)
      .addTransportAddresses(addrs : _*)
  }


  def _installClient(): TransportClient = {
    def logPrefix = s"_installClient():"

    // Закинуть имя кластера из оригинального конфига.
    // TODO Переименовать параметры sio-конфига во что-то, начинающееся с es.client.
    val configuration = injector.instanceOf[Configuration]
    val clusterNameOpt = configuration.getOptional[String]("es.cluster.name")

    // Законнектить свеженький клиент согласно адресам из конфига, если они там указаны.

    val addrs = configuration
      .getOptionalSeq[String]("es.client.transport.addrs")
      .fold [Iterator[InetSocketAddress]] {
        val local = new InetSocketAddress(
          "localhost",
          EsClientUtil.TRANSPORT_PORT_DFTL
        )
        Iterator.single(local)
      } { addrsStrs =>
        for {
          addr <- addrsStrs.iterator
          if addr.nonEmpty
        } yield {
          EsClientUtil.parseHostPortStr( addr )
        }
      }
      .map( new TransportAddress(_) )
      .toSeq

    LOGGER.trace(s"$logPrefix Cluster name: ${clusterNameOpt.orNull}\n Transport addrs: ${addrs.mkString(", ")}")

    _trClient = newTransportClient(addrs, clusterNameOpt)
    _trClient
  }


  // Constructor: немедленная инициализация es-клиента.
  LOGGER.trace("Installing new ES client...")
  _installClient()

  // При завершении работы надо зарубить клиент.
  injector.instanceOf[ApplicationLifecycle].addStopHook { () =>
    implicit val _ec = injector.instanceOf[ExecutionContext]
    Future {
      try {
        for ( c <- Option(_trClient) ) {
          c.close()
          LOGGER.trace(s"Successfully closed ES client $c")
          _trClient = null
        }
      } catch {
        case ex: Throwable =>
          LOGGER.error(s"Cannot close es client ${_trClient}", ex)
      }
    }
  }

}
