package models.mctx

import java.util.UUID

import _root_.models.im.DevScreen
import com.google.inject.assistedinject.Assisted
import com.google.inject.{Inject, Singleton}
import controllers.routes
import io.suggest.util.UuidUtil
import models.mproj.IMCommonDi
import models.req.ExtReqHdr.{firstForwarded, lastForwarded}
import models.req.IReqHdr
import org.joda.time.DateTime
import play.api.Play.{configuration, current, isDev}
import play.api.http.HeaderNames._
import play.api.i18n.Messages
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

// TODO Заинжектить инжектить этот объект в ContextApi.
object Context extends MyHostsT {

  val mobileUaPattern = "(iPhone|webOS|iPod|Android|BlackBerry|mobile|SAMSUNG|IEMobile|OperaMobi)".r.unanchored
  val isIpadRe = "iPad".r.unanchored
  val isIphoneRe = "iPhone".r.unanchored

  /** Основной хост и порт, на котором крутится выдача sio-market. */
  override val SC_HOST_PORT = configuration.getString("sio.sc.hostport") getOrElse "www.suggest.io"
  override val SC_PROTO = configuration.getString("sio.sc.proto") getOrElse "http"
  override val SC_URL_PREFIX = SC_PROTO + "://" + SC_HOST_PORT

  /** Хост и порт, на котором живёт часть сервиса с ограниченным доступом. */
  override val LK_HOST_PORT = configuration.getString("sio.lk.hostport") getOrElse "my.suggest.io"
  override val LK_PROTO = configuration.getString("sio.lk.proto") getOrElse "https"
  override val LK_URL_PREFIX = LK_PROTO + "://" + LK_HOST_PORT

  /** Дефолтовый хост и порт. Используется, когда по стечению обстоятельств, нет подходящего значения для хоста. */
  override val DFLT_HOST_PORT = configuration.getString("sio.hostport.dflt") getOrElse "suggest.io"
  /** Протокол, используемый при генерации ссылок на suggest.io. Обычно на локалхостах нет https вообще, в
    * то же время, на мастере только https. */
  override val DFLT_PROTO: String = configuration.getString("sio.proto.dflt") getOrElse "http"

  /** Регэксп для поиска в query string параметра, который хранит параметры клиентского экрана. */
  val SCREEN_ARG_NAME_RE = "a\\.screen".r

  /** Доверять ли заголовку Host: ? Обычно нет, т.к. nginx туда втыкает localhost.
    * Имеет смысл выставлять true на локалхостах разработчиков s.io. */
  val TRUST_HOST_HDR = configuration.getBoolean("sio.req.headers.host.trust") getOrElse false
  
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

}


/** Интерфейс констант для статической и динамической части контекста. Используется для самозащиты от ошибок в коде. */
trait MyHostsT {
  def SC_HOST_PORT: String
  def SC_PROTO: String
  def SC_URL_PREFIX: String

  def LK_HOST_PORT: String
  def LK_PROTO: String
  def LK_URL_PREFIX: String

  def DFLT_HOST_PORT: String
  def DFLT_PROTO: String
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
trait Context extends MyHostsT {

  /** Доступ к DI-инжектируемым сущностям.
    * Например, к утили какой-нить или DI-моделям и прочей утвари. */
  val api: ContextApi

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
      .getOrElse(Context.DFLT_PROTO)
      .toLowerCase
  }

  /** Является ли текущий коннекшен шифрованным? */
  lazy val isSecure: Boolean = myProto == "https"

  /** Если порт указан, то будет вместе с портом. Значение задаётся в конфиге. */
  lazy val myHost: String = {
    // Извлечь запрошенный хостнейм из данных форварда.
    var maybeHost = request.headers
      .get(X_FORWARDED_HOST)        // TODO Желательно ещё отрабатывать нестандартные порты.
      .map(_.trim)
      .filter(!_.isEmpty)
      .map { raw =>
        val h = lastForwarded(raw)
        // Если входящий запрос на backend, то нужно отобразить его на www.
        Context.BACKEND_HOST_RE.replaceFirstIn(h, "www.")
      }
    // Если форвард не найден, а конфиг разрешает доверять Host: заголовку, то дергаем его.
    if (maybeHost.isEmpty && Context.TRUST_HOST_HDR) {
      maybeHost = request.headers
        .get(HOST)
        .filter(!_.isEmpty)
    }
    // Нередко, тут недосягаемый код:
    if (maybeHost.isEmpty)
      Context.DFLT_HOST_PORT
    else
      maybeHost.get
  }

  lazy val currAudienceUrl: String = myProto + "://" + myHost

  implicit lazy val now : DateTime = DateTime.now

  def userAgent: Option[String] = request.headers.get(USER_AGENT)

  def uaMatches(re: Regex): Boolean = {
    userAgent.exists { x =>
      re.pattern.matcher(x).find()
    }
  }

  lazy val isMobile : Boolean = uaMatches(Context.mobileUaPattern)
  lazy val isIpad: Boolean = uaMatches(Context.isIpadRe)
  lazy val isIphone: Boolean = uaMatches(Context.isIphoneRe)

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
      .filter {
        case (k, _)  =>  Context.SCREEN_ARG_NAME_RE.pattern.matcher(k).matches()
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

  override def SC_HOST_PORT   = Context.SC_HOST_PORT
  override def SC_PROTO       = Context.SC_PROTO
  override def SC_URL_PREFIX  = Context.SC_URL_PREFIX
  override def LK_HOST_PORT   = Context.LK_HOST_PORT
  override def LK_PROTO       = Context.LK_PROTO
  override def LK_URL_PREFIX  = Context.LK_URL_PREFIX
  override def DFLT_HOST_PORT = Context.DFLT_HOST_PORT
  override def DFLT_PROTO     = Context.DFLT_PROTO

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
  val galleryUtil         : GalleryUtil,
  val cdn                 : CdnUtil,
  val dynImgUtil          : DynImgUtil,
  val n2NodesUtil         : N2NodesUtil
)


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

