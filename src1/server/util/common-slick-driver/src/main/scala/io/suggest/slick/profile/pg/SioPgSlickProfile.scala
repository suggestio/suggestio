package io.suggest.slick.profile.pg

import com.github.tminglei.slickpg._
import slick.jdbc.{ResultSetConcurrency, ResultSetType}
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
    with SimpleArrayPlainImplicits
    with DateTimeImplicits
  {

    /** Костыли для SqlAction */
    implicit class SqlActionPgExtensionMethods[R, S <: NoStream, E <: Effect.Read](val a: SqlAction[R, S, E]) {

      /** Заблокировать найденные ряды для последующего апдейта.
        *
        * @see Костыль-поддержка, связанный с [[https://github.com/slick/slick/issues/92]].
         */
      // TODO Когда в slick наконец реализуют поддержку FOR-LOCK clauses, нужно его удалить.
      def forUpdate = {
        a.overrideStatements {
          a.statements.map { _ + " FOR UPDATE" }
        }
      }

    }


    /** Костыли для DBIOAction. */
    implicit class SioDbioActionPgOpsExt[R, S <: NoStream, E <: Effect.Read](val a: DBIOAction[R, S, E]) {

      /** Согласно докам slick, для эффективного db.stream() для postgresql требуются некоторые костыли.
        * Эта функция окостыливает экшен для достижения макс.производительности.
        * Её нужно вызывать аналогично forUpdate, т.е. в финале.
        *
        * @see [[http://slick.lightbend.com/doc/3.2.0/dbio.html?highlight=stream#streaming]]
        */
      def forPgStreaming(fetchSize: Int) = {
        a.withStatementParameters(
          rsType        = ResultSetType.ForwardOnly,
          rsConcurrency = ResultSetConcurrency.ReadOnly,
          fetchSize     = fetchSize
        )
          .transactionally
      }

    }

  }

  protected class ExPgApi extends ExPgApiT

  override val api: ExPgApiT = new ExPgApi

}


/** Дефолтовая реализация slick-pg-драйвера. */
object SioPgSlickProfile extends SioPgSlickProfileT
