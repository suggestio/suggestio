package controllers

import com.google.inject.Inject
import controllers.ctag.NodeTagsEdit
import io.suggest.common.empty.EmptyUtil
import io.suggest.mbill2.m.order.MOrderStatuses
import io.suggest.model.geo.GeoPoint
import io.suggest.model.n2.tag.TagSearchUtil
import models.adv.form.MDatesPeriod
import models.adv.geo.tag.{AgtForm_t, MAgtFormResult, MForAdTplArgs}
import models.adv.price.GetPriceResp
import models.jsm.init.MTargets
import models.mctx.Context
import models.merr.MError
import models.mproj.ICommonDi
import models.req.IAdProdReq
import play.api.i18n.Messages
import play.api.libs.json.Json
import play.api.mvc.Result
import util.PlayMacroLogsImpl
import util.acl.{CanAdvertiseAd, CanAdvertiseAdUtil}
import util.adv.AdvFormUtil
import util.adv.geo.{AdvGeoBillUtil, AdvGeoFormUtil, AdvGeoLocUtil}
import util.billing.Bill2Util
import util.tags.TagsEditFormUtil
import views.html.lk.adv.geo._
import views.html.lk.adv.widgets.period._reportTpl
import views.html.lk.lkwdgts.price._priceValTpl

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
  override val tagSearchUtil      : TagSearchUtil,
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


  /** Асинхронный детектор начальной точки для карты георазмещения. */
  private def getGeoPoint0(adId: String)(implicit request: IAdProdReq[_]): Future[GeoPoint] = {
    lazy val logPrefix = s"getGeoPoint0($adId ${System.currentTimeMillis()}):"

    // Ищем начальную точку среди прошлых размещений текущей карточки.
    advGeoLocUtil.getGeoPointFromAdsGeoAdvs( Seq(adId) )
      .map( EmptyUtil.getF )
      // Если не получилось, то ищем начальную точку среди размещений продьюсера карточки.
      .recoverWith { case ex: NoSuchElementException =>
        LOGGER.trace(s"$logPrefix Cannot find old geo points for current ad adv-geo")
        val prodId = request.producer.id.get
        val prodIds = {
          val prodIds0 = List( prodId )
          // Добавляем текущего юзера в качестве продьюсера, вдруг в будущем оно тоже будет работать.
          request.user.personIdOpt.fold(prodIds0)(_ :: prodIds0)
        }
        val fut = advGeoLocUtil.getGeoPointFromProducer(prodIds, adId)
          .map( EmptyUtil.getF )
        fut.onFailure { case ex: NoSuchElementException =>
          LOGGER.trace(s"$logPrefix Cannot find old geo from other producer[$prodId] geo advs")
        }
        fut
      }
      // Ищем начальную точку среди других размещений текущего юзера
      .recoverWith { case ex: NoSuchElementException =>
        request.user.contractIdOptFut
          .flatMap { contractIdOpt =>
            advGeoLocUtil.getGeoPointFromUserGeoAdvs( contractIdOpt.get )
          }
          .map( EmptyUtil.getF )
      }
      // Ищем начальную точку карты из geoip
      .recoverWith { case ex: Throwable =>
        LOGGER.trace(s"$logPrefix Cannot find old geo using user contract.")
        advGeoLocUtil.getGeoPointFromRemoteAddr( request.remoteAddress )
          .map( EmptyUtil.getF )
      }
      // Выставить совсем дефолтовую начальную точку.
      .recover { case ex: Throwable =>
        LOGGER.info(s"$logPrefix Unable to detect geoip=${request.remoteAddress}")
        advGeoLocUtil.getGeoPointLastResort
      }
  }


  /**
    * Экшен рендера страницы размещения карточки в теге с географией.
    *
    * @param adId id отрабатываемой карточки.
    */
  def forAd(adId: String) = CanAdvertiseAdGet(adId, U.Lk, U.ContractId).async { implicit request =>
    // TODO Попытаться заполнить форму с помощью данных из черновиков, если они есть.
    //val draftItemsFut = advGeoBillUtil.findDraftsForAd(adId)
    val gp0Fut = getGeoPoint0(adId)

    val formEmpty = advGeoFormUtil.agtFormStrict

    val formFut = for (gp0 <- gp0Fut) yield {

      val radMapVal = advFormUtil.radMapValue0(gp0)

      // Залить начальные данные в маппинг формы.
      val res = MAgtFormResult(
        tags          = Nil,
        radMapVal     = radMapVal,
        period        = MDatesPeriod(),
        onMainScreen  = true
      )

      formEmpty.fill(res)
    }

    _forAd(formFut, Ok)
  }

  /**
   * common-код экшенов GET'а и POST'а формы forAdTpl.
   *
   * @param formFut Маппинг формы.
   * @param rs Статус ответа HTTP.
   * @return Фьючерс с ответом.
   */
  private def _forAd(formFut: Future[AgtForm_t], rs: Status)
                    (implicit request: IAdProdReq[_]): Future[Result] = {
    lazy val logPrefix = s"_forAd(${request.mad.idOrNull} ${System.currentTimeMillis}):"

    // Собрать данные о текущих гео-размещениях карточки, чтобы их отобразить юзеру на карте.
    val currAdvsFut = slick.db.run {
      advGeoBillUtil.findCurrentForAd(request.mad.id.get)
    }

    val ctxFut = for (ctxData0 <- request.user.lkCtxDataFut) yield {
      implicit val ctxData = ctxData0.copy(
        jsiTgs = Seq(MTargets.AdvGtagForm)
      )
      implicitly[Context]
    }

    val isSuFree = advFormUtil.maybeFreeAdv()
    val advPricingFut = formFut.flatMap { form =>
      advGeoBillUtil.getPricing(form.value, isSuFree)
    }

    // Заварить JSON-кашу из текущих размещений. Они будут скрыто отрендерены в шаблоне.
    val currAdvsJsonFut = for {
      ctx       <- ctxFut
      currAdvs  <- currAdvsFut
    } yield {
      LOGGER.trace(s"$logPrefix Found ${currAdvs.size} current advs")
      val fcoll = advGeoFormUtil.items2geoJson(currAdvs)(ctx)
      Json.toJson(fcoll)
    }

    for {
      ctx           <- ctxFut
      advPricing    <- advPricingFut
      form          <- formFut
      currAdvsJson  <- currAdvsJsonFut
    } yield {

      val rargs = MForAdTplArgs(
        mad           = request.mad,
        producer      = request.producer,
        form          = form,
        price         = advPricing,
        currAdvsJson  = currAdvsJson
      )

      val html = AdvGeoForAdTpl(rargs)(ctx)
      rs(html)
    }
  }


  /**
   * Экшен сабмита формы размещения карточки в теге с географией.
   *
   * @param adId id размещаемой карточки.
   * @return 302 see other, 416 not acceptable.
   */
  def forAdSubmit(adId: String) = CanAdvertiseAdPost(adId, U.Lk, U.PersonNode).async { implicit request =>
    advGeoFormUtil.agtFormStrict.bindFromRequest().fold(
      {formWithErrors =>
        LOGGER.debug(s"forAdSubmit($adId): Failed to bind form:\n ${formatFormErrors(formWithErrors)}")
        // Повторный биндинг с NoStrict-формой -- костыль для решения проблем с рендером локализованных
        // дат размещения. Даты форматируются через form.value.dates, а не через form("dates").value, поэтому
        // при ошибке биндинга возникает ситуация, когда form.value не определена, и появляется дырка на странице.
        val formWithErrors1 = advGeoFormUtil.agtFormTolerant
          .bindFromRequest()
          .copy(
            errors = formWithErrors.errors
          )
        val fweFut = Future.successful(formWithErrors1)
        _forAd(fweFut, NotAcceptable)
      },

      {result =>
        LOGGER.trace("Binded: " + result)
        // TODO Прикрутить free adv для суперпользователей.

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
              cart      <- bill2Util.ensureCart(
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
          if (!isSuFree) {
            implicit val messages = implicitly[Messages]
            // Пора вернуть результат работы юзеру: отредиректить в корзину-заказ для оплаты.
            Redirect(routes.LkBill2.cart(producerId, r = Some(routes.LkAdvGeo.forAd(adId).url)))
              .flashing(FLASH.SUCCESS -> messages("Added.n.items.to.cart", itemsAdded.size))

          } else {
            // Суперюзеры отправляются назад в эту же форму для дальнейшего размещения.
            Redirect( routes.LkAdvGeo.forAd(adId) )
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
    advGeoFormUtil.agtFormTolerant.bindFromRequest().fold(
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

        // Запустить рассчет стоимости размещаемого
        val pricingFut = for {
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
