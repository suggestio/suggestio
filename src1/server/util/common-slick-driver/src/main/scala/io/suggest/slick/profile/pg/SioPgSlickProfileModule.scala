package io.suggest.slick.profile.pg

import com.google.inject.AbstractModule

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.12.15 15:19
 * Description: ExPgSlickDriverT trait binding to object.
 */
class SioPgSlickProfileModule extends AbstractModule {

  override def configure(): Unit = {
    bind( classOf[SioPgSlickProfileT] )
      .toInstance( SioPgSlickProfile )
  }

}
