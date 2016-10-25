package util.cdn

import com.google.inject.{Inject, Singleton}
import controllers.routes
import models.ExternalCall
import models.mctx.Context
import models.im.{MImg3, MImgT}
import play.api.Configuration
import play.api.mvc.Call
import util.PlayMacroLogsImpl
import util.img.DynImgUtil

import scala.collection.JavaConversions._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.10.14 18:31
 * Description: Утииль для работы с CDN.
 */
@Singleton
class CdnUtil @Inject() (
  dynImgUtil      : DynImgUtil,
  configuration   : Configuration,
  corsUtil        : CorsUtil
)
  extends PlayMacroLogsImpl
{

  import LOGGER._

  /** Прочитать из конфига список CDN-хостов для указанного протокола. */
  private def _getCdnHostsForProto(proto: String): List[String] = {
    configuration.getStringList("cdn.hosts." + proto)
      .fold (List.empty[String]) (_.toList)
  }

  /** Карта протоколов и списков CDN-хостов, которые готовые обслуживать запросы. */
  val CDN_PROTO_HOSTS: Map[String, List[String]] = {
    configuration.getStringList("cdn.protocols")
      .fold [Iterator[String]] (Iterator("http", "https"))  { protosRaw =>
        protosRaw
          .iterator()
          .map(_.trim.toLowerCase)
      }
      .map { proto =>
        proto -> _getCdnHostsForProto(proto)
      }
      .filter { _._2.nonEmpty }
      .toMap
  }

  /** Раздавать ли шрифты через CDN? Дергается из шаблонов. Если Cors отключен, то этот параметр тоже отключается. */
  val FONTS_ENABLED: Boolean = {
    configuration.getBoolean("cdn.fonts.enabled")
      .exists(_ && corsUtil.IS_ENABLED)
  }

  /** Отключено использование CDN на хостах: */
  val DISABLED_ON_HOSTS: Set[String] = {
    configuration.getStringList("cdn.disabled.on.hosts")
      .fold (Set.empty[String]) (_.toSet)
  }

  // Печатаем карту в консоль при запуске.
  info {
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
    info(s"CDNs disabled on hosts: " + DISABLED_ON_HOSTS.mkString(", "))
  }

  val HAS_ANY_CDN: Boolean = CDN_PROTO_HOSTS.nonEmpty


  /** Выбрать подходящий CDN-хост для указанного протокола. */
  def chooseHostForProto(protoLc: String): Option[String] = {
    CDN_PROTO_HOSTS.get(protoLc)
      .flatMap(_.headOption)    // TODO Выбирать рандомный хост из списка хостов.
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
          if !DISABLED_ON_HOSTS.contains(reqHost) &&
            !(cdnHost equalsIgnoreCase reqHost)
        } yield {
          protoLc + "://" + cdnHost
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

  /** Вызов к dynImg через CDN. */
  def dynImg(dargs: MImgT)(implicit ctx: Context): Call = {
    forCall( dynImgUtil.imgCall(dargs) )
  }
  def dynImg(filename: String)(implicit ctx: Context): Call = {
    val img = MImg3(filename)
    dynImg(img)
  }

  /** Бывает, что нужно в зависимости от значения флага генерить полные и относительные ссылки.
    *
    * @param forceAbsoluteUrl true -- нужна абсолютная ссылка. false -- хватит и относительной.
    * @param call исходный вызов.
    * @return Строка с ссылкой.
    */
  def maybeAbsUrl(forceAbsoluteUrl: Boolean)(call: Call)(implicit ctx: Context): String = {
    if (forceAbsoluteUrl) {
      import ctx.request
      call.absoluteURL()
    } else {
      call.url
    }
  }

}

/** Интерфейс для доступа к DI-полю с инстансом [[CdnUtil]]. */
trait ICdnUtilDi {
  def cdnUtil: CdnUtil
}
