package controllers

import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.google.inject.Inject
import controllers.ctag.NodeTagsEdit
import io.suggest.adv.AdvConstants
import io.suggest.adv.geo._
import io.suggest.adv.geo.MGeoAdvExistPopupResp.pickler
import io.suggest.mbill2.m.item.{MItem, MItems}
import io.suggest.mbill2.m.item.status.MItemStatuses
import io.suggest.mbill2.m.order.{MOrderStatuses, MOrders}
import io.suggest.model.es.MEsUuId
import io.suggest.async.StreamsUtil
import io.suggest.bin.ConvCodecs
import io.suggest.common.empty.OptionUtil
import io.suggest.geo.MGeoPoint
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.item.typ.MItemTypes
import io.suggest.pick.{PickleSrvUtil, PickleUtil}
import models.adv.form.MDatesPeriod
import models.adv.geo.cur.{MAdvGeoBasicInfo, MAdvGeoShapeInfo}
import models.adv.geo.tag.{AgtForm_t, MAgtFormResult, MForAdTplArgs}
import models.adv.price.GetPriceResp
import models.jsm.init.MTargets
import models.mctx.Context
import models.mdt.MDateInterval
import models.merr.MError
import models.mproj.ICommonDi
import models.req.IAdProdReq
import org.joda.time.LocalDate
import play.api.i18n.Messages
import play.api.libs.json.Json
import play.api.mvc.Result
import util.PlayMacroLogsImpl
import util.acl.{CanAccessItem, CanAdvertiseAd, CanAdvertiseAdUtil, CanThinkAboutAdvOnMapAdnNode}
import util.adv.AdvFormUtil
import util.adv.geo.{AdvGeoBillUtil, AdvGeoFormUtil, AdvGeoLocUtil, AdvGeoMapUtil}
import util.billing.Bill2Util
import util.lk.LkTagsSearchUtil
import util.tags.TagsEditFormUtil
import util.DateTimePrettyPrinter.{dayOfWeek, formatDateFull}
import views.html.lk.adv.geo._
import views.html.lk.adv.widgets.period._reportTpl
import views.html.lk.lkwdgts.price._priceValTpl

import scala.concurrent.Future
import scala.concurrent.duration._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.11.15 14:51
  * Description: Контроллер размещения в гео-тегах.
  */
class LkAdvGeo @Inject() (
  advGeoFormUtil                  : AdvGeoFormUtil,
  advGeoBillUtil                  : AdvGeoBillUtil,
  advFormUtil                     : AdvFormUtil,
  bill2Util                       : Bill2Util,
  advGeoLocUtil                   : AdvGeoLocUtil,
  advGeoMapUtil                   : AdvGeoMapUtil,
  streamsUtil                     : StreamsUtil,
  pickleSrvUtil                   : PickleSrvUtil,
  override val mItems             : MItems,
  override val mOrders            : MOrders,
  override val tagSearchUtil      : LkTagsSearchUtil,
  override val tagsEditFormUtil   : TagsEditFormUtil,
  override val canAdvAdUtil       : CanAdvertiseAdUtil,
  override val mCommonDi          : ICommonDi
)
  extends SioControllerImpl
  with PlayMacroLogsImpl
  with CanAdvertiseAd
  with NodeTagsEdit
  with CanThinkAboutAdvOnMapAdnNode
  with CanAccessItem
{

  import mCommonDi._
  import streamsUtil.Implicits._

  // Сериализация:
  import MRcvrPopupResp.pickler
  import MFormS.pickler
  import pickleSrvUtil.Base64ByteBufEncoder


  /** Асинхронный детектор начальной точки для карты георазмещения. */
  private def getGeoPoint0(adId: String)(implicit request: IAdProdReq[_]): Future[MGeoPoint] = {
    import advGeoLocUtil.Detectors._
    val adIds = adId :: Nil
    // Ищем начальную точку среди прошлых размещений текущей карточки.

    FromAdsGeoAdvs(adIds)
      // Если не получилось, то ищем начальную точку среди размещений продьюсера карточки.
      .orElse {
        FromProducerGeoAdvs(
          producerIds  = Seq(request.producer.id, request.user.personIdOpt).flatten,
          excludeAdIds = adIds
        )
      }
      .orFromReqOrDflt
      .get
  }


  /**
    * Экшен рендера страницы размещения карточки в теге с географией.
    *
    * @param adId id отрабатываемой карточки.
    */
  def forAd(adId: String) = CanAdvertiseAdGet(adId, U.Lk, U.ContractId).async { implicit request =>
    // TODO Попытаться заполнить форму с помощью данных из черновиков, если они есть.
    //val draftItemsFut = advGeoBillUtil.findDraftsForAd(adId)
    val resLogic = new ForAdLogic(
      _isSuFree = request.user.isSuper
    )

    val formEmpty = advGeoFormUtil.formStrict

    val formFut = for (gp0 <- resLogic._geoPointFut) yield {

      val radMapVal = advFormUtil.radMapValue0(gp0)

      // Залить начальные данные в маппинг формы.
      val res = MAgtFormResult(
        tags          = Nil,
        radMapVal     = radMapVal,
        period        = MDatesPeriod(),
        onMainScreen  = true,
        rcvrs         = Nil   // По идее, тут изначально всё должно быть пусто.
      )

      formEmpty.fill(res)
    }

    resLogic.result(formFut, Ok)
  }


  /**
   * common-код экшенов GET'а и POST'а формы forAdTpl.
   *
   * @return Фьючерс с ответом.
   */
  private class ForAdLogic( _isSuFree: Boolean )(implicit request: IAdProdReq[_]) {

    val _geoPointFut: Future[MGeoPoint] = {
      getGeoPoint0(request.mad.id.get)
    }

    val _ctxFut = for (ctxData0 <- request.user.lkCtxDataFut) yield {
      implicit val ctxData = ctxData0.copy(
        jsiTgs = Seq(MTargets.AdvGtagForm)
      )
      implicitly[Context]
    }


    /** Рендер ответа.
      *
      * @param formFut Маппинг формы.
      * @param rs Статус ответа HTTP.
      */
    def result(formFut: Future[AgtForm_t], rs: Status): Future[Result] = {
      //lazy val logPrefix = s"_forAd(${request.mad.idOrNull} ${System.currentTimeMillis}):"

      val advPricingFut = formFut.flatMap { form =>
        advGeoBillUtil.getPricing(form.value, _isSuFree)
      }

      // Отрендерить текущие радиусные размещения в форму MRoot.
      val formStateSerFut: Future[String] = for {
        ctx           <- _ctxFut
        gp0           <- _geoPointFut
        // TODO Отрендерить в состояние текущих георазмещения в радиусах. currAdvsJson  <- currAdvsJsonFut
      } yield {
        // Собираем исходную root-модель формы.
        val mform = MFormS(
          adId = request.mad.id.get,
          mapState = MMapS(
            center = gp0,
            zoom   = 10
          ),
          adv4free = OptionUtil.maybe( request.user.isSuper ) {
            MAdv4FreeS(
              static = MAdv4FreeProps(
                fn    = AdvConstants.Su.ADV_FOR_FREE_FN,
                title = ctx.messages( "Adv.for.free.without.moderation" )
              ),
              checked = _isSuFree
            )
          }
        )
        // Сериализуем модель через boopickle + base64 для рендера бинаря прямо в HTML.
        PickleUtil.pickleConv[MFormS, ConvCodecs.Base64, String](mform)
      }

      // Собираем итоговый ответ на запрос: аргументы рендера, рендер html, рендер http-ответа.
      for {
        ctx           <- _ctxFut
        advPricing    <- advPricingFut
        form          <- formFut
        formStateSer  <- formStateSerFut
      } yield {

        val rargs = MForAdTplArgs(
          mad           = request.mad,
          producer      = request.producer,
          form          = form,
          price         = advPricing,
          formState     = formStateSer
        )

        val html = AdvGeoForAdTpl(rargs)(ctx)
        rs(html)
      }
    }

  }



  /**
   * Экшен сабмита формы размещения карточки в теге с географией.
   *
   * @param adId id размещаемой карточки.
   * @return 302 see other, 416 not acceptable.
   */
  def forAdSubmit(adId: String) = CanAdvertiseAdPost(adId, U.PersonNode).async { implicit request =>
    lazy val logPrefix = s"forAdSubmit($adId):"

    advGeoFormUtil.formStrict.bindFromRequest().fold(
      {formWithErrors =>
        // Запустить инициализацию U.Lk в фоне:
        request.user.lkCtxDataFut
        // Логгировать сразу, чтобы сообщения в логах не перемешивались.
        LOGGER.debug(s"$logPrefix Failed to bind form:\n ${formatFormErrors(formWithErrors)}")
        // Повторный биндинг с NoStrict-формой -- костыль для решения проблем с рендером локализованных
        // дат размещения. Даты форматируются через form.value.dates, а не через form("dates").value, поэтому
        // при ошибке биндинга возникает ситуация, когда form.value не определена, и появляется дырка на странице.
        val formWithErrors1 = advGeoFormUtil.formTolerant
          .bindFromRequest()
          .copy(
            errors = formWithErrors.errors
          )
        val fweFut = Future.successful(formWithErrors1)
        // Рендер результата.
        new ForAdLogic(
          _isSuFree = advFormUtil.maybeFreeAdv()
        )
          .result(fweFut, NotAcceptable)
      },

      {result =>
        LOGGER.trace(s"$logPrefix Binded: " + result)

        val isSuFree = advFormUtil.maybeFreeAdv()
        val status   = advFormUtil.suFree2newItemStatus(isSuFree)

        // Найти корзину юзера и добавить туда покупки.
        for {
          // Прочитать узел юзера. Нужно для чтения/инциализации к контракта
          personNode0 <- request.user.personNodeFut

          // Узнать контракт юзера
          e           <- bill2Util.ensureNodeContract(personNode0, request.user.mContractOptFut)

          producerId  = request.producer.id.get

          // Произвести добавление товаров в корзину.
          itemsAdded <- {
            // Надо определиться, правильно ли инициализацию корзины запихивать внутрь транзакции?
            val dbAction = for {
              // Найти/создать корзину
              cart    <- bill2Util.ensureCart(
                contractId = e.mc.id.get,
                status0    = MOrderStatuses.cartStatusForAdvSuperUser(isSuFree)
              )

              // Закинуть заказ в корзину юзера. Там же и рассчет цены будет.
              addRes  <- advGeoBillUtil.addToOrder(
                orderId     = cart.id.get,
                producerId  = producerId,
                adId        = adId,
                res         = result,
                status      = status
              )
            } yield {
              addRes
            }
            // Запустить экшен добавления в корзину на исполнение.
            import slick.driver.api._
            slick.db.run( dbAction.transactionally )
          }

        } yield {
          val rCall = routes.LkAdvGeo.forAd(adId)
          if (!isSuFree) {
            implicit val messages = implicitly[Messages]
            // Пора вернуть результат работы юзеру: отредиректить в корзину-заказ для оплаты.
            Redirect(routes.LkBill2.cart(producerId, r = Some(rCall.url)))
              .flashing(FLASH.SUCCESS -> messages("Added.n.items.to.cart", itemsAdded.size))

          } else {
            // Суперюзеры отправляются назад в эту же форму для дальнейшего размещения.
            Redirect( rCall )
              .flashing(FLASH.SUCCESS -> "Ads.were.adv")
          }
        }
      }
    )
  }


  /**
    * Экшн рассчета стоимости текущего размещения.
    *
    * @param adId id рекламной карточки.
    * @return 200 / 416 + JSON
    */
  def getPriceSubmit(adId: String) = CanAdvertiseAdPost(adId).async { implicit request =>
    lazy val logPrefix = s"getPriceSubmit($adId):"
    advGeoFormUtil.formTolerant.bindFromRequest().fold(
      {formWithErrors =>
        LOGGER.debug(s"$logPrefix Failed to bind form:\n${formatFormErrors(formWithErrors)}")
        val err = MError(
          msg = Some("Failed to bind form")
        )
        NotAcceptable( Json.toJson(err) )
      },

      {result =>
        val isSuFree = advFormUtil.maybeFreeAdv()
        val advPricingFut = advGeoBillUtil.getPricing(result, isSuFree)

        implicit val ctx = implicitly[Context]

        // Запустить рендер ценника размещаемого контента
        val pricingHtmlFut = for {
          advPricing <- advPricingFut
        } yield {
          LOGGER.trace(s"$logPrefix pricing => $advPricing, isSuFree = $isSuFree")
          val html = _priceValTpl(advPricing)(ctx)
          htmlCompressUtil.html2str4json(html)
        }

        // Отрендерить данные по периоду размещения
        val periodReportHtml = htmlCompressUtil.html2str4json {
          _reportTpl(result.period)(ctx)
        }

        for {
          pricingHtml <- pricingHtmlFut
        } yield {
          val resp = GetPriceResp(
            periodReportHtml  = periodReportHtml,
            priceHtml         = pricingHtml
          )
          Ok( Json.toJson(resp) )
        }
      }
    )
  }


  /**
    * Получение списка маркеров-точек узлов-ресиверов для карты.
    * @param adId id текущей размещаемой рекламной карточки.
    *             Пока используется как основание для проверки прав доступа.
    */
  def advRcvrsMap(adId: MEsUuId) = CanAdvertiseAdPost(adId).async { implicit request =>
    val nodesSrc = cache.getOrElse("advGeoNodesSrc", expiration = 10.seconds) {
      advGeoMapUtil.rcvrNodesMap()
    }
    // Сериализовать поток данных в JSON:
    val jsonStrSrc = streamsUtil.jsonSrcToJsonArrayNullEnded(
      nodesSrc.map { m =>
        Json.toJson( m.toGeoJson )
      }
    )
    // Вернуть chunked-ответ с потоком из JSON внутрях.
    Ok.chunked(jsonStrSrc)
      .as( withCharset(JSON) )
      .withHeaders(CACHE_10)
  }


  /** Текущие георазмещения карточки, т.е. размещения на карте в кружках.
    *
    * @param adId id интересующей рекламной карточки.
    * @return js.Array[GjFeature].
    */
  def existGeoAdvsMap(adId: String) = CanAdvertiseAdPost(adId).async { implicit request =>
    // Собрать данные о текущих гео-размещениях карточки, чтобы их отобразить юзеру на карте.
    val currAdvsSrc = slick.db
      .stream {
        val query = advGeoBillUtil.findCurrentForAdQ(request.mad.id.get)
        advGeoBillUtil.onlyGeoShapesInfo(query)
      }
      .toSource
      // Причесать сырой выхлоп базы, состоящий из пачки Option'ов.
      .mapConcat( MAdvGeoShapeInfo.applyOpt(_).toList )
      // Каждый элемент нужно скомпилить в пригодную для сериализации модель.
      .map { si =>
        // Сконвертить в GeoJSON и сериализовать в промежуточное JSON-представление.
        val gj = advGeoFormUtil.shapeInfo2geoJson(si)
        Json.toJson(gj)
      }

    // Превратить поток JSON-значений в "поточную строку", направленную в сторону юзера.
    val jsonStrSrc = streamsUtil.jsonSrcToJsonArrayNullEnded(currAdvsSrc)

    Ok.chunked(jsonStrSrc)
      .as( withCharset(JSON) )
      .withHeaders(CACHE_10)
  }

  /**
    * Экшен получения данных для рендера попапа по размещениям.
    * @param itemId id по таблице mitem.
    * @return Бинарный выхлоп с данными для react-рендера попапа.
    */
  def existGeoAdvsShapePopup(itemId: Gid_t) = CanAccessItemPost(itemId, edit = false).async { implicit request =>
    // Доп. проверка прав доступа: вдруг юзер захочет пропихнуть тут какой-то левый (но свой) item.
    // TODO Вынести суть ассерта на уровень отдельного ActionBuilder'а, проверяющего права доступа по аналогии с CanAccessItemPost.
    if (request.mitem.iType != MItemTypes.GeoPlace)
      throw new IllegalArgumentException(s"MItem[itemId] type is ${request.mitem.iType}, but here we need GeoPlace ${MItemTypes.GeoPlace}")

    // Наврядли можно отрендерить в попапе даже это количество...
    val itemsMax = 50

    // Запросить у базы инфы по размещениям в текущем месте...
    val itemsSrc = slick.db
      .stream {
        advGeoBillUtil.withSameGeoShapeAs(
          query   = advGeoBillUtil.findCurrentForAdQ( request.mitem.nodeId ),
          itemId  = itemId,
          limit   = itemsMax
        )
      }
      .toSource

    val rowsMsFut = itemsSrc
      // Причесать кортежи в нормальные инстансы
      .map( MAdvGeoBasicInfo.apply )
      // Сгруппировать и объеденить по периодам размещения.
      .groupBy(itemsMax, _.intervalOpt)
      .fold( List.empty[MAdvGeoBasicInfo] ) { (acc, e) => e :: acc }
      .map { infos =>
        // Нужно отсортировать item'ы по алфавиту или id, завернув их в итоге в Row
        val ivlOpt = infos.head.intervalOpt.map(MDateInterval.apply)
        val row = MGeoAdvExistRow(
          dateRange = __formatDtInterval( ivlOpt ),
          items = infos
            .sortBy(m => (m.tagFaceOpt, m.id) )
            .map { m =>
              MGeoItemInfo(
                itemId        = m.id,
                isOnlineNow   = m.status == MItemStatuses.Online,
                payload       = m.iType match {
                  case MItemTypes.GeoTag    => InGeoTag( m.tagFaceOpt.get )
                  case MItemTypes.GeoPlace  => OnMainScreen
                }
              )
            }
        )
        val startMs = ivlOpt.map(_.dtStart.getMillis)
        startMs -> row
      }
      // Вернуться на уровень основного потока...
      .mergeSubstreams
      // Собрать все имеющиеся результаты в единую коллекцию.
      .runFold( List.empty[(Option[Long], MGeoAdvExistRow)] ) { (acc, row) => row :: acc }

    // Параллельно считаем общее кол-во найденных item'ов, чтобы сравнить их с лимитом.
    val itemsCountFut = itemsSrc.runFold(0) { (counter, e) => counter + 1 }

    // Сборка непоточного бинарного ответа.
    for {
      rowsMs      <- rowsMsFut
      itemsCount  <- itemsCountFut
    } yield {
      // Отсортировать ряды по датам, собрать итоговую модель ответа...
      val mresp = MGeoAdvExistPopupResp(
        rows = rowsMs
          .sortBy(_._1)
          .map(_._2),
        haveMore = itemsCount >= itemsMax
      )
      // Сериализовать и вернуть результат:
      val pickled = PickleUtil.pickle(mresp)
      Ok( ByteString(pickled) )
        .withHeaders(CACHE_10)
    }
  }


  /** Рендер попапа при клике по узлу-ресиверу на карте ресиверов.
    *
    * @param adIdU id текущей карточки, которую размещают.
    * @param rcvrNodeIdU id узла, по которому кликнул юзер.
    * @return Бинарь для boopickle на стороне JS.
    */
  def rcvrMapPopup(adIdU: MEsUuId, rcvrNodeIdU: MEsUuId) = _rcvrMapPopup(adId = adIdU, rcvrNodeId = rcvrNodeIdU)
  private def _rcvrMapPopup(adId: String, rcvrNodeId: String) = CanThinkAboutAdvOnMapAdnNode(adId, nodeId = rcvrNodeId).async { implicit request =>

    // Запросить по биллингу карту подузлов для запрашиваемого ресивера.
    val subNodesIdsFut = advGeoBillUtil.findActiveSubNodeIdsOfRcvr(rcvrNodeId)

    lazy val logPrefix = s"geoNodePopup($adId,$rcvrNodeId)[${System.currentTimeMillis}]:"

    // Закинуть во множество подузлов id текущего ресивера.
    val allNodesIdsFut = for (subNodesIds <- subNodesIdsFut) yield {
      LOGGER.trace(s"$logPrefix Found ${subNodesIds.size} sub-nodes: ${subNodesIds.mkString(", ")}")
      subNodesIds + rcvrNodeId
    }

    // Нужно получить все узлы из кэша.
    val nodesFut = allNodesIdsFut.flatMap { allNodeIds =>
      mNodeCache.multiGet( allNodeIds )
    }

    // Сгруппировать под-узлы по типам узлов, внеся туда ещё и текущий ресивер.
    val nodesGrpsFut = for (subNodes <- nodesFut) yield {
      subNodes
        // Сгруппировать узлы по их типам. Для текущего узла тип будет None. Тогда он отрендерится без заголовка и в самом начале списка узлов.
        .groupBy { mnode =>
          if (mnode.id.contains(rcvrNodeId)) {
            None
          } else {
            Some(mnode.common.ntype)
          }
        }
        .toSeq
        // Очень кривая сортировка, но для наших нужд и такой пока достаточно.
        .sortBy( _._1.map(_.id) )
    }

    // Запустить получение списка всех текущих (черновики + busy) размещений на узле и его маячках из биллинга.
    val currAdvsSrc = {
      val pubFut = for {
        allNodeIds <- allNodesIdsFut
      } yield {
        slick.db.stream {
          advGeoBillUtil.findCurrForAdToRcvrs(
            adId      = adId,
            // TODO Добавить поддержку групп маячков ресиверов.
            rcvrIds   = allNodeIds,
            // Интересуют и черновики, и текущие размещения
            statuses  = MItemStatuses.advActual,
            // TODO Надо бы тут порешить, какой limit требуется на деле и требуется ли вообще. 20 взято с потолка.
            limitOpt  = Some(300)
          )
        }
      }
      // Выпрямляем Future[Publisher] в просто нормальный Source.
      pubFut.toSource
    }

    def __src2RcvrIdsSet(src: Source[MItem,_]): Future[Set[String]] = {
      // В текущих размещениях интересуют значения в поле rcvrId...
      src
        .mapConcat( _.rcvrIdOpt.toList )
        // Перегнать все id в Future[SetBuilder[String]]
        .toSetFut
    }

    // Запустить сбор rcvr node id'шников в потоке для текущих размещений.
    val advRcvrIdsActualFut = __src2RcvrIdsSet(currAdvsSrc)
    val advRcvrIdsBusyFut   = __src2RcvrIdsSet {
      currAdvsSrc.filter { i =>
        i.status.isAdvBusy
      }
    }

    // Собрать множество id узлов, у которых есть хотя бы одно online-размещение.
    val nodesHasOnlineFut = __src2RcvrIdsSet {
      currAdvsSrc
        .filter { _.status == MItemStatuses.Online }
    }

    // Карта интервалов размещения по id узлов.
    val intervalsMapFut: Future[Map[String, MDateInterval]] = currAdvsSrc
      .runFold( Map.newBuilder[String, MDateInterval] ) { (acc, i) =>
        for {
          dtInterval <- i.dtIntervalOpt
          rcvrId     <- i.rcvrIdOpt
        } {
          acc += rcvrId -> MDateInterval(dtInterval)
        }
        acc
      }
      .map( _.result() )

    implicit val ctx = implicitly[Context]

    // Сборка JSON-модели для рендера JSON-ответа, пригодного для рендера с помощью react.js.
    for {
      nodesHasOnline      <- nodesHasOnlineFut
      intervalsMap        <- intervalsMapFut
      advRcvrIdsActual    <- advRcvrIdsActualFut
      advRcvrIdsBusy      <- advRcvrIdsBusyFut
      nodesGrps           <- nodesGrpsFut
    } yield {

      val resp = MRcvrPopupResp(
        groups = for (g <- nodesGrps) yield {
          MRcvrPopupGroup(
            groupId = g._1.map(_.strId),
            nameOpt = for (ntype <- g._1) yield ctx.messages(ntype.plural),
            nodes   = for (n <- g._2; nodeId <- n.id) yield {
              MRcvrPopupNode(
                nodeId      = nodeId,
                isCreate    = !advRcvrIdsBusy.contains( nodeId ),
                checked     = advRcvrIdsActual.contains( nodeId ),
                nameOpt     = n.guessDisplayNameOrId,
                isOnlineNow = nodesHasOnline.contains( nodeId ),
                dateRange   = __formatDtInterval( intervalsMap.get( nodeId ) )(ctx)
              )
            }
          )
        }
      )

      Ok( ByteString( PickleUtil.pickle(resp) ) )
        // Чисто для подавления двойных запросов. Ведь в теле запроса могут быть данные формы, которые варьируются.
        .withHeaders(CACHE_10)
    }
  }

  /** Хидер короткого кеша, в основном для защиты от повторяющихся запросов. */
  private def CACHE_10 = CACHE_CONTROL -> "private, max-age=10"

  /** Рендер MDateInterval в MDateFormatted. */
  private def __formatDtInterval(ivlOpt: Option[MDateInterval])(implicit ctx: Context): Seq[MDateFormatted] = {
    ivlOpt.fold [List[MDateFormatted]] (Nil) { ivl =>
      def __formatDate(dt: LocalDate) = MDateFormatted(
        dow  = dayOfWeek(dt)(ctx),
        date = formatDateFull(dt)(ctx)
      )
      __formatDate(ivl.dateStart) :: __formatDate(ivl.dateEnd) :: Nil
    }
  }

}
