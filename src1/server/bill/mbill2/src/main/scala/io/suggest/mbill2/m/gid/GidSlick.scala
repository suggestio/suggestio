package io.suggest.mbill2.m.gid

import io.suggest.common.slick.driver.IDriver

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.12.15 10:19
 * Description: Во многих таблицах есть колонка global id.
 * Этот трейт подмешивается в DI-контейнер, затем GidColumn подмешивается к таблице,
 * и поле id добавляется в шейп.
 */
trait GidSlick extends IDriver {

  import driver.api._

  def ID_FN = "id"

  /** Добавить поддержку gid колонки. */
  trait GidColumn { that: Table[_] =>

    def id = column[Gid_t](ID_FN, O.PrimaryKey, O.AutoInc)

  }

}
