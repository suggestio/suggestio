package controllers

import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client
import util.acl._
import models._
import org.joda.time.Period
import play.api.db.DB
import com.github.nscala_time.time.OrderingImplicits._
import views.html.market.lk.adv._
import util.PlayMacroLogsImpl

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
  val advFormM = {
    import play.api.data._, Forms._
    import util.FormUtil._
    Form(
      "node" -> list(
        mapping(
          "adnId"       -> esIdM,
          "advertise"   -> boolean,
          "period"      -> isoPeriodM,
          "onStartPage" -> boolean
        )
        (AdvFormEntry.apply)
        (AdvFormEntry.unapply)
      )
    )
  }

  /** Страница управления размещением рекламной карточки. */
  def advForAd(adId: String) = CanAdvertiseAd(adId).async { implicit request =>
    // Запуск асинхронных операций: подготовка списка узлов, на которые можно вообще возмонжо опубликовать карточку.
    val rcvrsFut = collectReceivers(request.producerId)
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
    val advs = (advsReq ++ advsRefused ++ advsOk).sortBy(_.dateCreated)
    // Собираем карту adv.id -> rcvrId. Она нужна для сборки карты adv.id -> rcvr.
    val reqAdnIds = advsReq.map {
      advReq  =>  advReq.id.get -> advReq.rcvrAdnId
    }
    val refusedAdnIds = advsRefused.map {
      advRefused  =>  advRefused.id.get -> advRefused.refuserAdnId
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
      val formArgs = AdvFormTplArgs(adId, rcvrs, advFormM, busyAdns)
      val currAdvsArgs = CurrentAdvsTplArgs(advs, adv2adnMap, blockedSums)
      Ok(advForAdTpl(request.mad, currAdvsArgs, formArgs))
    }
  }


  /** Сабмит формы размещения рекламной карточки. */
  def advFormSubmit(adId: String) = CanAdvertiseAd(adId).async { implicit request =>
    advFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug(s"advFormSubmit($adId): form bind failed:\n${formatFormErrors(formWithErrors)}")
        NotAcceptable("Failed to bind adv.form.")
      },
      {advs =>
        // Перед сохранением надо проверить возможности публикации на каждый узел.
        // Получаем в фоне все возможные узлы-ресиверы.
        val allRcvrsFut = collectReceivers(request.producerId)
        // Синхронно отбросить ресиверы, у которых на которых уже есть размещение текущей карточки.
        val syncResult1 = DB.withConnection { implicit c =>
          // TODO Opt нам не нужен список сами advOk, нам нужен только список adn, т.е. ._1 .
          val advsOk = MAdvOk.findByAdId(adId)
          val advsReq = MAdvReq.findByAdId(adId)
          (advsOk, advsReq)
        }
        val (advsOk, advsReq) = syncResult1
        val busyAdnIds = ( adnAdvsOk.map(_._1) ++ advsReq.map(_.rcvrAdnId) ).toSet
        val advs1 = advs.filter { advEntry => !(busyAdnIds contains advEntry.adnId) }
        for {
          allRcvrs <- allRcvrsFut
        } yield {
          val allRcvrsAdnIds = allRcvrs.map(_.id.get).toSet
          val advs2 = advs1.filter { advEntry => allRcvrsAdnIds contains advEntry.adnId }
          // Пора сохранять новые реквесты на размещение в базу.
          DB.withTransaction { implicit c =>
            advs2.foreach { advEntry =>
              MAdvReq(adId = adId, amount = 10F, comissionPc = Some(0.30),

              )
            }
          }
        }
      }
    )
    ???
  }


  /** Собрать все узлы сети, пригодные для размещения рекламной карточки. */
  private def collectReceivers(dropRcvrId: String) = {
    MAdnNode.findByAllAdnRights(Seq(AdnRights.RECEIVER))
      // Самому себе через "управление размещением" публиковать нельзя.
      .map { _.filter(_.id.get != dropRcvrId) }
  }

}

sealed case class AdvFormEntry(adnId: String, advertise: Boolean, period: Period, onStartPage: Boolean)
