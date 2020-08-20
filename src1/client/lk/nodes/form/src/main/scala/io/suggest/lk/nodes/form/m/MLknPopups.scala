package io.suggest.lk.nodes.form.m

import diode.FastEq
import io.suggest.common.empty.EmptyProduct
import io.suggest.lk.m.MDeleteConfirmPopupS
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.03.17 22:27
  * Description: Модель состояния подсистемы попапов react-формы LkNodes.
  */
object MLknPopups {

  def empty = MLknPopups()

  implicit object MLknPopupsFastEq extends FastEq[MLknPopups] {
    override def eqv(a: MLknPopups, b: MLknPopups): Boolean = {
      (a.createNodeS ===* b.createNodeS) &&
      (a.deleteNodeS ===* b.deleteNodeS) &&
      (a.editTfDailyS ===* b.editTfDailyS) &&
      (a.editName ===* b.editName)
    }
  }

  val createNodeS = GenLens[MLknPopups](_.createNodeS)
  val deleteNodeS = GenLens[MLknPopups](_.deleteNodeS)
  val editTfDailyS = GenLens[MLknPopups](_.editTfDailyS)
  val editName = GenLens[MLknPopups](_.editName)

  @inline implicit def univEq: UnivEq[MLknPopups] = UnivEq.derive

}


/** Класс модели состояния попапов формы управления узлами.
  * Неявно-пустая модель.
  *
  * @param createNodeS Состояние попапа добавления узла, если есть.
  * @param deleteNodeS Состояние попапа удаления узла, если есть.
  * @param editName Состояние редактирования названия.
  */
case class MLknPopups(
                       createNodeS   : Option[MCreateNodeS]             = None,
                       deleteNodeS   : Option[MDeleteConfirmPopupS]     = None,
                       editTfDailyS  : Option[MEditTfDailyS]            = None,
                       editName      : Option[MEditNodeState]           = None,
                     )
  extends EmptyProduct
