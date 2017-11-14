package io.suggest.n2.edge

import diode.FastEq
import io.suggest.common.geom.d2.MSize2di
import io.suggest.file.MJsFileInfo
import io.suggest.jd.MJdEdge
import io.suggest.model.n2.edge.EdgeUid_t
import io.suggest.primo.id.IId
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.09.17 18:23
  * Description: Js-only модель-контейнер, объединяющая jd-эджи, js-файлы и srv-файлы воедино.
  * Появилась в связи с потребностями ad-edit, потому что нужно шаманить файлами на-лету, понимая
  * состояние сервера на текущий момент.
  *
  * Модель подразумевает, что эджи МОГУТ быть ассоциированы с файлом,
  * который МОЖЕТ быть на сервере И|ИЛИ на клиенте. А может и не быть.
  *
  * Модель также должна быть пригодна для выдачи, и возможных задач будущего.
  */
object MEdgeDataJs {

  def edgesDataMap2jdEdgesMap(deMap: Map[EdgeUid_t, MEdgeDataJs]): Map[EdgeUid_t, MJdEdge] = {
    deMap.mapValues { _.jdEdge }
  }

  def jdEdgesMap2EdgesDataMap(jdesMap: Map[EdgeUid_t, MJdEdge]): Map[EdgeUid_t, MEdgeDataJs] = {
    jdesMap.mapValues { MEdgeDataJs(_) }
  }


  /** Поддержка FastEq для инстансов [[MEdgeDataJs]]. */
  // TODO Не ясно, нужно ли оно, ведь инстансы живут в Map'е. Запилено на автомате.
  implicit object MEdgeDataJsFastEq extends FastEq[MEdgeDataJs] {
    override def eqv(a: MEdgeDataJs, b: MEdgeDataJs): Boolean = {
      (a.jdEdge ===* b.jdEdge) &&
        (a.fileJs ===* b.fileJs)
    }
  }

  implicit def univEq: UnivEq[MEdgeDataJs] = UnivEq.derive

}


/** Инстанс js-модели-контейнера объединённых данных по эджу, который возможно связан с файлом.
  *
  * @param jdEdge КроссПлатформенная инфа по эджу.
  * @param fileJs JS-only инфа по файлу, если есть. Обычно только в редакторе, например upload progress.
  */
case class MEdgeDataJs(
                        jdEdge    : MJdEdge,
                        fileJs    : Option[MJsFileInfo]   = None
                      )
  extends IId[Int]
{

  override final def id = jdEdge.id

  def withJdEdge(mJdEditEdge  : MJdEdge)          = copy(jdEdge = jdEdge)
  def withFileJs(fileJs       : Option[MJsFileInfo])  = copy(fileJs = fileJs)


  private def _imgSrcOrDflt(dflt: => Option[String]) = {
    fileJs
      .flatMap(_.blobUrl)
      .orElse(dflt)
  }

  def imgSrcOpt: Option[String] = {
    _imgSrcOrDflt {
      jdEdge.imgSrcOpt
    }
  }

  def origImgSrcOpt: Option[String] = {
    _imgSrcOrDflt {
      jdEdge.origImgSrcOpt
    }
  }

  def origWh: Option[MSize2di] = {
    fileJs
      .flatMap(_.whPx)
      .orElse( jdEdge.fileSrv.flatMap(_.whPx) )
  }

}
