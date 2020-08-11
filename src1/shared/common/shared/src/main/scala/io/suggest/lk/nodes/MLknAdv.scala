package io.suggest.lk.nodes

import io.suggest.common.empty.EmptyProduct
import japgolly.univeq.UnivEq
import monocle.macros.GenLens
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.08.2020 22:22
  * Description: Модель данных текущего размещения карточки на узле в форме lk-nodes.
  */
object MLknAdv {

  implicit def mLknAdvJson: OFormat[MLknAdv] = {
    (
      (__ \ "d").format[Boolean] and
      (__ \ "o").format[Boolean] and
      (__ \ "u").format[Boolean]
    )(apply, unlift(unapply))
  }

  @inline implicit def univEq: UnivEq[MLknAdv] = UnivEq.derive

  def hasAdv = GenLens[MLknAdv](_.hasAdv)
  def advShowOpened = GenLens[MLknAdv](_.advShowOpened)
  def alwaysOutlined = GenLens[MLknAdv](_.alwaysOutlined)

}


/** Контейнер данных размещения рекламной карточки.
  *
  * @param hasAdv Имеется ли размещение текущей рекламной карточки на указанном узле?
  * @param advShowOpened Рендерить размещённую карточку открытой?
  * @param alwaysOutlined Постоянная обводка?
  */
case class MLknAdv(
                    hasAdv                 : Boolean,
                    advShowOpened          : Boolean,
                    alwaysOutlined         : Boolean,
                  )
  extends EmptyProduct
