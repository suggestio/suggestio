package util.di

import play.api.db.slick.DatabaseConfigProvider
import slick.driver.JdbcProfile

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.12.15 21:17
 * Description: Интефейс DI-полю с инстансом провайдера db-конфигурации play-slick.
 */
trait ISlickDbConfigProvider {

  protected def dbConfigProvider: DatabaseConfigProvider

}


/** Получение инстанса dbConfig, с которым можно работать. */
trait ISlickDbConfig extends ISlickDbConfigProvider {

  protected val dbConfig = dbConfigProvider.get[JdbcProfile]

}
