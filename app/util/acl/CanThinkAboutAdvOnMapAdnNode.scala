package util.acl

import controllers.SioController
import io.suggest.model.n2.edge.MPredicates
import io.suggest.model.n2.node.MNodeTypes
import models.req.{MAdProdRcvrReq, MReq}
import play.api.mvc.{ActionBuilder, Request, Result}
import util.PlayMacroLogsI
import util.di.ICanAdvAdUtil

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.11.16 15:31
  * Description: ACL для проверок возможности размещения
  */
trait CanThinkAboutAdvOnMapAdnNode
  extends SioController
  with Csrf
  with OnUnauthUtilCtl
  with OnUnauthNodeCtl
  with PlayMacroLogsI
  with ICanAdvAdUtil
{

  import mCommonDi._

  sealed trait CanThinkAboutAdvOnMapAdnNodeBase
    extends ActionBuilder[MAdProdRcvrReq]
    with OnUnauthUtil
    with OnUnauthNode
  {

    /** id рекламной карточки, которую юзер хочет разместить на узле. */
    def adId: String

    /** id узла, на который юзер хочет что-то размещать. */
    def nodeId: String

    /**
      * Нужно проверить права доступа к рекламной карточке (CanAdvertiseAd).
      * А также проверить, можно ли размещаться на указанный узел через карту.
      */
    override def invokeBlock[A](request: Request[A], block: (MAdProdRcvrReq[A]) => Future[Result]): Future[Result] = {
      val personIdOpt = sessionUtil.getPersonId(request)
      personIdOpt.fold {
        onUnauth(request)

      } { personId =>
        // Юзер залогинен. Сразу же собираем все параллельные задачи...
        // НЕ используем multiget, т.к. очень вероятна ситуация, что хотя бы один узел уже в кэше.
        val madOptFut   = mNodeCache.getByIdType(adId,   MNodeTypes.Ad)
        val nodeOptFut  = mNodeCache.getByIdType(nodeId, MNodeTypes.AdnNode)

        // Подготовить синхронные данные:
        val user = mSioUsers(personIdOpt)
        val reqBlank = MReq(request, user)
        lazy val logPrefix = s"${getClass.getSimpleName}[${reqBlank.remoteAddress}]:"

        // Для снижения уровней вложенности, узел проверяем отдельно от карточки.
        val nodeCheckedOptFut = for (nodeOpt <- nodeOptFut) yield {
          val nodeOpt2 = nodeOpt.filter { mnode =>
            // Запрошенный узел существует. Проверить, размещён ли узел на карте adn-map
            mnode.edges
              .withPredicateIter( MPredicates.AdnMap )
              .nonEmpty
          }
          // Логгировать возможный результат.
          if (nodeOpt.isEmpty) {
            LOGGER.warn(s"$logPrefix Node $nodeId missing.")
          } else if (nodeOpt2.isEmpty) {
            LOGGER.warn(s"$logPrefix Node $nodeId do not have adn-map edge.")
          }
          nodeOpt2
        }

        // Когда придёт ответ от БД по запрошенной карточке...
        madOptFut.flatMap {
          case Some(mad) =>
            canAdvAdUtil.maybeAllowed(mad, reqBlank).flatMap {
              // Есть карточка, проверка прав на неё ок.
              case Some(req1) =>
                nodeCheckedOptFut.flatMap {
                  // Всё ок и с узлом. Запускаем экшен.
                  case Some(mnode) =>
                    val req2 = MAdProdRcvrReq(
                      mad       = mad,
                      producer  = req1.producer,
                      mnode     = mnode,
                      request   = request,
                      user      = user
                    )
                    block(req2)

                  // Нет узла или нельзя на него размещать. Логгирование уже выполнено внутри nodeCheckedOptFut.
                  case None =>
                    onUnauthNode(reqBlank)
                }

              // Нет доступа к карточке. Обычно, когда сессия истекла.
              case None =>
                LOGGER.debug(s"$logPrefix: maybeAllowed($personIdOpt, mad=${mad.id.get}) -> false.")
                onUnauthNode(reqBlank)
            }

          // Нет карточки такой вообще.
          case None =>
            LOGGER.debug(s"$logPrefix: MAd not found: " + adId)
            onUnauthNode(reqBlank)
        }
      }
    }

  }


  /** Абстрактная реализация трейта CanThinkAboutAdvOnMapAdnNodeBase. */
  abstract class CanThinkAboutAdvOnMapAdnNodeAbstract
    extends CanThinkAboutAdvOnMapAdnNodeBase
    with ExpireSession[MAdProdRcvrReq]


  /** Простейшая реализация CanThinkAboutAdvOnMapAdnNodeAbstract. */
  case class CanThinkAboutAdvOnMapAdnNode(
    override val adId   : String,
    override val nodeId : String
  )
    extends CanThinkAboutAdvOnMapAdnNodeAbstract


  /** Реализация CanThinkAboutAdvOnMapAdnNodeAbstract с выставлениме CSRF-токена. */
  case class CanThinkAboutAdvOnMapAdnNodeGet(
    override val adId   : String,
    override val nodeId : String
  )
    extends CanThinkAboutAdvOnMapAdnNodeAbstract
    with CsrfGet[MAdProdRcvrReq]


  /** Реализация CanThinkAboutAdvOnMapAdnNodeAbstract с проверкой CSRF-токена. */
  case class CanThinkAboutAdvOnMapAdnNodePost(
    override val adId   : String,
    override val nodeId : String
  )
    extends CanThinkAboutAdvOnMapAdnNodeAbstract
    with CsrfPost[MAdProdRcvrReq]

}
