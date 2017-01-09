package io.suggest.common.slick.driver

import slick.driver.JdbcProfile

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.12.15 10:30
 * Description: Интерфейс для DI-поля используемого slick-драйвера.
 */
trait IDriver {

  protected val driver: JdbcProfile

}


trait IPgDriver extends IDriver {

  override protected val driver: ExPgSlickDriverT

}