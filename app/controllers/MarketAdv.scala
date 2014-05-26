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

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.05.14 18:18
 * Description: Контроллер для управления процессом размещения рекламных карточек с узла на узел:
 * - узел 1 размещает рекламу на других узлах (форма, сабмит и т.д.).
 * - узелы-получатели одобряют или отсеивают входящие рекламные карточки.
 */
object MarketAdv extends SioController {

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
    // Запуск асинхронных операций.
    val rcvrsFut = MAdnNode.findByAllAdnRights(Seq(AdnRights.RECEIVER))
      // Самому себе через "управление размещением" публиковать нельзя.
      .map { _.filter(_.id.get != request.producerId) }
    // Работа с синхронными моделями.
    val syncResult = DB.withConnection { implicit c =>
      // Собираем всю инфу о размещении этой рекламной карточки
      val adnAdvsOk = MAdvOk.findByAdIdWithRcvrAdnId(adId)
      // Определяем список узлов, которые проходят по adv_ok. Это можно через транзакции и контракты.
      val advsReq = MAdvReq.findByAdId(adId)
      val advsRefused = MAdvRefuse.findByAdId(adId)
      // Собираем инфу о заблокированных средствах, относящихся к этой карточке.
      val blockedSums = MAdvReq.calculateBlockedSumForAd(adId)
      (adnAdvsOk, advsReq, advsRefused, blockedSums)
    }
    val (adnAdvsOk, advsReq, advsRefused, blockedSums) = syncResult
    val advsOk = adnAdvsOk.map(_._2)
    val advs = (advsReq ++ advsRefused ++ advsOk).sortBy(_.dateCreated)
    // Собираем карту adv.id -> rcvrId. Она нужна для сборки карты adv.id -> rcvr.
    val reqAdnIds = advsReq.map {
      advReq  =>  advReq.id.get -> advReq.rcvrAdnId
    }
    val refusedAdnIds = advsRefused.map {
      advRefused  =>  advRefused.id.get -> advRefused.refuserAdnId
    }
    val okAdnIds = adnAdvsOk.map {
      case (adnId, advOk)  =>  advOk.id.get -> adnId
    }
    val adv2adnIds: Map[Int, String] = (okAdnIds ++ reqAdnIds ++ refusedAdnIds).toMap
    val busyAdns: Map[String, MAdvI] = {
      val adnAdvsReq = advsReq.map { advReq  =>  advReq.rcvrAdnId -> advReq }
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
    ???
  }

}

sealed case class AdvFormEntry(adnId: String, advertise: Boolean, period: Period, onStartPage: Boolean)
