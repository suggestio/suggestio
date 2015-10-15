package util.acl

import io.suggest.di.{IExecutionContext, IEsClient}
import models._
import models.req.SioReqMd
import play.api.mvc.{Result, Request, ActionBuilder}
import scala.concurrent.Future
import util.acl.PersonWrapper.PwOpt_t

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.03.14 14:46
 * Description: Управление пользовательскими категориями. В основном для дедубликации кода между экшенами.
 */

trait UserCatAdm
  extends IEsClient
  with IExecutionContext
  with OnUnauthUtilCtl
{

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
  trait TreeUserCatAdmBase
    extends ActionBuilder[RequestUserCatAdm]
    with OnUnauthUtil
  {
    def ownerId: String
    override def invokeBlock[A](request: Request[A], block: (RequestUserCatAdm[A]) => Future[Result]): Future[Result] = {
      // TODO нужно проверить права над указанным субъектов ownerId.
      val pwOpt = PersonWrapper.getFromRequest(request)
      val srmFut = SioReqMd.fromPwOpt(pwOpt)
      if (PersonWrapper isSuperuser pwOpt) {
        srmFut flatMap { srm =>
          val req1 = RequestUserCatAdm(None, ownerId, pwOpt, request, srm)
          block(req1)
        }
      } else {
        onUnauth(request)
      }
    }
  }
  case class TreeUserCatAdm(ownerId: String)
    extends TreeUserCatAdmBase
    with ExpireSession[RequestUserCatAdm]



  /** Есть конкретная категория, с которой работаем. */
  trait UserCatAdmBase
    extends ActionBuilder[RequestUserCatAdm]
    with OnUnauthUtil
  {
    def catId: String
    override def invokeBlock[A](request: Request[A], block: (RequestUserCatAdm[A]) => Future[Result]): Future[Result] = {
      val pwOpt = PersonWrapper.getFromRequest(request)
      val srmFut = SioReqMd.fromPwOpt(pwOpt)
      if (PersonWrapper isSuperuser pwOpt) {
        MMartCategory.getById(catId) flatMap {
          case Some(cat) =>
            srmFut flatMap { srm =>
              val req1 = RequestUserCatAdm(Some(cat), cat.ownerId, pwOpt, request, srm)
              block(req1)
            }

          case _ =>
            onUnauth(request)
        }
      } else {
        onUnauth(request)
      }
    }
  }
  case class UserCatAdm(catId: String)
    extends UserCatAdmBase
    with ExpireSession[RequestUserCatAdm]


  /** Статический ActionBuilder на случай, если заведомо у юзера нет прав доступа. Такое бывает при неправильном вызове.
    * Реализован в виде класса из-за музейной редкости подобных запросов. */
  class UnauthCatAdm
    extends ActionBuilder[RequestUserCatAdm]
    with OnUnauthUtil
  {
    override def invokeBlock[A](request: Request[A], block: (RequestUserCatAdm[A]) => Future[Result]): Future[Result] = {
      onUnauth(request)
    }
  }

}


/** Реквест, в тело которого забита категория, к которой адресован реквест. И корректный ownerId тоже. */
case class RequestUserCatAdm[A](
  cat       : Option[MMartCategory],
  ownerId   : String,
  pwOpt     : PwOpt_t,
  request   : Request[A],
  sioReqMd  : SioReqMd
)
  extends AbstractRequestWithPwOpt(request)

