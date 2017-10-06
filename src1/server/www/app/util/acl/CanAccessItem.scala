package util.acl

import javax.inject.{Inject, Singleton}

import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.item.status.MItemStatuses
import io.suggest.mbill2.m.item.{MItem, MItems}
import io.suggest.mbill2.m.order.MOrders
import io.suggest.req.ReqUtil
import io.suggest.util.logs.MacroLogsImpl
import models.mproj.ICommonDi
import models.req.{MItemReq, MUserInit}
import play.api.mvc.{ActionBuilder, AnyContent, Request, Result}

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.03.16 10:15
  * Description: Аддон для контроллеров с поддержкой проверки прав доступа на изменение item'а.
  */
@Singleton
class CanAccessItem @Inject() (
                                aclUtil                 : AclUtil,
                                mItems                  : MItems,
                                mOrders                 : MOrders,
                                isAuth                  : IsAuth,
                                reqUtil                 : ReqUtil,
                                mCommonDi               : ICommonDi
                              )
  extends MacroLogsImpl
{

  import mCommonDi._

  /** @param itemId Номер item'а в таблице MItems.
    * @param edit Запрашивается доступ для изменения?
    *             false -- планируется только чтение.
    *             true -- планируется изменение/удаление.
    */
  def apply(itemId: Gid_t, edit: Boolean, userInits1: MUserInit*): ActionBuilder[MItemReq, AnyContent] = {
    new reqUtil.SioActionBuilderImpl[MItemReq] with InitUserCmds {

      override def userInits = userInits1

      protected def isItemAccessable(mitem: MItem): Boolean = {
        if (edit) {
          // Для редактирования доступны только ещё не оплаченные item'ы.
          mitem.status == MItemStatuses.Draft
        } else {
          true
        }
      }


      // Нужно пройти по цепочке: itemId -> orderId -> contractId. И сопоставить contractId с контрактом текущего юзера.
      override def invokeBlock[A](request: Request[A], block: (MItemReq[A]) => Future[Result]): Future[Result] = {

        val _itemId = itemId
        lazy val logPrefix = s"${getClass.getSimpleName}(${_itemId}):"

        val user = aclUtil.userFromRequest(request)

        if (user.isAnon) {
          // Анонимус по определению не может иметь доступа к биллингу.
          LOGGER.trace(s"$logPrefix Not logged in")
          isAuth.onUnauth(request)

        } else {
          // Получить на руки запрашиваемый MItem. Его нужно передать в action внутри реквеста.
          val mitemOptFut = slick.db.run {
            mItems.getById(_itemId)
          }

          // Узнать id контракта юзера.
          val userContractIdOptFut = {
            // Оптимизация: Для суперюзера значение этого параметра вычислять не требуется.
            if (user.isSuper) {
              null
            } else {
              // Для обычного юзера требуется узнать id контракта в фоне.
              user.contractIdOptFut
            }
          }

          // Запустить также инициализацию пользовательских данных в фоне, т.к. скорее всего проверка прав закончится успешно...
          maybeInitUser(user)

          // Узнать id контракта ордера, относящиегося к item'у.
          mitemOptFut.flatMap {
            // Есть запрошенный item в базе, ничего неожиданного.
            case Some(mitem) if isItemAccessable(mitem) =>
              // Для дедубликации кода собираем тут функцию возврата положительного ответа.
              def okF(): Future[Result] = {
                val req1 = MItemReq(mitem, request, user)
                block(req1)
              }

              // Одобряем суперюзера в обход всех проверок.
              if (user.isSuper) {
                okF()

              } else {
                // Запустить получение id контракта для ордера.
                val itemContractIdOptFut = slick.db.run {
                  mOrders.getContractId(mitem.orderId)
                }

                // Пока проверить, как дела с поиском id контракта текущего юзера.
                userContractIdOptFut.flatMap {
                  // У юзера есть какой-то свой контракт
                  case Some(userContractId) =>
                    // Дождаться получения id контракта item'а...
                    itemContractIdOptFut.flatMap { itemContractIdOpt =>
                      // Если id контрактов совпадают, то проверка выполнена удачно.
                      if ( itemContractIdOpt.contains(userContractId) ) {
                        okF()
                      } else {
                        LOGGER.warn(s"$logPrefix User#${user.personIdOpt.orNull} contract[$userContractId] tried to access to item of contract[$itemContractIdOpt]")
                        itemForbidden(request)
                      }
                    }

                  // У юзера нет контракта, item'ов у него тоже быть не может.
                  case None =>
                    LOGGER.warn(s"User#${user.personIdOpt.orNull} with NO contract refused to edit item")
                    itemForbidden(request)
                }
              }

            // Item не найден, внезапно.
            case unaccessableOpt =>
              unaccessableOpt.fold {
                LOGGER.debug(s"$logPrefix item not exists")
              } { mitem =>
                LOGGER.warn(s"$logPrefix Item exists, but not accessable: $mitem")
              }
              // Возвращаем 403, чтобы защититься от сканирования id ордеров.
              itemForbidden(request)
          }
        }

      }

      /** Результат с отказом доступа к item'у. */
      def itemForbidden(request: Request[_]): Future[Result] = {
        isAuth.onUnauth(request)
      }

    }
  }

}


/** Интерфейс для поля с DI-инстансом [[CanAccessItem]]. */
trait ICanAccessItemDi {
  val canAccessItem: CanAccessItem
}
