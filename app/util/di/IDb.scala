package util.di

import play.api.db.Database

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.10.15 13:40
 * Description: Интерфейс для доступа к database api (jdbc), инжектируемого через DI.
 *
 * 2016.feb: уход от использования anorm в сторону slick. Этот трейт не используется,
 * но поддержка ipgeobase пока на anorm без DI.
 */
trait IDb {
  def db: Database
}
