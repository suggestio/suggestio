package io.suggest.sc.sjs.m.msc.fsm

import io.suggest.sc.sjs.m.magent.IMScreen
import io.suggest.sc.sjs.m.mgeo._
import io.suggest.sc.sjs.m.mgrid.MGridData
import io.suggest.sjs.common.model.browser.{IBrowser, MBrowser}

import scala.scalajs.js

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.06.15 17:33
 * Description: Модель состояния конечного автомата интерфейса выдачи.
 */
object MStData {

  private def generationDflt = (js.Math.random() * 1000000000).toLong
  private def browserDflt    = MBrowser.detectBrowser

}


/**
 * Экземпляр immutable-контейнера для передачи данных Sc FSM между состояниями.
 * @param screen Данные по экрану, если известны.
 * @param geoLoc Текущие данные по геолокации.
 * @param generation Поколение выдачи для сортировки. Появилось из MSrv.generation.
 * @param adnIdOpt id текущего узла, если есть.
 */
case class MStData(
  screen                    : Option[IMScreen]  = None,
  override val geoLoc       : Option[MGeoLoc]   = None,
  grid                      : MGridData         = MGridData(),
  generation                : Long              = MStData.generationDflt,
  adnIdOpt                  : Option[String]    = None,
  browser                   : IBrowser          = MStData.browserDflt
)
  extends MGeoLocUtil
