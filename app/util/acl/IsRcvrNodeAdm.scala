package util.acl

import play.api.mvc.{Results, Result, ActionBuilder, Request}
import util.acl.PersonWrapper.PwOpt_t
import models._
import scala.concurrent.Future
import play.api.Play.current
import play.api.db.DB
import IsAuth.onUnauth
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.05.14 17:26
 * Description: Есть ли у юзера admin-доступ к узлам, на которые отправлена на публикацию указанная карточка?
 * На этом этапе неизвестно, с какой стороны на узел смотрит текущий юзер.
 */
object IsRcvrNodeAdm {
  
  def notFoundFut = Future successful Results.NotFound
  
  
}

import IsRcvrNodeAdm._

case class IsAnyRcvrNodeAdm(adId: String) extends ActionBuilder[AdAnyRcvrRequest] {
  override def invokeBlock[A](request: Request[A], block: (AdAnyRcvrRequest[A]) => Future[Result]): Future[Result] = {
    PersonWrapper.getFromRequest(request) match {
      case pwOpt @ Some(pw) =>
        val srmFut = SioReqMd.fromPwOpt(pwOpt)
        val madOptFut = MAd.getById(adId)
        MAdnNode.findByPersonId(pw.personId).flatMap { personNodes =>
          if (personNodes.isEmpty) {
            // Вероятно у этого юзера нет доступа в маркет. На этом и закончим.
            IsAdnNodeAdmin.onUnauth(request)
          } else {
            madOptFut flatMap {
              case None =>
                notFoundFut
              case Some(mad) =>
                val personAdnIds = personNodes.map(_.id.get).toSet
                val syncResult = DB.withConnection { implicit c =>
                  val advsOk = MAdvOk.findByAdIdAndRcvrs(adId, personAdnIds)
                  val advsReq = MAdvReq.findByAdIdAndRcvrs(adId, personAdnIds)
                  val advsRefused = MAdvRefuse.findByAdIdAndRcvrs(adId, personAdnIds)
                  (advsOk, advsReq, advsRefused)
                }
                srmFut flatMap { srm =>
                  val (advsOk, advsReq, advsRefused) = syncResult
                  val req1 = AdAnyRcvrRequest(mad, personNodes, advsOk, advsReq, advsRefused, pwOpt, request, srm)
                  block(req1)
                }
            }
          } // else if personNodes.isEmpty
        } // MAdnNode.findByPersonId()

      case None => onUnauth(request)
    }

  }
}

case class AdAnyRcvrRequest[A](
  mad: MAd,
  rcvrs: Seq[MAdnNode],
  advsOk: List[MAdvOk],
  advsReq: List[MAdvReq],
  advsRefused: List[MAdvRefuse],
  pwOpt: PwOpt_t,
  request: Request[A],
  sioReqMd: SioReqMd
)
  extends AbstractRequestWithPwOpt(request)



/**
 * Просмотр чьей-то карточки с точки зрения другого узла. Узел может иметь отношение к карточке или нет.
 * @param adId id рекламной карточки.
 * @param fromAdnId id узла, с точки зрения которого происходит просмотр. Возможно, это подставной узел.
 */
case class ThirdPartyAdAccess(adId: String, fromAdnId: String) extends ActionBuilder[AdRcvrRequest] {
  override def invokeBlock[A](request: Request[A], block: (AdRcvrRequest[A]) => Future[Result]): Future[Result] = {
    PersonWrapper.getFromRequest(request) match {
      case pwOpt @ Some(pw) =>
        val madOptFut = MAd.getById(adId)
        MAdnNode.getById(fromAdnId).flatMap {
          case Some(maybeRcvrNode) =>
            madOptFut flatMap {
              case Some(mad) =>
                // TODO Может стоит как-то запользовать IsAdnNodeAdmin.checkNodeCreds?
                val (srmFut, rcvrOpt) = if (maybeRcvrNode.personIds contains pw.personId) {
                  // Это узел, которым владеет юзер.
                  val _srmFut = SioReqMd.fromPwOptAdn(pwOpt, fromAdnId)
                  (_srmFut, Some(maybeRcvrNode))
                } else {
                  // Это левый узел, упрощаем всё до предела.
                  val _srmFut = SioReqMd.fromPwOpt(pwOpt)
                  (_srmFut, None)
                }
                srmFut flatMap { srm =>
                  val req1 = AdRcvrRequest(mad, maybeRcvrNode, rcvrOpt, pwOpt, request, srm)
                  block(req1)
                }

              // Эта рекламная карточка не найден.
              case None => notFoundFut
            }

          // from-узел не найден.
          case None => notFoundFut
        }
        
      // Юзер не залогинен вообще.
      case None => onUnauth(request)
    }
  }
}



case class AdRcvrRequest[A](
  mad: MAd,
  fromAdnNode: MAdnNode,
  rcvrOpt: Option[MAdnNode],
  pwOpt: PwOpt_t,
  request: Request[A],
  sioReqMd: SioReqMd
) extends AbstractRequestWithPwOpt(request) {
  def isRcvrAccess = rcvrOpt.isDefined
}

