package io.suggest.slick.profile

import io.suggest.slick.profile.pg.SioPgSlickProfileT

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.12.15 10:30
 * Description: Интерфейс для DI-поля используемого slick-драйвера.
 */
trait IProfile {

  protected val profile: SioPgSlickProfileT

}
