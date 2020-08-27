package io.suggest.lk.nodes.form.m

import enumeratum.{Enum, EnumEntry}
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.08.2020 15:37
  * Description: Роли узлов в дереве.
  */
object MTreeRoles extends Enum[MTreeRole] {

  /** Корневой элемент.
    * Чтобы дерево было однотонно, нужен невидимый "нулевой" элемент у основания дерева,
    * под которым живут все остальные элементы.
    */
  case object Root extends MTreeRole

  /** Существующий узел N2, выраженный узлом дерева nodes-формы.
    * Узлы приходят с сервера или отправляются на сервер. */
  case object Normal extends MTreeRole

  /** Виртуальный видимый элемент дерева, визуально группирующий обнаруженные маячки поблизости.
    * Действия для элемента просты: Можно свернуть-развернуть (или даже этого нельзя).
    */
  case object BeaconsDetected extends MTreeRole

  /** Сигнал от bluetooth-маячка поблизости. */
  case object BeaconSignal extends MTreeRole


  override def values = findValues

}


/** Класс элемента модели [[MTreeRoles]]. */
sealed abstract class MTreeRole extends EnumEntry

object MTreeRole {
  @inline implicit def univEq: UnivEq[MTreeRole] = UnivEq.derive

  implicit final class TreeRoleExt( private val treeRole: MTreeRole ) extends AnyVal {

    /** Разворачивать ли какие-то данные раскрытого узла дерева? */
    def canRenderDetails: Boolean = {
      (treeRole ==* MTreeRoles.Normal) ||
      (treeRole ==* MTreeRoles.BeaconSignal)
    }

  }

}
