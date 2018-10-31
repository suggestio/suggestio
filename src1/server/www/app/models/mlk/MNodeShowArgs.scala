package models.mlk

import io.suggest.mbill2.m.item.MItem
import io.suggest.model.n2.node.{MNode, MNodeType}
import play.api.mvc.Call

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.09.15 12:08
 * Description: Модель передачи аргументов рендера в шаблон adnNodeShowTpl.
 * @param ownedNodesStats Статистика по узлам, которыми владеет данный текущий узел.
 */

case class MNodeShowArgs(
                          mnode           : MNode,
                          logoImgCallOpt  : Option[Call],
                          bgColor         : String,
                          fgColor         : String,
                          gallery         : Seq[Call],
                          ownedNodesStats : Map[MNodeType, Long],
                          adnMapAdvs      : Seq[MItem],
                        )

