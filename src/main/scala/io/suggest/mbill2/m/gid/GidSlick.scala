package io.suggest.mbill2.m.gid

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.12.15 10:19
 * Description: Во многих таблицах есть колонка global id.
 */
object GidSlick {

  def ID_FN = "id"

}


import GidSlick._
import io.suggest.common.slick.driver.IDriver


/** Этот трейт подмешивается в DI-контейнер, затем GidColumn подмешивается к таблице, и вуаля. */
trait GidSlick extends IDriver {

  import driver.api._

  /** Добавить поддержку gid колонки. */
  trait GidColumn { that: Table[_] =>

    def id = column[Long](ID_FN, O.PrimaryKey, O.AutoInc)

  }

}
