package io.suggest.n2.edge.edit.m

import diode.FastEq
import diode.data.Pot
import io.suggest.file.MJsFileInfo
import io.suggest.lk.m.MErrorPopupS
import japgolly.univeq._
import monocle.macros.GenLens
import io.suggest.ueq.UnivEqUtil._
import io.suggest.ueq.JsUnivEqUtil._
import io.suggest.up.MFileUploadS

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
      (a.upload ===* b.upload) &&
      (a.saveReq ===* b.saveReq) &&
      (a.deleteDia ==* b.deleteDia) &&
      (a.fileJs ===* b.fileJs) &&
      (a.errorDia ===* b.errorDia) &&
      (a.fileExistNodeId ===* b.fileExistNodeId)
    }
  }

  @inline implicit def univEq: UnivEq[MEdgeEditS] = UnivEq.derive

  def nodeIds          = GenLens[MEdgeEditS]( _.nodeIds )
  def upload           = GenLens[MEdgeEditS]( _.upload )
  def saveReq          = GenLens[MEdgeEditS]( _.saveReq )
  def deleteDia        = GenLens[MEdgeEditS]( _.deleteDia )
  def fileJs           = GenLens[MEdgeEditS]( _.fileJs )
  val errorDia         = GenLens[MEdgeEditS]( _.errorDia )
  def fileExistNodeId  = GenLens[MEdgeEditS]( _.fileExistNodeId )
  def payoutData       = GenLens[MEdgeEditS]( _.payoutData )

}


/** Контейнер данных редактора эджа.
  *
  * @param nodeIds id узлов. Нужна Seq[], т.к. с Set[] будет постоянно перемешивание порядка id, затрудняя редактирование.
  * @param saveReq Состояние запроса на сервер за сохранением или удалением.
  * @param deleteDia Диалог подтверждения удаления эджа.
  * @param upload Запрос заливки файла на сервер.
  * @param fileJs Значение для MEdgeDataJs.fileJs
  */
case class MEdgeEditS(
                       nodeIds            : Seq[String],
                       upload             : MFileUploadS          = MFileUploadS.empty,
                       saveReq            : Pot[None.type]        = Pot.empty,
                       deleteDia          : MDeleteDiaS           = MDeleteDiaS.empty,
                       fileJs             : Option[MJsFileInfo]   = None,
                       errorDia           : Option[MErrorPopupS]  = None,
                       fileExistNodeId    : Option[String]        = None,
                       payoutData         : Pot[String]           = Pot.empty,
                     ) {

  lazy val isDisableSave: Boolean = {
    saveReq.isPending || payoutData.isPending || payoutData.isFailed
  }

}
