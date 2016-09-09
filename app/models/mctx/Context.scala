package models.mctx

import java.util.UUID

import _root_.models.im.DevScreen
import com.google.inject.assistedinject.Assisted
import com.google.inject.{Inject, Singleton}
import controllers.routes
import io.suggest.playx.{ICurrentAppHelpers, ICurrentConf}
import io.suggest.util.UuidUtil
import models.mproj.IMCommonDi
import models.req.ExtReqHdr.{firstForwarded, lastForwarded}
import models.req.IReqHdr
import models.usr.MSuperUsers
import org.joda.time.DateTime
import play.api.Application
import play.api.http.HeaderNames._
import play.api.i18n.Messages
import play.api.mvc.Call
import play.api.routing.Router.Tags._
import util.cdn.CdnUtil
import util.img.{DynImgUtil, GalleryUtil}
import util.jsa.init.ITargets
import util.n2u.N2NodesUtil

import scala.util.Random
import scala.util.matching.Regex

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.04.13 12:10
 * Description: Объект контекста запроса для прозрачной передачи состояния запроса из контроллеров в шаблоны
 * и между шаблонами. Контроллеры просто расширяют этот трайт, а шаблоны просто объявляют тип возвращаемого значения
 * метода в implicit-списке параметров.
 */

/** Статическая поддержка для экземпляров [[Context]] и прочих вещей. В основном, тут всякие константы. */
@Singleton
class ContextUtil @Inject() (
  override val current: Application
)
  extends ICurrentConf
  with ICurrentAppHelpers
{

  val mobileUaPattern = "(iPhone|webOS|iPod|Android|BlackBerry|mobile|SAMSUNG|IEMobile|OperaMobi)".r.unanchored
  val isIpadRe = "iPad".r.unanchored
  val isIphoneRe = "iPhone".r.unanchored

  /** Основной хост и порт, на котором крутится выдача sio-market. */
  val SC_HOST_PORT = configuration.getString("sio.sc.hostport").getOrElse("www.suggest.io")
  val SC_PROTO = configuration.getString("sio.sc.proto").getOrElse("http")
  val SC_URL_PREFIX = SC_PROTO + "://" + SC_HOST_PORT

  /** Генерация абсолютной ссылки через выдачу на основе строке относительной ссылки. */
  def toScAbsUrl(relUrl: String): String = {
    SC_URL_PREFIX + relUrl
  }
  /** Генерация абсолютной ссылки через выдачу на основе экземпляра Call. */
  def toScAbsUrl(call: Call): String = {
    toScAbsUrl( call.url )
  }

  /** Хост и порт, на котором живёт часть сервиса с ограниченным доступом. */
  val LK_HOST_PORT = configuration.getString("sio.lk.hostport").getOrElse("my.suggest.io")
  val LK_PROTO = configuration.getString("sio.lk.proto").getOrElse("https")
  val LK_URL_PREFIX = LK_PROTO + "://" + LK_HOST_PORT

  /** Дефолтовый хост и порт. Используется, когда по стечению обстоятельств, нет подходящего значения для хоста. */
  val DFLT_HOST_PORT = configuration.getString("sio.hostport.dflt").getOrElse("suggest.io")

  /** Протокол, используемый при генерации ссылок на suggest.io. Обычно на локалхостах нет https вообще, в
    * то же время, на мастере только https. */
  val DFLT_PROTO: String = configuration.getString("sio.proto.dflt").getOrElse("http")

  /** Регэксп для поиска в query string параметра, который хранит параметры клиентского экрана. */
  val SCREEN_ARG_NAME_RE = "a\\.screen".r

  /** Доверять ли заголовку Host: ? Обычно нет, т.к. nginx туда втыкает localhost.
    * Имеет смысл выставлять true на локалхостах разработчиков s.io. */
  val TRUST_HOST_HDR = configuration.getBoolean("sio.req.headers.host.trust")
    .contains(true) // getOrElse false
  
  val BACKEND_HOST_RE = "^backend\\.".r

  /** Бывает, что необходимо заменить локалхост на 127.0.0.1. Например, при разработке под твиттер.
    * @param source Исходная строка, т.е. ссылка, или её префикс или хостнейм.
    * @return Подправленная исходная строка.
    */
  def devReplaceLocalHostW127001(source: String): String = {
    if (isDev)
      source.replaceFirst("localhost", "127.0.0.1")
    else
      source
  }

  private val _protoPortRe = ":[0-9]+".r
  def removePortFromHostPort(hostPort: String): String = {
    _protoPortRe.replaceAllIn(hostPort, "")
  }

  /** Список собственных хостов (доменов) системы suggest.io. */
  val SIO_HOSTS: Set[String] = {
    Iterator(SC_HOST_PORT, DFLT_HOST_PORT, LK_HOST_PORT)
      .map { removePortFromHostPort }
      .toSet
  }

  /** Относится ли хост в запросе к собственным хостам suggest.io. */
  def isMyHostSio(myHost: String) = {
    // По идее, надо бы фильтровать тут левые адреса, но пока надеемся на nginx
    // и на около-нулевую возможную опасность возможной уязвимости на фоне блокирующего резолва внутрях InetAddress.
    /*val inet = InetAddress.getByName(myHost)
    inet.isLinkLocalAddress ||
      inet.isLoopbackAddress ||
      inet.isMulticastAddress || */
      SIO_HOSTS.contains(myHost)
  }

}

/** Интерфейс для DI-поля с инстансом [[ContextUtil]] внутри. */
trait IContextUtilDi {
  def ctxUtil: ContextUtil
}


/** Трейт-аддон для контроллеров, которым нужен доступ к сборке контекстов. */
trait ContextT { this: ITargets with IMCommonDi =>

  /**
   * Выдать контекст. Неявно вызывается при вызове шаблона из контроллера.
   * @return Экземпляр [[Context]].
   */
  implicit final def getContext2(implicit
                                 request  : IReqHdr,
                                 messages : Messages,
                                 ctxData  : CtxData = CtxData.empty): Context = {
    // Получить js init targets с уровня контроллера, объеденить с остальными, залить их в data.
    val ctxData1 = ctxData.prependJsiTgs(
      jsiTgs(request),
      request.user.jsiTgs
    )
    // Собрать контекст с обновлёнными данными в ctxData.
    mCommonDi.contextFactory.create(request, messages, ctxData1)
  }
}


/** Базовый трейт контекста. Используется всеми шаблонами и везде. Переименовывать и менять нельзя.
  * Интерфейс можно только расширять и аккуратно рефакторить, иначе хана.
  */
trait Context {

  /** Доступ к DI-инжектируемым сущностям.
    * Например, к утили какой-нить или DI-моделям и прочей утвари. */
  val api: ContextApi

  import api.ctxUtil

  def withData(data1: CtxData): Context

  // abstract val вместо def'ов в качестве возможной оптимизации обращений к ним со стороны scalac и jvm. Всегда можно вернуть def.

  /** Данные текущего реквеста. */
  implicit val request: IReqHdr

  /** Укороченный доступ к пользовательским данным sio-реквеста. */
  def user = request.user

  /** Текущий язык запроса. Определеляется в контроллерах на основе запроса. */
  implicit val messages: Messages

  /** Для быстрого задания значений r-параметров (path для возврата, см. routes) можно использовать этот метод. */
  def r = Some(request.path)

  /** Используемый протокол. */
  lazy val myProto: String = {
    request.headers
      .get(X_FORWARDED_PROTO)
      .filter(!_.isEmpty)
      .map { firstForwarded }
      .getOrElse( ctxUtil.DFLT_PROTO )
      .toLowerCase
  }

  /** Является ли текущий коннекшен шифрованным? */
  lazy val isSecure: Boolean = myProto == "https"

  /** Если порт указан, то будет вместе с портом. Значение задаётся в конфиге. */
  lazy val myHost: String = {
    // Попытаться извлечь запрошенный хостнейм из данных форварда.
    var maybeHostPort = for {
      xfhHdr0 <- request.headers.get(X_FORWARDED_HOST)
      xfhHdr  = xfhHdr0.trim
      if xfhHdr.nonEmpty
    } yield {
      val h = lastForwarded(xfhHdr)
      // Если входящий запрос на backend, то нужно отобразить его на www.
      ctxUtil.BACKEND_HOST_RE
        .replaceFirstIn(h, "www.")
        .toLowerCase
    }

    // Если форвард не найден, а конфиг разрешает доверять Host: заголовку, то дергаем его.
    if (maybeHostPort.isEmpty && ctxUtil.TRUST_HOST_HDR) {
      maybeHostPort = request.headers
        .get(HOST)
        .filter(!_.isEmpty)
    }

    // Вернуть хоть какой-нибудь хост.
    maybeHostPort.fold(ctxUtil.DFLT_HOST_PORT)(ctxUtil.removePortFromHostPort)
  }

  implicit lazy val now : DateTime = DateTime.now

  def userAgent: Option[String] = request.headers.get(USER_AGENT)

  def uaMatches(re: Regex): Boolean = {
    userAgent.exists { x =>
      re.pattern.matcher(x).find()
    }
  }

  lazy val isMobile : Boolean = uaMatches(ctxUtil.mobileUaPattern)
  lazy val isIpad: Boolean = uaMatches(ctxUtil.isIpadRe)
  lazy val isIphone: Boolean = uaMatches(ctxUtil.isIphoneRe)

  lazy val isDebug: Boolean     = request.getQueryString("debug").isDefined

  lazy val timestamp: Long = now.toInstant.getMillis

  /** Локальный ГСЧ, иногда нужен. */
  lazy val PRNG = new Random(System.currentTimeMillis())

  /** Рандомный id, существующий в рамках контекста.
    * Использутся, когда необходимо как-то индентифицировать весь текущий рендер (вебсокеты, например). */
  lazy val ctxId = UUID.randomUUID()
  lazy val ctxIdStr = UuidUtil.uuidToBase64(ctxId)

  /** Собрать ссылку на веб-сокет с учетом текущего соединения. */
  lazy val wsUrlPrefix: String = {
    val sb = new StringBuilder(32, "ws")
    if (isSecure)
      sb.append('s')
    sb.append("://")
      .append(myHost)
      .toString()
  }

  /** Пользователю может потребоваться помощь на любой странице. Нужны генератор ссылок в зависимости от обстоятельств. */
  def supportFormCall(adnIdOpt: Option[String] = None) = {
    val r = Some(request.path)
    adnIdOpt match {
      case Some(adnId) => routes.MarketLkSupport.supportFormNode(adnId, r)
      case None        => routes.MarketLkSupport.supportForm(r)
    }
  }

  /** Параметры экрана клиентского устройства. Эти данные можно обнаружить внутри query string. */
  lazy val deviceScreenOpt: Option[DevScreen] = {
    request.queryString
      .iterator
      .filter { case (k, _) =>
        ctxUtil.SCREEN_ARG_NAME_RE.pattern.matcher(k).matches()
      }
      .flatMap {
        case kv @ (k, vs) =>
          DevScreen.qsb
            .bind(k, Map(kv))
            .filter(_.isRight)
            .map(_.right.get)
      }
      .toStream
      .headOption
  }

  /**
   * Текущий контроллер, если вызывается. (fqcn)
   * @return "controllers.LkAdvExt" например.
   */
  def controller: Option[String] = request.tags.get(RouteController)

  /**
   * Короткое имя класса.
   * @return "LkAdvExt".
   */
  def controllerSimple = controller.map { className =>
    val i = className.lastIndexOf('.')
    className.substring(i + 1)
  }

  /**
   * Текущий экшен, если исполняется.
   * @return "forAd"
   */
  def action: Option[String] = request.tags.get(RouteActionMethod)

  /** Кастомные данные в контексте. */
  def data: CtxData

}


/** Шаблонам бывает нужно залезть в util'ь, модели или ещё куда-нить.
  * DI препятствует этому, поэтому необходимо обеспечивать доступ с помощью класса-костыля. */
@Singleton
class ContextApi @Inject() (
  override val ctxUtil    : ContextUtil,
  val galleryUtil         : GalleryUtil,
  val cdn                 : CdnUtil,
  val dynImgUtil          : DynImgUtil,
  val n2NodesUtil         : N2NodesUtil,
  val mSuperUsers         : MSuperUsers,
  override implicit val current: Application
)
  extends ICurrentAppHelpers
  with IContextUtilDi


/** Guice factory для сборки контекстов с использованием DI.
  * У шаблонов бывает необходимость залезть в util/models к статическим вещам. */
trait Context2Factory {
  /**
   * Сборка контекста, код метода реализуется автоматом через Guice Assisted inject.
   * @see [[GuiceDiModule]] для тюнинга assisted-линковки контекста.
   */
  def create(implicit request: IReqHdr, messages: Messages, ctxData: CtxData): Context2
}


// Непосредственные реализации контекстов. Расширять их API в обход trait Context не имеет смысла.

/** Основная реализация контекста, с которой работают sio-контроллеры автоматически. */
case class Context2 @Inject() (
  override val api                          : ContextApi,
  @Assisted override val data               : CtxData,
  @Assisted implicit override val request   : IReqHdr,
  @Assisted implicit override val messages  : Messages
)
  extends Context
{
  override def withData(data1: CtxData): Context = {
    copy(data = data1)
  }
}

