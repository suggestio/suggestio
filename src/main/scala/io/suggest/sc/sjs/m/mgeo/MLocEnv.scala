package io.suggest.sc.sjs.m.mgeo

import io.suggest.common.empty.EmptyProduct
import io.suggest.loc.LocationConstants._

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.09.16 15:14
  * Description: Неявно-пустая модель данных для сервера о состоянии физического окружения текущего устройства.
  */
case class MLocEnv(
  geo: Option[IGeoLocMin] = None
)
  extends EmptyProduct


object MLocEnv {

  def empty = MLocEnv()

  /** Сериализатор в JSON, понятный серваку. */
  def toJson(v: MLocEnv): js.Dictionary[js.Any] = {
    val d = js.Dictionary[js.Any]()
    for (g <- v.geo)
      d.update(GEO_LOC_FN, IGeoLocMin.toJson(g))
    d
  }

}
