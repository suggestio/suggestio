package models

import io.suggest.ym.model.common.{GeoDistanceQuery, AdnNodesSearchArgsT}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 12.08.14 16:49
 * Description:
 */
case class MAdnNodeSearch(
  qStr        : Option[String] = None,
  companyIds  : Seq[String] = Nil,
  adnSupIds   : Seq[String] = Nil,
  anyOfPersonIds: Seq[String] = Nil,
  advDelegateAdnIds: Seq[String] = Nil,
  withAdnRighs: Seq[AdnRight] = Nil,
  testNode    : Option[Boolean] = None,
  withoutIds  : Seq[String] = Nil,
  geoDistance : Option[GeoDistanceQuery] = None,
  hasLogo     : Option[Boolean] = None,
  withGeoDistanceSort: Option[GeoPoint] = None,
  maxResults  : Int = 10,
  offset      : Int = 0
) extends AdnNodesSearchArgsT
