package util.acl

import com.google.inject.Inject
import io.suggest.util.logs.MacroLogsImpl
import models._
import models.mproj.ICommonDi
import models.req.{MAdProdReq, MReq}
import play.api.mvc._
import util.n2u.N2NodesUtil

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.10.15 19:39
 * Description: Аддон для контроллера для поддержки проверки прав
 * на редактирование в рамках узла ad show levels.
 */

class CanUpdateSls @Inject() (
                               isAdnNodeAdmin         : IsAdnNodeAdmin,
                               val canEditAd          : CanEditAd,
                               n2NodesUtil            : N2NodesUtil,
                               override val mCommonDi : ICommonDi
                             )
  extends MacroLogsImpl
  with Csrf
{

  import mCommonDi._

  /** Проверка прав на возможность обновления уровней отображения рекламной карточки. */
  sealed trait CanUpdateSlsBase
    extends ActionBuilder[MAdProdReq]
    with canEditAd.AdEditBase
    with OnUnauthNode
  {

    override def invokeBlock[A](request: Request[A], block: (MAdProdReq[A]) => Future[Result]): Future[Result] = {
      val madOptFut = mNodesCache.getByIdType(adId, MNodeTypes.Ad)

      val personIdOpt = sessionUtil.getPersonId(request)
      val user = mSioUsers(personIdOpt)

      def reqErr = MReq(request, user)

      if (user.isAnon) {
        LOGGER.trace("invokeBlock(): Anonymous access prohibited to " + adId)
        onUnauthNode(reqErr)

      } else {
        madOptFut.flatMap {
          // Найдена запрошенная рекламная карточка
          case Some(mad) =>
            // Модер может запретить бесплатное размещение карточки. Если стоит черная метка, то на этом можно закончить.
            val isMdrProhibited = mad.edges
              .withPredicateIter( MPredicates.ModeratedBy )
              .flatMap(_.info.flag)
              .contains(false)

            if (isMdrProhibited) {
              // Админы s.io когда-то запретили бесплатно размещать эту карточку. Пока бан не снять, карточку публиковать бесплатно нельзя.
              LOGGER.debug("invokeBlock(): cannot update sls for false-moderated ad " + adId)
              forbiddenFut("false-moderated ad", request)

            } else {

              val producerIdOpt = n2NodesUtil.madProducerId(mad)
              mNodesCache.maybeGetByIdCached(producerIdOpt).flatMap { producerOpt =>
                val isNodeAdmin = producerOpt.exists { producer =>
                  isAdnNodeAdmin.isAdnNodeAdminCheck(producer, user)
                }
                if (isNodeAdmin) {
                  // Юзер является админом. Всё ок.
                  val req1 = MAdProdReq(mad, producerOpt.get, request, user)
                  block(req1)
                } else {
                  // Юзер не является админом, либо (маловероятно) producer-узел был удалён (и нельзя проверить права).
                  LOGGER.debug(s"invokeBlock(): No node-admin rights for update sls for ad=$adId producer=${producerOpt.flatMap(_.id)}")
                  forbiddenFut("No edit rights", request)
                }
              }
            }

          // Рекламная карточка не найдена.
          case None =>
            adNotFound(reqErr)
        }
      }
    }
  }

  sealed abstract class CanUpdateSlsAbstract
    extends CanUpdateSlsBase
    with ExpireSession[MAdProdReq]

  case class Post(adId: String)
    extends CanUpdateSlsAbstract
    with CsrfPost[MAdProdReq]

}
