package io.suggest.lk.nodes

import japgolly.univeq.UnivEq
import monocle.macros.GenLens
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.03.17 16:51
  * Description: Кросс-платформенная модель разных важных данных формы.
  * Обычно, она собирается на сервере и неизменно живёт внутри состояния формы.
  */

object MLknConf {

  @inline implicit def univEq: UnivEq[MLknConf] = UnivEq.derive

  implicit def mLknConfFormat: OFormat[MLknConf] = (
    (__ \ "n").formatNullable[String] and
    (__ \ "a").formatNullable[String]
  )(apply, unlift(unapply))


  def onNodeId = GenLens[MLknConf]( _.onNodeId )
  def adIdOpt = GenLens[MLknConf]( _.adIdOpt )

}


/** Конфиг формы.
  *
  * @param onNodeId Исходный id узла, на котором изначально открывается выдача.
  * @param adIdOpt id узла с карточкой, для ad-режима дерева узлов.
  */
case class MLknConf(
                     onNodeId : Option[String],
                     adIdOpt  : Option[String],
                   )
