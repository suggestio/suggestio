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
import util.PlayMacroLogsImpl
import scala.concurrent.Future
import play.api.templates.HtmlFormat
import play.api.mvc.{Result, AnyContent}
import java.sql.SQLException
import util.billing.MmpDailyBilling, MmpDailyBilling.assertAdvsReqRowsDeleted
import java.util.Currency
import play.api.data._, Forms._

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

  val ADVS_MODE_SELECT_LIMIT = configuration.getInt("adv.short.limit") getOrElse 2

  /** Отдельный маппинг для adv-формы, который парсит исходные данные по бесплатному размещению. */
  private val freeAdvFormM: Form[Option[Boolean]] = Form(
    "freeAdv" -> optional(boolean)
  )

  type AdvFormM_t = Form[List[AdvFormEntry]]

  /** Маппинг формы размещения рекламы на других узлах. */
  private def advFormM(lowerAdvDate: LocalDate): AdvFormM_t = {
    import util.FormUtil._
    Form(
      "node" -> {
        val dateOptM = optional(jodaLocalDate("yyyy-MM-dd"))
        // TODO list mapping не умеет unbind в нашем случае. Надо запилить свой map mapping, который будет биндить форму в карту adnId -> AdvFormEntry.
        list(
          tuple(
            "adnId"         -> esIdM,
            "advertise"     -> boolean,
            "onStartPage"   -> boolean,
            "onRcvrCat"     -> boolean,
            "dateStart"     -> dateOptM,
            "dateEnd"       -> dateOptM
          )
            .verifying("error.date", { m => m match {
              case (_, isAdv, _, _, dateStartOpt, dateEndOpt) =>
                // Если стоит галочка, то надо проверить даты.
                if (isAdv) {
                  // Проверить даты
                  val dateTestF = { d: LocalDate => !(d isBefore lowerAdvDate) }
                  dateStartOpt.exists(dateTestF) && dateEndOpt.exists(dateTestF) && {
                    dateStartOpt exists { dateStart =>
                      dateEndOpt.exists { dateEnd =>
                        !(dateStart isAfter dateEnd)
                      }
                    }
                  }
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
                case (acc, (adnId, isAdv @ true, onStartPage, onRcvrCat, Some(dateStart), Some(dateEnd))) =>
                  var showLevels: List[AdShowLevel] = Nil
                  if (onStartPage)
                    showLevels ::= AdShowLevels.LVL_START_PAGE
                  if (onRcvrCat)
                    showLevels ::= AdShowLevels.LVL_MEMBERS_CATALOG
                  val result = AdvFormEntry(
                    adnId = adnId,
                    advertise = isAdv,
                    showLevels = showLevels.toSet,
                    dateStart = dateStart,
                    dateEnd = dateEnd
                  )
                  result :: acc
                case (acc, _) => acc
              }
            },
            {_.map { e =>
              val onStartPage = e.showLevels contains AdShowLevels.LVL_START_PAGE
              val onRcvrCat = e.showLevels contains AdShowLevels.LVL_MEMBERS_CATALOG
              (e.adnId, e.advertise, onStartPage, onRcvrCat, Option(e.dateStart), Option(e.dateEnd))
            }}
          )
      }
    )
  }


  /** Страница управления размещением рекламной карточки. */
  def advForAd(adId: String) = CanAdvertiseAd(adId).async { implicit request =>
    val now = DateTime.now()
    val lowerAdvDt = if (request.isSuperuser)
      now
    else
      now.plusDays(1)
    val formM = advFormM(lowerAdvDt.toLocalDate)
    renderAdvFormFor(adId, formM)
      .map { Ok(_) }
  }

  /** Общий для экшенов код подготовки данных и рендера страницы advFormTpl, которая содержит форму размещения. */
  private def renderAdvFormFor(adId: String, form: AdvFormM_t)(implicit request: RequestWithAd[AnyContent]): Future[HtmlFormat.Appendable] = {
    // Запуск асинхронных операций: подготовка списка узлов, на которые можно вообще возможно опубликовать карточку.
    val rcvrsFut = collectReceivers(request.producerId)
    renderAdvFormForRcvrs(adId, form, rcvrsFut)
  }

  private def renderAdvFormForRcvrs(adId: String, form: AdvFormM_t, rcvrsFut: Future[Seq[MAdnNode]])
                                   (implicit request: RequestWithAd[AnyContent]): Future[HtmlFormat.Appendable] = {
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
    val adv2adnIds = mkAdv2adnIds(advsReq, advsRefused, advsOk)
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
      val adv2adnMap = mkAdv2adnMap(adv2adnIds, rcvrs1)
      // Запускаем рендер шаблона, собрав аргументы в соотв. группы.
      val formArgs = AdvFormTplArgs(adId, rcvrs1, form, busyAdns)
      val currAdvsArgs = CurrentAdvsTplArgs(advs, adv2adnMap, blockedSums)
      advForAdTpl(request.mad, currAdvsArgs, formArgs)
    }
  }


  private def mkAdv2adnMap(adv2adnIds: Map[Int, String], rcvrs: Seq[MAdnNode]): Map[Int, MAdnNode] = {
    val rcvrsMap = rcvrs.map { rcvr => rcvr.id.get -> rcvr }.toMap
    // Собираем карту adv.id -> rcvr.
    adv2adnIds.flatMap { case (advId, adnId) =>
      rcvrsMap.get(adnId)
        .fold { List.empty[(Int, MAdnNode)] }  { rcvr => List(advId -> rcvr) }
    }
  }


  private def mkAdv2adnIds(advss: List[MAdvI] *): Map[Int, String] = {
    advss.foldLeft [List[(Int, String)]] (Nil) { (acc1, advs) =>
      advs.foldLeft(acc1) { (acc2, e) =>
        e.id.get -> e.rcvrAdnId  ::  acc2
      }
    }.toMap
  }

  private def maybeFreeAdv(implicit request: AbstractRequestWithPwOpt[_]): (Boolean, LocalDate) = {
    val isFree = isFreeAdv( freeAdvFormM.bindFromRequest().fold({_ => None}, identity) )
    val now = DateTime.now
    val lowerDate: DateTime = if (isFree) {
      // Для бесплатного размещения: можно размещать хоть сейчас.
      now
    } else {
      // Для обычного размещения: можно отображаеть карточку в выдаче только с завтрашнего дня
      now.plusDays(1)
    }
    val result = isFree -> lowerDate.toLocalDate
    //trace("maybeFreeAdv(): (isFree, lowerDate) = " + result)
    result
  }

  /**
   * Рассчитать цену размещения. Сюда нужно сабмиттить форму также, как и в advFormSubmit().
   * @param adId id размещаемой рекламной карточки.
   * @return Инлайновый рендер отображаемой цены.
   */
  def getAdvPriceSubmit(adId: String) = CanAdvertiseAd(adId).async { implicit request =>
    val (isFree, lowerDate) = maybeFreeAdv
    advFormM(lowerDate).bindFromRequest().fold(
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
          val advPricing: MAdvPricing = if (adves2.isEmpty || isFree) {
            zeroPricing
          } else {
            MmpDailyBilling.getAdvPrices(request.mad, adves2)
          }
          Ok(_advFormPriceTpl(advPricing))
        }
      }
    )
  }

  /** Нулевая цена, передавая в соотв. шаблон. */
  private val zeroPricing: MAdvPricing = {
    val curr = Currency.getInstance(CurrencyCodeOpt.CURRENCY_CODE_DFLT)
    val prices = List(curr -> 0F)
    MAdvPricing(prices, hasEnoughtMoney = true)
  }


  /** Синхронная фильтрация присланного списка запросов на публикацию по уже размещённым adv.
    * @param adId id размещаемой рекламной карточки.
    * @param adves Результат сабмита формы [[advFormM]].
    * @return Новый adves, который НЕ содержит уже размещаемые карточки.
    */
  private def filterEntiesByBusyRcvrs(adId: String, adves: List[AdvFormEntry]): List[AdvFormEntry] = {
    val syncResult1 = DB.withConnection { implicit c =>
      val advsOk = MAdvOk.findNotExpiredByAdId(adId)
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
    val (isFree, lowerDate) = maybeFreeAdv
    val formBinded = advFormM(lowerDate).bindFromRequest()
    formBinded.fold(
      {formWithErrors =>
        debug(s"${logPrefix}form bind failed:\n${formatFormErrors(formWithErrors)}")
        renderAdvFormFor(adId, formWithErrors)
          .map(NotAcceptable(_))
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
          if (advs2.nonEmpty) {
            try {
              // В зависимости от настроек размещения
              val successMsg: String = if (isFree) {
                MmpDailyBilling.mkAdvsOk(request.mad, advs2)
                "Рекламные карточки отправлены на размещение."
              } else {
                MmpDailyBilling.mkAdvReqs(request.mad, advs2)
                "Запросы на размещение отправлены."
              }
              Redirect(routes.MarketAdv.advForAd(adId))
                .flashing("success" -> successMsg)
            } catch {
              case ex: SQLException =>
                warn(s"advFormSumbit($adId): Failed to commit adv transaction for advs:\n " + advs2, ex)
                // Для бесплатной инжекции: сгенерить экзепшен, чтобы привелегированному юзеру код ошибки отобразился на экране.
                if (isFree) throw ex
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


  /** Запрос к рендеру окошка с полноразмерной превьюшкой карточки и специфичным для конкретной ситуации функционалом.
    * @param adId id рекламной карточки.
    * @param povAdnId Опциональный id узла, с точки зрения которого идёт просмотр карточки.
    * @param advId Опциональный id adv-реквеста, к которому относится запрос. Появился в связи с возможностями
    *              внешней модерации, которая по определению допускает обработку нескольких активных реквестов
    *              для одной карточки.
    * @return Рендер отображения поверх текущей страницы.
    */
  def advFullWnd(adId: String, povAdnId: Option[String], advId: Option[Int], r: Option[String]) = {
    AdvWndAccess(adId, povAdnId, needMBB = false).async { implicit request =>
      if (request.isProducerAdmin) {
        // Узел-продьюсер смотрит инфу по размещению карточки. Нужно отобразить ему список по текущим векторам размещения.
        val limit = ADVS_MODE_SELECT_LIMIT
        val syncResult = DB.withConnection { implicit c =>
          val advsOk  = MAdvOk.findNotExpiredByAdId(adId, limit = limit)
          val advsReq = MAdvReq.findNotExpiredByAdId(adId, limit = limit)
          val advsRefused = MAdvRefuse.findByAdId(adId, limit = limit)
          (advsOk, advsReq, advsRefused)
        }
        val (advsOk, advsReq, advsRefused) = syncResult
        val adv2adnIds = mkAdv2adnIds(advsOk, advsReq, advsRefused)
        // Быстро генерим список с минимальным мусором
        val adnIds = adv2adnIds.valuesIterator.toSet.toSeq
        // Запускаем сбор узлов
        val rcvrsFut = MAdnNodeCache.multiGet(adnIds)
        val advs = advsOk ++ advsReq ++ advsRefused
        rcvrsFut.map { adnNodes =>
          val adn2advMap = mkAdv2adnMap(adv2adnIds, adnNodes)
          Ok(_advWndFullListTpl(request.mad, request.producer, advs, adn2advMap, goBackTo = r))
        }
      } else {
        // Доступ не-продьюсера к чужой рекламной карточке. Это узел-ресивер или узел-модератор, которому делегировали возможности размещения.
        advId.flatMap[MAdvI] { _advId =>
          DB.withConnection { implicit c =>
            MAdvOk.getById(_advId) orElse MAdvReq.getById(_advId)
          }
        }.filter {
          adv  =>  (adv.adId == adId) && (request.rcvrIds contains adv.rcvrAdnId)
        }.fold [Future[Result]] {
          error(s"advFullWnd($adId, pov=$povAdnId): Cannot find adv[$advId] for ad[$adId] and rcvrs = [${request.rcvrIds.mkString(", ")}]")
          InternalServerError("Unexpected situation.")
        } {
          // ok: предложение было одобрено юзером
          case advOk: MAdvOk =>
            Ok(_advWndFullOkTpl(request.mad, request.producer, advOk, goBackTo = r))

          // req: предложение на состоянии модерации. Надо бы отрендерить страницу судьбоносного набега на мозг
          case advReq: MAdvReq =>
            Ok(_advReqWndTpl(request.producer, request.mad, advReq, reqRefuseFormM, goBackTo = r))

          // should never occur
          case other =>
            throw new IllegalArgumentException("Unexpected result from MAdv models: " + other)
        }
      }
    }
  }


  /** Рендер страницы, которая появляется по ссылке-кнопке "рекламодатели". */
  // TODO Вместо IsAdnAdmin надо какой-то IsAdnRcvrAdmin
  def showNodeAdvs(adnId: String) = IsAdnNodeAdmin(adnId).async { implicit request =>
    // Отрабатываем делегирование adv-прав текущему узлу:
    MAdnNode.findIdsAdvDelegatedTo(adnId) flatMap { dgAdnIds =>
      var dgAdnIdsList = dgAdnIds.toList
      // Дописать в начало ещё текущей узел, если он также является рекламо-получателем.
      if (request.adnNode.adn.isReceiver)
        dgAdnIdsList ::= adnId
      // TODO Отрабатывать цепочное делегирование, когда узел делегирует дальше adv-права ещё какому-то узлу.
      val adnIdsSet = dgAdnIdsList.toSet
      // Получаем список реквестов, для всех необходимых ресиверов.
      val advsReq = DB.withConnection { implicit c =>
        MAdvReq.findByRcvrs(adnIdsSet)
      }
      // Список рекламных карточек.
      val adIds = advsReq
        .map(_.adId)
        .distinct
      // В фоне запрашиваем рекламные карточки, которые нужно модерачить.
      val madsFut = MAd.multiGet(adIds)
      // Строим карту adId -> MAdvReq
      val advReqMap = advsReq
        .map { advReq => advReq.adId -> advReq }
        .toMap
      madsFut map { mads =>
        // Выстраиваем порядок рекламных карточек.
        val reqsAndMads = mads
          .map { mad =>
            val madId = mad.id.get
            advReqMap(madId) -> mad
          }
          .sortBy(_._1.id.get)
        Ok(nodeAdvsTpl(request.adnNode, reqsAndMads))
      }
    }
  }


  /**
   * Рендер окошка рассмотрения рекламной карточки.
   * @param advReqId id запроса на размещение.
   * @return 404 если что-то не найдено, иначе 200.
   */
  def _showAdvReq(advReqId: Int, r: Option[String]) = CanReceiveAdvReq(advReqId).async { implicit request =>
    _showAdvReq1(reqRefuseFormM, r)
      .map { _.fold(identity, Ok(_))}
  }

  private def _showAdvReq1(refuseFormM: Form[String], r: Option[String])
                          (implicit request: RequestWithAdvReq[AnyContent]): Future[Either[Result, HtmlFormat.Appendable]] = {
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
          mad = madOpt.get,
          advReq = request.advReq,
          refuseFormM = refuseFormM,
          goBackTo = r
        ))
      } else {
        val advReqId = request.advReq.id.get
        warn(s"_showAdvReq1($advReqId): Something not found, but it should: mad=$madOpt producer=$adProducerOpt")
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
   * @return 406 если причина слишком короткая или слишком длинная.
   *         302 если всё ок.
   */
  def advReqRefuseSubmit(advReqId: Int, r: Option[String]) = CanReceiveAdvReq(advReqId).async { implicit request =>
    reqRefuseFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug(s"advReqRefuseSubmit($advReqId): Failed to bind refuse form:\n${formatFormErrors(formWithErrors)}")
        _showAdvReq1(formWithErrors, r)
          .map { _.fold(identity, NotAcceptable(_)) }
      },
      {reason =>
        val advRefused = MAdvRefuse(request.advReq, reason, DateTime.now)
        DB.withTransaction { implicit c =>
          val rowsDeleted = request.advReq.delete
          assertAdvsReqRowsDeleted(rowsDeleted, 1, advReqId)
          advRefused.save
        }
        RdrBackOr(r) { routes.MarketAdv.showNodeAdvs(request.rcvrNode.id.get) }
          .flashing("success" -> "Размещение рекламы отменено.")
      }
    )
  }

  /**
   * Юзер одобряет размещение рекламной карточки.
   * @param advReqId id одобряемого реквеста.
   * @return 302
   */
  def advReqAcceptSubmit(advReqId: Int, r: Option[String]) = CanReceiveAdvReq(advReqId).apply { implicit request =>
    // Надо провести платёж, запилить транзакции для prod и rcvr и т.д.
    MmpDailyBilling.acceptAdvReq(request.advReq, isAuto = false)
    // Всё сохранено. Можно отредиректить юзера, чтобы он дальше продолжил одобрять рекламные карточки.
    RdrBackOr(r) { routes.MarketAdv.showNodeAdvs(request.advReq.rcvrAdnId) }
      .flashing("success" -> "Реклама будет размещена.")
  }


  /** На основе маппинга формы и сессии суперюзера определить, как размещать рекламу:
    * бесплатно инжектить или за деньги размещать. */
  private def isFreeAdv(isFreeOpt: Option[Boolean])(implicit request: AbstractRequestWithPwOpt[_]): Boolean = {
    isFreeOpt
      .fold(false) { _ && request.isSuperuser }
  }

}

sealed case class AdvFormEntry(
  adnId: String, advertise: Boolean, showLevels: Set[AdShowLevel], dateStart: LocalDate, dateEnd: LocalDate
) extends AdvTerms

