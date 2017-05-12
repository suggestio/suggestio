package controllers

import akka.util.ByteString
import com.google.inject.Inject
import io.suggest.adn.mapf.{MLamForm, MLamFormInit}
import io.suggest.adv.geo.MMapProps
import io.suggest.es.model.MEsUuId
import io.suggest.geo.MGeoPoint
import io.suggest.init.routed.MJsiTgs
import io.suggest.mbill2.m.order.MOrderStatuses
import io.suggest.pick.{PickleSrvUtil, PickleUtil}
import io.suggest.util.logs.MacroLogsImpl
import io.suggest.www.util.req.ReqUtil
import models.madn.mapf.MAdnMapTplArgs
import io.suggest.bill.MGetPriceResp.getPriceRespPickler
import io.suggest.bin.ConvCodecs
import io.suggest.dt.MAdvPeriod
import models.mproj.ICommonDi
import models.req.INodeReq
import util.acl.IsNodeAdmin
import util.adn.mapf.{LkAdnMapBillUtil, LkAdnMapFormUtil}
import util.adv.AdvFormUtil
import util.adv.geo.AdvGeoLocUtil
import util.billing.Bill2Util
import util.mdr.MdrUtil
import util.sec.CspUtil
import views.html.lk.adn.mapf._

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.11.16 11:22
  * Description: Контроллер личного кабинета для связывания узла с точкой/местом на карте.
  * На карте в точках размещаются узлы ADN, и это делается за денежки.
  */
class LkAdnMap @Inject() (
                           advFormUtil                   : AdvFormUtil,
                           lkAdnMapFormUtil              : LkAdnMapFormUtil,
                           lkAdnMapBillUtil              : LkAdnMapBillUtil,
                           bill2Util                     : Bill2Util,
                           advGeoLocUtil                 : AdvGeoLocUtil,
                           pickleSrvUtil                 : PickleSrvUtil,
                           mdrUtil                       : MdrUtil,
                           reqUtil                       : ReqUtil,
                           cspUtil                       : CspUtil,
                           isNodeAdmin                   : IsNodeAdmin,
                           override val mCommonDi        : ICommonDi
)
  extends SioControllerImpl
  with MacroLogsImpl
{

  import LOGGER._
  import mCommonDi._
  import pickleSrvUtil._

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
        implicit val ctxData = ctxData0.withJsiTgs(
          MJsiTgs.AdnMapForm :: ctxData0.jsiTgs
        )
        getContext2
      }

      // Готовим дефолтовый MAdv4FreeProps.
      val a4fPropsOptFut = advFormUtil.a4fPropsOpt0CtxFut( ctxFut )

      val lamFormFut = for {
        geoPoint0     <- geoPointFut
        a4fPropsOpt   <- a4fPropsOptFut
      } yield {
        MLamForm(
          mapProps = MMapProps(
            center = geoPoint0,
            zoom = 10
          ),
          coord           = geoPoint0,
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
        priceResp     <- priceRespFut
        lamForm       <- lamFormFut
        a4fPropsOpt   <- a4fPropsOptFut
      } yield {
        val init = MLamFormInit(
          nodeId          = nodeId,
          priceResp       = priceResp,
          form            = lamForm,
          adv4FreeProps   = a4fPropsOpt
        )
        // Сериализовать в base64-строку:
        // Сериализуем модель через boopickle + base64 для рендера бинаря прямо в HTML.
        PickleUtil.pickleConv[MLamFormInit, ConvCodecs.Base64, String](init)
      }

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
        cspUtil.applyCspHdrOpt( cspUtil.CustomPolicies.PageWithOsmLeaflet ) {
          Ok(html)
        }

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
          val violsStr = violations.mkString(", ")
          debug(s"$logPrefix: Unable to bind form:\n $violsStr")
          NotAcceptable( violsStr )
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

                // Узнать, потребуется ли отправлять письмо модераторам по итогам работы...
                //mdrNotifyNeeded <- mdrUtil.isMdrNotifyNeeded

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
              routes.LkBill2.cart(nodeId, r = Some(rCall.url))

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
    val nodeId: String = esNodeId
    isNodeAdmin(nodeId).async(formPostBP) { implicit request =>
      lazy val logPrefix = s"getPriceSubmit($nodeId):"

      // TODO Код ниже практически идентичен LkAdvGeo.getPriceSubmit(). Наверное нужен дедублицирующий трейт с логикой этого экшена?
      // Биндинг текущего состояния формы размещения.
      lkAdnMapFormUtil.validateFromRequest().fold(
        // Неудачно почему-то. Пусть будет ошибка.
        {violations =>
          val violationsStr = violations.mkString(", ")
          LOGGER.error(s"$logPrefix Failed to bind form:\n $violationsStr")
          NotAcceptable( violationsStr )
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

}
