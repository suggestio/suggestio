package util.acl

import javax.inject.{Inject, Singleton}
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.item.status.MItemStatuses
import io.suggest.mbill2.m.item.{MItem, MItems}
import io.suggest.mbill2.m.order.{MOrderStatuses, MOrders}
import io.suggest.req.ReqUtil
import io.suggest.util.logs.MacroLogsImpl
import models.mproj.ICommonDi
import models.req.{MItemReq, MOrderIdsReq, MUserInit, MUserInits}
import play.api.mvc._
import japgolly.univeq._
import play.api.http.Status
import slick.dbio.DBIOAction

import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.03.16 10:15
  * Description: Аддон для контроллеров с поддержкой проверки прав доступа на изменение item'а.
  */
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

  /** Если много ids за раз, то тут лимит по кол-ву. */
  private def MAX_ITEM_IDS_PER_REQUEST = 50


  /** Проверка прав доступа к read/write к целому списку item'ов.
    *
    * @param itemIds Список item-ids без дубликатов.
    * @param edit Доступ на редактирование? false значит read-only.
    * @param userInits1 Цели для параллельной инициализации.
    * @return ActionBuider.
    */
  def apply(itemIds: Seq[Gid_t], edit: Boolean, userInits1: MUserInit*): ActionBuilder[MOrderIdsReq, AnyContent] = {
    val itemIdsLen = itemIds.length
    lazy val logPrefix = s"${getClass.getSimpleName}([$itemIdsLen]:${itemIds.mkString(",")}):"

    new reqUtil.SioActionBuilderImpl[MOrderIdsReq] {

      override def invokeBlock[A](request: Request[A], block: MOrderIdsReq[A] => Future[Result]): Future[Result] = {
        // Нужно с помощью нескольких SQL-запросов проверить, что item'ы находятся в статусе Draft,
        // и все они относятся к draft-ордеру (ордерам?).
        // Затем проверить доступ к ордеру по контракту текущего юзера.
        val user = aclUtil.userFromRequest( request )

        val maxItemIdsLen = MAX_ITEM_IDS_PER_REQUEST
        if (itemIds.isEmpty) {
          LOGGER.warn(s"$logPrefix No items defined in request url")
          errorHandler.onClientError( request, Status.BAD_REQUEST, "Ids expected")

        } else if (itemIdsLen > maxItemIdsLen) {
          LOGGER.warn(s"$logPrefix Too many item ids: $itemIdsLen, allowed max = $maxItemIdsLen")
          errorHandler.onClientError( request, Status.BAD_REQUEST, s"Too many ids: $itemIdsLen/$maxItemIdsLen" )

        } else if (user.isAnon) {
          // Анонимус по определению не может иметь доступа к биллингу.
          LOGGER.trace(s"$logPrefix Not logged in")
          isAuth.onUnauth(request)

        } else {
          val itemIdsSet = itemIds.toSet
          val itemIdsCount = itemIdsSet.size
          if (itemIdsCount !=* itemIdsLen) {
            // В списке есть повторяющиеся элементы. В этом нет смысла, и дальнейшие проверки невозможны.
            LOGGER.warn(s"$logPrefix Duplicate ids in itemIds, distict[$itemIdsCount, but $itemIdsLen expected] = [${itemIdsSet.mkString(", ")}]")
            errorHandler.onClientError( request, Status.BAD_REQUEST, "duplicate ids" )

          } else {
            // Запустить различные параллельные проверки. item'ов может быть много, поэтому все проверки - на стороне СУБД, без выкачивания item'ов сюда.
            val mcIdOptFut = user.contractIdOptFut

            // Запустить проверки на стороне СУБД:
            // 1. Все item'ы существуют. И если edit, то существуют со статусом Draft.
            // 2. Все ордеры этих item'ов в статусах корзин.
            // 3. Все эти ордеры законтрактованы текущим юзером.
            val checkItemsResFut = slick.db.run {
              val dbAction = for {
                // Посчитать, сколько item'ов подходит для ситуации:
                okItemsCount <- mItems.countByIdStatus(
                  itemIds  = itemIds,
                  statuses = if (edit) MItemStatuses.Draft :: Nil else Nil
                )
                if okItemsCount ==* itemIdsCount

                // Узнать id ордеров, к которым относятся item'ы.
                orderIds <- mItems.getOrderIds( itemIds )
                if orderIds.nonEmpty

                // Проверить, что все ордеры относятся к контракту юзера, и находятся в нужном статусе
                contractIdOpt <- DBIOAction.from( mcIdOptFut )
                contractId = contractIdOpt.get
                okOrdersCount <- mOrders.countByIdStatusContract(
                  ids         = orderIds,
                  statuses    = if (edit) MOrderStatuses.Draft :: Nil else Nil,
                  contractIds = contractId :: Nil
                )
                if okOrdersCount ==* orderIds.size

              } yield {
                LOGGER.trace(s"$logPrefix Verified ok: orderIds=${orderIds.mkString(", ")} contractId=$contractId")
                (orderIds, contractId)
              }
              // Не ясно, нужна ли транзакция, т.к. read-only.
              import slick.profile.api._
              dbAction.transactionally
            }

            // Запустить также инициализацию пользовательских данных в фоне, т.к. скорее всего проверка прав закончится успешно...
            MUserInits.initUser(user, userInits1)

            // Трансформировать результат проверки в результат запроса:
            checkItemsResFut.transformWith {
              // Успешная проверка доступа.
              case Success((orderIds, contractId)) =>
                val mreq = MOrderIdsReq(
                  orderIds    = orderIds,
                  contractId  = contractId,
                  request     = request,
                  user        = user
                )
                block(mreq)

              // Какая-то ошибка проверки прав.
              case Failure(ex) =>
                LOGGER.warn(s"$logPrefix Failed to ACL", ex)
                isAuth.onUnauth( request )
            }
          }
        }
      }

    }
  }


  /** @param itemId Номер item'а в таблице MItems.
    * @param edit Запрашивается доступ для изменения?
    *             false -- планируется только чтение.
    *             true -- планируется изменение/удаление.
    */
  def apply(itemId: Gid_t, edit: Boolean, userInits1: MUserInit*): ActionBuilder[MItemReq, AnyContent] = {
    lazy val logPrefix = s"${getClass.getSimpleName}($itemId):"

    new reqUtil.SioActionBuilderImpl[MItemReq] {

      protected def isItemAccessable(mitem: MItem): Boolean = {
        if (edit) {
          // Для редактирования доступны только ещё не оплаченные item'ы.
          mitem.status ==* MItemStatuses.Draft
        } else {
          true
        }
      }


      // Нужно пройти по цепочке: itemId -> orderId -> contractId. И сопоставить contractId с контрактом текущего юзера.
      override def invokeBlock[A](request: Request[A], block: (MItemReq[A]) => Future[Result]): Future[Result] = {
        val user = aclUtil.userFromRequest(request)

        if (user.isAnon) {
          // Анонимус по определению не может иметь доступа к биллингу.
          LOGGER.trace(s"$logPrefix Not logged in")
          isAuth.onUnauth(request)

        } else {
          // Получить на руки запрашиваемый MItem. Его нужно передать в action внутри реквеста.
          val mitemOptFut = slick.db.run {
            mItems.getById(itemId)
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
          MUserInits.initUser(user, userInits1)

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
