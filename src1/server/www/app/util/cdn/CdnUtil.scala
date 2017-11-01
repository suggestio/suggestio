package util.cdn

import javax.inject.{Inject, Singleton}

import controllers.routes
import io.suggest.playx.ExternalCall
import io.suggest.proto.HttpConst
import io.suggest.url.MHostInfo
import io.suggest.util.logs.MacroLogsImpl
import models.mctx.Context
import models.req.IReqHdr
import play.api.Configuration
import play.api.mvc.Call

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.10.14 18:31
 * Description: Утииль для работы с CDN.
 */
@Singleton
class CdnUtil @Inject() (
                          corsUtil        : CorsUtil,
                          configuration   : Configuration
                        )
  extends MacroLogsImpl
{

  /** Прочитать из конфига список CDN-хостов для указанного протокола. */
  def getCdnHostsForProto(proto: String): List[String] = {
    configuration.getOptional[Seq[String]]("cdn.hosts." + proto)
      .fold (List.empty[String]) (_.toList)
  }

  /** Карта протоколов и списков CDN-хостов, которые готовые обслуживать запросы. */
  val CDN_PROTO_HOSTS: Map[String, List[String]] = {
    configuration.getOptional[Seq[String]]("cdn.protocols")
      .fold [Iterator[String]] (Iterator(HttpConst.Proto.HTTP, HttpConst.Proto.HTTPS)) { protosRaw =>
        protosRaw
          .iterator
          .map(_.trim.toLowerCase)
      }
      .map { proto =>
        proto -> getCdnHostsForProto(proto)
      }
      .filter { _._2.nonEmpty }
      .toMap
  }

  /** Раздавать ли шрифты через CDN? Дергается из шаблонов. Если Cors отключен, то этот параметр тоже отключается. */
  val FONTS_ENABLED: Boolean = {
    configuration.getOptional[Boolean]("cdn.fonts.enabled")
      .exists(_ && corsUtil.IS_ENABLED)
  }

  /** Отключено использование CDN на хостах: */
  val DISABLED_ON_HOSTS: Set[String] = {
    configuration.getOptional[Seq[String]]("cdn.disabled.on.hosts")
      .fold (Set.empty[String]) (_.toSet)
  }

  val HAS_ANY_CDN: Boolean = CDN_PROTO_HOSTS.nonEmpty


  // Печатаем карту в консоль при запуске.
  LOGGER.info {
    val sb = new StringBuilder("CDNs map (proto -> hosts...) is:")
    CDN_PROTO_HOSTS
      .foreach { case (proto, hosts) =>
        sb.append("\n  ")
          .append(proto)
          .append(": ")
        hosts foreach { host =>
          sb.append(host)
            .append(", ")
        }
      }
    sb.toString()
  }

  if (DISABLED_ON_HOSTS.nonEmpty) {
    LOGGER.info(s"CDNs disabled on hosts: " + DISABLED_ON_HOSTS.mkString(", "))
  }


  /** Выбрать подходящий CDN-хост для указанного протокола. */
  def chooseHostForProto(protoLc: String): Option[String] = {
    CDN_PROTO_HOSTS
      .get(protoLc)
      .flatMap(_.headOption)    // TODO Выбирать рандомный хост из списка хостов.
  }

  def ctx2CdnHost(implicit ctx: Context): Option[String] = {
    chooseHostForProto( ctx.request.myProto )
  }

  /** Генератор вызовов к CDN или внутренних. */
  def forCall(c: Call)(implicit ctx: Context): Call = {
    if (!HAS_ANY_CDN || c.isInstanceOf[ExternalCall]) {
      c
    } else {
      val reqHost = ctx.request.host
      val urlPrefixOpt: Option[String] = if (DISABLED_ON_HOSTS.contains(reqHost)) {
        None
      } else {
        val protoLc = ctx.request.myProto
        for {
          cdnHost <- chooseHostForProto(protoLc)
          if {
            !DISABLED_ON_HOSTS.contains(reqHost) &&
              !(cdnHost equalsIgnoreCase reqHost)
          }
        } yield {
          // Не указываем протокол. Это хорошо, когда CDN работает по HTTP, а раздаёт по HTTPS.
          HttpConst.Proto.CURR_PROTO + cdnHost
        }
      }
      urlPrefixOpt.fold(c) { urlPrefix =>
        new ExternalCall(url = urlPrefix + c.url)
      }
    }
  }

  /** Вызов на asset через CDN. */
  def asset(file: String)(implicit ctx: Context): Call = {
    forCall( routes.Assets.versioned(file) )
  }


  /** Бывает, что нужно в зависимости от значения флага генерить полные и относительные ссылки.
    * Не очень уместный здесь код (к CDN напрямую не относится).
    *
    * @param forceAbsoluteUrl true -- нужна абсолютная ссылка. false -- хватит и относительной.
    * @param call исходный вызов.
    * @return Строка с ссылкой.
    */
  def maybeAbsUrl(forceAbsoluteUrl: Boolean)(call: Call)(implicit ctx: Context): String = {
    if (forceAbsoluteUrl) {
      import ctx.request
      val absUrl = call.absoluteURL()
      if (absUrl startsWith HttpConst.Proto.CURR_PROTO) {
        // Вот так бывает: протокол не указан, потому что forCall() больше не пишет протокол.
        // Значит, уже отсылка к CDN, и значит дописываем https:
        HttpConst.Proto.HTTPS_ + absUrl
      } else {
        absUrl
      }
    } else {
      call.url
    }
  }


  /** Отмаппить внутренний хост sio на CDN-хост.
    * Синхронный и быстрый метод (как и все остальные здесь), для возможности эффективного использования в шаблонах.
    *
    * @param host Данные хоста-ноды sio-кластера (см. medias2hosts()).
    * @return Хостнейм CDN-хоста.
    *         Если CDN недоступна, то вернуть обычный public-адрес.
    */
  def distHost2cdnUrlPrefix(host: MHostInfo)(implicit request: IReqHdr): String = {
    // TODO Дергать карту хостов и CDN.
    LOGGER.trace(s"distHost2cdnHost(${host.nameInt}): CDN+NODES SUPPORT NOT IMPLEMENTED HERE, returning ${host.namePublic}")
    HttpConst.Proto.CURR_PROTO + host.namePublic
  }


  /** Сборка URL, которая   */
  def distNodeCdnUrl(host: MHostInfo, call: Call)(implicit request: IReqHdr): String = {
    call match {
      // В теории возможно, что метод будет вызван с инстансом ExternalCall.
      case extCall: ExternalCall =>
        throw new IllegalArgumentException(s"ExternalCall is unsupported: $extCall ; host=$host")
      case _ =>
        distHost2cdnUrlPrefix(host) + call.url
    }
  }

}

/** Интерфейс для доступа к DI-полю с инстансом [[CdnUtil]]. */
trait ICdnUtilDi {
  def cdnUtil: CdnUtil
}
