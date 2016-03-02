package controllers.sysctl.mdr

import controllers.routes
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.item.{MItem, MItems, IMItem, ItemStatusChanged}
import io.suggest.mbill2.m.item.status.MItemStatuses
import io.suggest.mbill2.m.item.typ.{MItemType, MItemTypes}
import io.suggest.mbill2.util.effect.RW
import models._
import models.mctx.Context
import models.mdr._
import models.req.IReq
import play.api.mvc.{Call, Result}
import util.acl.{IsSuItem, IsSuItemAd, IsSuperuser, IsSuperuserMad}
import util.billing.{Bill2Util, IBill2UtilDi}
import util.di.IScUtil
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
  with IsSuItem
  with IsSuperuser
  with IsSuItemAd
  with IsSuperuserMad
  with IBill2UtilDi
  with IScUtil
{

  import mCommonDi._
  import slick.driver.api._

  override val bill2Util: Bill2Util
  override val mItems: MItems

  /** SQL для экшена поиска id карточек, нуждающихся в модерации. */
  protected[this] def _findPaidAdIds4MdrAction(args: MdrSearchArgs) = {
    val b0 = mItems
      .query
      .filter { i =>
        (i.iTypeStr inSet MItemTypes.onlyAdvTypesIds) &&
          (i.statusStr === MItemStatuses.AwaitingSioAuto.strId)
      }

    val b1 = args.hideAdIdOpt.fold(b0) { hideAdId =>
      b0.filter { i =>
        i.adId =!= hideAdId
      }
    }

    b1.map(_.adId)
      //.sortBy(_.id.asc)   // TODO Нужно подумать над сортировкой возвращаемого множества adId
      .distinct
      .take( args.limit )
      .drop( args.offset )
      .result
  }

  /**
    * Вывод страницы со списком карточек для модерации проплаченных размещений.
    *
    * @param args Аргументы поиска модерируемых карточек.
    * @return Страница с плиткой карточек, которые нужно модерировать по-платному направлению.
    */
  def paidAdvs(args: MdrSearchArgs) = IsSuperuser.async { implicit request =>
    // Залезть в items, найти там размещения, ожидающие подтверждения.
    val dbAction = _findPaidAdIds4MdrAction(args)

    // Запустить поиск id карточек по биллингу, а затем и поиск самих карточек.
    val madsFut = slick.db
      .run( dbAction )
      .flatMap { madIds =>
        mNodeCache.multiGet( madIds )
      }

    // Передать управление в _adsPage(), не дожидаясь ничего.
    _adsPage(madsFut, args)
  }

  // Общий код сборки всех SQL queries для сборки items модерации карточки
  private def _itemsQuery(nodeId: String) = {
    mItems.query
      .filter { i =>
        (i.adId === nodeId) &&
          (i.statusStr === MItemStatuses.AwaitingSioAuto.strId)
      }
  }


  /**
    * Обработка запроса страницы модерации одной рекламной карточки.
    * Модерация v2 подразумевает также обработку карточек.
    *
    * @param nodeId id карточки.
    * @return Страница
    */
  def forAd(nodeId: String) = IsSuperuserMadGet(nodeId).async { implicit request =>
    // Константа лимита отображаемых модератору mitems
    val ITEMS_LIMIT = 20

    // Используем один и тот же query-билдер несколько раз.
    val itemsQuery = _itemsQuery(nodeId)

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

    // Найти бесплатные размещения, подлежащие модерации
    val rcvrsSelf: Seq[MEdge] = {
      val es = request.mad.edges
      // Поиска self-receiver среди эджей:
      es.withPredicateIter( MPredicates.Receiver.Self )
        .filter { selfE =>
          // Если какой-то модератор уже сделал модерацию карточки, то не требуется модерировать бесплатное размещение:
          es.withPredicateIter( MPredicates.ModeratedBy ).isEmpty
        }
        .toSeq
    }

    implicit val ctx = implicitly[Context]

    // Для рендера карточки необходим подготовить brArgs
    val brArgsFut = scUtil.focusedBrArgsFor(request.mad)(ctx)

    // Узнать id продьюсера текущей, чтобы шаблон мог им воспользоваться.
    val producerIdOpt = n2NodesUtil.madProducerId( request.mad )

    for {
      // Дождаться получения необходимых для модерации item'ов
      mitems <- mitemsFut

      // Для рендера инфы по узлам надо получить карту инстансов этих узлов.
      mnodesMapFut = {
        // Собрать карту узлов, чтобы их можно было рендерить
        val nodeIdsSetB = Set.newBuilder[String]

        // Закинуть в карту id продьюсера.
        nodeIdsSetB ++= producerIdOpt

        // Закинуть в карту саморесивера. Он по идее совпадает с id продьюсера, но на всякий случай закидываем...
        nodeIdsSetB ++= rcvrsSelf.iterator
          .map(_.nodeId)

        // Закинуть в список необходимых узлов те, что в mitems.
        nodeIdsSetB ++= mitems.iterator
          .filter(_.iType == MItemTypes.AdvDirect)
          .flatMap(_.rcvrIdOpt)

        val nodeIdsSet = nodeIdsSetB.result()

        // Запустить запрос карты узлов
        mNodeCache.multiGetMap( nodeIdsSet )
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
            request.mad
          },
        tooManyItems  = tooManyItems,
        itemsCount    = itemsCount
      )

      // Отрендерить и вернуть ответ клиенту
      val html = forAdTpl(rargs)(ctx)
      Ok(html)
    }
  }


  /**
    * Модератор решил утвердить все оплаченные модерации сразу.
    *
    * @param nodeId id узла-карточки.
    * @return Редирект на модерацию следующей карточки.
    */
  def approveAllItemsSubmit(nodeId: String) = IsSuperuserMadPost(nodeId).async { implicit request =>
    _processItemsForAd(
      nodeId  = nodeId,
      q       = _itemsQuery(nodeId)
    )(bill2Util.approveItemAction)
  }


  /** Логика поштучной обработки item'ов. */
  private def _processOneItem[Res_t <: IMItem](dbAction: DBIOAction[Res_t, NoStream, RW]): Future[Res_t] = {
    // Запуск обновления MItems.
    val saveFut = slick.db.run {
      dbAction.transactionally
    }

    // Обрадовать другие компоненты системы новым событием
    saveFut.onSuccess { case res =>
      sn.publish( ItemStatusChanged(res.mitem) )
    }

    saveFut
  }

  /** Обработка пачек item'ов унифицирована. */
  private def _processItemsForAd[Res_t <: IMItem](nodeId: String, q: Query[mItems.MItemsTable, MItem, Seq])
                                                 (f: Gid_t => DBIOAction[Res_t, NoStream, RW]): Future[Result] = {
    // TODO Opt Тут можно db.stream применять
    val itemIdsFut = slick.db.run {
      _itemsQuery(nodeId)
        .map(_.id)
        .result
    }

    lazy val logPrefix = s"_processItemsForAd($nodeId ${System.currentTimeMillis}):"
    LOGGER.trace(s"$logPrefix Bulk approve items, $f")

    for {
      itemIds     <- itemIdsFut
      saveFut     = Future.traverse(itemIds) { itemId =>
        _processOneItem( f(itemId) )
          // Следует мягко разруливать ситуации, когда несколько модераторов одновременно аппрувят item'ы одновременно.
          .map { _ => true }
          .recover {
            // Вероятно, race conditions двух модераторов.
            case _: NoSuchElementException =>
              LOGGER.warn(s"$logPrefix Possibly conficting mdr MItem UPDATE. Suppressed.")
              false
            case ex: Throwable =>
              LOGGER.error(s"$logPrefix Unknown error occured while approving item $itemId", ex)
              true
          }
      }
      itemsCount  = itemIds.size
      saveRes     <- saveFut

    } yield {
      val countOk = saveRes.count(identity)
      val countFail = saveRes.count(!_)

      if (countFail == 0) {
        LOGGER.debug(s"$logPrefix Success with $itemsCount items")
        val rdrArgs = MdrSearchArgs(hideAdIdOpt = Some(nodeId))
        Redirect( routes.SysMdr.rdrToNextAd(rdrArgs) )
          .flashing(FLASH.SUCCESS -> s"Одобрено ${itemIds.size} размещений.")
      } else {
        LOGGER.warn(s"$logPrefix Done with errors, $countOk ok with $countFail failed items.")
        Redirect( routes.SysMdr.forAd(nodeId) )
          .flashing(FLASH.ERROR -> s"$countOk размещений подтверждено, $countFail с ошибками.")
      }
    }
  }

  /** Модератор подтверждает оплаченный item. */
  def approveItemSubmit(itemId: Gid_t) = IsSuperuserPost.async { implicit request =>
    val dbAction = bill2Util.approveItemAction(itemId)
    for {
      res <- _processOneItem(dbAction)
    } yield {
      // Отредиректить юзера для продолжения модерации
      Redirect( routes.SysMdr.forAd(res.mitem.adId) )
        .flashing(FLASH.SUCCESS -> s"Размещение #$itemId одобрено.")
    }
  }

  private def _refusePopup(call: Call, form: RefuseForm_t = refuseFormM)
                          (implicit request: IReq[_]): Future[Result] = {
    val args = MSysMdrRefusePopupTplArgs(
      refuseFormM = form,
      submitCall  = call
    )
    val render = _refusePopupTpl(args)
    Ok(render)
  }

  /** Запрос попапам с формой отказа в размещение item'а. */
  def refuseItemPopup(itemId: Gid_t) = IsSuItemAdGet(itemId).async { implicit request =>
    _refusePopup(routes.SysMdr.refuseItemSubmit(itemId))
  }


  /** Модератор отвергает оплаченный item с указанием причины отказа. */
  def refuseItemSubmit(itemId: Gid_t) = IsSuItemPost(itemId).async { implicit request =>
    lazy val logPrefix = s"refuseItem($itemId):"
    refuseFormM.bindFromRequest().fold(
      {formWithErrors =>
        LOGGER.debug(s"$logPrefix Failed to bind form:\n ${formatFormErrors(formWithErrors)}")
        Redirect( routes.SysMdr.forAd(request.mitem.adId) )
          .flashing(FLASH.ERROR -> "Ошибка в запросе отказа, проверьте причину.")
      },

      {reason =>
        // Запустить транзакцию отката оплаченного размещения в кошелек юзера.
        val dbAction  = bill2Util.refuseItemAction(itemId, Some(reason))
        val saveFut   = _processOneItem(dbAction)

        // Когда всё будет выполнено, надо отредиректить юзера на карточку.
        for {
          res <- saveFut
        } yield {
          // Отредиректить клиента на модерацию карточки.
          Redirect( routes.SysMdr.forAd(request.mitem.adId) )
            .flashing(FLASH.SUCCESS -> s"Отказано в размещении #$itemId.")
        }
      }
    )
  }

  /**
    * Рендер попапа для указания причины отсева множества item'ов для указанной карточки.
    *
    * @param nodeId id узла-карточки, которая модерируется в данный момент.
    * @return HTML попапа с формой отказа в размещении.
    */
  def refuseAllItems(nodeId: String) = IsSuperuserMadGet(nodeId).async { implicit request =>
    _refusePopup( routes.SysMdr.refuseAllItemsSubmit(nodeId) )
  }

  /**
    * Множенственный refuse всех модерируемых item'ов, относящихся к указанной карточке.
    *
    * @param nodeId id узла.
    * @return
    */
  def refuseAllItemsSubmit(nodeId: String) = IsSuperuserMadPost(nodeId).async { implicit request =>
    lazy val logPrefix = s"refuseAllItemsSubmit($nodeId):"
    refuseFormM.bindFromRequest().fold(
      {formWithErrors =>
        LOGGER.debug(s"$logPrefix Failed to bind form:\n ${formatFormErrors(formWithErrors)}")
        Redirect( routes.SysMdr.forAd(nodeId) )
          .flashing(FLASH.ERROR -> "Ошибка в запросе отказа, проверьте причину.")
      },

      {reason =>
        val someReason = Some(reason)
        // Запустить транзакцию отката оплаченного размещения в кошелек юзера.
        _processItemsForAd(
          nodeId = nodeId,
          q      = _itemsQuery(nodeId)
        )(bill2Util.refuseItemAction(_, someReason))
      }
    )
  }


  /**
    * Массовый аппрув item'ов, относящихся к какой-то группе item'ов.
    *
    * @param nodeId id узла-карточки.
    * @param itype id типа item'ов.
    * @return Редирект на текущую карточку.
    */
  def approveAllItemsTypeSubmit(nodeId: String, itype: MItemType) = IsSuperuserMadPost(nodeId).async { implicit request =>
    _processItemsForAd(
      nodeId = nodeId,
      q = _itemsQuery(nodeId)
        .filter(_.iTypeStr === itype.strId)
    )(bill2Util.approveItemAction)
  }

  def refuseAllItemsType(nodeId: String, itype: MItemType) = IsSuperuserMadGet(nodeId).async { implicit request =>
    _refusePopup( routes.SysMdr.refuseAllItemsTypeSubmit(nodeId, itype) )
  }

  def refuseAllItemsTypeSubmit(nodeId: String, itype: MItemType) = IsSuperuserMadPost(nodeId).async { implicit request =>
    lazy val logPrefix = s"refuseAllItemsTypeSubmit($nodeId, $itype):"
    refuseFormM.bindFromRequest().fold(
      {formWithErrors =>
        LOGGER.debug(s"$logPrefix Failed to bind form:\n ${formatFormErrors(formWithErrors)}")
        Redirect( routes.SysMdr.forAd(nodeId) )
          .flashing(FLASH.ERROR -> "Ошибка в запросе отказа, проверьте причину.")
      },
      {reason =>
        val someReason = Some(reason)
        _processItemsForAd(
          nodeId = nodeId,
          q = _itemsQuery(nodeId)
            .filter(_.iTypeStr === itype.strId)
        )(bill2Util.refuseItemAction(_, someReason))
      }
    )
  }

}
