package util.cdn

import com.google.inject.{Singleton, Inject}
import controllers.routes
import models.im.{MImg3_, MImgT}
import models.ExternalCall
import models.mctx.Context
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
  mImg3           : MImg3_
)
  extends PlayMacroLogsImpl
{

  import LOGGER._

  /** Прочитать из конфига список CDN-хостов для указанного протокола. */
  private def getCdnHostsForProto(proto: String): List[String] = {
    configuration.getStringList("cdn.hosts." + proto)
      .fold (List.empty[String]) (_.toList)
  }

  /** Карта протоколов и списков CDN-хостов, которые готовые обслуживать запросы. */
  val CDN_PROTO_HOSTS: Map[String, List[String]] = {
    configuration.getStringList("cdn.protocols")
      .fold [Iterator[String]] (Iterator("http", "https"))  { _.toIterator.map(_.toLowerCase) }
      .map { proto => proto -> getCdnHostsForProto(proto) }
      .filter { _._2.nonEmpty }
      .toMap
  }

  /** Раздавать ли шрифты через CDN? Дергается из шаблонов. Если Cors отключен, то этот параметр тоже отключается. */
  val FONTS_ENABLED: Boolean = {
    configuration.getBoolean("cdn.fonts.enabled")
      .exists(_ && CorsUtil.IS_ENABLED)
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
      val reqHost = ctx.myHost
      val urlPrefixOpt: Option[String] = if (DISABLED_ON_HOSTS contains reqHost) {
        None
      } else {
        val protoLc = ctx.myProto.toLowerCase
        chooseHostForProto(protoLc)
          .filter { _ => !DISABLED_ON_HOSTS.contains(reqHost) }
          .filter { cdnHost => !(cdnHost equalsIgnoreCase reqHost) }
          .map { host  =>  protoLc + "://" + host }
      }
      urlPrefixOpt match {
        case None =>
          c
        case Some(urlPrefix) =>
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
    val img = mImg3(filename)
    dynImg(img)
  }

}
