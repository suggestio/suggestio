package io.suggest.sc.sjs.m.msrv.nodes.find

import io.suggest.sc.NodeSearchConstants._
import io.suggest.sc.sjs.m.mgeo.IMGeoMode
import io.suggest.sc.sjs.m.msc.fsm.MScFsm
import io.suggest.sc.sjs.m.msrv.MSrv

import scala.scalajs.js.{Dictionary, Any}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.06.15 15:33
 * Description:
 */
trait MFindNodesArgs {

  def ftsQuery    : Option[String]
  def geo         : Option[IMGeoMode]
  def offset      : Option[Int]
  def limit       : Option[Int]
  def currAdnId   : Option[String]
  def nodeSwitch  : Option[Boolean]
  def withNeigh   : Option[Boolean]
  def apiVsn      : Int

  def toJson: Dictionary[Any] = {
    val d = Dictionary[Any](
      API_VSN_FN -> apiVsn
    )

    val _ftsQuery = ftsQuery
    if (_ftsQuery.isDefined)
      d.update(FTS_QUERY_FN, _ftsQuery.get)

    val _geo = geo
    if (_geo.isDefined)
      d.update(GEO_FN, _geo.get.toQsStr)

    val _offset = offset
    if (_offset.isDefined)
      d.update(OFFSET_FN, _offset.get)

    val _limit = limit
    if (_limit.isDefined)
      d.update(LIMIT_FN, _limit.get)

    val _currAdnId = currAdnId
    if (_currAdnId.isDefined)
      d.update(CURR_ADN_ID_FN, _currAdnId.get)

    val _nodeSwitch = nodeSwitch
    if (_nodeSwitch.isDefined)
      d.update(NODE_SWITCH_FN, _nodeSwitch.get)

    val _withNeight = withNeigh
    if (_withNeight.isDefined)
      d.update(WITH_NEIGHBORS_FN, _withNeight.get)

    d
  }

}

/** Трейт с пустой реализацией [[MFindNodesArgs]]. */
trait MFindNodesArgsEmpty extends MFindNodesArgs {
  override def ftsQuery   : Option[String]    = None
  override def limit      : Option[Int]       = None
  override def nodeSwitch : Option[Boolean]   = None
  override def currAdnId  : Option[String]    = None
  override def offset     : Option[Int]       = None
  override def withNeigh  : Option[Boolean]   = None
  override def geo        : Option[IMGeoMode] = None
}

/** Трейт с дефолтовыми значениями, которые извлекаются из моделей состояния выдачи. */
trait MFindNodesArgsDflt extends MFindNodesArgs {
  override def currAdnId  : Option[String] = MScFsm.state.rcvrAdnId
  override def apiVsn     : Int = MSrv.apiVsn
}