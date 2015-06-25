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


/** Интерфейс модели. */
trait IStData extends MGeoLocUtil {

  def screen      : Option[IMScreen]
  def grid        : MGridData
  def generation  : Long
  def adnIdOpt    : Option[String]
  def browser     : IBrowser

}


/**
 * Экземпляр immutable-контейнера для передачи данных Sc FSM между состояниями.
 * @param screen Данные по экрану, если известны.
 * @param geoLoc Текущие данные по геолокации.
 * @param generation Поколение выдачи для сортировки. Появилось из MSrv.generation.
 * @param adnIdOpt id текущего узла, если есть.
 */
case class MStData(
  override val screen       : Option[IMScreen]  = None,
  override val geoLoc       : Option[MGeoLoc]   = None,
  override val grid         : MGridData         = MGridData(),
  override val generation   : Long              = MStData.generationDflt,
  override val adnIdOpt     : Option[String]    = None,
  override val browser      : IBrowser          = MStData.browserDflt
)
  extends IStData
