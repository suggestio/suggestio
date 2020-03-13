package io.suggest.adn.edit.m

import diode.FastEq
import io.suggest.common.empty.EmptyProduct
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.04.18 13:43
  * Description: Список ошибок в редакторе.
  */
object MAdnEditErrors {

  // TODO Надо заменить эту модель на MMetaPub, поля использовать как поля ошибки.
  //      Единственная проблема, что поле name там не опциональное, а поле colors вообще не нужно.

  def empty = MAdnEditErrors()

  implicit object MAdnEditErrorsFastEq extends FastEq[MAdnEditErrors] {
    override def eqv(a: MAdnEditErrors, b: MAdnEditErrors): Boolean = {
      (a.name ===* b.name) &&
      (a.town ===* b.town) &&
      (a.address ===* b.address) &&
      (a.siteUrl ===* b.siteUrl) &&
      (a.info ===* b.info) &&
      (a.humanTraffic ===* b.humanTraffic) &&
      (a.audienceDescr ===* b.audienceDescr)
    }
  }

  @inline implicit def univEq: UnivEq[MAdnEditErrors] = UnivEq.derive

}


/** Контейнер ошибок в полях.
  *
  * @param name Messages-код ошибки в поле названия.
  */
case class MAdnEditErrors(
                           name           : Option[String] = None,
                           town           : Option[String] = None,
                           address        : Option[String] = None,
                           siteUrl        : Option[String] = None,
                           info           : Option[String] = None,
                           humanTraffic   : Option[String] = None,
                           audienceDescr  : Option[String] = None,
                         )
  extends EmptyProduct
{

  def withName(name: Option[String]) = copy(name = name)
  def withTown(town: Option[String]) = copy(town = town)
  def withAddress(address : Option[String]) = copy(address = address)
  def withSiteUrl(siteUrl: Option[String]) = copy(siteUrl = siteUrl)
  def withInfo(info: Option[String]) = copy(info = info)
  def withHumanTraffic(humanTraffic: Option[String]) = copy(humanTraffic = humanTraffic)
  def withAudienceDescr(audienceDescr: Option[String]) = copy(audienceDescr = audienceDescr)

}
