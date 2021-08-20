package io.suggest.model

import io.suggest.slick.profile.pg.SioPgSlickProfileT
import play.api.db.slick.DatabaseConfigProvider

import javax.inject.Inject

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Description: DI Container for slick API access.
 */
final class SlickHolder @Inject()(
                                 val _slickConfigProvider   : DatabaseConfigProvider
                               ) {

  val slick = _slickConfigProvider.get[SioPgSlickProfileT]

}