package io.suggest.mbill2.m.ott

import java.time.Instant
import java.util.UUID

import io.suggest.common.m.sql.ITableName
import io.suggest.mbill2.m.common.{InsertManyReturning, InsertUuidOneReturning, ModelContainer}
import io.suggest.mbill2.m.dt.{DateCreatedSlick, DateEndSlick}
import io.suggest.mbill2.m.gid.GetByUuid
import io.suggest.slick.profile.pg.SioPgSlickProfileT
import japgolly.univeq.UnivEq
import javax.inject.Inject
import monocle.macros.GenLens
import io.suggest.ueq.UnivEqUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.05.19 17:37
  * Description: slick-модель для хранения одноразовых токенов на стороне сервера.
  * Изначально с биллингом не пересекалась, но сам модуль плавно вырастает за пределы просто биллинга.
  */
class MOneTimeTokens @Inject() (
                                 override protected val profile      : SioPgSlickProfileT,
                               )
  extends DateCreatedSlick
  with DateEndSlick
  with TokenIdSlick
  with ITableName
  with ModelContainer
  with InsertUuidOneReturning
  with InsertManyReturning
  with GetByUuid
{

  import profile.api._

  override protected def _withId(el: MOneTimeToken, id: UUID): MOneTimeToken =
    (MOneTimeToken.id set id)(el)

  override type Id_t    = UUID
  override type Table_t = MOneTimeTokensTable
  override type El_t    = MOneTimeToken

  override val TABLE_NAME   = "one_time_token"


  /** Slick-описание таблицы заказов. */
  class MOneTimeTokensTable(tag: Tag)
    extends Table[MOneTimeToken](tag, TABLE_NAME)
    with TokenIdColumn
    with DateCreatedInstantColumn
    with DateEndInstantColumn
  {
    def info = column[Option[String]]("info")

    override def * = {
      (id, dateCreated, dateEnd, info) <> (
        (MOneTimeToken.apply _).tupled, MOneTimeToken.unapply
      )
    }
  }

  override val query = TableQuery[MOneTimeTokensTable]

  /** Найти и удалить старые неактуальные токены.
    *
    * @return DB-экшен, возвращающий кол-во удалённых рядов.
    */
  def deleteOld(): DBIOAction[Int, NoStream, Effect.Write] = {
    val now = Instant.now()
    query
      .filter { ott =>
        ott.dateEnd <= now
      }
      .delete
  }


  /** Обновление одного токена.
    *
    * @param ott Обновлённый токен.
    * @return Экшен, возвращающий кол-во обновлённых рядов (0 или 1).
    */
  def updateExisting(ott: MOneTimeToken): DBIOAction[Int, NoStream, Effect.Write] = {
    query
      .filter { o =>
        o.id === ott.id
      }
      .map { o =>
        (o.dateEnd, o.info)
      }
      .update((ott.dateEnd, ott.info))
  }

}


/** Класс описания одного токена одноразового.
  *
  * @param id рандомный id токена.
  * @param dateCreated Дата создания токена.
  * @param dateEnd Дата удаления токена из БД.
  * @param info Опциональные произвольные данные.
  */
case class MOneTimeToken(
                          id            : UUID,
                          dateCreated   : Instant         = Instant.now(),
                          dateEnd       : Instant,
                          info          : Option[String]  = None,
                        )
object MOneTimeToken {

  def id        = GenLens[MOneTimeToken](_.id)
  def dateEnd   = GenLens[MOneTimeToken](_.dateEnd)
  def info      = GenLens[MOneTimeToken](_.info)

  @inline implicit def univEq: UnivEq[MOneTimeToken] = UnivEq.derive

}
