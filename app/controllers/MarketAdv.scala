package controllers

import play.api.Play.{current, configuration}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client
import util.acl._
import models._
import org.joda.time.{DateTime, LocalDate}
import play.api.db.DB
import com.github.nscala_time.time.OrderingImplicits._
import views.html.market.lk.adv._
import util.{TplDataFormatUtil, PlayMacroLogsImpl}
import scala.concurrent.Future
import play.api.data.Form
import play.api.templates.HtmlFormat
import play.api.mvc.{Result, AnyContent}
import java.sql.SQLException
import io.suggest.ym.parsers.Price
import util.billing.MmpDailyBilling, MmpDailyBilling.assertAdvsReqRowsDeleted
import java.util.Currency

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.05.14 18:18
 * Description: Контроллер для управления процессом размещения рекламных карточек с узла на узел:
 * - узел 1 размещает рекламу на других узлах (форма, сабмит и т.д.).
 * - узелы-получатели одобряют или отсеивают входящие рекламные карточки.
 */
object MarketAdv extends SioController with PlayMacroLogsImpl {

  import LOGGER._

  /** Маппинг формы размещения рекламы на других узлах. */
  val advFormM: Form[List[AdvFormEntry]] = {
    import play.api.data._, Forms._
    import util.FormUtil._
    val dateOptM = optional(jodaLocalDate("yyyy-MM-dd"))
    Form(
      "node" -> {
        list(
          tuple(
            "adnId"       -> esIdM,
            "advertise"   -> boolean,
            "onStartPage" -> boolean,
            "dateStart"   -> dateOptM,
            "dateEnd"     -> dateOptM
          )
            .verifying("error.date", { m => m match {
              case (_, isAdv, _, dateStartOpt, dateEndOpt) =>
                // Если стоит галочка, то надо проверить даты.
                if (isAdv) {
                  // Проверить даты
                  val now = DateTime.now()
                  val dateTestF = { d: LocalDate => d.toDateTimeAtStartOfDay isAfter now}
                  dateStartOpt.exists(dateTestF) && dateEndOpt.exists(dateTestF)
                } else {
                  // Галочки нет, пропускаем мимо. На следующем шаге это дело будет отфильтровано.
                  true
                }
              case _ => false
            }})
        )
          .transform[List[AdvFormEntry]](
            {ts =>
              ts.foldLeft(List.empty[AdvFormEntry]) {
                case (acc, (adnId, isAdv @ true, onStartPage, Some(dateStart), Some(dateEnd))) =>
                  val result = AdvFormEntry(adnId = adnId, advertise = isAdv, onStartPage = onStartPage, dateStart = dateStart, dateEnd = dateEnd)
                  result :: acc
                case (acc, _) => acc
              }
            },
            {_.map { e =>
              (e.adnId, e.advertise, e.onStartPage, Option(e.dateStart), Option(e.dateEnd))
            }}
          )
      }
    )
  }


  /** Страница управления размещением рекламной карточки. */
  def advForAd(adId: String) = CanAdvertiseAd(adId).async { implicit request =>
    renderAdvFormFor(adId, advFormM).map { Ok(_) }
  }

  /** Общий для экшенов код подготовки данных и рендера страницы advFormTpl, которая содержит форму размещения. */
  private def renderAdvFormFor(adId: String, form: Form[List[AdvFormEntry]])(implicit request: RequestWithAd[AnyContent]): Future[HtmlFormat.Appendable] = {
    // Запуск асинхронных операций: подготовка списка узлов, на которые можно вообще возможно опубликовать карточку.
    val rcvrsFut = collectReceivers(request.producerId)
    renderAdvFormForRcvrs(adId, form, rcvrsFut)
  }

  private def renderAdvFormForRcvrs(adId: String, form: Form[List[AdvFormEntry]], rcvrsFut: Future[Seq[MAdnNode]])(implicit request: RequestWithAd[AnyContent]): Future[HtmlFormat.Appendable] = {
    // Работа с синхронными моделями.
    val syncResult = DB.withConnection { implicit c =>
      // Собираем всю инфу о размещении этой рекламной карточки
      val advsOk = MAdvOk.findNotExpiredByAdId(adId)
      // Определяем список узлов, которые проходят по adv_ok. Это можно через транзакции и контракты.
      val advsReq = MAdvReq.findByAdId(adId)
      val advsRefused = MAdvRefuse.findByAdId(adId)
      // Для сокрытия узлов, которые не имеют тарифного плана, надо получить список тех, у кого он есть.
      val adnIdsReady = MBillMmpDaily.findAllAdnIds
      // Собираем инфу о заблокированных средствах, относящихся к этой карточке.
      val blockedSums = MAdvReq.calculateBlockedSumForAd(adId)
      (advsOk, advsReq, advsRefused, adnIdsReady, blockedSums)
    }
    val (advsOk, advsReq, advsRefused, adnIdsReady, blockedSums) = syncResult
    val adnIdsReadySet = adnIdsReady.toSet
    trace(s"_advFormFor($adId): advsOk[${advsOk.size}] advsReq[${advsReq.size}] advsRefused[${advsRefused.size}] blockedSums=${blockedSums.mkString(",")}")
    val advs = (advsReq ++ advsRefused ++ advsOk).sortBy(_.dateCreated)
    // Собираем карту adv.id -> rcvrId. Она нужна для сборки карты adv.id -> rcvr.
    val reqAdnIds = advsReq.map {
      advReq  =>  advReq.id.get -> advReq.rcvrAdnId
    }
    val refusedAdnIds = advsRefused.map {
      advRefused  =>  advRefused.id.get -> advRefused.rcvrAdnId
    }
    val okAdnIds = advsOk.map {
      advOk  =>  advOk.id.get -> advOk.rcvrAdnId
    }
    val adv2adnIds: Map[Int, String] = (okAdnIds ++ reqAdnIds ++ refusedAdnIds).toMap
    val busyAdns: Map[String, MAdvI] = {
      val adnAdvsReq = advsReq.map { advReq  =>  advReq.rcvrAdnId -> advReq }
      val adnAdvsOk = advsOk.map { advOk => advOk.rcvrAdnId -> advOk }
      (adnAdvsOk ++ adnAdvsReq).toMap
    }
    for {
      rcvrs <- rcvrsFut
    } yield {
      // Выкинуть узлы, у которых нет своего тарифного плана.
      val rcvrs1 = rcvrs
        .filter { node => adnIdsReadySet contains node.id.get }
      val rcvrsMap = rcvrs1.map { rcvr => rcvr.id.get -> rcvr }.toMap
      // Собираем карту adv.id -> rcvr.
      val adv2adnMap = adv2adnIds.flatMap { case (advId, adnId) =>
        rcvrsMap.get(adnId)
          .fold { List.empty[(Int, MAdnNode)] }  { rcvr => List(advId -> rcvr) }
      }
      // Запускаем рендер шаблона, собрав аргументы в соотв. группы.
      val formArgs = AdvFormTplArgs(adId, rcvrs1, form, busyAdns)
      val currAdvsArgs = CurrentAdvsTplArgs(advs, adv2adnMap, blockedSums)
      advForAdTpl(request.mad, currAdvsArgs, formArgs)
    }
  }


  /**
   * Рассчитать цену размещения. Сюда нужно сабмиттить форму также, как и в [[advFormSubmit()]].
   * @param adId id размещаемой рекламной карточки.
   * @return Инлайновый рендер отображаемой цены.
   */
  def getAdvPriceSubmit(adId: String) = CanAdvertiseAd(adId).async { implicit request =>
    advFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug(s"getAdvPriceSubmit($adId): Failed to bind form:\n${formatFormErrors(formWithErrors)}")
        NotAcceptable("Cannot bind form.")
      },
      {adves =>
        val allRcvrIdsFut = MAdnNode.findIdsByAllAdnRights(Seq(AdnRights.RECEIVER))
          .map { _.filter(_ != request.producerId).toSet }
        val adves1 = filterEntiesByBusyRcvrs(adId, adves)
        allRcvrIdsFut.map { allRcvrIds =>
          val adves2 = filterEntiesByPossibleRcvrs(adves1, allRcvrIds)
          // Начинаем рассчитывать ценник.
          if (adves2.isEmpty) {
            val curr = Currency.getInstance(CurrencyCodeOpt.CURRENCY_CODE_DFLT)
            val prices = List(curr -> 0F)
            Ok(_advFormPriceTpl(prices, hasEnoughtMoney = true))
          } else {
            val syncResult = DB.withConnection { implicit c =>
              val someTrue = Some(true)
              val prices = adves2.foldLeft[List[Price]] (Nil) { (acc, adve) =>
                val rcvrContract = MBillContract.findForAdn(adve.adnId, isActive = someTrue)
                  .sortBy(_.id.get)
                  .head
                val rcvrPricing = MBillMmpDaily.findByContractId(rcvrContract.id.get)
                  .sortBy(_.id.get)
                  .head
                val advPrice = MmpDailyBilling.calculateAdvPrice(request.mad, rcvrPricing, adve)
                advPrice :: acc
              }
              val prices2 = prices
                .groupBy { _.currency.getCurrencyCode }
                .mapValues { p => p.head.currency -> p.map(_.price).sum }
                .values
              val mbb = MBillBalance.getByAdnId(request.producerId).get
              // Если есть разные валюты, то операция уже невозможна.
              val hasEnoughtMoney = prices2.size <= 1 && {
                prices2.headOption.exists { price =>
                  price._1.getCurrencyCode == mbb.currencyCode  &&  price._2 <= mbb.amount
                }
              }
              prices2 -> hasEnoughtMoney
            }
            val (prices, hasEnoughtMoney) = syncResult
            Ok(_advFormPriceTpl(prices, hasEnoughtMoney))
          }
        }
      }
    )
  }


  /** Синхронная фильтрация присланного списка запросов на публикацию по уже размещённым adv.
    * @param adId id размещаемой рекламной карточки.
    * @param adves Результат сабмита формы [[advFormM]].
    * @return Новый adves, который НЕ содержит уже размещаемые карточки.
    */
  private def filterEntiesByBusyRcvrs(adId: String, adves: List[AdvFormEntry]): List[AdvFormEntry] = {
    val syncResult1 = DB.withConnection { implicit c =>
      val advsOk = MAdvOk.findByAdId(adId)
      val advsReq = MAdvReq.findByAdId(adId)
      (advsOk, advsReq)
    }
    val (advsOk, advsReq) = syncResult1
    val busyAdnIds = {
      // Нано-оптимизация: использовать fold для накопления adnId из обоих списков и общую функцию для обоих fold'ов.
      val foldF = { (acc: List[String], e: MAdvI)  =>  e.rcvrAdnId :: acc }
      val acc1 = advsOk.foldLeft(List.empty[String])(foldF)
      advsReq.foldLeft(acc1)(foldF)
        .toSet
    }
    adves.filter { advEntry =>
      val result = !(busyAdnIds contains advEntry.adnId)
      if (!result)
        warn(s"filterEntriesByBusyRcvrs($adId): Dropping submit entry rcvrId=${advEntry.adnId} : Node already is busy by other adv by this adId.")
      result
    }
  }

  /** Фильтрануть список по допустимым ресиверам. */
  private def filterEntiesByPossibleRcvrs(adves: List[AdvFormEntry], allRcvrIds: Set[String]): List[AdvFormEntry] = {
    adves.filter { advEntry =>
      val result = allRcvrIds contains advEntry.adnId
      if (!result)
        warn(s"filterEntriesByPossibleRcvrs(): Dropping submit entry rcvrId=${advEntry.adnId} : Not in available rcvrs set")
      result
    }
  }


  /** Сабмит формы размещения рекламной карточки. */
  def advFormSubmit(adId: String) = CanAdvertiseAd(adId).async { implicit request =>
    lazy val logPrefix = s"advFormSubmit($adId): "
    val formBinded = advFormM.bindFromRequest()
    formBinded.fold(
      {formWithErrors =>
        debug(s"${logPrefix}form bind failed:\n${formatFormErrors(formWithErrors)}")
        renderAdvFormFor(adId, formWithErrors).map(NotAcceptable(_))
      },
      {adves =>
        trace(logPrefix + "adves entries submitted: " + adves)
        // Перед сохранением надо проверить возможности публикации на каждый узел.
        // Получаем в фоне все возможные узлы-ресиверы.
        val allRcvrsFut = collectReceivers(request.producerId)
        val advs1 = filterEntiesByBusyRcvrs(adId, adves)
        allRcvrsFut flatMap { allRcvrs =>
          val allRcvrIds = allRcvrs.map(_.id.get).toSet
          val advs2 = filterEntiesByPossibleRcvrs(advs1, allRcvrIds)
          // Пора сохранять новые реквесты на размещение в базу.
          if (!advs2.isEmpty) {
            try {
              DB.withTransaction { implicit c =>
                val mbb0 = MBillBalance.getByAdnId(request.producerId).get
                val someTrue = Some(true)
                val mbc = MBillContract.findForAdn(request.producerId, isActive = someTrue).head
                val prodCurrencyCode = mbb0.currencyCode
                advs2.foreach { advEntry =>
                  val rcvrContract = MBillContract.findForAdn(advEntry.adnId, isActive = someTrue)
                    .sortBy(_.id.get)
                    .headOption
                    .getOrElse {
                      MBillContract(adnId = advEntry.adnId, contractDate = DateTime.now).save
                    }
                  val rcvrPricing = MBillMmpDaily.findByContractId(rcvrContract.id.get)
                    .sortBy(_.id.get)
                    .head
                  val advPrice = MmpDailyBilling.calculateAdvPrice(request.mad, rcvrPricing, advEntry)
                  val rcvrCurrencyCode = advPrice.currency.getCurrencyCode
                  assert(
                    rcvrCurrencyCode == prodCurrencyCode,
                    s"Rcvr node ${advEntry.adnId} currency ($rcvrCurrencyCode) does not match to producer node ${request.producerId} currency ($prodCurrencyCode)"
                  )
                  MAdvReq(
                    adId = adId,
                    amount = advPrice.price,
                    comission = Some(mbc.sioComission),
                    prodContractId = mbc.id.get,
                    prodAdnId = request.producerId,
                    rcvrAdnId = advEntry.adnId,
                    dateStart = advEntry.dateStart.toDateTimeAtStartOfDay,
                    dateEnd = advEntry.dateEnd.toDateTimeAtStartOfDay,
                    onStartPage = advEntry.onStartPage
                  ).save
                  // Нужно заблокировать на счете узла необходимую сумму денег.
                  mbb0.updateBlocked(advPrice.price)
                }
              }
              Redirect(routes.MarketAdv.advForAd(adId))
                .flashing("success" -> "Запросы на размещение отправлены.")
            } catch {
              case ex: SQLException =>
                warn(s"advFormSumbit($adId): Failed to commit adv transaction for advs:\n " + advs2, ex)
                val formWithErrors = formBinded.withGlobalError("error.no.money")
                renderAdvFormForRcvrs(adId, formWithErrors, allRcvrsFut)
                  .map { NotAcceptable(_) }
            }
          } else {
            Redirect(routes.MarketAdv.advForAd(adId))
              .flashing("success" -> "Без изменений.")
          }
        }
      }
    )
  }


  /** Собрать все узлы сети, пригодные для размещения рекламной карточки. */
  private def collectReceivers(dropRcvrId: String) = {
    MAdnNode.findByAllAdnRights(Seq(AdnRights.RECEIVER))
      // Самому себе через "управление размещением" публиковать нельзя.
      .map { _.filter(_.id.get != dropRcvrId) }
  }


  /**
   * Рендер окна информации для карточки с точки зрения ресивера.
   * @param adId id рекламной карточки
   * @return
   */
  def advInfoWnd(adId: String, fromAdnId: String) = ThirdPartyAdAccess(adId, fromAdnId).apply { implicit request =>
    val syncResult = if(request.isRcvrAccess) {
      DB.withConnection { implicit c =>
        val advsOk = MAdvOk.findByAdIdAndRcvr(adId, fromAdnId)
        val advsReq = MAdvReq.findByAdIdAndRcvr(adId, fromAdnId)
        val advsRefused = MAdvRefuse.findByAdIdAndRcvr(adId, fromAdnId)
        (advsOk, advsReq, advsRefused)
      }
    } else {
      (Nil, Nil, Nil)
    }
    val (advsOk, advsReq, advsRefused) = syncResult
    val advs = advsOk ++ advsReq ++ advsRefused
    Ok(_advInfoWndTpl(request.mad, advs))
  }


  /** Рендер страницы, которая появляется по ссылке-кнопке "рекламодатели". */
  // TODO Вместо IsAdnAdmin надо какой-то IsAdnRcvrAdmin
  def showNodeAdvs(adnId: String) = IsAdnNodeAdmin(adnId).async { implicit request =>
    val advsReq = DB.withConnection { implicit c =>
      MAdvReq.findByRcvr(adnId)
    }
    val adIds = advsReq.map(_.adId).distinct
    val madsFut = MAd.multiGet(adIds)
    val advReqMap = advsReq.map { advReq => advReq.adId -> advReq }.toMap
    madsFut map { mads =>
      val reqsAndMads = mads
        .map { mad =>
          val madId = mad.id.get
          advReqMap(madId) -> mad
        }
        .sortBy(_._1.id.get)
      Ok(nodeAdvsTpl(request.adnNode, reqsAndMads))
    }
  }


  /**
   * Рендер окошка рассмотрения рекламной карточки.
   * @param advReqId id запроса на размещение.
   * @return 404 если что-то не найдено, иначе 200.
   */
  def _showAdvReq(advReqId: Int) = CanReceiveAdvReq(advReqId).async { implicit request =>
    _showAdvReq1(reqRefuseFormM)
      .map { _.fold(identity, Ok(_))}
  }

  private def _showAdvReq1(refuseFormM: Form[String])(implicit request: RequestWithAdvReq[AnyContent]): Future[Either[Result, HtmlFormat.Appendable]] = {
    val madOptFut = MAd.getById(request.advReq.adId)
    val adProducerOptFut = madOptFut flatMap { madOpt =>
      val prodIdOpt = madOpt.map(_.producerId)
      MAdnNodeCache.maybeGetByIdCached(prodIdOpt)
    }
    for {
      adProducerOpt <- adProducerOptFut
      madOpt        <- madOptFut
    } yield {
      if (madOpt.isDefined && adProducerOpt.isDefined) {
        Right(_advReqWndTpl(
          adProducer = adProducerOpt.get,
          adRcvr = request.rcvrNode,
          mad = madOpt.get,
          advReq = request.advReq,
          refuseFormM = refuseFormM
        ))
      } else {
        val advReqId = request.advReq.id.get
        warn(s"_showAdvReq($advReqId): Something not found, but it should: mad=$madOpt producer=$adProducerOpt")
        Left(http404AdHoc)
      }
    }
  }


  /** Маппинг формы отказа от размещения рекламной карточки. Указывать причину надо. */
  private val reqRefuseFormM = {
    import play.api.data.Forms._
    Form(
      "reason" -> nonEmptyText(minLength = 2, maxLength = 256)
    )
  }

  /**
   * Сабмит формы отказа от размещения рекламной карточки.
   * @param advReqId id реквеста.
   * @return 406 если причина слишком короткая или слишком длинная. 302 если всё ок.
   */
  def advReqRefuseSubmit(advReqId: Int) = CanReceiveAdvReq(advReqId).async { implicit request =>
    reqRefuseFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug(s"advReqRefuseSubmit($advReqId): Failed to bind refuse form:\n${formatFormErrors(formWithErrors)}")
        _showAdvReq1(formWithErrors)
          .map { _.fold(identity, NotAcceptable(_)) }
      },
      {reason =>
        val advRefused = MAdvRefuse(request.advReq, reason, DateTime.now)
        val advRefused1 = DB.withTransaction { implicit c =>
          val rowsDeleted = request.advReq.delete
          assertAdvsReqRowsDeleted(rowsDeleted, 1, advReqId)
          advRefused.save
        }
        Redirect(routes.MarketAdv.showNodeAdvs(request.rcvrNode.id.get))
          .flashing("success" -> "Размещение рекламы отменено.")
      }
    )
  }


  /**
   * Юзер одобряет размещение рекламной карточки.
   * @param advReqId id одобряемого реквеста.
   * @return 302
   */
  def advReqAcceptSubmit(advReqId: Int) = CanReceiveAdvReq(advReqId).apply { implicit request =>
    // Надо провести платёж, запилить транзакции для prod и rcvr и т.д.
    MmpDailyBilling.acceptAdvReq(request.advReq, isAuto = false)
    // Всё сохранено. Можно отредиректить юзера, чтобы он дальше продолжил одобрять рекламные карточки.
    Redirect(routes.MarketAdv.showNodeAdvs(request.advReq.rcvrAdnId))
      .flashing("success" -> "Реклама будет размещена.")
  }

}

sealed case class AdvFormEntry(
  adnId: String, advertise: Boolean, onStartPage: Boolean, dateStart: LocalDate, dateEnd: LocalDate
) extends AdvTerms

