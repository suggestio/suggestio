package util.acl

import com.google.inject.Inject
import models._
import models.mproj.ICommonDi
import models.req._
import play.api.mvc._
import util.di.ICanAdvAdUtil
import util.n2u.N2NodesUtil
import util.{PlayMacroLogsDyn, PlayMacroLogsI, PlayMacroLogsImpl}

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.05.14 18:24
 * Description: Проверка прав на размещение рекламной карточки.
 */

class CanAdvertiseAdUtil @Inject() (
  n2NodeUtil                      : N2NodesUtil,
  mCommonDi                       : ICommonDi
)
  extends PlayMacroLogsImpl
{

  import LOGGER._
  import mCommonDi._

  /** Является ли указанный узел рекламодателем? */
  def isAdvertiserNode(mnode: MNode): Boolean = {
    mnode.common.isEnabled  &&  mnode.extras.adn.exists(_.isProducer)
  }

  /**
   * Определить, можно ли пропускать реквест на исполнение экшена.
   * @param mad Рекламная карточка.
   * @param req Реквест sio.
   * @tparam A Параметр типа реквеста.
   * @return None если нельзя. Some([[models.req.MAdProdReq]]) если можно исполнять реквест.
   */
  def maybeAllowed[A](mad: MNode, req: IReq[A]): Future[Option[MAdProdReq[A]]] = {
    val prodIdOpt = n2NodeUtil.madProducerId(mad)
    // TODO Далее говнокод какой-то, переписать.
    def prodOptFut = mNodeCache.maybeGetByIdCached(prodIdOpt)
    def req2(mnode: MNode) = MAdProdReq(mad, mnode, req, req.user)
    if (req.user.isSuper) {
      prodOptFut.map { prodOpt =>
        val resOpt = prodOpt
          .filter { isAdvertiserNode }
          .map { req2 }
        if (resOpt.isEmpty)
          LOGGER.debug(s"maybeAllowed(${req.user.personIdOpt}, ${mad.id.get}): superuser, but ad producer node $prodIdOpt is not allowed to advertise.")
        resOpt
      }

    } else {
      prodIdOpt.fold {
        trace(s"maybeAllowed(${mad.id.get}): anonymous access prohibited")
        Future successful Option.empty[MAdProdReq[A]]
      } { personId =>
        prodOptFut.map { adnNodeOpt =>
          val resOpt = adnNodeOpt
            .filter { mnode =>
              val isOwnedByMe = mnode.edges
                .withPredicateIterIds( MPredicates.OwnedBy )
                .contains(personId)
              isOwnedByMe  &&  isAdvertiserNode(mnode)
            }
            .map { req2 }
          if (resOpt.isEmpty)
            debug(s"maybeAllowed($personId, ${mad.id.get}): User is not node $prodIdOpt admin or node is not a producer.")
          resOpt
        }
      }
    }
  }

}


/** Аддон для контроллеров для  */
trait CanAdvertiseAd
  extends OnUnauthNodeCtl
  with ICanAdvAdUtil
  with Csrf
{

  import mCommonDi._

  /** Редактировать карточку может только владелец магазина. */
  sealed trait CanAdvertiseAdBase
    extends ActionBuilder[MAdProdReq]
    with PlayMacroLogsI
    with OnUnauthNode
    with InitUserCmds
  {

    /** id запрошенной рекламной карточки. */
    def adId: String

    def invokeBlock[A](request: Request[A], block: (MAdProdReq[A]) => Future[Result]): Future[Result] = {
      val personIdOpt = sessionUtil.getPersonId(request)
      val madFut = mNodeCache.getByIdType(adId, MNodeTypes.Ad)
      val user = mSioUsers(personIdOpt)

      // Оптимистично запустить сбор запрошенных данных MSioUser.
      maybeInitUser(user)

      // Продолжить дальше работу асинхронно...
      val reqBlank = MReq(request, user)
      madFut.flatMap {
        // Карточка найден, проверить доступ...
        case Some(mad) =>
          canAdvAdUtil.maybeAllowed(mad, reqBlank).flatMap {
            case Some(req1) =>
              block(req1)
            case None =>
              LOGGER.debug(s"invokeBlock(): maybeAllowed($personIdOpt, mad=${mad.id.get}) -> false.")
              onUnauthNode(reqBlank)
          }

        // Нет запрашиваем карточки, отработать и этот вариант.
        case None =>
          LOGGER.debug("invokeBlock(): MAd not found: " + adId)
          onUnauthNode(reqBlank)
      }
    }
  }

  sealed abstract class CanAdvertiseAdBase2
    extends CanAdvertiseAdBase
    with ExpireSession[MAdProdReq]
    with PlayMacroLogsDyn

  /** Запрос какой-то формы размещения рекламной карточки. */
  case class CanAdvertiseAd(
    override val adId       : String,
    override val userInits  : MUserInit*
  )
    extends CanAdvertiseAdBase2

  /** Запрос какой-то формы размещения рекламной карточки с выставление CSRF в сессию. */
  case class CanAdvertiseAdGet(
    override val adId       : String,
    override val userInits  : MUserInit*
  )
    extends CanAdvertiseAdBase2
    with CsrfGet[MAdProdReq]

  /** Сабмит какой-то формы размещения рекламной карточки с проверкой CSRF в сессии. */
  case class CanAdvertiseAdPost(
    override val adId       : String,
    override val userInits  : MUserInit*
  )
    extends CanAdvertiseAdBase2
    with CsrfPost[MAdProdReq]

}
