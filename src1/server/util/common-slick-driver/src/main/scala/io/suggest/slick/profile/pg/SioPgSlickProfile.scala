package io.suggest.slick.profile.pg

import com.github.tminglei.slickpg._
import slick.sql.SqlAction

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.11.15 9:38
 * Description: Slick-драйвер для postgres'а.
 *
 * @see [[https://github.com/tminglei/slick-pg#usage]]
 */
trait SioPgSlickProfileT
  extends ExPostgresProfile
  with PgArraySupport
  with PgDate2Support
{

  /** Реализация API расширенного slick-pg-драйвера. */
  trait ExPgApiT
    extends API
    // slick-pg: Plain-импорты для sql-интерполятора, просто import'ы для lifted api.
    with ArrayImplicits with SimpleArrayPlainImplicits
    with DateTimeImplicits
  {

    protected val _strArrayTypeMapper = new SimpleArrayJdbcType[String]("text")

    //implicit val strListTypeMapper    = _strArrayTypeMapper.to(_.toList)
    implicit val strSeqTypeMapper     = _strArrayTypeMapper.to(_.toSeq)

    /** Костыль-поддержка, связанный с [[https://github.com/slick/slick/issues/92]]. */
    // TODO Когда в slick наконец реализуют поддержку FOR-LOCK clauses, нужно его удалить.
    implicit class SelectForExtensionMethods[R, S <: NoStream, E <: Effect.Read](val a: SqlAction[R, S, E]) {

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
object SioPgSlickProfile extends SioPgSlickProfileT
