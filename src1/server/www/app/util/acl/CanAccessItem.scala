package util.acl

import com.google.inject.{Inject, Singleton}
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.item.status.MItemStatuses
import io.suggest.mbill2.m.item.{MItem, MItems}
import io.suggest.mbill2.m.order.MOrders
import io.suggest.util.logs.MacroLogsImpl
import models.mproj.ICommonDi
import models.req.{MItemReq, MUserInit}
import play.api.mvc.{ActionBuilder, Request, Result}

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.03.16 10:15
  * Description: Аддон для контроллеров с поддержкой проверки прав доступа на изменение item'а.
  */
@Singleton
class CanAccessItem @Inject() (
                                mItems                  : MItems,
                                mOrders                 : MOrders,
                                override val mCommonDi  : ICommonDi
                              )
  extends Csrf
  with MacroLogsImpl
{

  import mCommonDi._

  /** Трейт для read и rw-доступа один и тот же.
    * Нужно просто реализовать метод isItemAccessable(). */
  sealed trait CanItemBase
    extends ActionBuilder[MItemReq]
    with OnUnauthUtil
    with InitUserCmds
  {

    /** Номер item'а в таблице MItems. */
    def itemId: Gid_t

    // Нужно пройти по цепочке: itemId -> orderId -> contractId. И сопоставить contractId с контрактом текущего юзера.
    override def invokeBlock[A](request: Request[A], block: (MItemReq[A]) => Future[Result]): Future[Result] = {

      val _itemId = itemId
      lazy val logPrefix = s"${getClass.getSimpleName}(${_itemId}):"

      val personIdOpt = sessionUtil.getPersonId(request)
      val user = mSioUsers(personIdOpt)

      if (user.isAnon) {
        // Анонимус по определению не может иметь доступа к биллингу.
        LOGGER.trace(s"$logPrefix Not logged in")
        onUnauth(request)

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
                      LOGGER.warn(s"$logPrefix User $personIdOpt contract[$userContractId] tried to access to item of contract[$itemContractIdOpt]")
                      itemForbidden(request)
                    }
                  }

                // У юзера нет контракта, item'ов у него тоже быть не может.
                case None =>
                  LOGGER.warn(s"User $personIdOpt with NO contract refused to edit item")
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
      onUnauth(request)
    }

    protected def isItemAccessable(mitem: MItem): Boolean

  }


  /** Дореализация [[CanItemBase]] для возможности выбирать проверяемые права флагом write. */
  sealed trait CanAccessItemBase extends CanItemBase {

    /**
      * Запрашивается доступ для изменения?
      *
      * @return false -- планируется только чтение.
      *         true -- планируется изменение/удаление.
      */
    def edit: Boolean

    override protected def isItemAccessable(mitem: MItem): Boolean = {
      if (edit) {
        // Для редактирования доступны только ещё не оплаченные item'ы.
        mitem.status == MItemStatuses.Draft
      } else {
        true
      }
    }
  }

  abstract class CanAccessItemAbstract
    extends CanAccessItemBase
    with ExpireSession[MItemReq]

  case class CanAccessItem(override val itemId: Gid_t, override val edit: Boolean, override val userInits: MUserInit*)
    extends CanAccessItemAbstract

  case class CanAccessItemGet(override val itemId: Gid_t, override val edit: Boolean, override val userInits: MUserInit*)
    extends CanAccessItemAbstract
    with CsrfGet[MItemReq]

  case class CanAccessItemPost(override val itemId: Gid_t, override val edit: Boolean, override val userInits: MUserInit*)
    extends CanAccessItemAbstract
    with CsrfPost[MItemReq]

}


trait ICanAccessItemDi {
  val canAccessItem: CanAccessItem
}
