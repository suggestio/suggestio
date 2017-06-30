package models.usr

import javax.inject.Inject
import io.suggest.util.logs.MacroLogsImpl
import models.mproj.ICommonDi

import scala.concurrent.Future
import scala.concurrent.duration._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.04.13 18:19
 * Description: Юзер, зареганный на сайте.
 *
 * 2015.sep.24: В связи с переездом на MNode эта модель осталась для совместимости.
 * Она пока останется, но отрезана от ES, и будет содержать только какую-то утиль, упрощающую жизнь.
 */

// TODO Спилить это счастье, когда будут выставлены корректные значения в MNode.
// Тогда будет достаточно guessDisplayName*().

// Статическая часть модели.
class MPerson @Inject() (
  mPersonIdents : MPersonIdents,
  mCommonDi     : ICommonDi
)
  extends MacroLogsImpl
{

  import mCommonDi._

  /** Асинхронно найти подходящее имя юзера в хранилищах и подмоделях. */
  def findUsername(personId: String): Future[Option[String]] = {
    for (emails <- mPersonIdents.findAllEmails(personId)) yield {
      emails.headOption
    }
  }

  /** Ключ в кеше для юзернейма. */
  private def personCacheKey(personId: String) = personId + ".pu"

  /** Сколько секунд кешировать найденный юзернейм в кеше play? */
  val USERNAME_CACHE_EXPIRATION_SEC: Int = configuration.getOptional[Int]("mperson.username.cache.seconds").getOrElse(100)

  /** Асинхронно найти подходящее имя для юзера используя кеш. */
  def findUsernameCached(personId: String): Future[Option[String]] = {
    val cacheKey = personCacheKey(personId)
    cache.get[String](cacheKey).flatMap { res =>
      if (res.nonEmpty) {
        Future.successful(res)
      } else {
        val resultFut = findUsername(personId)
        resultFut onSuccess {
          case Some(result) =>
            cache.set(cacheKey, result, USERNAME_CACHE_EXPIRATION_SEC.seconds)
          case None =>
            LOGGER.warn(s"findUsernameCached($personId): Username not found for user. Invalid session?")
        }
        resultFut
      }
    }
  }

}
