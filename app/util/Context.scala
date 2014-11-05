package util

import java.util.UUID

import controllers.routes
import io.suggest.util.UuidUtil
import models.im.DevScreen
import org.joda.time.DateTime
import play.api.i18n.Lang
import play.api.mvc.RequestHeader
import play.api.Play.{current, configuration}
import util.acl._, PersonWrapper.PwOpt_t
import play.api.http.HeaderNames._
import scala.util.Random
import SioRequestHeader.{firstForwarded, lastForwarded}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.04.13 12:10
 * Description: Объект контекста запроса для прозрачной передачи состояния запроса из контроллеров в шаблоны
 * и между шаблонами. Контроллеры просто расширяют этот трайт, а шаблоны просто объявляют тип возвращаемого значения
 * метода в implicit-списке параметров.
 */

object Context {

  /** Протокол, используемый при генерации ссылок на suggest.io. Обычно на локалхостах нет https вообще, в
    * то же время, на мастере только https. */
  val SIO_PROTO_DFLT: String = configuration.getString("sio.proto.dflt") getOrElse "http"

  val mobileUaPattern = "(iPhone|webOS|iPod|Android|BlackBerry|mobile|SAMSUNG|IEMobile|OperaMobi)".r.unanchored

  val isIpadRe = "iPad".r.unanchored
  val isIphoneRe = "iPhone".r.unanchored

  val MY_AUDIENCE_URL = controllers.Ident.AUDIENCE_URL

  val MY_HOST = configuration.getString("sio.hostport.dflt") getOrElse "suggest.io"

  /** Регэксп для поиска в query string параметра, который хранит параметры клиентского экрана. */
  val SCREEN_ARG_NAME_RE = "a\\.screen".r

  /** Доверять ли заголовку Host: ? Обычно нет, т.к. nginx туда втыкает localhost.
    * Имеет смысл выставлять true на локалхостах разработчиков s.io. */
  val TRUST_HOST_HDR = configuration.getBoolean("sio.req.headers.host.trust") getOrElse false
  
  val BACKEND_HOST_RE = "^backend\\.".r
}


trait ContextT {

  /**
   * Выдать контекст. Неявно вызывается при вызове шаблона из контроллера.
   * @return
   */
  implicit final def getContext2(implicit req:AbstractRequestWithPwOpt[_], lang:Lang): Context = {
    // TODO Следует брать дефолтовый Lang с учетом возможного ?lang=ru в qs запрашиваемой ссылки.
    //      Для этого надо override implicit def lang(implicit request: RequestHeader) в SioController.
    //      Это позволит кравелрам сопоставлять ссылку и страницу с конкретным языком. Нужно также не забыть link rel=canonical в шаблонах.
    Context2()
  }
}


/** Базовый трейт контекста. Используется всеми шаблонами и везде. Переименовывать и менять нельзя.
  * Интерфейс можно только расширять и аккуратно рефакторить, иначе хана.
  */
trait Context {

  implicit def request: RequestHeader
  implicit def pwOpt: PwOpt_t
  implicit def lang: Lang

  /** Для быстрого задания значений r-параметров (path для возврата, см. routes) можно использовать этот метод. */
  def r = Some(request.path)

  // srm в protected т.к. пока нет смысла её отображать. Раскрываем её api прямо тут:
  protected def sioReqMdOpt: Option[SioReqMd]
  def usernameOpt = sioReqMdOpt.flatMap(_.usernameOpt)
  def billBalanceOpt = sioReqMdOpt.flatMap(_.billBallanceOpt)

  /** Используемый протокол. */
  lazy val myProto: String = {
    request.headers
      .get(X_FORWARDED_PROTO)
      .filter(!_.isEmpty)
      .map { firstForwarded }
      .getOrElse(Context.SIO_PROTO_DFLT)
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
      Context.MY_HOST
    else
      maybeHost.get
  }

  lazy val currAudienceUrl: String = myProto + "://" + myHost

  def myAudienceUrl = Context.MY_AUDIENCE_URL

  implicit lazy val now : DateTime = DateTime.now

  def isAuth: Boolean = pwOpt.isDefined
  def isSuperuser: Boolean = PersonWrapper.isSuperuser(pwOpt)

  def flashMap = request.flash.data

  def userAgent: Option[String] = request.headers.get(USER_AGENT)

  lazy val isMobile : Boolean = {
    userAgent.exists { x =>
      Context.mobileUaPattern.pattern.matcher(x).find()
    }
  }

  lazy val isIpad: Boolean = {
    userAgent.exists { ua =>
      Context.isIpadRe.pattern.matcher(ua).find()
    }
  }

  lazy val isIphone: Boolean = {
    userAgent.exists { ua =>
      Context.isIphoneRe.pattern.matcher(ua).find()
    }
  }

  def langStr = lang.language

  lazy val canAddSites: Boolean = current.configuration.getBoolean("can_add_sites") getOrElse true
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
    val sb = new StringBuilder("ws")
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

}


// Непосредственные реализации контекстов. Расширять их API в обход trait Context не имеет смысла.

/** Контекст времён комбинируемых ActionBuilder'ов. */
case class Context2(
  implicit val request: AbstractRequestWithPwOpt[_],
  implicit val lang: Lang
) extends Context {
  implicit def pwOpt: PwOpt_t = request.pwOpt
  val sioReqMdOpt: Option[SioReqMd] = Some(request.sioReqMd)
}


/** Упрощенная версия контекста, используемая в минимальных условиях и вручную. */
case class ContextImpl(implicit val request: RequestHeader, val lang: Lang) extends Context {
  def pwOpt = PersonWrapper.getFromRequest(request)
  def sioReqMdOpt: Option[SioReqMd] = None
}

