package models.im.make

import io.suggest.common.geom.d2.ISize2di
import io.suggest.dev.MScreen
import models.blk.SzMult_t
import models.im.{CompressMode, MImgT}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.04.15 12:18
 * Description: Модель-контейнер аргументов для запроса данных для рендера
 */

case class MImgMakeArgs(
                         img          : MImgT,
                         targetSz     : ISize2di,
                         szMult       : SzMult_t,
                         devScreenOpt : Option[MScreen],
                         compressMode : Option[CompressMode] = None
                       )
