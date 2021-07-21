package io.suggest.lk

import io.suggest.ad.edit.LkAdEditInit
import io.suggest.adn.edit.LkAdnEditInit
import io.suggest.ads.LkAdsInit
import io.suggest.bill.cart.CartPageInit
import io.suggest.id.login.LoginFormInit
import io.suggest.lk.adn.map.LkAdnMapFormInitRouter
import io.suggest.lk.adv.geo.AdvGeoFormInitRouter
import io.suggest.lk.flash.FlashInitRouter
import io.suggest.lk.nodes.form.LkNodesInitRouter
import io.suggest.msg.ErrorMsgs
import io.suggest.log.Log
import io.suggest.sjs.JsApiUtil
import io.suggest.sjs.leaflet.{Leaflet, LeafletGlobal}
import io.suggest.sys.mdr.SysMdrInit
import io.suggest.xadv.ext.js.form.FormEventsInitRouter
import io.suggest.xadv.ext.js.runner.c.AdvExtRunnerInitRouter
import io.suggest.xplay.json.PlayJsonSjsUtil

import scala.util.Try

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.03.15 15:09
 * Description: Запуск js личного кабинета.
 */
object LkMain extends Log {

  /** Запуск скрипта на исполнение. Нужно произвести направленную инициализацию. */
  def main(args: Array[String]): Unit = {
    PlayJsonSjsUtil.init()

    if ( JsApiUtil.isDefinedSafe(LeafletGlobal.L) )
      Try( Leaflet.noConflict() )

    try {
      new LkInitRouter()
        .init()
    } catch {
      case ex: Throwable =>
        logger.error( ErrorMsgs.INIT_ROUTER_TARGET_RUN_FAIL, ex )
    }
  }

}


/** Stackable-реализация routed init. */
class LkInitRouter
  extends Log
  with LoginFormInit
  with LkAdEditInit
  with AdvGeoFormInitRouter
  with LkAdsInit
  with LkAdnMapFormInitRouter
  with LkNodesInitRouter
  with AdvExtRunnerInitRouter
  with FormEventsInitRouter
  with FlashInitRouter
  with LkAdnEditInit
  with CartPageInit
  with SysMdrInit
