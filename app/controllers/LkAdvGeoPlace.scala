package controllers

import com.google.inject.Inject
import models.adv.form.MDatesPeriod
import models.adv.geo.place.{MAgpForAdTplArgs, MAgpFormResult}
import models.adv.price.GetPriceResp
import models.jsm.init.MTargets
import models.mctx.Context
import models.merr.MError
import models.mproj.ICommonDi
import models.req.IAdProdReq
import play.api.data.Form
import play.api.i18n.Messages
import play.api.libs.json.Json
import play.api.mvc.Result
import util.PlayMacroLogsImpl
import util.acl.{CanAdvertiseAdUtil, CanAdvertiseAd}
import util.adv.AdvFormUtil
import util.adv.geo.place.{AgpBillUtil, AgpFormUtil}
import util.billing.Bill2Util
import views.html.lk.adv.geo.place.AgpForAdTpl
import views.html.lk.adv.widgets.period._reportTpl
import views.html.lk.lkwdgts.price._priceValTpl

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.03.16 14:55
  * Description: Контроллер для просто размещений в произвольном месте на карте.
  */
class LkAdvGeoPlace @Inject() (
  advFormUtil                     : AdvFormUtil,
  agpFormUtil                     : AgpFormUtil,
  agpBillUtil                     : AgpBillUtil,
  bill2Util                       : Bill2Util,
  override val canAdvAdUtil       : CanAdvertiseAdUtil,
  override val mCommonDi          : ICommonDi
)
  extends SioControllerImpl
  with PlayMacroLogsImpl
  with CanAdvertiseAd
{

  import mCommonDi._
  import LOGGER._

  /**
    * Реакция на запрос страницы размещения карточки в произвольном месте на карте.
    *
    * @param nodeId id карточки.
    * @return 200 OK + страница с формой размещения.
    */
  def forAd(nodeId: String) = CanAdvertiseAdGet(nodeId, U.Lk).async { implicit request =>
    val gp0Fut = advFormUtil.geoPoint0()
    val form0 = agpFormUtil.agpForm

    val form1Fut = for (gp0 <- gp0Fut) yield {
      val formVal0 = MAgpFormResult(
        radMapVal = advFormUtil.radMapValue0(gp0),
        period    = MDatesPeriod()
      )
      form0.fill(formVal0)
    }

    form1Fut.flatMap { form1 =>
      _forAd(form1, Ok)
    }
  }

  /** Общий код возврата страницы с формой размещения в месте живёт здесь. */
  private def _forAd(form: Form[MAgpFormResult], rs: Status)(implicit request: IAdProdReq[_]): Future[Result] = {
    val ctxData0Fut = request.user.lkCtxDataFut

    val isSuFree    = request.user.isSuper
    val pricingFut  = agpBillUtil.getPricing(form.value, isSuFree)

    for {
      ctxData0 <- ctxData0Fut
      pricing  <- pricingFut
    } yield {
      implicit val ctxData = ctxData0.copy(
        jsiTgs = Seq(MTargets.AdvGeoPlaceForm)
      )
      val rargs = MAgpForAdTplArgs(
        form      = form,
        mad       = request.mad,
        producer  = request.producer,
        price     = pricing
      )
      rs( AgpForAdTpl(rargs) )
    }
  }

  /**
    * Сабмит формы размещения в произвольном месте на карте.
    *
    * @param adId id узла (карточки).
    * @return Редирект.
    *         Not Acceptable со страницей и формой.
    */
  def forAdSubmit(adId: String) = CanAdvertiseAdPost(adId).async { implicit request =>
    agpFormUtil.agpForm.bindFromRequest().fold(
      {formWithErrors =>
        debug(s"forAdSubmit($adId): Failed to bind form:\n ${formatFormErrors(formWithErrors)}")
        // Следует тут быть острожнее с локализованными датами: в рендер нужно передавать форму с Form.value = Some, иначе даты в виджете интервала размещения слетают.
        _forAd(formWithErrors, NotAcceptable)
      },
      {res =>
        val isSuFree = advFormUtil.maybeFreeAdv()
        val status   = advFormUtil.suFree2newItemStatus(isSuFree)

        val producerId = request.producer.id.get

        // Найти корзину юзера и добавить туда покупки.
        for {
          // Прочитать узел юзера
          personNode0 <- request.user.personNodeFut

          // Узнать контракт юзера
          e           <- bill2Util.ensureNodeContract(personNode0, request.user.mContractOptFut)

          // Произвести добавление товаров в корзину.
          itemsAdded <- {
            // Надо определиться, правильно ли инициализацию корзины запихивать внутрь транзакции?
            val dbAction = for {
              // Найти/создать корзину
              cart    <- bill2Util.ensureCart(e.mc.id.get)
              // Закинуть заказ в корзину юзера. Там же и рассчет цены будет.
              addRes  <- agpBillUtil.addToOrder(
                orderId     = cart.id.get,
                producerId  = producerId,
                adId        = adId,
                res         = res,
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
          if (!isSuFree) {
            implicit val messages = implicitly[Messages]
            // Пора вернуть результат работы юзеру: отредиректить в корзину-заказ для оплаты.
            Redirect(routes.LkBill2.cart(producerId, r = Some(routes.LkAdvGeoPlace.forAd(adId).url)))
              .flashing(FLASH.SUCCESS -> messages("Added.n.items.to.cart", itemsAdded.size))

          } else {
            // Суперюзеры отправляются назад в эту же форму для дальнейшего размещения.
            Redirect( routes.LkAdvGeoPlace.forAd(adId) )
              .flashing(FLASH.SUCCESS -> "Ads.were.adv")
          }
        }
      }
    )
  }


  /**
    * Запрос стоимость размещения.
    *
    * @param nodeId id текущей карточки.
    * @return 200 OK + JSON.
    */
  def getPriceSubmit(nodeId: String) = CanAdvertiseAdPost(nodeId).async { implicit request =>
    lazy val logPrefix = s"getPriceSubmit($nodeId):"
    agpFormUtil.agpForm.bindFromRequest().fold(
      {formWithErrors =>
        debug(s"$logPrefix failed to bind form:\n ${formatFormErrors(formWithErrors)}")
        val err = MError(
          msg = Some("Failed to bind form")
        )
        NotAcceptable( Json.toJson(err) )
      },
      // Запустить рассчет цены и формирование результата
      {res =>
        trace(s"$logPrefix Binded: $res")

        val isSuFree = advFormUtil.maybeFreeAdv()
        val advPricingFut = agpBillUtil.getPricing(res, isSuFree)

        implicit val ctx = implicitly[Context]

        // Запустить рассчет стоимости размещаемого
        val pricingFut = for {
          advPricing <- advPricingFut
        } yield {
          LOGGER.trace(s"$logPrefix pricing => $advPricing, isSuFree = $isSuFree")
          val html = _priceValTpl(advPricing)(ctx)
          html2str4json(html)
        }

        // Отрендерить данные по периоду размещения
        val periodReportHtml = html2str4json {
          _reportTpl(res.period)(ctx)
        }

        for {
          pricingHtml <- pricingFut
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
