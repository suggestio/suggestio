package controllers

import akka.util.ByteString
import javax.inject.Inject
import io.suggest.adn.mapf.{MLamConf, MLamForm, MLamFormInit}
import io.suggest.adv.geo.OnGeoCapturing
import io.suggest.es.model.MEsUuId
import io.suggest.geo.MGeoPoint
import io.suggest.init.routed.MJsInitTargets
import io.suggest.mbill2.m.order.MOrderStatuses
import io.suggest.pick.PickleUtil
import io.suggest.pick.PickleSrvUtil._
import io.suggest.util.logs.MacroLogsImpl
import models.madn.mapf.MAdnMapTplArgs
import io.suggest.bill.MGetPriceResp.getPriceRespPickler
import io.suggest.bin.ConvCodecs
import io.suggest.ctx.CtxData
import io.suggest.dt.MAdvPeriod
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.item.typ.MItemTypes
import io.suggest.req.ReqUtil
import models.req.INodeReq
import util.acl.IsNodeAdmin
import util.adn.mapf.{LkAdnMapBillUtil, LkAdnMapFormUtil}
import util.adv.AdvFormUtil
import util.adv.geo.{AdvGeoLocUtil, AdvGeoRcvrsUtil}
import util.billing.Bill2Util
import util.sec.CspUtil
import views.html.lk.adn.mapf._
import io.suggest.scalaz.ScalazUtil.Implicits._

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.11.16 11:22
  * Description: Контроллер личного кабинета для связывания узла с точкой/местом на карте.
  * На карте в точках размещаются узлы ADN, и это делается за денежки.
  */
final class LkAdnMap @Inject() (
                                 sioControllerApi              : SioControllerApi,
                               )
  extends MacroLogsImpl
{

  import sioControllerApi._
  import mCommonDi._
  import mCommonDi.current.injector

  private lazy val advFormUtil = injector.instanceOf[AdvFormUtil]
  private lazy val lkAdnMapFormUtil = injector.instanceOf[LkAdnMapFormUtil]
  private lazy val lkAdnMapBillUtil = injector.instanceOf[LkAdnMapBillUtil]
  private lazy val bill2Util = injector.instanceOf[Bill2Util]
  private lazy val advGeoLocUtil = injector.instanceOf[AdvGeoLocUtil]
  private lazy val advGeoRcvrsUtil = injector.instanceOf[AdvGeoRcvrsUtil]
  private lazy val reqUtil = injector.instanceOf[ReqUtil]
  private lazy val lkGeoCtlUtil = injector.instanceOf[LkGeoCtlUtil]
  private lazy val cspUtil = injector.instanceOf[CspUtil]
  private lazy val isNodeAdmin = injector.instanceOf[IsNodeAdmin]


  /** Body-parser, декодирующий бинарь из запроса в инстанс MLamForm. */
  private def formPostBP = reqUtil.picklingBodyParser[MLamForm]


  /** Асинхронный детектор начальной точки для карты георазмещения. */
  private def getGeoPoint0(nodeId: String)(implicit request: INodeReq[_]): Future[MGeoPoint] = {
    import advGeoLocUtil.Detectors._
    // Ищем начальную точку среди прошлых размещений текущей карточки.
    FromProducerGeoAdvs(
      producerIds  = nodeId :: request.user.personIdOpt.toList
    )
      .orFromReqOrDflt
      // Запустить определение геоточки по обозначенной цепочке.
      .get
  }


  /**
    * Рендер страницы с формой размещения ADN-узла в точке на карте.
    * @param esNodeId id текущего ADN-узла.
    */
  def forNode(esNodeId: MEsUuId) = csrf.AddToken {
    val nodeId: String = esNodeId
    isNodeAdmin(nodeId, U.Lk).async { implicit request =>
      val geoPointFut = getGeoPoint0(nodeId)

      val ctxFut = for (ctxData0 <- request.user.lkCtxDataFut) yield {
        implicit val ctxData = CtxData.jsInitTargetsAppendOne(MJsInitTargets.AdnMapForm)(ctxData0)
        getContext2
      }

      val rcvrsMapUrlArgsFut = ctxFut.flatMap( advGeoRcvrsUtil.rcvrsMapUrlArgs()(_) )

      // Готовим дефолтовый MAdv4FreeProps.
      val a4fPropsOptFut = advFormUtil.a4fPropsOpt0CtxFut( ctxFut )

      val lamFormFut = for {
        geoPoint0     <- geoPointFut
        a4fPropsOpt   <- a4fPropsOptFut
      } yield {
        MLamForm(
          mapProps = lkAdnMapFormUtil.mapProps0( geoPoint0 ),
          mapCursor = lkAdnMapFormUtil.radCircle0( geoPoint0 ),
          datePeriod      = MAdvPeriod(),
          adv4freeChecked = advFormUtil.a4fCheckedOpt( a4fPropsOpt )
        )
      }

      val isSuFree = request.user.isSuper

      // Сразу посчитать стоимость размещения при текущей конфигурации.
      val priceRespFut = for {
        lamForm       <- lamFormFut
        abc           <- lkAdnMapBillUtil.advBillCtx(isSuFree, request.mnode, lamForm)
        priceResp     <- lkAdnMapBillUtil.getPricing(lamForm, isSuFree, abc)
      } yield {
        priceResp
      }

      // Собрать конфиг для MLamFormInit, сериализовать в Base64:
      val lamFormInitB64Fut = for {
        priceResp       <- priceRespFut
        lamForm         <- lamFormFut
        a4fPropsOpt     <- a4fPropsOptFut
        rcvrsMapUrlArgs <- rcvrsMapUrlArgsFut
      } yield {
        val init = MLamFormInit(
          conf = MLamConf(
            nodeId        = nodeId,
            rcvrsMap      = rcvrsMapUrlArgs
          ),
          priceResp       = priceResp,
          form            = lamForm,
          adv4FreeProps   = a4fPropsOpt
        )
        // Сериализовать в base64-строку:
        // Сериализуем модель через boopickle + base64 для рендера бинаря прямо в HTML.
        PickleUtil.pickleConv[MLamFormInit, ConvCodecs.Base64, String](init)
      }

      import cspUtil.Implicits._

      // Собрать наконец HTTP-ответ
      for {
        ctx             <- ctxFut
        lamFormInitB64  <- lamFormInitB64Fut
      } yield {
        // Собрать основные аргументы для рендера шаблона
        val rargs = MAdnMapTplArgs(
          mnode     = request.mnode,
          formB64   = lamFormInitB64
        )

        val html = AdnMapTpl(rargs)(ctx)

        // Навесить скорректированный CSP-заголовок на HTTP-ответ.
        Ok(html)
          .withCspHeader( cspUtil.CustomPolicies.PageWithOsmLeaflet )
      }
    }
  }


  /** Сабмит формы размещения узла. */
  def forNodeSubmit(esNodeId: MEsUuId) = csrf.Check {
    isNodeAdmin(esNodeId, U.PersonNode).async(formPostBP) { implicit request =>
      val nodeId: String = request.mnode.id.getOrElse(esNodeId)
      lazy val logPrefix = s"formNodeSubmit($nodeId):"

      // Биндим форму.
      lkAdnMapFormUtil.validateFromRequest().fold(
        // Забиндить не удалось, вернуть страницу с формой назад.
        {violations =>
          val violsStr = violations.iterator.mkString(", ")
          LOGGER.debug(s"$logPrefix: Unable to bind form:\n $violsStr")
          errorHandler.onClientError(request, NOT_ACCEPTABLE, violsStr)
        },

        // Бинд удался. Обработать покупку.
        {formRes =>
          // Стандартный код для разделения отработки беслпатного размещения от суперюзеров и платных размещений.
          val isSuFree = advFormUtil.isFreeAdv( formRes.adv4freeChecked )
          val abcFut   = lkAdnMapBillUtil.advBillCtx(isSuFree, request.mnode, formRes)
          val status   = advFormUtil.suFree2newItemStatus(isSuFree)

          // Найти или инициализировать корзину юзера, добавить туда покупки.
          for {
            // Прочитать узел юзера. Нужно для чтения/инциализации к контракта
            personNode0 <- request.user.personNodeFut

            // Узнать/инициализировать контракт юзера
            e           <- bill2Util.ensureNodeContract(personNode0, request.user.mContractOptFut)

            // Дождаться готовности bill-ctx.
            abc         <- abcFut

            // Добавить в корзину размещение узла на карте
            itemsAdded  <- {
              val dbAction = for {
                // Инициализировать корзину, если требуется...
                cart    <- bill2Util.ensureCart(
                  contractId = e.mc.id.get,
                  status0    = MOrderStatuses.cartStatusForAdvSuperUser(isSuFree)
                )

                // Закинуть заказ в корзину юзера. Там же и рассчет цены будет.
                addRes <- lkAdnMapBillUtil.addToOrder(
                  orderId = cart.id.get,
                  nodeId  = nodeId,
                  formRes = formRes,
                  status  = status,
                  abc     = abc
                )
              } yield {
                addRes
              }
              import slick.profile.api._
              slick.db.run( dbAction.transactionally )
            }

          } yield {
            // Логгируем удачное закидывание товара в корзину.
            LOGGER.debug {
              val n = "\n "
              s"$logPrefix Added ADN-map into cart ${e.mc.id.orNull}, su=$isSuFree: ${itemsAdded.mkString(n,n,"")}"
            }

            // Рендерить HTTP-ответ: Это 200 ok + url location в теле ответа.
            val rCall = routes.LkAdnMap.forNode(esNodeId)
            val retCall = if (!isSuFree) {
              // Обычный юзер отправляется читать свою корзину заказов.
              routes.LkBill2.orderPage(nodeId, r = Some(rCall.url))

            } else {
              // Суперюзеры отправляются назад в эту же форму для дальнейшего размещения.
              rCall
            }
            Ok(retCall.url)
          }
        }
      )
    }
  }


  /** Сабмит формы для рассчёт стоимости размещения. */
  def getPriceSubmit(esNodeId: MEsUuId) = csrf.Check {
    val nodeId = esNodeId.id
    isNodeAdmin(nodeId).async(formPostBP) { implicit request =>
      lazy val logPrefix = s"getPriceSubmit($nodeId):"

      // TODO Код ниже практически идентичен LkAdvGeo.getPriceSubmit(). Наверное нужен дедублицирующий трейт с логикой этого экшена?
      // Биндинг текущего состояния формы размещения.
      lkAdnMapFormUtil.validateFromRequest().fold(
        // Неудачно почему-то. Пусть будет ошибка.
        {violations =>
          val violationsStr = violations.iterator.mkString(", ")
          LOGGER.error(s"$logPrefix Failed to bind form:\n $violationsStr")
          errorHandler.onClientError(request, NOT_ACCEPTABLE, violationsStr)
        },

        // Забиндилось ок. Считаем ценник и рендерим результат...
        {formRes =>
          val isSuFree = advFormUtil.isFreeAdv( formRes.adv4freeChecked )
          val abcFut = lkAdnMapBillUtil.advBillCtx(isSuFree, request.mnode, formRes)

          val pricingFut = abcFut.flatMap { abc =>
            lkAdnMapBillUtil.getPricing(formRes, isSuFree, abc)
          }

          // Рендер HTTP-ответа с вычисленной ценой и конкретикой.
          for {
            pricing <- pricingFut
          } yield {
            val bbuf = PickleUtil.pickle( pricing )
            Ok( ByteString(bbuf) )
          }
        }
      )
    }
  }


  /** Текущие георазмещения карточки, т.е. размещения на карте в кружках.
    *
    * @param nodeId id текущего узла.
    * @return js.Array[GjFeature].
    */
  def currentNodeGeoGj(nodeId: MEsUuId) = csrf.Check {
    isNodeAdmin(nodeId).async { implicit request =>
      lkGeoCtlUtil.currentNodeItemsGsToGeoJson( nodeId, MItemTypes.adnMapTypes )
    }
  }


  def currentGeoItemPopup(itemId: Gid_t) = csrf.Check {
    lazy val logPrefix = s"existGeoAdvsShapePopup($itemId):"
    lkGeoCtlUtil.currentItemPopup(itemId, MItemTypes.adnMapTypes) { m =>
      m.iType match {
        case MItemTypes.GeoLocCaptureArea =>
          Some( OnGeoCapturing )
        case otherType =>
          LOGGER.error(s"$logPrefix Unexpected iType=$otherType for #${m.id}, Ignored item.")
          None
      }
    }
  }

}
