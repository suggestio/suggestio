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

  def empty = MAdnEditErrors()

  implicit object MAdnEditErrorsFastEq extends FastEq[MAdnEditErrors] {
    override def eqv(a: MAdnEditErrors, b: MAdnEditErrors): Boolean = {
      (a.name ===* b.name) &&
        (a.town ===* b.town) &&
        (a.address ===* b.address) &&
        (a.siteUrl ===* b.siteUrl)
    }
  }

  implicit def univEq: UnivEq[MAdnEditErrors] = UnivEq.derive

}


/** Контейнер ошибок в полях.
  *
  * @param name Messages-код ошибки в поле названия.
  */
case class MAdnEditErrors(
                           name    : Option[String] = None,
                           town    : Option[String] = None,
                           address : Option[String] = None,
                           siteUrl : Option[String] = None,
                         )
  extends EmptyProduct
{

  def withName(name: Option[String]) = copy(name = name)
  def withTown(town: Option[String]) = copy(town = town)
  def withAddress(address : Option[String]) = copy(address = address)
  def withSiteUrl(siteUrl: Option[String]) = copy(siteUrl = siteUrl)

}
