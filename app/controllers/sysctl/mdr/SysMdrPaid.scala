package controllers.sysctl.mdr

import controllers.routes
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.item.ItemStatusChanged
import io.suggest.mbill2.m.item.status.MItemStatuses
import io.suggest.mbill2.m.item.typ.MItemTypes
import models._
import models.mctx.Context
import models.mdr._
import util.acl.{IsSuItem, IsSuItemAd, IsSuperuser, IsSuperuserMad}
import util.billing.IBill2UtilDi
import util.di.IScUtil
import views.html.sys1.mdr._


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
  import dbConfig.driver.api._

  /**
    * Вывод страницы со списком карточек для модерации проплаченных размещений.
    *
    * @param args Аргументы поиска модерируемых карточек.
    * @return Страница с плиткой карточек, которые нужно модерировать по-платному направлению.
    */
  def paidAdvs(args: MdrSearchArgs) = IsSuperuser.async { implicit request =>
    // Залезть в items, найти там размещения, ожидающие подтверждения.
    val dbAction = {
      mItems.query
        .filter { i =>
          (i.iTypeStr inSet MItemTypes.onlyAdvTypesIds) &&
            (i.statusStr === MItemStatuses.AwaitingSioAuto.strId)
        }
        //.sortBy(_.id.asc)   // TODO Нужно подумать над сортировкой возвращаемого множества adId
        .map(_.adId)
        .distinct
        .take( args.limit )
        .drop( args.offset )
        .result
    }

    // Запустить поиск id карточек по биллингу, а затем и поиск самих карточек.
    val madsFut = dbConfig.db
      .run( dbAction )
      .flatMap { madIds =>
        mNodeCache.multiGet( madIds )
      }

    // Передать управление в _adsPage(), не дожидаясь ничего.
    _adsPage(madsFut, args)
  }



  /**
    * Обработка запроса страницы модерации одной рекламной карточки.
    * Модерация v2 подразумевает также обработку карточек.
    *
    * @param nodeId id карточки.
    * @return Страница
    */
  def forAd(nodeId: String) = IsSuperuserMadGet(nodeId).async { implicit request =>

    // Нужно запустить сборку списков item'ов на модерацию.
    val mitemsFut = dbConfig.db.run {
      mItems.query
        .filter { i =>
          (i.adId === nodeId) &&
            (i.statusStr === MItemStatuses.AwaitingSioAuto.strId)
        }
        // На всякий случай ограничиваем кол-во запрашиваемых item'ов. Нет никакого смысла вываливать слишком много данных на экран.
        .take(50)
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

      // Дождаться карты узлов
      mnodesMap <- mnodesMapFut
      // Дождаться аргументов рендера.
      brArgs <- brArgsFut

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
          }
      )

      // Отрендерить и вернуть ответ клиенту
      val html = forAdTpl(rargs)(ctx)
      Ok(html)
    }
  }


  /** Модератор подтверждает оплаченный item. */
  def approveItemSubmit(itemId: Gid_t) = IsSuperuserPost.async { implicit request =>
    // Запуск обновления MItems.
    val saveFut = dbConfig.db.run {
      bill2Util.approveItemAction(itemId)
        .transactionally
    }

    for {
      res <- saveFut
    } yield {
      // Обрадовать другие компоненты системы новым событием
      sn.publish( ItemStatusChanged(res.mitem) )

      // Отредиректить юзера для продолжения модерации
      Redirect( routes.SysMdr.forAd(res.mitem.adId) )
        .flashing(FLASH.SUCCESS -> s"Размещение #$itemId одобрено.")
    }
  }


  /** Запрос попапам с формой отказа в размещение item'а. */
  def refuseItemPopup(itemId: Gid_t) = IsSuItemAdGet(itemId).async { implicit request =>
    val args = MSysMdrRefusePopupTplArgs(
      refuseFormM    = refuseFormM,
      submitCall  = routes.SysMdr.refuseItemSubmit(itemId)
    )
    val render = _refusePopupTpl(args)
    Ok(render)
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
        val saveFut = dbConfig.db.run {
          bill2Util.refuseItemAction(itemId, Some(reason))
            .transactionally
        }

        // Когда всё будет выполнено, надо отредиректить юзера на карточку.
        for {
          res <- saveFut
        } yield {
          // Уведомить всех об изменении в статусе item'а
          sn.publish( ItemStatusChanged(res.mitem) )

          // Отредиректить клиента на модерацию карточки.
          Redirect( routes.SysMdr.forAd(request.mitem.adId) )
            .flashing(FLASH.SUCCESS -> s"Отказано в размещении #$itemId.")
        }
      }
    )
  }


}
