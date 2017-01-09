package io.suggest.es

import java.net.InetSocketAddress

import com.google.inject.{Inject, Provider, Singleton}
import io.suggest.di.IEsClient
import io.suggest.util.MacroLogsImpl
import org.elasticsearch.client.Client
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.InetSocketTransportAddress
import play.api.Configuration
import play.api.inject.ApplicationLifecycle

import scala.concurrent.{ExecutionContext, Future}
import scala.collection.JavaConversions._

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

trait EsClientT
  extends IEsClient
  with Provider[Client]
{
  override def get(): Client = esClient
}


/**
  * Реализация инжектируемого ES-клиента на базе транспортного клиента.
  * Начиная с elasticsearch 2.x рекомендуется использовать транспортный клиент на стороне приложения + локальную ноду.
  */
@Singleton
class TransportEsClient @Inject() (
  configuration   : Configuration,
  lifecycle       : ApplicationLifecycle,
  implicit val ec : ExecutionContext
)
  extends EsClientT
  with MacroLogsImpl
{

  import LOGGER._

  private var _trClient: TransportClient = _

  def _installClient(): TransportClient = {
    lazy val logPrefix = s"_installClient(${System.currentTimeMillis}):"

    val settingsBuilder = Settings.builder()

    // Закинуть имя кластера из оригинального конфига.
    // TODO Переименовать параметры sio-конфига во что-то, начинающееся с es.client.
    val clusterNameOpt = configuration.getString("cluster.name")
    clusterNameOpt.fold [Unit] {
      settingsBuilder.put("client.transport.ignore_cluster_name", true)
    } { clusterName =>
      settingsBuilder.put("cluster.name", clusterName)
    }
    val settings = settingsBuilder.build()

    debug(s"$logPrefix Cluster name: $clusterNameOpt")

    // Собираем клиентуру
    _trClient = TransportClient.builder()
      .settings(settings)
      .build()

    // Законнектить свеженький клиент согласно адресам из конфига, если они там указаны.
    val addrs = configuration
      .getStringList("es.client.transport.addrs")
      .fold [Iterator[InetSocketAddress]] {
        val local = new InetSocketAddress(
          "localhost",
          EsClientUtil.TRANSPORT_PORT_DFTL
        )
        Iterator(local)
      } { addrsStrs =>
        addrsStrs.iterator()
          .filter(_.nonEmpty)
          .map { addrStr =>
            EsClientUtil.parseHostPortStr(addrStr)
          }
      }
      .map( new InetSocketTransportAddress(_) )
      .toSeq

    debug(s"$logPrefix Transport addrs: ${addrs.mkString(", ")}")
    _trClient
      .addTransportAddresses(addrs : _*)
  }

  override implicit def esClient: Client = _trClient


  // Constructor: немедленная инициализация es-клиента.
  trace("Installing new ES client...")
  _installClient()

  // При завершении работы надо зарубить клиент.
  lifecycle.addStopHook { () =>
    Future {
      try {
        _trClient.close()
        trace(s"Successfully closed ES client ${_trClient}")
        _trClient = null
      } catch {
        case ex: Throwable =>
          error(s"Cannot close es client ${_trClient}", ex)
      }
    }
  }

}
