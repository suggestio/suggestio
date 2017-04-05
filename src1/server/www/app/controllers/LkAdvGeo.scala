package controllers

import java.time.{LocalDate, OffsetDateTime}

import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.google.inject.Inject
import controllers.ctag.NodeTagsEdit
import io.suggest.adv.AdvConstants
import io.suggest.adv.free.MAdv4FreeProps
import io.suggest.adv.geo._
import io.suggest.adv.rcvr._
import io.suggest.mbill2.m.item.MItem
import io.suggest.mbill2.m.item.status.MItemStatuses
import io.suggest.mbill2.m.order.MOrderStatuses
import io.suggest.async.StreamsUtil
import io.suggest.bill.MGetPriceResp
import io.suggest.bin.ConvCodecs
import io.suggest.common.empty.OptionUtil
import io.suggest.common.tags.edit.MTagsEditProps
import io.suggest.dt.interval.MRangeYmdOpt
import io.suggest.dt.MAdvPeriod
import io.suggest.geo.{MGeoCircle, MGeoPoint}
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.item.typ.MItemTypes
import io.suggest.pick.{PickleSrvUtil, PickleUtil}
import models.adv.geo.cur.{MAdvGeoBasicInfo, MAdvGeoShapeInfo}
import models.adv.geo.tag.MForAdTplArgs
import models.mctx.Context
import models.mproj.ICommonDi
import models.req.IAdProdReq
import play.api.libs.json.Json
import play.api.mvc.Result
import util.acl._
import util.adv.AdvFormUtil
import util.adv.geo.{AdvGeoBillUtil, AdvGeoFormUtil, AdvGeoLocUtil, AdvGeoMapUtil}
import util.billing.Bill2Util
import util.lk.LkTagsSearchUtil
import util.tags.TagsEditFormUtil
import views.html.lk.adv.geo._
import io.suggest.dt.YmdHelpersJvm
import io.suggest.es.model.MEsUuId
import io.suggest.init.routed.MJsiTgs
import io.suggest.primo.id.OptId
import io.suggest.util.logs.MacroLogsImpl
import io.suggest.www.util.req.ReqUtil
import models.MNode
import util.mdr.MdrUtil

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
                           ymdHelpersJvm                   : YmdHelpersJvm,
                           reqUtil                         : ReqUtil,
                           mdrUtil                         : MdrUtil,
                           canAccessItem                   : CanAccessItem,
                           canThinkAboutAdvOnMapAdnNode    : CanThinkAboutAdvOnMapAdnNode,
                           canAdvAd                        : CanAdvAd,
                           override val isAuth             : IsAuth,
                           override val tagSearchUtil      : LkTagsSearchUtil,
                           override val tagsEditFormUtil   : TagsEditFormUtil,
                           override val mCommonDi          : ICommonDi
)
  extends SioControllerImpl
  with MacroLogsImpl
  with NodeTagsEdit
{

  import mCommonDi._
  import streamsUtil.Implicits._
  import ymdHelpersJvm.Implicits._

  // Сериализация:
  import pickleSrvUtil.Base64ByteBufEncoder

  /** Макс.кол-во item'ов ресиверов, возвращаемых в одном rcvr-попапе. */
  private def RCVR_ITEMS_PER_POPUP_LIMIT = 50


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
  def forAd(adId: String) = csrf.AddToken {
    canAdvAd(adId, U.Lk, U.ContractId).async { implicit request =>
      // TODO Попытаться заполнить форму с помощью данных из черновиков, если они есть.
      //val draftItemsFut = advGeoBillUtil.findDraftsForAd(adId)
      val resLogic = new ForAdLogic(
        _isSuFree = request.user.isSuper
      )

      val formFut = for {
        gp0         <- resLogic._geoPointFut
        a4fPropsOpt <- resLogic._a4fPropsOptFut
      } yield {

        // TODO Распилить это на MMapProps и MGeoCircle.
        val radMapVal = advFormUtil.radMapValue0(gp0)

        // Залить начальные данные в маппинг формы.
        MFormS(
          mapProps = MMapProps(
            center = radMapVal.state.center,
            zoom   = radMapVal.state.zoom
          ),
          // TODO Найти текущее размещение в draft items (в корзине неоплаченных).
          onMainScreen = true,
          adv4freeChecked = a4fPropsOpt.map(_ => true),
          // TODO Найти текущие ресиверы в draft items (в корзине неоплаченных).
          rcvrsMap = Map.empty,
          // TODO Найти текущие теги в draft items (в корзине неоплаченных).
          tagsEdit = MTagsEditProps(),
          datePeriod = MAdvPeriod(),
          // TODO Найти текущее размещение в draft items (в корзине неоплаченных).
          radCircle = Some(MGeoCircle(
            center  = radMapVal.circle.center,
            radiusM = radMapVal.circle.radius.meters
          )),
          tzOffsetMinutes = MFormS.TZ_OFFSET_IGNORE
        )
      }

      resLogic.result(formFut, Ok)
    }
  }


  private def _getPricing(isSuFree: Boolean, mFormS: MFormS)
                         (implicit request: IAdProdReq[_], ctx: Context): Future[MGetPriceResp] = {
    bill2Util.maybeFreePricing(isSuFree) {
      // Найти все узлы, принадлежащие текущему юзеру:
      for {
        billCtx     <- advGeoBillUtil.advBillCtx(isSuFree, request.mad, mFormS)
        pricing     <- advGeoBillUtil.getPricing( billCtx )(ctx)
      } yield {
        pricing
      }
    }
  }

  /**
   * common-код экшенов GET'а и POST'а формы forAdTpl.
   *
   * @return Фьючерс с ответом.
   */
  // TODO Замёржить в forAd(), если эта дедубликация кода больше не требуется.
  private class ForAdLogic( _isSuFree: Boolean )(implicit request: IAdProdReq[_]) {

    val _geoPointFut: Future[MGeoPoint] = {
      getGeoPoint0(request.mad.id.get)
    }

    val _ctxFut = for (ctxData0 <- request.user.lkCtxDataFut) yield {
      implicit val ctxData = ctxData0.withJsiTgs(
        MJsiTgs.AdvGeoForm :: ctxData0.jsiTgs
      )
      getContext2
    }

    val _a4fPropsOptFut = for (ctx <- _ctxFut) yield {
      OptionUtil.maybe( request.user.isSuper ) {
        MAdv4FreeProps(
          fn    = AdvConstants.Su.ADV_FOR_FREE_FN,
          title = ctx.messages( "Adv.for.free.without.moderation" )
        )
      }
    }

    /** Рендер ответа.
      *
      * @param formFut Маппинг формы.
      * @param rs Статус ответа HTTP.
      */
    def result(formFut: Future[MFormS], rs: Status): Future[Result] = {
      def logPrefix = s"result(${request.mad.idOrNull} $rs):"

      // Считаем в фоне начальный ценник для размещения...
      val advPricingFut = for {
        form    <- formFut
        ctx     <- _ctxFut
        pricing <- _getPricing(_isSuFree, form)(request, ctx)
      } yield {
        LOGGER.trace(s"$logPrefix su=${_isSuFree}  prod=${request.producer.idOrNull}  pricing => $pricing")
        advFormUtil.prepareAdvPricing(pricing)
      }

      // Отрендерить текущие радиусные размещения в форму MRoot.
      val formStateSerFut: Future[String] = for {
        a4fPropsOpt   <- _a4fPropsOptFut
        formS         <- formFut
        advPricing    <- advPricingFut
      } yield {
        // Собираем исходную root-модель формы.
        val mFormInit = MFormInit(
          adId          = request.mad.id.get,
          adv4FreeProps = a4fPropsOpt,
          advPricing    = advPricing,
          form          = formS
        )

        // Сериализуем модель через boopickle + base64 для рендера бинаря прямо в HTML.
        PickleUtil.pickleConv[MFormInit, ConvCodecs.Base64, String](mFormInit)
      }

      // Собираем итоговый ответ на запрос: аргументы рендера, рендер html, рендер http-ответа.
      for {
        ctx           <- _ctxFut
        formStateSer  <- formStateSerFut
      } yield {
        val rargs = MForAdTplArgs(
          mad           = request.mad,
          producer      = request.producer,
          formState     = formStateSer
        )

        val html = AdvGeoForAdTpl(rargs)(ctx)
        rs(html)
      }
    }

  }

  private def _checkFormRcvrs(mFormS0: MFormS): Future[MFormS] = {
    if (mFormS0.rcvrsMap.nonEmpty) {
      val rcvrKeys2Fut = advGeoMapUtil.checkRcvrs(mFormS0.rcvrsMap.keys)
      lazy val logPrefix = s"_checkFormRcvrs(${mFormS0.hashCode()})[${System.currentTimeMillis()}]:"
      for (keys2 <- rcvrKeys2Fut) yield {
        val rcvrsMap2: RcvrsMap_t = keys2
          .iterator
          .map { key2 =>
            key2 -> mFormS0.rcvrsMap(key2)
          }
          .toMap
        LOGGER.trace(s"$logPrefix Rcvrs map updated:\n OLD = ${mFormS0.rcvrsMap}\n NEW = $rcvrsMap2")
        mFormS0.withRcvrsMap(rcvrsMap2)
      }
    } else {
      Future.successful(mFormS0)
    }
  }


  /**
   * Экшен сабмита формы размещения карточки в теге с географией.
   *
   * @param adId id размещаемой карточки.
   * @return 302 see other, 416 not acceptable.
   */
  def forAdSubmit(adId: String) = csrf.Check {
    canAdvAd(adId, U.PersonNode).async(formPostBP) { implicit request =>
      lazy val logPrefix = s"forAdSubmit($adId):"

      // Хватаем бинарные данные из тела запроса...
      advGeoFormUtil.validateForm( request.body ).fold(
        {violations =>
          LOGGER.debug(s"$logPrefix Failed to bind form: ${violations.mkString("\n", "\n ", "")}")
          NotAcceptable( violations.toString )
        },

        {mFormS =>
          LOGGER.trace(s"$logPrefix Binded: $mFormS")

          val mFormS2Fut = _checkFormRcvrs(mFormS)
          val isSuFree = advFormUtil.isFreeAdv( mFormS.adv4freeChecked )

          val abcFut = mFormS2Fut.flatMap { mFormS2 =>
            advGeoBillUtil.advBillCtx(isSuFree, request.mad, mFormS2)
          }

          val status   = advFormUtil.suFree2newItemStatus(isSuFree) // TODO Не пашет пока что. Нужно другой вызов тут.

          for {
          // Прочитать узел юзера. Нужно для чтения/инциализации к контракта
            personNode0 <- request.user.personNodeFut

            // Узнать контракт юзера
            e           <- bill2Util.ensureNodeContract(personNode0, request.user.mContractOptFut)

            // Дождаться готовности контекста биллинга:
            abc         <- abcFut

            // Произвести добавление товаров в корзину.
            (itemsCart, itemsAdded) <- {
              // Надо определиться, правильно ли инициализацию корзины запихивать внутрь транзакции?
              val dbAction = for {
                // Найти/создать корзину
                cart    <- bill2Util.ensureCart(
                  contractId = e.mc.id.get,
                  status0    = MOrderStatuses.cartStatusForAdvSuperUser(isSuFree)
                )

                // Узнать, потребуется ли послать письмецо модераторам после добавления item'ов.
                //mdrNotifyNeeded <- mdrUtil.isMdrNotifyNeeded

                // Закинуть заказ в корзину юзера. Там же и рассчет цены будет.
                addRes  <- advGeoBillUtil.addToOrder(
                  orderId     = cart.id.get,
                  status      = status,
                  abc         = abc
                )
              } yield {
                (cart, addRes)
              }
              // Запустить экшен добавления в корзину на исполнение.
              import slick.profile.api._
              slick.db.run( dbAction.transactionally )
            }

          } yield {
            LOGGER.debug(s"$logPrefix $itemsAdded items added into cart#${itemsCart.id.orNull} of contract#${e.mc.id.orNull} with item status '$status'.")

            val rCall = routes.LkAdvGeo.forAd(adId)
            val retCall = if (!isSuFree) {
              val producerId  = request.producer.id.get
              routes.LkBill2.cart(producerId, r = Some(rCall.url))
              //implicit val messages = implicitly[Messages]
              // Пора вернуть результат работы юзеру: отредиректить в корзину-заказ для оплаты.
              //Redirect()
              //  .flashing(FLASH.SUCCESS -> messages("Added.n.items.to.cart", itemsAdded.size))

            } else {
              // Суперюзеры отправляются назад в эту же форму для дальнейшего размещения.
              //Redirect( rCall )
              //  .flashing(FLASH.SUCCESS -> "Ads.were.adv")
              rCall
            }
            Ok(retCall.url)
          }
        }
      )
    }
  }


  /** Body parser для реквестов, содержащих внутри себя сериализованный инстанс MFormS. */
  private def formPostBP = reqUtil.picklingBodyParser[MFormS]


  /**
    * Экшн рассчета стоимости текущего размещения.
    *
    * @param adId id рекламной карточки.
    * @return 200 / 416 + JSON
    */
  def getPriceSubmit(adId: String) = csrf.Check {
    canAdvAd(adId).async(formPostBP) { implicit request =>
      lazy val logPrefix = s"getPriceSubmit($adId):"

      advGeoFormUtil.validateForm( request.body ).fold(
        {violations =>
          LOGGER.debug(s"$logPrefix Failed to validate form data: ${violations.mkString("\n", "\n ", "")}")
          NotAcceptable( violations.toString )
        },
        {mFormS =>
          val mFromS2Fut = _checkFormRcvrs(mFormS)
          val isSuFree = advFormUtil.isFreeAdv( mFormS.adv4freeChecked )

          // Запустить асинхронные операции: Надо обратиться к биллингу за рассчётом ценника:
          val pricingFut = for {
            mFormS2 <- mFromS2Fut
            pricing <- _getPricing(isSuFree, mFormS2)
          } yield {
            LOGGER.trace(s"$logPrefix request body =\n $mFormS=>$mFormS2 su=$isSuFree pricing => $pricing")
            advFormUtil.prepareAdvPricing(pricing)
          }

          for {
            pricing <- pricingFut
          } yield {
            // Сериализация результата.
            val bbuf = PickleUtil.pickle(pricing)
            Ok( ByteString(bbuf) )
          }
        }
      )
    }
  }


  /**
    * Получение списка маркеров-точек узлов-ресиверов для карты.
    * @param adId id текущей размещаемой рекламной карточки.
    *             Пока используется как основание для проверки прав доступа.
    */
  def advRcvrsMap(adId: MEsUuId) = csrf.Check {
    canAdvAd(adId).async { implicit request =>
      val nodesSrc = cache.getOrElse("advGeoNodesSrc", expiration = 10.seconds) {
        val msearch = advGeoMapUtil.onMapRcvrsSearch(30)
        advGeoMapUtil.rcvrNodesMap( msearch )
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
  }


  /** Текущие георазмещения карточки, т.е. размещения на карте в кружках.
    *
    * @param adId id интересующей рекламной карточки.
    * @return js.Array[GjFeature].
    */
  def existGeoAdvsMap(adId: String) = csrf.Check {
    canAdvAd(adId).async { implicit request =>
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

      streamsUtil.maybeTraceCount(currAdvsSrc, this) { totalCount =>
        s"existGeoAdvsMap($adId): streamed $totalCount GeoJSON features"
      }

      Ok.chunked(jsonStrSrc)
        .as( withCharset(JSON) )
        .withHeaders(CACHE_10)
    }
  }


  /**
    * Экшен получения данных для рендера попапа по размещениям.
    * @param itemId id по таблице mitem.
    * @return Бинарный выхлоп с данными для react-рендера попапа.
    */
  def existGeoAdvsShapePopup(itemId: Gid_t) = csrf.Check {
    canAccessItem(itemId, edit = false).async { implicit request =>
      def logPrefix = s"existGeoAdvsShapePopup($itemId):"

      // Доп. проверка прав доступа: вдруг юзер захочет пропихнуть тут какой-то левый (но свой) item.
      // TODO Вынести суть ассерта на уровень отдельного ActionBuilder'а, проверяющего права доступа по аналогии с CanAccessItemPost.
      if ( !MItemTypes.advGeoTypes.contains( request.mitem.iType ) )
        throw new IllegalArgumentException(s"$logPrefix Item itype==${request.mitem.iType}, but here we need GeoPlace: ${MItemTypes.GeoPlace}")

      // Наврядли можно отрендерить в попапе даже это количество...
      val itemsMax = RCVR_ITEMS_PER_POPUP_LIMIT

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

      implicit val ctx = implicitly[Context]

      val rowsMsFut = itemsSrc
        // Причесать кортежи в нормальные инстансы
        .map( MAdvGeoBasicInfo.apply )
        // Сгруппировать и объеденить по периодам размещения.
        .groupBy(itemsMax, { m => (m.dtStartOpt, m.dtEndOpt) })
        .fold( List.empty[MAdvGeoBasicInfo] ) { (acc, e) => e :: acc }
        .map { infos =>
          // Нужно отсортировать item'ы по алфавиту или id, завернув их в итоге в Row
          val info0 = infos.head
          val row = MGeoAdvExistRow(
            // Диапазон дат, если есть.
            dateRange = MRangeYmdOpt.applyFrom(
              dateStartOpt = _offDate2localDateOpt(info0.dtStartOpt)(ctx),
              dateEndOpt   = _offDate2localDateOpt(info0.dtEndOpt)(ctx)
            ),
            // Инфа по item'ам.
            items = infos
              .sortBy(m => (m.tagFaceOpt, m.id) )
              .flatMap { m =>
                val mgiPlOpt: Option[MGeoItemInfoPayload] = m.iType match {
                  case MItemTypes.GeoTag =>
                    m.tagFaceOpt
                      .map { InGeoTag.apply }
                  case MItemTypes.GeoPlace  =>
                    Some( OnMainScreen )
                  case otherType =>
                    LOGGER.error(s"$logPrefix Unexpected iType=$otherType for #${m.id}, Dropping adv data.")
                    None
                    //throw new IllegalArgumentException("Unexpected iType = " + otherType)
                }

                if (mgiPlOpt.isEmpty)
                  LOGGER.warn(s"$logPrefix Dropped adv data: $m")

                for (mgiPl <- mgiPlOpt) yield {
                  MGeoItemInfo(
                    itemId        = m.id,
                    isOnlineNow   = m.status == MItemStatuses.Online,
                    payload       = mgiPl
                  )
                }
              }
          )
          val startMs = info0.dtStartOpt.map(_.toInstant.toEpochMilli)
          startMs -> row
        }
        // Вернуться на уровень основного потока...
        .mergeSubstreams
        // Собрать все имеющиеся результаты в единую коллекцию.
        .runFold( List.empty[(Option[Long], MGeoAdvExistRow)] ) { (acc, row) => row :: acc }

      // Параллельно считаем общее кол-во найденных item'ов, чтобы сравнить их с лимитом.
      val itemsCountFut = streamsUtil.count(itemsSrc)

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
        LOGGER.trace(s"$logPrefix count=$itemsCount/$itemsMax haveMore=${mresp.haveMore} rows=${mresp.rows}")

        // Сериализовать и вернуть результат:
        val pickled = PickleUtil.pickle(mresp)
        Ok( ByteString(pickled) )
          .withHeaders(CACHE_10)
      }
    }
  }

  private def _offDate2localDateOpt(offDateOpt: Option[OffsetDateTime])(implicit ctx: Context): Option[LocalDate] = {
    // TODO Выставлять local-date на основе текущего offset'а юзера через ctx.
    offDateOpt.map(_.toLocalDate)
  }


  /** Рендер попапа при клике по узлу-ресиверу на карте ресиверов.
    *
    * @param adIdU id текущей карточки, которую размещают.
    * @param rcvrNodeIdU id узла, по которому кликнул юзер.
    * @return Бинарь для boopickle на стороне JS.
    */
  def rcvrMapPopup(adIdU: MEsUuId, rcvrNodeIdU: MEsUuId) = _rcvrMapPopup(adId = adIdU, rcvrNodeId = rcvrNodeIdU)

  private def _rcvrMapPopup(adId: String, rcvrNodeId: String) = csrf.Check {
    canThinkAboutAdvOnMapAdnNode(adId, nodeId = rcvrNodeId).async { implicit request =>

      import request.{mnode => rcvrNode}

      // Запросить по биллингу карту подузлов для запрашиваемого ресивера.
      val subNodesFut = advGeoMapUtil.findSubRcvrsOf(rcvrNodeId)

      lazy val logPrefix = s"rcvrMapPopup($adId,$rcvrNodeId)[${System.currentTimeMillis}]:"

      // Нужно получить все суб-узлы из кэша. Текущий узел традиционно уже есть в request'е.
      val subNodesIdsFut = subNodesFut.map(OptId.els2idsSet)

      // Закинуть во множество подузлов id текущего ресивера.
      val allNodesIdsFut = for (subNodesIds <- subNodesIdsFut) yield {
        LOGGER.trace(s"$logPrefix Found ${subNodesIds.size} sub-nodes: ${subNodesIds.mkString(", ")}")
        subNodesIds + rcvrNodeId
      }

      // Сгруппировать под-узлы по типам узлов, внеся туда ещё и текущий ресивер.
      val subNodesGrpsFut = for {
        subNodes <- subNodesFut
      } yield {
        subNodes
          // Сгруппировать узлы по их типам. Для текущего узла тип будет None. Тогда он отрендерится без заголовка и в самом начале списка узлов.
          .groupBy { mnode =>
            mnode.common.ntype
          }
          .toSeq
          // Очень кривая сортировка, но для наших нужд и такой пока достаточно.
          .sortBy( _._1.id )
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
        val src = pubFut.toSource
        streamsUtil.maybeTraceCount(src, this) { totalCount =>
          s"$logPrefix Found $totalCount curr advs to rcvrs"
        }
        src
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

      implicit val ctx = implicitly[Context]

      // Карта интервалов размещения по id узлов.
      val intervalsMapFut: Future[Map[String, MRangeYmdOpt]] = for {
        // Сбилдить карту на основе источника текущих размещений
        currAdvs <- currAdvsSrc.runFold(Map.newBuilder[String, MRangeYmdOpt]) { (acc, i) =>
          for {
            rcvrId <- i.rcvrIdOpt
          } {
            val rymd = MRangeYmdOpt.applyFrom(
              dateStartOpt = _offDate2localDateOpt(i.dateStartOpt)(ctx),
              dateEndOpt = _offDate2localDateOpt(i.dateEndOpt)(ctx)
            )
            acc += rcvrId -> rymd
          }
          acc
        }

      } yield {
        val r = currAdvs.result()
        LOGGER.trace(s"$logPrefix Found ${r.size} adv date ranges for nodes: ${r.keysIterator.mkString(", ")}")
        r
      }

      // Сборка JSON-модели для рендера JSON-ответа, пригодного для рендера с помощью react.js.
      for {
        nodesHasOnline      <- nodesHasOnlineFut
        intervalsMap        <- intervalsMapFut
        advRcvrIdsActual    <- advRcvrIdsActualFut
        advRcvrIdsBusy      <- advRcvrIdsBusyFut
        subNodesGrps        <- subNodesGrpsFut
      } yield {

        def __mkCheckBoxMeta(mnode: MNode): MRcvrPopupMeta = {
          val nodeId = mnode.id.get
          MRcvrPopupMeta(
            isCreate    = !advRcvrIdsBusy.contains( nodeId ),
            checked     = advRcvrIdsActual.contains( nodeId ),
            name        = mnode.guessDisplayNameOrId.getOrElse("???"),
            isOnlineNow = nodesHasOnline.contains( nodeId ),
            dateRange   = intervalsMap.getOrElse( nodeId , MRangeYmdOpt.empty )
          )
        }

        val resp = MRcvrPopupResp(
          node = Some(MRcvrPopupNode(
            id    = rcvrNodeId,

            // Чекбокс у данного узла можно отображать, если он является узлом-ресивером.
            // isReceiver здесь пока дублирует такую же проверку в ACL. Посмотрим, как дальше пойдёт...
            checkbox  = for {
              adn <- rcvrNode.extras.adn
              if adn.isReceiver
            } yield {
              __mkCheckBoxMeta( rcvrNode )
            },

            // Под-группы просто строим из под-узлов.
            subGroups = for {
              (ntype, nodes) <- subNodesGrps
            } yield {
              MRcvrPopupGroup(
                title = Some( ctx.messages( ntype.plural ) ),
                nodes = for (n <- nodes) yield {
                  MRcvrPopupNode(
                    id    = n.id.get,
                    checkbox  = Some( __mkCheckBoxMeta(n) ),
                    subGroups = Nil
                  )
                }
              )
            }
          ))
        )

        Ok( ByteString( PickleUtil.pickle(resp) ) )
          // Чисто для подавления двойных запросов. Ведь в теле запроса могут быть данные формы, которые варьируются.
          .withHeaders(CACHE_10)
      }
    }
  }

  /** Хидер короткого кеша, в основном для защиты от повторяющихся запросов. */
  private def CACHE_10 = CACHE_CONTROL -> "private, max-age=10"

}
