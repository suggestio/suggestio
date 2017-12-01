package io.suggest.sc.search.m

import diode.FastEq
import diode.data.Pot
import io.suggest.common.empty.NonEmpty
import io.suggest.sc.sc3.MSc3Tag
import io.suggest.ueq.UnivEqUtil._
import io.suggest.ueq.JsUnivEqUtil._
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.11.17 16:45
  * Description: Модель состояния вкладки со списком тегов.
  */
object MTagsSearchS {

  val empty = apply()

  /** Поддержка FastEq для инстансов [[MTagsSearchS]]. */
  implicit object MTagsSearchFastEq extends FastEq[MTagsSearchS] {
    override def eqv(a: MTagsSearchS, b: MTagsSearchS): Boolean = {
      (a.tagsReq        ===*  b.tagsReq) &&
        (a.hasMoreTags   ==*   b.hasMoreTags) &&
        (a.selectedId   ===*  b.selectedId)
    }
  }

  implicit def univEq: UnivEq[MTagsSearchS] = UnivEq.derive

}


/** Класс модели состояния вкладки с тегами.
  *
  * @param tagsReq Состояние реквеста с тегами.
  * @param selectedId Выбранный тег в списке.
  * @param hasMoreTags Есть ли ещё теги на сервере?
  */
case class MTagsSearchS(
                         tagsReq      : Pot[Seq[MSc3Tag]]     = Pot.empty,
                         hasMoreTags  : Boolean               = true,
                         // TODO Когда станет допустимо сразу несколько тегов, надо заменить на Set[String].
                         selectedId   : Option[String]        = None
                       )
  extends NonEmpty
{

  def withTagsReq(tagsReq: Pot[Seq[MSc3Tag]])       = copy(tagsReq = tagsReq)
  def withHasMoreTags(hasMoreTags: Boolean)         = copy(hasMoreTags = hasMoreTags)
  def withSelectedId(selectedId: Option[String])    = copy(selectedId = selectedId)

  override def isEmpty: Boolean = {
    MTagsSearchS.empty ===* this
  }

}
