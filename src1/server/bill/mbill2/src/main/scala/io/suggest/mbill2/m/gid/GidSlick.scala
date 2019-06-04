package io.suggest.mbill2.m.gid

import io.suggest.mbill2.m.common.ModelContainer

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.12.15 10:19
 * Description: Во многих таблицах есть колонка global id.
 * Этот трейт подмешивается в DI-контейнер, затем GidColumn подмешивается к таблице,
 * и поле id добавляется в шейп.
 */
trait GidSlick extends IdSlick {

  import profile.api._

  def ID_FN = "id"

  override type Id_t = Gid_t
  override type Table_t <: Table[El_t] with GidColumn

  /** Добавить поддержку gid колонки. */
  trait GidColumn extends IdColumn { that: Table[_] =>

    def id = column[Gid_t](ID_FN, O.PrimaryKey, O.AutoInc)

  }

}


trait IdSlick extends ModelContainer {
  import profile.api._

  override type Table_t <: Table[El_t] with IdColumn

  trait IdColumn {
    def id: Rep[Id_t]
  }
}
