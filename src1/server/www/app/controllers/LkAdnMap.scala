package controllers

import com.google.inject.Inject
import io.suggest.es.model.MEsUuId
import io.suggest.geo.MGeoPoint
import io.suggest.init.routed.MJsiTgs
import io.suggest.mbill2.m.order.MOrderStatuses
import io.suggest.util.logs.MacroLogsImpl
import models.adv.form.MDatesPeriod
import models.adv.price.GetPriceResp
import models.madn.mapf.{MAdnMapFormRes, MAdnMapTplArgs}
import models.maps.MapViewState
import models.mctx.Context
import models.merr.MError
import models.mproj.ICommonDi
import models.req.INodeReq
import play.api.data.Form
import play.api.i18n.Messages
import play.api.libs.json.Json
import play.api.mvc.Result
import util.acl.IsNodeAdmin
import util.adn.mapf.{LkAdnMapBillUtil, LkAdnMapFormUtil}
import util.adv.AdvFormUtil
import util.adv.geo.AdvGeoLocUtil
import util.billing.Bill2Util
import util.mdr.MdrUtil
import views.html.lk.adn.mapf._
import views.html.lk.adv.widgets.period._reportTpl
import views.html.lk.lkwdgts.price._priceValTpl

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
                           mdrUtil                       : MdrUtil,
                           isNodeAdmin                   : IsNodeAdmin,
                           override val mCommonDi        : ICommonDi
)
  extends SioControllerImpl
  with MacroLogsImpl
{

  import LOGGER._
  import mCommonDi._


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
    isNodeAdmin(esNodeId, U.Lk).async { implicit request =>
      // TODO Заполнить форму начальными данными: положение карты, начальная точка, начальный период размещения
      val nodeId: String = esNodeId
      val geoPointFut = getGeoPoint0(nodeId)

      val formResFut = for {
        geoPoint <- geoPointFut
      } yield {
        MAdnMapFormRes(
          point    = geoPoint,
          mapState = MapViewState(
            center = geoPoint
          ),
          period   = MDatesPeriod()
        )
      }

      formResFut.flatMap { formRes =>
        val form = lkAdnMapFormUtil.adnMapFormM
          .fill(formRes)

        _forNode(Ok, form)
      }
    }
  }


  /** Рендер страницы с формой узла */
  def _forNode(rs: Status, form: Form[MAdnMapFormRes])(implicit request: INodeReq[_]): Future[Result] = {
    // Собрать контекст для шаблонов. Оно зависит от контекста ЛК, + нужен доп.экшен для запуска js данной формы.
    val ctxFut = for {
      lkCtxData <- request.user.lkCtxDataFut
    } yield {
      implicit val ctx = lkCtxData.withJsiTgs(
        MJsiTgs.AdnMapForm :: lkCtxData.jsiTgs
      )
      implicitly[Context]
    }

    // Собрать основные аргументы для рендера шаблона
    val rargs = MAdnMapTplArgs(
      mnode = request.mnode,
      form  = form,
      price = bill2Util.zeroPricing     // TODO Считать ценник на основе данных состояния формы и su-флага.
    )

    // Отрендерить результат запроса.
    for {
      ctx <- ctxFut
    } yield {
      val html = AdnMapTpl(rargs)(ctx)
      rs(html)
    }
  }


  /** Сабмит формы размещения узла. */
  def forNodeSubmit(esNodeId: MEsUuId) = csrf.Check {
    isNodeAdmin(esNodeId, U.PersonNode).async { implicit request =>
      val nodeId: String = request.mnode.id.getOrElse(esNodeId)
      lazy val logPrefix = s"formNodeSubmit($nodeId):"

      // Биндим форму.
      lkAdnMapFormUtil.adnMapFormM.bindFromRequest().fold(
        // Забиндить не удалось, вернуть страницу с формой назад.
        {formWithErrors =>
          debug(s"$logPrefix: Unable to bind form:\n ${formatFormErrors(formWithErrors)}")
          _forNode(NotAcceptable, formWithErrors)
        },

        // Бинд удался. Обработать покупку.
        {formRes =>
          // Стандартный код для разделения отработки беслпатного размещения от суперюзеров и платных размещений.
          val isSuFree = advFormUtil.maybeFreeAdv()
          val status   = advFormUtil.suFree2newItemStatus(isSuFree)

          // Найти или инициализировать корзину юзера, добавить туда покупки.
          for {
          // Прочитать узел юзера. Нужно для чтения/инциализации к контракта
            personNode0 <- request.user.personNodeFut

            // Узнать/инициализировать контракт юзера
            e           <- bill2Util.ensureNodeContract(personNode0, request.user.mContractOptFut)

            // Добавить в корзину размещение узла на карте
            itemsAdded <- {
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
                  status  = status
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

            val rCall = routes.LkAdnMap.forNode(esNodeId)
            // Рендерить HTTP-ответ.
            if (!isSuFree) {
              implicit val messages = implicitly[Messages]
              // Пора вернуть результат работы юзеру: отредиректить в корзину-заказ для оплаты.
              Redirect(routes.LkBill2.cart(nodeId, r = Some(rCall.url)))
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
  }


  /** Сабмит формы для рассчёт стоимости размещения. */
  def getPriceSubmit(esNodeId: MEsUuId) = csrf.Check {
    isNodeAdmin(esNodeId).async { implicit request =>
      val nodeId: String = esNodeId
      lazy val logPrefix = s"getPriceSubmit($nodeId):"

      // TODO Код ниже практически идентичен LkAdvGeo.getPriceSubmit(). Наверное нужен дедублицирующий трейт с логикой этого экшена?
      // Биндинг текущего состояния формы размещения.
      lkAdnMapFormUtil.adnMapFormM.bindFromRequest().fold(
        // Неудачно почему-то. Пусть будет ошибка.
        {formWithErrors =>
          LOGGER.error(s"$logPrefix Failed to bind form:\n ${formatFormErrors(formWithErrors)}")
          val err = MError(
            msg = Some("Failed to bind form")
          )
          NotAcceptable( Json.toJson(err) )
        },

        // Забиндилось ок. Считаем ценник и рендерим результат...
        {formRes =>
          val isSuFree = advFormUtil.maybeFreeAdv()
          val pricingFut = lkAdnMapBillUtil.getPricing(formRes, isSuFree)

          implicit val ctx = implicitly[Context]

          // Запустить рендер ценника размещаемого контента
          val pricingHtmlFut = for {
            pricing <- pricingFut
          } yield {
            LOGGER.trace(s"$logPrefix formRes = $formRes, $isSuFree, pricing => $pricing")
            val html = _priceValTpl(pricing)(ctx)
            htmlCompressUtil.html2str4json(html)
          }

          // Отрендерить данные по периоду размещения
          val periodReportHtml = htmlCompressUtil.html2str4json {
            _reportTpl(formRes.period)(ctx)
          }

          // Рендер ответа.
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
  }

}
