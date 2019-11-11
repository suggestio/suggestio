package models.msys

import io.suggest.mbill2.m.item.MItem
import io.suggest.model.n2.node.MNode
import io.suggest.model.n2.node.search.MNodeSearch
import io.suggest.sc.sc3.MScQs
import models.blk

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.11.15 12:28
  * Description: Модель контейнера аргументов для шаблона [[views.html.sys1.market.adn.showAdnNodeAdsTpl]].
  */
case class MShowNodeAdsTplArgs(
                                mads         : Seq[blk.IRenderArgs],
                                nodeOpt      : Option[MNode],
                                rcvrsMap     : Map[String, Seq[MNode]],
                                qs           : MScQs,
                                ad2advMap    : Map[String, Iterable[MItem]],
                                msearch      : MNodeSearch
                              )
