package util.acl

import play.api.mvc._
import scala.concurrent.Future
import models.{MDomainAuthzT, MDomainQiAuthzTmp, MPersonDomainAuthzSuperuser, MPersonDomainAuthz}
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
  def onUnauthFut(hostname: String, pwOpt: PwOpt_t, req:RequestHeader): Future[Result] = {
    pwOpt match {
      // Юзер залогинен, но долбится на чужой домен
      case Some(_) =>
        val result = Results.Forbidden(s"Current user have no access to '$hostname'")
        Future.successful(result)

      // Юзер не залогинен, и долбится на сайт без соотв. qi-сессии. Повторяем логику из IsAuth.
      case None => IsAuth.onUnauth(req)
    }
  }

  /** Определить, может ли указанный юзер осуществлять управление указанным доменом.
   * @param dkey Ключ домена.
   * @param pwOpt Данные о текущем юзере.
   * @param request Реквест на случай необходимости чтения сессии.
   * @return None если нет прав. Some(authz) если есть причина, по которой юзеру следует разрешить управлять сайтом.
   */
  def isDkeyAdmin(dkey: String, pwOpt:PwOpt_t, request: RequestHeader): Future[Option[MDomainAuthzT]] = {
    pwOpt match {
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
        if (pw.isSuperuser) {
          val result = Some(MPersonDomainAuthzSuperuser(person_id=pw.personId, dkey=dkey))
          Future.successful(result)
        } else {
          MPersonDomainAuthz.getForPersonDkey(dkey, pw.personId)
        }
    }
  }

}


/** Абстрактный класс для различных реализаций сабжа. Основная цель декомпозиции: позволить контроллерам легко
  * переопределять путь редиректа в случае проблемы с правами, а так же менять другие части логики работы. */
abstract class IsDomainAdminAbstract extends ActionBuilder[AbstractRequestWithDAuthz] {

  def hostname: String

  override def invokeBlock[A](request: Request[A], block: (AbstractRequestWithDAuthz[A]) => Future[Result]): Future[Result] = {
    val pwOpt = PersonWrapper.getFromRequest(request)
    val srmFut = SioReqMd.fromPwOpt(pwOpt)
    val dkey = UrlUtil.normalizeHostname(hostname)
    IsDomainAdmin.isDkeyAdmin(dkey, pwOpt, request) flatMap {
      case Some(authzInfo) if authzInfo.isValid =>
        srmFut flatMap { srm =>
          val req1 = new RequestWithDAuthz(pwOpt, authzInfo, request, srm)
          block(req1)
        }

      case _ => onUnauthFut(pwOpt, request)
    }
  }

  def onUnauthFut(pwOpt: PwOpt_t, request: RequestHeader): Future[Result]
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

