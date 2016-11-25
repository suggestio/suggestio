package controllers

import akka.stream.scaladsl.Source
import com.google.inject.Inject
import controllers.ctag.NodeTagsEdit
import io.suggest.mbill2.m.order.MOrderStatuses
import io.suggest.model.es.MEsUuId
import io.suggest.model.geo.GeoPoint
import models.adv.form.MDatesPeriod
import models.adv.geo.tag.{AgtForm_t, MAgtFormResult, MForAdTplArgs}
import models.adv.price.GetPriceResp
import models.jsm.init.MTargets
import models.mctx.Context
import models.merr.MError
import models.mproj.ICommonDi
import models.req.IAdProdReq
import play.api.i18n.Messages
import play.api.libs.json.{JsNull, Json}
import play.api.mvc.Result
import util.PlayMacroLogsImpl
import util.acl.{CanAdvertiseAd, CanAdvertiseAdUtil, CanThinkAboutAdvOnMapAdnNode}
import util.adv.AdvFormUtil
import util.adv.geo.{AdvGeoBillUtil, AdvGeoFormUtil, AdvGeoLocUtil, AdvGeoMapUtil}
import util.billing.Bill2Util
import util.lk.LkTagsSearchUtil
import util.tags.TagsEditFormUtil
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
{

  import mCommonDi._


  /** Асинхронный детектор начальной точки для карты георазмещения. */
  private def getGeoPoint0(adId: String)(implicit request: IAdProdReq[_]): Future[GeoPoint] = {
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
  def forAdSubmit(adId: String) = CanAdvertiseAdPost(adId, U.PersonNode).async { implicit request =>
    lazy val logPrefix = s"forAdSubmit($adId):"

    advGeoFormUtil.agtFormStrict.bindFromRequest().fold(
      {formWithErrors =>
        // Запустить инициализацию U.Lk в фоне:
        request.user.lkCtxDataFut
        // Логгировать сразу, чтобы сообщения в логах не перемешивались.
        LOGGER.debug(s"$logPrefix Failed to bind form:\n ${formatFormErrors(formWithErrors)}")
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
  def advRcvrsGeoJson(adId: MEsUuId) = CanAdvertiseAd(adId).async { implicit request =>
    val nodesSource = cache.getOrElse("advGeoNodesSrc", expiration = 10.seconds) {
      advGeoMapUtil.rcvrNodesMap()
    }

    // Сериализуем JSON в поток. Для валидности JSON надо добавить "[" в начале, "]" в конце, и разделители между элементами.
    val delim = ",\n"

    val jsons = nodesSource.mapConcat { m =>
      val jsonStr = Json.stringify(
        Json.toJson( m.toGeoJson )
      )
      jsonStr :: delim :: Nil
    }

    // Собрать итоговый поток сознания.
    // TODO Тут рукописный генератор JSON. Следует задействовать тот, что *вроде бы* есть в akka-http или где-то ещё.
    val src = Source.single( "[" )
      .concat(jsons)
      .concat {
        Source(
          // TODO Чтобы последняя запятая не вызывала ошибки парсинга, добавляем JsNull в конец потока объектов.
          Json.stringify(JsNull) :: "]" :: Nil
        )
      }

    // Вернуть chunked-ответ наконец.
    Ok.chunked(src)
      .as("application/json; charset=utf8")
  }


  /** Рендер попапа при клике по узлу-ресиверу на карте ресиверов.
    *
    * @param nodeId id узла, по которому кликнул юзер.
    * @return HTML.
    */
  def geoNodePopup(adId: MEsUuId, nodeId: MEsUuId) = CanThinkAboutAdvOnMapAdnNode(adId, nodeId = nodeId).async { implicit request =>
    // Отрендерить popup html для узла и вернуть.

    ???
  }

}
