package util.acl

import com.google.inject.Inject
import io.suggest.es.model.MEsUuId
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.order.MOrders
import io.suggest.sec.util.Csrf
import io.suggest.util.logs.MacroLogsImpl
import models.mproj.ICommonDi
import models.req.{MNodeOrderReq, MUserInit}
import play.api.mvc.{ActionBuilder, Request, Result}

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.02.17 15:06
  * Description: Можно ли посмотреть ордер на указанном узле?
  * Да, если относится к текущему юзеру и есть права на узел.
  */
class CanViewOrder @Inject() (
                               mOrders         : MOrders,
                               isAuth          : IsAuth,
                               isAdnNodeAdmin  : IsAdnNodeAdmin,
                               val csrf        : Csrf,
                               mCommonDi       : ICommonDi
                             )
  extends MacroLogsImpl
{

  import mCommonDi._


  /** Базовая реализация проверки. */
  sealed trait Base
    extends ActionBuilder[MNodeOrderReq]
    with InitUserCmds
  {

    /** id запрошенного ордера. */
    val orderId: Gid_t

    /** На каком узле сейчас находимся? */
    val onNodeId: MEsUuId


    override def invokeBlock[A](request: Request[A], block: (MNodeOrderReq[A]) => Future[Result]): Future[Result] = {
      val personIdOpt = sessionUtil.getPersonId(request)

      // Отказ юзеру в обслуживании: для защиты от сканирования id и прочего, ответ непонятным юзерам всегда один.
      def forbid = isAuth.onUnauth(request)

      // Незалогиненных юзеров можно сразу посылать.
      personIdOpt.fold( forbid ) { personId =>
        val user = mSioUsers(personIdOpt)

        // Получить id контракта юзера.
        val usrContractIdOptFut = user.contractIdOptFut

        // Прочитать запрошенный ордер из базы.
        val morderOptFut = slick.db.run {
          mOrders.getById(orderId)
        }

        // Прочитать узел, заявленный в URL, и проверить права юзера для доступа на него.
        val nodeAdmOptFut = isAdnNodeAdmin.isAdnNodeAdmin(onNodeId, user)

        // Запускаем инициализацию полей модели user, т.к. маловероятно, что этот реквест пойдёт мимо кассы.
        maybeInitUser(user)

        lazy val logPrefix = s"invokeBlock($orderId)[${request.remoteAddress}]:"

        // Узел скорее всего в кеше, поэтому проверяем по узлу в первую очередь.
        nodeAdmOptFut.flatMap {

          // Есть запрошенный узел, и текущий юзер является админом этого узла.
          case Some(mnode) =>
            usrContractIdOptFut.flatMap {

              // У юзера есть контракт.
              case Some(userContractId) =>
                morderOptFut.flatMap {

                  // Есть запрошенный ордер. Надо проверить права доступа на ордер.
                  case Some(morder) =>
                    // Сверить номера контрактов.
                    if (morder.contractId == userContractId || user.isSuper) {
                      val req1 = MNodeOrderReq(
                        morder  = morder,
                        mnode   = mnode,
                        user    = user,
                        request = request
                      )
                      // Этот контракт принадлежит текущему юзеру.
                      block(req1)

                    } else {
                      // Контракт существует, но принадлежит какому-то другому пользователю.
                      LOGGER.warn(s"$logPrefix User $personId have contract[$userContractId], but order[$orderId] has contractId=${morder.contractId}. So user tried to access to foreign order.")
                      forbid
                    }

                  // Нет запрошенного ордера.
                  case None =>
                    LOGGER.warn(s"$logPrefix User $personId tried to access order $orderId, that does not exists.")
                    // Мимикрируем под 403, чтобы никто не мог сканировать номера ордеров по URL.
                    forbid
                }


              // Нет контракта у текущего юзера. Он не может иметь никакого доступа ни к какому ордеру.
              case None =>
                LOGGER.warn(s"$logPrefix User $personId tried to access to order $orderId, but user contract is NOT created yet.")
                forbid
            }

          case None =>
            LOGGER.warn(s"$logPrefix User $personId has is NOT admin of node $onNodeId.")
            forbid
        }
      }
    }

  }


  sealed abstract class Abstract
    extends Base


  case class Simple(
                    override val orderId    : Gid_t,
                    override val onNodeId   : MEsUuId,
                    override val userInits  : MUserInit*
                   )
    extends Abstract

  @inline
  def apply(orderId: Gid_t,  onNodeId: MEsUuId,  userInits: MUserInit*) = Simple(orderId, onNodeId, userInits: _*)


  case class Get(
                  override val orderId      : Gid_t,
                  override val onNodeId     : MEsUuId,
                  override val userInits    : MUserInit*
                )
    extends Abstract
    with csrf.Get[MNodeOrderReq]

  case class Post(
                   override val orderId     : Gid_t,
                   override val onNodeId    : MEsUuId,
                   override val userInits   : MUserInit*
                 )
    extends Abstract
    with csrf.Post[MNodeOrderReq]

}

/** Интерфейс для DI-поля с инстансом [[CanViewOrder]]. */
trait ICanViewOrder {
  val canViewOrder: CanViewOrder
}
