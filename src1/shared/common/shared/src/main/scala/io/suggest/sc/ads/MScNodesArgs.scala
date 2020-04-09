package io.suggest.sc.ads

import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.04.2020 16:43
  * Description: Контейнер данных для nodes search.
  */
object MScNodesArgs {

  object Fields {
    // TODO searchNodes: Это поле, просто чтобы было. Если нет полей, то qsb будет пуст. Удалить searchNodes, когда будут другие поля.
    val SEARCH_NODES_FN = "n"
  }

  implicit def scNodesArgsJson: OFormat[MScNodesArgs] = {
    val F = Fields
    (__ \ F.SEARCH_NODES_FN).format[Boolean]
      .inmap[MScNodesArgs]( apply, _._searchNodes )
  }

  @inline implicit def univEq: UnivEq[MScNodesArgs] = UnivEq.derive

}


/** Контейнер аргументов поискать узлов и тегов.
  *
  * @param _searchNodes Поле, чтобы было хотя бы одно поле.
  *                     true/false - значение уже утратило свой ранний смысл, хотя и то, и другое значение в коде пока сохранено.
  */
final case class MScNodesArgs(
                               // TODO searchNodes: Это поле, просто чтобы было. Если нет полей, то qsb будет пуст. Удалить searchNodes, когда будут другие поля.
                               _searchNodes       : Boolean         = true,
                             )
