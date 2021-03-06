package util.acl

import javax.inject.Inject
import io.suggest.es.model.MEsUuId
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.order.MOrders
import io.suggest.model.SlickHolder
import io.suggest.req.ReqUtil
import io.suggest.util.logs.MacroLogsImpl
import models.req.{MNodeOrderReq, MUserInit, MUserInits}
import play.api.inject.Injector
import play.api.mvc.{ActionBuilder, AnyContent, Request, Result}

import scala.concurrent.{ExecutionContext, Future}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.02.17 15:06
  * Description: Проверка прав биллинга: может ли юзер оплачивать заказ?
  * Да, если заказ в состоянии "корзины" и содержит item'ы.
  */
final class CanPayOrder @Inject() (
                                    injector: Injector,
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


  def apply(orderId         : Gid_t,
            onNodeId        : MEsUuId,
            onAlreadyPaidF  : MNodeOrderReq[_] => Future[Result],
            userInits1      : MUserInit*
           ): ActionBuilder[MNodeOrderReq, AnyContent] = {

    new reqUtil.SioActionBuilderImpl[MNodeOrderReq] {

      override def invokeBlock[A](request0: Request[A], block: (MNodeOrderReq[A]) => Future[Result]): Future[Result] = {
        val request = aclUtil.reqFromRequest( request0 )

        // Отказ юзеру в обслуживании: для защиты от сканирования id и прочего, ответ непонятным юзерам всегда один.
        def forbid = isAuth.onUnauth(request)

        val user = aclUtil.userFromRequest(request)

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
          val nodeAdmOptFut = isNodeAdmin.isAdnNodeAdmin(onNodeId, user)

          // Запускаем инициализацию полей модели user, т.к. маловероятно, что этот реквест пойдёт мимо кассы.
          MUserInits.initUser(user, userInits1)

          lazy val logPrefix = s"invokeBlock($orderId)[${request.remoteClientAddress}]:"

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
                        // Этот контракт принадлежит текущему юзеру. Но не факт, что ордер доступен для оплаты.
                        if (morder.status.canGoToPaySys) {
                          // Всё ок, передать управление экшену контроллера.
                          block(req1)

                        } else {
                          LOGGER.warn(s"$logPrefix Order[$orderId] is not payable. Order status is '${morder.status}' since ${morder.dateStatus}, user[$personId]")
                          onAlreadyPaidF(req1)
                        }

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

  }

}
