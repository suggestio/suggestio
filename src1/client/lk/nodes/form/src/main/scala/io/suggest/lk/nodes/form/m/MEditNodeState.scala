package io.suggest.lk.nodes.form.m

import diode.data.Pot
import japgolly.univeq._
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.03.17 15:01
  * Description: Модель состояния редактирования узла.
  */
object MEditNodeState {

  @inline implicit def univEq: UnivEq[MEditNodeState] = UnivEq.force

  val saving = GenLens[MEditNodeState](_.saving)

  implicit class EnsOptExt( private val ensOpt: Option[MEditNodeState] ) extends AnyVal {
    def name: String = ensOpt.fold("")(_.name)
    def nameValid: Boolean = ensOpt.exists(_.nameValid)
    def isPending: Boolean = ensOpt.exists(_.saving.isPending)
    def exceptionOption = ensOpt.flatMap(_.exceptionOption)
  }

}


case class MEditNodeState(
                           name      : String,
                           nameValid : Boolean,
                           saving    : Pot[_]          = Pot.empty
                         ) {

  lazy val exceptionOption = saving.exceptionOption

}
