package util

import org.joda.time.DateTime
import play.api.i18n.Lang
import play.api.mvc.{RequestHeader, Request}
import play.api.Play.current
import util.acl._, PersonWrapper.PwOpt_t
import io.suggest.util.UrlUtil
import java.sql.Connection

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
  val SIO_PROTO_DFLT = current.configuration.getString("sio.proto.dflt") getOrElse "https"
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
  def myProto = Context.SIO_PROTO_DFLT
  def myAudienceUrl = controllers.Ident.AUDIENCE_URL

  implicit lazy val now : DateTime = DateTime.now

  def isAuth:  Boolean = pwOpt.isDefined
  def isSuperuser: Boolean = pwOpt.exists(_.isSuperuser)

  def userAgent:  Option[String] = request.headers.get("User-Agent")

  val mobileUaPattern = "(iPhone|webOS|iPod|Android|BlackBerry|mobile|SAMSUNG|IEMobile|OperaMobi)".r.unanchored
  def isMobile : Boolean = {
    userAgent.exists(agent => {
      agent match {
        case mobileUaPattern(a) => true
        case _ => false
      }
    })
  }


  def langStr = lang.language

  lazy val canAddSites: Boolean = current.configuration.getBoolean("can_add_sites") getOrElse true
  lazy val isDebug: Boolean     = request.getQueryString("debug").isDefined

  lazy val timestamp: Long = now.toInstant.getMillis
}


// Непосредственные реализации контекстов. Расширять их API в обход trait Context не имеет смысла.

/** Контекст времён комбинируемых ActionBuilder'ов. */
case class Context2(
  implicit val request: AbstractRequestWithPwOpt[_],
  implicit val lang: Lang
) extends Context {
  implicit def pwOpt: PwOpt_t = request.pwOpt
}


/** Упрощенная версия контекста, используемая в минимальных условиях и вручную. */
case class ContextImpl(implicit val request: RequestHeader, val lang: Lang) extends Context {
  def pwOpt = PersonWrapper.getFromRequest(request)
}

