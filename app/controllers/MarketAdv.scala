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
import play.api.data.Form
import play.api.templates.HtmlFormat
import play.api.mvc.AnyContent
import java.sql.SQLException

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
    // Запуск асинхронных операций: подготовка списка узлов, на которые можно вообще возмонжо опубликовать карточку.
    val rcvrsFut = collectReceivers(request.producerId)
    renderAdvFormForRcvrs(adId, form, rcvrsFut)
  }

  private def renderAdvFormForRcvrs(adId: String, form: Form[List[AdvFormEntry]], rcvrsFut: Future[Seq[MAdnNode]])(implicit request: RequestWithAd[AnyContent]): Future[HtmlFormat.Appendable] = {
    // Работа с синхронными моделями.
    val syncResult = DB.withConnection { implicit c =>
      // Собираем всю инфу о размещении этой рекламной карточки
      val advsOk = MAdvOk.findByAdId(adId)
      // Определяем список узлов, которые проходят по adv_ok. Это можно через транзакции и контракты.
      val advsReq = MAdvReq.findByAdId(adId)
      val advsRefused = MAdvRefuse.findByAdId(adId)
      // Собираем инфу о заблокированных средствах, относящихся к этой карточке.
      val blockedSums = MAdvReq.calculateBlockedSumForAd(adId)
      (advsOk, advsReq, advsRefused, blockedSums)
    }
    val (advsOk, advsReq, advsRefused, blockedSums) = syncResult
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
      val rcvrsMap = rcvrs.map { rcvr => rcvr.id.get -> rcvr }.toMap
      // Собираем карту adv.id -> rcvr.
      val adv2adnMap = adv2adnIds.flatMap { case (advId, adnId) =>
        rcvrsMap.get(adnId)
          .fold { List.empty[(Int, MAdnNode)] }  { rcvr => List(advId -> rcvr) }
      }
      // Запускаем рендер шаблона, собрав аргументы в соотв. группы.
      val formArgs = AdvFormTplArgs(adId, rcvrs, form, busyAdns)
      val currAdvsArgs = CurrentAdvsTplArgs(advs, adv2adnMap, blockedSums)
      advForAdTpl(request.mad, currAdvsArgs, formArgs)
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
      {advs =>
        trace(logPrefix + "advs entries submitted: " + advs)
        // Перед сохранением надо проверить возможности публикации на каждый узел.
        // Получаем в фоне все возможные узлы-ресиверы.
        val allRcvrsFut = collectReceivers(request.producerId)
        // Синхронно отбросить ресиверы, у которых на которых уже есть размещение текущей карточки.
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
        val advs1 = advs.filter { advEntry =>
          val result = !(busyAdnIds contains advEntry.adnId)
          if (!result)
            warn(s"${logPrefix}Dropping submit entry rcvrId=${advEntry.adnId} : Node already is busy by other adv by this adId.")
          result
        }
        allRcvrsFut flatMap { allRcvrs =>
          val allRcvrsAdnIds = allRcvrs.map(_.id.get).toSet
          val advs2 = advs1.filter { advEntry =>
            val result = allRcvrsAdnIds contains advEntry.adnId
            if (!result)
              warn(s"${logPrefix}Dropping submit entry rcvrId=${advEntry.adnId} : Not in available rcvrs set")
            result
          }
          // Пора сохранять новые реквесты на размещение в базу.
          if (!advs2.isEmpty) {
            try {
              DB.withTransaction { implicit c =>
                val mbb0 = MBillBalance.getByAdnId(request.producerId).get
                val mbc = MBillContract.findForAdn(request.producerId, isActive = Some(true)).head
                val amount = 10F // TODO Нужно рассчитывать цену
                advs2.foreach { advEntry =>
                  MAdvReq(
                    adId = adId,
                    amount = amount,
                    comission = Some(mbc.sioComission),
                    prodContractId = mbc.id.get,
                    prodAdnId = request.producerId,
                    rcvrAdnId = advEntry.adnId,
                    dateStart = advEntry.dateStart.toDateTimeAtStartOfDay,
                    dateEnd = advEntry.dateEnd.toDateTimeAtStartOfDay,
                    onStartPage = advEntry.onStartPage
                  ).save
                  // Нужно заблокировать на счете узла необходимую сумму денег.
                  mbb0.updateBlocked(amount)
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

}

sealed case class AdvFormEntry(adnId: String, advertise: Boolean, onStartPage: Boolean, dateStart: LocalDate, dateEnd: LocalDate)
