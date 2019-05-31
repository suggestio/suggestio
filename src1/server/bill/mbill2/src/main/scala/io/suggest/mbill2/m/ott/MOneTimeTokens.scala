package io.suggest.mbill2.m.ott

import java.time.Instant
import java.util.UUID

import io.suggest.common.m.sql.ITableName
import io.suggest.mbill2.m.common.ModelContainer
import io.suggest.mbill2.m.dt.{DateCreatedSlick, DateEndSlick}
import io.suggest.slick.profile.pg.SioPgSlickProfileT
import javax.inject.{Inject, Singleton}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.05.19 17:37
  * Description: slick-модель для хранения одноразовых токенов на стороне сервера.
  * Изначально с биллингом не пересекалась, но сам модуль плавно вырастает за пределы просто биллинга.
  */
@Singleton
class MOneTimeTokens @Inject() (
                                 override protected val profile      : SioPgSlickProfileT,
                               )
  extends DateCreatedSlick
  with DateEndSlick
  with TokenIdSlick
  with ITableName
  with ModelContainer
{

  import profile.api._


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
    override def * = {
      (tokenId, dateCreated, dateEnd) <> (
        (MOneTimeToken.apply _).tupled, MOneTimeToken.unapply
      )
    }
  }

  override val query = TableQuery[MOneTimeTokensTable]

}


/** Класс описания одного токена одноразового.
  *
  * @param tokenId рандомный id токена.
  * @param dateCreated Дата создания токена.
  * @param deleteAfter Дата удаления токена из БД.
  */
case class MOneTimeToken(
                          tokenId       : UUID,
                          dateCreated   : Instant,
                          deleteAfter   : Instant,
                        )
