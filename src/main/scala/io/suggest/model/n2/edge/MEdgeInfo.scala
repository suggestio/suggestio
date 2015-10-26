package io.suggest.model.n2.edge

import io.suggest.common.{EmptyProduct, IEmpty}
import io.suggest.model.es.IGenEsMappingProps
import io.suggest.util.SioConstants
import io.suggest.ym.model.common.SinkShowLevel
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.10.15 12:47
 * Description: У рёбер [[io.suggest.model.n2.edge.MEdge]] могут быть дополнительные данные.
 * Здесь модель для этих опциональных данных.
 * Основное требование тут -- стараться избегать nested-объектов, т.к. тут уже nested-документ.
 */
object MEdgeInfo extends IGenEsMappingProps {

  val DYN_IMG_ARGS_FN     = "di"
  val SLS_FN              = "sls"

  /** Поддержка JSON. */
  implicit val FORMAT: Format[MEdgeInfo] = (
    (__ \ DYN_IMG_ARGS_FN).formatNullable[String] and
    (__ \ SLS_FN).format[Set[SinkShowLevel]]
  )(apply, unlift(unapply))

  /** Статический пустой экземпляр модели. */
  val empty = new MEdgeInfo() {
    override def nonEmpty = false
  }


  import io.suggest.util.SioEsUtil._
  
  /** Сборка полей ES-маппинга. */
  override def generateMappingProps: List[DocField] = {
    List(
      FieldString(DYN_IMG_ARGS_FN, index = FieldIndexingVariants.no, include_in_all = false),
      FieldString(SLS_FN,
        index           = FieldIndexingVariants.analyzed,
        include_in_all  = false,
        index_analyzer  = SioConstants.DEEP_NGRAM_AN,
        search_analyzer = SioConstants.MINIMAL_AN
      )
    )
  }

}


/** Интерфейс элементов модели. */
trait IEdgeInfo extends IEmpty {
  /** При указании на картинку бывает нужно указать исходный кроп или что-то ещё. */
  def dynImgArgs   : Option[String]
  /** При публикации карточке где-то нужно указывать show levels, т.е. где именно карточка отображается. */
  def sls          : Set[SinkShowLevel]
}


case class MEdgeInfo(
  dynImgArgs   : Option[String]       = None,
  sls          : Set[SinkShowLevel]   = Set.empty
)
  extends EmptyProduct
  with IEdgeInfo
{

  /** Форматирование для вывода в шаблонах. */
  override def toString: String = {
    if (nonEmpty) {
      val sb = new StringBuilder(32)
      dynImgArgs.foreach { dia =>
        sb.append("dynImg=").append(dia).append(" ")
      }
      if (sls.nonEmpty) {
        sb.append("sls=").append( sls.mkString(",") )
      }
      sb.toString()

    } else {
      ""
    }
  }

}
