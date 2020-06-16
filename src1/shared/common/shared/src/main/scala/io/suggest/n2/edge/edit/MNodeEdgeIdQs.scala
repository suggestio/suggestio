package io.suggest.n2.edge.edit

import diode.FastEq
import japgolly.univeq.UnivEq
import monocle.macros.GenLens
import play.api.libs.json._
import play.api.libs.functional.syntax._
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.01.2020 14:57
  * Description: Конфигурация для формы редактирования эджа.
  */
object MNodeEdgeIdQs {

  implicit object MNodeEdgeIdQsFeq extends FastEq[MNodeEdgeIdQs] {
    override def eqv(a: MNodeEdgeIdQs, b: MNodeEdgeIdQs): Boolean = {
      (a.nodeId ===* b.nodeId) &&
      (a.nodeVsn ==* b.nodeVsn) &&
      (a.edgeId ===* b.edgeId)
    }
  }

  @inline implicit def univEq: UnivEq[MNodeEdgeIdQs] = UnivEq.derive


  /** Контейнер с именами полей модели. */
  object Fields {
    /** Имя поля с id узла. */
    def NODE_ID_FN  = "n"
    /** Имя поля с номером es-версии узла. */
    def NODE_VSN_FN = "v"
    /** id (порядковый номер) эджа. */
    def EDGE_ID_FN  = "e"
  }


  /** Поддержка JSON. */
  implicit def edgeEditFormConfJson: OFormat[MNodeEdgeIdQs] = {
    val F = Fields
    (
      (__ \ F.NODE_ID_FN).format[String] and
      (__ \ F.NODE_VSN_FN).format[Long] and
      (__ \ F.EDGE_ID_FN).formatNullable[Int]
    )(apply, unlift(unapply))
  }

  def nodeVsn = GenLens[MNodeEdgeIdQs]( _.nodeVsn )

}


/** Контейнер данных-координат эджа.
  *
  * @param nodeId id узла.
  * @param nodeVsn версия редактируемого узла.
  * @param edgeId id редактируемая эджа.
  *               Если создание нового эджа, то None.
  */
case class MNodeEdgeIdQs(
                          nodeId      : String,
                          nodeVsn     : Long,
                          edgeId      : Option[Int] = None,
                        )
