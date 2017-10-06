package models.mctx

import java.net.IDN
import java.time.{Instant, OffsetDateTime, ZoneId}

import _root_.models.im.{DevScreen, MImgT}
import com.google.inject.assistedinject.Assisted
import javax.inject.{Inject, Singleton}

import controllers.routes
import io.suggest.i18n.MessagesF_t
import io.suggest.playx.{ICurrentAppHelpers, IsAppModes}
import io.suggest.common.empty.OptionUtil.BoolOptOps
import io.suggest.ctx.{CtxData, MCtxId, MCtxIds}
import models.mproj.IMCommonDi
import models.req.IReqHdr
import models.usr.MSuperUsers
import play.api.{Application, Configuration, Environment}
import play.api.http.HeaderNames._
import play.api.i18n.Messages
import play.api.mvc.Call
import util.cdn.CdnUtil
import util.i18n.JsMessagesUtil
import util.img.{DynImgUtil, GalleryUtil}
import util.jsa.init.ITargets
import util.n2u.N2NodesUtil
import util.support.SupportUtil

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
final class ContextUtil @Inject() (
                                    env           : Environment,
                                    configuration : Configuration
                                  )
  extends IsAppModes
{

  override protected def appMode = env.mode

  val mobileUaPattern = "(iPhone|webOS|iPod|Android|BlackBerry|mobile|SAMSUNG|IEMobile|OperaMobi)".r.unanchored
  val isIpadRe = "iPad".r.unanchored
  val isIphoneRe = "iPhone".r.unanchored

  /** Регэксп для поиска в query string параметра, который хранит параметры клиентского экрана. */
  val SCREEN_ARG_NAME_RE = "a\\.s(creen)?".r


  /** Самое дефолтовое имя главного домена. */
  private def MAIN_DOMAIN_DFLT = "suggest.io"

  /** На dev-системах удобно глобально выключить https в конфиге. */
  val HTTPS_DISABLED: Boolean = configuration.getOptional[Boolean]("sio.https.disabled").getOrElseFalse

  def HTTPS_ENABLED: Boolean = !HTTPS_DISABLED

  /** Дефолтовый протокол работы suggest.io. */
  def PROTO: String = {
    if (HTTPS_DISABLED) "http" else "https"
  }

  /** Хост:порт сайта suggest.io. */
  val HOST_PORT: String = configuration.getOptional[String]("sio.hostport.dflt").getOrElse(MAIN_DOMAIN_DFLT)

  /** Префикс абсолютных ссылок на сайт. */
  val URL_PREFIX: String = PROTO + "://" + HOST_PORT


  /** Основной хост и порт, на котором крутится выдача sio-market. */
  def SC_HOST_PORT: String = HOST_PORT
  def SC_PROTO: String = PROTO
  def SC_URL_PREFIX: String = URL_PREFIX

  /** Генерация абсолютной ссылки через выдачу на основе строке относительной ссылки. */
  def toScAbsUrl(relUrl: String): String = {
    SC_URL_PREFIX + relUrl
  }
  /** Генерация абсолютной ссылки через выдачу на основе экземпляра Call. */
  def toScAbsUrl(call: Call): String = {
    toScAbsUrl( call.url )
  }

  /** Хост и порт, на котором живёт часть сервиса с ограниченным доступом. */
  def LK_HOST_PORT: String = HOST_PORT
  def LK_PROTO: String = PROTO
  def LK_URL_PREFIX: String = URL_PREFIX


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
    val hosts = Set(
      SC_HOST_PORT,
      MAIN_DOMAIN_DFLT,
      HOST_PORT,
      LK_HOST_PORT,
      "япредлагаю.com",
      "isuggest.ru"
    )
    for (h <- hosts) yield {
      IDN.toASCII(
        removePortFromHostPort(h)
      )
    }
  }

  /** Относится ли хост в запросе к собственным хостам suggest.io. */
  def isMyHostSio(myHost: String): Boolean = {
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
  /** Функция типа MessagesF_t, которую можно использовать в кросс-платформенном коде. */
  def messagesF: MessagesF_t = messages( _, _: _* )

  /** Для быстрого задания значений r-параметров (path для возврата, см. routes) можно использовать этот метод. */
  def r = Some(request.uri)

  /** Объект-timestamp текущего контекста. */
  lazy val instant = Instant.now()

  /** Целочисленный timestamp текущего контекста в миллисекундах. */
  lazy val timestamp: Long = instant.toEpochMilli

  /** Текущие дата-время. */
  implicit lazy val now: OffsetDateTime = {
    // TODO Текущее время сейчас привязано к часовому поясу сервера/jvm. Это не хорошо.
    // TODO Нужно выбирать часовой пояс исходя из текущего клиента. Но это наверное будет Future[OffsetDateTime/ZonedDateTime]?
    instant
      .atZone( ZoneId.systemDefault() )
      .toOffsetDateTime
  }

  def userAgent: Option[String] = request.headers.get(USER_AGENT)

  def uaMatches(re: Regex): Boolean = {
    userAgent.exists { x =>
      re.pattern.matcher(x).find()
    }
  }

  def timeZone = ZoneId.systemDefault() // TODO Это не очень-то хорошая идея. Нужно из кукисов брать.

  def toOffsetTime(i: Instant): OffsetDateTime = {
    i.atZone( timeZone ).toOffsetDateTime
  }

  lazy val isMobile : Boolean = uaMatches(ctxUtil.mobileUaPattern)
  lazy val isIpad: Boolean = uaMatches(ctxUtil.isIpadRe)
  lazy val isIphone: Boolean = uaMatches(ctxUtil.isIphoneRe)

  lazy val isDebug: Boolean     = request.getQueryString("debug").isDefined


  /** Рандомный id, существующий в рамках контекста.
    * Использутся, когда необходимо как-то индентифицировать весь текущий рендер (вебсокеты, например). */
  lazy val ctxId: MCtxId = api.mCtxIds()
  lazy val ctxIdStr: String = MCtxId.intoString(ctxId)

  /** Собрать ссылку на веб-сокет с учетом текущего соединения. */
  lazy val wsUrlPrefix: String = {
    val sb = new StringBuilder(32, "ws")
    if (request.isTransferSecure)
      sb.append('s')
    sb.append("://")
      .append( request.host )
      .toString()
  }

  /** Пользователю может потребоваться помощь на любой странице. Нужны генератор ссылок в зависимости от обстоятельств. */
  def supportFormCall(adnIdOpt: Option[String] = None) = {
    val lkHelp = routes.LkHelp
    adnIdOpt match {
      case Some(adnId) => lkHelp.supportFormNode(adnId, r)
      case None        => lkHelp.supportForm(r)
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
        case kv @ (k, _) =>
          DevScreen.devScreenQsb
            .bind(k, Map(kv))
            .filter(_.isRight)
            .map(_.right.get)
      }
      .toStream
      .headOption
  }

  /** Кастомные данные в контексте. */
  def data: CtxData

  /** Генератор ссылки на ассет lk-messages.js, который содержит локализованные сообщения для client-side i18n
    * и кэшируется на клиенте. */
  def lkMessagesJs: Call = {
    routes.LkLang.lkMessagesJs(
      lang = messages.lang.code.toLowerCase,
      hash = api.jsMessagesUtil.hash
    )
  }

  def dynImgCall(img: MImgT): Call = {
    val imgCall = api.dynImgUtil.imgCall(img)
    api.cdn.forCall(imgCall)(this)
  }

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
  val jsMessagesUtil      : JsMessagesUtil,
  val mCtxIds             : MCtxIds,
  val supportUtil         : SupportUtil,
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

