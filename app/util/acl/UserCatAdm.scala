package util.acl

import models._
import play.api.mvc.{SimpleResult, Request, ActionBuilder}
import scala.concurrent.Future
import util.acl.PersonWrapper.PwOpt_t
import util.SiowebEsUtil.client
import play.api.libs.concurrent.Execution.Implicits.defaultContext

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.03.14 14:46
 * Description: Управление пользовательскими категориями. В основном для дедубликации кода между экшенами.
 */
object UserCatAdm {
  def apply(ownerId: Option[String], catId: Option[String]): ActionBuilder[RequestUserCatAdm] = {
    if (catId.isEmpty) {
      if (ownerId.isEmpty) {
        new UnauthCatAdm
      } else {
        TreeUserCatAdm(ownerId.get)
      }
    } else {
      UserCatAdm(catId.get)
    }
  }
}

/** Пока нет конкретной категории, с которой работаем. */
case class TreeUserCatAdm(ownerId: String) extends ActionBuilder[RequestUserCatAdm] {
  protected def invokeBlock[A](request: Request[A], block: (RequestUserCatAdm[A]) => Future[SimpleResult]): Future[SimpleResult] = {
    // TODO нужно проверить права над указанным субъектов ownerId.
    val pwOpt = PersonWrapper.getFromRequest(request)
    if (PersonWrapper isSuperuser pwOpt) {
      val req1 = RequestUserCatAdm(None, ownerId, pwOpt, request)
      block(req1)
    } else {
      IsAuth.onUnauth(request)
    }
  }
}

/** Есть конкретная категория, с которой работаем. */
case class UserCatAdm(catId: String) extends ActionBuilder[RequestUserCatAdm] {
  protected def invokeBlock[A](request: Request[A], block: (RequestUserCatAdm[A]) => Future[SimpleResult]): Future[SimpleResult] = {
    val pwOpt = PersonWrapper.getFromRequest(request)
    if (PersonWrapper isSuperuser pwOpt) {
      MMartCategory.getById(catId) flatMap {
        case Some(cat)  =>
          val req1 = RequestUserCatAdm(Some(cat), cat.ownerId, pwOpt, request)
          block(req1)

        case _ => IsAuth.onUnauth(request)
      }
    } else {
      IsAuth.onUnauth(request)
    }
  }
}

/** Статический ActionBuilder на случай, если заведомо у юзера нет прав доступа. Такое бывает при неправильном вызове.
  * Реализован в виде класса из-за музейной редкости подобных запросов. */
class UnauthCatAdm extends ActionBuilder[RequestUserCatAdm] {
  protected def invokeBlock[A](request: Request[A], block: (RequestUserCatAdm[A]) => Future[SimpleResult]): Future[SimpleResult] = {
    IsAuth onUnauth request
  }
}

/** Реквест, в тело которого забита категория, к которой адресован реквест. И корректный ownerId тоже. */
case class RequestUserCatAdm[A](cat: Option[MMartCategory], ownerId:String, pwOpt: PwOpt_t, request: Request[A])
  extends AbstractRequestWithPwOpt(request)
