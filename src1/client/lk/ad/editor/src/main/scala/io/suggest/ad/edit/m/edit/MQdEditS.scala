package io.suggest.ad.edit.m.edit

import com.quilljs.delta.Delta
import diode.FastEq
import io.suggest.ueq.UnivEqUtil._
import io.suggest.ueq.QuillUnivEqUtil._
import io.suggest.ueq.JsUnivEqUtil._
import japgolly.univeq.UnivEq
import monocle.macros.GenLens
import org.scalajs.dom

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.09.17 22:24
  * Description: Модель-контейнер данных состояния редактирования элемента контента.
  */
object MQdEditS {

  /** Поддержка FastEq для инстансов [[MQdEditS]]. */
  implicit object MQdEditSFastEq extends FastEq[MQdEditS] {
    override def eqv(a: MQdEditS, b: MQdEditS): Boolean = {
      (a.initDelta ===* b.initDelta) &&
      (a.realDelta ===* b.realDelta)
    }
  }

  @inline implicit def univEq: UnivEq[MQdEditS] = UnivEq.derive

  def initDelta = GenLens[MQdEditS](_.initDelta)
  def realDelta = GenLens[MQdEditS](_.realDelta)
  def newFiles = GenLens[MQdEditS](_.newFiles)


  implicit class QdEditExt( val qdEditS: MQdEditS ) extends AnyVal {

    def withInitRealDelta(initDelta: Delta, realDelta: Option[Delta] = None): MQdEditS =
      qdEditS.copy(initDelta = initDelta, realDelta = realDelta)

  }

}


/** Класс-контейнер данных состояния редактирования тексто-контента.
  *
  * @param initDelta Quill-delta инстанс ДЛЯ ИНИЦИАЛИЗАЦИИ/СБРОСА редактора.
  *               Т.е. он теряет актуальность во время непосредственного редактирования.
  * @param realDelta Реальная текущая дельта, актуальна в любой момент.
  *                  Используется как fallback, чтобы в момент незапланированного пере-рендера
  *                  quill-редактора, всё-таки был доступ к корректной дельте.
  *                  None означает, что текущая актуальная дельта лежит в initDelta.
  * @param newFiles Карта-очередь из файлов, которые проброшены из Quill-редактора в обход дельты.
  */
case class MQdEditS(
                     initDelta        : Delta,
                     realDelta        : Option[Delta]             = None,
                     newFiles         : Map[String, dom.File]     = Map.empty,
                   )
