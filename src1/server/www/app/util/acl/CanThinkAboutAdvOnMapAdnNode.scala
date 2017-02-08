package util.acl

import com.google.inject.Inject
import io.suggest.model.n2.node.{MNodeTypes, MNodes}
import io.suggest.util.logs.MacroLogsImpl
import models.mproj.ICommonDi
import models.req.{MAdProdRcvrReq, MReq}
import play.api.mvc.{ActionBuilder, Request, Result}
import util.adv.geo.AdvGeoMapUtil

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.11.16 15:31
  * Description: ACL для проверок возможности размещения
  */
class CanThinkAboutAdvOnMapAdnNode @Inject() (
                                               mNodes                 : MNodes,
                                               canAdvAd               : CanAdvAd,
                                               advGeoMapUtil          : AdvGeoMapUtil,
                                               isAdnNodeAdmin         : IsAdnNodeAdmin,
                                               isAuth                 : IsAuth,
                                               val csrf               : Csrf,
                                               mCommonDi              : ICommonDi
                                             )
  extends MacroLogsImpl
{

  import mCommonDi._

  sealed trait CanThinkAboutAdvOnMapAdnNodeBase
    extends ActionBuilder[MAdProdRcvrReq]
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
        isAuth.onUnauth(request)

      } { personId =>
        // Юзер залогинен. Сразу же собираем все параллельные задачи...
        val madOptFut   = mNodesCache.getByIdType(adId,   MNodeTypes.Ad)

        // Ищем целевой узел, проверяя права размещения на узле прямо в рамках ES-запроса:
        val nodeOptFut = mNodes.dynSearchOne(
          advGeoMapUtil.onMapRcvrsSearch(
            limit1      = 1,
            onlyWithIds = nodeId :: Nil
          )
        )

        // Подготовить синхронные данные:
        val user = mSioUsers(personIdOpt)
        val reqBlank = MReq(request, user)
        lazy val logPrefix = s"${getClass.getSimpleName}[${reqBlank.remoteAddress}]:"

        // Когда придёт ответ от БД по запрошенной карточке...
        madOptFut.flatMap {
          case Some(mad) =>
            canAdvAd.maybeAllowed(mad, reqBlank).flatMap {
              // Есть карточка, проверка прав на неё ок.
              case Some(req1) =>
                nodeOptFut.flatMap {
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
                    isAdnNodeAdmin.onUnauthNode(reqBlank)
                }

              // Нет доступа к карточке. Обычно, когда сессия истекла.
              case None =>
                LOGGER.debug(s"$logPrefix: maybeAllowed($personIdOpt, mad=${mad.id.get}) -> false.")
                isAdnNodeAdmin.onUnauthNode(reqBlank)
            }

          // Нет карточки такой вообще.
          case None =>
            LOGGER.debug(s"$logPrefix: MAd not found: " + adId)
            isAdnNodeAdmin.onUnauthNode(reqBlank)
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
  def apply(adId: String, nodeId: String) = CanThinkAboutAdvOnMapAdnNode(adId = adId, nodeId = nodeId)


  /** Реализация CanThinkAboutAdvOnMapAdnNodeAbstract с выставлениме CSRF-токена. */
  case class Get(
    override val adId   : String,
    override val nodeId : String
  )
    extends CanThinkAboutAdvOnMapAdnNodeAbstract
    with csrf.Get[MAdProdRcvrReq]


  /** Реализация CanThinkAboutAdvOnMapAdnNodeAbstract с проверкой CSRF-токена. */
  case class Post(
    override val adId   : String,
    override val nodeId : String
  )
    extends CanThinkAboutAdvOnMapAdnNodeAbstract
    with csrf.Post[MAdProdRcvrReq]

}
