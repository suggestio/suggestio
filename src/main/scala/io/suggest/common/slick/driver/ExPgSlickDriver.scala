package io.suggest.common.slick.driver

import com.github.tminglei.slickpg.{ExPostgresDriver, PgArraySupport, PgDateSupportJoda}
import slick.profile.SqlAction

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.11.15 9:38
 * Description: Slick-драйвер для postgres'а.
 *
 * @see [[https://github.com/tminglei/slick-pg#usage]]
 */
trait ExPgSlickDriverT
  extends ExPostgresDriver
  with PgArraySupport
  with PgDateSupportJoda
{

  /** Реализация API расширенного slick-pg-драйвера. */
  trait ExPgApiT
    extends API
    with ArrayImplicits
    with JodaDateTimeImplicits
  {

    protected val _strArrayTypeMapper = new SimpleArrayJdbcType[String]("text")

    //implicit val strListTypeMapper    = _strArrayTypeMapper.to(_.toList)
    implicit val strSeqTypeMapper     = _strArrayTypeMapper.to(_.toSeq)

    /** Костыль-поддержка, связанный с [[https://github.com/slick/slick/issues/92]]. */
    // TODO Когда в slick наконец реализуют поддержку FOR-LOCK clauses, нужно его удалить.
    implicit class SelectForExtensionMethods[E <: Effect.Read, R, S <: NoStream](val a: SqlAction[R, S, E]) {

      /** Заблокировать выбранные ряды  */
      def forUpdate = {
        a.overrideStatements {
          a.statements.map { _ + " FOR UPDATE" }
        }
      }
    }

  }

  object ExPgApi extends ExPgApiT

  override val api = ExPgApi

}


/** Дефолтовая реализация slick-pg-драйвера. */
object ExPgSlickDriver extends ExPgSlickDriverT
