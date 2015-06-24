package io.suggest.sc.sjs.m.msc.fsm

import io.suggest.sc.sjs.m.magent.IMScreen
import io.suggest.sc.sjs.m.mgeo._
import io.suggest.sc.sjs.m.mgrid.{MGridUtil, MGridParams, MGridState}

import scala.scalajs.js

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.06.15 17:33
 * Description: Модель состояния конечного автомата интерфейса выдачи.
 */
object MStData {

  def gridParamsDflt = MGridParams()
  def gridStateDflt  = MGridState()
  def generationDflt = (js.Math.random() * 1000000000).toLong

}


/**
 * Экземпляр immutable-контейнера для передачи данных Sc FSM между состояниями.
 * @param screen Данные по экрану, если известны.
 * @param geoLoc Текущие данные по геолокации.
 * @param gridParams Параметры сетки выдачи.
 *                   Изменяюстя редко, но при измении содержимое gridState становится противоречивым.
 * @param gridState Состояние плитки карточек. Появилось на основе MGrid.state.
 * @param generation Поколение выдачи для сортировки. Появилось из MSrv.generation.
 * @param adnIdOpt id текущего узла, если есть.
 */
case class MStData(
  screen                    : Option[IMScreen]  = None,
  override val geoLoc       : Option[MGeoLoc]   = None,
  override val gridParams   : MGridParams       = MStData.gridParamsDflt,
  override val gridState    : MGridState        = MStData.gridStateDflt,
  generation                : Long              = MStData.generationDflt,
  adnIdOpt                  : Option[String]    = None
)
  extends MGridUtil
  with MGeoLocUtil
