package io.suggest.sc.m.inx

import io.suggest.common.empty.EmptyProduct
import io.suggest.geo.MGeoPoint
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 04.12.18 14:56
  * Description: Инфа по отображаемому индексу выдачи.
  * Появилось для возможноси "выхода" из узла в предшествующие состояния выдачи.
  */
object MIndexView {

  def empty = apply()

  @inline implicit def univEq: UnivEq[MIndexView] = UnivEq.derive

}


/** Контейнер данных состояние индекса выдачи.
  *
  * @param rcvrId id текущего узла-ресивера.
  * @param inxGeoPoint Текущая гео-точка, в которой отображается выдача.
  *                    Обычно совпадает с центром гео-карты, но не всегда.
  * @param name Название узла.
  * @param fx доп.эффект при переключении в данный view.
  */
case class MIndexView(
                       rcvrId          : Option[String]            = None,
                       inxGeoPoint     : Option[MGeoPoint]         = None,
                       name            : Option[String]            = None,
                       switchCtxOpt    : Option[MScSwitchCtx]      = None,
                     )
  extends EmptyProduct
