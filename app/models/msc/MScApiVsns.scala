package models.msc

import io.suggest.common.menum.EnumMaybeWithId
import play.api.mvc.QueryStringBindable
import util.PlayMacroLogsImpl

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.05.15 13:35
  * Description: Версии API системы выдачи, чтобы сервер мог подстраиватсья под клиентов разных поколений.
  */
object MScApiVsns extends Enumeration with EnumMaybeWithId with PlayMacroLogsImpl {

  /** Экземпляр модели версий. */
  protected[this] class Val(val versionNumber: Int) extends super.Val(versionNumber) {

    override def toString(): String = id.toString

  }

  override type T = Val


  // 2016.jul.14 Наконец удаление выдачи v1 Coffee. Удаление полей Val. Осталась только Sjs1 выдача.

  /** Выдача, переписанная на scala.js. Выдача второго поколения или просто "sc v2". */
  val Sjs1: T = new Val(2)


  /** Какую версию использовать, если версия API не указана? */
  def unknownVsn: T = {
    Sjs1
  }

  /** Биндинги для url query string. */
  implicit def qsb(implicit intB: QueryStringBindable[Int]): QueryStringBindable[MScApiVsn] = {
    new QueryStringBindable[MScApiVsn] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MScApiVsn]] = {
        val optRes = for {
          maybeVsn <- intB.bind(key, params)
        } yield {
          maybeVsn.right.flatMap { vsnNum =>
            maybeWithId(vsnNum) match {
              case Some(vsn) =>
                Right(vsn)
              case None =>
                // Довольно неожиданная ситуация, что выкинута версия, используемая на клиентах. Или ксакеп какой-то ковыряется.
                val msg = "Unknown API version: " + vsnNum
                LOGGER.warn(msg, new Throwable)
                Left(msg)
            }
          }
        }
        // Если версия не задана вообще, то выставить её в дефолтовую. Первая выдача не возвращала никаких версий.
        optRes.orElse {
          Some( Right(unknownVsn) )
        }
      }

      override def unbind(key: String, value: MScApiVsn): String = {
        intB.unbind(key, value.versionNumber)
      }
    }
  }

}
