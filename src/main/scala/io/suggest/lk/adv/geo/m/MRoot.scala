package io.suggest.lk.adv.geo.m

import diode.data.Pot
import io.suggest.adv.geo.{MFormS, MRcvrPopupResp}
import io.suggest.common.tags.edit.MTagsFoundResp

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.12.16 11:16
  * Description: Корневая js-only модель diode-формы.
  * В ней хранится вообще всё, но сериализуемые для отправки на сервер данные хранятся в отдельных полях.
  *
  * @param form Состояние формы размещения, пригодное для сериализации на сервер или десериализации с сервера.
  * @param rcvrPopup Потенциальные данные попапа над маркером (точкой) ресивера.
  *                  Приходят с сервера по запросу, однако сам факт наличия/необходимости
  *                  такого запроса отражается в form.rcvrPopup.
  */
case class MRoot(
  form      : MFormS,
  rcvrPopup : Pot[MRcvrPopupResp] = Pot.empty,
  tagsFound : Pot[MTagsFoundResp] = Pot.empty
  // TODO areaPopup: Pot[???] = Pot.empty
) {

  def withForm(form2: MFormS) = copy(form = form2)
  def withTagsFound(tf: Pot[MTagsFoundResp]) = copy(tagsFound = tf)

}
