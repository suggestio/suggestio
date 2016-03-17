package controllers

import com.google.inject.Inject
import controllers.ctag.NodeTagsEdit
import io.suggest.model.geo.{CircleGs, Distance, GeoPoint}
import models.adv.form.MDatesPeriod
import models.adv.geo.tag.{MForAdTplArgs, MAgtFormResult, AgtForm_t}
import models.adv.price.GetPriceResp
import models.jsm.init.MTargets
import models.maps.{RadMapValue, MapViewState}
import models.mctx.Context
import models.merr.MError
import models.mproj.ICommonDi
import models.req.IAdProdReq
import models.GeoIp
import org.elasticsearch.common.unit.DistanceUnit
import play.api.i18n.Messages
import play.api.libs.json.Json
import play.api.mvc.Result
import util.PlayMacroLogsImpl
import util.acl.{CanAdvertiseAd, CanAdvertiseAdUtil}
import util.adv.AdvFormUtil
import util.adv.geo.tag.{AgtFormUtil, AgtBillUtil}
import util.billing.Bill2Util
import util.tags.TagsEditFormUtil
import views.html.lk.adv.geo.tag._
import views.html.lk.adv.widgets.period._reportTpl
import views.html.lk.lkwdgts.price._priceValTpl

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.11.15 14:51
  * Description: Контроллер размещения в гео-тегах.
  */
class LkAdvGeoTag @Inject() (
  agtFormUtil                     : AgtFormUtil,
  agtBillUtil                     : AgtBillUtil,
  advFormUtil                     : AdvFormUtil,
  bill2Util                       : Bill2Util,
  override val tagsEditFormUtil   : TagsEditFormUtil,
  override val canAdvAdUtil       : CanAdvertiseAdUtil,
  override val mCommonDi          : ICommonDi
)
  extends SioControllerImpl
  with PlayMacroLogsImpl
  with CanAdvertiseAd
  with NodeTagsEdit
{

  import mCommonDi._

  /**
    * Экшен рендера страницы размещения карточки в теге с географией.
    *
    * @param adId id отрабатываемой карточки.
    */
  def forAd(adId: String) = CanAdvertiseAdGet(adId, U.Lk).async { implicit request =>
    val ipLocFut = GeoIp.geoSearchInfoOpt
    val formEmpty = agtFormUtil.agtFormStrict

    val formFut = for {
      ipLocOpt <- ipLocFut
    } yield {
      val gp = ipLocOpt
        .flatMap(_.ipGeopoint)
        .getOrElse( GeoPoint(59.93769, 30.30887) )    // Штаб ВМФ СПб, который в центре СПб

      val radMapVal = RadMapValue(
        state = MapViewState(gp, zoom = 10),
        circle = CircleGs(gp, radius = Distance(10000, DistanceUnit.METERS))
      )

      val res = MAgtFormResult(
        tags      = Nil,
        radMapVal = radMapVal,
        dates     = MDatesPeriod()
      )

      formEmpty.fill(res)
    }

    formFut.flatMap { form =>
      _forAd(form, Ok)
    }
  }

  /**
   * common-код экшенов GET'а и POST'а формы forAdTpl.
   *
   * @param form Маппинг формы.
   * @param rs Статус ответа HTTP.
   * @return Фьючерс с ответом.
   */
  private def _forAd(form: AgtForm_t, rs: Status)
                    (implicit request: IAdProdReq[_]): Future[Result] = {
    for {
      ctxData0 <- request.user.lkCtxDataFut
    } yield {
      implicit val ctxData = ctxData0.copy(
        jsiTgs = Seq(MTargets.AdvGtagForm)
      )
      val rargs = MForAdTplArgs(
        mad       = request.mad,
        producer  = request.producer,
        form      = form,
        advPeriodsAvail = advFormUtil.advPeriodsAvailable,
        price     = bill2Util.zeroPricing
      )
      rs(AgtForAdTpl(rargs))
    }
  }


  /**
   * Экшен сабмита формы размещения карточки в теге с географией.
   *
   * @param adId id размещаемой карточки.
   * @return 302 see other, 416 not acceptable.
   */
  def forAdSubmit(adId: String) = CanAdvertiseAdPost(adId, U.Lk, U.PersonNode).async { implicit request =>
    agtFormUtil.agtFormStrict.bindFromRequest().fold(
      {formWithErrors =>
        LOGGER.debug(s"forAdSubmit($adId): Failed to bind form:\n ${formatFormErrors(formWithErrors)}")
        // Повторный биндинг с NoStrict-формой -- костыль для решения проблем с рендером локализованных
        // дат размещения. Даты форматируются через form.value.dates, а не через form("dates").value, поэтому
        // при ошибке биндинга возникает ситуация, когда form.value не определена, и появляется дырка на странице.
        val formWithErrors1 = agtFormUtil.agtFormTolerant
          .bindFromRequest()
          .copy(
            errors = formWithErrors.errors
          )
        _forAd(formWithErrors1, NotAcceptable)
      },
      {result =>
        LOGGER.trace("Binded: " + result)
        // TODO Прикрутить free adv для суперпользователей.

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
              addRes  <- agtBillUtil.addToOrder(
                orderId     = cart.id.get,
                producerId  = producerId,
                adId        = adId,
                res         = result
              )
            } yield {
              addRes
            }
            // Запустить экшен добавления в корзину на исполнение.
            import slick.driver.api._
            slick.db.run( dbAction.transactionally )
          }

        } yield {
          implicit val messages = implicitly[Messages]
          // Пора вернуть результат работы юзеру: отредиректить в корзину-заказ для оплаты.
          Redirect( routes.LkBill2.cart(producerId, r = Some(routes.LkAdvGeoTag.forAd(adId).url)) )
            .flashing( FLASH.SUCCESS -> messages("Added.n.items.to.cart", itemsAdded.size) )
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
    agtFormUtil.agtFormTolerant.bindFromRequest().fold(
      {formWithErrors =>
        LOGGER.debug(s"getPriceSubmit($adId): Failed to bind form:\n${formatFormErrors(formWithErrors)}")
        val err = MError(
          msg = Some("Failed to bind form")
        )
        NotAcceptable( Json.toJson(err) )
      },
      {result =>
        val advPricingFut = agtBillUtil.computePricing(result)

        implicit val ctx = implicitly[Context]

        // Запустить рассчет стоимости размещаемого
        val pricingFut = for {
          advPricing <- advPricingFut
        } yield {
          val html = _priceValTpl(advPricing)(ctx)
          html2str4json(html)
        }

        // Отрендерить данные по периоду размещения
        val periodReportHtml = html2str4json {
          _reportTpl(result.dates)(ctx)
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
