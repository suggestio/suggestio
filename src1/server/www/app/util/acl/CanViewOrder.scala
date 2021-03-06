package util.acl

import io.suggest.common.fut.FutureUtil

import javax.inject.Inject
import io.suggest.es.model.MEsUuId
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.order.MOrders
import io.suggest.model.SlickHolder
import io.suggest.n2.node.MNode
import io.suggest.req.ReqUtil
import io.suggest.util.logs.MacroLogsImpl
import models.req.{MNodeOptOrderReq, MUserInit, MUserInits}
import play.api.mvc.{ActionBuilder, AnyContent, Request, Result}
import japgolly.univeq._
import play.api.inject.Injector

import scala.concurrent.{ExecutionContext, Future}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.02.17 15:06
  * Description: Можно ли посмотреть ордер на указанном узле?
  * Да, если относится к текущему юзеру и есть права на узел.
  */
final class CanViewOrder @Inject() (
                                     injector        : Injector,
                                   )
  extends MacroLogsImpl
{

  private lazy val aclUtil = injector.instanceOf[AclUtil]
  private lazy val mOrders = injector.instanceOf[MOrders]
  private lazy val isAuth = injector.instanceOf[IsAuth]
  private lazy val isNodeAdmin = injector.instanceOf[IsNodeAdmin]
  private lazy val reqUtil = injector.instanceOf[ReqUtil]
  private lazy val slickHolder = injector.instanceOf[SlickHolder]
  implicit private lazy val ec = injector.instanceOf[ExecutionContext]

  /** Собрать ACL ActionBuilder проверки прав.
    *
    * @param orderId id запрошенного ордера.
    * @param onNodeId На каком узле сейчас находимся? None - значит вне узла.
    */
  def apply(
             orderId    : Gid_t,
             onNodeId   : Option[MEsUuId],
             userInits1 : MUserInit*
           ): ActionBuilder[MNodeOptOrderReq, AnyContent] = {

    new reqUtil.SioActionBuilderImpl[MNodeOptOrderReq] {

      override def invokeBlock[A](request0: Request[A], block: (MNodeOptOrderReq[A]) => Future[Result]): Future[Result] = {
        val request = aclUtil.reqFromRequest( request0 )
        val user = aclUtil.userFromRequest(request)

        // Отказ юзеру в обслуживании: для защиты от сканирования id и прочего, ответ непонятным юзерам всегда один.
        def forbid = isAuth.onUnauth(request)

        // Незалогиненных юзеров можно сразу посылать.
        user.personIdOpt.fold( forbid ) { personId =>
          import slickHolder.slick

          // Получить id контракта юзера.
          val usrContractIdOptFut = user.contractIdOptFut

          // Прочитать запрошенный ордер из базы.
          val morderOptFut = slick.db.run {
            mOrders.getById(orderId)
          }

          // Прочитать узел, заявленный в URL, и проверить права юзера для доступа на него.
          val nodeAdmOptFut = FutureUtil.optFut2futOpt(onNodeId)( isNodeAdmin.isAdnNodeAdmin(_, user) )

          // Запускаем инициализацию полей модели user, т.к. маловероятно, что этот реквест пойдёт мимо кассы.
          MUserInits.initUser(user, userInits1)

          lazy val logPrefix = s"($orderId${onNodeId.fold("")(" node#" + _)})[${request.remoteClientAddress}]:"

          // Функция с логикой вне узла, узел уже считается проверенным к этому моменту.
          def __proceed(mnodeOpt: Option[MNode]): Future[Result] = {
            usrContractIdOptFut.flatMap {

              // У юзера есть контракт.
              case Some(userContractId) =>
                morderOptFut.flatMap {

                  // Есть запрошенный ордер. Надо проверить права доступа на ордер.
                  case Some(morder) =>
                    // Сверить номера контрактов.
                    if ((morder.contractId ==* userContractId) || user.isSuper) {
                      val req1 = MNodeOptOrderReq(
                        morder    = morder,
                        mnodeOpt  = mnodeOpt,
                        user      = user,
                        request   = request
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
          }

          // Узел скорее всего в кеше, поэтому проверяем по узлу в первую очередь.
          nodeAdmOptFut.flatMap {
            // Есть запрошенный узел, и текущий юзер является админом этого узла.
            case someNode @ Some(_) =>
              __proceed( someNode )

            case noNode @ None =>
              if (onNodeId.isDefined) {
                LOGGER.warn(s"$logPrefix User $personId has is NOT admin of node $onNodeId.")
                forbid
              } else {
                __proceed( noNode )
              }
          }
        }
      }

    }
  }

}


/** Интерфейс для DI-поля с инстансом [[CanViewOrder]]. */
trait ICanViewOrder {
  val canViewOrder: CanViewOrder
}
