package util.di

import play.api.db.Database

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.10.15 13:40
 * Description: Интерфейс для доступа к database api (jdbc), инжектируемого через DI.
 */
trait IDb {
  def db: Database
}
