package io.suggest.model.img

import java.time.OffsetDateTime

import io.suggest.common.geom.d2.MSize2di

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.09.15 17:15
 * Description: Связка из MSize2di и данных времени создания.
 */

case class ImgSzDated(
  sz                : MSize2di,
  dateCreated       : OffsetDateTime
)
