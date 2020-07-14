package io.suggest.sc.m.inx

import diode.FastEq
import diode.data.Pot
import io.suggest.sc.index.MSc3IndexResp
import io.suggest.sc.m.menu.MMenuS
import io.suggest.sc.m.search.{MNodesFoundRowProps, MScSearch}
import io.suggest.sc.v.styl.ScCss
import io.suggest.ueq.JsUnivEqUtil._
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.07.17 10:31
  * Description: Модель, описывающая индекс выдачи и его состояния.
  */
object MScIndex {

  implicit object MScIndexFastEq extends FastEq[MScIndex] {
    override def eqv(a: MScIndex, b: MScIndex): Boolean = {
      (a.state    ===* b.state) &&
      (a.resp     ===* b.resp) &&
      (a.welcome  ===* b.welcome) &&
      (a.search   ===* b.search) &&
      (a.scCss    ===* b.scCss) &&
      (a.menu     ===* b.menu)
    }
  }

  @inline implicit def univEq: UnivEq[MScIndex] = UnivEq.derive

  val state   = GenLens[MScIndex](_.state)
  def resp    = GenLens[MScIndex](_.resp)
  val welcome = GenLens[MScIndex](_.welcome)
  val search  = GenLens[MScIndex](_.search)
  val scCss   = GenLens[MScIndex](_.scCss)
  val menu    = GenLens[MScIndex](_.menu)


  implicit final class MScIndexOpsExt( private val scIndex: MScIndex ) extends AnyVal {

    def isAnyPanelOpened: Boolean =
      scIndex.search.panel.opened || scIndex.menu.opened

    /** Первый запуск? */
    def isFirstRun: Boolean =
      scIndex.resp.isEmpty

  }

}


case class MScIndex(
                     state      : MScIndexState,
                     resp       : Pot[MSc3IndexResp]      = Pot.empty,
                     welcome    : Option[MWelcomeState]   = None,
                     search     : MScSearch,
                     scCss      : ScCss,
                     menu       : MMenuS                  = MMenuS.empty,
                   ) {

  /** Выбранные id узлов. */
  lazy val searchNodesSelectedIds: Set[String] = {
    val ids0 = search.geo.data.selTagIds
    state.rcvrId.fold(ids0)(ids0 + _)
  }

  lazy val respOpt = resp.toOption

  /** Кэширование данных для рендера рядов NodeFoundR. Обычно тут Nil. */
  lazy val searchGeoNodesFoundProps: Seq[MNodesFoundRowProps] = {
    // Нельзя nodeId.get, т.к. могут быть узлы без id.
    val g = search.geo

    (for {
      req <- g.found.req.iterator
      mnode <- req.resp.nodes
    } yield {
      val nodeId = mnode.props.idOrNameOrEmpty
      // Рендер одного ряда. На уровне компонента обитает shouldComponentUpdate() для
      MNodesFoundRowProps(
        node                = mnode,
        searchCss           = g.css,
        withDistanceToNull  = g.mapInit.state.center,
        selected            = searchNodesSelectedIds contains nodeId,
      )
    })
      .to( List )
  }

}
