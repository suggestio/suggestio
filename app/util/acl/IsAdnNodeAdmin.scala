package util.acl

import models._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.PlayLazyMacroLogsImpl
import scala.concurrent.Future
import util.acl.PersonWrapper.PwOpt_t
import play.api.mvc._
import util.SiowebEsUtil.client
import controllers.routes
import play.api.mvc.Result

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.04.14 18:35
 * Description: Проверка прав на управление абстрактным узлом рекламной сети.
 */
object IsAdnNodeAdmin {

  /** Что делать, когда юзер не авторизован, но долбится в ЛК? */
  def onUnauth(req: RequestHeader): Future[Result] = {
    Future.successful(
      Results.Redirect(routes.Market.index())
    )
  }


  /** Проверка прав на управления узлом с учётом того, что юзер может быть суперюзером s.io. */
  def isAdnNodeAdminCheck(adnNode: MAdnNode, pwOpt: PwOpt_t): Boolean = {
    PersonWrapper.isSuperuser(pwOpt) || isAdnNodeAdminCheckStrict(adnNode, pwOpt)
  }

  /** Проверка прав на домен без учёта суперюзеров. */
  def isAdnNodeAdminCheckStrict(adnNode: MAdnNode, pwOpt: PwOpt_t): Boolean = {
    pwOpt.exists { pw =>
      adnNode.personIds contains pw.personId
    }
  }


  def checkAdnNodeCredsFut(adnNodeOptFut: Future[Option[MAdnNode]], pwOpt: PwOpt_t): Future[Either[Option[MAdnNode], MAdnNode]] = {
    adnNodeOptFut map {
      checkAdnNodeCreds(_, pwOpt)
    }
  }

  def checkAdnNodeCreds(adnNodeOpt: Option[MAdnNode], pwOpt: PwOpt_t): Either[Option[MAdnNode], MAdnNode] = {
    adnNodeOpt.fold [Either[Option[MAdnNode], MAdnNode]] (Left(None)) { adnNode =>
      val isAllowed = isAdnNodeAdminCheck(adnNode, pwOpt)
      if (isAllowed) {
        Right(adnNode)
      } else {
        Left(adnNodeOpt)
      }
    }
  }

  def checkAdnNodeCredsOpt(adnNodeOptFut: Future[Option[MAdnNode]], pwOpt: PwOpt_t): Future[Option[MAdnNode]] = {
    checkAdnNodeCredsFut(adnNodeOptFut, pwOpt) map {
      case Right(adnNode) => Some(adnNode)
      case _ => None
    }
  }


  def isAdnNodeAdmin(adnId: String, pwOpt: PwOpt_t): Future[Option[MAdnNode]] = {
    val fut = MAdnNodeCache.getById(adnId)
    checkAdnNodeCredsOpt(fut, pwOpt)
  }

  def nodeNotFound(adnId: String)(implicit request: RequestHeader): Future[Result] = {
    controllers.Application.http404Fut
  }
}


import IsAdnNodeAdmin.onUnauth

/** В реквесте содержится администрируемый узел, если всё ок. */
sealed trait IsAdnNodeAdminBase extends ActionBuilder[AbstractRequestForAdnNode] {
  def adnId: String
  override def invokeBlock[A](request: Request[A], block: (AbstractRequestForAdnNode[A]) => Future[Result]): Future[Result] = {
    val pwOpt = PersonWrapper.getFromRequest(request)
    val srmFut = SioReqMd.fromPwOptAdn(pwOpt, adnId)
    IsAdnNodeAdmin.isAdnNodeAdmin(adnId, pwOpt) flatMap {
      case Some(adnNode) =>
        srmFut flatMap { srm =>
          val req1 = RequestForAdnNodeAdm(adnNode, isMyNode = true, request, pwOpt, srm)
          block(req1)
        }

      case _ => onUnauth(request)
    }
  }
}
case class IsAdnNodeAdmin(adnId: String)
  extends IsAdnNodeAdminBase
  with ExpireSession[AbstractRequestForAdnNode]



abstract class AbstractRequestForAdnNode[A](request: Request[A])
  extends AbstractRequestWithPwOpt(request) {
  def adnNode: MAdnNode
  def isMyNode: Boolean
}

case class RequestForAdnNodeAdm[A](adnNode: MAdnNode, isMyNode: Boolean, request: Request[A], pwOpt: PwOpt_t, sioReqMd: SioReqMd)
  extends AbstractRequestForAdnNode(request)


/** Доступ к узлу, к которому НЕ обязательно есть права на админство. */
sealed trait AdnNodeAccessBase extends ActionBuilder[RequestForAdnNode] {
  def adnId: String
  def povAdnIdOpt: Option[String]
  override def invokeBlock[A](request: Request[A], block: (RequestForAdnNode[A]) => Future[Result]): Future[Result] = {
    PersonWrapper.getFromRequest(request) match {
      case pwOpt @ Some(pw) =>
        val povAdnNodeOptFut = povAdnIdOpt.fold
          { Future successful Option.empty[MAdnNode] }
          { povAdnId => IsAdnNodeAdmin.isAdnNodeAdmin(povAdnId, pwOpt) }
        val adnNodeOptFut = MAdnNodeCache.getById(adnId)
        IsAdnNodeAdmin.checkAdnNodeCredsFut(adnNodeOptFut, pwOpt) flatMap {
          // Это админ текущего узла
          case Right(adnNode) =>
            SioReqMd.fromPwOptAdn(pwOpt, adnId) flatMap { srm =>
              povAdnNodeOptFut flatMap { povAdnNodeOpt =>
                val req1 = RequestForAdnNode(adnNode, povAdnNodeOpt, isMyNode = true, request, pwOpt, srm)
                block(req1)
              }
            }

          // Узел существует, но он не относится к текущему залогиненному юзеру. Это узел третьего лица, рекламодателя в частности.
          case Left(Some(adnNode)) =>
            povAdnNodeOptFut flatMap { povAdnNodeOpt =>
              val srmFut = povAdnNodeOpt match {
                // Это админ pov-узла подглядывает к рекламодателю или какому-то иному узлу.
                case Some(povAdnNode) =>
                  SioReqMd.fromPwOptAdn(pwOpt, povAdnNode.id.get)
                // Это кто-то иной косит под админа внешнего узела, скорее всего получил ссылку на ноду от другого человека.
                case None =>
                  SioReqMd.fromPwOpt(pwOpt)
              }
              srmFut flatMap { srm =>
                val req1 = RequestForAdnNode(adnNode, povAdnNodeOpt, isMyNode = false, request, pwOpt, srm)
                block(req1)
              }
            }

          // Узел не существует.
          case Left(None) =>
            Future successful Results.NotFound
        }

      // Отправить анонима на страницу логина.
      case None => IsAuth.onUnauth(request)
    }
  }
}
/**
 * Доступ к узлу, к которому НЕ обязательно есть права на админство.
 * @param adnId узел.
 */
case class AdnNodeAccess(adnId: String, povAdnIdOpt: Option[String])
  extends AdnNodeAccessBase
  with ExpireSession[RequestForAdnNode]


case class RequestForAdnNode[A](adnNode: MAdnNode, povAdnNodeOpt: Option[MAdnNode], isMyNode: Boolean,
                                request: Request[A], pwOpt: PwOpt_t, sioReqMd: SioReqMd)
  extends AbstractRequestForAdnNode(request) {

  def myNode: Option[MAdnNode] = if (isMyNode) Some(adnNode) else povAdnNodeOpt
  def myNodeId: Option[String] = myNode.flatMap(_.id)
}



case class SimpleRequestForAdnNode[A](adnNode: MAdnNode, request: Request[A], pwOpt: PwOpt_t, sioReqMd: SioReqMd)
  extends AbstractRequestForAdnNode(request) {
  override lazy val isMyNode = IsAdnNodeAdmin.isAdnNodeAdminCheck(adnNode, pwOpt)
}


/** Общий код проверок типа AdnNodeMaybeAuth. */
sealed trait AdnNodeMaybeAuthAbstract
  extends ActionBuilder[SimpleRequestForAdnNode]
  with PlayLazyMacroLogsImpl
{
  import LOGGER._

  def adnId: String
  override def invokeBlock[A](request: Request[A], block: (SimpleRequestForAdnNode[A]) => Future[Result]): Future[Result] = {
    val pwOpt = PersonWrapper.getFromRequest(request)
    val srmFut = SioReqMd.fromPwOpt(pwOpt)
    MAdnNodeCache.getById(adnId) flatMap {
      case Some(adnNode) =>
        if (isNodeValid(adnNode)) {
          srmFut flatMap { srm =>
            val req1 = SimpleRequestForAdnNode(adnNode, request, pwOpt, srm)
            block(req1)
          }
        } else {
          accessProhibited(adnNode, request)
        }

      case None =>
        nodeNotFound(request)
    }
  }

  def isNodeValid(adnNode: MAdnNode): Boolean

  def accessProhibited[A](adnNode: MAdnNode, request: Request[A]): Future[Result] = {
    warn(s"Failed access to acl-prohibited node: ${adnNode.id.get} (${adnNode.meta.name}) :: Returning 404 to ${request.remoteAddress}")
    IsAdnNodeAdmin.nodeNotFound(adnId)(request)
  }

  def nodeNotFound[A](request: Request[A]): Future[Result] = {
    warn(s"Node $adnId not found, requested by ${request.remoteAddress}")
    IsAdnNodeAdmin.nodeNotFound(adnId)(request)
  }
}

/** Промежуточный трейт из-за использования в ExpireSession модификатора abstract override. */
sealed trait AdnNodeMaybeAuthAbstractEs
  extends AdnNodeMaybeAuthAbstract
  with ExpireSession[SimpleRequestForAdnNode]


/**
 * Реализация [[AdnNodeMaybeAuthAbstract]] с поддержкой таймаута сессии.
 * @param adnId id узла.
 */
case class AdnNodeMaybeAuth(adnId: String) extends AdnNodeMaybeAuthAbstractEs {
  override def isNodeValid(adnNode: MAdnNode): Boolean = true
}


/**
 * Является ли этот узел публичным, т.е. отображаемым для анонимных юзеров?
 * Да, если не тестовый и если ресивер.
 * @param adnId id узла.
 */
case class AdnNodePubMaybeAuth(adnId: String) extends AdnNodeMaybeAuthAbstractEs {
  override def isNodeValid(adnNode: MAdnNode): Boolean = {
    adnNode.adn.isReceiver && !adnNode.adn.testNode
  }
}

