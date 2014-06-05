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
  override protected def invokeBlock[A](request: Request[A], block: (AdAnyRcvrRequest[A]) => Future[Result]): Future[Result] = {
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
  override protected def invokeBlock[A](request: Request[A], block: (AdRcvrRequest[A]) => Future[Result]): Future[Result] = {
    PersonWrapper.getFromRequest(request) match {
      case pwOpt @ Some(pw) =>
        val madOptFut = MAd.getById(adId)
        MAdnNodeCache.getByIdCached(fromAdnId).flatMap {
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


/**
 * Запрос окна с информацией о размещении карточки. Такое окно может запрашивать как создатель карточки,
 * так и узел-ресивер, который модерирует или уже отмодерировал карточку.
 * @param adId id рекламной карточки.
 * @param fromAdnId Опциональная точка зрения на карточку.
 */
case class AdvWndAccess(adId: String, fromAdnId: Option[String], needMBC: Boolean) extends ActionBuilder[AdvWndRequest] {
  override def invokeBlock[A](request: Request[A], block: (AdvWndRequest[A]) => Future[Result]): Future[Result] = {
    PersonWrapper.getFromRequest(request) match {
      case pwOpt @ Some(pw) =>
        import pw.personId
        MAd.getById(adId).flatMap {
          case Some(mad) =>
            val producerOptFut = MAdnNodeCache.getByIdCached(mad.producerId)
            val rcvrIdOpt0 = fromAdnId.filter(_ != mad.producerId)
            val isRcvrRelated = rcvrIdOpt0 exists { rcvrId =>
              DB.withConnection { implicit c =>
                MAdvOk.hasNotExpiredByAdIdAndRcvr(adId, rcvrId)  ||  MAdvReq.hasNotExpiredByAdIdAndRcvr(adId, rcvrId)
              }
            }
            val rcvrIdOpt = rcvrIdOpt0 filter { _ => isRcvrRelated }
            val rcvrOptFut = rcvrIdOpt
              .fold [Future[Option[MAdnNode]]] { Future successful None } { MAdnNodeCache.getByIdCached }
            producerOptFut flatMap {
              case Some(producer) =>
                val isProducerAdmin = IsAdnNodeAdmin.isAdnNodeAdminCheck(producer, personId)
                // Пока ресивер ещё не готов, проверяем, относится ли текущая рекламная карточка к указанному ресиверу или продьюсеру.
                if (isProducerAdmin || isRcvrRelated) {
                  // Юзер может смотреть рекламную карточку.
                  rcvrOptFut flatMap { rcvrOpt =>
                    val isRcvrAdmin = rcvrOpt
                      .exists { rcvr => IsAdnNodeAdmin.isAdnNodeAdminCheck(rcvr, personId) }
                    // Чтобы получить какой-либо доступ к окошку карточки, нужно быть или админом узла-продьюсера карточки, или же админом ресивера, переданного через fromAdnId.
                    if (isProducerAdmin || isRcvrAdmin) {
                      val srmFut = if (needMBC) {
                        // Узнаём для какого узла отображать кошелёк.
                        val myAdnId: String = if (isProducerAdmin) mad.producerId else if (isRcvrAdmin) rcvrIdOpt.get else ???
                        SioReqMd.fromPwOptAdn(pwOpt, myAdnId)
                      } else {
                        // Контроллер считает, что кошелёк не нужен.
                        SioReqMd.fromPwOpt(pwOpt)
                      }
                      srmFut flatMap { srm =>
                        val req1 = AdvWndRequest(mad, producer, rcvrOpt = rcvrOpt, isProducerAdmin, isRcvrAdmin = isRcvrAdmin, pwOpt, request, srm)
                        block(req1)
                      }
                    } else {
                      IsAdnNodeAdmin.onUnauth(request)
                    }
                  }
                } else {
                  IsAdnNodeAdmin.onUnauth(request)
                }

              case None =>
                Future successful Results.InternalServerError("Ad producer not found, but it should!")
            }

          case None => notFoundFut
        }

      case None => IsAuth.onUnauth(request)
    }
  }
}


case class AdvWndRequest[A](
  mad: MAd,
  producer: MAdnNode,
  rcvrOpt: Option[MAdnNode],
  isProducerAdmin: Boolean,
  isRcvrAdmin: Boolean,
  pwOpt: PwOpt_t,
  request: Request[A],
  sioReqMd: SioReqMd
) extends AbstractRequestWithPwOpt(request) {
  def isRcvrAccess = rcvrOpt.isDefined
}
