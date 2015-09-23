package io.suggest.model.n2.tag

import io.suggest.model.n2.node.MNode
import io.suggest.model.n2.tag.vertex.{EMTagVertexStaticT, EMTagVertex, MTagVertex}
import play.api.libs.json.OFormat
import play.api.libs.functional.syntax._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.09.15 10:25
 * Description: Модель-контейнер для различных tag-субмоделей (vertex, edge, ...) одного узла N2.
 * Модель используется только для удобной группировки tag-подмоделей, функциональная составляющая отсутствует.
 */
object MNodeTagInfo {

  /** Расшаренный пустой экземпляр для дедубликации пустых инстансов контейнера в памяти. */
  val empty = MNodeTagInfo()

  implicit val FORMAT: OFormat[MNodeTagInfo] = {
    EMTagVertex.FORMAT.inmap[MNodeTagInfo](
      {vertex =>
        // Если все аргументы пустые, то вернуть empty вместо создания нового, неизменяемо пустого инстанса.
        if (vertex.isEmpty)
          empty
        else
          MNodeTagInfo(vertex)
      },
      { _.vertex }
    )
  }

}


/** Аддон для модели [[MNode]] для поддержки маппинга полей. */
trait MNodeTagInfoMappingT
  extends EMTagVertexStaticT


/** Класс-контейнер-реализация модели. */
case class MNodeTagInfo(
  vertex: Option[MTagVertex] = None
)
