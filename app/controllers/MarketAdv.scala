package controllers

import java.sql.SQLException

import com.github.nscala_time.time.OrderingImplicits._
import com.google.inject.Inject
import io.suggest.adv.direct.AdvDirectFormConstants
import io.suggest.model.n2.node.search.MNodeSearchDfltImpl
import models._
import models.adv._
import models.adv.direct._
import models.adv.tpl.{MAdvForAdTplArgs, MAdvHistoryTplArgs, MCurrentAdvsTplArgs}
import models.mctx.Context
import models.mproj.ICommonDi
import models.req.{IAdProdReq, INodeAdvReqReq, IReq}
import models.jsm.init.MTargets
import play.api.data._
import play.api.libs.json.{JsString, Json}
import play.api.mvc.{AnyContent, Result}
import play.twirl.api.Html
import util.PlayMacroLogsImpl
import util.acl._
import util.adv.{AdvFormUtil, CtlGeoAdvUtil, DirectAdvFormUtil}
import util.async.AsyncUtil
import util.billing.{Bill2Util, MmpDailyBilling}
import util.lk.LkAdUtil
import util.n2u.N2NodesUtil
import util.showcase.ShowcaseUtil
import views.html.lk.adv._
import views.html.lk.adv.direct._
import views.html.lk.adv.widgets.period._reportTpl
import views.html.lk.lkwdgts.price._

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.05.14 18:18
 * Description: Контроллер для управления процессом размещения рекламных карточек с узла на узел:
 * - узел 1 размещает рекламу на других узлах (форма, сабмит и т.д.).
 * - узелы-получатели одобряют или отсеивают входящие рекламные карточки.
 */
class MarketAdv @Inject() (
  override val canAdvAdUtil       : CanAdvertiseAdUtil,
  mmpDailyBilling                 : MmpDailyBilling,
  lkAdUtil                        : LkAdUtil,
  scUtil                          : ShowcaseUtil,
  ctlGeoAdvUtil                   : CtlGeoAdvUtil,
  directAdvFormUtil               : DirectAdvFormUtil,
  advFormUtil                     : AdvFormUtil,
  bill2Util                       : Bill2Util,
  n2NodesUtil                     : N2NodesUtil,
  override val mCommonDi          : ICommonDi
)
  extends SioControllerImpl
  with PlayMacroLogsImpl
  with CanAdvertiseAd
  with IsAdnNodeAdmin
{

  import LOGGER._
  import mCommonDi._

  val ADVS_MODE_SELECT_LIMIT = configuration.getInt("adv.short.limit") getOrElse 2


  /** Страница управления размещением рекламной карточки. */
  def advForAd(adId: String) = CanAdvertiseAdGet(adId, U.Lk).async { implicit request =>
    val form0 = directAdvFormUtil.advForm
    // Залить в форму начальные данные.
    val res = FormResult()
    val formFilled = form0.fill(res)
    // Запускаем остальной рендер
    renderAdvForm(adId, formFilled, Ok)
  }

  /** Класс-контейнер для передачи результатов ряда операций с adv/bill-sql-моделями в renderAdvForm(). */
  private case class AdAdvInfoResult(
    advsOk      : List[MAdvOk],
    advsReq     : List[MAdvReq],
    advsRefused : List[MAdvRefuse]
  )

  /**
   * Очень асинхронно прочитать инфу по текущим размещениям карточки, вернув контейнер результатов.
   * @param adId id рекламной карточки.
   * @param limit Необязательный лимит.
   * @return Фьючерс с контейнером результатов.
   */
  private def getAdAdvInfo(adId: String, limit: Int = ctlGeoAdvUtil.LIMIT_DFLT): Future[AdAdvInfoResult] = {
    val advsOkFut  = ctlGeoAdvUtil.advFindNonExpiredByAdId(MAdvOk, adId, limit)
    val advsReqFut = ctlGeoAdvUtil.advFindByAdId(MAdvReq, adId, limit)
    for {
      advsRefused     <- ctlGeoAdvUtil.advFindByAdId(MAdvRefuse, adId, limit)
      advsOk          <- advsOkFut
      advsReq         <- advsReqFut
    } yield {
      AdAdvInfoResult(advsOk, advsReq, advsRefused)
    }
  }

  private def toAdvswArgs(adAdvInfo: AdAdvInfoResult, rcvrs: Seq[MNode]): MCurrentAdvsTplArgs = {
    import adAdvInfo._
    val advs = (advsReq ++ advsRefused ++ advsOk).sortBy(_.dateCreated)
    val adv2adnIds = mkAdv2adnIds(advsReq, advsRefused, advsOk)
    val adv2adnMap = mkAdv2adnMap(adv2adnIds, rcvrs)
    MCurrentAdvsTplArgs(advs, adv2adnMap)
  }

  /**
   * Рендер страницы с формой размещения. Сбор и подготовка данных для рендера идёт очень параллельно.
   * @param adId id рекламной карточки.
   * @param form Форма размещения рекламной карточки.
   * @param rcvrsAllFutOpt Опционально: асинхронный список ресиверов. Экшен контроллера может передавать его сюда.
   * @return Отрендеренная страница управления карточкой с формой размещения.
   */
  private def renderAdvForm(adId: String, form: DirectAdvFormM_t, rs: Status, rcvrsAllFutOpt: Option[Future[Seq[MNode]]] = None)
                           (implicit request: IAdProdReq[AnyContent]): Future[Result] = {
    // Если поиск ресиверов ещё не запущен, то сделать это.
    val rcvrsAllFut = rcvrsAllFutOpt.getOrElse {
      collectAllReceivers(request.producer)
    }
    // В фоне строим карту ресиверов, чтобы по ней быстро ориентироваться.
    val allRcvrsMapFut = for (rcvrs <- rcvrsAllFut) yield {
      rcvrs.iterator
        .map { rcvr  =>  rcvr.id.get -> rcvr }
        .toMap
    }

    // Для сокрытия узлов, которые не имеют тарифного плана, надо получить список тех, у кого он есть.
    val adnIdsReadyFut = ctlGeoAdvUtil.findAdnIdsMmpReady()

    // Работа с синхронными моделями: собрать инфу обо всех размещениях текущей рекламной карточки.
    val adAdvInfoFut = getAdAdvInfo(adId)

    // Сразу запускаем в фоне генерацию старого формата передачи ресиверов в шаблон.
    val rcvrsReadyFut = for {
      adnIdsReady <- adnIdsReadyFut
      rcvrs       <- rcvrsAllFut
    } yield {
      val adnIdsReadySet = adnIdsReady.toSet
      // Выкинуть узлы, у которых нет своего тарифного плана.
      rcvrs.filter { node =>
        adnIdsReadySet.contains( node.id.get )
      }
    }

    // Нужно заодно собрать карту (adnId -> Int), которая отражает целочисленные id узлов в list-маппинге.
    val adnId2indexMapFut: Future[Map[String, Int]] = for (rcvrs <- rcvrsReadyFut) yield {
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
    val ctxFut = for {
      lkCtxData <- request.user.lkCtxDataFut
    } yield {
      implicit val ctxData = lkCtxData.copy(
        jsiTgs = Seq( MTargets.AdvDirectForm )
      )
      implicitly[Context]
    }

    // Строим набор городов и их узлов, сгруппированных по категориям.
    val citiesFut: Future[Seq[MAdvFormCity]] = for {
      rcvrs     <- rcvrsReadyFut
      rcvrsMap  <- allRcvrsMapFut
      ctx       <- ctxFut
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
          val cats = cityNodes
            .groupBy { mnode =>
              mnode.extras
                .adn
                .flatMap( _.shownTypeIdOpt )
                .getOrElse( AdnShownTypes.MART.name )
            }
            .iterator
            .zipWithIndex
            .map { case ((shownTypeId, catNodes), i) =>
              val ast = AdnShownTypes.shownTypeId2val(shownTypeId)
              MAdvFormCityCat(
                shownType = ast,
                nodes = catNodes.map(MAdvFormNode.apply),
                name = ctx.messages(ast.pluralNoTown),
                i = i
              )
            }
            .toSeq
            .sortBy(_.name)
          (cityNode, cats)
        }
        .zipWithIndex
        .map { case ((cityNode, cats), i)  =>  MAdvFormCity(cityNode, cats, i) }
        .toSeq
        .sortBy(_.node.meta.basic.name)
      }

    // Строим карту уже занятых какими-то размещением узлы.
    val busyAdvsFut: Future[Map[String, MAdvI]] = {
      for (adAdvInfo <- adAdvInfoFut) yield {
        import adAdvInfo._
        val adnAdvsReq = advsReq.map { advReq  =>  advReq.rcvrAdnId -> advReq }
        val adnAdvsOk = advsOk.map { advOk => advOk.rcvrAdnId -> advOk }
        (adnAdvsOk ++ adnAdvsReq).toMap
      }
    }

    // Периоды размещения. Обычно одни и те же. Сразу считаем в текущем потоке:
    val advPeriodsAvailable = advFormUtil.advPeriodsAvailable

    // Сборка финального контейнера аргументов для _advFormTpl().
    val advFormTplArgsFut: Future[MAdvFormTplArgs] = for {
      adnId2indexMap  <- adnId2indexMapFut
      cities          <- citiesFut
      busyAdvs        <- busyAdvsFut
    } yield {
      MAdvFormTplArgs(
        adId            = adId,
        af              = form,
        busyAdvs        = busyAdvs,
        cities          = cities,
        adnId2formIndex = adnId2indexMap,
        advPeriodsAvail = advPeriodsAvailable
      )
    }

    val price0 = bill2Util.zeroPricing

    // Когда всё станет готово - рендерим результат.
    for {
      ctx           <- ctxFut
      formArgs      <- advFormTplArgsFut
    } yield {
      // Запускаем рендер шаблона, собрав аргументы в соотв. группы.
      val rargs = MAdvForAdTplArgs(
        mad       = request.mad,
        producer  = request.producer,
        formArgs  = formArgs,
        price     = price0
      )
      rs( advForAdTpl(rargs)(ctx) )
    }
  }


  /** Используя дерево гео-связей нужно найти родительский узел, имеющий указанный shownType. */
  private def findFirstGeoParentOfType(node: MNode, parentShowType: AdnShownType, nodes: Map[String, MNode]): Option[MNode] = {
    // Поднимаемся наверх по иерархии гео-родительства.
    val iter = node.edges
      .withPredicateIter( MPredicates.GeoParent.Direct )
      .map { _.nodeId }
      .flatMap(nodes.get)
      .flatMap { parentNode =>
        if (parentNode.extras.adn.flatMap(_.shownTypeIdOpt).contains( parentShowType.name) ) {
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

  private def mkAdv2adnMap(adv2adnIds: Map[Int, String], rcvrs: Seq[MNode]): Map[Int, MNode] = {
    val rcvrsMap = rcvrs.map { rcvr => rcvr.id.get -> rcvr }.toMap
    // Собираем карту adv.id -> rcvr.
    adv2adnIds.flatMap { case (advId, adnId) =>
      rcvrsMap.get(adnId)
        .fold { List.empty[(Int, MNode)] }  { rcvr => List(advId -> rcvr) }
    }
  }


  private def mkAdv2adnIds(advss: List[MAdvI] *): Map[Int, String] = {
    advss.foldLeft [List[(Int, String)]] (Nil) { (acc1, advs) =>
      advs.foldLeft(acc1) { (acc2, e) =>
        e.id.get -> e.rcvrAdnId  ::  acc2
      }
    }.toMap
  }

  private def maybeFreeAdv(implicit request: IReq[_]): Boolean = {
    // Раньше было ограничение на размещение с завтрашнего дня, теперь оно снято.
    val isFreeOpt = advFormUtil.freeAdvFormM
      .bindFromRequest()
      .fold({_ => None}, identity)
    isFreeAdv( isFreeOpt )
  }

  /**
    * Рассчитать цену размещения. Сюда нужно сабмиттить форму также, как и в advFormSubmit().
    * @param adId id размещаемой рекламной карточки.
    * @return Инлайновый рендер отображаемой цены.
    */
  def getAdvPriceSubmit(adId: String) = CanAdvertiseAdPost(adId).async { implicit request =>
    directAdvFormUtil.advForm.bindFromRequest().fold(
      {formWithErrors =>
        debug(s"getAdvPriceSubmit($adId): Failed to bind form:\n${formatFormErrors(formWithErrors)}")
        NotAcceptable("Cannot bind form.")
      },
      {formRes =>
        // Подготовить данные для рассчета стоимости размещения.
        val allRcvrIdsFut = collectAllReceivers(request.producer)
          .map { _.iterator.flatMap(_.id).toSet }

        val adves = _formRes2adves(formRes)
        val adves2Fut = for {
          adves1      <- filterEntiesByBusyRcvrs(adId, adves)
          allRcvrIds  <- allRcvrIdsFut
        } yield {
          filterEntiesByPossibleRcvrs(adves1, allRcvrIds)
        }

        // Начинаем рассчитывать ценник.
        val priceValHtmlFut = for {
          adves2      <- adves2Fut
          advPricing  <- {
            if (adves2.isEmpty || maybeFreeAdv) {
              Future.successful( bill2Util.zeroPricing )
            } else {
              mmpDailyBilling.getAdvPrices(request.mad, adves2)
            }
          }
        } yield {
          val html = _priceValTpl(advPricing)
          html: JsString
        }

        // Параллельно отрендерить отчет по датам размещения.
        val periodReportHtml: JsString = _reportTpl(formRes.period)

        // Отрендерить JSON-ответ
        for (priceValHtml <- priceValHtmlFut) yield {
          import AdvDirectFormConstants.PriceJson._
          // TODO Вынести это дело отсюда в отдельную модель?
          Ok(Json.obj(
            PERIOD_REPORT_HTML_FN -> periodReportHtml,
            PRICE_HTML_FN         -> priceValHtml
          ))
        }
      }
    )
  }


  /** Фильтрация присланного списка запросов на публикацию по уже размещённым adv.
    * @param adId id размещаемой рекламной карточки.
    * @param adves Результат сабмита формы advFormM.
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

  /** Конвертация результата маппинга формы в список размещений.
    * Это наследие, надо будет его удалить, используя FormResult напрямую. */
  private def _formRes2adves(formRes: FormResult): List[AdvFormEntry] = {
    formRes.nodes.foldLeft(List.empty[AdvFormEntry]) {
      case (acc, OneNodeInfo(adnId, isAdv, adSls) ) =>
        val ssls = for(sl <- adSls) yield {
          SinkShowLevels.withArgs(AdnSinks.SINK_GEO, sl)
        }
        val result = AdvFormEntry(
          adnId       = adnId,
          advertise   = isAdv,
          showLevels  = ssls,
          dateStart   = formRes.period.dateStart,
          dateEnd     = formRes.period.dateEnd
        )
        result :: acc
    }
  }

  /** Сабмит формы размещения рекламной карточки. */
  def advFormSubmit(adId: String) = CanAdvertiseAdPost(adId).async { implicit request =>
    lazy val logPrefix = s"advFormSubmit($adId): "
    val formBinded = directAdvFormUtil.advForm.bindFromRequest()
    formBinded.fold(
      {formWithErrors =>
        debug(s"${logPrefix}form bind failed:\n${formatFormErrors(formWithErrors)}")
        renderAdvForm(adId, formWithErrors, NotAcceptable)
      },
      {formRes =>
        val adves = _formRes2adves(formRes)

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
              val successMsgFut: Future[String] = {
                if (isFree) {
                  mmpDailyBilling.mkAdvsOk(request.mad, advs2)
                    .map { _ => "Ads.were.adv" }
                } else {
                  mmpDailyBilling.mkAdvReqs(request.mad, advs2)
                    .map { _ => "Adv.reqs.sent" }
                }
              }
              successMsgFut map { successMsg =>
                Redirect(routes.MarketAdv.advForAd(adId))
                  .flashing(FLASH.SUCCESS -> successMsg)
              }

            } catch {
              case ex: SQLException =>
                warn(s"advFormSumbit($adId): Failed to commit adv transaction for advs:\n " + advs2, ex)
                // Для бесплатной инжекции: сгенерить экзепшен, чтобы привелегированному юзеру код ошибки отобразился на экране.
                if (isFree) throw ex
                val formWithErrors = formBinded.withGlobalError("error.no.money")
                renderAdvForm(adId, formWithErrors, NotAcceptable, Some(allRcvrsFut))
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
  private def collectAllReceivers(producer: MNode): Future[Seq[MNode]] = {
    // TODO Этот запрос возвращает огромный список нод, которые рендерятся в огромный список. Надо переверстать эти шаблоны.
    val isTestOpt = producer.extras.adn.map(_.testNode)
    if ( isTestOpt.contains(true) )
      LOGGER.debug(s"collectAllReceivers(${producer.id.get}): Searching for isolated nodes with isTest = true")

    val msearch = new MNodeSearchDfltImpl {
      override def withAdnRights  = Seq(AdnRights.RECEIVER)
      override def testNode       = isTestOpt
      override def limit          = 500
      override def withoutIds     = producer.id.toSeq
      override def nodeTypes      = Seq( MNodeTypes.AdnNode )
    }
    MNode.dynSearch( msearch )
  }


  /** На основе маппинга формы и сессии суперюзера определить, как размещать рекламу:
    * бесплатно инжектить или за деньги размещать. */
  private def isFreeAdv(isFreeOpt: Option[Boolean])(implicit request: IReq[_]): Boolean = {
    isFreeOpt.exists { _ && request.user.isSuper }
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
      val rargs = MAdvHistoryTplArgs(
        mad           = request.mad,
        producer      = request.producer,
        currAdvsArgs  = toAdvswArgs(adAdvInfo, rcvrs)
      )
      Ok( advHistoryTpl(rargs) )
    }
  }

}

