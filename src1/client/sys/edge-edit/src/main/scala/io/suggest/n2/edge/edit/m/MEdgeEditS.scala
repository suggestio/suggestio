package io.suggest.n2.edge.edit.m

import diode.FastEq
import diode.data.Pot
import io.suggest.file.up.MFileUploadS
import japgolly.univeq._
import monocle.macros.GenLens
import io.suggest.ueq.UnivEqUtil._
import io.suggest.ueq.JsUnivEqUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.01.2020 14:02
  * Description: Доп.состояния редакторов эджа.
  */
object MEdgeEditS {

  implicit object MEdgeEditFastEq extends FastEq[MEdgeEditS] {
    override def eqv(a: MEdgeEditS, b: MEdgeEditS): Boolean = {
      (a.nodeIds ===* b.nodeIds) &&
      (a.saveReq ===* b.saveReq) &&
      (a.deleteDia ==* b.deleteDia)
    }
  }

  @inline implicit def univEq: UnivEq[MEdgeEditS] = UnivEq.derive

  lazy val nodeIds     = GenLens[MEdgeEditS]( _.nodeIds )
  lazy val saveReq     = GenLens[MEdgeEditS]( _.saveReq )
  lazy val deleteDia   = GenLens[MEdgeEditS]( _.deleteDia )

}


/** Контейнер данных редактора эджа.
  *
  * @param nodeIds id узлов. Нужна Seq[], т.к. с Set[] будет постоянно перемешивание порядка id, затрудняя редактирование.
  * @param saveReq Состояние запроса на сервер за сохранением или удалением.
  * @param deleteDia Диалог подтверждения удаления эджа.
  * @param upload Запрос заливки файла на сервер.
  */
case class MEdgeEditS(
                       nodeIds            : Seq[String],
                       upload             : MFileUploadS          = MFileUploadS.empty,
                       saveReq            : Pot[None.type]        = Pot.empty,
                       deleteDia          : MDeleteDiaS           = MDeleteDiaS.empty,
                     )
