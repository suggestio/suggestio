package io.suggest.adn.edit.m

import io.suggest.spa.DAction

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.04.18 13:15
  * Description: Экшены формы редактирования ADN-узла.
  */
sealed trait ILkAdnEditAction extends DAction

/** Обновить название узла. */
case class SetName(name: String) extends ILkAdnEditAction

/** Обновить город узла. */
case class SetTown(town: String) extends ILkAdnEditAction

/** Обновить адресок. */
case class SetAddress(address: String) extends ILkAdnEditAction

/** Обновить ссылку на сайт. */
case class SetSiteUrl(siteUrl: String) extends ILkAdnEditAction

/** Обновить инфу по товарам и услугам. */
case class SetInfo(infoAboutProducts: String) extends ILkAdnEditAction

/** Обновление значения человеческого траффика. */
case class SetHumanTraffic(humanTraffic: String) extends ILkAdnEditAction

/** Обновление инфы по описанию аудитории. */
case class SetAudienceDescr(audienceDescr: String) extends ILkAdnEditAction

