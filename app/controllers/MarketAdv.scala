package controllers

import com.google.inject.Inject
import io.suggest.adv.direct.AdvDirectFormConstants
import io.suggest.mbill2.m.item.{MItem, MItems}
import io.suggest.mbill2.m.item.status.MItemStatuses
import io.suggest.mbill2.m.order.MOrderWithItems
import io.suggest.model.common.OptId
import io.suggest.model.n2.node.search.MNodeSearchDfltImpl
import models._
import models.adv.direct._
import models.adv.tpl.MAdvForAdTplArgs
import models.jsm.init.MTargets
import models.mctx.Context
import models.mproj.ICommonDi
import models.req.{IAdProdReq, IReq}
import play.api.libs.json.{JsString, Json}
import play.api.mvc.{AnyContent, Result}
import util.PlayMacroLogsImpl
import util.acl._
import util.adv.direct.{DirectAdvFormUtil, AdvDirectBilling}
import util.adv.AdvFormUtil
import util.billing.{TfDailyUtil, Bill2Util}
import util.cal.CalendarUtil
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
  advDirectBilling                : AdvDirectBilling,
  directAdvFormUtil               : DirectAdvFormUtil,
  advFormUtil                     : AdvFormUtil,
  bill2Util                       : Bill2Util,
  mItems                          : MItems,
  tfDailyUtil                     : TfDailyUtil,
  calendarUtil                    : CalendarUtil,
  override val mCommonDi          : ICommonDi
)
  extends SioControllerImpl
  with PlayMacroLogsImpl
  with CanAdvertiseAd
{

  import LOGGER._
  import mCommonDi._


  /** Страница управления размещением рекламной карточки. */
  def advForAd(adId: String) = CanAdvertiseAdGet(adId, U.Lk).async { implicit request =>
    val form0 = directAdvFormUtil.advForm
    // Залить в форму начальные данные.
    val res = FormResult()
    val formFilled = form0.fill(res)
    // Запускаем остальной рендер
    renderAdvForm(formFilled, Ok)
  }


  /**
   * Рендер страницы с формой размещения. Сбор и подготовка данных для рендера идёт очень параллельно.
   *
   * @param form Форма размещения рекламной карточки.
   * @param rcvrsAllFutOpt Опционально: асинхронный список ресиверов. Экшен контроллера может передавать его сюда.
   * @return Отрендеренная страница управления карточкой с формой размещения.
   */
  private def renderAdvForm(form: DirectAdvFormM_t, rs: Status, rcvrsAllFutOpt: Option[Future[Seq[MNode]]] = None)
                           (implicit request: IAdProdReq[AnyContent]): Future[Result] = {
    // Если поиск ресиверов ещё не запущен, то сделать это.
    val rcvrsAllFut = rcvrsAllFutOpt.getOrElse {
      collectAllReceivers(request.producer)
    }
    // В фоне строим карту ресиверов, чтобы по ней быстро ориентироваться.
    val allRcvrsMapFut = for (rcvrs <- rcvrsAllFut) yield {
      OptId.els2idMap[String, MNode]( rcvrs )
    }

    val adId = request.mad.id.get

    // Нужно заодно собрать карту (adnId -> Int), которая отражает целочисленные id узлов в list-маппинге.
    val adnId2indexMapFut: Future[Map[String, Int]] = for {
      rcvrs <- rcvrsAllFut
    } yield {
      // Карта строится на основе данных из исходной формы и дополняется недостающими adn_id.
      val formIndex2adnIdMap0 = form("node").indexes
        .flatMap { fi => form(s"node[$fi].adnId").value.map(fi -> _) }
        .toMap
      val missingRcvrIds = OptId.els2idsSet(rcvrs) -- formIndex2adnIdMap0.valuesIterator
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
      rcvrs     <- rcvrsAllFut
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
                nodes     = catNodes.map(MAdvFormNode.apply),
                name      = ctx.messages(ast.pluralNoTown),
                i         = i
              )
            }
            .toSeq
            .sortBy(_.name)
          (cityNode, cats)
        }
        .zipWithIndex
        .map { case ((cityNode, cats), i) =>
          MAdvFormCity(cityNode, cats, i)
        }
        .toSeq
        .sortBy(_.node.meta.basic.name)
    }

    // Строим карту уже занятых какими-то размещением узлы.
    val busyAdvsFut: Future[Map[String, MItem]] = {
      slick.db.run {
        advDirectBilling.findBusyRcvrsMap(adId)
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

  private def maybeFreeAdv()(implicit request: IReq[_]): Boolean = {
    // Раньше было ограничение на размещение с завтрашнего дня, теперь оно снято.
    val isFreeOpt = advFormUtil.freeAdvFormM
      .bindFromRequest()
      .fold({_ => None}, identity)
    isFreeAdv( isFreeOpt )
  }

  /**
    * Рассчитать цену размещения. Сюда нужно сабмиттить форму также, как и в advFormSubmit().
    *
    * @param adId id размещаемой рекламной карточки.
    * @return Инлайновый рендер отображаемой цены.
    */
  def getAdvPriceSubmit(adId: String) = CanAdvertiseAdPost(adId, U.Balance).async { implicit request =>
    lazy val logPrefix = s"getAdvPriceSubmit($adId)#${System.currentTimeMillis}:"
    directAdvFormUtil.advForm.bindFromRequest().fold(
      {formWithErrors =>
        debug(s"$logPrefix Failed to bind form:\n${formatFormErrors(formWithErrors)}")
        NotAcceptable("Cannot bind form.")
      },

      // Всё ок. Надо рассчитать цену и вернуть результат.
      {formRes =>
        // Подготовить данные для рассчета стоимости размещения.
        val allRcvrIdsFut = for (rcvrs <- collectAllReceivers(request.producer)) yield {
          OptId.els2idsSet( rcvrs )
        }

        val adves2Fut = for {
          adves1      <- prepareFromEntries(adId, formRes)
          allRcvrIds  <- allRcvrIdsFut
        } yield {
          filterEntiesByPossibleRcvrs(adves1, allRcvrIds)
        }

        // Убедиться, что чтение балансов текущего юзера точно уже процессе (U.Balance)
        val userBalancesFut = request.user.mBalancesFut

        // Начинаем рассчитывать ценник.
        val priceValHtmlFut = for {
          adves2  <- adves2Fut
          prices  <- {
            if (adves2.isEmpty || maybeFreeAdv()) {
              Future.successful(Nil: Seq[MPrice])
            } else {
              for {
                allPrices <- advDirectBilling.getAdvPrices(request.mad, adves2)
              } yield {
                MPrice.sumPricesByCurrency(allPrices).toSeq
              }
            }
          }
          userBalances <- userBalancesFut
        } yield {
          val advPricing = if (prices.nonEmpty) {
            advDirectBilling.getAdvPricing(prices, userBalances)
          } else {
            bill2Util.zeroPricing
          }
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
    *
    * @param adId id размещаемой рекламной карточки.
    * @param fr Результат сабмита формы advFormM.
    * @return Новый adves, который НЕ содержит уже размещаемые карточки.
    */
  private def prepareFromEntries(adId: String, fr: FormResult): Future[List[AdvFormEntry]] = {
    lazy val logPrefix = s"filterEnties($adId)"

    // Собрать id конфликтующих ресиверов в засабмиченной форме и в существующем биллинге.
    val busyRcvrIdsFut = slick.db.run {
      advDirectBilling.findBusyRcvrsExact(adId, fr)
    }
    for (busyRcvrsIds <- busyRcvrIdsFut) {
      if (busyRcvrsIds.nonEmpty)
        warn(s"$logPrefix Conflicting receivers will be dropped: ${busyRcvrsIds.mkString(", ")}\n Form res: $fr")
    }

    val adves = _formRes2adves(fr)

    for (busyRcvrIds <- busyRcvrIdsFut) yield {
      adves.filter { advEntry =>
        val result = !(busyRcvrIds contains advEntry.adnId)
        if (!result)
          warn(s"$logPrefix Dropping submit entry rcvrId=${advEntry.adnId} : Node already is busy by other adv by this adId.")
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
      case (acc, oni) =>
        val ssls = for (sl <- oni.sls) yield {
          SinkShowLevels.withArgs(AdnSinks.SINK_GEO, sl)
        }
        val result = AdvFormEntry(
          adnId       = oni.adnId,
          advertise   = oni.isAdv,
          showLevels  = ssls,
          dateStart   = formRes.period.dateStart,
          dateEnd     = formRes.period.dateEnd
        )
        result :: acc
    }
  }

  /** Сабмит формы размещения рекламной карточки. */
  def advFormSubmit(adId: String) = CanAdvertiseAdPost(adId, U.Contract, U.PersonNode).async { implicit request =>
    lazy val logPrefix = s"advFormSubmit($adId): "
    val formBinded = directAdvFormUtil.advForm.bindFromRequest()
    formBinded.fold(
      {formWithErrors =>
        debug(s"${logPrefix}form bind failed:\n${formatFormErrors(formWithErrors)}")
        renderAdvForm(formWithErrors, NotAcceptable)
      },
      {formRes =>
        trace(logPrefix + "Starting with: " + formRes)

        val adves2Fut = prepareFromEntries(adId, formRes)

        // Перед сохранением надо проверить возможности публикации на каждый узел.
        // Получаем в фоне все возможные узлы-ресиверы.
        val rcvrsFut = {
          val ids = formRes.nodeIdsIter
            .filter { id => !request.producer.id.contains(id) }
          collectRcvrsFromIds(ids)
        }

        val adves3Fut = for {
          adves2    <- adves2Fut
          rcvrs     <- rcvrsFut
        } yield {
          val allRcvrIds = OptId.els2idsSet(rcvrs)
          filterEntiesByPossibleRcvrs(adves2, allRcvrIds)
        }

        // Начинаем двигаться в сторону сборки ответа...
        val respFut = for {
          adves3 <- adves3Fut
          // С пустым списком размещения ковыряться смысла нет. Исключение будет перехвачено в recover:
          if adves3.nonEmpty

          // Запустить подготовку контракта текущего юзера.
          personContractFut = {
            request.user
              .personNodeFut
              .flatMap { personNode =>
                bill2Util.ensureNodeContract(personNode, request.user.mContractOptFut)
              }
          }

          // Синхронно проанализировать состояние галочки бесплатного размещения.
          isFree = maybeFreeAdv()
          status = if (isFree) {
            MItemStatuses.Offline
          } else {
            MItemStatuses.Draft
          }

          rcvrs     <- rcvrsFut

          // Подготовить данные для рассчета стоимостей item'ов
          tfsMap    <- tfDailyUtil.getNodesTfsMap(rcvrs)
          mcalsCtx  <- {
            val calIds = tfDailyUtil.tfsMap2calIds(tfsMap)
            calendarUtil.getCalsCtx(calIds)
          }

          // Собрать данные для биллинга, которые должны бы собраться уже
          personContract    <- personContractFut

          // Залезть наконец в базу биллинга, сохранив в корзину добавленные размещения...
          billRes <- {
            val dbAction = for {
              cartOrder <- bill2Util.ensureCart( personContract.mc.id.get )
              itemsSaved <- {
                val items0 = advDirectBilling.mkAdvReqItems(cartOrder.id.get, request.mad, adves3, status, tfsMap, mcalsCtx)
                mItems.insertMany(items0)
              }
            } yield {
              MOrderWithItems(cartOrder, itemsSaved)
            }
            import slick.driver.api._
            slick.db.run( dbAction.transactionally )
          }

        } yield {
          // Всё сделано, отредиректить юзера
          if (!isFree) {
            // Обычные юзеры отправляются в корзину.
            val call = routes.LkBill2.cart(request.producer.id.get, r = Some(routes.LkAdvGeoTag.forAd(adId).url))
            Redirect(call)
          } else {
            // Суперюзеры sio отправляются на эту же страницу для дальнейшей возни
            Redirect( routes.MarketAdv.advForAd(adId) )
              .flashing(FLASH.SUCCESS -> "Ads.were.adv")
          }
        }

        // Если список размещения пуст, то надо вернуть форму.
        respFut.recover { case ex: NoSuchElementException =>
          Redirect(routes.MarketAdv.advForAd(adId))
            .flashing(FLASH.SUCCESS -> "No.changes")
        }
      }
    )
  }


  /** Собрать все узлы сети, пригодные для размещения рекламной карточки. */
  private def collectAllReceivers(producer: MNode): Future[Seq[MNode]] = {
    // TODO Этот запрос возвращает огромный список нод, которые рендерятся в огромный список.
    // Надо переверстать эти шаблоны, искать ноды по ajax в рамках гео-регионов и категорий/тегов этих нод.
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


  private def collectRcvrsFromIds(ids: TraversableOnce[String]): Future[Seq[MNode]] = {
    for (mnodes <- mNodeCache.multiGet(ids)) yield {
      collectRcvrsFromNodes(mnodes)
    }
  }

  private def collectRcvrsFromNodes(mnodes: TraversableOnce[MNode]): Seq[MNode] = {
    mnodes
      .toIterator
      .filter { mnode =>
        mnode.extras.adn.exists(_.rights.contains(AdnRights.RECEIVER)) &&
          mnode.common.ntype == MNodeTypes.AdnNode
      }
      .toSeq
  }


  /** На основе маппинга формы и сессии суперюзера определить, как размещать рекламу:
    * бесплатно инжектить или за деньги размещать. */
  private def isFreeAdv(isFreeOpt: Option[Boolean])(implicit request: IReq[_]): Boolean = {
    isFreeOpt.exists { _ && request.user.isSuper }
  }


}

