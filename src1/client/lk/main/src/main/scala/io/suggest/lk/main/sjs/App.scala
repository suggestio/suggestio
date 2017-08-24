package io.suggest.lk.main.sjs

import io.suggest.ad.edit.LkAdEditInit
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
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.controller.jshidden.JsHiddenInitRouter
import io.suggest.sjs.common.controller.vlines.VerticalLinesInitRouter
import io.suggest.sjs.common.log.Log
import io.suggest.xadv.ext.js.form.FormEventsInitRouter
import io.suggest.xadv.ext.js.runner.c.RunnerInitRouter

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.03.15 15:09
 * Description: Запуск js личного кабинета.
 */
object App extends Log {

  /** Запуск скрипта на исполнение. Нужно произвести направленную инициализацию. */
  def main(args: Array[String]): Unit = {
    val initFut = new LkInitRouter()
      .init()

    for (ex <- initFut.failed) {
      LOG.error(msg = "Init failed", ex = ex)
    }
  }

}


/** Stackable-реализация routed init. */
class LkInitRouter
  extends Log
  with CenterContentInitRouter
  with LkAdEditInit
  with TxnsListInit
  with CaptchaFormInit
  with HiddenCaptchaInit
  with RunnerInitRouter
  with FormEventsInitRouter
  with AdFormInitRouter
  with FlashInitRouter
  with VerticalLinesInitRouter
  with JsHiddenInitRouter
  with PopupsInitRouter
  with NodeEditInitRouter
  with AdvGeoFormInitRouter
  with AdvDirectFormInitRouter
  with LkAdnMapFormInitRouter
  with LkNodesInitRouter
