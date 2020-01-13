package io.suggest.form

import diode.FastEq
import io.suggest.n2.edge.EdgeUid_t
import io.suggest.scalaz.NodePath_t
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.04.18 11:55
  * Description: Модель какой-то ключевой информации о изображении или ином объекте,
  * которых на экране может быть одновременно несколько.
  *
  * Например, полезна если на экране сразу несколько изображений, которые могут порождать одинаковые события.
  */
object MFormResourceKey {

  implicit object MFormImgKeyFastEq extends FastEq[MFormResourceKey] {
    override def eqv(a: MFormResourceKey, b: MFormResourceKey): Boolean = {
      (a.frkType ===* b.frkType) &&
      (a.edgeUid ==* b.edgeUid) &&
      (a.nodePath ===* b.nodePath)
    }
  }

  @inline implicit def univEq: UnivEq[MFormResourceKey] = UnivEq.derive

}


/** Контейнер данных с ключевой инфой по ресурсу.
  *
  * @param frkType Ключ-категория.
  * @param edgeUid id эджа, когда известен есть.
  * @param nodePath Путь дa узла в jd-дереве, если есть.
  */
case class MFormResourceKey(
                             frkType      : Option[MFrkType]      = None,
                             edgeUid      : Option[EdgeUid_t]     = None,
                             nodePath     : Option[NodePath_t]    = None,
                           )
