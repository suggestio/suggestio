package controllers

import com.google.inject.Inject
import io.suggest.event.SioNotifierStaticClientI
import io.suggest.model.OptStrId
import io.suggest.playx.ICurrentConf
import io.suggest.ym.model.common.EMAdNetMember
import models.adv.geo.{ReqInfo, AdvFormEntry, WndFullArgs}
import org.elasticsearch.client.Client
import org.joda.time.format.ISOPeriodFormat
import play.api.i18n.MessagesApi
import play.twirl.api.Html
import util.acl._
import models._
import org.joda.time.{Period, LocalDate}
import play.api.db.Database
import com.github.nscala_time.time.OrderingImplicits._
import util.adv.CtlGeoAdvUtil
import util.async.AsyncUtil
import util.lk.LkAdUtil
import util.showcase.ShowcaseUtil
import util.xplay.SioHttpErrorHandler
import views.html.lk.adv._
import util.PlayMacroLogsImpl
import scala.concurrent.{ExecutionContext, Future}
import play.api.mvc.{Result, AnyContent}
import java.sql.SQLException
import util.billing.MmpDailyBilling
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
class MarketAdv @Inject() (
  override val messagesApi      : MessagesApi,
  override val current          : play.api.Application,
  implicit val db               : Database,
  override implicit val ec      : ExecutionContext,
  implicit val esClient         : Client,
  override implicit val sn      : SioNotifierStaticClientI
)
  extends SioControllerImpl
  with PlayMacroLogsImpl
  with ICurrentConf
{

  import LOGGER._

  private type AdvFormValueM_t = List[AdvFormEntry]
  private type AdvFormM_t = Form[AdvFormValueM_t]

  val ADVS_MODE_SELECT_LIMIT = configuration.getInt("adv.short.limit") getOrElse 2

  /** Отдельный маппинг для adv-формы, который парсит исходные данные по бесплатному размещению. */
  private def freeAdvFormM: Form[Option[Boolean]] = Form(
    "freeAdv" -> optional(boolean)
  )

  /** Значение поля node[].period.period в случае, когда юзер хочет вручную задать даты начала и окончания. */
  val CUSTOM_PERIOD = "custom"

  /** Маппинг для вертикальных уровней отображения. */
  private def adSlsM: Mapping[Set[AdShowLevel]] = {
    val b = boolean
    mapping(
      "onStartPage" -> b,
      "onRcvrCat"   -> b
    )
    {(onStartPage, onRcvrCat) =>
      var acc = List[AdShowLevel]( AdShowLevels.LVL_PRODUCER )
      if (onStartPage)
        acc ::= AdShowLevels.LVL_START_PAGE
      if (onRcvrCat)
        acc ::= AdShowLevels.LVL_CATS
      acc.toSet
    }
    {adSls =>
      val onStartPage = adSls contains AdShowLevels.LVL_START_PAGE
      val onRcvrCat = adSls contains AdShowLevels.LVL_CATS
      Some((onStartPage, onRcvrCat))
    }
  }

  private type DatePeriod_t = (LocalDate, LocalDate)
  private type DatePeriodOpt_t = Option[DatePeriod_t]

  /** Маппинг для интервала дат размещения. Его точно нельзя заворачивать в val из-за LocalDate.now(). */
  private def advDatePeriodOptM: Mapping[DatePeriodOpt_t] = {
    // option используется, чтобы избежать ошибок маппинга, если галочка isAdv убрана для текущего ресивера, и дата не выставлена одновременно.
    // TODO Неправильно введённые даты надо заворачивать в None.
    val dateOptM = optional( jodaLocalDate("yyyy-MM-dd") )
    tuple(
      "start" -> dateOptM
        .verifying("error.date.start.before.today", {dOpt => dOpt match {
          case Some(d)  => !d.isBefore(LocalDate.now)
          case None     => true
        }}),
      "end"   -> dateOptM
    )
    .transform [Option[(LocalDate, LocalDate)]] (
      {case (Some(dateStart), Some(dateEnd))  =>  Some(dateStart -> dateEnd)
       case _  =>  None },
      {case Some((dateStart, dateEnd))  =>  Some(dateStart) -> Some(dateEnd)
       case None  =>  None -> None }
    )
  }

  /** Форма исповедует select, который имеет набор предустановленных интервалов, а также имеет режим задания дат вручную. */
  private def advPeriodM: Mapping[DatePeriod_t] = {
    tuple(
      "period" -> nonEmptyText(minLength = 1, maxLength = 10)
        .transform [Option[QuickAdvPeriod]] (
          {case CUSTOM_PERIOD => None
           case periodRaw => QuickAdvPeriods.maybeWithName(periodRaw) },
          { _.fold(CUSTOM_PERIOD)(_.isoPeriod) }
        )
      ,
      "date"  -> advDatePeriodOptM
    )
    .verifying("error.required", { m => m match {
      case (periodOpt, datesOpt)  =>  periodOpt.isDefined || datesOpt.isDefined
    }})
      // Проверяем даты у тех, у кого выставлены галочки. end должна быть не позднее start.
    .verifying("error.date.end.before.start", { m => m match {
       // Если даты имеют смысл, то они заданы, и их проверяем.
       case (None, Some((dateStart, dateEnd)))    => !(dateStart isAfter dateEnd)
       // Остальные случаи не отрабатываем - смысла нет.
       case _ => true
    }})

    .transform [DatePeriod_t] (
      // В зависимости от имеющихся значений полей выбираем реальный период.
      { case (Some(qap), _) =>
          val now = LocalDate.now()
          now -> now.plus( qap.toPeriod.minusDays(1) )
        case (_, dpo) =>
          dpo.get
      },
      // unapply(). Нужно попытаться притянуть имеющийся интервал дат на какой-то период из списка QuickAdvPeriod.
      // При неудаче вернуть кастомный период.
      {case dp @ (dateStart, dateEnd) =>
        // Угадываем период либо откатываемся на custom_period
        val periodStr = new Period(dateStart, dateEnd).toString(ISOPeriodFormat.standard())
        QuickAdvPeriods.maybeWithName(periodStr) match {
          case Some(qap)  =>  Some(qap) -> None
          case None       =>  None -> Some(dp)
        }
      }
    )
  }

  /** Маппинг формы размещения рекламы на других узлах. */
  private def advFormM: AdvFormM_t = {
    import util.FormUtil._
    val nodesM = list(
      tuple(
        "adnId"         -> esIdM,
        "advertise"     -> boolean,
        "showLevel"     -> adSlsM
      )
    )
    Form[AdvFormValueM_t] (
      mapping(
        "node"   -> nodesM,
        "period" -> advPeriodM
      )
      {(nodesAdv, advPeriod) =>
        nodesAdv.foldLeft(List.empty[AdvFormEntry]) {
          case (acc, (adnId, isAdv @ true, adSls) ) =>
            val ssls = for(sl <- adSls) yield {
              SinkShowLevels.withArgs(AdnSinks.SINK_GEO, sl)
            }
            val result = AdvFormEntry(
              adnId       = adnId,
              advertise   = isAdv,
              showLevels  = ssls,
              dateStart   = advPeriod._1,
              dateEnd     = advPeriod._2
            )
            result :: acc
          case (acc, _) => acc
        }
      }
      {l =>
        l.headOption.map { first =>
          val nodesAdvs = l.map { e =>
            val adSls = e.showLevels.map(_.sl)
            (e.adnId, e.advertise, adSls)
          }
          val dates = first.dateStart -> first.dateEnd
          nodesAdvs -> dates
        }
      }
    )
  }


  /** Страница управления размещением рекламной карточки. */
  def advForAd(adId: String) = CanAdvertiseAdGet(adId).async { implicit request =>
    renderAdvForm(adId, advFormM)
      .map { Ok(_) }
  }

  /** Класс-контейнер для передачи результатов ряда операций с adv/bill-sql-моделями в renderAdvForm(). */
  private case class AdAdvInfoResult(advsOk: List[MAdvOk], advsReq: List[MAdvReq], advsRefused: List[MAdvRefuse])

  /**
   * Очень асинхронно прочитать инфу по текущим размещениям карточки, вернув контейнер результатов.
   * @param adId id рекламной карточки.
   * @param limit Необязательный лимит.
   * @return Фьючерс с контейнером результатов.
   */
  private def getAdAdvInfo(adId: String, limit: Int = CtlGeoAdvUtil.LIMIT_DFLT): Future[AdAdvInfoResult] = {
    val advsOkFut  = CtlGeoAdvUtil.advFindNonExpiredByAdId(MAdvOk, adId, limit)
    val advsReqFut = CtlGeoAdvUtil.advFindByAdId(MAdvReq, adId, limit)
    for {
      advsRefused     <- CtlGeoAdvUtil.advFindByAdId(MAdvRefuse, adId, limit)
      advsOk          <- advsOkFut
      advsReq         <- advsReqFut
    } yield {
      AdAdvInfoResult(advsOk, advsReq, advsRefused)
    }
  }

  private def toAdvswArgs(adAdvInfo: AdAdvInfoResult, rcvrs: Seq[MAdnNode]): CurrentAdvsTplArgs = {
    import adAdvInfo._
    val advs = (advsReq ++ advsRefused ++ advsOk).sortBy(_.dateCreated)
    val adv2adnIds = mkAdv2adnIds(advsReq, advsRefused, advsOk)
    val adv2adnMap = mkAdv2adnMap(adv2adnIds, rcvrs)
    CurrentAdvsTplArgs(advs, adv2adnMap)
  }

  /**
   * Рендер страницы с формой размещения. Сбор и подготовка данных для рендера идёт очень параллельно.
   * @param adId id рекламной карточки.
   * @param form Форма размещения рекламной карточки.
   * @param rcvrsAllFutOpt Опционально: асинхронный список ресиверов. Экшен контроллера может передавать его сюда.
   * @return Отрендеренная страница управления карточкой с формой размещения.
   */
  private def renderAdvForm(adId: String, form: AdvFormM_t, rcvrsAllFutOpt: Option[Future[Seq[MAdnNode]]] = None)
                           (implicit request: RequestWithAdAndProducer[AnyContent]): Future[Html] = {
    // Если поиск ресиверов ещё не запущен, то сделать это.
    val rcvrsAllFut = rcvrsAllFutOpt  getOrElse  collectAllReceivers(request.producer)
    // В фоне строим карту ресиверов, чтобы по ней быстро ориентироваться.
    val allRcvrsMapFut = rcvrsAllFut map { rcvrs =>
      rcvrs.iterator
        .map { rcvr  =>  rcvr.id.get -> rcvr }
        .toMap
    }

    // Для сокрытия узлов, которые не имеют тарифного плана, надо получить список тех, у кого он есть.
    val adnIdsReadyFut = CtlGeoAdvUtil.findAdnIdsMmpReady()

    // Работа с синхронными моделями: собрать инфу обо всех размещениях текущей рекламной карточки.
    val adAdvInfoFut = getAdAdvInfo(adId)

    // Сразу запускаем в фоне генерацию старого формата передачи ресиверов в шаблон.
    val rcvrsReadyFut = for {
      adnIdsReady <- adnIdsReadyFut
      rcvrs       <- rcvrsAllFut
    } yield {
      val adnIdsReadySet = adnIdsReady.toSet
      // Выкинуть узлы, у которых нет своего тарифного плана.
      // TODO Нельзя публиковать прямо в городах. Нужно фильтровать тут и при сабмите.
      rcvrs filter { node => adnIdsReadySet contains node.id.get }
    }

    // Нужно заодно собрать карту (adnId -> Int), которая отражает целочисленные id узлов в list-маппинге.
    val adnId2indexMapFut: Future[Map[String, Int]] = rcvrsReadyFut map { rcvrs =>
      // Карта строится на основе данных из исходной формы и дополняется недостающими adn_id.
      val formIndex2adnIdMap0 = form("node").indexes
        .flatMap { fi => form(s"node[$fi].adnId").value.map(fi -> _) }
        .toMap
      val missingRcvrIds = rcvrs.flatMap(_.id).toSet -- formIndex2adnIdMap0.valuesIterator
      // Делаем источник допустимых index'ов, которые могут быть в list-mapping'е.
      val indexesIter = (1 to Int.MaxValue)   // TODO с нуля начинать надо отсчет или с единицы?
        .iterator
        .filterNot(formIndex2adnIdMap0.contains)
      // Итератор по недостающим элементам карты.
      val missingResIter = missingRcvrIds
        .iterator
        .map { adnId => adnId -> indexesIter.next }
      // Итератор по готовым данным, уже забитых в форме.
      val existsingIter = formIndex2adnIdMap0
        .iterator
        .map { case (i, adnId)  =>  adnId -> i }
      // Склеиваем итераторы и делаем из них финальную неизменяемую карту.
      (missingResIter ++ existsingIter).toMap
    }

    // Кешируем определённый язык юзера прямо тут. Это нужно для обращения к Messages().
    implicit val ctx = implicitly[Context]
    // Строим набор городов и их узлов, сгруппированных по категориям.
    val citiesFut: Future[Seq[AdvFormCity]] = for {
      rcvrs     <- rcvrsReadyFut
      rcvrsMap  <- allRcvrsMapFut
    } yield {
      rcvrs
        .groupBy { rcvr =>
          findFirstGeoParentOfType(rcvr, AdnShownTypes.TOWN, rcvrsMap)
            .flatMap(_.id)
            .getOrElse("")
        }
        .iterator
        .filter(!_._1.isEmpty)
        .map { case (cityId, cityNodes) =>
          val cityNode = rcvrsMap(cityId)
          val cats = cityNodes.groupBy(_.adn.shownTypeId)
            .iterator
            .zipWithIndex
            .map { case ((shownTypeId, catNodes), i) =>
              val ast = AdnShownTypes.shownTypeId2val(shownTypeId)
              AdvFormCityCat(
                shownType = ast,
                nodes = catNodes.map(AdvFormNode.apply),
                name = ctx.messages(ast.pluralNoTown),
                i = i
              )
            }
            .toSeq
            .sortBy(_.name)
          (cityNode, cats)
        }
        .zipWithIndex
        .map { case ((cityNode, cats), i)  =>  AdvFormCity(cityNode, cats, i) }
        .toSeq
        .sortBy(_.node.meta.name)
    }

    // Строим карту уже занятых какими-то размещением узлы.
    val busyAdvsFut: Future[Map[String, MAdvI]] = adAdvInfoFut.map { adAdvInfo =>
      import adAdvInfo._
      val adnAdvsReq = advsReq.map { advReq  =>  advReq.rcvrAdnId -> advReq }
      val adnAdvsOk = advsOk.map { advOk => advOk.rcvrAdnId -> advOk }
      (adnAdvsOk ++ adnAdvsReq).toMap
    }

    // Периоды размещения. Обычно одни и те же. Сразу считаем в текущем потоке:
    val advPeriodsAvailable = (QuickAdvPeriods.ordered.map(_.isoPeriod) ++ List(CUSTOM_PERIOD))
      .map(ps => ps -> ctx.messages("adv.period." + ps))

    // Сборка финального контейнера аргументов для _advFormTpl().
    val advFormTplArgsFut: Future[AdvFormTplArgs] = for {
      adnId2indexMap  <- adnId2indexMapFut
      cities          <- citiesFut
      busyAdvs        <- busyAdvsFut
    } yield {
      AdvFormTplArgs(
        adId            = adId,
        af              = form,
        busyAdvs        = busyAdvs,
        cities          = cities,
        adnId2formIndex = adnId2indexMap,
        advPeriodsAvail = advPeriodsAvailable
      )
    }

    // Когда всё станет готово - рендерим результат.
    for {
      formArgs      <- advFormTplArgsFut
    } yield {
      // Запускаем рендер шаблона, собрав аргументы в соотв. группы.
      advForAdTpl(request.mad, request.producer, formArgs)(ctx)
    }
  }


  /** Используя дерево гео-связей нужно найти родительский узел, имеющий указанный shownType. */
  private def findFirstGeoParentOfType(node: MAdnNode, parentShowType: AdnShownType, nodes: Map[String, MAdnNode]): Option[MAdnNode] = {
    // Поднимаемся наверх по иерархии гео-родительства.
    val iter = node.geo
      .directParentIds
      .iterator // Для ленивого обхода коллекции используем итератор. Нам по факту нужен только первый подходящий узел.
      .flatMap(nodes.get)
      .flatMap { parentNode =>
        if (parentNode.adn.shownTypeId == parentShowType.name) {
          // Текущий узел имеет искомый id типа
          Some(parentNode)
        } else {
          findFirstGeoParentOfType(parentNode, parentShowType, nodes)
        }
      }
    // Берём первый элемент итератора (если есть), имитируя работу headOption() через if-else.
    if (iter.hasNext) {
      Some(iter.next())
    } else {
      None
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

  private def maybeFreeAdv(implicit request: AbstractRequestWithPwOpt[_]): Boolean = {
    // Раньше было ограничение на размещение с завтрашнего дня, теперь оно снято.
    val isFree = isFreeAdv( freeAdvFormM.bindFromRequest().fold({_ => None}, identity) )
    isFree
  }

  /**
   * Рассчитать цену размещения. Сюда нужно сабмиттить форму также, как и в advFormSubmit().
   * @param adId id размещаемой рекламной карточки.
   * @return Инлайновый рендер отображаемой цены.
   */
  def getAdvPriceSubmit(adId: String) = CanAdvertiseAdPost(adId).async { implicit request =>
    advFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug(s"getAdvPriceSubmit($adId): Failed to bind form:\n${formatFormErrors(formWithErrors)}")
        NotAcceptable("Cannot bind form.")
      },
      {adves =>
        val allRcvrIdsFut = collectAllReceivers(request.producer)
          .map { _.iterator.flatMap(_.id).toSet }
        val adves2Fut = for {
          adves1      <- filterEntiesByBusyRcvrs(adId, adves)
          allRcvrIds  <- allRcvrIdsFut
        } yield {
          filterEntiesByPossibleRcvrs(adves1, allRcvrIds)
        }
        adves2Fut.map { adves2 =>
          // Начинаем рассчитывать ценник.
          val advPricing: MAdvPricing = if (adves2.isEmpty || maybeFreeAdv) {
            zeroPricing
          } else {
            MmpDailyBilling.getAdvPrices(request.mad, adves2)
          }
          Ok(_advFormPriceTpl(advPricing))
        }(AsyncUtil.jdbcExecutionContext)
      }
    )
  }

  /** Нулевая цена, передавая в соотв. шаблон. */
  private def zeroPricing: MAdvPricing = {
    val curr = Currency.getInstance(CurrencyCodeOpt.CURRENCY_CODE_DFLT)
    val prices = List(curr -> 0F)
    MAdvPricing(prices, hasEnoughtMoney = true)
  }


  /** Синхронная фильтрация присланного списка запросов на публикацию по уже размещённым adv.
    * @param adId id размещаемой рекламной карточки.
    * @param adves Результат сабмита формы [[advFormM]].
    * @return Новый adves, который НЕ содержит уже размещаемые карточки.
    */
  private def filterEntiesByBusyRcvrs(adId: String, adves: List[AdvFormEntry]): Future[List[AdvFormEntry]] = {
    val advsResultFut = Future {
      db.withConnection { implicit c =>
        val advsOk = MAdvOk.findNotExpiredByAdId(adId)
        val advsReq = MAdvReq.findByAdId(adId)
        (advsOk, advsReq)
      }
    }(AsyncUtil.jdbcExecutionContext)
    advsResultFut.map { syncResult1 =>
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
  def advFormSubmit(adId: String) = CanAdvertiseAdPost(adId).async { implicit request =>
    lazy val logPrefix = s"advFormSubmit($adId): "
    val formBinded = advFormM.bindFromRequest()
    formBinded.fold(
      {formWithErrors =>
        debug(s"${logPrefix}form bind failed:\n${formatFormErrors(formWithErrors)}")
        renderAdvForm(adId, formWithErrors)
          .map(NotAcceptable(_))
      },
      {adves =>
        trace(logPrefix + "adves entries submitted: " + adves)
        // Перед сохранением надо проверить возможности публикации на каждый узел.
        // Получаем в фоне все возможные узлы-ресиверы.
        val allRcvrsFut = collectAllReceivers(request.producer)
        val advs2Fut = for {
          advs1     <- filterEntiesByBusyRcvrs(adId, adves)
          allRcvrs  <- allRcvrsFut
        } yield {
          val allRcvrIds = allRcvrs.iterator.map(_.id.get).toSet
          filterEntiesByPossibleRcvrs(advs1, allRcvrIds)
        }
        advs2Fut.flatMap { advs2 =>
          // Пора сохранять новые реквесты на размещение в базу.
          if (advs2.nonEmpty) {
            val isFree = maybeFreeAdv
            try {
              // В зависимости от настроек размещения
              val successMsg: String = if (isFree) {
                MmpDailyBilling.mkAdvsOk(request.mad, advs2)
                "Ads.were.adv"
              } else {
                MmpDailyBilling.mkAdvReqs(request.mad, advs2)
                "Adv.reqs.sent"
              }
              Redirect(routes.MarketAdv.advForAd(adId))
                .flashing(FLASH.SUCCESS -> successMsg)
            } catch {
              case ex: SQLException =>
                warn(s"advFormSumbit($adId): Failed to commit adv transaction for advs:\n " + advs2, ex)
                // Для бесплатной инжекции: сгенерить экзепшен, чтобы привелегированному юзеру код ошибки отобразился на экране.
                if (isFree) throw ex
                val formWithErrors = formBinded.withGlobalError("error.no.money")
                renderAdvForm(adId, formWithErrors, Some(allRcvrsFut))
                  .map { NotAcceptable(_) }
            }
          } else {
            Redirect(routes.MarketAdv.advForAd(adId))
              .flashing(FLASH.SUCCESS -> "No.changes")
          }
        }(AsyncUtil.jdbcExecutionContext)
      }
    )
  }


  /** Собрать все узлы сети, пригодные для размещения рекламной карточки. */
  private def collectAllReceivers(myNode: OptStrId with EMAdNetMember) = {
    // TODO Этот запрос возвращает огромный список нод, которые рендерятся в огромный список. Надо переверстать эти шаблоны.
    val nodesFut = MAdnNode.findByAllAdnRights(
      Seq(AdnRights.RECEIVER),
      withoutTestNodes = !myNode.adn.testNode,
      maxResults = 500
    )
    // Самому себе через "управление размещением" публиковать нельзя.
    nodesFut.map { nodes =>
      val dropRcvrId = myNode.id.get
      nodes.filter {
        _.id.get != dropRcvrId
      }
    }
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
      implicit val ctx = implicitly[Context]
      // Запуск сборки данных по фоновой картинке.
      val brArgsFut = ShowcaseUtil.focusedBrArgsFor(request.mad)(ctx)
      val wndFullArgsFut = brArgsFut map { brArgs =>
        WndFullArgs(
          producer    = request.producer,
          brArgs      = brArgs,
          goBackTo    = r
        )
      }
      // Есть разные варианты отображения в зависимости от роли. TODO Распилить сий велосипед это на два экшена.
      if (request.isProducerAdmin) {
        // Узел-продьюсер смотрит инфу по размещению карточки. Нужно отобразить ему список по текущим векторам размещения.
        val limit = ADVS_MODE_SELECT_LIMIT

        // Параллельные вызовы к моделям размещений.
        val advsOkFut = Future {
          db.withConnection { implicit c =>
            MAdvOk.findNotExpiredByAdId(adId, limit = limit)
          }
        }(AsyncUtil.jdbcExecutionContext)

        val advsReqFut = Future {
          db.withConnection { implicit c =>
            MAdvReq.findNotExpiredByAdId(adId, limit = limit)
          }
        }(AsyncUtil.jdbcExecutionContext)

        val advsRefusedFut = Future {
          db.withConnection { implicit c =>
            MAdvRefuse.findByAdId(adId, limit = limit)
          }
        }(AsyncUtil.jdbcExecutionContext)

        val adv2adnIdsFut = for {
          advsOk       <- advsOkFut
          advsReq      <- advsReqFut
          advsRefused  <- advsRefusedFut
        } yield {
          mkAdv2adnIds(advsOk, advsReq, advsRefused)
        }

        // Запускаем сбор узлов
        val rcvrsFut = adv2adnIdsFut flatMap { adv2adnIds =>
          val adnIds = adv2adnIds.valuesIterator.toSet
          MAdnNodeCache.multiGet(adnIds)
        }

        // Объединение всех списков
        val advsFut = for {
          advsOk       <- advsOkFut
          advsReq      <- advsReqFut
          advsRefused  <- advsRefusedFut
        } yield {
          val allIter = advsOk.iterator ++ advsReq.iterator ++ advsRefused.iterator
          allIter.toSeq
        }

        val adn2advMapFut = for {
          adv2adnIds <- adv2adnIdsFut
          rcvrs      <- rcvrsFut
        } yield {
          mkAdv2adnMap(adv2adnIds, rcvrs)
        }

        for {
          adn2advMap  <- adn2advMapFut
          advs        <- advsFut
          wndArgs     <- wndFullArgsFut
        } yield {
          val render = _advWndFullListTpl(wndArgs, advs, adn2advMap)(ctx)
          Ok(render)
        }

      } else {
        // Доступ не-продьюсера к чужой рекламной карточке. Это узел-ресивер или узел-модератор, которому делегировали возможности размещения.
        val advOptFut = advId.fold [Future[Option[MAdvI]]] (Future successful None) { _advId =>
          Future {
            db.withConnection { implicit c =>
              MAdvOk.getById(_advId) orElse MAdvReq.getById(_advId)
            }
          }(AsyncUtil.jdbcExecutionContext)
        }
        for {
          advOpt  <- advOptFut
          wndArgs <- wndFullArgsFut
        } yield {
          advOpt.filter { adv =>
            (adv.adId == adId) && (request.rcvrIds contains adv.rcvrAdnId)
          }.fold [Result] {
              error(s"advFullWnd($adId, pov=$povAdnId): Cannot find adv[$advId] for ad[$adId] and rcvrs = [${request.rcvrIds.mkString(", ")}]")
              InternalServerError("Unexpected situation.")
          } {
            // ok: предложение было одобрено юзером
            case advOk: MAdvOk =>
              Ok( _advWndFullOkTpl(wndArgs, advOk)(ctx) )

            // req: предложение на состоянии модерации. Надо бы отрендерить страницу судьбоносного набега на мозг
            case advReq: MAdvReq =>
              Ok( _advReqWndTpl(wndArgs, advReq, reqRefuseFormM)(ctx) )

            // should never occur
            case other =>
              throw new IllegalArgumentException("Unexpected result from MAdv models: " + other)
          }
        }
      }
    }
  }


  /** Рендер страницы, которая появляется по ссылке-кнопке "рекламодатели". */
  // TODO Вместо IsAdnAdmin надо какой-то IsAdnRcvrAdmin
  def showNodeAdvs(adnId: String) = IsAdnNodeAdmin(adnId).async { implicit request =>
    // Отрабатываем делегирование adv-прав текущему узлу:
    val dgAdnIdsFut: Future[Set[String]] = {
      // TODO N2 реализовать выборку id узлов, от которых делегировано право этому узлу в модерации запросов размещения.
      LOGGER.warn(s"showNodeAdvs($adnId): adv delegation not implemened")
      /*for {
        edges <- MEdge.dynSearch( FindEdgesByAdvDelegate(adnId) )
      } yield {
        var iter = edges.iterator
          .map { _.fromId }
        // Дописать в начало ещё текущей узел, если он также является рекламо-получателем.
        if (request.adnNode.adn.isReceiver)
          iter ++= Iterator(adnId)
        iter.toSet
        */
      Future successful Set.empty
    }

    // TODO Отрабатывать цепочное делегирование, когда узел делегирует дальше adv-права ещё какому-то узлу.
    val advsReqFut = dgAdnIdsFut.map { adnIdsSet =>
      // Получаем список реквестов, для всех необходимых ресиверов.
      db.withConnection { implicit c =>
        MAdvReq.findByRcvrs(adnIdsSet)
      }
    }(AsyncUtil.jdbcExecutionContext)

    // Список рекламных карточек для отображения.
    val madsFut = advsReqFut.flatMap { advsReq =>
      val adIds = advsReq
        .map(_.adId)
        .distinct
      // В фоне запрашиваем рекламные карточки, которые нужно модерачить.
      MAd.multiGetRev(adIds)
    }
    // Строим карту adId -> MAdvReq
    val advsReqMapFut = advsReqFut map { advsReq =>
      advsReq
        .map { advReq => advReq.adId -> advReq }
        .toMap
    }

    implicit val ctx = implicitly[Context]
    // Собираем данные по рендеру блоков карточек.
    val devScreenOpt = ctx.deviceScreenOpt
    val brArgsMapFut = madsFut.flatMap { mads =>
      Future.traverse(mads) { mad =>
        LkAdUtil.tiledAdBrArgs(mad, devScreenOpt)
          .map { brArgs => mad.id.get -> brArgs }
      }
        .map { _.toMap }
    }

    // Объединяем все карты данных по карточкам.
    val infosFut = for {
      advsReqMap <- advsReqMapFut
      mads       <- madsFut
      brArgsMap  <- brArgsMapFut
    } yield {
      val madsRich = mads.flatMap { mad =>
        for {
          madId  <- mad.id
          brArgs <- brArgsMap.get(madId)
          advReq <- advsReqMap.get(madId)
        } yield {
          ReqInfo(advReq, brArgs)
        }
      }
      // Выстраиваем порядок рекламных карточек на основе порядка запросов размещения: новые сверху.
      madsRich.sortBy { info =>
        info.req.id match {
          case Some(id) => -id
          case None     => Int.MaxValue
        }
      }
    }

    // Рендер результата.
    infosFut map { infos =>
      Ok( nodeAdvsTpl(request.adnNode, infos)(ctx) )
    }
  }


  /**
   * Рендер окошка рассмотрения рекламной карточки.
   * @param advReqId id запроса на размещение.
   * @return 404 если что-то не найдено, иначе 200.
   */
  def _showAdvReq(advReqId: Int, r: Option[String]) = CanReceiveAdvReqGet(advReqId).async { implicit request =>
    _showAdvReq1(reqRefuseFormM, r)
      .map { _.fold(identity, Ok(_))}
  }

  private def _showAdvReq1(refuseFormM: Form[String], r: Option[String])
                          (implicit request: RequestWithAdvReq[AnyContent]): Future[Either[Result, Html]] = {
    val madOptFut = MAd.getById(request.advReq.adId)
    val adProducerOptFut = madOptFut flatMap { madOpt =>
      val prodIdOpt = madOpt.map(_.producerId)
      MAdnNodeCache.maybeGetByIdCached(prodIdOpt)
    }
    adProducerOptFut flatMap { adProducerOpt =>
      madOptFut flatMap { madOpt =>
        if (madOpt.isDefined && adProducerOpt.isDefined) {
          val mad = madOpt.get
          implicit val ctx = implicitly[Context]
          val brArgsFut = ShowcaseUtil.focusedBrArgsFor(mad)(ctx)
          val wndArgsFut = brArgsFut map { brArgs =>
            WndFullArgs(
              producer    = adProducerOpt.get,
              brArgs      = brArgs,
              goBackTo    = r
            )
          }
          wndArgsFut map { wndArgs =>
            val render = _advReqWndTpl(wndArgs, request.advReq, refuseFormM)(ctx)
            Right(render)
          }

        } else {
          val advReqId = request.advReq.id.get
          warn(s"_showAdvReq1($advReqId): Something not found, but it should: mad=$madOpt producer=$adProducerOpt")
          val res = Left(SioHttpErrorHandler.http404ctx)
          Future successful res
        }
      }
    }
  }


  /** Маппинг формы отказа от размещения рекламной карточки. Указывать причину надо. */
  private def reqRefuseFormM = {
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
  def advReqRefuseSubmit(advReqId: Int, r: Option[String]) = CanReceiveAdvReqPost(advReqId).async { implicit request =>
    reqRefuseFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug(s"advReqRefuseSubmit($advReqId): Failed to bind refuse form:\n${formatFormErrors(formWithErrors)}")
        _showAdvReq1(formWithErrors, r)
          .map { _.fold(identity, NotAcceptable(_)) }
      },
      {reason =>
        Future {
          MmpDailyBilling.refuseAdvReq(request.advReq, reason)
        }(AsyncUtil.jdbcExecutionContext)
          .map { _ =>
            RdrBackOr(r) { routes.MarketAdv.showNodeAdvs(request.rcvrNode.id.get) }
              .flashing(FLASH.SUCCESS -> "Adv.req.refused")
          }
      }
    )
  }

  /**
   * Юзер одобряет размещение рекламной карточки.
   * @param advReqId id одобряемого реквеста.
   * @return 302
   */
  def advReqAcceptSubmit(advReqId: Int, r: Option[String]) = CanReceiveAdvReqPost(advReqId).async { implicit request =>
    // Надо провести платёж, запилить транзакции для prod и rcvr и т.д.
    Future {
      MmpDailyBilling.acceptAdvReq(request.advReq, isAuto = false)
    }(AsyncUtil.jdbcExecutionContext)
      .map { _ =>
        // Всё сохранено. Можно отредиректить юзера, чтобы он дальше продолжил одобрять рекламные карточки.
        RdrBackOr(r) { routes.MarketAdv.showNodeAdvs(request.advReq.rcvrAdnId) }
          .flashing(FLASH.SUCCESS -> "Adv.req.accepted")
      }
  }


  /** На основе маппинга формы и сессии суперюзера определить, как размещать рекламу:
    * бесплатно инжектить или за деньги размещать. */
  private def isFreeAdv(isFreeOpt: Option[Boolean])(implicit request: AbstractRequestWithPwOpt[_]): Boolean = {
    isFreeOpt
      .fold(false) { _ && request.isSuperuser }
  }


  /**
   * Отображение страницы истории размещений.
   * @param adId id рекламной карточки, для которой рендерим страницу.
   * @return 200 Ок со страницей.
   */
  def advHistory(adId: String) = CanAdvertiseAdGet(adId).async { implicit request =>
    val rcvrsAllFut = collectAllReceivers(request.producer)
    for {
      adAdvInfo <- getAdAdvInfo(adId)
      rcvrs     <- rcvrsAllFut
    } yield {
      val args = toAdvswArgs(adAdvInfo, rcvrs)
      Ok(advHistoryTpl(request.mad, request.producer, args))
    }
  }

}

