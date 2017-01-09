package io.suggest.common.slick.driver

import com.google.inject.AbstractModule

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.12.15 15:19
 * Description: ExPgSlickDriverT trait binding to object.
 */
class ExPgSlickDriverModule extends AbstractModule {

  override def configure(): Unit = {
    bind( classOf[ExPgSlickDriverT] )
      .toInstance( ExPgSlickDriver )
  }

}
