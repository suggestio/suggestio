package io.suggest.adn.edit.m

import io.suggest.common.html.HtmlConstants.SPACE
import io.suggest.img.MImgEdgeWithOps
import io.suggest.jd.MJdEdgeId
import io.suggest.lk.c.IPictureViewAdp
import io.suggest.lk.m.MFormResourceKey
import io.suggest.model.n2.edge.{EdgeUid_t, MPredicates}
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.04.18 22:10
  * Description: Доп.утиль для модели MAdnResView.
  */
object MAdnResViewUtil {

  implicit object ResViewAdp extends IPictureViewAdp[MAdnResView] {

    override def get(view: MAdnResView, resKey: MFormResourceKey): Option[MImgEdgeWithOps] = {
      val edgeWithOpsF = MImgEdgeWithOps(_: MJdEdgeId)
      resKey.pred.get match {
        case MPredicates.Logo =>
          view.logo.map(edgeWithOpsF)
        case MPredicates.WcFgImg =>
          view.wcFg.map(edgeWithOpsF)
        case other =>
          throw new IllegalArgumentException(resKey + SPACE + other)
      }
    }

    override def updated(view: MAdnResView, resKey: MFormResourceKey)(newValue: Option[MImgEdgeWithOps]): MAdnResView = {
      def newValueEdgeId = newValue.map(_.imgEdge)
      resKey.pred.get match {
        case MPredicates.Logo =>
          view.withLogo( newValueEdgeId )
        case MPredicates.WcFgImg =>
          view.withWcFg( newValueEdgeId )
        case other =>
          throw new IllegalArgumentException(resKey + SPACE + newValue + SPACE + other)
      }
    }

    override def forgetEdge(view: MAdnResView, edgeUid: EdgeUid_t): MAdnResView = {
      def __jdEdgeF(jdEdge: MJdEdgeId): Boolean =
        jdEdge.edgeUid !=* edgeUid

      view.copy(
        logo = view.logo.filter(__jdEdgeF),
        wcFg = view.wcFg.filter(__jdEdgeF)
      )
    }

  }

}
