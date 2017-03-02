package io.suggest.www.util.di

import io.suggest.slick.profile.pg.SioPgSlickProfileT
import play.api.db.slick.DatabaseConfigProvider

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.12.15 21:17
 * Description: Интефейс DI-полю с инстансом провайдера db-конфигурации play-slick.
 */
trait ISlickDbConfigProvider {

  def _slickConfigProvider: DatabaseConfigProvider

}

trait ISlickDbConfig extends ISlickDbConfigProvider {

  val slick = _slickConfigProvider.get[SioPgSlickProfileT]

}

