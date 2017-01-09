package io.suggest.lk.adv.geo.m

import diode.FastEq
import io.suggest.adv.geo.MFormS
import io.suggest.dt.MAdvPeriod
import io.suggest.lk.tags.edit.m.MTagsEditState

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.12.16 11:16
  * Description: Корневая js-only модель diode-формы.
  * В ней хранится вообще всё, но сериализуемые для отправки на сервер данные хранятся в отдельных полях.
  *
  * @param form Состояние формы размещения, пригодное для сериализации на сервер или десериализации с сервера.
  * @param geoAdv Данные по текущим георазмещениям на карте.
  * @param tags Контейнер данных по тегам.
  */
case class MRoot(
                  form          : MFormS,
                  tags          : MTagsEditState          = MTagsEditState.empty,
                  rcvr          : MRcvr                   = MRcvr(),
                  geoAdv        : MGeoAdvs                = MGeoAdvs(),
                  datePeriod    : MAdvPeriod              = MAdvPeriod()
                  // TODO areaPopup: Pot[???] = Pot.empty
) {

  def withForm(form2: MFormS) = copy(form = form2)
  def withTagsEditState(tes: MTagsEditState) = copy(tags = tes)
  def withRcvr(rcvr2: MRcvr) = copy(rcvr = rcvr2)
  def withCurrGeoAdvs(cga2: MGeoAdvs) = copy(geoAdv = cga2)
  def withDatePeriod(ivl: MAdvPeriod) = copy(datePeriod = ivl)

}

object MRoot {

  /** Реализация поддержки FastEq для инстансов [[MRoot]]. */
  implicit object MRootFastEq extends FastEq[MRoot] {
    override def eqv(a: MRoot, b: MRoot): Boolean = {
      (a.form eq b.form) &&
        (a.tags eq b.tags) &&
        (a.rcvr eq b.rcvr) &&
        (a.geoAdv eq b.geoAdv) &&
        (a.datePeriod eq b.datePeriod)
    }
  }

}
