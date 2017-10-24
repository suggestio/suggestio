package io.suggest.jd

import io.suggest.common.geom.d2.ISize2di

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.10.17 17:36
  * Description: Кросс-платформенная модель инфы по эджу для его валидации оного.
  * Появилась для возможности валидации на клиенте и на сервере, без привязки к конкретным сложным моделям эджей.
  */
case class MJdEdgeVldInfo(
                           jdEdge : MJdEditEdge,
                           img    : Option[MEdgePicInfo]
                         )



/** Модель для доступа к данным эджа и связанный с ним картинки. */
case class MEdgePicInfo(
                         isImg     : Boolean,
                         imgWh     : Option[ISize2di]
                       )

