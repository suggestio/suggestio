package io.suggest.lk.main.sjs

import io.suggest.ad.edit.LkAdEditInit
import io.suggest.adn.edit.LkAdnEditInit
import io.suggest.ads.LkAdsInit
import io.suggest.bill.cart.CartPageInit
import io.suggest.lk.adn.map.LkAdnMapFormInitRouter
import io.suggest.lk.adv.geo.AdvGeoFormInitRouter
import io.suggest.lk.flash.FlashInitRouter
import io.suggest.lk.ident.center.CenterContentInitRouter
import io.suggest.lk.ident.center.captcha.{CaptchaFormInit, HiddenCaptchaInit}
import io.suggest.lk.nodes.form.LkNodesInitRouter
import io.suggest.msg.ErrorMsgs
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.leaflet.Leaflet
import io.suggest.sys.mdr.SysMdrInit
import io.suggest.xadv.ext.js.form.FormEventsInitRouter
import io.suggest.xadv.ext.js.runner.c.AdvExtRunnerInitRouter

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
      case _: Throwable =>
    }

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
  with CaptchaFormInit
  with HiddenCaptchaInit
  with LkNodesInitRouter
  with AdvExtRunnerInitRouter
  with FormEventsInitRouter
  with FlashInitRouter
  with LkAdnEditInit
  with CartPageInit
  // TODO унести в SysInitRouter, когда он будет
  with SysMdrInit
