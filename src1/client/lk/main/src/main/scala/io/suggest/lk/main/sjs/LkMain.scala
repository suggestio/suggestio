package io.suggest.lk.main.sjs

import io.suggest.ad.edit.LkAdEditInit
import io.suggest.ads.LkAdsInit
import io.suggest.lk.ad.form.init.AdFormInitRouter
import io.suggest.lk.adn.edit.init.NodeEditInitRouter
import io.suggest.lk.adn.map.init.LkAdnMapFormInitRouter
import io.suggest.lk.adv.direct.init.AdvDirectFormInitRouter
import io.suggest.lk.adv.geo.AdvGeoFormInitRouter
import io.suggest.lk.bill.txn.TxnsListInit
import io.suggest.lk.flash.FlashInitRouter
import io.suggest.lk.ident.center.CenterContentInitRouter
import io.suggest.lk.ident.center.captcha.{CaptchaFormInit, HiddenCaptchaInit}
import io.suggest.lk.nodes.form.LkNodesInitRouter
import io.suggest.lk.popup.PopupsInitRouter
import io.suggest.msg.ErrorMsgs
import io.suggest.sjs.common.controller.jshidden.JsHiddenInitRouter
import io.suggest.sjs.common.controller.vlines.VerticalLinesInitRouter
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.leaflet.Leaflet
import io.suggest.xadv.ext.js.form.FormEventsInitRouter
import io.suggest.xadv.ext.js.runner.c.RunnerInitRouter

import scala.scalajs.js

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.03.15 15:09
 * Description: Запуск js личного кабинета.
 */
object LkMain extends Log {

  /** Запуск скрипта на исполнение. Нужно произвести направленную инициализацию. */
  def main(args: Array[String]): Unit = {
    try {
      Leaflet.noConflict()
    } catch {
      case ex: Throwable =>
        LOG.error( ErrorMsgs.SHOULD_NEVER_HAPPEN, ex, Leaflet )
    }
    //println("L.noConflict() done. L = " + js.Dynamic.global.L.toString )

    try {
      new LkInitRouter()
        .init()
    } catch {
      case ex: Throwable =>
        LOG.error( ErrorMsgs.INIT_ROUTER_TARGET_RUN_FAIL, ex, js.Dynamic.global.L.toString )
    }

  }

}


/** Stackable-реализация routed init. */
class LkInitRouter
  extends Log
  with LkAdEditInit
  with AdvGeoFormInitRouter
  with LkAdsInit
  with LkAdnMapFormInitRouter
  with CenterContentInitRouter
  with TxnsListInit
  with CaptchaFormInit
  with HiddenCaptchaInit
  with LkNodesInitRouter
  with RunnerInitRouter
  with FormEventsInitRouter
  with AdFormInitRouter
  with FlashInitRouter
  with VerticalLinesInitRouter
  with JsHiddenInitRouter
  with PopupsInitRouter
  with NodeEditInitRouter
  with AdvDirectFormInitRouter
