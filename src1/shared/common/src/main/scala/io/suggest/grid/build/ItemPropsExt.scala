package io.suggest.grid.build

import io.suggest.ad.blk.BlockMeta

import scala.language.implicitConversions

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.11.17 18:38
  * Description: Модель-контейнер расширенных аттрибутов одного item'а.
  */
case class ItemPropsExt(
                         blockMeta: BlockMeta
                       )

