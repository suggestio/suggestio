package util.acl

import io.suggest.common.fut.FutureUtil
import io.suggest.mbill2.m.item.{MItem, MItems}
import io.suggest.mbill2.m.item.status.MItemStatuses
import io.suggest.mbill2.m.item.typ.MItemTypes
import io.suggest.req.ReqUtil
import io.suggest.sys.mdr.MMdrResolution
import io.suggest.util.logs.MacroLogsImpl
import javax.inject.Inject
import models.req.MItemOptAdNodesChainReq
import play.api.http.Status
import play.api.mvc.{ActionBuilder, AnyContent, Request, Result}
import japgolly.univeq._
import models.mproj.ICommonDi

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.10.18 14:37
  * Description: ACL для проверки команды модерации от обычного или необычного юзера.
  */
class CanMdrResolute @Inject()(
                                aclUtil                 : AclUtil,
                                reqUtil                 : ReqUtil,
                                mItems                  : MItems,
                                isNodeAdmin             : IsNodeAdmin,
                                isAuth                  : IsAuth,
                                mCommonDi               : ICommonDi,
                              )
  extends MacroLogsImpl
{

  import mCommonDi.{slick, ec, mNodesCache, errorHandler => httpErrorHandler}

  /** Проверка прав доступа на дальнейшее исполнение резолюции модерации.
    * Следует помнить, что тело экшена также должно понимать, что это su-вызов или обычный юзер это модерирует,
    * внося коррективы в собственные действия.
    *
    * @param mdrRes Результат модерации, присланный юзером.
    * @return Экшен-билдер, возвращающий запрос, содержащий накопленные при исполнении данные.
    *         Надо понимать, что если su, то mitemOpt и nodesChain не проверяются - они пусты, даже если существуют.
    */
  def apply(mdrRes: MMdrResolution): ActionBuilder[MItemOptAdNodesChainReq, AnyContent] = {
    new reqUtil.SioActionBuilderImpl[MItemOptAdNodesChainReq] {

      override def invokeBlock[A](request: Request[A], block: MItemOptAdNodesChainReq[A] => Future[Result]): Future[Result] = {
        val req0 = aclUtil.reqFromRequest(request)
        lazy val logPrefix = s"(${mdrRes.nodeId} u#${req0.user.personIdOpt.orNull})#${System.currentTimeMillis()}:"

        LOGGER.trace(s"$logPrefix Starting for $mdrRes")

        // Запустить получение данных по узлу.
        val mnodeOptFut = mNodesCache.getById( mdrRes.nodeId )

        mnodeOptFut.flatMap {
          // Найден узел, как и ожидалось. Надо понять, есть ли доступ у юзера на модерацию.
          case Some(mnode) =>
            import mdrRes.{info => nfo}
            lazy val userMdrAllowedItemTypes = MItemTypes.userMdrAllowed

            if (req0.user.isSuper) {
              // Сразу запустить экшен, т.к. нет смысла
              block( MItemOptAdNodesChainReq.apply(None, mnode, Nil, req0.user, req0) )

              // Обычный юзер может модерировать rcvr-размещения на собственных узлах (и под-узлах).
              // Надо понять, подходит ли резолюция под допустимую:
            } else if (
              // Юзер НЕ может модерировать direct-free (это неявная пост-модерация только сверху s.io):
              nfo.directSelfAll || nfo.directSelfId.nonEmpty ||
              // Юзер модерирует входящие AdvDirect, ЛИБО item по id (в рамках разрешённых типов - это проверяется асинхронно):
              (nfo.itemId.nonEmpty && nfo.itemType.nonEmpty) ||
              // Нельзя модерировать типы, которые недопускаются моделью для user-модерации:
              !nfo.itemType.fold(true)( userMdrAllowedItemTypes.contains )
            ) {
              LOGGER.warn(s"$logPrefix User#${req0.user.personIdOpt.orNull} !su and cannot resolute in such way: $mdrRes")
              // Недопустимо для обычного юзера модерачить такие размещения.
              httpErrorHandler.onClientError(request, Status.FORBIDDEN)

            } else {
              // Код проверки ресивера на доступность для текущего юзера.
              def __checkRcvrAllowed(rcvrId: String) = for {
                allowedRcvrChainOpt <- isNodeAdmin.isNodeAdminUpFrom( rcvrId, req0.user )
                allowedRcvrChain = {
                  if (allowedRcvrChainOpt.isEmpty)
                    LOGGER.warn(s"$logPrefix isNodeAdmin() => false: permission denied.")
                  allowedRcvrChainOpt.get
                }
              } yield {
                LOGGER.trace(s"$logPrefix mdrRes.rcvrId#$rcvrId allowed ok: nodesChain=[${allowedRcvrChain.iterator.flatMap(_.id).mkString(" / ")}]")
                Some(allowedRcvrChain)
              }

              // Запустить проверку ресивера, заданного в mdrRes.
              val qsRcvrAllowedChainOptFut = FutureUtil.optFut2futOpt(mdrRes.rcvrIdOpt) { __checkRcvrAllowed }

              val itemNodeChainOptOrExFut = nfo.itemId.fold {
                // itemId не задан. Если rcvrId тоже не задан, то права можно дальше не проверять: границы исполнения будут неявно навязаны в теле экшена.
                for {
                  allowedRcvrChainOpt <- qsRcvrAllowedChainOptFut
                } yield {
                  // Допускается, что rcvrId может быть не задан !su-юзером, это означает что юзер аппрувит на всех своих узлах.
                  val qsRcvrNodeChain = allowedRcvrChainOpt getOrElse Nil
                  (Option.empty[MItem], qsRcvrNodeChain)
                }
              } { itemId =>
                LOGGER.trace(s"$logPrefix Will check item#${itemId}...")
                // Если itemID задан, то надо проверить принадлежность item'а к допустимости модерации текущим юзером.
                // Иначе - можно сразу запускать экшен, чтобы в экшене оно разрулилось само.
                for {
                  // Прочитать item, модерация которого запрашивается:
                  mitemOpt <- slick.db.run {
                    mItems.getById(itemId)
                  }

                  // Пусть будет NSEE, если нет такого item'а или item непонятного типа.
                  mitem = {
                    if (mitemOpt.isEmpty)
                      LOGGER.warn(s"$logPrefix Item#$itemId does not exists.")
                    mitemOpt.get
                  }

                  // Проверить допустимость статуса и типа item'а:
                  if {
                    val isAwaitingMdr = (mitem.status ==* MItemStatuses.AwaitingMdr)
                    if (!isAwaitingMdr)
                      LOGGER.warn(s"$logPrefix Item#$itemId exist, but status is ${mitem.status}, not awaiting mdr.")
                    isAwaitingMdr
                  } && {
                    // И что тип item'а допустим:
                    val isItemTypeOk = (userMdrAllowedItemTypes contains mitem.iType)
                    if (!isItemTypeOk)
                      LOGGER.warn(s"$logPrefix Item#$itemId itype=$isItemTypeOk, but allowed for user-mdr types = [${userMdrAllowedItemTypes.mkString(", ")}]")
                    isItemTypeOk
                  } && {
                    // Если задан mdrRes.rcvrId, то он должен точно совпадать c mitem.rcvrId:
                    val r = mdrRes.rcvrIdOpt.fold(true)(mitem.rcvrIdOpt.contains)
                    if (!r)
                      LOGGER.warn(s"$logPrefix Item#$itemId.rcvrId#${mitem.rcvrIdOpt.orNull} does not match to qs.rcvrId=${mdrRes.rcvrIdOpt.orNull}")
                    r
                  }

                  // Узнать целевой rcvr-узел, чтобы можно было проверить права на него:
                  itemRcvrId = {
                    if (mitem.rcvrIdOpt.isEmpty)
                      LOGGER.warn(s"$logPrefix item#$itemId rcvrId is missing, impossible to check node admin/mdr-permissions")
                    mitem.rcvrIdOpt.get
                  }

                  // Проверяем item.rcvrId:
                  itemRcvrIdNodeChainOpt <- if (mdrRes.rcvrIdOpt.isEmpty) {
                    // Возможна ситуация, когда mdrRes.rcvrId пуст, но у item'а есть ресивер - тогда надо проверить item.rcvrId отдельно:
                    val itemRcvrIdNodeChainOptFut = __checkRcvrAllowed(itemRcvrId)
                    // Убедится, что проверка mdrRes.rcvrId закончена без exception:
                    qsRcvrAllowedChainOptFut
                      .flatMap(_ => itemRcvrIdNodeChainOptFut)
                  } else {
                    // rcvrId задан в mdrRes и mitem, они совпадают (см. if выше). Возвращаем фьючерс с проверки mdrRes:
                    qsRcvrAllowedChainOptFut
                  }

                  itemRcvrIdNodeChain = {
                    if (itemRcvrIdNodeChainOpt.isEmpty)
                      LOGGER.warn(s"$logPrefix Item.rcvrId#$itemRcvrId fails to check nodesChain, denied")
                    itemRcvrIdNodeChainOpt.get
                  }

                } yield {
                  LOGGER.debug(s"$logPrefix Allowed")
                  // Тут надо возвращать или ошибку или Some(mitem), если всё выверено.
                  (mitemOpt, itemRcvrIdNodeChain)
                }
              }

              itemNodeChainOptOrExFut
                .flatMap { case (mitemOpt, nodesChain) =>
                  // Есть доступ. Собрать запрос, запустить тело экшена.
                  val req2 = MItemOptAdNodesChainReq(
                    mitemOpt    = mitemOpt,
                    mnode       = mnode,
                    nodesChain  = nodesChain,
                    user        = req0.user,
                    request     = req0
                  )
                  block(req2)
                }
                .recoverWith {
                  // item-id задан, но проверка доступа не удалась. Значит нет доступа.
                  case ex: Throwable =>
                    LOGGER.trace(s"$logPrefix User-mdr forbidden", ex)
                    if (req0.user.isAuth)
                      httpErrorHandler.onClientError( request, Status.FORBIDDEN )
                    else
                      isAuth.onUnauth(req0)
                }
            }

          // Нет узла, который так важен в вопросе модерации:
          case None =>
            LOGGER.warn(s"$logPrefix Node not found: ${mdrRes.nodeId}")
            httpErrorHandler.onClientError(request, Status.NOT_FOUND)
        }
      }

    }
  }

}
