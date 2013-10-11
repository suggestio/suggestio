package util.acl

import play.api.mvc._
import scala.concurrent.Future
import models.{MDomainAuthzT, MDomainQiAuthzTmp, MPersonDomainAuthzAdmin, MPersonDomainAuthz}
import util.DomainQi
import io.suggest.util.UrlUtil
import util.acl.PersonWrapper.PwOpt_t
import play.api.libs.concurrent.Execution.Implicits._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.10.13 19:04
 * Description: ACL-декоратор для проверки прав на управление доменом.
 */

object IsDomainAdmin {

  /** Фьючерс для onUnauth() */
  def onUnauthFut(hostname: String, pwOpt: PwOpt_t, req:RequestHeader): Future[SimpleResult] = {
    pwOpt match {
      // Юзер залогинен, но долбится на чужой домен
      case Some(_) =>
        val result = Results.Forbidden(s"Current user have no access to '$hostname'")
        Future.successful(result)

      // Юзер не залогинен, и долбится на сайт без соотв. qi-сессии. Повторяем логику из IsAuth.
      case None => IsAuth.onUnauth(req)
    }
  }

}


/** Абстрактный класс для различных реализаций сабжа. Основная цель декомпозиции: позволить контроллерам легко
  * переопределять путь редиректа в случае проблемы с правами, а так же менять другие части логики работы. */
abstract class IsDomainAdminAbstract extends ActionBuilder[AbstractRequestWithDAuthz] {

  def hostname: String

  protected def invokeBlock[A](request: Request[A], block: (AbstractRequestWithDAuthz[A]) => Future[SimpleResult]): Future[SimpleResult] = {
    val dkey = UrlUtil.normalizeHostname(hostname)
    val pwOpt = PersonWrapper.getFromRequest(request)
    val authzInfoOptFut: Future[Option[MDomainAuthzT]] = pwOpt match {
      // Анонимус. Возможно, он прошел валидацию уже. Нужно узнать из сессии текущий qi_id и проверить по базе.
      case None =>
        DomainQi.getQiFromSession(dkey)(request.session) match {
          case Some(qi_id) => MDomainQiAuthzTmp.getForDkeyId(dkey=dkey, id=qi_id)
          case None        => Future.successful(None)
        }

      // Юзер залогинен. Проверить права.
      case Some(pw) =>
        // TODO Надо проверить случай, когда у админа suggest.io есть добавленный домен. Всё ли нормально работает?
        // Если нет, то надо обращение к модели вынести на первый план, а только потом уже проверять isAdmin.
        if (pw.isAdmin) {
          Future.successful(
            Some(MPersonDomainAuthzAdmin(person_id=pw.id, dkey=dkey))
          )
        } else {
          MPersonDomainAuthz.getForPersonDkey(dkey, pw.id)
        }
    }
    authzInfoOptFut flatMap {
      case Some(authzInfo) if authzInfo.isValid =>
        val req1 = new RequestWithDAuthz(pwOpt, authzInfo, request)
        block(req1)

      case _ => onUnauthFut(pwOpt, request)
    }
  }

  def onUnauthFut(pwOpt: PwOpt_t, request: RequestHeader): Future[SimpleResult]
}

/**
 * Дефолтовая реализация декоратора ACL для проверки прав на домен. При проблеме с доступом, юзер отсылается согласно
 * статической IsDomainAdmin.onUnauthFut().
 * @param hostname Сырое имя хоста.
 */
case class IsDomainAdmin(hostname: String) extends IsDomainAdminAbstract {
  def onUnauthFut(pwOpt: PwOpt_t, request: RequestHeader) = {
    IsDomainAdmin.onUnauthFut(hostname, pwOpt, request)
  }
}

