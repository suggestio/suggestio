package io.suggest.slick.profile.pg

import io.suggest.slick.profile.IProfile

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.12.15 10:30
 * Description: Интерфейс для DI-поля используемого pg-slick-драйвера.
 */


trait IPgProfile extends IProfile {

  override protected val profile: SioPgSlickProfileT

}
