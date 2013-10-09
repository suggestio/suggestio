package util.acl

import play.api.mvc._
import scala.concurrent.Future
import models.{MDomainAuthzT, MDomainQiAuthzTmp, MPersonDomainAuthzAdmin, MPersonDomainAuthz}
import util.DomainQi
import io.suggest.util.UrlUtil
import util.acl.PersonWrapper.PwOpt_t

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.10.13 19:04
 * Description: ACL-декоратор для проверки прав на управление доменом.
 */

object IsDomainAdmin {

  /** При проблемах с доступом используется эта функция по дефолту. */
  def onUnauth(hostname:String, pwOpt: PwOpt_t, req: RequestHeader): SimpleResult = {
    pwOpt match {
      // Юзер залогинен, но долбится на чужой домен
      case Some(_) => Results.Forbidden(s"Current user have no access to '$hostname'")

      // Юзер не залогинен, и долбится на сайт без соотв. qi-сессии.
      case None    => IsAuth.onUnauthDefault(req)
    }
  }

  /** Фьючерс для onUnauth() */
  def onUnauthFut(hostname: String, pwOpt: PwOpt_t, req:RequestHeader): Future[SimpleResult] = {
    Future.successful(onUnauth(hostname, pwOpt, req))
  }

}


/** Абстрактный класс для различнх реализаций сабжа. */
abstract class IsDomainAdminAbstract extends ActionBuilder[AbstractRequestWithDAuthz] {

  def hostname: String

  protected def invokeBlock[A](request: Request[A], block: (AbstractRequestWithDAuthz[A]) => Future[SimpleResult]): Future[SimpleResult] = {
    val dkey = UrlUtil.normalizeHostname(hostname)
      val pwOpt = PersonWrapper.getFromRequest(request)
      val authzInfoOpt: Option[MDomainAuthzT] = pwOpt match {
        // Анонимус. Возможно, он прошел валидацию уже. Нужно узнать из сессии текущий qi_id и проверить по базе.
        case None =>
          DomainQi.getQiFromSession(dkey)(request.session) flatMap { qi_id =>
            MDomainQiAuthzTmp.get(dkey=dkey, id=qi_id) filter(_.isValid)
          }

        // Юзер залогинен. Проверить права.
        case Some(pw) =>
          // TODO Надо проверить случай, когда у админа suggest.io есть добавленный домен. Всё ли нормально работает?
          // Если нет, то надо обращение к модели вынести на первый план, а только потом уже проверять isAdmin.
          if (pw.isAdmin) {
            Some(MPersonDomainAuthzAdmin(person_id=pw.id, dkey=dkey))
          } else {
            MPersonDomainAuthz.getForPersonDkey(dkey, pw.id) filter(_.isValid)
          }
      }
      authzInfoOpt match {
        case Some(authzInfo) =>
          val req1 = new RequestWithDAuthz(pwOpt, authzInfo, request)
          block(req1)

        case None => onUnauthFut(pwOpt, request)
      }
  }

  def onUnauthFut(pwOpt: PwOpt_t, request: RequestHeader): Future[SimpleResult]
}

/**
 * Дефолтовая реализация декоратора ACL для проверки домена. Логика отсылки юзера описана в IsDomainAdmin.onUnauth()
 * @param hostname Сырое имя хоста.
 */
case class IsDomainAdmin(hostname:String) extends IsDomainAdminAbstract {
  def onUnauthFut(pwOpt: PwOpt_t, request: RequestHeader) = {
    IsDomainAdmin.onUnauthFut(hostname, pwOpt, request)
  }
}

