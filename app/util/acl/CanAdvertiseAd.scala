package util.acl

import com.google.inject.Inject
import io.suggest.di.{IEsClient, IExecutionContext}
import models.jsm.init.MTarget
import models.req.SioReqMd
import org.elasticsearch.client.Client
import play.api.mvc._
import models._
import util.di.ICanAdvAdUtil
import util.n2u.N2NodesUtil
import util.{PlayMacroLogsDyn, PlayMacroLogsI, PlayMacroLogsImpl}
import util.acl.PersonWrapper.PwOpt_t
import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.05.14 18:24
 * Description: Проверка прав на размещение рекламной карточки.
 */

class CanAdvertiseAdUtil @Inject() (
  mNodeCache                      : MAdnNodeCache,
  n2NodeUtil                      : N2NodesUtil,
  implicit private val ec         : ExecutionContext,
  implicit private val esClient   : Client
)
  extends PlayMacroLogsImpl
{

  import LOGGER._

  /** Является ли указанный узел рекламодателем? */
  def isAdvertiserNode(mnode: MNode): Boolean = {
    mnode.common.isEnabled  &&  mnode.extras.adn.exists(_.isProducer)
  }

  /**
   * Определить, можно ли пропускать реквест на исполнение экшена.
   * @param pwOpt Данные о текущем юзере.
   * @param mad Рекламная карточка.
   * @param request Реквест.
   * @tparam A Параметр типа реквеста.
   * @return None если нельзя. Some([[RequestWithAdAndProducer]]) если можно исполнять реквест.
   */
  def maybeAllowed[A](pwOpt: PwOpt_t, mad: MNode, request: Request[A], jsInitActions: Seq[MTarget] = Nil): Future[Option[RequestWithAdAndProducer[A]]] = {
    val prodIdOpt = n2NodeUtil.madProducerId(mad)
    def prodOptFut = mNodeCache.maybeGetByIdCached(prodIdOpt)
    if (PersonWrapper isSuperuser pwOpt) {
      prodOptFut flatMap {
        case Some(mnode) if isAdvertiserNode(mnode) =>
          SioReqMd.fromPwOptAdn(pwOpt, mnode.id.get) map { srm =>
            Some(RequestWithAdAndProducer(mad, request, pwOpt, srm, mnode))
          }
        case None =>
          debug(s"maybeAllowed($pwOpt, ${mad.id.get}): superuser, but ad producer node $prodIdOpt is not allowed to advertise.")
          Future successful None
      }

    } else {
      pwOpt match {
        case Some(pw) =>
          prodOptFut.flatMap { adnNodeOpt =>
            adnNodeOpt
              .filter { mnode =>
                val isOwnedByMe = mnode.edges
                  .withPredicateIterIds( MPredicates.OwnedBy )
                  .contains(pw.personId)
                isOwnedByMe  &&  isAdvertiserNode(mnode)
              }
              .fold {
                debug(s"maybeAllowed($pwOpt, ${mad.id.get}): User is not node $prodIdOpt admin or node is not a producer.")
                Future successful Option.empty[RequestWithAdAndProducer[A]]
              } {mnode =>
                SioReqMd.fromPwOptAdn(pwOpt, mnode.id.get, jsInitActions) map { srm =>
                  Some(RequestWithAdAndProducer(mad, request, pwOpt, srm, mnode))
                }
              }
          }

        case None =>
          trace(s"maybeAllowed(${mad.id.get}): anonymous access prohibited")
          Future successful None
      }
    }
  }

}


/** Аддон для контроллеров для  */
trait CanAdvertiseAd
  extends ICanAdvAdUtil
  with IExecutionContext
  with IEsClient
  with OnUnauthNodeCtl
{

  /** Редактировать карточку может только владелец магазина. */
  trait CanAdvertiseAdBase
    extends ActionBuilder[RequestWithAdAndProducer]
    with PlayMacroLogsI
    with OnUnauthNode
  {

    /** id запрошенной рекламной карточки. */
    def adId: String

    def invokeBlock[A](request: Request[A], block: (RequestWithAdAndProducer[A]) => Future[Result]): Future[Result] = {
      val pwOpt = PersonWrapper.getFromRequest(request)
      val madFut = MNode.getByIdType(adId, MNodeTypes.Ad)
      madFut.flatMap {
        case Some(mad) =>
          canAdvAdUtil.maybeAllowed(pwOpt, mad, request) flatMap {
            case Some(req1) =>
              block(req1)
            case None =>
              LOGGER.debug(s"invokeBlock(): maybeAllowed($pwOpt, mad=${mad.id.get}) -> false.")
              onUnauthNode(request, pwOpt)
          }

        case None =>
          LOGGER.debug("invokeBlock(): MAd not found: " + adId)
          onUnauthNode(request, pwOpt)
      }
    }
  }

  sealed abstract class CanAdvertiseAdBase2
    extends CanAdvertiseAdBase
    with ExpireSession[RequestWithAdAndProducer]
    with PlayMacroLogsDyn

  /** Запрос какой-то формы размещения рекламной карточки. */
  case class CanAdvertiseAdGet(override val adId: String)
    extends CanAdvertiseAdBase2
    with CsrfGet[RequestWithAdAndProducer]

  /** Сабмит какой-то формы размещения рекламной карточки. */
  case class CanAdvertiseAdPost(override val adId: String)
    extends CanAdvertiseAdBase2
    with CsrfPost[RequestWithAdAndProducer]

}
