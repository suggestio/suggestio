package controllers.sysctl.mdr

import controllers.routes
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.item.IMItem
import io.suggest.mbill2.m.item.typ.{MItemType, MItemTypes}
import models._
import models.mctx.Context
import models.mdr._
import models.req.IReq
import play.api.mvc.{Call, Result}
import util.acl._
import util.billing.{Bill2Util, IBill2UtilDi}
import util.di.IScUtil
import util.mdr.SysMdrUtil
import views.html.sys1.mdr._

import scala.concurrent.Future


/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.03.16 12:01
  * Description: Поддержка модерации проплаченных размещений для [[controllers.SysMdr]] контроллера.
  */
trait SysMdrPaid
  extends SysMdrBase
  with IIsSu
  with IBill2UtilDi
  with IScUtil
  with IIsSuNodeDi
{

  override val bill2Util  : Bill2Util
  override val sysMdrUtil : SysMdrUtil
  val isSuItem: IsSuItem
  val isSuItemNode: IsSuItemNode


  import mCommonDi._
  import slick.profile.api._


  /**
    * Вывод страницы со списком карточек для модерации проплаченных размещений.
    *
    * @param args Аргументы поиска модерируемых карточек.
    * @return Страница с плиткой карточек, которые нужно модерировать по-платному направлению.
    */
  def paidAdvs(args: MdrSearchArgs) = csrf.AddToken {
    isSu().async { implicit request =>
      // Залезть в items, найти там размещения, ожидающие подтверждения.
      val dbAction = sysMdrUtil.findPaidAdIds4MdrAction(args)

      // Запустить поиск id карточек по биллингу, а затем и поиск самих карточек.
      val madsFut = slick.db
        .run( dbAction )
        .flatMap { madIds =>
          mNodesCache.multiGet( madIds )
        }

      // Передать управление в _adsPage(), не дожидаясь ничего.
      _adsPage(madsFut, args)
    }
  }


  /**
    * Обработка запроса страницы модерации одной рекламной карточки.
    * Модерация v2 подразумевает также обработку карточек.
    *
    * @param nodeId id карточки.
    * @return Страница
    */
  def forAd(nodeId: String) = csrf.AddToken {
    isSuNode(nodeId).async { implicit request =>
      // Константа лимита отображаемых модератору mitems
      val ITEMS_LIMIT = 20

      // Используем один и тот же query-билдер несколько раз.
      val itemsQuery = sysMdrUtil.itemsQueryAwaiting(nodeId)

      // Нужно запустить сборку списков item'ов на модерацию.
      val mitemsFut = slick.db.run {
        itemsQuery
          // Ограничиваем кол-во запрашиваемых item'ов. Нет никакого смысла вываливать слишком много данных на экран.
          .take(ITEMS_LIMIT)
          // Тяжелая сортировка тут скорее всего не важна, поэтому опускаем её.
          .result
      }

      // Узнать общее кол-во item'ов для карточки, которые нужно отмодерировать.
      val itemsCountFut = slick.db.run {
        itemsQuery
          .length
          .result
      }

      implicit val ctx = implicitly[Context]

      // Для рендера карточки необходим подготовить brArgs
      val brArgsFut = scUtil.focusedBrArgsFor(request.mnode)(ctx)

      val edges = request.mnode.edges

      // Узнать, кто модерировал карточку ранее.
      val freeMdrs = {
        edges
          .withPredicateIter(MPredicates.ModeratedBy)
          .toSeq
      }

      // Найти бесплатные размещения карточки.
      val rcvrsSelf = {
        edges
          .withPredicateIter( MPredicates.Receiver.Self )
          .toSeq
      }

      // Узнать id продьюсера текущей, чтобы шаблон мог им воспользоваться.
      val producerIdOpt = n2NodesUtil.madProducerId( request.mnode )

      for {
      // Дождаться получения необходимых для модерации item'ов
        mitems <- mitemsFut

        // Для рендера инфы по узлам надо получить карту инстансов этих узлов.
        mnodesMapFut = {
          // Собрать карту узлов, чтобы их можно было рендерить
          val nodeIdsSetB = Set.newBuilder[String]

          // Закинуть в карту id продьюсера.
          nodeIdsSetB ++= producerIdOpt

          // Закинуть id модерарировших в общую кучу
          nodeIdsSetB ++= freeMdrs.iterator
            .flatMap(_.nodeIds)

          // Закинуть в карту саморесивера. Он по идее совпадает с id продьюсера, но на всякий случай закидываем...
          nodeIdsSetB ++= rcvrsSelf.iterator
            .flatMap(_.nodeIds)

          // Закинуть в список необходимых узлов те, что в mitems.
          nodeIdsSetB ++= mitems.iterator
            .filter(_.iType == MItemTypes.AdvDirect)
            .flatMap(_.rcvrIdOpt)

          val nodeIdsSet = nodeIdsSetB.result()

          // Запустить запрос карты узлов
          mNodesCache.multiGetMap( nodeIdsSet )
        }

        // Нужно выводить item'ы сгруппированными по смыслу.
        mitemsGrouped = {
          mitems
            .groupBy(_.iType)
            .toSeq
            .sortBy(_._1.strId)
        }

        // Посчитать, кол-во item'ов для рендера уже на пределе, или нет.
        tooManyItems = mitems.size == ITEMS_LIMIT

        // Дождаться оставльных асинхронных данных
        mnodesMap   <- mnodesMapFut
        brArgs      <- brArgsFut
        itemsCount  <- itemsCountFut

      } yield {

        // Собрать аргументы рендера шаблона
        val rargs = MSysMdrForAdTplArgs(
          brArgs        = brArgs,
          mnodesMap     = mnodesMap,
          mitemsGrouped = mitemsGrouped,
          freeAdvs      = rcvrsSelf,
          producer      = producerIdOpt
            .flatMap(mnodesMap.get)
            .getOrElse {
              LOGGER.warn(s"forAd($nodeId): Producer not found/not exists, using self as producer node.")
              request.mnode
            },
          tooManyItems  = tooManyItems,
          itemsCount    = itemsCount,
          freeMdrs      = freeMdrs
        )

        // Отрендерить и вернуть ответ клиенту
        val html = forAdTpl(rargs)(ctx)
        Ok(html)
      }
    }
  }


  /**
    * Модератор решил утвердить все оплаченные модерации сразу.
    *
    * @param nodeId id узла-карточки.
    * @return Редирект на модерацию следующей карточки.
    */
  def approveAllItemsSubmit(nodeId: String) = csrf.Check {
    isSuNode(nodeId).async { implicit request =>
      _processItemsForAd(
        nodeId  = nodeId,
        q       = sysMdrUtil.itemsQueryAwaiting(nodeId)
      )(bill2Util.approveItem)
    }
  }


  /** Обработка пачек item'ов унифицирована. */
  private def _processItemsForAd[Res_t <: IMItem](nodeId: String, q: sysMdrUtil.Q_t)
                                                 (f: Gid_t => DBIOAction[Res_t, NoStream, _]): Future[Result] = {
    lazy val logPrefix = s"_processItemsForAd($nodeId ${System.currentTimeMillis}):"
    for {
      saveRes <- sysMdrUtil._processItemsForAd(nodeId, q)(f)
    } yield {
      val countOk = saveRes.successMask.count(identity)
      val countFail = saveRes.successMask.count(!_)

      if (countFail == 0) {
        LOGGER.debug(s"$logPrefix Success with ${saveRes.itemsCount} items")
        val rdrArgs = MdrSearchArgs(hideAdIdOpt = Some(nodeId))
        Redirect( routes.SysMdr.rdrToNextAd(rdrArgs) )
          .flashing(FLASH.SUCCESS -> s"Одобрено ${saveRes.itemsCount} размещений.")
      } else {
        LOGGER.warn(s"$logPrefix Done with errors, $countOk ok with $countFail failed items.")
        Redirect( routes.SysMdr.forAd(nodeId) )
          .flashing(FLASH.ERROR -> s"$countOk размещений подтверждено, $countFail с ошибками.")
      }
    }
  }


  /** Модератор подтверждает оплаченный item. */
  def approveItemSubmit(itemId: Gid_t) = csrf.Check {
    isSu().async { implicit request =>
      val dbAction = bill2Util.approveItem(itemId)
      for {
        res <- sysMdrUtil._processOneItem(dbAction)
      } yield {
        // Отредиректить юзера для продолжения модерации
        Redirect( routes.SysMdr.forAd(res.mitem.nodeId) )
          .flashing(FLASH.SUCCESS -> s"Размещение #$itemId одобрено.")
      }
    }
  }


  private def _refusePopup(call: Call, form: RefuseForm_t = sysMdrUtil.refuseFormM)
                          (implicit request: IReq[_]): Future[Result] = {
    val args = MSysMdrRefusePopupTplArgs(
      refuseFormM = form,
      submitCall  = call
    )
    val render = _refusePopupTpl(args)
    Ok(render)
  }


  /** Запрос попапам с формой отказа в размещение item'а. */
  def refuseItemPopup(itemId: Gid_t) = csrf.AddToken {
    isSuItemNode(itemId).async { implicit request =>
      _refusePopup(routes.SysMdr.refuseItemSubmit(itemId))
    }
  }


  /** Модератор отвергает оплаченный item с указанием причины отказа. */
  def refuseItemSubmit(itemId: Gid_t) = csrf.Check {
    isSuItem(itemId).async { implicit request =>
      lazy val logPrefix = s"refuseItem($itemId):"
      sysMdrUtil.refuseFormM.bindFromRequest().fold(
        {formWithErrors =>
          LOGGER.debug(s"$logPrefix Failed to bind form:\n ${formatFormErrors(formWithErrors)}")
          Redirect( routes.SysMdr.forAd(request.mitem.nodeId) )
            .flashing(FLASH.ERROR -> "Ошибка в запросе отказа, проверьте причину.")
        },

        {res =>
          // Запустить транзакцию отката оплаченного размещения в кошелек юзера.
          val dbAction  = bill2Util.refuseItem(itemId, Some(res.reason))

          // Когда всё будет выполнено, надо отредиректить юзера на карточку.
          for {
            _ <- sysMdrUtil._processOneItem(dbAction)
          } yield {
            // Отредиректить клиента на модерацию карточки.
            Redirect( routes.SysMdr.forAd(request.mitem.nodeId) )
              .flashing(FLASH.SUCCESS -> s"Отказано в размещении #$itemId.")
          }
        }
      )
    }
  }

  /**
    * Рендер попапа для указания причины отсева множества item'ов для указанной карточки.
    *
    * @param nodeId id узла-карточки, которая модерируется в данный момент.
    * @return HTML попапа с формой отказа в размещении.
    */
  def refuseAllItems(nodeId: String) = csrf.AddToken {
    isSuNode(nodeId).async { implicit request =>
      _refusePopup( routes.SysMdr.refuseAllItemsSubmit(nodeId) )
    }
  }


  /**
    * Множенственный refuse всех модерируемых item'ов, относящихся к указанной карточке.
    *
    * @param nodeId id узла.
    * @return
    */
  def refuseAllItemsSubmit(nodeId: String) = csrf.Check {
    isSuNode(nodeId).async { implicit request =>
      lazy val logPrefix = s"refuseAllItemsSubmit($nodeId):"
      sysMdrUtil.refuseFormM.bindFromRequest().fold(
        {formWithErrors =>
          LOGGER.debug(s"$logPrefix Failed to bind form:\n ${formatFormErrors(formWithErrors)}")
          Redirect( routes.SysMdr.forAd(nodeId) )
            .flashing(FLASH.ERROR -> "Ошибка в запросе отказа, проверьте причину.")
        },

        {res =>
          val someReason = Some(res.reason)
          // Запустить транзакцию отката оплаченного размещения в кошелек юзера.
          _processItemsForAd(
            nodeId = nodeId,
            q      = sysMdrUtil.itemsQueryAwaiting(nodeId)
          )(bill2Util.refuseItem(_, someReason))
        }
      )
    }
  }


  /**
    * Массовый аппрув item'ов, относящихся к какой-то группе item'ов.
    *
    * @param nodeId id узла-карточки.
    * @param itype id типа item'ов.
    * @return Редирект на текущую карточку.
    */
  def approveAllItemsTypeSubmit(nodeId: String, itype: MItemType) = csrf.Check {
    isSuNode(nodeId).async { implicit request =>
      _processItemsForAd(
        nodeId = nodeId,
        q = sysMdrUtil.onlyItype( sysMdrUtil.itemsQueryAwaiting(nodeId), itype )
      )(bill2Util.approveItem)
    }
  }


  def refuseAllItemsType(nodeId: String, itype: MItemType) = csrf.AddToken {
    isSuNode(nodeId).async { implicit request =>
      _refusePopup( routes.SysMdr.refuseAllItemsTypeSubmit(nodeId, itype) )
    }
  }


  def refuseAllItemsTypeSubmit(nodeId: String, itype: MItemType) = csrf.Check {
    isSuNode(nodeId).async { implicit request =>
      lazy val logPrefix = s"refuseAllItemsTypeSubmit($nodeId, $itype):"
      sysMdrUtil.refuseFormM.bindFromRequest().fold(
        {formWithErrors =>
          LOGGER.debug(s"$logPrefix Failed to bind form:\n ${formatFormErrors(formWithErrors)}")
          Redirect( routes.SysMdr.forAd(nodeId) )
            .flashing(FLASH.ERROR -> "Ошибка в запросе отказа, проверьте причину.")
        },
        {res =>
          val someReason = Some(res.reason)
          _processItemsForAd(
            nodeId  = nodeId,
            q       = sysMdrUtil.onlyItype( sysMdrUtil.itemsQueryAwaiting(nodeId), itype)
          )(bill2Util.refuseItem(_, someReason))
        }
      )
    }
  }

}
