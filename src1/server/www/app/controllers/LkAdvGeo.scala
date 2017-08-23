package controllers

import akka.stream.scaladsl.Source
import akka.util.ByteString
import javax.inject.Inject

import controllers.ctag.NodeTagsEdit
import io.suggest.adv.geo._
import io.suggest.adv.rcvr._
import io.suggest.async.StreamsUtil
import io.suggest.bill.MGetPriceResp
import io.suggest.bin.ConvCodecs
import io.suggest.common.tags.edit.MTagsEditProps
import io.suggest.dt.{MAdvPeriod, YmdHelpersJvm}
import io.suggest.dt.interval.MRangeYmdOpt
import io.suggest.es.model.MEsUuId
import io.suggest.geo.MGeoPoint
import io.suggest.init.routed.MJsiTgs
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.item.{MItem, MItems}
import io.suggest.mbill2.m.item.status.MItemStatuses
import io.suggest.mbill2.m.item.typ.MItemTypes
import io.suggest.mbill2.m.order.MOrderStatuses
import io.suggest.model.n2.node.MNode
import io.suggest.pick.PickleUtil
import io.suggest.pick.PickleSrvUtil._
import io.suggest.primo.id.OptId
import io.suggest.util.logs.MacroLogsImpl
import io.suggest.www.util.req.ReqUtil
import models.adv.geo.tag.MForAdTplArgs
import models.mctx.Context
import models.mproj.ICommonDi
import models.req.IAdProdReq
import play.api.mvc.Result
import util.acl._
import util.adv.AdvFormUtil
import util.adv.geo.{AdvGeoBillUtil, AdvGeoFormUtil, AdvGeoLocUtil, AdvGeoRcvrsUtil}
import util.billing.Bill2Util
import util.lk.LkTagsSearchUtil
import util.mdr.MdrUtil
import util.sec.CspUtil
import util.tags.TagsEditFormUtil
import views.html.lk.adv.geo._
import io.suggest.scalaz.ScalazUtil.Implicits._

import scala.concurrent.Future

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
                           advGeoRcvrsUtil                 : AdvGeoRcvrsUtil,
                           streamsUtil                     : StreamsUtil,
                           ymdHelpersJvm                   : YmdHelpersJvm,
                           reqUtil                         : ReqUtil,
                           ignoreAuth                      : IgnoreAuth,
                           cspUtil                         : CspUtil,
                           mdrUtil                         : MdrUtil,
                           lkGeoCtlUtil                    : LkGeoCtlUtil,
                           mItems                          : MItems,
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
        // Залить начальные данные в маппинг формы.
        MFormS(
          mapProps = advGeoFormUtil.mapProps0(gp0),
          // TODO Найти текущее размещение в draft items (в корзине неоплаченных).
          onMainScreen = true,
          adv4freeChecked = advFormUtil.a4fCheckedOpt( a4fPropsOpt ),
          // TODO Найти текущие ресиверы в draft items (в корзине неоплаченных).
          rcvrsMap = Map.empty,
          // TODO Найти текущие теги в draft items (в корзине неоплаченных).
          tagsEdit = MTagsEditProps(),
          datePeriod = MAdvPeriod(),
          // TODO Найти текущее размещение в draft items (в корзине неоплаченных).
          radCircle = Some( advGeoFormUtil.radCircle0(gp0) ),
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
        billCtx     <- advGeoBillUtil.advBillCtx(isSuFree, request.mad, mFormS, addFreeRcvrs = true)
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
      implicit val ctxData = ctxData0.withJsiTgs(
        MJsiTgs.AdvGeoForm :: ctxData0.jsiTgs
      )
      getContext2
    }

    val _a4fPropsOptFut = advFormUtil.a4fPropsOpt0CtxFut( _ctxFut )

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
        pricing
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

        // Навесить скорректированный CSP-заголовок на HTTP-ответ, т.к. форма нуждается в доступе к картам OSM.
        cspUtil.applyCspHdrOpt( cspUtil.CustomPolicies.PageWithOsmLeaflet ) {
          rs(html)
        }
      }
    }

  }

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
            LOGGER.debug(s"$logPrefix $itemsAdded items added into cart#${itemsCart.id.orNull} of contract#${e.mc.id.orNull} with item status '$status'.")

            val rCall = routes.LkAdvGeo.forAd(adId)
            val retCall = if (!isSuFree) {
              // Обычный юзер отправляется читать свою корзину заказов.
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

      advGeoFormUtil.validateFromRequest().fold(
        {violations =>
          LOGGER.debug(s"$logPrefix Failed to validate form data: ${violations.iterator.mkString("\n", "\n ", "")}")
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
            pricing
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
      m.iType match {
        case MItemTypes.GeoTag =>
          m.tagFaceOpt
            .map { InGeoTag.apply }
        case MItemTypes.GeoPlace =>
          Some( OnMainScreen )
        case otherType =>
          LOGGER.error(s"$logPrefix Unexpected iType=$otherType for #${m.id}, Dropping adv data.")
          None
      }
    }
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
      val subNodesFut = advGeoRcvrsUtil.findSubRcvrsOf(rcvrNodeId)

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
              dateStartOpt = advGeoBillUtil.offDate2localDateOpt(i.dateStartOpt)(ctx),
              dateEndOpt   = advGeoBillUtil.offDate2localDateOpt(i.dateEndOpt)(ctx)
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

        Ok( ByteString( PickleUtil.pickle(resp) ) )
          // Чисто для подавления двойных запросов. Ведь в теле запроса могут быть данные формы, которые варьируются.
          .withHeaders(
            CACHE_CONTROL -> "private, max-age=10"
          )
      }
    }
  }

}
