package controllers

import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Keep, Sink}

import javax.inject.Inject
import io.suggest.adv.geo._
import io.suggest.adv.rcvr._
import io.suggest.bill.MGetPriceResp
import io.suggest.common.tags.edit.MTagsEditProps
import io.suggest.ctx.CtxData
import io.suggest.dt.MAdvPeriod
import io.suggest.dt.interval.MRangeYmdOpt
import io.suggest.es.model.MEsUuId
import io.suggest.geo.MGeoPoint
import io.suggest.init.routed.MJsInitTargets
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.item.MItem
import io.suggest.mbill2.m.item.status.MItemStatuses
import io.suggest.mbill2.m.item.typ.MItemTypes
import io.suggest.mbill2.m.order.MOrderStatuses
import io.suggest.n2.node.MNode
import io.suggest.util.logs.MacroLogsImpl
import models.mctx.Context
import models.req.IAdProdReq
import play.api.mvc.{BodyParser, Result}
import util.acl._
import util.adv.AdvFormUtil
import util.adv.geo.{AdvGeoBillUtil, AdvGeoFormUtil, AdvGeoLocUtil, AdvGeoRcvrsUtil}
import util.billing.{Bill2Conf, Bill2Util}
import util.lk.LkTagsSearchUtil
import util.sec.CspUtil
import views.html.lk.adv.geo._
import io.suggest.scalaz.ScalazUtil.Implicits._
import io.suggest.sec.util.Csrf
import io.suggest.streams.StreamsUtil
import io.suggest.tags.MTagsSearchQs
import models.adv.geo.MForAdTplArgs
import play.api.libs.json.Json
import util.adv.direct.AdvRcvrsUtil
import japgolly.univeq._
import play.api.http.HttpErrorHandler

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.11.15 14:51
  * Description: Контроллер размещения в гео-тегах.
  */
final class LkAdvGeo @Inject() (
                                 sioControllerApi                : SioControllerApi,
                               )
  extends MacroLogsImpl
{

  import sioControllerApi._
  import slickHolder.slick

  private lazy val advGeoFormUtil = injector.instanceOf[AdvGeoFormUtil]
  private lazy val advGeoBillUtil = injector.instanceOf[AdvGeoBillUtil]
  private lazy val advFormUtil = injector.instanceOf[AdvFormUtil]
  private lazy val bill2Conf = injector.instanceOf[Bill2Conf]
  private lazy val bill2Util = injector.instanceOf[Bill2Util]
  private lazy val advGeoLocUtil = injector.instanceOf[AdvGeoLocUtil]
  private lazy val advRcvrsUtil = injector.instanceOf[AdvRcvrsUtil]
  private lazy val advGeoRcvrsUtil = injector.instanceOf[AdvGeoRcvrsUtil]
  private lazy val streamsUtil = injector.instanceOf[StreamsUtil]
  private lazy val cspUtil = injector.instanceOf[CspUtil]
  private lazy val lkGeoCtlUtil = injector.instanceOf[LkGeoCtlUtil]
  private lazy val canThinkAboutAdvOnMapAdnNode = injector.instanceOf[CanThinkAboutAdvOnMapAdnNode]
  private lazy val canAdvAd = injector.instanceOf[CanAdvAd]
  private lazy val isAuth = injector.instanceOf[IsAuth]
  private lazy val tagSearchUtil = injector.instanceOf[LkTagsSearchUtil]
  private lazy val csrf = injector.instanceOf[Csrf]
  private lazy val errorHandler = injector.instanceOf[HttpErrorHandler]
  implicit private lazy val mat = injector.instanceOf[Materializer]


  /** Асинхронный детектор начальной точки для карты георазмещения. */
  private def getGeoPoint0(adId: String)(implicit request: IAdProdReq[_]): Future[MGeoPoint] = {
    import advGeoLocUtil.Detectors._
    val adIds = adId :: Nil
    // Ищем начальную точку среди прошлых размещений текущей карточки.

    FromAdsGeoAdvs(adIds)
      // Если не получилось, то ищем начальную точку среди размещений продьюсера карточки.
      .orElse {
        FromProducerGeoAdvs(
          producerIds = (request.producer.id :: request.user.personIdOpt :: Nil)
            .flatten,
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
        // Залить начальные данные в маппинг формы.
        MFormS(
          mapProps        = advGeoFormUtil.mapProps0(gp0),
          // TODO Найти текущее размещение в draft items (в корзине неоплаченных).
          onMainScreen    = true,
          adv4freeChecked = advFormUtil.a4fCheckedOpt( a4fPropsOpt ),
          // TODO Найти текущие ресиверы в draft items (в корзине неоплаченных).
          rcvrsMap        = Map.empty,
          // TODO Найти текущие теги в draft items (в корзине неоплаченных).
          tagsEdit        = MTagsEditProps.empty,
          // TODO Find current adv period (inside Cart order)
          datePeriod      = MAdvPeriod.default,
          // TODO Найти текущее размещение в draft items (в корзине неоплаченных).
          radCircle       = Some( advGeoFormUtil.radCircle0(gp0) ),
          tzOffsetMinutes = MFormS.TZ_OFFSET_IGNORE,
        )
      }

      resLogic.result(formFut, Ok, radEnabled = false)
    }
  }


  private def _getPricing(isSuFree: Boolean, mFormS: MFormS)
                         (implicit request: IAdProdReq[_], ctx: Context): Future[MGetPriceResp] = {
    bill2Conf.maybeFreePricing(isSuFree) {
      // Найти все узлы, принадлежащие текущему юзеру:
      for {
        billCtx     <- advGeoBillUtil.advBillCtx(
          isSuFree      = isSuFree,
          mad           = request.mad,
          res           = mFormS,
          addFreeRcvrs  = true,
          personId      = request.user.personIdOpt,
          adProducerId  = request.producer.id,
        )
        pricing     <- advGeoBillUtil.getPricing( billCtx )(ctx)
      } yield {
        advFormUtil.prepareAdvPricing( pricing )(ctx)
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
      implicit val ctxData = CtxData.jsInitTargetsAppendOne( MJsInitTargets.AdvGeoForm )(ctxData0)
      getContext2
    }

    val _rcvrsMapUrlArgsFut = _ctxFut.flatMap( advGeoRcvrsUtil.rcvrsMapUrlArgs()(_) )

    val _a4fPropsOptFut = advFormUtil.a4fPropsOpt0CtxFut( _ctxFut )

    /** Рендер ответа.
      *
      * @param formFut Маппинг формы.
      * @param rs Статус ответа HTTP.
      * @param radEnabled Значение для rad.enabled (включён ли круг на карте по умолчанию?).
      */
    def result(formFut: Future[MFormS], rs: Status, radEnabled: Boolean): Future[Result] = {
      def logPrefix = s"result(${request.mad.idOrNull} $rs):"

      // Отрендерить текущие радиусные размещения в форму MRoot.
      for {
        form    <- formFut
        formForPricing: MFormS = if (form.radCircle.isDefined !=* radEnabled) {
          MFormS.radCircle.modify(_.filter(_ => radEnabled))( form )
        } else {
          form
        }
        ctx     <- _ctxFut
        pricing <- _getPricing( _isSuFree, formForPricing )(request, ctx)
        a4fPropsOpt <- {
          LOGGER.trace(s"$logPrefix su=${_isSuFree}  prod=${request.producer.idOrNull}  pricing => $pricing")
          _a4fPropsOptFut
        }
        rcvrsMapUrlArgs   <- _rcvrsMapUrlArgsFut
      } yield {
        // Собираем исходную root-модель формы.
        val mFormInit = MFormInit(
          adId          = request.mad.id.get,
          adv4FreeProps = a4fPropsOpt,
          advPricing    = pricing,
          form          = form,
          rcvrsMap      = rcvrsMapUrlArgs,
          radEnabled    = radEnabled,
        )

        val rargs = MForAdTplArgs(
          mad           = request.mad,
          producer      = request.producer,
          formState     = Json.toJson( mFormInit ).toString(),
        )

        val html = AdvGeoForAdTpl(rargs)(ctx)

        // Навесить скорректированный CSP-заголовок на HTTP-ответ, т.к. форма нуждается в доступе к картам OSM.
        import cspUtil.Implicits._
        rs(html)
          .withCspHeader( cspUtil.CustomPolicies.PageWithOsmLeaflet )
      }
    }

  }


  /** Проверка присланной карты ресиверов. */
  private def _checkFormRcvrs(mFormS0: MFormS): Future[MFormS] = {
    if (mFormS0.rcvrsMap.nonEmpty) {
      val rcvrKeys2Fut = advGeoRcvrsUtil.checkRcvrs(mFormS0.rcvrsMap.keys)
      lazy val logPrefix = s"_checkFormRcvrs(${mFormS0.hashCode()})[${System.currentTimeMillis()}]:"
      for (keys2 <- rcvrKeys2Fut) yield {
        val rcvrsMap2: RcvrsMap_t = keys2
          .iterator
          .map { key2 =>
            key2 -> mFormS0.rcvrsMap(key2)
          }
          .toMap
        LOGGER.trace(s"$logPrefix Rcvrs map updated:\n OLD = ${mFormS0.rcvrsMap}\n NEW = $rcvrsMap2")
        (MFormS.rcvrsMap replace rcvrsMap2)(mFormS0)
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
      advGeoFormUtil.validateFromRequest().fold(
        {violations =>
          LOGGER.debug(s"$logPrefix Failed to bind form: ${violations.iterator.mkString("\n", "\n ", "")}")
          NotAcceptable( violations.toString )
        },

        {mFormS =>
          LOGGER.trace(s"$logPrefix Binded: $mFormS")

          val mFormS2Fut = _checkFormRcvrs(mFormS)
          val isSuFree = advFormUtil.isFreeAdv( mFormS.adv4freeChecked )

          val abcFut = mFormS2Fut.flatMap { mFormS2 =>
            advGeoBillUtil.advBillCtx(
              isSuFree  = isSuFree,
              mad       = request.mad,
              res       = mFormS2,
              personId  = request.user.personIdOpt,
              adProducerId = request.producer.id,
              addFreeRcvrs = false,
            )
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
                  contractId = e.contract.id.get,
                  status0    = MOrderStatuses.cartStatusForAdvSuperUser(isSuFree)
                )

                // Узнать, потребуется ли послать письмецо модераторам после добавления item'ов.
                //mdrNotifyNeeded <- mdrUtil.isMdrNotifyNeeded

                // Закинуть заказ в корзину юзера. Там же и рассчет цены будет.
                addRes  <- advGeoBillUtil.addToOrder(
                  adId        = adId,
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
            LOGGER.debug(s"$logPrefix $itemsAdded items added into cart#${itemsCart.id.orNull} of contract#${e.contract.id.orNull} with item status '$status'.")

            val rCall = routes.LkAdvGeo.forAd(adId)
            val retCall = if (!isSuFree) {
              // Обычный юзер отправляется читать свою корзину заказов.
              val producerId  = request.producer.id.get
              routes.LkBill2.orderPage(producerId, r = Some(rCall.url))
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
  private def formPostBP: BodyParser[MFormS] =
    parse.json[MFormS]


  /**
    * Экшн рассчета стоимости текущего размещения.
    *
    * @param adId id рекламной карточки.
    * @return 200 / 416 + JSON
    */
  def getPriceSubmit(adId: String) = csrf.Check {
    canAdvAd(adId).async(formPostBP) { implicit request =>
      lazy val logPrefix = s"getPriceSubmit($adId):"

      advGeoFormUtil.validateFromRequest().fold(
        {violations =>
          LOGGER.debug(s"$logPrefix Failed to validate form data: ${violations.iterator.mkString("\n", "\n ", "")}")
          errorHandler.onClientError(request, NOT_ACCEPTABLE, violations.toString)
        },
        {mFormS =>
          val mFromS2Fut = _checkFormRcvrs(mFormS)
          LOGGER.trace(s"$logPrefix orig form:\n $mFormS")

          val isSuFree = advFormUtil.isFreeAdv( mFormS.adv4freeChecked )

          // Запустить асинхронные операции: Надо обратиться к биллингу за рассчётом ценника:
          val pricingFut = for {
            mFormS2 <- mFromS2Fut
            pricing <- _getPricing( isSuFree, mFormS2 )
          } yield {
            LOGGER.trace(s"$logPrefix request body =\n $mFormS=>$mFormS2 su=$isSuFree pricing => $pricing")
            pricing
          }

          for {
            pricing <- pricingFut
          } yield {
            // Сериализация результата.
            Ok( Json.toJson(pricing) )
          }
        }
      )
    }
  }



  /** Текущие георазмещения карточки, т.е. размещения на карте в кружках.
    *
    * @param adId id интересующей рекламной карточки.
    * @return js.Array[GjFeature].
    */
  def existGeoAdvsMap(adId: String) = csrf.Check {
    canAdvAd(adId).async { implicit request =>
      lkGeoCtlUtil.currentNodeItemsGsToGeoJson( adId, MItemTypes.advGeoTypes )
    }
  }


  /**
    * Экшен получения данных для рендера попапа по размещениям.
    * @param itemId id по таблице mitem.
    * @return Бинарный выхлоп с данными для react-рендера попапа.
    */
  def existGeoAdvsShapePopup(itemId: Gid_t) = csrf.Check {
    lazy val logPrefix = s"existGeoAdvsShapePopup($itemId):"
    lkGeoCtlUtil.currentItemPopup(itemId, MItemTypes.advGeoTypes) { m =>
      val r = (
        (m.iType ==* MItemTypes.GeoPlace) ||
        (m.iType ==* MItemTypes.GeoTag)
      )

      if (!r)
        LOGGER.error(s"$logPrefix Unexpected iType=${m.iType} for #${m.id}, Dropping adv data.")

      r
    }
  }



  /** Рендер попапа при клике по узлу-ресиверу на карте ресиверов.
    *
    * @param adIdU id текущей карточки, которую размещают.
    * @param rcvrNodeIdU id узла, по которому кликнул юзер.
    * @return 200 OK + JSON.
    */
  def rcvrMapPopup(adIdU: MEsUuId, rcvrNodeIdU: MEsUuId) = csrf.Check {
    val adId = adIdU.id
    val rcvrNodeId = rcvrNodeIdU.id
    canThinkAboutAdvOnMapAdnNode(adId, nodeId = rcvrNodeId).async { implicit request =>

      import request.{mnode => rcvrNode}

      // Запросить по биллингу карту подузлов для запрашиваемого ресивера.
      val subNodesFut = advRcvrsUtil.findSubRcvrsOf(rcvrNodeId)

      lazy val logPrefix = s"rcvrMapPopup($adId,$rcvrNodeId)[${System.currentTimeMillis}]:"

      // Нужно получить все суб-узлы из кэша. Текущий узел традиционно уже есть в request'е.
      val subNodesIdsFut = for {
        subNodes <- subNodesFut
      } yield {
        subNodes
          .toIdIter[String]
          .to( Set )
      }

      // Закинуть во множество подузлов id текущего ресивера.
      val allNodesIdsFut = for {
        subNodesIds <- subNodesIdsFut
      } yield {
        LOGGER.trace(s"$logPrefix Found ${subNodesIds.size} sub-nodes: ${subNodesIds.mkString(", ")}")
        subNodesIds + rcvrNodeId
      }

      // Сгруппировать под-узлы по типам узлов, внеся туда ещё и текущий ресивер.
      val subNodesGrpsFut = for {
        subNodes <- subNodesFut
      } yield {
        subNodes
          // Сгруппировать узлы по их типам. Для текущего узла тип будет None. Тогда он отрендерится без заголовка и в самом начале списка узлов.
          .groupBy( _.common.ntype )
          .toSeq
          // Очень кривая сортировка, но для наших нужд и такой пока достаточно. TODO Сортировать по messages-названиям
          .sortBy( _._1.value )
      }


      import slick.profile.api._
      import streamsUtil.Implicits._

      val currItemsPublisherFut = for {
        allNodeIds <- allNodesIdsFut
      } yield {
        slick.db.stream {
          advGeoBillUtil
            .findCurrForAdToRcvrs(
              adId      = adId,
              // TODO Добавить поддержку групп маячков ресиверов.
              rcvrIds   = allNodeIds,
              // Интересуют и черновики, и текущие размещения
              statuses  = MItemStatuses.advActual,
              // TODO Надо бы тут порешить, какой limit требуется на деле и требуется ли вообще. 20 взято с потолка.
              limitOpt  = Some(300)
            )
            .forPgStreaming(20)
        }
      }

      // Запустить получение списка всех текущих (черновики + busy) размещений на узле и его маячках из биллинга.
      // Выпрямляем Future[Publisher] в просто нормальный Source:
      val currItemsSrc = currItemsPublisherFut
        .toSource
        .maybeTraceCount(this) { totalCount =>
          s"$logPrefix Found $totalCount curr advs to rcvrs"
        }

      // Sink, материализующий множество rcvrId из потока MItem:
      val items2rcvrIdsSink = Flow[MItem]
        .mapConcat( _.rcvrIdOpt.toList )
        .toMat( Sink.collection[String, Set[String]] )( Keep.right )

      implicit val ctx = implicitly[Context]

      val (((advRcvrIdsActualFut, advRcvrIdsBusyFut), nodesHasOnlineFut), intervalsMapFut) = currItemsSrc
        // Сбор всех (актуальных) rcvr id'шников в потоке для текущих размещений:
        .alsoToMat( items2rcvrIdsSink )(Keep.right)
        // Собираем только adv-busy-ресиверы:
        .alsoToMat {
          Flow[MItem]
            .filter( _.status.isAdvBusy )
            .toMat( items2rcvrIdsSink )(Keep.right)
        }( Keep.both )
        // Собрать множество id узлов, у которых есть хотя бы одно online-размещение:
        .alsoToMat {
          Flow[MItem]
            .filter( _.status ==* MItemStatuses.Online )
            .toMat( items2rcvrIdsSink )(Keep.right)
        }( Keep.both )
        // И собрать карту интервалов размещения по id узлов:
        .toMat {
          Flow[MItem]
            .mapConcat { i =>
              val kvsIter = for {
                rcvrId <- i.rcvrIdOpt.iterator
              } yield {
                val rymd = MRangeYmdOpt.applyFrom(
                  dateStartOpt = advGeoBillUtil.offDate2localDateOpt(i.dateStartOpt)(ctx),
                  dateEndOpt   = advGeoBillUtil.offDate2localDateOpt(i.dateEndOpt)(ctx)
                )
                rcvrId -> rymd
              }
              kvsIter.toList
            }
            .toMat( Sink.collection[(String, MRangeYmdOpt), Map[String, MRangeYmdOpt]] )(Keep.right)
        }( Keep.both )
        // И запустить этот велосипед на исполнение, сгенерив пачку фьючерсов:
        .run()

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
            isOnlineNow = nodesHasOnline.contains( nodeId ),
            dateRange   = intervalsMap.getOrElse( nodeId , MRangeYmdOpt.empty )
          )
        }

        val resp = MRcvrPopupResp(
          node = Some(MRcvrPopupNode(
            id    = rcvrNodeId,
            name  = rcvrNode.guessDisplayName,

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
                    id        = n.id.get,
                    name      = n.guessDisplayName,
                    checkbox  = Some( __mkCheckBoxMeta(n) ),
                    subGroups = Nil
                  )
                }
              )
            }
          ))
        )

        Ok( Json.toJson( resp ) )
          // Чисто для подавления двойных запросов. Ведь в теле запроса могут быть данные формы, которые варьируются.
          .withHeaders(
            CACHE_CONTROL -> "private, max-age=10"
          )
      }
    }
  }


  /** Поиск тегов по полубинарному протоколу (ответ бинарный).
    *
    * @param tsearch query string.
    * @return Сериализованная модель MTagsFound.
    */
  def tagsSearch2(tsearch: MTagsSearchQs) = csrf.Check {
    isAuth().async { implicit request =>
      for {
        found <-  tagSearchUtil.liveSearchTagsFromQs( tsearch )
      } yield {
        LOGGER.trace(s"tagSearch2(${request.rawQueryString}): Found ${found.tags.size} tags: ${found.tags.iterator.map(_.face).mkString(", ")}")
        Ok( Json.toJson(found) )
      }
    }
  }

}
