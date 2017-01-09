package io.suggest.sc.sjs.m.msrv.nodes.find

import io.suggest.sc.NodeSearchConstants._
import io.suggest.sc.sjs.m.mgeo.IMGeoMode
import io.suggest.sc.sjs.m.msrv.ToJsonWithApiVsnT

import scala.scalajs.js.{Dictionary, Any}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.06.15 15:33
 * Description: Модель аргументов запроса списка узлов.
 */
trait MFindNodesArgs extends ToJsonWithApiVsnT {

  def ftsQuery    : Option[String]
  def geo         : Option[IMGeoMode]
  def offset      : Option[Int]
  def limit       : Option[Int]
  def currAdnId   : Option[String]
  def nodeSwitch  : Option[Boolean]
  def withNeigh   : Option[Boolean]

  override def toJson: Dictionary[Any] = {
    val d = super.toJson

    for (fq <- ftsQuery)
      d(FTS_QUERY_FN) = fq
    for (g <- geo)
      d(GEO_FN) = g.toQsStr
    for (off <- offset)
      d(OFFSET_FN) = off
    for (lim <- limit)
      d(LIMIT_FN) = lim
    for (cai <- currAdnId)
      d(CURR_ADN_ID_FN) = cai
    for (ns <- nodeSwitch)
      d(NODE_SWITCH_FN) = ns
    for (wn <- withNeigh)
      d(WITH_NEIGHBORS_FN) = wn

    d
  }

}

/** Трейт с пустой реализацией [[MFindNodesArgs]]. */
trait MFindNodesArgsDflt extends MFindNodesArgs {
  override def ftsQuery   : Option[String]    = None
  override def limit      : Option[Int]       = None
  override def nodeSwitch : Option[Boolean]   = None
  override def currAdnId  : Option[String]    = None
  override def offset     : Option[Int]       = None
  override def withNeigh  : Option[Boolean]   = None
  override def geo        : Option[IMGeoMode] = None
}

/** Дефолтовая реализация трейта [[MFindNodesArgsDflt]]. */
class MFindNodesArgsDfltImpl
  extends MFindNodesArgsDflt